package com.mcm.games.battlebox;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** One side in Battle Box: a tournament team, an ad-hoc squad of solo players, or a practice dummy. */
final class Side {
    final String label;
    final ChatColor chat;
    final DyeColor dye;
    final List<UUID> members = new ArrayList<>();
    final boolean dummy;

    Side(String label, ChatColor chat, DyeColor dye, boolean dummy) {
        this.label = label;
        this.chat = chat;
        this.dye = dye;
        this.dummy = dummy;
    }

    // ---------- colour helpers ----------

    /** Wool colour for a tournament team name, or null if unknown. */
    static DyeColor dyeForTeam(String team) {
        if (team == null) return null;
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "red" -> DyeColor.RED;
            case "orange" -> DyeColor.ORANGE;
            case "yellow" -> DyeColor.YELLOW;
            case "lime" -> DyeColor.LIME;
            case "green" -> DyeColor.GREEN;
            case "cyan" -> DyeColor.CYAN;
            case "aqua", "light_blue", "lightblue" -> DyeColor.LIGHT_BLUE;
            case "blue" -> DyeColor.BLUE;
            case "purple" -> DyeColor.PURPLE;
            case "pink" -> DyeColor.PINK;
            case "white" -> DyeColor.WHITE;
            case "black" -> DyeColor.BLACK;
            case "gray", "grey" -> DyeColor.GRAY;
            case "magenta" -> DyeColor.MAGENTA;
            case "brown" -> DyeColor.BROWN;
            default -> null;
        };
    }

    static ChatColor chatFor(DyeColor dye) {
        return switch (dye) {
            case RED -> ChatColor.RED;
            case ORANGE -> ChatColor.GOLD;
            case YELLOW -> ChatColor.YELLOW;
            case LIME -> ChatColor.GREEN;
            case GREEN -> ChatColor.DARK_GREEN;
            case CYAN -> ChatColor.DARK_AQUA;
            case LIGHT_BLUE -> ChatColor.AQUA;
            case BLUE -> ChatColor.BLUE;
            case PURPLE -> ChatColor.DARK_PURPLE;
            case PINK, MAGENTA -> ChatColor.LIGHT_PURPLE;
            case WHITE -> ChatColor.WHITE;
            case LIGHT_GRAY -> ChatColor.GRAY;
            case GRAY, BLACK -> ChatColor.DARK_GRAY;
            case BROWN -> ChatColor.GOLD;
        };
    }

    static String niceName(DyeColor dye) {
        String s = dye.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) {
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    static Material wool(DyeColor dye) { return Material.valueOf(dye.name() + "_WOOL"); }

    static Material concrete(DyeColor dye) { return Material.valueOf(dye.name() + "_CONCRETE"); }

    static Material glass(DyeColor dye) { return Material.valueOf(dye.name() + "_STAINED_GLASS"); }

    /** Team material for the centre: concrete (default) or wool. */
    static Material centreBlock(DyeColor dye, boolean wool) { return wool ? wool(dye) : concrete(dye); }

    /** The dye colour of a concrete or wool block (not powder), or null for anything else. */
    static DyeColor dyeOfCentre(Material m) {
        String n = m.name();
        String base;
        if (n.endsWith("_CONCRETE")) base = n.substring(0, n.length() - 9);
        else if (n.endsWith("_WOOL")) base = n.substring(0, n.length() - 5);
        else return null;
        try {
            return DyeColor.valueOf(base);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Lower-case colour name used by the arena generators, e.g. "light_blue". */
    static String colourName(DyeColor dye) { return dye.name().toLowerCase(Locale.ROOT); }
}
