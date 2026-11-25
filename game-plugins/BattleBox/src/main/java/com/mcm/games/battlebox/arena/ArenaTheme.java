package com.mcm.games.battlebox.arena;

/**
 * Base class for a Battle Box arena theme (pure Java, no Bukkit). {@link #generate} writes one complete arena
 * copy into a {@link Grid}: floor, painted walls, ceiling, both spawn rooms with gates, the theme's structured
 * layout (forecourt, lane buildings, middle-third cover) and the centre dais. The layout is written for side A's
 * half and mirrored to side B by a 180-degree rotation around the centre ({@link #sym}), so both halves are fair.
 * A final post-pass fixes fence/pane/wall connections and drops unsupported carpets.
 */
public abstract class ArenaTheme {
    public enum Face { NORTH, SOUTH, WEST, EAST }

    protected Grid g;
    protected ArenaGeo geo;
    protected int R, H, W, CEIL;
    protected long seed;
    /** Team colour names for the two sides, e.g. "red", "light_blue". */
    protected String teamA = "red", teamB = "blue";
    private String neutral = "white_concrete";
    private int removedCarpets;

    public abstract String id();
    public abstract String displayName();
    public abstract String tagline();
    /** One line per layout section, for the README / admin command. */
    public abstract String layoutNotes();

    // ------------------------------------------------------------------ hooks
    protected abstract String floorAt(int x, int z);
    protected String subFloorAt(int x, int z) { return "smooth_stone"; }
    /** Inner wall surface. u runs along the wall (-W..W), y from -1 to H. */
    protected abstract String wallAt(Face f, int u, int y);
    /** Ceiling layer (y = CEIL) or null for none. */
    protected abstract String ceilingAt(int x, int z);
    /** {floor, wall, ceiling, trim, light} for spawn rooms. */
    protected abstract String[] roomPalette();
    /** The structured layout for side A's half (use the sym* helpers). */
    protected abstract void layout();
    protected void roof() { }
    protected void roomDecor(boolean sideB) { }
    /** Outer skin of the arena walls (only seen from outside / in renders). */
    protected String exteriorAt(Face f, int u, int y) { return "smooth_stone"; }
    /** Block for the dais steps (stairs material) and dais body. */
    protected String daisStairs() { return "smooth_quartz"; }
    protected String daisBody() { return "smooth_quartz"; }

    // ------------------------------------------------------------------ geometry hooks (overridden by imported arenas)

    public boolean isImported() { return false; }

    public int centreX() { return 0; }
    public int centreY(ArenaGeo geo) { return geo.CENTRE_Y; }
    public int centreZ() { return 0; }

    public boolean isCentreCell(ArenaGeo geo, int x, int y, int z) {
        return geo.isCentreCell(x, y, z);
    }

    public double[] spawn(ArenaGeo geo, boolean sideB, int i) {
        return geo.spawn(sideB, i);
    }

    public double[] overview(ArenaGeo geo, boolean sideB, int i) {
        return geo.overview(sideB, i);
    }

    public boolean inSpectatorBox(ArenaGeo geo, double x, double y, double z) {
        return geo.inSpectatorBox(x, y, z);
    }

    public int voidY() { return -8; }

    public double[] previewLocation(ArenaGeo geo) {
        return new double[]{0.5, 1.0, -(geo.R - 4) + 0.5, 0.0, 5.0};
    }

    public int[] minBounds(ArenaGeo geo) { return new int[]{geo.minX, geo.minY, geo.minZ}; }
    public int[] maxBounds(ArenaGeo geo) { return new int[]{geo.maxX, geo.maxY, geo.maxZ}; }

    // ------------------------------------------------------------------ template

    public final Grid generate(ArenaGeo geo, long seed, String teamA, String teamB) {
        return generate(geo, seed, teamA, teamB, "white_concrete");
    }

