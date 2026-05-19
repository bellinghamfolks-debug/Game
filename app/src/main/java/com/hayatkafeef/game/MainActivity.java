package com.hayatkafeef.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hayatkafeef.game.game.Prefs;
import com.hayatkafeef.game.render.VisionMode;

public class MainActivity extends Activity {

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);

        Button btnNew = findViewById(R.id.btn_new);
        Button btnContinue = findViewById(R.id.btn_continue);
        Button btnManual = findViewById(R.id.btn_manual);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnAbout = findViewById(R.id.btn_about);

        btnNew.setOnClickListener(v -> showVisionPicker(true));
        btnContinue.setOnClickListener(v -> {
            if (!prefs.hasSave()) {
                Toast.makeText(this, R.string.msg_no_save, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent it = new Intent(this, GameActivity.class);
            it.putExtra(GameActivity.EXTRA_NEW_GAME, false);
            startActivity(it);
        });
        btnManual.setOnClickListener(v -> startActivity(new Intent(this, ManualActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle(R.string.menu_about)
                    .setMessage(R.string.about_text)
                    .setPositiveButton(R.string.btn_close, null)
                    .show();
        });

        if (!prefs.hasSave()) btnContinue.setAlpha(0.55f);
    }

    private void showVisionPicker(boolean newGame) {
        final String[] names = {
                getString(R.string.vision_total),
                getString(R.string.vision_low),
                getString(R.string.vision_blur),
                getString(R.string.vision_central),
                getString(R.string.vision_peripheral),
                getString(R.string.vision_sighted)
        };
        final String[] descs = {
                getString(R.string.vision_total_desc),
                getString(R.string.vision_low_desc),
                getString(R.string.vision_blur_desc),
                getString(R.string.vision_central_desc),
                getString(R.string.vision_peripheral_desc),
                getString(R.string.vision_sighted_desc)
        };
        final VisionMode[] modes = {
                VisionMode.TOTAL,
                VisionMode.LOW,
                VisionMode.BLUR,
                VisionMode.CENTRAL,
                VisionMode.PERIPHERAL,
                VisionMode.SIGHTED
        };

        // Build a Material-style dialog with our own list so screen readers
        // see the description sub-line too.
        View root = LayoutInflater.from(this).inflate(R.layout.activity_vision_pick, null);
        LinearLayout list = root.findViewById(R.id.vision_list);

        AlertDialog dlg = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setView(root)
                .setNegativeButton(R.string.btn_close, null)
                .create();

        for (int i = 0; i < names.length; i++) {
            ViewGroup item = (ViewGroup) LayoutInflater.from(this)
                    .inflate(R.layout.item_vision, list, false);
            TextView nameView = item.findViewById(R.id.vision_name);
            TextView descView = item.findViewById(R.id.vision_desc);
            nameView.setText(names[i]);
            descView.setText(descs[i]);
            final VisionMode m = modes[i];
            item.setOnClickListener(v -> {
                prefs.setVisionMode(m);
                dlg.dismiss();
                Intent it = new Intent(MainActivity.this, GameActivity.class);
                it.putExtra(GameActivity.EXTRA_NEW_GAME, newGame);
                startActivity(it);
            });
            list.addView(item);
        }

        dlg.show();
    }
}
