package com.hayatkafeef.game.missions;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;

/** Generic mission predicates shared by all day scripts. */
public final class Missions {

    private Missions() {}

    /** Player must call cmdInteract on entity with the given id. */
    public static class Interact extends Mission {
        public final String entityId;
        public final int rewardMobility;
        public final int rewardSocial;
        public final int rewardTech;
        public final int rewardConfidence;
        public Interact(String id, String title, String desc, String hint, String entityId) {
            this(id, title, desc, hint, entityId, 0, 0, 0, 1);
        }
        public Interact(String id, String title, String desc, String hint, String entityId,
                        int mob, int soc, int tech, int conf) {
            super(id, title, desc, hint);
            this.entityId = entityId;
            this.rewardMobility = mob; this.rewardSocial = soc;
            this.rewardTech = tech;    this.rewardConfidence = conf;
        }
        @Override public boolean check(GameState gs) {
            return gs.flags.contains("i:" + entityId);
        }
        @Override public void onStart(GameState gs) {
            gs.flags.remove("i:" + entityId);
        }
        @Override public void onComplete(GameState gs) {
            gs.player.mobility += rewardMobility;
            gs.player.social += rewardSocial;
            gs.player.tech += rewardTech;
            gs.player.confidence += rewardConfidence;
            gs.player.clamp();
        }
    }

    /** Player must arrive in a given scene. */
    public static class EnterScene extends Mission {
        public final Scene.Id target;
        public EnterScene(String id, String title, String desc, String hint, Scene.Id t) {
            super(id, title, desc, hint);
            this.target = t;
        }
        @Override public boolean check(GameState gs) {
            return gs.currentSceneId == target;
        }
        @Override public void onComplete(GameState gs) {
            gs.player.mobility += 1;
            gs.player.confidence += 1;
            gs.player.clamp();
        }
    }

    /** Player must enter a circular region in a specific scene. */
    public static class ReachPoint extends Mission {
        public final Scene.Id scene;
        public final float x, y, r;
        public ReachPoint(String id, String title, String desc, String hint,
                          Scene.Id scene, float x, float y, float r) {
            super(id, title, desc, hint);
            this.scene = scene; this.x = x; this.y = y; this.r = r;
        }
        @Override public boolean check(GameState gs) {
            if (gs.currentSceneId != scene) return false;
            float dx = gs.player.x - x, dy = gs.player.y - y;
            return dx * dx + dy * dy <= r * r;
        }
        @Override public void onComplete(GameState gs) {
            gs.player.mobility += 1;
            gs.player.clamp();
        }
    }
}
