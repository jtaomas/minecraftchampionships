package com.mcm.games.parkourwarrior.map;

/** Zone 2 - Market Rooftops: shop windows, chimney stacks, roof railings, fruit crates, hanging shop signs, billboards. */
final class Market {
    private Market() {}

    static final String[] WALL = {"white_terracotta", "orange_terracotta", "yellow_terracotta", "light_gray_terracotta", "pink_terracotta"};
    static final String[] STRIPE = {"red", "lime", "blue", "orange", "magenta", "cyan"};

    /**
     * A block of houses: solid box x0..x1, z0..z1 from yBot to yTop, walls coloured per house (every hz blocks
     * along z), timber corners, windows on the face at x = face (or none), a gabled roof along z above yTop.
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

    // ---------------------------------------------------------------- M2-1 (THIN shop-window panes)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Shop Window Panes", "THIN");
        o.easy = true;
        // free-standing shop fronts: you land on the top edge of a glass pane (1/8 block deep) and jump on from it
        // to the next striped awning; awnings and panes alternate
        int[][] p = { // kind (0 awning 2x2, 1 pane row), x0, z, y
                {0, -1, 4, 0}, {1, -1, 8, 0}, {0, 0, 11, 0}, {1, -1, 15, 1}, {0, -1, 18, 1}, {1, -1, 22, 2}, {0, -1, 25, 2}};
        for (int i = 0; i < p.length; i++) {
            int k = p[i][0], x0 = p[i][1], z = p[i][2], y = p[i][3];
            String col = STRIPE[i % STRIPE.length];
            if (k == 0) {
                for (int x = x0; x <= x0 + 1; x++) for (int dz = 0; dz <= 1; dz++) {
                    o.set(x, y, z + dz, ((x + dz) & 1) == 0 ? col + "_wool" : "white_wool");
                    o.set(x, y - 1, z + dz, "spruce_planks");
                }
                for (int x = x0; x <= x0 + 1; x++) for (int yy = y - 2; yy >= y - 8; yy--) o.set(x, yy, z + 1, (yy & 3) == 0 ? "stripped_spruce_log[axis=y]" : WALL[i % WALL.length]);
                o.set(x0, y - 2, z, "lantern[hanging=true,waterlogged=false]");
                o.area(x0, y, z, x0 + 1, z + 1);
            } else {
                // shop front: two timber posts, a stone sill, the pane row on top of the sill's glass
                for (int x = x0; x <= x0 + 2; x++) {
                    o.set(x, y, z, "glass_pane");
                    o.set(x, y - 1, z, "glass_pane");
                    o.set(x, y - 2, z, "smooth_stone_slab[type=double,waterlogged=false]");
                    for (int yy = y - 3; yy >= y - 6; yy--) o.set(x, yy, z, WALL[(i + 2) % WALL.length]);
                }
                for (int sx : new int[]{x0 - 1, x0 + 3}) {
                    for (int yy = y - 6; yy <= y + 3; yy++) o.set(sx, yy, z, "stripped_dark_oak_log[axis=y]");
                    o.set(sx, y + 4, z, "lantern[hanging=false,waterlogged=false]");
                }
                o.area(x0, y, z, x0 + 2, z);
            }
        }
        o.end(0, 2, 30);
        Sh.padBase(o, 0, 2, 30, "brick_slab[type=double,waterlogged=false]", "bricks", "cobblestone", 3);
        return o;
    }

    // ---------------------------------------------------------------- M2-2 (POST chimneys)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Chimney Pots", "POST");
        o.easy = true;
        o.killDepth = 4;
        // a long roof far below, one-block chimney stacks sticking out of it at different heights
        for (int z = 2; z <= 27; z++) for (int k = 0; k <= 6; k++) {
            String m = (z / 4 & 1) == 0 ? "brick" : "granite";
            o.set(-6 + k, -14 + k, z, Bs.stairs(m, "east", false));
            o.set(6 - k, -14 + k, z, Bs.stairs(m, "west", false));
            for (int y = -20; y < -14 + k; y++) { o.set(-6 + k, y, z, k == 0 ? "orange_terracotta" : "stripped_spruce_wood[axis=y]"); o.set(6 - k, y, z, k == 0 ? "orange_terracotta" : "stripped_spruce_wood[axis=y]"); }
        }
        for (int z = 2; z <= 27; z++) o.set(0, -7, z, "bricks");
        int[][] ch = { // x, z, top y (all one block wide): 3b, 2+1, 3b, 3-1, 2+1, 3b, 2+1, 3b
                {0, 5, 0}, {1, 8, 1}, {0, 12, 1}, {-1, 16, 0}, {0, 19, 1}, {1, 23, 1}, {0, 26, 2}};
        for (int[] c : ch) {
            for (int y = -7; y <= c[2]; y++) o.set(c[0], y, c[1], y == c[2] ? "stone_bricks" : (y & 3) == 0 ? "mud_bricks" : "bricks");
            o.set(c[0], c[2] - 3, c[1] + 1, "brick_wall");
            o.set(c[0], c[2] - 3, c[1] - 1, "brick_wall");
            o.area(c[0], c[2], c[1], c[0], c[1]);
        }
        for (int z = 6; z <= 26; z += 8) o.set(-4, -6, z, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        o.end(0, 2, 30);
        Sh.padBase(o, 0, 2, 30, "polished_granite", "bricks", "granite", 3);
        return o;
    }

    // ---------------------------------------------------------------- M2-3 (WALL roof railings)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Railing Run", "WALL");
        o.easy = true;
        // stone railings (walls: 1.5 high, 3/8 block wide) running along the route on brick parapets; land on the
        // end of each railing, run along its narrow top and jump to the next one, offset sideways
        int[][] r = { // x, z0, z1, y (railing block y; standing height y + 1.5)
                {0, 4, 6}, {1, 9, 12}, {0, 16, 18}, {-1, 22, 24}, {0, 28, 29}};
        int[] ry = {-1, -1, -1, -2, -2};
        for (int i = 0; i < r.length; i++) {
            int x = r[i][0], z0 = r[i][1], z1 = r[i][2], y = ry[i];
            for (int z = z0; z <= z1; z++) {
                o.set(x, y, z, i % 2 == 0 ? "stone_brick_wall" : "andesite_wall");
                for (int yy = y - 1; yy >= y - 7; yy--) o.set(x, yy, z, (yy & 3) == 0 ? "stripped_dark_oak_log[axis=z]" : "bricks");
            }
            o.area(x, y, z0, x, z1);
        }
        o.end(0, -2, 33);
        Sh.padBase(o, 0, -2, 33, "polished_andesite", "bricks", "cobblestone", 3);
        return o;
    }

    // ---------------------------------------------------------------- B2-1 (GAP single crates)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Fruit Crate Tumble", "GAP");
        o.difficultyTag = "MEDIUM";
        // single crates now (one block of fruit on a crate stack): 3b, 3+1, 4-1, 3+1, 3+1, 3b
        int[][] cr = {{0, 5, 0, 0}, {1, 9, 1, 1}, {0, 14, 0, 2}, {0, 18, 1, 3}, {1, 22, 2, 0}, {0, 26, 2, 1}};
        String[] fruit = {"melon", "pumpkin", "hay_block[axis=y]", "moss_block"};
        for (int[] c : cr) {
            crate(o, c[0], c[2], c[1], fruit[c[3]]);
            o.area(c[0], c[2], c[1], c[0], c[1]);
        }
        o.end(0, 2, 30);
        Sh.padBase(o, 0, 2, 30, "spruce_planks", "stripped_spruce_wood[axis=y]", "spruce_planks", 2);
        return o;
    }

    /** One fruit block on a 1x1 crate stack going down 6, with a barrel and fence feet. */
    static void crate(Ob o, int x, int yTop, int z, String fruit) {
        o.set(x, yTop, z, fruit);
        for (int y = yTop - 1; y >= yTop - 6; y--) o.set(x, y, z, (y & 1) == 0 ? "barrel[facing=up,open=false]" : "stripped_spruce_wood[axis=y]");
        o.set(x, yTop - 7, z, "spruce_fence");
        o.set(x, yTop - 8, z, "spruce_fence");
    }

