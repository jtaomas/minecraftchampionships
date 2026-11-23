package com.mcm.api;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * World-building kit for arena generation: void worlds, a tick-spread block queue, shapes, weighted
 * palettes, seeded noise, floating islands, clouds, trees, decoration scatter and region dumps for
 * previews. Since core 3.2.0.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * World w = McmBuild.voidWorld("mcc_skybattle");
 * McmBuild.BuildQueue q = new McmBuild.BuildQueue(plugin);         // 40k blocks/tick budget
 * McmBuild.Palette grass = McmBuild.Palette.of(Material.GRASS_BLOCK, 8, Material.MOSS_BLOCK, 1);
 * McmBuild.Palette stone = McmBuild.Palette.of(Material.STONE, 5, Material.ANDESITE, 2, Material.TUFF, 2);
 * McmBuild.floatingIsland(q, w, new Location(w, 0, 100, 0), 18, 14, grass, stone, 42L);
 * McmBuild.tree(q, w, new Location(w, 3, 102, 2), McmBuild.TreeStyle.OAK_ROUND, 7L);
 * McmBuild.scatter(q, w, new Location(w, -25, 95, -25), new Location(w, 25, 110, 25),
 *         McmBuild.Palette.of(Material.SHORT_GRASS, 6, Material.POPPY, 1, Material.DANDELION, 1), 0.25, 9L);
 * q.then(() -> startTheGame()).start();
 * }</pre>
 *
 * <h2>Conventions</h2>
 * <ul>
 *   <li>Everything here must be called on the main thread (except that {@link #dumpRegion} does its file
 *       writing on its own worker thread internally).</li>
 *   <li>Shape methods only <i>enqueue</i>; nothing changes in the world until the queue runs
 *       ({@link BuildQueue#start()} or {@link BuildQueue#flush()}). Later operations overwrite earlier ones.</li>
 *   <li>Blocks are placed with {@code setBlockData(data, false)}: no physics, no block updates, so sand
 *       floats, leaves need {@code persistent=true} (the tree helpers do that), and fluids don't flow until updated.</li>
 *   <li>Round shapes take their centre in block coordinates: integer values centre the shape on that block,
 *       {@code .5} values centre it between blocks (even diameters).</li>
 *   <li>Shape methods return the number of block operations they enqueued (useful for block counts).</li>
 *   <li>A {@link BlockSource} returning {@code null} for a position leaves that position untouched.</li>
 * </ul>
 */
public final class McmBuild {
    private McmBuild() {}

    // =====================================================================================
    // Void world
    // =====================================================================================

    /** Chunk generator that generates nothing (pure void), fixed spawn at (0.5, 65, 0.5), plains biome everywhere. */
    public static class VoidGenerator extends ChunkGenerator {
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
        @Override public Location getFixedSpawnLocation(World world, Random random) { return new Location(world, 0.5, 65, 0.5); }
        @Override public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
            return new BiomeProvider() {
                @Override public Biome getBiome(WorldInfo info, int x, int y, int z) { return Biome.PLAINS; }
                @Override public List<Biome> getBiomes(WorldInfo info) { return Collections.singletonList(Biome.PLAINS); }
            };
        }
    }

    /**
     * Returns the world with this name, creating it as an empty void world (see {@link VoidGenerator}) if it
     * is not loaded; an existing world folder is loaded as-is. Safe to call repeatedly. Every call (re)applies:
     * no mob/patrol/trader/warden spawning, no daylight or weather cycle, time = noon (6000), clear weather,
     * no fire tick, no mob griefing, no insomnia, announceAdvancements=false, doImmediateRespawn=true,
     * spawn chunks not kept in memory, spawn at (0.5, 65, 0.5). Difficulty and keepInventory/fall damage are
     * left for the game to set. Returns null only if the server refused to create the world.
     */
    public static World voidWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null) {
            w = new WorldCreator(name)
                    .environment(World.Environment.NORMAL)
                    .generator(new VoidGenerator())
                    .generateStructures(false)
                    .createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setTime(6000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setClearWeatherDuration(20 * 60 * 60 * 24);
        w.setKeepSpawnInMemory(false);
        w.setSpawnLocation(0, 65, 0);
        return w;
    }

    // =====================================================================================
    // Block sources & palettes
    // =====================================================================================

    /** Supplies the block for a position. Return null to leave that position untouched. */
    @FunctionalInterface
    public interface BlockSource {
        BlockData at(int x, int y, int z);

        /** A source that always returns this material's default block data (AIR is allowed). */
        static BlockSource of(Material m) {
            final BlockData d = m.createBlockData();
            return (x, y, z) -> d;
        }

        /** A source that always returns this block data. */
        static BlockSource of(BlockData d) { return (x, y, z) -> d; }
    }

    /**
     * Weighted random block palette, e.g. {@code Palette.of(Material.STONE, 5, Material.ANDESITE, 2, Material.TUFF, 1)}.
     * Entries may also be {@link BlockData} (for stairs/slabs with state). Immutable once built.
     * As a {@link BlockSource} it picks deterministically from position + its seed (see {@link #seeded(long)}),
     * so rebuilding a map gives the same result.
     */
    public static final class Palette implements BlockSource {
        private final BlockData[] data;
        private final double[] cumulative;
        private final double total;
        private final long seed;

        private Palette(BlockData[] data, double[] cumulative, double total, long seed) {
            this.data = data; this.cumulative = cumulative; this.total = total; this.seed = seed;
        }

        /**
         * Builds a palette from alternating (Material|BlockData, weight) pairs, e.g.
         * {@code of(Material.STONE, 5, Material.ANDESITE, 2)}. Weights are any positive numbers.
         * @throws IllegalArgumentException on an odd argument count, wrong types or no positive weight
         */
        public static Palette of(Object... materialWeightPairs) {
            if (materialWeightPairs == null || materialWeightPairs.length == 0 || materialWeightPairs.length % 2 != 0)
                throw new IllegalArgumentException("Palette.of needs (Material|BlockData, weight) pairs");
            List<BlockData> ds = new ArrayList<>();
            List<Double> ws = new ArrayList<>();
            for (int i = 0; i < materialWeightPairs.length; i += 2) {
                Object o = materialWeightPairs[i];
                Object w = materialWeightPairs[i + 1];
                BlockData d;
                if (o instanceof Material m) d = m.createBlockData();
                else if (o instanceof BlockData bd) d = bd;
                else throw new IllegalArgumentException("Palette entry " + i / 2 + " is not a Material or BlockData: " + o);
                if (!(w instanceof Number num)) throw new IllegalArgumentException("Palette weight " + i / 2 + " is not a number: " + w);
                double weight = num.doubleValue();
                if (weight <= 0) continue;
                ds.add(d); ws.add(weight);
            }
            if (ds.isEmpty()) throw new IllegalArgumentException("Palette has no positive weights");
            double[] cum = new double[ds.size()];
            double t = 0;
            for (int i = 0; i < cum.length; i++) { t += ws.get(i); cum[i] = t; }
            return new Palette(ds.toArray(new BlockData[0]), cum, t, 0L);
        }

        /** A palette with a single material. */
        public static Palette single(Material m) { return of(m, 1); }

        /** Copy of this palette using another seed for its positional ({@link BlockSource}) picks. */
        public Palette seeded(long newSeed) { return new Palette(data, cumulative, total, newSeed); }

        /** Weighted random pick (shared block data instance; clone before mutating). */
        public BlockData pick(Random random) { return choose(random.nextDouble()); }

        /** Material of a weighted random pick. */
        public Material pickMaterial(Random random) { return pick(random).getMaterial(); }

        /** Deterministic weighted pick for a position and seed (same inputs, same result). */
        public BlockData pick(int x, int y, int z, long seed) { return choose(hash01(x, y, z, seed)); }

        /** Material of a deterministic pick. */
        public Material pickMaterial(int x, int y, int z, long seed) { return pick(x, y, z, seed).getMaterial(); }

        /** BlockSource implementation: {@code pick(x, y, z, seed)} with this palette's seed. */
        @Override public BlockData at(int x, int y, int z) { return pick(x, y, z, seed); }

        /** Number of entries. */
        public int size() { return data.length; }

        private BlockData choose(double r01) {
            double r = r01 * total;
            for (int i = 0; i < cumulative.length; i++) if (r < cumulative[i]) return data[i];
            return data[data.length - 1];
        }
    }

    /** Deterministic hash of a position and seed to a double in [0, 1). */
    public static double hash01(int x, int y, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= x * 0xC2B2AE3D27D4EB4FL;
        h = mix(h);
        h ^= y * 0x165667B19E3779F9L;
        h = mix(h);
        h ^= z * 0x27D4EB2F165667C5L;
        h = mix(h);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long mix(long h) {
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    // =====================================================================================
    // Noise
    // =====================================================================================

    /**
     * Seeded simplex noise (2D and 3D) with fractal octaves. Values of {@link #noise2}/{@link #noise3} are
     * roughly in [-1, 1]; {@link #fbm2}/{@link #fbm3} are normalised to the same range. Typical frequencies for
     * terrain features: 0.02 - 0.15 (multiply coordinates by the frequency). Thread-safe after construction.
     */
    public static final class Noise {
        private static final int[][] GRAD3 = {{1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
                {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}};
        private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
        private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
        private static final double F3 = 1.0 / 3.0;
        private static final double G3 = 1.0 / 6.0;
        private final short[] perm = new short[512];
        private final short[] permMod12 = new short[512];

        public Noise(long seed) {
            short[] p = new short[256];
            for (short i = 0; i < 256; i++) p[i] = i;
            Random r = new Random(seed);
            for (int i = 255; i > 0; i--) { int j = r.nextInt(i + 1); short t = p[i]; p[i] = p[j]; p[j] = t; }
            for (int i = 0; i < 512; i++) { perm[i] = p[i & 255]; permMod12[i] = (short) (perm[i] % 12); }
        }

        private static int fastFloor(double x) { int xi = (int) x; return x < xi ? xi - 1 : xi; }
        private static double dot(int[] g, double x, double y) { return g[0] * x + g[1] * y; }
        private static double dot(int[] g, double x, double y, double z) { return g[0] * x + g[1] * y + g[2] * z; }

        /** 2D simplex noise, roughly [-1, 1]. */
        public double noise2(double xin, double yin) {
            double n0, n1, n2;
            double s = (xin + yin) * F2;
            int i = fastFloor(xin + s), j = fastFloor(yin + s);
            double t = (i + j) * G2;
            double x0 = xin - (i - t), y0 = yin - (j - t);
            int i1, j1;
            if (x0 > y0) { i1 = 1; j1 = 0; } else { i1 = 0; j1 = 1; }
            double x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
            double x2 = x0 - 1.0 + 2.0 * G2, y2 = y0 - 1.0 + 2.0 * G2;
            int ii = i & 255, jj = j & 255;
            int gi0 = permMod12[ii + perm[jj]];
            int gi1 = permMod12[ii + i1 + perm[jj + j1]];
            int gi2 = permMod12[ii + 1 + perm[jj + 1]];
            double t0 = 0.5 - x0 * x0 - y0 * y0;
            if (t0 < 0) n0 = 0; else { t0 *= t0; n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0); }
            double t1 = 0.5 - x1 * x1 - y1 * y1;
            if (t1 < 0) n1 = 0; else { t1 *= t1; n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1); }
            double t2 = 0.5 - x2 * x2 - y2 * y2;
            if (t2 < 0) n2 = 0; else { t2 *= t2; n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2); }
            return 70.0 * (n0 + n1 + n2);
        }

        /** 3D simplex noise, roughly [-1, 1]. */
        public double noise3(double xin, double yin, double zin) {
            double n0, n1, n2, n3;
            double s = (xin + yin + zin) * F3;
            int i = fastFloor(xin + s), j = fastFloor(yin + s), k = fastFloor(zin + s);
            double t = (i + j + k) * G3;
            double x0 = xin - (i - t), y0 = yin - (j - t), z0 = zin - (k - t);
            int i1, j1, k1, i2, j2, k2;
            if (x0 >= y0) {
                if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
                else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
                else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
            } else {
                if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
                else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
                else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
            }
            double x1 = x0 - i1 + G3, y1 = y0 - j1 + G3, z1 = z0 - k1 + G3;
            double x2 = x0 - i2 + 2.0 * G3, y2 = y0 - j2 + 2.0 * G3, z2 = z0 - k2 + 2.0 * G3;
            double x3 = x0 - 1.0 + 3.0 * G3, y3 = y0 - 1.0 + 3.0 * G3, z3 = z0 - 1.0 + 3.0 * G3;
            int ii = i & 255, jj = j & 255, kk = k & 255;
            int gi0 = permMod12[ii + perm[jj + perm[kk]]];
            int gi1 = permMod12[ii + i1 + perm[jj + j1 + perm[kk + k1]]];
            int gi2 = permMod12[ii + i2 + perm[jj + j2 + perm[kk + k2]]];
            int gi3 = permMod12[ii + 1 + perm[jj + 1 + perm[kk + 1]]];
            double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
            if (t0 < 0) n0 = 0; else { t0 *= t0; n0 = t0 * t0 * dot(GRAD3[gi0], x0, y0, z0); }
            double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
            if (t1 < 0) n1 = 0; else { t1 *= t1; n1 = t1 * t1 * dot(GRAD3[gi1], x1, y1, z1); }
            double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
            if (t2 < 0) n2 = 0; else { t2 *= t2; n2 = t2 * t2 * dot(GRAD3[gi2], x2, y2, z2); }
            double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
            if (t3 < 0) n3 = 0; else { t3 *= t3; n3 = t3 * t3 * dot(GRAD3[gi3], x3, y3, z3); }
            return 32.0 * (n0 + n1 + n2 + n3);
        }

        /** Fractal 2D noise: {@code octaves} layers, each at {@code lacunarity}x frequency and {@code gain}x amplitude. Normalised to about [-1, 1]. */
        public double fbm2(double x, double y, int octaves, double lacunarity, double gain) {
            double sum = 0, amp = 1, freq = 1, norm = 0;
            for (int o = 0; o < Math.max(1, octaves); o++) {
                sum += amp * noise2(x * freq + o * 31.7, y * freq - o * 17.3);
                norm += amp; amp *= gain; freq *= lacunarity;
            }
            return sum / norm;
        }

        /** {@link #fbm2(double, double, int, double, double)} with lacunarity 2 and gain 0.5. */
        public double fbm2(double x, double y, int octaves) { return fbm2(x, y, octaves, 2.0, 0.5); }

        /** Fractal 3D noise, normalised to about [-1, 1]. */
        public double fbm3(double x, double y, double z, int octaves, double lacunarity, double gain) {
            double sum = 0, amp = 1, freq = 1, norm = 0;
            for (int o = 0; o < Math.max(1, octaves); o++) {
                sum += amp * noise3(x * freq + o * 31.7, y * freq + o * 11.1, z * freq - o * 17.3);
                norm += amp; amp *= gain; freq *= lacunarity;
            }
            return sum / norm;
        }

        /** {@link #fbm3(double, double, double, int, double, double)} with lacunarity 2 and gain 0.5. */
        public double fbm3(double x, double y, double z, int octaves) { return fbm3(x, y, z, octaves, 2.0, 0.5); }
    }

    // =====================================================================================
    // Build queue
    // =====================================================================================

    /**
     * Tick-spread block placement queue. Enqueue block sets, lazy region fills and callbacks; {@link #start()}
     * then applies them in order over as many ticks as needed, at most {@code blocksPerTick} blocks per tick and
     * at most ~25 ms of work per tick, so big builds never freeze the server. Blocks are placed without physics.
     *
     * <ul>
     *   <li>Operations run strictly in the order they were added; later writes to a block win.</li>
     *   <li>You may keep adding operations while the queue runs (also from {@link #run} callbacks); they are
     *       appended and processed in the same run.</li>
     *   <li>{@link #then} callbacks run once, when the queue next becomes empty after {@link #start()}.</li>
     *   <li>{@link #cancel()} stops immediately, drops pending operations and {@code then} callbacks; the queue can
     *       be reused afterwards. Call it from your game's {@code stop()}.</li>
     *   <li>Operations for a world that has been unloaded are skipped.</li>
     *   <li>Main thread only. Not thread-safe.</li>
     * </ul>
     */
    public static final class BuildQueue {
        /** Default budget: 40 000 blocks per tick. */
        public static final int DEFAULT_BLOCKS_PER_TICK = 40_000;
        private static final int RUN = -1, REGION = -2;
        private static final long COST_SET = 8, COST_READ = 1;
        private static final long MAX_NANOS_PER_TICK = 25_000_000L;

        private final Plugin plugin;
        private final int blocksPerTick;
        private int[] ops = new int[5 * 1024];
        private int count, cursor;
        private final List<World> worlds = new ArrayList<>();
        private final List<BlockData> datas = new ArrayList<>();
        private final Map<BlockData, Integer> dataIndex = new HashMap<>();
        private final Map<Material, Integer> materialIndex = new EnumMap<>(Material.class);
        private final List<Runnable> runnables = new ArrayList<>();
        private final List<Region> regions = new ArrayList<>();
        private final List<Runnable> onComplete = new ArrayList<>();
        private BukkitTask task;
        private int generation;
        private long total, processed, placed;

        private static final class Region {
            final int worldIdx, x1, y1, z1, sx, sy, sz;
            final long volume;
            final BlockSource src;
            final boolean skipAir;
            long idx;
            Region(int worldIdx, int x1, int y1, int z1, int sx, int sy, int sz, BlockSource src, boolean skipAir) {
                this.worldIdx = worldIdx; this.x1 = x1; this.y1 = y1; this.z1 = z1;
                this.sx = sx; this.sy = sy; this.sz = sz; this.src = src; this.skipAir = skipAir;
                this.volume = (long) sx * sy * sz;
            }
        }

        /** Queue with the default budget of {@link #DEFAULT_BLOCKS_PER_TICK} blocks per tick. */
        public BuildQueue(Plugin plugin) { this(plugin, DEFAULT_BLOCKS_PER_TICK); }

        /** Queue with a custom blocks-per-tick budget (minimum 1). */
        public BuildQueue(Plugin plugin, int blocksPerTick) {
            this.plugin = plugin;
            this.blocksPerTick = Math.max(1, blocksPerTick);
        }

        private int worldIdx(World w) {
            for (int i = 0; i < worlds.size(); i++) if (worlds.get(i) == w) return i;
            worlds.add(w);
            return worlds.size() - 1;
        }

        private int dataIdx(BlockData d) {
            Integer i = dataIndex.get(d);
            if (i != null) return i;
            datas.add(d);
            dataIndex.put(d, datas.size() - 1);
            return datas.size() - 1;
        }

        private void push(int a, int b, int c, int d, int e) {
            if ((count + 1) * 5 > ops.length) ops = Arrays.copyOf(ops, ops.length * 2);
            int base = count * 5;
            ops[base] = a; ops[base + 1] = b; ops[base + 2] = c; ops[base + 3] = d; ops[base + 4] = e;
            count++;
        }

        /** Enqueue one block. Null world/data or a y outside the world's height are ignored. */
        public BuildQueue set(World w, int x, int y, int z, BlockData data) {
            if (w == null || data == null || y < w.getMinHeight() || y >= w.getMaxHeight()) return this;
            push(worldIdx(w), x, y, z, dataIdx(data));
            total++;
            return this;
        }

        /** Enqueue one block (material's default state). */
        public BuildQueue set(World w, int x, int y, int z, Material m) {
            if (w == null || m == null || y < w.getMinHeight() || y >= w.getMaxHeight()) return this;
            Integer idx = materialIndex.get(m);
            if (idx == null) { idx = dataIdx(m.createBlockData()); materialIndex.put(m, idx); }
            push(worldIdx(w), x, y, z, idx);
            total++;
            return this;
        }

        /** Enqueue one block at a location (uses the location's world and block coordinates). */
        public BuildQueue set(Location l, BlockData data) {
            return l == null ? this : set(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), data);
        }

        /** Enqueue one block at a location. */
        public BuildQueue set(Location l, Material m) {
            return l == null ? this : set(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), m);
        }

        /** Enqueue one block from a source (no-op if the source returns null for that position). */
        public BuildQueue set(World w, int x, int y, int z, BlockSource src) {
            if (src == null) return this;
            return set(w, x, y, z, src.at(x, y, z));
        }

        /**
         * Enqueue a lazily-evaluated box fill (inclusive corners, any order). Uses almost no memory however big
         * the box is; {@code src} is called per block when the queue reaches it. If {@code skipAir} is true,
         * positions that are already air are left alone and cost only 1/8 of a block from the budget (use this to
         * clear mostly-empty regions cheaply).
         */
        public BuildQueue region(World w, int x1, int y1, int z1, int x2, int y2, int z2, BlockSource src, boolean skipAir) {
            if (w == null || src == null) return this;
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.max(w.getMinHeight(), Math.min(y1, y2)), maxY = Math.min(w.getMaxHeight() - 1, Math.max(y1, y2));
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            if (maxY < minY) return this;
            Region r = new Region(worldIdx(w), minX, minY, minZ, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1, src, skipAir);
            regions.add(r);
            push(REGION, regions.size() - 1, 0, 0, 0);
            total += r.volume;
            return this;
        }

        /** Enqueue a callback that runs (main thread) once every operation added before it has been applied. */
        public BuildQueue run(Runnable r) {
            if (r == null) return this;
            runnables.add(r);
            push(RUN, runnables.size() - 1, 0, 0, 0);
            total++;
            return this;
        }

        /** Add a callback for when the queue next becomes empty after {@link #start()} (each runs once). */
        public BuildQueue then(Runnable onCompleteCallback) {
            if (onCompleteCallback != null) onComplete.add(onCompleteCallback);
            return this;
        }

        /**
         * Start (or resume) processing on the next tick. Idempotent while running. If nothing is pending, the
         * {@link #then} callbacks run on the next tick. Does nothing if the plugin is disabled.
         */
        public BuildQueue start() {
            if (task != null || plugin == null || !plugin.isEnabled()) return this;
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
            return this;
        }

        /**
         * Apply everything pending right now, synchronously, then run the {@link #then} callbacks. Only for small
         * builds (it ignores the per-tick budget). Stops a running tick task first.
         */
        public void flush() {
            if (task != null) { task.cancel(); task = null; }
            int gen = generation;
            if (step(Long.MAX_VALUE, Long.MAX_VALUE) && gen == generation) complete();
        }

        /** Stop now and drop all pending operations and {@code then} callbacks. Safe to call any time, repeatedly. */
        public void cancel() {
            if (task != null) { task.cancel(); task = null; }
            generation++;
            reset();
            onComplete.clear();
        }

        /** True while a start()ed queue is still processing. */
        public boolean isRunning() { return task != null; }

        /** Number of block/callback operations still pending (a region counts as one). */
        public int pending() { return count - cursor; }

        /** Total blocks actually placed by this queue so far. */
        public long placed() { return placed; }

        /** Total units (blocks + callbacks, regions by volume) ever added (since the last cancel). */
        public long total() { return total; }

        /** Fraction done in [0, 1] (1 when nothing was ever added). */
        public double progress() { return total <= 0 ? 1.0 : Math.min(1.0, processed / (double) total); }

        private void reset() {
            count = 0; cursor = 0;
            if (ops.length > 5 * 65536) ops = new int[5 * 1024];
            runnables.clear(); regions.clear();
            datas.clear(); dataIndex.clear(); materialIndex.clear(); worlds.clear();
            total = 0; processed = 0;
        }

        private void tick() {
            int gen = generation;
            boolean drained = step((long) blocksPerTick * COST_SET, System.nanoTime() + MAX_NANOS_PER_TICK);
            if (drained && gen == generation && task != null) complete();
        }

        private void complete() {
            if (task != null) { task.cancel(); task = null; }
            // release memory but keep totals for progress()
            long t = total, p = processed;
            reset();
            total = t; processed = p;
            List<Runnable> cbs = new ArrayList<>(onComplete);
            onComplete.clear();
            for (Runnable r : cbs) safe(r);
        }

        private void safe(Runnable r) {
            if (r == null) return;
            try { r.run(); } catch (Throwable t) {
                if (plugin != null) plugin.getLogger().log(Level.SEVERE, "BuildQueue callback threw", t);
            }
        }

        /** Returns true when everything pending has been processed. */
        private boolean step(long budget, long deadline) {
            int gen = generation;
            boolean[] dead = new boolean[worlds.size()];
            for (int i = 0; i < dead.length; i++) dead[i] = Bukkit.getWorld(worlds.get(i).getUID()) == null;
            int sinceCheck = 0;
            while (cursor < count && budget > 0) {
                int b = cursor * 5;
                int kind = ops[b];
                if (kind == RUN) {
                    cursor++; processed++;
                    int ri = ops[b + 1];
                    Runnable r = runnables.get(ri);
                    runnables.set(ri, null);
                    safe(r);
                    if (gen != generation) return false; // cancelled inside callback
                    if (dead.length < worlds.size()) dead = Arrays.copyOf(dead, worlds.size());
                    budget -= COST_SET;
                    if (System.nanoTime() > deadline) break;
                    continue;
                }
                if (kind == REGION) {
                    int gi = ops[b + 1];
                    Region rg = regions.get(gi);
                    boolean isDead = rg.worldIdx < dead.length && dead[rg.worldIdx];
                    World w = worlds.get(rg.worldIdx);
                    long per = (long) rg.sx * rg.sz;
                    if (isDead) { processed += rg.volume - rg.idx; rg.idx = rg.volume; budget -= COST_READ; }
                    while (budget > 0 && rg.idx < rg.volume) {
                        long i = rg.idx++;
                        processed++;
                        if (isDead) { budget -= COST_READ; continue; }
                        int y = rg.y1 + (int) (i / per);
                        long rem = i % per;
                        int x = rg.x1 + (int) (rem / rg.sz);
                        int z = rg.z1 + (int) (rem % rg.sz);
                        BlockData d = rg.src.at(x, y, z);
                        if (d == null) { budget -= COST_READ; }
                        else {
                            org.bukkit.block.Block blk = w.getBlockAt(x, y, z);
                            if (rg.skipAir && blk.getType().isAir()) { budget -= COST_READ; }
                            else { blk.setBlockData(d, false); placed++; budget -= COST_SET; }
                        }
                        if ((++sinceCheck & 1023) == 0 && System.nanoTime() > deadline) { budget = 0; }
                    }
                    if (rg.idx >= rg.volume) { regions.set(gi, null); cursor++; }
                    continue;
                }
                cursor++; processed++;
                if (kind < dead.length && !dead[kind]) {
                    worlds.get(kind).getBlockAt(ops[b + 1], ops[b + 2], ops[b + 3]).setBlockData(datas.get(ops[b + 4]), false);
                    placed++;
                }
                budget -= COST_SET;
                if ((++sinceCheck & 1023) == 0 && System.nanoTime() > deadline) break;
            }
            return cursor >= count;
        }
    }

    // =====================================================================================
    // Shapes
    // =====================================================================================

    private static BlockSource src(Material m) { return BlockSource.of(m); }

    /** Solid box between two corners (inclusive, any order). Lazy: memory-cheap for any size. Returns its volume. */
    public static long fill(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, BlockSource s) {
        q.region(w, x1, y1, z1, x2, y2, z2, s, false);
        return (long) (Math.abs(x2 - x1) + 1) * (Math.abs(y2 - y1) + 1) * (Math.abs(z2 - z1) + 1);
    }
    /** Solid box of one material. */
    public static long fill(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        return fill(q, w, x1, y1, z1, x2, y2, z2, src(m));
    }
    /** Solid box between two locations (world of {@code a}). */
    public static long fill(BuildQueue q, Location a, Location b, BlockSource s) {
        return fill(q, a.getWorld(), a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ(), s);
    }
    /** Solid box between two locations (world of {@code a}). */
    public static long fill(BuildQueue q, Location a, Location b, Material m) { return fill(q, a, b, src(m)); }

    /** All six faces of a box (inclusive corners). The inside is left untouched. */
    public static long hollowBox(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, BlockSource s) {
        int ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
        long n = fill(q, w, ax, ay, az, bx, ay, bz, s);
        if (by > ay) n += fill(q, w, ax, by, az, bx, by, bz, s);
        if (by - ay >= 2) n += walls(q, w, ax, ay + 1, az, bx, by - 1, bz, s);
        return n;
    }
    /** All six faces of a box, one material. */
    public static long hollowBox(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        return hollowBox(q, w, x1, y1, z1, x2, y2, z2, src(m));
    }

    /** The four vertical walls of a box (no floor or ceiling). */
    public static long walls(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, BlockSource s) {
        int ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
        long n = fill(q, w, ax, ay, az, bx, by, az, s);
        if (bz > az) n += fill(q, w, ax, ay, bz, bx, by, bz, s);
        if (bz - az >= 2) {
            n += fill(q, w, ax, ay, az + 1, ax, by, bz - 1, s);
            if (bx > ax) n += fill(q, w, bx, ay, az + 1, bx, by, bz - 1, s);
        }
        return n;
    }
    /** The four vertical walls of a box, one material. */
    public static long walls(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        return walls(q, w, x1, y1, z1, x2, y2, z2, src(m));
    }

    /** Sets every block between two locations (world of {@code min}) to air, skipping blocks that are already air (cheap on void worlds). */
    public static long clearRegion(BuildQueue q, World w, Location min, Location max) {
        q.region(w, min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(), max.getBlockZ(),
                BlockSource.of(Material.AIR), true);
        return (long) (Math.abs(max.getBlockX() - min.getBlockX()) + 1) * (Math.abs(max.getBlockY() - min.getBlockY()) + 1)
                * (Math.abs(max.getBlockZ() - min.getBlockZ()) + 1);
    }

    private interface Inside { boolean test(int x, int y, int z); }

    private static int emitBox(BuildQueue q, World w, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                               Inside in, boolean hollow, BlockSource s) {
        int n = 0;
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            if (!in.test(x, y, z)) continue;
            if (hollow && in.test(x + 1, y, z) && in.test(x - 1, y, z) && in.test(x, y + 1, z) && in.test(x, y - 1, z)
                    && in.test(x, y, z + 1) && in.test(x, y, z - 1)) continue;
            BlockData d = s.at(x, y, z);
            if (d == null) continue;
            q.set(w, x, y, z, d);
            n++;
        }
        return n;
    }

    /** Sphere. {@code hollow} = one-block-thick shell. */
    public static int sphere(BuildQueue q, World w, double cx, double cy, double cz, double radius, BlockSource s, boolean hollow) {
        return ellipsoid(q, w, cx, cy, cz, radius, radius, radius, s, hollow);
    }
    /** Sphere, one material. */
    public static int sphere(BuildQueue q, World w, double cx, double cy, double cz, double radius, Material m, boolean hollow) {
        return sphere(q, w, cx, cy, cz, radius, src(m), hollow);
    }

    /** Axis-aligned ellipsoid with radii rx, ry, rz. {@code hollow} = one-block-thick shell. */
    public static int ellipsoid(BuildQueue q, World w, double cx, double cy, double cz, double rx, double ry, double rz, BlockSource s, boolean hollow) {
        final double ex = rx + 0.25, ey = ry + 0.25, ez = rz + 0.25;
        Inside in = (x, y, z) -> {
            double dx = (x - cx) / ex, dy = (y - cy) / ey, dz = (z - cz) / ez;
            return dx * dx + dy * dy + dz * dz <= 1.0;
        };
        return emitBox(q, w, (int) Math.floor(cx - ex), (int) Math.floor(cy - ey), (int) Math.floor(cz - ez),
                (int) Math.ceil(cx + ex), (int) Math.ceil(cy + ey), (int) Math.ceil(cz + ez), in, hollow, s);
    }
    /** Ellipsoid, one material. */
    public static int ellipsoid(BuildQueue q, World w, double cx, double cy, double cz, double rx, double ry, double rz, Material m, boolean hollow) {
        return ellipsoid(q, w, cx, cy, cz, rx, ry, rz, src(m), hollow);
    }

    /** Vertical cylinder from {@code y} up to {@code y + height - 1}. {@code hollow} = walls only (no top/bottom caps). */
    public static int cylinder(BuildQueue q, World w, double cx, int y, double cz, double radius, int height, BlockSource s, boolean hollow) {
        if (height <= 0) return 0;
        final double e = radius + 0.25;
        int n = 0;
        int minX = (int) Math.floor(cx - e), maxX = (int) Math.ceil(cx + e), minZ = (int) Math.floor(cz - e), maxZ = (int) Math.ceil(cz + e);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!inCircle(x, z, cx, cz, e)) continue;
            if (hollow && inCircle(x + 1, z, cx, cz, e) && inCircle(x - 1, z, cx, cz, e) && inCircle(x, z + 1, cx, cz, e) && inCircle(x, z - 1, cx, cz, e)) continue;
            for (int yy = y; yy < y + height; yy++) {
                BlockData d = s.at(x, yy, z);
                if (d != null) { q.set(w, x, yy, z, d); n++; }
            }
        }
        return n;
    }
    /** Vertical cylinder, one material. */
    public static int cylinder(BuildQueue q, World w, double cx, int y, double cz, double radius, int height, Material m, boolean hollow) {
        return cylinder(q, w, cx, y, cz, radius, height, src(m), hollow);
    }

    private static boolean inCircle(int x, int z, double cx, double cz, double e) {
        double dx = x - cx, dz = z - cz;
        return dx * dx + dz * dz <= e * e;
    }

    /**
     * Vertical cone with its base disc at {@code y} and its tip {@code |height| - 1} blocks away: upward for a
     * positive height, downward (stalactite / island underside) for a negative one. {@code hollow} = outer
     * one-block shell of each layer only.
     */
    public static int cone(BuildQueue q, World w, double cx, int y, double cz, double baseRadius, int height, BlockSource s, boolean hollow) {
        if (height == 0) return 0;
        int h = Math.abs(height), dir = height > 0 ? 1 : -1;
        int n = 0;
        for (int i = 0; i < h; i++) {
            double r = baseRadius * (1.0 - i / (double) h);
            int yy = y + i * dir;
            if (hollow) n += ring(q, w, cx, yy, cz, r, s);
            else n += disc(q, w, cx, yy, cz, r, s);
        }
        return n;
    }
    /** Cone, one material. */
    public static int cone(BuildQueue q, World w, double cx, int y, double cz, double baseRadius, int height, Material m, boolean hollow) {
        return cone(q, w, cx, y, cz, baseRadius, height, src(m), hollow);
    }

    /** Filled horizontal disc at height {@code y}. Always includes at least the centre block. */
    public static int disc(BuildQueue q, World w, double cx, int y, double cz, double radius, BlockSource s) {
        final double e = Math.max(0, radius) + 0.25;
        int n = 0;
        int minX = (int) Math.floor(cx - e), maxX = (int) Math.ceil(cx + e), minZ = (int) Math.floor(cz - e), maxZ = (int) Math.ceil(cz + e);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!inCircle(x, z, cx, cz, e) && !(x == (int) Math.round(cx) && z == (int) Math.round(cz))) continue;
            BlockData d = s.at(x, y, z);
            if (d != null) { q.set(w, x, y, z, d); n++; }
        }
        return n;
    }
    /** Filled horizontal disc, one material. */
    public static int disc(BuildQueue q, World w, double cx, int y, double cz, double radius, Material m) {
        return disc(q, w, cx, y, cz, radius, src(m));
    }

    private static int ring(BuildQueue q, World w, double cx, int y, double cz, double radius, BlockSource s) {
        final double e = Math.max(0, radius) + 0.25;
        int n = 0;
        int minX = (int) Math.floor(cx - e), maxX = (int) Math.ceil(cx + e), minZ = (int) Math.floor(cz - e), maxZ = (int) Math.ceil(cz + e);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            boolean in = inCircle(x, z, cx, cz, e) || (x == (int) Math.round(cx) && z == (int) Math.round(cz));
            if (!in) continue;
            if (inCircle(x + 1, z, cx, cz, e) && inCircle(x - 1, z, cx, cz, e) && inCircle(x, z + 1, cx, cz, e) && inCircle(x, z - 1, cx, cz, e)) continue;
            BlockData d = s.at(x, y, z);
            if (d != null) { q.set(w, x, y, z, d); n++; }
        }
        return n;
    }

    /** Straight 3D line of blocks between two block positions (inclusive). */
    public static int line(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, BlockSource s) {
        int dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int n = 0;
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + dx * t), y = (int) Math.round(y1 + dy * t), z = (int) Math.round(z1 + dz * t);
            BlockData d = s.at(x, y, z);
            if (d != null) { q.set(w, x, y, z, d); n++; }
        }
        return n;
    }
    /** Straight line, one material. */
    public static int line(BuildQueue q, World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        return line(q, w, x1, y1, z1, x2, y2, z2, src(m));
    }
    /** Straight line between two locations (world of {@code a}). */
    public static int line(BuildQueue q, Location a, Location b, BlockSource s) {
        return line(q, a.getWorld(), a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ(), s);
    }

    // =====================================================================================
    // Natural features
    // =====================================================================================

    private static final Palette DEFAULT_SOIL = Palette.of(Material.DIRT, 7, Material.COARSE_DIRT, 1, Material.ROOTED_DIRT, 1);

    /**
     * {@link #floatingIsland(BuildQueue, World, Location, int, int, Palette, Palette, Palette, long)} with a dirt
     * soil layer (dirt / coarse dirt / rooted dirt). For non-grass tops (sand, snow...) use the overload with
     * your own soil palette.
     */
    public static int floatingIsland(BuildQueue q, World w, Location center, int radius, int depth, Palette top, Palette under, long seed) {
        return floatingIsland(q, w, center, radius, depth, top, DEFAULT_SOIL, under, seed);
    }

    /**
     * Natural-looking floating island. {@code center} is the middle of the top surface. The outline is a noisy
     * circle of about {@code radius} (0.8x - 1.1x), the top is a gentle 0-2 block dome made of one layer of
     * {@code top} over 2-3 layers of {@code soil}, and the underside tapers to about {@code depth} blocks below
     * the centre with noisy, stalactite-like spikes ({@code under} palette). Deterministic for a seed.
     * The top surface of each column is between {@code center.y} and {@code center.y + 2}.
     *
     * @return number of blocks enqueued
     */
    public static int floatingIsland(BuildQueue q, World w, Location center, int radius, int depth,
                                     Palette top, Palette soil, Palette under, long seed) {
        Noise n = new Noise(seed);
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int ext = (int) Math.ceil(radius * 1.15) + 1;
        Palette topP = top.seeded(seed), soilP = soil.seeded(seed + 1), underP = under.seeded(seed + 2);
        int count = 0;
        for (int dx = -ext; dx <= ext; dx++) for (int dz = -ext; dz <= ext; dz++) {
            double edge = radius * (0.95 + 0.17 * n.fbm2(dx * 0.05, dz * 0.05, 3));
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > edge || edge <= 0) continue;
            double t = dist / edge;
            int topOff = (int) Math.round((1 - t * t) * 2.0 * (0.5 + 0.5 * n.noise2(dx * 0.09 + 100, dz * 0.09 + 100)));
            double taper = Math.pow(1 - t, 1.25);
            double rough = 0.75 + 0.5 * (0.5 + 0.5 * n.fbm2(dx * 0.12 + 300, dz * 0.12 + 300, 2));
            int d = (int) Math.round(depth * taper * rough);
            double spike = n.noise2(dx * 0.33 + 700, dz * 0.33 + 700);
            if (spike > 0.5) d += (int) Math.round((spike - 0.5) / 0.5 * depth * 0.45 * (1 - t * 0.4));
            d = Math.max(1, d);
            int x = cx + dx, z = cz + dz;
            int yTop = cy + topOff;
            int soilDepth = 2 + (hash01(x, 0, z, seed) < 0.5 ? 1 : 0);
            int yBottom = cy - d;
            for (int y = yTop; y >= yBottom; y--) {
                int k = yTop - y;
                BlockData bd = k == 0 ? topP.at(x, y, z) : (k <= soilDepth ? soilP.at(x, y, z) : underP.at(x, y, z));
                q.set(w, x, y, z, bd);
                count++;
            }
        }
        return count;
    }

    /** {@link #cloud(BuildQueue, World, Location, int, long, Palette)} with white concrete / wool / snow. */
    public static int cloud(BuildQueue q, World w, Location center, int size, long seed) {
        return cloud(q, w, center, size, seed, Palette.of(Material.WHITE_CONCRETE, 4, Material.WHITE_WOOL, 3, Material.SNOW_BLOCK, 1));
    }

    /**
     * Puffy cloud: several overlapping flattened blobs spread mostly along X, with a flat-ish bottom.
     * {@code size} is roughly the cloud's length in blocks (8 - 60 looks good). Decorative only - keep clouds
     * outside the play area.
     */
    public static int cloud(BuildQueue q, World w, Location center, int size, long seed, Palette palette) {
        Random r = new Random(seed);
        Palette p = palette.seeded(seed);
        int s = Math.max(4, size);
        int blobs = 3 + s / 5;
        Map<Long, BlockData> buf = new LinkedHashMap<>();
        double cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int b = 0; b < blobs; b++) {
            double ox = (r.nextDouble() - 0.5) * s * 0.8;
            double oz = (r.nextDouble() - 0.5) * s * 0.35;
            double rr = s * (0.14 + r.nextDouble() * 0.14) * (1.0 - Math.abs(ox) / s);
            rr = Math.max(2.0, rr);
            double ry = rr * 0.55, rz = rr * 0.85;
            double bx = cx + ox, bz = cz + oz, by = cy + (r.nextDouble() * 0.6) * ry;
            double floor = cy - ry * 0.35;
            for (int x = (int) Math.floor(bx - rr - 1); x <= (int) Math.ceil(bx + rr + 1); x++)
                for (int y = (int) Math.floor(by - ry - 1); y <= (int) Math.ceil(by + ry + 1); y++)
                    for (int z = (int) Math.floor(bz - rz - 1); z <= (int) Math.ceil(bz + rz + 1); z++) {
                        if (y < floor) continue;
                        double dx = (x - bx) / (rr + 0.25), dy = (y - by) / (ry + 0.25), dz = (z - bz) / (rz + 0.25);
                        if (dx * dx + dy * dy + dz * dz > 1.0) continue;
                        buf.putIfAbsent(pack(x, y, z), p.at(x, y, z));
                    }
        }
        return flushBuf(q, w, buf);
    }

    /** Tree styles for {@link #tree}. All leaves are placed persistent (they never decay). */
    public enum TreeStyle {
        /** Oak trunk 5-7 high with a lumpy round canopy. */
        OAK_ROUND,
        /** Tall spruce with layered, alternating cone-shaped canopy. */
        SPRUCE_CONE,
        /** Slim birch with a tall oval canopy. */
        BIRCH,
        /** Short cherry trunk splitting into 2-3 branches with wide pink canopies and hanging blossoms. */
        CHERRY,
        /** Low round azalea bush (azalea + flowering azalea leaves) on a 1-block stem. */
        AZALEA_BUSH,
        /** Leafless dark-oak trunk with crooked branches. */
        DEAD,
        /** Leaning jungle-log palm with drooping frond leaves. */
        PALM
    }

    /** {@link #tree(BuildQueue, World, Location, TreeStyle, long, double)} at scale 1. */
    public static int tree(BuildQueue q, World w, Location base, TreeStyle style, long seed) {
        return tree(q, w, base, style, seed, 1.0);
    }

    /**
     * Custom tree with its trunk starting at {@code base} (the air block just above the ground) and growing up.
     * {@code scale} multiplies heights and canopy radii (0.6 - 2.0 sensible). Deterministic for a seed.
     * @return number of blocks enqueued
     */
    public static int tree(BuildQueue q, World w, Location base, TreeStyle style, long seed, double scale) {
        Random r = new Random(seed);
        double s = Math.max(0.3, scale);
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        Map<Long, BlockData> buf = new LinkedHashMap<>();
        switch (style) {
            case OAK_ROUND -> {
                int h = (int) Math.round((5 + r.nextInt(3)) * s);
                double R = (2.6 + r.nextDouble() * 0.8) * s;
                BlockData lv = leaves(Material.OAK_LEAVES);
                blob(buf, bx, by + h - 1, bz, R, R * 0.8, R, lv, seed, 0.18);
                for (int i = 0; i < 2; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    blob(buf, bx + Math.cos(a) * R * 0.6, by + h - 2, bz + Math.sin(a) * R * 0.6, R * 0.65, R * 0.55, R * 0.65, lv, seed + i + 1, 0.2);
                }
                trunk(buf, bx, by, bz, h, log(Material.OAK_LOG, Axis.Y));
            }
            case SPRUCE_CONE -> {
                int h = (int) Math.round((9 + r.nextInt(4)) * s);
                double maxR = 3.2 * s;
                BlockData lv = leaves(Material.SPRUCE_LEAVES);
                int start = by + Math.max(2, (int) Math.round(2 * s));
                for (int y = start; y <= by + h; y++) {
                    double frac = (y - start) / (double) Math.max(1, by + h - start);
                    double rad = (1 - frac) * maxR;
                    if (((y - start) & 1) == 1) rad *= 0.6;
                    discBuf(buf, bx, y, bz, rad, lv, seed, 0.08);
                }
                buf.put(pack(bx, by + h + 1, bz), lv);
                trunk(buf, bx, by, bz, h, log(Material.SPRUCE_LOG, Axis.Y));
            }
            case BIRCH -> {
                int h = (int) Math.round((6 + r.nextInt(3)) * s);
                BlockData lv = leaves(Material.BIRCH_LEAVES);
                blob(buf, bx, by + h - 1.5, bz, 2.2 * s, 3.0 * s, 2.2 * s, lv, seed, 0.15);
                trunk(buf, bx, by, bz, h, log(Material.BIRCH_LOG, Axis.Y));
            }
            case CHERRY -> {
                int h = (int) Math.round((4 + r.nextInt(2)) * s);
                BlockData lv = leaves(Material.CHERRY_LEAVES);
                BlockData wood = Material.CHERRY_WOOD.createBlockData();
                int branches = 2 + r.nextInt(2);
                double a0 = r.nextDouble() * Math.PI * 2;
                List<int[]> ends = new ArrayList<>();
                for (int i = 0; i < branches; i++) {
                    double a = a0 + i * (Math.PI * 2 / branches) + (r.nextDouble() - 0.5) * 0.6;
                    double len = (3 + r.nextDouble() * 1.5) * s;
                    int ex = bx + (int) Math.round(Math.cos(a) * len), ez = bz + (int) Math.round(Math.sin(a) * len);
                    int ey = by + h + (int) Math.round((1.5 + r.nextDouble()) * s);
                    ends.add(new int[]{ex, ey, ez});
                }
                for (int[] e : ends) blob(buf, e[0], e[1] + 1, e[2], 3.2 * s, 1.8 * s, 3.2 * s, lv, seed + e[0] * 31L + e[2], 0.12);
                hangBelow(buf, lv, seed, 0.3);
                for (int[] e : ends) lineBuf(buf, bx, by + h - 1, bz, e[0], e[1], e[2], wood);
                trunk(buf, bx, by, bz, h, log(Material.CHERRY_LOG, Axis.Y));
            }
            case AZALEA_BUSH -> {
                BlockData a = leaves(Material.AZALEA_LEAVES), f = leaves(Material.FLOWERING_AZALEA_LEAVES);
                double rx = 2.3 * s, ry = 1.6 * s;
                int stem = Math.max(1, (int) Math.round(s));
                double cyy = by + stem + ry * 0.4;
                for (int x = (int) Math.floor(bx - rx - 1); x <= (int) Math.ceil(bx + rx + 1); x++)
                    for (int y = by; y <= (int) Math.ceil(cyy + ry + 1); y++)
                        for (int z = (int) Math.floor(bz - rx - 1); z <= (int) Math.ceil(bz + rx + 1); z++) {
                            double dx = (x - bx) / (rx + 0.25), dy = (y - cyy) / (ry + 0.25), dz = (z - bz) / (rx + 0.25);
                            double dd = dx * dx + dy * dy + dz * dz;
                            if (dd > 1.0) continue;
                            if (dd > 0.6 && hash01(x, y, z, seed) < 0.15) continue;
                            buf.put(pack(x, y, z), hash01(x, y, z, seed + 5) < 0.4 ? f : a);
                        }
                trunk(buf, bx, by, bz, stem, log(Material.OAK_LOG, Axis.Y));
            }
            case DEAD -> {
                int h = (int) Math.round((5 + r.nextInt(4)) * s);
                BlockData wood = Material.DARK_OAK_WOOD.createBlockData();
                int branches = 3 + r.nextInt(2);
                for (int i = 0; i < branches; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    int sy = by + h / 2 + r.nextInt(Math.max(1, h / 2));
                    double len = (2 + r.nextDouble() * 2.5) * s;
                    int ex = bx + (int) Math.round(Math.cos(a) * len), ez = bz + (int) Math.round(Math.sin(a) * len);
                    int ey = sy + 1 + r.nextInt(Math.max(1, (int) Math.round(3 * s)));
                    lineBuf(buf, bx, sy, bz, ex, ey, ez, wood);
                    if (r.nextBoolean()) {
                        double a2 = a + (r.nextBoolean() ? 0.8 : -0.8);
                        lineBuf(buf, ex, ey, ez, ex + (int) Math.round(Math.cos(a2) * 1.5 * s), ey + 1, ez + (int) Math.round(Math.sin(a2) * 1.5 * s), wood);
                    }
                }
                trunk(buf, bx, by, bz, h, log(Material.DARK_OAK_LOG, Axis.Y));
            }
            case PALM -> {
                int h = (int) Math.round((7 + r.nextInt(3)) * s);
                double lean = r.nextDouble() * Math.PI * 2;
                double leanAmt = (2.0 + r.nextDouble()) * s;
                BlockData logD = log(Material.JUNGLE_LOG, Axis.Y);
                BlockData lv = leaves(Material.JUNGLE_LEAVES);
                int tx = bx, tz = bz;
                List<int[]> trunkPts = new ArrayList<>();
                for (int i = 0; i < h; i++) {
                    double f = i / (double) h;
                    tx = bx + (int) Math.round(Math.cos(lean) * leanAmt * f * f);
                    tz = bz + (int) Math.round(Math.sin(lean) * leanAmt * f * f);
                    trunkPts.add(new int[]{tx, by + i, tz});
                }
                int ty = by + h;
                int fronds = 7 + r.nextInt(2);
                for (int i = 0; i < fronds; i++) {
                    double a = i * (Math.PI * 2 / fronds) + (r.nextDouble() - 0.5) * 0.4;
                    int len = (int) Math.round((3.5 + r.nextDouble()) * s);
                    for (int j = 1; j <= len; j++) {
                        int fx = tx + (int) Math.round(Math.cos(a) * j), fz = tz + (int) Math.round(Math.sin(a) * j);
                        int fy = ty - (int) Math.floor(j * j / (len * 1.3));
                        buf.put(pack(fx, fy, fz), lv);
                        if (j == len) buf.put(pack(fx, fy - 1, fz), lv);
                    }
                }
                buf.put(pack(tx, ty, tz), lv);
                buf.put(pack(tx, ty + 1, tz), lv);
                for (int[] p : trunkPts) buf.put(pack(p[0], p[1], p[2]), logD);
            }
        }
        return flushBuf(q, w, buf);
    }

    /** Ground blocks plants usually grow on (for {@link #scatter}'s {@code onlyOn}). */
    public static final Set<Material> NATURAL_GROUND = Collections.unmodifiableSet(EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.PODZOL,
            Material.MYCELIUM, Material.MOSS_BLOCK, Material.MUD, Material.MUDDY_MANGROVE_ROOTS));

    /** {@link #scatter(BuildQueue, World, Location, Location, Palette, double, long, Set)} on any solid, occluding top surface. */
    public static void scatter(BuildQueue q, World w, Location min, Location max, Palette plants, double chance, long seed) {
        scatter(q, w, min, max, plants, chance, seed, null);
    }

    /**
     * Decorates top surfaces: for each column in the region, finds the highest non-air block; if it is an
     * occluding solid (and in {@code onlyOn}, when given) and the block above is air, places a plant from
     * {@code plants} there with probability {@code chance} (deterministic per column). Two-block plants
     * (tall grass, rose bush, sunflower...) get their upper half too when there is room.
     * The scan runs <i>inside the queue</i> (one x-slice per step), so call it after enqueuing the terrain and it
     * sees the terrain as built.
     */
    public static void scatter(BuildQueue q, World w, Location min, Location max, Palette plants, double chance, long seed, Set<Material> onlyOn) {
        final int x1 = Math.min(min.getBlockX(), max.getBlockX()), x2 = Math.max(min.getBlockX(), max.getBlockX());
        final int z1 = Math.min(min.getBlockZ(), max.getBlockZ()), z2 = Math.max(min.getBlockZ(), max.getBlockZ());
        final int yLo = Math.max(w.getMinHeight(), Math.min(min.getBlockY(), max.getBlockY()));
        final int yHi = Math.min(w.getMaxHeight() - 2, Math.max(min.getBlockY(), max.getBlockY()));
        final Palette p = plants.seeded(seed);
        for (int xx = x1; xx <= x2; xx++) {
            final int x = xx;
            q.run(() -> {
                if (Bukkit.getWorld(w.getUID()) == null) return;
                for (int z = z1; z <= z2; z++) {
                    if (hash01(x, 7, z, seed) >= chance) continue;
                    for (int y = yHi; y >= yLo; y--) {
                        Material m = w.getBlockAt(x, y, z).getType();
                        if (m.isAir()) continue;
                        if (m.isOccluding() && (onlyOn == null || onlyOn.contains(m)) && w.getBlockAt(x, y + 1, z).getType().isAir()) {
                            BlockData d = p.at(x, y + 1, z);
                            if (d instanceof Bisected bi) {
                                if (w.getBlockAt(x, y + 2, z).getType().isAir()) {
                                    Bisected lower = (Bisected) bi.clone();
                                    lower.setHalf(Bisected.Half.BOTTOM);
                                    Bisected upper = (Bisected) bi.clone();
                                    upper.setHalf(Bisected.Half.TOP);
                                    w.getBlockAt(x, y + 1, z).setBlockData(lower, false);
                                    w.getBlockAt(x, y + 2, z).setBlockData(upper, false);
                                }
                            } else if (d != null) {
                                w.getBlockAt(x, y + 1, z).setBlockData(d, false);
                            }
                        }
                        break;
                    }
                }
            });
        }
    }

    // ----- tree / buffer helpers -----

    private static BlockData leaves(Material m) {
        BlockData d = m.createBlockData();
        if (d instanceof Leaves l) l.setPersistent(true);
        return d;
    }

    private static BlockData log(Material m, Axis a) {
        BlockData d = m.createBlockData();
        if (d instanceof Orientable o && o.getAxes().contains(a)) o.setAxis(a);
        return d;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int flushBuf(BuildQueue q, World w, Map<Long, BlockData> buf) {
        for (Map.Entry<Long, BlockData> e : buf.entrySet()) {
            long k = e.getKey();
            int x = ((int) (k >> 38) << 6) >> 6;
            int z = ((int) ((k >>> 12) & 0x3FFFFFF) << 6) >> 6;
            int y = ((int) (k & 0xFFF) << 20) >> 20;
            q.set(w, x, y, z, e.getValue());
        }
        return buf.size();
    }

    private static void trunk(Map<Long, BlockData> buf, int x, int y, int z, int h, BlockData d) {
        for (int i = 0; i < h; i++) buf.put(pack(x, y + i, z), d);
    }

    private static void blob(Map<Long, BlockData> buf, double cx, double cy, double cz, double rx, double ry, double rz,
                             BlockData d, long seed, double holes) {
        double ex = rx + 0.25, ey = ry + 0.25, ez = rz + 0.25;
        for (int x = (int) Math.floor(cx - ex); x <= (int) Math.ceil(cx + ex); x++)
            for (int y = (int) Math.floor(cy - ey); y <= (int) Math.ceil(cy + ey); y++)
                for (int z = (int) Math.floor(cz - ez); z <= (int) Math.ceil(cz + ez); z++) {
                    double dx = (x - cx) / ex, dy = (y - cy) / ey, dz = (z - cz) / ez;
                    double dd = dx * dx + dy * dy + dz * dz;
                    if (dd > 1.0) continue;
                    if (dd > 0.65 && hash01(x, y, z, seed) < holes) continue;
                    buf.put(pack(x, y, z), d);
                }
    }

    private static void discBuf(Map<Long, BlockData> buf, int cx, int y, int cz, double r, BlockData d, long seed, double holes) {
        double e = Math.max(0, r) + 0.25;
        for (int x = (int) Math.floor(cx - e); x <= (int) Math.ceil(cx + e); x++)
            for (int z = (int) Math.floor(cz - e); z <= (int) Math.ceil(cz + e); z++) {
                double dx = x - cx, dz = z - cz;
                double dd = dx * dx + dz * dz;
                if (dd > e * e && !(x == cx && z == cz)) continue;
                if (dd > (e - 1) * (e - 1) && e > 1.5 && hash01(x, y, z, seed) < holes) continue;
                buf.put(pack(x, y, z), d);
            }
    }

    private static void lineBuf(Map<Long, BlockData> buf, int x1, int y1, int z1, int x2, int y2, int z2, BlockData d) {
        int dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            buf.put(pack((int) Math.round(x1 + dx * t), (int) Math.round(y1 + dy * t), (int) Math.round(z1 + dz * t)), d);
        }
    }

    /** Adds 1-2 hanging leaves under the lowest leaf of some columns (cherry blossoms). */
    private static void hangBelow(Map<Long, BlockData> buf, BlockData lv, long seed, double chance) {
        Map<Long, int[]> lowest = new HashMap<>();
        for (Map.Entry<Long, BlockData> e : buf.entrySet()) {
            if (e.getValue() != lv) continue;
            long k = e.getKey();
            int x = ((int) (k >> 38) << 6) >> 6;
            int z = ((int) ((k >>> 12) & 0x3FFFFFF) << 6) >> 6;
            int y = ((int) (k & 0xFFF) << 20) >> 20;
            long col = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
            int[] cur = lowest.get(col);
            if (cur == null || y < cur[1]) lowest.put(col, new int[]{x, y, z});
        }
        for (int[] p : lowest.values()) {
            double h = hash01(p[0], p[1], p[2], seed + 99);
            if (h >= chance) continue;
            buf.putIfAbsent(pack(p[0], p[1] - 1, p[2]), lv);
            if (h < chance * 0.4) buf.putIfAbsent(pack(p[0], p[1] - 2, p[2]), lv);
        }
    }

    // =====================================================================================
    // Region dump (previews)
    // =====================================================================================

    /** Result of {@link #dumpRegion}. {@code error} is null on success; a {@link CancellationException} if aborted. */
    public record DumpResult(File file, long blocks, int sizeX, int sizeY, int sizeZ, Throwable error) {
        public boolean success() { return error == null; }
    }

    /** {@link #dumpRegion(World, Location, Location, File, Consumer)} without a callback. */
    public static BukkitTask dumpRegion(World w, Location min, Location max, File out) {
        return dumpRegion(w, min, max, out, null);
    }

    /**
     * Writes every non-air block of a region to a gzipped text file for the preview renderer:
     * first line {@code MCMDUMP 1 <sx> <sy> <sz>}, then one line {@code x y z namespaced_id} per non-air block
     * (coordinates relative to the region's minimum corner, e.g. {@code 3 0 5 minecraft:stone}); block order is
     * by chunk, not sorted. Corners may be given in any order; y is clamped to the world height.
     *
     * <p>Chunk snapshots are taken on the main thread a few chunks per tick (loading chunks if needed); the
     * file is written on a worker thread. {@code onDone} (may be null) runs on the main thread when finished or
     * failed. Parent directories are created. Cancelling the returned task aborts the dump (the partial file is
     * deleted and onDone receives a CancellationException). Scheduled under the core plugin.
     */
    public static BukkitTask dumpRegion(World w, Location min, Location max, File out, Consumer<DumpResult> onDone) {
        final Plugin plugin = JavaPlugin.getProvidingPlugin(McmBuild.class);
        final int minX = Math.min(min.getBlockX(), max.getBlockX()), maxX = Math.max(min.getBlockX(), max.getBlockX());
        final int minZ = Math.min(min.getBlockZ(), max.getBlockZ()), maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        final int minY = Math.max(w.getMinHeight(), Math.min(min.getBlockY(), max.getBlockY()));
        final int maxY = Math.min(w.getMaxHeight() - 1, Math.max(min.getBlockY(), max.getBlockY()));
        final int sx = maxX - minX + 1, sy = Math.max(0, maxY - minY + 1), sz = maxZ - minZ + 1;
        final int cx0 = minX >> 4, cx1 = maxX >> 4, cz0 = minZ >> 4, cz1 = maxZ >> 4;
        final int chunksX = cx1 - cx0 + 1;
        final int totalChunks = chunksX * (cz1 - cz0 + 1);
        final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(48);
        final Object end = new Object();
        final AtomicBoolean endSent = new AtomicBoolean(false);
        final AtomicBoolean failed = new AtomicBoolean(false);
        final int perTick = 6;
        File parent = out.getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();

        record Snap(int cx, int cz, ChunkSnapshot snap) {}

        final BukkitTask reader = new BukkitRunnable() {
            int ci = 0;
            @Override public void run() {
                if (failed.get() || Bukkit.getWorld(w.getUID()) == null) { cancel(); return; }
                int n = 0;
                while (ci < totalChunks && n < perTick && queue.remainingCapacity() > 1) {
                    int cx = cx0 + ci % chunksX, cz = cz0 + ci / chunksX;
                    ChunkSnapshot s = w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false);
                    queue.offer(new Snap(cx, cz, s));
                    ci++; n++;
                }
                if (ci >= totalChunks && queue.offer(end)) { endSent.set(true); cancel(); }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long blocks = 0;
            Throwable error = null;
            Map<Material, String> keys = new EnumMap<>(Material.class);
            try (BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(
                    new GZIPOutputStream(new FileOutputStream(out)), StandardCharsets.UTF_8), 1 << 16)) {
                wr.write("MCMDUMP 1 " + sx + " " + sy + " " + sz + "\n");
                StringBuilder sb = new StringBuilder(64);
                while (true) {
                    Object o = queue.poll(500, TimeUnit.MILLISECONDS);
                    if (o == null) {
                        if (reader.isCancelled() && !endSent.get()) throw new CancellationException("dump aborted");
                        continue;
                    }
                    if (o == end) break;
                    Snap s = (Snap) o;
                    int bx = s.cx() << 4, bz = s.cz() << 4;
                    int xa = Math.max(minX, bx), xb = Math.min(maxX, bx + 15);
                    int za = Math.max(minZ, bz), zb = Math.min(maxZ, bz + 15);
                    for (int x = xa; x <= xb; x++) for (int z = za; z <= zb; z++) for (int y = minY; y <= maxY; y++) {
                        Material m = s.snap().getBlockType(x - bx, y, z - bz);
                        if (m.isAir()) continue;
                        String key = keys.computeIfAbsent(m, mm -> mm.getKey().toString());
                        sb.setLength(0);
                        sb.append(x - minX).append(' ').append(y - minY).append(' ').append(z - minZ).append(' ').append(key).append('\n');
                        wr.write(sb.toString());
                        blocks++;
                    }
                }
            } catch (Throwable t) {
                error = t;
                failed.set(true);
                out.delete();
            }
            final DumpResult res = new DumpResult(out, blocks, sx, sy, sz, error);
            if (error != null && !(error instanceof CancellationException)) {
                plugin.getLogger().log(Level.WARNING, "dumpRegion to " + out + " failed", error);
            }
            if (onDone != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try { onDone.accept(res); } catch (Throwable t) { plugin.getLogger().log(Level.SEVERE, "dumpRegion callback threw", t); }
                });
            }
        });
        return reader;
    }

    /** Returns {@code Arrays.asList} style list of the tree styles (convenience for random picks). */
    public static List<TreeStyle> treeStyles() { return Arrays.asList(TreeStyle.values()); }
}