    /** @param neutralCentre block the 9 centre cells start filled with (white concrete / white wool). */
    public Grid generate(ArenaGeo geo, long seed, String teamA, String teamB, String neutralCentre) {
        this.geo = geo;
        this.R = geo.R; this.H = geo.H; this.W = geo.W; this.CEIL = geo.CEIL;
        this.seed = seed;
        this.teamA = teamA == null ? "red" : teamA;
        this.teamB = teamB == null ? "blue" : teamB;
        this.neutral = neutralCentre == null ? "white_concrete" : neutralCentre;
        this.g = geo.newGrid();
        floorAndWalls();
        roof();
        spawnRoom(false);
        spawnRoom(true);
        layout();
        centre();
        exterior();
        gatePassage(false);
        gatePassage(true);
        removedCarpets = g.postProcess();
        Grid out = g;
        g = null;
        return out;
    }

    /** Carpets dropped by the last post-pass (should be 0). */
    public int removedCarpets() { return removedCarpets; }

    private void floorAndWalls() {
        for (int x = -W; x <= W; x++)
            for (int z = -W; z <= W; z++) {
                set(x, -2, z, "stone");
                set(x, -1, z, subFloorAt(x, z));
                if (Math.abs(x) <= R && Math.abs(z) <= R) set(x, 0, z, floorAt(x, z));
                String c = ceilingAt(x, z);
                if (c != null) set(x, CEIL, z, c);
            }
        for (int u = -W; u <= W; u++)
            for (int y = -1; y <= H; y++) {
                set(u, y, -W, wallAt(Face.NORTH, u, y));
                set(u, y, W, wallAt(Face.SOUTH, u, y));
                set(-W, y, u, wallAt(Face.WEST, u, y));
                set(W, y, u, wallAt(Face.EAST, u, y));
            }
    }

