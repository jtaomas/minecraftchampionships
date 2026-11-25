package com.mcm.games.hungergames.map;

import com.mcm.games.hungergames.LootTable;
import org.bukkit.Material;

/**
 * Urban SG map inspired by MCC City Living: road grid, Civic Plaza Cornucopia,
 * 16+ multi-storey buildings with staircases & rooftops, Central Park, and Library.
 */
public final class CityDistrictMap implements SgMap {
    private static final int RADIUS = 124;
    private static final int Y = 64;

    @Override
    public String id() {
        return "city_district";
    }

    @Override
    public String displayName() {
        return "City District";
    }

    @Override
    public boolean boatRecommended() {
        return false;
    }

    @Override
    public int radius() {
        return RADIUS;
    }

    @Override
    public void generate(SgCanvas c) {
        buildRoadsAndPlaza(c);
        buildCornucopia(c);
        buildDistrictBuildings(c);
        buildCityPark(c);
        buildPerimeterWall(c);
        registerAirdrops(c);
    }

    private void buildRoadsAndPlaza(SgCanvas c) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double dist = Math.hypot(x, z);
                if (dist > RADIUS) continue;

                c.set(x, Y - 2, z, Material.STONE);
                c.set(x, Y - 1, z, Material.COBBLESTONE);

                int ax = Math.abs(x);
                int az = Math.abs(z);
                boolean mainAve = ax <= 5 || az <= 5;
                boolean outerRing = (ax >= 44 && ax <= 50) || (az >= 44 && az <= 50);
                boolean lane = mainAve || outerRing;
                boolean sidewalk = !lane && (ax <= 7 || az <= 7 || (ax >= 42 && ax <= 52) || (az >= 42 && az <= 52));