    // ---------------------------------------------------------------- B2-2 (HANG shop signs)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Swinging Shop Signs", "HANG");
        o.difficultyTag = "MEDIUM";
        int beamY = 7;
        for (int z : new int[]{2, 28}) for (int y = -7; y <= beamY; y++) o.set(-4, y, z, "stripped_dark_oak_log[axis=y]");
        for (int z = 2; z <= 28; z++) o.set(-4, beamY, z, "stripped_dark_oak_log[axis=z]");
        // signboards hang edge-on (three wide, one thick) from chains: 1-deep landings with no run-up,
        // jumps 3b, 3+1, 3+1, 4-1, 3+1, 3b
        int[][] sg = {{0, 5, 0, 0}, {1, 9, 1, 1}, {0, 13, 2, 2}, {1, 18, 1, 3}, {0, 22, 2, 4}};
        for (int i = 0; i < sg.length; i++) {
            int x = sg[i][0], z = sg[i][1], y = sg[i][2];
            String col = STRIPE[sg[i][3]];
            for (int dx = -1; dx <= 1; dx++) {
                o.set(x + dx, y, z, "stripped_spruce_wood[axis=x]");
                o.set(x + dx, y - 1, z, col + "_wool");
                o.set(x + dx, y - 2, z, dx == 0 ? "gold_block" : col + "_wool");
                o.set(x + dx, y - 3, z, "stripped_spruce_wood[axis=x]");
            }
            for (int dx = -1; dx <= 1; dx += 2) Sh.chain(o, x + dx, y + 1, beamY - 1, z);
            for (int xx = -3; xx <= Math.max(2, x + 1); xx++) o.set(xx, beamY, z, "stripped_dark_oak_log[axis=x]");
            o.area(x - 1, y, z, x + 1, z);
        }
        o.end(0, 2, 26);
        Sh.padBase(o, 0, 2, 26, "dark_oak_planks", "stripped_dark_oak_wood[axis=y]", "dark_oak_planks", 2);
        return o;
    }

    // ---------------------------------------------------------------- B2-3 (NEO billboards)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Billboard Bend", "NEO");
        o.difficultyTag = "HARD";
        // a tall facade on the left; one-wide flower-box ledges (3 long) stick out of it and billboards stand
        // between them, flush with the ledges' outer edge: two single neos, two double neos (two-thick boards),
        // then a running 4-1 and a 2+1 up to the pad
        houses(o, -7, -3, 1, 33, -12, 9, -3, 7, 4, true);
        String[] bb = {"red", "yellow", "lime", "light_blue", "orange", "magenta"};
        // ledge: z0 (first z), y; wall thickness after it (0 = no billboard, plain gap 4)
        int[][] L = {{4, 0, 1}, {8, 0, 1}, {12, 0, 2}, {17, 0, 2}, {22, -1, 0}, {29, -2, 0}};
        for (int i = 0; i < L.length; i++) {
            int z0 = L[i][0], y = L[i][1];
            for (int z = z0; z < z0 + 3; z++) { o.set(-2, y, z, "spruce_planks"); o.set(-2, y - 1, z, Bs.stairs("spruce", "east", true)); }
            Ob.Node n = o.area(-2, y, z0, -2, z0 + 2);
            if (i > 0 && L[i - 1][2] > 0) n.neo = true;
            int t = L[i][2];
            if (t > 0) {
                int bz = z0 + 3;
                for (int dz = 0; dz < t; dz++) for (int yy = y - 1; yy <= y + 4; yy++)
                    o.set(-2, yy, bz + dz, yy == y + 4 ? "stripped_birch_log[axis=y]" : bb[i % bb.length] + "_concrete");
                o.set(-2, y + 2, bz, bb[i % bb.length] + "_glazed_terracotta");
                o.set(-2, y + 5, bz, "lantern[hanging=false,waterlogged=false]");
            }
        }
        o.end(-1, -1, 35);
        Sh.padBase(o, -1, -1, 35, "spruce_planks", "stripped_spruce_wood[axis=y]", "spruce_planks", 2);
        return o;
    }
}
