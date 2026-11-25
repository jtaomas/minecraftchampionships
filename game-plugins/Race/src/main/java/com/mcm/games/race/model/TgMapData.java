package com.mcm.games.race.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java output of a {@link TgMap} generator (no Bukkit types). Contract between the runtime (package
 * {@code com.mcm.games.race}) and the map generators ({@code com.mcm.games.race.maps.a/b}).
 *
 * <h2>Coordinates</h2>
 * Local integer block coordinates. The runtime pastes local (0,0,0) at world
 * ({@code slotIndex * map-spacing}, {@code base-y}, 0) in world {@code mcc_race} (defaults: spacing 600,
 * base-y 64). Keep every block within local x/z -250..+250 and y -60..+180. Recommended (not required):
 * start strip around z = 0, course running towards +Z, goal pen at the far end.
 *
 * <h2>Blocks</h2>
 * Vanilla block-state strings like {@code minecraft:oak_stairs[facing=north,half=top]} ("minecraft:" optional).
 * Placed without physics, so set connection states yourself (fences, panes, walls). Never use the 16 dyed
 * {@code *_concrete} blocks: concrete is reserved for players (only player-placed concrete may be broken).
 *
 * <h2>Run-time sequence</h2>
 * <ol>
 *   <li>Place {@link #blocks}; then for every {@link #variants} group place the block list of the option the
 *       runtime picked this round (variant placements overwrite {@link #blocks}).</li>
 *   <li>Pre-round: players stand at {@link #startSpawns} (boat maps: seated in boats at {@link #boatSpawns}).
 *       {@link #startBarrier} positions must hold real blocks in {@link #blocks} (e.g. glass/barrier) that fence
 *       the strip in; the runtime additionally teleports anyone who leaves {@link #startStrip} back.</li>
 *   <li>GO: every {@link #startBarrier} and {@link #dropFloor} position is set to air.</li>
 *   <li>Racing: below {@link #voidY}, outside {@link #playBounds}, inside a {@link #hazardRegions} box, or
 *       touching a {@link #hazardBlocks} type (feet block or block below) = respawn at a start spawn.</li>
 *   <li>Finish: punching a goal chicken (spawned at {@link #chickenSpawns}, kept inside {@link #pen}, visible name
 *       tag). Unnamed decoys from {@link TgMap#decoyChickens()} do nothing. Optional lethal falls:
 *       {@link TgMap#lethalFallHeight()}.</li>
 *   <li>Round end: the runtime re-pastes the original {@link #blocks} over every changed position.</li>
 * </ol>
 */
public final class TgMapData {

    /** Integer block position. */
    public record Pos(int x, int y, int z) {}

    /** Feet position + facing (Minecraft yaw: 0 = +Z/south, 90 = -X/west, 180 = -Z, -90 = +X). */
    public record Spawn(double x, double y, double z, float yaw, float pitch) {
        public Spawn(double x, double y, double z, float yaw) { this(x, y, z, yaw, 0f); }
    }

    /** Inclusive integer box. {@link #contains(double, double, double)} tests block-space (floor) coordinates. */
    public record Region(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public static Region of(int x1, int y1, int z1, int x2, int y2, int z2) {
            return new Region(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        }
        public boolean contains(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
            return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
        }
        public double centerX() { return (minX + maxX + 1) / 2.0; }
        public double centerZ() { return (minZ + maxZ + 1) / 2.0; }
    }

    /** One block of a variant option. {@code block} null / "air" = clear. */
    public record Placement(int x, int y, int z, String block) {}

    /**
     * A named group of alternative block sets; the runtime picks exactly one option per round.
     * Example: turnstile {@code "gate_07"}, category {@code "turnstile"}, options OPEN / JAMMED / DROP.
     * Options sharing a group should normally touch the same positions (each option sets air where it wants air).
     */
    public static final class VariantGroup {
        public final String name;
        public final String category;
        /** option name -> blocks; insertion order kept. */
        public final LinkedHashMap<String, List<Placement>> options = new LinkedHashMap<>();
        /** Optional relative weights per option (missing = 1). */
        public final Map<String, Integer> weights = new HashMap<>();

        public VariantGroup(String name, String category) {
            this.name = name;
            this.category = category;
        }

        /** Get-or-create the block list of an option. */
        public List<Placement> option(String option) {
            return options.computeIfAbsent(option, k -> new ArrayList<>());
        }

        public VariantGroup add(String option, int x, int y, int z, String block) {
            option(option).add(new Placement(x, y, z, block));
            return this;
        }
    }

    /**
     * Constraint over all groups of a category: at least {@code minCount} groups pick {@code option}
     * (and at most {@code maxCount}, -1 = no limit). Example: {@code new VariantRule("turnstile","OPEN",4,-1)}.
     */
    public record VariantRule(String category, String option, int minCount, int maxCount) {}

    /** Sparse block store; also the sink the generator writes into. */
    public interface Blocks {
        void set(int x, int y, int z, String blockData);
        /** Block-state string at the position (with "minecraft:"), or null for air / unset. */
        String get(int x, int y, int z);
        /** Visit every non-air block. Order unspecified. */
        void forEach(Visitor v);
        int size();
    }

    @FunctionalInterface
    public interface Visitor { void block(int x, int y, int z, String blockData); }

    /** Default sparse implementation (HashMap keyed by packed coordinates). */
    public static final class SparseBlocks implements Blocks {
        private final Map<Long, String> map = new HashMap<>();
        private final Map<String, String> intern = new HashMap<>();

        public static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }
        public static int kx(long k) { return (int) (k >> 38) << 6 >> 6; }
        public static int kz(long k) { return (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6; }
        public static int ky(long k) { return (int) (k & 0xFFF) << 20 >> 20; }

        public static String normalize(String b) {
            if (b == null || b.isEmpty() || b.equals("air") || b.equals("minecraft:air")) return null;
            return b.indexOf(':') < 0 ? "minecraft:" + b : b;
        }

        @Override public void set(int x, int y, int z, String b) {
            long k = key(x, y, z);
            String f = normalize(b);
            if (f == null) { map.remove(k); return; }
            map.put(k, intern.computeIfAbsent(f, s -> s));
        }
        @Override public String get(int x, int y, int z) { return map.get(key(x, y, z)); }
        @Override public void forEach(Visitor v) {
            for (Map.Entry<Long, String> e : map.entrySet()) {
                long k = e.getKey();
                v.block(kx(k), ky(k), kz(k), e.getValue());
            }
        }
        @Override public int size() { return map.size(); }
    }

    // ------------------------------------------------------------------ data

    public final String mapId;
    public final long seed;
    public final Blocks blocks;

    /** Feet positions on the start strip, >= 40, all facing the course. */
    public final List<Spawn> startSpawns = new ArrayList<>();
    /** Box around the start strip (feet space). Before GO, anyone outside it is put back. Required. */
    public Region startStrip;
    /** Blocks (present in {@link #blocks}) removed at GO. */
    public final List<Pos> startBarrier = new ArrayList<>();
    /** Blocks (present in {@link #blocks}) removed at GO, e.g. the elytra gantry floor or a drop hatch. */
    public final List<Pos> dropFloor = new ArrayList<>();
    /** Boat maps: one boat per point, >= 40 (else startSpawns are used). */
    public final List<Spawn> boatSpawns = new ArrayList<>();

    /** Goal chicken spawn points, >= 8, inside {@link #pen}. */
    public final List<Spawn> chickenSpawns = new ArrayList<>();
    /** Goal pen interior (feet space of chickens). Chickens leaving it are put back. Required. */
    public Region pen;

    /** Feet below this local y = respawn. */
    public int voidY = -20;
    /** Block types (e.g. "minecraft:lava", "minecraft:magma_block") that mean respawn on touch. */
    public final Set<String> hazardBlocks = new LinkedHashSet<>();
    /** Boxes that mean respawn when a racer's feet enter them (e.g. a "drop corridor" dead end, a pit). */
    public final List<Region> hazardRegions = new ArrayList<>();
    /** Racers outside this box respawn; finished spectators are confined to it. Required. */
    public Region playBounds;
    /** Bridging maps: concrete may only be placed inside this box (null = playBounds). */
    public Region buildBounds;

    /** Per-round block alternatives (see {@link VariantGroup}). */
    public final List<VariantGroup> variants = new ArrayList<>();
    public final List<VariantRule> variantRules = new ArrayList<>();

    /** Spectator / overview teleports; [0] is used for finished players and for the map title-card shot. */
    public final List<Spawn> overviewPoints = new ArrayList<>();

    /** Map-specific extra kit items as "material[:count]" strings, e.g. "minecraft:firework_rocket:1". */
    public final List<String> extraItems = new ArrayList<>();

    /** Free-form generator / validator notes (e.g. "fastLine" -> "~24 s"). Shown by {@code /race info}. */
    public final Map<String, String> notes = new LinkedHashMap<>();

    public TgMapData(String mapId, long seed, Blocks blocks) {
        this.mapId = mapId;
        this.seed = seed;
        this.blocks = blocks;
    }

    public TgMapData(String mapId, long seed) { this(mapId, seed, new SparseBlocks()); }

    // ------------------------------------------------------------------ helpers

    /** Fill an inclusive box with one block (null = air). */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    blocks.set(x, y, z, block);
    }

    public VariantGroup variant(String name, String category) {
        VariantGroup g = new VariantGroup(name, category);
        variants.add(g);
        return g;
    }

    /** Bounding box of every block in {@link #blocks} plus all variant placements ({@code null} if empty). */
    public Region blockBounds() {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        blocks.forEach((x, y, z, d) -> grow(b, x, y, z));
        for (VariantGroup g : variants)
            for (Collection<Placement> l : g.options.values())
                for (Placement p : l) grow(b, p.x(), p.y(), p.z());
        if (b[0] == Integer.MAX_VALUE) return null;
        return new Region(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    private static void grow(int[] b, int x, int y, int z) {
        b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
        b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
    }

    /** Basic contract check; returns problems (empty = ok). The runtime logs these and refuses a broken map. */
    public List<String> check(Kit kit) {
        List<String> p = new ArrayList<>();
        if (startSpawns.size() < 40) p.add("startSpawns < 40 (" + startSpawns.size() + ")");
        if (chickenSpawns.size() < 8) p.add("chickenSpawns < 8 (" + chickenSpawns.size() + ")");
        if (startStrip == null) p.add("startStrip missing");
        if (pen == null) p.add("pen missing");
        if (playBounds == null) p.add("playBounds missing");
        if (kit == Kit.BOAT && boatSpawns.isEmpty()) p.add("boat map without boatSpawns (using startSpawns)");
        int[] concrete = {0};
        blocks.forEach((x, y, z, d) -> { if (d.endsWith("_concrete")) concrete[0]++; });
        if (concrete[0] > 0) p.add("map uses dyed concrete (" + concrete[0] + " blocks) - reserved for players");
        return p;
    }
}
