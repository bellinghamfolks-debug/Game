package com.hayatkafeef.game.missions;

import com.hayatkafeef.game.game.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Drives the sequence of missions for the current day.
 * - check(state) is called every frame.
 * - When the current mission's predicate becomes true, the manager
 *   advances and notifies the listener so the engine can narrate.
 */
public class MissionManager {

    public interface Listener {
        /** previous just completed; next may be null if the day is over. */
        void onMissionCompleted(Mission previous, Mission next);
        void onAllMissionsCompleted();
    }

    private final List<Mission> all;
    private int idx;
    private Listener listener;

    public MissionManager(List<Mission> missions) {
        this.all = missions != null ? missions : new ArrayList<>();
        this.idx = 0;
    }

    public void setListener(Listener l) { this.listener = l; }

    public Mission current() { return idx < all.size() ? all.get(idx) : null; }
    public int progress() { return idx; }
    public int total() { return all.size(); }
    public List<Mission> all() { return Collections.unmodifiableList(all); }
    public boolean allCompleted() { return idx >= all.size(); }

    public void check(GameState gs) {
        Mission cur = current();
        if (cur == null) return;
        if (!cur.completed && cur.check(gs)) {
            cur.completed = true;
            cur.onComplete(gs);
            idx++;
            Mission next = current();
            if (listener != null) {
                listener.onMissionCompleted(cur, next);
                if (next == null) listener.onAllMissionsCompleted();
            }
        }
    }

    public void load(String blob) {
        if (blob == null || blob.isEmpty()) return;
        try {
            int v = Integer.parseInt(blob.trim());
            idx = Math.max(0, Math.min(all.size(), v));
            for (int i = 0; i < idx; i++) all.get(i).completed = true;
        } catch (NumberFormatException ignored) {}
    }
    public String save() { return Integer.toString(idx); }
}
