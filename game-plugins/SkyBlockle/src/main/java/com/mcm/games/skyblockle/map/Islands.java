package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel.IslandType;

/**
 * Template builders for every island kind (our own designs on the researched structure). Each template is
 * built in its local frame (see {@link Tpl}): standing surface at y = 0, front (towards the castle) = north (-z).
 * Contents follow research/skyblockle_maps.md ("Image-search pass"); anything not seen there is kept minimal.
 */
final class Islands {
    private Islands() {}

    static final String LEAF_OAK = "oak_leaves[distance=1,persistent=false,waterlogged=false]";
    static final String LEAF_BIRCH = "birch_leaves[distance=1,persistent=false,waterlogged=false]";

    interface Mask { double m(int x, int z); }   // 0 centre .. 1 edge, >= 1 outside

    /** Round-ish mask with a little edge noise. */
    static Mask round(double rx, double rz, double wobble, long seed) {
        return (x, z) -> {
            double d = Math.sqrt((x * x) / (rx * rx) + (z * z) / (rz * rz));
            return d + (Noise.value2(x * 0.45, z * 0.45, seed) - 0.5) * wobble;
        };
    }

    /**
     * Floating island body: surface block at {@code top}, {@code soilDepth} layers of soil, then rock tapering to a
     * point {@code depth} below the surface (deeper at the centre), with hanging drips.
     */
    static void blob(Tpl t, int r, Mask mask, int top, double depth, long seed,
                     Noise.Pal surface, Noise.Pal soil, int soilDepth, Noise.Pal rock) {
        for (int x = -r - 2; x <= r + 2; x++)
            for (int z = -r - 2; z <= r + 2; z++) {
                double m = mask.m(x, z);
                if (m >= 1) continue;
                double fall = Math.pow(Math.max(0, 1 - m), 0.9);
                int total = 1 + (int) Math.round((soilDepth + depth) * fall
                        + (Noise.value2(x * 0.7 + 3, z * 0.7, seed + 5) - 0.5) * 1.6);
                if (Noise.hash01(x, 7, z, seed + 9) < 0.14 * fall) total += 1 + (int) (Noise.hash01(x, 8, z, seed) * 3);
                int bottom = top - Math.max(1, total);
                for (int y = top; y >= bottom; y--) {
                    String b;
                    if (y == top) b = surface.at(x, y, z, seed);
                    else if (y >= top - soilDepth) b = soil.at(x, y, z, seed + 1);
                    else b = rock.at(x, y, z, seed + 2);
                    t.set(x, y, z, b);
                }
            }
    }

