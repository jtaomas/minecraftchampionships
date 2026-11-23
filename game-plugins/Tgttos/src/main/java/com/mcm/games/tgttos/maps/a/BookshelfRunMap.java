package com.mcm.games.tgttos.maps.a;

import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Design;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Noise.Pal;
import com.mcm.games.tgttos.maps.a.util.Stairs;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Bookshelf Run (parkour, giant scale, snowballs only). Mouse-sized players on a giant wall shelf: a front row of
 * standing books (fast line on their tops: 2-3 gaps, rises, a head-hitter under a lying book) beside a cluttered
 * back alley (safe line), a leaning-book ramp, then off the shelf end: a 3-wide pencil over the void (choke) to
 * a dictionary stack climbed by push-pins (fast), or a ruler slide down to the desk and round to a spoon ramp
 * (slow). The goal is a teacup on the desk under a giant desk lamp.
 *
 * <p>Local frame: x across (shelf front edge x = -6, room wall x = 10), z along the run. Shelf top y = 0 (feet 1);
 * start atlas top y = 2 (feet 3); desk top y = -12 (feet -11); teacup floor y = -11 (feet -10).
 */
public final class BookshelfRunMap extends AMap {

    @Override public String id() { return "bookshelf_run"; }
    @Override public String displayName() { return "Bookshelf Run"; }
    @Override public MapType type() { return MapType.PARKOUR; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }

    private Gen g;
    static final String PAGES = "smooth_sandstone";
    private final Pal pages = new Pal("smooth_sandstone", 70, "birch_planks", 20, "sandstone", 10);
    private final Pal walnut = new Pal("dark_oak_planks", 70, "spruce_planks", 20, "stripped_dark_oak_wood[axis=y]", 10);
    private final Pal desk = new Pal("spruce_planks", 60, "dark_oak_planks", 30, "stripped_spruce_wood[axis=y]", 10);
    private static final String[] COVERS = {"red_terracotta", "green_terracotta", "blue_terracotta", "red_nether_bricks",
            "dark_prismarine", "brown_terracotta", "cyan_terracotta", "crimson_planks", "warped_planks", "black_terracotta"};

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;
        room();
        shelf();
        startAtlas(d);
        frontBooks();
        alley();
        dominoRamp();
        pencilAndRuler();
        deskAndStack();
        cup(d, de);
        lamp(de);
        roomScenery();
        // desk crumbs, sugar cubes and cheese: no free sprint on the desk or the big dictionary
        java.util.List<Region> keep = java.util.List.of(Region.of(-10, -12, 77, 11, 0, 98), Region.of(-2, -12, 94, 1, 0, 101), Region.of(4, -12, 58, 10, -10, 61));
        g.breakRuns(-20, 18, 59, 105, -11, 8, keep, "bone_block[axis=y]", "white_terracotta", "yellow_terracotta", "brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]");
        g.breakRuns(-12, 4, 60, 78, -5, 8, java.util.List.of(), "gold_block", "pink_terracotta", "lime_terracotta");

        d.voidY = -20;
        d.playBounds = Region.of(-22, -24, -2, 13, 36, 108);
        d.overviewPoints.add(new Spawn(-24, 22, 20, -60f, 30f));
        d.overviewPoints.add(new Spawn(-6.5, 12, -8, -20f, 20f));
        d.notes.put("fastLine", "book tops (2-3 gaps, head-hitter), pencil choke, push-pins up the dictionaries, leap into the cup");
        d.notes.put("safeLine", "back alley clutter, step books, ruler slide to the desk, round to the spoon ramp");

        de.searchBox = Region.of(-22, -22, -2, 13, 30, 106);
        de.lane.add(Region.of(-20, -30, -4, 9, 30, 96));
        de.sprintExempt.add(Region.of(-22, -12, 58, 13, -10, 106));    // desk top crossings checked per line

