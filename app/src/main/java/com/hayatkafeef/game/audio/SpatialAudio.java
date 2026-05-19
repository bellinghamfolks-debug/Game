package com.hayatkafeef.game.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight runtime-synthesized "spatial" audio.
 * No mp3 files: we synthesize short PCM tones / noise per cue,
 * with stereo pan + volume based on the entity angle/distance.
 */
public class SpatialAudio {

    private static final String TAG = "Hayat.Audio";
    private static final int SAMPLE_RATE = 22050;

    private final Map<String, AudioTrack> active = new HashMap<>();
    private boolean enabled = true;

    public void setEnabled(boolean v) {
        this.enabled = v;
        if (!v) stopAll();
    }

    /**
     * Play a short directional cue.
     * @param key        unique key to avoid spamming the same source
     * @param freqHz     base tone frequency
     * @param durationMs duration in ms
     * @param panX       -1..+1, left to right
     * @param volume     0..1
     * @param noisy      if true, mix in noise (footstep / engine feel)
     */
    public void cue(String key, int freqHz, int durationMs, float panX, float volume, boolean noisy) {
        if (!enabled) return;
        panX = clamp(panX, -1f, 1f);
        volume = clamp(volume, 0f, 1f);
        int frames = SAMPLE_RATE * Math.max(40, durationMs) / 1000;
        short[] pcm = new short[frames * 2]; // stereo
        double tau = 2.0 * Math.PI * freqHz / SAMPLE_RATE;
        float panL = (1f - panX) * 0.5f + 0.25f; // never fully zero
        float panR = (1f + panX) * 0.5f + 0.25f;
        double rampIn = Math.min(frames * 0.1, 600);
        double rampOut = Math.min(frames * 0.2, 1200);

        for (int i = 0; i < frames; i++) {
            double env = 1.0;
            if (i < rampIn) env = i / rampIn;
            else if (i > frames - rampOut) env = (frames - i) / rampOut;
            double tone = Math.sin(tau * i);
            double sample = tone;
            if (noisy) {
                double n = (Math.random() * 2.0 - 1.0) * 0.5;
                sample = 0.55 * tone + 0.45 * n;
            }
            sample *= env * volume;
            short s = (short) (sample * 25000);
            pcm[2 * i] = (short) (s * panL);
            pcm[2 * i + 1] = (short) (s * panR);
        }

        try {
            AudioTrack prev = active.remove(key);
            if (prev != null) { try { prev.stop(); prev.release(); } catch (Exception ignored) {} }

            AudioTrack at = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    pcm.length * 2,
                    AudioTrack.MODE_STATIC);
            at.write(pcm, 0, pcm.length);
            at.setStereoVolume(1f, 1f);
            at.play();
            active.put(key, at);
        } catch (Exception e) {
            Log.w(TAG, "cue err: " + e);
        }
    }

    /**
     * Compute pan/volume from entity offset and player heading.
     */
    public static float[] panAndVolume(float px, float py, float heading, float ex, float ey, float maxDist) {
        float dx = ex - px;
        float dy = ey - py;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        // angle from player's forward direction:
        float worldAngle = (float) Math.atan2(dy, dx);
        float rel = worldAngle - heading;
        // normalize to -PI..PI
        while (rel > Math.PI) rel -= 2 * Math.PI;
        while (rel < -Math.PI) rel += 2 * Math.PI;
        // pan: left (-1) when sound is to the player's left, right (+1) when to the right.
        float pan = (float) Math.sin(rel);
        // volume falls off with distance.
        float vol = Math.max(0f, 1f - dist / maxDist);
        return new float[]{pan, vol};
    }

    public void stopAll() {
        for (AudioTrack at : active.values()) {
            try { at.stop(); at.release(); } catch (Exception ignored) {}
        }
        active.clear();
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
