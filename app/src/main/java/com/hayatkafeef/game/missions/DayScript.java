package com.hayatkafeef.game.missions;

import com.hayatkafeef.game.game.Scene;

import java.util.ArrayList;
import java.util.List;

/** Hand-authored day scripts. Day 1 is the only fully-fleshed one for now. */
public final class DayScript {

    private DayScript() {}

    public static MissionManager forDay(int day) {
        switch (day) {
            case 1: default: return day1();
        }
    }

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
        return new MissionManager(m);
    }
}
