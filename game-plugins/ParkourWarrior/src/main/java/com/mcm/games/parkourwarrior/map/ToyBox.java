package com.mcm.games.parkourwarrior.map;

/**
 * Zone 1 - Toy Box: studded building bricks, dominoes, toy rockets with ladders, alphabet blocks, a striped peg
 * pole and jack-in-the-box springs. Main line = real sprint jumps (3b, 2+1, 3-1), no one-block hops.
 */
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
        // 2x2 brick towers: 3b, 2+1, 2+1, 3b, 2+1, 3-1, then a 3b onto the pad
        int[][] b = { // x0, top, z0, bricks
                {-1, 0, 5, 2}, {0, 1, 9, 2}, {-1, 2, 13, 3}, {0, 2, 18, 3}, {-1, 3, 22, 3}, {0, 2, 27, 3}};
        for (int i = 0; i < b.length; i++) {
            int[] r = b[i];
            brickTower(o, r[0], r[1], r[2], r[0] + 1, r[2] + 1, r[3], i * 5 + 1);
            o.area(r[0], r[1], r[2], r[0] + 1, r[2] + 1);
        }
        ball(o, -9, -1, 13, 3);
        brickTower(o, 8, -4, 20, 10, 24, 2, 6);
        o.end(0, 2, 33);
        Sh.padBase(o, 0, 2, 33, "red_concrete", "yellow_concrete", "yellow_terracotta", 3);
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

    // ---------------------------------------------------------------- M1-2 (THIN dominoes)
    static Ob m2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Domino Line-Up", "THIN");
        o.easy = true;
        // dominoes are one block thick: every landing is a 1-deep strip and every takeoff has no run-up
        int[][] d = { // x, z, top y
                {0, 5, 0}, {1, 9, 0}, {0, 12, 1}, {-1, 16, 1}, {0, 20, 0}, {1, 23, 1}};
        int[] pips = {3, 5, 2, 6, 4, 1};
        for (int i = 0; i < d.length; i++) {
            domino(o, d[i][0], d[i][2], d[i][1], pips[i], (pips[i] + 2) % 6 + 1);
            o.area(d[i][0] - 1, d[i][2], d[i][1], d[i][0] + 1, d[i][1]);
        }
        // a small domino spiral standing on a toy plank far below to the side (scenery)
        for (int i = 0; i < 9; i++) {
            double a = i * 0.7;
            int x = -10 + (int) Math.round(Math.cos(a) * (1 + i * 0.45)), z = 13 + (int) Math.round(Math.sin(a) * (1 + i * 0.45));
            for (int y = -9; y <= -7; y++) o.set(x, y, z, y == -8 ? "white_concrete" : "black_concrete");
        }
        for (int x = -15; x <= -5; x++) for (int z = 8; z <= 18; z++) if ((x + 10) * (x + 10) + (z - 13) * (z - 13) <= 26) o.set(x, -10, z, "oak_planks");
        o.end(0, 1, 28);
        Sh.padBase(o, 0, 1, 28, "white_concrete", "black_concrete", "gray_concrete", 2);
        return o;
    }

    /** Domino standing upright: 3 wide (x), 1 thick (z), 8 tall below and including yTop; pips on both faces. */
    static void domino(Ob o, int cx, int yTop, int z, int a, int b) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 7; dy++) {
            String s = "black_concrete";
            if (dy == 4) s = "gray_concrete";
            else if (dy >= 1 && dy <= 3 && pip(a, dx + 1, dy - 1)) s = "white_concrete";
            else if (dy >= 5 && dy <= 7 && pip(b, dx + 1, dy - 5)) s = "white_concrete";
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

    // ---------------------------------------------------------------- M1-3 (LADDER toy rockets)
    static Ob m3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Toy Rocket Launch", "LADDER");
        o.easy = true;
        // launch block, then a running catch onto the first rocket's ladder, climb, jump sideways to its fin,
        // a 3b to the second launch block and a catch onto the second rocket, exit onto its nose platform
        brickTower(o, -1, 0, 4, 0, 5, 2, 2);
        o.area(-1, 0, 4, 0, 5);
        rocket(o, 0, 10, 7, "red", "white");
        o.ladder(0, 1, 5, 8, "north");
        // tail fin platform at the top rung level, two blocks to the side of the ladder
        for (int x = 3; x <= 4; x++) for (int z = 7; z <= 8; z++) { o.set(x, 5, z, "white_concrete"); o.set(x, 4, z, "red_concrete"); o.set(x, 3, z, "red_terracotta"); }
        for (int y = -2; y <= 2; y++) o.set(3, y, 8, "red_concrete");
        o.area(3, 5, 7, 4, 8);
        brickTower(o, 3, 5, 12, 4, 13, 4, 4);
        o.area(3, 5, 12, 4, 13);
        rocket(o, 4, 18, 13, "blue", "yellow");
        o.ladder(4, 6, 8, 16, "north");
        // nose-cone deck to the side of the second rocket
        for (int x = 0; x <= 1; x++) for (int z = 15; z <= 17; z++) { o.set(x, 8, z, "yellow_concrete"); o.set(x, 7, z, "blue_terracotta"); }
        for (int y = 1; y <= 6; y++) o.set(0, y, 16, "blue_concrete");
        o.area(0, 8, 15, 1, 17);
        o.end(0, 8, 22);
        Sh.padBase(o, 0, 8, 22, "blue_concrete", "white_concrete", "light_gray_concrete", 3);
        return o;
    }

    /** Toy rocket: 3x3 body (cx, cz is the body centre) from y -6 to topY with stripes, a cone and a window. */
    static void rocket(Ob o, int cx, int cz, int topY, String a, String b) {
        for (int y = -6; y <= topY; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            o.set(cx + dx, y, cz + dz, Math.floorMod(y, 4) == 0 ? c(b) : c(a));
        o.set(cx, topY + 1, cz, c(b));
        o.set(cx, topY + 2, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        o.set(cx, topY - 2, cz - 1, "light_blue_stained_glass");
        for (int[] d : new int[][]{{-2, 0}, {2, 0}}) for (int y = -7; y <= -4; y++) o.set(cx + d[0], y, cz + d[1], c(b));
        o.set(cx, -7, cz, "orange_concrete");
        o.set(cx, -8, cz, "orange_stained_glass");
    }

    // ---------------------------------------------------------------- B1-1 (DROP alphabet blocks)
    static Ob b1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Alphabet Avalanche", "DROP");
        o.difficultyTag = "EASY";
        // long running drops: 4-1, 4-2, 4-1 onto 3-wide letter blocks, then a 3+1 up onto the pad
        letterCube(o, -1, -1, 6, 'A', "red", "yellow", 3);
        o.area(-1, -1, 6, 1, 8);
        letterCube(o, 0, -3, 13, 'B', "blue", "white", 3);
        o.area(0, -3, 13, 2, 15);
        letterCube(o, -1, -4, 20, 'C', "lime", "magenta", 3);
        o.area(-1, -4, 20, 1, 22);
        o.end(0, -3, 27);
        Sh.padBase(o, 0, -3, 27, "yellow_concrete", "orange_concrete", "orange_terracotta", 2);
        return o;
    }

    /** n x n x n alphabet block with top at yTop, min corner (x0, z0); the letter is drawn on the side faces. */
    static void letterCube(Ob o, int x0, int yTop, int z0, char letter, String frame, String face, int n) {
        String[] L = letter == 'A' ? new String[]{".#.", "###", "#.#"} : letter == 'B' ? new String[]{"##.", "###", "##."}
                : new String[]{".##", "#..", ".##"};
        int m = n - 1;
        for (int x = 0; x < n; x++) for (int y = 0; y < n + 2; y++) for (int z = 0; z < n; z++) {
            boolean edge = (x == 0 || x == m ? 1 : 0) + (z == 0 || z == m ? 1 : 0) >= 2 || y == n + 1;
            String s = edge ? frame + "_concrete" : face + "_concrete";
            int row = n + 1 - y;
            if (!edge && row >= 0 && row < 3) {
                int col = (x == 0 || x == m) ? z : x;
                if (col < 3 && L[row].charAt(col) == '#') s = frame + "_wool";
            }
            o.set(x0 + x, yTop - n - 1 + y, z0 + z, s);
        }
        o.set(x0 + 1, yTop - n - 2, z0 + 1, frame + "_terracotta");
    }

    // ---------------------------------------------------------------- B1-2 (SPIRAL pegs)
    static Ob b2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Peg Pole Spiral", "SPIRAL");
        o.difficultyTag = "MEDIUM";
        int cz = 17, top = 0;
        // two toy blocks lead to a thick striped pole; one-block pegs wind up its left side from the front to the
        // back (alternating 3+1 and flat 3b between axis and diagonal pegs); the pad is behind the pole
        brickTower(o, -1, 0, 5, 0, 6, 3, 3);
        o.area(-1, 0, 5, 0, 6);
        brickTower(o, 0, 0, 9, 1, 9, 3, 5);
        o.area(0, 0, 9, 1, 9);
        for (int y = -9; y <= 9; y++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (!Sh.inDisc(dx, dz, 1.6)) continue;
            int ang = (int) Math.floor((Math.atan2(dz, dx) + Math.PI) * 4 / Math.PI);
            o.set(dx, y, cz + dz, Math.floorMod(ang + y, 4) < 2 ? "red_concrete" : "white_concrete");
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 10; dy <= 13; dy++)
            if (dx * dx + dz * dz + (dy - 11.5) * (dy - 11.5) <= 5) o.set(dx, dy, cz + dz, "yellow_concrete");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.set(dx, -10, cz + dz, "red_terracotta");
        o.set(0, -11, cz, "end_rod[facing=down]");
        String[] cols = {"blue", "lime", "orange", "magenta", "light_blue"};
        int[][] pegs = {{0, -5, 1}, {-4, -4, 2}, {-5, 0, 2}, {-4, 4, 3}, {0, 5, 3}};
        for (int i = 0; i < pegs.length; i++) {
            int x = pegs[i][0], z = cz + pegs[i][1], y = pegs[i][2];
            String col = cols[i % cols.length];
            o.set(x, y, z, col + "_concrete");
            for (int k = 1; k <= 3; k++) o.set(x, y - k, z, col + "_terracotta");
            // the peg's arm back into the pole, four blocks under the foothold (along x, then along z)
            int px = x, pz = z;
            for (int k = 0; k < 10 && !Sh.inDisc(px, pz - cz, 1.6); k++) {
                o.set(px, y - 4, pz, col + "_terracotta");
                if (Math.abs(px) > 1) px -= Integer.signum(px); else pz += Integer.signum(cz - pz);
            }
            o.area(x, y, z, x, z);
        }
        int pz0 = cz + 8;
        o.end(0, 4, pz0 + 1);
        Sh.padBase(o, 0, 4, pz0 + 1, "red_concrete", "white_concrete", "red_terracotta", 3);
        return o;
    }

    // ---------------------------------------------------------------- B1-3 (SLIME jack-in-the-boxes)
    static Ob b3(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Pop Goes the Jester", "SLIME");
        o.difficultyTag = "HARD";
        // a 4-1 onto a two-bell hat, drop onto a spring box and bounce onto the next hat, a 4b, a 3+1 and a
        // second 4b along three more hats (keep your momentum), then the second spring bounces you two up and three across to the pad
        hat(o, 0, -1, 6, "yellow");
        hat(o, 0, -1, 7, "yellow");
        o.area(0, -1, 6, 0, 7);
        jackBox(o, 0, -7, 10, "blue", "yellow");
        o.areaT(Ob.T.SLIME, -1, -7, 9, 1, 11);
        hat(o, 1, -4, 14, "red");
        o.area(1, -4, 14, 1, 14);
        hat(o, 0, -4, 19, "lime");
        o.area(0, -4, 19, 0, 19);
        hat(o, 1, -3, 23, "orange");
        o.area(1, -3, 23, 1, 23);
        hat(o, 1, -3, 28, "magenta");
        o.area(1, -3, 28, 1, 28);
        jackBox(o, 1, -9, 31, "lime", "magenta");
        o.areaT(Ob.T.SLIME, 0, -9, 30, 2, 32);
        o.end(1, -7, 37);
        Sh.padBase(o, 1, -7, 37, "magenta_concrete", "lime_concrete", "lime_terracotta", 2);
        return o;
    }

    /** A jester on a spring: a one-block gold hat bell at yTop, a thin striped body and a spring below. */
    static void hat(Ob o, int x, int yTop, int z, String col) {
        o.set(x, yTop, z, "gold_block");
        o.set(x, yTop - 1, z, col + "_wool");
        o.set(x, yTop - 2, z, "white_concrete");
        o.set(x, yTop - 3, z, col + "_concrete");
        for (int y = yTop - 8; y <= yTop - 4; y++) o.set(x, y, z, (y & 1) == 0 ? "iron_bars" : "chain[axis=y,waterlogged=false]");
        o.set(x, yTop - 9, z, col + "_terracotta");
    }

    /** Open toy box 3x3 whose top layer is a slime spring at yTop, with patterned walls below. */
    static void jackBox(Ob o, int cx, int yTop, int cz, String a, String b) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            o.set(cx + dx, yTop, cz + dz, "slime_block");
            for (int y = yTop - 1; y >= yTop - 3; y--)
                o.set(cx + dx, y, cz + dz, ((dx + dz + y) & 1) == 0 ? a + "_concrete" : b + "_concrete");
        }
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) o.set(cx + dx, yTop - 4, cz + dz, a + "_terracotta");
        o.set(cx + 2, yTop - 2, cz, "lightning_rod[facing=east,powered=false,waterlogged=false]");
    }
}
