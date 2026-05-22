using System.Collections.Generic;
using UnityEngine;

namespace BlindLife.Achievements
{
    public class Achievement
    {
        public readonly string id;
        public readonly string title;
        public readonly string description;
        public bool unlocked;

        public Achievement(string id, string title, string description)
        {
            this.id = id; this.title = title; this.description = description;
        }
    }

    public static class Achievements
    {
        public const string FirstWalk     = "first_walk";
        public const string FirstHint     = "first_hint";
        public const string FirstNav      = "first_nav";
        public const string Listener      = "listener";
        public const string Survivor      = "survivor";
        public const string IronWill      = "iron_will";
        public const string DayOne        = "day_one";
        public const string WeekDone      = "week_done";
        public const string AiUser        = "ai_user";
        public const string Social        = "social";
        public const string BookLover     = "book_lover";

        public static List<Achievement> Defaults() => new List<Achievement>
        {
            new Achievement(FirstWalk, "أول خطوة",      "مشيت لأول مرة."),
            new Achievement(FirstHint, "طلبت تلميحًا",   "ضغطت زر التلميح للمرة الأولى."),
            new Achievement(FirstNav,  "أول إرشاد",     "استخدمت المرشد الصوتي لأول مرة."),
            new Achievement(Listener,  "آذان صاغية",    "وصفت البيئة عشر مرات."),
            new Achievement(Survivor,  "ناجٍ",           "تجنّبت خمسة مخاطر."),
            new Achievement(IronWill,  "إرادة من حديد", "تجنّبت عشرة مخاطر متتالية."),
            new Achievement(DayOne,    "اليوم الأول",  "أكملت اليوم الأول."),
            new Achievement(WeekDone,  "أسبوع كامل",  "أكملت سبعة أيام كاملة."),
            new Achievement(AiUser,    "مع الذكاء",   "فعّلت مفتاح Gemini."),
            new Achievement(Social,    "اجتماعي",      "تكلمت مع خمس شخصيات مختلفة."),
            new Achievement(BookLover, "محبّ القراءة", "زرت المكتبة وتحدثت مع أمين المكتبة.")
        };
    }
}
