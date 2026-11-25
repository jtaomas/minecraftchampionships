package com.mcm.games.parkourwarrior.map;

/** Zone 3 - Shipwreck in the Sky: rigging ropes, a rope bridge, cargo nets, barrels, a gun deck, hull ribs. */
final class Shipwreck {
    private Shipwreck() {}

    static void sail(Ob o, int x0, int x1, int yTop, int z, int drop) {
        for (int x = x0; x <= x1; x++) {
            int len = drop - Math.abs(x - (x0 + x1) / 2) / 2 - (int) (W.h(o.wx(x, z), 0, o.wz(x, z), 41) * 2);
            for (int y = yTop; y > yTop - len; y--) o.set(x, y, z, W.h(o.wx(x, z), y, o.wz(x, z), 43) < 0.12 ? "light_gray_wool" : "white_wool");
        }
    }

    /** A broken mast: a spruce log column at (x, z) from y0 to y1 with a yard arm across the top. */
    static void mast(Ob o, int x, int z, int y0, int y1, int yardHalf) {
        for (int y = y0; y <= y1; y++) o.set(x, y, z, "stripped_spruce_log[axis=y]");
        for (int dx = -yardHalf; dx <= yardHalf; dx++) if (dx != 0) o.set(x + dx, y1, z, "stripped_spruce_log[axis=x]");
        o.set(x, y1 + 1, z, "lantern[hanging=false,waterlogged=false]");
    }

