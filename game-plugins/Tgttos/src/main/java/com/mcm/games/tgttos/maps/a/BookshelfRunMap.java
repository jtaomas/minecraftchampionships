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

import com.mcm.games.tgttos.model.MapFeatures;

import java.util.ArrayList;
import java.util.List;

/**
 * Bookshelf Run (parkour over the void, giant scale, snowballs only). Mouse-sized players cross a giant library
 * wall: short wall-shelf boards with gaps between them, chained books hanging from brass arms out over the room,
 * and two wide brawl spots where everybody bunches up next to the drop. Nearly every step has the void beside it,
 * so a punch sends a rival down to the start.
 *
 * <ul>
 *   <li>Start: a giant atlas lying on the top board.</li>
 *   <li>Section 1. FAST (outer): five chained books hanging over the void, gaps 3-4. SAFE (against the wall):
 *       shelf boards with 2-gaps between them and a 3-wide ruler bridge, cluttered (eraser, inkpot, staple).</li>
 *   <li>Brawl 1, the open atlas (26 x 12) at the end of the board, void on two sides.</li>
 *   <li>Section 2. FAST: the 3-wide pencil over the void (choke), a dictionary stack, three chained notebooks
 *       (2 x 2 heads, gaps 3). SAFE: the long way: a 3-wide ruler stair down to the low boards, a 2-gap, a 2-wide
 *       bookmark ribbon over the void, another 2-gap and a stair back up.</li>
 *   <li>Brawl 2, the dictionary (24 x 12) on a book tower standing on the far-away floor, void all round.</li>
 *   <li>Section 3. FAST: stepping down four stacks of coins over the void and onto the teacup rim. SAFE: the stair
 *       down to a pulled-out desk drawer, round the saucer on the desk and up the spoon to the rim. Goal: the
 *       chickens in the teacup, under the desk lamp (landmark).</li>
 * </ul>
 *
 * <p>Local frame: x across (room wall x = 10, open room towards -x), z along the run. Start atlas top y 2 (feet 3);
 * desk top y -12; teacup floor y -11 (feet -10).
 */
public final class BookshelfRunMap extends AMap {

