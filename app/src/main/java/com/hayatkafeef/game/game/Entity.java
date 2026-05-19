package com.hayatkafeef.game.game;

/** A thing in a scene: person, object, door, obstacle. */
public class Entity {
    public enum Kind {
        PERSON,    // can talk
        DOOR,      // transition
        BENCH,
        TREE,
        WALL,
        STAIRS,
        BUS,
        CAR,
        ELEVATOR,
        DESK,
        BED,
        SHOP,
        FOUNTAIN,
        PILLAR
    }

    public final Kind kind;
    public float x, y;
    /** Display radius / footprint in world tiles. */
    public float radius;
    /** Optional id used for dialogue/scripts. */
    public String id;
    /** Human-readable name in Arabic, used by TTS and HUD. */
    public String name;
    /** Optional secondary tag for description, e.g. "صديق", "زميل". */
    public String tag;
    /** Does this entity make sound (for spatial audio cue)? */
    public boolean makesSound;
    /** Loop frequency for sound (Hz) — used by SpatialAudio.tone() */
    public int soundHz;
    /** Sound role: 0 silent, 1 ambient, 2 hazard, 3 friendly. */
    public int soundRole;

    public Entity(Kind kind, float x, float y, float radius, String name) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.name = name;
    }

    public Entity tag(String t) { this.tag = t; return this; }
    public Entity id(String s) { this.id = s; return this; }
    public Entity sound(int hz, int role) {
        this.makesSound = true;
        this.soundHz = hz;
        this.soundRole = role;
        return this;
    }
}
