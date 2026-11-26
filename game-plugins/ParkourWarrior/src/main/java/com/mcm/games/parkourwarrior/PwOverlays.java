package com.mcm.games.parkourwarrior;

import com.mcm.api.McmBuild;
import com.mcm.api.McmMapOverlay;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.function.Function;
import java.util.logging.Level;

/**
 * Map Edit overlays (core {@code /mcm mapedit}) for Parkour Warrior: {@code plugins/McmParkourWarrior/mapedits/<map>.overlay.gz}
 * (map = {@code classic} or an imported map id) is queued after the course blocks and timed blocks, so it wins over
 * them. Timed blocks, movers, crumbles and belts keep running on their own cells: edits there are warned about on save.
 */
final class PwOverlays {
    private PwOverlays() {}

    /** Queue the overlay of {@code mapId} (null if none or unusable); returns it so the caller can wipe its cells later. */
    static McmMapOverlay place(Plugin plugin, McmBuild.BuildQueue q, World w, String mapId, int ox, int oy, int oz, Function<String, BlockData> data) {
        try {
            McmMapOverlay ov = McmMapOverlay.of(plugin, mapId);
            if (ov == null || ov.isEmpty()) return null;
            ov.forEach((x, y, z, state) -> q.set(w, x + ox, y + oy, z + oz, data.apply(state)));
            plugin.getLogger().info("Parkour Warrior: applying the Map Edit overlay of '" + mapId + "' (" + ov.size() + " blocks)");
            return ov;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Parkour Warrior: map overlay of '" + mapId + "' could not be applied; using the original course", t);
            return null;
        }
    }

    /** Queue air over every cell of an overlay that was applied before (the course is being replaced). */
    static void wipe(McmBuild.BuildQueue q, World w, McmMapOverlay ov, int ox, int oy, int oz) {
        if (ov == null) return;
        BlockData air = Material.AIR.createBlockData();
        ov.forEach((x, y, z, state) -> q.set(w, x + ox, y + oy, z + oz, air));
    }
}