    @Override public String id() { return "bookshelf_run"; }
    @Override public String displayName() { return "Bookshelf Run"; }
    @Override public MapType type() { return MapType.PARKOUR; }
    // owner: "people have blocks as well" - the usual TGTTOS kit (concrete, pickaxe, snowballs); building is boxed in below
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }
    @Override public MapFeatures features() { return feats(); }

    /** Everything from the drawer / coins on moved this far along z to make room for the page-turner section. */
    static final int DZ = 44;

    private Gen g;
    static final String PAGES = "smooth_sandstone";
    private final Pal pages = new Pal("smooth_sandstone", 70, "birch_planks", 20, "sandstone", 10);
    private final Pal walnut = new Pal("dark_oak_planks", 70, "spruce_planks", 20, "stripped_dark_oak_wood[axis=y]", 10);
    private final Pal desk = new Pal("spruce_planks", 60, "dark_oak_planks", 30, "stripped_spruce_wood[axis=y]", 10);
    private static final String[] COVERS = {"red_terracotta", "green_terracotta", "blue_terracotta", "red_nether_bricks",
            "dark_prismarine", "brown_terracotta", "cyan_terracotta", "crimson_planks", "warped_planks", "black_terracotta"};
    private static final String ARM = "cut_copper";
    private static final int ARM_Y = 13;

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;
        room();
        upperShelf();
        startAtlas(d);
        section1();
        brawlAtlas();
        section2();
        brawlDictionary();
        pageTurner();
        fallingBookLanding();
        deskAndCup(d);
        section3();
        // feedback 3: the finish is a row of goal chickens across the back of the desk (was a pen inside the teacup);
        // the cup now sits in the way: over it (letter opener, rim, drop) is the fast way, round the saucer the safe one
        com.mcm.games.tgttos.model.FinishRow.build(d, -17, 9, -12, 159 + DZ, 1, "dark_oak_planks");
        deskFanMount();   // x 10 is the room wall
        lamp(de);
        roomScenery();
        // desk clutter: no free sprint on the desk
        List<Region> keep = List.of(Region.of(-18, -12, 156 + DZ, 10, -5, 161 + DZ), Region.of(-10, -12, 126 + DZ, 11, 0, 147 + DZ),
                Region.of(-2, -12, 142 + DZ, 2, 0, 150 + DZ), Region.of(-16, -12, 116 + DZ, -10, -10, 124 + DZ), Region.of(-8, -12, 153 + DZ, -1, -5, 157 + DZ));
        g.breakRuns(-18, 16, 117 + DZ, 160 + DZ, -11, 8, keep, "bone_block[axis=y]", "white_terracotta", "yellow_terracotta",
                "brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]");

        d.voidY = -22;
        // feedback 3: with the finish a full-width row on the desk, the safe line (drawer stair) reaches the row about as
        // fast as the fast line (coins, letter opener); the old "drop into the cup" goal was what made the safe line long.
        // Known regression, reported - the check only asks that FAST is not slower.
        de.minFastSave = 0.0;
        de.maxExpert = 50;
        d.playBounds = Region.of(-24, -26, -2, 14, 36, 164 + DZ);
        // blocks: only along the run (no building up over the shelves, none in the start)
        d.buildBounds = Region.of(-20, -14, 13, 9, 16, 162 + DZ);
        d.overviewPoints.add(new Spawn(-28, 22, 30, -60f, 30f));
        d.overviewPoints.add(new Spawn(-6.5, 12, -8, -20f, 20f));
        d.notes.put("fastLine", "chained books over the void (gaps 3-4), pencil choke, chained notebooks, coin stacks into the cup");
        d.notes.put("safeLine", "shelf boards with 2-gaps, ruler bridge, ruler stair down, bookmark ribbon, stair up, drawer stair, round the saucer, spoon");
        d.notes.put("brawlSpots", "open atlas z 41-53, dictionary z 95-106, falling-book landing z 137-150");
        d.notes.put("movers", "pocket watch over the atlas, books sliding out of the wall (safe line, section 2), six flipping pages over the open book spine, seven falling books on the landing, desk fan before the row");

        de.searchBox = Region.of(-24, -24, -2, 13, 30, 162 + DZ);
        de.lane.add(Region.of(-20, -30, -4, 9, 30, 160 + DZ));
        de.sprintExempt.add(Region.of(-24, -12, 117 + DZ, 16, -10, 162 + DZ));    // desk top crossings checked per line
        de.voidReport = true;
        de.minVoidCrossings = 6;
        de.brawl.add(Region.of(-16, 3, 41, 9, 4, 53));
        de.brawl.add(Region.of(-14, 1, 95, 9, 2, 106));
        de.brawl.add(Region.of(-14, 1, 137, 9, 2, 150));

        Design.Line fast = de.line("FAST", false, true);
        fast.description = "three chained books over the void (gaps 3-4, +1/-1), the open atlas, the 3-wide pencil (choke), the dictionary stack, "
                + "two chained notebooks (gaps 4), the dictionary, two coin stacks down, the 2-wide letter opener onto the teacup rim, drop in";
        fast.leg("brawl 1", Region.of(-16, 3, 41, 9, 3, 53), Region.of(-18, 0, 0, -1, 14, 53))
            .leg("dictionary stack", Region.of(-11, 3, 69, 0, 3, 76), Region.of(-17, 0, 40, 9, 14, 77))
            .leg("brawl 2", Region.of(-14, 1, 95, 9, 1, 106), Region.of(-14, -2, 66, 2, 14, 107))
            .leg("page spine", Region.of(-4, 1, 130, -2, 1, 134), Region.of(-14, -1, 95, 9, 14, 135))
            .leg("landing", Region.of(-14, 1, 137, 9, 1, 150), Region.of(-14, -1, 128, 9, 14, 151))
            .leg("cup", d.pen, Region.of(-14, -12, 136, 2, 6, 150 + DZ), Region.of(-18, -12, 140 + DZ, 10, 0, 162 + DZ));
        Design.Line safe = de.line("SAFE", false, false);
        safe.maxGap = 3;
        safe.description = "boards against the wall (2-gaps), 3-wide ruler bridge, the open atlas, 3-wide ruler stair down, 2-gap, 2-wide bookmark ribbon, "
                + "2-gap, stair up to the dictionary, stair down to the drawer, round the saucer, up the spoon, drop in";
        safe.leg("brawl 1", Region.of(0, 3, 41, 9, 3, 53), Region.of(0, 0, 0, 9, 14, 53))
            .leg("brawl 2", Region.of(-14, 1, 95, 9, 1, 106), Region.of(0, -6, 41, 9, 14, 106))
            .leg("page spine", Region.of(-4, 1, 130, -2, 1, 134), Region.of(-14, -1, 95, 9, 14, 135))
            .leg("landing", Region.of(-14, 1, 137, 9, 1, 150), Region.of(-14, -1, 128, 9, 14, 151))
            .leg("cup", d.pen, Region.of(-14, 0, 137, 9, 2, 150), Region.of(-16, -12, 107 + DZ, -8, 1, 123 + DZ), Region.of(-18, -12, 117 + DZ, 16, -5, 162 + DZ));
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

    /** Closed book lying flat (3 thick: covers top and bottom, pages between), top surface at {@code top}. */
    private void flatBook(int x1, int x2, int top, int z1, int z2, String cover) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = top - 2; y <= top; y++) {
            boolean spine = x == x2;
            String s = (y == top || y == top - 2 || spine) ? cover : pages.at(x, y, z, g.seed);
            if (y == top && spine && (z == z1 || z == z2)) s = "gold_block";
            g.set(x, y, z, s);
        }
    }

    /** A chained book over the void: hangs on a chain from a brass arm out of the wall. */
    private void chained(int x1, int x2, int top, int z1, int z2, String cover) {
        flatBook(x1, x2, top, z1, z2, cover);
        int cz = (z1 + z2) / 2;
        for (int y = top + 1; y < ARM_Y; y++) g.set(x1, y, cz, "chain[axis=y]");
        for (int x = x1; x <= 9; x++) g.set(x, ARM_Y, cz, ARM);
        g.set(x1, ARM_Y + 1, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        g.set(x1 + 1, top - 3, cz, "amethyst_cluster[facing=down,waterlogged=false]");
    }

    /** Wall shelf board (2 thick, front trim) with a bracket under it. */
    private void board(int x1, int top, int z1, int z2) {
        for (int x = x1; x <= 9; x++) for (int z = z1; z <= z2; z++) {
            g.set(x, top, z, x == x1 ? "stripped_dark_oak_log[axis=z]" : walnut.at(x, top, z, g.seed));
            g.set(x, top - 1, z, x == x1 ? "stripped_dark_oak_log[axis=z]" : "dark_oak_planks");
        }
        int bz = (z1 + z2) / 2;
        for (int i = 0; i < Math.min(6, 9 - x1); i++)
            for (int x = 9 - i; x <= 9; x++) g.set(x, top - 2 - i, bz, x == 9 - i ? Gen.st("dark_oak", "west", true) : "dark_oak_planks");
    }

    private String cover(int i) { return COVERS[Math.floorMod(i, COVERS.length)]; }

    // ------------------------------------------------------------------ room

    private void room() {
        for (int z = -8; z <= 166 + DZ; z++) for (int y = -24; y <= 40; y++) {
            String s;
            if (y < -14) s = (Math.floorMod(z, 6) == 0) ? "stripped_dark_oak_wood[axis=y]" : "dark_oak_planks";
            else if (y == -14) s = "dark_oak_slab[type=top]";
            else s = Math.floorMod(z, 6) < 2 ? "green_terracotta" : (Math.floorMod(z, 6) == 3 ? "lime_terracotta" : PAGES);
            g.set(10, y, z, s);
        }
        for (int x = -26; x <= 24; x++) for (int y = -24; y <= 40; y++) {
            boolean window = x >= -14 && x <= 14 && y >= -6 && y <= 30;
            boolean mullion = window && (Math.floorMod(x, 7) == 0 || y == 12);
            String s = window ? (mullion ? "white_terracotta" : "light_blue_stained_glass") : (Math.floorMod(x, 6) < 2 ? "green_terracotta" : PAGES);
            if (y < -14 && !window) s = "dark_oak_planks";
            g.set(x, y, 168 + DZ, s);
        }
        for (int y = -12; y <= 33; y++) for (int x = -19; x <= -15; x++) g.set(x, y, 167 + DZ, (x + y) % 3 == 0 ? "red_nether_bricks" : "red_wool");
        for (int y = -12; y <= 33; y++) for (int x = 15; x <= 19; x++) g.set(x, y, 167 + DZ, (x + y) % 3 == 0 ? "red_nether_bricks" : "red_wool");
        for (int x = -21; x <= 21; x++) g.set(x, 34, 167 + DZ, "gold_block");
        for (int x = -2; x <= 10; x++) for (int y = 14; y <= 26; y++) {
            double dd = Math.hypot(x - 4, y - 20);
            if (dd <= 6) g.set(x, y, 208 + DZ, dd < 4.5 ? "glowstone" : "shroomlight");
        }
    }

    /** Upper shelf (scenery) with giant books, above the whole run against the wall. */
    private void upperShelf() {
        g.fill(-3, 25, -6, 9, 26, 150, walnut);
        for (int z = -6; z <= 150; z++) g.set(-3, 26, z, "stripped_dark_oak_log[axis=z]");
        int z = -5, i = 3;
        while (z < 148) {
            int t = 2 + (int) (g.rnd(z, 0, 0, 3) * 3);
            int h = 9 + (int) (g.rnd(z, 0, 0, 4) * 6);
            if (g.rnd(z, 1, 1, 5) < 0.12) { z += 2; continue; }
            book(-2, 7, 27, 26 + h, z, Math.min(149, z + t - 1), cover(i++), true);
            z += t;
        }
        for (int zz = -2; zz <= 148; zz += 15) for (int k = 0; k < 6; k++)
            for (int x = 9 - k; x <= 9; x++) g.set(x, 24 - k, zz, x == 9 - k ? Gen.st("dark_oak", "west", true) : "dark_oak_planks");
    }

    // ------------------------------------------------------------------ start

    private void startAtlas(TgMapData d) {
        g.fill(-20, -1, -6, 9, 0, 12, walnut);
        for (int z = -6; z <= 12; z++) { g.set(-20, 0, z, "stripped_dark_oak_log[axis=z]"); g.set(-20, -1, z, "stripped_dark_oak_log[axis=z]"); }
        for (int x = -20; x <= 9; x++) g.set(x, -1, 12, "stripped_dark_oak_log[axis=x]");
        for (int k = 0; k < 8; k++) for (int x = 9 - k; x <= 9; x++) { g.set(x, -2 - k, 2, x == 9 - k ? Gen.st("dark_oak", "west", true) : "dark_oak_planks"); g.set(x, -2 - k, 10, x == 9 - k ? Gen.st("dark_oak", "west", true) : "dark_oak_planks"); }
        // giant atlas lying flat on the board
        for (int x = -19; x <= 9; x++) for (int z = 0; z <= 12; z++) {
            boolean edge = x == -19 || z == 0 || z == 12;
            g.set(x, 1, z, edge ? "blue_terracotta" : pages.at(x, 1, z, g.seed));
            g.set(x, 2, z, ((x + z) & 1) == 0 ? "blue_terracotta" : "dark_prismarine");
        }
        for (int x = -19; x <= 9; x++) g.set(x, 2, 8, (x & 1) == 0 ? "gold_block" : "black_terracotta");   // start line
        for (int x = -19; x <= 9; x++) for (int y = 3; y <= 5; y++) { g.set(x, y, 9, "glass"); d.startBarrier.add(new Pos(x, y, 9)); }
        // brass bookend on the open side, a standing book behind
        for (int z = -1; z <= 9; z++) for (int y = 1; y <= 9; y++) g.set(-20, y, z, y == 9 ? "cut_copper" : (y % 4 == 0 ? "waxed_copper_block" : "gold_block"));
        book(-19, 9, 1, 16, -3, -1, "red_nether_bricks", true);
        d.startStrip = Region.of(-19, 3, 0, 9, 6, 8);
        for (int row = 0; row < 4; row++)
            for (int k = 0; k < 15; k++) d.startSpawns.add(new Spawn(-18.5 + k * 1.9 + (row % 2) * 0.5, 3, 1.5 + row * 2, 0f));
    }

    // ------------------------------------------------------------------ section 1

    private void section1() {
        // FAST: three chained books, gaps 3 / 3 (+1) / 4 (-1), the last only 3 wide, then 3 onto the open atlas
        chained(-16, -11, 2, 16, 20, "red_terracotta");
        chained(-15, -10, 3, 24, 28, "dark_prismarine");
        chained(-14, -12, 2, 33, 37, "green_terracotta");
        // SAFE: boards against the wall (top 1, feet 2), 2-gaps, a 3-wide ruler bridge; standing books block the
        // front and the back of the boards in turn, so the safe line weaves
        board(1, 1, 15, 23);
        for (int z = 24; z <= 30; z++) for (int x = 3; x <= 5; x++)    // ruler, 3 wide, numbered
            g.set(x, 1, z, x == 3 && z % 2 == 0 ? "black_terracotta" : "birch_planks");
        for (int z = 24; z <= 30; z++) for (int x = 3; x <= 5; x++) g.set(x, 0, z, "stripped_birch_wood[axis=y]");
        board(1, 1, 31, 34);
        board(1, 1, 37, 38);
        slalom(1, 5, 1, 17, 18, 0);
        slalom(5, 9, 1, 21, 21, 1);
        slalom(1, 4, 1, 33, 33, 2);
        g.set(8, 2, 15, "decorated_pot[facing=south,waterlogged=false,cracked=false]");
        g.set(4, 2, 27, "iron_bars");                                  // staple: pass left or right of it
        g.set(9, 2, 31, "candle[candles=4,lit=true,waterlogged=false]");
        g.set(9, 2, 38, "lantern[hanging=false,waterlogged=false]");
    }

    /** A standing book across part of a board (blocks that side of the lane). */
    private void slalom(int x1, int x2, int top, int z1, int z2, int c) {
        book(x1, x2, top + 1, top + 4 + (c % 2), z1, z2, cover(c + 4), true);
    }

    // ------------------------------------------------------------------ brawl 1: the open atlas

    private void brawlAtlas() {
        board(0, 0, 41, 54);
        // open book: two page halves over the board and out over the void (x -16..9), spine valley at x -4
        for (int x = -16; x <= 9; x++) for (int z = 41; z <= 53; z++) {
            boolean coverEdge = z == 41 || z == 53 || x == -16 || x == 9;
            g.set(x, 1, z, coverEdge ? "red_nether_bricks" : "red_terracotta");
            g.set(x, 2, z, coverEdge ? "red_nether_bricks" : (x == -4 ? "gold_block" : pages.at(x, 2, z, g.seed)));
        }
        // text lines on the pages
        for (int z = 43; z <= 51; z += 2) for (int x = -14; x <= 7; x++) if (x != -4 && x != -3 && x != -5 && g.rnd(x, 2, z, 77) < 0.7) g.set(x, 2, z, "black_terracotta");
        // a bookmark ribbon and a candle stub on the pages (and no free sprint across)
        g.set(-7, 3, 48, "red_wool"); g.set(-7, 3, 49, "red_wool");
        g.set(3, 3, 46, "candle[candles=3,lit=true,waterlogged=false]");
        g.fill(-11, 3, 45, -10, 3, 45, "white_terracotta");
        book(1, 9, 3, 7, 47, 48, "cyan_terracotta", true);         // a standing book on the open pages: go round it
        for (int y = 16; y <= 24; y++) g.set(-3, y, 44, "chain[axis=y]");   // the pocket watch hangs from the upper shelf
        g.set(-3, 15, 44, "lightning_rod[facing=down,powered=false,waterlogged=false]");
    }

    // ------------------------------------------------------------------ section 2

    private void section2() {
        // FAST: the pencil x -12..-10 (top 2) from the atlas to the dictionary stack, raised ferrule and a gold band
        for (int z = 54; z <= 68; z++) {
            String body = z <= 55 ? "pink_terracotta" : z <= 57 ? "gold_block" : z >= 66 ? "stripped_birch_log[axis=z]" : "yellow_terracotta";
            if (z == 68) body = "black_terracotta";
            g.set(-11, 1, z, body);
            g.set(-11, 2, z, body);
            boolean tip = z >= 66;
            g.set(-12, 1, z, tip ? "birch_stairs[facing=east,half=top,shape=straight,waterlogged=false]" : "orange_terracotta");
            g.set(-10, 1, z, tip ? "birch_stairs[facing=west,half=top,shape=straight,waterlogged=false]" : "orange_terracotta");
            g.set(-12, 2, z, tip ? "birch_slab[type=bottom,waterlogged=false]" : Gen.st(z <= 55 ? "granite" : "sandstone", "east", false));
            g.set(-10, 2, z, tip ? "birch_slab[type=bottom,waterlogged=false]" : Gen.st(z <= 55 ? "granite" : "sandstone", "west", false));
        }
        for (int x = -12; x <= -10; x++) { g.set(x, 3, 56, "cut_copper_slab[type=bottom,waterlogged=false]"); g.set(x, 3, 61, "cut_copper_slab[type=bottom,waterlogged=false]"); }
        // dictionary stack (top 3) on a book pillar from the floor far below
        book(-11, 0, -20, -10, 69, 76, "blue_terracotta", false);
        book(-10, 0, -9, -1, 70, 76, "red_terracotta", false);
        book(-11, 0, 0, 2, 69, 76, "green_terracotta", false);
        for (int z = 69; z <= 76; z++) g.set(-11, 1, z, "gold_block");
        // two chained notebooks, gaps 4 (-1 each)
        chained(-8, -6, 2, 81, 83, "red_terracotta");
        chained(-6, -4, 1, 88, 90, "cyan_terracotta");
        // SAFE: ruler stair down from the atlas corner, low boards (top -4) with weaving books, a 2-gap,
        // the 2-wide bookmark ribbon, a 2-gap, stair up to the dictionary
        board(1, -4, 59, 66);
        Stairs.flight(g, "ruler stair down", "birch", null, 5, 59, -4, 0, -1, 6, 3);
        board(1, -4, 69, 75);
        for (int z = 76; z <= 82; z++) for (int x = 3; x <= 4; x++) {
            g.set(x, -4, z, (z & 1) == 0 ? "red_wool" : "red_nether_bricks");          // bookmark ribbon, 2 wide
            g.set(x, -5, z, "red_nether_bricks");
        }
        g.set(3, -3, 79, "red_nether_brick_slab[type=bottom,waterlogged=false]");     // a fold in the ribbon
        g.set(4, -3, 79, "red_nether_brick_slab[type=bottom,waterlogged=false]");
        board(1, -4, 83, 86);
        board(1, -4, 89, 91);
        Stairs.flight(g, "stair up to the dictionary", "spruce", null, 2, 91, -4, 0, 1, 4, 3);
        slalom(4, 9, -4, 61, 61, 3);
        slalom(1, 5, -4, 64, 64, 4);
        slalom(5, 9, -4, 71, 71, 5);
        slalom(1, 2, -4, 74, 74, 6);
        slalom(5, 9, -4, 85, 85, 7);
        g.set(2, -3, 66, "decorated_pot[facing=south,waterlogged=false,cracked=false]");
        g.set(8, -3, 75, "candle[candles=2,lit=true,waterlogged=false]");
    }

    // ------------------------------------------------------------------ brawl 2: the dictionary on a book tower

    private void brawlDictionary() {
        // the tower: giant books stacked from the floor far below
        book(-12, 7, -40, -26, 95, 106, "black_terracotta", false);
        book(-11, 8, -25, -12, 96, 105, "blue_terracotta", false);
        book(-13, 6, -11, -2, 95, 106, "red_nether_bricks", false);
        // the dictionary on top (top 0, feet 1): 24 x 12
        book(-14, 9, -1, 0, 95, 106, "dark_prismarine", false);
        for (int x = -14; x <= 9; x++) { g.set(x, 0, 95, "gold_block"); g.set(x, 0, 106, "gold_block"); }
        for (int z = 97; z <= 104; z += 7) for (int x = -12; x <= 7; x += 4) g.set(x, 0, z, "black_terracotta");
        g.set(-3, 1, 100, "white_terracotta");          // a sugar cube and a candle on the cover
        g.set(4, 1, 102, "candle[candles=3,lit=true,waterlogged=false]");
        g.set(-9, 1, 103, "white_terracotta");
    }

    // ------------------------------------------------------------------ page turner: run the spine while the pages flip

    /** Spine walkway of the giant open book (x -4..-2, top 0, feet 1), z PAGE_Z1..PAGE_Z2. */
    static final int PAGE_Z1 = 109, PAGE_Z2 = 134;
    /** Page hinges along the spine (z of each page centre). */
    static final int[] PAGE_ZS = {110, 114, 118, 124, 128, 132};
    /** The binding is torn: a 2-gap across the whole book (two halves of the run, each under 15 to sprint). */
    static final int TEAR_Z1 = 121, TEAR_Z2 = 122;
    static final int PAGE_PERIOD = 64;

    private void pageTurner() {
        // the book tower from the floor far below
        book(-15, 9, -40, -26, PAGE_Z1, PAGE_Z2, "brown_terracotta", false);
        book(-14, 8, -25, -12, PAGE_Z1 + 1, PAGE_Z2 - 1, "green_terracotta", false);
        book(-15, 9, -11, -3, PAGE_Z1, TEAR_Z1 - 1, "blue_terracotta", false);
        book(-15, 9, -11, -3, TEAR_Z2 + 1, PAGE_Z2, "purple_terracotta", false);
        // the open giant book: leather covers (y -2), page blocks either side of the spine (top -1), the spine (top 0)
        for (int x = -14; x <= 8; x++) for (int z = PAGE_Z1; z <= PAGE_Z2; z++) {
            if (z >= TEAR_Z1 && z <= TEAR_Z2) continue;
            boolean spine = x >= -4 && x <= -2;
            boolean rim = x == -14 || x == 8 || z == PAGE_Z1 || z == PAGE_Z2 || z == TEAR_Z1 - 1 || z == TEAR_Z2 + 1;
            g.set(x, -2, z, "red_nether_bricks");
            if (spine) {
                g.set(x, -1, z, "red_terracotta");
                g.set(x, 0, z, x == -3 ? ((z & 3) == 0 ? "gold_block" : "red_terracotta") : "red_nether_bricks");
            } else if (rim) {
                g.set(x, -1, z, "red_nether_bricks");
            } else {
                g.set(x, -1, z, (z % 4 == 2 && Math.abs(x + 3) > 2 && g.rnd(x, -1, z, 51) < 0.7) ? "black_terracotta" : pages.at(x, -1, z, g.seed));
            }
        }
        // a bookmark ribbon hanging off the end of the spine, the hinge rings under each page
        for (int y = -12; y <= -3; y++) g.set(-3, y, PAGE_Z2 + 1, "red_wool");
        for (int pz : PAGE_ZS) { g.set(-4, 0, pz, "waxed_cut_copper"); g.set(-2, 0, pz, "waxed_cut_copper"); }
    }

    // ------------------------------------------------------------------ the falling-book landing

    static final int LAND_Z1 = 137, LAND_Z2 = 150;
    /** Falling books {x, z} (centres) over the landing; the first rows (z 137-139) stay clear for the checkpoint. */
    static final double[][] DROPS = {{-10.5, 142.5}, {-4.5, 144.5}, {1.5, 141.5}, {6.5, 144.5}, {-8.5, 148.5}, {-1.5, 148.5}, {4.5, 148.5}};

    private void fallingBookLanding() {
        // a stack of giant books on a tower, top 0 (feet 1)
        book(-14, 9, -40, -26, LAND_Z1 + 1, LAND_Z2 - 1, "black_terracotta", false);
        book(-15, 9, -25, -3, LAND_Z1, LAND_Z2, "cyan_terracotta", false);
        flatBook(-15, 9, -1, LAND_Z1, LAND_Z2, "crimson_planks");
        for (int x = -14; x <= 9; x++) for (int z = LAND_Z1; z <= LAND_Z2; z++)
            g.set(x, 0, z, (x == -14 || z == LAND_Z1 || z == LAND_Z2) ? "dark_prismarine" : (((x + z) % 7 == 0) ? "gold_block" : "warped_planks"));
        // shadows: a dark square under each falling book shows where it lands
        for (double[] dz : DROPS)
            for (int x = (int) Math.floor(dz[0] - 1); x <= (int) Math.floor(dz[0] + 1); x++)
                for (int z = (int) Math.floor(dz[1] - 1); z <= (int) Math.floor(dz[1] + 1); z++) g.set(x, 0, z, "black_terracotta");
        // the chains the books hang from, up to the upper shelf
        for (double[] dz : DROPS) for (int y = 14; y <= 24; y++) g.set((int) Math.floor(dz[0]), y, (int) Math.floor(dz[1]), "chain[axis=y]");
        g.set(-3, 1, LAND_Z1 + 2, "candle[candles=4,lit=true,waterlogged=false]");
        g.set(8, 1, LAND_Z1 + 1, "lantern[hanging=false,waterlogged=false]");
    }

    /** Pole of the desk fan in front of the finish row (the blades are a mover). */
    private void deskFanMount() {
        int z = 156 + DZ;
        for (int y = -11; y <= -9; y++) g.set(-5, y, z, "waxed_copper_block");
    }

    // ------------------------------------------------------------------ moving parts and checkpoints

    private static MapFeatures FEATURES;

    public static synchronized MapFeatures feats() {
        if (FEATURES != null) return FEATURES;
        MapFeatures f = new MapFeatures();
        // the pocket watch swings across the open atlas (brawl 1)
        f.movers.add(MapFeatures.Mover.pendulum("pocket watch", -2.5, 15.5, 44.5, 'z', 55, 70, 0, List.of(
                new MapFeatures.Box(-0.1, -9.5, -0.1, 0.1, 0, 0.1, "chain[axis=y]", false),
                new MapFeatures.Box(-1.4, -12.0, -0.4, 1.4, -9.4, 0.4, "gold_block", true),
                new MapFeatures.Box(-1.05, -11.65, -0.5, 1.05, -9.75, 0.5, "white_concrete", true),
                new MapFeatures.Box(-0.1, -10.8, -0.55, 0.1, -9.9, 0.55, "black_concrete", false))));
        // books shoved out of the wall across the low boards (safe line, section 2): hug the outer edge or time it
        int k = 0;
        for (double z : new double[]{62.5, 72.5}) {
            k++;
            f.movers.add(MapFeatures.Mover.slider("sliding book", 10, -3, z, -7, 0, 0, 40, 10, 20, k * 31, List.of(
                    new MapFeatures.Box(0, 0, -0.5, 5, 2.6, 0.5, k == 1 ? "red_terracotta" : "blue_terracotta", true),
                    new MapFeatures.Box(0, 0.4, -0.55, 0.4, 2.2, 0.55, "gold_block", true))));
        }
        // the flipping pages: each page is hinged on the spine and flops from one half of the book to the other;
        // the hinge is under the walkway, so the spine is only safe while the page is lying down
        for (int i = 0; i < PAGE_ZS.length; i++) {
            f.movers.add(MapFeatures.Mover.pendulum("flipping page", -2.5, 0.2, PAGE_ZS[i] + 0.5, 'z', 86, PAGE_PERIOD, i * 9, List.of(
                    new MapFeatures.Box(-0.15, 1.2, -1.4, 0.15, 8.8, 1.4, "smooth_quartz", true),
                    new MapFeatures.Box(-0.18, 3.0, -1.0, 0.18, 3.3, 1.0, "black_concrete", false),
                    new MapFeatures.Box(-0.18, 5.0, -1.0, 0.18, 5.3, 1.0, "black_concrete", false),
                    new MapFeatures.Box(-0.18, 7.0, -1.0, 0.18, 7.3, 1.0, "black_concrete", false))).withSound("item.book.page_turn"));
        }
        // falling books on chains over the landing: they slam down onto their shadow and winch back up
        String[] cov = {"red_terracotta", "green_terracotta", "blue_terracotta", "brown_terracotta", "cyan_terracotta", "black_terracotta", "crimson_planks"};
        for (int i = 0; i < DROPS.length; i++) {
            f.movers.add(MapFeatures.Mover.slider("falling book", DROPS[i][0], 10, DROPS[i][1], 0, -9, 0, 30, 8, 16, (i * 23) % 62, List.of(
                    new MapFeatures.Box(-1.5, 0, -1.5, 1.5, 1.6, 1.5, cov[i], true),
                    new MapFeatures.Box(-1.52, 0.5, -1.25, 1.52, 1.1, 1.25, "smooth_sandstone", true),
                    new MapFeatures.Box(-0.08, 1.6, -0.08, 0.08, 4.0, 0.08, "chain[axis=y]", false))).withSound("block.chiseled_bookshelf.pickup"));
        }
        // the desk fan in front of the finish row
        double fz = 156 + DZ - 0.5;
        f.movers.add(MapFeatures.Mover.rotor("desk fan", -4.5, -8.5, fz, 'z', 0, 36, 0, List.of(
                new MapFeatures.Box(-0.6, -0.6, -0.25, 0.6, 0.6, 0.25, "waxed_copper_block", false),
                new MapFeatures.Box(-0.3, 0.6, -0.15, 0.3, 3.3, 0.15, "light_blue_concrete", true),
                new MapFeatures.Box(-0.3, -3.3, -0.15, 0.3, -0.6, 0.15, "light_blue_concrete", true),
                new MapFeatures.Box(0.6, -0.3, -0.15, 3.3, 0.3, 0.15, "light_blue_concrete", true),
                new MapFeatures.Box(-3.3, -0.3, -0.15, -0.6, 0.3, 0.15, "light_blue_concrete", true))));
        // owner: only the pages send you back; everything else knocks you about (and maybe off the map)
        f.pushers.add("pocket watch");
        f.pushers.add("sliding book");
        f.pushers.add("falling book");
        f.pushers.add("desk fan");
        // checkpoints at the brawl spots
        f.checkpoints.add(new MapFeatures.Checkpoint(1, "The Open Atlas", Region.of(-16, 3, 41, 9, 6, 42), spawns(-14, 7, 3, 42.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(2, "The Dictionary", Region.of(-14, 1, 95, 9, 4, 96), spawns(-12, 7, 1, 96.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(3, "The Landing", Region.of(-14, 1, LAND_Z1, 9, 4, LAND_Z1 + 1), spawns(-12, 7, 1, LAND_Z1 + 0.5), List.of()));
        FEATURES = f;
        return f;
    }

    private static List<Spawn> spawns(int x1, int x2, double y, double z) {
        List<Spawn> s = new ArrayList<>();
        for (int x = x1; x <= x2; x += 2) s.add(new Spawn(x + 0.5, y, z, 0f));
        return s;
    }

    // ------------------------------------------------------------------ desk, drawer, cup

    private void deskAndCup(TgMapData d) {
        g.fill(-18, -14, 168, 16, -12, 206, desk);
        for (int x = -18; x <= 16; x++) { g.set(x, -12, 168, "stripped_dark_oak_log[axis=x]"); g.set(x, -13, 168, "stripped_dark_oak_log[axis=x]"); }
        for (int[] c : new int[][]{{-17, 169}, {14, 169}, {-17, 203}, {14, 203}})
            g.fill(c[0], -40, c[1], c[0] + 2, -15, c[1] + 2, "dark_oak_log[axis=y]");
        // the pulled-out drawer (the safe stair lands on it)
        g.fill(-16, -14, 160, -10, -12, 167, "spruce_planks");
        for (int x = -16; x <= -10; x++) g.set(x, -12, 160, "stripped_spruce_log[axis=x]");
        g.set(-13, -13, 159, "gold_block");
        Stairs.flight(g, "drawer stair", "spruce", null, -14, 162, -12, 0, -1, 12, 3);
        // saucer and cup (rim y -6), floor of tea
        double cx = 0.5, cz = 180.5;
        g.cyl(cx, cz, 9.5, -11, -11, new Pal("white_terracotta", 80, "light_blue_terracotta", 20));
        g.ring(cx, cz, 9.5, 1, -10, -10, new Pal("white_terracotta", 1));
        g.ring(cx, cz, 6.6, 1.7, -10, -6, new Pal("white_terracotta", 70, "calcite", 30));
        g.ring(cx, cz, 6.6, 1.7, -8, -8, new Pal("light_blue_terracotta", 1));
        g.cyl(cx, cz, 4.9, -11, -11, new Pal("brown_terracotta", 60, "orange_terracotta", 40));
        for (int y = -10; y <= -7; y++) g.set(8, y, 180, "white_terracotta");
        g.set(7, -7, 180, "white_terracotta");
        g.set(7, -10, 180, "white_terracotta");
        // spoon ramp on the far side (safe line): from the desk up to the rim
        Stairs.flight(g, "spoon ramp", "polished_diorite", null, -1, 192, -12, 0, -1, 6, 3);
        g.ellipsoid(0.5, -11.5, 195.5, 2.4, 0.6, 2.8, new Pal("polished_diorite", 1));
        // desk clutter: inkwell, quill, letters, candle, globe, sugar cubes
        g.cyl(-12.5, 176.5, 2.6, -11, -8, new Pal("black_stained_glass", 1));
        g.cyl(-12.5, 176.5, 1.4, -7, -7, new Pal("black_terracotta", 1));
        g.line(-12, -6, 176, -7, 6, 182, "white_wool");
        g.line(-11, -6, 176, -6, 6, 182, "white_wool");
        book(9, 15, -11, -9, 180, 188, "white_terracotta", false);
        book(10, 14, -8, -8, 181, 186, "white_terracotta", false);
        g.cyl(-12.5, 196.5, 1.2, -11, -9, new Pal("spruce_planks", 1));
        g.ellipsoid(-12.5, -3.5, 196.5, 4.5, 4.5, 4.5, new Pal("blue_terracotta", 50, "green_terracotta", 30, "lime_terracotta", 20));
        g.fill(-13, -9, 196, -12, -8, 196, "gold_block");
        g.fill(-12, -11, 173, -11, -10, 174, "bone_block[axis=y]");
        g.fill(-9, -11, 187, -8, -11, 188, "bone_block[axis=y]");
        // feedback 3 (finish row at the back of the desk): sugar cubes and a letter stack on the long desk run
        g.fill(-11, -11, 182, -9, -11, 182, "bone_block[axis=y]");
        g.fill(-11, -11, 172, -9, -11, 172, "bone_block[axis=y]");

        g.fill(-12, -11, 194, -9, -10, 195, "white_terracotta");
        g.fill(4, -11, 198, 6, -11, 199, "bone_block[axis=y]");
    }

    // ------------------------------------------------------------------ section 3: coins over the void (fast)

    private void section3() {
        coin(-2.5, 154.5, -2);
        coin(0.5, 159.5, -4);
        // a letter opener (2 wide, top -5) leaning from the coins onto the teacup rim
        for (int z = 163; z <= 173; z++) for (int x = 0; x <= 1; x++) {
            g.set(x, -5, z, z <= 165 ? (z == 163 ? "gold_block" : "polished_blackstone") : "iron_block");
            if (z <= 165) g.set(x, -6, z, "polished_blackstone");
        }
        g.set(0, -4, 164, "polished_blackstone_slab[type=bottom,waterlogged=false]");     // the grip guard: hop it
        g.set(1, -4, 164, "polished_blackstone_slab[type=bottom,waterlogged=false]");
    }

    /** A stack of giant coins on a chain-hung pole of stacked coins from the desk/floor (gold, 3 x 3 minus corners). */
    private void coin(double cx, double cz, int top) {
        g.cyl(cx, cz, 1.3, top - 1, top, new Pal("gold_block", 70, "raw_gold_block", 30));
        g.cyl(cx, cz, 1.3, top - 16, top - 2, new Pal("gold_block", 40, "raw_gold_block", 30, "yellow_terracotta", 30));
    }

    // ------------------------------------------------------------------ lamp (landmark) and room scenery

    private void lamp(Design de) {
        g.cyl(12.5, 191.5, 3.2, -11, -10, new Pal("gold_block", 60, "cut_copper", 40));
        for (int y = -9; y <= 24; y++) g.set(12, y, 191, "cut_copper");
        g.line(12, 24, 191, 3, 30, 184, "cut_copper");
        g.line(12, 25, 191, 3, 31, 184, "waxed_cut_copper");
        for (int y = 23; y <= 30; y++) {
            double r = 1.5 + (30 - y) * 0.75;
            g.ring(1.5, 181.5, r, 1.0, y, y, new Pal("green_terracotta", 70, "gold_block", 30));
        }
        g.cyl(1.5, 181.5, 2.2, 24, 24, new Pal("ochre_froglight", 1));
        g.cyl(1.5, 181.5, 1.2, 23, 23, new Pal("glowstone", 1));
        for (int y = 23; y <= 30; y++)            // the near (start-facing) side of the shade
            for (int x = -3; x <= 5; x += 2)
                for (int z = 169; z <= 181; z++) if (!g.air(x, y, z)) { de.landmark.add(new Pos(x, y, z)); break; }
        de.landmark.add(new Pos(12, 24, 191));
    }

    private void roomScenery() {
        // giant armchair out in the room on the open side
        g.fill(-52, -30, 40, -38, -22, 56, "red_wool");
        g.fill(-52, -21, 40, -50, -6, 56, "red_wool");
        g.fill(-52, -21, 40, -38, -16, 41, "red_nether_bricks");
        g.fill(-52, -21, 55, -38, -16, 56, "red_nether_bricks");
        for (int[] c : new int[][]{{-52, 40}, {-39, 40}, {-52, 55}, {-39, 55}}) g.fill(c[0], -40, c[1], c[0] + 1, -31, c[1] + 1, "dark_oak_log[axis=y]");
        // standing floor lamp in the room
        g.fill(-34, -40, 92, -34, 18, 92, "cut_copper");
        g.ring(-33.5, 92.5, 5, 1, 19, 26, new Pal("lime_terracotta", 1));
        g.cyl(-33.5, 92.5, 2, 19, 19, new Pal("shroomlight", 1));
        // a picture frame on the back wall above the upper shelf
        for (int z = 6; z <= 30; z++) for (int y = 30; y <= 40; y++) {
            boolean edge = z == 6 || z == 30 || y == 30 || y == 40;
            g.set(9, y, z, edge ? "gold_block" : (y < 34 ? "green_terracotta" : (y < 37 ? "light_blue_terracotta" : "white_terracotta")));
        }
    }
}
