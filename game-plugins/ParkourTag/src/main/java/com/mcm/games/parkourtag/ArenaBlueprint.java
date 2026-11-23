package com.mcm.games.parkourtag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One Parkour Tag arena, generated deterministically (fixed seed) so every copy is identical.
 * Coordinates are relative: x and z 0..W-1, floor at y = 0, barrier ceiling at y = H.
 */
final class ArenaBlueprint {
    static final int W = 41;
    static final int H = 22;
    static final int PAD_X0 = 17, PAD_X1 = 23;
    static final int PAD_A_Z0 = 1, PAD_A_Z1 = 3;
    static final int PAD_B_Z0 = 37, PAD_B_Z1 = 39;
    static final int LEDGE_Y = 6;

    record Placement(int x, int y, int z, BlockData data) {}

    private final Map<Long, BlockData> map = new LinkedHashMap<>();

    private static long key(int x, int y, int z) { return ((long) x << 32) | ((long) y << 16) | (long) z; }

    private boolean inBounds(int x, int y, int z) { return x >= 0 && x < W && z >= 0 && z < W && y >= 0 && y <= H; }

    private void set(int x, int y, int z, BlockData d) { if (inBounds(x, y, z)) map.put(key(x, y, z), d); }
    private void set(int x, int y, int z, Material m) { set(x, y, z, m.createBlockData()); }
    private void remove(int x, int y, int z) { map.remove(key(x, y, z)); }
    private boolean has(int x, int y, int z) { return map.containsKey(key(x, y, z)); }
    private static BlockData bd(String s) { return Bukkit.createBlockData(s); }

    /** A climbable column (ladder or vine) from y1 to y2 that overrides anything already there. */
    private void climb(int x, int z, int y1, int y2, String data) {
        BlockData d = bd(data);
        for (int y = y1; y <= y2; y++) set(x, y, z, d);
    }

    static List<Placement> generate() {
        ArenaBlueprint b = new ArenaBlueprint();
        b.build();
        List<Placement> out = new ArrayList<>(b.map.size());
        for (Map.Entry<Long, BlockData> e : b.map.entrySet()) {
            long k = e.getKey();
            int x = (int) (k >> 32), y = (int) ((k >> 16) & 0xFFFF), z = (int) (k & 0xFFFF);
            out.add(new Placement(x, y, z, e.getValue()));
        }
        return out;
    }

