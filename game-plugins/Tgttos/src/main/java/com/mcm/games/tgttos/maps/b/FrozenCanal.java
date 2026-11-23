package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Noise;
import com.mcm.games.tgttos.maps.b.util.Noise.Pal;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Frozen Canal (boats on ice). A town canal frozen into a 20-wide packed-ice lane with two S-bends between
 * red-brick quays. Every bend has thin-ice holes on its inside (into the void) with a blue-ice racing strip
 * threaded between the holes and the inner quay: the fast line. Drift wide and you hit snow slush (boats crawl).
 * Three low footbridges (exactly 2 headroom) with piers split the lane; moored barges are bumpers. Half way the
 * canal splits: the 6-wide lock (choke, boats jam) or the 8-wide bypass round the lock-keeper's island (longer).
 * Goal: a lantern-lit skating rink pen under a striped finish arch and a tall lantern tree.
 *
 * <p>Local frame: ice surface y = 0 (boats at y 1), start tray x -21..20 / z 0..8, course towards +Z.
 */
public final class FrozenCanal implements TgMap {

    @Override public String id() { return "frozen_canal"; }
    @Override public String displayName() { return "Frozen Canal"; }
    @Override public MapType type() { return MapType.BOAT; }
    @Override public Kit kit() { return Kit.BOAT; }

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    /** Canal centre line. */
    public static final double[][] LINE = {{0, 9}, {0, 34}, {24, 58}, {24, 86}, {-6, 116}, {-6, 140}};
    public static final double HALF = 10;
    static final double[][] BYPASS = {{18, 60}, {10, 67}, {10, 81}, {18, 88}};
    public static final int X1 = -64, X2 = 64, Z1 = -24, Z2 = 180;
    public static final Region RINK = Region.of(-22, 0, 134, 10, 0, 158);
    public static final Region LOCK = Region.of(21, 0, 68, 26, 0, 80);

    static final Pal BRICK = new Pal("bricks", 6, "mud_bricks", 1, "granite", 1);
    static final Pal QUAY = new Pal("stone_bricks", 5, "polished_andesite", 2, "cracked_stone_bricks", 1);
    static final Pal ICE = new Pal("packed_ice", 8, "ice", 2);

    // cell classes
    static final int LAND = 0, CANAL = 1, HOLE = 2, BLUE = 3, SLUSH = 4, QUAYC = 5, PIER = 6;

    static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double t = ((px - ax) * vx + (pz - az) * vz) / (vx * vx + vz * vz);
        t = Math.max(0, Math.min(1, t));
        double dx = px - (ax + vx * t), dz = pz - (az + vz * t);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static double polyDist(double[][] l, double x, double z) {
        double best = 1e9;
        for (int i = 1; i < l.length; i++) best = Math.min(best, segDist(x, z, l[i - 1][0], l[i - 1][1], l[i][0], l[i][1]));
        return best;
    }

    int[][] cls;

    int c(int x, int z) { return x < X1 || x > X2 || z < Z1 || z > Z2 ? LAND : cls[x - X1][z - Z1]; }

    void put(int x, int z, int v) { if (x >= X1 && x <= X2 && z >= Z1 && z <= Z2) cls[x - X1][z - Z1] = v; }

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        plan();
        town(g);          // ground, quays, houses (scenery) first
        canal(g);
        tray(g);
        bridges(g);
        lock(g);
        rink(g);
        g.scenery = true;
        barges(g);
        lamps(g);
        g.scenery = false;

        d.voidY = -12;
        d.playBounds = Region.of(-40, -14, -2, 50, 40, 162);
        d.overviewPoints.add(new Spawn(30.5, 40, -10.5, 20f, 35f));
        d.overviewPoints.add(new Spawn(-6.5, 30, 112.5, 0f, 35f));
        d.notes.put("safeLine", "outside of each bend (5+ wide, past the slush), bypass channel round the lock island");
        d.notes.put("fastLine", "blue-ice strips on the insides of the bends between the thin-ice holes, straight through the 6-wide lock");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ plan (2D)

