package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.world.EventLog;

import java.util.List;

/**
 * End-of-day summary. Reads the EventLog and player stats and produces
 * a short reflective paragraph in Arabic, ending with one piece of
 * advice for tomorrow. Local fallback always works.
 */
public class AiSummaryManager {

    public interface Callback {
        void onSummary(String text, boolean fromAi);
    }

    private final AiClient client;
    private final AiSafetyFilter safety;

    public AiSummaryManager(AiClient client, AiSafetyFilter safety) {
        this.client = client;
        this.safety = safety;
    }

    public void daySummary(GameState gs, EventLog log, boolean aiEnabled, int mode, Callback cb) {
        List<EventLog.Entry> recent = log.recent(25);
        String local = AiFallbackProvider.summarize(gs, recent);
        if (!aiEnabled || mode <= 0 || client == null || !client.isConfigured()) {
            cb.onSummary(local, false);
            return;
        }

        String system = "أنت راوي يلخّص يوم لاعب كفيف في لعبة قصصية بالعربية. "
                + "اكتب فقرة دافئة قصيرة (3 إلى 5 جمل) تذكر أهم قراراته اليوم ومهاراته. "
                + "اختم بنصيحة شخصية واحدة لليوم القادم.";

        StringBuilder u = new StringBuilder();
        u.append("اليوم رقم ").append(gs.day).append(".\n");
        u.append("المهارات: حركة ").append(gs.player.mobility)
                .append("، علاقات ").append(gs.player.social)
                .append("، تقنية ").append(gs.player.tech)
                .append("، ثقة ").append(gs.player.confidence).append(".\n");
        u.append("أحداث اليوم بالترتيب الزمني:\n");
        for (EventLog.Entry e : recent) {
            u.append("- ").append(e.timeStr).append(' ').append(e.text).append('\n');
        }

        client.ask(system, u.toString(), new AiClient.Callback() {
            @Override public void onReply(String text) {
                String safe = safety.filter(text);
                cb.onSummary(safe != null && !safe.isEmpty() ? safe : local, safe != null);
            }
            @Override public void onError(String msg) { cb.onSummary(local, false); }
        });
    }
}
