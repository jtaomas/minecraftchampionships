package com.mcm.games.tgttos.maps.a;

import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Design;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Noise;
import com.mcm.games.tgttos.maps.a.util.Noise.Pal;
import com.mcm.games.tgttos.maps.a.util.Stairs;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Dam Face (climb, bridging kit). A pale curved dam in a rock canyon faces the start across a stilling basin.
 * Built route: 3-wide ladder banks zig-zagging between yellow bamboo catwalk terraces every 8 blocks (pipes to
 * hop, grating holes to jump). Fast route: pillar straight up the face (overhanging terrace lips, ~41 blocks of
 * the 64). The crest has a parapet with one 4-wide stair cut in the middle (choke) and side ladders far out.
 *
 * <p>Local frame: x across (lane -18..17), z along the run. Apron/start y = 0 (feet 1); basin water surface and
 * dam toe y = -3 (feet -2); terraces at y = 8, 16, 24, 32; crest road y = 36 (feet 37).
 */
public final class DamFaceMap extends AMap {

    @Override public String id() { return "dam_face"; }
    @Override public String displayName() { return "Dam Face"; }
    @Override public MapType type() { return MapType.CLIMB; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }

    private Gen g;
    private final Pal face = new Pal("smooth_stone", 45, "calcite", 20, "stone", 15, "polished_andesite", 12, "andesite", 8);
    private final Pal faceLow = new Pal("stone", 35, "andesite", 25, "smooth_stone", 20, "mossy_stone_bricks", 10, "tuff", 10);
    private final Pal apron = new Pal("polished_andesite", 50, "smooth_stone", 30, "andesite", 20);
    private final Pal rock = new Pal("stone", 35, "andesite", 20, "tuff", 20, "cobblestone", 10, "deepslate", 15);
    private final Pal rockDeep = new Pal("deepslate", 40, "cobbled_deepslate", 25, "tuff", 25, "stone", 10);
    private final Pal catwalk = new Pal("bamboo_mosaic", 70, "bamboo_planks", 30);
    private final Pal copper = new Pal("oxidized_copper", 50, "weathered_copper", 25, "oxidized_cut_copper", 25);

    static final int TOE = -3, CREST = 36;
    static final int[] LADDER_L = {-15, -13}, LADDER_R = {12, 14};

    /** Face z of tier 0 at column x (concave: the middle is further away). */
    static int zf(int x) { return 23 + (int) Math.round(2.0 * (1 - (x / 19.0) * (x / 19.0))); }

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;
        apronAndBasin(d);
        dam(de);
        crest(d, de);
        canyon();
        reservoir();
        sealSides();
        startStrip(d);

        d.voidY = -12;
        d.playBounds = Region.of(-19, -14, -8, 18, 48, 62);
        d.buildBounds = Region.of(-18, -6, 8, 17, 42, 60);
        d.overviewPoints.add(new Spawn(0.5, 30, -16, 0f, 22f));
        d.overviewPoints.add(new Spawn(-16, 44, 30, -120f, 35f));
        d.notes.put("fastLine", "baffle jumps, pillar up the middle under the terrace lips (~41 blocks), centre stair choke, ~27 s expert");
        d.notes.put("safeLine", "side wall + ladder banks zig-zagging between catwalks, no blocks, ~45 s expert");

        de.searchBox = Region.of(-19, -8, -6, 18, 44, 62);
        de.lane.add(Region.of(-19, -40, -12, 18, 90, 130));
        de.lazyCorridor = Region.of(-2, -8, 0, 1, 44, 60);
        de.sprintExempt.add(Region.of(-19, 8, 20, 18, 34, 40));   // catwalks run across (x); checked per line
        // playtest 2: no walkable surface may touch an open drop to the void (the owner fell through one)
        de.holeScan = true;

