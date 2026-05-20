package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.world.EventLog;

import java.util.List;
import java.util.Random;

/**
 * The "no internet, no API key" content layer.
 *
 * Everything an AI manager could produce — descriptions, hints, summaries,
 * mission narration, dialogue colour, day intros — has a hand-authored
 * Arabic equivalent here. Managers funnel through this provider on any
 * failure path, so the player never sees an empty bubble or a network
 * error popup.
 */
public final class OfflineContentProvider {

    private OfflineContentProvider() {}

    private static final Random RNG = new Random();

    // ---------------- mission narration ----------------

    public static String missionStart(Mission m) {
        if (m == null) return "";
        return "مهمة جديدة: " + m.title + ". " + (m.description != null ? m.description : "");
    }

    public static String missionDone(Mission m) {
        if (m == null) return "";
        String[] praise = {
                "أحسنت! أتممت: ",
                "ممتاز! أنجزت: ",
                "تمام! تم: ",
                "رائع! أنهيت: ",
                "خطوة عظيمة! "
        };
        return praise[RNG.nextInt(praise.length)] + m.title + ".";
    }

    public static String dayIntro(int day) {
        switch (day) {
            case 1: return "صباح اليوم الأول. خذ وقتك للتعرف على العالم.";
            case 2: return "صباح اليوم الثاني. اليوم درس في الأصوات.";
            case 3: return "صباح اليوم الثالث. الجامعة تنتظرك بعمق أكبر.";
            case 4: return "صباح اليوم الرابع. ستلتقي بمواقف قد تكون محرجة. ثق بنفسك.";
            case 5: return "صباح اليوم الخامس. اليوم اختبار. تنفّس بعمق.";
            case 6: return "صباح اليوم السادس. الجو ممطر والشارع مزدحم. خذ حذرك.";
            case 7: return "صباح اليوم الأخير من الأسبوع. اجعله يومًا تفتخر به.";
            default: return "صباح يوم جديد.";
        }
    }

    public static String dayComplete(int day) {
        if (day >= 7) return "أكملت الأسبوع الأول كاملًا. مبروك! تستطيع الآن التجوّل بحرية أو بدء أسبوع جديد.";
        return "انتهت مهام اليوم رقم " + day + ". افتح القائمة لتسمع ملخصًا، ثم نم استعدادًا لليوم القادم.";
    }

    // ---------------- hints ----------------

    public static String hintFor(Mission m, GameState gs) {
        if (m == null) return "تجوّل قليلًا. اضغط مطوّلًا لتسمع الوصف، أو اضغط 🧭 لاختيار وجهة.";
        StringBuilder sb = new StringBuilder();
        sb.append("المهمة الحالية: ").append(m.title).append(". ");
        if (m.hint != null && !m.hint.isEmpty()) sb.append(m.hint);
        else sb.append("اضغط مطوّلًا للوصف ثم اقترب وانقر مرتين.");
        return sb.toString();
    }

    // ---------------- summary ----------------

    public static String summarize(GameState gs, List<EventLog.Entry> recent) {
        StringBuilder sb = new StringBuilder();
        sb.append("ملخص اليوم ").append(gs.day).append(": ");
        sb.append("الحركة ").append(gs.player.mobility).append("، ");
        sb.append("العلاقات ").append(gs.player.social).append("، ");
        sb.append("التقنية ").append(gs.player.tech).append("، ");
        sb.append("الثقة ").append(gs.player.confidence).append(". ");
        if (recent != null && !recent.isEmpty()) {
            EventLog.Entry last = recent.get(recent.size() - 1);
            sb.append("آخر ما حدث: ").append(last.text).append(". ");
        }
        sb.append(adviceForStats(gs));
        return sb.toString();
    }

    private static String adviceForStats(GameState gs) {
        int weakest = weakestStat(gs);
        switch (weakest) {
            case 0: return "نصيحة: ركّز غدًا على التنقل. هز الجهاز لإعادة الاتجاه عند الحاجة.";
            case 1: return "نصيحة: تحدّث مع الناس أكثر. الأصدقاء يفتحون أبوابًا.";
            case 2: return "نصيحة: جرّب أزرارك الذكية أكثر — التقنية تُساعد.";
            default: return "نصيحة: ثقتك تُبنى بقرارات صغيرة. اختر بحكمة.";
        }
    }

    private static int weakestStat(GameState gs) {
        int min = gs.player.mobility, idx = 0;
        if (gs.player.social < min) { min = gs.player.social; idx = 1; }
        if (gs.player.tech < min) { min = gs.player.tech; idx = 2; }
        if (gs.player.confidence < min) { idx = 3; }
        return idx;
    }

    // ---------------- generic descriptive flavor ----------------

    public static String onArrival(Scene scene) {
        if (scene == null) return "";
        switch (scene.id) {
            case HOME: return "أنت في غرفتك. الباب على يمين البعيد.";
            case STREET: return "خرجتَ إلى الشارع. الجامعة على اليمين، والمقهى أمامك.";
            case UNIVERSITY: return "أنت داخل الجامعة. القاعة بجوار المصعد.";
            case CAFE: return "دخلتَ المقهى. رائحة قهوة طازجة.";
        }
        return "";
    }

    public static String nearestPhrase(Entity e, float distTiles) {
        if (e == null) return "أمامك مساحة فارغة.";
        int steps = Math.max(1, Math.round(distTiles));
        return e.name + " قريب منك على بُعد " + steps + " خطوة.";
    }
}
