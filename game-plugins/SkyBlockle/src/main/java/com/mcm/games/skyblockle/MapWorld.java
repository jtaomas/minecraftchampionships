package com.mcm.games.skyblockle;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.games.skyblockle.stub.StubMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Owns the Sky Blockle world: picks the generator, places a {@link MapModel}, fills chests, and wipes every chunk
 * the map or the players touched so each game starts from a fresh map.
 */
final class MapWorld {
    static final String WORLD_NAME = "mcc_skyblockle";
    static final String SKYMAP_CLASS = "com.mcm.games.skyblockle.map.SkyMap";

    private final JavaPlugin plugin;
    private World world;
    private MapModel model;
    private int baseY = 100;
    /** Chunks that may contain non-air blocks (map footprint + anything players/physics changed). */
    private final Set<Long> dirty = new HashSet<>();
    private McmBuild.BuildQueue queue;
    private BukkitTask clearTask;
    private int generation;
    private final Map<String, BlockData> dataCache = new HashMap<>();
    private final Map<MapModel.IslandType, Integer> chestCounts = new EnumMap<>(MapModel.IslandType.class);
    private int brewingStands;
    private final Map<String, Integer> tableCounts = new java.util.TreeMap<>();
    private String generatorUsed = "?";

    MapWorld(JavaPlugin plugin) { this.plugin = plugin; }

    World world() { return world; }
    MapModel model() { return model; }
    int baseY() { return baseY; }
    String generatorUsed() { return generatorUsed; }
    Map<MapModel.IslandType, Integer> chestCounts() { return chestCounts; }
    int brewingStands() { return brewingStands; }
    boolean busy() { return (queue != null && queue.isRunning()) || clearTask != null; }

    // ------------------------------------------------------------------ world