    private void build() {
        Random rnd = new Random(20201114L);

        // Floor, walls, ceiling
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < W; z++) {
                boolean border = x == 0 || z == 0 || x == W - 1 || z == W - 1;
                if (border) {
                    set(x, 0, z, Material.STONE_BRICKS);
                    for (int y = 1; y <= 3; y++) set(x, y, z, y == 3 ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICKS);
                    for (int y = 4; y < H; y++) set(x, y, z, Material.BARRIER);
                } else {
                    set(x, 0, z, ((x / 5 + z / 5) % 2 == 0) ? Material.SMOOTH_STONE : Material.POLISHED_ANDESITE);
                }
                set(x, H, z, Material.BARRIER);
            }
        }

        // Start pads (recoloured per round)
        for (int x = PAD_X0; x <= PAD_X1; x++) {
            for (int z = PAD_A_Z0; z <= PAD_A_Z1; z++) set(x, 0, z, Material.WHITE_CONCRETE);
            for (int z = PAD_B_Z0; z <= PAD_B_Z1; z++) set(x, 0, z, Material.WHITE_CONCRETE);
        }

        // Cell features: 5x5 grid of 8-wide cells, each with a 5x5 build area and 3-wide corridors.
        for (int cx = 0; cx < 5; cx++) {
            for (int cz = 0; cz < 5; cz++) {
                int ox = 2 + cx * 8, oz = 2 + cz * 8;
                if (cx == 2 && (cz == 0 || cz == 4)) continue; // keep the start pads open
                if (cx == 2 && cz == 2) { centerTower(ox, oz); continue; }
                switch (rnd.nextInt(5)) {
                    case 0 -> tower(ox, oz, 5 + 2 * rnd.nextInt(3));
                    case 1 -> steps(ox, oz);
                    case 2 -> stilts(ox, oz);
                    case 3 -> vineWall(ox, oz);
                    default -> hill(ox, oz);
                }
            }
        }

        // Elevated ledge ring along the walls, with jump gaps
        for (int i = 1; i <= W - 2; i++) {
            int[][] spots = {{1, i}, {W - 2, i}, {i, 1}, {i, W - 2}};
            for (int[] s : spots) {
                if ((s[0] + s[1]) % 6 == 0) continue;
                set(s[0], LEDGE_Y, s[1], Material.SPRUCE_PLANKS);
            }
        }
        // Ladders up to the ledge
        for (int p : new int[]{10, 30}) {
            climb(1, p, 1, LEDGE_Y, "minecraft:ladder[facing=east]");
            climb(W - 2, p, 1, LEDGE_Y, "minecraft:ladder[facing=west]");
            climb(p, 1, 1, LEDGE_Y, "minecraft:ladder[facing=south]");
            climb(p, W - 2, 1, LEDGE_Y, "minecraft:ladder[facing=north]");
        }
        // Vine strips on the walls
        for (int p : new int[]{15, 16, 24, 25}) {
            climb(1, p, 1, LEDGE_Y, "minecraft:vine[west=true]");
            climb(W - 2, p, 1, LEDGE_Y, "minecraft:vine[east=true]");
        }
        for (int p : new int[]{5, 6, 34, 35}) {
            climb(p, 1, 1, LEDGE_Y, "minecraft:vine[north=true]");
            climb(p, W - 2, 1, LEDGE_Y, "minecraft:vine[south=true]");
        }

        // Floating stepping stones for extra routes
        Material[] stones = {Material.LIME_CONCRETE, Material.PRISMARINE_BRICKS, Material.OAK_PLANKS};
        int placed = 0;
        for (int attempt = 0; attempt < 400 && placed < 45; attempt++) {
            int x = 3 + rnd.nextInt(W - 6), z = 6 + rnd.nextInt(W - 12), y = 2 + rnd.nextInt(8);
            if (x >= PAD_X0 - 2 && x <= PAD_X1 + 2 && (z <= 7 || z >= W - 8)) continue;
            boolean clear = true;
            for (int dx = -1; dx <= 1 && clear; dx++)
                for (int dz = -1; dz <= 1 && clear; dz++)
                    for (int dy = -2; dy <= 2; dy++)
                        if (has(x + dx, y + dy, z + dz)) { clear = false; break; }
            if (!clear) continue;
            if (rnd.nextBoolean()) set(x, y, z, bd("minecraft:oak_slab[type=bottom]"));
            else set(x, y, z, stones[rnd.nextInt(stones.length)]);
            placed++;
        }
    }

    // ---- features (local area 5x5 starting at ox, oz) ----

    private void tower(int ox, int oz, int h) {
        for (int y = 1; y < h; y++)
            for (int x = 1; x <= 2; x++)
                for (int z = 1; z <= 2; z++) set(ox + x, y, oz + z, Material.CYAN_TERRACOTTA);
        for (int x = 0; x <= 3; x++)
            for (int z = 0; z <= 3; z++) set(ox + x, h, oz + z, Material.WHITE_CONCRETE);
        climb(ox + 3, oz + 1, 1, h, "minecraft:ladder[facing=east]");
        climb(ox, oz + 2, 1, h, "minecraft:vine[east=true]");
        set(ox + 4, h - 2, oz + 3, bd("minecraft:oak_slab[type=bottom]"));
    }

    private void steps(int ox, int oz) {
        List<int[]> ring = new ArrayList<>();
        for (int x = 0; x <= 4; x++) ring.add(new int[]{x, 0});
        for (int z = 1; z <= 4; z++) ring.add(new int[]{4, z});
        for (int x = 3; x >= 0; x--) ring.add(new int[]{x, 4});
        for (int z = 3; z >= 1; z--) ring.add(new int[]{0, z});
        int n = 0;
        for (int i = 0; i < ring.size(); i += 2) {
            int[] p = ring.get(i);
            int top = 1 + n / 2;
            set(ox + p[0], top, oz + p[1], n % 2 == 0 ? Material.OAK_PLANKS : Material.BIRCH_PLANKS);
            n++;
        }
        for (int y = 1; y <= 5; y++) set(ox + 2, y, oz + 2, Material.MOSSY_COBBLESTONE);
        set(ox + 2, 6, oz + 2, bd("minecraft:azalea_leaves[persistent=true]"));
        climb(ox + 2, oz + 1, 1, 5, "minecraft:vine[south=true]");
    }

    private void stilts(int ox, int oz) {
        for (int x = 0; x <= 4; x++)
            for (int z = 0; z <= 3; z++) set(ox + x, 4, oz + z, Material.SPRUCE_PLANKS);
        int[][] posts = {{0, 0}, {4, 0}, {0, 3}, {4, 3}};
        for (int[] p : posts) for (int y = 1; y <= 3; y++) set(ox + p[0], y, oz + p[1], Material.OAK_FENCE);
        for (int y = 1; y <= 3; y++) set(ox + 2, y, oz + 3, Material.OAK_LOG);
        climb(ox + 2, oz + 4, 1, 4, "minecraft:ladder[facing=south]");
        set(ox, 5, oz, bd("minecraft:azalea_leaves[persistent=true]"));
        set(ox + 4, 5, oz, bd("minecraft:flowering_azalea_leaves[persistent=true]"));
        set(ox + 1, 6, oz + 1, Material.SPRUCE_PLANKS);
    }

    private void vineWall(int ox, int oz) {
        for (int x = 0; x <= 4; x++)
            for (int y = 1; y <= 6; y++) set(ox + x, y, oz + 2, Material.MOSSY_STONE_BRICKS);
        remove(ox + 2, 1, oz + 2);
        remove(ox + 2, 2, oz + 2);
        for (int x : new int[]{0, 1, 3, 4}) {
            climb(ox + x, oz + 1, 1, 6, "minecraft:vine[south=true]");
            climb(ox + x, oz + 3, 1, 6, "minecraft:vine[north=true]");
        }
        set(ox, 7, oz + 2, bd("minecraft:mossy_stone_brick_slab[type=bottom]"));
        set(ox + 4, 7, oz + 2, bd("minecraft:mossy_stone_brick_slab[type=bottom]"));
    }

    private void hill(int ox, int oz) {
        BlockData slab = bd("minecraft:sandstone_slab[type=bottom]");
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                int d = Math.max(Math.abs(x - 2), Math.abs(z - 2));
                int halves = (3 - d) * 2 - 1;
                int full = halves / 2;
                for (int y = 1; y <= full; y++) set(ox + x, y, oz + z, Material.SANDSTONE);
                if (halves % 2 == 1) set(ox + x, full + 1, oz + z, slab);
            }
        }
        for (int y = 3; y <= 5; y++) set(ox + 2, y, oz + 2, Material.CUT_SANDSTONE);
        set(ox + 2, 6, oz + 2, slab);
    }

    private void centerTower(int ox, int oz) {
        int h = 11;
        for (int y = 1; y < h; y++)
            for (int x = 1; x <= 3; x++)
                for (int z = 1; z <= 3; z++) set(ox + x, y, oz + z, Material.QUARTZ_BRICKS);
        for (int x = 0; x <= 4; x++)
            for (int z = 0; z <= 4; z++) set(ox + x, h, oz + z, Material.SMOOTH_QUARTZ);
        climb(ox + 2, oz, 1, h, "minecraft:ladder[facing=north]");
        climb(ox + 2, oz + 4, 1, h, "minecraft:ladder[facing=south]");
        climb(ox, oz + 2, 1, h, "minecraft:vine[east=true]");
        climb(ox + 4, oz + 2, 1, h, "minecraft:vine[west=true]");
        set(ox, h + 1, oz, Material.LANTERN);
        set(ox + 4, h + 1, oz, Material.LANTERN);
        set(ox, h + 1, oz + 4, Material.LANTERN);
        set(ox + 4, h + 1, oz + 4, Material.LANTERN);
        // mid-height balconies
        set(ox + 1, 6, oz - 1, bd("minecraft:quartz_slab[type=bottom]"));
        set(ox + 3, 6, oz + 5, bd("minecraft:quartz_slab[type=bottom]"));
    }
}