    static void oakTree(Tpl t, int x, int y0, int z, int h, long seed, String log, String leaf) {
        int topY = y0 + h - 1;
        for (int y = topY - 2; y <= topY + 1; y++) {
            int rad = y <= topY - 1 ? 2 : 1;
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++) {
                    boolean corner = Math.abs(dx) == rad && Math.abs(dz) == rad;
                    if (corner && (rad == 1 || Noise.hash01(x + dx, y, z + dz, seed) < 0.55)) continue;
                    if (y == topY + 1 && corner) continue;
                    if (!t.has(x + dx, y, z + dz)) t.set(x + dx, y, z + dz, leaf);
                }
        }
        for (int y = y0; y <= topY; y++) t.set(x, y, z, log);
    }

    // ------------------------------------------------------------------ start island

    /**
     * Team start: a small grass/dirt island (our own rounded shape with one bitten corner, not an L), one oak
     * tree, one chest, nothing else (research: start = grass/dirt + one tree + one chest, no team colours).
     */
    static Tpl start(long seed) {
        Tpl t = new Tpl("start");
        Noise.Pal grass = new Noise.Pal("grass_block[snowy=false]", 1);
        Noise.Pal dirt = new Noise.Pal("dirt", 6, "coarse_dirt", 1);
        Noise.Pal under = new Noise.Pal("dirt", 4, "coarse_dirt", 2, "rooted_dirt", 1);
        Mask m = (x, z) -> {
            if (Math.abs(x) > 3 || Math.abs(z) > 3) return 2;
            if (Math.abs(x) == 3 && Math.abs(z) == 3) return 2;            // rounded corners
            if (x >= 2 && z >= 2) return 2;                                   // bitten back-right corner
            if (x == 3 && z == 1) return 2;
            return Math.sqrt(x * x + z * z) / 4.4;
        };
        blob(t, 3, m, 0, 3.2, seed, grass, dirt, 2, under);
        oakTree(t, -2, 1, 2, 5, seed, "oak_log[axis=y]", LEAF_OAK);
        t.chest(2, 1, -2, "west", IslandType.START);
        int[][] sp = {{0, -1}, {1, 0}, {-1, 0}, {0, 1}};
        for (int[] s : sp) t.spawns.add(new int[]{s[0], 1, s[1]});
        return t;
    }

    // ------------------------------------------------------------------ generator shrine

    /**
     * Cobblestone generator: a small roofed stone-brick shrine (~9 across, ~7 tall) with a sunken basin: lava
     * source, a cobblestone cell (pre-filled), a one-block dip that the water falls into, water source. Classic
     * flow-based generator; it needs fluid ticks (place with physics or tick the fluids once) to run.
     */
    static Tpl generator(long seed) {
        Tpl t = new Tpl("generator");
        Noise.Pal top = new Noise.Pal("stone_bricks", 5, "mossy_stone_bricks", 2, "cracked_stone_bricks", 1.5, "cobblestone", 1.2);
        Noise.Pal body = new Noise.Pal("cobblestone", 4, "stone", 3, "andesite", 2, "mossy_cobblestone", 1.5);
        Noise.Pal rock = new Noise.Pal("stone", 4, "andesite", 2, "cobblestone", 2, "tuff", 1);
        Mask m = (x, z) -> Math.sqrt(x * x + z * z) / 4.75;
        blob(t, 4, m, 0, 5.5, seed + 1, top, body, 1, rock);
        // rim course: polished andesite edge with a few slabs for a carved look
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 3.9 && d < 4.75 && t.has(x, 0, z)) t.set(x, 0, z, Noise.hash01(x, 0, z, seed) < 0.7 ? "polished_andesite" : "stone_bricks");
            }
        // basin: x -2..1 at z = 0
        t.set(-2, 0, 0, "lava[level=0]");
        t.set(-1, 0, 0, "cobblestone");
        t.set(0, 0, 0, null);
        t.set(0, -1, 0, null);
        t.set(1, 0, 0, "water[level=0]");
        t.set(0, -2, 0, "stone_bricks");
        for (int x = -3; x <= 2; x++) for (int z = -1; z <= 1; z += 2) t.set(x, 0, z, "chiseled_stone_bricks");
        t.set(-3, 0, 0, "chiseled_stone_bricks");
        t.set(2, 0, 0, "chiseled_stone_bricks");
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) if (!(x == 0 && z == 0)) t.set(x, -1, z, "stone_bricks");
        t.set(-2, -1, 0, "stone_bricks");
        t.set(1, -1, 0, "stone_bricks");
        // canopy posts and hip roof
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = 1; y <= 3; y++) t.set(3 * sx, y, 3 * sz, y == 1 ? "chiseled_stone_bricks" : "stone_brick_wall");
            }
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                int ax = Math.abs(x), az = Math.abs(z);
                if (ax == 4 && az == 4) continue;
                if (ax == 4 || az == 4) {
                    String f = az == 4 ? (z < 0 ? "south" : "north") : (x < 0 ? "east" : "west");
                    t.set(x, 4, z, Bs.stairs("stone_brick", f, false));
                } else t.set(x, 4, z, Noise.hash01(x, 4, z, seed) < 0.25 ? "mossy_stone_bricks" : "stone_bricks");
            }
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                int ax = Math.abs(x), az = Math.abs(z);
                if (ax == 3 && az == 3) continue;
                if (ax == 3 || az == 3) {
                    String f = az == 3 ? (z < 0 ? "south" : "north") : (x < 0 ? "east" : "west");
                    t.set(x, 5, z, Bs.stairs("stone_brick", f, false));
                } else if (ax <= 1 && az <= 1) t.set(x, 5, z, "stone_bricks");
                else t.set(x, 5, z, Bs.slab("stone_brick", false));
            }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) t.set(x, 6, z, Math.abs(x) + Math.abs(z) == 2 ? Bs.slab("stone_brick", false) : "chiseled_stone_bricks");
        t.set(0, 7, 0, "stone_brick_wall");
        t.set(0, 8, 0, "lantern[hanging=false,waterlogged=false]");
        // hanging lantern over the basin, cobwebs in two roof corners
        t.set(0, 3, 0, "chain[axis=y,waterlogged=false]");
        t.set(0, 2, 0, "lantern[hanging=true,waterlogged=false]");
        t.set(3, 3, -2, "cobweb");
        t.set(-3, 3, 2, "cobweb");
        t.chest(2, 1, 2, "north", IslandType.GENERATOR);
        // hidden chest buried in the rock (research: skyblockle_video.md #5, "found while mining"). Position is
        // our choice: 3 blocks below the floor surface, off-centre behind the basin, so it is not under a fluid cell
        // and is never exposed on the tapered underside (the rock there is 4-5+ deep). Every face is solid rock.
        int hx = -1, hy = -3, hz = 2;
        int[][] around = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] d : around)
            if (!t.has(hx + d[0], hy + d[1], hz + d[2])) t.set(hx + d[0], hy + d[1], hz + d[2], rock.at(hx + d[0], hy + d[1], hz + d[2], seed + 2));
        t.chest(hx, hy, hz, "north", IslandType.GENERATOR, "hidden");
        genOres(t, seed, hx, hy, hz);
        return t;
    }

    /** Ore counts per generator island (DEFAULTS, NOT RESEARCHED: the video only shows that these ores exist). */
    static final int GEN_COAL_ORE = 4, GEN_IRON_ORE = 4, GEN_REDSTONE_ORE = 2;
    /** Plain rock blocks an ore may replace (the generator's rock/body palettes). */
    static final java.util.Set<String> GEN_ORE_HOST = java.util.Set.of("minecraft:stone", "minecraft:andesite",
            "minecraft:cobblestone", "minecraft:tuff", "minecraft:mossy_cobblestone");

    /**
     * Ores embedded in the generator rock (research: skyblockle_video.md #5 / line 113: coal, iron and redstone ore
     * in the generator island's rock, seen while mining). Each ore replaces a plain rock block that is fully
     * enclosed (all 6 neighbours are blocks, so nothing changes outside), at y -5..-2 (below the basin), not in a
     * column under the lava/water/drop cells, not the hidden chest or one of its 6 neighbours, and at least 2
     * blocks (Chebyshev) from every other ore. Picked by a seeded hash order from the template, so every wedge
     * gets the same ores.
     */
    static void genOres(Tpl t, long seed, int hx, int hy, int hz) {
        java.util.List<int[]> cand = new java.util.ArrayList<>();
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                if (z == 0 && x >= -2 && x <= 1) continue;               // under lava / cobble cell / drop / water
                for (int y = -5; y <= -2; y++) {
                    String b = t.get(x, y, z);
                    if (b == null || !GEN_ORE_HOST.contains(b)) continue;
                    if (Math.abs(x - hx) + Math.abs(y - hy) + Math.abs(z - hz) <= 1) continue;
                    boolean enclosed = t.has(x + 1, y, z) && t.has(x - 1, y, z) && t.has(x, y + 1, z) && t.has(x, y - 1, z)
                            && t.has(x, y, z + 1) && t.has(x, y, z - 1);
                    if (enclosed) cand.add(new int[]{x, y, z});
                }
            }
        cand.sort(java.util.Comparator.comparingDouble(c -> Noise.hash01(c[0], c[1], c[2], seed + 77)));
        String[] want = new String[GEN_COAL_ORE + GEN_IRON_ORE + GEN_REDSTONE_ORE];
        int k = 0;
        for (int i = 0; i < GEN_IRON_ORE; i++) want[k++] = "iron_ore";
        for (int i = 0; i < GEN_COAL_ORE; i++) want[k++] = "coal_ore";
        for (int i = 0; i < GEN_REDSTONE_ORE; i++) want[k++] = "redstone_ore[lit=false]";
        java.util.List<int[]> placed = new java.util.ArrayList<>();
        for (int spacing = 2; spacing >= 1 && placed.size() < want.length; spacing--)
            for (int[] c : cand) {
                if (placed.size() >= want.length) break;
                boolean ok = true;
                for (int[] p : placed)
                    if (Math.max(Math.abs(p[0] - c[0]), Math.max(Math.abs(p[1] - c[1]), Math.abs(p[2] - c[2]))) < spacing
                            || p == c) { ok = false; break; }
                if (ok) { placed.add(c); t.set(c[0], c[1], c[2], want[placed.size() - 1]); }
            }
        if (placed.size() < want.length)
            throw new IllegalStateException("generator rock too small for " + want.length + " ores, placed " + placed.size());
    }

    // ------------------------------------------------------------------ pillar

    /** 2x2 red column (~18 tall) on a small rock, chest on top (reached by towering up). */
    static Tpl pillar(long seed, int h) {
        Tpl t = new Tpl("pillar");
        t.pillar = true;
        Noise.Pal top = new Noise.Pal("stone", 4, "andesite", 2, "moss_block", 1.2, "cobblestone", 1);
        Noise.Pal rock = new Noise.Pal("stone", 4, "andesite", 2, "tuff", 1.5, "cobblestone", 1);
        blob(t, 2, (x, z) -> Math.sqrt((x + 0.5) * (x + 0.5) + (z + 0.5) * (z + 0.5)) / 2.9, 0, 4.5, seed + 3, top, rock, 1, rock);
        for (int y = 1; y <= h; y++)
            for (int x = -1; x <= 0; x++)
                for (int z = -1; z <= 0; z++) {
                    String b = (y % 5 == 0) ? "red_terracotta" : (y == 1 ? "polished_andesite" : "red_concrete");
                    if (y == h) b = "red_nether_bricks";
                    t.set(x, y, z, b);
                }
        t.chest(-1, h + 1, -1, "south", IslandType.MID);
        return t;
    }

    // ------------------------------------------------------------------ fort hut

    /**
     * Fort: open-sided timber hut (~9x9, 8 tall) on a ragged dark-stone base 11 wide, flat stone roof with
     * merlons and cobwebs, outside ladder to the roof, chest inside. Our own material mix (deepslate/tuff base,
     * spruce frame, deepslate-tile roof).
     */
    static Tpl fort(long seed) {
        Tpl t = new Tpl("fort");
        Noise.Pal top = new Noise.Pal("cobbled_deepslate", 3, "tuff", 2, "polished_tuff", 1, "deepslate", 1);
        Noise.Pal rock = new Noise.Pal("deepslate", 3, "cobbled_deepslate", 3, "tuff", 2, "andesite", 1);
        Mask m = (x, z) -> {
            double d = Math.max(Math.abs(x), Math.abs(z)) / 5.9;
            if (Math.abs(x) == 5 && Math.abs(z) == 5) d = 1.2;
            return d + (Noise.value2(x * 0.6, z * 0.6, seed + 4) - 0.5) * 0.12;
        };
        blob(t, 5, m, 0, 6.5, seed + 4, top, rock, 1, rock);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) if (Math.abs(x) < 5 && Math.abs(z) < 5) t.set(x, 0, z, "cobbled_deepslate");
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) t.set(x, 0, z, (x + z & 1) == 0 ? "spruce_planks" : "stripped_spruce_log[axis=x]");
        // corner posts
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            for (int y = 1; y <= 6; y++) t.set(4 * sx, y, 4 * sz, "stripped_spruce_log[axis=y]");
        // four sides: low plank walls, doorway in the middle, open windows above, beam on top
        for (int side = 0; side < 4; side++) {
            for (int s = -3; s <= 3; s++) {
                int x = Bs.rx(s, -4, side), z = Bs.rz(s, -4, side);
                boolean door = Math.abs(s) <= 1;
                for (int y = 1; y <= 2; y++) if (!door) t.set(x, y, z, "spruce_planks");
                if (Math.abs(s) == 2) for (int y = 3; y <= 5; y++) t.set(x, y, z, "spruce_fence");
                String axis = (side & 1) == 0 ? "x" : "z";
                t.set(x, 6, z, "stripped_spruce_log[axis=" + axis + "]");
                if (door) t.set(x, 5, z, Bs.slab("spruce", true));
            }
        }
        // east wall column at z = 3 carries the ladder
        for (int y = 1; y <= 5; y++) t.set(4, y, 3, "stripped_spruce_log[axis=y]");
        for (int y = 1; y <= 7; y++) t.set(5, y, 3, "ladder[facing=east,waterlogged=false]");
        t.climbs.add(new int[]{5, 1, 3, 8});
        // roof
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            t.set(x, 7, z, (Math.abs(x) == 4 || Math.abs(z) == 4) ? "polished_tuff" : Noise.hash01(x, 7, z, seed) < 0.6 ? "deepslate_tiles" : "polished_deepslate");
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (Math.abs(x) != 4 && Math.abs(z) != 4) continue;
            boolean merlon = ((x + z) & 1) == 0;
            if (x == 4 && z == 3) merlon = false;                       // ladder exit
            if (merlon) t.set(x, 8, z, "deepslate_bricks");
            if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                t.set(x, 8, z, "polished_tuff");
                t.set(x, 9, z, "lantern[hanging=false,waterlogged=false]");
            }
        }
        t.set(-2, 8, 2, "cobweb");
        t.set(2, 8, -2, "cobweb");
        t.set(0, 6, 0, "chain[axis=y,waterlogged=false]");
        t.set(0, 5, 0, "lantern[hanging=true,waterlogged=false]");
        t.chest(-3, 1, -3, "east", IslandType.FORT);
        return t;
    }

    // ------------------------------------------------------------------ spike islets

    /** Small sand (or red terracotta) islet with a short block stack and a chest. */
    static Tpl spike(long seed, boolean red) {
        Tpl t = new Tpl(red ? "spike_red" : "spike_sand");
        Noise.Pal top = red ? new Noise.Pal("red_sand", 3, "smooth_red_sandstone", 1)
                : new Noise.Pal("sand", 3, "smooth_sandstone", 1);
        Noise.Pal soil = red ? new Noise.Pal("red_sandstone", 1) : new Noise.Pal("sandstone", 1);
        Noise.Pal rock = red ? new Noise.Pal("red_terracotta", 3, "terracotta", 2, "orange_terracotta", 1, "red_sandstone", 1)
                : new Noise.Pal("sandstone", 3, "smooth_sandstone", 1, "yellow_terracotta", 1, "cut_sandstone", 1);
        blob(t, 2, (x, z) -> Math.sqrt(x * x + z * z) / 2.6, 0, 3.8, seed + (red ? 7 : 6), top, soil, 1, rock);
        String ch = red ? "chiseled_red_sandstone" : "chiseled_sandstone";
        String mid = red ? "red_terracotta" : "cut_sandstone";
        String wall = red ? "red_sandstone_wall" : "sandstone_wall";
        t.set(0, 1, 1, ch);
        t.set(0, 2, 1, mid);
        t.set(0, 3, 1, ch);
        t.set(0, 4, 1, wall);
        t.set(0, 5, 1, "lantern[hanging=false,waterlogged=false]");
        t.chest(0, 1, -1, "north", IslandType.MID);
        return t;
    }

    // ------------------------------------------------------------------ inner decks

    /** Enchanting deck: round oak deck on a rock, log posts with lanterns, side rails, enchanting table + 4 bookshelves, anvil, chest. */
    static Tpl enchanting(long seed) {
        Tpl t = new Tpl("enchanting");
        Noise.Pal planks = new Noise.Pal("oak_planks", 1);
        Noise.Pal rock = new Noise.Pal("stone", 3, "cobblestone", 3, "mossy_cobblestone", 1.5, "andesite", 1);
        Mask m = (x, z) -> (Math.abs(x) == 3 && Math.abs(z) == 3) ? 2 : Math.sqrt(x * x + z * z) / 4.3;
        blob(t, 3, m, 0, 5, seed + 8, planks, new Noise.Pal("oak_planks", 1), 1, rock);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (!t.has(x, 0, z)) continue;
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3 || (Math.abs(x) == 2 && Math.abs(z) == 3);
            if (edge) t.set(x, 0, z, Math.abs(x) == 3 ? "stripped_oak_log[axis=z]" : "stripped_oak_log[axis=x]");
            t.set(x, -1, z, "spruce_planks");
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            t.set(3 * sx, 1, 2 * sz, "oak_log[axis=y]");
            t.set(3 * sx, 2, 2 * sz, "oak_log[axis=y]");
            t.set(3 * sx, 3, 2 * sz, "lantern[hanging=false,waterlogged=false]");
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int z = -1; z <= 1; z++) t.set(3 * sx, 1, z, "oak_fence");
        t.set(0, 1, 1, "enchanting_table");
        t.set(-2, 1, 1, "bookshelf");
        t.set(2, 1, 1, "bookshelf");
        t.set(-2, 1, 2, "bookshelf");
        t.set(2, 1, 2, "bookshelf");
        t.set(-2, 1, -2, "cobweb");
        // anvil (research §2/§7, H: the deck has one, "Repair & Name" seen): beside the chest, off the x -1..1 walkway,
        // not between the table and a bookshelf, on the plank floor
        t.set(2, 1, -1, "anvil[facing=north]");
        t.chest(1, 1, -2, "west", IslandType.ENCHANTING);
        return t;
    }

    /** Brewing deck: pale calcite/diorite islet, brewing stand, cauldron, azalea and moss, chest. */
    static Tpl brewing(long seed) {
        Tpl t = new Tpl("brewing");
        Noise.Pal top = new Noise.Pal("polished_diorite", 3, "calcite", 3, "diorite", 1);
        Noise.Pal soil = new Noise.Pal("calcite", 3, "diorite", 1);
        Noise.Pal rock = new Noise.Pal("calcite", 3, "diorite", 2, "light_blue_terracotta", 1.5, "white_terracotta", 1);
        blob(t, 3, (x, z) -> Math.sqrt(x * x + z * z) / 3.4, 0, 4.8, seed + 9, top, soil, 1, rock);
        t.set(0, 1, 0, "brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]");
        t.set(1, 1, 1, "cauldron");
        t.set(-1, 1, 1, null);
        t.chest(-1, 1, 1, "south", IslandType.BREWING);
        t.set(2, 1, -1, "flowering_azalea");
        t.set(-2, 1, -1, "moss_carpet");
        t.set(-2, 1, 0, "moss_carpet");
        t.set(1, 1, 2, "moss_carpet");
        t.set(2, 0, -1, "moss_block");
        t.set(-2, 1, 2, "diorite_wall");
        t.set(-2, 2, 2, "lantern[hanging=false,waterlogged=false]");
        return t;
    }

    // ------------------------------------------------------------------ shared grass island

    /** Shared island between two teams: a round grass blob (clearly not a start shape), birch tree, flowers, cauldron, chest. */
    static Tpl grass(long seed) {
        Tpl t = new Tpl("grass");
        Noise.Pal grass = new Noise.Pal("grass_block[snowy=false]", 1);
        Noise.Pal dirt = new Noise.Pal("dirt", 5, "coarse_dirt", 1);
        Noise.Pal rock = new Noise.Pal("dirt", 3, "stone", 3, "andesite", 1, "cobblestone", 1);
        blob(t, 4, round(4.6, 4.6, 0.18, seed + 11), 0, 5.5, seed + 11, grass, dirt, 2, rock);
        oakTree(t, 1, 1, 1, 6, seed + 12, "birch_log[axis=y]", LEAF_BIRCH);
        String[] fl = {"dandelion", "poppy", "cornflower", "short_grass", "short_grass", "oxeye_daisy"};
        int i = 0;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (!"minecraft:grass_block[snowy=false]".equals(t.get(x, 0, z)) || t.has(x, 1, z)) continue;
            if (Noise.hash01(x, 1, z, seed + 13) < 0.22) t.set(x, 1, z, fl[(i++) % fl.length]);
        }
        t.set(-2, 1, -1, "cauldron");
        t.set(2, 1, -2, null);
        t.chest(2, 1, -2, "west", IslandType.MID);
        return t;
    }

    /** Small stepping rock (no chest), used where a shared island would otherwise be out of bridging range. */
    static Tpl islet(long seed) {
        Tpl t = new Tpl("islet");
        Noise.Pal top = new Noise.Pal("moss_block", 2, "stone", 2, "cobblestone", 1, "grass_block[snowy=false]", 1);
        Noise.Pal rock = new Noise.Pal("stone", 3, "andesite", 2, "cobblestone", 1, "dirt", 1);
        blob(t, 1, (x, z) -> Math.sqrt(x * x + z * z) / 1.9, 0, 2.8, seed + 14, top, rock, 1, rock);
        return t;
    }

    // ------------------------------------------------------------------ scenery

    /** Rough boulder on a thin stalk behind each generator (scenery; plain stone, no ores, no chest). */
    static Tpl boulder(long seed) {
        Tpl t = new Tpl("boulder");
        Noise.Pal top = new Noise.Pal("stone", 3, "moss_block", 2, "andesite", 1, "grass_block[snowy=false]", 1);
        Noise.Pal rock = new Noise.Pal("stone", 4, "andesite", 2, "tuff", 1.5, "cobblestone", 1);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) for (int y = -9; y <= 1; y++) {
            double nz = (Noise.value2(x * 0.5, z * 0.5 + y * 0.3, seed + 15) - 0.5) * 0.35;
            double e = Math.sqrt(x * x / 20.0 + z * z / 16.0 + (y + 3) * (y + 3) / 14.0) + nz;
            if (e < 1) t.set(x, y, z, y == 1 || !inside(x, y + 1, z, seed) ? top.at(x, y, z, seed) : rock.at(x, y, z, seed));
        }
        for (int y = -16; y <= -7; y++) {
            int w = y < -12 ? 0 : 1;
            for (int x = -w; x <= 0; x++) for (int z = -w; z <= 0; z++) t.set(x, y, z, rock.at(x, y, z, seed + 1));
        }
        t.set(0, -17, 0, "pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]");
        return t;
    }

    private static boolean inside(int x, int y, int z, long seed) {
        double nz = (Noise.value2(x * 0.5, z * 0.5 + y * 0.3, seed + 15) - 0.5) * 0.35;
        return Math.sqrt(x * x / 20.0 + z * z / 16.0 + (y + 3) * (y + 3) / 14.0) + nz < 1;
    }

    /** Big stone asteroid at the rim between two teams, with natural ore patches (coal, iron, copper). */
    static Tpl asteroid(long seed) {
        Tpl t = new Tpl("asteroid");
        int rx = 8, rz = 7;
        for (int x = -rx - 1; x <= rx + 1; x++) for (int z = -rz - 1; z <= rz + 1; z++) {
            double d = Math.sqrt((double) x * x / (rx * rx + 0.5) + (double) z * z / (rz * rz + 0.5))
                    + (Noise.value2(x * 0.3, z * 0.3, seed + 16) - 0.5) * 0.25;
            if (d >= 1) continue;
            int top = (int) Math.round((1 - d) * 2.2 + (Noise.value2(x * 0.4 + 9, z * 0.4, seed + 17) - 0.5) * 2.5);
            int bottom = -(int) Math.round(3 + 10 * Math.pow(1 - d, 0.6) + Noise.hash01(x, 0, z, seed) * 2);
            for (int y = top; y >= bottom; y--) {
                boolean deep = y < -5;
                double ore = Noise.value2(x * 0.55 + y * 0.4, z * 0.55 - y * 0.3, seed + 18);
                String b;
                if (y == top) b = Noise.hash01(x, y, z, seed + 19) < 0.45 ? "moss_block" : (Noise.hash01(x, y, z, seed + 20) < 0.3 ? "andesite" : "stone");
                else if (ore > 0.86) b = deep ? "deepslate_iron_ore" : "iron_ore";
                else if (ore < 0.1) b = deep ? "deepslate_coal_ore" : "coal_ore";
                else if (ore > 0.78 && ore < 0.8) b = "copper_ore";
                else if (deep) b = Noise.hash01(x, y, z, seed + 21) < 0.6 ? "deepslate" : "cobbled_deepslate";
                else b = new Noise.Pal("stone", 5, "andesite", 2, "tuff", 1, "cobblestone", 1).at(x, y, z, seed + 22);
                t.set(x, y, z, b);
            }
        }
        return t;
    }

    /** Rock base for a showpiece. */
    private static void showBase(Tpl t, long seed) {
        Noise.Pal top = new Noise.Pal("grass_block[snowy=false]", 3, "moss_block", 1, "stone", 1);
        Noise.Pal soil = new Noise.Pal("dirt", 3, "stone", 1);
        Noise.Pal rock = new Noise.Pal("stone", 4, "andesite", 2, "cobblestone", 1, "tuff", 1);
        blob(t, 5, round(5.4, 5.4, 0.2, seed), 0, 6, seed, top, soil, 1, rock);
    }

    /** Showpiece A: giant hourglass (oak frame, glass bulbs, sand). */
    static Tpl hourglass(long seed) {
        Tpl t = new Tpl("show_hourglass");
        showBase(t, seed + 30);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (x * x + z * z > 11) continue;
            t.set(x, 1, z, "spruce_planks");
            t.set(x, 15, z, "spruce_planks");
            if (x * x + z * z > 7) { t.set(x, 1, z, "stripped_spruce_log[axis=y]"); t.set(x, 15, z, "stripped_spruce_log[axis=y]"); }
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            for (int y = 2; y <= 14; y++) t.set(2 * sx, y, 2 * sz, y == 2 || y == 14 ? "spruce_planks" : "spruce_fence");
        for (int y = 2; y <= 14; y++) {
            double rr = y <= 8 ? 2.4 - (y - 2) * 0.35 : 0.3 + (y - 8) * 0.35;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                double r = Math.sqrt(x * x + z * z);
                if (r > rr + 0.5) continue;
                boolean shell = r > rr - 0.6;
                boolean sand = !shell && (y <= 4 || y >= 11);
                t.set(x, y, z, shell ? "glass" : sand ? "sand" : null);
            }
        }
        t.set(0, 8, 0, "glass");
        t.set(0, 16, 0, "lantern[hanging=false,waterlogged=false]");
        return t;
    }

    /** Showpiece B: a giant pickaxe stuck handle-first into a rock. */
    static Tpl pickaxe(long seed) {
        Tpl t = new Tpl("show_pickaxe");
        showBase(t, seed + 40);
        for (int y = -1; y <= 13; y++) for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++)
            t.set(x, y, z, (y % 4 == 3) ? "dark_oak_planks" : "stripped_birch_log[axis=y]");
        for (int i = -14; i <= 14; i++) {
            double tt = i / 14.0;
            int x = (int) Math.round(tt * 7) + (i >= 0 ? 1 : 0);
            int y = 14 - (int) Math.round(3.5 * tt * tt);
            for (int dy = 0; dy <= 1; dy++) for (int z = 0; z <= 1; z++) {
                boolean tip = Math.abs(i) >= 12;
                String b = tip ? "white_concrete" : dy == 1 ? "light_blue_concrete" : "cyan_terracotta";
                t.set(x, y + dy, z, b);
            }
        }
        for (int x = -1; x <= 2; x++) for (int z = -1; z <= 2; z++) for (int y = 13; y <= 16; y++)
            if ((x == -1 || x == 2 || z == -1 || z == 2) && y >= 14 && y <= 15) t.set(x, y, z, "dark_oak_planks");
        t.set(-2, 1, 2, "cobblestone");
        t.set(3, 1, -1, "mossy_cobblestone");
        t.set(-1, 1, -2, "cobblestone_slab[type=bottom,waterlogged=false]");
        return t;
    }

    /** Showpiece C: a big oak in a brick planter. */
    static Tpl planter(long seed) {
        Tpl t = new Tpl("show_planter");
        showBase(t, seed + 50);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            double r = Math.sqrt(x * x + z * z);
            if (r > 3.4) continue;
            boolean rim = r > 2.4;
            t.set(x, 1, z, rim ? "bricks" : "podzol[snowy=false]");
            if (rim) t.set(x, 2, z, Bs.slab("brick", false));
        }
        for (int y = 2; y <= 10; y++) for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++) t.set(x, y, z, "oak_log[axis=y]");
        int[][] br = {{3, 8, 0}, {-2, 9, 1}, {1, 9, 3}, {0, 9, -2}};
        for (int[] b : br) {
            int sx = Integer.signum(b[0]), sz = Integer.signum(b[2]);
            for (int k = 1; k <= Math.max(Math.abs(b[0]), Math.abs(b[2])); k++)
                t.set((sx > 0 ? 1 : 0) + sx * k, b[1], (sz > 0 ? 1 : 0) + sz * k, "oak_log[axis=" + (sx != 0 ? "x" : "z") + "]");
        }
        for (int x = -5; x <= 6; x++) for (int z = -5; z <= 6; z++) for (int y = 8; y <= 15; y++) {
            double dx = x - 0.5, dz = z - 0.5, dy = (y - 11.5) * 1.25;
            double e = Math.sqrt(dx * dx + dz * dz + dy * dy) + (Noise.value2(x * 0.6 + y, z * 0.6, seed + 51) - 0.5) * 1.4;
            if (e < 5.2 && !t.has(x, y, z)) t.set(x, y, z, LEAF_OAK);
        }
        t.set(-2, 2, -1, "short_grass");
        t.set(2, 2, -2, "fern");
        return t;
    }
}
