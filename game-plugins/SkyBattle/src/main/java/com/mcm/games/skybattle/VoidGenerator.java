package com.mcm.games.skybattle;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Generates nothing: the whole world is void. */
public final class VoidGenerator extends ChunkGenerator {
    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public List<BlockPopulator> getDefaultPopulators(World world) { return Collections.emptyList(); }
    @Override public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, Arena.BASE_Y + 25, 0.5);
    }
}
