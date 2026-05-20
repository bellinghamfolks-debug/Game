package com.hayatkafeef.game.achievements;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the catalogue of achievements, the set of unlocked IDs, and
 * the integer / set counters that drive them.
 *
 * Triggers come from GameEngine (mission complete, hazard survived,
 * NPC interaction, etc.). Counters and unlocked state survive across
 * sessions via {@link #toJson()} / {@link #fromJson(String)}.
 */
public class AchievementManager {

    public interface Listener {
        void onUnlocked(Achievement a);
    }

    private final Map<String, Achievement> all = new LinkedHashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Set<String>> distinctSets = new HashMap<>();
    private Listener listener;

    public AchievementManager() {
        for (Achievement a : Achievements.defaults()) all.put(a.id, a);
    }

    public void setListener(Listener l) { this.listener = l; }

    public List<Achievement> all() { return new ArrayList<>(all.values()); }

    public int unlockedCount() {
        int n = 0;
        for (Achievement a : all.values()) if (a.unlocked) n++;
        return n;
    }
    public int total() { return all.size(); }

    public boolean isUnlocked(String id) {
        Achievement a = all.get(id);
        return a != null && a.unlocked;
    }

    public synchronized void unlock(String id) {
        Achievement a = all.get(id);
        if (a == null || a.unlocked) return;
        a.unlocked = true;
        a.unlockedAt = System.currentTimeMillis();
        if (listener != null) listener.onUnlocked(a);
    }

    /** Add 1 to a counter, then test it against a threshold and unlock if reached. */
    public synchronized void bump(String counterId, int threshold, String achievementId) {
        int n = counters.getOrDefault(counterId, 0) + 1;
        counters.put(counterId, n);
        if (n >= threshold) unlock(achievementId);
    }

    /** Reset a counter to zero (used by 'consecutive' style achievements). */
    public synchronized void reset(String counterId) {
        counters.put(counterId, 0);
    }

    /** Record a distinct member of a set; unlock when the set has minSize entries. */
    public synchronized void recordDistinct(String setId, String member,
                                            int minSize, String achievementId) {
        if (member == null || member.isEmpty()) return;
        Set<String> set = distinctSets.computeIfAbsent(setId, k -> new HashSet<>());
        if (set.add(member) && set.size() >= minSize) unlock(achievementId);
    }

    public synchronized int counter(String id) { return counters.getOrDefault(id, 0); }
    public synchronized int distinctCount(String id) {
        Set<String> set = distinctSets.get(id);
        return set == null ? 0 : set.size();
    }

    // ---------- persistence ----------

    public synchronized String toJson() {
        try {
            JSONObject root = new JSONObject();
            JSONArray unlocked = new JSONArray();
            for (Achievement a : all.values()) {
                if (a.unlocked) {
                    JSONObject o = new JSONObject();
                    o.put("id", a.id);
                    o.put("t", a.unlockedAt);
                    unlocked.put(o);
                }
            }
            root.put("u", unlocked);

            JSONObject ctr = new JSONObject();
            for (Map.Entry<String, Integer> e : counters.entrySet()) ctr.put(e.getKey(), e.getValue());
            root.put("c", ctr);

            JSONObject dst = new JSONObject();
            for (Map.Entry<String, Set<String>> e : distinctSets.entrySet()) {
                dst.put(e.getKey(), new JSONArray(new ArrayList<>(e.getValue())));
            }
            root.put("d", dst);
            return root.toString();
        } catch (JSONException ex) { return ""; }
    }

    public synchronized void loadJson(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray unlocked = root.optJSONArray("u");
            if (unlocked != null) {
                for (int i = 0; i < unlocked.length(); i++) {
                    JSONObject o = unlocked.getJSONObject(i);
                    Achievement a = all.get(o.optString("id"));
                    if (a != null) { a.unlocked = true; a.unlockedAt = o.optLong("t", 0); }
                }
            }
            JSONObject ctr = root.optJSONObject("c");
            if (ctr != null) {
                java.util.Iterator<String> it = ctr.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    counters.put(k, ctr.optInt(k, 0));
                }
            }
            JSONObject dst = root.optJSONObject("d");
            if (dst != null) {
                java.util.Iterator<String> it = dst.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    JSONArray arr = dst.optJSONArray(k);
                    if (arr == null) continue;
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) set.add(arr.optString(i));
                    distinctSets.put(k, set);
                }
            }
        } catch (JSONException ignored) {}
    }
}
