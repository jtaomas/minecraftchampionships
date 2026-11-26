package com.mcm.games.battlebox;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * The Battle Box kits. One universal set for every map/theme for now (see {@link Loadouts}); the per-theme tables are
 * kept in the file but unused. Tinkerer is disabled but its class/spell code stays for a future Wizard kit.
 */
enum Kit {
    MEDIC("Medic", "Sustain", Material.GLISTERING_MELON_SLICE, ChatColor.LIGHT_PURPLE, true,
            "Heals the team with splash potions."),
    MARKSMAN("Marksman", "Ranged", Material.CROSSBOW, ChatColor.GREEN, true,
            "Power crossbow, a three-shot Multishot crossbow and wind charges."),
    TINKERER("Tinkerer", "Utility", Material.CROSSBOW, ChatColor.AQUA, false,
            "Disabled for now (spell kit, kept for a future Wizard)."),
    RUNNER("Runner", "Mobility", Material.STONE_AXE, ChatColor.YELLOW, true,
            "Fast axe flanker with splash Swiftness."),
    BRAWLER("Brawler", "Frontline", Material.IRON_SWORD, ChatColor.RED, true,
            "Hits hard up close, with a golden apple.");

    final String display;
    final String role;
    final Material icon;
    final ChatColor color;
    final boolean enabled;
    final String blurb;

    Kit(String display, String role, Material icon, ChatColor color, boolean enabled, String blurb) {
        this.display = display;
        this.role = role;
        this.icon = icon;
        this.color = color;
        this.enabled = enabled;
        this.blurb = blurb;
    }

    String colored() { return color + display; }

    /** The kits a player can pick and be assigned (Tinkerer is out for now). */
    static java.util.List<Kit> enabled() {
        java.util.List<Kit> out = new java.util.ArrayList<>();
        for (Kit k : values()) if (k.enabled) out.add(k);
        return out;
    }
}
