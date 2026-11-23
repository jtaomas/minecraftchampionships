package com.mcm.games.findthebutton;

import com.mcm.api.McmBuild;
import com.mcm.games.findthebutton.map.FtbMap;
import com.mcm.games.findthebutton.map.Grid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Copies the pure-Java map grids into the world and converts map-local coordinates. */
final class MapPlacer {
    private MapPlacer() {}

    /** World y of the map floor blocks (local y = 0). */
    static final int BASE_Y = 64;
    /** Maps are laid out along +x, this far apart. */
    static final int SPACING = 64;

    private static final Map<String, BlockData> CACHE = new HashMap<>();

    static int originX(int mapIndex) { return mapIndex * SPACING; }
    static int originZ(int mapIndex) { return 0; }

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
            if (log != null) log.warning("Find the Button: bad block state '" + s + "', using " + d.getAsString());
        }
        CACHE.put(s, d);
        return d;
    }

    /** Queue the whole grid (air included, so this also resets the map, removing any stray button). */
    static void enqueue(McmBuild.BuildQueue q, World w, int mapIndex, FtbMap map, Logger log) {
        Grid g = map.grid();
        int ox = originX(mapIndex), oy = BASE_Y, oz = originZ(mapIndex);
        BlockData air = Material.AIR.createBlockData();
        q.region(w, ox + g.minX, oy + g.minY, oz + g.minZ, ox + g.maxX, oy + g.maxY, oz + g.maxZ, (x, y, z) -> {
            int lx = x - ox, ly = y - oy, lz = z - oz;
            if (g.isAir(lx, ly, lz)) return air;
            return data(g.get(lx, ly, lz), log);
        }, false);
    }

    /** World location of a local block position (block corner). */
    static Location at(World w, int mapIndex, double lx, double ly, double lz) {
        return new Location(w, originX(mapIndex) + lx, BASE_Y + ly, originZ(mapIndex) + lz);
    }
}
