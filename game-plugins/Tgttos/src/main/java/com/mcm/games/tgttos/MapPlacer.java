package com.mcm.games.tgttos;

import com.mcm.api.McmBuild;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.SparseBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Pastes TgMapData into the world at a slot offset and converts local/world coordinates. */
final class MapPlacer {
    private final Map<String, BlockData> cache = new HashMap<>();
    /** Per slot: packed local keys of every non-air block pasted there last time (for clearing stale blocks). */
    private final Map<Integer, Set<Long>> pasted = new HashMap<>();
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

    /**
     * Queue a full paste of {@code d} at {@code slot}: clears blocks left from the previous paste at this slot,
     * places every block, then the chosen variant options.
     */
    void enqueue(McmBuild.BuildQueue q, World w, int slot, TgMapData d, Map<String, String> variantChoice) {
        int ox = ox(slot), oy = oy(), oz = oz(slot);
        Set<Long> now = new HashSet<>();
        d.blocks.forEach((x, y, z, b) -> now.add(SparseBlocks.key(x, y, z)));
        for (TgMapData.VariantGroup g : d.variants)
            for (List<TgMapData.Placement> l : g.options.values())
                for (TgMapData.Placement p : l) now.add(SparseBlocks.key(p.x(), p.y(), p.z()));
        Set<Long> old = pasted.get(slot);
        BlockData air = Material.AIR.createBlockData();
        if (old != null) {
            for (long k : old) {
                if (!now.contains(k))
                    q.set(w, ox + SparseBlocks.kx(k), oy + SparseBlocks.ky(k), oz + SparseBlocks.kz(k), air);
            }
        }
        d.blocks.forEach((x, y, z, b) -> q.set(w, ox + x, oy + y, oz + z, data(b)));
        for (TgMapData.VariantGroup g : d.variants) {
            String opt = variantChoice == null ? null : variantChoice.get(g.name);
            if (opt == null) continue;
            List<TgMapData.Placement> l = g.options.get(opt);
            if (l == null) continue;
            for (TgMapData.Placement p : l)
                q.set(w, ox + p.x(), oy + p.y(), oz + p.z(), data(SparseBlocks.normalize(p.block())));
        }
        pasted.put(slot, now);
    }

    /** Block position (world) of a local pos. */
    int wx(int slot, int x) { return ox(slot) + x; }
    int wy(int y) { return oy() + y; }
    int wz(int slot, int z) { return oz(slot) + z; }
}
