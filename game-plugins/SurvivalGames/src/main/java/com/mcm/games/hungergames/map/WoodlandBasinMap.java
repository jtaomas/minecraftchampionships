package com.mcm.games.hungergames.map;

import com.mcm.games.hungergames.LootTable;
import org.bukkit.Material;

/**
 * Rolling woodland, winding river, farm & ruins SG map inspired by MCC World of Cars (Forest/Farm).
 */
public final class WoodlandBasinMap implements SgMap {
    private static final int RADIUS = 124;
    private static final int WATER_Y = 63;

    @Override
    public String id() {
        return "woodland_basin";
    }

    @Override
    public String displayName() {
        return "Woodland Basin";
    }

    @Override
    public boolean boatRecommended() {
        return true;
    }

    @Override
    public int radius() {
        return RADIUS;
    }

    private int terrainHeight(int x, int z) {
        double dist = Math.hypot(x, z);
        if (dist <= 22) return 64;
        double ridge = Math.max(0.0, (dist - 36.0) / 85.0) * 6.5;
        double n = SgCanvas.noise2D(x, z, 424242L) * 4.5;
        int h = (int) Math.round(64.0 + ridge + n);
        // Carve meandering river channel along z ≈ 18*sin(x*0.045) + 46 and mirrored north fork
        double riverZ = 18.0 * Math.sin(x * 0.045) + 48.0;
        double dRiver = Math.abs(z - riverZ);
        if (dRiver < 6.0 && dist > 26 && dist < RADIUS - 5) {
            double depth = (6.0 - dRiver) * 0.65;
            h = (int) Math.round(Math.min(h, WATER_Y - depth));
        }
        return Math.max(59, Math.min(76, h));
    }

    @Override
    public void generate(SgCanvas c) {
        buildTerrain(c);
        buildCornucopia(c);
        buildBridges(c);
        buildRedBarn(c, -62, -46);
        buildLumberMill(c, 56, -42);
        buildWatchtower(c, -56, 62);
        buildRuinedChapel(c, 58, 56);
        buildRiversideDocks(c, 0, 48);
        scatterTreesAndCamps(c);
        registerAirdrops(c);
    }

    private void buildTerrain(SgCanvas c) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double dist = Math.hypot(x, z);
                if (dist > RADIUS) continue;

                int h = terrainHeight(x, z);
                for (int y = 57; y <= h; y++) {
                    Material m;
                    if (y < h - 2) m = Material.STONE;
                    else if (y < h) m = Material.DIRT;
                    else m = (h <= WATER_Y) ? Material.SAND : (h >= 73 ? Material.COARSE_DIRT : Material.GRASS_BLOCK);
                    c.set(x, y, z, m);
                }
                if (h < WATER_Y) {
                    for (int y = h + 1; y <= WATER_Y; y++) {
                        c.set(x, y, z, Material.WATER);
                    }
                } else if (dist > 22 && dist < 110 && (Math.abs(x) <= 2 || Math.abs(z) <= 2)) {
                    c.set(x, h, z, Material.DIRT_PATH);
                }

