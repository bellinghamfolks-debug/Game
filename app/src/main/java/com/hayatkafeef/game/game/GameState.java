package com.hayatkafeef.game.game;

/** All mutable world state lives here. */
public class GameState {

    public Player player = new Player();
    public Scene scene;
    public Scene.Id currentSceneId = Scene.Id.HOME;
    /** Game-clock minutes since 06:00. */
    public int minutes = 0;
    /** Day count. */
    public int day = 1;
    /** Boolean flags / story bits. */
    public final java.util.HashSet<String> flags = new java.util.HashSet<>();

    public void enterScene(Scene s, float spawnX, float spawnY) {
        this.scene = s;
        this.currentSceneId = s.id;
        player.x = spawnX;
        player.y = spawnY;
    }

    public String formatTime() {
        int total = 6 * 60 + minutes;
        int hh = (total / 60) % 24;
        int mm = total % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", hh, mm);
    }

    public void tickMinutes(int m) {
        minutes += m;
        if (minutes >= 18 * 60) {
            minutes = 0;
            day++;
        }
    }

    /** Serialize critical bits to a tiny string. */
    public String toBlob() {
        StringBuilder sb = new StringBuilder();
        sb.append("S:").append(currentSceneId.name()).append('\n');
        sb.append("M:").append(minutes).append('\n');
        sb.append("D:").append(day).append('\n');
        sb.append("P:").append(player.toJson()).append('\n');
        if (!flags.isEmpty()) {
            sb.append("F:");
            boolean first = true;
            for (String f : flags) {
                if (!first) sb.append(',');
                sb.append(f);
                first = false;
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static GameState fromBlob(String blob) {
        GameState gs = new GameState();
        if (blob == null) return gs;
        for (String line : blob.split("\n")) {
            if (line.startsWith("S:")) {
                try { gs.currentSceneId = Scene.Id.valueOf(line.substring(2)); }
                catch (Exception ignored) {}
            } else if (line.startsWith("M:")) {
                try { gs.minutes = Integer.parseInt(line.substring(2)); } catch (Exception ignored) {}
            } else if (line.startsWith("D:")) {
                try { gs.day = Integer.parseInt(line.substring(2)); } catch (Exception ignored) {}
            } else if (line.startsWith("P:")) {
                gs.player = Player.fromJson(line.substring(2));
            } else if (line.startsWith("F:")) {
                for (String f : line.substring(2).split(",")) {
                    if (!f.isEmpty()) gs.flags.add(f);
                }
            }
        }
        return gs;
    }
}
