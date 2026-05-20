package com.hayatkafeef.game.achievements;

/**
 * One milestone the player can unlock. Achievements are defined once
 * in {@link Achievements} and stored persistently inside
 * {@link AchievementManager}.
 */
public class Achievement {

    public final String id;
    public final String title;
    public final String description;
    public boolean unlocked;
    public long unlockedAt;

    public Achievement(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }
}
