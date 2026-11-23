package com.mcm.games.railroadrush;

import org.bukkit.Material;

import java.util.Random;

/**
 * Block template for one lane, in lane-local coordinates (lx relative to the lane centre x).
 * Every lane uses the same template in a game, so all teams face the same course.
 *
 * Course runs along +Z: start pad (z 0..7), tutorial room, copper door 1 (z 55), room 2, door 2 (z 111),
 * room 3, door 3 (z 171), golden station (z 172..181).
 */
final class Layout {
    static final int MIN_LX = -7, MAX_LX = 7, MIN_Y = 56, MAX_Y = 74, MIN_Z = -3, MAX_Z = 183;
    static final Material PAD = Material.LIGHT_GRAY_GLAZED_TERRACOTTA; // placeholder for team colour
    static final Material LEAVES = Material.ACACIA_LEAVES;

    private final Material[][][] m = new Material[MAX_LX - MIN_LX + 1][MAX_Y - MIN_Y + 1][MAX_Z - MIN_Z + 1];

    Material get(int lx, int y, int z) {
        if (lx < MIN_LX || lx > MAX_LX || y < MIN_Y || y > MAX_Y || z < MIN_Z || z > MAX_Z) return Material.AIR;
        return m[lx - MIN_LX][y - MIN_Y][z - MIN_Z];
    }

    private void set(int lx, int y, int z, Material mat) {
        if (lx < MIN_LX || lx > MAX_LX || y < MIN_Y || y > MAX_Y || z < MIN_Z || z > MAX_Z) return;
        m[lx - MIN_LX][y - MIN_Y][z - MIN_Z] = mat;
    }

    static boolean chasm(int z) {
        return (z >= 66 && z <= 70) || (z >= 120 && z <= 127) || (z >= 146 && z <= 151);
    }

    static boolean door(int z) {
        for (int d : RailroadRushGame.DOORS) if (d == z) return true;
        return false;
    }

