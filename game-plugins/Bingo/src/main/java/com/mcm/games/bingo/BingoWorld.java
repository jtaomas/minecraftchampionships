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
    static final int BORDER = 1200;

    /** Index of the next game area (area 0 around the world spawn is never used: its chunks predate the layout). */
    private int nextArea = 1;
    private final BingoBiomeProvider biomes = new BingoBiomeProvider();
    private java.util.concurrent.CompletableFuture<AreaLayout> pending;
    private int pendingIndex = -1;
    private Settings settings = new Settings();

    public World get() { return Bukkit.getWorld(NAME); }

    void settings(Settings s) { this.settings = s; }

    /**
     * Plugin enable: generate a fresh mcc_bingo (random seed) now, at server start. Creating a world at game start
     * stalls the main thread for seconds (world creation + synchronous spawn-area generation), so it is never done
     * then. Each game uses a new area of this world instead (see {@link #nextArea}).
     */
    public World prepare() {
        if (Bukkit.getWorld(NAME) == null) deleteFolderSafely();
        World w = createFresh();
        nextArea = 1;
        if (w != null) prefetch(w, nextArea);
        return w;
    }

    /** Starts planning area {@code index} on a background thread (noise sampling only, no chunks). */
    private void prefetch(World w, int index) {
        if (pendingIndex == index && pending != null) return;
        final org.bukkit.generator.BiomeProvider vanilla;
        try { vanilla = w.vanillaBiomeProvider(); } catch (Throwable t) { pending = null; pendingIndex = -1; return; }
        final long seed = w.getSeed();
        final boolean place = settings.placeStructures;
        final org.bukkit.generator.WorldInfo info = w;
        pendingIndex = index;
        pending = java.util.concurrent.CompletableFuture.supplyAsync(() -> plan(info, vanilla, seed, index, place));
    }

    private static AreaLayout plan(org.bukkit.generator.WorldInfo info, org.bukkit.generator.BiomeProvider vanilla,
                                   long seed, int index, boolean place) {
        AreaLayout.Sampler s = (x, y, z) -> vanilla.getBiome(info, x, y, z);
        return AreaLayout.plan(index, index * AREA_SPACING, 0, BORDER / 2, seed, s, place);
    }

    /**
     * The plan of a fresh, never-visited area for the next game, with the border moved there and its layout handed
     * to the biome provider (before any of its chunks generate). Usually planned in the background already; if not,
     * planned now (a few thousand noise samples, tens of milliseconds). Null if the world is missing.
     */
    public AreaLayout nextArea() {
        cancelPendingCleanup();
        World w = get();
        if (w == null) {
            w = createFresh(); // only if something unloaded it; normally created at enable
            if (w == null) return null;
            nextArea = 1;
        }
        configure(w);
        int index = nextArea++;
        AreaLayout layout = null;
        long t0 = System.nanoTime();
        if (pending != null && pendingIndex == index) {
            try { layout = pending.get(); } catch (Exception e) { log.warning("Area planning failed: " + e); }
        }
        if (layout == null) {
            try { layout = plan(w, w.vanillaBiomeProvider(), w.getSeed(), index, settings.placeStructures); }
            catch (Throwable t) { log.severe("Could not plan Bingo area: " + t); return null; }
        }
        pending = null;
        pendingIndex = -1;
        if (!(w.getGenerator() == null && biomesAttached(w))) {
            log.warning(NAME + " was not created by this plugin instance; the biome layout may not apply.");
        }
        biomes.register(layout);
        log.info("Bingo " + layout.describe() + String.format(" (%.0f ms wait)", (System.nanoTime() - t0) / 1e6));
        WorldBorder border = w.getWorldBorder();
        border.setCenter(layout.cx + 0.5, layout.cz + 0.5);
        border.setSize(BORDER);
        border.setWarningDistance(16);
        prefetch(w, nextArea);
        return layout;
    }

    private boolean biomesAttached(World w) {
        return w.getBiomeProvider() == biomes;
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
                .biomeProvider(biomes)
                .seed(seed);
        World w = wc.createWorld();
        if (w == null) {
            log.severe("Failed to create world " + NAME);
            return null;
        }
        biomes.attachVanilla(w.vanillaBiomeProvider());
        w.getPopulators().add(new ExtraCaves(settings.caveChance, settings.caveEntranceShare));
        log.info("Generated " + NAME + " with seed " + seed);
        configure(w);
        // Don't keep spawn chunks loaded and ticking for the whole session; each game preloads its own area.
        w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
        return w;
    }

    private void configure(World w) {
        Settings s = settings;
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, s.daylightCycle);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, s.mobSpawning);
        // server.properties has spawn-animals/spawn-monsters=false; Bukkit applies that to every world as spawn flags.
        w.setSpawnFlags(s.mobSpawning, s.mobSpawning);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.NATURAL_REGENERATION, true);
        w.setTime(s.startTime);
        w.setStorm(false);
        w.setThundering(false);
        w.setClearWeatherDuration(20 * 60 * 60);
        w.setPVP(true);
        w.setDifficulty(Difficulty.NORMAL);
        w.setAutoSave(false);
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
