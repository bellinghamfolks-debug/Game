package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Offline environment describer that uses simple geometry to produce
 * short, natural Arabic sentences. No internet required.
 */
public class EnvDescriber {

    /** Generate one-paragraph description of what surrounds the player. */
    public static String describe(GameState gs) {
        if (gs == null || gs.scene == null) return "أنت في مكان غير محدد.";
        Scene scene = gs.scene;
        List<String> chunks = new ArrayList<>();
        chunks.add("أنت في " + scene.name + ".");
        // direction reference
        chunks.add("اتجاهك " + headingWord(gs.player.heading) + ".");

        // collect nearby entities, sorted by distance
        List<NearEntity> near = new ArrayList<>();
        for (Entity e : scene.entities) {
            float dx = e.x - gs.player.x, dy = e.y - gs.player.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d <= 8f) near.add(new NearEntity(e, d, relSide(gs, e)));
        }
        Collections.sort(near, (a, b) -> Float.compare(a.dist, b.dist));

        if (near.isEmpty()) {
            chunks.add("لا شيء قريب منك مباشرة. المكان هادئ.");
        } else {
            int count = Math.min(4, near.size());
            for (int i = 0; i < count; i++) {
                NearEntity ne = near.get(i);
                String dist = humanDist(ne.dist);
                String side = ne.side;
                String name = ne.e.name != null ? ne.e.name : "شيء";
                String tag = ne.e.tag != null ? " — " + ne.e.tag : "";
                chunks.add(name + " " + side + " يبعد " + dist + tag + ".");
            }
            if (near.size() > count) {
                chunks.add("هناك أيضًا " + (near.size() - count) + " عناصر أبعد.");
            }
        }

        return join(chunks, " ");
    }

    /** Very short "what's right in front of me" cue, fast to repeat. */
    public static String describeShort(GameState gs) {
        if (gs == null || gs.scene == null) return "";
        Entity e = gs.scene.nearest(gs.player.x, gs.player.y, 3.5f);
        if (e == null) return "أمامك مساحة فارغة.";
        float dx = e.x - gs.player.x, dy = e.y - gs.player.y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        return e.name + " " + relSide(gs, e) + " على بُعد " + humanDist(d) + ".";
    }

    public static String describeOnArrival(Scene s) {
        if (s == null) return "";
        switch (s.id) {
            case HOME: return "أنت في غرفتك. الباب على يمين البعيد.";
            case STREET: return "خرجتَ إلى الشارع. الجامعة على اليمين، والمقهى أمامك.";
            case UNIVERSITY: return "أنت داخل الجامعة. القاعة بجوار المصعد.";
            case CAFE: return "دخلتَ المقهى. رائحة قهوة طازجة.";
        }
        return "";
    }

    private static String headingWord(float h) {
        // normalize 0..2pi
        double a = h;
        while (a < 0) a += 2 * Math.PI;
        while (a >= 2 * Math.PI) a -= 2 * Math.PI;
        // 0 = east, pi/2 = south, pi = west, 3pi/2 = north
        double deg = Math.toDegrees(a);
        if (deg < 22.5 || deg >= 337.5) return "شرق";
        if (deg < 67.5) return "جنوب شرق";
        if (deg < 112.5) return "جنوب";
        if (deg < 157.5) return "جنوب غرب";
        if (deg < 202.5) return "غرب";
        if (deg < 247.5) return "شمال غرب";
        if (deg < 292.5) return "شمال";
        return "شمال شرق";
    }

    private static String humanDist(float d) {
        if (d < 0.5f) return "خطوة واحدة";
        if (d < 1.5f) return "خطوتين";
        if (d < 2.5f) return "ثلاث خطوات";
        if (d < 4.0f) return "أربع خطوات";
        if (d < 6.0f) return "ستّ خطوات";
        return "أبعد من ذلك";
    }

    private static String relSide(GameState gs, Entity e) {
        float dx = e.x - gs.player.x, dy = e.y - gs.player.y;
        float worldAngle = (float) Math.atan2(dy, dx);
        double rel = worldAngle - gs.player.heading;
        while (rel > Math.PI) rel -= 2 * Math.PI;
        while (rel < -Math.PI) rel += 2 * Math.PI;
        double deg = Math.toDegrees(rel);
        if (deg > -22.5 && deg < 22.5) return "أمامك";
        if (deg >= 22.5 && deg < 67.5) return "أمامك على اليمين";
        if (deg >= 67.5 && deg < 112.5) return "على يمينك";
        if (deg >= 112.5 && deg < 157.5) return "خلفك على اليمين";
        if (deg <= -22.5 && deg > -67.5) return "أمامك على اليسار";
        if (deg <= -67.5 && deg > -112.5) return "على يسارك";
        if (deg <= -112.5 && deg > -157.5) return "خلفك على اليسار";
        return "خلفك";
    }

    private static String join(List<String> items, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static class NearEntity {
        final Entity e;
        final float dist;
        final String side;
        NearEntity(Entity e, float d, String s) { this.e = e; this.dist = d; this.side = s; }
    }
}
