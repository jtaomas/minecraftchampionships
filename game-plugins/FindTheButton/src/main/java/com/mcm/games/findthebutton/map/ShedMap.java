package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/**
 * Garden shed (north-west corner) and a hedged back yard with a doghouse, pergola, raised flower bed, bird table,
 * compost bins and a small tree. The yard is open to the sky but closed off by an invisible barrier ceiling and
 * barrier walls above the hedge, so nobody can leave the map.
 */
public final class ShedMap extends FtbMap {
    public ShedMap() { super(7); }

    @Override public String id() { return "shed"; }
    @Override public String displayName() { return "Garden Shed"; }
    @Override public int previewCutY() { return 4; }
    @Override public String tagline() { return "Check the shed, the hedge and the doghouse"; }

    private static final String LEAVES = "minecraft:oak_leaves[persistent=true]";

    @Override
    protected void generate() {
        Pal grass = mix(31, "minecraft:grass_block", 8, "minecraft:moss_block", 1);
        Pal hedge = (x, y, z) -> {
            if (y >= 4) return "minecraft:barrier";
            if (y == 1 && ((x + z) % 4 == 0)) return "minecraft:oak_log[axis=y]";
            return hash(x, y, z, 32) < 0.3 ? "minecraft:azalea_leaves[persistent=true]" : LEAVES;
        };
        shell(grass, hedge, one("minecraft:barrier"));

        // ---- shed: x 0..10, z 0..9 (walls on x = 0, z = 0, x = 10, z = 9), floor planks, flat roof at y = 5
        Pal planks = mix(33, "minecraft:spruce_planks", 5, "minecraft:stripped_spruce_wood[axis=y]", 1);
        fill(1, 0, 1, 9, 0, 8, mix(34, "minecraft:spruce_planks", 3, "minecraft:dark_oak_planks", 1));
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x <= 10; x++) { set(x, y, 0, planks.at(x, y, 0)); set(x, y, 9, planks.at(x, y, 9)); }
            for (int z = 0; z <= 9; z++) { set(0, y, z, planks.at(0, y, z)); set(10, y, z, planks.at(10, y, z)); }
            set(0, y, 0, "minecraft:stripped_spruce_log[axis=y]"); set(10, y, 0, "minecraft:stripped_spruce_log[axis=y]");
            set(0, y, 9, "minecraft:stripped_spruce_log[axis=y]"); set(10, y, 9, "minecraft:stripped_spruce_log[axis=y]");
        }
        fill(0, 5, 0, 10, 5, 9, "minecraft:dark_oak_planks");
        for (int x = 0; x <= 11; x++) set(x, 5, 10, stairs("minecraft:dark_oak", "north", false));
        for (int z = 0; z <= 9; z++) set(11, 5, z, stairs("minecraft:dark_oak", "west", false));
        fill(5, 1, 9, 5, 2, 9, (String) null);                       // door opening
        set(10, 2, 4, "minecraft:glass_pane"); set(10, 3, 4, "minecraft:glass_pane");
        set(10, 2, 5, "minecraft:glass_pane"); set(10, 3, 5, "minecraft:glass_pane");
        wallTorch(4, 3, 10, "south");
        wallTorch(6, 3, 10, "south");

        // shed inside: workbench along the west wall, tool hooks, seed barrels, rafter, potting bench
        set(1, 1, 2, "minecraft:crafting_table");
        set(1, 1, 3, "minecraft:smithing_table");
        set(1, 1, 4, "minecraft:spruce_planks");
        set(1, 2, 4, "minecraft:potted_oak_sapling");
        set(1, 3, 2, "minecraft:chain[axis=y]");
        set(1, 3, 3, "minecraft:lightning_rod[facing=up]");
        set(0, 2, 6, null);
        set(-1, 2, 6, "minecraft:spruce_planks");
        spot("cubby in the shed wall", "medium", 0, 2, 6, WEST);
        set(9, 1, 1, "minecraft:barrel[facing=up]");
        set(9, 2, 1, "minecraft:barrel[facing=up]");
        set(9, 1, 2, "minecraft:barrel[facing=up]");
        set(8, 1, 1, "minecraft:barrel[facing=up]");
        spot("on the seed barrel", "easy", 8, 2, 1, DOWN);
        for (int x = 1; x <= 9; x++) set(x, 4, 4, log("minecraft:stripped_spruce_log", 'x'));
        hangingLantern(3, 3, 4, 3);
        spot("on the side of the shed rafter", "medium", 6, 4, 5, NORTH);
        table(6, 7, 8, 8, "minecraft:spruce_fence", "minecraft:spruce");
        spot("under the potting bench", "medium", 7, 1, 7, DOWN);
        set(2, 1, 8, "minecraft:composter");
        set(3, 1, 8, "minecraft:cauldron");

        // ---- yard
        // doghouse (x 16..18, z 2..4), doorway 2 high on the south side
        fill(16, 1, 2, 18, 2, 4, mix(35, "minecraft:red_terracotta", 3, "minecraft:terracotta", 1));
        fill(17, 1, 3, 17, 2, 4, (String) null);
        for (int x = 15; x <= 19; x++) set(x, 3, 3, "minecraft:dark_oak_planks");
        for (int x = 15; x <= 19; x++) { set(x, 3, 2, stairs("minecraft:dark_oak", "south", false)); set(x, 3, 4, stairs("minecraft:dark_oak", "north", false)); }
        set(17, 3, 4, stairs("minecraft:dark_oak", "north", false));
        spot("inside the doghouse", "medium", 17, 1, 3, NORTH);

        // pergola (posts at x 12/16, z 11/15; beams at y = 4)
        for (int[] p : new int[][]{{12, 11}, {16, 11}, {12, 15}, {16, 15}}) fill(p[0], 1, p[1], p[0], 3, p[1], "minecraft:oak_fence");
        for (int x = 12; x <= 16; x++) { set(x, 4, 11, log("minecraft:oak_log", 'x')); set(x, 4, 15, log("minecraft:oak_log", 'x')); }
        for (int z = 12; z <= 14; z++) for (int x = 12; x <= 16; x += 2) set(x, 4, z, slab("minecraft:oak", false));
        set(13, 1, 13, stairs("minecraft:oak", "west", false));       // garden bench
        set(14, 1, 13, stairs("minecraft:oak", "east", false));
        spot("under the pergola beam", "easy", 14, 3, 11, UP);

        // raised flower bed (x 3..7, z 13..15)
        fill(3, 1, 13, 7, 1, 15, "minecraft:mossy_stone_bricks");
        fill(4, 1, 14, 6, 1, 14, "minecraft:rooted_dirt");
        set(4, 2, 14, "minecraft:rose_bush[half=lower]"); set(4, 3, 14, "minecraft:rose_bush[half=upper]");
        set(6, 2, 14, "minecraft:peony[half=lower]"); set(6, 3, 14, "minecraft:peony[half=upper]");
        spot("in the raised flower bed", "medium", 5, 2, 14, DOWN);
        for (int z = 12; z <= 16; z += 4) for (int x = 3; x <= 7; x += 2) set(x, 1, z, "minecraft:poppy");

        // bird table on a post with crates to climb
        set(15, 1, 6, "minecraft:oak_fence"); set(15, 2, 6, "minecraft:oak_fence");
        set(15, 3, 6, "minecraft:oak_planks");
        set(13, 1, 6, "minecraft:barrel[facing=up]");
        set(14, 1, 6, "minecraft:barrel[facing=up]"); set(14, 2, 6, "minecraft:barrel[facing=up]");
        spot("on the bird table (climb the crates)", "hard", 15, 4, 6, DOWN);

        // compost bins in the south-east corner
        set(19, 1, 17, "minecraft:composter"); set(20, 1, 17, "minecraft:composter"); set(19, 1, 18, "minecraft:composter");
        spot("behind the compost bins", "medium", 20, 1, 18, DOWN);

        // gap in the east hedge
        set(21, 2, 10, null);
        set(22, 2, 10, "minecraft:oak_log[axis=y]");                  // hidden trunk the button sits on
        spot("in a gap in the hedge", "hard", 21, 2, 10, EAST);

        // small tree in the south-west corner
        fill(2, 1, 18, 2, 4, 18, "minecraft:oak_log[axis=y]");
        for (int x = 1; x <= 4; x++) for (int z = 16; z <= 20; z++) for (int y = 4; y <= 6; y++) {
            int d = Math.abs(x - 2) + Math.abs(z - 18) + Math.abs(y - 5);
            if (d <= 3 && air(x, y, z) && hash(x, y, z, 36) < 0.85) set(x, y, z, LEAVES);
        }
        // stepping-stone path from the start to the shed door
        for (int z = 10; z <= 17; z++) if (z % 2 == 0) set(5, 0, z, "minecraft:cobblestone");
        set(10, 1, 12, "minecraft:water_cauldron[level=3]");

        lightGrid(5, 3);
        light(5, 3, 2); light(2, 3, 6); light(8, 3, 7);
    }
}
