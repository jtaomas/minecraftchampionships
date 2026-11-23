package com.mcm.games.parkourwarrior.map;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scenery islands floating 20-80 blocks off the course, themed per zone. Nothing here is on (or near) a route:
 * every piece is rejected if any course block lies within {@link #CLEAR} blocks of its footprint.
 */
final class Backdrop {
    private Backdrop() {}

    static final int CLEAR = 18;
    private static final int G = 4; // occupancy grid size

    static void build(W w, List<int[]> hubs, int[] fork, int[][] finishes) {
        Set<Long> occ = new HashSet<>();
        w.b.forEach((x, y, z, s) -> occ.add(cell(x >> 2, z >> 2)));
        // zone pieces: three per zone, alternating sides, west first (spurs run east)
        for (int s = 1; s <= 5; s++) {
            int[] h = hubs.get(s - 1);
            int zEnd = s < 5 ? hubs.get(s)[2] : fork[2];
            for (int k = 0; k < 4; k++) {
                int z = h[2] + (zEnd - h[2]) * (2 * k + 1) / 8;
                int side = k == 3 ? 1 : -1;
                int y = h[1] + (int) (W.h(s, k, z, 7) * 36) - 18;
                place(w, occ, s, k, h[0], y, z, side);
            }
            // a tall column marking each hub from afar
            column(w, occ, h[0] - 30, h[1], h[2] + 2, s);
        }
        // ending scenery behind each finish shrine
        for (int d = 0; d < 3; d++) {
            int[] f = finishes[d];
            int dx = d == 0 ? -1 : d == 2 ? 1 : 0;
            for (int t = 0; t < 2; t++) {
                int x = f[0] + dx * (24 + t * 14) + (dx == 0 ? (t == 0 ? -10 : 10) : 0);
                int z = f[2] + 24 + t * 10;
                int y = f[1] - 8 + t * 12;
                if (!clear(occ, x, z, 7)) continue;
                if (d == 0) meadow(w, x, y, z, t);
                else if (d == 1) iceberg(w, x, y, z, t);
                else library(w, x, y, z, t);
            }
        }
    }

    // ------------------------------------------------------------------ placement

    private static long cell(int gx, int gz) { return ((long) gx << 32) ^ (gz & 0xffffffffL); }

    private static boolean clear(Set<Long> occ, int x, int z, int r) {
        int rr = r + CLEAR;
        for (int gx = (x - rr) >> 2; gx <= (x + rr) >> 2; gx++)
            for (int gz = (z - rr) >> 2; gz <= (z + rr) >> 2; gz++)
                if (occ.contains(cell(gx, gz))) return false;
        return true;
    }

    private static void mark(Set<Long> occ, int x, int z, int r) {
        // keep backdrop pieces from piling into each other (smaller margin than the course)
        for (int gx = (x - r) >> 2; gx <= (x + r) >> 2; gx++)
            for (int gz = (z - r) >> 2; gz <= (z + r) >> 2; gz++) occ.add(cell(gx, gz));
    }

    /** Find a free spot 22..80 blocks out on the given side and build the zone piece there. */
    private static void place(W w, Set<Long> occ, int s, int k, int lineX, int y, int z, int side) {
        for (int off = 26; off <= 74; off += 6) {
            int x = lineX + side * (off + (int) (W.h(s, k, off, 3) * 8));
            int r = 7;
            if (!clear(occ, x, z, r)) continue;
            switch (s) {
                case 1 -> toy(w, x, y, z, k);
                case 2 -> rooftop(w, x, y, z, k);
                case 3 -> hull(w, x, y, z, k, side);
                case 4 -> clock(w, x, y, z, k);
                default -> geode(w, x, y, z, k);
            }
            mark(occ, x, z, r + 6);
            return;
        }
    }

    // ------------------------------------------------------------------ shared shapes

    /** A floating rock island: flat top of radius r at y, tapering down. */
    private static void island(W w, int cx, int y, int cz, double r, String top, String soil, int salt) {
        int depth = (int) Math.round(r * 1.3) + 2;
        for (int d = 0; d <= depth; d++) {
            double rr = r * (1 - Math.pow(d / (double) (depth + 1), 1.3));
            int ri = (int) Math.ceil(rr);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double n = W.h(cx + dx, y - d, cz + dz, salt) * 0.9;
                if (dx * dx + dz * dz > (rr + n - 0.45) * (rr + n - 0.45)) continue;
                String b = d == 0 ? top : d <= 2 ? soil
                        : W.h(cx + dx, y - d, cz + dz, salt + 1) < 0.3 ? "minecraft:andesite"
                        : W.h(cx + dx, y - d, cz + dz, salt + 2) < 0.15 ? "minecraft:cobblestone" : "minecraft:stone";
                w.set(cx + dx, y - d, cz + dz, b);
            }
        }
    }

    private static void sphere(W w, int cx, int cy, int cz, double r, String s, boolean hollow) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++) for (int dz = -ri; dz <= ri; dz++) {
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d <= r && (!hollow || d > r - 1.2)) w.set(cx + dx, cy + dy, cz + dz, s);
        }
    }

    /** Disc in the x/y plane (faces along z) - clock faces, gears. */
    private static void vdisc(W w, int cx, int cy, int z, double r, String s) {
        int ri = (int) Math.ceil(r);
        for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++)
            if (dx * dx + dy * dy <= r * r) w.set(cx + dx, cy + dy, z, s);
    }

    // ------------------------------------------------------------------ zone pieces

    /** Toy Box: a giant alphabet block, a spinning top or a balloon cluster. */
    private static void toy(W w, int x, int y, int z, int k) {
        String[] col = Sh.TOY;
        switch (k % 3) {
            case 0 -> { // alphabet block on a little island
                island(w, x, y, z, 5, "minecraft:moss_block", "minecraft:dirt", 11);
                String c = col[k % col.length], e = col[(k + 3) % col.length];
                for (int dx = -3; dx <= 3; dx++) for (int dy = 1; dy <= 7; dy++) for (int dz = -3; dz <= 3; dz++) {
                    boolean edge = (Math.abs(dx) == 3 ? 1 : 0) + (dy == 1 || dy == 7 ? 1 : 0) + (Math.abs(dz) == 3 ? 1 : 0) >= 2;
                    w.set(x + dx, y + dy, z + dz, edge ? "minecraft:" + e + "_concrete" : "minecraft:" + c + "_concrete");
                }
                // a raised "A" on the two long faces
                int[][] glyph = {{0, 6}, {-1, 5}, {1, 5}, {-1, 4}, {1, 4}, {-2, 3}, {-1, 3}, {0, 3}, {1, 3}, {2, 3}, {-2, 2}, {2, 2}};
                for (int[] g : glyph) { w.set(x + g[0], y + g[1], z - 4, "minecraft:white_concrete"); w.set(x + g[0], y + g[1], z + 4, "minecraft:white_concrete"); }
            }
            case 1 -> { // spinning top balanced on its point
                for (int dy = 0; dy <= 9; dy++) {
                    double r = dy < 5 ? dy * 1.1 : (9 - dy) * 1.4 + 0.5;
                    int ri = (int) Math.ceil(r);
                    for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) if (dx * dx + dz * dz <= r * r) {
                        double a = Math.atan2(dz, dx);
                        w.set(x + dx, y + dy, z + dz, "minecraft:" + col[(int) ((a + Math.PI) / (Math.PI / 3)) % 6] + "_terracotta");
                    }
                }
                for (int dy = 10; dy <= 12; dy++) w.set(x, y + dy, z, "minecraft:stripped_birch_log[axis=y]");
                island(w, x, y - 1, z, 3, "minecraft:smooth_stone", "minecraft:stone", 12);
            }
            default -> { // balloon bunch tethered to a rock
                island(w, x, y, z, 4, "minecraft:grass_block[snowy=false]", "minecraft:dirt", 13);
                int[][] bal = {{0, 12, 0}, {-3, 10, 1}, {3, 11, -1}, {1, 14, 3}};
                for (int i = 0; i < bal.length; i++) {
                    int bx = x + bal[i][0], by = y + bal[i][1], bz = z + bal[i][2];
                    sphere(w, bx, by, bz, 2.2, "minecraft:" + col[(i + k) % col.length] + "_wool", false);
                    w.set(bx, by - 3, bz, "minecraft:" + col[(i + k) % col.length] + "_wool");
                    for (int yy = y + 1; yy < by - 3; yy++) w.set(bx + (x - bx) * (by - 3 - yy) / Math.max(1, by - 3 - y), yy, bz + (z - bz) * (by - 3 - yy) / Math.max(1, by - 3 - y), "minecraft:white_stained_glass_pane");
                }
            }
        }
    }

    /** Market Rooftops: a lone house top with a chimney and a washing line. */
    private static void rooftop(W w, int x, int y, int z, int k) {
        island(w, x, y, z, 6, "minecraft:cobblestone", "minecraft:stone", 21 + k);
        String[] wall = {"minecraft:white_terracotta", "minecraft:yellow_terracotta", "minecraft:light_blue_terracotta", "minecraft:pink_terracotta"};
        String wl = wall[k % wall.length];
        for (int dx = -4; dx <= 4; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = 1; dy <= 5; dy++) {
            boolean frame = (Math.abs(dx) == 4 && Math.abs(dz) == 3);
            boolean win = dy == 3 && (Math.abs(dx) == 4 || Math.abs(dz) == 3) && ((dx + dz) & 1) == 0 && !frame;
            w.set(x + dx, y + dy, z + dz, frame ? "minecraft:spruce_log[axis=y]" : win ? "minecraft:glass_pane" : wl);
        }
        for (int t = 0; t <= 4; t++) for (int dx = -5; dx <= 5; dx++) {
            w.set(x + dx, y + 6 + t, z - 4 + t, "minecraft:" + "brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
            w.set(x + dx, y + 6 + t, z + 4 - t, "minecraft:" + "brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            if (t < 4) for (int dz = -3 + t; dz <= 3 - t; dz++) if (Math.abs(dx) == 4) w.set(x + dx, y + 6 + t, z + dz, wl);
        }
        for (int dx = -5; dx <= 5; dx++) w.set(x + dx, y + 10, z, "minecraft:brick_slab[type=bottom,waterlogged=false]");
        for (int dy = 7; dy <= 13; dy++) w.set(x + 2, y + dy, z - 2, "minecraft:bricks");
        w.set(x + 2, y + 14, z - 2, "minecraft:campfire[facing=north,lit=true,signal_fire=true,waterlogged=false]");
        // washing line between two poles
        for (int dy = 1; dy <= 6; dy++) { w.set(x - 6, y + dy, z + 5, "minecraft:spruce_fence"); w.set(x + 6, y + dy, z + 5, "minecraft:spruce_fence"); }
        for (int t = -5; t <= 5; t++) {
            w.set(x + t, y + 6, z + 5, "minecraft:chain[axis=x,waterlogged=false]");
            if ((t & 1) == 1 || t == -4) w.set(x + t, y + 5, z + 5, "minecraft:" + Sh.TOY[(t + 8 + k) % 8] + "_wool");
        }
    }

    /** Shipwreck in the Sky: a broken bow section drifting in cloud. */
    private static void hull(W w, int x, int y, int z, int k, int side) {
        int len = 12;
        for (int t = 0; t < len; t++) {
            double half = 4.5 * Math.sqrt(1 - Math.pow(t / (double) len, 2));
            int hz = z - len / 2 + t;
            for (int dy = -4; dy <= 3; dy++) {
                double hw = half * Math.sqrt(Math.max(0, 1 - Math.pow((dy + 4) / 9.0 - 0.9, 2) * 0.7));
                int hwi = (int) Math.round(hw);
                if (hwi < 0) continue;
                for (int dx = -hwi; dx <= hwi; dx++) {
                    boolean shell = Math.abs(dx) == hwi || dy == -4;
                    if (shell) w.set(x + dx, y + dy, hz, dy == -2 ? "minecraft:stripped_spruce_wood[axis=y]" : "minecraft:dark_oak_planks");
                    else if (dy == 0) w.set(x + dx, y, hz, "minecraft:spruce_planks");
                }
            }
            // broken off edge: jagged
            if (t == 0) for (int dx = -4; dx <= 4; dx++) if (W.h(x + dx, y, hz, 31) < 0.5) w.set(x + dx, y + 1, hz, "minecraft:dark_oak_fence");
        }
        for (int dy = 1; dy <= 14; dy++) w.set(x, y + dy, z, "minecraft:stripped_dark_oak_log[axis=y]");
        for (int dx = -5; dx <= 5; dx++) w.set(x + dx, y + 11, z, "minecraft:stripped_dark_oak_log[axis=x]");
        for (int dx = -4; dx <= 4; dx++) for (int dy = 5; dy <= 10; dy++)
            if (W.h(x + dx, y + dy, z, 33) > 0.12) w.set(x + dx, y + dy, z + 1, dy == 8 ? "minecraft:red_wool" : "minecraft:white_wool");
        w.set(x, y + 15, z, "minecraft:lantern[hanging=false,waterlogged=false]");
        // cloud bank under the keel
        for (int i = 0; i < 6; i++) {
            int cx = x + (int) ((W.h(i, k, x, 34) - 0.5) * 16), cz = z + (int) ((W.h(i, k, z, 35) - 0.5) * 16);
            sphere(w, cx, y - 7 - (i & 1), cz, 2.5 + W.h(i, x, z, 36) * 1.5, "minecraft:white_concrete_powder", false);
        }
    }

    /** Clockwork Workshop: a floating clock face or a pair of meshing gears. */
    private static void clock(W w, int x, int y, int z, int k) {
        if ((k & 1) == 0) {
            int cy = y + 8;
            vdisc(w, x, cy, z, 8.4, "minecraft:polished_deepslate");
            vdisc(w, x, cy, z - 1, 7.4, "minecraft:smooth_quartz");
            for (int hr = 0; hr < 12; hr++) {
                double a = hr * Math.PI / 6;
                w.set(x + (int) Math.round(Math.sin(a) * 6), cy + (int) Math.round(Math.cos(a) * 6), z - 2, hr % 3 == 0 ? "minecraft:gold_block" : "minecraft:black_concrete");
            }
            for (int t = 0; t <= 5; t++) w.set(x, cy + t, z - 2, "minecraft:black_concrete");
            for (int t = 0; t <= 3; t++) w.set(x + t, cy - t / 2, z - 2, "minecraft:black_concrete");
            w.set(x, cy, z - 2, "minecraft:gold_block");
            for (int dy = -10; dy < -7; dy++) w.set(x, cy + dy, z, "minecraft:chain[axis=y,waterlogged=false]");
            w.set(x, cy - 11, z, "minecraft:lantern[hanging=true,waterlogged=false]");
        } else {
            gear(w, x - 4, y + 6, z, 6, "minecraft:copper_block", "minecraft:cut_copper");
            gear(w, x + 6, y + 10, z, 4, "minecraft:gold_block", "minecraft:raw_gold_block");
            for (int dz = -2; dz <= 2; dz++) { w.set(x - 4, y + 6, z + dz, "minecraft:iron_block"); w.set(x + 6, y + 10, z + dz, "minecraft:iron_block"); }
        }
    }

    private static void gear(W w, int cx, int cy, int z, int r, String body, String rim) {
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dy = -r - 1; dy <= r + 1; dy++) {
            double d = Math.sqrt(dx * dx + dy * dy), a = Math.atan2(dy, dx);
            boolean tooth = d <= r + 1.2 && d > r - 0.2 && ((int) Math.floor((a + Math.PI) / (Math.PI / (r + 2)))) % 2 == 0;
            if (d <= r - 0.3 && d > 1.5 && !(d < r - 1.5 && Math.abs(dx) > 1 && Math.abs(dy) > 1)) w.set(cx + dx, cy + dy, z, body);
            else if (tooth) w.set(cx + dx, cy + dy, z, rim);
        }
    }

    /** Crystal Mine: a cracked-open geode or a cluster of floating crystal shards. */
    private static void geode(W w, int x, int y, int z, int k) {
        if ((k & 1) == 0) {
            int cy = y + 4;
            sphere(w, x, cy, z, 6.5, "minecraft:smooth_basalt", true);
            sphere(w, x, cy, z, 5.5, "minecraft:calcite", true);
            sphere(w, x, cy, z, 4.5, "minecraft:amethyst_block", true);
            // crack it open on the side facing the course
            int open = x < 0 ? 1 : -1;
            for (int dx = 2; dx <= 7; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++)
                if (dy * dy + dz * dz <= 9 - dx / 2) w.set(x + open * dx, cy + dy, z + dz, null);
            for (int dy = -3; dy <= 3; dy += 2) w.set(x, cy + dy, z + (dy > 0 ? 3 : -3), "minecraft:amethyst_cluster[facing=" + (dy > 0 ? "north" : "south") + ",waterlogged=false]");
            w.set(x, cy - 3, z, "minecraft:budding_amethyst");
            w.set(x, cy - 2, z, "minecraft:amethyst_cluster[facing=up,waterlogged=false]");
        } else {
            for (int i = 0; i < 4; i++) {
                int sx = x + (int) ((W.h(i, k, x, 41) - 0.5) * 12), sz = z + (int) ((W.h(i, k, z, 42) - 0.5) * 12);
                int sy = y + (int) (W.h(i, x, z, 43) * 10);
                int len = 5 + (int) (W.h(i, sx, sz, 44) * 6);
                for (int t = -len; t <= len; t++) {
                    int rr = t < -len + 2 || t > len - 2 ? 0 : 1;
                    for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) if (Math.abs(dx) + Math.abs(dz) <= rr)
                        w.set(sx + dx, sy + t, sz + dz, Math.abs(t) < 2 ? "minecraft:purple_stained_glass" : "minecraft:amethyst_block");
                }
            }
        }
    }

    /** A tall slender column in the course's own style (quartz shaft, copper cap, lantern). */
    private static void column(W w, Set<Long> occ, int x, int y, int z, int s) {
        if (!clear(occ, x, z, 2)) return;
        for (int dy = -30; dy <= 22; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
            if (dy < -26 && corner) continue;
            w.set(x + dx, y + dy, z + dz, corner ? "minecraft:quartz_pillar[axis=y]" : (dy % 6 == 0 ? "minecraft:chiseled_quartz_block" : "minecraft:smooth_quartz"));
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            if (Math.abs(dx) + Math.abs(dz) <= 3) w.set(x + dx, y + 23, z + dz, "minecraft:waxed_cut_copper");
        w.set(x, y + 24, z, "minecraft:waxed_copper_block");
        w.set(x, y + 25, z, "minecraft:lantern[hanging=false,waterlogged=false]");
        mark(occ, x, z, 4);
    }

    // ------------------------------------------------------------------ ending scenery

    private static void meadow(W w, int x, int y, int z, int t) {
        island(w, x, y, z, 7, "minecraft:grass_block[snowy=false]", "minecraft:dirt", 51 + t);
        // oak tree with a beehive
        for (int dy = 1; dy <= 6; dy++) w.set(x, y + dy, z, "minecraft:oak_log[axis=y]");
        for (int dx = -3; dx <= 3; dx++) for (int dy = 5; dy <= 9; dy++) for (int dz = -3; dz <= 3; dz++)
            if (dx * dx + (dy - 7) * (dy - 7) * 2 + dz * dz <= 11 && !(dx == 0 && dz == 0 && dy <= 6))
                w.set(x + dx, y + dy, z + dz, "minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]");
        w.set(x + 1, y + 4, z, "minecraft:bee_nest[facing=east,honey_level=3]");
        String[] fl = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:oxeye_daisy", "minecraft:allium"};
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            if (dx * dx + dz * dz > 30 || (Math.abs(dx) < 2 && Math.abs(dz) < 2)) continue;
            if (w.get(x + dx, y, z + dz) != null && w.air(x + dx, y + 1, z + dz) && W.h(x + dx, y, z + dz, 55) < 0.3)
                w.set(x + dx, y + 1, z + dz, fl[(int) (W.h(x + dx, y, z + dz, 56) * fl.length)]);
        }
    }

    private static void iceberg(W w, int x, int y, int z, int t) {
        for (int dy = -8; dy <= 9; dy++) {
            double r = dy < 0 ? 6 + dy * 0.5 : 6 - dy * 0.6;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                double n = W.h(x + dx, y + dy, z + dz, 61 + t);
                if (dx * dx + dz * dz <= (r + n) * (r + n) - 1)
                    w.set(x + dx, y + dy, z + dz, dy > 6 ? "minecraft:snow_block" : n < 0.25 ? "minecraft:blue_ice" : "minecraft:packed_ice");
            }
        }
    }

    private static void library(W w, int x, int y, int z, int t) {
        island(w, x, y, z, 4, "minecraft:dark_oak_planks", "minecraft:stone_bricks", 71 + t);
        int top = 10 + t * 4;
        for (int dy = 1; dy <= top; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) w.set(x + dx, y + dy, z + dz, "minecraft:dark_oak_log[axis=y]");
            else if (Math.abs(dx) == 2 || Math.abs(dz) == 2) w.set(x + dx, y + dy, z + dz, dy % 5 == 0 ? "minecraft:dark_oak_planks" : "minecraft:bookshelf");
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) w.set(x + dx, y + top + 1, z + dz, "minecraft:dark_oak_slab[type=bottom,waterlogged=false]");
        w.set(x, y + top + 2, z, "minecraft:lectern[facing=north,has_book=true,powered=false]");
        // loose pages drifting off
        for (int i = 0; i < 5; i++) w.set(x + 4 + i * 2, y + top - i * 2, z - 3 + i, "minecraft:white_wool");
    }
}