    /** Load or create the void world. The first time per server start a stale folder is deleted (clean map). */
    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            File folder = new File(Bukkit.getWorldContainer(), WORLD_NAME);
            if (folder.exists()) deleteFolder(folder.toPath());
            w = McmBuild.voidWorld(WORLD_NAME);
        } else {
            McmBuild.voidWorld(WORLD_NAME); // re-apply gamerules
        }
        world = w;
        if (w != null) {
            w.setGameRule(GameRule.KEEP_INVENTORY, false);
            w.setGameRule(GameRule.DO_TILE_DROPS, true);
            w.setGameRule(GameRule.DO_ENTITY_DROPS, true);
            w.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            w.setPVP(true);
            w.setAutoSave(false);
        }
        return w;
    }

    private void deleteFolder(Path root) {
        try (Stream<Path> s = Files.walk(root)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException e) {
            plugin.getLogger().warning("Sky Blockle: could not delete old world folder: " + e);
        }
    }

    // ------------------------------------------------------------------ generator

    /** Generate a map (pure Java, safe off the main thread). mode: auto | skymap | stub. Bounds are re-measured. */
    static MapModel generate(String mode, int wedges, long seed, java.util.logging.Logger log, String[] usedOut) {
        MapModel m = generateRaw(mode, wedges, seed, log, usedOut);
        measure(m, log);
        return m;
    }

    /** Re-measure minY/maxY/mapRadius from the actual blocks (the runtime relies on them for void, build cap, border). */
    static void measure(MapModel m, java.util.logging.Logger log) {
        int[] b = {Integer.MAX_VALUE, Integer.MIN_VALUE};
        double[] r = {0};
        m.blocks.forEach((x, y, z, s) -> {
            if (y < b[0]) b[0] = y;
            if (y > b[1]) b[1] = y;
            double dx = x - m.castleX, dz = z - m.castleZ;
            double d = dx * dx + dz * dz;
            if (d > r[0]) r[0] = d;
        });
        if (b[0] == Integer.MAX_VALUE) { log.warning("Sky Blockle: generated map has no blocks"); return; }
        if (b[0] != m.minY || b[1] != m.maxY) { m.minY = b[0]; m.maxY = b[1]; }
        int rad = (int) Math.ceil(Math.sqrt(r[0]));
        if (rad > m.mapRadius) {
            log.info("Sky Blockle: map blocks reach radius " + rad + " (model said " + m.mapRadius + "); using " + rad);
            m.mapRadius = rad;
        }
        if (m.spawns.isEmpty()) log.warning("Sky Blockle: generated map has no spawns");
    }

    private static MapModel generateRaw(String mode, int wedges, long seed, java.util.logging.Logger log, String[] usedOut) {
        if (!"stub".equals(mode)) {
            try {
                Class<?> c = Class.forName(SKYMAP_CLASS, true, MapWorld.class.getClassLoader());
                Method m = c.getMethod("generate", int.class, long.class);
                Object o = m.invoke(null, wedges, seed);
                if (o instanceof MapModel mm) {
                    if (usedOut != null) usedOut[0] = "SkyMap";
                    return mm;
                }
                log.warning("Sky Blockle: SkyMap.generate returned " + o + "; using the stub map");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                if ("skymap".equals(mode)) log.warning("Sky Blockle: map.generator is 'skymap' but " + SKYMAP_CLASS + ".generate(int,long) is missing; using the stub map");
            } catch (InvocationTargetException e) {
                log.log(Level.SEVERE, "Sky Blockle: SkyMap.generate failed; using the stub map", e.getCause());
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Sky Blockle: SkyMap could not be used; using the stub map", t);
            }
        }
        if (usedOut != null) usedOut[0] = "stub";
        return StubMap.generate(wedges, seed);
    }

    // ------------------------------------------------------------------ coordinates

    Location toWorld(double x, double y, double z) { return new Location(world, x, baseY + y, z); }

    Location castleCentre() {
        if (model == null) return new Location(world, 0.5, baseY + 1, 0.5);
        return new Location(world, model.castleX + 0.5, baseY + model.castleY + 1, model.castleZ + 0.5);
    }

    Location overview() {
        Location l = castleCentre().add(0, 30, 0);
        l.setPitch(60);
        return l;
    }

    int minWorldY() { return model == null ? baseY - 40 : baseY + model.minY; }
    int maxWorldY() { return model == null ? baseY + 80 : baseY + model.maxY; }
    int mapRadius() { return model == null ? 170 : model.mapRadius; }

    static long chunkKey(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }

    void markDirtyBlock(int x, int z) { dirty.add(chunkKey(x >> 4, z >> 4)); }

    void markDirty(Block b) { if (b != null) markDirtyBlock(b.getX(), b.getZ()); }

    // ------------------------------------------------------------------ build

    /**
     * Wipe the old map, then place the model, recolour team markers, fill chests and brewing stands, kick fluids.
     * {@code wedgeColours} maps wedge index to its owner's colour. Runs over several ticks; {@code done} on the main thread.
     */
    void build(MapModel m, Settings s, LootTables loot, Random rnd, Map<Integer, ChatColor> wedgeColours,
               DoubleConsumer progress, Runnable done) {
        cancel();
        if (world == null) ensureWorld();
        if (world == null) { plugin.getLogger().severe("Sky Blockle: world missing, cannot build"); return; }
        this.model = m;
        this.baseY = s.baseY;
        final int gen = ++generation;
        clear(progress == null ? null : p -> progress.accept(p * 0.4), () -> {
            if (gen != generation) return;
            place(m, s, loot, rnd, wedgeColours, progress, done, gen);
        });
    }

    private void place(MapModel m, Settings s, LootTables loot, Random rnd, Map<Integer, ChatColor> wedgeColours,
                       DoubleConsumer progress, Runnable done, int gen) {
        World w = world;
        queue = new McmBuild.BuildQueue(plugin, s.blocksPerTick);
        final McmBuild.BuildQueue q = queue;
        List<int[]> fluids = new ArrayList<>();
        List<int[]> stands = new ArrayList<>();
        final int by = baseY;
        m.blocks.forEach((x, y, z, b) -> {
            int wy = by + y;
            if (wy < w.getMinHeight() || wy >= w.getMaxHeight()) return;
            dirty.add(chunkKey(x >> 4, z >> 4));
            q.set(w, x, wy, z, data(b));
            if (b.startsWith("minecraft:water") || b.startsWith("minecraft:lava")) fluids.add(new int[]{x, wy, z});
            else if (b.startsWith("minecraft:brewing_stand")) stands.add(new int[]{x, wy, z});
        });
        for (MapModel.TeamMarker tm : m.teamMarkers) {
            ChatColor c = wedgeColours.getOrDefault(tm.wedge(), ChatColor.WHITE);
            String dye = McmFx.dyeOf(c).name().toLowerCase(java.util.Locale.ROOT);
            q.set(w, tm.x(), by + tm.y(), tm.z(), data("minecraft:" + dye + tm.suffix()));
        }
        q.run(() -> fillContainers(m, s, loot, rnd, stands, false));
        if (s.fluidPhysics && !fluids.isEmpty()) q.run(() -> kickFluids(fluids));
        q.then(() -> {
            if (gen != generation) return;
            if (progress != null) progress.accept(1.0);
            if (done != null) done.run();
        });
        if (progress != null) {
            BukkitTask[] t = new BukkitTask[1];
            t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (gen != generation || !q.isRunning()) { t[0].cancel(); return; }
                progress.accept(0.4 + 0.6 * q.progress());
            }, 5L, 10L);
        }
        q.start();
    }

    BlockData data(String s) {
        BlockData d = dataCache.get(s);
        if (d != null) return d;
        try {
            d = Bukkit.createBlockData(s);
        } catch (IllegalArgumentException e) {
            int i = s.indexOf('[');
            try { d = Bukkit.createBlockData(i > 0 ? s.substring(0, i) : s); }
            catch (IllegalArgumentException e2) { d = Material.STONE.createBlockData(); }
            plugin.getLogger().warning("Sky Blockle: bad block state '" + s + "', using " + d.getAsString());
        }
        dataCache.put(s, d);
        return d;
    }

    /** Fill every registered chest from its island type's table; brewing stands from "brewing_stand". */
    void fillContainers(MapModel m, Settings s, LootTables loot, Random rnd, List<int[]> stands, boolean refill) {
        if (world == null || m == null) return;
        if (!refill) { chestCounts.clear(); tableCounts.clear(); }
        int missing = 0;
        for (MapModel.ChestSpot c : m.chests) {
            Block b = world.getBlockAt(c.x(), baseY + c.y(), c.z());
            BlockState st = b.getState();
            Inventory inv;
            if (st instanceof Chest ch) inv = ch.getBlockInventory();
            else if (st instanceof Container ct) inv = ct.getInventory();
            else {
                if (refill) continue;
                missing++;
                b.setType(Material.CHEST, false);
                st = b.getState();
                inv = st instanceof Chest ch2 ? ch2.getBlockInventory() : null;
            }
            if (inv == null) continue;
            if (!refill) inv.clear();
            String table = loot.tableFor(c.type(), c.variant() != null ? c.variant() : inferVariant(m, c));
            loot.fill(inv, table, rnd, s.randomSlots);
            if (!refill) { chestCounts.merge(c.type(), 1, Integer::sum); tableCounts.merge(table, 1, Integer::sum); }
        }
        if (missing > 0) plugin.getLogger().warning("Sky Blockle: " + missing + " registered chest spot(s) had no chest block; placed plain chests there");
        if (stands != null) {
            if (!refill) brewingStands = 0;
            for (int[] p : stands) {
                if (world.getBlockAt(p[0], p[1], p[2]).getState() instanceof BrewingStand bs) {
                    if (!refill) bs.getInventory().clear();
                    loot.fill(bs.getInventory(), "brewing_stand", rnd, false);
                    if (!refill) brewingStands++;
                }
            }
        }
    }

    /**
     * Variant for a chest without one: MID chests on a red concrete/wool column = "pillar", on sand/sandstone/
     * terracotta/granite = "spike", on grass/dirt/moss = "grass". Other types: null (plain table).
     */
    static String inferVariant(MapModel m, MapModel.ChestSpot c) {
        if (c.type() != MapModel.IslandType.MID) return null;
        for (int dy = 1; dy <= 2; dy++) {
            String b = m.blocks.get(c.x(), c.y() - dy, c.z());
            if (b == null) continue;
            if (b.contains("red_concrete") || b.contains("red_wool") || b.contains("red_nether_brick") || b.contains("tnt")) return "pillar";
            if (b.contains("sand") || b.contains("terracotta") || b.contains("granite")) return "spike";
            if (b.contains("grass_block") || b.contains("dirt") || b.contains("moss")) return "grass";
            return null;
        }
        return null;
    }

    Map<String, Integer> tableCounts() { return tableCounts; }

    /** Refill: a fresh roll into the empty slots of every map chest (loot.refill-at). */
    void refill(Settings s, LootTables loot, Random rnd) {
        if (model != null) fillContainers(model, s, loot, rnd, null, true);
    }

    /** Re-place fluids with physics so they start flowing (cobblestone generators). */
    private void kickFluids(List<int[]> fluids) {
        BlockData air = Material.AIR.createBlockData();
        for (int[] p : fluids) {
            Block b = world.getBlockAt(p[0], p[1], p[2]);
            BlockData d = b.getBlockData();
            Material t = d.getMaterial();
            if (t != Material.WATER && t != Material.LAVA) continue;
            b.setBlockData(air, false);
            b.setBlockData(d, true);
        }
    }

    // ------------------------------------------------------------------ cleanup

    /** Wipe every dirty chunk (all non-air blocks, block entities and non-player entities). */
    void clear(java.util.function.DoubleConsumer progress, Runnable done) {
        if (clearTask != null) { clearTask.cancel(); clearTask = null; }
        if (world == null) { if (done != null) done.run(); return; }
        final World w = world;
        final int minH = w.getMinHeight(), maxH = w.getMaxHeight();
        final List<Long> todo = new ArrayList<>(dirty);
        final int total = Math.max(1, todo.size());
        final long budget = Math.max(2, plugin.getConfig().getInt("map.clear-ms-per-tick", 20)) * 1_000_000L;
        for (Entity e : new ArrayList<>(w.getEntities())) if (!(e instanceof Player)) e.remove();
        final Iterator<Long> it = todo.iterator();
        final int[] doneCount = {0};
        clearTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long end = System.nanoTime() + budget;
            while (it.hasNext() && System.nanoTime() < end) {
                long k = it.next();
                int cx = (int) (k >> 32), cz = (int) k;
                try {
                    clearChunk(w, cx, cz, minH, maxH);
                    dirty.remove(k);
                } catch (Throwable t) {
                    plugin.getLogger().warning("Sky Blockle: failed to clear chunk " + cx + "," + cz + ": " + t);
                }
                doneCount[0]++;
            }
            if (progress != null) progress.accept(doneCount[0] / (double) total);
            if (!it.hasNext()) {
                if (clearTask != null) { clearTask.cancel(); clearTask = null; }
                for (Entity e : new ArrayList<>(w.getEntities())) if (!(e instanceof Player)) e.remove();
                if (done != null) {
                    try { done.run(); } catch (Throwable t) { plugin.getLogger().log(Level.SEVERE, "Sky Blockle: after-clear step failed", t); }
                }
            }
        }, 1L, 1L);
    }

    private static void clearChunk(World w, int cx, int cz, int minH, int maxH) {
        Chunk ch = w.getChunkAt(cx, cz);
        for (Entity e : ch.getEntities()) if (!(e instanceof Player)) e.remove();
        ChunkSnapshot snap = ch.getChunkSnapshot(false, false, false);
        int sections = (maxH - minH) >> 4;
        for (int sy = 0; sy < sections; sy++) {
            if (snap.isSectionEmpty(sy)) continue;
            int y0 = minH + (sy << 4);
            for (int y = y0; y < y0 + 16; y++) for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (!snap.getBlockType(x, y, z).isAir()) ch.getBlock(x, y, z).setType(Material.AIR, false);
            }
        }
    }

    /** Stop any build/clear in progress (the dirty set is kept, so the next clear still wipes everything). */
    void cancel() {
        generation++;
        if (queue != null) { queue.cancel(); queue = null; }
        if (clearTask != null) { clearTask.cancel(); clearTask = null; }
    }

    /** Cancel and wipe the world (between games). */
    void wipe(Runnable done) {
        cancel();
        clear(null, done);
    }
}
