package com.mcm.games.parkourwarrior.map;

/** Zone 3 - Shipwreck in the Sky: a toppled mast, a rope bridge, cargo nets, barrels, a gun deck, hull ribs. */
final class Shipwreck {
    private Shipwreck() {}

    static void sail(Ob o, int x0, int x1, int yTop, int z, int drop) {
        for (int x = x0; x <= x1; x++) {
            int len = drop - Math.abs(x - (x0 + x1) / 2) / 2 - (int) (W.h(o.wx(x, z), 0, o.wz(x, z), 41) * 2);
            for (int y = yTop; y > yTop - len; y--) o.set(x, y, z, W.h(o.wx(x, z), y, o.wz(x, z), 43) < 0.12 ? "light_gray_wool" : "white_wool");
        }
    }

    // ---------------------------------------------------------------- M3-1 (BEAM)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Fallen Mainmast", "BEAM");
        o.easy = true;
        // three broken mast pieces (spruce logs) stepping down, a crow's nest between the first two
        for (int z = 2; z <= 8; z++) o.set(0, 0, z, "stripped_spruce_log[axis=z]");
        o.set(0, 0, 9, "spruce_fence");
        o.area(0, 0, 2, 0, 8);
        // crow's nest: round plank floor with a barrel hoop below
        for (int x = -1; x <= 1; x++) for (int z = 10; z <= 12; z++) o.set(x, -1, z, "spruce_planks");
        for (int x = -2; x <= 2; x++) for (int z = 9; z <= 13; z++) if ((Math.abs(x) == 2 || z == 9 || z == 13) && !(Math.abs(x) == 2 && (z == 9 || z == 13))) {
            o.set(x, -1, z, "barrel[facing=up,open=false]");
            o.set(x, -2, z, "spruce_planks");
            o.safe(x, -1, z, x, z);
        }
        for (int x = -1; x <= 1; x++) for (int z = 10; z <= 12; z++) o.set(x, -3, z, "dark_oak_planks");
        o.set(0, -4, 11, "spruce_fence"); o.set(0, -5, 11, "lantern[hanging=true,waterlogged=false]");
        o.area(-1, -1, 10, 1, 12);
        for (int z = 14; z <= 20; z++) o.set(0, -2, z, "stripped_spruce_log[axis=z]");
        o.area(0, -2, 14, 0, 20);
        for (int z = 23; z <= 27; z++) o.set(1, -3, z, "stripped_spruce_log[axis=z]");
        o.area(1, -3, 23, 1, 27);
        // yard arms crossing under the mast with torn sails, rigging chains
        for (int[] y : new int[][]{{4, -1}, {17, -3}}) {
            for (int x = -5; x <= 5; x++) o.set(x, y[1], y[0], x == 0 ? "stripped_spruce_log[axis=z]" : "stripped_spruce_log[axis=x]");
            sail(o, -4, 4, y[1] - 1, y[0] + 1, 6);
            o.set(-5, y[1] - 1, y[0], "lantern[hanging=true,waterlogged=false]");
            o.set(5, y[1] - 1, y[0], "lantern[hanging=true,waterlogged=false]");
        }
        for (int z = 24; z <= 26; z++) o.set(0, -4, z, "spruce_slab[type=top,waterlogged=false]");
        sail(o, -3, 3, -5, 25, 4);
        o.end(0, -3, 30);
        Sh.padBase(o, 0, -3, 30, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- M3-2 (GAP)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Plankless Rope Bridge", "GAP");
        o.easy = true;
        int[][] pl = {{3, 4, 0}, {7, 7, -1}, {10, 11, -1}, {15, 15, -1}, {18, 19, 0}, {22, 22, 0}};
        // anchor posts
        for (int z : new int[]{2, 24}) for (int sx = -2; sx <= 2; sx += 4) {
            for (int y = -5; y <= 4; y++) o.set(sx, y, z, "stripped_dark_oak_log[axis=y]");
            o.set(sx, 5, z, "lantern[hanging=false,waterlogged=false]");
        }
        // hand ropes: fences sagging between the posts
        for (int z = 2; z <= 24; z++) {
            int sag = (z > 5 && z < 21) ? 1 : 0;
            for (int sx = -2; sx <= 2; sx += 4) o.set(sx, 2 - sag, z, "spruce_fence");
        }
        for (int[] p : pl) {
            for (int z = p[0]; z <= p[1]; z++) {
                for (int x = -1; x <= 1; x++) o.set(x, p[2], z, z == p[0] ? "spruce_planks" : "stripped_spruce_wood[axis=x]");
                int sag = (z > 5 && z < 21) ? 1 : 0;
                for (int sx = -2; sx <= 2; sx += 4) for (int y = p[2] - 1; y < 2 - sag; y++) o.set(sx, y, z, "spruce_fence");
                for (int sx = -2; sx <= 2; sx += 4) o.set(sx, p[2] - 1, z, "spruce_fence");
            }
            o.area(-1, p[2], p[0], 1, p[1]);
        }
        // dangling broken planks below the gaps (well under the deck)
        for (int z : new int[]{5, 13, 16}) { Sh.chain(o, -1, -4, -2, z); o.set(-1, -5, z, "spruce_trapdoor[facing=north,half=bottom,open=true]"); }
        o.end(0, 0, 26);
        Sh.padBase(o, 0, 0, 26, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- M3-3 (LADDER catches)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Cargo Net Catch", "LADDER");
        o.easy = true;
        // a sky-ship's hull wall on the right with three cargo nets
        for (int z = 1; z <= 19; z++) for (int y = -9; y <= 6; y++) {
            int depth = y < -5 ? (y + 9) / 2 : 2;
            for (int x = 3; x <= 3 + depth; x++) {
                String st = (y & 3) == 0 ? "stripped_dark_oak_log[axis=z]" : (y == 6 ? "dark_oak_planks" : "spruce_planks");
                o.set(x, y, z, st);
            }
        }
        for (int z = 1; z <= 19; z++) o.set(3, 7, z, (z & 1) == 0 ? "dark_oak_fence" : "spruce_fence");
        int[][] nets = {{4, -1, 2}, {9, 0, 4}, {14, 1, 3}}; // z, bottom, top rung
        for (int[] n : nets) {
            for (int dz = -1; dz <= 1; dz++) if (dz != 0) for (int y = n[1]; y <= n[2]; y++) o.set(2, y, n[0] + dz, "ladder[facing=west,waterlogged=false]");
            o.set(2, n[2] + 1, n[0], "lantern[hanging=false,waterlogged=false]");
            o.set(3, n[2] + 1, n[0], "spruce_planks");
            o.set(2, n[2] + 1, n[0], null);
        }
        o.ladder(2, -1, 2, 4, "west");
        crates(o, 0, 1, 5, 6, 2);
        o.area(0, 2, 5, 1, 6);
        o.ladder(2, 0, 4, 9, "west");
        crates(o, 0, 1, 10, 11, 4);
        o.area(0, 4, 10, 1, 11);
        o.ladder(2, 1, 3, 14, "west");
        crates(o, 0, 1, 15, 16, 3);
        o.area(0, 3, 15, 1, 16);
        o.end(0, 0, 20);
        Sh.padBase(o, 0, 0, 20, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    /** Cargo crates against the hull: x0..x1, z0..z1, top at yTop, down 4. */
    static void crates(Ob o, int x0, int x1, int z0, int z1, int yTop) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = yTop - 4; y <= yTop; y++) {
            String st = y == yTop ? "spruce_planks" : (y == yTop - 2 ? "barrel[facing=east,open=false]" : "stripped_spruce_wood[axis=y]");
            o.set(x, y, z, st);
        }
        o.set(x1 + 1, yTop - 4, z0, "spruce_planks");
    }

    // ---------------------------------------------------------------- B3-1 (POST)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Barrel Scatter", "POST");
        o.difficultyTag = "MEDIUM";
        int[][] p = {{0, 5, 0}, {1, 9, 1}, {0, 13, 1}, {-1, 17, 0}, {0, 21, 1}};
        for (int[] b : p) {
            for (int y = b[2] - 4; y <= b[2]; y++) o.set(b[0], y, b[1], y == b[2] ? "barrel[facing=up,open=false]" : "barrel[facing=north,open=false]");
            for (int[] d : new int[][]{{1, 0}, {0, 1}}) for (int y = b[2] - 4; y <= b[2] - 3; y++) o.set(b[0] + d[0], y, b[1] + d[1], "barrel[facing=east,open=false]");
            o.set(b[0], b[2] - 5, b[1], "chain[axis=y,waterlogged=false]");
            o.set(b[0], b[2] - 6, b[1], "chain[axis=y,waterlogged=false]");
            o.area(b[0], b[2], b[1], b[0], b[1]);
        }
        o.end(0, 1, 25);
        Sh.padBase(o, 0, 1, 25, "spruce_planks", "barrel[facing=up,open=false]", "spruce_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B3-2 (HEAD)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Gun Deck Duck", "HEAD");
        o.difficultyTag = "HARD";
        int[][] c = {{4, 0}, {8, 0}, {12, 0}, {16, -1}, {20, -1}};
        // upper deck ceiling (planks with beams), hull sides with gunports
        for (int z = 4; z <= 22; z++) {
            int cy = z <= 15 ? 3 : 2;
            for (int x = -4; x <= 4; x++) o.set(x, cy, z, (z & 3) == 0 ? "stripped_dark_oak_log[axis=x]" : "spruce_planks");
            for (int x = -4; x <= 4; x++) o.set(x, cy + 1, z, (x + z & 1) == 0 ? "dark_oak_planks" : "spruce_planks");
            for (int sx = -5; sx <= 5; sx += 10) for (int y = -4; y <= cy + 1; y++) {
                boolean port = (z % 4 == 0) && y >= 0 && y <= 1;
                o.set(sx, y, z, port ? null : (y & 1) == 0 ? "dark_oak_planks" : "spruce_planks");
            }
        }
        for (int[] k : c) {
            int z = k[0], y = k[1];
            for (int x = -1; x <= 1; x++) o.set(x, y, z, x == 1 ? "coal_block" : "polished_blackstone");
            o.set(2, y, z, "polished_blackstone_wall");
            o.set(-1, y - 1, z, "dark_oak_planks");
            o.set(0, y - 1, z, "dark_oak_planks");
            o.set(1, y - 1, z, "stripped_dark_oak_log[axis=z]");
            for (int yy = y - 2; yy >= -5; yy--) o.set(-1, yy, z, "dark_oak_fence");
            o.area(-1, y, z, 1, z);
        }
        o.end(0, -1, 24);
        Sh.padBase(o, 0, -1, 24, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B3-3 (NEO)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Hull Rib Run", "NEO");
        o.difficultyTag = "HARD";
        // a broken keel that steps up, crossed by rib frames from alternating sides
        int[] ribs = {4, 9, 14, 19};
        int[] ky = {0, 1, 1, 2, 2};
        int prev = 2;
        for (int i = 0; i <= ribs.length; i++) {
            int end = i < ribs.length ? ribs[i] - 1 : prev + 1;
            int y = ky[i];
            int from = i == 0 ? 2 : Math.max(prev, end - 1);
            for (int z = from; z <= end; z++) { o.set(0, y, z, "stripped_dark_oak_log[axis=z]"); o.set(0, y - 1, z, "dark_oak_planks"); }
            for (int z = from; z <= end; z++) for (int yy = y - 2; yy >= -4; yy--) o.set(0, yy, z, "dark_oak_fence");
            Ob.Node n = o.area(0, y, from, 0, end);
            if (i > 0) n.neo = true;
            if (i < ribs.length) {
                int rz = ribs[i], side = (i & 1) == 0 ? -1 : 1;
                for (int dz = 0; dz <= 1; dz++) {
                    int z = rz + dz;
                    o.set(0, y, z, "dark_oak_planks");
                    o.set(0, y - 1, z, "dark_oak_planks");
                    for (int yy = y + 1; yy <= y + 4; yy++) { o.set(0, yy, z, "stripped_spruce_log[axis=y]"); o.set(side, yy, z, "stripped_spruce_log[axis=y]"); }
                    for (int yy = y + 4; yy <= y + 7; yy++) o.set(2 * side, yy, z, "stripped_spruce_log[axis=y]");
                    for (int yy = y + 7; yy <= y + 10; yy++) o.set(3 * side, yy, z, "stripped_spruce_log[axis=y]");
                }
                o.set(3 * side, y + 11, rz, "lantern[hanging=false,waterlogged=false]");
                prev = rz + 2;
            }
        }
        // hull strakes far below
        for (int z = 2; z <= 24; z++) for (int sx = -1; sx <= 1; sx += 2) for (int k = 0; k < 3; k++) o.set(sx * (2 + k), -3 - k, z, k == 1 ? "spruce_planks" : "dark_oak_planks");
        o.end(0, 2, 27);
        Sh.padBase(o, 0, 2, 27, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }
}
