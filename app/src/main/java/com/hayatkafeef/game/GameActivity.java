package com.hayatkafeef.game;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.hayatkafeef.game.ai.AiClient;
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
import com.hayatkafeef.game.render.GameView;
import com.hayatkafeef.game.render.VisionMode;

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
    private AiClient ai;

    private TextView hudLocation, hudTime, hudStats;
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
        ai = AiClient.fromPrefs(prefs.geminiKey(), prefs.geminiModel(), prefs.proxyUrl());

        gameView = findViewById(R.id.game_view);
        hudLocation = findViewById(R.id.hud_location);
        hudTime = findViewById(R.id.hud_time);
        hudStats = findViewById(R.id.hud_stats);
        overlay = findViewById(R.id.overlay);
        overlayTitle = findViewById(R.id.overlay_title);
        overlayBody = findViewById(R.id.overlay_body);
        overlayButtons = findViewById(R.id.overlay_buttons);

        engine = new GameEngine(this, gameView, tts, audio, haptics, this);
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

        // Input wiring
        gestures = new GestureController(this, new GestureController.Listener() {
            @Override public void onWalk() { engine.cmdWalk(); }
            @Override public void onStop() { engine.cmdStop(); }
            @Override public void onTurnLeft() { engine.cmdTurnLeft(); }
            @Override public void onTurnRight() { engine.cmdTurnRight(); }
            @Override public void onInteract() { engine.cmdInteract(); }
            @Override public void onDescribe() { engine.cmdDescribe(); }
        });
        gameView.setInputBridge(this::onGameTouch);

        shake = new ShakeDetector(this, () -> {
            engine.cmdShakeReorient();
        });

        // Action bar buttons (a11y-friendly equivalents of swipes)
        findViewById(R.id.a_left).setOnClickListener(v -> engine.cmdTurnLeft());
        findViewById(R.id.a_right).setOnClickListener(v -> engine.cmdTurnRight());
        findViewById(R.id.a_walk).setOnClickListener(v -> {
            // toggle walk on press
            engine.cmdWalk();
            main.postDelayed(engine::cmdStop, 900);
        });
        findViewById(R.id.a_describe).setOnClickListener(v -> engine.cmdDescribe());
        findViewById(R.id.a_interact).setOnClickListener(v -> engine.cmdInteract());
        findViewById(R.id.a_nav).setOnClickListener(v -> openNavMenu());
        findViewById(R.id.a_pause).setOnClickListener(v -> togglePause());

        // welcome
        main.postDelayed(() -> {
            tts.speak(getString(R.string.onb_1));
            main.postDelayed(() -> tts.speak(EnvDescriber.describeOnArrival(gameView != null ? engine.state().scene : null)), 2000);
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

        // already guiding? offer to cancel.
        if (engine.nav().isActive()) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage(R.string.nav_already_active)
                    .setPositiveButton(R.string.msg_yes, (d, w) -> engine.cancelNavigation())
                    .setNegativeButton(R.string.msg_no, null)
                    .show();
            tts.speakNow(getString(R.string.nav_already_active));
            return;
        }

        // Build the list of meaningful, named entities in the current scene.
        final java.util.List<com.hayatkafeef.game.game.Entity> targets = new java.util.ArrayList<>();
        for (com.hayatkafeef.game.game.Entity e : engine.state().scene.entities) {
            if (e.name == null || e.name.isEmpty()) continue;
            targets.add(e);
        }
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.nav_no_targets, Toast.LENGTH_LONG).show();
            tts.speakNow(getString(R.string.nav_no_targets));
            return;
        }
        // Sort by distance so the most relevant items are at the top.
        final float px = engine.state().player.x;
        final float py = engine.state().player.y;
        java.util.Collections.sort(targets, (a, b) -> {
            float da = (a.x - px) * (a.x - px) + (a.y - py) * (a.y - py);
            float db = (b.x - px) * (b.x - px) + (b.y - py) * (b.y - py);
            return Float.compare(da, db);
        });

        String[] labels = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            com.hayatkafeef.game.game.Entity e = targets.get(i);
            float d = (float) Math.hypot(e.x - px, e.y - py);
            int steps = Math.max(1, Math.round(d));
            String suffix = e.tag != null && !e.tag.isEmpty() ? " (" + e.tag + ")" : "";
            labels[i] = e.name + suffix + " — " + steps + " خطوة تقريبًا";
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        b.setTitle(R.string.nav_title);
        b.setItems(labels, (d, which) -> engine.startNavigation(targets.get(which)));
        b.setNegativeButton(R.string.btn_close, null);
        AlertDialog dlg = b.create();
        dlg.show();

        // Announce the title so screen readers and audio-only players hear it.
        tts.speakNow(getString(R.string.nav_title));
    }

    private void togglePause() {
        paused = !paused;
        if (paused) {
            engine.stop();
            audio.stopAll();
            showOverlay(getString(R.string.hud_paused), getString(R.string.btn_resume),
                    new String[]{ getString(R.string.btn_resume), getString(R.string.btn_close), getString(R.string.menu_manual) },
                    new Runnable[]{
                            () -> { paused = false; engine.start(); hideOverlay(); },
                            () -> { save(); finish(); },
                            () -> {
                                startActivity(new android.content.Intent(this, ManualActivity.class));
                            }
                    });
            tts.speakNow(getString(R.string.hud_paused));
        } else {
            hideOverlay();
            engine.start();
        }
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
    }

    @Override
    public void onMessage(String msg) {
        // Optional: flash a transient banner. We rely on TTS for accessibility.
        // Toast is short and announced by TalkBack as live region.
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void requestDialog(Entity e) {
        DialogueSystem.Convo convo = DialogueSystem.forEntity(e, engine.state());
        if (convo == null) return;
        inDialog = true;
        // build buttons for each choice
        String[] labels = new String[convo.choices.size() + 1];
        Runnable[] actions = new Runnable[convo.choices.size() + 1];
        for (int i = 0; i < convo.choices.size(); i++) {
            final DialogueSystem.Choice c = convo.choices.get(i);
            labels[i] = c.text;
            actions[i] = () -> {
                engine.applyChoiceEffects(c);
                String reply = c.reply;
                // optional online enrichment
                if (ai != null && ai.isConfigured()) {
                    ai.ask(
                            "أنت كاتب حوارات للعبة عن شخصية كفيفة في الحياة اليومية. " +
                            "تابع الحوار بطريقة قصيرة وودودة بالعربية. لا تتجاوز جملتين.",
                            "اللاعب اختار: " + c.text + ".\nرد الشخصية الأصلي: " + reply + ".\nأكمل أو حسّن الرد دون تغيير المعنى.",
                            new AiClient.Callback() {
                                @Override public void onReply(String text) {
                                    showOverlay(convo.npc, text != null && !text.isEmpty() ? text : reply,
                                            new String[]{getString(R.string.btn_continue)},
                                            new Runnable[]{() -> { hideOverlay(); inDialog = false; }});
                                    tts.speak(text != null ? text : reply);
                                }
                                @Override public void onError(String msg) {
                                    showOverlay(convo.npc, reply,
                                            new String[]{getString(R.string.btn_continue)},
                                            new Runnable[]{() -> { hideOverlay(); inDialog = false; }});
                                    tts.speak(reply);
                                }
                            });
                } else {
                    showOverlay(convo.npc, reply,
                            new String[]{getString(R.string.btn_continue)},
                            new Runnable[]{() -> { hideOverlay(); inDialog = false; }});
                    tts.speak(reply);
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
        // Just announce. Engine already moved the player.
        if (to != null) Toast.makeText(this, to.name, Toast.LENGTH_SHORT).show();
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
        // Announce for screen readers
        overlay.setContentDescription(title + ". " + body);
        overlay.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private void hideOverlay() {
        overlay.setVisibility(View.GONE);
    }
}
