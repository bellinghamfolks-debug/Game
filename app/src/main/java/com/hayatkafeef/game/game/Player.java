package com.hayatkafeef.game.game;

/** Player position, facing and stats. */
public class Player {
    /** World tile coordinates (float for smooth motion). */
    public float x, y;
    /** Facing angle in radians. 0 = +X (east), pi/2 = +Y (south). */
    public float heading;

    public int mobility = 30;
    public int social = 30;
    public int tech = 20;
    public int confidence = 25;

    /** Devices the player owns. */
    public boolean ownsCane = true;
    public boolean ownsSmartGlasses = false;
    public boolean ownsSmartCane = false;
    public boolean ownsAiAssistant = false;
    public boolean ownsHeadphones = false;

    public void clamp() {
        mobility = clamp01(mobility);
        social = clamp01(social);
        tech = clamp01(tech);
        confidence = clamp01(confidence);
    }
    private static int clamp01(int v) { return Math.max(0, Math.min(100, v)); }

    public String toJson() {
        StringBuilder s = new StringBuilder();
        s.append("{\"x\":").append(x)
                .append(",\"y\":").append(y)
                .append(",\"h\":").append(heading)
                .append(",\"mob\":").append(mobility)
                .append(",\"soc\":").append(social)
                .append(",\"tch\":").append(tech)
                .append(",\"conf\":").append(confidence)
                .append(",\"cane\":").append(ownsCane)
                .append(",\"sg\":").append(ownsSmartGlasses)
                .append(",\"sc\":").append(ownsSmartCane)
                .append(",\"ai\":").append(ownsAiAssistant)
                .append(",\"hp\":").append(ownsHeadphones)
                .append("}");
        return s.toString();
    }

    public static Player fromJson(String raw) {
        Player p = new Player();
        if (raw == null) return p;
        p.x = parseFloat(raw, "\"x\":", 4f);
        p.y = parseFloat(raw, "\"y\":", 4f);
        p.heading = parseFloat(raw, "\"h\":", 0f);
        p.mobility = (int) parseFloat(raw, "\"mob\":", 30);
        p.social = (int) parseFloat(raw, "\"soc\":", 30);
        p.tech = (int) parseFloat(raw, "\"tch\":", 20);
        p.confidence = (int) parseFloat(raw, "\"conf\":", 25);
        p.ownsCane = parseBool(raw, "\"cane\":", true);
        p.ownsSmartGlasses = parseBool(raw, "\"sg\":", false);
        p.ownsSmartCane = parseBool(raw, "\"sc\":", false);
        p.ownsAiAssistant = parseBool(raw, "\"ai\":", false);
        p.ownsHeadphones = parseBool(raw, "\"hp\":", false);
        return p;
    }

    private static float parseFloat(String raw, String key, float def) {
        int i = raw.indexOf(key);
        if (i < 0) return def;
        i += key.length();
        int end = i;
        while (end < raw.length() && "-0123456789.eE".indexOf(raw.charAt(end)) >= 0) end++;
        try { return Float.parseFloat(raw.substring(i, end)); } catch (Exception e) { return def; }
    }
    private static boolean parseBool(String raw, String key, boolean def) {
        int i = raw.indexOf(key);
        if (i < 0) return def;
        return raw.startsWith("true", i + key.length());
    }
}
