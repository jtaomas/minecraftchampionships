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
import java.util.List;
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
        barrier = null;
        McmApi.unregisterAll(this);
    }

    /** The game world, (re)created if it is not loaded. May be null if creation failed. */
    World world() {
        if (world == null || Bukkit.getWorld(WORLD_NAME) == null) world = createWorld();
        return world;
    }

    /** Work budget per tick for queued build jobs; a single job is kept to a few ms so a tick stays near this. */
    private static final long BUDGET_NANOS = 12_000_000L;
    /** A chunk-load barrier gives up waiting after this long (then the build would load chunks synchronously). */
    private static final long BARRIER_TIMEOUT_NANOS = 30_000_000_000L;

    /** Pending-count of the chunk-load barrier currently being waited on, or null. */
    private int[] barrier;
    private long barrierSince;
    private boolean debugTiming;
    private long debugT0;

    /**
     * Heavy block jobs run in order, spread over ticks with a ~12 ms budget per tick, so large builds never
     * freeze the server.
     */
    void enqueue(Runnable job) {
        queue.addLast(job);
    }

    /**
     * Enqueue a barrier: when the queue reaches it, the given chunks are loaded (and generated if needed)
     * asynchronously and each gets a plugin ticket as it arrives; later jobs wait until all have arrived. The
     * barrier runs in queue order, so tickets are only added after earlier queued removeTickets jobs.
     */
    void enqueueChunkLoad(World w, List<int[]> chunks) {
        queue.addLast(() -> {
            final int[] pending = {chunks.size()};
            barrier = pending;
            barrierSince = System.nanoTime();
            for (int[] c : chunks) {
                final int cx = c[0], cz = c[1];
                w.getChunkAtAsync(cx, cz, true).whenComplete((chunk, err) -> {
                    // Paper completes chunk futures on the main thread; the chunk is loaded, so this is cheap.
                    if (chunk != null && isEnabled() && Bukkit.getWorld(w.getUID()) != null) w.addPluginChunkTicket(cx, cz, this);
                    pending[0]--;
                });
            }
            if (debugTiming) getLogger().info("[debug-timing] Sands of Time: async-loading " + chunks.size() + " chunks");
        });
    }

    private boolean waitingForChunks() {
        if (barrier == null) return false;
        if (barrier[0] <= 0) { barrier = null; return false; }
        if (System.nanoTime() - barrierSince < BARRIER_TIMEOUT_NANOS) return true;
        getLogger().warning("Sands of Time: chunk preload timed out with " + barrier[0] + " chunk(s) missing; continuing");
        barrier = null;
        return false;
    }

    /** Re-read {@code debug-timing} from config.yml (default false). */
    void refreshDebugTiming() {
        try {
            reloadConfig();
            debugTiming = getConfig().getBoolean("debug-timing", false);
        } catch (Throwable t) {
            debugTiming = false;
        }
    }

    private void drainQueue() {
        if (waitingForChunks() || queue.isEmpty()) return;
        long start = System.nanoTime();
        long deadline = start + BUDGET_NANOS;
        int ran = 0;
        while (!queue.isEmpty() && !waitingForChunks() && System.nanoTime() < deadline) {
            Runnable job = queue.pollFirst();
            ran++;
            try {
                job.run();
            } catch (Throwable t) {
                getLogger().warning("Sands of Time build job failed: " + t);
                t.printStackTrace();
            }
        }
        if (debugTiming) {
            long spent = System.nanoTime() - start;
            if (ran > 0) getLogger().info(String.format("[debug-timing] Sands of Time build tick: %.2f ms, %d job(s), %d queued%s",
                    spent / 1e6, ran, queue.size(), barrier != null ? ", waiting for " + barrier[0] + " chunk(s)" : ""));
        }
    }

    /** Debug helper: marks the start of a measured phase. */
    void debugMark() { debugT0 = System.nanoTime(); }

    /** Debug helper: logs the time since {@link #debugMark()}. */
    void debugLog(String what) {
        if (debugTiming) getLogger().info(String.format("[debug-timing] Sands of Time %s after %.0f ms", what, (System.nanoTime() - debugT0) / 1e6));
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
