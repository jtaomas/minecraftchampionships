package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Noise;
import com.mcm.games.tgttos.maps.b.util.Noise.Pal;
import com.mcm.games.tgttos.maps.b.util.Stairs;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Kite Festival (elytra). A launch gantry high above a winding green valley between two floating ridges; the
 * gantry floor drops at GO. Three festival arches close the valley: striped banner screens with one small window
 * (5 x 5) low down and a crossbar on top. Above the crossbars the sky is full of tethered kites: the high (safe)
 * line clears every arch but has to slalom round the kites and their tethers; the low (fast) line dives and threads
 * the three windows in a straight line, then uses its speed to pull up onto the goal meadow. Goal: a fenced
 * meadow pen on a floating hill under a giant kite on a mast (landmark).
 *
 * <p>Local frame: gantry floor y 40 (x -18..17, z 0..6), valley void below, goal meadow top y 17, course +Z.
 */
public final class KiteFestival implements TgMap {

    @Override public String id() { return "kite_festival"; }
    @Override public String displayName() { return "Kite Festival"; }
    @Override public MapType type() { return MapType.ELYTRA; }
    @Override public Kit kit() { return Kit.ELYTRA_FIREWORKS; }

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    public static final int GANTRY_Y = 40, MEADOW_Y = 17, LEDGE = 10, HALF = 24;
    /** Arches: z, top of the banner wall (crossbar), window bottom y (window 5 x 5 at the centre), side of the big gate (-1/+1), gate bottom y (gate 9 wide x 10 high at the valley wall). */
    public static final int[][] ARCHES = {{45, 47, 19, 1, 24}, {80, 52, 15, -1, 19}, {112, 56, 12, 1, 15}};
    /** Kites: z, x offset from the valley centre, centre y, half width, tether side (-1 / +1). */
    public static final int[][] KITES = {{22, -4, 39, 5, -1}, {33, 5, 37, 6, -1}, {60, -8, 37, 6, -1}, {70, 5, 33, 6, 1},
            {93, -4, 32, 6, -1}, {102, 5, 30, 6, 1}};

    static final Pal GRASS = new Pal("grass_block", 6, "moss_block", 1);
    static final Pal ROCK = new Pal("stone", 5, "andesite", 2, "tuff", 1, "cobblestone", 1);
    static final String[] WOOL = {"red_wool", "white_wool", "yellow_wool"};

    /** Valley centre line x(z). */
    public static double cx(double z) {
        if (z <= 30) return 0;
        if (z <= 60) return (z - 30) / 30.0 * 10;
        if (z <= 95) return 10 - (z - 60) / 35.0 * 18;
        if (z <= 125) return -8 + (z - 95) / 30.0 * 8;
        return 0;
    }

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        g.scenery = true;
        ridges(g);
        g.scenery = false;
        gantry(g);
        for (int[] a : ARCHES) arch(g, a[0], a[1], a[2], a[3], a[4]);
        for (int[] k : KITES) kite(g, k);
        meadow(g);
        g.scenery = true;
        skyKites(g);
        g.scenery = false;

        d.voidY = 0;
        d.playBounds = Region.of(-70, -5, -14, 70, 90, 172);
        d.overviewPoints.add(new Spawn(0.5, 60, -12.5, 0f, 25f));
        d.overviewPoints.add(new Spawn(30.5, 45, 80.5, 90f, 20f));
        d.notes.put("safeLine", "over the three arch crossbars, slaloming round the six kites and their tethers");
        d.notes.put("fastLine", "dive and thread the three 5x5 arch windows (y 19 / 15 / 12), pull up onto the meadow");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ ridges

