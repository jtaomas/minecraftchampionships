package com.mcm.games.rocketspleef.map;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry and builders for the 4 MCC-inspired Rocket Spleef Rush floating void maps:
 * journey_isles, sky_temple, santorini_cliffs, and overlode_core.
 */
public final class ArenaMaps {
    private static final Map<String, RocketArenaMap> MAPS = new LinkedHashMap<>();

    static {
        register(new JourneyIslesMap());
        register(new SkyTempleMap());
        register(new SantoriniCliffsMap());
        register(new OverlodeCoreMap());
    }

    private ArenaMaps() {}

    private static void register(RocketArenaMap map) {
        MAPS.put(map.id(), map);
    }

    public static List<String> ids() {
        return List.copyOf(MAPS.keySet());
    }

    public static RocketArenaMap get(String id) {
        return id == null ? null : MAPS.get(id.toLowerCase());
    }

    public static RocketArenaMap forRound(int roundIndex) {
        List<RocketArenaMap> all = new ArrayList<>(MAPS.values());
        return all.get(Math.floorMod(roundIndex, all.size()));
    }

    // -------------------------------------------------------------------------
    // 1. Journey Isles (Journey-inspired floating sky-archipelago + dynamic pads)
    // -------------------------------------------------------------------------
    private static final class JourneyIslesMap implements RocketArenaMap {
        @Override public String id() { return "journey_isles"; }
        @Override public String displayName() { return "Journey Isles"; }
        @Override public boolean supportsDynamicPads() { return true; }

        @Override
        public void generate(RsCanvas c, int scaleRadius) {
            int r = Math.max(24, Math.min(34, scaleRadius));

            // Main central mossy floating island with stone spire underside
            c.taperedIsland(0, 86, 0, 9, 6, Material.GRASS_BLOCK, Material.STONE, Material.COBBLESTONE);
            c.ring(0, 86, 0, 6.5, 9.0, Material.MOSS_BLOCK);
            c.tree(0, 86, 0, Material.OAK_LOG, Material.OAK_LEAVES, 5);

            // High central sky-ring (y = 104) with 4 anchor pillars
            c.ring(0, 104, 0, 5.5, 8.5, Material.WHITE_CONCRETE);
            c.ring(0, 104, 0, 4.5, 5.5, Material.LIGHT_BLUE_CONCRETE);

            // 6 outer stepped islands + lantern bridges + trees + guaranteed solid spawns
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                int ix = (int) Math.round(Math.cos(a) * r);
                int iz = (int) Math.round(Math.sin(a) * r);
                int iy = (i % 2 == 0) ? 95 : 91;

                c.taperedIsland(ix, iy, iz, 7, 5, Material.GRASS_BLOCK, Material.ANDESITE, Material.STONE);
                c.disc(ix, iy, iz, 3.2, Material.SMOOTH_STONE);
                c.bridge(ix, iy, iz, 0, 86, 0, 1, Material.OAK_PLANKS);

                // Side tree & lantern away from spawn center
                int tx = ix + (int) Math.round(Math.cos(a) * 3.5);
                int tz = iz + (int) Math.round(Math.sin(a) * 3.5);
                c.tree(tx, iy, tz, Material.BIRCH_LOG, Material.AZALEA_LEAVES, 4);
                c.set(ix, iy + 1, iz, Material.SEA_LANTERN);

                // Place 2 spawns per outer island directly on the smooth stone pad
                int s1x = ix + (int) Math.round(Math.cos(a + 0.7) * 2.0);
                int s1z = iz + (int) Math.round(Math.sin(a + 0.7) * 2.0);
                int s2x = ix + (int) Math.round(Math.cos(a - 0.7) * 2.0);
                int s2z = iz + (int) Math.round(Math.sin(a - 0.7) * 2.0);
                c.spawn(s1x, iy, s1z);
                c.spawn(s2x, iy, s2z);
            }

