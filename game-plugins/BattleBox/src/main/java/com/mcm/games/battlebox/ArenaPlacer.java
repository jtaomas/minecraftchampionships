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

    /** Queue the whole grid at origin (ox, oy, oz), clearing any leftover blocks from {@code prevBounds} if larger. */
    static void enqueue(McmBuild.BuildQueue q, World w, int ox, int oy, int oz, Grid g, int[] prevBounds, Logger log) {
        if (AIR == null) AIR = Material.AIR.createBlockData();
        int minX = prevBounds == null ? g.minX : Math.min(g.minX, prevBounds[0]);
        int minY = prevBounds == null ? g.minY : Math.min(g.minY, prevBounds[1]);
        int minZ = prevBounds == null ? g.minZ : Math.min(g.minZ, prevBounds[2]);
        int maxX = prevBounds == null ? g.maxX : Math.max(g.maxX, prevBounds[3]);
        int maxY = prevBounds == null ? g.maxY : Math.max(g.maxY, prevBounds[4]);
        int maxZ = prevBounds == null ? g.maxZ : Math.max(g.maxZ, prevBounds[5]);
        q.region(w, ox + minX, oy + minY, oz + minZ, ox + maxX, oy + maxY, oz + maxZ, (x, y, z) -> {
            int lx = x - ox, ly = y - oy, lz = z - oz;
            if (g.isAir(lx, ly, lz)) return AIR;
            return data(g.get(lx, ly, lz), log);
        }, false);
    }

    /** Queue the whole grid at origin (ox, oy, oz). */
    static void enqueue(McmBuild.BuildQueue q, World w, int ox, int oy, int oz, Grid g, Logger log) {
        enqueue(q, w, ox, oy, oz, g, null, log);
    }
}
