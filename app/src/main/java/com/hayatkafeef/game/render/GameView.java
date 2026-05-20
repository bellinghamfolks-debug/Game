package com.hayatkafeef.game.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Top-down view of the current scene. The world is procedurally drawn
 * each frame with:
 *
 *   - per-scene sky and ground textures (cached)
 *   - tree sway, NPC breathing, water pulse animations
 *   - falling leaves, fountain spray, bus exhaust, library dust motes
 *   - player walk-bob when moving
 *   - camera shake on hazards
 *   - time-of-day tint driven by the in-game clock
 *   - vision-mode post-process (blur veil, tunnel, peripheral, etc.)
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

    private Bitmap groundTexture;
    private Scene.Id groundFor;

    // animation
    private final long startTimeMs = System.currentTimeMillis();
    private long lastFrameMs = startTimeMs;
    private float lastPlayerX, lastPlayerY;
    private boolean playerMoving;
    private long lastMoveAtMs;
    private float playerBobPhase;

    // shake
    private float shakeAmount;
    private long shakeStartMs;
    private long shakeEndMs;

    // particles (world-space)
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    public GameView(Context c) { super(c); init(); }
    public GameView(Context c, AttributeSet a) { super(c, a); init(); }
    public GameView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        paint.setFilterBitmap(true);
        texPaint.setFilterBitmap(true);
    }

    public void setState(GameState s) {
        this.state = s;
        if (s != null) {
            this.lastPlayerX = s.player.x;
            this.lastPlayerY = s.player.y;
        }
    }
    public void setVisionMode(VisionMode m) { this.visionMode = m; }
    public void setInputBridge(InputBridge b) { this.inputBridge = b; }

    /** Trigger a camera shake. Intensity 0..1, durationMs how long it decays. */
    public void shake(float intensity, long durationMs) {
        long now = System.currentTimeMillis();
        this.shakeAmount = Math.max(this.shakeAmount, Math.max(0f, Math.min(1f, intensity)));
        this.shakeStartMs = now;
        this.shakeEndMs = Math.max(this.shakeEndMs, now + durationMs);
    }

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
            // 50 fps target — smooth motion without burning battery.
            try { Thread.sleep(20); } catch (InterruptedException ignored) {}
        }
    }

    private float t() { return (System.currentTimeMillis() - startTimeMs) / 1000f; }

    private void renderFrame(Canvas c) {
        int w = c.getWidth();
        int h = c.getHeight();
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;

        // ---- detect movement & spawn particles ----
        if (state != null) {
            float dx = state.player.x - lastPlayerX;
            float dy = state.player.y - lastPlayerY;
            float speed = (float) Math.hypot(dx, dy) / Math.max(0.001f, dt);
            if (speed > 0.4f) {
                playerMoving = true;
                lastMoveAtMs = now;
            } else if (now - lastMoveAtMs > 250) {
                playerMoving = false;
            }
            playerBobPhase += dt * 12f * (playerMoving ? 1f : 0f);
            lastPlayerX = state.player.x;
            lastPlayerY = state.player.y;
        }
        if (state != null && state.scene != null) {
            spawnSceneParticles(state.scene, dt);
            updateParticles(dt);
        }

        // ---- TOTAL blind: black with breathing dot ----
        if (visionMode == VisionMode.TOTAL) {
            c.drawColor(0xFF050608);
            float pulseT = (now % 2000) / 2000f;
            float r = 8 + 6 * (float) Math.sin(pulseT * 2 * Math.PI);
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

        // ---- camera shake offset ----
        float sx = 0f, sy = 0f;
        if (now < shakeEndMs && shakeAmount > 0f) {
            float remaining = Math.max(0f, (shakeEndMs - now) / 600f);
            float amp = shakeAmount * Math.min(1f, remaining) * 14f;
            sx = (float) (Math.sin(now * 0.06) * amp);
            sy = (float) (Math.cos(now * 0.09) * amp);
        } else {
            shakeAmount = 0f;
        }

        // ---- backgrounds ----
        drawSky(c, w, h, scene.id);
        drawClouds(c, w, h, scene.id);

        // ---- world transform ----
        float tile = Math.max(28f, Math.min(w, h) * 0.12f);
        float cx = w / 2f, cy = h * 0.58f;

        // walking camera bob
        float camBobY = playerMoving ? (float) Math.sin(playerBobPhase) * 3f : 0f;

        c.save();
        c.translate(cx + sx, cy + sy + camBobY);
        c.rotate((float) Math.toDegrees(-Math.PI / 2 - state.player.heading));
        c.translate(-state.player.x * tile, -state.player.y * tile);

        drawGround(c, scene, tile);
        drawEntityShadows(c, scene, tile);
        drawEntities(c, scene, tile);
        drawPlayer(c, tile);
        drawWorldParticles(c, tile);

        c.restore();

        // ---- post-process overlays in screen space ----
        drawAmbient(c, w, h, scene.id);
        drawTimeOfDay(c, w, h, state.minutes);
        applyVisionFilter(c, w, h);
    }

    // ====================== drawing layers ======================

    private void drawSky(Canvas c, int w, int h, Scene.Id id) {
        int top, bottom;
        switch (id) {
            case STREET: top = 0xFFB7D7EF; bottom = 0xFFEFD9B7; break;
            case UNIVERSITY: top = 0xFFC8D8E8; bottom = 0xFFE2D7C4; break;
            case CAFE: top = 0xFF2A1F1A; bottom = 0xFF1A1410; break;
            case LIBRARY: top = 0xFF2A2418; bottom = 0xFF1C160E; break;
            case HOME: default: top = 0xFF22262C; bottom = 0xFF0F1115; break;
        }
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                Math.max(w, h) * 0.85f,
                new int[]{0x00000000, 0x33000000, 0x88000000},
                new float[]{0.5f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    private void drawClouds(Canvas c, int w, int h, Scene.Id id) {
        if (id != Scene.Id.STREET && id != Scene.Id.UNIVERSITY) return;
        float time = t();
        for (int i = 0; i < 5; i++) {
            float speed = 12f + i * 6f;
            float x = ((time * speed + i * 220f) % (w + 400f)) - 200f;
            float y = 40f + i * 22f;
            float r = 50f + i * 14f;
            paint.setShader(new RadialGradient(x - r * 0.3f, y - r * 0.3f, r,
                    0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r, paint);
            paint.setShader(null);
        }
    }

    private void drawGround(Canvas c, Scene scene, float tile) {
        if (groundTexture == null || groundFor != scene.id ||
                groundTexture.getWidth() != Math.max(64, (int) (tile * 2))) {
            groundTexture = buildGroundTile(scene.id, Math.max(64, (int) (tile * 2)));
            groundFor = scene.id;
        }
        int tw = groundTexture.getWidth();
        int th = groundTexture.getHeight();
        for (int x = 0; x < scene.width * tile; x += tw) {
            for (int y = 0; y < scene.height * tile; y += th) {
                c.drawBitmap(groundTexture, x, y, texPaint);
            }
        }
        if (scene.id == Scene.Id.STREET) {
            float roadTop = 4.5f * tile;
            float roadBot = 9.5f * tile;
            paint.setShader(new LinearGradient(0, roadTop, 0, roadBot,
                    0xFF40454C, 0xFF222932, Shader.TileMode.CLAMP));
            c.drawRect(0, roadTop, scene.width * tile, roadBot, paint);
            paint.setShader(null);
            paint.setColor(0xFFE0D58A);
            float dashY = (roadTop + roadBot) / 2f;
            // animated dashes — appear to scroll west to east slowly
            float offset = (t() * tile * 0.6f) % (tile * 0.8f);
            for (float x = -tile * 0.8f + offset; x < scene.width * tile; x += tile * 0.8f) {
                c.drawRect(x, dashY - tile * 0.04f, x + tile * 0.4f, dashY + tile * 0.04f, paint);
            }
            paint.setColor(0xFF9C9C9C);
            c.drawRect(0, roadTop - tile * 0.08f, scene.width * tile, roadTop, paint);
            c.drawRect(0, roadBot, scene.width * tile, roadBot + tile * 0.08f, paint);
        } else if (scene.id == Scene.Id.UNIVERSITY) {
            paint.setColor(0x22000000);
            paint.setStrokeWidth(1.5f);
            for (int i = 0; i <= scene.width; i += 2) c.drawLine(i * tile, 0, i * tile, scene.height * tile, paint);
            for (int j = 0; j <= scene.height; j += 2) c.drawLine(0, j * tile, scene.width * tile, j * tile, paint);
        } else if (scene.id == Scene.Id.CAFE || scene.id == Scene.Id.LIBRARY) {
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
            case STREET:     baseTop = 0xFF6E8F4C; baseBot = 0xFF3F5A28; break;
            case UNIVERSITY: baseTop = 0xFFC7BFA8; baseBot = 0xFF8C846E; break;
            case CAFE:       baseTop = 0xFF5C3A1A; baseBot = 0xFF301B0A; break;
            case LIBRARY:    baseTop = 0xFF4A3622; baseBot = 0xFF261A0E; break;
            case HOME: default: baseTop = 0xFF7A5E40; baseBot = 0xFF3D2B16; break;
        }
        p.setShader(new LinearGradient(0, 0, 0, size, baseTop, baseBot, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, size, size, p);
        p.setShader(null);
        Random localRng = new Random(id.ordinal() * 1337L);
        if (id == Scene.Id.UNIVERSITY) {
            p.setStrokeWidth(1.2f);
            for (int i = 0; i < 8; i++) {
                p.setColor((localRng.nextInt(30) << 24) | 0x00FFFFFF);
                float x0 = localRng.nextFloat() * size, y0 = localRng.nextFloat() * size;
                float x1 = x0 + localRng.nextFloat() * size * 0.4f - size * 0.2f;
                float y1 = y0 + localRng.nextFloat() * size * 0.4f - size * 0.2f;
                g.drawLine(x0, y0, x1, y1, p);
            }
        } else if (id == Scene.Id.CAFE || id == Scene.Id.LIBRARY) {
            p.setColor(0xFF1A0E04);
            float plankH = size / 4f;
            for (int i = 1; i <= 3; i++) g.drawRect(0, i * plankH - 1, size, i * plankH + 1, p);
            for (int i = 0; i < 30; i++) {
                p.setColor((40 + localRng.nextInt(40)) << 24);
                float y = localRng.nextFloat() * size;
                g.drawLine(localRng.nextFloat() * size, y, localRng.nextFloat() * size, y, p);
            }
        } else {
            for (int i = 0; i < 120; i++) {
                int a = 40 + localRng.nextInt(40);
                int shade = localRng.nextInt(2) == 0 ? 0xFF : 0x00;
                p.setColor((a << 24) | (shade << 16) | (shade << 8) | shade);
                float x = localRng.nextFloat() * size;
                float y = localRng.nextFloat() * size;
                g.drawCircle(x, y, 0.5f + localRng.nextFloat() * 1.2f, p);
            }
        }
        return bm;
    }

    private void drawEntityShadows(Canvas c, Scene scene, float tile) {
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
    }

    private void drawEntities(Canvas c, Scene scene, float tile) {
        List<Entity> sorted = new ArrayList<>(scene.entities);
        java.util.Collections.sort(sorted, (a, b) -> Float.compare(a.y, b.y));
        int sizePx = (int) (tile * 1.8f);
        float time = t();

        for (Entity e : sorted) {
            Bitmap sprite = sprites.get(e.kind, sizePx);
            float ex = e.x * tile, ey = e.y * tile;

            switch (e.kind) {
                case TREE: {
                    // sway: rotate around the trunk base
                    float sway = (float) Math.sin(time * 0.8f + e.x * 0.7f) * 2.4f;
                    c.save();
                    c.translate(ex, ey + tile * 0.5f);
                    c.rotate(sway);
                    c.drawBitmap(sprite, -sizePx / 2f, -sizePx / 2f - tile * 0.5f, paint);
                    c.restore();
                    break;
                }
                case PERSON: {
                    // breathing: subtle vertical scale
                    float breath = 1f + (float) Math.sin(time * 1.6f + e.x * 0.9f) * 0.022f;
                    c.save();
                    c.translate(ex, ey);
                    c.scale(1f, breath, 0, sizePx * 0.2f);
                    c.drawBitmap(sprite, -sizePx / 2f, -sizePx / 2f, paint);
                    c.restore();
                    break;
                }
                case FOUNTAIN: {
                    // water pulse: gentle scale of whole sprite
                    float pulse = 1f + (float) Math.sin(time * 3.2f) * 0.018f;
                    c.save();
                    c.translate(ex, ey);
                    c.scale(pulse, pulse);
                    c.drawBitmap(sprite, -sizePx / 2f, -sizePx / 2f, paint);
                    c.restore();
                    break;
                }
                case BUS: case CAR: {
                    // subtle bob to suggest idle engine
                    float bob = (float) Math.sin(time * 6.5f + e.x) * 0.6f;
                    c.drawBitmap(sprite, ex - sizePx / 2f, ey - sizePx / 2f + bob, paint);
                    break;
                }
                default:
                    c.drawBitmap(sprite, ex - sizePx / 2f, ey - sizePx / 2f, paint);
            }
        }
    }

    private void drawPlayer(Canvas c, float tile) {
        int psize = (int) (tile * 1.7f);
        float bob = playerMoving ? (float) Math.sin(playerBobPhase) * tile * 0.04f : 0f;
        Bitmap sprite = sprites.player(psize, state.player.heading);
        c.drawBitmap(sprite,
                state.player.x * tile - psize / 2f,
                state.player.y * tile - psize / 2f - bob, paint);
    }

    private void drawWorldParticles(Canvas c, float tile) {
        for (Particle p : particles) {
            float fade = Math.max(0f, Math.min(1f, p.life / p.maxLife));
            int alpha = (int) (((p.color >>> 24) & 0xFF) * fade);
            paint.setColor((p.color & 0x00FFFFFF) | (alpha << 24));
            float sz = p.size * tile;
            if (p.kind == 0) { // leaf
                c.save();
                c.translate(p.x * tile, p.y * tile);
                c.rotate(p.rotation);
                c.drawOval(-sz, -sz * 0.4f, sz, sz * 0.4f, paint);
                c.restore();
            } else if (p.kind == 1) { // rain drop / spray
                c.drawRect(p.x * tile - sz * 0.10f, p.y * tile,
                        p.x * tile + sz * 0.10f, p.y * tile + sz * 0.6f, paint);
            } else if (p.kind == 2) { // fountain spray
                c.drawCircle(p.x * tile, p.y * tile, sz * 0.5f, paint);
            } else if (p.kind == 3) { // dust / smoke
                c.drawCircle(p.x * tile, p.y * tile, sz * 0.5f, paint);
            }
        }
    }

    // ====================== particles ======================

    private static class Particle {
        float x, y, vx, vy;
        float life, maxLife;
        int color;
        int kind; // 0=leaf, 1=rain, 2=spray, 3=dust
        float size;
        float rotation, rotSpeed;
    }

    private float leafSpawnAccum, dustSpawnAccum, sprayAccum, exhaustAccum, rainAccum;

    private void spawnSceneParticles(Scene scene, float dt) {
        boolean raining = (state != null && state.day == 6)
                && (scene.id == Scene.Id.STREET || scene.id == Scene.Id.UNIVERSITY);
        if (raining) {
            rainAccum += dt;
            if (rainAccum > 0.05f) {
                rainAccum = 0f;
                for (int i = 0; i < 5; i++) spawnRain(scene);
            }
        }
        if (!raining && (scene.id == Scene.Id.STREET || scene.id == Scene.Id.UNIVERSITY)) {
            leafSpawnAccum += dt;
            if (leafSpawnAccum > 0.5f) {
                leafSpawnAccum = 0f;
                spawnLeaf(scene);
            }
        }
        if (scene.id == Scene.Id.LIBRARY) {
            dustSpawnAccum += dt;
            if (dustSpawnAccum > 0.35f) {
                dustSpawnAccum = 0f;
                spawnDust(scene);
            }
        }
        // Continuous emitters on specific entities
        sprayAccum += dt;
        exhaustAccum += dt;
        for (Entity ent : scene.entities) {
            if (ent.kind == Entity.Kind.FOUNTAIN && sprayAccum > 0.05f) {
                spawnSpray(ent);
            }
            if (ent.kind == Entity.Kind.BUS && exhaustAccum > 0.25f) {
                spawnExhaust(ent);
            }
        }
        if (sprayAccum > 0.05f) sprayAccum = 0f;
        if (exhaustAccum > 0.25f) exhaustAccum = 0f;
    }

    private void spawnLeaf(Scene scene) {
        Particle p = new Particle();
        p.x = rng.nextFloat() * scene.width;
        p.y = -0.5f;
        p.vx = -0.4f - rng.nextFloat() * 0.5f;
        p.vy = 0.5f + rng.nextFloat() * 0.4f;
        p.maxLife = 9f + rng.nextFloat() * 4f;
        p.life = p.maxLife;
        int[] leafColors = {0xCCD0A040, 0xCCB87020, 0xCCC09030, 0xCCAA6018};
        p.color = leafColors[rng.nextInt(leafColors.length)];
        p.kind = 0;
        p.size = 0.10f + rng.nextFloat() * 0.06f;
        p.rotation = rng.nextFloat() * 360f;
        p.rotSpeed = rng.nextFloat() * 180f - 90f;
        particles.add(p);
    }

    private void spawnDust(Scene scene) {
        Particle p = new Particle();
        p.x = rng.nextFloat() * scene.width;
        p.y = rng.nextFloat() * scene.height;
        p.vx = (rng.nextFloat() - 0.5f) * 0.15f;
        p.vy = (rng.nextFloat() - 0.5f) * 0.10f;
        p.maxLife = 4f + rng.nextFloat() * 2f;
        p.life = p.maxLife;
        p.color = 0x88FFEAB0;
        p.kind = 3;
        p.size = 0.035f + rng.nextFloat() * 0.02f;
        particles.add(p);
    }

    private void spawnSpray(Entity ent) {
        Particle p = new Particle();
        p.x = ent.x + (rng.nextFloat() - 0.5f) * 0.3f;
        p.y = ent.y - 0.2f;
        p.vx = (rng.nextFloat() - 0.5f) * 1.4f;
        p.vy = -1.6f - rng.nextFloat() * 0.6f;
        p.maxLife = 1.0f;
        p.life = p.maxLife;
        p.color = 0xCCB8DEF7;
        p.kind = 2;
        p.size = 0.07f;
        particles.add(p);
    }

    private void spawnRain(Scene scene) {
        Particle p = new Particle();
        p.x = rng.nextFloat() * scene.width;
        p.y = -0.5f;
        p.vx = -0.6f;
        p.vy = 9f + rng.nextFloat() * 2f;
        p.maxLife = 2.0f;
        p.life = p.maxLife;
        p.color = 0x88B8DEF7;
        p.kind = 1;
        p.size = 0.18f;
        particles.add(p);
    }

    private void spawnExhaust(Entity ent) {
        Particle p = new Particle();
        p.x = ent.x - 0.9f;
        p.y = ent.y + 0.35f;
        p.vx = -0.4f - rng.nextFloat() * 0.4f;
        p.vy = -0.35f - rng.nextFloat() * 0.3f;
        p.maxLife = 1.8f;
        p.life = p.maxLife;
        p.color = 0x77B0B0B0;
        p.kind = 3;
        p.size = 0.28f + rng.nextFloat() * 0.12f;
        particles.add(p);
    }

    private void updateParticles(float dt) {
        // cap particle count to keep allocation cost bounded
        while (particles.size() > 220) particles.remove(0);
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt;
            if (p.life <= 0f) { it.remove(); continue; }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.kind == 2) {
                // spray gets gravity & drag
                p.vy += 4.5f * dt;
                p.vx *= (1f - 0.4f * dt);
            }
            if (p.kind == 0) {
                // leaf sway
                p.vx += (float) Math.sin(t() * 2f + p.x) * 0.45f * dt;
            }
            if (p.kind == 3) {
                p.vx *= (1f - 0.6f * dt);
                p.vy *= (1f - 0.6f * dt);
                // dust drifts up gently
                p.vy -= 0.05f * dt;
            }
            if (p.kind == 1) p.vy += 9f * dt;
            p.rotation += p.rotSpeed * dt;
        }
    }

    // ====================== screen-space overlays ======================

    private void drawAmbient(Canvas c, int w, int h, Scene.Id id) {
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
            case LIBRARY:
                paint.setColor(0x1AE0C36A);
                c.drawRect(0, 0, w, h, paint);
                break;
            default: break;
        }
    }

    private void drawTimeOfDay(Canvas c, int w, int h, int minutes) {
        // 0 = 06:00, 1080 = 24:00. Choose a tint that drifts with the clock.
        int tint;
        if (minutes < 90)        tint = 0x44FFC890; // dawn warm
        else if (minutes < 540)  tint = 0x00000000; // bright day
        else if (minutes < 720)  tint = 0x33FF9050; // golden hour
        else if (minutes < 840)  tint = 0x55C06030; // dusk
        else                     tint = 0x77202550; // night
        if ((tint >>> 24) == 0) return;
        paint.setColor(tint);
        c.drawRect(0, 0, w, h, paint);
    }

    private void applyVisionFilter(Canvas c, int w, int h) {
        switch (visionMode) {
            case SIGHTED: return;
            case LOW:
                paint.setColor(0x66000000);
                c.drawRect(0, 0, w, h, paint);
                paint.setColor(0x22808080);
                c.drawRect(0, 0, w, h, paint);
                return;
            case BLUR:
                paint.setColor(0x55AABBCC);
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.max(w, h) * 0.55f,
                        new int[]{0x00000000, 0x55000000, 0xCC000000},
                        new float[]{0.5f, 0.8f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            case CENTRAL:
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.min(w, h) * 0.20f,
                        new int[]{0x00000000, 0x00000000, 0xFF000000},
                        new float[]{0f, 0.75f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            case PERIPHERAL:
                paint.setShader(new RadialGradient(w / 2f, h * 0.55f,
                        Math.min(w, h) * 0.40f,
                        new int[]{0xFF000000, 0xCC000000, 0x00000000},
                        new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                return;
            case TOTAL: default: return;
        }
    }
}
