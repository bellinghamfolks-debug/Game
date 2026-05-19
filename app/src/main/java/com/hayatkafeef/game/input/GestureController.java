package com.hayatkafeef.game.input;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Maps swipe/tap/longpress to high-level game actions.
 */
public class GestureController extends GestureDetector.SimpleOnGestureListener {

    public interface Listener {
        void onWalk();
        void onStop();
        void onTurnLeft();
        void onTurnRight();
        void onInteract();
        void onDescribe();
    }

    private final Listener listener;
    private final GestureDetector detector;

    public GestureController(Context ctx, Listener l) {
        this.listener = l;
        this.detector = new GestureDetector(ctx, this);
        this.detector.setIsLongpressEnabled(true);
    }

    public boolean handle(MotionEvent ev) { return detector.onTouchEvent(ev); }

    @Override public boolean onDown(MotionEvent e) { return true; }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
        float dx = e2.getX() - (e1 != null ? e1.getX() : e2.getX());
        float dy = e2.getY() - (e1 != null ? e1.getY() : e2.getY());
        if (Math.abs(dx) < 60 && Math.abs(dy) < 60) return false;
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) listener.onTurnRight(); else listener.onTurnLeft();
        } else {
            if (dy < 0) listener.onWalk(); else listener.onStop();
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        listener.onInteract();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        listener.onDescribe();
    }
}
