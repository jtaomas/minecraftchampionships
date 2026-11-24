package com.mcm.games.hungergames.map;

import com.mcm.games.hungergames.LootTable;
import org.bukkit.Material;

/**
 * Coastal harbour, navigable canals, moored multi-deck ships & citadel SG map.
 * Starter Oak Boats excel across the wide waterways connecting docks and islands.
 */
public final class HarbourCitadelMap implements SgMap {
    private static final int RADIUS = 124;
    private static final int WATER_Y = 63;

    @Override
    public String id() {
        return "harbour_citadel";
    }

    @Override
    public String displayName() {
        return "Harbour Citadel";
    }

    @Override
    public boolean boatRecommended() {
        return true;
    }

    @Override
    public int radius() {
        return RADIUS;
    }

    @Override
    public void generate(SgCanvas c) {
        buildHarbourBasin(c);
        buildCornucopiaCitadel(c);
        buildGalleonShip(c, -36, -54, Material.RED_WOOL);
        buildGalleonShip(c, 42, -52, Material.BLUE_WOOL);
        buildGalleonShip(c, -44, 54, Material.YELLOW_WOOL);
        buildLighthouse(c, 0, -86);
        buildWarehouse(c, -82, 0, Material.BRICKS);
        buildWarehouse(c, 82, 0, Material.STONE_BRICKS);
        buildArcaneStudy(c, 0, 82);
        buildMarketStalls(c);
        registerAirdrops(c);
    }

    private void buildHarbourBasin(SgCanvas c) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double dist = Math.hypot(x, z);
                if (dist > RADIUS) continue;

                // Water basin with sandy floor
                c.set(x, 58, z, Material.STONE);
                c.set(x, 59, z, Material.SAND);
                for (int y = 60; y <= WATER_Y; y++) {
                    c.set(x, y, z, Material.WATER);
                }

                // Four main stone harbour quays along N/S/E/W + outer seawall ring
                boolean centralIsland = dist <= 25;
                boolean outerQuay = dist >= 72 && dist <= 116;
                boolean canalCut = (Math.abs(x) <= 14 || Math.abs(z) <= 14) && dist > 25 && dist < 118;
                boolean innerPier = ((Math.abs(x) <= 4 && dist <= 76) || (Math.abs(z) <= 4 && dist <= 76)) && dist > 23;

                if (centralIsland || (outerQuay && !canalCut)) {
                    for (int y = 59; y <= WATER_Y + 1; y++) {
                        c.set(x, y, z, y == WATER_Y + 1 ? Material.STONE_BRICKS : Material.COBBLESTONE);
                    }
                } else if (innerPier) {
                    c.set(x, WATER_Y + 1, z, Material.SPRUCE_PLANKS);
                }

