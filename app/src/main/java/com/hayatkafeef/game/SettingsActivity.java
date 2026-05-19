package com.hayatkafeef.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.hayatkafeef.game.ai.AiClient;
import com.hayatkafeef.game.game.Prefs;
import com.hayatkafeef.game.render.VisionMode;

public class SettingsActivity extends Activity {

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        prefs = new Prefs(this);

        final Spinner sp = findViewById(R.id.sp_vision);
        final SeekBar sb = findViewById(R.id.sb_tts);
        final CheckBox cbVib = findViewById(R.id.cb_vibration);
        final CheckBox cbSpa = findViewById(R.id.cb_spatial);
        final EditText et = findViewById(R.id.et_proxy);
        final Button testBtn = findViewById(R.id.btn_test_proxy);
        final Button resetBtn = findViewById(R.id.btn_reset);
        final Button saveBtn = findViewById(R.id.btn_save);

        String[] names = {
                getString(R.string.vision_total),
                getString(R.string.vision_low),
                getString(R.string.vision_blur),
                getString(R.string.vision_central),
                getString(R.string.vision_peripheral),
                getString(R.string.vision_sighted)
        };
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        sp.setAdapter(ad);
        sp.setSelection(prefs.visionMode().id());
        sb.setProgress(prefs.ttsRate());
        cbVib.setChecked(prefs.vibrationEnabled());
        cbSpa.setChecked(prefs.spatialAudio());
        et.setText(prefs.proxyUrl());

        testBtn.setOnClickListener(v -> {
            String url = et.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, R.string.msg_offline_mode, Toast.LENGTH_SHORT).show();
                return;
            }
            new AiClient(url).test(new AiClient.Callback() {
                @Override public void onReply(String text) {
                    Toast.makeText(SettingsActivity.this, R.string.msg_proxy_ok, Toast.LENGTH_SHORT).show();
                }
                @Override public void onError(String msg) {
                    Toast.makeText(SettingsActivity.this, R.string.msg_proxy_fail, Toast.LENGTH_LONG).show();
                }
            });
        });

        resetBtn.setOnClickListener(v -> new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage(R.string.msg_reset_confirm)
                .setPositiveButton(R.string.msg_yes, (d, w) -> {
                    prefs.clearSave();
                    Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.msg_no, null)
                .show());

        saveBtn.setOnClickListener(v -> {
            prefs.setVisionMode(VisionMode.fromId(sp.getSelectedItemPosition()));
            prefs.setTtsRate(sb.getProgress());
            prefs.setVibrationEnabled(cbVib.isChecked());
            prefs.setSpatialAudio(cbSpa.isChecked());
            prefs.setProxyUrl(et.getText().toString().trim());
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