    private void plan() {
        cls = new int[X2 - X1 + 1][Z2 - Z1 + 1];
        for (int x = X1; x <= X2; x++)
            for (int z = Z1; z <= Z2; z++) {
                double px = x + 0.5, pz = z + 0.5;
                boolean in = polyDist(LINE, px, pz) <= HALF;
                if (z >= 0 && z <= 8 && x >= -21 && x <= 20) in = true;                       // start tray
                if (z > 8 && z <= 20) { double w = 21 - (z - 8) * 0.9; if (px >= -w && px <= w) in = true; }   // taper
                if (polyDist(BYPASS, px, pz) <= 4) in = true;
                if (RINK.contains(x, 0, z)) {
                    double cx = Math.max(Math.abs(x + 6.0) - 12, 0), cz = Math.max(Math.abs(z - 146.0) - 8, 0);
                    in = cx * cx + cz * cz <= 16 || in;
                }
                // lock island and right lock wall
                if (z >= 68 && z <= 80 && ((x >= 14 && x <= 20) || (x >= 27 && x <= 35))) in = false;
                put(x, z, in ? CANAL : LAND);
            }
        // quays: land cells next to the canal
        for (int x = X1; x <= X2; x++)
            for (int z = Z1; z <= Z2; z++) {
                if (c(x, z) != LAND) continue;
                boolean near = false;
                for (int dx = -2; dx <= 2 && !near; dx++) for (int dz = -2; dz <= 2 && !near; dz++) if (c(x + dx, z + dz) == CANAL) near = true;
                if (near) put(x, z, QUAYC);
            }
        // bend insides: thin-ice holes + blue-ice racing strip; outsides: slush
        bend(0, 34, 1, 0.0, 1, 1);     // V1: turning towards +x, inside +x
        bend(24, 58, -1, 0.0, 1, 1);   // V2: inside -x
        bend(24, 86, -1, 0.0, 1, 1);   // V3: inside -x
        bend(-6, 116, 1, 0.0, 1, 1);   // V4: inside +x
        // slush patches in the straights (force steering at least every ~25 blocks)
        int[][] slush = {{-6, 18, 4, 2}, {5, 26, 3, 2}, {28, 66, 3, 3}, {18, 92, 3, 2}, {-12, 128, 4, 2}, {-1, 124, 3, 2}, {6, 100, 3, 3}};
        for (int[] s : slush) for (int x = s[0]; x < s[0] + s[2]; x++) for (int z = s[1]; z < s[1] + s[3]; z++) if (c(x, z) == CANAL) put(x, z, SLUSH);
    }

    /** Holes at 4..6 off the centre line on the inside of vertex (vx,vz), blue strip at 7..9, slush at -7..-9. */
    private void bend(double vx, double vz, int insideX, double unused, int a, int b) {
        for (int x = (int) vx - 16; x <= (int) vx + 16; x++)
            for (int z = (int) vz - 12; z <= (int) vz + 12; z++) {
                if (c(x, z) != CANAL) continue;
                double side = (x + 0.5 - vx) * insideX;               // >0 = inside of the bend
                double dc = polyDist(LINE, x + 0.5, z + 0.5);
                double dv = Math.hypot(x + 0.5 - vx, z + 0.5 - vz);
                if (dv > 12) continue;
                if (side > 0 && dc >= 3.5 && dc <= 6.2 && dv <= 9) {
                    int hx = Math.floorMod(x, 4), hz = Math.floorMod(z, 4);
                    if (hx <= 1 && hz <= 1 || (hx >= 2 && hz >= 2 && dv < 6)) put(x, z, HOLE);
                }
                if (side > 0 && dc > 6.2 && dc <= 9.5) put(x, z, BLUE);
                if (side < 0 && dc >= 7 && dc <= 9.5 && dv <= 9 && ((x + z) & 3) != 0) put(x, z, SLUSH);
            }
    }

    // ------------------------------------------------------------------ build

    private void canal(G g) {
        for (int x = X1; x <= X2; x++)
            for (int z = Z1; z <= Z2; z++) {
                int k = c(x, z);
                if (k == LAND || k == QUAYC) continue;
                g.scenery = true;
                for (int y = -4; y <= -1; y++) g.set(x, y, z, y == -4 ? "stone" : "mud_bricks");
                g.scenery = false;
                switch (k) {
                    case HOLE -> {
                        for (int y = -4; y <= 0; y++) g.set(x, y, z, null);
                    }
                    case BLUE -> g.set(x, 0, z, "blue_ice");
                    case SLUSH -> g.set(x, 0, z, "snow_block");
                    default -> g.set(x, 0, z, ICE.at(x, 0, z, g.seed));
                }
                for (int y = 1; y <= 3; y++) g.set(x, y, z, null);
            }
        // hole rims: broken floes (ice slabs are not a thing: use packed ice + snow layers on the quay side)
        for (int x = X1; x <= X2; x++)
            for (int z = Z1; z <= Z2; z++)
                if (c(x, z) == HOLE) {
                    g.scenery = true;
                    g.set(x, -5, z, null);
                    g.scenery = false;
                }
    }

