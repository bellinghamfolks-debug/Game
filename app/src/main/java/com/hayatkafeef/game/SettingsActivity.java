package com.hayatkafeef.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
        final CheckBox cbAi = findViewById(R.id.cb_ai_enabled);
        final Spinner spAi = findViewById(R.id.sp_ai_mode);
        final CheckBox cbAna = findViewById(R.id.cb_ai_analysis);
        final Button clearMem = findViewById(R.id.btn_clear_memory);
        final EditText etKey = findViewById(R.id.et_gemini_key);
        final EditText etModel = findViewById(R.id.et_gemini_model);
        final EditText etProxy = findViewById(R.id.et_proxy);
        final Button getKeyBtn = findViewById(R.id.btn_get_key);
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
        etKey.setText(prefs.geminiKey());
        etModel.setText(prefs.geminiModel());
        etProxy.setText(prefs.proxyUrl());

        cbAi.setChecked(prefs.aiEnabled());
        String[] modes = {
                getString(R.string.setting_ai_mode_off),
                getString(R.string.setting_ai_mode_basic),
                getString(R.string.setting_ai_mode_advanced)
        };
        ArrayAdapter<String> aiAd = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, modes);
        spAi.setAdapter(aiAd);
        spAi.setSelection(prefs.aiMode());
        cbAna.setChecked(prefs.aiAnalysisAllowed());

        clearMem.setOnClickListener(v -> {
            prefs.clearAiMemory();
            Toast.makeText(this, R.string.msg_memory_cleared, Toast.LENGTH_SHORT).show();
        });

        getKeyBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://aistudio.google.com/app/apikey")));
            } catch (Exception ignored) {}
        });

        testBtn.setOnClickListener(v -> {
            // Save current edits first so the test reflects them.
            String key = etKey.getText().toString().trim();
            String model = etModel.getText().toString().trim();
            String proxy = etProxy.getText().toString().trim();
            AiClient client = AiClient.fromPrefs(key, model, proxy);
            if (!client.isConfigured()) {
                Toast.makeText(this, R.string.msg_key_required, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.setting_ai_test, Toast.LENGTH_SHORT).show();
            client.test(new AiClient.Callback() {
                @Override public void onReply(String text) {
                    int msg = client.mode() == AiClient.Mode.GEMINI
                            ? R.string.msg_ai_mode_gemini : R.string.msg_ai_mode_proxy;
                    Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();
                }
                @Override public void onError(String msg) {
                    Toast.makeText(SettingsActivity.this,
                            getString(R.string.msg_proxy_fail) + "\n" + msg,
                            Toast.LENGTH_LONG).show();
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
            prefs.setGeminiKey(etKey.getText().toString());
            prefs.setGeminiModel(etModel.getText().toString());
            prefs.setProxyUrl(etProxy.getText().toString().trim());
            prefs.setAiEnabled(cbAi.isChecked());
            prefs.setAiMode(spAi.getSelectedItemPosition());
            prefs.setAiAnalysisAllowed(cbAna.isChecked());
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
