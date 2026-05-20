package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.missions.Mission;

/**
 * "أعطِني تلميحًا" — returns a context-aware hint.
 * Always emits a local hint first; if AI is enabled and reachable,
 * fetches a personalised version that replaces it via the callback.
 */
public class AiHintManager {

    public interface Callback {
        /** fromAi=true means the text came from the LLM. */
        void onHint(String text, boolean fromAi);
    }

    private final AiClient client;
    private final AiSafetyFilter safety;

    public AiHintManager(AiClient client, AiSafetyFilter safety) {
        this.client = client;
        this.safety = safety;
    }

    public void ask(GameState gs, Mission mission, boolean aiEnabled, int mode, Callback cb) {
        String local = AiFallbackProvider.hintFor(mission, gs);
        if (!aiEnabled || mode <= 0 || client == null || !client.isConfigured()) {
            cb.onHint(local, false);
            return;
        }

        String system = "أنت مدرب صبور لشخصية كفيفة في لعبة حياة يومية. أعطِ تلميحًا قصيرًا واضحًا "
                + "(جملة أو جملتين) باستخدام اتجاهات نسبية فقط (يمين/يسار/أمام/خلف). "
                + "لا تكشف الحل كاملًا. لا تذكر إحداثيات. اكتب بالعربية الفصحى المبسطة فقط.";

        StringBuilder u = new StringBuilder();
        u.append("المكان: ").append(gs.scene != null ? gs.scene.name : "?").append(". ");
        u.append("اليوم رقم ").append(gs.day).append(". ");
        u.append("حالة اللاعب — حركة ").append(gs.player.mobility)
                .append("، علاقات ").append(gs.player.social)
                .append("، تقنية ").append(gs.player.tech)
                .append("، ثقة ").append(gs.player.confidence).append(". ");
        if (mission != null) {
            u.append("المهمة الحالية: ").append(mission.title)
                    .append(" — ").append(mission.description).append(". ");
        }
        u.append("التلميح المحلي للمرجع: ").append(local).append(". ");
        u.append("أعطِ تلميحًا أكثر تخصيصًا بنفس المعنى.");

        client.ask(system, u.toString(), new AiClient.Callback() {
            @Override public void onReply(String text) {
                String safe = safety.filter(text);
                if (safe != null && !safe.isEmpty()) cb.onHint(safe, true);
                else cb.onHint(local, false);
            }
            @Override public void onError(String msg) { cb.onHint(local, false); }
        });
    }
}