    private int ground(G g, int x, int z) {
        return 2 + (int) Math.round(Noise.fbm2(x * 0.05, z * 0.05, g.seed + 3) * 2 - 1) ;
    }

    private void town(G g) {
        g.scenery = true;
        for (int x = X1; x <= X2; x++)
            for (int z = Z1; z <= Z2; z++) {
                int k = c(x, z);
                if (k != LAND && k != QUAYC) continue;
                // island outline (rounded), tapered underside
                double ex = x / 64.0, ez = (z - 78) / 102.0;
                double e = ex * ex + ez * ez;
                if (e > 1) continue;
                int top = k == QUAYC ? 2 : ground(g, x, z) + (e > 0.75 ? -1 : 0);
                int bottom = -4 - (int) ((1 - e) * 10 * (0.6 + 0.4 * g.rnd(x, 0, z)));
                for (int y = bottom; y < top; y++) g.set(x, y, z, y < -4 ? "stone" : y < 0 ? "dirt" : "stone_bricks");
                if (k == QUAYC) {
                    // quay wall face (bricks) with a stone coping walkway
                    for (int y = -3; y <= 1; y++) g.set(x, y, z, BRICK.at(x, y, z, g.seed));
                    g.set(x, 2, z, QUAY.at(x, 2, z, g.seed));
                } else {
                    double r = Noise.fbm2(x * 0.15, z * 0.15, g.seed + 8);
                    g.set(x, top, z, r < 0.45 ? "snow_block" : r < 0.6 ? "packed_ice" : r < 0.8 ? "stone_bricks" : "polished_andesite");
                    if (r < 0.45 && g.rnd(x, 1, z) < 0.5) g.set(x, top + 1, z, "snow[layers=" + (1 + (int) (g.rnd(x, 2, z) * 2)) + "]");
                }
            }
        // row houses along the canal (outside the quays)
        for (int gx = X1 + 2; gx <= X2 - 8; gx += 9)
            for (int gz = Z1 + 4; gz <= Z2 - 8; gz += 9) {
                int hx = gx + (int) (g.rnd(gx, 1, gz) * 2), hz = gz + (int) (g.rnd(gx, 2, gz) * 2);
                int w = 6 + (int) (g.rnd(gx, 3, gz) * 2), dp = 6 + (int) (g.rnd(gx, 4, gz) * 2);
                boolean ok = true;
                double mind = 1e9;
                for (int x = hx - 2; x <= hx + w + 1 && ok; x++)
                    for (int z = hz - 2; z <= hz + dp + 1 && ok; z++) {
                        if (c(x, z) != LAND) ok = false;
                        double ex = x / 64.0, ez = (z - 78) / 102.0;
                        if (ex * ex + ez * ez > 0.8) ok = false;
                        mind = Math.min(mind, polyDist(LINE, x, z));
                    }
                if (!ok || mind > 34) continue;
                if (hz < 8 && Math.abs(hx) < 30) continue;          // keep the view behind the tray open
                if (RINK.minZ() - 6 <= hz + dp && hz <= RINK.maxZ() + 4 && hx + w >= -26 && hx <= 14) continue;
                house(g, hx, hz, w, dp);
            }
        g.scenery = false;
    }

