package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Turn-by-turn voice guidance.
 *
 * - {@link #start(GameState, Entity)} plans an A* path and returns an
 *   intro sentence summarizing the route.
 * - {@link #tick(GameState)} is called every game frame. When the player
 *   passes a waypoint, drifts off course, or hasn't heard anything for
 *   a few seconds, this returns a short sentence to speak.
 * - {@link #cancel()} stops guidance.
 *
 * The narration uses relative directions so the player only has to
 * understand "left / right / straight / behind", not compass.
 */
public class NavGuide {

    public static class Segment {
        public final float angle;       // world angle of this leg
        public final float length;      // tiles
        public final float endX, endY;  // world coordinates
        Segment(float a, float l, float ex, float ey) {
            this.angle = a; this.length = l; this.endX = ex; this.endY = ey;
        }
    }

    private Entity target;
    private List<Segment> segments;
    private int idx;
    private boolean spokenSegmentStart;
    private boolean reached;
    private long lastEncourageMs;
    private long lastReplanMs;
    private float initialDistance;

    public boolean isActive() {
        return segments != null && idx < segments.size() && !reached;
    }
    public boolean isReached() { return reached; }
    public Entity target() { return target; }

    /**
     * Plan a path from the player's current position to target.
     * Returns the introductory sentence to speak, or a fallback message.
     */
    public String start(GameState gs, Entity target) {
        this.target = target;
        this.idx = 0;
        this.reached = false;
        this.spokenSegmentStart = false;
        long now = System.currentTimeMillis();
        this.lastEncourageMs = now;
        this.lastReplanMs = now;

        List<Pathfinder.Point> path = Pathfinder.find(gs.scene, gs.player.x, gs.player.y, target);
        if (path == null || path.size() < 2) {
            segments = null;
            return "آسف، لم أجد طريقًا واضحًا إلى " + nameOf(target) + ". جرّب الاقتراب أكثر أو هز الجهاز.";
        }
        segments = toSegments(path);
        if (segments.isEmpty()) {
            segments = null;
            reached = true;
            return "أنت قريب جدًا من " + nameOf(target) + ". انقر مرتين للتفاعل.";
        }

        this.initialDistance = totalLength();

        StringBuilder sb = new StringBuilder();
        sb.append("سأرشدك إلى ").append(nameOf(target)).append(". ");
        sb.append("المسافة الكلية تقريبًا ").append(steps(initialDistance)).append(". ");

        Segment first = segments.get(0);
        sb.append(relativeTurnPhrase(gs.player.heading, first.angle))
                .append(" ثم امشِ ").append(steps(first.length)).append(".");

        // brief preview of next leg if there is one
        if (segments.size() > 1) {
            Segment second = segments.get(1);
            sb.append(" بعدها ")
                    .append(relativeTurnPhrase(first.angle, second.angle))
                    .append(" وامشِ ").append(steps(second.length)).append(".");
        }
        if (segments.size() > 2) sb.append(" بعد ذلك سأتابع توجيهك خطوة بخطوة.");
        spokenSegmentStart = true; // we just spoke the first segment intro
        return sb.toString();
    }

    /**
     * Per-tick update. Returns text to speak, or null.
     */
    public String tick(GameState gs) {
        if (!isActive()) return null;
        Segment cur = segments.get(idx);
        float dist = (float) Math.hypot(cur.endX - gs.player.x, cur.endY - gs.player.y);
        long now = System.currentTimeMillis();

        // 1) reached current waypoint?
        if (dist < 0.7f) {
            idx++;
            if (idx >= segments.size()) {
                reached = true;
                return "وصلتَ إلى " + nameOf(target) + ". انقر مرتين للتفاعل.";
            }
            spokenSegmentStart = true;
            Segment next = segments.get(idx);
            return relativeTurnPhrase(cur.angle, next.angle)
                    + " وامشِ " + steps(next.length) + ".";
        }

        // 2) drift detection: if player is far from this segment's line, re-plan
        float lateral = lateralDistanceFromSegment(gs, cur);
        if (lateral > 2.0f && now - lastReplanMs > 3000) {
            lastReplanMs = now;
            return "خرجت عن المسار قليلًا. سأعيد حساب الطريق… "
                    + restart(gs);
        }

        // 3) periodic encouragement / correction every ~4s
        if (now - lastEncourageMs > 4200) {
            lastEncourageMs = now;
            float headingErr = wrap(cur.angle - gs.player.heading);
            String correction = headingCorrection(headingErr);
            return correction + " بقي " + steps(dist) + ".";
        }
        return null;
    }

    private String restart(GameState gs) {
        Entity t = this.target;
        return start(gs, t);
    }

    public void cancel() {
        segments = null;
        idx = 0;
        reached = false;
        target = null;
    }

    // ---------------- helpers ----------------

    private static List<Segment> toSegments(List<Pathfinder.Point> path) {
        List<Segment> out = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            Pathfinder.Point a = path.get(i);
            Pathfinder.Point b = path.get(i + 1);
            float dx = b.x - a.x, dy = b.y - a.y;
            float len = (float) Math.hypot(dx, dy);
            if (len < 0.3f) continue;
            float angle = (float) Math.atan2(dy, dx);
            out.add(new Segment(angle, len, b.x, b.y));
        }
        return out;
    }

    private float totalLength() {
        float t = 0;
        if (segments == null) return 0;
        for (Segment s : segments) t += s.length;
        return t;
    }

    /** Perpendicular distance of the player from the segment's straight line. */
    private static float lateralDistanceFromSegment(GameState gs, Segment s) {
        // segment goes from (endX - len*cos, endY - len*sin) to (endX, endY)
        float ax = s.endX - s.length * (float) Math.cos(s.angle);
        float ay = s.endY - s.length * (float) Math.sin(s.angle);
        float vx = s.endX - ax, vy = s.endY - ay;
        float wx = gs.player.x - ax, wy = gs.player.y - ay;
        float c2 = vx * vx + vy * vy;
        if (c2 < 1e-4f) return 0;
        float t = Math.max(0f, Math.min(1f, (wx * vx + wy * vy) / c2));
        float px = ax + t * vx, py = ay + t * vy;
        return (float) Math.hypot(gs.player.x - px, gs.player.y - py);
    }

    private static String steps(float tiles) {
        int n = Math.max(1, Math.round(tiles));
        switch (n) {
            case 1: return "خطوة واحدة";
            case 2: return "خطوتين";
            case 3: return "ثلاث خطوات";
            case 4: return "أربع خطوات";
            case 5: return "خمس خطوات";
            case 6: return "ست خطوات";
            case 7: return "سبع خطوات";
            case 8: return "ثماني خطوات";
            case 9: return "تسع خطوات";
            case 10: return "عشر خطوات";
            default: return "حوالي " + n + " خطوة";
        }
    }

    private static String relativeTurnPhrase(float fromAngle, float toAngle) {
        double d = Math.toDegrees(wrap(toAngle - fromAngle));
        // In screen-style world: +y is "south" but we describe relative to player.
        // Positive delta = clockwise = right. We use that convention.
        double abs = Math.abs(d);
        if (abs < 15) return "استمر مباشرة";
        if (abs < 45) return d > 0 ? "اتجه قليلًا لليمين" : "اتجه قليلًا لليسار";
        if (abs < 110) return d > 0 ? "استدر يمينًا" : "استدر يسارًا";
        if (abs < 160) return d > 0 ? "استدر بقوة لليمين" : "استدر بقوة لليسار";
        return "استدر للخلف";
    }

    private static String headingCorrection(float headingErr) {
        double d = Math.toDegrees(headingErr);
        double abs = Math.abs(d);
        if (abs < 12) return "أحسنت، استمر";
        if (abs < 30) return d > 0 ? "صحح يمينًا قليلًا" : "صحح يسارًا قليلًا";
        if (abs < 90) return d > 0 ? "اتجه يمينًا" : "اتجه يسارًا";
        return "غيّر اتجاهك، أنت بعيد عن المسار";
    }

    private static float wrap(float a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
    private static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private static String nameOf(Entity e) {
        return e == null ? "الهدف" : (e.name != null ? e.name : "الهدف");
    }
}
