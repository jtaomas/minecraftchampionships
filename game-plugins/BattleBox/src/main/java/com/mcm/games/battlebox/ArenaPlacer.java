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
        enqueue(q, w, ox, oy, oz, g, prevBounds, log, null);
    }

    /** Local box {minX, minY, minZ, maxX, maxY, maxZ} this copy occupies: the grid plus its overlay layer. */
    static int[] footprint(Grid g, BbOverlay.Layer layer) {
        int[] b = {g.minX, g.minY, g.minZ, g.maxX, g.maxY, g.maxZ};
        if (layer != null) {
            for (int i = 0; i < 3; i++) { b[i] = Math.min(b[i], layer.bounds[i]); b[i + 3] = Math.max(b[i + 3], layer.bounds[i + 3]); }
        }
        return b;
    }

    /**
     * As above, with the Map Edit overlay layered over the grid (the overlay wins; its managed zones are already
     * left out of the layer). The cleared box covers the grid, the overlay and the previous round's footprint.
     */
    static void enqueue(McmBuild.BuildQueue q, World w, int ox, int oy, int oz, Grid g, int[] prevBounds, Logger log, BbOverlay.Layer layer) {
        if (AIR == null) AIR = Material.AIR.createBlockData();
        int[] f = footprint(g, layer);
        if (prevBounds != null) {
            for (int i = 0; i < 3; i++) { f[i] = Math.min(f[i], prevBounds[i]); f[i + 3] = Math.max(f[i + 3], prevBounds[i + 3]); }
        }
        q.region(w, ox + f[0], oy + f[1], oz + f[2], ox + f[3], oy + f[4], oz + f[5], (x, y, z) -> {
            int lx = x - ox, ly = y - oy, lz = z - oz;
            if (layer != null) {
                BlockData o = layer.get(lx, ly, lz);
                if (o != null) return o;
            }
            if (g.isAir(lx, ly, lz)) return AIR;
            return data(g.get(lx, ly, lz), log);
        }, false);
    }

    /** Queue the whole grid at origin (ox, oy, oz). */
    static void enqueue(McmBuild.BuildQueue q, World w, int ox, int oy, int oz, Grid g, Logger log) {
        enqueue(q, w, ox, oy, oz, g, null, log);
    }
}
