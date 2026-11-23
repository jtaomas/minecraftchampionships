package com.mcm.games.battlebox.arena;

/**
 * Greenhouse Conservatory: a white iron-and-glass hall on a red-brick knee wall with a glass roof and a raised
 * lantern bay over the centre.
 *
 * Archetype: <b>central hall</b>. The centre is inside a glass Palm Hall (x, z -8..8) that you enter through one
 * door on each of its four sides; everything else is built around getting into it. Layout per half (side A shown,
 * side B is the 180-degree copy):
 * <ul>
 *   <li><b>Potting House</b> (spawn building, x -6..6, z -17..-12): west and east ground doors, plus an indoor
 *       stair up to a roof balcony; from the balcony a footbridge runs to the Palm Tower.</li>
 *   <li><b>Left flank: hedge maze</b> - a serpentine of 3-high hedges with 2-wide paths from the back yard down to
 *       the hall's side door (and on into the enemy's lawn).</li>
 *   <li><b>Right flank: open lawn</b> with almost no cover, overlooked by the <b>Palm Tower</b> (x 11..15): glass
 *       mid-floor reached by the footbridge, ladder to an exposed deck at y 8 - a risky shooting perch.</li>
 *   <li><b>Front yard</b> between the Potting House and the hall's front door: low cold frames only.</li>
 * </ul>
 */
public final class GreenhouseTheme extends ArenaTheme {
    @Override public String id() { return "greenhouse"; }
    @Override public String displayName() { return "Greenhouse Conservatory"; }
    @Override public String tagline() { return "Glass, iron and a lot of flower pots"; }

    @Override
    public String layoutNotes() {
        return "Central hall: the centre is inside a glass Palm Hall entered by one door on each side | Potting House spawn: "
                + "2 ground doors + indoor stair to a roof balcony | hedge maze on the left flank (serpentine) | open lawn on the "
                + "right flank under a tall Palm Tower (ladder to an exposed deck 8 up, footbridge from the spawn roof)";
    }

    private static final Pal BRICK = Pal.of("bricks", 8, "granite", 1, "mud_bricks", 1);
    private static final Pal STONEB = Pal.of("stone_bricks", 5, "mossy_stone_bricks", 3, "cracked_stone_bricks", 1);
    private static final Pal LEAVES = Pal.of("azalea_leaves[persistent=true]", 5, "flowering_azalea_leaves[persistent=true]", 3,
            "oak_leaves[persistent=true]", 2);
    private static final Pal FLOWERS = Pal.of("pink_tulip", 2, "red_tulip", 2, "allium", 2, "oxeye_daisy", 2, "cornflower", 2,
            "short_grass", 3, "fern", 2, "azure_bluet", 1, "lily_of_the_valley", 1, "dandelion", 1);
    private static final Pal SOIL = Pal.of("grass_block", 5, "moss_block", 3, "rooted_dirt", 1);

    @Override
    protected String floorAt(int x, int z) {
        // side A coordinates (side B is the 180-degree copy)
        int ax = z <= 0 ? x : -x, az = z <= 0 ? z : -z;
        if (Math.abs(x) > 8 || Math.abs(z) > 8) {
            if (ax <= -9 && az <= -1 && az >= -12) return SOIL.at(x, 0, z, seed + 1);                               // hedge maze
            if (ax >= 9 && az <= -1 && az >= -9) return rnd(x, 0, z, 77) < 0.12 ? "moss_block" : "grass_block";   // open lawn
        }
        int m = Math.max(Math.abs(x), Math.abs(z));
        if (m == R && !(Math.abs(x) <= 3)) return SOIL.at(x, 0, z, seed);
        if (m == R - 1) return "polished_andesite";
        if (Math.abs(x) <= 1 && Math.abs(z) >= 9 && Math.abs(z) <= 11) return Math.abs(z) % 2 == 0 ? "polished_diorite" : "calcite";
        return (((x + 64) >> 1) + ((z + 64) >> 1) & 1) == 0 ? "birch_planks" : "calcite";
    }

