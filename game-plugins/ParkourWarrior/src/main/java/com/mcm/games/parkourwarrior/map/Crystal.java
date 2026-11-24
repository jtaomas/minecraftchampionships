package com.mcm.games.parkourwarrior.map;

/** Zone 5 - Crystal Mine: amethyst pillars in geodes, hanging lantern cages, a rhythm lift, rail trestles, dripstone, crystal walls. */
final class Crystal {
    private Crystal() {}

    /** A geode lump: shell of smooth basalt, calcite layer, amethyst core; radius r centred at (cx, cy, cz). */
    static void geode(Ob o, int cx, int cy, int cz, double r) {
        int R = (int) Math.ceil(r);
        for (int x = -R; x <= R; x++) for (int y = -R; y <= R; y++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + y * y * 1.3 + z * z) + W.h(o.wx(cx + x, cz + z), cy + y, o.wz(cx + x, cz + z), 51) * 0.6;
            if (d > r) continue;
            o.set(cx + x, cy + y, cz + z, d > r - 0.9 ? "smooth_basalt" : d > r - 1.8 ? "calcite" : "amethyst_block");
        }
    }

    /** Amethyst pillar: top cells (x0..x1, z) at yTop, column down to a geode lump. */
    static void crystalPillar(Ob o, int x0, int x1, int z, int yTop) {
        for (int x = x0; x <= x1; x++) for (int y = yTop - 4; y <= yTop; y++) o.set(x, y, z, y == yTop ? "amethyst_block" : (y == yTop - 2 ? "budding_amethyst" : "amethyst_block"));
        geode(o, x0, yTop - 6, z, 2.4);
        o.set(x0, yTop - 9, z, "pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]");
    }

    // ---------------------------------------------------------------- M5-1 (POST)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Geode Steps", "POST");
        int[][] p = {{0, 5, 0}, {1, 9, 1}, {0, 13, 1}, {-1, 17, 2}, {0, 21, 2}};
        for (int[] c : p) {
            crystalPillar(o, c[0], c[0] + 1, c[1], c[2]);
            o.area(c[0], c[2], c[1], c[0] + 1, c[1]);
        }
        o.end(0, 2, 25);
        Sh.padBase(o, 0, 2, 25, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }

    /** Lantern cage: copper lid 3x3 at y (centre carries the chain), lantern core and bars below. Returns ring cells. */
    static int[][] cage(Ob o, int cx, int y, int cz, int cableY) {
        java.util.List<int[]> ring = new java.util.ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            o.set(cx + dx, y, cz + dz, (dx == 0 || dz == 0) ? "waxed_cut_copper" : "waxed_copper_block");
            o.set(cx + dx, y - 1, cz + dz, dx == 0 && dz == 0 ? "sea_lantern" : "iron_bars");
            o.set(cx + dx, y - 2, cz + dz, dx == 0 && dz == 0 ? "sea_lantern" : "iron_bars");
            o.set(cx + dx, y - 3, cz + dz, "waxed_cut_copper_slab[type=top,waterlogged=false]");
            if (!(dx == 0 && dz == 0)) ring.add(new int[]{cx + dx, y, cz + dz});
        }
        o.set(cx, y - 4, cz, "lantern[hanging=true,waterlogged=false]");
        Sh.chain(o, cx, y + 1, cableY - 1, cz);
        return ring.toArray(new int[0][]);
    }

    // ---------------------------------------------------------------- M5-2 (HANG)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Lantern Line", "HANG");
        int cable = 7;
        int[][] p = {{0, 5, 0}, {1, 11, 0}, {-1, 17, 1}, {0, 23, 1}};
        for (int z = 2; z <= 26; z++) o.set(0, cable + 1, z, "stripped_dark_oak_log[axis=z]");
        for (int[] c : p) {
            o.cells(Ob.T.AREA, cage(o, c[0], c[2], c[1], cable + 1));
            for (int x = Math.min(0, c[0]); x <= Math.max(0, c[0]); x++) o.set(x, cable + 1, c[1], "stripped_dark_oak_log[axis=x]");
            o.set(c[0], cable, c[1], "chain[axis=y,waterlogged=false]");
        }
        // timber gantry posts at both ends
        for (int z : new int[]{2, 26}) for (int sx = -3; sx <= 3; sx += 6) for (int y = -8; y <= cable + 1; y++) o.set(sx, y, z, "stripped_dark_oak_log[axis=y]");
        for (int z : new int[]{2, 26}) for (int x = -3; x <= 3; x++) o.set(x, cable + 1, z, "stripped_dark_oak_log[axis=x]");
        o.end(0, 1, 28);
        Sh.padBase(o, 0, 1, 28, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }

    // ---------------------------------------------------------------- M5-3 (TIMED lift)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Rattling Lift", "TIMED");
        int[][] p = {{4, 0}, {7, 1}, {11, 2}, {14, 2}, {18, 2}, {21, 3}};
        // inclined shaft: timber arches over the path
        for (int k = 0; k < p.length; k++) {
            int z = p[k][0], y = p[k][1];
            for (int yy = y - 6; yy <= y + 5; yy++) for (int sx = -3; sx <= 3; sx += 6) o.set(sx, yy, z, "stripped_spruce_log[axis=y]");
            for (int x = -3; x <= 3; x++) o.set(x, y + 6, z, "stripped_spruce_log[axis=x]");
            o.set(0, y + 5, z, "lantern[hanging=true,waterlogged=false]");
            // the lift cage is a single copper step on a chain (the old side guide rails were a bypass and are gone)
            for (int yy = y + 1; yy <= y + 5; yy++) o.set(1, yy, z, "chain[axis=y,waterlogged=false]");
            o.timed("waxed_cut_copper", 90, 22, Math.floorMod(-k * 15, 90), 15, null, new int[]{0, y, z});
        }
        for (int z = 2; z <= 23; z++) o.set(-3, -8, z, "stripped_spruce_log[axis=z]");
        for (int z = 2; z <= 23; z++) o.set(3, -8, z, "stripped_spruce_log[axis=z]");
        o.end(0, 4, 25);
        Sh.padBase(o, 0, 4, 25, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }

    // ---------------------------------------------------------------- B5-1 (BEAM)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Runaway Rails", "BEAM");
        o.difficultyTag = "HARD";
        int[][] seg = {{0, 4, 5, 0}, {1, 10, 11, 0}, {1, 15, 16, 1}, {0, 21, 22, 1}};  // x, z0, z1, y
        for (int[] g : seg) {
            for (int z = g[1]; z <= g[2]; z++) {
                o.set(g[0], g[3], z, "dark_oak_planks");
                o.set(g[0], g[3] + 1, z, "rail[shape=north_south,waterlogged=false]");
                if (z == g[1] || z == g[2]) for (int y = g[3] - 1; y >= g[3] - 8; y--) o.set(g[0], y, z, "dark_oak_fence");
                o.set(g[0] - 1, g[3] - 2, z, "dark_oak_slab[type=top,waterlogged=false]");
                o.set(g[0] + 1, g[3] - 2, z, "dark_oak_slab[type=top,waterlogged=false]");
                o.set(g[0], g[3] - 1, z, "stripped_dark_oak_log[axis=z]");
            }
            for (int x = g[0] - 1; x <= g[0] + 1; x++) o.set(x, g[3] - 5, (g[1] + g[2]) / 2, "stripped_dark_oak_log[axis=x]");
            o.area(g[0], g[3], g[1], g[0], g[2]);
        }
        // a derailed minecart body tipped over below the line (scenery)
        for (int x = 3; x <= 4; x++) for (int z = 14; z <= 15; z++) o.set(x + 1, -7, z, "iron_block");
        o.end(0, 1, 27);
        Sh.padBase(o, 0, 1, 27, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }

    // ---------------------------------------------------------------- B5-2 (DROP)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Dripstone Descent", "DROP");
        o.difficultyTag = "MEDIUM";
        int[][] p = {{0, 5, -1}, {2, 11, -4}, {-1, 17, -7}, {2, 23, -10}, {0, 29, -13}};
        for (int[] c : p) {
            for (int y = c[2] - 9; y <= c[2]; y++) {
                int r = (c[2] - y) / 4;
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r) o.set(c[0] + dx, y, c[1] + dz, ((y + dx) & 3) == 0 ? "tuff" : "dripstone_block");
            }
            o.area(c[0], c[2], c[1], c[0], c[1]);
        }
        // stalactites hanging from a ceiling high above
        for (int z = 3; z <= 23; z++) for (int x = -4; x <= 4; x++) o.set(x, 6, z, ((x + z) & 3) == 0 ? "tuff" : "dripstone_block");
        for (int[] t : new int[][]{{-3, 6, 3}, {3, 11, 4}, {-2, 19, 2}, {3, 22, 3}, {-4, 14, 3}}) {
            for (int k = 1; k <= t[2]; k++) o.set(t[0], 6 - k, t[1], "pointed_dripstone[thickness=" + (k == t[2] ? "tip" : k == 1 ? "base" : "middle") + ",vertical_direction=down,waterlogged=false]");
        }
        o.end(0, -14, 33);
        Sh.padBase(o, 0, -14, 33, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }

    // ---------------------------------------------------------------- B5-2 (NEO, silver)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Crystal Curtain", "NEO");
        o.difficultyTag = "HARD";
        // geode wall on the left with one-wide amethyst ledges (3 long); single crystal blades stand between the
        // first ledges, flush with their outer edge (two single neos), then three 3+1 jumps up the bare ledges
        for (int z = 1; z <= 35; z++) for (int y = -8; y <= 9; y++) for (int x = -6; x <= -3; x++)
            o.set(x, y, z, x == -3 ? (W.h(x, y, z, 5) < 0.3 ? "amethyst_block" : "calcite") : x == -6 ? "smooth_basalt" : "calcite");
        // ledge: z0, y, blade after it (1) or none (0)
        int[][] L = {{4, 0, 1}, {8, 0, 1}, {12, 0, 0}, {18, 1, 0}, {24, 2, 0}, {30, 3, 0}};
        for (int i = 0; i < L.length; i++) {
            int z0 = L[i][0], y = L[i][1];
            for (int z = z0; z < z0 + 3; z++) { o.set(-2, y, z, "amethyst_block"); o.set(-2, y - 1, z, "calcite"); }
            Ob.Node n = o.area(-2, y, z0, -2, z0 + 2);
            if (i > 0 && L[i - 1][2] > 0) n.neo = true;
            if (L[i][2] > 0) {
                int bz = z0 + 3;
                for (int yy = y - 1; yy <= y + 4; yy++) o.set(-2, yy, bz, yy == y + 4 ? "budding_amethyst" : "purple_stained_glass");
                o.set(-2, y + 5, bz, "amethyst_cluster[facing=up,waterlogged=false]");
            }
        }
        o.end(-1, 3, 36);
        Sh.padBase(o, -1, 3, 36, "polished_deepslate", "deepslate_tiles", "cobbled_deepslate", 3);
        return o;
    }
}
