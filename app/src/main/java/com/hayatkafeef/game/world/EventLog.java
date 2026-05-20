package com.hayatkafeef.game.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Rolling log of meaningful in-game events. Used by the Event Log
 * screen ("ما الذي حدث للتو؟") and by AiSummaryManager to build the
 * end-of-day summary.
 */
public class EventLog {

    public static class Entry {
        public final String timeStr;
        public final String text;
        public final long whenMs;
        public Entry(String t, String s) {
            this.timeStr = t; this.text = s; this.whenMs = System.currentTimeMillis();
        }
    }

    private static final int MAX = 60;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private Entry lastSpoken;

    public synchronized void add(String timeStr, String text) {
        if (text == null) return;
        String t = text.trim();
        if (t.isEmpty()) return;
        if (entries.size() >= MAX) entries.pollFirst();
        Entry e = new Entry(timeStr, t);
        entries.offerLast(e);
        lastSpoken = e;
    }

    public synchronized Entry last() { return lastSpoken; }

    public synchronized List<Entry> recent(int n) {
        List<Entry> list = new ArrayList<>(entries);
        if (list.size() > n) list = list.subList(list.size() - n, list.size());
        return new ArrayList<>(list);
    }

    public synchronized String renderRecent(int n) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : recent(n)) {
            sb.append(e.timeStr).append(" — ").append(e.text).append('\n');
        }
        return sb.toString().trim();
    }

    public synchronized void clear() { entries.clear(); lastSpoken = null; }
}
