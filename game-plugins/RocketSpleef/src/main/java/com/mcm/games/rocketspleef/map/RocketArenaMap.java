package com.mcm.games.rocketspleef.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;

/**
 * Contract for a Rocket Spleef Rush floating void arena map.
 */
public interface RocketArenaMap {
    String id();

    String displayName();

    boolean supportsDynamicPads();

    record BuildResult(List<Location> spawns, List<RsCanvas.Pos> crumbleOrder) {}

    void generate(RsCanvas canvas, int scaleRadius);

    default BuildResult build(World world, int scaleRadius) {
        RsCanvas canvas = new RsCanvas();
        generate(canvas, scaleRadius);
        List<RsCanvas.Pos> crumbleOrder = canvas.buildInto(world);
        List<Location> spawns = canvas.spawnLocations(world);
        return new BuildResult(spawns, crumbleOrder);
    }

    default void clear(World world, List<RsCanvas.Pos> blocks) {
        if (world == null || blocks == null) return;
        for (RsCanvas.Pos p : blocks) {
            var b = world.getBlockAt(p.x(), p.y(), p.z());
            if (b.getType() != Material.AIR) {
                b.setType(Material.AIR, false);
            }
        }
    }
}
