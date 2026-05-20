package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.world.EventLog;

import java.util.List;

/**
 * Thin facade that delegates to {@link OfflineContentProvider}.
 * Kept for callers that already wired against this class name; new
 * code should use {@link OfflineContentProvider} directly.
 */
public final class AiFallbackProvider {

    private AiFallbackProvider() {}

    public static String hintFor(Mission m, GameState gs) {
        return OfflineContentProvider.hintFor(m, gs);
    }

    public static String summarize(GameState gs, List<EventLog.Entry> recent) {
        return OfflineContentProvider.summarize(gs, recent);
    }

    public static String narrateMissionStart(Mission m) {
        return OfflineContentProvider.missionStart(m);
    }

    public static String narrateMissionDone(Mission m) {
        return OfflineContentProvider.missionDone(m);
    }

    public static String narrateDayComplete(int day) {
        return OfflineContentProvider.dayComplete(day);
    }
}
