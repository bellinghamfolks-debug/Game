package com.hayatkafeef.game.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.hayatkafeef.game.game.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * High-detail procedurally drawn sprites cached as bitmaps.
 * Every sprite is built from multiple layered passes — drop shadow,
 * base gradient, mid tones, highlights, and texture/noise — so the
 * APK stays tiny while the visuals feel illustrated.
 */
public class SpriteCache {

    private final Map<String, Bitmap> cache = new HashMap<>();

    // ---------- public API ----------

    public Bitmap get(Entity.Kind kind, int sizePx) {
        // size buckets every 16 px so we don't redraw for one-pixel changes
        int s = ((sizePx + 8) / 16) * 16;
        if (s < 32) s = 32;
        String key = kind.name() + ":" + s;
        Bitmap b = cache.get(key);
        if (b != null) return b;
        b = build(kind, s);
        cache.put(key, b);
        return b;
    }

    /** Player sprite, with the cane oriented along `heading` (radians). */
    public Bitmap player(int sizePx, float heading) {
        int s = ((sizePx + 8) / 16) * 16;
        if (s < 32) s = 32;
        // bucket heading by 12 degrees to allow ~30 cached angles
        int hb = (int) (Math.round(heading / (Math.PI / 15.0)));
        String key = "PLAYER:" + s + ":" + hb;
        Bitmap b = cache.get(key);
        if (b != null) return b;
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = newPaint();
        drawPlayer(c, p, s, heading);
        cache.put(key, bm);
        return bm;
    }

    public void clear() { cache.clear(); }

    // ---------- dispatch ----------

    private Bitmap build(Entity.Kind kind, int s) {
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = newPaint();
        switch (kind) {
            case PERSON:    drawPerson(c, p, s, kind.ordinal()); break;
            case DOOR:      drawDoor(c, p, s); break;
            case BENCH:     drawBench(c, p, s); break;
            case TREE:      drawTree(c, p, s); break;
            case WALL:      drawWall(c, p, s); break;
            case STAIRS:    drawStairs(c, p, s); break;
            case BUS:       drawBus(c, p, s); break;
            case CAR:       drawCar(c, p, s); break;
            case ELEVATOR:  drawElevator(c, p, s); break;
            case DESK:      drawDesk(c, p, s); break;
            case BED:       drawBed(c, p, s); break;
            case SHOP:      drawShop(c, p, s); break;
            case FOUNTAIN:  drawFountain(c, p, s); break;
            case PILLAR:    drawPillar(c, p, s); break;
        }
        return bm;
    }

    // ---------- helpers ----------

