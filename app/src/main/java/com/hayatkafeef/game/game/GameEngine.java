package com.hayatkafeef.game.game;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hayatkafeef.game.ai.EnvDescriber;
import com.hayatkafeef.game.audio.SpatialAudio;
import com.hayatkafeef.game.audio.TtsManager;
import com.hayatkafeef.game.haptics.HapticManager;
import com.hayatkafeef.game.render.GameView;
import com.hayatkafeef.game.render.VisionMode;

/**
 * Glue layer that owns the running game and reacts to player input.
 */
public class GameEngine implements EventSystem.Effects {

    public interface View {
        void onState(GameState gs);
        void onMessage(String msg);
        void requestDialog(Entity e);
        void onSceneTransition(Scene to);
    }

    private final Context ctx;
    private final TtsManager tts;
    private final SpatialAudio audio;
    private final HapticManager haptics;
    private final GameView gameView;
    private final View bridge;
    private final EventSystem events = new EventSystem();
    private final Handler main = new Handler(Looper.getMainLooper());

    private GameState state = new GameState();
    private VisionMode vision = VisionMode.SIGHTED;
    private boolean walking = false;
    private long lastTickMs;

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
                      SpatialAudio audio, HapticManager haptics, View bridge) {
        this.ctx = ctx;
        this.gameView = gv;
        this.tts = tts;
        this.audio = audio;
        this.haptics = haptics;
        this.bridge = bridge;
    }

    public GameState state() { return state; }
    public void setState(GameState s) {
        this.state = s;
        if (s.scene == null) s.scene = Scenes.create(s.currentSceneId);
        gameView.setState(s);
    }
    public void setVisionMode(VisionMode m) {
        this.vision = m;
        gameView.setVisionMode(m);
    }
    public VisionMode getVisionMode() { return vision; }

    public void start() { main.postDelayed(tick, 50); }
    public void stop() { main.removeCallbacks(tick); audio.stopAll(); }

    private void update(long dtMs) {
        if (state == null || state.scene == null) return;
        // walking
        if (walking) {
            float speed = 1.2f; // tiles/second
            float step = speed * (dtMs / 1000f);
            float nx = state.player.x + (float) Math.cos(state.player.heading) * step;
            float ny = state.player.y + (float) Math.sin(state.player.heading) * step;
            if (!collides(nx, ny)) {
                state.player.x = nx;
                state.player.y = ny;
            } else {
                walking = false;
                haptics.warn();
                tts.speakNow(blockedMessage());
            }
            // clamp to scene bounds
            state.player.x = clamp(state.player.x, 0.5f, state.scene.width - 0.5f);
            state.player.y = clamp(state.player.y, 0.5f, state.scene.height - 0.5f);
            // pulse footstep haptic and a soft footstep sound
            if (System.currentTimeMillis() % 600 < 50) {
                haptics.tick();
                audio.cue("step", 90, 80, 0f, 0.3f, true);
            }
            state.tickMinutes(0); // walking doesn't add to clock burst; events do
        }

        // proximity haptics for nearest entity
        Entity near = state.scene.nearest(state.player.x, state.player.y, 1.6f);
        if (near != null) {
            float dx = near.x - state.player.x, dy = near.y - state.player.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 0.9f) hapticForKind(near.kind);
        }

        // pulse ambient sound for entities that make sound
        for (Entity e : state.scene.entities) {
            if (!e.makesSound) continue;
            float[] pv = SpatialAudio.panAndVolume(state.player.x, state.player.y,
                    state.player.heading, e.x, e.y, 12f);
            if (pv[1] <= 0.05f) continue;
            // only cue occasionally to avoid spam
            if (Math.random() < 0.012) {
                int role = e.soundRole;
                int hz = e.soundHz > 0 ? e.soundHz : 200;
                boolean noisy = role == 2; // hazard = engine-like
                audio.cue("ent_" + e.id, hz, 350, pv[0], pv[1] * 0.55f, noisy);
            }
        }

        // random events
        String ev = events.maybeFire(state, this);
        if (ev != null) {
            tts.speak(ev);
            bridge.onMessage(ev);
        }

        bridge.onState(state);
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

    // -------- input commands ----------

    public void cmdWalk() {
        walking = true;
        tts.speakNow("مشي");
        haptics.confirm();
    }
    public void cmdStop() {
        walking = false;
        tts.speakNow("توقف");
        haptics.confirm();
    }
    public void cmdTurnRight() {
        state.player.heading += (float) (Math.PI / 8.0);
        tts.speakNow("يمين");
        haptics.confirm();
    }
    public void cmdTurnLeft() {
        state.player.heading -= (float) (Math.PI / 8.0);
        tts.speakNow("يسار");
        haptics.confirm();
    }
    public void cmdInteract() {
        Entity e = state.scene.nearest(state.player.x, state.player.y, 1.6f);
        if (e == null) {
            tts.speakNow("لا يوجد شيء قريب للتفاعل معه.");
            return;
        }
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
        // generic
        tts.speakNow(e.name + ". " + (e.tag != null ? e.tag : ""));
        haptics.confirm();
    }
    public void cmdDescribe() {
        String desc = EnvDescriber.describe(state);
        tts.speakNow(desc);
        bridge.onMessage(desc);
    }
    public void cmdShakeReorient() {
        // Snap heading to nearest cardinal direction (east/south/west/north)
        double a = state.player.heading;
        while (a < 0) a += 2 * Math.PI;
        while (a >= 2 * Math.PI) a -= 2 * Math.PI;
        double snap = Math.round(a / (Math.PI / 2)) * (Math.PI / 2);
        state.player.heading = (float) (snap % (2 * Math.PI));
        tts.speakNow("أعيد ضبط الاتجاه. " + EnvDescriber.describeShort(state));
        haptics.confirm();
    }

    private void doTransition(Scene.Id target) {
        Scene to = Scenes.create(target);
        // Place player near a corresponding return door if found, else just (2,2)
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
        tts.speakNow(EnvDescriber.describeOnArrival(to));
        haptics.confirm();
        state.tickMinutes(5);
    }

    /** Apply dialogue choice effects. */
    public void applyChoiceEffects(DialogueSystem.Choice c) {
        if (c == null) return;
        state.player.mobility += c.dMobility;
        state.player.social += c.dSocial;
        state.player.tech += c.dTech;
        state.player.confidence += c.dConfidence;
        state.player.clamp();
        if (c.flag != null) state.flags.add(c.flag);
        state.tickMinutes(3);
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
