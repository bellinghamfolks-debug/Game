package com.hayatkafeef.game.ai;

/**
 * Sanitises AI output before it reaches the player. Returns null if
 * the text should be rejected, otherwise a cleaned-up version.
 *
 * - Trims, caps length, strips role markers and stray HTML.
 * - Rejects empty or single-character replies.
 * - Rejects content containing a small block-list (extend as needed).
 */
public class AiSafetyFilter {

    private static final int MAX_LEN = 480;

    private static final String[] BLOCKED = {
            // Place-holders; expand based on telemetry/playtests.
            "<script", "</script", "javascript:", "data:text/html"
    };

    public String filter(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.length() < 2) return null;
        // strip common chatty role markers if the LLM echoes them
        t = t.replaceAll("(?i)^assistant[:：\\-]+", "");
        t = t.replaceAll("(?i)^user[:：\\-]+", "");
        t = t.replaceAll("(?i)^system[:：\\-]+", "");
        t = t.replaceAll("<[^>]{1,80}>", "");
        // remove markdown emphasis stars that TTS reads as "نجمة"
        t = t.replaceAll("\\*+", "");
        t = t.replaceAll("_+", "");
        t = t.replaceAll("`+", "");
        t = t.trim();
        for (String b : BLOCKED) if (t.toLowerCase().contains(b)) return null;
        if (t.length() > MAX_LEN) t = t.substring(0, MAX_LEN);
        return t;
    }
}