                // Harbour seawall boundary
                if (dist >= RADIUS - 3) {
                    for (int y = 59; y <= WATER_Y + 7; y++) {
                        c.set(x, y, z, y == WATER_Y + 7 ? Material.STONE_BRICK_SLAB : Material.STONE_BRICKS);
                    }
                }
            }
        }
    }

    private void buildCornucopiaCitadel(SgCanvas c) {
        int y = WATER_Y + 1;
        c.cylinder(0, 0, y + 1, y + 1, 9.0, Material.SMOOTH_STONE);
        c.cylinder(0, 0, y + 2, y + 2, 5.0, Material.PRISMARINE_BRICKS);

        int[][] pillars = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] p : pillars) {
            for (int dy = 2; dy <= 7; dy++) c.set(p[0], y + dy, p[1], Material.PRISMARINE_BRICKS);
            c.set(p[0], y + 8, p[1], Material.SEA_LANTERN);
        }

        for (int i = 0; i < 12; i++) {
            double ang = (2.0 * Math.PI * i) / 12.0;
            double r = (i % 2 == 0) ? 7.0 : 3.8;
            int cy = (i % 2 == 0) ? y + 2 : y + 3;
            c.chest((int) Math.round(Math.cos(ang) * r), cy, (int) Math.round(Math.sin(ang) * r), LootTable.Tier.CORNUCOPIA);
        }

        for (int i = 0; i < 24; i++) {
            double ang = (2.0 * Math.PI * i) / 24.0;
            int px = (int) Math.round(Math.cos(ang) * 16.5);
            int pz = (int) Math.round(Math.sin(ang) * 16.5);
            c.pedestal(px, y + 1, pz, Material.SMOOTH_QUARTZ);
        }
    }

    private void buildGalleonShip(SgCanvas c, int cx, int cz, Material sailWool) {
        int deckY = WATER_Y + 2;
        int holdY = WATER_Y - 1;
        // Hull & lower hold
        for (int dx = -12; dx <= 12; dx++) {
            int halfW = Math.max(1, 5 - Math.abs(dx) / 3);
            for (int dz = -halfW; dz <= halfW; dz++) {
                boolean rim = Math.abs(dz) == halfW || Math.abs(dx) >= 11;
                c.set(cx + dx, holdY - 1, cz + dz, Material.DARK_OAK_PLANKS);
                for (int y = holdY; y <= deckY; y++) {
                    c.set(cx + dx, y, cz + dz, (rim || y == deckY) ? Material.SPRUCE_PLANKS : Material.AIR);
                }
                if (rim) c.set(cx + dx, deckY + 1, cz + dz, Material.OAK_FENCE);
            }
        }
        // Gangplank toward center/pier + hold stair
        c.fill(cx - 2, deckY, cz + 5, cx + 2, deckY, cz + 12, Material.OAK_PLANKS);
        c.staircase(cx - 4, holdY, cz, 1, 0, 3, Material.SPRUCE_STAIRS);
        c.fill(cx - 1, deckY, cz, cx, deckY, cz, Material.AIR);

        // Stern captain's cabin
        c.hollowBox(cx + 5, deckY, cz - 3, cx + 11, deckY + 4, cz + 3, Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS, Material.DARK_OAK_SLAB);
        c.doorway(cx + 5, deckY + 1, cz, 0, 1, 1, 3);

        // Central mast & wool sail
        for (int dy = 1; dy <= 11; dy++) c.set(cx, deckY + dy, cz, Material.OAK_LOG);
        for (int dz = -4; dz <= 4; dz++) {
            for (int dy = 5; dy <= 10; dy++) {
                c.set(cx - 1, deckY + dy, cz + dz, sailWool);
            }
        }

        c.chest(cx + 9, deckY + 1, cz, LootTable.Tier.CORNUCOPIA);
        c.chest(cx - 6, holdY, cz, LootTable.Tier.OUTER);
        c.chest(cx - 8, deckY + 1, cz, LootTable.Tier.OUTER);
    }

    private void buildLighthouse(SgCanvas c, int cx, int cz) {
        int baseY = WATER_Y + 1;
        c.cylinder(cx, cz, baseY, baseY, 7.0, Material.STONE_BRICKS);
        for (int dy = 1; dy <= 18; dy++) {
            Material ring = ((dy / 3) % 2 == 0) ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d >= 3.0 && d <= 4.3) {
                        c.set(x, baseY + dy, z, ring);
                    } else if (d < 3.0) {
                        c.set(x, baseY + dy, z, Material.AIR);
                    }
                }
            }
        }
        c.doorway(cx - 1, baseY + 1, cz + 4, 1, 0, 2, 3);
        // Spiral steps inside lighthouse
        for (int dy = 1; dy <= 16; dy++) {
            double ang = dy * 0.65;
            int sx = cx + (int) Math.round(Math.cos(ang) * 2.0);
            int sz = cz + (int) Math.round(Math.sin(ang) * 2.0);
            c.set(sx, baseY + dy, sz, Material.SMOOTH_STONE_SLAB);
        }
        c.cylinder(cx, cz, baseY + 17, baseY + 17, 4.5, Material.STONE_BRICKS);
        c.set(cx, baseY + 19, cz, Material.SEA_LANTERN);
        c.chest(cx + 2, baseY + 18, cz, LootTable.Tier.CORNUCOPIA);
        c.chest(cx - 2, baseY + 1, cz - 2, LootTable.Tier.OUTER);
    }

    private void buildWarehouse(SgCanvas c, int cx, int cz, Material wall) {
        int y = WATER_Y + 1;
        c.hollowBox(cx - 9, y, cz - 10, cx + 9, y + 5, cz + 10, wall, Material.STONE_BRICKS, Material.SPRUCE_PLANKS);
        c.hollowBox(cx - 9, y + 5, cz - 10, cx + 9, y + 10, cz + 10, wall, Material.SPRUCE_PLANKS, Material.DARK_OAK_SLAB);
        c.doorway(cx - 9, y + 1, cz - 1, 0, 1, 3, 3);
        c.doorway(cx + 9, y + 1, cz - 1, 0, 1, 3, 3);
        c.staircase(cx - 6, y + 1, cz - 6, 1, 0, 5, Material.OAK_STAIRS);
        c.fill(cx - 1, y + 6, cz - 6, cx, y + 8, cz - 6, Material.AIR);

        c.chest(cx + 5, y + 1, cz + 6, LootTable.Tier.OUTER);
        c.chest(cx - 5, y + 1, cz + 6, LootTable.Tier.OUTER);
        c.chest(cx + 5, y + 6, cz - 6, LootTable.Tier.CORNUCOPIA);
        c.chest(cx, y + 11, cz, LootTable.Tier.OUTER);
    }

    private void buildArcaneStudy(SgCanvas c, int cx, int cz) {
        int y = WATER_Y + 1;
        c.hollowBox(cx - 7, y, cz - 7, cx + 7, y + 6, cz + 7, Material.CHISELED_STONE_BRICKS, Material.POLISHED_DEEPSLATE, Material.PRISMARINE_SLAB);
        c.doorway(cx - 1, y + 1, cz - 7, 1, 0, 3, 3);
        c.doorway(cx - 7, y + 1, cz - 1, 0, 1, 2, 3);
        c.doorway(cx + 7, y + 1, cz - 1, 0, 1, 2, 3);
        c.enchantArea(cx, y, cz);
        c.chest(cx - 5, y + 1, cz + 5, LootTable.Tier.CORNUCOPIA);
        c.chest(cx + 5, y + 1, cz + 5, LootTable.Tier.OUTER);
    }

    private void buildMarketStalls(SgCanvas c) {
        int y = WATER_Y + 1;
        int[][] spots = {{44, 44}, {58, 44}, {44, 58}, {-58, -82}, {58, -82}, {-58, 82}, {58, 82}};
        Material[] wools = {Material.ORANGE_WOOL, Material.LIME_WOOL, Material.CYAN_WOOL, Material.MAGENTA_WOOL,
                Material.YELLOW_WOOL, Material.RED_WOOL, Material.LIGHT_BLUE_WOOL};
        for (int i = 0; i < spots.length; i++) {
            int sx = spots[i][0];
            int sz = spots[i][1];
            for (int dy = 1; dy <= 3; dy++) {
                c.set(sx - 2, y + dy, sz - 2, Material.OAK_FENCE);
                c.set(sx + 2, y + dy, sz - 2, Material.OAK_FENCE);
                c.set(sx - 2, y + dy, sz + 2, Material.OAK_FENCE);
                c.set(sx + 2, y + dy, sz + 2, Material.OAK_FENCE);
            }
            for (int x = sx - 2; x <= sx + 2; x++) {
                for (int z = sz - 2; z <= sz + 2; z++) {
                    c.set(x, y + 4, z, ((x + z) % 2 == 0) ? wools[i] : Material.WHITE_WOOL);
                }
            }
            c.chest(sx, y + 1, sz, LootTable.Tier.OUTER);
        }
    }

    private void registerAirdrops(SgCanvas c) {
        c.addAirdropSpot(0, 42);
        c.addAirdropSpot(0, -42);
        c.addAirdropSpot(42, 0);
        c.addAirdropSpot(-42, 0);
        c.addAirdropSpot(82, 42);
        c.addAirdropSpot(-82, -42);
    }
}
