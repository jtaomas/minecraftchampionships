package com.mcm.games.hitw;

import org.bukkit.Material;

import java.util.*;

/**
 * One moving wall. It slides along X (axisX) or Z, one cell per step, covering lateral -HALF..HALF and
 * rows WALL_Y..WALL_Y+WALL_H-1. The cell position along the axis is derived from dir and progress:
 * pos = dir * (progress - START), so progress 0 is the far start and 2*START the far end.
 */
final class Wall {
    static final int LANES = Arena.HALF * 2 + 1;
    static final int TOTAL = Arena.START * 2;
    /** Progress at which the wall reaches the first platform cell. */
    static final int ENTRY = Arena.START - Arena.HALF;

    /** A wave of walls launched together; awards "wall survived" once all of them are gone. */
    static final class Wave {
        int remaining;
        boolean crossed;
    }

    final boolean axisX;
    int dir;
    int progress;
    final boolean[][] solid = new boolean[LANES][Arena.WALL_H];
    final EnumSet<WallType> types;
    final Material material;
    final int baseInterval;
    final boolean waltz;
    final boolean turbo;
    final Wave wave;
    int cooldown;
    int pause;
    boolean removed;
    private boolean returned, morphed, warped, endered, tricked;
    private final Set<Integer> freezeAt = new HashSet<>();
    private final int holes;
    private final int difficulty;
    private final Random rnd;

