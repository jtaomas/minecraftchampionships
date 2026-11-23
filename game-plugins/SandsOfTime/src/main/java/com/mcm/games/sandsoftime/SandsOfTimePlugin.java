package com.mcm.games.sandsoftime;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.stream.Stream;

public final class SandsOfTimePlugin extends JavaPlugin {
    static final String WORLD_NAME = "mcc_sandsoftime";

    private SandsOfTimeGame game;
    private World world;
    private final Deque<Runnable> queue = new ArrayDeque<>();
    private BukkitTask queueTask;

    @Override
    public void onEnable() {
        Items.init(this);
        world = createWorld();
        queueTask = Bukkit.getScheduler().runTaskTimer(this, this::drainQueue, 1L, 1L);
        game = new SandsOfTimeGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error while stopping Sands of Time: " + t);
            }
        }
        if (queueTask != null) queueTask.cancel();
        queue.clear();
        McmApi.unregisterAll(this);
    }

    /** The game world, (re)created if it is not loaded. May be null if creation failed. */
    World world() {
        if (world == null || Bukkit.getWorld(WORLD_NAME) == null) world = createWorld();
        return world;
    }

    /** Heavy block jobs run one per tick so large builds never freeze the server. */
    void enqueue(Runnable job) {
        queue.addLast(job);
    }

    private void drainQueue() {
        Runnable job = queue.pollFirst();
        if (job == null) return;
        try {
            job.run();
        } catch (Throwable t) {
            getLogger().warning("Sands of Time build job failed: " + t);
            t.printStackTrace();
        }
    }

    private World createWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            // Every server start begins with a clean, empty world (no stale dungeons).
            deleteFolder(new File(Bukkit.getWorldContainer(), WORLD_NAME).toPath());
            w = new WorldCreator(WORLD_NAME)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        }
        if (w == null) {
            getLogger().severe("Could not create world " + WORLD_NAME);
            return null;
        }
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_MOB_LOOT, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setDifficulty(Difficulty.NORMAL);
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000);
        w.setSpawnLocation(SandsOfTimeGame.HOLD_X, SandsOfTimeGame.HOLD_Y + 1, SandsOfTimeGame.HOLD_Z);
        return w;
    }

    private void deleteFolder(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { }
            });
        } catch (IOException e) {
            getLogger().warning("Could not delete old world folder: " + e);
        }
    }
}
