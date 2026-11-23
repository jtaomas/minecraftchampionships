package com.mcm.games.parkourwarrior.map;

/** Zone 2 - Market Rooftops: striped awnings, chimney stacks, drainpipes, fruit crates, hanging shop signs, billboards. */
final class Market {
    private Market() {}

    static final String[] WALL = {"white_terracotta", "orange_terracotta", "yellow_terracotta", "light_gray_terracotta", "pink_terracotta"};
    static final String[] STRIPE = {"red", "lime", "blue", "orange", "magenta", "cyan"};

    /**
     * A block of houses: solid box x0..x1, z0..z1 from yBot to yTop, walls coloured per house (every hz blocks
     * along z), timber corners, windows (glass panes in the wall plane) on the face at x = face (or none),
     * a gabled roof along z above yTop. Underside tapers into rock.
     */
    static void houses(Ob o, int x0, int x1, int z0, int z1, int yBot, int yTop, int face, int hz, int salt, boolean roof) {
        for (int z = z0; z <= z1; z++) {
            int house = Math.floorDiv(z - z0, hz);
            String wall = WALL[Math.floorMod(house + salt, WALL.length)];
            boolean seam = Math.floorMod(z - z0, hz) == 0;
            for (int x = x0; x <= x1; x++) for (int y = yBot; y <= yTop; y++) {
                boolean shell = x == x0 || x == x1 || z == z0 || z == z1 || y == yTop;
                if (!shell) { o.set(x, y, z, "stripped_spruce_wood[axis=y]"); continue; }
                String s = seam || z == z0 || z == z1 ? "stripped_dark_oak_log[axis=y]" : wall;
                if (y == yBot) s = "stone_bricks";
                if (y == yTop && !seam) s = "dark_oak_planks";
                if (x == face && !seam && y > yBot + 1 && y < yTop - 1 && Math.floorMod(y - yBot, 4) >= 2 && (Math.floorMod(z - z0, hz) == 2 || Math.floorMod(z - z0, hz) == hz - 2))
                    s = "glass";
                o.set(x, y, z, s);
            }
        }
        if (roof) {
            int w = x1 - x0;
            for (int z = z0; z <= z1; z++) for (int k = 0; k <= w / 2; k++) {
                String m = Math.floorMod(Math.floorDiv(z - z0, hz) + salt, 2) == 0 ? "brick" : "dark_oak";
                o.set(x0 + k, yTop + 1 + k, z, Bs.stairs(m, "east", false));
                o.set(x1 - k, yTop + 1 + k, z, Bs.stairs(m, "west", false));
                if (x0 + k >= x1 - k) o.set(x0 + k, yTop + 1 + k, z, m.equals("brick") ? "bricks" : "dark_oak_planks");
            }
        }
        for (int d = 1; d <= 5; d++) for (int x = x0 + d / 2; x <= x1 - d / 2; x++) for (int z = z0 + d / 2; z <= z1 - d / 2; z++)
            if (W.h(o.wx(x, z), d, o.wz(x, z), 31) < 0.85 - d * 0.1) o.soft(x, yBot - d, z, d < 3 ? "cobblestone" : "tuff");
    }

