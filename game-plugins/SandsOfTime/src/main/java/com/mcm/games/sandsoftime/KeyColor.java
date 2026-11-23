package com.mcm.games.sandsoftime;

import org.bukkit.Material;

/** Vault / key colours. */
enum KeyColor {
    RED("Red", "§c", Material.RED_CONCRETE, Material.RED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA),
    GREEN("Green", "§a", Material.LIME_CONCRETE, Material.LIME_TERRACOTTA, Material.LIME_GLAZED_TERRACOTTA),
    GOLD("Gold", "§6", Material.YELLOW_CONCRETE, Material.YELLOW_TERRACOTTA, Material.YELLOW_GLAZED_TERRACOTTA);

    final String label;
    final String chat;
    final Material door;
    final Material accent;
    final Material frame;

    KeyColor(String label, String chat, Material door, Material accent, Material frame) {
        this.label = label;
        this.chat = chat;
        this.door = door;
        this.accent = accent;
        this.frame = frame;
    }

    String keyName() { return chat + label + " Key"; }
    String vaultName() { return chat + label + " Vault"; }
}
