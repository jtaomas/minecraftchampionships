package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/**
 * Abandoned mineshaft: a timbered main gallery (south, start) and a back chamber with a raised rock ledge,
 * joined by a framed tunnel. Rails, crates, lanterns on chains.
 */
public final class MineshaftMap extends FtbMap {
    public MineshaftMap() { super(7); }

    @Override public String id() { return "mineshaft"; }
    @Override public String displayName() { return "Mineshaft"; }
    @Override public String tagline() { return "Dig around the timbers and the crates"; }

    private static final Pal ROCK = mix(41, "minecraft:stone", 5, "minecraft:andesite", 3, "minecraft:tuff", 2,
            "minecraft:cobblestone", 1, "minecraft:coal_ore", 0.4, "minecraft:iron_ore", 0.25, "minecraft:copper_ore", 0.15);
    private static final String LOG_Y = "minecraft:oak_log[axis=y]";

    @Override
    protected void generate() {
        shell(mix(42, "minecraft:coarse_dirt", 3, "minecraft:stone", 2, "minecraft:andesite", 1, "minecraft:tuff", 1), ROCK, ROCK);

        // partition between main gallery (z 11..20) and back chamber (z 1..8), tunnel at x 9..11
        fill(1, 1, 9, 20, 7, 10, ROCK);
        fill(9, 1, 9, 11, 3, 10, (String) null);
        for (int y = 1; y <= 3; y++) { set(8, y, 10, LOG_Y); set(12, y, 10, LOG_Y); set(8, y, 9, LOG_Y); set(12, y, 9, LOG_Y); }
        for (int x = 8; x <= 12; x++) { set(x, 4, 10, log("minecraft:oak_log", 'x')); set(x, 4, 9, log("minecraft:oak_log", 'x')); }
        spot("tunnel ceiling", "easy", 9, 3, 9, UP);

        // main gallery timber frames at z = 12 and 16: posts at x = 2 and 19 (gap behind), beam at y = 5
        for (int z : new int[]{12, 16}) {
            fill(2, 1, z, 2, 4, z, LOG_Y);
            fill(19, 1, z, 19, 4, z, LOG_Y);
            for (int x = 1; x <= 20; x++) set(x, 5, z, log("minecraft:oak_log", 'x'));
            hangingLantern(6, 4, z, 4);
            hangingLantern(15, 4, z, 4);
        }
        spot("behind the timber post", "medium", 1, 2, 16, EAST);
        spot("under the timber beam", "easy", 10, 4, 12, UP);

        // rails down the middle into the tunnel
        for (int z = 7; z <= 20; z++) if (air(10, 1, z)) set(10, 1, z, "minecraft:rail[shape=north_south]");
        set(10, 1, 6, "minecraft:polished_andesite");                // buffer stop

        // crevice in the east wall at chest height
        set(21, 2, 14, null);
        set(22, 2, 14, "minecraft:stone");
        spot("crevice in the rock wall", "medium", 21, 2, 14, EAST);

        // dug-out hole in the floor
        set(6, 0, 15, null);
        set(6, -1, 15, "minecraft:stone");
        spot("in a hole in the floor", "medium", 6, 0, 15, DOWN);
        set(7, 1, 15, "minecraft:cobblestone_slab[type=bottom]");

        // tall crate stack (climbable) in the south-east
        set(15, 1, 18, "minecraft:spruce_planks");
        fill(16, 1, 18, 16, 2, 18, "minecraft:barrel[facing=up]");
        fill(17, 1, 18, 17, 3, 18, "minecraft:spruce_planks");
        set(17, 1, 19, "minecraft:barrel[facing=up]");
        spot("on top of the tall crate stack", "hard", 17, 4, 18, DOWN);
        set(3, 1, 19, "minecraft:barrel[facing=up]");
        set(4, 1, 19, "minecraft:spruce_planks");
        set(3, 2, 19, "minecraft:lantern[hanging=false]");

        // ---- back chamber: raised ledge (x 1..7, z 1..5, top at y = 4) with stone stairs
        fill(1, 1, 1, 7, 3, 5, ROCK);
        staircase("ledge stairs", "minecraft:cobblestone", 10, 4, 0, -1, 0, 3);
        set(9, 1, 4, "minecraft:cobblestone");
        spot("in the nook under the ledge stairs", "medium", 8, 1, 4, WEST);
        set(2, 4, 1, "minecraft:barrel[facing=up]");
        set(1, 4, 2, "minecraft:barrel[facing=up]");
        spot("up on the ledge, in the corner", "hard", 1, 4, 1, DOWN);
        set(5, 4, 3, "minecraft:lantern[hanging=false]");

        // barrels in the north-east corner of the back chamber
        set(19, 1, 3, "minecraft:barrel[facing=up]");
        set(19, 1, 2, "minecraft:barrel[facing=up]");
        set(20, 1, 4, "minecraft:barrel[facing=up]");
        set(19, 2, 3, "minecraft:barrel[facing=up]");
        spot("behind the barrels", "medium", 20, 1, 3, DOWN);

        // crevice in the back chamber's west wall (below the ledge end)
        set(0, 2, 7, null);
        set(-1, 2, 7, "minecraft:stone");
        spot("crack in the back wall", "medium", 0, 2, 7, WEST);

        // back chamber beam + lanterns
        for (int x = 8; x <= 20; x++) set(x, 5, 6, log("minecraft:oak_log", 'x'));
        fill(8, 1, 6, 8, 4, 6, LOG_Y);
        fill(20, 1, 6, 20, 4, 6, LOG_Y);
        hangingLantern(14, 4, 6, 4);
        hangingLantern(14, 6, 2, 7);

        lightGrid(4, 6);
        lightGrid(5, 3);
    }
}
