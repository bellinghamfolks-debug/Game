package com.hayatkafeef.game.achievements;

import java.util.ArrayList;
import java.util.List;

/** Hand-authored list of all unlockable achievements. */
public final class Achievements {

    private Achievements() {}

    public static final String FIRST_WALK     = "first_walk";
    public static final String FIRST_HINT     = "first_hint";
    public static final String FIRST_NAV      = "first_nav";
    public static final String LISTENER       = "listener";
    public static final String POLITE         = "polite";
    public static final String SOCIAL         = "social";
    public static final String SURVIVOR       = "survivor";
    public static final String IRON_WILL      = "iron_will";
    public static final String BOOK_LOVER     = "book_lover";
    public static final String EXPLORER       = "explorer";
    public static final String DAY_ONE        = "day_one";
    public static final String WEEK_DONE      = "week_done";
    public static final String AI_USER        = "ai_user";
    public static final String LIBRARY_FRIEND = "library_friend";
    public static final String IRON_PLAYER    = "iron_player";
    public static final String NAV_MASTER     = "nav_master";

    public static List<Achievement> defaults() {
        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement(FIRST_WALK,     "أول خطوة",            "مشيت لأول مرة في عالم اللعبة."));
        list.add(new Achievement(FIRST_HINT,     "طلبت تلميحًا",         "ضغطت زر التلميح للمرة الأولى."));
        list.add(new Achievement(FIRST_NAV,      "أول إرشاد",            "استخدمت المرشد الصوتي لأول مرة."));
        list.add(new Achievement(LISTENER,       "آذان صاغية",          "استخدمت وصف البيئة عشر مرات."));
        list.add(new Achievement(POLITE,         "مؤدَّب",                "قلت ‘شكرًا’ خمس مرات في الحوارات."));
        list.add(new Achievement(SOCIAL,         "اجتماعي",              "تكلمت مع عشر شخصيات مختلفة."));
        list.add(new Achievement(SURVIVOR,       "ناجٍ",                  "تجنّبت خمسة مخاطر."));
        list.add(new Achievement(IRON_WILL,      "إرادة من حديد",        "تجنّبت عشرة مخاطر متتالية بدون ارتطام."));
        list.add(new Achievement(BOOK_LOVER,     "محبّ القراءة",         "زرت المكتبة لأول مرة."));
        list.add(new Achievement(LIBRARY_FRIEND, "صديق المكتبة",         "تحدثت مع أمين المكتبة."));
        list.add(new Achievement(EXPLORER,       "مستكشف",                "زرت كل الأماكن الخمسة في اللعبة."));
        list.add(new Achievement(DAY_ONE,        "اليوم الأول",          "أكملت كل مهام اليوم الأول."));
        list.add(new Achievement(WEEK_DONE,      "أسبوع كامل",           "أكملت سبعة أيام كاملة."));
        list.add(new Achievement(AI_USER,        "مع الذكاء الاصطناعي",  "ربطت مفتاح Gemini واختبرت الاتصال."));
        list.add(new Achievement(IRON_PLAYER,    "لاعب صلب",             "أكملت يومًا كاملًا على الصعوبة الصعبة."));
        list.add(new Achievement(NAV_MASTER,     "ملاح ماهر",            "استخدمت المرشد الصوتي عشر مرات."));
        return list;
    }
}
