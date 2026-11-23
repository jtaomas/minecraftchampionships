package com.mcm.games.battlebox;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/** The five Battle Box kits (our own names). Loadouts depend on the arena theme: see {@link Loadouts}. */
enum Kit {
    MEDIC("Medic", "Sustain", Material.GLISTERING_MELON_SLICE, ChatColor.LIGHT_PURPLE,
            "Keeps the team alive with healing."),
    MARKSMAN("Marksman", "Ranged", Material.BOW, ChatColor.GREEN,
            "Most arrows, plus one mobility charge."),
    TINKERER("Tinkerer", "Utility", Material.CROSSBOW, ChatColor.AQUA,
            "Punch bow and tricky thrown gadgets."),
    RUNNER("Runner", "Mobility", Material.STONE_AXE, ChatColor.YELLOW,
            "Fast axe flanker with speed charges."),
    BRAWLER("Brawler", "Frontline", Material.IRON_SWORD, ChatColor.RED,
            "Hits hard up close.");

    final String display;
    final String role;
    final Material icon;
    final ChatColor color;
    final String blurb;

    Kit(String display, String role, Material icon, ChatColor color, String blurb) {
        this.display = display;
        this.role = role;
        this.icon = icon;
        this.color = color;
        this.blurb = blurb;
    }

    String colored() { return color + display; }
}
