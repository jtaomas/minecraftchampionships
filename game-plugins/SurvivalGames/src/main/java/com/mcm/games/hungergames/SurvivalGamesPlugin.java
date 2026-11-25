package com.mcm.games.hungergames;

import com.mcm.api.McmApi;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Random;

/**
 * Main plugin entry point for McmSurvivalGames (id: hungergames).
 */
public final class SurvivalGamesPlugin extends JavaPlugin {
    public static final String WORLD_NAME = "mcc_hungergames";

    private World gameWorld;
    private SurvivalGamesGame game;
    private Map<String, ImportedSgWorld> importedWorlds = Map.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.importedWorlds = ImportedSgLoader.loadAll(
                new File(getDataFolder(), "worlds"),
                getServer().getWorldContainer(),
                getLogger()
        );
        getLogger().info("Loaded " + importedWorlds.size() + " imported SG world(s): " + importedWorlds.keySet());
        this.gameWorld = createVoidWorld(WORLD_NAME);
        this.game = new SurvivalGamesGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    public Map<String, ImportedSgWorld> getImportedWorlds() {
        return importedWorlds;
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.stop();
        }
    }

    public World getGameWorld() {
        if (gameWorld == null) {
            gameWorld = createVoidWorld(WORLD_NAME);
        }
        return gameWorld;
    }

    private World createVoidWorld(String name) {
        World existing = getServer().getWorld(name);
        if (existing != null) {
            configureWorld(existing);
            return existing;
        }
        WorldCreator creator = new WorldCreator(name);
        creator.generator(new ChunkGenerator() {
            @Override
            public boolean shouldGenerateNoise() { return false; }

            @Override
            public boolean shouldGenerateSurface() { return false; }

            @Override
            public boolean shouldGenerateCaves() { return false; }

            @Override
            public boolean shouldGenerateDecorations() { return false; }

            @Override
            public boolean shouldGenerateMobs() { return false; }

            @Override
            public boolean shouldGenerateStructures() { return false; }

            @Override
            public Location getFixedSpawnLocation(World world, Random random) {
                return new Location(world, 0.5, 66.0, 0.5);
            }
        });
        World world = creator.createWorld();
        if (world != null) {
            configureWorld(world);
        }
        return world;
    }

    public void configureWorld(World world) {
        world.setDifficulty(Difficulty.NORMAL);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }
}