    private static Paint newPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        p.setFilterBitmap(true);
        return p;
    }

    private static void shadow(Canvas c, Paint p, float cx, float cy, float rx, float ry) {
        int saved = p.getColor();
        p.setShader(new RadialGradient(cx, cy, Math.max(rx, ry),
                new int[]{0x55000000, 0x22000000, 0x00000000},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p);
        p.setShader(null);
        p.setColor(saved);
    }

    /** Solid color + lighter top, darker bottom gradient between two ARGB hex. */
    private static Shader vGrad(float x, float y0, float y1, int top, int bottom) {
        return new LinearGradient(x, y0, x, y1, top, bottom, Shader.TileMode.CLAMP);
    }

    private static Shader hGrad(float x0, float x1, float y, int left, int right) {
        return new LinearGradient(x0, y, x1, y, left, right, Shader.TileMode.CLAMP);
    }

    /** Speckle texture in a region. */
    private static void speckle(Canvas c, Paint p, RectF r, int color, int alpha, int n, long seed) {
        Random rng = new Random(seed);
        p.setShader(null);
        int base = (color & 0x00FFFFFF) | (alpha << 24);
        p.setColor(base);
        for (int i = 0; i < n; i++) {
            float x = r.left + rng.nextFloat() * r.width();
            float y = r.top + rng.nextFloat() * r.height();
            float radius = 0.4f + rng.nextFloat() * 0.9f;
            c.drawCircle(x, y, radius, p);
        }
    }

    // ---------- player ----------

    private void drawPlayer(Canvas c, Paint p, int s, float heading) {
        float cx = s / 2f, cy = s * 0.58f;
        // shadow
        shadow(c, p, cx, cy + s * 0.34f, s * 0.32f, s * 0.10f);

        // legs
        p.setShader(vGrad(0, cy + s * 0.12f, cy + s * 0.34f, 0xFF2B3340, 0xFF161B22));
        c.drawRoundRect(new RectF(cx - s * 0.13f, cy + s * 0.08f, cx - s * 0.02f, cy + s * 0.34f),
                s * 0.04f, s * 0.04f, p);
        c.drawRoundRect(new RectF(cx + s * 0.02f, cy + s * 0.08f, cx + s * 0.13f, cy + s * 0.34f),
                s * 0.04f, s * 0.04f, p);

        // torso (jacket)
        p.setShader(vGrad(0, cy - s * 0.05f, cy + s * 0.18f, 0xFF3D4A5E, 0xFF1F2632));
        c.drawRoundRect(new RectF(cx - s * 0.22f, cy - s * 0.10f, cx + s * 0.22f, cy + s * 0.20f),
                s * 0.10f, s * 0.10f, p);
        // jacket zipper highlight
        p.setShader(null);
        p.setColor(0x55FFFFFF);
        p.setStrokeWidth(s * 0.012f);
        c.drawLine(cx, cy - s * 0.05f, cx, cy + s * 0.18f, p);

        // neck
        p.setShader(null);
        p.setColor(0xFFCFA988);
        c.drawRect(cx - s * 0.05f, cy - s * 0.16f, cx + s * 0.05f, cy - s * 0.08f, p);

        // head with gradient skin
        p.setShader(new RadialGradient(cx - s * 0.04f, cy - s * 0.30f, s * 0.18f,
                0xFFFFD9B0, 0xFFC79775, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy - s * 0.26f, s * 0.14f, p);

        // hair
        p.setShader(vGrad(0, cy - s * 0.42f, cy - s * 0.22f, 0xFF3A2A1C, 0xFF1A120A));
        Path hair = new Path();
        hair.moveTo(cx - s * 0.14f, cy - s * 0.22f);
        hair.cubicTo(cx - s * 0.16f, cy - s * 0.46f,
                cx + s * 0.16f, cy - s * 0.46f,
                cx + s * 0.14f, cy - s * 0.22f);
        hair.lineTo(cx + s * 0.10f, cy - s * 0.30f);
        hair.lineTo(cx - s * 0.10f, cy - s * 0.30f);
        hair.close();
        c.drawPath(hair, p);

        // dark glasses
        p.setShader(null);
        p.setColor(0xFF1A1F26);
        c.drawRoundRect(new RectF(cx - s * 0.12f, cy - s * 0.28f, cx - s * 0.01f, cy - s * 0.22f),
                s * 0.025f, s * 0.025f, p);
        c.drawRoundRect(new RectF(cx + s * 0.01f, cy - s * 0.28f, cx + s * 0.12f, cy - s * 0.22f),
                s * 0.025f, s * 0.025f, p);
        // bridge
        p.setStrokeWidth(s * 0.012f);
        c.drawLine(cx - s * 0.01f, cy - s * 0.25f, cx + s * 0.01f, cy - s * 0.25f, p);
        // lens reflections
        p.setColor(0x44FFFFFF);
        c.drawCircle(cx - s * 0.08f, cy - s * 0.26f, s * 0.012f, p);
        c.drawCircle(cx + s * 0.05f, cy - s * 0.26f, s * 0.012f, p);

        // cane: extends along heading from a "hand" at shoulder
        float handX = cx + (float) Math.cos(heading) * s * 0.18f;
        float handY = cy + (float) Math.sin(heading) * s * 0.18f + s * 0.02f;
        float tipX = cx + (float) Math.cos(heading) * s * 0.52f;
        float tipY = cy + (float) Math.sin(heading) * s * 0.52f + s * 0.12f;

        // cane shadow
        p.setColor(0x55000000);
        p.setStrokeWidth(s * 0.07f);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(handX + s * 0.02f, handY + s * 0.04f, tipX + s * 0.02f, tipY + s * 0.04f, p);

        // cane shaft (white)
        p.setColor(0xFFF3F3F3);
        p.setStrokeWidth(s * 0.05f);
        c.drawLine(handX, handY, tipX, tipY, p);

        // candy-cane stripes
        p.setColor(0xFFE07474);
        p.setStrokeWidth(s * 0.05f);
        int stripes = 4;
        for (int i = 1; i <= stripes; i++) {
            float t1 = i / (float) (stripes + 1);
            float t2 = t1 + 0.04f;
            float x1 = handX + (tipX - handX) * t1;
            float y1 = handY + (tipY - handY) * t1;
            float x2 = handX + (tipX - handX) * t2;
            float y2 = handY + (tipY - handY) * t2;
            c.drawLine(x1, y1, x2, y2, p);
        }

        // cane tip (red ball)
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setShader(new RadialGradient(tipX - s * 0.01f, tipY - s * 0.01f, s * 0.05f,
                0xFFFF9D9D, 0xFFB23B3B, Shader.TileMode.CLAMP));
        c.drawCircle(tipX, tipY, s * 0.05f, p);
        p.setShader(null);
    }

    // ---------- person (NPC) ----------

    private void drawPerson(Canvas c, Paint p, int s, int seed) {
        // pseudo-random palette per NPC (stable across redraws)
        Random rng = new Random(0x9E3779B97F4A7C15L * (seed + 1));
        int shirt = randomShirt(rng);
        int hairCol = randomHair(rng);
        int skin = randomSkin(rng);
        boolean female = rng.nextBoolean();

        float cx = s / 2f, cy = s * 0.58f;
        shadow(c, p, cx, cy + s * 0.34f, s * 0.30f, s * 0.10f);

        // legs / dress
        p.setShader(vGrad(0, cy + s * 0.12f, cy + s * 0.36f,
                darken(shirt, 0.6f), darken(shirt, 0.3f)));
        if (female) {
            Path skirt = new Path();
            skirt.moveTo(cx - s * 0.20f, cy + s * 0.10f);
            skirt.lineTo(cx + s * 0.20f, cy + s * 0.10f);
            skirt.lineTo(cx + s * 0.28f, cy + s * 0.36f);
            skirt.lineTo(cx - s * 0.28f, cy + s * 0.36f);
            skirt.close();
            c.drawPath(skirt, p);
        } else {
            c.drawRoundRect(new RectF(cx - s * 0.14f, cy + s * 0.08f, cx - s * 0.02f, cy + s * 0.34f),
                    s * 0.04f, s * 0.04f, p);
            c.drawRoundRect(new RectF(cx + s * 0.02f, cy + s * 0.08f, cx + s * 0.14f, cy + s * 0.34f),
                    s * 0.04f, s * 0.04f, p);
        }

        // torso
        p.setShader(vGrad(0, cy - s * 0.10f, cy + s * 0.18f, lighten(shirt, 0.15f), darken(shirt, 0.2f)));
        c.drawRoundRect(new RectF(cx - s * 0.22f, cy - s * 0.10f, cx + s * 0.22f, cy + s * 0.20f),
                s * 0.10f, s * 0.10f, p);

        // shirt details (collar / button line)
        p.setShader(null);
        p.setColor(darken(shirt, 0.35f));
        p.setStrokeWidth(s * 0.012f);
        c.drawLine(cx, cy - s * 0.05f, cx, cy + s * 0.15f, p);

        // arms
        p.setColor(skin);
        c.drawRoundRect(new RectF(cx - s * 0.30f, cy - s * 0.05f, cx - s * 0.22f, cy + s * 0.18f),
                s * 0.04f, s * 0.04f, p);
        c.drawRoundRect(new RectF(cx + s * 0.22f, cy - s * 0.05f, cx + s * 0.30f, cy + s * 0.18f),
                s * 0.04f, s * 0.04f, p);

        // neck
        c.drawRect(cx - s * 0.05f, cy - s * 0.16f, cx + s * 0.05f, cy - s * 0.08f, p);

        // head
        p.setShader(new RadialGradient(cx - s * 0.04f, cy - s * 0.30f, s * 0.18f,
                lighten(skin, 0.2f), darken(skin, 0.2f), Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy - s * 0.26f, s * 0.14f, p);

        // hair
        p.setShader(vGrad(0, cy - s * 0.42f, cy - s * 0.18f,
                lighten(hairCol, 0.1f), darken(hairCol, 0.3f)));
        Path hair = new Path();
        if (female) {
            hair.moveTo(cx - s * 0.18f, cy - s * 0.18f);
            hair.cubicTo(cx - s * 0.22f, cy - s * 0.50f,
                    cx + s * 0.22f, cy - s * 0.50f,
                    cx + s * 0.18f, cy - s * 0.18f);
            hair.lineTo(cx + s * 0.18f, cy - s * 0.05f);
            hair.lineTo(cx + s * 0.10f, cy - s * 0.22f);
            hair.lineTo(cx - s * 0.10f, cy - s * 0.22f);
            hair.lineTo(cx - s * 0.18f, cy - s * 0.05f);
            hair.close();
        } else {
            hair.moveTo(cx - s * 0.14f, cy - s * 0.22f);
            hair.cubicTo(cx - s * 0.16f, cy - s * 0.46f,
                    cx + s * 0.16f, cy - s * 0.46f,
                    cx + s * 0.14f, cy - s * 0.22f);
            hair.lineTo(cx + s * 0.10f, cy - s * 0.30f);
            hair.lineTo(cx - s * 0.10f, cy - s * 0.30f);
            hair.close();
        }
        c.drawPath(hair, p);

        // face
        p.setShader(null);
        p.setColor(0xFF111111);
        c.drawCircle(cx - s * 0.04f, cy - s * 0.27f, s * 0.012f, p);
        c.drawCircle(cx + s * 0.04f, cy - s * 0.27f, s * 0.012f, p);
        p.setColor(0xFFC76B6B);
        c.drawArc(cx - s * 0.03f, cy - s * 0.22f, cx + s * 0.03f, cy - s * 0.18f,
                0, 180, false, p);
    }

    // ---------- environment objects ----------

    private void drawDoor(Canvas c, Paint p, int s) {
        // frame
        p.setShader(vGrad(0, 0, s, 0xFF3A2814, 0xFF1F140A));
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.04f, s * 0.90f, s * 0.96f),
                s * 0.06f, s * 0.06f, p);

        // door panel
        p.setShader(vGrad(0, s * 0.06f, s * 0.94f, 0xFFA1683A, 0xFF6B3F1E));
        c.drawRoundRect(new RectF(s * 0.16f, s * 0.08f, s * 0.84f, s * 0.94f),
                s * 0.04f, s * 0.04f, p);

        // wood grain
        p.setShader(null);
        p.setColor(0x33000000);
        p.setStrokeWidth(s * 0.008f);
        for (int i = 0; i < 6; i++) {
            float y = s * (0.18f + i * 0.13f);
            c.drawLine(s * 0.22f, y, s * 0.78f, y, p);
        }

        // upper panel inset
        p.setColor(0x33000000);
        c.drawRoundRect(new RectF(s * 0.22f, s * 0.14f, s * 0.78f, s * 0.50f),
                s * 0.03f, s * 0.03f, p);
        p.setShader(vGrad(0, s * 0.14f, s * 0.50f, 0xFF8C552F, 0xFF6B3F1E));
        c.drawRoundRect(new RectF(s * 0.24f, s * 0.16f, s * 0.76f, s * 0.48f),
                s * 0.025f, s * 0.025f, p);

        // lower panel
        p.setShader(null);
        p.setColor(0x33000000);
        c.drawRoundRect(new RectF(s * 0.22f, s * 0.54f, s * 0.78f, s * 0.90f),
                s * 0.03f, s * 0.03f, p);
        p.setShader(vGrad(0, s * 0.54f, s * 0.90f, 0xFF8C552F, 0xFF6B3F1E));
        c.drawRoundRect(new RectF(s * 0.24f, s * 0.56f, s * 0.76f, s * 0.88f),
                s * 0.025f, s * 0.025f, p);

        // knob
        p.setShader(new RadialGradient(s * 0.74f, s * 0.55f, s * 0.06f,
                0xFFFFE08A, 0xFF8C6A1E, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.76f, s * 0.56f, s * 0.05f, p);
        p.setShader(null);
        p.setColor(0xFFFFF1B8);
        c.drawCircle(s * 0.745f, s * 0.545f, s * 0.012f, p);
    }

    private void drawBench(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.88f, s * 0.42f, s * 0.06f);

        // legs
        p.setShader(vGrad(0, s * 0.55f, s * 0.88f, 0xFF2A2018, 0xFF120B07));
        c.drawRoundRect(new RectF(s * 0.16f, s * 0.55f, s * 0.22f, s * 0.86f), s * 0.02f, s * 0.02f, p);
        c.drawRoundRect(new RectF(s * 0.78f, s * 0.55f, s * 0.84f, s * 0.86f), s * 0.02f, s * 0.02f, p);

        // seat slats
        for (int i = 0; i < 3; i++) {
            float y = s * (0.50f + i * 0.06f);
            p.setShader(vGrad(0, y, y + s * 0.05f, 0xFFB07A48, 0xFF7A4F26));
            c.drawRoundRect(new RectF(s * 0.10f, y, s * 0.90f, y + s * 0.05f),
                    s * 0.018f, s * 0.018f, p);
        }
        // backrest slats
        for (int i = 0; i < 3; i++) {
            float y = s * (0.18f + i * 0.08f);
            p.setShader(vGrad(0, y, y + s * 0.05f, 0xFFB07A48, 0xFF7A4F26));
            c.drawRoundRect(new RectF(s * 0.16f, y, s * 0.84f, y + s * 0.05f),
                    s * 0.018f, s * 0.018f, p);
        }
        // armrests
        p.setShader(null);
        p.setColor(0xFF8C5A2E);
        c.drawRoundRect(new RectF(s * 0.12f, s * 0.18f, s * 0.18f, s * 0.50f), s * 0.02f, s * 0.02f, p);
        c.drawRoundRect(new RectF(s * 0.82f, s * 0.18f, s * 0.88f, s * 0.50f), s * 0.02f, s * 0.02f, p);
    }

    private void drawTree(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.94f, s * 0.34f, s * 0.07f);

        // trunk
        p.setShader(vGrad(0, s * 0.55f, s * 0.95f, 0xFF6F4A2A, 0xFF3D2614));
        c.drawRoundRect(new RectF(s * 0.42f, s * 0.55f, s * 0.58f, s * 0.95f),
                s * 0.04f, s * 0.04f, p);
        // bark texture
        p.setShader(null);
        p.setColor(0x33000000);
        p.setStrokeWidth(s * 0.01f);
        c.drawLine(s * 0.46f, s * 0.60f, s * 0.46f, s * 0.90f, p);
        c.drawLine(s * 0.52f, s * 0.62f, s * 0.52f, s * 0.92f, p);

        // canopy: three soft blobs
        canopyBlob(c, p, s * 0.50f, s * 0.30f, s * 0.36f, 0xFF6FAF50, 0xFF275C24);
        canopyBlob(c, p, s * 0.30f, s * 0.42f, s * 0.24f, 0xFF80C760, 0xFF2F6E2D);
        canopyBlob(c, p, s * 0.72f, s * 0.42f, s * 0.24f, 0xFF80C760, 0xFF2F6E2D);

        // small highlights
        p.setShader(null);
        p.setColor(0x55FFFFFF);
        c.drawCircle(s * 0.46f, s * 0.22f, s * 0.025f, p);
        c.drawCircle(s * 0.60f, s * 0.20f, s * 0.018f, p);
    }
    private void canopyBlob(Canvas c, Paint p, float cx, float cy, float r, int light, int dark) {
        p.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r, light, dark, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
    }

    private void drawWall(Canvas c, Paint p, int s) {
        // brick base
        p.setShader(vGrad(0, 0, s, 0xFF8B5A3C, 0xFF4D2F1F));
        c.drawRect(0, 0, s, s, p);
        // bricks
        p.setShader(null);
        Random rng = new Random(7);
        for (int row = 0; row < 5; row++) {
            float y = row * s / 5f;
            float xoff = (row % 2 == 0) ? 0 : s / 8f;
            for (int col = -1; col < 5; col++) {
                float x = col * s / 4f + xoff;
                int b = 60 + rng.nextInt(40);
                p.setColor(0xFF000000 | (b << 16) | ((b - 10) << 8) | (b - 20));
                c.drawRect(x + s * 0.012f, y + s * 0.01f,
                        x + s / 4f - s * 0.012f, y + s / 5f - s * 0.01f, p);
            }
        }
        // mortar overlay
        p.setColor(0x22000000);
        c.drawRect(0, 0, s, s, p);
    }

    private void drawStairs(Canvas c, Paint p, int s) {
        for (int i = 0; i < 5; i++) {
            float y0 = s * i / 5f;
            float y1 = s * (i + 1) / 5f;
            int top = 110 + i * 22;
            int bot = 70 + i * 18;
            p.setShader(vGrad(0, y0, y1,
                    Color.rgb(top, top - 10, top - 20),
                    Color.rgb(bot, bot - 10, bot - 20)));
            c.drawRect(0, y0, s, y1, p);
            // step nose highlight
            p.setShader(null);
            p.setColor(0x55FFFFFF);
            c.drawRect(0, y0, s, y0 + s * 0.012f, p);
            // step shadow line
            p.setColor(0x55000000);
            c.drawRect(0, y1 - s * 0.012f, s, y1, p);
        }
    }

    private void drawBus(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.92f, s * 0.46f, s * 0.05f);

        // body
        p.setShader(vGrad(0, s * 0.18f, s * 0.80f, 0xFFF2C868, 0xFFB07F26));
        c.drawRoundRect(new RectF(s * 0.04f, s * 0.20f, s * 0.96f, s * 0.80f),
                s * 0.10f, s * 0.10f, p);
        // belt line
        p.setShader(null);
        p.setColor(0xFF4D341A);
        c.drawRect(s * 0.04f, s * 0.55f, s * 0.96f, s * 0.58f, p);

        // windows row
        for (int i = 0; i < 4; i++) {
            float x = s * (0.10f + i * 0.20f);
            p.setShader(vGrad(0, s * 0.26f, s * 0.50f, 0xFFB8DEF7, 0xFF6FA9D6));
            c.drawRoundRect(new RectF(x, s * 0.26f, x + s * 0.16f, s * 0.50f),
                    s * 0.025f, s * 0.025f, p);
            p.setShader(null);
            p.setColor(0x55FFFFFF);
            c.drawRoundRect(new RectF(x + s * 0.012f, s * 0.27f, x + s * 0.05f, s * 0.34f),
                    s * 0.018f, s * 0.018f, p);
        }
        // door
        p.setShader(vGrad(0, s * 0.58f, s * 0.78f, 0xFF8B6228, 0xFF4D341A));
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.58f, s * 0.22f, s * 0.78f),
                s * 0.02f, s * 0.02f, p);
        // headlight
        p.setShader(new RadialGradient(s * 0.92f, s * 0.43f, s * 0.05f,
                0xFFFFFFC0, 0xFF888042, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.92f, s * 0.43f, s * 0.04f, p);

        // wheels
        wheel(c, p, s * 0.22f, s * 0.84f, s * 0.08f);
        wheel(c, p, s * 0.78f, s * 0.84f, s * 0.08f);
    }
    private void wheel(Canvas c, Paint p, float cx, float cy, float r) {
        p.setShader(new RadialGradient(cx, cy, r, 0xFF555555, 0xFF111111, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        p.setColor(0xFF999999);
        c.drawCircle(cx, cy, r * 0.45f, p);
        p.setColor(0xFF333333);
        c.drawCircle(cx, cy, r * 0.18f, p);
    }

    private void drawCar(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.88f, s * 0.44f, s * 0.05f);

        // lower body
        p.setShader(vGrad(0, s * 0.50f, s * 0.78f, 0xFFE57373, 0xFF8E2A2A));
        Path body = new Path();
        body.moveTo(s * 0.06f, s * 0.78f);
        body.lineTo(s * 0.10f, s * 0.50f);
        body.lineTo(s * 0.30f, s * 0.30f);
        body.lineTo(s * 0.70f, s * 0.30f);
        body.lineTo(s * 0.90f, s * 0.50f);
        body.lineTo(s * 0.94f, s * 0.78f);
        body.close();
        c.drawPath(body, p);

        // greenhouse
        p.setShader(vGrad(0, s * 0.30f, s * 0.50f, 0xFFB8DEF7, 0xFF6FA9D6));
        Path glass = new Path();
        glass.moveTo(s * 0.30f, s * 0.30f);
        glass.lineTo(s * 0.36f, s * 0.18f);
        glass.lineTo(s * 0.64f, s * 0.18f);
        glass.lineTo(s * 0.70f, s * 0.30f);
        glass.close();
        c.drawPath(glass, p);
        // glare
        p.setShader(null);
        p.setColor(0x77FFFFFF);
        c.drawRect(s * 0.38f, s * 0.22f, s * 0.46f, s * 0.28f, p);

        // door line
        p.setColor(0x55000000);
        p.setStrokeWidth(s * 0.012f);
        c.drawLine(s * 0.50f, s * 0.32f, s * 0.50f, s * 0.74f, p);

        // headlight
        p.setShader(new RadialGradient(s * 0.90f, s * 0.62f, s * 0.06f,
                0xFFFFFFC8, 0xFF665020, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.90f, s * 0.62f, s * 0.045f, p);

        // wheels
        wheel(c, p, s * 0.26f, s * 0.80f, s * 0.08f);
        wheel(c, p, s * 0.74f, s * 0.80f, s * 0.08f);
    }

    private void drawElevator(Canvas c, Paint p, int s) {
        // frame
        p.setShader(vGrad(0, 0, s, 0xFF6E7176, 0xFF34373B));
        c.drawRoundRect(new RectF(s * 0.06f, s * 0.04f, s * 0.94f, s * 0.96f),
                s * 0.04f, s * 0.04f, p);
        // doors
        p.setShader(hGrad(s * 0.10f, s * 0.50f, 0, 0xFFD9DCDF, 0xFF9FA3A8));
        c.drawRect(s * 0.12f, s * 0.10f, s * 0.49f, s * 0.90f, p);
        p.setShader(hGrad(s * 0.50f, s * 0.90f, 0, 0xFF9FA3A8, 0xFFD9DCDF));
        c.drawRect(s * 0.51f, s * 0.10f, s * 0.88f, s * 0.90f, p);
        // seam
        p.setShader(null);
        p.setColor(0xFF111111);
        c.drawRect(s * 0.49f, s * 0.10f, s * 0.51f, s * 0.90f, p);

        // floor indicator panel
        p.setColor(0xFF1A1F26);
        c.drawRoundRect(new RectF(s * 0.36f, s * 0.06f, s * 0.64f, s * 0.16f),
                s * 0.02f, s * 0.02f, p);
        // up arrow lit
        p.setColor(0xFFE0C36A);
        Path arr = new Path();
        arr.moveTo(s * 0.50f, s * 0.08f);
        arr.lineTo(s * 0.56f, s * 0.13f);
        arr.lineTo(s * 0.44f, s * 0.13f);
        arr.close();
        c.drawPath(arr, p);
    }

    private void drawDesk(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.94f, s * 0.40f, s * 0.05f);

        // top
        p.setShader(vGrad(0, s * 0.30f, s * 0.50f, 0xFFB07A48, 0xFF7A4F26));
        c.drawRoundRect(new RectF(s * 0.05f, s * 0.30f, s * 0.95f, s * 0.50f),
                s * 0.03f, s * 0.03f, p);
        // wood grain
        p.setShader(null);
        p.setColor(0x33000000);
        p.setStrokeWidth(s * 0.01f);
        for (int i = 0; i < 4; i++) c.drawLine(s * 0.08f, s * (0.34f + i * 0.04f),
                s * 0.92f, s * (0.34f + i * 0.04f), p);

        // legs
        p.setShader(vGrad(0, s * 0.50f, s * 0.90f, 0xFF7A4F26, 0xFF3F260F));
        c.drawRect(s * 0.10f, s * 0.50f, s * 0.18f, s * 0.90f, p);
        c.drawRect(s * 0.82f, s * 0.50f, s * 0.90f, s * 0.90f, p);
        // back rail
        c.drawRect(s * 0.18f, s * 0.85f, s * 0.82f, s * 0.90f, p);

        // a lamp on the desk
        p.setShader(null);
        p.setColor(0xFF2A2F36);
        c.drawCircle(s * 0.78f, s * 0.30f, s * 0.04f, p);
        Path lamp = new Path();
        lamp.moveTo(s * 0.78f, s * 0.30f);
        lamp.lineTo(s * 0.78f, s * 0.20f);
        lamp.lineTo(s * 0.88f, s * 0.14f);
        c.drawPath(lamp, p);
        p.setShader(new RadialGradient(s * 0.85f, s * 0.10f, s * 0.07f,
                0xFFFFEFA8, 0xFF765E14, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.84f, s * 0.10f, s * 0.06f, p);

        // a book
        p.setShader(null);
        p.setColor(0xFF4A6B8A);
        c.drawRect(s * 0.20f, s * 0.26f, s * 0.42f, s * 0.32f, p);
        p.setColor(0xFFE0C36A);
        c.drawRect(s * 0.22f, s * 0.28f, s * 0.40f, s * 0.30f, p);
    }

    private void drawBed(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.92f, s * 0.46f, s * 0.06f);

        // base / frame
        p.setShader(vGrad(0, s * 0.50f, s * 0.92f, 0xFF6B4A2A, 0xFF2F1D0E));
        c.drawRoundRect(new RectF(s * 0.04f, s * 0.50f, s * 0.96f, s * 0.88f),
                s * 0.05f, s * 0.05f, p);
        // mattress
        p.setShader(vGrad(0, s * 0.32f, s * 0.55f, 0xFFFFF2D8, 0xFFD9B988));
        c.drawRoundRect(new RectF(s * 0.06f, s * 0.30f, s * 0.94f, s * 0.55f),
                s * 0.04f, s * 0.04f, p);
        // blanket with stripes
        p.setShader(vGrad(0, s * 0.36f, s * 0.55f, 0xFF6FA9D6, 0xFF3F6B92));
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.36f, s * 0.94f, s * 0.55f),
                s * 0.04f, s * 0.04f, p);
        p.setShader(null);
        p.setColor(0x33FFFFFF);
        for (int i = 0; i < 5; i++)
            c.drawRect(s * (0.12f + i * 0.16f), s * 0.36f, s * (0.16f + i * 0.16f), s * 0.55f, p);
        // pillow
        p.setShader(vGrad(0, s * 0.20f, s * 0.32f, 0xFFFFFFFF, 0xFFD9D2BF));
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.20f, s * 0.40f, s * 0.36f),
                s * 0.04f, s * 0.04f, p);
        // headboard
        p.setShader(vGrad(0, s * 0.06f, s * 0.30f, 0xFF8C5A2E, 0xFF4A2C12));
        c.drawRoundRect(new RectF(s * 0.04f, s * 0.06f, s * 0.96f, s * 0.30f),
                s * 0.05f, s * 0.05f, p);
    }

    private void drawShop(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.94f, s * 0.40f, s * 0.05f);
        // body of coffee machine
        p.setShader(vGrad(0, s * 0.10f, s * 0.90f, 0xFF6E7176, 0xFF2C2F33));
        c.drawRoundRect(new RectF(s * 0.18f, s * 0.10f, s * 0.82f, s * 0.90f),
                s * 0.05f, s * 0.05f, p);
        // display
        p.setShader(null);
        p.setColor(0xFF111417);
        c.drawRoundRect(new RectF(s * 0.30f, s * 0.16f, s * 0.70f, s * 0.30f),
                s * 0.02f, s * 0.02f, p);
        p.setColor(0xFFE0C36A);
        c.drawRect(s * 0.36f, s * 0.20f, s * 0.50f, s * 0.24f, p);
        // buttons
        int[] btns = {0xFFD9D2BF, 0xFFE07474, 0xFF6FA9D6};
        for (int i = 0; i < 3; i++) {
            p.setColor(btns[i]);
            c.drawCircle(s * (0.32f + i * 0.10f), s * 0.36f, s * 0.022f, p);
        }
        // spout
        p.setColor(0xFF1A1F26);
        c.drawRoundRect(new RectF(s * 0.46f, s * 0.48f, s * 0.54f, s * 0.62f),
                s * 0.01f, s * 0.01f, p);
        // cup
        p.setShader(vGrad(0, s * 0.66f, s * 0.84f, 0xFFFFFFFF, 0xFFB8B8B8));
        c.drawRoundRect(new RectF(s * 0.42f, s * 0.66f, s * 0.58f, s * 0.84f),
                s * 0.02f, s * 0.02f, p);
        // coffee in cup
        p.setShader(null);
        p.setColor(0xFF4A2C12);
        c.drawRoundRect(new RectF(s * 0.44f, s * 0.70f, s * 0.56f, s * 0.78f),
                s * 0.015f, s * 0.015f, p);
        // steam
        p.setColor(0x55FFFFFF);
        c.drawCircle(s * 0.46f, s * 0.60f, s * 0.02f, p);
        c.drawCircle(s * 0.52f, s * 0.56f, s * 0.022f, p);
        c.drawCircle(s * 0.50f, s * 0.50f, s * 0.018f, p);
    }

    private void drawFountain(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.92f, s * 0.42f, s * 0.06f);
        // outer basin (stone)
        p.setShader(new RadialGradient(s * 0.46f, s * 0.46f, s * 0.46f,
                0xFFB8AFA0, 0xFF5D5648, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.5f, s * 0.55f, s * 0.42f, p);
        // water
        p.setShader(new RadialGradient(s * 0.46f, s * 0.50f, s * 0.36f,
                0xFFB6E0FF, 0xFF3F76A6, Shader.TileMode.CLAMP));
        c.drawCircle(s * 0.5f, s * 0.55f, s * 0.34f, p);
        // ripples
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.012f);
        p.setColor(0x77FFFFFF);
        c.drawCircle(s * 0.5f, s * 0.55f, s * 0.20f, p);
        p.setColor(0x44FFFFFF);
        c.drawCircle(s * 0.5f, s * 0.55f, s * 0.28f, p);
        p.setStyle(Paint.Style.FILL);

        // central pillar with water jet
        p.setShader(vGrad(0, s * 0.30f, s * 0.55f, 0xFF8E867B, 0xFF4A4438));
        c.drawRoundRect(new RectF(s * 0.46f, s * 0.30f, s * 0.54f, s * 0.55f),
                s * 0.02f, s * 0.02f, p);
        // jet
        p.setShader(vGrad(0, s * 0.10f, s * 0.30f, 0x88FFFFFF, 0xFFB6E0FF));
        Path jet = new Path();
        jet.moveTo(s * 0.46f, s * 0.30f);
        jet.cubicTo(s * 0.40f, s * 0.20f, s * 0.60f, s * 0.20f, s * 0.54f, s * 0.30f);
        jet.lineTo(s * 0.52f, s * 0.30f);
        jet.cubicTo(s * 0.56f, s * 0.22f, s * 0.44f, s * 0.22f, s * 0.48f, s * 0.30f);
        jet.close();
        c.drawPath(jet, p);
        // splash droplets
        p.setShader(null);
        p.setColor(0x99FFFFFF);
        c.drawCircle(s * 0.40f, s * 0.36f, s * 0.012f, p);
        c.drawCircle(s * 0.60f, s * 0.36f, s * 0.014f, p);
        c.drawCircle(s * 0.34f, s * 0.46f, s * 0.010f, p);
    }

    private void drawPillar(Canvas c, Paint p, int s) {
        shadow(c, p, s * 0.5f, s * 0.92f, s * 0.30f, s * 0.05f);
        // shaft
        p.setShader(hGrad(s * 0.30f, s * 0.70f, 0, 0xFFE6E0D0, 0xFF6E6859));
        c.drawRect(s * 0.34f, s * 0.16f, s * 0.66f, s * 0.84f, p);
        // marble veins
        p.setShader(null);
        p.setColor(0x33000000);
        p.setStrokeWidth(s * 0.008f);
        c.drawLine(s * 0.38f, s * 0.20f, s * 0.44f, s * 0.80f, p);
        c.drawLine(s * 0.56f, s * 0.22f, s * 0.62f, s * 0.78f, p);
        // capital
        p.setShader(vGrad(0, s * 0.10f, s * 0.20f, 0xFFFFF6E0, 0xFFB7AC8C));
        c.drawRect(s * 0.28f, s * 0.10f, s * 0.72f, s * 0.18f, p);
        // base
        p.setShader(vGrad(0, s * 0.82f, s * 0.92f, 0xFFCABF9A, 0xFF6F684E));
        c.drawRect(s * 0.28f, s * 0.82f, s * 0.72f, s * 0.92f, p);
    }

    // ---------- color helpers ----------

    private static int randomShirt(Random rng) {
        int[] pal = {0xFF4F6E9C, 0xFF7E4B4B, 0xFF4F7F5A, 0xFF8E6E2A, 0xFF6E4787, 0xFF2E5E78, 0xFF8E563E};
        return pal[rng.nextInt(pal.length)];
    }
    private static int randomHair(Random rng) {
        int[] pal = {0xFF2A1F1A, 0xFF3F2A18, 0xFF5C3A1C, 0xFF1A1A1A, 0xFF8A6B3A};
        return pal[rng.nextInt(pal.length)];
    }
    private static int randomSkin(Random rng) {
        int[] pal = {0xFFE0B07E, 0xFFCFA988, 0xFFB58970, 0xFF8A6447, 0xFFE5C29A};
        return pal[rng.nextInt(pal.length)];
    }
    private static int lighten(int color, float t) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) Math.min(255, ((color >> 16) & 0xFF) + 255 * t);
        int g = (int) Math.min(255, ((color >> 8) & 0xFF) + 255 * t);
        int b = (int) Math.min(255, (color & 0xFF) + 255 * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    private static int darken(int color, float t) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) Math.max(0, ((color >> 16) & 0xFF) * (1 - t));
        int g = (int) Math.max(0, ((color >> 8) & 0xFF) * (1 - t));
        int b = (int) Math.max(0, (color & 0xFF) * (1 - t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
