package com.mcm.lobby;

import java.util.ArrayList;
import java.util.List;

/**
 * The fishing harbour on the south beach (around x 0..70, z 148..214, where the sand path from the plaza ends).
 * One composition, built in this order:
 * <ol>
 *   <li>a cove carved into the beach with an irregular (noise) shoreline: a wide sand strip at walking height,
 *       then water that deepens steadily toward the open sea (sand flats, then sand/gravel, then gravel/clay);</li>
 *   <li>a natural rock breakwater curving out from the eastern shore, highest and mossy at its root, sinking to
 *       wave-washed boulders at its tip, with a rubble skirt under water and a small harbour lantern at the end;</li>
 *   <li>a spruce jetty: one main arm straight out from the end of the path, an L-shaped head reaching east, and a
 *       short finger pier on the west, on log pilings, with bollards, mooring chains and a few lanterns;</li>
 *   <li>three boats moored at it (two rowing boats and a sailing boat) and a small boathouse on the east shore;</li>
 *   <li>seagrass in the shallows and kelp beds in the deeper water, in patches, plus a few boulders on the west beach.</li>
 * </ol>
 * The harbour has no functional parts (no spawns, portals or NPCs). Every random choice is a noise / hash lookup,
 * so the build is identical on every run.
 */
final class HubHarbour {
    static final int SEA = HubLayout.SEA;
    static final int DECK = SEA + 1;               // jetty deck block y (stand at 64)
    static final int X0 = 0, X1 = 70, Z0 = 146, Z1 = 214;   // sculpted box

    // jetty layout (deck cells)
    static final int JX = 30;                      // main arm centre x (3 wide: 29..31)
    static final int J_ROOT = 154, J_END = 188;    // main arm z range
    static final int HEAD_Z0 = 186, HEAD_Z1 = 189, HEAD_X1 = 43;  // L-head running east
    static final int FINGER_Z = 172, FINGER_X0 = 22;              // west finger pier (z 172..173)

    final HubGenerator g;
    final Draw d;
    final HNoise nz;

    HubHarbour(HubGenerator g) { this.g = g; this.d = g.d; this.nz = new HNoise(g.seed * 5 + 17); }

    void build() {
        coast();
        breakwater();
        jetty();
        boats();
        boathouse();
        shoreDetails();
        g.reserveBox(X0, Z0 + 4, X1, Z1);
    }

    // ------------------------------------------------------------------------------------------------ coastline
    /** Shoreline z for a column x: a gently wavy coast with a cove pushed inland around the jetty. */
    double coastZ(double x) {
        double z = 168 + 2.6 * nz.noise2(x * 0.07, 3.1) + 1.1 * nz.noise2(x * 0.21 + 7, 1.7);
        z -= 9.0 * Math.exp(-Math.pow((x - 31) / 10.5, 2));      // the cove
        z += 3.0 * Math.exp(-Math.pow((x - 9) / 6.0, 2));        // a low sandy point on the west side
        return z;
    }

    /** Signed distance (blocks) seaward of the coastline: negative on land. */
    double seaward(int x, int z) { return z + 0.5 - coastZ(x + 0.5) + 0.8 * nz.noise2(x * 0.19, z * 0.19); }