    private void house(G g, int x0, int z0, int w, int dp) {
        int base = ground(g, x0, z0);
        int h = 6 + (int) (g.rnd(x0, 5, z0) * 6);
        String wall = g.rnd(x0, 6, z0) < 0.6 ? "bricks" : g.rnd(x0, 7, z0) < 0.5 ? "white_terracotta" : "mud_bricks";
        boolean alongX = w >= dp;
        for (int x = x0; x <= x0 + w; x++)
            for (int z = z0; z <= z0 + dp; z++) {
                for (int y = base - 1; y <= base + h; y++) {
                    boolean edge = x == x0 || x == x0 + w || z == z0 || z == z0 + dp;
                    if (!edge) { g.set(x, y, z, y == base - 1 ? "spruce_planks" : null); continue; }
                    boolean corner = (x == x0 || x == x0 + w) && (z == z0 || z == z0 + dp);
                    String b = corner ? "stripped_spruce_log" : wall;
                    int ly = y - base;
                    if (!corner && ly >= 2 && ly < h - 1 && ly % 3 != 1 && ((x + z) & 1) == 0) b = "yellow_stained_glass_pane";
                    if (y == base - 1) b = "stone_bricks";
                    g.set(x, y, z, b);
                }
                if ((x + z) % 5 == 0 && x > x0 && x < x0 + w && z > z0 && z < z0 + dp) g.set(x, base + 2, z, "lantern[hanging=false]");
            }
        // gable roof with a snow cap
        int span = alongX ? dp : w;
        for (int i = 0; i <= span / 2 + 1; i++) {
            int ry = base + h + 1 + i;
            for (int a = (alongX ? x0 - 1 : z0 - 1); a <= (alongX ? x0 + w + 1 : z0 + dp + 1); a++) {
                int lo = (alongX ? z0 : x0) - 1 + i, hi = (alongX ? z0 + dp : x0 + w) + 1 - i;
                if (lo > hi) continue;
                for (int s : new int[]{lo, hi}) {
                    int x = alongX ? a : s, z = alongX ? s : a;
                    String face = alongX ? (s == lo ? "north" : "south") : (s == lo ? "west" : "east");
                    String facing = alongX ? (s == lo ? "south" : "north") : (s == lo ? "east" : "west");
                    g.set(x, ry, z, lo == hi ? "dark_oak_slab[type=bottom]" : "spruce_stairs[facing=" + facing + ",half=bottom,shape=straight]");
                    if (g.rnd(x, ry, z) < 0.7) g.setIfAir(x, ry + 1, z, "snow[layers=1]");
                }
                // gable ends filled
                for (int s = lo + 1; s < hi; s++) {
                    int x = alongX ? a : s, z = alongX ? s : a;
                    if (a == (alongX ? x0 : z0) || a == (alongX ? x0 + w : z0 + dp)) g.set(x, ry, z, wall);
                }
            }
        }
        // chimney
        int cx = x0 + 1, cz = z0 + 1;
        for (int y = base + h; y <= base + h + span / 2 + 3; y++) g.set(cx, y, cz, "bricks");
        g.set(cx, base + h + span / 2 + 4, cz, "campfire[lit=true,signal_fire=false]");
    }

    // ------------------------------------------------------------------ start tray

    private void tray(G g) {
        TgMapData d = g.d;
        for (int x = -21; x <= 20; x++)
            for (int z = 0; z <= 8; z++) g.set(x, 0, z, z == 8 ? ((x & 1) == 0 ? "blue_ice" : "packed_ice") : "packed_ice");
        // back and side walls of the tray, glass barrier at z 9
        g.scenery = true;
        for (int x = -23; x <= 22; x++) for (int y = -2; y <= 3; y++) { g.set(x, y, -1, y <= 2 ? BRICK.at(x, y, -1, g.seed) : "spruce_fence"); }
        g.scenery = false;
        for (int y = 1; y <= 3; y++) {
            for (int z = -1; z <= 9; z++) { g.set(-22, y, z, "glass"); g.set(21, y, z, "glass"); }
            for (int x = -21; x <= 20; x++) { g.set(x, y, 9, "glass"); d.startBarrier.add(new TgMapData.Pos(x, y, 9)); }
        }
        for (int x = -21; x <= 20; x++) g.set(x, 0, 9, "packed_ice");
        d.startStrip = Region.of(-21, 1, 0, 20, 4, 8);
        for (int r = 0; r < 2; r++)
            for (int i = 0; i < 20; i++) {
                double x = -20.0 + i * 2.1, z = 2.5 + r * 3.5;
                d.boatSpawns.add(new Spawn(x, 1, z, 0f));
                d.startSpawns.add(new Spawn(x, 1, z, 0f));
            }
    }

    // ------------------------------------------------------------------ bridges, lock, rink

