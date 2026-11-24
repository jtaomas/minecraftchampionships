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

    /**
     * Distance between the centres of consecutive game areas. The border is 1200 wide, so areas never overlap and
     * every game plays on terrain nobody has touched.
     */
    static final int AREA_SPACING = 4096;

    /** Index of the next game area (0 = around the world spawn). */
    private int nextArea;

    public World get() { return Bukkit.getWorld(NAME); }

    /**
     * Plugin enable: generate a fresh mcc_bingo (random seed) now, at server start. Creating a world at game start
     * stalls the main thread for seconds (world creation + synchronous spawn-area generation), so it is never done
     * then. Each game uses a new, untouched area of this world instead (see {@link #nextAreaCenter}).
     */
    public World prepare() {
        if (Bukkit.getWorld(NAME) == null) deleteFolderSafely();
        World w = createFresh();
        nextArea = 0;
        return w;
    }

    /**
     * The centre of a fresh, never-visited area for the next game, with the border moved there. Area 0 is the
     * world spawn (picked by vanilla); later areas are {@link #AREA_SPACING} blocks further along +X, nudged onto
     * land using the vanilla biome source (no chunk is loaded or generated here). The returned Y is a placeholder:
     * the caller finds the surface once the chunks are loaded asynchronously. Null if the world is missing.
     */
    public Location nextAreaCenter() {
        cancelPendingCleanup();
        World w = get();
        if (w == null) {
            w = createFresh(); // only if something unloaded it; normally created at enable
            if (w == null) return null;
            nextArea = 0;
        }
        configure(w);
        Location spawn = w.getSpawnLocation();
        int area = nextArea++;
        Location center;
        if (area == 0) {
            center = spawn.clone();
        } else {
            int bx = spawn.getBlockX() + area * AREA_SPACING, bz = spawn.getBlockZ();
            int[] land = findLand(w, bx, bz);
            center = new Location(w, land[0] + 0.5, spawn.getY(), land[1] + 0.5);
        }
        WorldBorder border = w.getWorldBorder();
        border.setCenter(center.getX(), center.getZ());
        border.setSize(1200);
        border.setWarningDistance(16);
        return center;
    }

    private static boolean badBiome(org.bukkit.block.Biome b) {
        if (b == null) return true;
        String k = b.getKey().getKey();
        return k.contains("ocean") || k.contains("river") || k.equals("beach") || k.equals("stony_shore");
    }

    /** Spiral search (step 96, up to ~500 blocks out) for a spot whose centre and ring of radius 40 are on land. */
    private static int[] findLand(World w, int bx, int bz) {
        org.bukkit.generator.BiomeProvider bp;
        try {
            bp = w.vanillaBiomeProvider();
        } catch (Throwable t) {
            return new int[]{bx, bz};
        }
        int[] fallback = null;
        int y = 64;
        for (int ring = 0; ring <= 5; ring++) {
            for (int dx = -ring; dx <= ring; dx++)
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = bx + dx * 96, z = bz + dz * 96;
                    if (badBiome(bp.getBiome(w, x, y, z))) continue;
                    if (fallback == null) fallback = new int[]{x, z};
                    boolean ok = true;
                    for (int a = 0; a < 8 && ok; a++) {
                        double ang = Math.PI * a / 4;
                        int sx = x + (int) Math.round(Math.cos(ang) * 40), sz = z + (int) Math.round(Math.sin(ang) * 40);
                        if (badBiome(bp.getBiome(w, sx, y, sz))) ok = false;
                    }
                    if (ok) return new int[]{x, z};
                }
        }
        return fallback != null ? fallback : new int[]{bx, bz};
    }

    /** Unload + delete any previous mcc_bingo, then generate a new one with a random seed (enable only). */
    private World createFresh() {
        cancelPendingCleanup();
        unloadAndDelete();
        World existing = Bukkit.getWorld(NAME);
        if (existing != null) {
            log.warning("Could not unload the old " + NAME + "; reusing it.");
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
        // Don't keep spawn chunks loaded and ticking for the whole session; each game preloads its own area.
        w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
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

    /**
     * After a game: wait for the core to move players out, then send anyone still in mcc_bingo to the main world.
     * The world itself stays loaded (unloading/creating worlds mid-session stalls the server); the next game
     * uses a new area of it, and the folder is deleted at the next server start.
     */
    public void scheduleCleanup(long delayTicks) {
        cancelPendingCleanup();
        if (!plugin.isEnabled()) return;
        pendingCleanup = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingCleanup = null;
            World w = Bukkit.getWorld(NAME);
            if (w != null) evacuate(w);
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