    Layout(Random r) {
        for (Material[][] a : m) for (Material[] b : a) java.util.Arrays.fill(b, Material.AIR);

        int endZ = RailroadRushGame.END_Z;
        for (int z = MIN_Z; z <= MAX_Z; z++) {
            for (int lx = MIN_LX; lx <= MAX_LX; lx++) {
                set(lx, 73, z, Material.BARRIER);
                boolean shell = z < 0 || z > endZ || Math.abs(lx) >= 6;
                if (shell) {
                    for (int y = 62; y <= 72; y++) set(lx, y, z, band(y));
                    continue;
                }
                if (!chasm(z)) {
                    set(lx, 62, z, Material.RED_TERRACOTTA);
                    set(lx, 63, z, floor(lx, z, r));
                }
            }
        }

        // Ores in the inner wall layer
        for (int z = 8; z <= 170; z++) {
            if (door(z)) continue;
            for (int side : new int[]{-6, 6}) {
                for (int y = 64; y <= 68; y++) {
                    double v = r.nextDouble();
                    if (v < 0.045) set(side, y, z, Material.GOLD_ORE);
                    else if (z >= 56 && v < 0.057) set(side, y, z, Material.EMERALD_ORE);
                }
            }
        }

        // Copper checkpoint doors (full height up to the barrier ceiling)
        for (int d : RailroadRushGame.DOORS) {
            for (int lx = -5; lx <= 5; lx++) {
                for (int y = 64; y <= 72; y++) {
                    Material mat = y == 72 ? Material.WAXED_COPPER_BLOCK : Material.WAXED_CUT_COPPER;
                    if (lx == 0 && (y == 66 || y == 67)) mat = Material.WAXED_CHISELED_COPPER;
                    set(lx, y, d, mat);
                }
            }
            for (int side : new int[]{-6, 6}) for (int y = 62; y <= 72; y++) set(side, y, d, Material.WAXED_COPPER_BLOCK);
        }

        // Start rail
        set(0, 64, RailroadRushGame.START_Z, Material.RAIL);

        // --- Room 1 (tutorial): rails only. Route around the hedge, mine cobble/gravel.
        for (int i = 0; i < 6; i++) {
            int z0 = 12 + i * 7 + r.nextInt(3);
            int lx0 = -4 + r.nextInt(8);
            pile(r, lx0, z0, 2, 2, 1 + r.nextInt(2), 0.12);
        }
        for (int z = 30; z <= 31; z++) for (int lx = -5; lx <= 1; lx++) for (int y = 64; y <= 65; y++) set(lx, y, z, LEAVES);
        for (int lx = -1; lx <= 5; lx++) for (int y = 64; y <= 65; y++) set(lx, y, 42, Material.COBBLESTONE);

        // --- Room 2: chasm, stone barrier (needs explosions), plateau, leaf thicket
        pile(r, -4 + r.nextInt(8), 59, 2, 2, 2, 0.15);
        pile(r, -4 + r.nextInt(8), 73, 2, 2, 1, 0.15);
        pile(r, -4 + r.nextInt(8), 85, 2, 2, 2, 0.15);
        for (int z = 78; z <= 79; z++) for (int lx = -5; lx <= 5; lx++) for (int y = 64; y <= 66; y++)
            set(lx, y, z, r.nextDouble() < 0.7 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
        for (int z = 90; z <= 100; z++) for (int lx = -5; lx <= 5; lx++) set(lx, 64, z, Material.SMOOTH_RED_SANDSTONE);
        for (int z = 103; z <= 106; z++) for (int lx = -5; lx <= 5; lx++) for (int y = 64; y <= 66; y++)
            if (r.nextDouble() < 0.85) set(lx, y, z, LEAVES);

        // --- Room 3: two chasms, stone pillars, stone wall, leaves
        for (int z0 : new int[]{114, 130, 140, 155, 165}) pile(r, -4 + r.nextInt(8), z0, 2, 2, 1 + r.nextInt(2), 0.15);
        for (int i = 0; i < 10; i++) {
            int z = 112 + r.nextInt(57);
            if (chasm(z) || door(z) || (z >= 135 && z <= 138)) continue;
            int lx = -5 + r.nextInt(11);
            for (int y = 64; y <= 66; y++) set(lx, y, z, Material.STONE_BRICKS);
        }
        for (int z = 136; z <= 137; z++) for (int lx = -5; lx <= 5; lx++) for (int y = 64; y <= 65; y++)
            set(lx, y, z, r.nextDouble() < 0.5 ? Material.STONE_BRICKS : Material.CRACKED_STONE_BRICKS);
        for (int z = 158; z <= 160; z++) for (int lx = -5; lx <= 5; lx++) for (int y = 64; y <= 65; y++)
            if (r.nextDouble() < 0.7) set(lx, y, z, LEAVES);
    }

    private void pile(Random r, int lx0, int z0, int w, int d, int h, double goldChance) {
        for (int lx = lx0; lx < lx0 + w; lx++) {
            if (lx < -5 || lx > 5) continue;
            for (int z = z0; z < z0 + d; z++) {
                if (chasm(z) || door(z)) continue;
                for (int y = 64; y < 64 + h; y++) {
                    double v = r.nextDouble();
                    set(lx, y, z, v < goldChance ? Material.GOLD_ORE : v < 0.65 ? Material.COBBLESTONE : Material.GRAVEL);
                }
            }
        }
    }

    private static Material band(int y) {
        return switch (Math.floorMod(y, 6)) {
            case 0 -> Material.ORANGE_TERRACOTTA;
            case 1 -> Material.TERRACOTTA;
            case 2 -> Material.RED_TERRACOTTA;
            case 3 -> Material.YELLOW_TERRACOTTA;
            case 4 -> Material.ORANGE_TERRACOTTA;
            default -> Material.WHITE_TERRACOTTA;
        };
    }

    private static Material floor(int lx, int z, Random r) {
        if (z <= RailroadRushGame.START_Z) return PAD;
        if (door(z)) return Material.WAXED_CUT_COPPER;
        if (z >= 172) return Math.abs(lx) == 5 ? Material.GOLD_BLOCK : Material.SMOOTH_SANDSTONE;
        double v = r.nextDouble();
        if (v < 0.10) return Material.RED_SANDSTONE;
        if (v < 0.16) return Material.CUT_RED_SANDSTONE;
        return Material.SMOOTH_RED_SANDSTONE;
    }
}
