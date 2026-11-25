package com.mcm.games.tgttos.maps.a;

import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Design;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Noise.Pal;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Laundry Line (bridging). Rooftop to rooftop across two streets of a red-brick terrace town. Washing lines
 * and hanging sheets thread the street gaps; a line of chimney stacks gives a pure-parkour fast line; a pitched
 * middle roof has a chimney-breast wall along its ridge with one 4-wide gap (choke) in the centre.
 *
 * <p>Local frame: x across (lane -21..20), z along the run, roof decks at y = 0 (feet y = 1).
 * Roof A z -8..12 (start strip z 0..6), street 1 z 13..36 (24), roof B (pitched) z 37..48, street 2 z 49..68,
 * roof C z 69..104 with the roof-garden pen and the weather-vane tower.
 */
public final class LaundryLineMap extends AMap {

    @Override public String id() { return "laundry_line"; }
    @Override public String displayName() { return "Laundry Line"; }
    @Override public MapType type() { return MapType.BRIDGE; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }

    private Gen g;

    // palettes
    private final Pal brick = new Pal("bricks", 70, "mud_bricks", 6, "granite", 4, "terracotta", 6, "bricks", 14);
    private final Pal brick2 = new Pal("bricks", 30, "terracotta", 20, "red_terracotta", 10, "mud_bricks", 30, "granite", 10);
    private final Pal soot = new Pal("deepslate_bricks", 40, "cobbled_deepslate", 30, "deepslate_tiles", 20, "tuff", 10);
    private final Pal deck = new Pal("polished_deepslate", 40, "deepslate_tiles", 30, "cobbled_deepslate", 15, "tuff", 15);
    private final Pal slate = new Pal("deepslate_tiles", 60, "polished_deepslate", 25, "cobbled_deepslate", 15);
    private final Pal stone = new Pal("stone_bricks", 60, "cracked_stone_bricks", 15, "andesite", 15, "polished_andesite", 10);
    private final Pal rubble = new Pal("cobbled_deepslate", 30, "tuff", 30, "andesite", 20, "bricks", 10, "gravel", 10);
    private final Pal linen = new Pal("white_wool", 70, "light_gray_wool", 20, "white_wool", 10);
    private final Pal pastel = new Pal("white_wool", 50, "pink_wool", 25, "light_blue_wool", 25);

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;

        buildingA();
        roofB();
        buildingC();
        street1();
        street2();
        neighbours();
        skyline();
        startStrip(d);
        pen(d);
        tower(d, de);
        // break up long runs on the goal roof (flower tubs, crates, vents)
        g.breakRuns(-22, 21, 69, 103, 1, 11, java.util.List.of(Region.of(-16, 0, 76, 16, 9, 80)),
                "barrel[facing=up,open=false]", "potted_azalea_bush", "polished_deepslate_wall", "hay_block[axis=y]", "composter[level=5]");

        d.voidY = -8;
        d.playBounds = Region.of(-23, -14, -9, 22, 45, 106);
        d.buildBounds = Region.of(-21, -6, 8, 20, 14, 90);
        d.overviewPoints.add(new Spawn(0.5, 30, -14, 0f, 32f));
        d.overviewPoints.add(new Spawn(-30, 24, 42, -90f, 25f));
        d.notes.put("fastLine", "chimney stacks (left of centre) + ridge choke + chimney stacks, no blocks, ~17 s expert");
        d.notes.put("safeLine", "own bridge on the right, round the wide ridge gap, ~46 blocks, ~26 s expert / ~45 s median");

        de.searchBox = Region.of(-23, -10, -2, 22, 20, 104);
        de.lane.add(Region.of(-22, -40, -8, 21, 60, 104));
        de.lazyCorridor = Region.of(-2, -6, 0, 2, 14, 90);
        de.sprintExempt.add(Region.of(-23, 4, 41, 22, 6, 44));   // ridge top runs across (x), not along the course