    private void ridges(G g) {
        for (int z = -14; z <= 170; z++) {
            double c = cx(z);
            for (int side = -1; side <= 1; side += 2)
                for (int t = 0; t <= 16; t++) {
                    double n = Noise.fbm2((c + side * (HALF + t)) * 0.07, z * 0.07, g.seed + (side > 0 ? 5 : 9));
                    double wid = 11 + 6 * n;
                    if (t > wid) continue;
                    int x = (int) Math.floor(c) + side * (HALF + t) + (side < 0 ? -1 : 0);
                    double fall = z > 118 ? (z - 118) * 0.55 : z < -2 ? (-2 - z) * 1.2 : 0;
                    int top = (int) Math.round(33 + t * 0.9 + n * 7 - fall - (t > wid - 3 ? (t - (wid - 3)) * 3 : 0));
                    if (top < 8) continue;
                    int bottom = top - 4 - (int) ((1 - t / 17.0) * 10 * (0.5 + n));
                    for (int y = bottom; y <= top; y++) {
                        String s = y == top ? GRASS.at(x, y, z, g.seed) : y >= top - 2 ? "dirt" : ROCK.at(x, y, z, g.seed);
                        if (y == top && top >= 47) s = "stone";
                        g.set(x, y, z, s);
                    }
                    // dressing on the ridge
                    double r = g.rnd(x, top, z);
                    if (r < 0.012 && t > 2 && t < wid - 3) tree(g, x, top + 1, z);
                    else if (r < 0.10) g.setIfAir(x, top + 1, z, r < 0.05 ? "short_grass" : r < 0.07 ? "poppy" : r < 0.085 ? "dandelion" : "oxeye_daisy");
                }
        }
        // festival tents on the ridges
        int[][] tents = {{-30, 30}, {28, 52}, {-24, 90}, {22, 104}, {-30, 64}, {27, 18}};
        for (int[] t : tents) tent(g, t[0], t[1]);
    }

    private int ground(G g, int x, int z) {
        for (int y = 60; y > 0; y--) { String s = g.get(x, y, z); if (s != null && !s.contains("grass[") && !s.contains("short_grass") && !s.contains("poppy") && !s.contains("dandelion") && !s.contains("daisy")) return y; }
        return -1;
    }

