package com.hayatkafeef.game.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hand-written branching dialogue. Each method returns a small
 * conversation tree as a flat list of (line, effect) tuples that the
 * GameActivity can present as text + choices.
 */
public class DialogueSystem {

    public static class Line {
        public final String text;
        public final boolean fromPlayer;
        public Line(String text, boolean fromPlayer) { this.text = text; this.fromPlayer = fromPlayer; }
    }

    public static class Choice {
        public final String text;
        public final int dMobility, dSocial, dTech, dConfidence;
        public final String flag;
        public final String reply;
        public Choice(String text, String reply, int dM, int dS, int dT, int dC, String flag) {
            this.text = text; this.reply = reply;
            this.dMobility = dM; this.dSocial = dS; this.dTech = dT; this.dConfidence = dC;
            this.flag = flag;
        }
    }

    public static class Convo {
        public final String npc;
        public final String opener;
        public final List<Choice> choices = new ArrayList<>();
        public Convo(String npc, String opener) { this.npc = npc; this.opener = opener; }
    }

    private static final Random RNG = new Random();

    /** Pick a relevant conversation for a given NPC id. */
    public static Convo forEntity(Entity e, GameState gs) {
        if (e == null) return null;
        String id = e.id == null ? "" : e.id;
        switch (id) {
            case "classmate": return classmate(gs);
            case "professor": return professor(gs);
            case "curious": return curious(gs);
            case "pedestrian1": return pedestrianMale(gs);
            case "pedestrian2": return pedestrianFemale(gs);
            case "waiter": return waiter(gs);
            case "oldfriend": return oldFriend(gs);
            case "librarian": return librarian(gs);
            case "library_student": return libraryStudent(gs);
        }
        // generic friendly NPC
        Convo c = new Convo(e.name, e.name + " يبتسم لك ويقول: السلام عليكم.");
        c.choices.add(new Choice("ردّ التحية", "ردّ بأدب وأخذتما تتبادلان نظرات قصيرة.", 0, 1, 0, 1, null));
        c.choices.add(new Choice("اطلب وصف المكان", "وصف لك " + e.name + " المكان من حوله بإيجاز.", 0, 0, 0, 1, null));
        c.choices.add(new Choice("اعتذر وانصرف", "أومأ برأسه واستمر في طريقه.", 0, 0, 0, 0, null));
        return c;
    }

    private static Convo classmate(GameState gs) {
        Convo c = new Convo("زميلتك", "ينادونها سارة. تقول: مرحبًا، وصلت متأخرًا. هل تريد ملخّص ما فاتك؟");
        c.choices.add(new Choice(
                "نعم، أرجوكِ", "أعطتك ملخصًا سريعًا وأرسلت لك ملاحظاتها على الهاتف.",
                0, 2, 1, 2, "got_notes"));
        c.choices.add(new Choice(
                "أحاول الاعتماد على نفسي", "ابتسمت: حسنًا، لكن طلب المساعدة ليس ضعفًا.",
                0, 1, 0, -1, null));
        c.choices.add(new Choice(
                "اسأليها عن الأستاذ", "قالت: الأستاذ صارم لكنه عادل، وغالبًا يحب الأسئلة.",
                0, 0, 0, 1, "knows_prof"));
        return c;
    }

    private static Convo professor(GameState gs) {
        boolean knows = gs.flags.contains("knows_prof");
        Convo c = new Convo("الأستاذ",
                knows ? "أهلًا، فهمت أنك سألتَ عني. سؤالك؟"
                      : "تفضل. عندك سؤال؟");
        c.choices.add(new Choice(
                "اسأله عن طريقة تقديم المشاريع", "شرح لك الخيارات: شفهي، أو ملف صوتي، أو شريك يقرأ معك.",
                0, 1, 2, 1, "knows_options"));
        c.choices.add(new Choice(
                "اطلب وقتًا إضافيًا للاختبار", "وافق بهدوء وقال: هذا حقك، أرسل لي طلبًا رسميًا.",
                0, 0, 1, 2, null));
        c.choices.add(new Choice(
                "اشكره وانصرف", "ودّعك بكلمة قصيرة.", 0, 0, 0, 0, null));
        return c;
    }

    private static Convo curious(GameState gs) {
        Convo c = new Convo("زميل فضولي",
                "يقول بصوت عالٍ: كيف تعرف أين تمشي؟ هل ترى شيئًا أصلًا؟");
        c.choices.add(new Choice(
                "اشرح بهدوء", "شرحتَ بهدوء أن العصا والصوت يكفيان، وانتهى متفهمًا.",
                0, 1, 0, 2, null));
        c.choices.add(new Choice(
                "اطلب منه الكفّ عن الأسئلة", "بدا محرجًا واعتذر، ثم انسحب.",
                0, -1, 0, 1, "snubbed_curious"));
        c.choices.add(new Choice(
                "اقلب الموقف بمزحة", "ضحك واعتذر. أصبح أكثر تهذيبًا.",
                0, 2, 0, 2, null));
        return c;
    }

