package com.hayatkafeef.game.game;

import java.util.ArrayList;
import java.util.List;

/** A bounded 2D world the player explores. */
public class Scene {

    public enum Id { HOME, STREET, UNIVERSITY, CAFE, LIBRARY }

    public final Id id;
    /** Display name (Arabic). */
    public final String name;
    /** Width and height in tiles. */
    public final float width;
    public final float height;
    /** Ambient palette indices: sky, ground, accent. */
    public final int paletteSky;
    public final int paletteGround;
    public final int paletteAccent;
    /** Ambient sound role for whole scene: 0 silent, 1 indoor, 2 street, 3 cafe, 4 campus. */
    public final int ambient;

    public final List<Entity> entities = new ArrayList<>();
    /** Exits: tied to a door entity id => target scene id. */
    public final java.util.Map<String, Id> exits = new java.util.HashMap<>();

    public Scene(Id id, String name, float w, float h, int sky, int ground, int accent, int ambient) {
        this.id = id;
        this.name = name;
        this.width = w;
        this.height = h;
        this.paletteSky = sky;
        this.paletteGround = ground;
        this.paletteAccent = accent;
        this.ambient = ambient;
    }

    public Entity add(Entity e) { entities.add(e); return e; }

    public Entity findById(String id) {
        if (id == null) return null;
        for (Entity e : entities) if (id.equals(e.id)) return e;
        return null;
    }

    /** Nearest entity within max radius from (px, py). */
    public Entity nearest(float px, float py, float maxR) {
        Entity best = null;
        float bestD = Float.MAX_VALUE;
        for (Entity e : entities) {
            float dx = e.x - px, dy = e.y - py;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < bestD && d <= maxR) { bestD = d; best = e; }
        }
        return best;
    }
}