            // Mid-tier connecting clouds/islands between main spokes
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * (i + 0.5)) / 6.0;
                int mx = (int) Math.round(Math.cos(a) * (r * 0.62));
                int mz = (int) Math.round(Math.sin(a) * (r * 0.62));
                c.taperedIsland(mx, 98, mz, 4, 2, Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.CALCITE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. Sky Temple (Temple-inspired tiered sandstone & prismarine ruins)
    // -------------------------------------------------------------------------
    private static final class SkyTempleMap implements RocketArenaMap {
        @Override public String id() { return "sky_temple"; }
        @Override public String displayName() { return "Sky Temple"; }
        @Override public boolean supportsDynamicPads() { return true; }

        @Override
        public void generate(RsCanvas c, int scaleRadius) {
            int r = Math.max(24, Math.min(34, scaleRadius));

            // Central stepped sandstone ziggurat courtyard
            c.taperedIsland(0, 82, 0, 12, 6, Material.SANDSTONE, Material.CUT_SANDSTONE, Material.STONE);
            c.ring(0, 83, 0, 9.5, 11.5, Material.CHISELED_SANDSTONE);

            // Elevated inner shrine canopy (y = 97)
            c.disc(0, 97, 0, 6.2, Material.PRISMARINE_BRICKS);
            c.dome(0, 98, 0, 5, Material.DARK_PRISMARINE);
            c.set(0, 103, 0, Material.SEA_LANTERN);
            for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                c.pillar(p[0], 83, 97, p[1], 0.8, Material.SMOOTH_SANDSTONE);
            }

            // 4 cardinal sanctuary pavilions with domed roofs & aqueduct bridges
            int[][] cardinals = {{r, 0}, {-r, 0}, {0, r}, {0, -r}};
            for (int i = 0; i < cardinals.length; i++) {
                int cx = cardinals[i][0];
                int cz = cardinals[i][1];
                c.taperedIsland(cx, 91, cz, 7, 5, Material.SMOOTH_SANDSTONE, Material.SANDSTONE, Material.TERRACOTTA);
                c.ring(cx, 91, cz, 5.5, 7.0, Material.PRISMARINE_BRICKS);
                // 4 pavilion columns + open dome canopy
                for (int[] off : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
                    c.pillar(cx + off[0], 92, 97, cz + off[1], 0.6, Material.SANDSTONE);
                }
                c.dome(cx, 97, cz, 5, Material.PRISMARINE);
                c.bridge(cx, 91, cz, 0, 83, 0, 1, Material.CUT_SANDSTONE);

                // Guaranteed solid spawns inside each pavilion
                c.spawn(cx - 2, 91, cz);
                c.spawn(cx + 2, 91, cz);
                c.spawn(cx, 91, cz - 2);
            }

            // 4 diagonal floating ruin rings connecting the outer pavilions
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI / 4.0) + i * (Math.PI / 2.0);
                int rx = (int) Math.round(Math.cos(a) * (r * 0.78));
                int rz = (int) Math.round(Math.sin(a) * (r * 0.78));
                c.taperedIsland(rx, 88, rz, 5, 3, Material.PRISMARINE_BRICKS, Material.SANDSTONE, Material.STONE);
                c.set(rx, 89, rz, Material.SEA_LANTERN);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. Santorini Cliffs (Mediterranean whitewashed quartz & blue domes)
    // -------------------------------------------------------------------------
    private static final class SantoriniCliffsMap implements RocketArenaMap {
        @Override public String id() { return "santorini_cliffs"; }
        @Override public String displayName() { return "Santorini Cliffs"; }
        @Override public boolean supportsDynamicPads() { return true; }

        @Override
        public void generate(RsCanvas c, int scaleRadius) {
            int r = Math.max(24, Math.min(34, scaleRadius));

            // Central sunken Mediterranean plaza + bell-tower arch
            c.taperedIsland(0, 84, 0, 10, 5, Material.QUARTZ_BRICKS, Material.CALCITE, Material.COBBLESTONE);
            c.disc(0, 84, 0, 4.5, Material.LIGHT_BLUE_TERRACOTTA);
            c.arch(-9, 0, 9, 0, 85, 11, 1, Material.WHITE_CONCRETE);
            c.arch(0, -9, 0, 9, 85, 11, 1, Material.WHITE_CONCRETE);

            // Outer cliff promenade ring (stepped multi-height islands with iconic blue domes)
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                int ix = (int) Math.round(Math.cos(a) * (r - 1));
                int iz = (int) Math.round(Math.sin(a) * (r - 1));
                int iy = (i % 2 == 0) ? 94 : 90;

                c.taperedIsland(ix, iy, iz, 7, 5, Material.CALCITE, Material.WHITE_TERRACOTTA, Material.STONE);
                c.disc(ix, iy, iz, 3.5, Material.SMOOTH_QUARTZ);

                // Whitewashed villa walls + iconic Santorini blue dome offset to outer edge
                int vx = ix + (int) Math.round(Math.cos(a) * 2.5);
                int vz = iz + (int) Math.round(Math.sin(a) * 2.5);
                c.ring(vx, iy + 1, vz, 2.2, 3.2, Material.WHITE_CONCRETE);
                c.ring(vx, iy + 2, vz, 2.2, 3.2, Material.WHITE_CONCRETE);
                c.dome(vx, iy + 3, vz, 3, (i % 2 == 0) ? Material.BLUE_CONCRETE : Material.LIGHT_BLUE_CONCRETE);

                c.bridge(ix, iy, iz, 0, 84, 0, 1, Material.QUARTZ_SLAB);

                // 2 guaranteed solid spawns on the open terrace side of each cliff
                int sx = ix - (int) Math.round(Math.cos(a) * 2.5);
                int sz = iz - (int) Math.round(Math.sin(a) * 2.5);
                c.spawn(sx, iy, sz);
                c.spawn(ix, iy, iz);
            }

            // High coastal observation ring
            c.ring(0, 102, 0, r * 0.45, r * 0.56, Material.SMOOTH_QUARTZ);
        }
    }

