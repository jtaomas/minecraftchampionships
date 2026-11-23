package com.mcm.games.hitw;

import org.bukkit.Material;

/** Wall behaviours, loosely after the MCC wall types. */
enum WallType {
    STANDARD("Standard", "§a", Material.LIME_CONCRETE, 0),
    SIKE("Standard", "§a", Material.LIME_CONCRETE, 0),          // fake: vanishes right before the platform (looks standard on purpose)
    FROZEN("Frozen", "§b", Material.LIGHT_BLUE_CONCRETE, 30),    // stops for a moment on its way
    WALTZ("Waltz", "§d", Material.MAGENTA_CONCRETE, 30),         // moves two blocks at a time
    SPRINT("Sprint", "§6", Material.ORANGE_CONCRETE, 100),       // alternates strolling and sprinting
    MORPH("Morph", "§d", Material.PINK_CONCRETE, 100),           // changes its holes just before the platform
    ENDER("Ender", "§5", Material.PURPLE_CONCRETE, 100),         // teleports to the opposite side
    RETURNING("Returning", "§e", Material.YELLOW_CONCRETE, 140), // turns around at the end and comes back
    TRICK("Trick", "§c", Material.RED_CONCRETE, 140),            // creeps up, pauses, then rushes across
    WARP("Warp", "§3", Material.CYAN_CONCRETE, 140);             // warps forward to the platform edge

    final String label;
    final String color;
    final Material material;
    /** Seconds into the round from which this type can appear. */
    final int unlockSecond;

    WallType(String label, String color, Material material, int unlockSecond) {
        this.label = label;
        this.color = color;
        this.material = material;
        this.unlockSecond = unlockSecond;
    }
}
