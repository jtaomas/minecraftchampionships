package com.mcm.games.bingo;

import org.bukkit.block.Biome;

import java.util.*;

import static org.bukkit.block.Biome.*;

/**
 * The biome plan of one game area: 13 chosen surface biomes as warped Voronoi cells around the area centre (4 inner
 * cells meeting at the start point, 9 outer cells reaching the border), lush/dripstone caves under two cells, and the
 * structure sites the plugin places itself. Water keeps the vanilla terrain: where vanilla would put ocean, river or
 * beach, {@link #mapWater} returns the water biome matching the cell's climate.
 *
 * <p>Pure Java (no server calls) except through {@link Sampler}, so the offline check can run it.
 */
public final class AreaLayout {

    /** The vanilla biome at a block position (only used to see where vanilla terrain has water). */
    public interface Sampler { Biome biome(int x, int y, int z); }

    public record Cell(double x, double z, Biome surface, Biome under, boolean inner) {}

    /** A structure to place: kind is the env tag (village, desert_pyramid, ...), id the vanilla structure id. */
    public record Site(String kind, String id, int x, int z, int chunkRadius) {}

    public final int index;
    public final int cx, cz;
    public final int half;
    public final List<Cell> cells;
    public final List<Site> sites;
    /** Surface biomes (and cave biomes) that really occur inside the border, with enough area to find them. */
    public final Set<Biome> biomes;
    private final long warpSeed;

    private AreaLayout(int index, int cx, int cz, int half, List<Cell> cells, List<Site> sites, Set<Biome> biomes, long warpSeed) {
        this.index = index;
        this.cx = cx;
        this.cz = cz;
        this.half = half;
        this.cells = cells;
        this.sites = sites;
        this.biomes = biomes;
        this.warpSeed = warpSeed;
    }

    // ================================================================ lookup (thread-safe, world-gen threads)

