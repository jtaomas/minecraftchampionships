package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;
import com.mcm.games.skyblockle.MapModel.IslandType;

/**
 * The centre castle (our own design): a round stone-brick drum ~49 across that is a sunken bowl inside.
 * <ul>
 *   <li>flat arena floor, radius 12 (25 across), surface block at y = 0, with our own sun-and-rays inlay and a
 *       moss ring at its edge;</li>
 *   <li>5 seating tiers (1 high, 1 deep; red and yellow seat rows) up to a walkway ring at y = 6;</li>
 *   <li>a 2-thick wall up to y = 16 with 8 arched entrances at walkway level, upper stained-glass windows,
 *       merlons, a walkable wall top and 4 inner ladders;</li>
 *   <li>4 square cardinal towers with the castle chests, a gate to an outside balcony, a stair aisle from the
 *       floor straight into each tower, and our own stepped red/yellow spire caps (no crowns);</li>
 *   <li>an outer apron ring at y = 6 where bridges land, and a tapered hanging rock underside to y ~ -24.</li>
 * </ul>
 * Everything is exactly 4-fold symmetric (features are computed in a canonical quadrant).
 */
final class Castle {
    private Castle() {}

    static final double FLOOR_R = 12.5, TIER_END = 17.5, WALK_END = 20.5, WALL_END = 22.5, APRON_END = 24.5;
    static final int RIM_Y = 6, WALL_TOP = 16;

    /** Rotate (x,z) into the canonical quadrant x > 0, z >= 0. Returns {x', z'} (0,0 stays). */
    static int[] canon(int x, int z) {
        for (int q = 0; q < 4; q++) {
            int a = Bs.rx(x, z, q), b = Bs.rz(x, z, q);
            if (a > 0 && b >= 0) return new int[]{a, b};
        }
        return new int[]{0, 0};
    }

    static boolean inTowerFoot(int x, int z) {
        for (int q = 0; q < 4; q++) {
            int a = Bs.rx(x, z, q), b = Bs.rz(x, z, q);
            if (Math.abs(a) <= 3 && b >= 19 && b <= 25) return true;
            if (Math.abs(a) <= 2 && b >= 26 && b <= 27) return true;
        }
        return false;
    }

    /** Lowest body y at horizontal distance d (hanging, tapered underside). */
    static int bottom(double d, int cx, int cz, long seed) {
        double f = Math.max(0, 1 - (d / 25.5) * (d / 25.5));
        double n = Noise.value2(cx * 0.35, cz * 0.35, seed + 61);
        int b = RIM_Y - 3 - (int) Math.round(30 * Math.pow(f, 0.75) + (n - 0.5) * 4);
        if (Noise.hash01(cx, 0, cz, seed + 62) < 0.08) b -= 2 + (int) (Noise.hash01(cx, 1, cz, seed + 63) * 6);
        return Math.min(b, RIM_Y - 2);
    }

    /** Arc-length index along the circle for patterns (canonical coordinates). */
    private static int arcIdx(int cx, int cz, double d) {
        return (int) Math.floor(Math.atan2(cz, cx) * d);
    }

    private static double canonAngle(int cx, int cz) { return Math.toDegrees(Math.atan2(cz, cx)); }

