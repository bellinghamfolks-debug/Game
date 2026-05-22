using System.Collections.Generic;
using UnityEngine;

namespace BlindLife
{
    /// <summary>
    /// Minimal in-world dialogue. For now each NPC has a one-line opener;
    /// future versions can plug in branching responses and AI enrichment
    /// just like the Canvas-era DialogueSystem.
    /// </summary>
    public static class DialogueSystem
    {
        static readonly Dictionary<string, string> Openers = new Dictionary<string, string>
        {
            { "classmate",   "أهلًا. هل تريد ملخص ما فاتك في المحاضرة؟" },
            { "professor",   "تفضل. عندك سؤال؟" },
            { "waiter",      "أهلًا. عربية كالعادة أم تجرب جديدًا اليوم؟" },
            { "librarian",   "أهلًا. لدي كتب صوتية ونسخ برايل. ماذا تحتاج؟" },
            { "pedestrian1", "ممكن أساعدك تعبر؟" },
            { "pedestrian2", "السلام عليكم، تحتاج اتجاهًا؟" },
            { "oldfriend",   "أهلًا يا صديق، كيف الحال اليوم؟" },
        };

        public static void StartConversation(Interactable npc)
        {
            if (npc == null) return;
            string opener = Openers.TryGetValue(npc.id, out var s)
                ? s
                : ((npc.displayName ?? "غريب") + " يبتسم لك ويقول السلام عليكم.");

            var gm = GameManager.Instance;
            if (gm != null && gm.Dialogues != null && gm.AiEnabled)
            {
                gm.Dialogues.EnrichReply(npc.id, npc.displayName, opener, true, (text, fromAi) =>
                {
                    AccessibilityManager.Instance?.SpeakNow((npc.displayName ?? "") + ". " + text);
                });
            }
            else
            {
                AccessibilityManager.Instance?.SpeakNow((npc.displayName ?? "") + ". " + opener);
            }
            AccessibilityManager.Instance?.Confirm();
        }
    }
}