    /** Footbridge across the canal at z (deck y 3 = headroom exactly 2), piers splitting the lane. */
    private void bridge(G g, int z, int xa, int xb, int[] piers) {
        g.scenery = true;
        for (int x = xa; x <= xb; x++)
            for (int k = 0; k < 3; k++) {
                g.set(x, 3, z + k, k == 1 ? "spruce_planks" : "stone_brick_slab[type=top]");
                g.set(x, 4, z + k, k == 1 ? null : "spruce_fence");
                if (k != 1 && (x % 4 == 0)) g.set(x, 4, z + k, "stone_brick_wall");
            }
        for (int x = xa; x <= xb; x++) for (int k = 0; k < 3; k += 2) if (x % 8 == 0) g.set(x, 5, z + k, "lantern[hanging=false]");
        g.scenery = false;
        for (int px : piers)
            for (int x = px; x <= px + 1; x++)
                for (int k = 0; k < 3; k++)
                    for (int y = 0; y <= 2; y++) g.set(x, y, z + k, y == 0 ? "stone_bricks" : BRICK.at(x, y, z + k, g.seed));
    }

    private void bridges(G g) {
        bridge(g, 26, -12, 12, new int[]{-6, 4});
        bridge(g, 92, 6, 36, new int[]{18, 27});
        bridge(g, 124, -18, 6, new int[]{-11, -3});
    }

    private void lock(G g) {
        g.scenery = true;
        // lock walls (stone bricks, 3 high above the ice) and the keeper's house on the island
        for (int z = 68; z <= 80; z++) {
            for (int y = 0; y <= 3; y++) { g.set(20, y, z, QUAY.at(20, y, z, g.seed)); g.set(27, y, z, QUAY.at(27, y, z, g.seed)); }
            if (z == 68 || z == 80) for (int x = 21; x <= 26; x++) { g.set(x, 4, z, "dark_oak_planks"); g.set(x, 5, z, "dark_oak_fence"); }
        }
        // lock gates swung open against the walls (dark oak, striped)
        for (int y = 1; y <= 3; y++) { g.set(21, y, 67, "dark_oak_planks"); g.set(26, y, 67, "dark_oak_planks"); g.set(21, y, 81, "dark_oak_planks"); g.set(26, y, 81, "dark_oak_planks"); }
        int base = 3;
        for (int x = 14; x <= 19; x++)
            for (int z = 70; z <= 78; z++)
                for (int y = base; y <= base + 6; y++) {
                    boolean edge = x == 14 || x == 19 || z == 70 || z == 78;
                    if (edge) g.set(x, y, z, y == base + 6 ? "spruce_planks" : (y == base + 3 && (z & 1) == 0 && x != 14 && x != 19) ? "yellow_stained_glass_pane" : "white_terracotta");
                }
        for (int i = 0; i <= 3; i++) for (int z = 69 + i; z <= 79 - i; z++) { g.set(13 + i, base + 7 + i, z, "red_terracotta"); g.set(20 - i, base + 7 + i, z, "red_terracotta"); }
        g.set(16, base + 2, 74, "lantern[hanging=false]");
        g.scenery = false;
    }

