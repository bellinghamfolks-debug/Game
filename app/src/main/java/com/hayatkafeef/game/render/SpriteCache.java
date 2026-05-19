package com.hayatkafeef.game.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.hayatkafeef.game.game.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Procedurally drawn sprites cached as bitmaps. No image assets needed.
 * Each entity Kind has its own little "look".
 */
public class SpriteCache {

    private final Map<String, Bitmap> cache = new HashMap<>();

    public Bitmap get(Entity.Kind kind, int sizePx) {
        String key = kind.name() + ":" + sizePx;
        Bitmap b = cache.get(key);
        if (b != null) return b;
        b = build(kind, sizePx);
        cache.put(key, b);
        return b;
    }

    private Bitmap build(Entity.Kind kind, int s) {
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        switch (kind) {
            case PERSON: drawPerson(c, p, s); break;
            case DOOR: drawDoor(c, p, s); break;
            case BENCH: drawBench(c, p, s); break;
            case TREE: drawTree(c, p, s); break;
            case WALL: drawWall(c, p, s); break;
            case STAIRS: drawStairs(c, p, s); break;
            case BUS: drawBus(c, p, s); break;
            case CAR: drawCar(c, p, s); break;
            case ELEVATOR: drawElevator(c, p, s); break;
            case DESK: drawDesk(c, p, s); break;
            case BED: drawBed(c, p, s); break;
            case SHOP: drawShop(c, p, s); break;
            case FOUNTAIN: drawFountain(c, p, s); break;
            case PILLAR: drawPillar(c, p, s); break;
        }
        return bm;
    }

    /** Player sprite (drawn separately because it has facing). */
    public Bitmap player(int s, float heading) {
        String key = "PLAYER:" + s + ":" + ((int) (heading * 16));
        Bitmap b = cache.get(key);
        if (b != null) return b;
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = s / 2f, cy = s / 2f;
        // shadow
        p.setColor(0x55000000);
        c.drawOval(cx - s * 0.35f, cy + s * 0.25f, cx + s * 0.35f, cy + s * 0.42f, p);
        // body
        p.setColor(0xFF222933);
        c.drawCircle(cx, cy, s * 0.30f, p);
        // head highlight
        p.setColor(0xFFE0C36A);
        c.drawCircle(cx, cy - s * 0.05f, s * 0.10f, p);
        // facing indicator: small cane in front
        p.setColor(0xFFEEEEEE);
        float len = s * 0.45f;
        float ex = cx + (float) Math.cos(heading) * len;
        float ey = cy + (float) Math.sin(heading) * len;
        p.setStrokeWidth(s * 0.05f);
        c.drawLine(cx, cy, ex, ey, p);
        // red tip
        p.setColor(0xFFE07474);
        c.drawCircle(ex, ey, s * 0.06f, p);
        cache.put(key, bm);
        return bm;
    }

    private void drawPerson(Canvas c, Paint p, int s) {
        float cx = s / 2f, cy = s / 2f;
        p.setColor(0x55000000);
        c.drawOval(cx - s * 0.30f, cy + s * 0.25f, cx + s * 0.30f, cy + s * 0.40f, p);
        // body
        p.setColor(0xFF6C90B5);
        c.drawRoundRect(new RectF(cx - s * 0.22f, cy - s * 0.10f, cx + s * 0.22f, cy + s * 0.30f), s * 0.08f, s * 0.08f, p);
        // head
        p.setColor(0xFFE0B07E);
        c.drawCircle(cx, cy - s * 0.22f, s * 0.16f, p);
        // hair
        p.setColor(0xFF2A1F1A);
        Path hair = new Path();
        hair.moveTo(cx - s * 0.16f, cy - s * 0.22f);
        hair.quadTo(cx, cy - s * 0.40f, cx + s * 0.16f, cy - s * 0.22f);
        hair.close();
        c.drawPath(hair, p);
    }

    private void drawDoor(Canvas c, Paint p, int s) {
        p.setColor(0xFF8B5A2B);
        c.drawRoundRect(new RectF(s * 0.15f, s * 0.05f, s * 0.85f, s * 0.95f), s * 0.05f, s * 0.05f, p);
        p.setColor(0xFFE0C36A);
        c.drawCircle(s * 0.75f, s * 0.55f, s * 0.05f, p);
        p.setColor(0xFF563419);
        p.setStrokeWidth(s * 0.02f);
        p.setStyle(Paint.Style.STROKE);
        c.drawRoundRect(new RectF(s * 0.20f, s * 0.10f, s * 0.80f, s * 0.90f), s * 0.03f, s * 0.03f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawBench(Canvas c, Paint p, int s) {
        p.setColor(0xFF8B5A2B);
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.45f, s * 0.90f, s * 0.65f), s * 0.04f, s * 0.04f, p);
        p.setColor(0xFF563419);
        c.drawRect(s * 0.15f, s * 0.65f, s * 0.22f, s * 0.85f, p);
        c.drawRect(s * 0.78f, s * 0.65f, s * 0.85f, s * 0.85f, p);
    }

