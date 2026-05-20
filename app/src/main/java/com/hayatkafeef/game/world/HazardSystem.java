package com.hayatkafeef.game.world;

import com.hayatkafeef.game.game.GameState;
import com.hayatkafeef.game.game.Scene;

import java.util.Random;

/**
 * Active hazards with a reaction window:
 *
 * 1. The game announces a warning ("دراجة قادمة من يمينك! توقف").
 * 2. For ~1.6 seconds the player can stop walking.
 * 3. After the window, the hazard resolves:
 *      - If still walking ⇒ impact (stat loss, scary sound).
 *      - If stopped ⇒ near miss (small confidence + mobility gain).
 *
 * Unlike EventSystem (passive flavor), HazardSystem creates real
 * stakes that reward the player for listening.
 */
public class HazardSystem {

    public enum Kind { BICYCLE, CAR, OPEN_HOLE }

    public interface Listener {
        void onWarning(String text, Kind kind);
        void onResolve(String text, boolean impact, Kind kind);
    }

    private final Random rng = new Random();

    private boolean armed;
    private long warnedAtMs;
    private Kind kind;
    private static final long WINDOW_MS = 1600;
    private static final long COOLDOWN_MS = 14000;
    private long lastHazardEndMs;

    public boolean isArmed() { return armed; }

    public void tick(GameState gs, boolean walking, Listener listener) {
        if (gs == null || gs.scene == null) return;
        long now = System.currentTimeMillis();

        if (armed) {
            if (now - warnedAtMs >= WINDOW_MS) {
                // resolve
                boolean impact = walking;
                if (impact) applyImpact(gs, kind);
                else applyNearMiss(gs);
                lastHazardEndMs = now;
                String txt = resolveText(kind, impact);
                Kind k = kind;
                armed = false;
                kind = null;
                listener.onResolve(txt, impact, k);
            }
            return;
        }

        if (now - lastHazardEndMs < COOLDOWN_MS) return;
        // probability per second depends on scene & difficulty
        float chance = chanceForScene(gs.currentSceneId);
        if (chance <= 0) return;
        // tick is called ~20Hz, so divide
        if (rng.nextFloat() > chance / 20f) return;

        // arm a new hazard
        kind = pickKind(gs.currentSceneId);
        armed = true;
        warnedAtMs = now;
        listener.onWarning(warningText(kind), kind);
    }

    public void reset() {
        armed = false;
        kind = null;
        lastHazardEndMs = 0;
    }

    // -------- internals --------

    private float chanceForScene(Scene.Id id) {
        switch (id) {
            case STREET: return 0.05f;     // ~5% per second
            case UNIVERSITY: return 0.02f; // crowded, but mostly safe
            default: return 0.005f;
        }
    }

    private Kind pickKind(Scene.Id id) {
        if (id == Scene.Id.STREET) {
            float r = rng.nextFloat();
            if (r < 0.55f) return Kind.BICYCLE;
            return Kind.CAR;
        }
        return Kind.OPEN_HOLE;
    }

    private String warningText(Kind k) {
        switch (k) {
            case BICYCLE: return "تنبيه! دراجة قادمة من يمينك. توقف الآن.";
            case CAR:     return "تنبيه! سيارة قريبة. ثبّت قدميك.";
            case OPEN_HOLE: return "تنبيه! شيء مرتفع أمامك. توقف.";
        }
        return "تنبيه!";
    }

    private String resolveText(Kind k, boolean impact) {
        if (impact) {
            switch (k) {
                case BICYCLE: return "اصطدمت بالدراجة. خفض مهارة الحركة قليلًا.";
                case CAR:     return "خدشتك السيارة. كن أكثر حذرًا.";
                case OPEN_HOLE: return "تعثّرت قليلًا. لا شيء خطير، لكن انتبه.";
            }
            return "ارتطمت بشيء.";
        }
        switch (k) {
            case BICYCLE: return "أحسنت! الدراجة عبرت بأمان.";
            case CAR:     return "أحسنت! السيارة ابتعدت.";
            case OPEN_HOLE: return "تجنّبتها. مرر يدك واستمر بحذر.";
        }
        return "بأمان.";
    }

    private void applyImpact(GameState gs, Kind k) {
        switch (k) {
            case BICYCLE: gs.player.mobility -= 3; gs.player.confidence -= 2; break;
            case CAR:     gs.player.mobility -= 5; gs.player.confidence -= 4; break;
            case OPEN_HOLE: gs.player.mobility -= 1; break;
        }
        gs.player.clamp();
    }

    private void applyNearMiss(GameState gs) {
        gs.player.mobility += 1;
        gs.player.confidence += 1;
        gs.player.clamp();
    }
}
