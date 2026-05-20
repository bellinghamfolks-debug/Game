package com.hayatkafeef.game.missions;

import com.hayatkafeef.game.game.GameState;

/**
 * A unit of progress. The check() predicate is invoked every game tick
 * and when it becomes true, MissionManager advances to the next mission.
 *
 * Each Mission carries its own local hint string. AiHintManager may
 * choose to use it directly or feed it as context to the LLM for a
 * more personalised hint.
 */
public abstract class Mission {

    public final String id;
    public final String title;
    public final String description;
    public final String hint;
    public boolean completed;

    protected Mission(String id, String title, String description, String hint) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.hint = hint;
    }

    /** Predicate evaluated each frame. */
    public abstract boolean check(GameState gs);

    /** Optional reward / side-effect hook. */
    public void onComplete(GameState gs) {}
}