                // Natural cliff border at perimeter
                if (dist >= RADIUS - 2) {
                    for (int y = h + 1; y <= h + 5; y++) {
                        c.set(x, y, z, y == h + 5 ? Material.OAK_LEAVES : Material.MOSSY_COBBLESTONE);
                    }
                }
            }
        }
    }

    private void buildCornucopia(SgCanvas c) {
        int y = 64;
        c.cylinder(0, 0, y + 1, y + 1, 9.5, Material.MOSSY_STONE_BRICKS);
        c.cylinder(0, 0, y + 2, y + 2, 5.0, Material.STONE_BRICKS);

        int[][] arches = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] a : arches) {
            for (int dy = 2; dy <= 6; dy++) {
                c.set(a[0], y + dy, a[1], dy % 2 == 0 ? Material.MOSSY_STONE_BRICKS : Material.CRACKED_STONE_BRICKS);
            }
            c.set(a[0], y + 7, a[1], Material.LANTERN);
        }

        // 12 Cornucopia chests
        for (int i = 0; i < 12; i++) {
            double ang = (2.0 * Math.PI * i) / 12.0;
            double r = (i % 2 == 0) ? 7.0 : 3.8;
            int cy = (i % 2 == 0) ? y + 2 : y + 3;
            c.chest((int) Math.round(Math.cos(ang) * r), cy, (int) Math.round(Math.sin(ang) * r), LootTable.Tier.CORNUCOPIA);
        }

        // 24 spawn pedestals
        for (int i = 0; i < 24; i++) {
            double ang = (2.0 * Math.PI * i) / 24.0;
            int px = (int) Math.round(Math.cos(ang) * 16.5);
            int pz = (int) Math.round(Math.sin(ang) * 16.5);
            c.pedestal(px, y + 1, pz, Material.STONE_BRICKS);
        }
    }

    private void buildBridges(SgCanvas c) {
        int[] bridgeXs = {-68, -2, 68};
        for (int bx : bridgeXs) {
            int rz = (int) Math.round(18.0 * Math.sin(bx * 0.045) + 48.0);
            for (int x = bx - 2; x <= bx + 2; x++) {
                for (int z = rz - 7; z <= rz + 7; z++) {
                    int arch = Math.abs(z - rz) <= 2 ? 2 : (Math.abs(z - rz) <= 5 ? 1 : 0);
                    int by = WATER_Y + 1 + arch;
                    c.set(x, by, z, Material.SPRUCE_PLANKS);
                    if (x == bx - 2 || x == bx + 2) c.set(x, by + 1, z, Material.OAK_FENCE);
                }
            }
        }
    }

    private void buildRedBarn(SgCanvas c, int cx, int cz) {
        int y = flattenPad(c, cx - 8, cz - 7, cx + 8, cz + 7);
        c.hollowBox(cx - 8, y, cz - 7, cx + 8, y + 5, cz + 7, Material.RED_TERRACOTTA, Material.SPRUCE_PLANKS, Material.OAK_PLANKS);
        c.hollowBox(cx - 8, y + 5, cz - 7, cx + 8, y + 9, cz + 7, Material.SPRUCE_PLANKS, Material.OAK_PLANKS, Material.DARK_OAK_SLAB);
        c.doorway(cx - 1, y + 1, cz + 7, 1, 0, 3, 3);
        c.doorway(cx - 1, y + 1, cz - 7, 1, 0, 3, 3);
        c.staircase(cx - 6, y + 1, cz - 4, 1, 0, 5, Material.OAK_STAIRS);
        c.fill(cx - 1, y + 6, cz - 4, cx, y + 8, cz - 4, Material.AIR);

        // Hay bales & chests
        c.fill(cx + 4, y + 1, cz - 4, cx + 6, y + 2, cz - 3, Material.HAY_BLOCK);
        c.chest(cx + 5, y + 1, cz + 4, LootTable.Tier.OUTER);
        c.chest(cx - 5, y + 6, cz + 4, LootTable.Tier.CORNUCOPIA);
        c.chest(cx + 5, y + 6, cz - 4, LootTable.Tier.OUTER);
    }

    private void buildLumberMill(SgCanvas c, int cx, int cz) {
        int y = flattenPad(c, cx - 7, cz - 7, cx + 7, cz + 7);
        c.hollowBox(cx - 7, y, cz - 7, cx + 7, y + 5, cz + 7, Material.OAK_LOG, Material.COBBLESTONE, Material.SPRUCE_SLAB);
        c.doorway(cx - 7, y + 1, cz, 0, 1, 2, 3);
        c.doorway(cx, y + 1, cz + 7, 1, 0, 2, 3);
        c.fill(cx + 2, y + 1, cz - 4, cx + 5, y + 2, cz - 2, Material.SPRUCE_LOG);
        c.chest(cx - 4, y + 1, cz - 4, LootTable.Tier.OUTER);
        c.chest(cx + 4, y + 1, cz + 4, LootTable.Tier.OUTER);
        c.chest(cx, y + 6, cz, LootTable.Tier.OUTER);
    }

    private void buildWatchtower(SgCanvas c, int cx, int cz) {
        int y = flattenPad(c, cx - 5, cz - 5, cx + 5, cz + 5);
        for (int f = 0; f < 3; f++) {
            int fy = y + f * 5;
            c.hollowBox(cx - 4, fy, cz - 4, cx + 4, fy + 5, cz + 4, Material.STONE_BRICKS, Material.SPRUCE_PLANKS, Material.SPRUCE_PLANKS);
            c.staircase(cx - 2, fy + 1, cz - 2 + (f % 2 == 0 ? 0 : 4), 1, 0, 5, Material.STONE_BRICK_STAIRS);
        }
        c.doorway(cx + 4, y + 1, cz, 0, 1, 2, 3);
        int topY = y + 15;
        for (int x = cx - 4; x <= cx + 4; x += 2) {
            c.set(x, topY + 1, cz - 4, Material.STONE_BRICK_WALL);
            c.set(x, topY + 1, cz + 4, Material.STONE_BRICK_WALL);
        }
        c.chest(cx + 2, y + 1, cz + 2, LootTable.Tier.OUTER);
        c.chest(cx, topY + 1, cz, LootTable.Tier.CORNUCOPIA);
    }

    private void buildRuinedChapel(SgCanvas c, int cx, int cz) {
        int y = flattenPad(c, cx - 7, cz - 7, cx + 7, cz + 7);
        c.hollowBox(cx - 7, y, cz - 7, cx + 7, y + 6, cz + 7, Material.MOSSY_STONE_BRICKS, Material.POLISHED_ANDESITE, Material.AIR);
        c.doorway(cx - 7, y + 1, cz - 1, 0, 1, 3, 4);
        c.doorway(cx - 1, y + 1, cz - 7, 1, 0, 3, 4);
        c.enchantArea(cx, y, cz);
        c.chest(cx + 5, y + 1, cz + 5, LootTable.Tier.CORNUCOPIA);
        c.chest(cx - 5, y + 1, cz + 5, LootTable.Tier.OUTER);
    }

    private void buildRiversideDocks(SgCanvas c, int cx, int cz) {
        for (int x = cx - 8; x <= cx + 8; x++) {
            for (int z = cz - 16; z <= cz - 6; z++) {
                c.set(x, WATER_Y + 1, z, Material.OAK_PLANKS);
            }
        }
        c.chest(cx - 6, WATER_Y + 2, cz - 10, LootTable.Tier.OUTER);
        c.chest(cx + 6, WATER_Y + 2, cz - 10, LootTable.Tier.OUTER);
    }

    private void scatterTreesAndCamps(SgCanvas c) {
        for (int i = 0; i < 42; i++) {
            double ang = i * 2.39996;
            double r = 28.0 + ((i * 19) % 82);
            int x = (int) Math.round(Math.cos(ang) * r);
            int z = (int) Math.round(Math.sin(ang) * r);
            if (Math.hypot(x + 62, z + 46) < 15 || Math.hypot(x - 56, z + 42) < 14
                    || Math.hypot(x + 56, z - 62) < 12 || Math.hypot(x - 58, z - 56) < 14) {
                continue;
            }
            int h = terrainHeight(x, z);
            if (h <= WATER_Y) continue;
            if (i % 5 == 0) {
                c.set(x, h + 1, z, Material.CAMPFIRE);
                c.chest(x + 2, h + 1, z, LootTable.Tier.OUTER);
            } else {
                Material log = (i % 2 == 0) ? Material.OAK_LOG : Material.SPRUCE_LOG;
                Material leaf = (i % 2 == 0) ? Material.OAK_LEAVES : Material.SPRUCE_LEAVES;
                c.tree(x, h, z, log, leaf, 4 + (i % 3), 2);
            }
        }
    }

    private int flattenPad(SgCanvas c, int x1, int z1, int x2, int z2) {
        int y = Math.max(WATER_Y + 1, terrainHeight((x1 + x2) / 2, (z1 + z2) / 2));
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                c.fill(x, 58, z, x, y, z, Material.STONE);
                c.set(x, y, z, Material.GRASS_BLOCK);
                c.fill(x, y + 1, z, x, y + 12, z, Material.AIR);
            }
        }
        return y;
    }

    private void registerAirdrops(SgCanvas c) {
        c.addAirdropSpot(0, -40);
        c.addAirdropSpot(-34, 0);
        c.addAirdropSpot(34, 0);
        c.addAirdropSpot(-35, -35);
        c.addAirdropSpot(35, 35);
        c.addAirdropSpot(0, 76);
    }
}
