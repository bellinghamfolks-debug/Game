package com.hayatkafeef.game.ai;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Per-NPC persistent memory of the player's behaviour, used by
 * AiDialogueManager to color future conversations.
 *
 * Stored as JSON inside SharedPreferences via Prefs.setAiMemory(...).
 */
public class CharacterMemory {

    public static class Entry {
        public int interactions;
        public int kindness;         // -10 .. +10
        public int helpAccepted;
        public int helpRefused;
        public long lastSeenMs;
        public Entry() {}
        public Entry(int i, int k, int ha, int hr, long t) {
            interactions = i; kindness = k; helpAccepted = ha; helpRefused = hr; lastSeenMs = t;
        }
    }

    private final Map<String, Entry> entries = new HashMap<>();

    public synchronized Entry get(String npcId) {
        if (npcId == null) return new Entry();
        Entry e = entries.get(npcId);
        if (e == null) { e = new Entry(); entries.put(npcId, e); }
        return e;
    }

    public synchronized void recordInteraction(String npcId) {
        if (npcId == null) return;
        Entry e = get(npcId);
        e.interactions++;
        e.lastSeenMs = System.currentTimeMillis();
    }

    public synchronized void recordKindness(String npcId, int delta) {
        if (npcId == null) return;
        Entry e = get(npcId);
        e.kindness = clamp(e.kindness + delta, -10, 10);
    }

    public synchronized void recordHelp(String npcId, boolean accepted) {
        if (npcId == null) return;
        Entry e = get(npcId);
        if (accepted) e.helpAccepted++; else e.helpRefused++;
    }

    /** Short Arabic phrase describing the current relationship. */
    public synchronized String describeRelationship(String npcId) {
        Entry e = entries.get(npcId);
        if (e == null || e.interactions == 0) return "أول لقاء";
        if (e.kindness >= 5) return "علاقة دافئة، قابلتها " + e.interactions + " مرة";
        if (e.kindness <= -5) return "علاقة متوترة، قابلتها " + e.interactions + " مرة";
        if (e.helpAccepted > e.helpRefused) return "تعتمد عليها أحيانًا";
        if (e.helpRefused > e.helpAccepted) return "تفضّل ألا تطلب منها مساعدة";
        if (e.interactions == 1) return "تقابلتما مرة واحدة";
        return "علاقة عادية";
    }

    public synchronized void clear() { entries.clear(); }

    public synchronized String toJson() {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, Entry> kv : entries.entrySet()) {
                Entry e = kv.getValue();
                JSONObject o = new JSONObject();
                o.put("i", e.interactions);
                o.put("k", e.kindness);
                o.put("ha", e.helpAccepted);
                o.put("hr", e.helpRefused);
                o.put("t", e.lastSeenMs);
                root.put(kv.getKey(), o);
            }
            return root.toString();
        } catch (JSONException ex) { return ""; }
    }

    public static CharacterMemory fromJson(String json) {
        CharacterMemory m = new CharacterMemory();
        if (json == null || json.isEmpty()) return m;
        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> it = root.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject o = root.getJSONObject(k);
                m.entries.put(k, new Entry(
                        o.optInt("i", 0),
                        o.optInt("k", 0),
                        o.optInt("ha", 0),
                        o.optInt("hr", 0),
                        o.optLong("t", 0)));
            }
        } catch (JSONException ignored) {}
        return m;
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