    private static Convo pedestrianMale(GameState gs) {
        Convo c = new Convo("شخص في الشارع", "يقول: ممكن أساعدك تعبر؟");
        c.choices.add(new Choice(
                "نعم، شكرًا", "أمسك بطرف كوعك ومشى معك حتى الجهة المقابلة.",
                1, 1, 0, 0, null));
        c.choices.add(new Choice(
                "أعرف الطريق، شكرًا", "ابتسم وانصرف باحترام.",
                1, 0, 0, 1, null));
        c.choices.add(new Choice(
                "اسأله عن الحافلة", "قال: تأتي بعد دقيقتين تقريبًا من اليسار.",
                0, 0, 0, 0, "bus_soon"));
        return c;
    }

    private static Convo pedestrianFemale(GameState gs) {
        Convo c = new Convo("امرأة لطيفة", "تقول: السلام عليكم، تحتاج اتجاهًا؟");
        c.choices.add(new Choice(
                "اطلب اتجاه الجامعة", "أشارت يمينًا ووصفت لك المعالم.",
                0, 1, 0, 1, "knows_uni_dir"));
        c.choices.add(new Choice(
                "اطلب اتجاه المقهى", "قالت: أمامك مباشرة ثم باب على اليمين.",
                0, 1, 0, 1, "knows_cafe_dir"));
        c.choices.add(new Choice(
                "شكرًا، أعرف طريقي", "ابتسمت ومشت.", 0, 0, 0, 1, null));
        return c;
    }

    private static Convo waiter(GameState gs) {
        Convo c = new Convo("النادل", "أهلًا. عربية كالعادة أم تجرب جديدًا اليوم؟");
        c.choices.add(new Choice(
                "كالعادة من فضلك", "أحضر لك قهوة سادة مع كوب ماء.", 0, 1, 0, 1, null));
        c.choices.add(new Choice(
                "اسأله عن الأصناف", "قرأ لك القائمة بهدوء.", 0, 1, 0, 0, null));
        c.choices.add(new Choice(
                "اطلب طاولة هادئة", "أرشدك إلى ركن بعيد عن السماعات.", 1, 0, 0, 1, "quiet_seat"));
        return c;
    }

    private static Convo oldFriend(GameState gs) {
        Convo c = new Convo("صديقك القديم", "أهلًا يا صديق، كيف الحال اليوم؟");
        c.choices.add(new Choice(
                "تعبت قليلًا", "ربّت على كتفك وقال: خذ نَفَسًا، نحن هنا.", 0, 2, 0, 2, null));
        c.choices.add(new Choice(
                "اسأله عن أداة جديدة", "أراك تطبيقًا يصف الصور لحظيًّا.",
                0, 0, 3, 1, "knows_app"));
        c.choices.add(new Choice(
                "حدّثه عن الدراسة", "استمع بانتباه وأبدى رأيًا مفيدًا.",
                0, 2, 0, 1, null));
        return c;
    }

    private static Convo librarian(GameState gs) {
        Convo c = new Convo("أمين المكتبة",
                "أهلًا بك. لديّ كتب صوتية ونسخ بطريقة برايل. ماذا تحتاج؟");
        c.choices.add(new Choice(
                "اطلب كتابًا صوتيًا في المنهج",
                "أعدّ لك قائمة على هاتفك. الكتب جاهزة للاستماع مساء اليوم.",
                0, 0, 3, 1, "got_audiobook"));
        c.choices.add(new Choice(
                "اسأل عن مكان هادئ للقراءة",
                "هناك زاوية بعيدة عن الباب. اتجه يمينًا ثم أمامك.",
                1, 1, 0, 1, null));
        c.choices.add(new Choice(
                "اطلب مساعدة في بحث",
                "أعطاك مرشدًا للموضوع وملخصًا قصيرًا، ووعدك بالمتابعة.",
                0, 2, 2, 1, "research_help"));
        c.choices.add(new Choice(
                "اشكره وانصرف",
                "ابتسم بهدوء ووعدك بالعودة في أي وقت.", 0, 1, 0, 0, null));
        return c;
    }

    private static Convo libraryStudent(GameState gs) {
        Convo c = new Convo("طالب يقرأ",
                "همس: عذرًا، هل يمكنك التحدث بصوت أخفض؟ أحاول التركيز.");
        c.choices.add(new Choice(
                "اعتذر بهدوء",
                "ابتسم ورجع لكتابه دون أن يضيف شيئًا.",
                0, 1, 0, 1, null));
        c.choices.add(new Choice(
                "اطلب منه توصية لكتاب",
                "أوصى بكتاب عن المدن، وأخبرك بمكانه على الرفّ.",
                0, 1, 1, 1, "knows_book"));
        c.choices.add(new Choice(
                "تجاهل وانصرف",
                "بدا منزعجًا قليلًا، لكنه عاد للقراءة.", 0, -1, 0, 0, null));
        return c;
    }

    /** Quick random utterance used by the random-event system. */
    public static String randomFlavor() {
        String[] arr = {
                "شخص يضحك بصوت عالٍ من بعيد.",
                "هاتف يرنّ ولا أحد يجيب.",
                "خطوات سريعة تمرّ خلفك.",
                "نسمة هواء باردة على وجهك.",
                "صوت ملعقة تطرق فنجانًا.",
                "كتاب يسقط على أرضية خشبية.",
                "زقزقة عصفور في الأعلى."
        };
        return arr[RNG.nextInt(arr.length)];
    }
}
