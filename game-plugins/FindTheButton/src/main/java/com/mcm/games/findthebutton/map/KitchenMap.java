package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/** Farmhouse kitchen with a walk-in pantry (north-east), a brick bread oven, an island and a dining table. */
public final class KitchenMap extends FtbMap {
    public KitchenMap() { super(6); }

    @Override public String id() { return "kitchen"; }
    @Override public String displayName() { return "Kitchen"; }
    @Override public String tagline() { return "Something's cooking... and something's hidden"; }

    @Override
    protected void generate() {
        Pal floor = checker("minecraft:white_concrete", "minecraft:gray_concrete");
        Pal wall = (x, y, z) -> {
            int along = (x == 0 || x == 21) ? z : x;
            if (along == 0 || along == 21) return "minecraft:stripped_spruce_log[axis=y]";
            if (y == 1) return "minecraft:spruce_planks";
            if (y <= 3) return hash(x, y, z, 21) < 0.25 ? "minecraft:polished_diorite" : "minecraft:calcite";
            if (y == height) return "minecraft:stripped_spruce_wood[axis=y]";
            return "minecraft:white_terracotta";
        };
        shell(floor, wall, mix(4, "minecraft:spruce_planks", 3, "minecraft:stripped_spruce_wood[axis=x]", 1));
        // ceiling beams (y = 6) with pot-rack chains
        for (int x = 1; x <= 20; x++) { set(x, 6, 7, log("minecraft:stripped_spruce_log", 'x')); set(x, 6, 14, log("minecraft:stripped_spruce_log", 'x')); }
        hangingLantern(4, 5, 14, 5);
        hangingLantern(16, 5, 14, 5);
        hangingLantern(3, 5, 7, 5);

        // ---- pantry walls (x = 14, z = 9), doorway at x = 17..18
        fill(14, 1, 1, 14, 5, 9, "minecraft:spruce_planks");
        fill(15, 1, 9, 20, 5, 9, "minecraft:spruce_planks");
        fill(17, 1, 9, 18, 2, 9, (String) null);
        for (int y = 1; y <= 5; y++) { set(14, y, 9, "minecraft:stripped_spruce_log[axis=y]"); set(16, y, 9, "minecraft:stripped_spruce_log[axis=y]"); set(19, y, 9, "minecraft:stripped_spruce_log[axis=y]"); }
        // pantry: barrel shelves along the north wall, one slot empty
        fill(15, 1, 1, 20, 2, 1, "minecraft:barrel[facing=south]");
        set(17, 2, 1, null);
        spot("empty slot in the pantry shelf", "medium", 17, 2, 1, NORTH);
        fill(15, 3, 1, 20, 3, 1, slab("minecraft:spruce", false));
        // sacks of flour (hay) along the east wall, button tucked behind one
        set(19, 1, 7, "minecraft:hay_block[axis=y]");
        set(20, 1, 8, "minecraft:hay_block[axis=y]");
        set(19, 1, 8, "minecraft:hay_block[axis=y]");
        set(19, 2, 8, "minecraft:hay_block[axis=x]");
        spot("behind the flour sacks", "medium", 20, 1, 7, EAST);
        set(15, 1, 5, "minecraft:barrel[facing=up]");
        set(15, 2, 5, "minecraft:decorated_pot");
        set(15, 1, 7, "minecraft:composter");

        // ---- counters along the north wall (x = 1..13): stove, sink, cupboards, fridge
        for (int x = 1; x <= 11; x++) set(x, 1, 1, hash(x, 1, 1, 22) < 0.4 ? "minecraft:barrel[facing=south]" : "minecraft:spruce_planks");
        set(4, 1, 1, "minecraft:smoker[facing=south,lit=false]");
        set(5, 1, 1, "minecraft:furnace[facing=south,lit=false]");
        set(8, 1, 1, "minecraft:water_cauldron[level=3]");
        set(11, 1, 1, "minecraft:barrel[facing=up]");
        set(2, 2, 1, "minecraft:potted_red_tulip");
        set(10, 2, 1, "minecraft:candle[candles=2,lit=true]");
        // stove hood over smoker + furnace
        fill(4, 4, 1, 5, 5, 2, "minecraft:polished_andesite");
        fill(4, 4, 2, 5, 4, 2, slab("minecraft:polished_andesite", true));
        set(4, 4, 2, "minecraft:polished_andesite");
        spot("under the stove hood", "medium", 4, 3, 2, UP);
        // upper cupboards (y = 4..5) except over the hood and the fridge corner; one cupboard is open
        for (int x = 1; x <= 9; x++) {
            if (x == 4 || x == 5) continue;
            set(x, 4, 1, "minecraft:spruce_planks");
            set(x, 5, 1, "minecraft:barrel[facing=south]");
        }
        set(6, 4, 1, null);
        spot("inside the open cupboard", "easy", 6, 4, 1, NORTH);
        // fridge (2 high) in the corner, button on top: climb the counter to see it
        fill(12, 1, 1, 13, 2, 1, "minecraft:white_concrete");
        set(12, 2, 2, "minecraft:iron_trapdoor[facing=south,half=top,open=true]");
        spot("on top of the fridge", "hard", 13, 3, 1, DOWN);

        // ---- spice shelf on the west wall (low bench, jars on top)
        fill(1, 1, 8, 1, 1, 12, "minecraft:stripped_spruce_wood[axis=z]");
        set(1, 2, 8, "minecraft:potted_dead_bush");
        set(1, 2, 10, "minecraft:decorated_pot");
        set(1, 2, 12, "minecraft:potted_fern");
        set(1, 2, 9, "minecraft:candle[candles=3,lit=true]");
        spot("between the spice jars", "medium", 1, 2, 11, DOWN);

        // ---- kitchen island (x = 6..10, z = 9..10), open shelf facing north
        fill(6, 1, 9, 10, 1, 10, mix(23, "minecraft:spruce_planks", 2, "minecraft:barrel[facing=up]", 1));
        fill(6, 2, 9, 10, 2, 10, slab("minecraft:polished_granite", false));
        set(8, 1, 9, null);
        spot("in the island's open shelf", "medium", 8, 1, 9, SOUTH);
        spot("under the beam over the island", "medium", 8, 5, 7, UP);

        // ---- dining table with chairs
        table(4, 13, 6, 15, "minecraft:dark_oak_fence", "minecraft:spruce");
        set(3, 1, 14, stairs("minecraft:dark_oak", "west", false));
        set(7, 1, 14, stairs("minecraft:dark_oak", "east", false));
        set(5, 3, 14, "minecraft:candle[candles=3,lit=true]");
        spot("under the dining table", "medium", 5, 1, 15, DOWN);

        // ---- brick bread oven in the south-east corner, opening faces north
        fill(17, 1, 18, 20, 3, 20, mix(24, "minecraft:bricks", 5, "minecraft:mud_bricks", 1));
        fill(18, 4, 19, 19, 6, 20, "minecraft:bricks");
        set(18, 2, 18, null);
        set(18, 2, 19, null);
        set(18, 1, 18, slab("minecraft:smooth_stone", true));
        spot("inside the bread oven", "hard", 18, 2, 19, SOUTH);

        // rug and a bin near the start
        for (int x = 9; x <= 12; x++) for (int z = 12; z <= 16; z++) set(x, 1, z, (x == 9 || x == 12 || z == 12 || z == 16) ? "minecraft:yellow_carpet" : "minecraft:orange_carpet");
        set(1, 1, 20, "minecraft:composter");

        lightGrid(4, 5);
        lightGrid(5, 2);
    }
}
