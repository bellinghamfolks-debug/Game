package com.hayatkafeef.game.game;

import com.hayatkafeef.game.audio.SpatialAudio;
import com.hayatkafeef.game.audio.TtsManager;
import com.hayatkafeef.game.haptics.HapticManager;

/**
 * Bundles the three "speak / sound / shake" outputs behind one façade,
 * so callers can express intent (confirm / warn / danger) instead of
 * coordinating three subsystems each time.
 */
public class AudioFeedbackManager {

    private final TtsManager tts;
    private final SpatialAudio audio;
    private final HapticManager haptics;

    public AudioFeedbackManager(TtsManager tts, SpatialAudio audio, HapticManager haptics) {
        this.tts = tts;
        this.audio = audio;
        this.haptics = haptics;
    }

    public TtsManager tts() { return tts; }
    public SpatialAudio audio() { return audio; }
    public HapticManager haptics() { return haptics; }

    public void confirm() {
        haptics.confirm();
    }

    public void confirm(String text) {
        if (text != null && !text.isEmpty()) tts.speakNow(text);
        haptics.confirm();
    }

    public void warn(String text) {
        haptics.warn();
        if (text != null && !text.isEmpty()) tts.speakNow(text);
    }

    public void danger(String text) {
        haptics.danger();
        if (text != null && !text.isEmpty()) tts.speakNow(text);
        // sharp tone, centered, brief
        audio.cue("alert", 880, 220, 0f, 0.7f, false);
    }

    public void say(String text) {
        if (text != null && !text.isEmpty()) tts.speak(text);
    }
}
