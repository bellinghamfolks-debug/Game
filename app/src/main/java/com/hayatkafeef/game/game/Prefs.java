package com.hayatkafeef.game.game;

import android.content.Context;
import android.content.SharedPreferences;

import com.hayatkafeef.game.render.VisionMode;

public class Prefs {

    public static final String FILE = "hayatkafeef_prefs";

    private static final String K_VISION = "vision_mode";
    private static final String K_TTS_RATE = "tts_rate";       // 50..200, default 100
    private static final String K_VIBRATION = "vibration";
    private static final String K_SPATIAL = "spatial";
    private static final String K_PROXY = "ai_proxy_url";
    private static final String K_GEMINI_KEY = "gemini_api_key";
    private static final String K_GEMINI_MODEL = "gemini_model";
    private static final String K_AI_ENABLED = "ai_enabled";
    private static final String K_AI_MODE = "ai_mode";              // 0 off, 1 basic, 2 advanced
    private static final String K_AI_ANALYSIS = "ai_analysis";
    private static final String K_AI_MEMORY = "ai_memory_json";
    private static final String K_COMMENTARY = "commentary_level";  // 0 verbose, 1 normal, 2 brief, 3 audio-only
    private static final String K_DIFFICULTY = "difficulty";        // 0 easy, 1 normal, 2 hard
    private static final String K_HAS_SAVE = "has_save";
    private static final String K_SAVE_BLOB = "save_blob";
    private static final String K_ONBOARDED = "onboarded";

    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        this.sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public VisionMode visionMode() {
        return VisionMode.fromId(sp.getInt(K_VISION, VisionMode.SIGHTED.id()));
    }
    public void setVisionMode(VisionMode m) { sp.edit().putInt(K_VISION, m.id()).apply(); }

    public int ttsRate() { return sp.getInt(K_TTS_RATE, 100); }
    public void setTtsRate(int v) { sp.edit().putInt(K_TTS_RATE, Math.max(50, Math.min(200, v))).apply(); }

    public boolean vibrationEnabled() { return sp.getBoolean(K_VIBRATION, true); }
    public void setVibrationEnabled(boolean v) { sp.edit().putBoolean(K_VIBRATION, v).apply(); }

    public boolean spatialAudio() { return sp.getBoolean(K_SPATIAL, true); }
    public void setSpatialAudio(boolean v) { sp.edit().putBoolean(K_SPATIAL, v).apply(); }

    public String proxyUrl() { return sp.getString(K_PROXY, ""); }
    public void setProxyUrl(String v) { sp.edit().putString(K_PROXY, v == null ? "" : v).apply(); }

    public String geminiKey() { return sp.getString(K_GEMINI_KEY, ""); }
    public void setGeminiKey(String v) { sp.edit().putString(K_GEMINI_KEY, v == null ? "" : v.trim()).apply(); }

    public String geminiModel() { return sp.getString(K_GEMINI_MODEL, DEFAULT_GEMINI_MODEL); }
    public void setGeminiModel(String v) {
        sp.edit().putString(K_GEMINI_MODEL, (v == null || v.trim().isEmpty()) ? DEFAULT_GEMINI_MODEL : v.trim()).apply();
    }

    public boolean aiEnabled() { return sp.getBoolean(K_AI_ENABLED, true); }
    public void setAiEnabled(boolean v) { sp.edit().putBoolean(K_AI_ENABLED, v).apply(); }

    /** 0 = off, 1 = basic enrichment, 2 = advanced (analyses player style). */
    public int aiMode() { return sp.getInt(K_AI_MODE, 1); }
    public void setAiMode(int v) { sp.edit().putInt(K_AI_MODE, Math.max(0, Math.min(2, v))).apply(); }

    public boolean aiAnalysisAllowed() { return sp.getBoolean(K_AI_ANALYSIS, true); }
    public void setAiAnalysisAllowed(boolean v) { sp.edit().putBoolean(K_AI_ANALYSIS, v).apply(); }

    public String aiMemory() { return sp.getString(K_AI_MEMORY, ""); }
    public void setAiMemory(String v) { sp.edit().putString(K_AI_MEMORY, v == null ? "" : v).apply(); }
    public void clearAiMemory() { sp.edit().remove(K_AI_MEMORY).apply(); }

    /** 0 verbose, 1 normal, 2 brief, 3 audio-only. */
    public int commentaryLevel() { return sp.getInt(K_COMMENTARY, 1); }
    public void setCommentaryLevel(int v) { sp.edit().putInt(K_COMMENTARY, Math.max(0, Math.min(3, v))).apply(); }

    public int difficulty() { return sp.getInt(K_DIFFICULTY, 1); }
    public void setDifficulty(int v) { sp.edit().putInt(K_DIFFICULTY, Math.max(0, Math.min(2, v))).apply(); }

    public boolean hasSave() { return sp.getBoolean(K_HAS_SAVE, false); }
    public String saveBlob() { return sp.getString(K_SAVE_BLOB, ""); }
    public void writeSave(String blob) {
        sp.edit().putBoolean(K_HAS_SAVE, true).putString(K_SAVE_BLOB, blob).apply();
    }
    public void clearSave() {
        sp.edit().remove(K_HAS_SAVE).remove(K_SAVE_BLOB).apply();
    }

    public boolean onboarded() { return sp.getBoolean(K_ONBOARDED, false); }
    public void setOnboarded(boolean v) { sp.edit().putBoolean(K_ONBOARDED, v).apply(); }
}
