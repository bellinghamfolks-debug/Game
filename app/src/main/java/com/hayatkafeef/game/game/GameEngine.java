package com.hayatkafeef.game.game;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hayatkafeef.game.ai.AiClient;
import com.hayatkafeef.game.ai.AiDialogueManager;
import com.hayatkafeef.game.ai.AiHintManager;
import com.hayatkafeef.game.ai.AiSafetyFilter;
import com.hayatkafeef.game.ai.AiSummaryManager;
import com.hayatkafeef.game.ai.CharacterMemory;
import com.hayatkafeef.game.ai.EnvDescriber;
import com.hayatkafeef.game.ai.NavGuide;
import com.hayatkafeef.game.ai.OfflineContentProvider;
import com.hayatkafeef.game.audio.SpatialAudio;
import com.hayatkafeef.game.audio.TtsManager;
import com.hayatkafeef.game.haptics.HapticManager;
import com.hayatkafeef.game.missions.DayScript;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.missions.MissionManager;
import com.hayatkafeef.game.render.GameView;
import com.hayatkafeef.game.render.VisionMode;
import com.hayatkafeef.game.world.EventLog;
import com.hayatkafeef.game.world.HazardSystem;
import com.hayatkafeef.game.world.PlayerProfile;

/**
 * Central game controller — owns the world state, render bridge, input
 * commands, missions, AI managers, and the live update loop.
 */
public class GameEngine implements EventSystem.Effects {

    public interface View {
        void onState(GameState gs);
        void onMessage(String msg);
        void requestDialog(Entity e);
        void onSceneTransition(Scene to);
        void onMissionChanged(Mission previous, Mission next);
        void onDayCompleted(int day);
        /** A new hazard is armed; the player has reactionWindowMs to stop. */
        void onHazardWarning(String text, String shortLabel, long reactionWindowMs);
        /** The hazard has resolved (impact==true means the player was hit). */
        void onHazardResolved(String text, boolean impact);
    }

    // dependencies
    private final Context ctx;
    private final TtsManager tts;
    private final SpatialAudio audio;
    private final HapticManager haptics;
    private final GameView gameView;
    private final View bridge;
    private final Prefs prefs;

    // world systems
    private final EventSystem events = new EventSystem();
    private final NavGuide nav = new NavGuide();
    private final EventLog log = new EventLog();
    private final HazardSystem hazards = new HazardSystem();
    private final PlayerProfile profile = new PlayerProfile();

    // AI layer
    private final AiSafetyFilter aiSafety = new AiSafetyFilter();
    private AiClient aiClient;
    private AiHintManager hintMgr;
    private AiSummaryManager summaryMgr;
    private AiDialogueManager dialogueMgr;
    private CharacterMemory characterMemory;

    private MissionManager missions;
    private final Handler main = new Handler(Looper.getMainLooper());

