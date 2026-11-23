package com.mcm.games.parkourwarrior.map;

/** Zone 1 - Toy Box: studded building bricks, dominoes, a tin train, alphabet blocks, a striped pole, a jack-in-the-box. */
final class ToyBox {
    private ToyBox() {}

    static String c(String col) { return col + "_concrete"; }

    /** A toy brick stack: bricks 3 tall (2 concrete + a terracotta seam) with studs on the top brick. */
    static void brickTower(Ob o, int x0, int yTop, int z0, int x1, int z1, int bricks, int salt) {
        for (int k = 0; k < bricks; k++) {
            String col = Sh.TOY[(salt + k * 3) % Sh.TOY.length];
            int yt = yTop - 3 * k;
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                o.set(x, yt, z, c(col));
                o.set(x, yt - 1, z, c(col));
                o.set(x, yt - 2, z, col + "_terracotta");
            }
        }
        String top = Sh.TOY[salt % Sh.TOY.length];
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) if (((x + z) & 1) == 0) o.set(x, yTop + 1, z, Sh.stud(top));
        int yb = yTop - 3 * bricks;
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        o.set(mx, yb, mz, "white_concrete");
        o.set(mx, yb - 1, mz, "end_rod[facing=down]");
    }

    // ---------------------------------------------------------------- M1-1 (UP)
    static Ob m1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Brick by Brick", "UP");
        o.easy = true;
        int[][] b = { // x0, top, z0, x1, z1, bricks
                {-1, 0, 3, 1, 5, 2}, {0, 1, 7, 2, 9, 2}, {-2, 2, 11, 0, 13, 3}, {-1, 3, 16, 1, 18, 3},
                {0, 3, 22, 2, 24, 3}, {-1, 4, 27, 1, 29, 3}};
        for (int i = 0; i < b.length; i++) {
            int[] r = b[i];
            brickTower(o, r[0], r[1], r[2], r[3], r[4], r[5], i * 5 + 1);
            o.area(r[0], r[1], r[2], r[3], r[4]);
        }
        // a dropped toy ball and a loose brick off to the side (scenery, well away from the route)
        ball(o, -9, 0, 12, 3);
        brickTower(o, 8, -3, 20, 10, 24, 2, 6);
        o.end(0, 4, 32);
        Sh.padBase(o, 0, 4, 32, "red_concrete", "yellow_concrete", "yellow_terracotta", 3);
        return o;
    }

    static void ball(Ob o, int cx, int cy, int cz, int r) {
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            if (x * x + y * y + z * z > r * r + r) continue;
            String col = ((x + r) / 2 + (y + 10)) % 3 == 0 ? "white" : (y > 0 ? "red" : "blue");
            if (Math.abs(y) <= 0 && Math.abs(x) <= 0) col = "yellow";
            o.set(cx + x, cy + y, cz + z, col + "_wool");
        }
    }

    // ---------------------------------------------------------------- M1-2 (GAP)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Domino Line-Up", "GAP");
        o.easy = true;
        int[] zs = {4, 8, 11, 15, 19, 22};
        int[] xs = {0, 1, 0, -1, 0, 1};
        int[] pips = {3, 5, 2, 6, 4, 1};
        for (int i = 0; i < zs.length; i++) {
            domino(o, xs[i], 0, zs[i], pips[i], (pips[i] + 2) % 6 + 1, false);
            o.area(xs[i] - 1, 0, zs[i], xs[i] + 1, zs[i]);
        }
        // toppled dominoes lying off to the side, well clear of the route
        // a small domino spiral standing on a toy plank far to the side (scenery)
        for (int i = 0; i < 9; i++) {
            double a = i * 0.7;
            int x = -10 + (int) Math.round(Math.cos(a) * (1 + i * 0.45)), z = 13 + (int) Math.round(Math.sin(a) * (1 + i * 0.45));
            for (int y = -6; y <= -4; y++) o.set(x, y, z, y == -5 ? "white_concrete" : "black_concrete");
        }
        for (int x = -15; x <= -5; x++) for (int z = 8; z <= 18; z++) if ((x + 10) * (x + 10) + (z - 13) * (z - 13) <= 26) o.set(x, -7, z, "oak_planks");
        o.end(0, 0, 26);
        Sh.padBase(o, 0, 0, 26, "white_concrete", "black_concrete", "gray_concrete", 2);
        return o;
    }

    /** Domino standing upright: 3 wide (x), 1 thick (z), 8 tall below and including yTop; pips on both faces. */
    static void domino(Ob o, int cx, int yTop, int z, int a, int b, boolean x) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 7; dy++) {
            String s = "black_concrete";
            int row = dy; // 0 = top
            if (row == 4) s = "gray_concrete";
            else if (row >= 1 && row <= 3 && pip(a, dx + 1, row - 1)) s = "white_concrete";
            else if (row >= 5 && row <= 7 && pip(b, dx + 1, row - 5)) s = "white_concrete";
            o.set(cx + dx, yTop - dy, z, s);
        }
        o.set(cx, yTop - 8, z, "polished_blackstone_wall");
    }

    static boolean pip(int n, int c, int r) {
        boolean centre = c == 1 && r == 1;
        boolean diagA = (c == 0 && r == 0) || (c == 2 && r == 2);
        boolean diagB = (c == 2 && r == 0) || (c == 0 && r == 2);
        boolean mid = (c == 0 || c == 2) && r == 1;
        switch (n) {
            case 1: return centre;
            case 2: return diagA;
            case 3: return diagA || centre;
            case 4: return diagA || diagB;
            case 5: return diagA || diagB || centre;
            default: return diagA || diagB || mid;
        }
    }

    /** A fallen domino lying flat (3 x 8 slab, 1 thick) - scenery. */
    static void leaning(Ob o, int x, int y, int z) {
        for (int dz = 0; dz < 3; dz++) for (int k = 0; k < 7; k++) {
            o.set(x, y - k / 2, z + dz + k / 3 * 0, k == 3 ? "gray_concrete" : "black_concrete");
        }
    }

    // ---------------------------------------------------------------- M1-3 (BEAM)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Tin Train Trestle", "BEAM");
        o.easy = true;
        // track bed cells (x, z) at y = 0, with gaps where pieces are missing
        int[][] seg1 = {{0, 2}, {0, 6}}, seg2 = {{1, 7}, {1, 10}}, seg3 = {{1, 13}, {1, 15}}, seg4 = {{0, 23}, {0, 25}};
        track(o, seg1, "north_south");
        track(o, seg2, "north_south");
        track(o, seg3, "north_south");
        track(o, new int[][]{{1, 16}, {1, 21}}, null);   // under the train (no rails)
        track(o, seg4, "north_south");
        o.area(0, 0, 2, 0, 6);
        o.area(1, 0, 7, 1, 10);
        o.area(1, 0, 13, 1, 15);
        // toy train: flatcar (lime) then cab (red) on the track
        for (int z = 16; z <= 18; z++) { o.set(1, 1, z, "lime_concrete"); wheels(o, 1, z); }
        o.set(1, 2, 16, "yellow_concrete");                      // little cargo crate at the back of the flatcar? keep clear
        o.set(1, 2, 16, null);
        for (int z = 19; z <= 20; z++) { o.set(1, 1, z, "red_concrete"); o.set(1, 2, z, "red_concrete"); wheels(o, 1, z); }
        o.set(1, 1, 21, "black_concrete");
        o.set(1, 1, 21, "red_terracotta");
        o.area(1, 1, 16, 1, 18);
        o.area(1, 2, 19, 1, 20);
        o.area(1, 1, 21, 1, 21);
        o.area(0, 0, 23, 0, 25);
        // smoke puffs high above the cab
        puff(o, 1, 7, 20); puff(o, 0, 9, 23); puff(o, -1, 11, 27);
        o.end(0, 0, 28);
        Sh.padBase(o, 0, 0, 28, "blue_concrete", "white_concrete", "light_gray_concrete", 2);
        return o;
    }

    static void wheels(Ob o, int x, int z) {
        o.set(x - 1, 1, z, "polished_blackstone_button[face=wall,facing=west,powered=false]");
        o.set(x + 1, 1, z, "polished_blackstone_button[face=wall,facing=east,powered=false]");
    }

    static void puff(Ob o, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++)
            if (Math.abs(dx) + Math.abs(dz) + dy <= 2) o.set(x + dx, y + dy, z + dz, "white_wool");
    }

    /** Toy track: planks bed on fence stilts, rails on top (shape given, or none). */
    static void track(Ob o, int[][] se, String shape) {
        int x0 = se[0][0], z0 = se[0][1], x1 = se[1][0], z1 = se[1][1];
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
            o.set(x, 0, z, (z & 1) == 0 ? "stripped_dark_oak_log[axis=x]" : "dark_oak_planks");
            if (shape != null) o.set(x, 1, z, "rail[shape=" + shape + ",waterlogged=false]");
            if (z % 3 == 0) {
                for (int y = -1; y >= -5; y--) o.set(x, y, z, "dark_oak_fence");
                o.set(x, -6, z, "stripped_dark_oak_log[axis=y]");
                // sleeper cross-beam under the bed
                o.set(x - 1, -1, z, "dark_oak_slab[type=top,waterlogged=false]");
                o.set(x + 1, -1, z, "dark_oak_slab[type=top,waterlogged=false]");
            }
        }
    }

    // ---------------------------------------------------------------- B1-1 (DROP)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Alphabet Avalanche", "DROP");
        o.difficultyTag = "EASY";
        letterCube(o, -2, -1, 5, 'A', "red", "yellow");
        o.area(-2, -1, 5, 2, 9);
        letterCube(o, -1, -3, 14, 'B', "blue", "white");
        o.area(-1, -3, 14, 3, 18);
        letterCube(o, -3, -4, 22, 'C', "lime", "magenta");
        o.area(-3, -4, 22, 1, 26);
        o.end(0, -3, 30);
        Sh.padBase(o, 0, -3, 30, "yellow_concrete", "orange_concrete", "orange_terracotta", 2);
        return o;
    }

    /** 5x5x5 alphabet block with top at yTop, min corner (x0, z0); letter drawn on the two side faces (x). */
    static void letterCube(Ob o, int x0, int yTop, int z0, char letter, String frame, String face) {
        String[] L = letter == 'A' ? new String[]{".#.", "#.#", "###", "#.#"} : letter == 'B' ? new String[]{"##.", "###", "#.#", "##."}
                : new String[]{".##", "#..", "#..", ".##"};
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
            boolean edge = (x == 0 || x == 4 ? 1 : 0) + (y == 0 || y == 4 ? 1 : 0) + (z == 0 || z == 4 ? 1 : 0) >= 2;
            String s = edge ? frame + "_concrete" : face + "_concrete";
            boolean sideFace = (z == 0 || z == 4 || x == 0 || x == 4) && !edge;
            if (sideFace && y >= 1 && y <= 4) {
                int col = (x == 0 || x == 4) ? z - 1 : x - 1;
                int row = 4 - y; // y=4 top row... letter rows 0..3 map to y 4..1, clip to 3 cols
                if (row >= 0 && row < 4 && col >= 0 && col < 3 && L[row].charAt(col) == '#') s = frame + "_wool";
            }
            if (y == 4 && !edge) s = face + "_concrete";
            o.set(x0 + x, yTop - 4 + y, z0 + z, s);
        }
        o.set(x0 + 2, yTop - 5, z0 + 2, frame + "_terracotta");
    }

    // ---------------------------------------------------------------- B1-2 (SPIRAL)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Peg Pole Spiral", "SPIRAL");
        o.difficultyTag = "EASY";
        int cz = 9;
        // thick striped pole (radius 2.5), y -8 .. 11, with a ball finial
        for (int y = -8; y <= 14; y++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (!Sh.inDisc(dx, dz, 2.5)) continue;
            int ang = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) * 4 / Math.PI);
            String col = Math.floorMod(ang + y, 4) < 2 ? "red_concrete" : "white_concrete";
            o.set(dx, y, cz + dz, col);
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 14; dy <= 17; dy++)
            if (dx * dx + dz * dz + (dy - 15.5) * (dy - 15.5) <= 5) o.set(dx, dy, cz + dz, "yellow_concrete");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.set(dx, -9, cz + dz, "red_terracotta");
        o.set(0, -10, cz, "end_rod[facing=down]");
        // pegs spiral 1.5 turns around the pole: from the near side (180 deg) to the far side, rising
        String[] cols = {"blue", "lime", "orange", "magenta", "yellow", "light_blue"};
        for (int k = 0; k <= 12; k++) {
            double a = Math.toRadians(180 + 45 * k);
            int y = k * 3 / 4;
            double ux = Math.sin(a), uz = Math.cos(a);
            int px = (int) Math.round(ux * 5), pz = cz + (int) Math.round(uz * 5);
            int qx = (int) Math.round(ux * 3.6), qz = cz + (int) Math.round(uz * 3.6);
            int bx = (int) Math.round(ux * 2.7), bz = cz + (int) Math.round(uz * 2.7);
            String col = cols[k % cols.length];
            java.util.List<int[]> pc = new java.util.ArrayList<>();
            for (int[] c : new int[][]{{px, pz}, {qx, qz}, {px, qz}, {qx, pz}}) {
                boolean dup = false;
                for (int[] e : pc) if (e[0] == c[0] && e[2] == c[1]) dup = true;
                if (!dup) { pc.add(new int[]{c[0], y, c[1]}); o.set(c[0], y, c[1], col + "_concrete"); }
            }
            o.set(qx, y - 1, qz, col + "_terracotta");
            o.set(qx, y - 2, qz, col + "_terracotta");
            o.set(bx, y - 2, qz, col + "_terracotta");
            o.set(qx, y - 2, bz, col + "_terracotta");
            o.set(bx, y - 2, bz, col + "_terracotta");
            o.cells(Ob.T.AREA, pc.toArray(new int[0][]));
        }
        o.end(0, 9, 18);
        Sh.padBase(o, 0, 9, 18, "red_concrete", "white_concrete", "red_terracotta", 2);
        return o;
    }


    // ---------------------------------------------------------------- B1-3 (SLIME)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Pop Goes the Jester", "SLIME");
        o.difficultyTag = "MEDIUM";
        jackBox(o, 0, -4, 5, "blue", "yellow");
        o.areaT(Ob.T.SLIME, -1, -4, 4, 1, 6);
        // jester head on a spring rising out of a second box
        jackBox(o, 0, -8, 10, "red", "white");
        for (int y = -7; y <= -4; y++) o.set(0, y, 10, (y & 1) == 0 ? "iron_bars" : "chain[axis=y,waterlogged=false]");
        for (int dx = -1; dx <= 1; dx++) for (int dz = 9; dz <= 11; dz++) for (int y = -3; y <= -1; y++)
            o.set(dx, y, dz, y == -3 ? "red_wool" : "white_concrete");
        o.set(-1, -2, 9, "black_concrete"); o.set(1, -2, 9, "black_concrete");      // eyes facing the player
        o.set(0, -3, 9, "red_concrete");                                            // smile
        o.set(-1, 0, 11, "yellow_wool"); o.set(1, 0, 11, "yellow_wool");          // two-point jester hat
        o.set(-1, 1, 11, "gold_block"); o.set(1, 1, 11, "gold_block");            // bells
        o.cells(Ob.T.AREA, new int[]{-1, -1, 9}, new int[]{0, -1, 9}, new int[]{1, -1, 9}, new int[]{-1, -1, 10},
                new int[]{0, -1, 10}, new int[]{1, -1, 10}, new int[]{0, -1, 11});
        jackBox(o, 0, -5, 16, "lime", "magenta");
        o.areaT(Ob.T.SLIME, -1, -5, 15, 1, 17);
        o.end(0, -2, 21);
        Sh.padBase(o, 0, -2, 21, "magenta_concrete", "lime_concrete", "lime_terracotta", 2);
        return o;
    }

    /** Open toy box 3x3 whose top layer is a slime spring at yTop, with patterned walls below. */
    static void jackBox(Ob o, int cx, int yTop, int cz, String a, String b) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            o.set(cx + dx, yTop, cz + dz, "slime_block");
            for (int y = yTop - 1; y >= yTop - 3; y--)
                o.set(cx + dx, y, cz + dz, ((dx + dz + y) & 1) == 0 ? a + "_concrete" : b + "_concrete");
        }
        // corner posts and a crank handle on the side
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) o.set(cx + dx, yTop - 4, cz + dz, a + "_terracotta");
        o.set(cx + 2, yTop - 2, cz, "lightning_rod[facing=east,powered=false,waterlogged=false]");
    }
}
