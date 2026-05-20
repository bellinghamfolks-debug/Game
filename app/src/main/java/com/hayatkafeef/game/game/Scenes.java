package com.hayatkafeef.game.game;

/** Factory for the built-in scenes. */
public class Scenes {

    public static Scene home() {
        Scene s = new Scene(Scene.Id.HOME, "البيت", 16, 12, 0x0E1116, 0x6B5A48, 0x8B5A2B, 1);
        // The bed also hosts the alarm. Interacting with it stops the alarm.
        s.add(new Entity(Entity.Kind.BED, 2.0f, 2.0f, 1.2f, "سرير")
                .tag("سريرك — صوت منبه قادم منه")
                .id("bed")
                .sound(880, 2));
        s.add(new Entity(Entity.Kind.DESK, 12.0f, 2.0f, 1.0f, "مكتب").tag("مكتبك").id("desk"));
        // The cane rests against the desk — short, low-profile entity.
        s.add(new Entity(Entity.Kind.PILLAR, 13.2f, 3.0f, 0.35f, "عصاك")
                .tag("عصا المشي البيضاء")
                .id("cane"));
        s.add(new Entity(Entity.Kind.WALL, 8.0f, 6.0f, 0.6f, "عمود").tag("عمود في المنتصف").id("pillar1"));
        Entity door = new Entity(Entity.Kind.DOOR, 14.5f, 10.0f, 0.8f, "باب البيت").tag("باب يخرج للشارع").id("door_to_street");
        s.add(door);
        s.exits.put("door_to_street", Scene.Id.STREET);
        s.add(new Entity(Entity.Kind.PILLAR, 1.0f, 10.0f, 0.4f, "ساعة حائط").tag("تكتك خفيف").id("clock").sound(440, 1));
        return s;
    }

    public static Scene street() {
        Scene s = new Scene(Scene.Id.STREET, "الشارع", 28, 14, 0x8FB7D6, 0x3B3F46, 0x4F7A4A, 2);
        // sidewalks: trees as obstacles on edges
        for (int i = 2; i <= 24; i += 4) {
            s.add(new Entity(Entity.Kind.TREE, i, 2.5f, 0.7f, "شجرة").id("tree_n" + i));
            s.add(new Entity(Entity.Kind.TREE, i, 11.5f, 0.7f, "شجرة").id("tree_s" + i));
        }
        // bench
        s.add(new Entity(Entity.Kind.BENCH, 6.5f, 3.5f, 0.6f, "مقعد").tag("مقعد خشبي").id("bench1"));
        // crossing people
        s.add(new Entity(Entity.Kind.PERSON, 10, 7, 0.5f, "رجل")
                .tag("شخص يسير ببطء").id("pedestrian1"));
        s.add(new Entity(Entity.Kind.PERSON, 18, 8, 0.5f, "امرأة")
                .tag("شخصية ودودة").id("pedestrian2"));
        // bus stop
        s.add(new Entity(Entity.Kind.BUS, 25, 6.5f, 1.5f, "حافلة")
                .tag("حافلة متوقفة في الموقف").id("bus").sound(110, 2));
        // doors / transitions
        Entity homeDoor = new Entity(Entity.Kind.DOOR, 1.0f, 4.0f, 0.8f, "باب بيتك").tag("ادخل البيت").id("door_home");
        s.add(homeDoor);
        s.exits.put("door_home", Scene.Id.HOME);
        Entity uniDoor = new Entity(Entity.Kind.DOOR, 26.5f, 4.0f, 0.9f, "بوابة الجامعة").tag("ادخل الجامعة").id("door_uni");
        s.add(uniDoor);
        s.exits.put("door_uni", Scene.Id.UNIVERSITY);
        Entity cafeDoor = new Entity(Entity.Kind.DOOR, 14.0f, 12.5f, 0.8f, "باب المقهى").tag("ادخل المقهى").id("door_cafe");
        s.add(cafeDoor);
        s.exits.put("door_cafe", Scene.Id.CAFE);
        // fountain ambient
        s.add(new Entity(Entity.Kind.FOUNTAIN, 14, 7, 0.8f, "نافورة")
                .tag("صوت ماء هادئ").id("fountain").sound(220, 1));
        return s;
    }

