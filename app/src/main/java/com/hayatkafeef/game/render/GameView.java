package com.hayatkafeef.game.render;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;

/**
 * Top-down view of the current scene, applying the configured VisionMode
 * as a post-render filter.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface InputBridge { boolean onTouch(MotionEvent ev); }

    private GameState state;
    private VisionMode visionMode = VisionMode.SIGHTED;
    private final SpriteCache sprites = new SpriteCache();
    private Thread renderThread;
    private volatile boolean running;
    private InputBridge inputBridge;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context c) { super(c); init(); }
    public GameView(Context c, AttributeSet a) { super(c, a); init(); }
    public GameView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void setState(GameState s) { this.state = s; }
    public void setVisionMode(VisionMode m) { this.visionMode = m; }
    public void setInputBridge(InputBridge b) { this.inputBridge = b; }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (inputBridge != null) inputBridge.onTouch(event);
        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder h) {
        running = true;
        renderThread = new Thread(this, "HayatRender");
        renderThread.start();
    }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    @Override public void surfaceDestroyed(SurfaceHolder h) {
        running = false;
        try { if (renderThread != null) renderThread.join(500); } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        while (running) {
            SurfaceHolder h = getHolder();
            Canvas canvas = null;
            try {
                canvas = h.lockCanvas();
                if (canvas != null) renderFrame(canvas);
            } catch (Exception ignored) {
            } finally {
                if (canvas != null) {
                    try { h.unlockCanvasAndPost(canvas); } catch (Exception ignored) {}
                }
            }
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
    }

    private void renderFrame(Canvas c) {
        int w = c.getWidth();
        int h = c.getHeight();
        // base background
        c.drawColor(0xFF0E1116);

        if (state == null || state.scene == null) return;

        // TOTAL = black, but show a small breathing dot so the screen isn't 100% black on accident
        if (visionMode == VisionMode.TOTAL) {
            paint.setColor(0xFF0E1116);
            c.drawRect(0, 0, w, h, paint);
            paint.setColor(0xFF1A1F26);
            c.drawCircle(w / 2f, h / 2f, 12, paint);
            return;
        }

        Scene scene = state.scene;
        // Camera follows player. We render the scene with the player at the screen center,
        // and the world rotated so player faces "up" on the screen.
        float tile = Math.max(20f, Math.min(w, h) * 0.10f);
        float cx = w / 2f, cy = h * 0.55f;

        // Save and rotate so player heading points up.
        c.save();
        c.translate(cx, cy);
        c.rotate((float) Math.toDegrees(-Math.PI / 2 - state.player.heading));
        c.translate(-state.player.x * tile, -state.player.y * tile);

        // ground
        paint.setColor(scene.paletteGround | 0xFF000000);
        c.drawRect(0, 0, scene.width * tile, scene.height * tile, paint);

        // grid lines for orientation hint (only visible in sighted/low modes via filter later)
        paint.setColor(0x22000000);
        paint.setStrokeWidth(1.5f);
        for (int i = 0; i <= scene.width; i++) {
            c.drawLine(i * tile, 0, i * tile, scene.height * tile, paint);
        }
        for (int j = 0; j <= scene.height; j++) {
            c.drawLine(0, j * tile, scene.width * tile, j * tile, paint);
        }

        // entities
        int sizePx = (int) (tile * 1.6f);
        for (Entity e : scene.entities) {
            float ex = e.x * tile - sizePx / 2f;
            float ey = e.y * tile - sizePx / 2f;
            c.drawBitmap(sprites.get(e.kind, sizePx), ex, ey, paint);
        }

        // player on top
        int psize = (int) (tile * 1.5f);
        c.drawBitmap(sprites.player(psize, state.player.heading),
                state.player.x * tile - psize / 2f,
                state.player.y * tile - psize / 2f, paint);

        c.restore();

        // post-process by vision mode
        applyVisionFilter(c, w, h);
    }

    private void applyVisionFilter(Canvas c, int w, int h) {
        switch (visionMode) {
            case SIGHTED: return;
            case LOW: {
                // darken + reduce contrast by an overlay
                paint.setColor(0x66000000);
                c.drawRect(0, 0, w, h, paint);
                return;
            }
            case BLUR: {
                // We can't easily blur an already-drawn canvas without a Bitmap,
                // so we simulate haziness with a translucent veil + soft vignette.
                paint.setColor(0x80AABBCC);
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.max(w, h) * 0.55f,
                        new int[]{0x00000000, 0x55000000, 0xCC000000},
                        new float[]{0.5f, 0.8f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            }
            case CENTRAL: {
                // tunnel: only small circle visible
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.min(w, h) * 0.18f,
                        new int[]{0x00000000, 0x00000000, 0xFF000000},
                        new float[]{0f, 0.75f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            }
            case PERIPHERAL: {
                // center dark, edges visible
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.min(w, h) * 0.40f,
                        new int[]{0xFF000000, 0xCC000000, 0x00000000},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            }
            case TOTAL: default: return;
        }
    }
}
