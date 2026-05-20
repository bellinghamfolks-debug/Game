package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.DialogueSystem;
import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.GameState;

/**
 * Conversation enrichment + relationship memory.
 *
 * - recordChoice() updates the CharacterMemory for the NPC based on
 *   the player's selected dialogue option (kindness deltas).
 * - enrichReply() returns the canonical reply immediately via the
 *   callback when AI is off; when AI is on, calls Gemini with the
 *   relationship context and returns the enriched text in the same
 *   callback.
 */
public class AiDialogueManager {

    public interface Callback {
        void onReply(String text, boolean fromAi);
    }

    private final AiClient client;
    private final AiSafetyFilter safety;
    private final CharacterMemory memory;

    public AiDialogueManager(AiClient client, AiSafetyFilter safety, CharacterMemory memory) {
        this.client = client;
        this.safety = safety;
        this.memory = memory;
    }

    public CharacterMemory memory() { return memory; }

    /** Updates relationship memory based on the choice's social delta. */
    public void recordChoice(Entity npc, DialogueSystem.Choice choice) {
        if (npc == null || npc.id == null || choice == null) return;
        memory.recordInteraction(npc.id);
        if (choice.dSocial > 0) memory.recordKindness(npc.id, +1);
        else if (choice.dSocial < 0) memory.recordKindness(npc.id, -1);
        // accept/refuse-help heuristic from the choice text
        String t = choice.text == null ? "" : choice.text;
        if (t.contains("نعم") || t.contains("شكرًا") || t.contains("أرجو")) memory.recordHelp(npc.id, true);
        else if (t.contains("أعرف") || t.contains("بنفسي") || t.contains("لا")) memory.recordHelp(npc.id, false);
    }

    public void enrichReply(Entity npc, DialogueSystem.Choice choice, GameState gs,
                            String originalReply, boolean aiEnabled, int mode, Callback cb) {
        if (!aiEnabled || mode <= 0 || client == null || !client.isConfigured()) {
            cb.onReply(originalReply, false);
            return;
        }
        String system = "أنت كاتب حوارات للعبة عن شخصية كفيفة. أعِد كتابة رد الشخصية بأسلوب طبيعي "
                + "ودافئ بالعربية، جملتين كحد أقصى، بدون تغيير المعنى أو إضافة أحداث جديدة.";
        StringBuilder u = new StringBuilder();
        u.append("الشخصية: ").append(npc != null ? npc.name : "غريب").append("\n");
        if (npc != null && npc.id != null) {
            u.append("ذاكرة العلاقة معها: ").append(memory.describeRelationship(npc.id)).append("\n");
        }
        if (mode >= 2 && gs != null) {
            u.append("أسلوب اللاعب — ثقة ").append(gs.player.confidence)
                    .append("، علاقات ").append(gs.player.social).append("\n");
        }
        if (choice != null) u.append("اختار اللاعب: ").append(choice.text).append("\n");
        u.append("الرد الأصلي: ").append(originalReply).append("\n");
        u.append("أعِد كتابة الرد فقط.");

        client.ask(system, u.toString(), new AiClient.Callback() {
            @Override public void onReply(String text) {
                String safe = safety.filter(text);
                cb.onReply(safe != null && !safe.isEmpty() ? safe : originalReply, safe != null);
            }
            @Override public void onError(String msg) { cb.onReply(originalReply, false); }
        });
    }
}