        Design.Line fast = de.line("FAST", false, true);
        fast.description = "leaning-book ramp onto the book tops, gaps 2-3 with rises and a head-hitter, leaning ramp, pencil over the void (3 wide), push-pins, coins, leap into the cup";
        fast.leg("book tops", Region.of(-6, 5, 35, 0, 5, 36), Region.of(-8, 0, 0, 2, 12, 37))
            .leg("pencil", Region.of(-3, 3, 50, 1, 3, 56), Region.of(-8, 0, 29, 3, 12, 57))
            .leg("dictionary D", Region.of(-5, 8, 70, 0, 8, 75), Region.of(-9, -6, 50, 4, 12, 76))
            .leg("cup", d.pen, Region.of(-8, -11, 68, 8, 12, 94));
        Design.Line safe = de.line("SAFE", false, false);
        safe.maxGap = 2;
        safe.description = "back alley (hop clutter), step books up to the leaning ramp top, ruler slide to the desk, round the stack to the spoon ramp";
        safe.leg("tome top", Region.of(2, 6, 38, 9, 6, 42), Region.of(1, 0, 0, 9, 9, 42))
            .leg("desk", Region.of(3, -11, 60, 9, -11, 62), Region.of(-6, -12, 38, 9, 9, 62))
            .leg("cup", d.pen, Region.of(-12, -12, 56, 13, 0, 104));
    }

    // ------------------------------------------------------------------ helpers

    /** Standing or lying giant book: solid cover, page block on the top/back faces, gold bands on the spine (x1 side). */
    private void book(int x1, int x2, int y1, int y2, int z1, int z2, String cover, boolean spineFront) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) {
            boolean coverFace = z == z1 || z == z2;
            String s;
            if (coverFace) s = cover;
            else if (spineFront && x == x1) s = (y == y1 + 1 || y == y2 - 1) ? "gold_block" : cover;
            else if (y == y2 || x == x2) s = pages.at(x, y, z, g.seed);
            else s = cover;
            g.set(x, y, z, s);
        }
    }

    private String cover(int i) { return COVERS[Math.floorMod(i, COVERS.length)]; }

    // ------------------------------------------------------------------ room, shelf

    private void room() {
        // back wall with wallpaper stripes and a wainscot
        for (int z = -8; z <= 58; z++) for (int y = -18; y <= 40; y++) {
            String s;
            if (y < -8) s = (Math.floorMod(z, 6) == 0) ? "stripped_dark_oak_wood[axis=y]" : "dark_oak_planks";
            else if (y == -8) s = "dark_oak_slab[type=top]";
            else s = Math.floorMod(z, 6) < 2 ? "green_terracotta" : (Math.floorMod(z, 6) == 3 ? "lime_terracotta" : PAGES);
            g.set(10, y, z, s);
        }
        // wall to the window side (behind the desk)
        for (int x = -26; x <= 24; x++) for (int y = -18; y <= 40; y++) {
            boolean window = x >= -14 && x <= 14 && y >= -6 && y <= 30;
            boolean mullion = window && (Math.floorMod(x, 7) == 0 || y == 12);
            String s = window ? (mullion ? "white_terracotta" : "light_blue_stained_glass") : (Math.floorMod(x, 6) < 2 ? "green_terracotta" : PAGES);
            if (y < -14 && !window) s = "dark_oak_planks";
            g.set(x, y, 110, s);
        }
        // curtains
        for (int y = -12; y <= 33; y++) for (int x = -19; x <= -15; x++) g.set(x, y, 109, (x + y) % 3 == 0 ? "red_nether_bricks" : "red_wool");
        for (int y = -12; y <= 33; y++) for (int x = 15; x <= 19; x++) g.set(x, y, 109, (x + y) % 3 == 0 ? "red_nether_bricks" : "red_wool");
        for (int x = -21; x <= 21; x++) g.set(x, 34, 109, "gold_block");
        // the sky outside: sun disc behind the window
        g.cyl(4.5, 140.5, 5, 0, 0, new Pal("air", 1));
        for (int x = -2; x <= 10; x++) for (int y = 14; y <= 26; y++) {
            double dd = Math.hypot(x - 4, y - 20);
            if (dd <= 6) g.set(x, y, 150, dd < 4.5 ? "glowstone" : "shroomlight");
        }
    }

    private void shelf() {
        g.fill(-6, -3, -6, 9, 0, 47, walnut);
        for (int z = -6; z <= 47; z++) { g.set(-6, 0, z, "stripped_dark_oak_log[axis=z]"); g.set(-6, -3, z, "stripped_dark_oak_log[axis=z]"); }
        for (int x = -6; x <= 9; x++) { g.set(x, 0, 47, "stripped_dark_oak_log[axis=x]"); g.set(x, -3, 47, "stripped_dark_oak_log[axis=x]"); }
        // ornate brackets under the shelf
        for (int z = -2; z <= 46; z += 12) for (int i = 0; i < 8; i++) {
            for (int x = 9 - i; x <= 9; x++) g.set(x, -4 - i, z, i == 0 || x == 9 - i ? Gen.st("dark_oak", "west", true) : "dark_oak_planks");
            for (int x = 9 - i; x <= 9; x++) g.set(x, -4 - i, z + 1, i == 0 || x == 9 - i ? Gen.st("dark_oak", "west", true) : "dark_oak_planks");
        }
        // upper shelf (scenery) with giant books
        g.fill(-6, 25, -6, 9, 26, 56, walnut);
        for (int z = -6; z <= 56; z++) g.set(-6, 26, z, "stripped_dark_oak_log[axis=z]");
        int z = -5, i = 3;
        while (z < 54) {
            int t = 2 + (int) (g.rnd(z, 0, 0, 3) * 3);
            int h = 9 + (int) (g.rnd(z, 0, 0, 4) * 6);
            if (g.rnd(z, 1, 1, 5) < 0.12) { z += 2; continue; }
            book(-5, 7, 27, 26 + h, z, Math.min(55, z + t - 1), cover(i++), true);
            z += t;
        }
    }

    private void startAtlas(TgMapData d) {
        // giant atlas lying flat, sticking out over the front of the shelf
        for (int x = -19; x <= 9; x++) for (int z = 0; z <= 8; z++) {
            boolean edge = x == -19 || z == 0 || z == 8;
            g.set(x, 1, z, edge ? "blue_terracotta" : pages.at(x, 1, z, g.seed));
            g.set(x, 2, z, ((x + z) & 1) == 0 ? "blue_terracotta" : "dark_prismarine");
        }
        for (int x = -19; x <= 9; x++) g.set(x, 2, 8, (x & 1) == 0 ? "gold_block" : "black_terracotta");   // start line on the cover edge
        g.fill(-19, 0, 0, -7, 0, 8, "blue_terracotta");
        for (int x = -19; x <= 9; x++) for (int y = 3; y <= 5; y++) { g.set(x, y, 9, "glass"); d.startBarrier.add(new Pos(x, y, 9)); }
        // brass bookend on the open side, a standing book behind
        for (int z = -1; z <= 9; z++) for (int y = 1; y <= 9; y++) g.set(-20, y, z, y == 9 ? "cut_copper" : (y % 4 == 0 ? "waxed_copper_block" : "gold_block"));
        for (int z = -1; z <= 9; z++) g.set(-20, 0, z, "gold_block");
        book(-19, 9, 1, 16, -3, -1, "red_nether_bricks", true);
        d.startStrip = Region.of(-19, 3, 0, 9, 6, 8);
        for (int row = 0; row < 4; row++)
            for (int k = 0; k < 15; k++) d.startSpawns.add(new Spawn(-18.5 + k * 1.9 + (row % 2) * 0.5, 3, 1.5 + row * 2, 0f));
    }

    // ------------------------------------------------------------------ section 1: book row (fast) and alley (safe)

    private void frontBooks() {
        // entry: a leaning book as a ramp from the shelf up to the first top (x -6..-3)
        Stairs.flight(g, "leaning entry book", "red_nether_brick", "red_nether_bricks", -6, 10, 0, 0, 1, 5, 4);
        // standing book row on the front half (x -6..1): {z1, z2, topY}
        int[][] row = {{15, 17, 5}, {20, 22, 6}, {24, 26, 4}, {29, 31, 4}, {35, 36, 4}};
        int i = 0;
        for (int[] b : row) book(-6, 1, 1, b[2], b[0], b[1], cover(i++), true);
        // thin books in the low gaps (below the jump arcs), so the row reads as a full shelf
        book(-6, 1, 1, 2, 18, 19, cover(7), true);
        book(-6, 1, 1, 1, 27, 28, cover(8), true);
        book(-6, 1, 1, 1, 32, 34, cover(6), true);
        // lying book on two posts: a low ceiling over B4 and B5 (head-hitter on the 3-gap between them)
        book(1, 1, 5, 6, 29, 29, "gold_block", false);
        book(1, 1, 5, 6, 36, 36, "gold_block", false);
        book(-6, 1, 7, 8, 29, 36, "dark_prismarine", false);
        for (int z = 29; z <= 36; z++) g.set(-6, 7, z, z == 32 || z == 33 ? "gold_block" : "dark_prismarine");
    }

    private void alley() {
        // clutter along the back alley (x 2..9), the safe line hops and weaves
        book(3, 7, 1, 1, 12, 13, "pink_terracotta", false);           // eraser
        g.fill(2, 1, 17, 7, 1, 17, "iron_bars");                     // paperclip loop
        g.fill(2, 1, 19, 7, 1, 19, "iron_bars");
        g.fill(2, 1, 17, 2, 1, 19, "iron_bars");
        g.fill(7, 1, 17, 7, 1, 19, "iron_bars");
        book(5, 6, 1, 1, 22, 22, "pink_terracotta", false);           // eraser step
        book(2, 9, 1, 2, 23, 26, "orange_terracotta", false);         // toppled paperback (2 high)
        g.ring(5.5, 30.5, 2.6, 1, 1, 1, new Pal("white_terracotta", 1));   // tape roll
        g.ring(5.5, 30.5, 1.6, 1, 1, 1, new Pal("brown_terracotta", 1));
        g.set(8, 1, 14, "candle[candles=4,lit=true,waterlogged=false]");
        g.set(9, 1, 33, "lantern[hanging=false,waterlogged=false]");
        // staggered paperbacks up to the tome (x 4..9)
        for (int k = 0; k < 4; k++) book(4, 9, 1, 1 + k, 34 + k, 34 + k, cover(k + 3), false);
    }

    private void dominoRamp() {
        // one huge tome lying across the shelf (z 38..42, top 5): both lines meet on it
        book(-6, 9, 1, 5, 38, 42, "red_terracotta", true);
        for (int x = -6; x <= 9; x += 3) g.set(x, 5, 40, "gold_block");
        // shelf end z 43..47: a sharpener and an inkpot as obstacles
        book(5, 7, 1, 2, 44, 45, "gray_terracotta", false);
        g.set(-5, 1, 45, "decorated_pot[facing=south,waterlogged=false,cracked=false]");
    }

    // ------------------------------------------------------------------ pencil (choke) and ruler (long way)

    private void pencilAndRuler() {
        // pencil x -3..-1, y 1..2, from z 43 (eraser end on the shelf) to z 64 (tip on dictionary B)
        for (int z = 43; z <= 64; z++) {
            String body = z < 45 ? "pink_terracotta" : z < 47 ? "gold_block" : z > 60 ? "stripped_birch_log[axis=z]" : "yellow_terracotta";
            if (z >= 63) body = "black_terracotta";
            g.set(-2, 1, z, body);
            g.set(-2, 2, z, body);
            g.set(-3, 1, z, z >= 61 ? "birch_stairs[facing=east,half=top,shape=straight,waterlogged=false]" : "orange_terracotta");
            g.set(-1, 1, z, z >= 61 ? "birch_stairs[facing=west,half=top,shape=straight,waterlogged=false]" : "orange_terracotta");
            g.set(-3, 2, z, z >= 61 ? "birch_slab[type=bottom,waterlogged=false]" : Gen.st(z < 45 ? "granite" : "smooth_sandstone", "east", false).replace("smooth_sandstone_stairs", "sandstone_stairs"));
            g.set(-1, 2, z, z >= 61 ? "birch_slab[type=bottom,waterlogged=false]" : Gen.st(z < 45 ? "granite" : "smooth_sandstone", "west", false).replace("smooth_sandstone_stairs", "sandstone_stairs"));
        }
        g.set(-2, 1, 42, "pink_terracotta");                           // step onto the eraser end
        for (int z = 52; z <= 53; z++) { g.set(-2, 3, z, "gold_block"); g.set(-3, 3, z, "cut_copper_slab[type=bottom,waterlogged=false]"); g.set(-1, 3, z, "cut_copper_slab[type=bottom,waterlogged=false]"); }
        // ruler: long way round, a slide from the shelf corner down to the desk (birch steps, numbered with dark marks)
        Stairs.flight(g, "ruler slide", "birch", null, 5, 59, -12, 0, -1, 12, 5);
        for (int k = 0; k < 12; k += 2) g.set(9, -11 + k, 59 - k, Gen.st("dark_oak", "north", false));
    }

    // ------------------------------------------------------------------ desk, dictionary stack

    private void deskAndStack() {
        g.fill(-24, -14, 58, 22, -12, 106, desk);
        for (int x = -24; x <= 22; x++) { g.set(x, -12, 58, "stripped_dark_oak_log[axis=x]"); g.set(x, -13, 58, "stripped_dark_oak_log[axis=x]"); }
        for (int[] c : new int[][]{{-23, 59}, {20, 59}, {-23, 104}, {20, 104}})
            g.fill(c[0], -40, c[1], c[0] + 2, -15, c[1] + 2, "dark_oak_log[axis=y]");
        // A (bottom), B (pencil rests on it), C, D
        book(-12, 4, -11, -6, 60, 78, "blue_terracotta", false);
        book(-9, 4, -5, 0, 62, 76, "red_terracotta", false);
        book(-7, 2, 1, 4, 69, 72, "green_terracotta", false);
        book(-5, 0, 5, 7, 70, 75, "dark_prismarine", false);
        for (int z = 60; z <= 78; z++) g.set(-12, -9, z, "gold_block");
        for (int z = 62; z <= 76; z++) g.set(-9, -2, z, "gold_block");
        // FAST: push-pins (1x1 heads) up from B to C
        pin(-2, 1, 63);
        pin(1, 2, 65);
        pin(-1, 3, 67);
        // SAFE-ish: sticky-note pads stair on B (x 2..4)
        g.fill(2, 1, 63, 4, 1, 65, "lime_terracotta");
        g.fill(2, 1, 66, 4, 2, 67, "pink_terracotta");
        g.fill(2, 1, 68, 4, 3, 68, "light_blue_terracotta");
        // coins from C up to D
        g.cyl(1.5, 71.5, 1.3, 5, 5, new Pal("gold_block", 1));
        g.cyl(1.5, 73.5, 1.3, 5, 6, new Pal("gold_block", 1));
        // desk clutter: inkwell, quill, stack of letters, candle, globe on the far side
        g.cyl(-15.5, 66.5, 2.6, -11, -8, new Pal("black_stained_glass", 1));
        g.cyl(-15.5, 66.5, 1.4, -7, -7, new Pal("black_terracotta", 1));
        g.line(-15, -6, 66, -10, 6, 72, "white_wool");
        g.line(-14, -6, 66, -9, 6, 72, "white_wool");
        book(9, 15, -11, -9, 70, 78, "white_terracotta", false);
        book(10, 14, -8, -8, 71, 76, "white_terracotta", false);
        g.cyl(14.5, 92.5, 1.5, -11, -3, new Pal("white_terracotta", 1));
        g.set(14, -2, 92, "fire_coral_block");
        g.set(14, -1, 92, "lantern[hanging=false,waterlogged=false]");
        g.cyl(-14.5, 92.5, 1.2, -11, -9, new Pal("spruce_planks", 1));
        g.ellipsoid(-14.5, -3.5, 92.5, 4.5, 4.5, 4.5, new Pal("blue_terracotta", 50, "green_terracotta", 30, "lime_terracotta", 20));
        g.fill(-15, -9, 92, -14, -8, 92, "gold_block");
    }

    private void pin(int x, int headY, int z) {
        for (int y = 1; y < headY; y++) g.set(x, y, z, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        g.set(x, headY, z, "red_terracotta");
    }

    // ------------------------------------------------------------------ cup (goal) and lamp (landmark)

    private void cup(TgMapData d, Design de) {
        double cx = 0.5, cz = 87.5;
        // saucer
        g.cyl(cx, cz, 9.5, -11, -11, new Pal("white_terracotta", 80, "light_blue_terracotta", 20));
        g.ring(cx, cz, 9.5, 1, -10, -10, new Pal("white_terracotta", 1));
        // cup walls (rim at y -6), floor of tea
        g.ring(cx, cz, 6.6, 1.2, -10, -6, new Pal("white_terracotta", 70, "calcite", 30));
        g.ring(cx, cz, 6.6, 1.2, -8, -8, new Pal("light_blue_terracotta", 1));
        g.cyl(cx, cz, 5.4, -11, -11, new Pal("brown_terracotta", 60, "orange_terracotta", 40));
        // handle on the +x side
        for (int y = -10; y <= -7; y++) { g.set(8, y, 87, "white_terracotta"); }
        g.set(7, -7, 87, "white_terracotta");
        g.set(7, -10, 87, "white_terracotta");
        d.pen = Region.of(-3, -10, 84, 3, -8, 90);
        for (int i = 0; i < 10; i++) d.chickenSpawns.add(new Spawn(-2.5 + (i % 5) * 1.3, -10, 85.5 + (i / 5) * 3, 180f));
        // spoon ramp on the far side (safe line): from the desk up to the rim
        Stairs.flight(g, "spoon ramp", "polished_diorite", null, -1, 99, -12, 0, -1, 6, 2);
        g.ellipsoid(-0.5, -11.5, 101.5, 2.2, 0.6, 2.8, new Pal("polished_diorite", 1));
        // sugar cubes beside the saucer
        g.fill(-12, -11, 84, -11, -10, 85, "white_concrete_powder".replace("white_concrete_powder", "bone_block[axis=y]"));
        g.fill(-11, -11, 88, -10, -11, 89, "bone_block[axis=y]");
    }

    private void lamp(Design de) {
        // base on the desk behind-right of the cup, arm up and over the cup, glowing shade
        g.cyl(12.5, 99.5, 3.2, -11, -10, new Pal("gold_block", 60, "cut_copper", 40));
        for (int y = -9; y <= 24; y++) g.set(12, y, 99, "cut_copper");
        g.line(12, 24, 99, 3, 30, 92, "cut_copper");
        g.line(12, 25, 99, 3, 31, 92, "waxed_cut_copper");
        // shade: cone opening downward over the cup
        for (int y = 23; y <= 30; y++) {
            double r = 1.5 + (30 - y) * 0.75;
            g.ring(1.5, 89.5, r, 1.0, y, y, new Pal("green_terracotta", 70, "gold_block", 30));
        }
        g.cyl(1.5, 89.5, 2.2, 24, 24, new Pal("ochre_froglight", 1));
        g.cyl(1.5, 89.5, 1.2, 23, 23, new Pal("glowstone", 1));
        for (int y = 23; y <= 30; y += 2) de.landmark.add(new Pos(1 - (int) Math.round(1.5 + (30 - y) * 0.75) + 1, y, 89));
        de.landmark.add(new Pos(1, 30, 89));
        de.landmark.add(new Pos(12, 24, 99));
    }

    private void roomScenery() {
        // giant armchair floating out in the room on the open side
        g.fill(-52, -30, 30, -38, -22, 46, "red_wool");
        g.fill(-52, -21, 30, -50, -6, 46, "red_wool");
        g.fill(-52, -21, 30, -38, -16, 31, "red_nether_bricks");
        g.fill(-52, -21, 45, -38, -16, 46, "red_nether_bricks");
        for (int[] c : new int[][]{{-52, 30}, {-39, 30}, {-52, 45}, {-39, 45}}) g.fill(c[0], -40, c[1], c[0] + 1, -31, c[1] + 1, "dark_oak_log[axis=y]");
        // standing floor lamp in the room
        g.fill(-34, -40, 74, -34, 18, 74, "cut_copper");
        g.ring(-33.5, 74.5, 5, 1, 19, 26, new Pal("lime_terracotta", 1));
        g.cyl(-33.5, 74.5, 2, 19, 19, new Pal("shroomlight", 1));
        // a picture frame on the back wall above the shelf
        for (int z = 6; z <= 30; z++) for (int y = 30; y <= 40; y++) {
            boolean edge = z == 6 || z == 30 || y == 30 || y == 40;
            g.set(9, y, z, edge ? "gold_block" : (y < 34 ? "green_terracotta" : (y < 37 ? "light_blue_terracotta" : "white_terracotta")));
        }
    }
}