                if (dist <= 26) {
                    boolean ring = dist >= 22;
                    c.set(x, Y, z, ring ? Material.SMOOTH_STONE : Material.STONE_BRICKS);
                } else if (lane) {
                    boolean dash = ((ax <= 1 && Math.floorMod(z, 6) < 3) || (az <= 1 && Math.floorMod(x, 6) < 3)) && dist > 28;
                    c.set(x, Y, z, dash ? Material.YELLOW_CONCRETE : Material.GRAY_CONCRETE);
                } else if (sidewalk) {
                    c.set(x, Y, z, Material.SMOOTH_STONE);
                } else {
                    c.set(x, Y, z, Material.STONE_BRICKS);
                }
            }
        }

        // Streetlamps along main avenues and ring roads
        int[] lampCoords = {-84, -64, -34, 34, 64, 84};
        for (int p : lampCoords) {
            placeLamp(c, p, -7);
            placeLamp(c, p, 7);
            placeLamp(c, -7, p);
            placeLamp(c, 7, p);
        }
    }

    private void placeLamp(SgCanvas c, int x, int z) {
        if (Math.hypot(x, z) > RADIUS - 4) return;
        for (int dy = 1; dy <= 4; dy++) c.set(x, Y + dy, z, Material.IRON_BARS);
        c.set(x, Y + 5, z, Material.SEA_LANTERN);
    }

    private void buildCornucopia(SgCanvas c) {
        c.cylinder(0, 0, Y + 1, Y + 1, 9.5, Material.QUARTZ_BRICKS);
        c.cylinder(0, 0, Y + 2, Y + 2, 5.5, Material.SMOOTH_QUARTZ);
        c.cylinder(0, 0, Y + 3, Y + 3, 2.5, Material.GOLD_BLOCK);

        // Central civic spire & sea-lantern beacon
        for (int dy = 4; dy <= 10; dy++) c.set(0, Y + dy, 0, Material.QUARTZ_PILLAR);
        c.set(0, Y + 11, 0, Material.SEA_LANTERN);
        int[][] arches = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] a : arches) {
            for (int dy = 2; dy <= 6; dy++) c.set(a[0], Y + dy, a[1], Material.QUARTZ_PILLAR);
            c.set(a[0], Y + 7, a[1], Material.GOLD_BLOCK);
        }

        // Stairs into central dais
        for (int s = -2; s <= 2; s++) {
            c.set(s, Y + 1, 10, Material.QUARTZ_SLAB);
            c.set(s, Y + 1, -10, Material.QUARTZ_SLAB);
            c.set(10, Y + 1, s, Material.QUARTZ_SLAB);
            c.set(-10, Y + 1, s, Material.QUARTZ_SLAB);
        }

        // 12 Tier-2 Cornucopia chests
        for (int i = 0; i < 12; i++) {
            double ang = (2.0 * Math.PI * i) / 12.0;
            double r = (i % 2 == 0) ? 7.2 : 4.2;
            int cy = (i % 2 == 0) ? Y + 2 : Y + 3;
            int cx = (int) Math.round(Math.cos(ang) * r);
            int cz = (int) Math.round(Math.sin(ang) * r);
            c.chest(cx, cy, cz, LootTable.Tier.CORNUCOPIA);
        }

        // 24 Spawn pedestals on radius 17
        for (int i = 0; i < 24; i++) {
            double ang = (2.0 * Math.PI * i) / 24.0;
            int px = (int) Math.round(Math.cos(ang) * 17.0);
            int pz = (int) Math.round(Math.sin(ang) * 17.0);
            c.pedestal(px, Y + 1, pz, Material.SMOOTH_QUARTZ);
        }
    }

    private void buildDistrictBuildings(SgCanvas c) {
        // Inner quadrant blocks (between +/- 9..40) — leaving NE quadrant (x>0, z<0) partially open for City Park + Library
        buildMultiStorey(c, -36, -36, 16, 15, 2, Material.BRICKS, Material.DARK_OAK_PLANKS, Material.STONE_BRICK_SLAB, true, false);
        buildMultiStorey(c, -20, -36, 11, 13, 3, Material.TERRACOTTA, Material.SPRUCE_PLANKS, Material.DEEPSLATE_TILE_SLAB, true, false);
        buildMultiStorey(c, -36, -19, 14, 10, 2, Material.WHITE_CONCRETE, Material.OAK_PLANKS, Material.SMOOTH_STONE_SLAB, false, false);

        buildMultiStorey(c, -36, 10, 15, 16, 3, Material.SANDSTONE, Material.BIRCH_PLANKS, Material.SANDSTONE_SLAB, true, false);
        buildMultiStorey(c, -19, 10, 10, 14, 2, Material.RED_TERRACOTTA, Material.SPRUCE_PLANKS, Material.BRICK_SLAB, false, false);
        buildMultiStorey(c, -36, 28, 24, 12, 2, Material.DEEPSLATE_BRICKS, Material.OAK_PLANKS, Material.POLISHED_DEEPSLATE_SLAB, true, false);

        buildMultiStorey(c, 10, 10, 16, 15, 3, Material.LIGHT_GRAY_CONCRETE, Material.DARK_OAK_PLANKS, Material.STONE_SLAB, true, false);
        buildMultiStorey(c, 28, 10, 12, 15, 2, Material.YELLOW_TERRACOTTA, Material.BIRCH_PLANKS, Material.SMOOTH_SANDSTONE_SLAB, false, false);
        buildMultiStorey(c, 10, 27, 29, 13, 2, Material.BRICKS, Material.SPRUCE_PLANKS, Material.STONE_BRICK_SLAB, true, false);

        // Library in NE block edge (with Enchanting Table landmark)
        buildMultiStorey(c, 10, -38, 16, 16, 2, Material.CHISELED_STONE_BRICKS, Material.DARK_OAK_PLANKS, Material.DARK_OAK_SLAB, true, true);

        // Outer blocks (beyond ring road 54..102)
        buildMultiStorey(c, -86, -34, 24, 20, 3, Material.DEEPSLATE_TILES, Material.SPRUCE_PLANKS, Material.POLISHED_BLACKSTONE_SLAB, true, false);
        buildMultiStorey(c, -86, 12, 24, 24, 2, Material.ORANGE_TERRACOTTA, Material.OAK_PLANKS, Material.ACACIA_SLAB, true, false);
        buildMultiStorey(c, -36, -86, 22, 24, 3, Material.SMOOTH_SANDSTONE, Material.BIRCH_PLANKS, Material.SANDSTONE_SLAB, true, false);
        buildMultiStorey(c, 14, -86, 22, 24, 2, Material.CYAN_TERRACOTTA, Material.OAK_PLANKS, Material.PRISMARINE_SLAB, true, false);
        buildMultiStorey(c, -36, 56, 22, 24, 3, Material.BRICKS, Material.DARK_OAK_PLANKS, Material.BRICK_SLAB, true, false);
        buildMultiStorey(c, 14, 56, 24, 24, 2, Material.STONE_BRICKS, Material.SPRUCE_PLANKS, Material.SMOOTH_STONE_SLAB, true, false);
        buildMultiStorey(c, 58, -32, 22, 20, 3, Material.WHITE_TERRACOTTA, Material.OAK_PLANKS, Material.QUARTZ_SLAB, true, false);
        buildMultiStorey(c, 58, 12, 22, 24, 2, Material.POLISHED_ANDESITE, Material.DARK_OAK_PLANKS, Material.ANDESITE_SLAB, true, false);
    }

    private void buildMultiStorey(SgCanvas c, int x0, int z0, int w, int d, int floors,
                                  Material wall, Material floorMat, Material roofTrim,
                                  boolean roofAccess, boolean isLibrary) {
        int floorH = 5;
        int x1 = x0 + w - 1;
        int z1 = z0 + d - 1;
        int totalH = floors * floorH;

        for (int f = 0; f < floors; f++) {
            int fy = Y + f * floorH;
            c.hollowBox(x0, fy, z0, x1, fy + floorH, z1, wall, f == 0 ? Material.POLISHED_ANDESITE : floorMat, floorMat);

            // Windows along walls
            for (int x = x0 + 2; x <= x1 - 2; x += 3) {
                c.set(x, fy + 2, z0, Material.GLASS_PANE);
                c.set(x, fy + 3, z0, Material.GLASS_PANE);
                c.set(x, fy + 2, z1, Material.GLASS_PANE);
                c.set(x, fy + 3, z1, Material.GLASS_PANE);
            }
            for (int z = z0 + 2; z <= z1 - 2; z += 3) {
                c.set(x0, fy + 2, z, Material.GLASS_PANE);
                c.set(x0, fy + 3, z, Material.GLASS_PANE);
                c.set(x1, fy + 2, z, Material.GLASS_PANE);
                c.set(x1, fy + 3, z, Material.GLASS_PANE);
            }

            // Interior lighting
            c.set(x0 + w / 2, fy + floorH - 1, z0 + d / 2, Material.SEA_LANTERN);
        }

        // Ground doorways on all 4 sides facing sidewalks
        c.doorway(x0 + w / 2 - 1, Y + 1, z0, 1, 0, 2, 3);
        c.doorway(x0 + w / 2 - 1, Y + 1, z1, 1, 0, 2, 3);
        c.doorway(x0, Y + 1, z0 + d / 2 - 1, 0, 1, 2, 3);
        c.doorway(x1, Y + 1, z0 + d / 2 - 1, 0, 1, 2, 3);

        // Staircases connecting floor 0 -> 1 -> ... -> rooftop
        int stairLevels = roofAccess ? floors : Math.max(0, floors - 1);
        for (int f = 0; f < stairLevels; f++) {
            int baseY = Y + f * floorH + 1;
            int sx = x0 + 2;
            int sz = z0 + 2 + (f % 2 == 0 ? 0 : 2);
            c.staircase(sx, baseY, sz, 1, 0, floorH, Material.OAK_STAIRS);
            // Clear landing at top
            c.fill(sx + floorH, baseY + floorH, sz, sx + floorH + 1, baseY + floorH + 2, sz, Material.AIR);
        }

        // Parapet trim around rooftop
        int roofY = Y + totalH;
        for (int x = x0; x <= x1; x++) {
            c.set(x, roofY + 1, z0, roofTrim);
            c.set(x, roofY + 1, z1, roofTrim);
        }
        for (int z = z0; z <= z1; z++) {
            c.set(x0, roofY + 1, z, roofTrim);
            c.set(x1, roofY + 1, z, roofTrim);
        }

        if (isLibrary) {
            c.enchantArea(x0 + w / 2, Y, z0 + d / 2);
            c.chest(x1 - 2, Y + 1, z1 - 2, LootTable.Tier.CORNUCOPIA);
        } else {
            c.chest(x0 + w - 3, Y + 1, z0 + 2, LootTable.Tier.OUTER);
        }

        if (floors >= 2) {
            c.chest(x0 + w - 3, Y + floorH + 1, z1 - 3, LootTable.Tier.OUTER);
        }
        if (roofAccess) {
            c.chest(x0 + w / 2, roofY + 1, z0 + d / 2, floors >= 3 ? LootTable.Tier.CORNUCOPIA : LootTable.Tier.OUTER);
        }
    }

    private void buildCityPark(SgCanvas c) {
        // NE quadrant park (x: 28..40, z: -38..-10)
        for (int x = 28; x <= 40; x++) {
            for (int z = -38; z <= -10; z++) {
                boolean pond = Math.hypot(x - 34, z + 24) <= 4.5;
                if (pond) {
                    c.set(x, Y - 1, z, Material.SAND);
                    c.set(x, Y, z, Material.WATER);
                } else {
                    c.set(x, Y, z, Material.GRASS_BLOCK);
                }
            }
        }
        c.tree(30, Y, -14, Material.OAK_LOG, Material.OAK_LEAVES, 4, 2);
        c.tree(38, Y, -34, Material.BIRCH_LOG, Material.BIRCH_LEAVES, 5, 2);
        c.chest(34, Y + 1, -12, LootTable.Tier.OUTER);
        c.chest(38, Y + 1, -24, LootTable.Tier.OUTER);
    }

    private void buildPerimeterWall(SgCanvas c) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = Math.hypot(x, z);
                if (d >= RADIUS - 2 && d <= RADIUS) {
                    for (int dy = 1; dy <= 7; dy++) {
                        c.set(x, Y + dy, z, dy == 7 ? Material.SMOOTH_STONE_SLAB : Material.STONE_BRICKS);
                    }
                }
            }
        }
    }

    private void registerAirdrops(SgCanvas c) {
        c.addAirdropSpot(0, 47);
        c.addAirdropSpot(0, -47);
        c.addAirdropSpot(47, 0);
        c.addAirdropSpot(-47, 0);
        c.addAirdropSpot(34, -20);
        c.addAirdropSpot(-47, -47);
        c.addAirdropSpot(47, 47);
    }
}
