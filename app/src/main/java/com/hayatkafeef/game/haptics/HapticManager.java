package com.hayatkafeef.game.haptics;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class HapticManager {

    private final Vibrator vib;
    private boolean enabled = true;

    public HapticManager(Context ctx) {
        this.vib = (Vibrator) ctx.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setEnabled(boolean v) { this.enabled = v; }

    public void tick() { pattern(new long[]{0, 25}, -1); }              // very short: close object
    public void warn() { pattern(new long[]{0, 50, 80, 50}, -1); }       // double: obstacle
    public void danger() { pattern(new long[]{0, 200}, -1); }            // long: danger
    public void stairs() { pattern(new long[]{0, 30, 50, 30, 50, 30}, -1); } // triple: stairs
    public void door() { pattern(new long[]{0, 90, 70, 30}, -1); }
    public void person() { pattern(new long[]{0, 40, 40, 40}, -1); }
    public void confirm() { pattern(new long[]{0, 20, 30, 20}, -1); }
    public void error() { pattern(new long[]{0, 150, 80, 150}, -1); }

    private void pattern(long[] pat, int repeat) {
        if (!enabled || vib == null || !vib.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pat, repeat));
            } else {
                vib.vibrate(pat, repeat);
            }
        } catch (Exception ignored) {}
    }
}