    // -------------------------------------------------------------------------
    // 4. Overlode Core (Futuristic dark-deepslate sky-foundry & neon conduits)
    // -------------------------------------------------------------------------
    private static final class OverlodeCoreMap implements RocketArenaMap {
        @Override public String id() { return "overlode_core"; }
        @Override public String displayName() { return "Overlode Core"; }
        @Override public boolean supportsDynamicPads() { return true; }

        @Override
        public void generate(RsCanvas c, int scaleRadius) {
            int r = Math.max(24, Math.min(34, scaleRadius));

            // Central dark-slate reactor deck
            c.taperedIsland(0, 84, 0, 10, 5, Material.DEEPSLATE_TILES, Material.BLACK_CONCRETE, Material.DEEPSLATE);
            c.ring(0, 84, 0, 7.5, 9.5, Material.CYAN_CONCRETE);
            c.pillar(0, 85, 105, 0, 1.5, Material.MAGENTA_STAINED_GLASS);
            c.disc(0, 105, 0, 6.0, Material.POLISHED_BLACKSTONE);
            c.ring(0, 105, 0, 4.5, 6.0, Material.MAGENTA_CONCRETE);

            Material[] trims = {
                    Material.CYAN_CONCRETE, Material.MAGENTA_CONCRETE,
                    Material.LIME_CONCRETE, Material.YELLOW_CONCRETE
            };

            // 8 foundry pylon platforms around perimeter + glowing glass gantries
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8.0;
                int px = (int) Math.round(Math.cos(a) * r);
                int pz = (int) Math.round(Math.sin(a) * r);
                int py = 90 + (i % 3) * 4;
                Material trim = trims[i % trims.length];

                c.taperedIsland(px, py, pz, 5, 4, Material.POLISHED_DEEPSLATE, Material.GRAY_CONCRETE, Material.DEEPSLATE);
                c.ring(px, py, pz, 4.0, 5.2, trim);
                c.bridge(px, py, pz, 0, 84, 0, 1, Material.CYAN_STAINED_GLASS);
                c.set(px, py + 1, pz, Material.SEA_LANTERN);

                // Guaranteed solid spawn on the platform next to the center trim
                int sx = px - (int) Math.round(Math.cos(a) * 1.5);
                int sz = pz - (int) Math.round(Math.sin(a) * 1.5);
                c.spawn(sx, py, sz);
                if (i < 4) {
                    c.spawn(px + (int) Math.round(Math.sin(a) * 2.0), py, pz - (int) Math.round(Math.cos(a) * 2.0));
                }
            }

            // Suspended outer octagonal energy ring
            c.ring(0, 96, 0, r * 0.56, r * 0.66, Material.BLACK_STAINED_GLASS);
        }
    }
}