    @Override protected String subFloorAt(int x, int z) { return "dirt"; }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 2) return y == 2 && Math.abs(u) % 4 == 0 ? "polished_granite" : BRICK.at(u, y, f.ordinal(), seed);
        if (y == 3) return "white_concrete";
        if (y == H) return (Math.abs(u) & 1) == 0 ? "white_concrete" : "lime_concrete";
        if (y == H - 1) return "white_concrete";
        if (Math.abs(u) % 6 == 0 || y == 8) return "white_concrete";
        return sky(f, u, y, 3);
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        if (y <= 3) return BRICK.at(u, y, f.ordinal() + 7, seed);
        if (Math.abs(u) % 6 == 0 || y == 8 || y == H) return "white_concrete";
        return "light_blue_stained_glass";
    }

    @Override
    protected String ceilingAt(int x, int z) {
        if (Math.abs(x) < 6 && Math.abs(z) < 6) return null;   // open to the lantern bay
        if (Math.abs(x) == W || Math.abs(z) == W) return "white_concrete";
        return (Math.abs(x) % 4 == 0 || Math.abs(z) % 4 == 0) ? "white_concrete" : "glass";
    }

    @Override
    protected String[] roomPalette() {
        return new String[]{"spruce_planks", "bricks", "white_concrete", "spruce", "glowstone"};
    }

    @Override protected String daisStairs() { return "smooth_quartz"; }

    @Override
    protected void roof() {
        int top = CEIL + 3;
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                boolean ring = Math.abs(x) == 6 || Math.abs(z) == 6;
                if (ring) {
                    for (int y = CEIL; y < top; y++) {
                        boolean post = (Math.abs(x) % 3 == 0 && Math.abs(z) % 3 == 0) || y == CEIL;
                        set(x, y, z, post ? "white_concrete" : "glass_pane");
                    }
                }
                set(x, top, z, ring || x % 3 == 0 || z % 3 == 0 ? "white_concrete" : "glass");
            }
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) set(sx * 6, top + 1, sz * 6, "end_rod[facing=up]");
        set(0, top + 1, 0, "lightning_rod[facing=up]");
        for (int y = top - 1; y >= top - 5; y--) set(0, y, 0, "chain[axis=y]");
        set(0, top - 6, 0, "sea_lantern");
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) set(d[0], top - 6, d[1], "lantern[hanging=true]");
    }

    // ------------------------------------------------------------------ layout

    private final Mat wallM = (x, y, z) -> y <= 2 ? BRICK.at(x, y, z, seed) : "white_concrete";
    private final Mat post = m("quartz_pillar[axis=y]");

    @Override
    protected void layout() {
        pottingHouse();
        palmHall();
        palmTower();
        hedgeMaze();
        lawn();
        frontYard();
        for (int[] h : new int[][]{{-15, -14}, {-4, -10}, {15, -4}, {-13, -5}})
            symHangingLantern(h[0], CEIL - 1, h[1], 3, "lantern");

        waypoint("back yard", -12, 1, -13);
        waypoint("maze bend 1", -13, 1, -10);
        waypoint("maze bend 2", -13, 1, -7);
        waypoint("maze bend 3", -13, 1, -4);
        waypoint("hall side door", -9, 1, -2);
        waypoint("potting house roof", -3, 5, -15);
        waypoint("footbridge", 9, 5, -12);
        waypoint("tower mid floor", 13, 5, -11);
        waypoint("tower deck", 12, 9, -11);
        waypoint("open lawn", 13, 1, -4);
        waypoint("front yard", 0, 1, -10);
        waypoint("inside the hall", -6, 1, -3);
        waypoint("enemy maze via the lawn", 12, 1, 2);
    }

    /** Spawn building: x -6..6, z -17..-12, roof balcony at y 4 (stand 5). */
    private void pottingHouse() {
        for (int z = -17; z <= -12; z++)
            for (int y = 1; y <= 3; y++) { symM(-6, y, z, wallM); symM(6, y, z, wallM); }
        for (int x = -6; x <= 6; x++)
            for (int y = 1; y <= 3; y++) symM(x, y, -12, wallM);
        for (int y = 1; y <= 3; y++)
            for (int[] p : new int[][]{{-6, -12}, {6, -12}, {-6, -17}, {6, -17}, {-2, -12}, {2, -12}}) symM(p[0], y, p[1], post);
        for (int x = -6; x <= 6; x++)
            for (int z = -17; z <= -12; z++) {
                boolean edge = Math.abs(x) == 6 || z == -12;
                sym(x, 4, z, edge ? "white_concrete" : ((x + z) & 1) == 0 ? "smooth_quartz" : "birch_planks");
            }
        // doors: west (to the maze side) and east (to the lawn side)
        symClear(-6, 1, -15, -6, 2, -14);
        symClear(6, 1, -16, 6, 2, -15);
        for (int x : new int[]{-4, -3, 3, 4}) sym(x, 2, -12, "lime_glazed_terracotta");   // solid tiles, not windows: keeps the gate hidden
        // indoor stair along the front wall, climbing east to the roof balcony
        staircase("Potting House stairs", 1, -13, 0, E, 4, 1, "birch", "smooth_quartz");
        // balcony parapet: fences on the sides, flower boxes on the front with a gap to drop into the front yard
        for (int z = -17; z <= -12; z++) { sym(-6, 5, z, "birch_fence"); if (z < -14 || z > -11) sym(6, 5, z, "birch_fence"); }
        for (int x = -6; x <= 6; x++) if (Math.abs(x) >= 3) sym(x, 5, -12, (x & 1) == 0 ? "birch_fence" : "potted_red_tulip");
        sym(-5, 5, -17, "potted_azalea_bush"); sym(0, 5, -17, "composter[level=4]"); sym(-2, 5, -17, "potted_fern");
        sym(-6, 6, -12, "lantern"); sym(6, 6, -17, "lantern");
        for (int x = -1; x <= 1; x++)
            for (int z = -17; z <= -14; z++) { set(x, 0, z, teamA + "_concrete"); set(-x, 0, -z, teamB + "_concrete"); }
        set(-1, 3, -13, teamA + "_wall_banner[facing=south]");
        set(1, 3, 13, teamB + "_wall_banner[facing=north]");
        // potting benches against the back wall corners (doors stay free)
        sym(-5, 1, -17, "barrel[facing=up]"); sym(-5, 2, -17, "potted_red_tulip");
        sym(-4, 1, -17, "spruce_slab[type=top]"); sym(-4, 2, -17, "potted_cactus");
        sym(5, 1, -17, "spruce_slab[type=top]"); sym(5, 2, -17, "potted_fern");
        sym(4, 1, -17, "composter[level=5]");
        sym(-5, 1, -13, "water_cauldron[level=3]");
        sym(-4, 3, -15, "lantern[hanging=true]"); sym(4, 3, -15, "lantern[hanging=true]");

        exitDoor("west door", -6, 1, -15, -6, 3, -14);
        exitDoor("east door", 6, 1, -16, 6, 3, -15);
        exitDoor("stair to the roof balcony", 1, 1, -13, 5, 6, -13);
    }

    /** The glass Palm Hall around the centre: walls at |x| = 8 / |z| = 8, roof at y 7, one door per side. */
    private void palmHall() {
        int w = 8;
        Mat hallWall = (x, y, z) -> {
            boolean corner = Math.abs(x) == w && Math.abs(z) == w;
            int u = Math.abs(z) == w ? x : z;
            if (corner || Math.floorMod(u, 4) == 0) return y == 6 ? "white_concrete" : "quartz_pillar[axis=y]";
            if (y <= 2) return BRICK.at(x, y, z, seed + 5);
            if (y == 6) return "white_concrete";
            return "white_stained_glass_pane";
        };
        for (int x = -w; x <= w; x++) for (int y = 1; y <= 6; y++) symM(x, y, -w, hallWall);
        for (int z = -w; z <= 0; z++) for (int y = 1; y <= 6; y++) { symM(-w, y, z, hallWall); symM(w, y, z, hallWall); }
        // living-wall panel in the middle of the front wall (blocks the long axis), ivy on both faces
        for (int x = -3; x <= 3; x++)
            for (int y = 1; y <= 6; y++) sym(x, y, -w, y == 6 ? "white_concrete" : Math.abs(x) == 3 ? "quartz_pillar[axis=y]" : STONEB.at(x, y, -w, seed));
        for (int x = -2; x <= 2; x++) for (int y = 2; y <= 5; y++) if (rnd(x, y, -w, 61) < 0.6) sym(x, y, -w - 1, "vine[south=true]");
        for (int x = -2; x <= 2; x++) for (int y = 2; y <= 5; y++) if (rnd(x, y, -w, 62) < 0.5) sym(x, y, -w + 1, "vine[north=true]");
        // doors: front (z = -8, x -5..-4) and left side (x = -8, z -3..-2); the enemy copies give the other two sides
        symClear(-5, 1, -w, -4, 2, -w);
        symClear(-w, 1, -3, -w, 2, -2);
        // glass roof with white ribs
        for (int x = -w; x <= w; x++)
            for (int z = -w; z <= 0; z++) {
                boolean rib = Math.abs(x) == w || z == -w || Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0;
                sym(x, 7, z, rib ? "white_concrete" : "glass");
            }
        for (int x = -w; x <= w; x += 4) sym(x, 8, -w, "end_rod[facing=up]");
        // corner palms in the hall (canopies overhead), benches along the walls away from the doors
        palm(-6, -6, 4);
        palm(6, -6, 4);
        sym(-7, 1, -6, "spruce_stairs[facing=east,half=bottom]");
        sym(2, 1, -7, "spruce_stairs[facing=south,half=bottom]");
        sym(3, 1, -7, "spruce_stairs[facing=south,half=bottom]");
        sym(7, 1, -3, "potted_azalea_bush");
        sym(-1, 6, -7, "lantern[hanging=true]");
    }

    private void palm(int x, int z, int h) {
        sym(x, 0, z, "moss_block");
        for (int y = 1; y <= h; y++) sym(x, y, z, "jungle_log[axis=y]");
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                int d = Math.abs(dx) + Math.abs(dz);
                if (d == 0 || d > 2) continue;
                if (Math.abs(x + dx) > 7 || Math.abs(z + dz) > 7) continue;
                sym(x + dx, h + (d == 1 ? 1 : 0), z + dz, "jungle_leaves[persistent=true]");
            }
        sym(x, h + 1, z, "jungle_leaves[persistent=true]");
    }

    /** Palm Tower on the right flank: x 11..15, z -14..-10; mid floor y 4, exposed deck y 8, ladder inside. */
    private void palmTower() {
        int x1 = 11, x2 = 15, z1 = -14, z2 = -10;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                for (int y = 1; y <= 7; y++) {
                    if (!edge) { sym(x, y, z, (String) null); continue; }
                    String b = corner ? "quartz_pillar[axis=y]" : y <= 3 ? BRICK.at(x, y, z, seed + 8) : y == 4 ? "white_concrete" : "white_stained_glass_pane";
                    sym(x, y, z, b);
                }
                sym(x, 4, z, edge ? "white_concrete" : "birch_planks");
                sym(x, 8, z, edge ? "white_concrete" : "smooth_quartz");
                if (edge) sym(x, 9, z, corner ? "white_concrete" : "birch_fence");
            }
        for (int[] c : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) sym(c[0], 10, c[1], "lantern");
        // doors: ground door on the lawn side, mid-floor door to the footbridge
        symClear(13, 1, z2, 13, 2, z2);
        symClear(x1, 5, -13, x1, 6, -12);
        symLadder(13, -13, 1, 8, S);
        sym(14, 5, -11, "potted_jungle_sapling"); sym(12, 1, -11, "barrel[facing=up]");
        sym(14, 9, -11, "potted_bamboo");
        // footbridge from the Potting House roof (x 6) to the tower door (x 11) at y 4
        for (int x = 7; x <= 10; x++)
            for (int z = -14; z <= -11; z++) {
                boolean rail = z == -14 || z == -11;
                sym(x, 4, z, rail ? "white_concrete" : "birch_planks");
                if (rail) sym(x, 5, z, "birch_fence");
            }
        for (int y = 1; y <= 3; y++) { sym(7, y, -14, "quartz_pillar[axis=y]"); sym(10, y, -11, "quartz_pillar[axis=y]"); }
        sym(7, 6, -11, "lantern");
    }

    /** Serpentine hedge maze on the left flank, x -17..-9, z -12..-1 (paths 2 wide). */
    private void hedgeMaze() {
        int[][] rows = {{-12, -15, -9}, {-9, -17, -12}, {-6, -15, -9}, {-3, -17, -13}};
        for (int[] r : rows) for (int x = r[1]; x <= r[2]; x++) hedge(x, r[0]);
        for (int z = -11; z <= -10; z++) hedge(-9, z);
        // topiary balls at two bends and a sundial in the last straight
        topiary(-17, -11);
        topiary(-9, -5);
        sym(-15, 1, -2, "chiseled_stone_bricks"); sym(-15, 2, -2, "stone_brick_slab[type=bottom]");
        sym(-10, 1, -7, "potted_red_tulip");
        // back yard: a little round fountain and potting crates (left of the spawn) - low obstacles
        int fx = -12, fz = -15;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                sym(fx + dx, 1, fz + dz, dx == 0 && dz == 0 ? "chiseled_stone_bricks" : "stone_brick_slab[type=bottom]");
        sym(fx, 2, fz, "water_cauldron[level=3]");
        sym(-16, 1, -17, "barrel[facing=up]"); sym(-15, 1, -17, "composter[level=6]");
        sym(-8, 1, -17, "hay_block[axis=x]");
    }

    private void hedge(int x, int z) {
        for (int y = 1; y <= 2; y++) sym(x, y, z, LEAVES.at(x, y, z, seed + 21));
        sym(x, 3, z, rnd(x, 3, z, 22) < 0.8 ? "oak_leaves[persistent=true]" : "flowering_azalea_leaves[persistent=true]");
    }

    private void topiary(int x, int z) {
        sym(x, 1, z, "moss_block");
        sym(x, 2, z, "oak_leaves[persistent=true]");
        sym(x, 3, z, "flowering_azalea_leaves[persistent=true]");
    }

    /** Open lawn on the right flank (x 9..17, z -9..-1): almost empty, one low flower bed and a basin. */
    private void lawn() {
        for (int x = 13; x <= 16; x++) { sym(x, 1, -7, "moss_block"); sym(x, 2, -7, FLOWERS.at(x, 2, -7, seed + 5)); }
        int cx = 12, cz = -3;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) sym(cx + dx, 1, cz + dz, "smooth_quartz_slab[type=bottom]");
        sym(cx, 0, cz, "water"); sym(cx, -1, cz, "prismarine_bricks"); sym(cx, 1, cz, "lily_pad");
        sym(16, 1, -12, "potted_azalea_bush"); sym(16, 1, -16, "barrel[facing=up]");
        sym(9, 1, -17, "spruce_stairs[facing=south,half=bottom]"); sym(10, 1, -17, "spruce_stairs[facing=south,half=bottom]");
    }

    /** Front yard between the Potting House and the hall: low cold frames only. */
    private void frontYard() {
        for (int x = 1; x <= 4; x++) {
            sym(x, 1, -10, x == 1 || x == 4 ? "stripped_birch_log[axis=x]" : "farmland[moisture=7]");
            if (x == 2 || x == 3) sym(x, 2, -10, rnd(x, 2, -10, 4) < 0.5 ? "wheat[age=5]" : "carrots[age=6]");
        }
        sym(-3, 1, -10, "composter[level=3]");
        sym(-6, 1, -10, "potted_fern");
    }
}
