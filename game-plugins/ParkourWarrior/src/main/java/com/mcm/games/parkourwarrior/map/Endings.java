package com.mcm.games.parkourwarrior.map;

/**
 * The three endings: Easy "Meadow & Beehive" (honeycomb columns, giant flowers), Medium "Frozen Harbour" (ice
 * floes, a lighthouse ladder) and Hard "Library Tower" (book spines up a shelf wall with a rolling ladder, then a
 * spiral stair with flickering treads). Each is two obstacles ending at a finish shrine.
 */
final class Endings {
    private Endings() {}

    // ================================================================ EASY: Meadow & Beehive
    static Ob easy1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Honeycomb Hop", "UP");
        o.easy = true;
        int[][] c = {{-1, 3, 0}, {0, 7, 1}, {-1, 11, 2}, {0, 15, 2}};
        for (int i = 0; i < c.length; i++) {
            int x0 = c[i][0], z0 = c[i][1], y = c[i][2];
            int depth = 7 + i * 2;
            // a straight 3x3 honeycomb column (no lower ring: the old ring was a side path past the tops)
            for (int x = x0; x <= x0 + 2; x++) for (int z = z0; z <= z0 + 2; z++) for (int yy = y; yy >= y - depth; yy--) {
                boolean corner = x != x0 + 1 && z != z0 + 1;
                String st = yy == y ? "honeycomb_block" : (corner && (yy + i) % 3 == 0 ? "bee_nest[facing=north,honey_level=5]" : "honeycomb_block");
                o.set(x, yy, z, st);
            }
            o.area(x0, y, z0, x0 + 2, z0 + 2);
        }
        // a bee (yellow/black) hovering off to the side
        for (int x = 7; x <= 8; x++) for (int z = 8; z <= 10; z++) o.set(x, 4, z, z == 9 ? "black_wool" : "yellow_wool");
        o.set(7, 5, 9, "white_stained_glass"); o.set(8, 5, 9, "white_stained_glass");
        o.end(0, 2, 20);
        meadowBase(o, 0, 2, 20);
        return o;
    }

    static void meadowBase(Ob o, int x, int y, int z) {
        Sh.padBase(o, x, y, z, "moss_block", "grass_block[snowy=false]", "dirt", 3);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (Math.abs(dx) == 2 && Math.abs(dz) == 2) o.set(x + dx, y - 1, z + dz, "moss_block");
    }

    static Ob easy2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Bloom Stepping", "GAP");
        o.easy = true;
        String[] petal = {"pink_concrete", "light_blue_concrete", "orange_concrete"};
        int[][] f = {{0, 4, 0}, {1, 9, 0}, {0, 14, 0}};
        for (int i = 0; i < f.length; i++) {
            int x = f[i][0], z = f[i][1], y = f[i][2];
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.set(x + dx, y, z + dz, dx == 0 && dz == 0 ? "yellow_concrete" : petal[i]);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) o.set(x + dx, y - 1, z + dz, petal[i].replace("concrete", "terracotta"));
            for (int yy = y - 1; yy >= y - 10; yy--) o.set(x, yy, z, "green_concrete");
            o.set(x + 1, y - 5, z, "azalea_leaves[distance=7,persistent=true,waterlogged=false]");
            o.set(x, y - 3, z + 1, "azalea_leaves[distance=7,persistent=true,waterlogged=false]");
            o.set(x, y - 4, z - 1, "azalea_leaves[distance=7,persistent=true,waterlogged=false]");
            o.set(x - 1, y - 7, z, "azalea_leaves[distance=7,persistent=true,waterlogged=false]");
            o.area(x - 1, y, z - 1, x + 1, z + 1);
        }
        o.end(0, 0, 18);
        meadowBase(o, 0, 0, 18);
        return o;
    }

    // ================================================================ MEDIUM: Frozen Harbour
    static Ob med1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Ice Floe Slide", "ICE");
        // one-wide blue-ice floes (3 long), each shifted sideways, with four-block leads of open water between them: the ice keeps
        // your speed, so the long jumps only work with a clean sprint-jump off the floe's far edge
        int[][] fl = {{0, 5, 6, 0}, {2, 10, 10, 1}, {0, 15, 16, 1}, {2, 21, 21, 1}};  // x0, z0, z1, y
        for (int[] f : fl) {
            for (int z = f[1]; z <= f[2]; z++) {
                int x = f[0];
                o.set(x, f[3], z, "blue_ice");
                for (int k = 1; k <= 6; k++) {
                    double sh = k * 0.35;
                    if (Math.abs(z - (f[1] + f[2]) / 2.0) <= 2.6 - sh) o.set(x, f[3] - k, z, k < 3 ? "packed_ice" : "ice");
                }
                if (z == (f[1] + f[2]) / 2) for (int k = 2; k <= 4; k++) { o.set(x - 1, f[3] - k, z, "packed_ice"); o.set(x + 1, f[3] - k - 1, z, "ice"); }
            }
            o.areaT(Ob.T.ICE, f[0], f[3], f[1], f[0], f[2]);
        }
        o.end(1, 1, 27);
        Sh.padBase(o, 1, 1, 27, "packed_ice", "snow_block", "packed_ice", 3);
        return o;
    }

    static Ob med2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Lighthouse Ladder", "LADDER");
        int cz = 9, top = 6;
        // rowboat moored below the tower (scenery)
        for (int z = 6; z <= 10; z++) for (int x = 5; x <= 7; x++) {
            if (Math.abs(x - 6) + Math.abs(z - 8) > 3) continue;
            o.set(x, -6, z, "spruce_planks");
            if (x != 6) o.set(x, -5, z, "spruce_slab[type=bottom,waterlogged=false]");
        }
        // striped tower with a rib on its north face carrying the ladder
        for (int y = -12; y <= top; y++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (!Sh.inDisc(dx, dz, 2.8)) continue;
            o.set(dx, y, cz + dz, (Math.floorDiv(y, 3) & 1) == 0 ? "red_concrete" : "white_concrete");
        }
        for (int y = -12; y <= top; y++) o.set(0, y, cz - 3, "polished_andesite");
        // catch the ladder on the run, climb to the little balcony at the top
        o.ladder(0, 0, top, cz - 4, "north");
        for (int x = -1; x <= 1; x++) { o.set(x, top, cz - 5, "polished_andesite"); o.set(x, top - 1, cz - 5, "polished_andesite_slab[type=top,waterlogged=false]"); }
        for (int y = top - 2; y >= top - 4; y--) o.set(0, y, cz - 5, "andesite_wall");
        o.area(-1, top, cz - 5, 1, cz - 5);
        // then two 3+1 jumps up the broken gallery hanging off the east side
        gallery(o, 5, 5, cz - 4, cz - 3, top + 1);
        gallery(o, 5, 5, cz + 1, cz + 2, top + 2);
        // lamp room covering the whole tower top (no walkable ring to cut across)
        for (int y = top + 1; y <= top + 4; y++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (!Sh.inDisc(dx, dz, 2.8)) continue;
            boolean edge = !Sh.inDisc(dx, dz, 1.8);
            o.set(dx, y, cz + dz, y == top + 4 ? "red_concrete" : edge ? (y == top + 1 ? "white_concrete" : "light_blue_stained_glass") : (y == top + 2 ? "sea_lantern" : "air"));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.set(dx, top + 5, cz + dz, Math.abs(dx) + Math.abs(dz) <= 1 ? "red_concrete" : "air");
        o.set(0, top + 6, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        o.end(5, top + 2, cz + 6);
        Sh.padBase(o, 5, top + 2, cz + 6, "packed_ice", "snow_block", "packed_ice", 3);
        return o;
    }

    /** A broken gallery piece: andesite deck x0..x1, z0..z1 at y on a wall bracket. */
    static void gallery(Ob o, int x0, int x1, int z0, int z1, int y) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            o.set(x, y, z, "polished_andesite");
            for (int k = 1; k <= 3; k++) o.set(x, y - k, z, k == 1 ? "polished_andesite_slab[type=top,waterlogged=false]" : "andesite_wall");
        }
        o.area(x0, y, z0, x1, z1);
    }

    // ================================================================ HARD: Library Tower
    static final String[] BOOK = {"red_wool", "blue_wool", "green_wool", "brown_wool", "purple_wool", "cyan_wool", "orange_wool", "black_wool"};

    static Ob hard1(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Spine Scramble", "UP");
        int wz = 7, roof = 11;
        // the library: a solid shelf wall (front face z = wz) with a parapet roof at y = roof
        for (int x = -7; x <= 7; x++) for (int z = wz; z <= wz + 9; z++) for (int y = -12; y <= roof; y++) {
            boolean face = z == wz;
            String st;
            if (y == roof) st = "dark_oak_planks";
            else if (face) st = (x == -7 || x == 7) ? "stripped_dark_oak_log[axis=y]" : (Math.floorMod(y, 4) == 0 ? "dark_oak_slab[type=double,waterlogged=false]" : "bookshelf");
            else st = (x == -7 || x == 7 || z == wz + 9) ? (Math.floorMod(y, 5) == 0 ? "stripped_dark_oak_log[axis=x]" : "red_nether_bricks") : "dark_oak_planks";
            o.set(x, y, z, st);
        }
        for (int x = -7; x <= 7; x++) for (int z = wz; z <= wz + 9; z++) o.safe(x, roof, z, x, z);
        // tall arched windows on the sides
        for (int z = wz + 2; z <= wz + 7; z += 5) for (int y = -6; y <= roof - 3; y++) for (int sx = -7; sx <= 7; sx += 14) o.set(sx, y, z, "yellow_stained_glass");
        // reading stool to start from
        for (int x = -2; x <= 0; x++) for (int z = 3; z <= 4; z++) { o.set(x, 0, z, BOOK[(x + z + 8) & 7]); o.set(x, -1, z, BOOK[(x + z + 11) & 7]); o.set(x, -2, z, BOOK[(x + z + 13) & 7]); }
        o.set(-1, -3, 3, "dark_oak_fence"); o.set(-1, -3, 4, "dark_oak_fence");
        o.area(-2, 0, 3, 0, 4);
        // book spines: a 3+1 onto a double spine, a four-block flat leap to a single one, the rolling ladder up to its
        // top step, then two 3+1s from single spine to single spine
        spine(o, -5, 1, wz - 1, 0);
        spine2(o, -1, 0, 2, wz - 1, 1);
        spine(o, 5, 2, wz - 1, 2);
        // rolling ladder with its top step standing out from the shelf
        o.ladder(6, 3, 8, wz - 1, "north");
        for (int x = 5; x <= 6; x++) { o.set(x, 8, wz - 2, "stripped_dark_oak_wood[axis=x]"); o.set(x, 7, wz - 2, "dark_oak_trapdoor[facing=north,half=top,open=false,waterlogged=false]"); }
        for (int y = 6; y >= 2; y--) o.set(5, y, wz - 2, "dark_oak_fence");
        o.area(5, 8, wz - 2, 6, wz - 2);
        spine(o, 1, 9, wz - 1, 3);
        spine(o, -3, 10, wz - 1, 4);
        o.end(-3, roof, wz + 2);
        // a chandelier in front of the shelves (scenery, well above the lower spines)
        Frame.chandelier(o.w, o.wx(0, 3), o.wy(14), o.wz(0, 3));
        return o;
    }

    /** Two book spines side by side (x0..x1) sticking out of the shelf. */
    static void spine2(Ob o, int x0, int x1, int y, int z, int i) {
        for (int x = x0; x <= x1; x++) { o.set(x, y, z, BOOK[(i + x - x0) % BOOK.length]); o.set(x, y - 1, z, BOOK[(i + 3) % BOOK.length].replace("wool", "terracotta")); }
        o.area(x0, y, z, x1, z);
    }

    /** One book spine sticking out of the shelf (1x1 foothold) with a smaller book under it. */
    static void spine(Ob o, int x, int y, int z, int i) {
        o.set(x, y, z, BOOK[i % BOOK.length]);
        o.set(x, y - 1, z, BOOK[(i + 3) % BOOK.length].replace("wool", "terracotta"));
        o.area(x, y, z, x, z);
    }

    static Ob hard2(W w, Ob.Slot s) {
        Ob o = new Ob(w, s, "Missing Treads", "SPIRAL");
        int cx = 1, cz = 7;
        // central column: a stack of giant books
        for (int y = 0; y <= 10; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (Math.abs(dx) + Math.abs(dz) < 2) o.set(cx + dx, y, cz + dz, dx == 0 && dz == 0 ? "bookshelf" : BOOK[(y / 2 + dx + 2 * dz + 8) % BOOK.length].replace("wool", "concrete"));
        Frame.chandelier(o.w, o.wx(cx, cz), o.wy(14), o.wz(cx, cz));
        for (int y = 11; y <= 14; y++) o.set(cx, y, cz, "chain[axis=y,waterlogged=false]");
        // walk across the library roof to the foot of the stair
        o.area(6, 0, 0, 7, 6);
        int[] ang = {45, 135, 180, 270, 315, 0};
        for (int k = 0; k < ang.length; k++) {
            double r = Math.toRadians(ang[k]);
            int x = cx + (int) Math.round(Math.sin(r) * 3), z = cz + (int) Math.round(Math.cos(r) * 3);
            int y = k + 1;
            for (int yy = y - 1; yy >= -12; yy--) {
                if (yy <= 0 && Bs.isAir(o.get(x, yy, z)) == false && yy < y - 1) break;
                o.set(x, yy, z, yy == y - 1 ? "dark_oak_slab[type=top,waterlogged=false]" : "dark_oak_fence");
            }
            if (k >= 1) {
                o.set(x, y - 1, z, "gold_block");
                o.timed("dark_oak_planks", 60, 36, Math.floorMod(-k * 10, 60), 15, null, new int[]{x, y, z});
            } else {
                o.set(x, y, z, "dark_oak_planks");
                o.area(x, y, z, x, z);
            }
        }
        o.end(cx, 6, cz + 6);
        return o;
    }

}