    void coast() {
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            int old = g.hAt(x, z);
            double t = seaward(x, z);
            int target;
            if (t < 0) target = DECK + Math.max(0, (int) Math.ceil((-t - 7) * 0.3));          // flat walking strip, then rising sand
            else target = SEA - 1 - (int) Math.floor(t * 0.3 + 0.6 + 0.7 * nz.noise2(x * 0.08 + 3, z * 0.08));
            target = Math.max(target, SEA - 9);
            double edge = Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0, Z1 - z)) / 10.0;
            double w = HubGenerator.smooth(edge);
            int nh = (int) Math.round(old + (target - old) * w);
            boolean raiseSpit = t < 0 && nh > old && old >= SEA - 2 && nh <= DECK;   // the sandy point may build up a little
            if (nh < old || raiseSpit) g.reshape(x, z, nh);
            if (g.hAt(x, z) < SEA) seabed(x, z);
        }
    }

    /** Sea floor by depth: sand flats, then sand with gravel patches, then gravel and clay; seagrass/kelp in patches. */
    void seabed(int x, int z) {
        int top = g.hAt(x, z);
        int depth = SEA - top;                     // water blocks above the floor
        double p = nz.noise2(x * 0.11 + 40, z * 0.11 - 9);
        String floor = depth <= 2 ? "sand" : depth <= 4 ? (p > 0.35 ? "gravel" : "sand") : (p > 0.1 ? "gravel" : p < -0.45 ? "clay" : "sand");
        d.set(x, top, z, floor);
        for (int y = top + 1; y <= SEA; y++) d.set(x, y, z, "water");
        double patch = nz.noise2(x * 0.09 - 21, z * 0.09 + 13);
        double r = d.rnd(x, 11, z, 121);
        if (depth >= 2 && depth <= 4 && patch > 0.25 && r < 0.55) {
            if (r < 0.12 && depth >= 3) { d.set(x, top + 1, z, "tall_seagrass[half=lower]"); d.set(x, top + 2, z, "tall_seagrass[half=upper]"); }
            else d.set(x, top + 1, z, "seagrass");
        } else if (depth >= 5 && patch < -0.35 && r < 0.3) {
            int hgt = 1 + (int) (d.rnd(x, 12, z, 122) * Math.min(3, depth - 3));
            for (int y = top + 1; y < top + hgt; y++) d.set(x, y, z, "kelp_plant");
            d.set(x, top + hgt, z, "kelp[age=25]");
        }
    }

    // ------------------------------------------------------------------------------------------------ breakwater
    static final double[][] ARM = {{62, 162}, {58, 171}, {54, 180}, {49, 189}, {44, 196}};

    /** Distance to the breakwater spine and the spine parameter s (0 root .. 1 tip). */
    double[] armDist(double x, double z) {
        double best = 1e9, bs = 0, total = 0;
        double[] len = new double[ARM.length - 1];
        for (int i = 0; i < len.length; i++) { len[i] = Math.hypot(ARM[i + 1][0] - ARM[i][0], ARM[i + 1][1] - ARM[i][1]); total += len[i]; }
        double acc = 0;
        for (int i = 0; i < len.length; i++) {
            double ax = ARM[i][0], az = ARM[i][1], vx = ARM[i + 1][0] - ax, vz = ARM[i + 1][1] - az;
            double t = Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / (vx * vx + vz * vz)));
            double dd = Math.hypot(x - (ax + vx * t), z - (az + vz * t));
            if (dd < best) { best = dd; bs = (acc + len[i] * t) / total; }
            acc += len[i];
        }
        return new double[]{best, bs};
    }

    void breakwater() {
        for (int x = 34; x <= X1; x++) for (int z = 155; z <= 205; z++) {
            double[] a = armDist(x + 0.5, z + 0.5);
            double s = a[1];
            double r = 5.2 - 2.6 * s + 0.9 * nz.noise2(x * 0.23 + 5, z * 0.23);
            double dd = a[0];
            int old = g.hAt(x, z);
            if (dd < r) {
                double crown = 1 - (dd / r) * (dd / r);
                int top = DECK + (int) Math.round((3.6 - 3.0 * s) * crown + 0.9 * nz.noise2(x * 0.31, z * 0.31 + 2) - 0.4);
                if (s > 0.8 && crown < 0.5) top = Math.min(top, SEA);                 // the tip breaks up into wave-washed stones
                top = Math.max(top, Math.min(old, SEA));
                if (top <= old && old >= SEA) continue;                                 // already land at least this high
                for (int y = Math.min(old, SEA - 6); y <= top; y++) d.set(x, y, z, rock(x, y, z, top, s));
                for (int y = top + 1; y <= SEA; y++) d.set(x, y, z, "water");
                for (int y = Math.max(top + 1, SEA + 1); y <= SEA + 5; y++) d.air(x, y, z);
                if (top >= SEA) g.paved(x, z, top);
                else { g.h[g.idx(x, z)] = top; g.reserve(x, z); }
            } else if (dd < r + 2.6 && old < SEA - 1) {
                // rubble skirt under water
                int top = Math.min(SEA - 1, Math.max(old, SEA - 2 - (int) Math.round((dd - r) * 1.2)));
                if (top <= old) continue;
                for (int y = old; y <= top; y++) d.set(x, y, z, d.pick(x, y, z, 131, "gravel", 3, "cobblestone", 2, "mossy_cobblestone", 1, "andesite", 1));
                for (int y = top + 1; y <= SEA; y++) d.set(x, y, z, "water");
                g.h[g.idx(x, z)] = top;
                g.reserve(x, z);
            }
        }
        // harbour lantern on the last big boulder
        int lx = 45, lz = 194;
        int t = floorY(lx, lz, SEA + 8);
        if (t < SEA) { for (int y = t + 1; y <= SEA; y++) d.set(lx, y, lz, "cobblestone"); t = SEA; }
        d.set(lx, t + 1, lz, "cobblestone_wall");
        d.set(lx, t + 2, lz, "cobblestone_wall");
        d.set(lx, t + 3, lz, "lantern[hanging=false]");
    }

    /** Rock of the breakwater: big coherent patches (low-frequency noise), moss on the upper, older root. */
    String rock(int x, int y, int z, int top, double s) {
        double p = nz.noise2(x * 0.17 + 11, (z + y * 0.7) * 0.17);
        if (y == top && top > DECK && s < 0.55 && nz.noise2(x * 0.13 - 3, z * 0.13 + 8) > 0.05) return d.pick(x, y, z, 132, "moss_block", 2, "mossy_cobblestone", 3);
        if (y < SEA - 1) return p > 0.2 ? "mossy_cobblestone" : "cobblestone";
        if (p > 0.35) return "andesite";
        if (p > -0.1) return d.pick(x, y, z, 133, "stone", 4, "cobblestone", 1);
        if (p > -0.45) return "cobblestone";
        return "tuff";
    }

    // ------------------------------------------------------------------------------------------------ jetty
    final List<int[]> deckCells = new ArrayList<>();

    void deck(int x, int z, String board) {
        int old = g.hAt(x, z);
        d.set(x, DECK, z, board);
        for (int y = DECK + 1; y <= Math.max(old, DECK) + 3; y++) d.air(x, y, z);
        g.paved(x, z, DECK);
        deckCells.add(new int[]{x, z});
    }

    /** Highest solid block at or below {@code fromY}, looking through water and sea plants. */
    int floorY(int x, int z, int fromY) {
        for (int y = fromY; y > SEA - 16; y--) {
            String id = d.id(x, y, z);
            if (id == null || id.equals("water") || id.contains("seagrass") || id.startsWith("kelp")) continue;
            return y;
        }
        return SEA - 16;
    }

    /** Spruce pile from the sea floor up to {@code toY}. */
    void pile(int x, int z, int toY) {
        int f = floorY(x, z, toY);
        for (int y = f + 1; y <= toY; y++) d.set(x, y, z, "spruce_log[axis=y]");
    }

    /** Deck block: spruce boards, stripped-log stringers along the edges of each arm. */
    static String board(int row, boolean edgeBeam, String axis) {
        return edgeBeam ? "stripped_spruce_log[axis=" + axis + "]" : "spruce_planks";
    }

    void jetty() {
        // main arm: boards across it, edge beams along it
        for (int z = J_ROOT; z <= J_END; z++) for (int x = JX - 1; x <= JX + 1; x++) {
            if (g.hAt(x, z) > DECK) continue;                         // still dry sand above deck height: the path meets the deck there
            deck(x, z, board(z, x != JX, "z"));
        }
        // L-head running east
        for (int x = JX + 2; x <= HEAD_X1; x++) for (int z = HEAD_Z0; z <= HEAD_Z1; z++)
            deck(x, z, board(x, z == HEAD_Z0 || z == HEAD_Z1, "x"));
        // west finger pier
        for (int x = FINGER_X0; x <= JX - 2; x++) for (int z = FINGER_Z; z <= FINGER_Z + 1; z++)
            deck(x, z, board(x, false, "x"));
        // pilings: under the deck edges every 4 blocks where there is water; bollards on top at the corners
        for (int[] c : deckCells) {
            int x = c[0], z = c[1];
            boolean edge = !isDeck(x - 1, z) || !isDeck(x + 1, z) || !isDeck(x, z - 1) || !isDeck(x, z + 1);
            if (!edge || floorY(x, z, DECK - 1) >= SEA) continue;            // on the sand: no pile needed
            boolean onGrid = (x == JX - 1 || x == JX + 1) ? Math.floorMod(z - J_ROOT, 4) == 0 : Math.floorMod(x - JX, 4) == 0;
            if (onGrid) pile(x, z, DECK - 1);
        }
        int[][] bollards = {{JX - 1, 164}, {JX + 1, 176}, {JX - 1, 184}, {FINGER_X0, FINGER_Z}, {FINGER_X0, FINGER_Z + 1},
                {JX - 1, HEAD_Z1}, {36, HEAD_Z1}, {HEAD_X1, HEAD_Z0}, {HEAD_X1, HEAD_Z1}, {39, HEAD_Z0}};
        for (int[] b : bollards) {
            pile(b[0], b[1], DECK - 1);
            d.set(b[0], DECK, b[1], "spruce_log[axis=y]");
            d.set(b[0], DECK + 1, b[1], "spruce_log[axis=y]");
        }
        // lantern posts: few, deliberate (root, the finger, the head corner)
        int[][] lamps = {{JX + 1, J_ROOT + 3}, {JX - 1, 172 - 2}, {HEAD_X1, HEAD_Z1 - 1}, {JX + 1, 182}};
        for (int[] l : lamps) {
            d.set(l[0], DECK + 1, l[1], "spruce_fence");
            d.set(l[0], DECK + 2, l[1], "spruce_fence");
            d.set(l[0], DECK + 3, l[1], "lantern[hanging=false]");
        }
        // crates and a net-barrel stack at the head, a coil of rope (cobweb-free: a brown carpet on a slab) by the root
        d.set(HEAD_X1 - 1, DECK + 1, HEAD_Z0 + 1, "barrel[facing=up]");
        d.set(HEAD_X1 - 1, DECK + 1, HEAD_Z0 + 2, "barrel[facing=up]");
        d.set(HEAD_X1 - 1, DECK + 2, HEAD_Z0 + 1, "barrel[facing=north]");
        d.set(HEAD_X1 - 2, DECK + 1, HEAD_Z0 + 1, "spruce_trapdoor[half=bottom,facing=north,open=false]");
        // the walk along every arm is checked by the validator
        g.registerPathWalk("jetty main arm", HubGenerator.lineCells(JX, J_ROOT, JX, HEAD_Z0 + 1));
        g.registerPathWalk("jetty head", HubGenerator.lineCells(JX, HEAD_Z0 + 1, HEAD_X1 - 3, HEAD_Z0 + 1));
        g.registerPathWalk("jetty finger", HubGenerator.lineCells(JX, FINGER_Z, FINGER_X0 + 1, FINGER_Z));
    }

    boolean isDeck(int x, int z) {
        for (int[] c : deckCells) if (c[0] == x && c[1] == z) return true;
        return false;
    }

    // ------------------------------------------------------------------------------------------------ boats
    void boats() {
        // rowing boat moored along the north side of the finger pier, stern by the main arm, bow to the west
        rowboat(JX - 3, FINGER_Z - 3, 7, -1, "spruce", "birch_slab[type=bottom]");
        chain(JX - 5, FINGER_Z - 1, "z");
        // rowing boat on the east side of the main arm, bow out to sea
        rowboatZ(JX + 4, 175, 6, "dark_oak", "oak_slab[type=bottom]");
        chain(JX + 2, 176, "x");
        // sailing boat along the south side of the L-head
        sailboat(28, HEAD_Z1 + 4);
        chain(33, HEAD_Z1 + 1, "z");
    }

    /** Mooring chain lying on the water surface between a boat's rim and the jetty edge. */
    void chain(int x, int z, String axis) { d.set(x, SEA + 1, z, "chain[axis=" + axis + "]"); }

    /** Rowing boat lying along x: stern at x0, bow toward dir (+1/-1), 3 wide centred on zc. */
    void rowboat(int x0, int zc, int len, int dir, String wood, String rim) {
        for (int i = 0; i < len; i++) {
            int x = x0 + dir * i;
            int half = i >= len - 1 ? 0 : 1;
            for (int dz = -half; dz <= half; dz++) {
                int z = zc + dz;
                d.set(x, SEA, z, wood + "_planks");
                boolean rimCell = Math.abs(dz) == half || i == 0 || i == len - 1;
                if (rimCell) d.set(x, SEA + 1, z, rim);
                else if (i == len / 2) d.set(x, SEA + 1, z, rim);           // the thwart
            }
            g.reserve(x, zc);
        }
    }

    /** Rowing boat lying along z: stern at z0 (by the jetty), bow pointing south (+z). */
    void rowboatZ(int xc, int z0, int len, String wood, String rim) {
        for (int i = 0; i < len; i++) {
            int z = z0 + i;
            int half = i >= len - 1 ? 0 : 1;
            for (int dx = -half; dx <= half; dx++) {
                int x = xc + dx;
                d.set(x, SEA, z, wood + "_planks");
                boolean rimCell = Math.abs(dx) == half || i == 0 || i == len - 1;
                if (rimCell || i == len / 2) d.set(x, SEA + 1, z, rim);
            }
        }
    }

    /** Small sloop along x below the L-head: 5 wide, 12 long, mast with a white mainsail and a red jib. */
    void sailboat(int x0, int zc) {
        int len = 12;
        for (int i = 0; i < len; i++) {
            int x = x0 + i;
            int half = i >= len - 1 ? 0 : i >= len - 3 ? 1 : 2;
            if (i == 0) half = 1;
            for (int dz = -half; dz <= half; dz++) {
                int z = zc + dz;
                d.set(x, SEA, z, "dark_oak_planks");
                d.set(x, SEA - 1, z, Math.abs(dz) < half || half == 0 ? "dark_oak_planks" : "water");
                boolean rimCell = Math.abs(dz) == half || i == 0 || i == len - 1;
                d.set(x, SEA + 1, z, rimCell ? "spruce_slab[type=bottom]" : (i >= 2 && i <= 4 ? "spruce_planks" : null));
            }
        }
        // stripe along the hull side facing the harbour (white strake)
        for (int i = 1; i < len - 3; i++) d.set(x0 + i, SEA, zc - 2, "white_terracotta");
        // small cabin at the stern
        for (int x = x0 + 2; x <= x0 + 4; x++) for (int z = zc - 1; z <= zc + 1; z++) d.set(x, SEA + 2, z, "spruce_slab[type=bottom]");
        // mast + sails
        int mx = x0 + 7;
        for (int y = SEA + 1; y <= SEA + 13; y++) d.set(mx, y, zc, "spruce_fence");
        for (int x = x0 + 3; x < mx; x++) d.set(x, SEA + 3, zc, "stripped_spruce_log[axis=x]");     // boom
        for (int y = SEA + 4; y <= SEA + 12; y++) {
            int w = (int) Math.round((SEA + 12 - y) * 0.45);                                   // triangular mainsail
            for (int x = mx - 1; x >= mx - 1 - w && x >= x0 + 3; x--) d.set(x, y, zc, "white_wool");
        }
        for (int y = SEA + 3; y <= SEA + 10; y++) {
            int w = (int) Math.round((SEA + 10 - y) * 0.35);                                   // jib
            for (int x = mx + 1; x <= mx + 1 + w && x <= x0 + len - 2; x++) d.set(x, y, zc, y == SEA + 3 ? "white_wool" : "red_wool");
        }
        d.set(mx, SEA + 14, zc, "red_banner[rotation=4]");
        for (int x = x0; x < x0 + len; x++) for (int z = zc - 2; z <= zc + 2; z++) g.reserve(x, z);
    }

    // ------------------------------------------------------------------------------------------------ boathouse
    void boathouse() {
        int x0 = 40, x1 = 46, z0 = 154, z1 = 159, fy = DECK;
        // foundation + floor
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int old = g.hAt(x, z);
            for (int y = Math.min(old, fy) - 2; y < fy; y++) d.set(x, y, z, "cobblestone");
            for (int y = fy + 1; y <= Math.max(old, fy) + 2; y++) d.air(x, y, z);
            d.set(x, fy, z, (x == x0 || x == x1 || z == z0 || z == z1) ? "stripped_spruce_log[axis=" + (x == x0 || x == x1 ? "z" : "x") + "]" : "spruce_planks");
            g.paved(x, z, fy);
        }
        // walls
        for (int y = fy + 1; y <= fy + 3; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean wx = x == x0 || x == x1, wz = z == z0 || z == z1;
            if (!wx && !wz) { d.air(x, y, z); continue; }
            String b = (wx && wz) ? "stripped_spruce_log[axis=y]" : "spruce_planks";
            if (z == z1 && x >= x0 + 2 && x <= x1 - 2 && y <= fy + 2) b = null;          // wide doors to the water
            if (z == z1 && x > x0 && x < x1 && y == fy + 3) b = "stripped_spruce_log[axis=x]";  // lintel
            if (x == x0 && z == z0 + 3 && y <= fy + 2) b = null;                          // side door from the jetty
            if (y == fy + 2 && ((z == z0 && x == x0 + 3) || (x == x1 && (z == z0 + 2 || z == z0 + 3)))) b = "glass_pane";
            d.set(x, y, z, b);
        }
        // gable roof, ridge along x, overhanging one block
        int rows = (z1 - z0 + 2) / 2 + 1;
        for (int k = 0; k < rows; k++) {
            int y = fy + 4 + k;
            int zn = z0 - 1 + k, zs = z1 + 1 - k;
            if (zn > zs) break;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                d.set(x, y, zn, Draw.stairs("dark_oak", "south", false));
                if (zs != zn) d.set(x, y, zs, Draw.stairs("dark_oak", "north", false));
            }
            // gable ends
            for (int z = zn + 1; z <= zs - 1; z++) { d.set(x0, y, z, "spruce_planks"); d.set(x1, y, z, "spruce_planks"); }
        }
        // inside: barrels, a net crate, a hanging lantern
        d.set(x0 + 1, fy + 1, z0 + 1, "barrel[facing=up]");
        d.set(x0 + 2, fy + 1, z0 + 1, "barrel[facing=up]");
        d.set(x0 + 1, fy + 2, z0 + 1, "barrel[facing=up]");
        d.set(x1 - 1, fy + 1, z0 + 1, "composter");
        d.set(x0 + 3, fy + 5, z0 + 2, "lantern[hanging=true]");
        d.set(x0 + 3, fy + 6, z0 + 2, "spruce_planks");
        // a little slipway of boards running from the doors down to the water
        for (int z = z1 + 1; z <= z1 + 9; z++) for (int x = x0 + 2; x <= x1 - 2; x++) {
            int gt = g.hAt(x, z);
            if (gt < SEA - 1) break;
            d.set(x, Math.min(gt, DECK), z, x == x0 + 2 || x == x1 - 2 ? "stripped_spruce_log[axis=z]" : "spruce_planks");
        }
        g.reserveBox(x0 - 2, z0 - 2, x1 + 2, z1 + 9);
    }

    // ------------------------------------------------------------------------------------------------ shore
    void shoreDetails() {
        // three boulder groups on the west beach, sitting in the wet sand at the waterline
        double[][] groups = {{8, 0, 2.2}, {15, 1, 1.6}, {3, 2, 1.4}};
        for (double[] gr : groups) {
            int gx = (int) gr[0];
            int gz = (int) Math.floor(coastZ(gx + 0.5)) + (int) gr[1] - 1;
            int base = g.hAt(gx, gz);
            d.ellipsoid(gx + 0.5, base + 0.6, gz + 0.5, gr[2] + 0.4, gr[2], gr[2], (x, y, z) -> y <= base - 1 ? null
                    : d.pick(x, y, z, 141, "stone", 3, "andesite", 2, "mossy_cobblestone", 1));
            for (int x = gx - 3; x <= gx + 3; x++) for (int z = gz - 3; z <= gz + 3; z++) g.reserve(x, z);
        }
        // driftwood on the sand east of the boulders
        int dz = (int) Math.floor(coastZ(19.5)) - 3;
        int dy = g.hAt(19, dz);
        if (dy >= SEA) for (int x = 18; x <= 21; x++) if (g.hAt(x, dz) == dy) d.set(x, dy + 1, dz, "stripped_oak_log[axis=x]");
        // a stack of fish barrels at the jetty root, beside the path
        int bx = JX - 3, bz = J_ROOT + 2, by = g.hAt(bx, bz);
        if (by >= SEA) {
            d.set(bx, by + 1, bz, "barrel[facing=up]");
            d.set(bx, by + 1, bz + 1, "barrel[facing=up]");
            d.set(bx, by + 2, bz, "barrel[facing=east]");
            g.reserveBox(bx - 1, bz - 1, bx + 1, bz + 2);
        }
    }
}