    private void tree(G g, int x, int y, int z) {
        int h = 4 + (int) (g.rnd(x, 7, z) * 3);
        for (int i = 0; i < h; i++) g.set(x, y + i, z, "oak_log");
        for (int dy = h - 2; dy <= h + 1; dy++) {
            int r = dy >= h ? 1 : 2;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && dy < h)) g.setIfAir(x + dx, y + dy, z + dz, "oak_leaves[persistent=true,distance=7]");
        }
    }

    private void tent(G g, int x0, int z0) {
        int base = ground(g, x0, z0);
        if (base < 0) return;
        for (int dy = 0; dy <= 4; dy++) {
            int r = 4 - dy;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -3; dz <= 3; dz++) {
                    boolean shell = Math.abs(dx) == r || dy == 4 || Math.abs(dz) == 3;
                    if (!shell) { g.set(x0 + dx, base + 1 + dy, z0 + dz, null); continue; }
                    if (Math.abs(dz) == 3 && Math.abs(dx) <= 1 && dy <= 1) { g.set(x0 + dx, base + 1 + dy, z0 + dz, null); continue; }
                    g.set(x0 + dx, base + 1 + dy, z0 + dz, ((dx + 40) / 2) % 2 == 0 ? "red_wool" : "white_wool");
                }
            for (int dz = -3; dz <= 3; dz++) g.set(x0, base + 6, z0 + dz, "yellow_wool");
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) g.set(x0 + dx, base, z0 + dz, "spruce_planks");
        g.set(x0, base + 7, z0 - 3, "red_banner[rotation=0]");
        g.set(x0, base + 1, z0, "lantern[hanging=false]");
    }

    // ------------------------------------------------------------------ gantry

    private void gantry(G g) {
        TgMapData d = g.d;
        int y = GANTRY_Y;
        for (int x = -18; x <= 17; x++)
            for (int z = 0; z <= 6; z++) {
                g.set(x, y, z, ((x + z) & 1) == 0 ? "spruce_planks" : "birch_planks");
                d.dropFloor.add(new Pos(x, y, z));
            }
        for (int yy = y + 1; yy <= y + 3; yy++)
            for (int x = -18; x <= 17; x++) { g.set(x, yy, 7, "glass"); d.startBarrier.add(new Pos(x, yy, 7)); }
        g.scenery = true;
        // frame: side glass, back rail, four scaffold legs down into the void, striped awning
        for (int yy = y + 1; yy <= y + 4; yy++) {
            for (int z = -1; z <= 7; z++) { g.set(-19, yy, z, "glass"); g.set(18, yy, z, "glass"); }
            for (int x = -19; x <= 18; x++) g.set(x, yy, -1, yy == y + 4 ? "spruce_slab[type=bottom]" : "glass");
        }
        for (int x = -19; x <= 18; x++) { g.set(x, y, -1, "dark_oak_planks"); g.set(x, y, 7, "dark_oak_planks"); }
        for (int z = -1; z <= 7; z++) { g.set(-19, y, z, "dark_oak_planks"); g.set(18, y, z, "dark_oak_planks"); }
        for (int[] l : new int[][]{{-20, -2}, {19, -2}, {-20, 8}, {19, 8}})
            for (int yy = 8; yy <= y + 8; yy++) g.set(l[0], yy, l[1], yy % 6 == 0 ? "stripped_dark_oak_log" : "dark_oak_log");
        for (int yy = 10; yy <= y - 2; yy += 6) {
            g.line(-20, yy, -2, -20, yy + 5, 8, "dark_oak_fence");
            g.line(19, yy, -2, 19, yy + 5, 8, "dark_oak_fence");
        }
        for (int x = -21; x <= 20; x++)
            for (int z = -3; z <= 9; z++) {
                int yy = y + 8 + (z <= 3 ? 0 : 0);
                g.set(x, yy, z, ((x + 40) / 3) % 2 == 0 ? "red_wool" : "white_wool");
            }
        for (int x = -21; x <= 20; x += 2) g.set(x, y + 7, 9, ((x + 40) / 3) % 2 == 0 ? "red_carpet" : "white_carpet");
        for (int x = -21; x <= 20; x += 6) g.set(x, y + 7, 8, "lantern[hanging=true]");
        g.text("KITE FESTIVAL", 26, y + 14, 4, -1, 0, 1, "yellow_wool");
        for (int x = -28; x <= 28; x++) g.set(x, y + 15, 4, null);
        // sign board behind the letters
        for (int x = -28; x <= 27; x++) for (int yy = y + 9; yy <= y + 15; yy++) g.set(x, yy, 5, yy == y + 15 || yy == y + 9 ? "red_wool" : "dark_oak_planks");
        g.scenery = false;
        g.startSpawns(-18, 17, 0, 6, y + 1, 15, 3);
        d.startStrip = Region.of(-18, y + 1, 0, 17, y + 4, 6);
    }

    // ------------------------------------------------------------------ arches + kites

    /** x range of the big gate of an arch. */
    public static int[] gateX(int z, int side) {
        int c = (int) Math.floor(cx(z));
        return side > 0 ? new int[]{c + HALF - 10, c + HALF - 2} : new int[]{c - HALF + 1, c - HALF + 9};
    }

    private void arch(G g, int z, int top, int win, int side, int gate) {
        int c = (int) Math.floor(cx(z));
        int[] gx = gateX(z, side);
        // banner wall across the valley and over both ridges up to the crossbar: a 5 x 5 window in the middle
        // (fast, straight) and a big 9 x 10 gate at one valley wall (safe, alternating sides = slalom)
        for (int x = c - HALF - 18; x <= c + HALF + 17; x++)
            for (int y = 2; y < top; y++) {
                if (g.get(x, y, z) != null && !com.mcm.games.tgttos.maps.b.util.Bs.passable(g.get(x, y, z))) continue;
                boolean window = x >= c - 2 && x <= c + 2 && y >= win && y <= win + 4;
                boolean big = x >= gx[0] && x <= gx[1] && y >= gate && y <= gate + 9;
                if (window || big) continue;
                String s = WOOL[Math.floorMod(x - c, 6) / 2];
                if ((y == win - 1 || y == win + 5) && x >= c - 3 && x <= c + 3 || (x == c - 3 || x == c + 3) && y >= win - 1 && y <= win + 5) s = "gold_block";
                if ((y == gate - 1 || y == gate + 10) && x >= gx[0] - 1 && x <= gx[1] + 1 || (x == gx[0] - 1 || x == gx[1] + 1) && y >= gate - 1 && y <= gate + 10)
                    s = "stripped_spruce_log";
                g.set(x, y, z, s);
            }
        for (int x = c - HALF - 18; x <= c + HALF + 17; x++) { g.set(x, top, z, "dark_oak_planks"); g.set(x, top + 1, z, "spruce_slab[type=bottom]"); }
        g.scenery = true;
        // pylons + flags at both ends of the crossbar, bunting, lanterns at the window and the gate
        for (int px : new int[]{c - HALF - 19, c + HALF + 18}) {
            for (int y = 0; y <= top + 6; y++) for (int dz = -1; dz <= 1; dz++) g.set(px, y, z + dz, dz == 0 ? "stripped_spruce_log" : "spruce_log");
            g.set(px, top + 7, z, "yellow_wool");
            g.set(px, top + 7, z - 1, "red_banner[rotation=8]");
        }
        for (int x = c - HALF - 18; x <= c + HALF + 17; x += 2) g.set(x, top - 1, z - 1, x % 4 == 0 ? "red_carpet" : "yellow_carpet");
        g.set(c - 4, win + 2, z - 1, "lantern[hanging=false]");
        g.set(c + 4, win + 2, z - 1, "lantern[hanging=false]");
        g.set(gx[0] - 2, gate + 5, z - 1, "lantern[hanging=false]");
        g.set(gx[1] + 2, gate + 5, z - 1, "lantern[hanging=false]");
        g.scenery = false;
    }

    private void kite(G g, int[] k) {
        int kz = k[0], ky = k[2], hw = k[3], side = k[4];
        int kx = (int) Math.floor(cx(kz)) + k[1];
        int hh = hw + 1;
        String a = WOOL[Math.floorMod(kz, 3)], b = WOOL[Math.floorMod(kz + 1, 3)];
        for (int dx = -hw; dx <= hw; dx++)
            for (int dy = -hh; dy <= hh; dy++) {
                if (Math.abs(dx) / (double) hw + Math.abs(dy) / (double) hh > 1.0001) continue;
                g.set(kx + dx, ky + dy, kz, (dx >= 0) == (dy >= 0) ? a : b);
            }
        for (int dx = -hw; dx <= hw; dx++) g.set(kx + dx, ky, kz + 1, "spruce_fence");
        for (int dy = -hh; dy <= hh; dy++) g.set(kx, ky + dy, kz + 1, "spruce_fence");
        // tail
        int ty = ky - hh - 1;
        for (int i = 0; i < 5; i++) {
            g.set(kx + ((i & 1) == 0 ? 0 : side), ty - i, kz, i % 2 == 0 ? "string" : "red_wool");
        }
        // tether: from the kite's lower corner down to a stake on the ridge on the kite's own side
        double ax = cx(kz) + side * (HALF + 3);
        g.tether(kx + side * hw, ky, kz + 1, ax, 31, kz + 6, "chain");
    }

    /** Decorative kites far above / beside the play space (scenery). */
    private void skyKites(G g) {
        int[][] s = {{-38, 58, 40}, {40, 54, 70}, {-36, 50, 110}, {36, 60, 20}, {30, 64, 140}};
        for (int[] k : s) {
            int x0 = k[0], y0 = k[1], z0 = k[2];
            for (int dx = -3; dx <= 3; dx++)
                for (int dy = -4; dy <= 4; dy++)
                    if (Math.abs(dx) / 3.0 + Math.abs(dy) / 4.0 <= 1.0001) g.set(x0 + dx, y0 + dy, z0, WOOL[Math.floorMod(dx * dy + z0, 3)]);
            int gy = ground(g, x0 + (x0 > 0 ? 4 : -4), z0 + 4);
            if (gy > 0) g.tether(x0, y0 - 4, z0, x0 + (x0 > 0 ? 4 : -4), gy + 1, z0 + 4, "chain");
        }
    }

    // ------------------------------------------------------------------ goal meadow + landmark

    private void meadow(G g) {
        TgMapData d = g.d;
        g.scenery = true;
        // floating hill under the meadow
        for (int x = -16; x <= 15; x++)
            for (int z = 124; z <= 166; z++) {
                double ex = (x + 0.5) / 16.0, ez = (z - 145.0) / 21.5;
                double e = ex * ex + ez * ez;
                if (e > 1) continue;
                int bottom = MEADOW_Y - 3 - (int) ((1 - e) * 16);
                for (int y = bottom; y < MEADOW_Y; y++) g.set(x, y, z, y >= MEADOW_Y - 3 ? "dirt" : ROCK.at(x, y, z, g.seed));
                g.set(x, MEADOW_Y, z, GRASS.at(x, MEADOW_Y, z, g.seed));
            }
        g.scenery = false;
        // landing meadow surface is route
        for (int x = -12; x <= 11; x++) for (int z = 128; z <= 156; z++) if (g.get(x, MEADOW_Y, z) != null) g.set(x, MEADOW_Y, z, GRASS.at(x, MEADOW_Y, z, g.seed));
        // pen: low hedge ring (1 high) with two gates facing the course
        int x1 = -7, x2 = 6, z1 = 139, z2 = 152;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean ring = x == x1 || x == x2 || z == z1 || z == z2;
                if (!ring) continue;
                boolean gate = z == z1 && (x >= -5 && x <= -3 || x >= 2 && x <= 4);
                g.set(x, MEADOW_Y + 1, z, gate ? null : "oak_leaves[persistent=true,distance=7]");
            }
        d.pen = Region.of(x1 + 1, MEADOW_Y + 1, z1 + 1, x2 - 1, MEADOW_Y + 3, z2 - 1);
        g.chickens(d.pen, 12);
        g.scenery = true;
        // hay bales and a picnic, flower beds
        int[][] hay = {{-11, 142}, {-11, 147}, {10, 144}, {10, 149}, {-10, 133}, {9, 132}};
        for (int[] h : hay) { g.set(h[0], MEADOW_Y + 1, h[1], "hay_block[axis=x]"); g.set(h[0], MEADOW_Y + 1, h[1] + 1, "hay_block[axis=x]"); }
        for (int x = -14; x <= 14; x++)
            for (int z = 126; z <= 164; z++)
                if (g.get(x, MEADOW_Y, z) != null && g.get(x, MEADOW_Y + 1, z) == null && (x < -12 || x > 11 || z > 156) && g.rnd(x, 3, z) < 0.25)
                    g.set(x, MEADOW_Y + 1, z, g.rnd(x, 4, z) < 0.5 ? "cornflower" : "poppy");
        // landmark: giant kite on a mast behind the pen
        int mx = 0, mz = 160;
        for (int y = MEADOW_Y + 1; y <= 64; y++) g.set(mx, y, mz, "stripped_spruce_log");
        for (int y = MEADOW_Y + 1; y <= MEADOW_Y + 3; y++) for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) g.set(mx + o[0], y, mz + o[1], "spruce_fence");
        int ky = 68, hw = 8, hh = 11;
        for (int dx = -hw; dx <= hw; dx++)
            for (int dy = -hh; dy <= hh; dy++) {
                if (Math.abs(dx) / (double) hw + Math.abs(dy) / (double) hh > 1.0001) continue;
                boolean edge = Math.abs(dx) / (double) hw + Math.abs(dy) / (double) hh > 0.85;
                String s = edge ? "yellow_wool" : (dx >= 0) == (dy >= 0) ? "red_wool" : "white_wool";
                g.set(mx + dx, ky + dy, mz, s);
            }
        for (int dx = -hw; dx <= hw; dx++) g.set(mx + dx, ky, mz + 1, "spruce_fence");
        for (int dy = -hh; dy <= hh; dy++) g.set(mx, ky + dy, mz + 1, "spruce_fence");
        for (int i = 0; i < 12; i++) {
            int tx = mx + (int) Math.round(Math.sin(i * 0.7) * 2), ty = ky - hh - 1 - i;
            g.set(tx, ty, mz - 1, i % 3 == 2 ? "yellow_wool" : "red_wool");
        }
        g.text("MCM", 5, ky + 2, mz - 1, -1, 0, 1, "black_wool");
        g.landmark.add(new double[]{mx + 0.5, ky + 0.5, mz + 0.5});
        g.landmark.add(new double[]{mx + 4.5, ky - 3.5, mz + 0.5});
        g.scenery = false;
        // a short climb from the low landing ledge (window line) onto the meadow
        g.scenery = false;
        for (int x = -6; x <= 5; x++)
            for (int z = 117; z <= 122; z++) {
                g.set(x, LEDGE, z, "coarse_dirt");
                g.scenery = true;
                g.set(x, LEDGE - 1, z, "dirt");
                if (z > 118 || (x + z) % 3 == 0) g.set(x, LEDGE - 2, z, "stone");
                g.scenery = false;
                for (int y = LEDGE + 1; y <= LEDGE + 4; y++) g.set(x, y, z, null);
            }
        Stairs.flight(g, "meadow", "spruce", "dirt", -6, 123, LEDGE, 0, 1, MEADOW_Y - LEDGE, 12, LEDGE - 2);
    }
}
