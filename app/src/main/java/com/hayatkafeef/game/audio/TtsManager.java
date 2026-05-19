package com.hayatkafeef.game.audio;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

/** Wrapper around Android TTS that picks Arabic when available, English otherwise. */
public class TtsManager {

    private static final String TAG = "Hayat.TTS";

    private final TextToSpeech tts;
    private boolean ready;
    private Locale spoken = new Locale("ar");
    private float rate = 1.0f;

    public TtsManager(Context ctx) {
        this.tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // try ar first, then en
                int r = tts.setLanguage(new Locale("ar"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                    spoken = Locale.ENGLISH;
                }
                ready = true;
            } else {
                Log.w(TAG, "TTS init failed: " + status);
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) {}
            @Override public void onError(String utteranceId) {}
        });
    }

    public boolean isArabic() { return "ar".equals(spoken.getLanguage()); }

    public void setRate(float r) {
        this.rate = Math.max(0.4f, Math.min(2.0f, r));
        if (tts != null) tts.setSpeechRate(this.rate);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        if (!ready || tts == null) return;
        try {
            String id = UUID.randomUUID().toString();
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, id);
        } catch (Exception e) {
            Log.w(TAG, "speak err: " + e);
        }
    }

    public void speakNow(String text) {
        if (text == null || text.isEmpty()) return;
        if (!ready || tts == null) return;
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString());
        } catch (Exception e) {
            Log.w(TAG, "speakNow err: " + e);
        }
    }

    public void stop() { if (tts != null) try { tts.stop(); } catch (Exception ignored) {} }

    public void shutdown() {
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
    }
}
