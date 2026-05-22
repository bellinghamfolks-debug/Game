using System.Collections.Generic;
using UnityEngine;

namespace BlindLife.Missions
{
    /// <summary>
    /// Hand-authored day-by-day mission scripts. Day 1 = tutorial,
    /// 2 = audio, 3 = university, 4 = social, 5 = exam pressure,
    /// 6 = rain + crowd, 7 = mastery run. Coordinates map to the
    /// procedural buildings in GameBootstrap (which all sit within
    /// a 60×60 world centered at the origin).
    /// </summary>
    public static class DayScript
    {
        // Building anchor positions matching GameBootstrap. Update both
        // files together if you move buildings.
        public static readonly Vector3 HomePos       = new Vector3(-15, 0, -15);
        public static readonly Vector3 UniversityPos = new Vector3( 18, 0, -16);
        public static readonly Vector3 CafePos       = new Vector3(-16, 0,  16);
        public static readonly Vector3 LibraryPos    = new Vector3( 18, 0,  18);

        public static MissionManager ForDay(int day)
        {
            switch (day)
            {
                case 2: return Day2();
                case 3: return Day3();
                case 4: return Day4();
                case 5: return Day5();
                case 6: return Day6();
                case 7: return Day7();
                default: return Day1();
            }
        }

        public static MissionManager Day1()
        {
            var m = new List<Mission>
            {
                new InteractMission("door_home",  "افتح باب البيت",
                    "ابدأ يومك بالخروج. باب البيت في الجهة الجنوبية من بيتك.",
                    "ابحث عن الباب في الجدار الجنوبي للبيت. اقترب وانقر مرتين.",
                    "Door_Home", 1, 0, 0, 1),

                new ReachPointMission("near_cafe", "اقترب من المقهى",
                    "تنزّه بهدوء، وتعرّف على شارعك.",
                    "المقهى في الزاوية الشمالية الغربية من العالم.",
                    CafePos, 4f),

                new InteractMission("ask_directions", "اسأل أحدًا عن الجامعة",
                    "اقترب من أحد المارة وتحدث معه.",
                    "ابحث عن شخص في الشارع وانقر مرتين عليه.",
                    "pedestrian2", 0, 2, 0, 1),

                new ReachPointMission("near_uni", "اقترب من بوابة الجامعة",
                    "اتجه إلى المبنى الكبير في الشرق.",
                    "🧭 اطلب الإرشاد إلى الجامعة.",
                    UniversityPos, 5f),

                new InteractMission("door_uni", "ادخل الجامعة",
                    "البوابة في الجهة الجنوبية من المبنى.",
                    "اقترب من الباب وانقر مرتين.",
                    "Door_University", 1, 0, 1, 1),

                new InteractMission("meet_professor", "تحدّث مع الأستاذ",
                    "اسأل عن المحاضرة القادمة.",
                    "الأستاذ بالقرب من بوابة الجامعة.",
                    "professor", 0, 1, 2, 1),

                new InteractMission("to_cafe", "اختم يومك في المقهى",
                    "تستحق فنجان قهوة.",
                    "ادخل باب المقهى.",
                    "Door_Cafe", 0, 1, 0, 1)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day2()
        {
            var m = new List<Mission>
            {
                new InteractMission("exit2", "اخرج للشارع", "اليوم درس في الأصوات.",
                    "باب البيت.", "Door_Home", 1, 0, 0, 0),
                new InteractMission("listen_bus", "اقترب من الحافلة",
                    "تتبع صوت محرك ثقيل في الشارع.",
                    "صوت محرك مميز يدلك على الحافلة.",
                    "Bus", 1, 0, 0, 1),
                new InteractMission("ask_male", "اسأل رجلًا عن الوقت",
                    "تواصل اجتماعي بسيط.",
                    "اقترب من أحد المارة.",
                    "pedestrian1", 0, 2, 0, 1),
                new ReachPointMission("walk_around", "تجوّل في وسط الشارع",
                    "اعرف معالم المكان.",
                    "اتجه نحو منتصف العالم.",
                    Vector3.zero, 4f),
                new InteractMission("home2", "ارجع للبيت",
                    "كفاك تنزّهًا اليوم.",
                    "باب البيت.",
                    "Door_Home", 0, 0, 0, 2)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day3()
        {
            var m = new List<Mission>
            {
                new InteractMission("door_uni_3", "إلى الجامعة",
                    "محاضرة طويلة اليوم.",
                    "🧭 → بوابة الجامعة.",
                    "Door_University", 1, 0, 0, 1),
                new InteractMission("classmate3", "اسأل زميلتك عن القاعة",
                    "زميلتك تعرف المبنى.",
                    "🧭 → زميلة دراسة.",
                    "classmate", 0, 2, 0, 1),
                new InteractMission("prof3", "تحدّث مع الأستاذ",
                    "اطرح سؤالًا ذكيًا.",
                    "🧭 → الأستاذ.",
                    "professor", 0, 1, 3, 2),
                new InteractMission("library3", "ادخل المكتبة",
                    "هدوء وكتب صوتية.",
                    "🧭 → باب المكتبة.",
                    "Door_Library", 1, 0, 1, 1),
                new InteractMission("librarian3", "تحدّث مع أمين المكتبة",
                    "اطلب كتابًا صوتيًا.",
                    "🧭 → أمين المكتبة.",
                    "librarian", 0, 1, 2, 1),
                new InteractMission("home3", "ارجع للبيت",
                    "غدًا يوم اجتماعي.",
                    "🧭 → باب البيت.",
                    "Door_Home", 0, 0, 0, 1)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day4()
        {
            var m = new List<Mission>
            {
                new InteractMission("exit4", "اخرج للشارع",
                    "اليوم تختبر تفاعلاتك مع الناس.",
                    "خذ نفسًا واخرج.",
                    "Door_Home", 1, 0, 0, 1),
                new InteractMission("kind_woman4", "تحدّث مع امرأة لطيفة",
                    "اشرح لها وضعك بثقة.",
                    "🧭 → امرأة.",
                    "pedestrian2", 0, 3, 0, 2),
                new InteractMission("uni4", "ادخل الجامعة",
                    "موقف اجتماعي قد ينتظرك.",
                    "🧭 → بوابة الجامعة.",
                    "Door_University", 0, 0, 0, 1),
                new InteractMission("support4", "اطلب دعم زميلتك",
                    "الصداقة الحقيقية تستحق الطلب.",
                    "🧭 → زميلة دراسة.",
                    "classmate", 0, 3, 0, 2),
                new InteractMission("vent_friend4", "تحدّث مع صديقك القديم",
                    "افضفض، الكلام يخفف.",
                    "🧭 → باب المقهى ثم الصديق القديم.",
                    "oldfriend", 0, 2, 0, 3),
                new InteractMission("home4", "البيت",
                    "يوم طويل. ارجع.",
                    "🧭 → باب البيت.",
                    "Door_Home", 0, 0, 0, 2)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day5()
        {
            var m = new List<Mission>
            {
                new InteractMission("rush5", "اخرج بسرعة",
                    "يوم الاختبار. الوقت ضيق.",
                    "🧭 → باب البيت.",
                    "Door_Home", 1, 0, 0, 1),
                new InteractMission("exam_uni5", "اركض للجامعة",
                    "لا تتأخر.",
                    "🧭 → بوابة الجامعة.",
                    "Door_University", 1, 0, 2, 1),
                new InteractMission("prof5", "تسلّم النتيجة من الأستاذ",
                    "مهما كانت، اعرف أنك بذلت جهدك.",
                    "🧭 → الأستاذ.",
                    "professor", 0, 1, 2, 3),
                new InteractMission("cafe5", "هدّئ نفسك في المقهى",
                    "تستحق قهوة هادئة.",
                    "🧭 → باب المقهى.",
                    "Door_Cafe", 0, 1, 0, 1),
                new InteractMission("waiter5", "اطلب طلبك المعتاد",
                    "النادل يعرفك.",
                    "🧭 → النادل.",
                    "waiter", 0, 1, 0, 1),
                new InteractMission("home5", "البيت",
                    "نوم الأبطال.",
                    "🧭 → باب البيت.",
                    "Door_Home", 0, 0, 0, 3)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day6()
        {
            var m = new List<Mission>
            {
                new InteractMission("rain_exit6", "اخرج للشارع المبتل",
                    "أرض زلقة وأصوات مكتومة.",
                    "خطواتك أبطأ اليوم.",
                    "Door_Home", 2, 0, 0, 1),
                new ReachPointMission("center6", "اعبر إلى الوسط",
                    "المطر يخفي الأصوات. ركّز.",
                    "اتجه ببطء نحو منتصف الشارع.",
                    Vector3.zero, 3f),
                new InteractMission("uni6", "ادخل الجامعة",
                    "بعض الجفاف، أخيرًا.",
                    "🧭 → بوابة الجامعة.",
                    "Door_University", 0, 0, 0, 1),
                new InteractMission("friend_uni6", "اسأل زميلتك إن وصلت بأمان",
                    "كن صديقًا في الأيام الصعبة.",
                    "🧭 → زميلة دراسة.",
                    "classmate", 0, 3, 0, 1),
                new InteractMission("warm_cafe6", "ادفأ في المقهى",
                    "رائحة قهوة دافئة في يوم مطير.",
                    "🧭 → باب المقهى.",
                    "Door_Cafe", 0, 0, 0, 1),
                new InteractMission("home6", "البيت",
                    "نوم بطّاني دافئ.",
                    "🧭 → باب البيت.",
                    "Door_Home", 0, 0, 0, 3)
            };
            return new MissionManager(m);
        }

        public static MissionManager Day7()
        {
            var m = new List<Mission>
            {
                new InteractMission("exit_final", "اخرج بثقة",
                    "اليوم الأخير من الأسبوع.",
                    "🧭 → باب البيت.",
                    "Door_Home", 1, 0, 0, 2),
                new InteractMission("thank_helper", "اشكر شخصًا ساعدك",
                    "الامتنان جزء من النضج.",
                    "🧭 → أحد المارة.",
                    "pedestrian2", 0, 3, 0, 2),
                new InteractMission("uni_final", "إلى الجامعة",
                    "آخر محاضرة في الأسبوع.",
                    "🧭 → الجامعة.",
                    "Door_University", 1, 0, 0, 1),
                new InteractMission("thank_classmate", "اشكر زميلتك",
                    "ساعدتك كثيرًا هذا الأسبوع.",
                    "🧭 → زميلة دراسة.",
                    "classmate", 0, 4, 0, 2),
                new InteractMission("library_final", "زر المكتبة",
                    "أمين المكتبة سيفتقدك.",
                    "🧭 → باب المكتبة.",
                    "Door_Library", 0, 1, 1, 1),
                new InteractMission("toast_friend", "ارفع كأس قهوة مع صديقك",
                    "نضج كامل.",
                    "🧭 → باب المقهى ثم الصديق.",
                    "oldfriend", 0, 4, 0, 4),
                new InteractMission("home_final", "نَم بفخر",
                    "أنت بطل قصتك.",
                    "🧭 → باب البيت.",
                    "Door_Home", 2, 2, 2, 5)
            };
            return new MissionManager(m);
        }
    }
}