    // ---------------------------------------------------------------- M2-1 (DIAG)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Awning Alley", "DIAG");
        o.easy = true;
        houses(o, -7, -3, 2, 28, -8, 6, -3, 6, 0, true);
        houses(o, 3, 7, 2, 28, -8, 6, 3, 6, 2, true);
        int[][] aw = { // side (-1 left / 1 right), z0, y
                {-1, 3, 0}, {1, 7, 1}, {-1, 11, 1}, {1, 15, 2}, {-1, 20, 2}, {1, 24, 2}};
        for (int i = 0; i < aw.length; i++) {
            int sd = aw[i][0], z0 = aw[i][1], y = aw[i][2];
            int xa = sd < 0 ? -2 : 1, xb = sd < 0 ? -1 : 2;
            String col = STRIPE[i % STRIPE.length];
            for (int z = z0; z <= z0 + 2; z++) {
                o.set(xa, y, z, ((z - z0) & 1) == 0 ? col + "_wool" : "white_wool");
                o.set(xb, y, z, ((z - z0) & 1) == 0 ? col + "_wool" : "white_wool");
                o.set(sd < 0 ? xb : xa, y - 1, z, Bs.stairs("spruce", sd < 0 ? "west" : "east", true));
                o.set(sd < 0 ? xa : xb, y - 1, z, "spruce_planks");
            }
            Sh.chain(o, sd < 0 ? xb : xa, y - 3, y - 2, z0 + 1);
            o.set(sd < 0 ? xb : xa, y - 4, z0 + 1, "lantern[hanging=true,waterlogged=false]");
            // shop door below the awning
            o.set(sd < 0 ? -3 : 3, y - 2, z0 + 1, "spruce_planks");
            o.set(sd < 0 ? -3 : 3, y - 3, z0 + 1, "spruce_planks");
            o.area(xa, y, z0, xb, z0 + 2);
        }
        o.end(0, 2, 29);
        Sh.padBase(o, 0, 2, 29, "brick_slab[type=double,waterlogged=false]", "bricks", "cobblestone", 3);
        return o;
    }

    // ---------------------------------------------------------------- M2-2 (POST)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Chimney Pots", "POST");
        o.easy = true;
        o.killDepth = 3;
        // a long terracotta-roofed hall far below, ridge at y = -4
        for (int z = 2; z <= 23; z++) for (int k = 0; k <= 6; k++) {
            String m = (z / 4 & 1) == 0 ? "brick" : "granite";
            o.set(-6 + k, -10 + k, z, Bs.stairs(m, "east", false));
            o.set(6 - k, -10 + k, z, Bs.stairs(m, "west", false));
            for (int y = -16; y < -10 + k; y++) { o.set(-6 + k, y, z, k == 0 ? "orange_terracotta" : "stripped_spruce_wood[axis=y]"); o.set(6 - k, y, z, k == 0 ? "orange_terracotta" : "stripped_spruce_wood[axis=y]"); }
        }
        for (int z = 2; z <= 23; z++) o.set(0, -3, z, "bricks");
        for (int x = -6; x <= 6; x++) for (int y = -16; y <= -10; y++) { o.set(x, y, 1, "orange_terracotta"); o.set(x, y, 24, "orange_terracotta"); }
        int[][] ch = { // x0, x1, z, top y
                {0, 1, 4, 0}, {-2, -1, 7, 0}, {0, 1, 10, 1}, {1, 2, 14, 1}, {-1, 0, 17, 1}, {0, 1, 20, 2}};
        for (int[] c : ch) {
            for (int x = c[0]; x <= c[1]; x++) {
                for (int y = -9; y <= c[3]; y++) o.set(x, y, c[2], y == c[3] ? "stone_bricks" : (y & 3) == 0 ? "mud_bricks" : "bricks");
                o.set(x, c[3] - 3, c[2] + 1, "brick_wall");
                o.set(x, c[3] - 3, c[2] - 1, "brick_wall");
            }
            o.area(c[0], c[3], c[2], c[1], c[2]);
        }
        // TV aerials and a weathervane on the ridge (below the chimney tops, never at foothold height)
        for (int z = 6; z <= 22; z += 8) { o.set(-4, -2, z, "lightning_rod[facing=up,powered=false,waterlogged=false]"); }
        o.end(0, 2, 24);
        Sh.padBase(o, 0, 2, 24, "polished_granite", "bricks", "granite", 3);
        return o;
    }

    // ---------------------------------------------------------------- M2-3 (LADDER)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Drainpipe Dash", "LADDER");
        o.easy = true;
        // porch canopy
        for (int x = -3; x <= -1; x++) for (int z = 3; z <= 5; z++) o.set(x, 0, z, z == 3 ? "spruce_slab[type=double,waterlogged=false]" : "spruce_planks");
        for (int x = -3; x <= -1; x++) { o.set(x, -1, 3, Bs.stairs("spruce", "north", true)); }
        for (int y = -6; y <= -1; y++) { o.set(-3, y, 4, "spruce_fence"); }
        o.area(-3, 0, 4, -1, 5);
        // low bakery: front wall z = 8, roof at y = 3
        houses(o, -5, 4, 8, 13, -9, 3, 99, 6, 1, false);
        for (int x = -5; x <= 4; x++) for (int z = 8; z <= 13; z++) o.set(x, 3, z, (x + z & 1) == 0 ? "smooth_stone" : "stone");
        o.ladder(-2, 1, 3, 7, "north");
        o.area(-3, 3, 8, 1, 11);
        // taller guild house behind: front wall z = 15, roof at y = 6
        houses(o, -4, 4, 15, 22, -9, 6, 99, 8, 3, false);
        for (int x = -4; x <= 4; x++) for (int z = 15; z <= 22; z++) o.set(x, 6, z, (x + z & 1) == 0 ? "polished_andesite" : "andesite");
        o.ladder(1, 4, 6, 14, "north");
        o.area(-1, 6, 15, 3, 17);
        // drainpipe hoppers (decor only, above the pipes' tops)
        o.set(-2, 4, 8, "hopper[enabled=true,facing=down]");
        o.set(1, 7, 15, "hopper[enabled=true,facing=down]");
        // parapets except the walking lane
        for (int z = 15; z <= 22; z++) { o.set(-4, 7, z, "andesite_wall"); o.set(4, 7, z, "andesite_wall"); }
        o.end(0, 6, 21);
        o.safe(-4, 6, 15, 4, 22);
        o.safe(-5, 3, 8, 4, 13);
        o.safe(-3, 0, 3, -1, 3);
        return o;
    }

    // ---------------------------------------------------------------- B2-1 (GAP)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Fruit Crate Tumble", "GAP");
        o.difficultyTag = "MEDIUM";
        int[][] cr = { // x, z, top y, fruit index, stack
                {0, 5, 0, 0, 3}, {-2, 10, -1, 1, 2}, {1, 15, -1, 2, 3}, {-1, 20, -2, 3, 2}, {1, 25, -1, 0, 3}};
        String[] fruit = {"melon", "pumpkin", "hay_block[axis=y]", "moss_block"};
        for (int[] c : cr) {
            crateStack(o, c[0], c[2], c[1], fruit[c[3]], c[4]);
            o.area(c[0], c[2], c[1], c[0] + 1, c[1] + 1);
        }
        o.end(0, -1, 30);
        Sh.padBase(o, 0, -1, 30, "spruce_planks", "stripped_spruce_wood[axis=y]", "spruce_planks", 2);
        return o;
    }

    /** 2x2 crates stacked downward from yTop (each crate 2 tall), fruit heaped on the top layer. */
    static void crateStack(Ob o, int x, int yTop, int z, String fruit, int n) {
        for (int k = 0; k < n; k++) {
            int yt = yTop - 2 * k - (k == 0 ? 0 : 0);
            int sx = k == 0 ? 0 : (k & 1);
            for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
                o.set(x + dx, yt, z + dz, k == 0 ? fruit : "spruce_planks");
                o.set(x + dx, yt - 1, z + dz, (k & 1) == 0 ? "stripped_spruce_wood[axis=y]" : "barrel[facing=up,open=false]");
            }
            if (sx != 0) { /* keep footprints aligned: no exposed ledges */ }
        }
        o.set(x, yTop - 2 * n, z, "spruce_fence");
        o.set(x + 1, yTop - 2 * n, z + 1, "spruce_fence");
    }

    // ---------------------------------------------------------------- B2-2 (HANG)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Swinging Shop Signs", "HANG");
        o.difficultyTag = "MEDIUM";
        int beamY = 6;
        // two timber gallows posts and the overhead beam
        for (int z : new int[]{2, 26}) for (int y = -6; y <= beamY; y++) o.set(-4, y, z, "stripped_dark_oak_log[axis=y]");
        for (int z = 2; z <= 26; z++) o.set(-4, beamY, z, "stripped_dark_oak_log[axis=z]");
        for (int x = -4; x <= 3; x++) for (int z = 2; z <= 26; z += 24) o.set(x, beamY, z, "stripped_dark_oak_log[axis=x]");
        for (int z = 2; z <= 26; z++) o.set(3, beamY, z, "stripped_dark_oak_log[axis=z]");
        int[][] sg = { // x (centre), z, y, colour index
                {0, 5, 0, 0}, {-1, 9, 0, 1}, {0, 13, 1, 2}, {1, 17, 1, 3}, {0, 21, 1, 4}};
        for (int i = 0; i < sg.length; i++) {
            int x = sg[i][0], z = sg[i][1], y = sg[i][2];
            String col = STRIPE[sg[i][3]];
            for (int dx = -1; dx <= 1; dx++) {
                o.set(x + dx, y, z, "stripped_spruce_wood[axis=x]");
                o.set(x + dx, y - 1, z, col + "_wool");
                o.set(x + dx, y - 2, z, dx == 0 ? "gold_block" : col + "_wool");
                o.set(x + dx, y - 3, z, "stripped_spruce_wood[axis=x]");
            }
            // chains from both ends to cross bars under the beam
            for (int dx = -1; dx <= 1; dx += 2) Sh.chain(o, x + dx, y + 1, beamY - 1, z);
            for (int xx = Math.min(-3, x - 1); xx <= Math.max(2, x + 1); xx++) o.set(xx, beamY, z, "stripped_dark_oak_log[axis=x]");
            o.area(x, y, z, x, z);
        }
        o.end(0, 1, 25);
        Sh.padBase(o, 0, 1, 25, "dark_oak_planks", "stripped_dark_oak_wood[axis=y]", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B2-3 (NEO)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Billboard Bend", "NEO");
        o.difficultyTag = "HARD";
        // a tall façade on the left (x <= -3) with flower-box ledges; billboards jut out between them
        houses(o, -7, -3, 3, 25, -10, 8, -3, 7, 4, true);
        int[][] ledge = {{4, 0}, {8, 0}, {12, 1}, {16, 1}, {20, 1}};
        for (int[] l : ledge) {
            for (int x = -2; x <= -1; x++) o.set(x, l[1], l[0], "spruce_planks");
            o.set(-1, l[1] + 1, l[0], "red_tulip");
            o.set(-2, l[1] + 1, l[0], "potted_red_tulip");
            o.set(-2, l[1] + 1, l[0], null);
            o.set(-2, l[1] - 1, l[0], Bs.stairs("spruce", "east", true));
            o.set(-1, l[1] - 1, l[0], Bs.stairs("spruce", "east", true));
            o.set(-1, l[1] + 1, l[0], null);
        }
        // the route: ledge 1 is reached by a plain jump; each next one is around a billboard
        String[] bb = {"red", "yellow", "lime", "light_blue"};
        o.area(-2, 0, 4, -1, 4);
        for (int i = 1; i < ledge.length; i++) {
            int bz = ledge[i][0] - 2;
            int y = ledge[i - 1][1];
            for (int x = -2; x <= 0; x++) for (int yy = y + 1; yy <= y + 4; yy++)
                o.set(x, yy, bz, (x == 0 || yy == y + 4) ? "stripped_birch_log[axis=y]" : bb[i - 1] + "_concrete");
            o.set(-1, y + 2, bz, bb[i - 1] + "_glazed_terracotta");
            o.set(0, y + 5, bz, "lantern[hanging=false,waterlogged=false]");
            Ob.Node n = o.area(-2, ledge[i][1], ledge[i][0], -1, ledge[i][0]);
            n.neo = true;
        }
        o.end(1, 1, 24);
        Sh.padBase(o, 1, 1, 24, "spruce_planks", "stripped_spruce_wood[axis=y]", "spruce_planks", 2);
        return o;
    }
}
