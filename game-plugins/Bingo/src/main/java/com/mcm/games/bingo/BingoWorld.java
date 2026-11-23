package com.mcm.games.bingo;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Owns the survival world "mcc_bingo". It is regenerated with a fresh random seed for every game.
 *
 * Deletion is deliberately paranoid: the only folder that can ever be removed is exactly
 * {@code <world container>/mcc_bingo}, it must not be a symlink, it must not be loaded,
 * and it must not be (or contain) the server's main world.
 */
public final class BingoWorld {
    public static final String NAME = "mcc_bingo";

    private final Plugin plugin;
    private final Logger log;
    private BukkitTask pendingCleanup;

    public BingoWorld(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public World get() { return Bukkit.getWorld(NAME); }

    /** Unload + delete any previous mcc_bingo, then generate a new one with a random seed. */
    public World createFresh() {
        cancelPendingCleanup();
        unloadAndDelete();
        World existing = Bukkit.getWorld(NAME);
        if (existing != null) {
            log.warning("Could not unload the old " + NAME + "; reusing it for this game.");
            configure(existing);
            return existing;
        }
        long seed = ThreadLocalRandom.current().nextLong();
        WorldCreator wc = new WorldCreator(NAME)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(true)
                .seed(seed);
        World w = wc.createWorld();
        if (w == null) {
            log.severe("Failed to create world " + NAME);
            return null;
        }
        log.info("Generated " + NAME + " with seed " + seed);
        configure(w);
        return w;
    }

    private void configure(World w) {
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.NATURAL_REGENERATION, true);
        w.setTime(1000);
        w.setStorm(false);
        w.setThundering(false);
        w.setClearWeatherDuration(20 * 60 * 60);
        w.setPVP(false);
        w.setDifficulty(Difficulty.NORMAL);
        w.setAutoSave(false);
        Location spawn = w.getSpawnLocation();
        WorldBorder border = w.getWorldBorder();
        border.setCenter(spawn.getX(), spawn.getZ());
        border.setSize(1200);
        border.setWarningDistance(16);
    }

    /** After a game: wait for the core to move players out, then unload and delete. */
    public void scheduleCleanup(long delayTicks) {
        cancelPendingCleanup();
        if (!plugin.isEnabled()) return;
        pendingCleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingCleanup = null;
            unloadAndDelete();
        }, delayTicks);
    }

    public void cancelPendingCleanup() {
        if (pendingCleanup != null) {
            pendingCleanup.cancel();
            pendingCleanup = null;
        }
    }

    /** Unload only (used on plugin disable, where scheduling is not allowed). */
    public void unloadNow() {
        cancelPendingCleanup();
        World w = Bukkit.getWorld(NAME);
        if (w == null) return;
        evacuate(w);
        Bukkit.unloadWorld(w, false);
    }

    public void unloadAndDelete() {
        World w = Bukkit.getWorld(NAME);
        if (w != null) {
            evacuate(w);
            if (!Bukkit.unloadWorld(w, false)) {
                log.warning("Bukkit refused to unload " + NAME + "; not deleting it.");
                return;
            }
        }
        deleteFolderSafely();
    }

    private void evacuate(World w) {
        World fallback = null;
        for (World other : Bukkit.getWorlds()) {
            if (!other.equals(w)) { fallback = other; break; }
        }
        if (fallback == null) return;
        for (Player p : new ArrayList<>(w.getPlayers())) {
            p.teleport(fallback.getSpawnLocation());
        }
    }

    /** Deletes exactly {@code <worldContainer>/mcc_bingo} and nothing else. */
    public void deleteFolderSafely() {
        try {
            File container = Bukkit.getWorldContainer().getCanonicalFile();
            File target = new File(container, NAME).getCanonicalFile();

            // 1. Name and location must be exactly <container>/mcc_bingo.
            if (!NAME.equals(target.getName())) { refuse("name mismatch: " + target); return; }
            File parent = target.getParentFile();
            if (parent == null || !parent.getCanonicalFile().equals(container)) { refuse("not directly inside world container: " + target); return; }
            if (target.equals(container)) { refuse("target is the world container"); return; }
            if (!target.exists()) return;

            Path targetPath = target.toPath();
            // 2. Never follow a symlink / junction out of the container.
            if (Files.isSymbolicLink(targetPath) || !targetPath.toRealPath().equals(targetPath.toAbsolutePath().normalize())) {
                refuse("target is a link: " + target); return;
            }
            if (!target.isDirectory()) { refuse("target is not a directory: " + target); return; }

            // 3. Must not be any loaded world (including the main one), nor contain one.
            for (World w : Bukkit.getWorlds()) {
                File wf = w.getWorldFolder().getCanonicalFile();
                if (w.getName().equals(NAME) || wf.equals(target) || wf.toPath().startsWith(targetPath)) {
                    refuse("folder belongs to loaded world " + w.getName()); return;
                }
            }
            String mainWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getName();
            if (NAME.equals(mainWorld)) { refuse("mcc_bingo is the main world"); return; }

            // 4. Walk and delete. walkFileTree does not follow links, so a link inside is removed as a file.
            Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toAbsolutePath().normalize().startsWith(targetPath)) return FileVisitResult.CONTINUE;
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (dir.toAbsolutePath().normalize().startsWith(targetPath)) Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("Deleted old " + NAME + " folder.");
        } catch (IOException e) {
            log.warning("Could not fully delete " + NAME + " folder (will retry next game): " + e.getMessage());
        }
    }

    private void refuse(String why) {
        log.severe("Refusing to delete world folder (" + why + ")");
    }
}