    Wall(boolean axisX, int dir, EnumSet<WallType> types, int baseInterval, boolean turbo,
         int holes, int difficulty, int platformHalf, int startDelay, Wave wave, Random rnd) {
        this.axisX = axisX;
        this.dir = dir;
        this.types = types;
        this.baseInterval = Math.max(1, baseInterval);
        this.waltz = types.contains(WallType.WALTZ);
        this.turbo = turbo;
        this.holes = holes;
        this.difficulty = difficulty;
        this.wave = wave;
        this.rnd = rnd;
        this.cooldown = this.baseInterval;
        this.pause = startDelay;
        Material m = WallType.STANDARD.material;
        for (WallType t : types) if (t != WallType.STANDARD && t != WallType.SIKE) { m = t.material; break; }
        this.material = m;
        if (types.contains(WallType.FROZEN)) {
            int n = 1 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) freezeAt.add(ENTRY - 2 + rnd.nextInt(Arena.HALF + 4));
        }
        carve(platformHalf);
    }

    int pos() { return dir * (progress - Arena.START); }

    String label() {
        StringBuilder sb = new StringBuilder();
        for (WallType t : types) {
            if (t == WallType.SIKE) continue;
            if (t == WallType.STANDARD && types.size() > 1) continue;
            if (sb.length() > 0) sb.append("§7+");
            sb.append(t.color).append(t.label);
        }
        if (sb.length() == 0) sb.append(WallType.STANDARD.color).append(WallType.STANDARD.label);
        return sb.toString();
    }

    String fromName() {
        if (axisX) return dir > 0 ? "West" : "East";
        return dir > 0 ? "North" : "South";
    }

    // ---- hole shapes: {lateral offset, row} ----
    private static final int[][] DOOR1 = {{0, 0}, {0, 1}};
    private static final int[][] DOOR2 = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
    private static final int[][] ARCH = {{-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}, {0, 2}};
    private static final int[][] TALL = {{0, 0}, {0, 1}, {0, 2}};
    private static final int[][] TEE = {{0, 0}, {0, 1}, {-1, 2}, {0, 2}, {1, 2}};
    private static final int[][] PLUS = {{0, 0}, {0, 1}, {0, 2}, {-1, 1}, {1, 1}};
    private static final int[][] JUMP1 = {{0, 1}, {0, 2}, {0, 3}};
    private static final int[][] JUMP2 = {{0, 1}, {1, 1}, {0, 2}, {1, 2}, {0, 3}, {1, 3}};
    private static final int[][] STAIR = {{0, 0}, {0, 1}, {1, 1}, {1, 2}, {1, 3}};
    private static final int[][][] EASY = {DOOR2, ARCH, PLUS, TEE, DOOR2, TALL};
    private static final int[][][] MEDIUM = {DOOR1, DOOR2, ARCH, TALL, TEE, PLUS, JUMP1, JUMP2, STAIR};
    private static final int[][][] HARD = {DOOR1, TALL, JUMP1, JUMP2, STAIR, PLUS, DOOR1};

    /** Fill the wall and cut holes, all within the current platform width. */
    void carve(int platformHalf) {
        for (boolean[] col : solid) Arrays.fill(col, true);
        int[][][] pool = difficulty <= 0 ? EASY : difficulty == 1 ? MEDIUM : HARD;
        int lo = -platformHalf + 1, hi = platformHalf - 1;
        if (hi < lo) { lo = hi = 0; }
        List<Integer> anchors = new ArrayList<>();
        for (int a = lo; a <= hi; a++) anchors.add(a);
        Collections.shuffle(anchors, rnd);
        int placed = 0;
        List<Integer> used = new ArrayList<>();
        for (int a : anchors) {
            if (placed >= holes) break;
            boolean near = false;
            for (int u : used) if (Math.abs(u - a) < 3) { near = true; break; }
            if (near) continue;
            int[][] shape = pool[rnd.nextInt(pool.length)];
            for (int[] c : shape) {
                int lat = a + c[0];
                if (lat < -platformHalf || lat > platformHalf) continue;
                solid[lat + Arena.HALF][c[1]] = false;
            }
            used.add(a);
            placed++;
        }
        if (placed == 0) { solid[Arena.HALF][0] = false; solid[Arena.HALF][1] = false; }
    }

    /** Advance one tick. Calls back into the game after each single-cell step. */
    void tick(HoleInTheWallGame game) {
        if (removed) return;
        if (pause > 0) { pause--; return; }
        if (--cooldown > 0) return;
        int n = (waltz ? 2 : 1) * (turbo ? 2 : 1);
        for (int i = 0; i < n && !removed && pause == 0; i++) stepOnce(game);
        cooldown = nextInterval();
    }

    private int nextInterval() {
        int iv = baseInterval;
        if (types.contains(WallType.TRICK)) iv = progress < ENTRY - 2 ? iv * 2 : 1;
        if (types.contains(WallType.SPRINT)) iv = ((progress / 3) % 2 == 0) ? iv * 2 : Math.max(1, iv / 2);
        if (waltz) iv = iv * 2;
        return Math.max(1, iv);
    }

    private void stepOnce(HoleInTheWallGame game) {
        progress++;
        game.onWallStep(this);
        if (progress >= TOTAL) {
            if (types.contains(WallType.RETURNING) && !returned) {
                returned = true;
                dir = -dir;
                progress = 0;
                game.wallEvent(this, "returning");
            } else {
                removed = true;
            }
            return;
        }
        if (types.contains(WallType.SIKE) && progress == ENTRY - 1) {
            removed = true;
            game.wallEvent(this, "sike");
            return;
        }
        if (types.contains(WallType.ENDER) && !endered && progress == ENTRY - 1) {
            endered = true;
            dir = -dir; // mirrors the position to the other side
            game.wallEvent(this, "ender");
            return;
        }
        if (types.contains(WallType.WARP) && !warped && progress == 1) {
            warped = true;
            progress = ENTRY - 1;
            game.wallEvent(this, "warp");
            return;
        }
        if (types.contains(WallType.MORPH) && !morphed && progress == ENTRY - 2) {
            morphed = true;
            carve(game.platformHalf());
            game.wallEvent(this, "morph");
        }
        if (types.contains(WallType.TRICK) && !tricked && progress == ENTRY - 2) {
            tricked = true;
            pause = 14;
            game.wallEvent(this, "trick");
        }
        if (freezeAt.remove(progress)) {
            pause = 15 + rnd.nextInt(20);
            game.wallEvent(this, "frozen");
        }
    }
}
