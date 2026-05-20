package com.hayatkafeef.game.world;

/**
 * Lightweight running profile of how the player is doing.
 * Used by the engine to drive the "adaptive" commentary level —
 * struggling players hear more, expert players hear less.
 */
public class PlayerProfile {

    private int hintsRequested;
    private int hazardsFailed;
    private int hazardsSurvived;
    private int missionsCompleted;
    private int collisions;
    private long startedAtMs = System.currentTimeMillis();

    public void onHint() { hintsRequested++; }
    public void onHazardImpact() { hazardsFailed++; }
    public void onHazardSurvived() { hazardsSurvived++; }
    public void onMissionDone() { missionsCompleted++; }
    public void onCollision() { collisions++; }

    /** Returns a struggle score 0..1; higher means the player is having a hard time. */
    public float struggle() {
        float hintWeight   = Math.min(1f, hintsRequested * 0.10f);
        float crashWeight  = Math.min(1f, hazardsFailed * 0.20f);
        float collideWeight= Math.min(1f, collisions * 0.05f);
        float survivalGain = Math.min(0.5f, hazardsSurvived * 0.05f);
        float progressGain = Math.min(0.4f, missionsCompleted * 0.04f);
        float s = (hintWeight + crashWeight + collideWeight) - (survivalGain + progressGain);
        return Math.max(0f, Math.min(1f, s));
    }

    /** Suggested verbosity (0=verbose, 1=normal, 2=brief, 3=audio-only). */
    public int suggestedCommentaryLevel() {
        float st = struggle();
        if (st > 0.6f) return 0;     // suffering — explain everything
        if (st > 0.3f) return 1;     // a little lost — normal
        if (st > 0.05f) return 2;    // doing OK — brief
        return 2;                    // expert — short
    }
}