    static void build(MapModel.Blocks out, long seed) {
        Noise.Pal masonry = new Noise.Pal("stone_bricks", 6, "mossy_stone_bricks", 1.6, "cracked_stone_bricks", 1.4);
        Noise.Pal body = new Noise.Pal("stone", 4, "andesite", 2, "cobblestone", 2, "tuff", 1);
        Noise.Pal deep = new Noise.Pal("deepslate", 3, "tuff", 2, "cobbled_deepslate", 2, "stone", 1);
        String[] glass = {"red_stained_glass_pane", "yellow_stained_glass_pane", "orange_stained_glass_pane"};
        int R = 28;
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                double d = Math.sqrt(x * x + z * z);
                boolean tower = inTowerFoot(x, z);
                if (d > APRON_END && !tower) continue;
                int[] c = canon(x, z);
                int cx = c[0], cz = c[1];
                double dd = Math.min(d, 25.5);
                int surf;
                if (d < FLOOR_R) surf = 0;
                else if (d < TIER_END) surf = (int) Math.floor(d - FLOOR_R) + 1;
                else surf = RIM_Y;
                if (tower) surf = RIM_Y;
                int bot = bottom(dd, cx, cz, seed);
                for (int y = bot; y <= surf; y++) {
                    String b;
                    boolean outer = d >= WALL_END - 0.5 || tower;
                    if (y == surf) b = surface(d, cx, cz, tower, seed);
                    else if (outer && y >= RIM_Y - 3) b = y == RIM_Y - 1 ? "polished_andesite" : masonry.at(cx, y, cz, seed + 64);
                    else if (d >= FLOOR_R && y > 0) b = masonry.at(cx, y, cz, seed + 65);
                    else if (y < -8) b = deep.at(cx, y, cz, seed + 66);
                    else b = body.at(cx, y, cz, seed + 67);
                    out.set(x, y, z, b);
                }
                // the wall ring
                if (!tower && d >= WALK_END && d < WALL_END) {
                    double ang = canonAngle(cx, cz);
                    double arcArch = Math.min(Math.abs(ang - 22.5), Math.abs(ang - 67.5)) * Math.PI / 180 * d;
                    double arcWin = Math.min(Math.abs(ang - 33.75), Math.abs(ang - 56.25)) * Math.PI / 180 * d;
                    boolean inner = d < WALK_END + 1.0;
                    int ai = arcIdx(cx, cz, d);
                    for (int y = RIM_Y + 1; y <= WALL_TOP; y++) {
                        String b;
                        if (y == RIM_Y + 1) b = "polished_andesite";
                        else if (y == 11) b = (ai & 1) == 0 ? "red_nether_bricks" : "chiseled_stone_bricks";
                        else if (y == WALL_TOP) b = inner ? "polished_andesite" : "stone_bricks";
                        else b = masonry.at(cx, y, cz, seed + 68);
                        boolean arch = arcArch <= 1.6 && y <= 9 || arcArch <= 0.6 && y == 10;
                        if (arch) b = null;
                        if (arcWin <= 0.55 && y >= 12 && y <= 14) b = inner ? null : glass[Math.floorMod(ai + y, 3)];
                        out.set(x, y, z, b);
                    }
                    if (arcArch <= 0.6 && inner) out.set(x, 10, z, "lantern[hanging=true,waterlogged=false]");
                    if (arcArch > 1.6 && arcArch <= 2.6) {   // arch jambs: chiseled
                        for (int y = RIM_Y + 2; y <= 9; y++) out.set(x, y, z, "chiseled_stone_bricks");
                    }
                    if (!inner) {
                        boolean merlon = (Math.floorMod(ai, 3) != 0);
                        if (merlon) out.set(x, WALL_TOP + 1, z, "stone_bricks");
                        if (merlon && Math.floorMod(ai, 9) == 4) out.set(x, WALL_TOP + 2, z, "lantern[hanging=false,waterlogged=false]");
                    }
                }
            }
        // buttresses on the apron every ~7 blocks of arc (skipping arches and towers)
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d < WALL_END || d >= WALL_END + 1.0 || inTowerFoot(x, z)) continue;
            int[] c = canon(x, z);
            double ang = canonAngle(c[0], c[1]);
            double arcArch = Math.min(Math.abs(ang - 22.5), Math.abs(ang - 67.5)) * Math.PI / 180 * d;
            double arcB = Math.min(Math.min(Math.abs(ang - 11.25), Math.abs(ang - 45)), Math.abs(ang - 78.75)) * Math.PI / 180 * d;
            if (arcB > 0.6 || arcArch < 3) continue;
            for (int y = RIM_Y + 1; y <= 13; y++) out.set(x, y, z, y == 11 ? "red_nether_bricks" : "polished_andesite");
            out.set(x, 14, z, "polished_andesite_slab[type=bottom,waterlogged=false]");
        }
        // inner ladders at 45 degrees (canonical (14,15) against wall (15,15)); 4-fold
        for (int q = 0; q < 4; q++) {
            int lx = Bs.rx(14, 15, q), lz = Bs.rz(14, 15, q);
            int wx = Bs.rx(15, 15, q), wz = Bs.rz(15, 15, q);
            String facing = Bs.rot("ladder[facing=west,waterlogged=false]", q);
            for (int y = RIM_Y + 1; y <= WALL_TOP; y++) {
                out.set(lx, y, lz, facing);
                if (out.get(wx, y, wz) == null || !Bs.fullSolid(out.get(wx, y, wz))) out.set(wx, y, wz, "stone_bricks");
            }
            out.set(lx, WALL_TOP + 1, lz, null);
        }
        // seat-row lanterns on the walkway edge at the arch angles: small posts
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d < TIER_END || d >= TIER_END + 1) continue;
            int[] c = canon(x, z);
            double ang = canonAngle(c[0], c[1]);
            if (Math.abs(ang - 45) * Math.PI / 180 * d <= 0.5) {
                out.set(x, RIM_Y + 1, z, "stone_brick_wall");
                out.set(x, RIM_Y + 2, z, "lantern[hanging=false,waterlogged=false]");
            }
        }
    }

    private static String surface(double d, int cx, int cz, boolean tower, long seed) {
        if (tower) return "polished_andesite";
        if (d < FLOOR_R) {
            if (d < 1.5) return "red_terracotta";
            if (d < 2.6) return "yellow_terracotta";
            if (d >= 11.5) return "moss_block";
            if (d >= 6.5 && d < 7.5) return "chiseled_stone_bricks";
            boolean ray = cz == 0 || cx == cz;
            if (ray && d >= 3) return d < 6.5 ? "yellow_terracotta" : "smooth_stone";
            return Noise.hash01(cx, 0, cz, seed + 69) < 0.15 ? "andesite" : "polished_andesite";
        }
        if (d < TIER_END) {
            int k = (int) Math.floor(d - FLOOR_R) + 1;
            switch (k) {
                case 1: return "mossy_stone_bricks";
                case 2: return "red_terracotta";
                case 3: return "stone_bricks";
                case 4: return "yellow_terracotta";
                default: return "polished_andesite";
            }
        }
        if (d < WALK_END) return d < TIER_END + 1 ? "stone_bricks" : (Noise.hash01(cx, 6, cz, seed + 70) < 0.2 ? "andesite" : "polished_andesite");
        if (d < WALL_END) return "stone_bricks";
        return Noise.hash01(cx, 6, cz, seed + 71) < 0.25 ? "mossy_stone_bricks" : "stone_bricks";
    }

    /** One tower + its stair aisle and balcony, built for the +z (south) side in castle coordinates. */
    static Tpl tower(long seed) {
        Tpl t = new Tpl("tower");
        final int TT = 23;
        Noise.Pal masonry = new Noise.Pal("stone_bricks", 6, "mossy_stone_bricks", 1.2, "cracked_stone_bricks", 1.2);
        int z0 = 19, z1 = 25;
        for (int x = -3; x <= 3; x++)
            for (int z = z0; z <= z1; z++) {
                boolean wall = Math.abs(x) == 3 || z == z0 || z == z1;
                boolean quoin = Math.abs(x) == 3 && (z == z0 || z == z1);
                t.set(x, RIM_Y, z, "polished_andesite");
                for (int y = RIM_Y + 1; y <= TT; y++) {
                    String b = null;
                    if (wall) {
                        b = quoin ? "polished_andesite" : masonry.at(x, y, z, seed + 72);
                        if (y == 11) b = quoin ? "polished_andesite" : ((x + z) & 1) == 0 ? "red_nether_bricks" : "chiseled_stone_bricks";
                    }
                    if (y == 11 && !wall) b = (x == 2 && z == 22) ? null : "spruce_planks";
                    if (y == 17 && !wall) b = "spruce_planks";
                    if (y == TT) b = "stone_bricks";
                    if (wall && y == TT - 1 && !quoin) b = "chiseled_stone_bricks";
                    t.set(x, y, z, b);
                }
            }
        // doors (inner from the walkway / aisle, outer gate to the balcony)
        for (int x = -1; x <= 1; x++) for (int y = RIM_Y + 1; y <= RIM_Y + 3; y++) { t.set(x, y, z0, null); t.set(x, y, z1, null); }
        t.set(0, RIM_Y + 4, z0, "chiseled_stone_bricks");
        t.set(0, RIM_Y + 4, z1, "chiseled_stone_bricks");
        // slit windows in the upper room
        for (int y = 13; y <= 14; y++) {
            t.set(0, y, z1, "yellow_stained_glass_pane");
            t.set(3, y, 22, "yellow_stained_glass_pane");
            t.set(-3, y, 22, "yellow_stained_glass_pane");
        }
        for (int y = 19; y <= 20; y++) {
            t.set(0, y, z1, "red_stained_glass_pane");
            t.set(3, y, 22, "red_stained_glass_pane");
            t.set(-3, y, 22, "red_stained_glass_pane");
            t.set(0, y, z0, "red_stained_glass_pane");
        }
        // roof corbel + parapet + stepped spire (our own cap: square red/yellow spire, no crown)
        int R0 = TT, P = TT + 1;
        for (int x = -4; x <= 4; x++) for (int z = z0 - 1; z <= z1 + 1; z++) {
            boolean edge = Math.abs(x) == 4 || z == z0 - 1 || z == z1 + 1;
            if (!edge) continue;
            if (Math.abs(x) == 4 && (z == z0 - 1 || z == z1 + 1)) {
                for (int y = R0 - 1; y <= P; y++) t.set(x, y, z, "polished_andesite");
                t.set(x, P + 1, z, "stone_brick_wall");
                t.set(x, P + 2, z, "lantern[hanging=false,waterlogged=false]");
                continue;
            }
            String f = Math.abs(x) == 4 ? (x < 0 ? "east" : "west") : (z < 22 ? "south" : "north");
            t.set(x, R0, z, Bs.stairs("stone_brick", f, true));
            if (((x + z) & 1) == 0) t.set(x, P, z, "stone_bricks");
        }
        for (int x = -2; x <= 2; x++) for (int z = 20; z <= 24; z++) {
            boolean rim = Math.abs(x) == 2 || z == 20 || z == 24;
            t.set(x, P, z, "red_terracotta");
            t.set(x, P + 1, z, rim ? "yellow_terracotta" : "red_terracotta");
            t.set(x, P + 2, z, rim ? "red_terracotta" : "red_terracotta");
        }
        for (int x = -1; x <= 1; x++) for (int z = 21; z <= 23; z++) {
            t.set(x, P + 3, z, "red_terracotta");
            t.set(x, P + 4, z, (Math.abs(x) == 1 || z != 22) ? "yellow_terracotta" : "red_terracotta");
            t.set(x, P + 5, z, "red_concrete");
        }
        t.set(0, P + 6, 22, "red_concrete");
        t.set(0, P + 7, 22, "yellow_concrete");
        t.set(0, P + 8, 22, "yellow_concrete");
        t.set(0, P + 9, 22, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        // banners on the outer face, either side of the gate
        t.set(-2, 15, z1 + 1, "red_wall_banner[facing=south]");
        t.set(2, 15, z1 + 1, "red_wall_banner[facing=south]");
        // ladder to the upper room, lantern, chest
        for (int y = RIM_Y + 1; y <= 12; y++) t.set(2, y, 22, "ladder[facing=west,waterlogged=false]");
        t.climbs.add(new int[]{2, RIM_Y + 1, 22, 12});
        t.set(0, 10, 22, "lantern[hanging=true,waterlogged=false]");
        t.chest(-2, RIM_Y + 1, 22, "east", IslandType.CASTLE);
        // balcony outside the gate
        for (int x = -2; x <= 2; x++) for (int z = z1 + 1; z <= z1 + 2; z++) {
            t.set(x, RIM_Y, z, Math.abs(x) == 2 ? "polished_andesite" : "stone_bricks");
            if (Math.abs(x) == 2) t.set(x, RIM_Y + 1, z, "stone_brick_wall");
        }
        // stair aisle from the arena floor straight into the tower door (staircase helper)
        for (int x = -1; x <= 1; x++) Stairs.flight(t, "aisle", "stone_brick", "stone_bricks", x, 13, 0, 0, 1, 6);
        for (int z = 13; z <= 18; z++) for (int sx = -1; sx <= 1; sx += 2) t.set(2 * sx, Math.min(6, z - 12), z, "chiseled_stone_bricks");
        return t;
    }
}
