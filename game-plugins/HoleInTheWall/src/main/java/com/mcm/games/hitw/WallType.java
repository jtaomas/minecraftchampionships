package com.mcm.games.hitw;

import org.bukkit.Material;

/**
 * Wall behaviours after the MCC event wall types (wiki: 11 types) plus four from MCC Island.
 * Every wall body is slime like in MCC; a special type colours the piston caps on top so it can be read.
 * {@code stage}: 0 = can be one of the first types of a game, 1 = from the third unlock, 2 = from the fifth.
 */
enum WallType {
    // MCC event types
    STANDARD("Standard", "§a", null, 0),
    SIKE("Standard", "§a", null, 0),                            // fake: vanishes right before the platform (looks standard on purpose)
    FROZEN("Frozen", "§b", Material.LIGHT_BLUE_CONCRETE, 0),    // randomly stops mid-track, then resumes
    WALTZ("Waltz", "§d", Material.MAGENTA_CONCRETE, 0),         // two blocks at a time, 3/4 rhythm
    SPRINT("Sprint", "§6", Material.ORANGE_CONCRETE, 0),        // alternates strolling and sprinting
    MORPH("Morph", "§d", Material.PINK_CONCRETE, 0),            // changes its holes just before the platform
    ENDER("Ender", "§5", Material.PURPLE_CONCRETE, 1),          // teleports to the opposite side
    RETURNING("Returning", "§e", Material.YELLOW_CONCRETE, 1),  // turns around at the end and comes back
    WARP("Warp", "§3", Material.CYAN_CONCRETE, 1),              // warps forward to the platform edge
    TRICK("Trick", "§c", Material.RED_CONCRETE, 1),             // rushes the platform, then breaks above it
    SIDESWIPE("Sideswipe", "§9", Material.BLUE_CONCRETE, 1),    // swings round a quarter turn to come in from another side
    // MCC Island types
    RUSH("Rush", "§c", Material.RED_TERRACOTTA, 0),             // fast approach, slows down over the platform
    SANDWICH("Sandwich", "§2", Material.GREEN_CONCRETE, 0),     // a pair from opposite sides that meet in the middle
    DANCE("Dance", "§d", Material.PURPLE_TERRACOTTA, 2),        // steps forward, then back over the platform, 2-3 times
    VENGEFUL("Vengeful", "§4", Material.BLACK_CONCRETE, 2);     // narrower, follows a player left/right until it arrives

    final String label;
    final String color;
    /** Piston-cap colour; null = real pistons (Standard / Sike). */
    final Material capMaterial;
    final int stage;

    WallType(String label, String color, Material capMaterial, int stage) {
        this.label = label;
        this.color = color;
        this.capMaterial = capMaterial;
        this.stage = stage;
    }

    /** The types a game can unlock (everything except Standard and Sike, which are always on). */
    static WallType[] specials() {
        return new WallType[]{FROZEN, WALTZ, SPRINT, MORPH, ENDER, RETURNING, WARP, TRICK, SIDESWIPE,
                RUSH, SANDWICH, DANCE, VENGEFUL};
    }
}
