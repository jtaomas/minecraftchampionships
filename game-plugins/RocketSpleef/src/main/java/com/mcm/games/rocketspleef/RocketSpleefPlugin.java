package com.mcm.games.rocketspleef;

import com.mcm.api.McmApi;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public final class RocketSpleefPlugin extends JavaPlugin {
    private RocketSpleefGame game;

    @Override
    public void onEnable() {
        RocketConfig config = new RocketConfig(this);
        World world = createVoidWorld(RocketSpleefGame.WORLD_NAME);
        game = new RocketSpleefGame(this, config, world);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error while stopping Rocket Spleef Rush: " + t);
            }
        }
        McmApi.unregisterAll(this);
    }

    static World createVoidWorld(String name) {
        World existing = org.bukkit.Bukkit.getWorld(name);
        if (existing != null) {
            configureWorld(existing);
            return existing;
        }
        WorldCreator wc = new WorldCreator(name);
        wc.generator(new ChunkGenerator() {
            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                return createChunkData(world);
            }
        });
        World w = wc.createWorld();
        if (w != null) configureWorld(w);
        return w;
    }

    private static void configureWorld(World w) {
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setTime(6000L);
        w.setSpawnLocation(0, 100, 0);
    }
}
