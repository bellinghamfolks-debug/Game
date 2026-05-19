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
    private static final String K_HAS_SAVE = "has_save";
    private static final String K_SAVE_BLOB = "save_blob";
    private static final String K_ONBOARDED = "onboarded";

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
