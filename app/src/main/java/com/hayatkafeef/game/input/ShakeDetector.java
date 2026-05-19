package com.hayatkafeef.game.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    public interface Listener { void onShake(); }

    private final SensorManager sm;
    private final Sensor accel;
    private final Listener listener;
    private long lastShake;
    private static final float THRESHOLD = 18f;   // m/s^2 over baseline
    private static final long COOLDOWN_MS = 800;

    public ShakeDetector(Context ctx, Listener l) {
        this.listener = l;
        this.sm = (SensorManager) ctx.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        this.accel = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public void start() {
        if (sm != null && accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
    }
    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent e) {
        float x = e.values[0], y = e.values[1], z = e.values[2];
        double mag = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
        if (mag > THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShake > COOLDOWN_MS) {
                lastShake = now;
                listener.onShake();
            }
        }
    }
    @Override public void onAccuracyChanged(Sensor s, int a) {}
}
