package com.mcm.games.rocketspleef;

import com.mcm.games.rocketspleef.map.Canvas;
import com.mcm.games.rocketspleef.map.MapGen;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A generated map pasted into the void world: remembers every block it placed so it can be cleared. */
final class PastedMap {
    final MapGen gen;
    final World world;
    final int anchorY;           // world y of the canvas' lowest block
    final int dy;                // world y = canvas y + dy
    final int minWorldY, maxWorldY, radius;
    final List<Location> spawns = new ArrayList<>();
    private final int[] placed;  // x, y, z triples

    PastedMap(MapGen gen, World world, int anchorY) {
        this.gen = gen;
        this.world = world;
        this.anchorY = anchorY;
        Canvas c = gen.generate();
        this.dy = anchorY - c.minY();
        this.minWorldY = anchorY;
        this.maxWorldY = c.maxY() + dy;
        this.radius = c.maxHorizRadius();
        Map<String, BlockData> cache = new HashMap<>();
        int[] arr = new int[c.count() * 3];
        int[] n = {0};
        c.forEach((x, y, z, id) -> {
            BlockData bd = cache.computeIfAbsent(id, Bukkit::createBlockData);
            world.getBlockAt(x, y + dy, z).setBlockData(bd, false);
            arr[n[0]++] = x; arr[n[0]++] = y + dy; arr[n[0]++] = z;
        });
        this.placed = arr;
        for (Canvas.Pos p : c.pickSpawns(48)) {
            spawns.add(new Location(world, p.x() + 0.5, p.y() + dy + 1.0, p.z() + 0.5));
        }
        // sort by angle around the centre so a team gets a neighbouring slice of spawns
        spawns.sort((a, b) -> Double.compare(Math.atan2(a.getZ(), a.getX()), Math.atan2(b.getZ(), b.getX())));
    }

    /** Removes everything this paste placed, plus the box around it (TNT / rocket debris never adds blocks). */
    void clear() {
        for (int i = 0; i < placed.length; i += 3) {
            Block b = world.getBlockAt(placed[i], placed[i + 1], placed[i + 2]);
            if (!b.getType().isAir()) b.setType(Material.AIR, false);
        }
    }

    Location center() { return new Location(world, 0.5, (minWorldY + maxWorldY) / 2.0, 0.5); }
}
