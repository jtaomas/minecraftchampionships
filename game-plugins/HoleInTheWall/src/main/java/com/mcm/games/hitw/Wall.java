package com.mcm.games.hitw;

import java.util.*;

/**
 * One moving wall. It slides along X (axisX) or Z, one cell per step, covering lateral -HALF..HALF and
 * rows WALL_Y..WALL_Y+WALL_H-1. The cell position along the axis is derived from dir and progress:
 * pos = dir * (progress - START), so progress 0 is the far start and 2*START the far end.
 *
 * Its holes come from the MCC wall set ({@link WallPatterns}): a 14-wide pattern is laid on the 17 lanes
 * (randomly mirrored and shifted by one lane, the edge column repeated), and it is re-rolled until at least
 * one lane on the current platform can be passed standing or with a jump.
 */
final class Wall {
    static final int LANES = Arena.HALF * 2 + 1;
    static final int TOTAL = Arena.START * 2;
    /** Progress at which the wall reaches the first platform cell. */
    static final int ENTRY = Arena.START - Arena.HALF;

    static final byte AIR = 0, SOLID = 1, CAP = 2, LADDER_L = 3, LADDER_R = 4;
    /** Vengeful walls keep only this many pattern columns. */
    private static final int VENGEFUL_WIDTH = 9;

    /** A wave of walls launched together; awards "wall survived" once all of them are gone. */
    static final class Wave {
        int remaining;
        boolean crossed;
    }

    boolean axisX;
    int dir;
    int progress;
    /** [lane][row], row 0 = floor level. */
    final byte[][] cells = new byte[LANES][Arena.WALL_H];
    final EnumSet<WallType> types;
    final int baseInterval;
    final boolean waltz;
    final boolean turbo;
    final Wave wave;
    final int tier;
    String patternName = "?";
    int cooldown;
    int pause;
    boolean removed;
    private boolean returned, morphed, warped, endered, tricked, swiped;
    private int danceBacks, danceForward;
    private final int danceMax;
    private final Set<Integer> freezeAt = new HashSet<>();
    private final Random rnd;
    /** Vengeful: the narrow pattern and its current lateral centre. */
    private byte[][] narrow;
    private int shift;