    private void spawnRoom(boolean b) {
        String[] p = roomPalette();
        int sg = b ? 1 : -1;
        String team = b ? teamB : teamA;
        for (int x = -geo.ROOM_HALF - 1; x <= geo.ROOM_HALF + 1; x++)
            for (int a = geo.ROOM_IN; a <= geo.ROOM_OUT; a++)
                for (int y = -1; y <= geo.ROOM_CEIL; y++) {
                    int z = sg * a;
                    boolean shell = Math.abs(x) == geo.ROOM_HALF + 1 || a == geo.ROOM_OUT || y <= 0 || y == geo.ROOM_CEIL;
                    if (!shell) { set(x, y, z, null); continue; }
                    String bl = y == geo.ROOM_CEIL ? p[2] : y <= 0 ? p[0] : p[1];
                    if (y == 0 && Math.abs(x) <= 1) bl = team + "_concrete";
                    if (y == 0 && Math.abs(x) == 2) bl = "white_concrete";
                    set(x, y, z, bl);
                }
        for (int a = geo.ROOM_IN + 1; a <= geo.ROOM_OUT - 1; a += 2) {
            set(-3, geo.ROOM_CEIL, sg * a, p[4]);
            set(3, geo.ROOM_CEIL, sg * a, p[4]);
        }
        String toArena = b ? "north" : "south";
        for (int x = -4; x <= 4; x += 2)
            set(x, 3, sg * (geo.ROOM_OUT - 1), team + "_wall_banner[facing=" + toArena + "]");
        for (int a = geo.ROOM_IN + 1; a <= geo.ROOM_OUT - 2; a++) {
            set(-geo.ROOM_HALF, 1, sg * a, States.stairs(p[3], "west", false));
            set(geo.ROOM_HALF, 1, sg * a, States.stairs(p[3], "east", false));
        }
        set(-geo.ROOM_HALF, 1, sg * (geo.ROOM_OUT - 1), "barrel[facing=up]");
        set(geo.ROOM_HALF, 1, sg * (geo.ROOM_OUT - 1), "barrel[facing=up]");
        // gate (team glass) and its hazard frame in the arena wall
        for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
            for (int y = 1; y <= geo.GATE_TOP; y++) set(x, y, sg * W, team + "_stained_glass");
        for (int x = -geo.GATE_HALF - 1; x <= geo.GATE_HALF + 1; x++) set(x, geo.GATE_TOP + 1, sg * W, "yellow_concrete");
        for (int y = 0; y <= geo.GATE_TOP + 1; y++) {
            set(-geo.GATE_HALF - 1, y, sg * W, (y & 1) == 0 ? "black_concrete" : "yellow_concrete");
            set(geo.GATE_HALF + 1, y, sg * W, (y & 1) == 0 ? "yellow_concrete" : "black_concrete");
        }
        set(0, geo.GATE_TOP + 2, sg * W, team + "_glazed_terracotta");
        for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++) set(x, 0, sg * W, team + "_concrete");
        roomDecor(b);
    }

    /**
     * The 2 rows on each side of the gate (spawn room front row and the first arena row) must be open so the
     * gate really leads somewhere once its glass is removed.
     */
    private void gatePassage(boolean b) {
        int sg = b ? 1 : -1;
        for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
            for (int y = 1; y <= geo.GATE_TOP; y++) {
                set(x, y, sg * (W + 1), null);
                set(x, y, sg * R, null);
            }
    }

    /** Skin around the outside of the arena walls, leaving the spawn rooms alone. */
    private void exterior() {
        int o = W + 1;
        int rh = geo.ROOM_HALF + 1;
        for (int u = -o; u <= o; u++)
            for (int y = -1; y <= H; y++) {
                boolean room = Math.abs(u) <= rh && y <= geo.ROOM_CEIL;
                if (!room && g.isAir(u, y, -o)) set(u, y, -o, exteriorAt(Face.NORTH, u, y));
                if (!room && g.isAir(u, y, o)) set(u, y, o, exteriorAt(Face.SOUTH, u, y));
                if (g.isAir(-o, y, u)) set(-o, y, u, exteriorAt(Face.WEST, u, y));
                if (g.isAir(o, y, u)) set(o, y, u, exteriorAt(Face.EAST, u, y));
            }
    }

    /**
     * Centre dais: two stair steps (rings 4 and 3) up to a hazard-striped ring (2) around the 3x3 cells, which
     * start filled with the neutral block. Lamp posts sit on the dais corners.
     */
    private void centre() {
        int d = geo.DAIS;
        for (int x = -d; x <= d; x++)
            for (int z = -d; z <= d; z++)
                for (int y = 1; y <= 6; y++) set(x, y, z, null);
        String st = daisStairs(), body = daisBody();
        for (int x = -d; x <= d; x++)
            for (int z = -d; z <= d; z++) {
                int m = Math.max(Math.abs(x), Math.abs(z));
                boolean corner = Math.abs(x) == Math.abs(z);
                String face = Math.abs(x) > Math.abs(z) ? (x > 0 ? "west" : "east") : (z > 0 ? "north" : "south");
                if (m == d) {
                    set(x, 1, z, corner ? body : States.stairs(st, face, false));
                } else if (m == d - 1) {
                    set(x, 1, z, body);
                    set(x, 2, z, corner ? body : States.stairs(st, face, false));
                } else if (m == 2) {
                    set(x, 1, z, body);
                    // hazard ring; its four corners are sea lanterns (the lantern frame around the cells)
                    set(x, geo.RING_Y, z, corner ? "sea_lantern" : ((x + z) & 1) == 0 ? "yellow_concrete" : "black_concrete");
                } else {
                    // the 9 objective cells sit INSIDE the dais top, flush with the ring, where the sea lanterns
                    // used to be; the lanterns move underneath (they glow through when a cell is broken)
                    set(x, 1, z, "sea_lantern");
                    set(x, geo.CENTRE_Y, z, neutral);
                }
            }
        // edge ring of the floor around the dais
        for (int x = -d - 1; x <= d + 1; x++)
            for (int z = -d - 1; z <= d + 1; z++)
                if (Math.max(Math.abs(x), Math.abs(z)) == d + 1) set(x, 0, z, "smooth_quartz");
        // corner lamp posts (decor) on the outer corners
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                set(sx * d, 2, sz * d, "andesite_wall");
                set(sx * d, 3, sz * d, "lantern");
                set(sx * (d - 1), 3, sz * (d - 1), "yellow_candle[candles=3,lit=true]");
            }
        // register the dais steps (one lane up the middle of each side) for the staircase check
        g.addLane(new Grid.Lane("dais N", 0, -d, 0, 0, 1, 2));
        g.addLane(new Grid.Lane("dais S", 0, d, 0, 0, -1, 2));
        g.addLane(new Grid.Lane("dais W", -d, 0, 0, 1, 0, 2));
        g.addLane(new Grid.Lane("dais E", d, 0, 0, -1, 0, 2));
    }

    // ------------------------------------------------------------------ painted sky

    /**
     * Painted-sky wall pixel: light-blue concrete with chunky white block clouds (and a pixel sun on the
     * south wall). Returns the sky block for (face, u, y); low = lowest sky row.
     */
    protected final String sky(Face f, int u, int y, int low) {
        if (f == Face.SOUTH) {
            int du = u + 9, dy = y - (H - 4);
            int d2 = du * du + dy * dy;
            if (d2 <= 2) return "yellow_concrete";
            if (d2 <= 5) return "shroomlight";
            if (d2 <= 8 && ((du + dy) & 1) == 0) return "yellow_terracotta";
        }
        int cu = Math.floorDiv(u, 2);
        double n = Noise.fbm2(cu * 0.38 + f.ordinal() * 37, y * 0.42, seed + 99);
        double bias = (y - low) / (double) Math.max(1, H - 1 - low);
        if (y > low && n + bias * 0.1 > 0.71) return n > 0.76 ? "white_concrete" : "white_wool";
        if (y > low && n + bias * 0.1 > 0.69) return "white_concrete_powder";
        return "light_blue_concrete";
    }

    /** Two-colour checker frieze block. */
    protected static String checker(int u, int y, String a, String b) {
        return ((u + y) & 1) == 0 ? a : b;
    }

    // ------------------------------------------------------------------ helpers

    /** A block source by position (for weighted palettes and patterns). */
    protected interface Mat { String at(int x, int y, int z); }

    protected final Mat m(String s) { return (x, y, z) -> s; }

    protected final Mat m(Pal p, long salt) { return (x, y, z) -> p.at(x, y, z, seed + salt); }

    protected final void set(int x, int y, int z, String b) { g.set(x, y, z, b); }

    protected final String get(int x, int y, int z) { return g.get(x, y, z); }

    /** Set a block and its 180-degree mirror (-x, y, -z). */
    protected final void sym(int x, int y, int z, String b) {
        g.set(x, y, z, b);
        g.set(-x, y, -z, States.rot180(b));
    }

    protected final void symM(int x, int y, int z, Mat m) {
        g.set(x, y, z, m.at(x, y, z));
        g.set(-x, y, -z, States.rot180(m.at(-x, y, -z)));
    }

    protected final void fill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, b);
    }

    protected final void symFill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) sym(x, y, z, b);
    }

    protected final void symFill(int x1, int y1, int z1, int x2, int y2, int z2, Mat m) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) symM(x, y, z, m);
    }

    protected final void symFill(int x1, int y1, int z1, int x2, int y2, int z2, Pal p, long salt) {
        symFill(x1, y1, z1, x2, y2, z2, m(p, salt));
    }

    /** Clear a box (mirrored). */
    protected final void symClear(int x1, int y1, int z1, int x2, int y2, int z2) {
        symFill(x1, y1, z1, x2, y2, z2, (String) null);
    }

    /** Perimeter walls of a footprint from y1 to y2 (mirrored); corners use {@code post}. */
    protected final void symShell(int x1, int z1, int x2, int z2, int y1, int y2, Mat wall, Mat post) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean ex = x == x1 || x == x2, ez = z == z1 || z == z2;
                if (!ex && !ez) continue;
                for (int y = y1; y <= y2; y++) symM(x, y, z, (ex && ez && post != null) ? post : wall);
            }
    }

    /** A room: perimeter walls y1..y2 with corner posts, interior cleared (mirrored). */
    protected final void symRoom(int x1, int z1, int x2, int z2, int y1, int y2, Mat wall, Mat post) {
        symShell(x1, z1, x2, z2, y1, y2, wall, post);
        for (int x = x1 + 1; x < x2; x++)
            for (int z = z1 + 1; z < z2; z++)
                for (int y = y1; y <= y2; y++) sym(x, y, z, (String) null);
    }

    /** Floor/roof plate at y with an edge material (mirrored). */
    protected final void symPlate(int x1, int z1, int x2, int z2, int y, Mat edge, Mat inner) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++)
                symM(x, y, z, (x == x1 || x == x2 || z == z1 || z == z2) ? edge : inner);
    }

    /**
     * Gabled roof over a footprint starting at baseY: the ridge runs along x (alongX) or z. Stairs on the slopes,
     * {@code fill} in the gable ends, a ridge of {@code ridge}. Mirrored.
     */
    protected final void symGable(int x1, int z1, int x2, int z2, int baseY, boolean alongX, String stairMat, Mat fill, String ridge) {
        int span = alongX ? (z2 - z1) : (x2 - x1);
        int half = span / 2;
        for (int i = 0; i <= half; i++) {
            int y = baseY + i;
            for (int a = (alongX ? x1 : z1) - 1; a <= (alongX ? x2 : z2) + 1; a++) {
                boolean end = a < (alongX ? x1 : z1) || a > (alongX ? x2 : z2);
                int lo = (alongX ? z1 : x1) + i - 1, hi = (alongX ? z2 : x2) - i + 1;
                boolean ridgeRow = lo + 1 >= hi - 1;
                if (end) continue;
                if (ridgeRow) {
                    for (int b = lo + 1; b <= hi - 1; b++) put(alongX, a, y, b, ridge);
                    continue;
                }
                put(alongX, a, y, lo, States.stairs(stairMat, alongX ? "south" : "east", false));
                put(alongX, a, y, hi, States.stairs(stairMat, alongX ? "north" : "west", false));
                boolean gableEnd = a == (alongX ? x1 : z1) || a == (alongX ? x2 : z2);
                for (int b = lo + 1; b <= hi - 1; b++)
                    if (gableEnd) { if (alongX) symM(a, y, b, fill); else symM(b, y, a, fill); }
                    else put(alongX, a, y, b, null);
            }
        }
    }

    private void put(boolean alongX, int a, int y, int b, String s) {
        if (alongX) sym(a, y, b, s); else sym(b, y, a, s);
    }

    /** Set only where the cell is currently air (mirrored). */
    protected final void symIfAir(int x, int y, int z, String b) {
        if (States.isAir(get(x, y, z))) set(x, y, z, b);
        if (States.isAir(get(-x, y, -z))) set(-x, y, -z, States.rot180(b));
    }

    /** Railing around the edge of a deck at height y (only where air). */
    protected final void symRail(int x1, int z1, int x2, int z2, int y, String rail) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++)
                if (x == x1 || x == x2 || z == z1 || z == z2) symIfAir(x, y, z, rail);
    }

    private static final String[] FACES = {"north", "east", "south", "west"};
    private static final int[][] FD = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private static int faceIdx(String f) {
        for (int i = 0; i < 4; i++) if (FACES[i].equals(f)) return i;
        throw new IllegalArgumentException(f);
    }

    /**
     * Staircase helper (mirrored + registered for validation). The first step sits on top of the floor block at
     * (x, floorY, z) — i.e. the step itself is at floorY + 1 — and each further step is one block further in
     * {@code facing} and one higher, so exactly {@code topY - floorY} steps are placed; the last step is at topY,
     * flush with the upper level whose first block (the landing) is the next block along. {@code width} lanes are
     * laid side by side to the right of the climbing direction. Under the steps is filled with {@code support};
     * headroom (2 blocks) above every step and the landing is cleared, cutting the stairwell into upper floors.
     * The landing block is placed with {@code support} if it is not already a solid block.
     */
    protected final void staircase(String name, int x, int z, int floorY, String facing, int topY, int width,
                                   String stairMat, String support) {
        int f = faceIdx(facing);
        int dx = FD[f][0], dz = FD[f][1];
        int px = FD[(f + 1) & 3][0], pz = FD[(f + 1) & 3][1];   // to the right of the climbing direction
        int n = topY - floorY;
        for (int w = 0; w < width; w++) {
            int lx = x + px * w, lz = z + pz * w;
            for (int i = 1; i <= n; i++) {
                int cx = lx + dx * (i - 1), cz = lz + dz * (i - 1), cy = floorY + i;
                for (int y = floorY + 1; y < cy; y++) sym(cx, y, cz, support);
                sym(cx, cy, cz, States.stairs(stairMat, facing, false));
                sym(cx, cy + 1, cz, (String) null);
                sym(cx, cy + 2, cz, (String) null);
                // stepping up onto the next step lifts the head over THIS step to cy + 3.8: 3 free cells
                if (i < n) sym(cx, cy + 3, cz, (String) null);
            }
            int ex = lx + dx * n, ez = lz + dz * n, ey = floorY + n;
            if (Shapes.standTop(get(ex, ey, ez)) < 0.99 || Shapes.standTop(get(ex, ey, ez)) > 1.01) sym(ex, ey, ez, support);
            sym(ex, ey + 1, ez, (String) null);
            sym(ex, ey + 2, ez, (String) null);
            // approach cell headroom (3 cells: the head rises while stepping onto step 1)
            sym(lx - dx, floorY + 1, lz - dz, (String) null);
            sym(lx - dx, floorY + 2, lz - dz, (String) null);
            sym(lx - dx, floorY + 3, lz - dz, (String) null);
            g.addLane(new Grid.Lane(name + " A" + w, lx, lz, floorY, dx, dz, n));
            g.addLane(new Grid.Lane(name + " B" + w, -lx, -lz, floorY, -dx, -dz, n));
        }
    }

    /**
     * Register an intended spawn-building exit for side A (and its 180-degree copy for side B): the box of cells
     * x1..x2, y1..y2, z1..z2 is the doorway. The offline route check closes all other exits and requires the
     * centre to be reachable through this one, and requires the centre to be unreachable with all exits closed.
     */
    protected final void exitDoor(String name, int x1, int y1, int z1, int x2, int y2, int z2) {
        g.addRoute(new Grid.Route(name, true, false, x1, y1, z1, x2, y2, z2));
        g.addRoute(new Grid.Route(name, true, true, -x1, y1, -z1, -x2, y2, -z2));
    }

    /** Register a lane waypoint (standing spot, feet at y) for side A and its copy for side B. */
    protected final void waypoint(String name, int x, int y, int z) {
        g.addRoute(new Grid.Route(name, false, false, x, y, z, x, y, z));
        g.addRoute(new Grid.Route(name, false, true, -x, y, -z, -x, y, -z));
    }

    /** Ladder from y1 to y2 at (x, z) facing {@code facing} (away from its wall), mirrored. */
    protected final void symLadder(int x, int z, int y1, int y2, String facing) {
        for (int y = y1; y <= y2; y++) sym(x, y, z, "ladder[facing=" + facing + "]");
    }

    protected final double rnd(int x, int y, int z, long salt) { return Noise.hash01(x, y, z, seed * 31 + salt); }

    /** Hanging lantern: chain from (x, top, z) down len blocks, lantern below it. Mirrored. */
    protected final void symHangingLantern(int x, int top, int z, int len, String lantern) {
        for (int i = 0; i < len; i++) sym(x, top - i, z, "chain[axis=y]");
        sym(x, top - len, z, lantern + "[hanging=true]");
    }

    /** Facing toward +z / -z for side A props (mirrored automatically). */
    protected static final String S = "south", N = "north", E = "east", Wst = "west";
}