        Design.Line fast = de.line("FAST", false, true);
        fast.description = "chimney-stack jumps left of centre (gaps 2-3, +1 rises, head-hitter towel), centre ridge choke, second chimney row with a 4-gap";
        fast.leg("ridge choke", Region.of(-2, 5, 42, 1, 5, 43), Region.of(-11, -2, 7, 2, 8, 44))
            .leg("roof C", Region.of(-6, 1, 69, 8, 1, 73), Region.of(-4, -3, 42, 8, 8, 74))
            .leg("pen", d.pen, Region.of(-8, 0, 69, 8, 4, 90));
        Design.Line safe = de.line("SAFE", true, false);
        safe.maxGap = 2;
        safe.description = "own bridge on the right weaving between sheets, over the roof through the wide right ridge gap, bridge again";
        safe.leg("right ridge gap", Region.of(11, 5, 42, 18, 5, 43), Region.of(5, -2, 7, 20, 3, 37), Region.of(5, -2, 37, 20, 6, 44))
            .leg("roof C", Region.of(6, 1, 69, 20, 1, 73), Region.of(5, -3, 42, 20, 6, 48), Region.of(5, -3, 48, 20, 3, 74))
            .leg("pen", d.pen, Region.of(-8, 0, 69, 20, 4, 90));
    }

    // ------------------------------------------------------------------ buildings in the lane

    /** Hollow terrace block: facade walls from bottom to top-1 with windows, deck at top, rubble underside. */
    private void block(int x1, int x2, int z1, int z2, int top, int bottom, Pal wall, Pal roof, boolean deckRoof) {
        for (int y = bottom; y < top; y++) {
            for (int x = x1; x <= x2; x++) { facade(x, y, z1, x, y, true); facade(x, y, z2, x, y, true); }
            for (int z = z1; z <= z2; z++) { facade(x1, y, z, z, y, false); facade(x2, y, z, z, y, false); }
        }
        // inner layer: lit rooms / curtains behind the windows, brick elsewhere
        for (int y = bottom; y < top; y++) {
            for (int x = x1 + 1; x <= x2 - 1; x++) { inner(x, y, z1 + 1, g.get(x, y, z1)); inner(x, y, z2 - 1, g.get(x, y, z2)); }
            for (int z = z1 + 1; z <= z2 - 1; z++) { inner(x1 + 1, y, z, g.get(x1, y, z)); inner(x2 - 1, y, z, g.get(x2, y, z)); }
        }
        if (deckRoof) g.fill(x1, top, z1, x2, top, z2, roof);
        // floor slab + rubble underside
        g.fill(x1, bottom - 1, z1, x2, bottom - 1, z2, soot);
        g.underside(x1, z1, x2, z2, bottom - 1, 7, rubble, 11);
        // interior floors (seen through the windows) and storey bands
        for (int y = bottom + 5; y < top - 1; y += 6) g.fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, "spruce_planks");
        for (int y = bottom + 5; y < top; y += 6) {
            for (int x = x1; x <= x2; x++) { g.set(x, y, z1, "stone_bricks"); g.set(x, y, z2, "stone_bricks"); }
            for (int z = z1; z <= z2; z++) { g.set(x1, y, z, "stone_bricks"); g.set(x2, y, z, "stone_bricks"); }
        }
    }

    private void inner(int x, int y, int z, String outer) {
        if (outer != null && outer.contains("glass_pane")) {
            double r = g.rnd(x, y / 2, z, 41);
            g.set(x, y, z, r < 0.22 ? "shroomlight" : r < 0.32 ? "ochre_froglight" : r < 0.6 ? "white_wool" : r < 0.8 ? "brown_wool" : "dark_oak_planks");
        } else g.set(x, y, z, "bricks");
    }

    private void facade(int x, int y, int z, int u, int v, boolean alongX) {
        facade0(x, y, z, u, v);
    }

    private void facade0(int x, int y, int z, int u, int v) {
        int bottom = -24;
        int fl = Math.floorMod(v - bottom, 6);   // storey position
        int col = Math.floorMod(u, 4);
        String s;
        if ((fl == 2 || fl == 3) && col == 1) s = "glass_pane";
        else if (fl == 4 && col == 1) s = "calcite";
        else if (fl == 1 && col == 1) s = "smooth_stone";
        else s = brick.at(x, y, z, g.seed);
        g.set(x, y, z, s);
    }

    private void buildingA() {
        block(-22, 21, -8, 12, 0, -24, brick, deck, true);
        // parapets along the sides of the lane on roof A and the back
        for (int z = -8; z <= 12; z++) { parapet(-22, z); parapet(21, z); }
        for (int x = -22; x <= 21; x++) parapet(x, -8);
        // front cornice under the edge (decor, below the bridge level)
        for (int x = -22; x <= 21; x++) { g.set(x, -1, 12, "stone_bricks"); g.set(x, -2, 12, "polished_deepslate"); }
        // roof clutter behind the start strip (scenery): water tank, stair hut, chimney stacks
        tank(-12, -5);
        hut(6, -6, 12, -3);
        stack(-20, -21 + 1, -6, -5, 0, 6, true);
        stack(16, 17, -6, -5, 0, 5, true);
        // roof A front (z 8..14): skylights and vents break it up
        skylight(-16, 9);
        skylight(9, 9);
        vent(-1, 11);
        vent(-10, 12);
        vent(16, 11);
        g.set(4, 1, 13, "barrel[facing=up]");
        g.set(-6, 1, 9, "barrel[facing=up]");
    }

    private void parapet(int x, int z) {
        g.set(x, 1, z, brick.at(x, 1, z, g.seed));
        if (Math.floorMod(z, 6) == 0) {
            g.set(x, 2, z, "stone_bricks");
            g.set(x, 3, z, "stone_bricks");
            g.set(x, 4, z, (Math.floorMod(z, 12) == 0) ? "lantern[hanging=false,waterlogged=false]" : "flower_pot");
        } else g.set(x, 2, z, "deepslate_tile_slab[type=bottom]");
    }

    private void tank(int x, int z) {
        for (int[] c : new int[][]{{x, z}, {x + 4, z}, {x, z + 4}, {x + 4, z + 4}}) g.fill(c[0], 1, c[1], c[0], 3, c[1], "spruce_fence");
        g.fill(x, 4, z, x + 4, 4, z + 4, "spruce_planks");
        g.cyl(x + 2.5, z + 2.5, 2.6, 5, 9, new Pal("spruce_planks", 60, "stripped_spruce_log[axis=y]", 40));
        g.cyl(x + 2.5, z + 2.5, 1.6, 6, 9, new Pal("air", 1));
        for (int xx = x; xx <= x + 4; xx++) for (int zz = z; zz <= z + 4; zz++) if (!g.air(xx, 9, zz)) g.set(xx, 10, zz, "spruce_slab[type=bottom]");
        g.set(x + 2, 10, z + 2, "spruce_planks");
        g.set(x + 2, 11, z + 2, "spruce_slab[type=bottom]");
    }

    private void hut(int x1, int z1, int x2, int z2) {
        g.walls(x1, 1, z1, x2, 4, z2, stone);
        g.fill(x1, 5, z1, x2, 5, z2, "deepslate_tile_slab[type=bottom]");
        g.set(x1 + 2, 1, z2, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
        g.set(x1 + 2, 2, z2, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
        g.set(x1 + 4, 3, z2 + 1, "lantern[hanging=false]");
        g.set(x1 + 4, 2, z2 + 1, "air");
        g.set(x1 + 4, 3, z2, "stone_bricks");
        g.set(x1 + 4, 3, z2 + 1, "wall_torch[facing=south]");
        for (int x = x1 + 1; x < x2; x++) for (int z = z1 + 1; z < z2; z++) for (int y = 1; y <= 4; y++) g.set(x, y, z, null);
    }

    /** Chimney stack from y=from to top; pots on top for scenery stacks. */
    private void stack(int x1, int x2, int z1, int z2, int from, int top, boolean pots) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            for (int y = from; y < top; y++) g.set(x, y, z, brick2.at(x, y, z, g.seed));
            g.set(x, top, z, "stone_bricks");
            if (pots) g.set(x, top + 1, z, (x + z) % 2 == 0 ? "decorated_pot[facing=south,waterlogged=false,cracked=false]" : "flower_pot");
        }
        if (pots) g.set(x1, top + 1, z1, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
    }

    private void skylight(int x, int z) {
        g.fill(x, 1, z, x + 3, 1, z + 2, "polished_andesite");
        g.fill(x, 2, z, x + 3, 2, z + 2, "glass");
        g.fill(x + 1, 3, z, x + 2, 3, z + 2, "glass");
        g.fill(x + 1, 2, z + 1, x + 2, 2, z + 1, "glass");
    }

    private void vent(int x, int z) {
        g.set(x, 1, z, "polished_deepslate_wall");
        g.set(x, 2, z, "polished_deepslate_wall");
        g.set(x, 3, z, "lightning_rod[facing=up,powered=false,waterlogged=false]");
    }

    private void roofB() {
        int x1 = -24, x2 = 23, z1 = 37, z2 = 48;
        block(x1, x2, z1, z2, 0, -24, brick, deck, false);
        for (int x = x1; x <= x2; x++) {
            g.set(x, 0, z1, "smooth_stone");
            g.set(x, 0, z2, "smooth_stone");
            g.set(x, -1, z1, "stone_bricks");    // flush cornice band (no ledge to stand on)
            g.set(x, -1, z2, "stone_bricks");
            for (int i = 1; i <= 4; i++) {
                int ya = i;                       // ascent from z1 side
                String sa = (x == x1 || x == x2) ? brick.at(x, ya, z1 + i, g.seed) : (i % 2 == 0 ? Gen.st("deepslate_tile", "south", false) : Gen.st("deepslate_tile", "south", false));
                g.set(x, ya, z1 + i, sa);
                g.fill(x, 0, z1 + i, x, ya - 1, z1 + i, slate);
                String sb = (x == x1 || x == x2) ? brick.at(x, ya, z2 - i, g.seed) : Gen.st("deepslate_tile", "north", false);
                g.set(x, ya, z2 - i, sb);
                g.fill(x, 0, z2 - i, x, ya - 1, z2 - i, slate);
            }
            g.fill(x, 0, 42, x, 4, 43, slate);
            g.set(x, 4, 42, "polished_deepslate");
            g.set(x, 4, 43, "polished_deepslate");
        }
        // gable ends rise as parapets
        for (int z = z1; z <= z2; z++) {
            int top = Math.min(z - z1, z2 - z) + 1;
            if (z >= 42 && z <= 43) top = 6;
            for (int y = 1; y <= top + 1; y++) { g.set(x1, y, z, brick.at(x1, y, z, g.seed)); g.set(x2, y, z, brick.at(x2, y, z, g.seed)); }
        }
        // chimney-breast wall on the ridge with three gaps: centre (choke, x -2..1) and two wide ones
        int[][] seg = {{-23, -20}, {-11, -3}, {2, 10}, {19, 22}};
        for (int[] s : seg) {
            for (int x = s[0]; x <= s[1]; x++) for (int z = 42; z <= 43; z++) {
                for (int y = 5; y <= 10; y++) g.set(x, y, z, brick2.at(x, y, z, g.seed));
                g.set(x, 11, z, "stone_brick_slab[type=bottom]");
            }
            // pots along the cap, stone band
            for (int x = s[0]; x <= s[1]; x++) {
                g.set(x, 8, 41, "stone_brick_slab[type=top]");
                g.set(x, 8, 44, "stone_brick_slab[type=top]");
                if ((x & 1) == 0) { g.set(x, 11, 42, "decorated_pot[facing=north,waterlogged=false,cracked=false]"); }
                else g.set(x, 11, 43, "flower_pot");
            }
        }
        // gap jambs: stone quoins and a hanging lantern over the choke
        for (int y = 5; y <= 10; y++) for (int z = 42; z <= 43; z++) {
            g.set(-3, y, z, "stone_bricks"); g.set(2, y, z, "stone_bricks");
            g.set(10, y, z, "stone_bricks"); g.set(19, y, z, "stone_bricks");
            g.set(-11, y, z, "stone_bricks"); g.set(-20, y, z, "stone_bricks");
        }
        for (int x = -2; x <= 1; x++) { g.set(x, 10, 42, "stone_brick_slab[type=top]"); g.set(x, 10, 43, "stone_brick_slab[type=top]"); }
        g.fill(-2, 9, 42, 1, 9, 43, "air");
        g.set(-1, 9, 42, "chain[axis=y]");
        g.set(-1, 8, 42, "lantern[hanging=true,waterlogged=false]");
        // dormers on both pitches (between the gaps) - scenery bumps that also break up the slope
        for (int x : new int[]{-16, -7, 6, 14}) { dormer(x, 39, "north"); dormer(x, 46, "south"); }
    }

    private void dormer(int x, int z, String face) {
        int dz = face.equals("north") ? -1 : 1;
        int y = face.equals("north") ? 3 : 3;
        g.fill(x, 2, z, x + 2, y + 1, z, brick);
        g.set(x + 1, 3, z, "glass_pane");
        g.set(x + 1, 2, z, "glass_pane");
        g.set(x, y + 2, z, Gen.st("deepslate_tile", "east", false));
        g.set(x + 1, y + 2, z, "deepslate_tile_slab[type=bottom]");
        g.set(x + 2, y + 2, z, Gen.st("deepslate_tile", "west", false));
    }

    private void buildingC() {
        block(-26, 25, 69, 104, 0, -24, brick, deck, true);
        for (int z = 69; z <= 104; z++) { parapet(-23, z); parapet(22, z); }
        for (int x = -26; x <= 25; x++) parapet(x, 104);
        for (int z = 69; z <= 104; z++) for (int x = -26; x <= -24; x++) g.set(x, 1, z, null);
        for (int z = 69; z <= 104; z++) for (int x = 23; x <= 25; x++) g.set(x, 1, z, null);
        for (int x = -26; x <= 25; x++) { g.set(x, -1, 69, "stone_bricks"); g.set(x, -2, 69, "polished_deepslate"); }
        // roof garden dressing (breaks long runs beside the pen)
        // (feedback 3: the two flower beds that stood right in front of the finish row moved back to z 70..72)
        bed(-14, 70, -10, 72);
        bed(9, 70, 13, 72);
        bed(-20, 84, -17, 90);
        greenhouse(10, 80, 16, 87);
        tank(-17, 77);
        bench(-10, 82);
        skylight(-21, 81);
        hut(-21, 86, -16, 90);
        stack(-13, -12, 92, 93, 0, 5, true);
        stack(18, 19, 82, 83, 0, 5, true);
        skylight(17, 72);
        vent(-18, 72);
        vent(19, 90);
        g.set(-12, 1, 72, "barrel[facing=up,open=false]");
        g.set(-11, 1, 72, "barrel[facing=up,open=false]");
        g.set(-11, 2, 72, "barrel[facing=up,open=false]");
        bench(8, 91);
        // drying lines on roof C (left) with a couple of sheets
        for (int z = 93; z <= 101; z += 4) {
            g.set(-21, 1, z, "spruce_fence"); g.set(-21, 2, z, "spruce_fence"); g.set(-21, 3, z, "spruce_fence");
            g.set(-9, 1, z, "spruce_fence"); g.set(-9, 2, z, "spruce_fence"); g.set(-9, 3, z, "spruce_fence");
            for (int x = -20; x <= -10; x++) g.set(x, 3, z, "chain[axis=x]");
            for (int x = -19; x <= -16; x++) g.fill(x, 1, z, x, 2, z, linen);
            for (int x = -14; x <= -12; x++) g.set(x, 2, z, pastel.at(x, 2, z, g.seed));
        }
        g.fill(10, 1, 95, 16, 3, 101, stone);
        g.fill(10, 4, 95, 16, 4, 101, "deepslate_tile_slab[type=bottom]");
        g.set(13, 1, 95, "spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        g.set(13, 2, 95, "spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
        g.set(12, 3, 94, "lantern[hanging=false,waterlogged=false]");
        g.set(12, 2, 94, "spruce_fence");
        g.set(12, 1, 94, "spruce_fence");
    }

    private void bed(int x1, int z1, int x2, int z2) {
        g.fill(x1, 1, z1, x2, 1, z2, "spruce_planks");
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            g.set(x, 1, z, edge ? "stripped_spruce_log[axis=" + (x == x1 || x == x2 ? "z" : "x") + "]" : "moss_block");
            if (!edge) {
                double r = g.rnd(x, 2, z);
                g.set(x, 2, z, r < 0.35 ? "flowering_azalea" : r < 0.6 ? "azalea" : r < 0.8 ? "red_tulip" : "cornflower");
            }
        }
    }

    private void greenhouse(int x1, int z1, int x2, int z2) {
        g.fill(x1, 1, z1, x2, 1, z2, "polished_andesite");
        for (int y = 2; y <= 4; y++) {
            for (int x = x1; x <= x2; x++) { g.set(x, y, z1, "glass_pane"); g.set(x, y, z2, "glass_pane"); }
            for (int z = z1; z <= z2; z++) { g.set(x1, y, z, "glass_pane"); g.set(x2, y, z, "glass_pane"); }
        }
        for (int x = x1; x <= x2; x += 3) for (int y = 2; y <= 4; y++) { g.set(x, y, z1, "spruce_fence"); g.set(x, y, z2, "spruce_fence"); }
        g.fill(x1, 5, z1, x2, 5, z2, "glass");
        g.fill(x1 + 1, 6, z1 + 1, x2 - 1, 6, z2 - 1, "glass");
        for (int x = x1 + 1; x < x2; x++) for (int z = z1 + 1; z < z2; z++) g.set(x, 2, z, g.rnd(x, 2, z) < 0.5 ? "potted_fern" : "air");
        g.set(x1, 2, z1 + 3, "air");
        g.set(x1, 3, z1 + 3, "air");
    }

    private void bench(int x, int z) {
        g.set(x, 1, z, Gen.st("spruce", "north", false));
        g.set(x + 1, 1, z, Gen.st("spruce", "north", false));
        g.set(x + 2, 1, z, Gen.st("spruce", "north", false));
        g.set(x - 1, 1, z, "spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]");
        g.set(x + 3, 1, z, "spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]");
    }

    // ------------------------------------------------------------------ the streets (gaps)

    /** Sheet hanging along x at row z, from the line at lineY down to hem (inclusive). */
    private void sheetX(int x1, int x2, int z, int hem, int lineY, Pal p) {
        for (int x = x1; x <= x2; x++) {
            int h = hem + (int) Math.round(g.rnd(x, 0, z, 77) * 0.6) * ((x == x1 || x == x2) ? 1 : 0);
            for (int y = h; y < lineY; y++) g.set(x, y, z, p.at(x, y, z, g.seed));
        }
        g.set(x1, lineY - 1, z, "white_wool");
    }

    private void lineX(int x1, int x2, int z, int y) { for (int x = x1; x <= x2; x++) if (g.air(x, y, z)) g.set(x, y, z, "chain[axis=x]"); }

    private void lineZ(int x, int z1, int z2, int y) { for (int z = z1; z <= z2; z++) if (g.air(x, y, z)) g.set(x, y, z, "chain[axis=z]"); }

    /** Iron street lamp post rising from the street far below to the top y, with a lantern arm. */
    private void post(int x, int z, int top, boolean lamp) {
        for (int y = -30; y <= top; y++) g.set(x, y, z, y < -26 ? "polished_deepslate" : "iron_bars");
        g.set(x, -31, z, "polished_deepslate_wall");
        g.set(x, top + 1, z, "polished_deepslate");
        if (lamp) {
            g.set(x, top + 2, z, "lantern[hanging=false,waterlogged=false]");
        }
    }

    private void street1() {
        // FAST: chimney stacks left of centre (top y = standing block)
        stack(-6, -5, 15, 16, -30, 0, false);
        stack(-7, -6, 20, 21, -30, 0, false);
        stack(-5, -4, 24, 25, -30, 1, false);
        stack(-5, -4, 29, 30, -30, 1, false);
        stack(-3, -2, 33, 34, -30, 0, false);
        // scenery chimneys further out (pots, not on a line)
        stack(-27, -26, 24, 25, -30, 3, true);
        stack(25, 26, 30, 31, -30, 4, true);
        // posts that carry the lines (iron, from below)
        post(-11, 15, 5, false);
        post(-11, 36, 5, false);
        post(-2, 21, 5, false);
        post(-2, 29, 5, false);
        post(-21, 21, 6, true);
        post(20, 21, 6, true);
        post(20, 29, 6, true);
        // line along z on the left (separates the chimney line from the far left)
        sheetZ(-11, 17, 22, 1, 5, linen);
        sheetZ(-11, 27, 33, 1, 5, pastel);
        // cross lines
        lineX(-1, 19, 21, 5);
        lineX(-20, -12, 21, 6);
        sheetX(0, 3, 21, 1, 5, linen);
        sheetX(7, 10, 21, 1, 5, pastel);
        sheetX(14, 17, 21, 1, 5, linen);
        sheetX(18, 19, 21, 3, 5, pastel);                 // towels: pass underneath
        sheetX(-19, -15, 21, 2, 6, linen);
        lineX(-1, 19, 29, 5);
        sheetX(-1, 1, 29, 1, 5, pastel);
        sheetX(5, 8, 29, 3, 5, linen);                    // towel
        sheetX(9, 12, 29, 1, 5, linen);
        sheetX(16, 19, 29, 1, 5, pastel);
        lineX(3, 19, 25, 5);
        sheetX(4, 6, 25, 1, 5, linen);
        sheetX(11, 15, 25, 1, 5, pastel);
        lineX(5, 20, 33, 5);
        sheetX(6, 9, 33, 1, 5, linen);
        sheetX(13, 16, 33, 1, 5, linen);
        sheetX(18, 20, 33, 1, 5, pastel);
        post(3, 25, 5, false);
        post(5, 33, 5, false);
        // head-hitter towel over the flat 3-gap of the chimney line (feet 2, hem 5)
        lineX(-10, -3, 27, 6);
        sheetX(-6, -4, 27, 5, 6, pastel);
        post(-10, 27, 5, false);
        // lines from roof A to roof B high up (decor), with shirts
    }

    private void sheetZ(int x, int z1, int z2, int hem, int lineY, Pal p) {
        for (int z = z1; z <= z2; z++) for (int y = hem; y < lineY; y++) g.set(x, y, z, p.at(x, y, z, g.seed));
        lineZ(x, z1, z2, lineY);
    }

    private void street2() {
        // FAST: chimney stacks right of centre, with the 4-gap between T2 and T3
        stack(0, 1, 51, 52, -30, -1, false);
        stack(1, 2, 56, 57, -30, -1, false);
        stack(2, 3, 62, 63, -30, -1, false);
        stack(4, 5, 66, 67, -30, 0, false);
        stack(-27, -26, 58, 59, -30, 4, true);
        stack(25, 26, 65, 66, -30, 3, true);
        post(-21, 54, 6, true);
        post(20, 54, 6, true);
        post(-21, 62, 6, true);
        post(20, 62, 6, true);
        post(-6, 58, 4, true);                            // street lamp in the middle of the centre lane
        lineX(-20, 19, 54, 6);
        sheetX(-20, -16, 54, 1, 6, linen);
        sheetX(-12, -8, 54, 1, 6, pastel);
        sheetX(-5, -3, 54, 1, 6, linen);
        sheetX(-1, 2, 54, 3, 6, pastel);                  // head-hitter towel over the chimney 3-gap (feet 0)
        sheetX(7, 10, 54, 1, 6, linen);
        sheetX(14, 18, 54, 1, 6, pastel);
        lineX(-20, 19, 60, 6);
        sheetX(-18, -14, 60, 1, 6, pastel);
        sheetX(-10, -7, 60, 1, 6, linen);
        sheetX(9, 13, 60, 1, 6, linen);
        sheetX(16, 19, 60, 2, 6, pastel);
        lineX(8, 19, 57, 5);
        sheetX(11, 15, 57, 1, 5, linen);
        post(8, 57, 5, false);
        lineX(5, 19, 65, 5);
        sheetX(6, 9, 65, 1, 5, pastel);
        sheetX(14, 17, 65, 1, 5, linen);
        post(5, 64, 5, false);
        post(-11, 49, 5, false);
        post(-11, 68, 5, false);
        sheetZ(-11, 50, 52, 2, 6, linen);
        sheetZ(-11, 63, 66, 1, 6, pastel);
    }

    // ------------------------------------------------------------------ scenery

    private void neighbours() {
        int[][] rows = {{-8, 12}, {37, 48}, {69, 104}};
        for (int r = 0; r < rows.length; r++) {
            int z1 = rows[r][0], z2 = rows[r][1];
            for (int side = -1; side <= 1; side += 2) {
                int x = side < 0 ? -27 : 26;
                if (r == 1) x = side < 0 ? -25 : 24;
                int i = 0;
                while (Math.abs(x) < 46) {
                    int w = 6 + (int) (g.rnd(i, r, side, 5) * 5);
                    int xa = side < 0 ? x - w + 1 : x, xb = side < 0 ? x : x + w - 1;
                    int top = 2 + (int) (g.rnd(i, r, side, 6) * 9) + (Math.abs(x) < 35 ? 3 : 0);
                    house(xa, xb, z1, z2, top, i + r * 10 + (side + 1) * 100, Math.abs(x) < 34 ? -8 : top - 6);
                    x += side * (w + (g.rnd(i, r, side, 9) < 0.2 ? 1 : 0));
                    i++;
                }
            }
        }
        // back rows behind the start and beyond the goal, and between streets far out (massing only)
        for (int z : new int[]{-26, 112}) {
            int x = -64;
            int i = 0;
            while (x < 64) {
                int w = 7 + (int) (g.rnd(i, z, 0, 15) * 6);
                int top = -2 + (int) (g.rnd(i, z, 1, 15) * 10) + (z > 100 ? (int) ((z - 100) * 0.15) : 0);
                house(x, x + w - 1, z, z + 12, top, i + z * 7, top - 5);
                x += w;
                i++;
            }
        }
    }

    /** Scenery house: hollow brick box with windows, a flat or pitched slate roof and chimneys. */
    private void house(int x1, int x2, int z1, int z2, int top, int salt, int bottom) {
        Pal w = g.rnd(salt, 1, 1, 3) < 0.25 ? brick2 : g.rnd(salt, 1, 2, 3) < 0.2 ? stone : brick;
        for (int y = bottom; y < top; y++) {
            for (int x = x1; x <= x2; x++) { wallCell(x, y, z1, x, w); wallCell(x, y, z2, x, w); }
            for (int z = z1; z <= z2; z++) { wallCell(x1, y, z, z, w); wallCell(x2, y, z, z, w); }
        }
        g.fill(x1, bottom - 1, z1, x2, bottom - 1, z2, soot);
        if (bottom < -6) g.underside(x1, z1, x2, z2, bottom - 1, 3, rubble, salt);
        boolean pitched = g.rnd(salt, 2, 2, 3) < 0.6;
        if (pitched) {
            // ridge along x (over the depth z)
            int depth = z2 - z1 + 1, half = depth / 2;
            for (int x = x1; x <= x2; x++) for (int i = 0; i < half; i++) {
                int y = top + i / 2;
                boolean stairRow = i % 2 == 0;
                g.set(x, y, z1 + i, stairRow ? Gen.st("deepslate_tile", "south", false) : "deepslate_tiles");
                g.set(x, y, z2 - i, stairRow ? Gen.st("deepslate_tile", "north", false) : "deepslate_tiles");
                if (i > 0) { g.fill(x, top, z1 + i, x, y - 1, z1 + i, "bricks"); g.fill(x, top, z2 - i, x, y - 1, z2 - i, "bricks"); }
            }
            int ry = top + (half - 1) / 2;
            for (int x = x1; x <= x2; x++) for (int z = z1 + half; z <= z2 - half; z++) g.set(x, ry + (depth % 2 == 1 ? 0 : 0), z, "polished_deepslate");
        } else {
            g.fill(x1, top, z1, x2, top, z2, deck);
            for (int x = x1; x <= x2; x++) { g.set(x, top + 1, z1, "deepslate_tile_slab[type=bottom]"); g.set(x, top + 1, z2, "deepslate_tile_slab[type=bottom]"); }
        }
        // chimneys on the ridge line
        int cz = (z1 + z2) / 2;
        int ch = top + 3 + (int) (g.rnd(salt, 3, 3, 3) * 3);
        if (g.rnd(salt, 4, 4, 3) < 0.8) stack(x1 + 1, x1 + 2, cz, cz + 1, top, ch, true);
        if (x2 - x1 > 8 && g.rnd(salt, 5, 5, 3) < 0.6) stack(x2 - 2, x2 - 1, cz, cz + 1, top, ch - 1, true);
        if (g.rnd(salt, 6, 6, 3) < 0.35) g.set(x1 + 3, top + 1, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
    }

    private void wallCell(int x, int y, int z, int u, Pal w) {
        int fl = Math.floorMod(y + 22, 6);
        int col = Math.floorMod(u, 4);
        String s;
        if ((fl == 2 || fl == 3) && col == 1) s = "glass_pane";
        else if (fl == 4 && col == 1) s = "calcite";
        else if (fl == 1 && col == 1) s = "smooth_stone";
        else if (fl == 5) s = "stone_bricks";
        else s = w.at(x, y, z, g.seed);
        g.set(x, y, z, s);
    }

    private void skyline() {
        // church spire beyond the goal on the right
        int cx = 38, cz = 174;
        g.fill(cx - 4, -20, cz - 4, cx + 4, 22, cz + 4, stone);
        g.fill(cx - 3, 23, cz - 3, cx + 3, 26, cz + 3, "stone_bricks");
        for (int y = 27; y <= 52; y++) {
            double r = 3.6 * (1 - (y - 27) / 26.0);
            g.cyl(cx + 0.5, cz + 0.5, Math.max(0.6, r), y, y, slate);
        }
        g.set(cx, 53, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        for (int y = 14; y <= 18; y++) g.set(cx, y, cz - 5, y == 16 ? "glowstone" : "calcite");
        // gasholder frame on the left
        g.ring(-46.5, 170.5, 11, 1, -10, 18, new Pal("air", 7, "iron_bars", 3));
        for (int a = 0; a < 12; a++) {
            double t = a * Math.PI / 6;
            int x = (int) Math.floor(-46.5 + 10.5 * Math.cos(t)), z = (int) Math.floor(136.5 + 10.5 * Math.sin(t));
            g.fill(x, -14, z, x, 20, z, "polished_deepslate_wall");
            g.set(x, 21, z, "lantern[hanging=false,waterlogged=false]");
        }
        g.cyl(-46.5, 170.5, 10.2, 6, 6, new Pal("deepslate_tiles", 1));
        // a few factory chimneys far out
        for (int[] c : new int[][]{{-70, 60}, {72, 30}, {-60, 160}, {64, 170}}) {
            g.cyl(c[0] + 0.5, c[1] + 0.5, 2.2, -20, 34, brick2);
            g.ring(c[0] + 0.5, c[1] + 0.5, 2.6, 1, 30, 31, new Pal("stone_bricks", 1));
        }
    }

    // ------------------------------------------------------------------ start, pen, landmark

    private void startStrip(TgMapData d) {
        for (int x = -18; x <= 17; x++) for (int z = 0; z <= 6; z++)
            g.set(x, 0, z, ((x + z) & 1) == 0 ? "polished_blackstone" : "polished_deepslate");
        for (int x = -18; x <= 17; x++) {
            g.set(x, 0, 7, (x & 1) == 0 ? "smooth_quartz" : "polished_blackstone");
            for (int y = 1; y <= 3; y++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new Pos(x, y, 7)); }
        }
        for (int z = -1; z <= 7; z++) for (int y = 1; y <= 3; y++) {
            g.set(-19, y, z, y == 3 ? "deepslate_tile_slab[type=bottom]" : brick.at(-19, y, z, g.seed));
            g.set(18, y, z, y == 3 ? "deepslate_tile_slab[type=bottom]" : brick.at(18, y, z, g.seed));
        }
        for (int z = -1; z <= 7; z++) { g.set(-19, 3, z, "bricks"); g.set(18, 3, z, "bricks"); g.set(-19, 4, z, "deepslate_tile_slab[type=bottom]"); g.set(18, 4, z, "deepslate_tile_slab[type=bottom]"); }
        for (int x = -19; x <= 18; x++) for (int y = 1; y <= 3; y++) g.set(x, y, -1, y == 3 ? "stone_bricks" : brick.at(x, y, -1, g.seed));
        for (int x = -19; x <= 18; x++) g.set(x, 4, -1, "deepslate_tile_slab[type=bottom]");
        // lanterns on the back wall
        for (int x = -16; x <= 16; x += 8) g.set(x, 4, -1, "lantern[hanging=false,waterlogged=false]");
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        for (int row = 0; row < 3; row++)
            for (int i = 0; i < 20; i++) d.startSpawns.add(new Spawn(-17.5 + i * 1.8 + (row % 2) * 0.6, 1, 1.5 + row * 2, 0f));
    }

    /** Feedback 3: the finish is a row of goal chickens across roof C (was a 13 x 13 planter pen in the middle). */
    private void pen(TgMapData d) {
        com.mcm.games.tgttos.model.FinishRow.build(d, -15, 15, 0, 78, 1, "polished_deepslate");
    }

    private void tower(TgMapData d, Design de) {
        int x1 = -5, x2 = 5, z1 = 92, z2 = 102;
        for (int y = 1; y <= 22; y++) for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (!edge) continue;
            boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
            g.set(x, y, z, corner ? "stone_bricks" : (y % 7 == 0 ? "stone_bricks" : brick.at(x, y, z, g.seed)));
        }
        // clock face toward the start
        for (int x = -3; x <= 3; x++) for (int y = 14; y <= 20; y++) {
            double dx = x, dy = y - 17;
            double rr = Math.sqrt(dx * dx + dy * dy);
            if (rr <= 3.4) g.set(x, y, z1, rr > 2.6 ? "polished_deepslate" : "smooth_quartz");
        }
        g.set(0, 17, z1 - 1, "polished_deepslate_wall");
        g.set(0, 18, z1 - 1, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        g.set(1, 17, z1 - 1, "lightning_rod[facing=east,powered=false,waterlogged=false]");
        g.set(0, 17, z1, "gold_block");
        // windows lit
        for (int y = 4; y <= 10; y += 6) for (int x = -2; x <= 2; x += 2) { g.set(x, y, z1, "glass_pane"); g.set(x, y + 1, z1, "glass_pane"); g.set(x, y, z1 + 1, "glowstone"); g.set(x, y + 1, z1 + 1, "glowstone"); }
        // cornice + copper cupola
        for (int x = x1 - 1; x <= x2 + 1; x++) for (int z = z1 - 1; z <= z2 + 1; z++) {
            boolean edge = x == x1 - 1 || x == x2 + 1 || z == z1 - 1 || z == z2 + 1;
            if (edge) g.set(x, 23, z, "stone_brick_slab[type=bottom]");
            else g.set(x, 23, z, "stone_bricks");
        }
        for (int y = 24; y <= 31; y++) {
            double r = 5.2 - (y - 24) * 0.62;
            g.cyl(0.5, 97.5, r, y, y, new Pal("oxidized_cut_copper", 50, "weathered_cut_copper", 30, "oxidized_copper", 20));
        }
        // open belfry: arches through the cupola base
        g.fill(-4, 24, 97, 4, 25, 97, "air");
        g.fill(0, 24, 93, 0, 25, 101, "air");
        g.set(0, 26, 97, "chain[axis=y]");
        g.set(0, 25, 97, "bell[attachment=ceiling,facing=north,powered=false]");
        // vane pole and the golden cockerel (profile faces the start)
        for (int y = 32; y <= 36; y++) g.set(0, y, 97, "polished_deepslate_wall");
        for (int x = -3; x <= 3; x++) g.set(x, 34, 97, "chain[axis=x]");
        g.set(0, 34, 94, "chain[axis=z]"); g.set(0, 34, 95, "chain[axis=z]"); g.set(0, 34, 96, "chain[axis=z]");
        g.set(0, 34, 98, "chain[axis=z]"); g.set(0, 34, 99, "chain[axis=z]"); g.set(0, 34, 100, "chain[axis=z]");
        String[] cock = {
                "....RR....",
                "...RGGR...",
                "...GGGGO..",
                "GG.GGGG...",
                "GGGGGGGG..",
                ".GGGGGGG..",
                "..GGGGG...",
                "....G.....",
                "...GGG...."};
        int top = 46;
        for (int r = 0; r < cock.length; r++) for (int c = 0; c < cock[r].length(); c++) {
            char ch = cock[r].charAt(c);
            if (ch == '.') continue;
            int x = 5 - c, y = top - r;
            g.set(x, y, 97, ch == 'R' ? "red_terracotta" : ch == 'O' ? "orange_terracotta" : "gold_block");
            if (ch == 'G' && r == 4) de.landmark.add(new Pos(x, y, 97));
        }
        for (int y = 37; y <= 38; y++) g.set(0, y, 97, "polished_deepslate_wall");
        de.landmark.add(new Pos(0, 31, 97));
        de.landmark.add(new Pos(0, 17, 92));
    }
}