    private void rink(G g) {
        TgMapData d = g.d;
        // pen in the rink: snow-brick ring 1 high with 4-wide openings (boats can drive in), ice inside
        int x1 = -13, x2 = 0, z1 = 140, z2 = 153;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean ring = x == x1 || x == x2 || z == z1 || z == z2;
                g.set(x, 0, z, ring ? "polished_andesite" : ((x + z) & 1) == 0 ? "packed_ice" : "ice");
                if (ring) g.set(x, 1, z, ((x + z) & 1) == 0 ? "red_wool" : "white_wool");
            }
        for (int x = -8; x <= -5; x++) g.set(x, 1, z1, null);
        for (int z = 145; z <= 148; z++) { g.set(x1, 1, z, null); g.set(x2, 1, z, null); }
        d.pen = Region.of(x1 + 1, 1, z1 + 1, x2 - 1, 3, z2 - 1);
        g.chickens(d.pen, 12);
        g.scenery = true;
        // striped finish arch over the canal at z 128..129
        for (int y = 1; y <= 12; y++)
            for (int k = 0; k < 2; k++) { g.set(-17, y, 128 + k, (y / 2) % 2 == 0 ? "red_wool" : "white_wool"); g.set(5, y, 128 + k, (y / 2) % 2 == 0 ? "red_wool" : "white_wool"); }
        for (int x = -17; x <= 5; x++) for (int k = 0; k < 2; k++) { g.set(x, 12, 128 + k, ((x + 40) / 2) % 2 == 0 ? "red_wool" : "white_wool"); g.set(x, 13, 128 + k, "black_wool"); }
        g.text("FINISH", 4, 11, 127, -1, 0, 1, "black_wool");
        // landmark: tall lantern tree behind the rink
        int tx = -6, tz = 164;
        int gy = 2;
        for (int y = gy; y <= gy + 44; y++) g.set(tx, y, tz, "spruce_log");
        for (int y = gy + 6; y <= gy + 42; y++) {
            double r = (gy + 44 - y) * 0.32 + 1;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dz = -ri; dz <= ri; dz++) {
                    double dd = Math.hypot(dx, dz);
                    if (dd > r || (dx == 0 && dz == 0)) continue;
                    if ((y % 4 == 0 && dd > r - 1.2) || dd > r - 0.8 && g.rnd(tx + dx, y, tz + dz) < 0.12)
                        g.set(tx + dx, y, tz + dz, ((dx + dz + y) & 1) == 0 ? "sea_lantern" : "glowstone");
                    else if (y % 4 != 3 || dd < r - 1) g.set(tx + dx, y, tz + dz, "spruce_leaves[persistent=true,distance=7]");
                }
        }
        for (int y = gy + 44; y <= gy + 48; y++) g.set(tx, y, tz, "gold_block");
        g.set(tx - 1, gy + 46, tz, "gold_block"); g.set(tx + 1, gy + 46, tz, "gold_block");
        g.set(tx, gy + 46, tz - 1, "gold_block"); g.set(tx, gy + 46, tz + 1, "gold_block");
        g.landmark.add(new double[]{tx + 0.5, gy + 47, tz + 0.5});
        g.landmark.add(new double[]{tx + 0.5, gy + 40, tz + 0.5});
        // rink border of hay bales and benches, lamp posts with lanterns
        for (int x = RINK.minX() - 2; x <= RINK.maxX() + 2; x += 4) for (int z : new int[]{RINK.minZ() - 2, RINK.maxZ() + 3}) {
            if (c(x, z) == QUAYC || c(x, z) == LAND) { for (int y = 3; y <= 5; y++) g.set(x, y, z, "dark_oak_fence"); g.set(x, 6, z, "lantern[hanging=false]"); }
        }
        g.text("MCM", 1, 20, 158, -1, 0, 1, "red_wool");
        g.scenery = false;
    }

    private void barges(G g) {
        // moored barges along the quays (bumpers): hull + cabin + cargo
        int[][] b = {{-9, 13, 0}, {6, 40, 1}, {31, 60, 0}, {30, 100, 1}, {-15, 133, 0}};
        for (int[] s : b) {
            int x0 = s[0], z0 = s[1];
            boolean alongZ = s[2] == 0;
            int L = 9, W = 3;
            for (int i = 0; i < L; i++)
                for (int j = 0; j < W; j++) {
                    int x = alongZ ? x0 + j : x0 + i, z = alongZ ? z0 + i : z0 + j;
                    if (c(x, z) != CANAL && c(x, z) != SLUSH && c(x, z) != BLUE) continue;
                    boolean rim = j == 0 || j == W - 1 || i == 0 || i == L - 1;
                    g.set(x, 1, z, rim ? "dark_oak_planks" : "spruce_planks");
                    g.set(x, 2, z, rim ? "dark_oak_slab[type=bottom]" : (i % 3 == 1 ? "barrel[facing=up,open=false]" : null));
                    if (i == L - 2 && !rim) { g.set(x, 2, z, "spruce_planks"); g.set(x, 3, z, "red_terracotta"); }
                }
        }
    }

    private void lamps(G g) {
        // lamp posts on the quays every ~7 blocks along the canal
        for (int x = X1; x <= X2; x += 1)
            for (int z = Z1; z <= Z2; z += 1) {
                if (c(x, z) != QUAYC || g.rnd(x, 9, z) > 0.05) continue;
                boolean edge = false;
                for (int[] dd : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (c(x + dd[0], z + dd[1]) == CANAL) edge = true;
                if (edge) continue;
                if (g.get(x, 3, z) != null) continue;
                for (int y = 3; y <= 5; y++) g.set(x, y, z, "dark_oak_fence");
                g.set(x, 6, z, "lantern[hanging=false]");
            }
    }
}