    private GameState state = new GameState();
    private VisionMode vision = VisionMode.SIGHTED;
    private boolean walking = false;
    private long lastTickMs;
    private String lastSpoken = "";

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long dt = lastTickMs == 0 ? 30 : (now - lastTickMs);
            lastTickMs = now;
            update(dt);
            main.postDelayed(this, 50);
        }
    };

    public GameEngine(Context ctx, GameView gv, TtsManager tts,
                      SpatialAudio audio, HapticManager haptics, View bridge, Prefs prefs) {
        this.ctx = ctx;
        this.gameView = gv;
        this.tts = tts;
        this.audio = audio;
        this.haptics = haptics;
        this.bridge = bridge;
        this.prefs = prefs;
        rebuildAi();
        // Load persisted character memory.
        this.characterMemory = CharacterMemory.fromJson(prefs.aiMemory());
        // dialogue manager depends on memory
        this.dialogueMgr = new AiDialogueManager(aiClient, aiSafety, characterMemory);
        // difficulty -> hazards
        this.hazards.setDifficulty(prefs.difficulty());
    }

    public PlayerProfile profile() { return profile; }
    public HazardSystem hazards() { return hazards; }

    /** Recreate AI managers when settings change. */
    public void rebuildAi() {
        this.aiClient = AiClient.fromPrefs(prefs.geminiKey(), prefs.geminiModel(), prefs.proxyUrl());
        this.hintMgr = new AiHintManager(aiClient, aiSafety);
        this.summaryMgr = new AiSummaryManager(aiClient, aiSafety);
        if (this.characterMemory != null) {
            this.dialogueMgr = new AiDialogueManager(aiClient, aiSafety, characterMemory);
        }
    }

    public GameState state() { return state; }
    public EventLog log() { return log; }
    public MissionManager missions() { return missions; }
    public NavGuide nav() { return nav; }
    public CharacterMemory characterMemory() { return characterMemory; }
    public AiDialogueManager dialogue() { return dialogueMgr; }

    public void setState(GameState s) {
        this.state = s;
        if (s.scene == null) s.scene = Scenes.create(s.currentSceneId);
        gameView.setState(s);
        // bind missions for the current day
        this.missions = DayScript.forDay(Math.max(1, s.day));
        this.missions.setListener(new MissionManager.Listener() {
            @Override public void onMissionCompleted(Mission previous, Mission next) {
                onMissionDone(previous, next);
            }
            @Override public void onAllMissionsCompleted() {
                bridge.onDayCompleted(state.day);
                String msg = OfflineContentProvider.dayComplete(state.day);
                addLog(msg);
                tts.speakNow(msg);
            }
        });
        // Restore saved progress within the day.
        this.missions.load(String.valueOf(s.missionIdx));
        this.missions.prime(state);
    }

    /** Wake up the next morning: advance day, reset to home, fresh missions. */
    public void advanceToNextDay() {
        if (state == null) return;
        int next = Math.min(7, state.day + 1);
        state.day = next;
        state.minutes = 0;
        state.missionIdx = 0;
        // keep persistent skill stats, drop transient interaction flags
        state.flags.clear();
        Scene home = Scenes.create(Scene.Id.HOME);
        state.enterScene(home, 4, 5);
        state.player.heading = 0;
        gameView.setState(state);
        this.missions = DayScript.forDay(state.day);
        this.missions.setListener(new MissionManager.Listener() {
            @Override public void onMissionCompleted(Mission previous, Mission next2) {
                onMissionDone(previous, next2);
            }
            @Override public void onAllMissionsCompleted() {
                bridge.onDayCompleted(state.day);
                String msg = OfflineContentProvider.dayComplete(state.day);
                addLog(msg);
                tts.speakNow(msg);
            }
        });
        this.missions.prime(state);
        hazards.reset();
        String intro = OfflineContentProvider.dayIntro(state.day);
        tts.speakNow(intro);
        addLog(intro);
        Mission first = this.missions.current();
        if (first != null) {
            tts.speak("مهمة جديدة: " + first.title + ". " + first.description);
        }
    }

    public void setVisionMode(VisionMode m) {
        this.vision = m;
        gameView.setVisionMode(m);
    }
    public VisionMode getVisionMode() { return vision; }

    public void start() {
        main.removeCallbacks(tick);
        lastTickMs = 0;
        main.postDelayed(tick, 50);
    }
    public void stop() { main.removeCallbacks(tick); audio.stopAll(); persistMemory(); }

    private void persistMemory() {
        if (characterMemory != null) prefs.setAiMemory(characterMemory.toJson());
    }

    private void update(long dtMs) {
        if (state == null || state.scene == null) return;

        if (walking) {
            float baseSpeed = 1.2f;
            float speed = baseSpeed * (0.8f + 0.004f * state.player.mobility);
            float step = speed * (dtMs / 1000f);
            float nx = state.player.x + (float) Math.cos(state.player.heading) * step;
            float ny = state.player.y + (float) Math.sin(state.player.heading) * step;
            if (!collides(nx, ny)) {
                state.player.x = nx;
                state.player.y = ny;
            } else {
                walking = false;
                haptics.warn();
                profile.onCollision();
                String msg = blockedMessage();
                tts.speakNow(msg);
                addLog(msg);
            }
            state.player.x = clamp(state.player.x, 0.5f, state.scene.width - 0.5f);
            state.player.y = clamp(state.player.y, 0.5f, state.scene.height - 0.5f);
            if (System.currentTimeMillis() % 600 < 50) {
                haptics.tick();
                audio.cue("step", 90, 80, 0f, 0.3f, true);
            }
        }

        // proximity haptics
        Entity near = state.scene.nearest(state.player.x, state.player.y, 1.6f);
        if (near != null) {
            float dx = near.x - state.player.x, dy = near.y - state.player.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 0.9f) hapticForKind(near.kind);
        }

        // spatial ambient
        for (Entity e : state.scene.entities) {
            if (!e.makesSound) continue;
            float[] pv = SpatialAudio.panAndVolume(state.player.x, state.player.y,
                    state.player.heading, e.x, e.y, 12f);
            if (pv[1] <= 0.05f) continue;
            if (Math.random() < 0.012) {
                int hz = e.soundHz > 0 ? e.soundHz : 200;
                boolean noisy = e.soundRole == 2;
                audio.cue("ent_" + e.id, hz, 350, pv[0], pv[1] * 0.55f, noisy);
            }
        }

        // hazards (real stakes)
        hazards.tick(state, walking, new HazardSystem.Listener() {
            @Override public void onWarning(String text, HazardSystem.Kind kind) {
                tts.speakNow(text);
                haptics.danger();
                addLog(text);
                String shortLabel = kindShortLabel(kind);
                bridge.onHazardWarning(text, shortLabel, hazards.windowMs());
            }
            @Override public void onResolve(String text, boolean impact, HazardSystem.Kind kind) {
                tts.speakNow(text);
                if (impact) { haptics.error(); profile.onHazardImpact(); }
                else { haptics.confirm(); profile.onHazardSurvived(); }
                addLog(text);
                bridge.onHazardResolved(text, impact);
            }
        });

        // passive flavor events (suppressed at brief / audio-only verbosity)
        int verbosity = effectiveCommentaryLevel();
        if (verbosity <= 1) {
            String ev = events.maybeFire(state, this);
            if (ev != null) {
                tts.speak(ev);
                bridge.onMessage(ev);
                addLog(ev);
            }
        }

        // live navigation
        if (nav.isActive()) {
            String guidance = nav.tick(state);
            if (guidance != null) {
                tts.speak(guidance);
                bridge.onMessage(guidance);
            }
        }

        // mission checks
        if (missions != null) missions.check(state);

        bridge.onState(state);
    }

    private void onMissionDone(Mission previous, Mission next) {
        profile.onMissionDone();
        // Persist progress in the state blob field for save/load.
        if (missions != null) state.missionIdx = missions.progress();
        String done = OfflineContentProvider.missionDone(previous);
        tts.speakNow(done);
        addLog(done);
        bridge.onMissionChanged(previous, next);
        if (next != null) {
            String start = OfflineContentProvider.missionStart(next);
            tts.speak(start);
            addLog("مهمة جديدة: " + next.title);
        }
        haptics.confirm();
    }

    private boolean collides(float x, float y) {
        for (Entity e : state.scene.entities) {
            if (e.kind == Entity.Kind.DOOR) continue;
            float dx = e.x - x, dy = e.y - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < e.radius + 0.35f) return true;
        }
        return false;
    }

    private String blockedMessage() {
        Entity e = state.scene.nearest(state.player.x, state.player.y, 1.2f);
        if (e == null) return "هناك شيء أمامك. توقف.";
        return e.name + " أمامك. توقف.";
    }

    private String kindShortLabel(HazardSystem.Kind k) {
        switch (k) {
            case BICYCLE: return "دراجة";
            case CAR: return "سيارة";
            case OPEN_HOLE: return "عائق";
        }
        return "خطر";
    }

    private void hapticForKind(Entity.Kind k) {
        switch (k) {
            case STAIRS: haptics.stairs(); break;
            case DOOR: haptics.door(); break;
            case PERSON: haptics.person(); break;
            case CAR: case BUS: haptics.danger(); break;
            case WALL: case PILLAR: case TREE: case BENCH: haptics.warn(); break;
            default: haptics.tick();
        }
    }

    private void addLog(String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        log.add(state.formatTime(), trimmed);
        lastSpoken = trimmed;
    }

    // -------- input commands --------

    public void cmdWalk() {
        walking = true;
        if (effectiveCommentaryLevel() <= 1) tts.speakNow("مشي");
        haptics.confirm();
    }
    public void cmdStop() {
        walking = false;
        if (effectiveCommentaryLevel() <= 1) tts.speakNow("توقف");
        haptics.confirm();
    }
    public void cmdTurnRight() {
        state.player.heading += (float) (Math.PI / 8.0);
        if (effectiveCommentaryLevel() <= 1) tts.speakNow("يمين");
        haptics.confirm();
    }
    public void cmdTurnLeft() {
        state.player.heading -= (float) (Math.PI / 8.0);
        if (effectiveCommentaryLevel() <= 1) tts.speakNow("يسار");
        haptics.confirm();
    }
    public void cmdInteract() {
        Entity e = state.scene.nearest(state.player.x, state.player.y, 1.6f);
        if (e == null) {
            tts.speakNow("لا يوجد شيء قريب للتفاعل معه.");
            return;
        }
        // Record interaction flag for missions.
        if (e.id != null) state.flags.add("i:" + e.id);

        if (e.kind == Entity.Kind.DOOR) {
            Scene.Id target = state.scene.exits.get(e.id);
            if (target != null) {
                doTransition(target);
                return;
            }
        }
        if (e.kind == Entity.Kind.PERSON) {
            bridge.requestDialog(e);
            return;
        }
        // generic: speak and log
        String msg = e.name + ". " + (e.tag != null ? e.tag : "");
        tts.speakNow(msg);
        addLog("تفاعلت مع " + e.name);
        haptics.confirm();
    }
    public void cmdDescribe() {
        String desc = EnvDescriber.describe(state);
        tts.speakNow(desc);
        addLog(desc);
        bridge.onMessage(desc);
    }
    public void cmdShakeReorient() {
        double a = state.player.heading;
        while (a < 0) a += 2 * Math.PI;
        while (a >= 2 * Math.PI) a -= 2 * Math.PI;
        double snap = Math.round(a / (Math.PI / 2)) * (Math.PI / 2);
        state.player.heading = (float) (snap % (2 * Math.PI));
        String msg = "أعيد ضبط الاتجاه. " + EnvDescriber.describeShort(state);
        tts.speakNow(msg);
    }

    /** Ask the hint manager (async if AI is on). */
    public void cmdHint() {
        profile.onHint();
        Mission cur = missions == null ? null : missions.current();
        boolean aiOn = prefs.aiEnabled() && prefs.aiMode() >= 1;
        hintMgr.ask(state, cur, aiOn, prefs.aiMode(), (text, fromAi) -> {
            tts.speakNow(text);
            addLog("تلميح: " + text);
            bridge.onMessage(text);
        });
    }

    /** Repeat the last narration. */
    public void cmdRepeat() {
        if (lastSpoken == null || lastSpoken.isEmpty()) {
            tts.speakNow("لا يوجد ما أكرره الآن.");
            return;
        }
        tts.speakNow(lastSpoken);
    }

    public void cmdMissionStatus() {
        Mission cur = missions == null ? null : missions.current();
        if (cur == null) {
            tts.speakNow("اكتملت جميع مهام اليوم.");
            return;
        }
        int p = missions.progress() + 1;
        int t = missions.total();
        String txt = "المهمة " + p + " من " + t + ": " + cur.title + ". " + cur.description;
        tts.speakNow(txt);
        bridge.onMessage(txt);
    }

    public interface SummaryCallback { void onReady(String text, boolean fromAi); }
    public void cmdDaySummary(SummaryCallback cb) {
        boolean aiOn = prefs.aiEnabled() && prefs.aiMode() >= 1;
        summaryMgr.daySummary(state, log, aiOn, prefs.aiMode(),
                (text, fromAi) -> {
                    tts.speak(text);
                    addLog("ملخص اليوم: " + text);
                    if (cb != null) cb.onReady(text, fromAi);
                });
    }

    public NavGuide startNavigation(Entity target) {
        if (target == null) return nav;
        String intro = nav.start(state, target);
        tts.speakNow(intro);
        addLog(intro);
        haptics.confirm();
        return nav;
    }

    public void cancelNavigation() {
        if (!nav.isActive()) return;
        nav.cancel();
        tts.speakNow("ألغيتُ الإرشاد.");
        haptics.confirm();
    }

    private void doTransition(Scene.Id target) {
        Scene to = Scenes.create(target);
        float sx = 2f, sy = 2f;
        for (Entity e : to.entities) {
            if (e.kind == Entity.Kind.DOOR && to.exits.get(e.id) == state.currentSceneId) {
                sx = e.x + 1.0f;
                sy = e.y;
                break;
            }
        }
        state.enterScene(to, sx, sy);
        gameView.setState(state);
        bridge.onSceneTransition(to);
        String msg = EnvDescriber.describeOnArrival(to);
        // Use shorter messages when commentary level is set to brief.
        int level = effectiveCommentaryLevel();
        if (level >= 2) msg = to.name + ".";
        tts.speakNow(msg);
        addLog(msg);
        haptics.confirm();
        state.tickMinutes(5);
        hazards.reset();
    }

    /** Returns the effective verbosity: explicit setting, or adaptive recommendation. */
    private int effectiveCommentaryLevel() {
        int level = prefs.commentaryLevel();
        // If user picked "adaptive" via mode 3 (audio-only) we keep brief.
        // Otherwise treat level 0..2 explicitly. Adaptive is implicit when AI
        // analysis is allowed: it shifts the floor based on struggle score.
        if (prefs.aiAnalysisAllowed() && profile != null) {
            int adaptive = profile.suggestedCommentaryLevel();
            if (level > adaptive) level = adaptive;
        }
        return level;
    }

    /** Apply dialogue choice effects and update relationship memory. */
    public void applyChoiceEffects(Entity npc, DialogueSystem.Choice c) {
        if (c == null) return;
        state.player.mobility += c.dMobility;
        state.player.social += c.dSocial;
        state.player.tech += c.dTech;
        state.player.confidence += c.dConfidence;
        state.player.clamp();
        if (c.flag != null) state.flags.add(c.flag);
        state.tickMinutes(3);
        if (dialogueMgr != null) dialogueMgr.recordChoice(npc, c);
        persistMemory();
    }

    // EventSystem.Effects
    @Override public void applyHaptic(int kind) {
        switch (kind) {
            case 1: haptics.tick(); break;
            case 2: haptics.warn(); break;
            case 3: haptics.danger(); break;
            case 4: haptics.stairs(); break;
        }
    }
    @Override public void applyDelay(int minutes) { state.tickMinutes(minutes); }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
