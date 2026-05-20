package com.hayatkafeef.game.missions;

import com.hayatkafeef.game.game.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-authored missions for each day of the first story arc.
 * Each day has a clear theme that builds on what came before:
 *   1. tutorial — learn the controls
 *   2. city audio — recognize sounds without sight
 *   3. university life — depth, hierarchy, finding rooms
 *   4. social awkwardness — choices matter
 *   5. exam pressure — efficiency
 *   6. weather + crowd — sensory overload
 *   7. mastery run — combine everything
 */
public final class DayScript {

    private DayScript() {}

    public static MissionManager forDay(int day) {
        switch (day) {
            case 2: return day2();
            case 3: return day3();
            case 4: return day4();
            case 5: return day5();
            case 6: return day6();
            case 7: return day7();
            case 1: default: return day1();
        }
    }

    // ---------------- DAY 1: tutorial ----------------

    public static MissionManager day1() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "stop_alarm", "أوقف المنبه",
                "صوت منبه يأتي من جهة السرير. اقترب وانقر مرتين لإيقافه.",
                "ابدأ بضغطة مطوّلة لتسمع المكان. السرير قريب منك.",
                "bed", 0, 0, 0, 1));
        m.add(new Missions.Interact(
                "find_cane", "ابحث عن عصاك",
                "تذكر أين تركتها أمس. عصا المشي لا تترك البيت بدونها.",
                "العصا قرب المكتب. اضغط زر المرشد الصوتي 🧭 واطلب الذهاب إليها.",
                "cane", 1, 0, 0, 1));
        m.add(new Missions.Interact(
                "open_door", "افتح باب البيت",
                "وقت الخروج. ابحث عن باب البيت.",
                "استخدم 🧭 واختر باب البيت.",
                "door_to_street", 1, 0, 0, 0));
        m.add(new Missions.EnterScene(
                "to_street", "اخرج إلى الشارع",
                "الهواء بارد، والأصوات أكثر. أنت الآن في الشارع.",
                "الباب يفتح للخارج. اعبر العتبة.",
                Scene.Id.STREET));
        m.add(new Missions.Interact(
                "ask_directions", "اسأل أحدًا عن الجامعة",
                "بدل أن تخمّن، اسأل أحد المارة عن البوابة.",
                "اقترب من شخص في الشارع وانقر مرتين.",
                "pedestrian2", 0, 2, 0, 1));
        m.add(new Missions.ReachPoint(
                "cross_street", "اعبر الطريق بأمان",
                "الطريق في منتصف الشارع. اسمع جيدًا قبل العبور.",
                "تحرّك جنوبًا واحذر أصوات المحركات.",
                Scene.Id.STREET, 14f, 11f, 1.6f));
        m.add(new Missions.EnterScene(
                "enter_uni", "ادخل الجامعة",
                "البوابة الشرقية مفتوحة. ادخل قبل بدء المحاضرة.",
                "بوابة الجامعة في أقصى يمين الشارع.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "find_hall", "صل إلى قاعة المحاضرة",
                "المصعد قد يكون معطلًا اليوم. تنبّه للسلالم.",
                "القاعة في منتصف الجامعة شمالًا. اطلب الإرشاد لـ ‘قاعة المحاضرة’.",
                "door_hall", 1, 0, 1, 0));
        m.add(new Missions.Interact(
                "meet_professor", "تحدّث مع الأستاذ",
                "اسأله عن طريقة تقديم المشاريع.",
                "الأستاذ بجوار القاعة. انقر مرتين بعد الاقتراب.",
                "professor", 0, 1, 2, 1));
        m.add(new Missions.EnterScene(
                "to_cafe", "اختم يومك في المقهى",
                "وقت استراحة. اذهب للمقهى وقابل صديقك القديم.",
                "المقهى في وسط الشارع، باب يلتفت يمينًا.",
                Scene.Id.CAFE));
        m.add(new Missions.Interact(
                "sleep_1", "عُد للبيت ونَم",
                "اعتنِ بنفسك. ارجع للبيت واستلق على السرير.",
                "اطلب الإرشاد لـ ‘السرير’ بعد أن تعود.",
                "bed", 0, 0, 0, 1));
        return new MissionManager(m);
    }

    // ---------------- DAY 2: city audio ----------------

    public static MissionManager day2() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "morning_clock", "اعرف الوقت",
                "ساعة الحائط ستخبرك بالساعة. تكتك خفيف يقودك إليها.",
                "تتبع صوت التكتك إلى الجدار الجنوبي الغربي.",
                "clock", 0, 0, 1, 0));
        m.add(new Missions.Interact(
                "find_phone", "ابحث عن هاتفك على المكتب",
                "تركته هناك ليلًا. اقترب من المكتب.",
                "🧭 → المكتب، ثم انقر مرتين.",
                "desk", 0, 0, 2, 0));
        m.add(new Missions.Interact(
                "exit_day2", "اخرج للشارع",
                "اليوم درس في تمييز الأصوات.",
                "باب البيت كما تعرف.",
                "door_to_street", 1, 0, 0, 0));
        m.add(new Missions.EnterScene(
                "scene_street_d2", "أنصت لمدينتك",
                "ركّز على أصوات السيارات، الناس، الماء.",
                "خذ نفسًا واستمع. اضغط مطوّلًا للوصف.",
                Scene.Id.STREET));
        m.add(new Missions.Interact(
                "find_fountain", "حدّد موقع النافورة بالسمع",
                "الماء يتدفق في مكان ما. تتبع الصوت.",
                "صوت ماء هادئ. 🧭 → نافورة.",
                "fountain", 2, 0, 1, 1));
        m.add(new Missions.Interact(
                "find_bus", "اقترب من الحافلة",
                "صوت المحرك مميز. لا تخطئ بها.",
                "🧭 → حافلة. ابقَ في الرصيف.",
                "bus", 1, 0, 0, 1));
        m.add(new Missions.Interact(
                "ask_male", "اسأل رجلًا عن الوقت",
                "تواصل اجتماعي بسيط.",
                "🧭 → رجل، ثم انقر مرتين.",
                "pedestrian1", 0, 2, 0, 1));
        m.add(new Missions.Interact(
                "sit_bench", "خذ استراحة على المقعد",
                "ركّز على ما تسمعه وأنت ساكن.",
                "المقعد قريب من باب بيتك.",
                "bench1", 1, 0, 0, 1));
        m.add(new Missions.EnterScene(
                "home_d2", "ارجع للبيت",
                "كفاك تنزّهًا اليوم.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_2", "نَم",
                "نَم وتجهّز ليوم الجامعة الكامل.",
                "اقترب من السرير وانقر مرتين.",
                "bed", 0, 0, 0, 2));
        return new MissionManager(m);
    }

    // ---------------- DAY 3: university depth ----------------

    public static MissionManager day3() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "morning_d3", "ابدأ يومك",
                "صباح الخير. اليوم لديك محاضرة طويلة.",
                "أوقف المنبه.",
                "bed", 0, 0, 0, 1));
        m.add(new Missions.EnterScene(
                "street_d3", "إلى الشارع",
                "اخرج للجامعة.",
                "🧭 → باب البيت → الشارع.",
                Scene.Id.STREET));
        m.add(new Missions.EnterScene(
                "uni_d3", "ادخل الجامعة",
                "اليوم ستجرّب أعماق المبنى.",
                "🧭 → بوابة الجامعة.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "ask_classmate", "اسأل زميلتك عن القاعة",
                "زميلتك سارة تعرف المكان.",
                "🧭 → زميلة دراسة.",
                "classmate", 0, 2, 0, 1));
        m.add(new Missions.Interact(
                "try_elevator", "جرّب المصعد",
                "ربما يعمل اليوم، ربما لا.",
                "🧭 → مصعد.",
                "elevator", 0, 0, 2, 0));
        m.add(new Missions.Interact(
                "reach_hall_d3", "ادخل قاعة المحاضرة",
                "ركّز على صوت السبورة.",
                "🧭 → قاعة المحاضرة.",
                "door_hall", 2, 0, 1, 1));
        m.add(new Missions.Interact(
                "speak_prof_d3", "اطرح سؤالًا على الأستاذ",
                "اظهر اهتمامك. الأسئلة الجيدة تفتح أبوابًا.",
                "🧭 → الأستاذ.",
                "professor", 0, 1, 3, 2));
        m.add(new Missions.Interact(
                "curious_d3", "تعامل مع الزميل الفضولي",
                "أجبه بهدوء أو حدد له حدودك.",
                "🧭 → زميل فضولي. اختر بحكمة.",
                "curious", 0, 1, 0, 2));
        m.add(new Missions.EnterScene(
                "back_street_d3", "إلى الشارع",
                "انتهت المحاضرة.",
                "🧭 → البوابة الخارجية.",
                Scene.Id.STREET));
        m.add(new Missions.EnterScene(
                "home_d3", "البيت",
                "ارتح. غدًا يوم مختلف.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_3", "نَم", "استرح جيدًا.",
                "اقترب من السرير.",
                "bed", 0, 0, 0, 2));
        return new MissionManager(m);
    }

    // ---------------- DAY 4: social awkwardness ----------------

    public static MissionManager day4() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "wake_d4", "استيقظ",
                "تشعر اليوم بقليل من القلق الاجتماعي.",
                "أوقف المنبه.",
                "bed", 0, 0, 0, 1));
        m.add(new Missions.EnterScene(
                "street_d4", "إلى الشارع",
                "اليوم تختبر تفاعلاتك مع الناس.",
                "خذ نفسًا عميقًا واخرج.",
                Scene.Id.STREET));
        m.add(new Missions.Interact(
                "kind_woman_d4", "تحدّث مع امرأة لطيفة",
                "اشرح لها وضعك بثقة.",
                "🧭 → امرأة.",
                "pedestrian2", 0, 3, 0, 2));
        m.add(new Missions.EnterScene(
                "uni_d4", "ادخل الجامعة",
                "موقف محرج قد ينتظرك هناك.",
                "🧭 → بوابة الجامعة.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "curious_d4", "واجه الزميل الفضولي",
                "اختر بحكمة: تشرح، تمزح، أم تنسحب؟",
                "كل اختيار يؤثر على ثقتك.",
                "curious", 0, 1, 0, 3));
        m.add(new Missions.Interact(
                "support_classmate", "اطلب دعم زميلتك",
                "الصداقة الحقيقية تستحق المطالبة بها.",
                "🧭 → زميلة دراسة.",
                "classmate", 0, 3, 0, 2));
        m.add(new Missions.EnterScene(
                "cafe_d4", "خذ استراحة في المقهى",
                "بعض الهدوء بعد ذلك الموقف.",
                "🧭 → المقهى.",
                Scene.Id.CAFE));
        m.add(new Missions.Interact(
                "vent_friend", "تحدّث مع صديقك القديم",
                "افضفض. الكلام يخفف.",
                "🧭 → صديق قديم.",
                "oldfriend", 0, 2, 0, 3));
        m.add(new Missions.EnterScene(
                "home_d4", "البيت",
                "يوم طويل. ارجع.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_4", "نَم", "يومك انتهى.",
                "السرير.",
                "bed", 0, 0, 0, 2));
        return new MissionManager(m);
    }

    // ---------------- DAY 5: exam pressure ----------------

    public static MissionManager day5() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "wake_d5", "استيقظ — يوم الاختبار",
                "الوقت ضيق. لا تنسَ شيئًا.",
                "أوقف المنبه واخرج بسرعة.",
                "bed", 0, 0, 0, 1));
        m.add(new Missions.Interact(
                "grab_phone_d5", "خذ هاتفك من المكتب",
                "ستحتاج له اليوم.",
                "🧭 → مكتب.",
                "desk", 0, 0, 2, 0));
        m.add(new Missions.Interact(
                "out_fast", "اخرج للشارع",
                "كل دقيقة تحسب.",
                "🧭 → باب البيت.",
                "door_to_street", 1, 0, 0, 1));
        m.add(new Missions.EnterScene(
                "to_uni_fast", "اركض للجامعة",
                "ركّز، لكن لا تتسرع لدرجة الاصطدام.",
                "اعبر الشارع بحذر.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "exam_hall", "ادخل قاعة الاختبار",
                "هذه هي اللحظة.",
                "🧭 → قاعة المحاضرة.",
                "door_hall", 1, 0, 2, 1));
        m.add(new Missions.Interact(
                "results_prof", "تسلّم النتيجة من الأستاذ",
                "مهما كانت، اعرف أنك بذلت جهدك.",
                "🧭 → الأستاذ.",
                "professor", 0, 1, 2, 3));
        m.add(new Missions.EnterScene(
                "decompress_cafe", "هدّئ نفسك في المقهى",
                "تستحق فنجان قهوة.",
                "🧭 → المقهى.",
                Scene.Id.CAFE));
        m.add(new Missions.Interact(
                "waiter_d5", "اطلب طلبك المعتاد",
                "النادل يعرفك. لا داعي لشرح كثير.",
                "🧭 → النادل.",
                "waiter", 0, 1, 0, 1));
        m.add(new Missions.EnterScene(
                "home_d5", "البيت",
                "نَم نوم الأبطال.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_5", "نَم", "اختبار وانتهى.",
                "السرير.",
                "bed", 0, 0, 0, 3));
        return new MissionManager(m);
    }

    // ---------------- DAY 6: hard day / sensory overload ----------------

    public static MissionManager day6() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "wake_d6", "استيقظ — اليوم مزدحم",
                "هناك مطر خفيف وضوضاء أكثر.",
                "أوقف المنبه.",
                "bed", 0, 0, 0, 1));
        m.add(new Missions.Interact(
                "ready_cane_d6", "أمسك عصاك بثبات",
                "اليوم تحتاجها أكثر من أي يوم.",
                "🧭 → عصاك.",
                "cane", 2, 0, 0, 1));
        m.add(new Missions.EnterScene(
                "wet_street", "إلى الشارع المبتلّ",
                "أرض زلقة، أصوات مكتومة، حذار.",
                "خطواتك أبطأ، لكنك أقوى.",
                Scene.Id.STREET));
        m.add(new Missions.ReachPoint(
                "cross_wet", "اعبر الطريق رغم المطر",
                "الصوت لا ينتشر كالمعتاد. ركّز.",
                "اقترب من منتصف الشارع جنوبًا.",
                Scene.Id.STREET, 14f, 11f, 1.8f));
        m.add(new Missions.EnterScene(
                "uni_d6", "ادخل الجامعة",
                "بعض الجفاف، أخيرًا.",
                "🧭 → بوابة الجامعة.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "support_friend_d6", "اسأل زميلتك إن وصلت بأمان",
                "الكل يعاني اليوم. كن صديقًا.",
                "🧭 → زميلة دراسة.",
                "classmate", 0, 3, 0, 1));
        m.add(new Missions.Interact(
                "patience_prof", "تعامل مع الأستاذ بصبر",
                "هو متوتر أيضًا.",
                "🧭 → الأستاذ.",
                "professor", 0, 2, 1, 2));
        m.add(new Missions.EnterScene(
                "warm_cafe", "ادفأ في المقهى",
                "رائحة القهوة دافئة في مثل هذا اليوم.",
                "🧭 → المقهى.",
                Scene.Id.CAFE));
        m.add(new Missions.Interact(
                "warm_friend", "تحدّث مع صديقك القديم",
                "الأصدقاء في الأيام الصعبة.",
                "🧭 → صديق قديم.",
                "oldfriend", 0, 3, 0, 2));
        m.add(new Missions.EnterScene(
                "home_d6", "البيت أخيرًا",
                "تستحق راحة كاملة.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_6", "نَم", "غدًا اليوم الأخير من الأسبوع.",
                "السرير.",
                "bed", 0, 0, 0, 3));
        return new MissionManager(m);
    }

    // ---------------- DAY 7: mastery run ----------------

    public static MissionManager day7() {
        List<Mission> m = new ArrayList<>();
        m.add(new Missions.Interact(
                "wake_final", "صباح اليوم الأخير",
                "تجاوزت أسبوعًا كاملًا. اليوم تجمع كل ما تعلمت.",
                "أوقف المنبه.",
                "bed", 0, 0, 0, 2));
        m.add(new Missions.Interact(
                "cane_final", "خذ عصاك",
                "صديقتك المخلصة.",
                "🧭 → عصاك.",
                "cane", 2, 0, 0, 1));
        m.add(new Missions.Interact(
                "exit_final", "اخرج بثقة",
                "الباب يفتح على عالم تعرفه الآن.",
                "🧭 → باب البيت.",
                "door_to_street", 1, 0, 0, 2));
        m.add(new Missions.Interact(
                "fountain_final", "زر النافورة كذكرى",
                "في يوم ما، كنت لا تستطيع تمييز اتجاهها.",
                "🧭 → نافورة.",
                "fountain", 1, 0, 0, 2));
        m.add(new Missions.Interact(
                "thank_helper", "اشكر شخصًا ساعدك",
                "الامتنان جزء من النضج.",
                "🧭 → امرأة (أو رجل).",
                "pedestrian2", 0, 3, 0, 2));
        m.add(new Missions.ReachPoint(
                "cross_final", "اعبر الطريق بثبات",
                "اعتدت على الأصوات.",
                "وسط الشارع جنوبًا.",
                Scene.Id.STREET, 14f, 11f, 1.6f));
        m.add(new Missions.EnterScene(
                "uni_final", "إلى الجامعة",
                "آخر محاضرة في الأسبوع.",
                "🧭 → بوابة الجامعة.",
                Scene.Id.UNIVERSITY));
        m.add(new Missions.Interact(
                "thank_classmate", "اشكر زميلتك",
                "ساعدتك كثيرًا هذا الأسبوع.",
                "🧭 → زميلة دراسة.",
                "classmate", 0, 4, 0, 2));
        m.add(new Missions.Interact(
                "final_question", "اطرح سؤالًا أخيرًا على الأستاذ",
                "اترك انطباعًا قويًا.",
                "🧭 → الأستاذ.",
                "professor", 0, 2, 3, 3));
        m.add(new Missions.EnterScene(
                "celebration_cafe", "احتفل في المقهى",
                "أسبوع كامل، وأنت أقوى مما بدأت.",
                "🧭 → المقهى.",
                Scene.Id.CAFE));
        m.add(new Missions.Interact(
                "toast_friend", "ارفع كأس قهوة مع صديقك",
                "هذا ما يُسمى نضجًا.",
                "🧭 → صديق قديم.",
                "oldfriend", 0, 4, 0, 4));
        m.add(new Missions.EnterScene(
                "home_final", "البيت أخيرًا",
                "اليوم الأخير من الأسبوع الأول.",
                "🧭 → باب بيتك.",
                Scene.Id.HOME));
        m.add(new Missions.Interact(
                "sleep_final", "نَم بفخر",
                "أنت بطل قصتك. النهاية… أم البداية؟",
                "السرير.",
                "bed", 2, 2, 2, 5));
        return new MissionManager(m);
    }
}
