package com.hayatkafeef.game.game;

import android.view.View;
import android.widget.TextView;

import com.hayatkafeef.game.R;
import com.hayatkafeef.game.missions.Mission;
import com.hayatkafeef.game.missions.MissionManager;

/**
 * Owns the HUD strip and the mission line. Activity hands it the four
 * TextViews and calls {@link #update(GameState, MissionManager)} every
 * frame; nothing else needs to know how HUD strings are laid out.
 */
public class HudController {

    private final android.app.Activity activity;
    private final TextView hudLocation, hudTime, hudStats, hudMission;

    public HudController(android.app.Activity activity, View root) {
        this.activity = activity;
        this.hudLocation = root.findViewById(R.id.hud_location);
        this.hudTime = root.findViewById(R.id.hud_time);
        this.hudStats = root.findViewById(R.id.hud_stats);
        this.hudMission = root.findViewById(R.id.hud_mission);
    }

    public void update(GameState gs, MissionManager missions) {
        if (gs == null) return;
        hudLocation.setText(activity.getString(R.string.hud_location,
                gs.scene != null ? gs.scene.name : "—"));
        hudTime.setText(activity.getString(R.string.hud_time,
                gs.formatTime() + " · يوم " + gs.day));
        hudStats.setText(
                activity.getString(R.string.hud_stat_mobility) + " " + gs.player.mobility + "  ·  " +
                activity.getString(R.string.hud_stat_social) + " " + gs.player.social + "  ·  " +
                activity.getString(R.string.hud_stat_tech) + " " + gs.player.tech + "  ·  " +
                activity.getString(R.string.hud_stat_confidence) + " " + gs.player.confidence
        );
        Mission cur = missions != null ? missions.current() : null;
        if (cur != null) {
            int p = missions.progress() + 1;
            int t = missions.total();
            hudMission.setText("◉ " + p + "/" + t + " — " + cur.title);
        } else {
            hudMission.setText("✓ اكتملت مهام اليوم");
        }
        hudMission.setVisibility(View.VISIBLE);
    }
}
