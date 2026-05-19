package com.hayatkafeef.game.ai;

import com.hayatkafeef.game.game.Entity;
import com.hayatkafeef.game.game.Scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A* path search on a 0.5-tile grid for the current scene.
 * Doors are walkable, every other entity is an obstacle inflated by
 * the player's body radius.
 */
public class Pathfinder {

    private static final int CPT = 2; // cells per tile

    public static class Point {
        public final float x, y;
        public Point(float x, float y) { this.x = x; this.y = y; }
    }

    /** Returns a smoothed list of world-coordinate points from (px,py) to a cell near target. */
    public static List<Point> find(Scene scene, float px, float py, Entity target) {
        if (scene == null || target == null) return null;
        int W = Math.max(1, (int) Math.ceil(scene.width * CPT));
        int H = Math.max(1, (int) Math.ceil(scene.height * CPT));
        boolean[][] blocked = buildGrid(scene, target, W, H);

        int sx = clamp((int) (px * CPT), 0, W - 1);
        int sy = clamp((int) (py * CPT), 0, H - 1);
        int gx = clamp((int) (target.x * CPT), 0, W - 1);
        int gy = clamp((int) (target.y * CPT), 0, H - 1);

        // start cell can be blocked (player overlapping margin); free it
        if (blocked[sx][sy]) blocked[sx][sy] = false;
        if (blocked[gx][gy]) {
            int[] near = findNearestFree(blocked, gx, gy, 8);
            if (near == null) return null;
            gx = near[0]; gy = near[1];
        }

        int[] ddx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] ddy = {0, 0, 1, -1, 1, -1, 1, -1};
        float[] cost = {1f, 1f, 1f, 1f, 1.4142f, 1.4142f, 1.4142f, 1.4142f};

        int N = W * H;
        float[] g = new float[N];
        float[] f = new float[N];
        int[] parent = new int[N];
        boolean[] closed = new boolean[N];
        Arrays.fill(g, Float.POSITIVE_INFINITY);
        Arrays.fill(f, Float.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        int startIdx = sy * W + sx;
        int goalIdx = gy * W + gx;
        g[startIdx] = 0;
        f[startIdx] = heuristic(sx, sy, gx, gy);

        PriorityQueue<int[]> open = new PriorityQueue<>((a, b) -> Float.compare(f[a[0]], f[b[0]]));
        open.add(new int[]{startIdx});

        while (!open.isEmpty()) {
            int cur = open.poll()[0];
            if (cur == goalIdx) return smooth(reconstruct(parent, cur, W), blocked);
            if (closed[cur]) continue;
            closed[cur] = true;

            int cx = cur % W, cy = cur / W;
            for (int i = 0; i < 8; i++) {
                int nx = cx + ddx[i], ny = cy + ddy[i];
                if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                if (blocked[nx][ny]) continue;
                if (Math.abs(ddx[i]) + Math.abs(ddy[i]) == 2) {
                    if (blocked[cx + ddx[i]][cy]) continue;
                    if (blocked[cx][cy + ddy[i]]) continue;
                }
                int neighbor = ny * W + nx;
                float tentative = g[cur] + cost[i];
                if (tentative < g[neighbor]) {
                    g[neighbor] = tentative;
                    parent[neighbor] = cur;
                    f[neighbor] = tentative + heuristic(nx, ny, gx, gy);
                    open.add(new int[]{neighbor});
                }
            }
        }
        return null;
    }

    /** Line-of-sight smoothing: drop intermediate cells while a direct line is clear. */
    private static List<Point> smooth(List<Point> raw, boolean[][] blocked) {
        if (raw == null || raw.size() < 3) return raw;
        List<Point> out = new ArrayList<>();
        int i = 0;
        out.add(raw.get(0));
        while (i < raw.size() - 1) {
            int j = raw.size() - 1;
            while (j > i + 1) {
                if (lineClear(raw.get(i), raw.get(j), blocked)) break;
                j--;
            }
            out.add(raw.get(j));
            i = j;
        }
        return out;
    }

    private static boolean lineClear(Point a, Point b, boolean[][] blocked) {
        // sample every 0.25 tile
        float dist = (float) Math.hypot(b.x - a.x, b.y - a.y);
        int steps = Math.max(2, (int) Math.ceil(dist / 0.25f));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float fx = a.x + (b.x - a.x) * t;
            float fy = a.y + (b.y - a.y) * t;
            int cx = (int) (fx * CPT);
            int cy = (int) (fy * CPT);
            if (cx < 0 || cy < 0 || cx >= blocked.length || cy >= blocked[0].length) return false;
            if (blocked[cx][cy]) return false;
        }
        return true;
    }

    private static List<Point> reconstruct(int[] parent, int goal, int W) {
        List<Point> path = new ArrayList<>();
        int cur = goal;
        while (cur != -1) {
            int cx = cur % W, cy = cur / W;
            path.add(new Point((cx + 0.5f) / CPT, (cy + 0.5f) / CPT));
            cur = parent[cur];
        }
        Collections.reverse(path);
        return path;
    }

    private static float heuristic(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        return Math.max(dx, dy) + 0.4142f * Math.min(dx, dy);
    }

    private static boolean[][] buildGrid(Scene scene, Entity target, int W, int H) {
        boolean[][] blocked = new boolean[W][H];
        for (Entity e : scene.entities) {
            if (e == target) continue;
            if (e.kind == Entity.Kind.DOOR) continue; // doors are walkable
            float r = e.radius + 0.35f;
            int rc = (int) Math.ceil(r * CPT) + 1;
            int cx = Math.round(e.x * CPT);
            int cy = Math.round(e.y * CPT);
            for (int dx = -rc; dx <= rc; dx++) {
                for (int dy = -rc; dy <= rc; dy++) {
                    int x = cx + dx, y = cy + dy;
                    if (x < 0 || x >= W || y < 0 || y >= H) continue;
                    float fx = (x + 0.5f) / CPT, fy = (y + 0.5f) / CPT;
                    float ddx = fx - e.x, ddy = fy - e.y;
                    if (ddx * ddx + ddy * ddy <= r * r) blocked[x][y] = true;
                }
            }
        }
        return blocked;
    }

    private static int[] findNearestFree(boolean[][] blocked, int gx, int gy, int maxR) {
        int W = blocked.length, H = blocked[0].length;
        for (int r = 1; r <= maxR; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = gx + dx, y = gy + dy;
                    if (x < 0 || x >= W || y < 0 || y >= H) continue;
                    if (!blocked[x][y]) return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
