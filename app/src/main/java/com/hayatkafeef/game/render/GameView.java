package com.hayatkafeef.game.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;

import java.util.Random;

/**
 * Top-down view of the current scene, applying the configured VisionMode
 * as a post-render filter. Renders a sky/ambience layer plus a textured
 * ground per scene, then layered entities with soft shadows.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface InputBridge { boolean onTouch(MotionEvent ev); }

    private GameState state;
    private VisionMode visionMode = VisionMode.SIGHTED;
    private final SpriteCache sprites = new SpriteCache();
    private Thread renderThread;
    private volatile boolean running;
    private InputBridge inputBridge;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint texPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Cached ground texture per scene id. */
    private Bitmap groundTexture;
    private Scene.Id groundFor;

    public GameView(Context c) { super(c); init(); }
    public GameView(Context c, AttributeSet a) { super(c, a); init(); }
    public GameView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        paint.setFilterBitmap(true);
        texPaint.setFilterBitmap(true);
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

        // TOTAL = mostly black, with a soft pulsing dot center so it's not 100% blank.
        if (visionMode == VisionMode.TOTAL) {
            c.drawColor(0xFF050608);
            // gentle "you are here" pulse
            float t = (System.currentTimeMillis() % 2000) / 2000f;
            float r = 8 + 6 * (float) Math.sin(t * 2 * Math.PI);
            paint.setShader(new RadialGradient(w / 2f, h / 2f, r * 2,
                    0x66E0C36A, 0x00000000, Shader.TileMode.CLAMP));
            c.drawCircle(w / 2f, h / 2f, r * 2, paint);
            paint.setShader(null);
            return;
        }

        if (state == null || state.scene == null) {
            c.drawColor(0xFF0E1116);
            return;
        }
        Scene scene = state.scene;

        // Sky / background gradient based on scene
        drawSky(c, w, h, scene.id);

        // World layer: ground + entities, rotated so the player faces "up"
        float tile = Math.max(28f, Math.min(w, h) * 0.12f);
        float cx = w / 2f, cy = h * 0.58f;

        c.save();
        c.translate(cx, cy);
        c.rotate((float) Math.toDegrees(-Math.PI / 2 - state.player.heading));
        c.translate(-state.player.x * tile, -state.player.y * tile);

        drawGround(c, scene, tile);

        // entity shadows first (so all sit on the same plane)
        for (Entity e : scene.entities) {
            float ex = e.x * tile;
            float ey = e.y * tile;
            paint.setShader(new RadialGradient(ex, ey + tile * 0.55f,
                    e.radius * tile * 1.4f,
                    new int[]{0x66000000, 0x22000000, 0x00000000},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawOval(ex - e.radius * tile * 1.2f,
                    ey + tile * 0.30f,
                    ex + e.radius * tile * 1.2f,
                    ey + tile * 0.65f, paint);
            paint.setShader(null);
        }

        // entities (sorted by y for proper layering — south behind north)
        java.util.List<Entity> sorted = new java.util.ArrayList<>(scene.entities);
        java.util.Collections.sort(sorted, (a, b) -> Float.compare(a.y, b.y));

        int sizePx = (int) (tile * 1.8f);
        for (Entity e : sorted) {
            float ex = e.x * tile - sizePx / 2f;
            float ey = e.y * tile - sizePx / 2f;
            c.drawBitmap(sprites.get(e.kind, sizePx), ex, ey, paint);
        }

        // player on top. The world is rotated by -(PI/2 + heading), so to make
        // the cane always point "up" on the screen we draw the sprite with the
        // same heading the world is using.
        int psize = (int) (tile * 1.7f);
        c.drawBitmap(sprites.player(psize, state.player.heading),
                state.player.x * tile - psize / 2f,
                state.player.y * tile - psize / 2f, paint);

        c.restore();

        // ambient overlay (warm tint during day, etc.)
        drawAmbient(c, w, h, scene.id);

        // post-process by vision mode
        applyVisionFilter(c, w, h);
    }

    private void drawSky(Canvas c, int w, int h, Scene.Id id) {
        int top, bottom;
        switch (id) {
            case STREET: top = 0xFFB7D7EF; bottom = 0xFFEFD9B7; break;        // sky to warm horizon
            case UNIVERSITY: top = 0xFFC8D8E8; bottom = 0xFFE2D7C4; break;
            case CAFE: top = 0xFF2A1F1A; bottom = 0xFF1A1410; break;           // warm dark interior
            case HOME: default: top = 0xFF22262C; bottom = 0xFF0F1115; break;
        }
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // soft vignette
        paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                Math.max(w, h) * 0.85f,
                new int[]{0x00000000, 0x33000000, 0x88000000},
                new float[]{0.5f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    private void drawGround(Canvas c, Scene scene, float tile) {
        if (groundTexture == null || groundFor != scene.id ||
                groundTexture.getWidth() != Math.max(64, (int) (tile * 2))) {
            groundTexture = buildGroundTile(scene.id, Math.max(64, (int) (tile * 2)));
            groundFor = scene.id;
        }
        // tile the ground over the scene area
        int tw = groundTexture.getWidth();
        int th = groundTexture.getHeight();
        for (int x = 0; x < scene.width * tile; x += tw) {
            for (int y = 0; y < scene.height * tile; y += th) {
                c.drawBitmap(groundTexture, x, y, texPaint);
            }
        }

        // scene-specific extras
        if (scene.id == Scene.Id.STREET) {
            // road in the middle horizontally
            float roadTop = 4.5f * tile;
            float roadBot = 9.5f * tile;
            paint.setShader(new LinearGradient(0, roadTop, 0, roadBot,
                    0xFF40454C, 0xFF222932, Shader.TileMode.CLAMP));
            c.drawRect(0, roadTop, scene.width * tile, roadBot, paint);
            paint.setShader(null);
            // dashed center line
            paint.setColor(0xFFE0D58A);
            float dashY = (roadTop + roadBot) / 2f;
            for (float x = 0; x < scene.width * tile; x += tile * 0.8f) {
                c.drawRect(x, dashY - tile * 0.04f, x + tile * 0.4f, dashY + tile * 0.04f, paint);
            }
            // curbs
            paint.setColor(0xFF9C9C9C);
            c.drawRect(0, roadTop - tile * 0.08f, scene.width * tile, roadTop, paint);
            c.drawRect(0, roadBot, scene.width * tile, roadBot + tile * 0.08f, paint);
        } else if (scene.id == Scene.Id.UNIVERSITY) {
            // marble floor grid
            paint.setColor(0x22000000);
            paint.setStrokeWidth(1.5f);
            for (int i = 0; i <= scene.width; i += 2) {
                c.drawLine(i * tile, 0, i * tile, scene.height * tile, paint);
            }
            for (int j = 0; j <= scene.height; j += 2) {
                c.drawLine(0, j * tile, scene.width * tile, j * tile, paint);
            }
        } else if (scene.id == Scene.Id.CAFE) {
            // wooden plank floor — horizontal stripes
            for (int i = 0; i < scene.height; i++) {
                float y = i * tile;
                paint.setColor(i % 2 == 0 ? 0x22000000 : 0x11FFFFFF);
                c.drawRect(0, y, scene.width * tile, y + tile, paint);
            }
        }
    }

    private Bitmap buildGroundTile(Scene.Id id, int size) {
        Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int baseTop, baseBot;
        switch (id) {
            case STREET:     baseTop = 0xFF6E8F4C; baseBot = 0xFF3F5A28; break;  // sidewalk grass tint
            case UNIVERSITY: baseTop = 0xFFC7BFA8; baseBot = 0xFF8C846E; break;  // marble
            case CAFE:       baseTop = 0xFF5C3A1A; baseBot = 0xFF301B0A; break;  // wood
            case HOME: default: baseTop = 0xFF7A5E40; baseBot = 0xFF3D2B16; break;
        }
        p.setShader(new LinearGradient(0, 0, 0, size, baseTop, baseBot, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, size, size, p);
        p.setShader(null);

        // texture
        Random rng = new Random(id.ordinal() * 1337L);
        if (id == Scene.Id.UNIVERSITY) {
            // marble veins
            p.setStrokeWidth(1.2f);
            for (int i = 0; i < 8; i++) {
                p.setColor((rng.nextInt(30) << 24) | 0x00FFFFFF);
                float x0 = rng.nextFloat() * size, y0 = rng.nextFloat() * size;
                float x1 = x0 + rng.nextFloat() * size * 0.4f - size * 0.2f;
                float y1 = y0 + rng.nextFloat() * size * 0.4f - size * 0.2f;
                g.drawLine(x0, y0, x1, y1, p);
            }
        } else if (id == Scene.Id.CAFE) {
            // plank seams
            p.setColor(0xFF1A0E04);
            float plankH = size / 4f;
            for (int i = 1; i <= 3; i++) g.drawRect(0, i * plankH - 1, size, i * plankH + 1, p);
            // wood grain
            p.setStrokeWidth(1.0f);
            for (int i = 0; i < 30; i++) {
                p.setColor((40 + rng.nextInt(40)) << 24 | 0x00000000);
                float y = rng.nextFloat() * size;
                g.drawLine(rng.nextFloat() * size, y, rng.nextFloat() * size, y, p);
            }
        } else {
            // grass / fabric speckle
            for (int i = 0; i < 120; i++) {
                int a = 40 + rng.nextInt(40);
                int shade = rng.nextInt(2) == 0 ? 0xFF : 0x00;
                p.setColor((a << 24) | (shade << 16) | (shade << 8) | shade);
                float x = rng.nextFloat() * size;
                float y = rng.nextFloat() * size;
                g.drawCircle(x, y, 0.5f + rng.nextFloat() * 1.2f, p);
            }
        }
        return bm;
    }

    private void drawAmbient(Canvas c, int w, int h, Scene.Id id) {
        // a soft top warmth for outdoor scenes, cool indoor
        switch (id) {
            case STREET:
                paint.setShader(new LinearGradient(0, 0, 0, h * 0.5f,
                        0x33FFE0A8, 0x00000000, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h * 0.5f, paint);
                paint.setShader(null);
                break;
            case CAFE:
                paint.setColor(0x22E0C36A);
                c.drawRect(0, 0, w, h, paint);
                break;
            default: break;
        }
    }

    private void applyVisionFilter(Canvas c, int w, int h) {
        switch (visionMode) {
            case SIGHTED: return;
            case LOW: {
                // darken + reduce contrast
                paint.setColor(0x66000000);
                c.drawRect(0, 0, w, h, paint);
                // slight desaturation hint via a grey overlay
                paint.setColor(0x22808080);
                c.drawRect(0, 0, w, h, paint);
                return;
            }
            case BLUR: {
                // veil + soft vignette to simulate haziness
                paint.setColor(0x55AABBCC);
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
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.min(w, h) * 0.20f,
                        new int[]{0x00000000, 0x00000000, 0xFF000000},
                        new float[]{0f, 0.75f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            }
            case PERIPHERAL: {
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
