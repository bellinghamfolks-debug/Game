using System.Collections.Generic;
using System.Text;
using BlindLife.Missions;
using UnityEngine;

namespace BlindLife.AI
{
    /// <summary>
    /// Hint / summary / dialogue managers — each takes an AiClient and
    /// falls back to OfflineContent on failure or when AI is disabled.
    /// </summary>
    public class AiHintManager
    {
        readonly AiClient _ai;
        public AiHintManager(AiClient ai) { _ai = ai; }

        public void Ask(GameState gs, Mission mission, bool aiEnabled,
            System.Action<string, bool> done)
        {
            string local = OfflineContent.HintFor(mission);
            if (!aiEnabled || _ai == null || !_ai.IsConfigured)
            {
                done?.Invoke(local, false);
                return;
            }
            string system = "أنت مدرّب صبور لشخصية كفيفة. أعطِ تلميحًا قصيرًا واضحًا (جملة أو جملتين) "
                + "باتجاهات نسبية فقط (يمين/يسار/أمام/خلف). لا تكشف الحل كاملًا. بالعربية فقط.";
            var sb = new StringBuilder();
            sb.Append("اليوم ").Append(gs.day).Append(". ");
            sb.Append("حركة ").Append(gs.mobility).Append("، علاقات ").Append(gs.social)
              .Append("، تقنية ").Append(gs.tech).Append("، ثقة ").Append(gs.confidence).Append(". ");
            if (mission != null)
            {
                sb.Append("المهمة: ").Append(mission.title).Append(" — ")
                  .Append(mission.description).Append(". ");
            }
            sb.Append("التلميح المحلي: ").Append(local).Append(". أعطِ تلميحًا أفضل.");
            _ai.Ask(system, sb.ToString(),
                reply => done?.Invoke(AiSafetyFilter.Filter(reply) ?? local, true),
                err   => done?.Invoke(local, false));
        }
    }

    public class AiSummaryManager
    {
        readonly AiClient _ai;
        public AiSummaryManager(AiClient ai) { _ai = ai; }

        public void DaySummary(GameState gs, List<string> recentEvents, bool aiEnabled,
            System.Action<string, bool> done)
        {
            string local = OfflineContent.Summarize(gs, recentEvents);
            if (!aiEnabled || _ai == null || !_ai.IsConfigured)
            {
                done?.Invoke(local, false);
                return;
            }
            string system = "أنت راوي يلخّص يوم لاعب كفيف بفقرة دافئة (3 إلى 5 جمل) بالعربية. "
                + "اختم بنصيحة شخصية لليوم القادم.";
            var sb = new StringBuilder();
            sb.Append("اليوم ").Append(gs.day).Append(". ");
            sb.Append("المهارات: حركة ").Append(gs.mobility)
              .Append("، علاقات ").Append(gs.social)
              .Append("، تقنية ").Append(gs.tech)
              .Append("، ثقة ").Append(gs.confidence).Append(".\n");
            sb.Append("الأحداث:\n");
            if (recentEvents != null)
                foreach (var e in recentEvents) sb.Append("- ").Append(e).Append('\n');
            _ai.Ask(system, sb.ToString(),
                reply => done?.Invoke(AiSafetyFilter.Filter(reply) ?? local, true),
                err   => done?.Invoke(local, false));
        }
    }

    public class AiDialogueManager
    {
        readonly AiClient _ai;
        readonly CharacterMemory _memory;

        public CharacterMemory Memory => _memory;

        public AiDialogueManager(AiClient ai, CharacterMemory memory)
        {
            _ai = ai;
            _memory = memory;
        }

        public void EnrichReply(string npcId, string npcName, string originalReply,
            bool aiEnabled, System.Action<string, bool> done)
        {
            if (!aiEnabled || _ai == null || !_ai.IsConfigured)
            {
                done?.Invoke(originalReply, false);
                return;
            }
            string system = "أنت كاتب حوارات للعبة عن شخصية كفيفة. أعِد كتابة رد الشخصية بأسلوب طبيعي "
                + "دافئ، جملتين كحد أقصى، بدون تغيير المعنى.";
            var sb = new StringBuilder();
            sb.Append("الشخصية: ").Append(npcName ?? "غريب").Append("\n");
            if (!string.IsNullOrEmpty(npcId))
                sb.Append("ذاكرة العلاقة: ").Append(_memory.DescribeRelationship(npcId)).Append("\n");
            sb.Append("الرد الأصلي: ").Append(originalReply).Append("\n");
            sb.Append("أعِد كتابة الرد فقط.");
            _ai.Ask(system, sb.ToString(),
                reply => done?.Invoke(AiSafetyFilter.Filter(reply) ?? originalReply, true),
                err   => done?.Invoke(originalReply, false));
        }
    }
}