    public Cell cellAt(int x, int z) {
        double wx = x + 34 * noise(x, z, warpSeed, 150) + 12 * noise(x, z, warpSeed ^ 0x5bd1e995L, 50);
        double wz = z + 34 * noise(x, z, warpSeed ^ 0x9E3779B9L, 150) + 12 * noise(x, z, warpSeed ^ 0x2545F491L, 50);
        Cell best = cells.get(0);
        double bd = Double.MAX_VALUE;
        for (Cell c : cells) {
            double dx = wx - c.x, dz = wz - c.z;
            double d = dx * dx + dz * dz;
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    /** Value noise in [-1, 1] with lattice size {@code size}. */
    private static double noise(int x, int z, long seed, int size) {
        double fx = Math.floorDiv(x, size), fz = Math.floorDiv(z, size);
        double tx = (x - fx * size) / (double) size, tz = (z - fz * size) / (double) size;
        long ix = (long) fx, iz = (long) fz;
        double a = hash(ix, iz, seed), b = hash(ix + 1, iz, seed), c = hash(ix, iz + 1, seed), d = hash(ix + 1, iz + 1, seed);
        double sx = tx * tx * (3 - 2 * tx), sz = tz * tz * (3 - 2 * tz);
        return (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sz;
    }

    private static double hash(long x, long z, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h ^= (h >>> 31); h *= 0xBF58476D1CE4E5B9L; h ^= (h >>> 29); h *= 0x94D049BB133111EBL; h ^= (h >>> 32);
        return ((h >>> 11) * 0x1.0p-53) * 2 - 1;
    }

    // ================================================================ water and climate

    public static boolean isWater(Biome b) {
        if (b == null) return false;
        return switch (b) {
            case OCEAN, DEEP_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN, WARM_OCEAN,
                 FROZEN_OCEAN, DEEP_FROZEN_OCEAN, RIVER, FROZEN_RIVER, BEACH, SNOWY_BEACH, STONY_SHORE -> true;
            default -> false;
        };
    }

    private static boolean isDeep(Biome b) {
        return b == DEEP_OCEAN || b == DEEP_COLD_OCEAN || b == DEEP_LUKEWARM_OCEAN || b == DEEP_FROZEN_OCEAN;
    }

    /** 0 frozen, 1 cold, 2 temperate, 3 warm, 4 hot. */
    static int climate(Biome b) {
        return switch (b) {
            case SNOWY_PLAINS, ICE_SPIKES, SNOWY_TAIGA, GROVE, SNOWY_SLOPES -> 0;
            case TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, WINDSWEPT_HILLS, WINDSWEPT_FOREST,
                 WINDSWEPT_GRAVELLY_HILLS -> 1;
            case JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE, SAVANNA, SAVANNA_PLATEAU, MANGROVE_SWAMP, SWAMP -> 3;
            case DESERT, BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS -> 4;
            default -> 2;
        };
    }

    /** The water biome to use where vanilla has {@code v} (ocean/river/beach) inside a cell of {@code cell}. */
    public static Biome mapWater(Biome v, Biome cell) {
        int c = climate(cell);
        if (v == STONY_SHORE) return STONY_SHORE;
        if (v == RIVER || v == FROZEN_RIVER) return c == 0 ? FROZEN_RIVER : RIVER;
        if (v == BEACH || v == SNOWY_BEACH) return c == 0 ? SNOWY_BEACH : BEACH;
        boolean deep = isDeep(v);
        return switch (c) {
            case 0 -> deep ? DEEP_FROZEN_OCEAN : FROZEN_OCEAN;
            case 1 -> deep ? DEEP_COLD_OCEAN : COLD_OCEAN;
            case 3 -> deep ? DEEP_LUKEWARM_OCEAN : LUKEWARM_OCEAN;
            case 4 -> deep ? DEEP_LUKEWARM_OCEAN : WARM_OCEAN;
            default -> deep ? DEEP_OCEAN : OCEAN;
        };
    }

    /** What the world-gen provider returns at the surface of (x, z) given vanilla's biome {@code v} there. */
    public Biome surfaceBiome(int x, int z, Biome v) {
        Cell c = cellAt(x, z);
        return isWater(v) ? mapWater(v, c.surface) : c.surface;
    }

    // ================================================================ planning

    /** Biome families; one biome of each is used, then 2 extras from the leftovers. */
    static final Biome[][] FAMILIES = {
            {PLAINS, PLAINS, SUNFLOWER_PLAINS, MEADOW},
            {FOREST, FLOWER_FOREST, BIRCH_FOREST},
            {DARK_FOREST},
            {TAIGA, OLD_GROWTH_SPRUCE_TAIGA, SNOWY_TAIGA},
            {DESERT},
            {BADLANDS, WOODED_BADLANDS, ERODED_BADLANDS},
            {SNOWY_PLAINS, ICE_SPIKES, GROVE},
            {JUNGLE, BAMBOO_JUNGLE, JUNGLE, SPARSE_JUNGLE},
            {SWAMP, MANGROVE_SWAMP},
            {CHERRY_GROVE, CHERRY_GROVE, CHERRY_GROVE, MUSHROOM_FIELDS},
            {SAVANNA, SAVANNA, SAVANNA_PLATEAU},
    };
    static final Biome[] EXTRAS = {FLOWER_FOREST, BIRCH_FOREST, MEADOW, SUNFLOWER_PLAINS, WINDSWEPT_HILLS, OLD_GROWTH_SPRUCE_TAIGA,
            MANGROVE_SWAMP, SWAMP, BAMBOO_JUNGLE, FOREST, MUSHROOM_FIELDS, CHERRY_GROVE, GROVE};

    static final Set<Biome> TREE_BIOMES = EnumSet.of(FOREST, FLOWER_FOREST, BIRCH_FOREST, DARK_FOREST, TAIGA,
            OLD_GROWTH_SPRUCE_TAIGA, SNOWY_TAIGA, CHERRY_GROVE, SAVANNA, SAVANNA_PLATEAU, JUNGLE, BAMBOO_JUNGLE);

    static String villageFor(Biome b) {
        return switch (b) {
            case PLAINS, SUNFLOWER_PLAINS, MEADOW -> "minecraft:village_plains";
            case DESERT -> "minecraft:village_desert";
            case SAVANNA, SAVANNA_PLATEAU -> "minecraft:village_savanna";
            case TAIGA, OLD_GROWTH_SPRUCE_TAIGA -> "minecraft:village_taiga";
            case SNOWY_PLAINS, SNOWY_TAIGA -> "minecraft:village_snowy";
            default -> null;
        };
    }

    static String portalFor(Biome b) {
        return switch (b) {
            case DESERT, BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS -> "minecraft:ruined_portal_desert";
            case JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE -> "minecraft:ruined_portal_jungle";
            case SWAMP, MANGROVE_SWAMP -> "minecraft:ruined_portal_swamp";
            case WINDSWEPT_HILLS, GROVE -> "minecraft:ruined_portal_mountain";
            default -> "minecraft:ruined_portal";
        };
    }

    /** Sample spacing used for the biome census and site search. */
    static final int STEP = 24;
    /** Minimum census samples (each ~24x24 blocks) for a biome to count as present. */
    static final int MIN_SAMPLES = 8;

    /**
     * Plans area {@code index}: picks a centre near (nominalX, nominalZ) with land at the start, the 13 cells, the
     * census of what really generates (vanilla terrain decides land vs water) and the structure sites.
     * Pure computation from noise; no chunk is touched. Safe off the main thread.
     */
    public static AreaLayout plan(int index, int nominalX, int nominalZ, int half, long seed, Sampler vanilla, boolean placeStructures) {
        Random rnd = new Random(seed * 31 + index * 0x9E3779B97F4A7C15L);
        int[] center = findCenter(vanilla, nominalX, nominalZ);
        int cx = center[0], cz = center[1];

        // --- biomes: one per family, then 2 extras not already used
        List<Biome> chosen = new ArrayList<>();
        for (Biome[] fam : FAMILIES) chosen.add(fam[rnd.nextInt(fam.length)]);
        List<Biome> extras = new ArrayList<>(Arrays.asList(EXTRAS));
        Collections.shuffle(extras, rnd);
        for (Biome b : extras) {
            if (chosen.size() >= 13) break;
            if (!chosen.contains(b)) chosen.add(b);
        }
        Collections.shuffle(chosen, rnd);
        // Inner 4 must contain a tree biome and no mushroom fields (no wood there).
        moveInner(chosen, rnd);

        List<Cell> cells = new ArrayList<>();
        double rot = rnd.nextDouble() * Math.PI / 2;
        int lushInner = rnd.nextInt(4);
        for (int i = 0; i < 4; i++) {
            double a = rot + Math.PI / 4 + i * Math.PI / 2;
            double r = 95 + rnd.nextInt(30);
            Biome s = chosen.get(i);
            cells.add(new Cell(cx + Math.cos(a) * r, cz + Math.sin(a) * r, s, i == lushInner ? LUSH_CAVES : null, true));
        }
        int outer = chosen.size() - 4;
        int dripOuter = rnd.nextInt(outer);
        int lushOuter = (dripOuter + 1 + rnd.nextInt(outer - 1)) % outer;
        double rot2 = rnd.nextDouble() * Math.PI * 2;
        for (int i = 0; i < outer; i++) {
            double a = rot2 + i * 2 * Math.PI / outer + (rnd.nextDouble() - 0.5) * 0.3;
            double r = 330 + rnd.nextInt(90);
            Biome under = i == dripOuter ? DRIPSTONE_CAVES : (i == lushOuter && rnd.nextBoolean() ? LUSH_CAVES : null);
            cells.add(new Cell(cx + Math.cos(a) * r, cz + Math.sin(a) * r, chosen.get(4 + i), under, false));
        }

        AreaLayout pre = new AreaLayout(index, cx, cz, half, List.copyOf(cells), List.of(), Set.of(), rnd.nextLong());

        // --- census: what really generates inside the border (vanilla decides land/water)
        Map<Biome, Integer> counts = new EnumMap<>(Biome.class);
        Map<Cell, Integer> landPerCell = new HashMap<>();
        Map<Cell, List<int[]>> landSamples = new HashMap<>();
        List<int[]> beaches = new ArrayList<>(), oceans = new ArrayList<>();
        int lim = half - 8;
        for (int x = cx - lim; x <= cx + lim; x += STEP) {
            for (int z = cz - lim; z <= cz + lim; z += STEP) {
                Biome v = vanilla.biome(x, 64, z);
                Cell c = pre.cellAt(x, z);
                Biome b = isWater(v) ? mapWater(v, c.surface) : c.surface;
                counts.merge(b, 1, Integer::sum);
                if (!isWater(v)) {
                    landPerCell.merge(c, 1, Integer::sum);
                    landSamples.computeIfAbsent(c, k -> new ArrayList<>()).add(new int[]{x, z});
                } else if (v == BEACH || v == SNOWY_BEACH) {
                    beaches.add(new int[]{x, z});
                } else if (v != RIVER && v != FROZEN_RIVER && v != STONY_SHORE) {
                    oceans.add(new int[]{x, z});
                }
            }
        }
        Set<Biome> present = EnumSet.noneOf(Biome.class);
        for (Map.Entry<Biome, Integer> e : counts.entrySet()) if (e.getValue() >= MIN_SAMPLES) present.add(e.getKey());
        for (Cell c : cells) if (c.under != null && landPerCell.getOrDefault(c, 0) >= 20) present.add(c.under);

        // --- structure sites
        List<Site> sites = new ArrayList<>();
        if (placeStructures) {
            int siteLim = half - 120; // room for the village's chunk radius inside the border
            List<int[]> used = new ArrayList<>();
            used.add(new int[]{cx, cz});
            // village: in a village-capable cell, outer preferred
            List<Cell> vCells = new ArrayList<>();
            for (Cell c : cells) if (villageFor(c.surface) != null && landPerCell.getOrDefault(c, 0) >= 30) vCells.add(c);
            vCells.sort(Comparator.comparing((Cell c) -> c.inner));
            for (Cell c : vCells) {
                int[] p = bestSample(landSamples.get(c), c, cx, cz, 170, 470, siteLim, used, 200);
                if (p != null) { sites.add(new Site("village", villageFor(c.surface), p[0], p[1], 6)); used.add(p); break; }
            }
            for (Cell c : cells) {
                if (c.surface == DESERT && sites.stream().noneMatch(s -> s.kind.equals("desert_pyramid"))) {
                    int[] p = bestSample(landSamples.get(c), c, cx, cz, 120, 520, half - 40, used, 110);
                    if (p != null) { sites.add(new Site("desert_pyramid", "minecraft:desert_pyramid", p[0], p[1], 2)); used.add(p); }
                }
                if ((c.surface == JUNGLE || c.surface == BAMBOO_JUNGLE || c.surface == SPARSE_JUNGLE)
                        && sites.stream().noneMatch(s -> s.kind.equals("jungle_pyramid"))) {
                    int[] p = bestSample(landSamples.get(c), c, cx, cz, 120, 520, half - 40, used, 110);
                    if (p != null) { sites.add(new Site("jungle_pyramid", "minecraft:jungle_pyramid", p[0], p[1], 2)); used.add(p); }
                }
            }
            // ruined portal: on land 80-220 blocks from the start
            List<int[]> allLand = new ArrayList<>();
            for (List<int[]> l : landSamples.values()) allLand.addAll(l);
            allLand.sort(Comparator.comparingLong(p -> (long) p[0] * 73856093L ^ (long) p[1] * 19349663L)); // stable, seed-free order
            Collections.shuffle(allLand, rnd);
            for (int[] p : allLand) {
                double d = Math.hypot(p[0] - cx, p[1] - cz);
                if (d < 80 || d > 220 || far(p, used, 90)) continue;
                sites.add(new Site("ruined_portal", portalFor(pre.cellAt(p[0], p[1]).surface), p[0], p[1], 2));
                used.add(p);
                break;
            }
            // shipwreck + buried treasure
            Collections.shuffle(beaches, rnd);
            Collections.shuffle(oceans, rnd);
            int[] wreck = null;
            for (int[] p : beaches) {
                if (Math.hypot(p[0] - cx, p[1] - cz) < 90 || outside(p, cx, cz, half - 40) || far(p, used, 90)) continue;
                wreck = p;
                sites.add(new Site("shipwreck", "minecraft:shipwreck_beached", p[0], p[1], 2));
                used.add(p);
                break;
            }
            if (wreck == null) {
                for (int[] p : oceans) {
                    if (Math.hypot(p[0] - cx, p[1] - cz) < 90 || outside(p, cx, cz, half - 40) || far(p, used, 90)) continue;
                    sites.add(new Site("shipwreck", "minecraft:shipwreck", p[0], p[1], 2));
                    used.add(p);
                    break;
                }
            }
            for (int[] p : beaches) {
                if (Math.hypot(p[0] - cx, p[1] - cz) < 60 || outside(p, cx, cz, half - 24) || far(p, used, 40)) continue;
                sites.add(new Site("buried_treasure", "minecraft:buried_treasure", p[0], p[1], 1));
                used.add(p);
                break;
            }
        }
        return new AreaLayout(index, cx, cz, half, pre.cells, List.copyOf(sites), Collections.unmodifiableSet(present), pre.warpSeed);
    }

    /** True if p is NOT within {@code lim} of the centre on both axes (i.e. outside the allowed square). */
    private static boolean outside(int[] p, int cx, int cz, int lim) {
        return Math.abs(p[0] - cx) > lim || Math.abs(p[1] - cz) > lim;
    }

    /** True if p is closer than {@code minDist} to any used point. */
    private static boolean far(int[] p, List<int[]> used, double minDist) {
        for (int[] u : used) if (Math.hypot(p[0] - u[0], p[1] - u[1]) < minDist) return true;
        return false;
    }

    /** The land sample of a cell closest to the cell seed, within the distance band and the border margin. */
    private static int[] bestSample(List<int[]> samples, Cell c, int cx, int cz, double minD, double maxD, int lim,
                                    List<int[]> used, double minSep) {
        if (samples == null) return null;
        int[] best = null;
        double bd = Double.MAX_VALUE;
        for (int[] p : samples) {
            double d = Math.hypot(p[0] - cx, p[1] - cz);
            if (d < minD || d > maxD || outside(p, cx, cz, lim) || far(p, used, minSep)) continue;
            double k = Math.hypot(p[0] - c.x, p[1] - c.z);
            if (k < bd) { bd = k; best = p; }
        }
        return best;
    }

    private static void moveInner(List<Biome> chosen, Random rnd) {
        // swap mushroom fields out of the inner four
        for (int i = 0; i < 4; i++) {
            if (chosen.get(i) != MUSHROOM_FIELDS) continue;
            for (int j = 4; j < chosen.size(); j++) {
                if (chosen.get(j) != MUSHROOM_FIELDS) { Collections.swap(chosen, i, j); break; }
            }
        }
        boolean tree = false;
        for (int i = 0; i < 4; i++) if (TREE_BIOMES.contains(chosen.get(i))) tree = true;
        if (!tree) {
            for (int j = 4; j < chosen.size(); j++) {
                if (TREE_BIOMES.contains(chosen.get(j))) { Collections.swap(chosen, rnd.nextInt(4), j); break; }
            }
        }
    }

    /**
     * Spiral search (step 96, up to ~500 blocks) for a centre with land at the spot and on a radius-40 ring; among
     * those, prefer one with water (ocean or river) within 450 blocks so water items exist.
     */
    static int[] findCenter(Sampler v, int bx, int bz) {
        int[] firstLand = null, fallback = null;
        for (int ring = 0; ring <= 5; ring++) {
            for (int dx = -ring; dx <= ring; dx++)
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = bx + dx * 96, z = bz + dz * 96;
                    if (isWater(v.biome(x, 64, z))) continue;
                    if (fallback == null) fallback = new int[]{x, z};
                    boolean ok = true;
                    for (int a = 0; a < 8 && ok; a++) {
                        double ang = Math.PI * a / 4;
                        if (isWater(v.biome(x + (int) Math.round(Math.cos(ang) * 40), 64, z + (int) Math.round(Math.sin(ang) * 40)))) ok = false;
                    }
                    if (!ok) continue;
                    if (firstLand == null) firstLand = new int[]{x, z};
                    for (int a = 0; a < 16; a++) {
                        double ang = Math.PI * a / 8;
                        for (int r : new int[]{200, 330, 450}) {
                            Biome b = v.biome(x + (int) Math.round(Math.cos(ang) * r), 64, z + (int) Math.round(Math.sin(ang) * r));
                            if (b != null && b != BEACH && b != STONY_SHORE && isWater(b)) return new int[]{x, z};
                        }
                    }
                }
        }
        if (firstLand != null) return firstLand;
        return fallback != null ? fallback : new int[]{bx, bz};
    }

    /** Short description for the log. */
    public String describe() {
        StringBuilder sb = new StringBuilder("area ").append(index).append(" at ").append(cx).append(',').append(cz).append(": ");
        for (Cell c : cells) {
            sb.append(c.surface.name().toLowerCase(Locale.ROOT));
            if (c.under != null) sb.append('+').append(c.under.name().toLowerCase(Locale.ROOT));
            sb.append(c.inner ? "* " : " ");
        }
        sb.append("| present ").append(biomes.size()).append(" | sites ");
        for (Site s : sites) sb.append(s.kind).append('@').append(s.x).append(',').append(s.z).append(' ');
        return sb.toString();
    }
}
