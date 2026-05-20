package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.world.EventLog;

import java.util.List;

/**
 * Local-only fall-back strings. Always returns something useful even
 * with no AI key, no network, and no LLM available. Every AI manager
 * funnels through here when the request fails or the user is offline.
 */
public final class AiFallbackProvider {

    private AiFallbackProvider() {}

    public static String hintFor(Mission m, GameState gs) {
        if (m == null) return "تجوّل قليلًا. اضغط مطوّلًا لتسمع وصف ما حولك، أو اضغط 🧭 ليرشدك المرشد إلى أقرب وجهة.";
        StringBuilder sb = new StringBuilder();
        sb.append("المهمة: ").append(m.title).append(". ");
        if (m.hint != null && !m.hint.isEmpty()) sb.append(m.hint);
        else sb.append("اضغط مطوّلًا لوصف البيئة، ثم اقترب وانقر مرتين.");
        return sb.toString();
    }

    public static String summarize(GameState gs, List<EventLog.Entry> recent) {
        StringBuilder sb = new StringBuilder();
        sb.append("ملخص اليوم رقم ").append(gs.day).append(": ");
        sb.append("الحركة ").append(gs.player.mobility).append("، ");
        sb.append("العلاقات ").append(gs.player.social).append("، ");
        sb.append("التقنية ").append(gs.player.tech).append("، ");
        sb.append("الثقة ").append(gs.player.confidence).append(". ");
        if (recent != null && !recent.isEmpty()) {
            EventLog.Entry last = recent.get(recent.size() - 1);
            sb.append("آخر حدث: ").append(last.text);
        }
        sb.append(" — نَم وعد غدًا.");
        return sb.toString();
    }

    public static String narrateMissionStart(Mission m) {
        if (m == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("مهمة جديدة: ").append(m.title).append(". ");
        if (m.description != null) sb.append(m.description);
        return sb.toString();
    }

    public static String narrateMissionDone(Mission m) {
        if (m == null) return "";
        return "أحسنت! أتممت: " + m.title + ".";
    }

    public static String narrateDayComplete(int day) {
        return "انتهى اليوم رقم " + day + ". افتح القائمة لتسمع ملخصًا.";
    }
}
