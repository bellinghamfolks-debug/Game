package com.hayatkafeef.game.game;

/**
 * Thin wrapper around {@link Prefs} that centralises save / load policy.
 * Decoupling it from the activity gives us one place to add migration,
 * cloud sync, or autosave throttling later.
 */
public class SaveManager {

    private final Prefs prefs;
    private long lastSaveMs;
    private static final long MIN_INTERVAL_MS = 5_000;

    public SaveManager(Prefs prefs) {
        this.prefs = prefs;
    }

    public boolean hasSave() { return prefs.hasSave(); }

    public GameState load() {
        if (!prefs.hasSave()) return null;
        GameState gs = GameState.fromBlob(prefs.saveBlob());
        if (gs.scene == null) gs.scene = Scenes.create(gs.currentSceneId);
        return gs;
    }

    public void save(GameState state) {
        if (state == null) return;
        prefs.writeSave(state.toBlob());
        lastSaveMs = System.currentTimeMillis();
    }

    /** Save no more often than MIN_INTERVAL_MS; used by autosave hooks. */
    public void autosave(GameState state) {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs < MIN_INTERVAL_MS) return;
        save(state);
    }

    public void clear() { prefs.clearSave(); }
}
