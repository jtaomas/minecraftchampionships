package com.mcm.games.parkourtag;

import com.mcm.api.McmMapOverlay;
import com.mcm.games.parkourtag.map.PtMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Map Edit overlays (core {@code /mcm mapedit}) for Parkour Tag: {@code plugins/McmParkourTag/mapedits/<map>.overlay.gz}
 * is appended to a map's placements, so every arena copy gets it on top of the generated blocks (before the game paints
 * cages / doors / signs, which are the game's own). Calls are guarded: a core without Map Edit changes nothing.
 */
final class PtOverlay {
    /** Blocks an overlay may place outside the map's own box (the edit area margin), so arena copies never touch. */
    static final int MARGIN = 16;
    private static Plugin plugin;

    private PtOverlay() {}

    static void init(Plugin p) { plugin = p; }

    static McmMapOverlay of(PtMap m) {
        if (plugin == null || m == null) return null;
        try { return McmMapOverlay.of(plugin, m.id()); } catch (Throwable t) { return null; }
    }

    /** The map's placements followed by its overlay's (later writes win), or {@code base} itself without an overlay. */
    static List<ArenaBuilder.Placement> merge(PtMap m, List<ArenaBuilder.Placement> base) {
        try {
            McmMapOverlay ov = of(m);
            if (ov == null || ov.isEmpty()) return base;
            List<ArenaBuilder.Placement> out = new ArrayList<>(base.size() + ov.size());
            out.addAll(base);
            Map<String, BlockData> cache = new HashMap<>();
            ov.forEach((x, y, z, state) -> {
                BlockData d = cache.computeIfAbsent(state, s -> {
                    try { return Bukkit.createBlockData(s); } catch (IllegalArgumentException e) { return Material.AIR.createBlockData(); }
                });
                out.add(new ArenaBuilder.Placement(x, y, z, d));
            });
            return out;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Parkour Tag: map overlay of " + m.id() + " could not be applied; playing the original map", t);
            return base;
        }
    }

    /** Local box {x0, y0, z0, x1, y1, z1} to clear for one arena copy: the map plus its overlay (clamped to the edit margin). */
    static int[] box(PtMap m) {
        int[] b = {0, 0, 0, m.sizeX() - 1, m.sizeY(), m.sizeZ() - 1};
        try {
            McmMapOverlay ov = of(m);
            int[] ob = ov == null ? null : ov.bounds();
            if (ob != null) {
                b[0] = Math.max(-MARGIN, Math.min(b[0], ob[0])); b[1] = Math.min(b[1], ob[1]);
                b[2] = Math.max(-MARGIN, Math.min(b[2], ob[2]));
                b[3] = Math.min(m.sizeX() - 1 + MARGIN, Math.max(b[3], ob[3])); b[4] = Math.max(b[4], ob[4]);
                b[5] = Math.min(m.sizeZ() - 1 + MARGIN, Math.max(b[5], ob[5]));
            }
        } catch (Throwable ignored) { }
        return b;
    }
}
