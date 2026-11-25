package com.mcm.games.hungergames;

import org.bukkit.Location;
import org.bukkit.World;

import java.nio.file.Path;

/**
 * Immutable descriptor for a real-world Survival Games map loaded from plugins/McmSurvivalGames/worlds/<id>/map.yml.
 */
public record ImportedSgWorld(
        String id,
        String displayName,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        boolean boatRecommended,
        int minY,
        int maxY,
        Path templateDir
) {
    public Location centerLocation(World world) {
        return new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
    }
}
