using System.Collections.Generic;
using System.Text;
using BlindLife.Missions;

namespace BlindLife.AI
{
    /// <summary>
    /// Hand-authored Arabic strings used whenever AI is off or fails.
    /// Every AI manager funnels through here as a fallback.
    /// </summary>
    public static class OfflineContent
    {
        public static string MissionStart(Mission m)
        {
            if (m == null) return "";
            return "مهمة جديدة: " + m.title + ". " + (m.description ?? "");
        }

        public static string MissionDone(Mission m)
        {
            if (m == null) return "";
            string[] praise = { "أحسنت! أتممت: ", "ممتاز! أنجزت: ", "تمام! تم: ", "رائع! أنهيت: ", "خطوة عظيمة! " };
            return praise[UnityEngine.Random.Range(0, praise.Length)] + m.title + ".";
        }

        public static string DayIntro(int day) => day switch
        {
            1 => "صباح اليوم الأول. خذ وقتك للتعرف على العالم.",
            2 => "صباح اليوم الثاني. اليوم درس في الأصوات.",
            3 => "صباح اليوم الثالث. الجامعة تنتظرك بعمق أكبر.",
            4 => "صباح اليوم الرابع. ستلتقي بمواقف قد تكون محرجة.",
            5 => "صباح اليوم الخامس. اليوم اختبار. تنفّس بعمق.",
            6 => "صباح اليوم السادس. الجو ممطر والشارع مزدحم.",
            7 => "صباح اليوم الأخير. اجعله يومًا تفتخر به.",
            _ => "صباح يوم جديد."
        };

        public static string DayComplete(int day)
        {
            if (day >= 7) return "أكملت الأسبوع الأول كاملًا. مبروك! يمكنك التجوّل بحرية الآن.";
            return "انتهت مهام اليوم " + day + ". افتح القائمة لتسمع ملخصًا، ثم نم استعدادًا لليوم القادم.";
        }

        public static string HintFor(Mission m)
        {
            if (m == null) return "تجوّل قليلًا. اضغط مطوّلًا لتسمع الوصف، أو اضغط 🧭 لاختيار وجهة.";
            var sb = new StringBuilder();
            sb.Append("المهمة: ").Append(m.title).Append(". ");
            sb.Append(string.IsNullOrEmpty(m.hint) ? "اضغط مطوّلًا للوصف ثم اقترب وانقر مرتين." : m.hint);
            return sb.ToString();
        }

        public static string Summarize(GameState gs, List<string> recentEvents)
        {
            var sb = new StringBuilder();
            sb.Append("ملخص اليوم ").Append(gs.day).Append(": ");
            sb.Append("الحركة ").Append(gs.mobility).Append("، ");
            sb.Append("العلاقات ").Append(gs.social).Append("، ");
            sb.Append("التقنية ").Append(gs.tech).Append("، ");
            sb.Append("الثقة ").Append(gs.confidence).Append(". ");
            if (recentEvents != null && recentEvents.Count > 0)
                sb.Append("آخر ما حدث: ").Append(recentEvents[recentEvents.Count - 1]).Append(". ");
            sb.Append("نصيحة: ").Append(AdviceFor(gs));
            return sb.ToString();
        }

        static string AdviceFor(GameState gs)
        {
            int min = gs.mobility, idx = 0;
            if (gs.social < min)     { min = gs.social;     idx = 1; }
            if (gs.tech < min)       { min = gs.tech;       idx = 2; }
            if (gs.confidence < min) { idx = 3; }
            return idx switch
            {
                0 => "ركّز غدًا على التنقل.",
                1 => "تحدّث مع الناس أكثر.",
                2 => "جرّب التقنية أكثر.",
                _ => "ثقتك تبنى بقرارات صغيرة."
            };
        }
    }
}