    // ---------------------------------------------------------------- M3-1 (VINE rigging)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Rigging Ropes", "VINE");
        // a deck plank, then three broken masts standing beside the route with vine rigging on their inner face:
        // catch the first rope on the run, let go and drift forward onto the next (longer, lower) rope twice, then
        // let go of the last one and drop onto the crow's nest
        // owner playtest: first jump slightly easier (the deck plank is two deep, so the rope catch is a 2-gap),
        // second rope three vines wide (a mast plank face either side of the mast carries the extra vines)
        for (int x = -1; x <= 1; x++) for (int z = 5; z <= 6; z++) { o.set(x, 0, z, "spruce_planks"); o.set(x, -1, z, "dark_oak_planks"); }
        for (int y = -7; y <= -2; y++) o.set(0, y, 5, "stripped_spruce_log[axis=y]");
        o.area(-1, 0, 5, 1, 6);
        mast(o, 1, 9, -10, 7, 0);
        o.vine(0, 1, 4, 9, "east");
        mast(o, -1, 13, -10, 6, 0);
        for (int y = -3; y <= 4; y++) for (int z = 12; z <= 14; z += 2) o.set(-1, y, z, "stripped_spruce_wood[axis=y]");
        Ob.Node rope2 = o.vine(0, -3, 4, 13, "west");
        for (int z = 12; z <= 14; z += 2) {
            String st = Bs.withProp("vine[east=false,north=false,south=false,up=false,west=false]", "west", "true");
            for (int y = -3; y <= 4; y++) { o.set(0, y, z, st); rope2.cells.add(new int[]{o.wx(0, z), o.wy(y), o.wz(0, z)}); }
        }
        mast(o, 1, 17, -10, 5, 0);
        o.vine(0, -6, 2, 17, "east");
        for (int x = -1; x <= 1; x++) for (int z = 20; z <= 22; z++) { o.set(x, -3, z, "spruce_planks"); o.set(x, -4, z, "dark_oak_planks"); }
        for (int y = -10; y <= -5; y++) o.set(0, y, 21, "stripped_spruce_log[axis=y]");
        o.area(-1, -3, 20, 1, 22);
        sail(o, -4, -2, 4, 15, 6);
        o.end(0, -2, 27);
        Sh.padBase(o, 0, -2, 27, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- M3-2 (GAP plank bridge)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Plankless Rope Bridge", "GAP");
        o.easy = true;
        // single planks (one deep, no run-up) left hanging in the rope bridge; every other one has sagged into a
        // half plank, so the bridge climbs half a block per three-block jump
        int[][] pl = {{4, 0, 0}, {8, 1, 1}, {12, 1, 0}, {16, 2, 1}, {20, 2, 0}, {24, 3, 1}};
        for (int z : new int[]{2, 27}) for (int sx = -2; sx <= 2; sx += 4) {
            for (int y = -6; y <= 7; y++) o.set(sx, y, z, "stripped_dark_oak_log[axis=y]");
            o.set(sx, 8, z, "lantern[hanging=false,waterlogged=false]");
        }
        // hand ropes high above the planks (chains) so nobody walks on them
        for (int z = 3; z <= 26; z++) for (int sx = -2; sx <= 2; sx += 4) o.set(sx, 7, z, "chain[axis=z,waterlogged=false]");
        for (int[] p : pl) {
            int z = p[0], y = p[1];
            for (int x = -1; x <= 1; x++) o.set(x, y, z, p[2] == 1 ? "spruce_slab[type=bottom,waterlogged=false]" : x == 0 ? "spruce_planks" : "stripped_spruce_wood[axis=x]");
                        o.set(-2, y, z, "spruce_fence");
            o.set(2, y, z, "spruce_fence");
            for (int sx = -2; sx <= 2; sx += 4) for (int yy = y + 1; yy <= 6; yy++) o.set(sx, yy, z, "chain[axis=y,waterlogged=false]");
            o.area(-1, y, z, 1, z);
        }
        for (int z : new int[]{6, 14, 19}) { Sh.chain(o, -1, -5, -3, z); o.set(-1, -6, z, "spruce_trapdoor[facing=north,half=bottom,open=true]"); }
        o.end(0, 3, 28);
        Sh.padBase(o, 0, 3, 28, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- M3-3 (LADDER cargo nets)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Cargo Net Catch", "LADDER");
        // a sky-ship's hull wall on the right with three cargo nets; each crate stack is three higher than the
        // last, so every net has to be caught on the run (3-gaps after the first) and left with a diagonal jump onto a
        // single row of crates
        for (int z = 1; z <= 26; z++) for (int y = -9; y <= 13; y++) {
            int depth = y < -5 ? (y + 9) / 2 : 2;
            for (int x = 3; x <= 3 + depth; x++) {
                String st = (y & 3) == 0 ? "stripped_dark_oak_log[axis=z]" : (y == 13 ? "dark_oak_planks" : "spruce_planks");
                o.set(x, y, z, st);
            }
        }
        for (int z = 1; z <= 26; z++) o.set(3, 14, z, (z & 1) == 0 ? "dark_oak_fence" : "spruce_fence");
        o.ladder(2, 0, 3, 4, "west");
        crates(o, 1, 1, 7, 8, 3);
        o.area(1, 3, 7, 1, 8);
        o.ladder(2, 3, 6, 12, "west");
        crates(o, 1, 1, 15, 16, 6);
        o.area(1, 6, 15, 1, 16);
        o.ladder(2, 6, 9, 20, "west");
        crates(o, 1, 1, 23, 24, 9);
        o.area(1, 9, 23, 1, 24);
        for (int z : new int[]{6, 14, 22}) o.set(3, 1, z, "lantern[hanging=false,waterlogged=false]");
        o.end(0, 9, 29);
        Sh.padBase(o, 0, 9, 29, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    /** Cargo crates against the hull: x0..x1, z0..z1, top at yTop, down 4. */
    static void crates(Ob o, int x0, int x1, int z0, int z1, int yTop) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = yTop - 4; y <= yTop; y++) {
            String st = y == yTop ? "spruce_planks" : (y == yTop - 2 ? "barrel[facing=east,open=false]" : "stripped_spruce_wood[axis=y]");
            o.set(x, y, z, st);
        }
    }

    // ---------------------------------------------------------------- B3-1 (POST barrels)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Barrel Scatter", "POST");
        o.difficultyTag = "MEDIUM";
        // single barrels on chains: 3+1, 3b, 3b, 4-1, 3+1, 3+1
        int[][] p = {{0, 5, 1}, {1, 9, 1}, {0, 13, 1}, {-1, 18, 0}, {0, 22, 1}, {1, 26, 2}};
        for (int[] b : p) {
            for (int y = b[2] - 3; y <= b[2]; y++) o.set(b[0], y, b[1], y == b[2] ? "barrel[facing=up,open=false]" : "barrel[facing=north,open=false]");
            for (int y = b[2] - 7; y <= b[2] - 4; y++) o.set(b[0], y, b[1], "chain[axis=y,waterlogged=false]");
            o.set(b[0], b[2] - 8, b[1], "spruce_planks");
            o.area(b[0], b[2], b[1], b[0], b[1]);
        }
        o.end(0, 2, 30);
        Sh.padBase(o, 0, 2, 30, "spruce_planks", "barrel[facing=up,open=false]", "spruce_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B3-2 (HEAD gun deck)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Gun Deck Duck", "HEAD");
        o.difficultyTag = "MEDIUM";
        // gun carriages under the low deck ceiling (top-slab beams 2.5 above your feet): every jump is a
        // 2-gap head-hitter, the last one also steps down
        int[][] c = {{4, 0}, {8, 0}, {12, 0}, {16, 0}, {20, -1}};
        int cy = 3;
        for (int z = 3; z <= 23; z++) {
            for (int x = -3; x <= 3; x++) {
                int yc = cy;
                o.set(x, yc, z, (z & 3) == 0 ? "dark_oak_slab[type=top,waterlogged=false]" : "spruce_slab[type=top,waterlogged=false]");
                o.set(x, yc + 1, z, (x + z & 1) == 0 ? "dark_oak_planks" : "spruce_planks");
            }
            for (int sx = -4; sx <= 4; sx += 8) for (int y = -5; y <= cy + 1; y++)
                o.set(sx, y, z, (z % 4 == 0 && y >= 0 && y <= 1) ? "glass" : (y & 1) == 0 ? "dark_oak_planks" : "spruce_planks");
        }
        for (int[] k : c) {
            int z = k[0], y = k[1];
            for (int x = -1; x <= 1; x++) for (int dz = 0; dz <= 1; dz++) {
                o.set(x, y, z + dz, x == 1 ? "coal_block" : "polished_blackstone");
                o.set(x, y - 1, z + dz, "dark_oak_planks");
            }
            o.set(2, y, z, "polished_blackstone_wall");
            for (int yy = y - 2; yy >= -6; yy--) o.set(0, yy, z, "dark_oak_fence");
            o.area(-1, y, z, 1, z + 1);
        }
        o.end(0, -1, 25);
        Sh.padBase(o, 0, -1, 25, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B3-3 (NEO hull ribs)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Hull Rib Run", "NEO");
        o.difficultyTag = "HARD";
        // a broken keel (one wide, three-long pieces) crossed by rib frames that alternate sides: single neos
        // and double neos around single and paired ribs, alternating sides, then a running 4-1 and a 2+1
        int[][] seg = { // z0, y, rib after it: side (-1 left / 1 right / 0 none), thickness
                {3, 0, -1, 1}, {7, 0, 1, 2}, {12, 0, -1, 1}, {16, -1, 1, 2}, {21, -2, 0, 0}, {28, -3, 0, 0}};
        for (int i = 0; i < seg.length; i++) {
            int z0 = seg[i][0], y = seg[i][1];
            for (int z = z0; z <= z0 + 2; z++) {
                o.set(0, y, z, "stripped_dark_oak_log[axis=z]");
                o.set(0, y - 1, z, "dark_oak_planks");
                for (int yy = y - 2; yy >= -6; yy--) o.set(0, yy, z, "dark_oak_fence");
            }
            Ob.Node n = o.area(0, y, z0, 0, z0 + 2);
            if (i > 0 && seg[i - 1][3] > 0) n.neo = true;
            int side = seg[i][2], t = seg[i][3];
            for (int dz = 0; dz < t; dz++) {
                int z = z0 + 3 + dz;
                // the rib: from the keel line out to the side and up, closing at x = 0
                for (int yy = y - 1; yy <= y + 4; yy++) for (int k = 0; k <= 4; k++) o.set(side * k, yy, z, k == 0 ? "stripped_spruce_log[axis=y]" : "spruce_planks");
                for (int yy = y + 4; yy <= y + 8; yy++) o.set(side * 5, yy, z, "stripped_spruce_log[axis=y]");
            }
            if (t > 0) o.set(side * 5, y + 9, z0 + 3, "lantern[hanging=false,waterlogged=false]");
        }
        for (int z = 2; z <= 30; z++) for (int sx = -1; sx <= 1; sx += 2) for (int k = 0; k < 3; k++) o.set(sx * (3 + k), -8 - k, z, k == 1 ? "spruce_planks" : "dark_oak_planks");
        o.end(0, -2, 34);
        Sh.padBase(o, 0, -2, 34, "dark_oak_planks", "spruce_planks", "dark_oak_planks", 2);
        return o;
    }
}
