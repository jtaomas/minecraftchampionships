package com.mcm.games.battlebox;

import com.mcm.api.McmBuild;
import com.mcm.games.battlebox.arena.Grid;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Copies a pure-Java {@link Grid} into the world through a core BuildQueue (air included, so it also resets). */
final class ArenaPlacer {
    private ArenaPlacer() {}

    private static final Map<String, BlockData> CACHE = new HashMap<>();
    private static BlockData AIR;

    static BlockData data(String s, Logger log) {
        BlockData d = CACHE.get(s);
        if (d != null) return d;
        try {
            d = Bukkit.createBlockData(s);
        } catch (IllegalArgumentException e) {
            int i = s.indexOf('[');
            try {
                d = Bukkit.createBlockData(i > 0 ? s.substring(0, i) : s);
            } catch (IllegalArgumentException e2) {
                d = Material.STONE.createBlockData();
            }
            if (log != null) log.warning("Battle Box: bad block state '" + s + "', using " + d.getAsString());
        }
        CACHE.put(s, d);
        return d;
    }

    /** Queue the whole grid at origin (ox, oy, oz). */
    static void enqueue(McmBuild.BuildQueue q, World w, int ox, int oy, int oz, Grid g, Logger log) {
        if (AIR == null) AIR = Material.AIR.createBlockData();
        q.region(w, ox + g.minX, oy + g.minY, oz + g.minZ, ox + g.maxX, oy + g.maxY, oz + g.maxZ, (x, y, z) -> {
            int lx = x - ox, ly = y - oy, lz = z - oz;
            if (g.isAir(lx, ly, lz)) return AIR;
            return data(g.get(lx, ly, lz), log);
        }, false);
    }
}
