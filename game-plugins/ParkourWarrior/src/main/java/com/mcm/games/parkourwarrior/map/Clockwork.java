package com.mcm.games.parkourwarrior.map;

/**
 * Zone 4 - Clockwork Workshop: gear trains, an escapement with teeth that tick in and out, springs, pendulums,
 * axle posts and a cuckoo clock whose perches come out on the beat. Timed blocks live here.
 */
final class Clockwork {
    private Clockwork() {}

    /** Horizontal gear: disc radius r at y, 8 teeth at radius r+1, hub cap, axle down to an iron bracket. */
    static void gear(Ob o, int cx, int y, int cz, int r, String body, String rim, boolean register) {
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
            boolean in = Sh.inDisc(dx, dz, r);
            boolean tooth = !in && Math.max(Math.abs(dx), Math.abs(dz)) == r + 1
                    && (dx == 0 || dz == 0 || (Math.abs(dx) == Math.abs(dz) && r >= 2));
            if (!in && !tooth) continue;
            String st = tooth ? rim : (Math.abs(dx) + Math.abs(dz) <= 1 ? "waxed_copper_block" : body);
            if (in && Sh.inDisc(dx, dz, r) && !Sh.inDisc(dx, dz, r - 1) && r >= 2) st = rim;
            o.set(cx + dx, y, cz + dz, st);
            cells.add(new int[]{cx + dx, y, cz + dz});
            if (in) o.set(cx + dx, y - 1, cz + dz, "waxed_cut_copper_slab[type=top,waterlogged=false]");
        }
        for (int yy = y - 2; yy >= y - 7; yy--) o.set(cx, yy, cz, (yy & 1) == 0 ? "iron_block" : "polished_deepslate");
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { o.set(cx + d[0], y - 4, cz + d[1], "polished_deepslate"); o.set(cx + 2 * d[0], y - 4, cz + 2 * d[1], "waxed_copper_bulb[lit=true,powered=false]"); }
        o.set(cx, y - 8, cz, "lightning_rod[facing=down,powered=false,waterlogged=false]");
        if (register) o.cells(Ob.T.AREA, cells.toArray(new int[0][]));
    }

    // ---------------------------------------------------------------- M4-1 (ROUND - gear tops at varying heights)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Gear Train", "ROUND");
        // small gears only (3-wide disc, single teeth sticking out): two- and three-block jumps between them, climbing
        gear(o, 0, 0, 6, 1, "gold_block", "waxed_cut_copper", true);
        gear(o, 1, 0, 14, 1, "waxed_exposed_copper", "waxed_cut_copper", true);
        gear(o, 1, 1, 22, 1, "waxed_copper_block", "gold_block", true);
        gear(o, -1, 1, 30, 1, "gold_block", "waxed_cut_copper", true);
        // a big vertical drive wheel standing beside the train (scenery, 6+ blocks away)
        for (int dy = -7; dy <= 7; dy++) for (int dz = -7; dz <= 7; dz++) {
            double d = Math.sqrt(dy * dy + dz * dz);
            if (d > 7.4) continue;
            String st = d > 6.5 ? ((Math.round(Math.atan2(dy, dz) * 16 / Math.PI) & 1) == 0 ? "gold_block" : null)
                    : d > 5.5 ? "waxed_cut_copper" : (dy == 0 || dz == 0 || Math.abs(dy) == Math.abs(dz)) ? "waxed_exposed_cut_copper" : d < 1.5 ? "iron_block" : null;
            if (st != null) o.set(-8, dy - 1, 18 + dz, st);
        }
        o.end(0, 2, 36);
        Sh.padBase(o, 0, 2, 36, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }

    // ---------------------------------------------------------------- M4-2 (TIMED escapement teeth)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Tick-Tock Teeth", "TIMED");
        // single escapement teeth on hanging pallet arms, nothing underneath (the old side rails were a bypass):
        // the A teeth are out on 0..23 of 40, the B teeth on 20..43, a short overlap to hop across
        int[][] zs = {{4, 0}, {7, 0}, {11, 0}, {14, 1}, {18, 1}, {21, 2}, {24, 2}};
        for (int i = 0; i < zs.length; i++) {
            int z = zs[i][0], y = zs[i][1];
            boolean a = (i & 1) == 0;
            // pallet arm: a short copper bracket two blocks to the side, a bulb that lights the tooth's slot
            o.set(a ? -3 : 3, y, z, "waxed_cut_copper");
            o.set(a ? -3 : 3, y - 1, z, "waxed_copper_bulb[lit=true,powered=false]");
            for (int yy = y + 1; yy <= y + 8; yy++) o.set(a ? -3 : 3, yy, z, "chain[axis=y,waterlogged=false]");
            o.timed(a ? "gold_block" : "waxed_copper_block", 40, 23, a ? 0 : 20, 12, null, new int[]{0, y, z});
        }
        // escapement wheel standing to the left, and a clock face to the right (scenery)
        for (int dy = -6; dy <= 6; dy++) for (int dz = -6; dz <= 6; dz++) {
            double d = Math.sqrt(dy * dy + dz * dz);
            if (d <= 6.4 && d > 5.4) o.set(7, dy, 14 + dz, (dy + dz & 1) == 0 ? "white_concrete" : "white_terracotta");
            else if (d <= 5.4) o.set(7, dy, 14 + dz, d < 1 ? "gold_block" : "smooth_quartz");
        }
        for (int k = 1; k <= 4; k++) o.set(6, k, 13, "black_concrete");
        for (int k = 1; k <= 3; k++) o.set(6, 0, 13 + k, "black_concrete");
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            o.set(6, (int) Math.round(Math.sin(a) * 5), 13 + (int) Math.round(Math.cos(a) * 5), "black_concrete");
        }
        o.end(0, 2, 28);
        Sh.padBase(o, 0, 2, 28, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }

    /** A spring: slime pad in a copper cup on a coil of iron bars and chains. */
    static void spring(Ob o, int cx, int yTop, int cz) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            o.set(cx + dx, yTop, cz + dz, "slime_block");
            o.set(cx + dx, yTop - 1, cz + dz, "waxed_cut_copper");
            o.set(cx + dx, yTop - 2, cz + dz, Math.abs(dx) + Math.abs(dz) == 2 ? "waxed_copper_block" : "waxed_cut_copper_slab[type=top,waterlogged=false]");
        }
        for (int y = yTop - 2; y >= yTop - 7; y--) {
            o.set(cx, y, cz, (y & 1) == 0 ? "iron_bars" : "chain[axis=y,waterlogged=false]");
            int ph = Math.floorMod(y, 4);
            int[][] coil = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            o.set(cx + coil[ph][0], y, cz + coil[ph][1], "iron_bars");
        }
        o.set(cx, yTop - 8, cz, "waxed_copper_block");
    }

    /** Brass shelf on a wall bracket: x0..x1, z0..z1 at y. */
    static void shelf(Ob o, int x0, int x1, int z0, int z1, int y) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            o.set(x, y, z, "waxed_cut_copper");
            o.set(x, y - 1, z, Bs.stairs("waxed_cut_copper", "south", true));
        }
        for (int yy = y - 2; yy >= y - 6; yy--) o.set(x0, yy, (z0 + z1) / 2, "polished_deepslate_wall");
        o.set(x1, y + 3, z1, "air");
    }

    // ---------------------------------------------------------------- M4-3 (SLIME)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Spring Loaded", "SLIME");
        spring(o, 0, -4, 5);
        o.areaT(Ob.T.SLIME, -1, -4, 4, 1, 6);
        shelf(o, -1, 1, 9, 10, -1);
        o.area(-1, -1, 9, 1, 10);
        spring(o, 0, -5, 14);
        o.areaT(Ob.T.SLIME, -1, -5, 13, 1, 15);
        shelf(o, -1, 1, 18, 19, -2);
        o.area(-1, -2, 18, 1, 19);
        // a workbench of clock parts hanging far below (scenery)
        for (int x = -4; x <= 4; x++) for (int z = 4; z <= 20; z++) if ((x + z) % 5 == 0) o.set(x, -14, z, "dark_oak_planks");
        o.end(0, -2, 23);
        Sh.padBase(o, 0, -2, 23, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }

    // ---------------------------------------------------------------- B4-1 (TIMED pendulums)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Pendulum Hall", "TIMED");
        o.difficultyTag = "HARD";
        int beam = 7;
        for (int z = 2; z <= 24; z++) o.set(0, beam, z, "stripped_dark_oak_log[axis=z]");
        for (int z : new int[]{2, 24}) for (int y = -6; y < beam; y++) for (int sx = -4; sx <= 4; sx += 8) o.set(sx, y, z, "polished_deepslate");
        for (int z : new int[]{2, 24}) for (int x = -4; x <= 4; x++) o.set(x, beam, z, "stripped_dark_oak_log[axis=x]");
        int[] zs = {5, 10, 15, 20};
        for (int i = 0; i < zs.length; i++) {
            int z = zs[i];
            int by = i == 0 ? 0 : 1;
            Sh.chain(o, 0, 5, beam - 1, z);
            o.set(0, 4, z, "waxed_copper_block");
            // onAt(t) = (t + phase) mod period < on, so a block that comes on LATER needs a SMALLER phase
            int ph = Math.floorMod(-i * 15, 60);
            o.timed("gold_block", 60, 18, ph, 15, null, new int[]{0, by, z}, new int[]{0, by, z + 1});
            // swing extremes (scenery bobs, off the route)
            java.util.List<com.mcm.games.parkourwarrior.CourseModel.BlockPos> ps = new java.util.ArrayList<>();
            for (int sx : new int[]{-5, 5}) for (int dz = 0; dz <= 1; dz++) {
                int X = o.wx(sx, z + dz), Y = o.wy(2), Z = o.wz(sx, z + dz);
                ps.add(new com.mcm.games.parkourwarrior.CourseModel.BlockPos(X, Y, Z));
                o.timedPreview.put(W.key(X, Y, Z), "minecraft:waxed_exposed_copper");
            }
            o.timed.add(new com.mcm.games.parkourwarrior.CourseModel.Timed(o.id, ps, "minecraft:waxed_exposed_copper", 60, 20, Math.floorMod(ph + 30, 60), 0, null, null));
        }
        o.end(0, 1, 25);
        Sh.padBase(o, 0, 1, 25, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }

    // ---------------------------------------------------------------- B4-2 (POST axles)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Axle Hop", "POST");
        o.difficultyTag = "HARD";
        int[][] p = {{0, 5, 0}, {1, 9, 1}, {0, 13, 0}, {-1, 17, 1}, {0, 21, 2}};
        for (int[] a : p) {
            o.set(a[0], a[2], a[1], "iron_block");
            for (int y = a[2] - 1; y >= a[2] - 7; y--) o.set(a[0], y, a[1], (y & 1) == 0 ? "polished_deepslate" : "waxed_copper_block");
            int gy = a[2] - 3;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}})
                o.set(a[0] + d[0], gy, a[1] + d[1], Math.abs(d[0]) + Math.abs(d[1]) == 2 ? "waxed_cut_copper" : "gold_block");
            o.area(a[0], a[2], a[1], a[0], a[1]);
        }
        o.end(0, 2, 25);
        Sh.padBase(o, 0, 2, 25, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }

    // ---------------------------------------------------------------- B4-3 (TIMED cuckoo perches)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Cuckoo Chorus", "TIMED");
        o.difficultyTag = "HARD";
        // the cuckoo clock: a tall dark-oak case on the left with a pitched roof, doors at each perch
        for (int z = 2; z <= 24; z++) for (int y = -6; y <= 10; y++) for (int x = -6; x <= -3; x++) {
            boolean shell = x == -3 || y == -6 || y == 10 || z == 2 || z == 24;
            if (!shell) { o.set(x, y, z, "spruce_planks"); continue; }
            o.set(x, y, z, (z % 6 == 2) ? "stripped_dark_oak_log[axis=y]" : (y & 3) == 0 ? "dark_oak_planks" : "spruce_planks");
        }
        for (int z = 2; z <= 24; z++) for (int k = 0; k <= 3; k++) { o.set(-6 + k, 11 + k, z, Bs.stairs("dark_oak", "east", false)); }
        // every perch is one higher than the last: five single-block 3+1 jumps on the beat
        int[][] pz = {{4, 1}, {8, 2}, {12, 3}, {16, 4}, {20, 5}};
        for (int i = 0; i < pz.length; i++) {
            int z = pz[i][0], y = pz[i][1];
            // door frame (gold) around a dark doorway
            for (int yy = y + 1; yy <= y + 2; yy++) o.set(-3, yy, z, "black_concrete");
            o.set(-3, y + 3, z, "gold_block");
            o.set(-3, y + 1, z - 1, "gold_block"); o.set(-3, y + 1, z + 1, "gold_block");
            o.set(-4, y, z, "waxed_cut_copper");
            o.timed("yellow_concrete", 50, 15, Math.floorMod(-i * 12, 50), 12, null, new int[]{-2, y, z});
        }
        o.end(-1, 5, 24);
        Sh.padBase(o, -1, 5, 24, "waxed_cut_copper", "waxed_copper_block", "polished_deepslate", 2);
        return o;
    }
}
