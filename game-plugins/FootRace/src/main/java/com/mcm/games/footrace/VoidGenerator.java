package com.mcm.games.footrace;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Generates nothing: the whole arena is placed in code. */
final class VoidGenerator extends ChunkGenerator {
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return Track.spectatorSpot(world);
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
}
