package com.mcm.games.tgttos;

import com.mcm.games.tgttos.model.TgMapData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Local/world coordinate conversion for map slots and a block-state cache (pasting: {@link Paster}). */
final class MapPlacer {
    private final Map<String, BlockData> cache = new HashMap<>();
    private final Logger log;
    int baseY = 64, spacing = 600;

    MapPlacer(Logger log) { this.log = log; }

    int ox(int slot) { return slot * spacing; }
    int oy() { return baseY; }
    int oz(int slot) { return 0; }

    Location loc(World w, int slot, TgMapData.Spawn s) {
        return new Location(w, ox(slot) + s.x(), oy() + s.y(), oz(slot) + s.z(), s.yaw(), s.pitch());
    }

    Location loc(World w, int slot, double x, double y, double z) {
        return new Location(w, ox(slot) + x, oy() + y, oz(slot) + z);
    }

    /** Local coordinates of a world location (double). */
    double lx(int slot, double wx) { return wx - ox(slot); }
    double ly(double wy) { return wy - oy(); }
    double lz(int slot, double wz) { return wz - oz(slot); }

    BlockData data(String s) {
        if (s == null) return Material.AIR.createBlockData();
        BlockData d = cache.get(s);
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
            log.warning("TGTTOS: bad block state '" + s + "', using " + d.getAsString());
        }
        cache.put(s, d);
        return d;
    }

    /** Block position (world) of a local pos. */
    int wx(int slot, int x) { return ox(slot) + x; }
    int wy(int y) { return oy() + y; }
    int wz(int slot, int z) { return oz(slot) + z; }
}