    public static Scene university() {
        Scene s = new Scene(Scene.Id.UNIVERSITY, "الجامعة", 22, 14, 0xA0BDD6, 0x70654A, 0x4F7A4A, 4);
        // pillars and walls
        for (int i = 4; i <= 18; i += 4) {
            s.add(new Entity(Entity.Kind.PILLAR, i, 5.5f, 0.5f, "عمود").id("p" + i));
        }
        // benches and a fountain
        s.add(new Entity(Entity.Kind.BENCH, 6, 9, 0.6f, "مقعد").id("uni_bench1"));
        s.add(new Entity(Entity.Kind.BENCH, 14, 9, 0.6f, "مقعد").id("uni_bench2"));
        // students
        s.add(new Entity(Entity.Kind.PERSON, 8, 8, 0.5f, "زميلة دراسة").tag("زميلتك التي تساعدك أحيانًا").id("classmate"));
        s.add(new Entity(Entity.Kind.PERSON, 11, 7, 0.5f, "زميل فضولي").tag("شخص يسأل كثيرًا").id("curious"));
        s.add(new Entity(Entity.Kind.PERSON, 16, 6, 0.5f, "الأستاذ").tag("أستاذ المادة").id("professor"));
        // lecture hall door
        Entity hall = new Entity(Entity.Kind.DOOR, 11, 2.0f, 0.9f, "قاعة المحاضرة").tag("ادخل قاعة المحاضرة").id("door_hall");
        s.add(hall);
        // elevator (sometimes broken — handled by EventSystem)
        s.add(new Entity(Entity.Kind.ELEVATOR, 4, 2.0f, 0.8f, "مصعد").id("elevator").sound(330, 1));
        // exit
        Entity exit = new Entity(Entity.Kind.DOOR, 1.0f, 12.0f, 0.9f, "البوابة الخارجية").tag("اخرج للشارع").id("door_back_street");
        s.add(exit);
        s.exits.put("door_back_street", Scene.Id.STREET);
        return s;
    }

    public static Scene cafe() {
        Scene s = new Scene(Scene.Id.CAFE, "المقهى", 14, 10, 0x2A1F1A, 0x4A3326, 0xE0C36A, 3);
        s.add(new Entity(Entity.Kind.DESK, 7, 4, 1.0f, "طاولة").id("table1"));
        s.add(new Entity(Entity.Kind.DESK, 3, 7, 1.0f, "طاولة").id("table2"));
        s.add(new Entity(Entity.Kind.DESK, 11, 7, 1.0f, "طاولة").id("table3"));
        s.add(new Entity(Entity.Kind.PERSON, 4, 7, 0.5f, "النادل").tag("نادل ودود").id("waiter"));
        s.add(new Entity(Entity.Kind.PERSON, 10, 7, 0.5f, "صديق قديم").tag("صديق يعرف عنك الكثير").id("oldfriend"));
        // ambient coffee machine
        s.add(new Entity(Entity.Kind.SHOP, 13, 1, 0.8f, "ماكينة قهوة").tag("صوت طحن البن").id("machine").sound(150, 1));
        Entity out = new Entity(Entity.Kind.DOOR, 7, 9.5f, 0.8f, "باب الخروج").tag("اخرج للشارع").id("door_back_street");
        s.add(out);
        s.exits.put("door_back_street", Scene.Id.STREET);
        return s;
    }

    public static Scene create(Scene.Id id) {
        switch (id) {
            case HOME: return home();
            case STREET: return street();
            case UNIVERSITY: return university();
            case CAFE: return cafe();
        }
        return home();
    }
}
