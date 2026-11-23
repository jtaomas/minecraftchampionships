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
            for (int x = x0 - 1; x <= x0 + 3; x++) for (int z = z0 - 1; z <= z0 + 3; z++) {
                int ex = Math.abs(x - (x0 + 1)), ez = Math.abs(z - (z0 + 1));
                if (ex + ez > 3) continue;                 // hexagon-ish 5x5 minus corners
                boolean top = ex <= 1 && ez <= 1;
                int yt = top ? y : y - 1;
                for (int yy = yt; yy >= y - depth + (ex + ez); yy--) {
                    String st = yy == yt ? (top ? "honeycomb_block" : "bee_nest[facing=north,honey_level=0]") : ((yy + ex + ez) % 4 == 0 ? "bee_nest[facing=north,honey_level=5]" : "honeycomb_block");
                    o.set(x, yy, z, st);
                }
                if (!top) o.set(x, y, z, null);
            }
            o.area(x0, y, z0, x0 + 2, z0 + 2);
            // ring around the top is one lower: register as part of the column
            for (int x = x0 - 1; x <= x0 + 3; x++) for (int z = z0 - 1; z <= z0 + 3; z++) o.safe(x, y - 1, z, x, z);
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
            for (int[] d : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) o.set(x + d[0], y - 1, z + d[1], petal[i].replace("concrete", "wool"));
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) o.set(x + dx, y - 1, z + dz, petal[i].replace("concrete", "terracotta"));
            for (int yy = y - 2; yy >= y - 10; yy--) o.set(x, yy, z, "green_concrete");
            o.set(x + 1, y - 5, z, "azalea_leaves[distance=7,persistent=true,waterlogged=false]");
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
        int[][] fl = {{-1, 4, 6, 0}, {0, 10, 12, 0}, {-1, 16, 18, 0}};  // x0, z0, z1, y ; floes 3 wide
        for (int[] f : fl) {
            for (int x = f[0]; x <= f[0] + 2; x++) for (int z = f[1]; z <= f[2]; z++) {
                o.set(x, f[3], z, "blue_ice");
                for (int k = 1; k <= 6; k++) {
                    double sh = k * 0.35;
                    if (Math.abs(x - (f[0] + 1)) + Math.abs(z - (f[1] + f[2]) / 2.0) <= 2.6 - sh + 1) o.set(x, f[3] - k, z, k < 3 ? "packed_ice" : "ice");
                }
            }
            for (int x = f[0] - 1; x <= f[0] + 3; x++) for (int z = f[1] - 1; z <= f[2] + 1; z++)
                if (x < f[0] || x > f[0] + 2 || z < f[1] || z > f[2]) o.set(x, f[3] - 1, z, "snow_block");
            o.areaT(Ob.T.ICE, f[0], f[3], f[1], f[0] + 2, f[2]);
        }
        o.end(0, 0, 23);
        Sh.padBase(o, 0, 0, 23, "packed_ice", "snow_block", "packed_ice", 3);
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
        // striped tower
        for (int y = -12; y <= top; y++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (!Sh.inDisc(dx, dz, 2.8)) continue;
            o.set(dx, y, cz + dz, (Math.floorDiv(y, 3) & 1) == 0 ? "red_concrete" : "white_concrete");
        }
        // gallery ring with a gap over the ladder
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            if (!Sh.inDisc(dx, dz, 4.8) || Sh.inDisc(dx, dz, 2.8)) continue;
            if (dx == 0 && dz < 0) continue;
            o.set(dx, top, cz + dz, "polished_andesite");
            o.set(dx, top - 1, cz + dz, Sh.inDisc(dx, dz, 3.8) ? "polished_andesite_slab[type=top,waterlogged=false]" : "andesite_wall");
        }
        o.ladder(0, 0, top, cz - 4, "north");
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) if (Sh.inDisc(dx, dz, 2.8)) o.safe(dx, top, cz + dz, dx, cz + dz);
        java.util.List<int[]> ring = new java.util.ArrayList<>();
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++)
            if (Sh.inDisc(dx, dz, 4.8) && !Sh.inDisc(dx, dz, 2.8) && !(dx == 0 && dz < 0)) ring.add(new int[]{dx, top, cz + dz});
        o.cells(Ob.T.AREA, ring.toArray(new int[0][]));
        // lamp room
        for (int y = top + 1; y <= top + 4; y++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (!Sh.inDisc(dx, dz, 2.2)) continue;
            boolean edge = !Sh.inDisc(dx, dz, 1.2);
            o.set(dx, y, cz + dz, y == top + 4 ? "red_concrete" : edge ? (y == top + 1 ? "white_concrete" : "light_blue_stained_glass") : (y == top + 2 ? "sea_lantern" : "air"));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.set(dx, top + 5, cz + dz, Math.abs(dx) + Math.abs(dz) <= 1 ? "red_concrete" : "air");
        o.set(0, top + 6, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        o.end(0, top, cz + 6);
        return o;
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
        int[][] sp = {{-5, 1}, {-1, 2}, {3, 3}};
        for (int i = 0; i < sp.length; i++) spine(o, sp[i][0], sp[i][1], wz - 1, i);
        // rolling ladder on a brass rail
        for (int x = 4; x <= 6; x++) o.set(x, roof - 1, wz - 1, "chain[axis=x,waterlogged=false]");
        o.ladder(5, 3, 8, wz - 1, "north");
        int[][] sp2 = {{2, 8}, {-2, 9}, {-6, 10}};
        for (int i = 0; i < sp2.length; i++) spine(o, sp2[i][0], sp2[i][1], wz - 1, i + 3);
        o.end(-5, roof, wz + 1);
        // a chandelier in front of the shelves (scenery, well above the lower spines)
        Frame.chandelier(o.w, o.wx(0, 3), o.wy(14), o.wz(0, 3));
        return o;
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
                o.timed("dark_oak_planks", 60, 36, k * 10, 15, null, new int[]{x, y, z});
            } else {
                o.set(x, y, z, "dark_oak_planks");
                o.area(x, y, z, x, z);
            }
        }
        o.end(cx, 6, cz + 6);
        return o;
    }

}
