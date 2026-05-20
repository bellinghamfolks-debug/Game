package com.hayatkafeef.game.game;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import com.hayatkafeef.game.R;
import com.hayatkafeef.game.input.GestureController;
import com.hayatkafeef.game.input.ShakeDetector;
import com.hayatkafeef.game.render.GameView;

/**
 * Owns all input wiring: gesture detector, shake detector, and the
 * bottom action-bar buttons. Translates everything into engine
 * commands. Activity simply constructs one and forwards lifecycle.
 */
public class InputController {

    public interface Host {
        void openNavMenu();
        void onPauseToggle();
        void onHint();
        /** Return false while a modal dialog or pause is up to gate world input. */
        boolean inputEnabled();
    }

    private final android.app.Activity activity;
    private final GameEngine engine;
    private final GameView gameView;
    private final GestureController gestures;
    private final ShakeDetector shake;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Host host;

    public InputController(android.app.Activity activity, GameEngine engine, GameView gameView, Host host) {
        this.activity = activity;
        this.engine = engine;
        this.gameView = gameView;
        this.host = host;

        this.gestures = new GestureController(activity, new GestureController.Listener() {
            @Override public void onWalk() { engine.cmdWalk(); }
            @Override public void onStop() { engine.cmdStop(); }
            @Override public void onTurnLeft() { engine.cmdTurnLeft(); }
            @Override public void onTurnRight() { engine.cmdTurnRight(); }
            @Override public void onInteract() { engine.cmdInteract(); }
            @Override public void onDescribe() { engine.cmdDescribe(); }
        });
        gameView.setInputBridge(this::onGameTouch);

        this.shake = new ShakeDetector(activity, engine::cmdShakeReorient);

        bindButtons(activity);
    }

    private void bindButtons(android.app.Activity a) {
        a.findViewById(R.id.a_left).setOnClickListener(v -> engine.cmdTurnLeft());
        a.findViewById(R.id.a_right).setOnClickListener(v -> engine.cmdTurnRight());
        a.findViewById(R.id.a_walk).setOnClickListener(v -> {
            engine.cmdWalk();
            main.postDelayed(engine::cmdStop, 900);
        });
        a.findViewById(R.id.a_describe).setOnClickListener(v -> engine.cmdDescribe());
        a.findViewById(R.id.a_interact).setOnClickListener(v -> engine.cmdInteract());
        a.findViewById(R.id.a_nav).setOnClickListener(v -> host.openNavMenu());
        a.findViewById(R.id.a_hint).setOnClickListener(v -> host.onHint());
        a.findViewById(R.id.a_repeat).setOnClickListener(v -> engine.cmdRepeat());
        a.findViewById(R.id.a_pause).setOnClickListener(v -> host.onPauseToggle());

        // Long-press shortcuts add extra functionality without adding more buttons.
        a.findViewById(R.id.a_describe).setOnLongClickListener(v -> { engine.cmdMissionStatus(); return true; });
        a.findViewById(R.id.a_interact).setOnLongClickListener(v -> { engine.cmdRepeat(); return true; });
        a.findViewById(R.id.a_hint).setOnLongClickListener(v -> { engine.cmdMissionStatus(); return true; });
    }

    private boolean onGameTouch(MotionEvent ev) {
        if (host != null && !host.inputEnabled()) return false;
        return gestures.handle(ev);
    }

    public void resume() { if (shake != null) shake.start(); }
    public void pause() { if (shake != null) shake.stop(); }
}