    Wall(boolean axisX, int dir, EnumSet<WallType> types, int baseInterval, boolean turbo,
         int tier, int platformHalf, int startDelay, Wave wave, Random rnd) {
        this.axisX = axisX;
        this.dir = dir;
        this.types = types;
        this.baseInterval = Math.max(1, baseInterval);
        this.waltz = types.contains(WallType.WALTZ);
        this.turbo = turbo;
        this.tier = tier;
        this.wave = wave;
        this.rnd = rnd;
        this.cooldown = this.baseInterval;
        this.pause = startDelay;
        this.danceMax = 2 + rnd.nextInt(2);
        if (types.contains(WallType.FROZEN)) {
            int n = 1 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) freezeAt.add(ENTRY - 2 + rnd.nextInt(Arena.HALF + 4));
        }
        carve(platformHalf, null);
    }

    int pos() { return dir * (progress - Arena.START); }

    boolean solid(int lane, int row) {
        byte c = cells[lane][row];
        return c == SOLID || c == CAP;
    }

    /** The special type that colours the caps, or null for Standard / Sike. */
    WallType capType() {
        for (WallType t : types) if (t.capMaterial != null) return t;
        return null;
    }

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

    // ------------------------------------------------------------------ holes

    /** Lay an MCC pattern of this wall's tier on the lanes; re-roll until it can be passed on the platform. */
    void carve(int platformHalf, String avoid) {
        List<WallPatterns.Pattern> pool = WallPatterns.tier(tier);
        for (int attempt = 0; attempt < 16; attempt++) {
            WallPatterns.Pattern p = pool.get(rnd.nextInt(pool.size()));
            if (p.name().equals(avoid) && pool.size() > 1) continue;
            lay(p, rnd.nextBoolean(), 1 + rnd.nextInt(2));
            if (types.contains(WallType.VENGEFUL)) makeNarrow();
            if (passableLanes(platformHalf) > 0) { patternName = p.name(); return; }
        }
        // nothing fitted the shrunken platform: keep the last pattern and cut one doorway in the middle
        patternName = patternName + "+door";
        int li = Arena.HALF + (rnd.nextInt(Math.max(1, platformHalf * 2 - 1)) - (platformHalf - 1));
        cells[li][0] = AIR;
        cells[li][1] = AIR;
    }

    private void lay(WallPatterns.Pattern p, boolean mirror, int padLeft) {
        int cols = WallPatterns.COLS;
        for (int li = 0; li < LANES; li++) {
            int c = Math.max(0, Math.min(cols - 1, li - padLeft));
            boolean pad = li - padLeft != c;
            int src = mirror ? cols - 1 - c : c;
            for (int row = 0; row < Arena.WALL_H; row++) {
                char ch = p.rows()[Arena.WALL_H - 1 - row].charAt(src);
                byte b = switch (ch) {
                    case '#' -> SOLID;
                    case 'P' -> pad ? SOLID : CAP;
                    case '<' -> pad ? AIR : (mirror ? LADDER_R : LADDER_L);
                    case '>' -> pad ? AIR : (mirror ? LADDER_L : LADDER_R);
                    default -> AIR;
                };
                cells[li][row] = b;
            }
        }
    }

    /** Lanes on the platform a player can get through: two open cells from the floor, or a raised slot to jump into. */
    int passableLanes(int platformHalf) {
        int n = 0;
        for (int lat = -platformHalf; lat <= platformHalf; lat++) {
            int li = lat + Arena.HALF;
            if (li < 0 || li >= LANES) continue;
            boolean stand = !solid(li, 0) && !solid(li, 1);
            boolean jump = solid(li, 0) && !solid(li, 1) && !solid(li, 2);
            if (stand || jump) n++;
        }
        return n;
    }

    /** Vengeful: keep a 9-lane piece of the pattern (open everywhere else); it is moved sideways later. */
    private void makeNarrow() {
        int start = (LANES - VENGEFUL_WIDTH) / 2;
        narrow = new byte[VENGEFUL_WIDTH][];
        for (int i = 0; i < VENGEFUL_WIDTH; i++) narrow[i] = cells[start + i].clone();
        shift = 0;
        placeNarrow();
    }

    private void placeNarrow() {
        for (byte[] col : cells) Arrays.fill(col, AIR);
        int start = (LANES - VENGEFUL_WIDTH) / 2 + shift;
        for (int i = 0; i < VENGEFUL_WIDTH; i++) {
            int li = start + i;
            if (li >= 0 && li < LANES) System.arraycopy(narrow[i], 0, cells[li], 0, Arena.WALL_H);
        }
    }

    // ------------------------------------------------------------------ movement

    /** Advance one tick. Calls back into the game after each single-cell step. */
    void tick(HoleInTheWallGame game) {
        if (removed) return;
        if (pause > 0) { pause--; return; }
        if (--cooldown > 0) return;
        int n = (waltz ? 2 : 1) * (turbo ? 2 : 1);
        for (int i = 0; i < n && !removed && pause == 0; i++) {
            if (danceBackDue()) { stepBack(game); break; }
            stepOnce(game);
        }
        cooldown = nextInterval();
    }

    private int nextInterval() {
        int iv = baseInterval;
        if (types.contains(WallType.TRICK)) iv = progress < ENTRY - 6 ? iv : 1;
        if (types.contains(WallType.RUSH)) iv = progress < ENTRY - 1 ? Math.max(1, iv / 3) : iv * 3 / 2 + 1;
        if (types.contains(WallType.SPRINT)) iv = ((progress / 3) % 2 == 0) ? iv * 2 : Math.max(1, iv / 2);
        if (types.contains(WallType.DANCE) && danceBacks < danceMax && progress >= ENTRY - 1) iv = iv + 2;
        if (waltz) iv = (progress / 2) % 3 == 2 ? iv * 3 : iv * 2;   // two beats, then a rest
        return Math.max(1, iv);
    }

    private boolean danceBackDue() {
        return types.contains(WallType.DANCE) && danceBacks < danceMax && progress > ENTRY && danceForward >= 2;
    }

    /** Dance: one step back towards where it came from (it pushes whoever slipped through early). */
    private void stepBack(HoleInTheWallGame game) {
        progress--;
        danceBacks++;
        danceForward = 0;
        game.onWallStep(this, -dir);
        if (danceBacks == 1) game.wallEvent(this, "dance");
    }

    private void stepOnce(HoleInTheWallGame game) {
        progress++;
        if (types.contains(WallType.DANCE) && progress > ENTRY) danceForward++;
        if (narrow != null && progress < ENTRY - 1) trackTarget(game);
        game.onWallStep(this, dir);
        if (types.contains(WallType.SANDWICH) && progress >= Arena.START) {
            wave.crossed = true;
            removed = true;
            game.wallEvent(this, "sandwich");
            return;
        }
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
        if (types.contains(WallType.TRICK) && !tricked && progress == ENTRY + 1) {
            tricked = true;
            removed = true;
            game.wallEvent(this, "trick");
            return;
        }
        if (types.contains(WallType.ENDER) && !endered && progress == ENTRY - 1) {
            endered = true;
            dir = -dir; // mirrors the position to the other side
            game.wallEvent(this, "ender");
            return;
        }
        if (types.contains(WallType.SIDESWIPE) && !swiped && progress == ENTRY - 2) {
            swiped = true;
            game.wallEvent(this, "sideswipe-from");
            axisX = !axisX;            // a quarter turn: same distance out, from a neighbouring side
            dir = rnd.nextBoolean() ? 1 : -1;
            game.wallEvent(this, "sideswipe");
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
            carve(game.platformHalf(), patternName);
            game.wallEvent(this, "morph");
        }
        if (freezeAt.remove(progress)) {
            pause = 15 + rnd.nextInt(20);
            game.wallEvent(this, "frozen");
        }
    }

    /** Vengeful: slide one lane towards the chosen player's lateral position. */
    private void trackTarget(HoleInTheWallGame game) {
        double lat = game.vengefulTarget(this);
        if (Double.isNaN(lat)) return;
        int want = (int) Math.floor(lat);
        int max = Arena.HALF - VENGEFUL_WIDTH / 2;
        want = Math.max(-max, Math.min(max, want));
        if (want > shift) shift++;
        else if (want < shift) shift--;
        else return;
        placeNarrow();
    }
}
