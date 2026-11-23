package com.mcm.games.hitw;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Generates nothing: an empty void world. */
final class VoidGenerator extends ChunkGenerator {
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, Arena.FLOOR_Y + 1, 0.5);
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
}