        Design.Line fast = de.line("FAST", true, true);
        fast.description = "jump the baffle blocks, pillar up the middle stepping out under each terrace lip, centre stair cut (choke)";
        fast.leg("dam toe", Region.of(-5, TOE + 1, 20, 4, TOE + 1, 23), Region.of(-9, -6, 7, 7, 2, 24))
            .leg("terrace 3", Region.of(-5, 33, 33, 4, 33, 37), Region.of(-6, -3, 19, 5, 34, 38))
            .leg("pen", d.pen, Region.of(-6, 32, 30, 6, 40, 58));
        Design.Line safe = de.line("SAFE", false, false);
        safe.maxGap = 2;
        safe.description = "left training wall, ladder banks left/right/left/right between the catwalks, side or centre to the crest";
        safe.leg("pen", d.pen, Region.of(-18, -6, 7, 17, 42, 58));
    }

    // ------------------------------------------------------------------ apron, basin

    private void apronAndBasin(TgMapData d) {
        // apron slab z -8..10, 6 deep body with tapered rock underneath
        g.fill(-21, -5, -8, 20, 0, 10, apron);
        g.underside(-21, -8, 20, 10, -5, 9, rockDeep, 5);
        // bollards and a gauge hut on the apron front (hazard striped)
        for (int x = -15; x <= 15; x += 6) {
            g.set(x, 1, 9, "yellow_terracotta");
            g.set(x, 2, 9, "black_terracotta");
            g.set(x, 3, 9, "polished_andesite_slab[type=bottom]");
        }
        for (int x = -21; x <= 20; x++) g.set(x, 0, 10, (x & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
        // basin: walls + floor, water 3 deep (surface cell y = -3)
        g.fill(-21, -9, 11, 20, -6, 19, rockDeep);
        g.fill(-13, -5, 11, 12, -3, 19, "water");
        g.fill(-21, -5, 11, -14, 0, 19, apron);        // left training wall (walkway top y = 0)
        g.fill(13, -5, 11, 20, 0, 19, apron);          // right training wall
        g.underside(-21, 11, 20, 19, -9, 6, rockDeep, 6);
        // training walls step down to the toe with real stairs
        for (int z = 18; z <= 19; z++) { g.fill(-16, -5, z, -14, 0, z, (String) null); g.fill(13, -5, z, 15, 0, z, (String) null); }
        g.fill(-16, -6, 18, -14, -4, 19, apron);
        g.fill(13, -6, 18, 15, -4, 19, apron);
        Stairs.flight(g, "left wall -> toe (down)", "polished_andesite", "polished_andesite", -16, 20, TOE, 0, -1, 3, 3);
        Stairs.flight(g, "right wall -> toe (down)", "polished_andesite", "polished_andesite", 13, 20, TOE, 0, -1, 3, 3);
        // walls around the descending stairs
        for (int z = 17; z <= 19; z++) { g.fill(-17, -3, z, -17, 1, z, apron); g.fill(-13, -3, z, -13, 0, z, apron); g.fill(12, -3, z, 12, 0, z, apron); g.fill(16, -3, z, 16, 1, z, apron); }
        // baffle blocks (stepping stones, tops at the water surface level -3)
        int[][] baffles = {{-7, 12}, {-2, 12}, {3, 12}, {8, 12}, {-4, 16}, {1, 16}, {6, 16}, {-10, 16}};
        for (int[] b : baffles) {
            g.fill(b[0], -5, b[1], b[0] + 1, TOE, b[1] + 1, "polished_andesite");
            g.set(b[0], TOE, b[1], "dark_prismarine");
            g.set(b[0] + 1, TOE, b[1] + 1, "dark_prismarine");
        }
        // dam toe walkway y = -3, from z 20 up to the face
        // x -19 / 18 included (playtest 2 fix): the walkway used to stop one column short of the dam's side columns,
        // leaving a 1 x 3 hole to the void at each end of the toe (x -19 / 18, z 20..22)
        for (int x = -19; x <= 18; x++) for (int z = 20; z < zf(x); z++) {
            g.fill(x, -9, z, x, TOE, z, z == 20 ? "dark_prismarine" : faceLow.at(x, -4, z, g.seed));
        }
        g.underside(-18, 20, 17, 25, -9, 6, rockDeep, 7);
        // apron-to-basin edge rail on the sides (keeps the start honest), lamps
        for (int z = 11; z <= 16; z++) { g.set(-21, 1, z, "bamboo_fence"); g.set(20, 1, z, "bamboo_fence"); }
        g.set(-21, 2, 11, "lantern[hanging=false,waterlogged=false]");
        g.set(20, 2, 11, "lantern[hanging=false,waterlogged=false]");
    }

    // ------------------------------------------------------------------ the dam

    private boolean ladderCol(int x, int k) {
        int[] r = (k % 2 == 0) ? LADDER_L : LADDER_R;
        return x >= r[0] && x <= r[1];
    }

    private void dam(Design de) {
        for (int x = -19; x <= 18; x++) {
            int z0 = zf(x);
            for (int k = 0; k <= 3; k++) {
                int yb = k == 0 ? TOE + 1 : 8 * k + 1, yt = 8 * k + 8;
                int fz = z0 + 3 * k;
                // face shell (2 thick), joints every 6 columns, darker low
                for (int y = yb; y <= yt; y++) for (int t = 0; t <= 1; t++) {
                    String s = Math.floorMod(x, 6) == 0 ? "polished_andesite" : (y < 6 ? faceLow : face).at(x, y, fz + t, g.seed);
                    if (t == 0 && y == yt - 1 && Math.floorMod(x, 3) == 0) s = "tuff";       // stain under the lip
                    g.set(x, y, fz + t, s);
                }
                // terrace k on top: lip + 3 deep catwalk
                int ty = yt;
                for (int z = fz - 1; z <= fz + 2; z++) g.set(x, ty, z, z == fz - 1 ? (Math.floorMod(x, 4) < 2 ? "yellow_terracotta" : "black_terracotta") : catwalk.at(x, ty, z, g.seed));
                g.set(x, ty - 1, fz - 1, "smooth_stone_slab[type=top]");
                // ladder bank of tier k (on the face, standing on terrace k-1 / the toe)
                if (ladderCol(x, k)) {
                    for (int y = yb; y <= yt; y++) g.set(x, y, fz - 1, "ladder[facing=north,waterlogged=false]");
                }
                // railings on the lip of terrace k (gaps every 4th, none over the next ladder bank top)
                boolean nextLadder = ladderCol(x, k + 1) || (k == 3 && false);
                if (!ladderCol(x, k) && Math.floorMod(x, 4) != 1)
                    g.set(x, ty + 1, fz - 1, "bamboo_fence");
                if (nextLadder) { /* keep the catwalk clear in front of the next bank */ }
            }
            // tier 4 (to the crest) and the crest road
            int fz = z0 + 12;
            for (int y = 33; y <= CREST; y++) for (int t = 0; t <= 1; t++) g.set(x, y, fz + t, face.at(x, y, fz + t, g.seed));
        }
        // obstacles on the catwalks: pipes to hop and grating holes to jump
        for (int k = 0; k <= 3; k++) {
            int ty = 8 * k + 8;
            int[] pipes = (k % 2 == 0) ? new int[]{-9, -3, 5, 10} : new int[]{-10, -5, 2, 8};
            for (int px : pipes) for (int z = zf(px) + 3 * k; z <= zf(px) + 3 * k + 2; z++) {
                g.set(px, ty + 1, z, "polished_deepslate");
                if (z == zf(px) + 3 * k + 2) g.set(px, ty + 2, z, "lever[face=floor,facing=east,powered=false]");
            }
            int hx = (k % 2 == 0) ? 1 : -7;
            for (int x = hx; x <= hx + 1; x++) for (int z = zf(x) + 3 * k - 1; z <= zf(x) + 3 * k + 2; z++) g.set(x, ty, z, null);
            for (int x = hx; x <= hx + 1; x++) { g.set(x, ty + 1, zf(x) + 3 * k - 1, null); g.set(x, ty - 1, zf(x) + 3 * k - 1, null); }
            // playtest 2 fix: the back row of each grating hole (z = face + 2) sits over the hollow inside of the dam, so a
            // racer who missed the jump fell straight into the void. A grate one block down makes it a 1-deep pit there;
            // the front row still drops onto the terrace below (intended)
            for (int x = hx; x <= hx + 1; x++) g.set(x, ty - 1, zf(x) + 3 * k + 2, "polished_deepslate");
            // valve boxes and lamps against the face of the next tier
            for (int x = -16; x <= 16; x += 8) {
                int z = zf(x) + 3 * k + 3;
                g.set(x, ty + 3, z - 1, "lantern[hanging=false,waterlogged=false]");
                g.set(x, ty + 2, z - 1, "polished_deepslate_wall");
                g.set(x, ty + 1, z - 1, "polished_deepslate_wall");
            }
        }
        for (int x = -2; x <= 1; x++) g.set(x, 33, zf(x) + 8, null);
        // side ladders to the crest (long way round)
        for (int x = -16; x <= -14; x++) for (int y = 33; y <= CREST; y++) g.set(x, y, zf(x) + 11, "ladder[facing=north,waterlogged=false]");
        for (int x = 13; x <= 15; x++) for (int y = 33; y <= CREST; y++) g.set(x, y, zf(x) + 11, "ladder[facing=north,waterlogged=false]");
        // sluice gates in the lowest tier (teal copper panels with chain hoists), not on the ladder banks
        for (int gx : new int[]{-9, -4, 4, 9}) {
            for (int x = gx - 1; x <= gx + 1; x++) for (int y = TOE + 2; y <= 5; y++) g.set(x, y, zf(x), (y == 5 || x == gx) ? "oxidized_cut_copper" : "oxidized_copper");
            for (int y = 6; y <= 7; y++) g.set(gx, y, zf(gx), "chain[axis=y]");
        }
        // big painted depth marks on the face (scenery numerals as bars)
        for (int k = 0; k < 4; k++) for (int x = 5; x <= 8; x++) g.set(x, 8 * k + 4, zf(x) + 3 * k, "cyan_terracotta");
    }

    // ------------------------------------------------------------------ crest, pen, control tower

    private void crest(TgMapData d, Design de) {
        for (int x = -19; x <= 18; x++) {
            int fz = zf(x) + 12;
            for (int z = fz; z <= 60; z++) g.set(x, CREST, z, (z == 60 || x == -19 || x == 18) ? "smooth_stone" : ((x + z) % 7 == 0 ? "polished_andesite" : "stone_bricks"));
            for (int z = fz + 2; z <= 60; z++) g.set(x, CREST - 1, z, "stone");
            // crest parapet (2 walls high) except the stair cut and the side ladders
            boolean gap = (x >= -2 && x <= 1) || (x >= -16 && x <= -14) || (x >= 13 && x <= 15);
            if (!gap) {
                g.set(x, CREST + 1, fz, "stone_brick_wall");
                g.set(x, CREST + 2, fz, Math.floorMod(x, 6) == 0 ? "lantern[hanging=false,waterlogged=false]" : "stone_brick_wall");
            }
            // rear parapet above the reservoir
            g.set(x, CREST + 1, 60, "stone_brick_wall");
        }
        // centre stair cut through tier 4 (choke, 4 wide)
        Stairs.flight(g, "centre crest stair", "stone", "smooth_stone", -2, zf(0) + 9, 32, 0, 1, 4, 4);
        // pen: low bamboo-walled yard in the middle of the crest
        int x1 = -6, x2 = 6, z1 = 43, z2 = 55;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (edge) g.set(x, CREST + 1, z, ((x + z) & 1) == 0 ? "stripped_bamboo_block[axis=y]" : "bamboo_block[axis=y]");
            else g.set(x, CREST, z, g.rnd(x, 0, z) < 0.75 ? "moss_block" : "grass_block[snowy=false]");
        }
        g.set(-4, CREST + 1, 52, "hay_block[axis=y]");
        g.set(4, CREST + 1, 46, "hay_block[axis=x]");
        d.pen = Region.of(x1 + 1, CREST + 1, z1 + 1, x2 - 1, CREST + 3, z2 - 1);
        for (int i = 0; i < 10; i++) d.chickenSpawns.add(new Spawn(-3.5 + (i % 5) * 1.8, CREST + 1, 46.5 + (i / 5) * 5, 180f));
        // orange/white striped finish arch over the pen front
        for (int y = CREST + 1; y <= CREST + 8; y++) { g.set(-7, y, 43, (y & 1) == 0 ? "orange_terracotta" : "white_terracotta"); g.set(7, y, 43, (y & 1) == 0 ? "orange_terracotta" : "white_terracotta"); }
        for (int x = -7; x <= 7; x++) g.set(x, CREST + 9, 43, (x & 1) == 0 ? "orange_terracotta" : "white_terracotta");
        // gate-machinery houses on the crest sides
        machineHouse(-17, 40, -11, 47);
        machineHouse(10, 40, 16, 47);
        // footbridge to the intake tower
        for (int z = 61; z <= 67; z++) for (int x = -1; x <= 1; x++) g.set(x, CREST, z, x == 0 ? "bamboo_mosaic" : "smooth_stone_slab[type=bottom]");
        for (int z = 61; z <= 67; z++) { g.set(-2, CREST + 1, z, "bamboo_fence"); g.set(2, CREST + 1, z, "bamboo_fence"); }
        g.set(0, CREST + 1, 60, null);
        g.set(-1, CREST + 1, 60, null);
        g.set(1, CREST + 1, 60, null);
        // crest lamps & bollards break the long runs (outside the pen)
        g.breakRuns(-19, 18, 37, 59, CREST + 1, 9, java.util.List.of(Region.of(-6, CREST, 43, 6, CREST + 3, 55), Region.of(-1, CREST, 59, 1, CREST + 2, 67)),
                "andesite_wall", "barrel[facing=up,open=false]", "lightning_rod[facing=up,powered=false,waterlogged=false]", "yellow_terracotta");
        // the intake / control tower (landmark), standing in the reservoir
        double cx = 0.5, cz = 72.5;
        g.cyl(cx, cz, 5.5, 20, 64, new Pal("smooth_stone", 50, "calcite", 30, "polished_andesite", 20));
        for (int y = 30; y <= 62; y += 8) g.ring(cx, cz, 6.2, 0.9, y, y, new Pal("smooth_stone_slab[type=bottom]", 1));
        for (int y = 40; y <= 60; y += 8) g.ring(cx, cz, 5.5, 1, y, y + 2, new Pal("smooth_stone", 5, "glass", 2));
        g.cyl(cx, cz, 6.6, 65, 65, copper);
        g.ring(cx, cz, 6.6, 1, 66, 69, new Pal("glass", 3, "oxidized_cut_copper", 1));
        g.cyl(cx, cz, 5.6, 66, 69, new Pal("air", 1));
        g.set(0, 66, 72, "sea_lantern");
        g.cyl(cx, cz, 6.6, 70, 70, copper);
        for (int y = 71; y <= 74; y++) g.cyl(cx, cz, 6.6 - (y - 70) * 1.4, y, y, copper);
        for (int y = 75; y <= 88; y++) g.set(0, y, 72, y == 88 ? "lightning_rod[facing=up,powered=false,waterlogged=false]" : "iron_bars");
        g.set(0, 84, 72, "redstone_lamp[lit=true]");
        // lit round sign on a frame above the cabin, facing the start
        for (int x = -5; x <= 5; x++) for (int y = 73; y <= 83; y++) {
            double dx = x, dy = y - 78;
            double rr = Math.sqrt(dx * dx + dy * dy);
            if (rr <= 5.3 && rr > 4.2) g.set(x, y, 68, "sea_lantern");
            else if (rr <= 4.2) g.set(x, y, 68, (Math.abs(x) <= 1 || Math.abs(y - 78) <= 1) && rr <= 3.2 ? "glowstone" : "light_gray_terracotta");
        }
        for (int y = 70; y <= 73; y++) { g.set(-3, y, 68, "iron_bars"); g.set(3, y, 68, "iron_bars"); }
        for (int x = -5; x <= 5; x++) for (int y = 73; y <= 83; y++) if (!g.air(x, y, 68)) de.landmark.add(new Pos(x, y, 68));
        de.landmark.add(new Pos(0, 84, 72));
        g.fill(-1, 37, 66, 1, 39, 67, "air");
        g.set(0, 37, 67, "oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        g.set(0, 38, 67, "oak_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
    }

    private void machineHouse(int x1, int z1, int x2, int z2) {
        g.walls(x1, CREST + 1, z1, x2, CREST + 5, z2, new Pal("smooth_stone", 60, "calcite", 40));
        g.fill(x1 + 1, CREST + 1, z1 + 1, x2 - 1, CREST + 5, z2 - 1, (String) null);
        for (int x = x1 + 1; x < x2; x += 2) { g.set(x, CREST + 3, z1, "glass_pane"); g.set(x, CREST + 3, z2, "glass_pane"); }
        g.fill(x1 - 1, CREST + 6, z1 - 1, x2 + 1, CREST + 6, z2 + 1, copper);
        g.fill(x1, CREST + 7, z1, x2, CREST + 7, z2, "oxidized_cut_copper_slab[type=bottom]");
        g.set((x1 + x2) / 2, CREST + 2, z1 + 1, "grindstone[face=floor,facing=north]");
        g.set((x1 + x2) / 2, CREST + 4, z1 + 2, "shroomlight");
    }

    // ------------------------------------------------------------------ canyon and reservoir

    private void canyon() {
        Pal top = new Pal("grass_block[snowy=false]", 55, "moss_block", 20, "coarse_dirt", 15, "podzol[snowy=false]", 10);
        for (int side = -1; side <= 1; side += 2) {
            for (int z = -14; z <= 112; z++) {
                double n = Noise.fbm2(z * 0.07, side * 3.1, g.seed + 21);
                int h = Math.min(40 + (int) Math.round(10 * n) - (z > 70 ? (z - 70) / 4 : 0), 4 + (int) Math.round((z + 14) * 1.15 + 6 * n));
                for (int y = -18; y <= h; y++) {
                    double m = Noise.fbm2(z * 0.12, y * 0.12, g.seed + 33 + side);
                    int inner = (side < 0 ? -20 : 19) + side * (int) Math.round(4 * m + (y < -6 ? (-6 - y) * 0.6 : 0) + (z < 8 && z > -14 ? 2 : 0));
                    int thick = 3;
                    for (int t = 0; t < thick; t++) {
                        int x = inner + side * t;
                        String s = y < 6 ? rockDeep.at(x, y, z, g.seed) : rock.at(x, y, z, g.seed);
                        if (y > 30 && g.rnd(x, y, z, 7) < 0.15) s = "calcite";
                        g.set(x, y, z, s);
                    }
                    if (y == h) {
                        for (int x = inner; side < 0 ? x >= inner - 7 : x <= inner + 7; x += side) {
                            g.set(x, y, z, top.at(x, y, z, g.seed));
                            g.set(x, y - 1, z, "dirt");
                        }
                    }
                }
                // ledges and pines on top
                int ex = (side < 0 ? -26 : 25);
                if (Math.floorMod(z * 7 + side * 3, 11) == 0) pine(ex + side * (int) (g.rnd(z, 1, side) * 5), h + 1, z, 5 + (int) (g.rnd(z, 2, side) * 6));
                if (g.rnd(z, 3, side, 4) < 0.08) g.set((side < 0 ? -22 : 21), h + 1, z, "fern");
            }
        }
        // a waterfall-coloured spill chute on the left canyon wall (blue ice column - no flowing water)
        for (int y = -10; y <= 34; y++) for (int x = -23; x <= -22; x++) g.set(x, y, 30, g.rnd(x, y, 30) < 0.5 ? "packed_ice" : "blue_ice");
    }

    /**
     * Playtest 2 fix: the canyon rock band wanders with noise and in places stood 1-3 blocks off the dam's side columns
     * (x -19 / 18), leaving open slits to the void beside every terrace end and along both crest edges. Fill the air
     * between the dam edge and the rock, flush with the dam surface at that z (a 3-deep lid, no new kerb; the look is unchanged).
     */
    private void sealSides() {
        for (int z = 18; z <= 106; z++)
            for (int side = -1; side <= 1; side += 2) {
                int ex = side < 0 ? -19 : 18;
                // every surface of the dam's side column (toe, each terrace, crest, reservoir) gets a lid out to the rock
                for (int top = -9; top <= CREST + 1; top++) {
                    if (g.air(ex, top, z) || !g.air(ex, top + 1, z)) continue;
                    for (int t = 1; t <= 6; t++) {
                        int x = ex + side * t;
                        boolean rockHere = !g.air(x, top, z);
                        for (int y = top - 2; y <= top; y++) if (g.air(x, y, z)) g.set(x, y, z, y < 6 ? rockDeep.at(x, y, z, g.seed) : rock.at(x, y, z, g.seed));
                        if (rockHere) break;
                    }
                }
            }
    }

    private void pine(int x, int y, int z, int h) {
        for (int i = 0; i < h; i++) g.set(x, y + i, z, "spruce_log[axis=y]");
        for (int i = 2; i <= h; i++) {
            int r = Math.max(0, (h - i) / 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && i < h)) g.soft(x + dx, y + i, z + dz, "spruce_leaves");
        }
        g.set(x, y + h, z, "spruce_leaves");
    }

    private void reservoir() {
        // bed at y 33, water y 34..35, from the crest back to a far shore at z 104
        for (int x = -19; x <= 18; x++) for (int z = 61; z <= 104; z++) {
            g.set(x, 33, z, z > 100 ? "gravel" : "mud");
            g.set(x, 34, z, z > 102 ? "sand" : "water");
            g.set(x, 35, z, z > 101 ? (z > 103 ? "grass_block[snowy=false]" : "sand") : "water");
        }
        for (int x = -19; x <= 18; x++) for (int z = 61; z <= 104; z++) if (g.air(x, 36, z) && z > 103) g.set(x, 36, z, "short_grass");
        g.underside(-19, 61, 18, 104, 33, 8, rockDeep, 9);
        // buoy line across the reservoir
        for (int x = -18; x <= 17; x += 3) g.set(x, 36, 80, (x / 3) % 2 == 0 ? "red_terracotta" : "white_terracotta");
    }

    private void startStrip(TgMapData d) {
        for (int x = -18; x <= 17; x++) for (int z = 0; z <= 6; z++) g.set(x, 0, z, ((x + z) & 1) == 0 ? "polished_blackstone" : "polished_basalt[axis=y]");
        for (int x = -18; x <= 17; x++) {
            g.set(x, 0, 7, (x & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
            for (int y = 1; y <= 3; y++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new Pos(x, y, 7)); }
        }
        for (int z = -1; z <= 7; z++) for (int y = 1; y <= 3; y++) { g.set(-19, y, z, apron.at(-19, y, z, g.seed)); g.set(18, y, z, apron.at(18, y, z, g.seed)); }
        for (int x = -19; x <= 18; x++) for (int y = 1; y <= 3; y++) g.set(x, y, -1, y == 3 ? "smooth_stone" : apron.at(x, y, -1, g.seed));
        for (int x = -16; x <= 16; x += 8) g.set(x, 4, -1, "lantern[hanging=false,waterlogged=false]");
        // gauge house behind the start
        g.walls(-6, 1, -8, 6, 6, -3, new Pal("smooth_stone", 60, "calcite", 40));
        g.fill(-5, 1, -7, 5, 6, -4, (String) null);
        g.fill(-7, 7, -9, 7, 7, -2, copper);
        for (int x = -4; x <= 4; x += 2) g.set(x, 4, -3, "glass_pane");
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        for (int row = 0; row < 3; row++)
            for (int i = 0; i < 20; i++) d.startSpawns.add(new Spawn(-17.5 + i * 1.8 + (row % 2) * 0.6, 1, 1.5 + row * 2, 0f));
    }
}
