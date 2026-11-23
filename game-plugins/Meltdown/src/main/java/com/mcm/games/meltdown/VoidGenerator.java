package com.mcm.games.meltdown;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Generates nothing: the arena is built in code. */
public final class VoidGenerator extends ChunkGenerator {
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, Arena.CENTER_X + 0.5, Arena.SPECTATOR_Y, Arena.CENTER_Z + 0.5);
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
}
