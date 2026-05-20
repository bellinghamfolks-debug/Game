package com.hayatkafeef.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hayatkafeef.game.ai.AiDialogueManager;
import com.hayatkafeef.game.ai.EnvDescriber;
import com.hayatkafeef.game.audio.SpatialAudio;
import com.hayatkafeef.game.audio.TtsManager;
import com.hayatkafeef.game.game.DialogueSystem;
import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameEngine;
import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Prefs;
import com.hayatkafeef.game.game.Scene;
import com.hayatkafeef.game.game.Scenes;
import com.hayatkafeef.game.haptics.HapticManager;
import com.hayatkafeef.game.input.GestureController;
import com.hayatkafeef.game.input.ShakeDetector;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.render.GameView;
import com.hayatkafeef.game.render.VisionMode;
import com.hayatkafeef.game.world.EventLog;

public class GameActivity extends Activity implements GameEngine.View {

    public static final String EXTRA_NEW_GAME = "new_game";

    private Prefs prefs;
    private TtsManager tts;
    private SpatialAudio audio;
    private HapticManager haptics;
    private GameEngine engine;
    private GameView gameView;
    private GestureController gestures;
    private ShakeDetector shake;

    private TextView hudLocation, hudTime, hudStats, hudMission;
    private View overlay;
    private TextView overlayTitle, overlayBody;
    private LinearLayout overlayButtons;
    private boolean paused;
    private boolean inDialog;

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_game);

        prefs = new Prefs(this);
        tts = new TtsManager(this);
        tts.setRate(prefs.ttsRate() / 100f);
        audio = new SpatialAudio();
        audio.setEnabled(prefs.spatialAudio());
        haptics = new HapticManager(this);
        haptics.setEnabled(prefs.vibrationEnabled());

        gameView = findViewById(R.id.game_view);
        hudLocation = findViewById(R.id.hud_location);
        hudTime = findViewById(R.id.hud_time);
        hudStats = findViewById(R.id.hud_stats);
        hudMission = findViewById(R.id.hud_mission);
        overlay = findViewById(R.id.overlay);
        overlayTitle = findViewById(R.id.overlay_title);
        overlayBody = findViewById(R.id.overlay_body);
        overlayButtons = findViewById(R.id.overlay_buttons);

        engine = new GameEngine(this, gameView, tts, audio, haptics, this, prefs);
        gameView.setVisionMode(prefs.visionMode());

        boolean isNew = getIntent().getBooleanExtra(EXTRA_NEW_GAME, true);
        GameState gs;
        if (!isNew && prefs.hasSave()) {
            gs = GameState.fromBlob(prefs.saveBlob());
            gs.scene = Scenes.create(gs.currentSceneId);
        } else {
            gs = new GameState();
            gs.currentSceneId = Scene.Id.HOME;
            gs.scene = Scenes.create(gs.currentSceneId);
            gs.player.x = 4;
            gs.player.y = 5;
            gs.player.heading = 0;
        }
        engine.setState(gs);
        engine.setVisionMode(prefs.visionMode());

        gestures = new GestureController(this, new GestureController.Listener() {
            @Override public void onWalk() { engine.cmdWalk(); }
            @Override public void onStop() { engine.cmdStop(); }
            @Override public void onTurnLeft() { engine.cmdTurnLeft(); }
            @Override public void onTurnRight() { engine.cmdTurnRight(); }
            @Override public void onInteract() { engine.cmdInteract(); }
            @Override public void onDescribe() { engine.cmdDescribe(); }
        });
        gameView.setInputBridge(this::onGameTouch);

        shake = new ShakeDetector(this, () -> engine.cmdShakeReorient());

        // action bar
        findViewById(R.id.a_left).setOnClickListener(v -> engine.cmdTurnLeft());
        findViewById(R.id.a_right).setOnClickListener(v -> engine.cmdTurnRight());
        findViewById(R.id.a_walk).setOnClickListener(v -> {
            engine.cmdWalk();
            main.postDelayed(engine::cmdStop, 900);
        });
        findViewById(R.id.a_describe).setOnClickListener(v -> engine.cmdDescribe());
        findViewById(R.id.a_interact).setOnClickListener(v -> engine.cmdInteract());
        findViewById(R.id.a_nav).setOnClickListener(v -> openNavMenu());
        findViewById(R.id.a_hint).setOnClickListener(v -> engine.cmdHint());
        findViewById(R.id.a_repeat).setOnClickListener(v -> engine.cmdRepeat());
        findViewById(R.id.a_pause).setOnClickListener(v -> togglePause());

        // welcome
        main.postDelayed(() -> {
            tts.speak(getString(R.string.onb_1));
            main.postDelayed(() -> {
                tts.speak(EnvDescriber.describeOnArrival(engine.state().scene));
                // announce first mission
                Mission first = engine.missions() != null ? engine.missions().current() : null;
                if (first != null) {
                    main.postDelayed(() -> {
                        String t = "مهمة جديدة: " + first.title + ". " + first.description;
                        tts.speak(t);
                    }, 2200);
                }
            }, 1800);
        }, 600);

        engine.start();
    }

    private boolean onGameTouch(MotionEvent ev) {
        if (inDialog || paused) return false;
        gestures.handle(ev);
        return true;
    }

    @Override protected void onResume() {
        super.onResume();
        if (shake != null) shake.start();
        // settings may have changed; refresh AI client + tts rate
        if (engine != null) engine.rebuildAi();
        if (tts != null) tts.setRate(prefs.ttsRate() / 100f);
    }
    @Override protected void onPause() {
        if (shake != null) shake.stop();
        save();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (engine != null) engine.stop();
        if (audio != null) audio.stopAll();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    private void save() {
        if (engine == null || engine.state() == null) return;
        prefs.writeSave(engine.state().toBlob());
    }

    private void openNavMenu() {
        if (engine == null || engine.state() == null || engine.state().scene == null) return;
        if (engine.nav().isActive()) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage(R.string.nav_already_active)
                    .setPositiveButton(R.string.msg_yes, (d, w) -> engine.cancelNavigation())
                    .setNegativeButton(R.string.msg_no, null)
                    .show();
            tts.speakNow(getString(R.string.nav_already_active));
            return;
        }
        final java.util.List<Entity> targets = new java.util.ArrayList<>();
        for (Entity e : engine.state().scene.entities) {
            if (e.name == null || e.name.isEmpty()) continue;
            targets.add(e);
        }
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.nav_no_targets, Toast.LENGTH_LONG).show();
            tts.speakNow(getString(R.string.nav_no_targets));
            return;
        }
        final float px = engine.state().player.x;
        final float py = engine.state().player.y;
        java.util.Collections.sort(targets, (a, b) -> {
            float da = (a.x - px) * (a.x - px) + (a.y - py) * (a.y - py);
            float db = (b.x - px) * (b.x - px) + (b.y - py) * (b.y - py);
            return Float.compare(da, db);
        });
        String[] labels = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            Entity e = targets.get(i);
            float d = (float) Math.hypot(e.x - px, e.y - py);
            int steps = Math.max(1, Math.round(d));
            String suffix = e.tag != null && !e.tag.isEmpty() ? " (" + e.tag + ")" : "";
            labels[i] = e.name + suffix + " — " + steps + " خطوة تقريبًا";
        }
        AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.nav_title)
                .setItems(labels, (d, which) -> engine.startNavigation(targets.get(which)))
                .setNegativeButton(R.string.btn_close, null)
                .create();
        dlg.show();
        tts.speakNow(getString(R.string.nav_title));
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            engine.stop();
            audio.stopAll();
            showOverlay(getString(R.string.hud_paused), "",
                    new String[]{
                            getString(R.string.btn_resume),
                            getString(R.string.menu_mission_status),
                            getString(R.string.menu_event_log),
                            getString(R.string.menu_day_summary),
                            getString(R.string.menu_manual),
                            getString(R.string.btn_close)
                    },
                    new Runnable[]{
                            () -> { paused = false; engine.start(); hideOverlay(); },
                            () -> { hideOverlay(); paused = false; engine.start(); engine.cmdMissionStatus(); },
                            this::showEventLog,
                            this::showDaySummary,
                            () -> startActivity(new Intent(this, ManualActivity.class)),
                            () -> { save(); finish(); }
                    });
            tts.speakNow(getString(R.string.hud_paused));
        } else {
            hideOverlay();
            engine.start();
        }
    }

    private void showEventLog() {
        EventLog log = engine.log();
        String body = log.renderRecent(15);
        if (body == null || body.isEmpty()) body = getString(R.string.msg_log_empty);
        showOverlay(getString(R.string.menu_event_log), body,
                new String[]{ getString(R.string.btn_close) },
                new Runnable[]{ () -> { hideOverlay(); paused = false; engine.start(); } });
        tts.speakNow(getString(R.string.menu_event_log) + ". " + body);
    }

    private void showDaySummary() {
        boolean lastDay = engine.state() != null && engine.state().day >= 7
                && engine.missions() != null && engine.missions().allCompleted();
        String[] btns;
        Runnable[] acts;
        boolean dayOver = engine.missions() != null && engine.missions().allCompleted();
        if (dayOver && !lastDay) {
            btns = new String[]{
                    getString(R.string.btn_next_day),
                    getString(R.string.btn_close)
            };
            acts = new Runnable[]{
                    () -> { hideOverlay(); paused = false; engine.advanceToNextDay(); engine.start(); },
                    () -> { hideOverlay(); paused = false; engine.start(); }
            };
        } else if (lastDay) {
            btns = new String[]{
                    getString(R.string.btn_free_play),
                    getString(R.string.btn_close)
            };
            acts = new Runnable[]{
                    () -> { hideOverlay(); paused = false; engine.start(); },
                    () -> { hideOverlay(); paused = false; engine.start(); }
            };
        } else {
            btns = new String[]{ getString(R.string.btn_close) };
            acts = new Runnable[]{ () -> { hideOverlay(); paused = false; engine.start(); } };
        }
        showOverlay(getString(R.string.menu_day_summary), getString(R.string.msg_summary_loading), btns, acts);
        tts.speakNow(getString(R.string.msg_summary_loading));
        engine.cmdDaySummary((text, fromAi) -> overlayBody.setText(text));
    }

    // ----- GameEngine.View -----

    @Override
    public void onState(GameState gs) {
        if (gs == null) return;
        hudLocation.setText(getString(R.string.hud_location, gs.scene != null ? gs.scene.name : "—"));
        hudTime.setText(getString(R.string.hud_time, gs.formatTime() + " · يوم " + gs.day));
        hudStats.setText(
                getString(R.string.hud_stat_mobility) + " " + gs.player.mobility + "  ·  " +
                getString(R.string.hud_stat_social) + " " + gs.player.social + "  ·  " +
                getString(R.string.hud_stat_tech) + " " + gs.player.tech + "  ·  " +
                getString(R.string.hud_stat_confidence) + " " + gs.player.confidence
        );
        Mission cur = engine.missions() != null ? engine.missions().current() : null;
        if (cur != null) {
            hudMission.setText("◉ " + cur.title);
            hudMission.setVisibility(View.VISIBLE);
        } else {
            hudMission.setText("✓ اكتملت مهام اليوم");
        }
    }

    @Override
    public void onMessage(String msg) {
        // Toast acts as a TalkBack live region; main delivery is TTS.
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void requestDialog(Entity e) {
        DialogueSystem.Convo convo = DialogueSystem.forEntity(e, engine.state());
        if (convo == null) return;
        inDialog = true;
        String[] labels = new String[convo.choices.size() + 1];
        Runnable[] actions = new Runnable[convo.choices.size() + 1];
        for (int i = 0; i < convo.choices.size(); i++) {
            final DialogueSystem.Choice c = convo.choices.get(i);
            labels[i] = c.text;
            actions[i] = () -> {
                engine.applyChoiceEffects(e, c);
                boolean aiOn = prefs.aiEnabled() && prefs.aiMode() >= 1;
                final AiDialogueManager dm = engine.dialogue();
                if (dm != null) {
                    dm.enrichReply(e, c, engine.state(), c.reply, aiOn, prefs.aiMode(),
                            (text, fromAi) -> {
                                showOverlay(convo.npc, text,
                                        new String[]{ getString(R.string.btn_continue) },
                                        new Runnable[]{ () -> { hideOverlay(); inDialog = false; } });
                                tts.speak(text);
                            });
                } else {
                    showOverlay(convo.npc, c.reply,
                            new String[]{ getString(R.string.btn_continue) },
                            new Runnable[]{ () -> { hideOverlay(); inDialog = false; } });
                    tts.speak(c.reply);
                }
            };
        }
        labels[convo.choices.size()] = getString(R.string.btn_close);
        actions[convo.choices.size()] = () -> { hideOverlay(); inDialog = false; };
        showOverlay(convo.npc, convo.opener, labels, actions);
        tts.speak(convo.npc + ". " + convo.opener);
    }

    @Override
    public void onSceneTransition(Scene to) {
        if (to != null) Toast.makeText(this, to.name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMissionChanged(Mission previous, Mission next) {
        // hud refreshes on next onState; nothing to do here.
    }

    @Override
    public void onDayCompleted(int day) {
        // Trigger the day summary overlay automatically.
        main.postDelayed(() -> {
            if (!isFinishing()) {
                paused = true;
                engine.stop();
                showDaySummary();
            }
        }, 1500);
    }

    // ----- Overlay helper -----

    private void showOverlay(String title, String body, String[] buttons, Runnable[] onClick) {
        overlay.setVisibility(View.VISIBLE);
        overlayTitle.setText(title == null ? "" : title);
        overlayBody.setText(body == null ? "" : body);
        overlayButtons.removeAllViews();
        for (int i = 0; i < buttons.length; i++) {
            Button b = new Button(this);
            b.setText(buttons[i]);
            b.setTextSize(17);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            b.setLayoutParams(lp);
            b.setBackgroundResource(i == 0 ? R.drawable.btn_primary : R.drawable.btn_ghost);
            b.setTextColor(getColor(i == 0 ? R.color.black : R.color.text_primary));
            final Runnable r = onClick[i];
            b.setOnClickListener(v -> { if (r != null) r.run(); });
            overlayButtons.addView(b);
        }
        overlay.setContentDescription(title + ". " + body);
        overlay.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private void hideOverlay() {
        overlay.setVisibility(View.GONE);
    }
}