    private void drawTree(Canvas c, Paint p, int s) {
        p.setColor(0xFF563419);
        c.drawRect(s * 0.43f, s * 0.55f, s * 0.57f, s * 0.95f, p);
        p.setColor(0xFF3C6B36);
        c.drawCircle(s * 0.50f, s * 0.40f, s * 0.32f, p);
        p.setColor(0xFF4F8A45);
        c.drawCircle(s * 0.35f, s * 0.45f, s * 0.20f, p);
        c.drawCircle(s * 0.65f, s * 0.45f, s * 0.20f, p);
    }

    private void drawWall(Canvas c, Paint p, int s) {
        p.setColor(0xFF6B5A48);
        c.drawRect(0, 0, s, s, p);
        p.setColor(0xFF4E3F32);
        p.setStrokeWidth(2);
        for (int i = 1; i < 4; i++) c.drawLine(0, s * i / 4f, s, s * i / 4f, p);
    }

    private void drawStairs(Canvas c, Paint p, int s) {
        for (int i = 0; i < 5; i++) {
            int g = 60 + i * 25;
            p.setColor(Color.rgb(g, g, g));
            c.drawRect(0, s * i / 5f, s, s * (i + 1) / 5f, p);
        }
    }

    private void drawBus(Canvas c, Paint p, int s) {
        p.setColor(0xFFE0C36A);
        c.drawRoundRect(new RectF(s * 0.05f, s * 0.20f, s * 0.95f, s * 0.80f), s * 0.08f, s * 0.08f, p);
        p.setColor(0xFF98C7F0);
        c.drawRect(s * 0.15f, s * 0.30f, s * 0.45f, s * 0.55f, p);
        c.drawRect(s * 0.55f, s * 0.30f, s * 0.85f, s * 0.55f, p);
        p.setColor(0xFF222933);
        c.drawCircle(s * 0.25f, s * 0.82f, s * 0.08f, p);
        c.drawCircle(s * 0.75f, s * 0.82f, s * 0.08f, p);
    }

    private void drawCar(Canvas c, Paint p, int s) {
        p.setColor(0xFFD45656);
        c.drawRoundRect(new RectF(s * 0.05f, s * 0.45f, s * 0.95f, s * 0.80f), s * 0.10f, s * 0.10f, p);
        p.setColor(0xFFB04545);
        c.drawRoundRect(new RectF(s * 0.20f, s * 0.30f, s * 0.80f, s * 0.55f), s * 0.10f, s * 0.10f, p);
        p.setColor(0xFF222933);
        c.drawCircle(s * 0.25f, s * 0.82f, s * 0.08f, p);
        c.drawCircle(s * 0.75f, s * 0.82f, s * 0.08f, p);
    }

    private void drawElevator(Canvas c, Paint p, int s) {
        p.setColor(0xFFB8BCC2);
        c.drawRect(s * 0.10f, s * 0.05f, s * 0.90f, s * 0.95f, p);
        p.setColor(0xFF6B6F75);
        c.drawRect(s * 0.48f, s * 0.10f, s * 0.52f, s * 0.90f, p);
        p.setColor(0xFFE0C36A);
        c.drawCircle(s * 0.50f, s * 0.18f, s * 0.06f, p);
    }

    private void drawDesk(Canvas c, Paint p, int s) {
        p.setColor(0xFF8B5A2B);
        c.drawRect(s * 0.05f, s * 0.30f, s * 0.95f, s * 0.55f, p);
        p.setColor(0xFF563419);
        c.drawRect(s * 0.10f, s * 0.55f, s * 0.20f, s * 0.95f, p);
        c.drawRect(s * 0.80f, s * 0.55f, s * 0.90f, s * 0.95f, p);
    }

    private void drawBed(Canvas c, Paint p, int s) {
        p.setColor(0xFF6B5A48);
        c.drawRoundRect(new RectF(s * 0.05f, s * 0.20f, s * 0.95f, s * 0.85f), s * 0.06f, s * 0.06f, p);
        p.setColor(0xFFE0C36A);
        c.drawRoundRect(new RectF(s * 0.10f, s * 0.30f, s * 0.90f, s * 0.55f), s * 0.04f, s * 0.04f, p);
    }

    private void drawShop(Canvas c, Paint p, int s) {
        p.setColor(0xFF2A2F36);
        c.drawRect(s * 0.10f, s * 0.20f, s * 0.90f, s * 0.95f, p);
        p.setColor(0xFFE0C36A);
        c.drawRect(s * 0.15f, s * 0.30f, s * 0.85f, s * 0.45f, p);
    }

    private void drawFountain(Canvas c, Paint p, int s) {
        p.setColor(0xFF98C7F0);
        c.drawCircle(s / 2f, s / 2f, s * 0.40f, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(s / 2f, s / 2f, s * 0.15f, p);
        p.setColor(0xFFB58F2C);
        p.setStrokeWidth(s * 0.04f);
        p.setStyle(Paint.Style.STROKE);
        c.drawCircle(s / 2f, s / 2f, s * 0.40f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawPillar(Canvas c, Paint p, int s) {
        p.setColor(0xFFB8BCC2);
        c.drawRect(s * 0.35f, s * 0.10f, s * 0.65f, s * 0.90f, p);
        p.setColor(0xFF6B6F75);
        c.drawRect(s * 0.30f, s * 0.05f, s * 0.70f, s * 0.15f, p);
        c.drawRect(s * 0.30f, s * 0.85f, s * 0.70f, s * 0.95f, p);
    }
}
