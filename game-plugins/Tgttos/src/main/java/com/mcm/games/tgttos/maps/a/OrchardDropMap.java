package com.mcm.games.tgttos.maps.a;

import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Design;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Noise;
import com.mcm.games.tgttos.maps.a.util.Noise.Pal;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.List;

/**
 * Orchard Drop (drop map, snowballs only). Players start on the gallery of a white windmill 60 blocks above a
 * floating orchard. Below hang giant cut apples (flat tops, pools of water in the core) over the void:
 * two big, near apples on the left (safe line: drop, walk across, drop, then a long walk to the yard gate) or two
 * small, far apples on the right (fast line: 5-wide targets, 9-12 block reach), which put you down right in front
 * of the cider yard gate (4 wide, the choke; the way round through the side gap is longer). The pen sits in the
 * cider yard under the cider-house tower with a giant apple on top (landmark).
 *
 * <p>Local frame: x across, z along the run. Gallery deck top y 59 (feet 60); meadow and yard top y 0 (feet 1).
 * Palette: apple red / leaf green / oak and white plaster.
 */
public final class OrchardDropMap extends AMap {

    @Override public String id() { return "orchard_drop"; }
    @Override public String displayName() { return "Orchard Drop"; }
    @Override public MapType type() { return MapType.DROP; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }

    private Gen g;
    private final Pal red = new Pal("red_terracotta", 45, "red_wool", 30, "red_nether_bricks", 10, "red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", 15);
    private final Pal green = new Pal("lime_terracotta", 45, "green_terracotta", 25, "moss_block", 20, "lime_wool", 10);
    private final Pal flesh = new Pal("white_terracotta", 50, "smooth_sandstone", 30, "birch_planks", 20);
    private final Pal plaster = new Pal("white_terracotta", 55, "calcite", 35, "diorite", 10);
    private final Pal rock = new Pal("stone", 35, "andesite", 25, "dirt", 20, "coarse_dirt", 10, "tuff", 10);
    private final Pal leaves = new Pal("oak_leaves[distance=1,persistent=true,waterlogged=false]", 70, "azalea_leaves[distance=1,persistent=true,waterlogged=false]", 22, "red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", 8);
    private final Pal bark = new Pal("oak_log[axis=y]", 60, "dark_oak_log[axis=y]", 40);
    private final Pal grass = new Pal("grass_block[snowy=false]", 75, "moss_block", 15, "coarse_dirt", 10);

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;
        windmill(d);
        // ---- the targets: big near apples (safe, left), small far apples (fast, right)
        apple(-11.5, 18.5, 6.5, 42, 2.2, false, true);
        apple(-21.5, 30.5, 6.0, 27, 2.0, true, true);
        apple(9.5, 21.5, 2.6, 24, 0.9, false, false);
        apple(8.5, 33.5, 2.6, 10, 0.9, true, false);
        meadowAndYard(d);
        ciderTower(de);
        scenery();
        List<Region> keep = List.of(d.pen, Region.of(6, 0, 47, 9, 4, 53));
        g.breakRuns(-30, 30, 34, 98, 1, 8, keep, "barrel[facing=up,open=false]", "hay_block[axis=y]", "composter[level=6]",
                "red_wool", "lime_terracotta", "oak_log[axis=y]");

        d.voidY = -10;
        d.playBounds = Region.of(-30, -10, -6, 30, 66, 92);
        d.overviewPoints.add(new Spawn(38, 52, -6, 40f, 30f));
        d.overviewPoints.add(new Spawn(0.5, 64, 4, 0f, 45f));
        d.notes.put("fastLine", "right: small far apples (5 wide) at y 24 and y 10, lands at the yard gate");
        d.notes.put("safeLine", "left: big near apples at y 42 and y 27, then the long walk across the meadow");

        de.searchBox = Region.of(-30, -12, -6, 30, 64, 94);
        de.lane.add(Region.of(-29, -40, -1, 29, 63, 90));

        Region startBox = Region.of(-19, 59, -1, 18, 63, 10);
        Design.Line fast = de.line("FAST", false, true);
        fast.description = "walk off the right side, 36-block fall onto a 5-wide apple 10 out, 14-block fall onto the second small apple, hop to the meadow in front of the 4-wide yard gate";
        fast.leg("small apple 1", Region.of(6, 24, 18, 13, 25, 25), startBox, Region.of(2, 23, -1, 18, 63, 26))
            .leg("small apple 2", Region.of(5, 10, 30, 12, 11, 37), Region.of(2, 9, 17, 16, 30, 38))
            .leg("pen", d.pen, Region.of(-29, 0, 36, 29, 12, 92), Region.of(4, 9, 28, 14, 12, 38));
        Design.Line safe = de.line("SAFE", false, false);
        safe.maxGap = 2;
        safe.description = "walk off the left side onto the big apple below, cross it, drop to the second big apple, drop to the meadow, walk across to the gate (or round by the side gap)";
        safe.leg("big apple 1", Region.of(-18, 42, 12, -5, 43, 25), startBox, Region.of(-19, 40, -1, 0, 63, 26))
            .leg("big apple 2", Region.of(-28, 27, 24, -15, 28, 37), Region.of(-29, 26, 10, 0, 44, 38))
            .leg("pen", d.pen, Region.of(-29, 0, 36, 29, 12, 92), Region.of(-29, 26, 24, -14, 28, 38));
    }

    // ------------------------------------------------------------------ windmill and start gallery

    private void windmill(TgMapData d) {
        double cx = 0.5, cz = -13.5;
        // island under the mill
        for (int x = -14; x <= 15; x++) for (int z = -28; z <= 1; z++) {
            double dd = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (dd <= 13.5 + 2 * Noise.fbm2(x * 0.2, z * 0.2, g.seed + 5)) g.set(x, 0, z, grass.at(x, 0, z, g.seed));
        }
        g.underside(-14, -28, 15, 1, 0, 22, rock, 11);
        // tapering tower (plaster with oak timber bands and windows)
        for (int y = 1; y <= 66; y++) {
            double r = 10.5 - 3.5 * y / 66.0;
            boolean band = y % 12 == 0;
            for (int x = (int) Math.floor(cx - r) - 1; x <= (int) Math.ceil(cx + r) + 1; x++)
                for (int z = (int) Math.floor(cz - r) - 1; z <= (int) Math.ceil(cz + r) + 1; z++) {
                    double dd = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                    if (dd > r || dd <= r - 1.6) continue;
                    double ang = Math.atan2(z + 0.5 - cz, x + 0.5 - cx);
                    boolean window = y % 12 >= 4 && y % 12 <= 6 && Math.floorMod((int) Math.round(ang * 4 / Math.PI), 2) == 0
                            && Math.abs(Math.sin(ang * 4)) < 0.25;
                    String s = band ? "stripped_oak_log[axis=y]" : window ? "glass_pane" : plaster.at(x, y, z, g.seed);
                    if (Math.floorMod((int) Math.round(ang * 16 / Math.PI), 4) == 0 && !window) s = "stripped_dark_oak_log[axis=y]";
                    g.set(x, y, z, s);
                }
        }
        // cap: a dark onion-ish dome with a finial
        for (int y = 67; y <= 78; y++) {
            double r = 7.6 * Math.sqrt(Math.max(0, 1 - Math.pow((y - 66.5) / 12.0, 2)));
            g.cyl(cx, cz, r, y, y, new Pal("dark_oak_planks", 60, "spruce_planks", 25, "dark_oak_log[axis=y]", 15));
        }
        for (int y = 79; y <= 83; y++) g.set(0, y, -14, y == 83 ? "gold_block" : "chain[axis=y]");
        // sails on the back face (z = -24): four lattice arms with cloth
        double sy = 70;
        for (int a = 0; a < 4; a++) {
            double ang = Math.toRadians(25 + a * 90);
            double ux = Math.cos(ang), uy = Math.sin(ang), vx = -uy, vy = ux;
            for (double t = 2; t <= 25; t += 0.5)
                for (double w = 0; w <= 5; w += 0.5) {
                    int x = (int) Math.floor(cx + ux * t + vx * w), y = (int) Math.floor(sy + uy * t + vy * w);
                    String s = w == 0 ? "stripped_oak_log[axis=y]" : ((int) t % 5 == 0 || w >= 5) ? "spruce_planks" : "white_wool";
                    if (t < 5 && w > 0) continue;
                    g.set(x, y, -25, s);
                }
        }
        g.ellipsoid(cx, sy, -24.5, 2.4, 2.4, 1.2, new Pal("dark_oak_planks", 1));
        for (int z = -26; z <= -20; z++) g.set(0, (int) sy, z, "dark_oak_log[axis=z]");
        // mill house annex behind the gallery (closes the back of the start strip)
        for (int x = -18; x <= 17; x++) for (int z = -6; z <= -1; z++) for (int y = 55; y <= 67; y++) {
            boolean face = z == -1;
            String s;
            if (y == 67 || y == 55) s = "stripped_oak_log[axis=x]";
            else if (face && (Math.floorMod(x, 6) == 0)) s = "stripped_dark_oak_log[axis=y]";
            else if (face && y >= 62 && y <= 64 && Math.floorMod(x, 6) >= 2 && Math.floorMod(x, 6) <= 4) s = "glass_pane";
            else s = plaster.at(x, y, z, g.seed);
            g.set(x, y, z, s);
        }
        for (int x = -19; x <= 18; x++) for (int k = 0; k <= 4; k++) g.set(x, 68 + k, -1 - k, Gen.st("dark_oak", "south", false));
        // gallery deck (start strip z 0..6, start line z 6, barrier z 7, lip to z 9)
        for (int x = -18; x <= 17; x++) for (int z = 0; z <= 9; z++) {
            String s = ((x + z) & 1) == 0 ? "spruce_planks" : "stripped_spruce_wood[axis=y]";
            if (z == 6) s = (x & 1) == 0 ? "red_terracotta" : "white_terracotta";
            if (z == 9) s = "stripped_oak_log[axis=x]";
            g.set(x, 59, z, s);
            g.set(x, 58, z, "spruce_slab[type=top,waterlogged=false]");
        }
        // brackets under the deck
        for (int x = -18; x <= 17; x += 5) for (int i = 0; i < 9; i++) {
            g.set(x, 57 - i, 8 - i, Gen.st("dark_oak", "north", true));
            for (int z = -1; z < 8 - i; z++) if (z >= 7 - i) g.set(x, 57 - i, z, "dark_oak_planks");
        }
        // side railings (fence + post), unjumpable
        for (int z = 0; z <= 9; z++) { g.set(-19, 59, z, "spruce_planks"); g.set(18, 59, z, "spruce_planks");
            g.set(-19, 60, z, "spruce_fence"); g.set(18, 60, z, "spruce_fence"); }
        for (int y = 60; y <= 63; y++) { g.set(-19, y, 9, "stripped_oak_log[axis=y]"); g.set(18, y, 9, "stripped_oak_log[axis=y]");
            g.set(-19, y, 0, "stripped_oak_log[axis=y]"); g.set(18, y, 0, "stripped_oak_log[axis=y]"); }
        g.set(-19, 64, 9, "lantern[hanging=false,waterlogged=false]");
        g.set(18, 64, 9, "lantern[hanging=false,waterlogged=false]");
        // flower boxes along the railing bottoms
        for (int z = 1; z <= 8; z += 2) { g.set(-20, 59, z, "red_terracotta"); g.set(-20, 60, z, "poppy"); g.set(19, 59, z, "red_terracotta"); g.set(19, 60, z, "poppy"); }
        // barrier (glass) and start strip
        for (int x = -18; x <= 17; x++) for (int y = 60; y <= 62; y++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new Pos(x, y, 7)); }
        d.startStrip = Region.of(-18, 60, 0, 17, 63, 6);
        for (int row = 0; row < 4; row++)
            for (int k = 0; k < 13; k++) d.startSpawns.add(new Spawn(-17.5 + k * 2.75 + (row % 2) * 0.6, 60, 0.6 + row * 1.6, 0f));
    }

    // ------------------------------------------------------------------ apples

    /** Giant apple cut flat at {@code top}: skin shell, flesh ring on top, a pool of water in the core, stem and leaf on the rim. */
    private void apple(double cx, double cz, double R, int top, double wr, boolean isGreen, boolean leafLeft) {
        Pal skin = isGreen ? green : red;
        double cy = top - R * 0.3, ry = R * 0.85;
        for (int x = (int) Math.floor(cx - R) - 1; x <= (int) Math.ceil(cx + R) + 1; x++)
            for (int z = (int) Math.floor(cz - R) - 1; z <= (int) Math.ceil(cz + R) + 1; z++)
                for (int y = (int) Math.floor(cy - ry) - 1; y <= top; y++) {
                    double a = (x + 0.5 - cx) / R, c = (y + 0.5 - cy) / ry, e = (z + 0.5 - cz) / R;
                    double q = a * a + c * c + e * e;
                    if (q > 1) continue;
                    double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                    String s;
                    if (y == top) {
                        double rim = R * Math.sqrt(Math.max(0, 1 - Math.pow((top + 0.5 - cy) / ry, 2)));
                        if (rr <= wr) s = "water";
                        else if (rr >= rim - 1.0) s = skin.at(x, y, z, g.seed);
                        else if (rr <= wr + 0.9 && R > 4) s = "brown_terracotta";      // the core around the pool
                        else s = flesh.at(x, y, z, g.seed);
                    } else if (q > 0.62) s = skin.at(x, y, z, g.seed);
                    else s = (y == top - 1) ? flesh.at(x, y, z, g.seed) : "air";
                    if (s.equals("air")) continue;
                    g.set(x, y, z, s);
                }
        // calyx at the bottom
        int by = (int) Math.floor(cy - ry);
        g.set((int) Math.floor(cx), by, (int) Math.floor(cz), "brown_terracotta");
        g.set((int) Math.floor(cx), by - 1, (int) Math.floor(cz), "dark_oak_log[axis=y]");
        // stem and leaf on the far rim (big apples only; small ones keep a clean 5-wide top)
        if (R > 4) {
            int sx = (int) Math.floor(cx + (leafLeft ? -1 : 1) * (R * 0.55)), sz = (int) Math.floor(cz + R * 0.5);
            for (int y = top + 1; y <= top + 4; y++) g.set(sx, y, sz, "dark_oak_log[axis=y]");
            g.set(sx + (leafLeft ? -1 : 1), top + 4, sz, "dark_oak_log[axis=x]");
            g.ellipsoid(sx + (leafLeft ? -3 : 3) + 0.5, top + 5, sz + 0.5, 2.5, 0.7, 1.5, new Pal("moss_block", 60, "lime_terracotta", 40));
        } else {
            int sz = (int) Math.floor(cz) + 2;
            g.set((int) Math.floor(cx), top, sz, "dark_oak_log[axis=y]");
        }
    }

    // ------------------------------------------------------------------ meadow, cider yard, pen

    private void meadowAndYard(TgMapData d) {
        // island top (y 0), rounded outline with a ragged edge, tapered rocky underside with roots
        for (int x = -29; x <= 29; x++) for (int z = 36; z <= 96; z++) {
            double ex = Math.max(0, Math.abs(x) - 24) / 5.5, ez = z < 44 ? (44 - z) / 8.0 : Math.max(0, z - 88) / 8.0;
            double e = ex * ex + ez * ez;
            double n = 0.35 * Noise.fbm2(x * 0.15, z * 0.15, g.seed + 21);
            if (e > 1 + n) continue;
            g.set(x, 0, z, grass.at(x, 0, z, g.seed));
        }
        g.underside(-29, 36, 29, 96, 0, 26, rock, 23);
        for (int i = 0; i < 18; i++) {            // hanging roots
            int x = -24 + (int) (g.rnd(i, 0, 0, 31) * 48), z = 40 + (int) (g.rnd(i, 0, 0, 32) * 50);
            int len = 3 + (int) (g.rnd(i, 0, 0, 33) * 6);
            int y = -1;
            while (y > -30 && !g.air(x, y, z)) y--;
            for (int k = 0; k < len; k++) g.set(x, y - k, z, k == len - 1 ? "hanging_roots" : "rooted_dirt");
        }
        // cider yard walls: barrel and crate stacks 3 high; front gate x 2..5 at z 50; side gap x 22, z 70..77
        for (int x = -22; x <= 22; x++) for (int z = 50; z <= 90; z++) {
            boolean wall = z == 50 || z == 90 || x == -22 || x == 22;
            if (!wall) continue;
            if (z == 50 && x >= 6 && x <= 9) continue;
            if (Math.abs(x) == 22 && z >= 70 && z <= 77) continue;
            for (int y = 1; y <= 3; y++) {
                double r = g.rnd(x, y, z, 41);
                String s = Math.floorMod(x + z, 7) == 0 ? "stripped_oak_log[axis=y]"
                        : r < 0.45 ? "barrel[facing=up,open=false]" : r < 0.7 ? "oak_planks" : r < 0.85 ? "hay_block[axis=y]" : "spruce_planks";
                g.set(x, y, z, s);
            }
            if (Math.floorMod(x + z, 7) == 0) { g.set(x, 4, z, "oak_fence"); g.set(x, 5, z, "lantern[hanging=false,waterlogged=false]"); }
            else g.set(x, 4, z, g.rnd(x, 4, z, 42) < 0.5 ? "red_wool" : "lime_terracotta");   // heaped apples on the stacks
        }
        // gate posts with an arch sign
        for (int y = 1; y <= 7; y++) { g.set(5, y, 50, "stripped_dark_oak_log[axis=y]"); g.set(10, y, 50, "stripped_dark_oak_log[axis=y]"); }
        for (int x = 5; x <= 10; x++) { g.set(x, 7, 50, "dark_oak_planks"); g.set(x, 8, 50, (x & 1) == 0 ? "red_wool" : "lime_wool"); }
        g.set(7, 6, 50, "chain[axis=y]"); g.set(8, 6, 50, "chain[axis=y]");
        g.set(7, 5, 50, "lantern[hanging=true,waterlogged=false]"); g.set(8, 5, 50, "lantern[hanging=true,waterlogged=false]");
        for (int y = 1; y <= 6; y++) { g.set(21, y, 69, "stripped_dark_oak_log[axis=y]"); g.set(21, y, 78, "stripped_dark_oak_log[axis=y]");
            g.set(-21, y, 69, "stripped_dark_oak_log[axis=y]"); g.set(-21, y, 78, "stripped_dark_oak_log[axis=y]"); }
        // hawthorn hedge splitting the meadow: the left (big-apple) landing has no way to the gate, only round the side
        for (int z = 30; z <= 49; z++) for (int x = -1; x <= 0; x++) if (!g.air(x, 0, z)) {
            g.set(x, 1, z, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
            g.set(x, 2, z, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
            g.set(x, 3, z, (z % 4 == 0) ? "flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]" : "azalea_leaves[distance=1,persistent=true,waterlogged=false]");
            if (z % 5 == 0 && x == 0) g.set(x, 4, z, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
        }
        // a hay-bale pair in the gate lane (no free sprint from the gate to the pen)
        g.set(6, 1, 57, "hay_block[axis=x]"); g.set(7, 1, 57, "hay_block[axis=x]"); g.set(8, 1, 54, "barrel[facing=up,open=false]"); g.set(9, 1, 54, "hay_block[axis=x]");
        // meadow dressing in front of the yard (apple crates, a cart, fallen apples)
        crate(-13, 44); crate(-8, 47); crate(12, 45); crate(17, 41);
        cart(-16, 39);
        fallenApple(9, 43, true); fallenApple(-8, 47, false); fallenApple(24, 58, true); fallenApple(-25, 64, false);
        // yard dressing: cider press, barrel racks, carts
        press(-12, 58);
        for (int z = 56; z <= 84; z += 5) { g.set(18, 1, z, "barrel[facing=east,open=false]"); g.set(19, 1, z, "barrel[facing=east,open=false]"); g.set(18, 2, z, "barrel[facing=east,open=false]"); }
        for (int z = 56; z <= 84; z += 6) { g.set(-19, 1, z, "barrel[facing=up,open=false]"); g.set(-18, 1, z + 1, "barrel[facing=up,open=false]"); g.set(-19, 2, z, "composter[level=8]"); }
        crate(-2, 58); crate(-6, 80); crate(-8, 68); crate(-17, 63); crate(15, 82); cart(-14, 70);
        // path of coarse dirt from the gate to the pen
        for (int z = 51; z <= 63; z++) for (int x = 6; x <= 9; x++) g.set(x, 0, z, g.rnd(x, 0, z, 9) < 0.6 ? "coarse_dirt" : "dirt_path");
        // pen: low ring of logs and planks with flower pots, interior 11 x 11
        int x1 = 2, x2 = 14, z1 = 64, z2 = 76;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (edge) {
                g.set(x, 1, z, ((x + z) & 1) == 0 ? "stripped_oak_log[axis=y]" : "oak_planks");
                g.set(x, 2, z, (x + z) % 3 == 0 ? "potted_red_tulip" : "air");
            } else {
                g.set(x, 0, z, g.rnd(x, 0, z, 3) < 0.7 ? "moss_block" : "grass_block[snowy=false]");
                g.set(x, 1, z, "air");
                if (g.rnd(x, 1, z, 4) < 0.07) g.set(x, 1, z, "short_grass");
            }
        }
        g.set(4, 1, 74, "hay_block[axis=y]");
        g.set(12, 1, 66, "composter[level=4]");
        d.pen = Region.of(x1 + 1, 1, z1 + 1, x2 - 1, 3, z2 - 1);
        for (int i = 0; i < 10; i++) d.chickenSpawns.add(new Spawn(4.5 + (i % 5) * 1.8, 1, 67.5 + (i / 5) * 5, 180f));
    }

    private void crate(int x, int z) {
        g.fill(x, 1, z, x + 1, 1, z + 1, "stripped_oak_wood[axis=y]");
        g.set(x, 2, z, "red_wool"); g.set(x + 1, 2, z + 1, "lime_wool");
    }

    private void cart(int x, int z) {
        g.fill(x, 2, z, x + 2, 2, z + 4, "spruce_planks");
        for (int zz = z; zz <= z + 4; zz++) { g.set(x - 1, 3, zz, "spruce_fence"); g.set(x + 3, 3, zz, "spruce_fence"); }
        g.set(x - 1, 1, z + 1, "dark_oak_log[axis=x]"); g.set(x + 3, 1, z + 1, "dark_oak_log[axis=x]");
        g.set(x - 1, 1, z + 3, "dark_oak_log[axis=x]"); g.set(x + 3, 1, z + 3, "dark_oak_log[axis=x]");
        g.fill(x, 3, z + 1, x + 2, 3, z + 3, "red_wool");
        g.set(x + 1, 4, z + 2, "lime_wool");
        g.set(x + 1, 1, z + 2, "dark_oak_fence");
    }

    private void fallenApple(int x, int z, boolean isGreen) {
        g.ellipsoid(x + 0.5, 1.6, z + 0.5, 1.6, 1.3, 1.6, isGreen ? green : red);
        g.set(x, 3, z, "dark_oak_log[axis=y]");
    }

    private void press(int x, int z) {
        g.fill(x, 1, z, x + 4, 1, z + 4, "stripped_spruce_wood[axis=y]");
        g.fill(x + 1, 2, z + 1, x + 3, 3, z + 3, "barrel[facing=up,open=false]");
        for (int y = 2; y <= 7; y++) { g.set(x, y, z, "spruce_log[axis=y]"); g.set(x + 4, y, z, "spruce_log[axis=y]"); }
        for (int xx = x; xx <= x + 4; xx++) g.set(xx, 8, z, "spruce_log[axis=x]");
        for (int y = 4; y <= 7; y++) g.set(x + 2, y, z + 2, y == 4 ? "piston[extended=false,facing=down]" : "chain[axis=y]");
        g.set(x + 2, 8, z + 2, "spruce_planks");
        for (int zz = z; zz <= z + 2; zz++) g.set(x + 2, 8, zz, "spruce_log[axis=z]");
    }

    // ------------------------------------------------------------------ cider-house tower (landmark)

    private void ciderTower(Design de) {
        int x1 = 3, x2 = 13, z1 = 80, z2 = 88;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = 1; y <= 30; y++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (!edge) continue;
            boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
            String s;
            if (y <= 8) s = corner ? "stone_bricks" : (g.rnd(x, y, z, 5) < 0.3 ? "cobblestone" : "stone_bricks");
            else if (corner || y % 7 == 2) s = "stripped_dark_oak_log[axis=y]";
            else if (z == z1 && y % 7 >= 4 && y % 7 <= 5 && Math.floorMod(x - 4, 3) == 0) s = "glass_pane";
            else s = plaster.at(x, y, z, g.seed);
            g.set(x, y, z, s);
        }
        g.set(8, 1, 80, "spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        g.set(8, 2, 80, "spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
        // red roof pyramid
        for (int k = 0; k <= 6; k++) for (int x = x1 - 1 + k; x <= x2 + 1 - k; x++) for (int z = z1 - 1 + k; z <= z2 + 1 - k; z++) {
            boolean edge = x == x1 - 1 + k || x == x2 + 1 - k || z == z1 - 1 + k || z == z2 + 1 - k;
            if (edge || k == 6) g.set(x, 31 + k, z, (k & 1) == 0 ? "red_nether_bricks" : "red_terracotta");
        }
        for (int y = 38; y <= 41; y++) g.set(8, y, 84, "dark_oak_log[axis=y]");
        // the giant apple on the spike, with stem and leaf
        g.ellipsoid(8.5, 46.5, 84.5, 5.2, 4.8, 5.2, red);
        for (int y = 51; y <= 54; y++) g.set(8, y, 84, "dark_oak_log[axis=y]");
        g.ellipsoid(10.5, 54.5, 84.5, 2.6, 0.7, 1.4, new Pal("moss_block", 60, "lime_terracotta", 40));
        
        de.landmark.add(new Pos(8, 46, 80));
        de.landmark.add(new Pos(8, 50, 82));
        de.landmark.add(new Pos(8, 53, 84));
        de.landmark.add(new Pos(8, 54, 84));
        // bunting from the tower to the gate posts
        for (int i = 0; i <= 30; i++) {
            double t = i / 30.0;
            int x = 5, y = (int) Math.round(29 + (9 - 29) * t - 6 * Math.sin(Math.PI * t)), z = (int) Math.round(80 + (50 - 80) * t);
            g.soft(x, y, z, (i & 1) == 0 ? "red_wool" : "lime_wool");
        }
    }

    // ------------------------------------------------------------------ scenery: orchard islands, trees, birds

    private void scenery() {
        tree(-44, 14, 6, 24, 11, 51);
        tree(43, 26, 10, 22, 10, 52);
        tree(-46, 58, -2, 26, 12, 53);
        tree(45, 70, 2, 24, 11, 54);
        tree(-20, 106, -4, 28, 12, 55);
        tree(24, 110, 0, 26, 11, 56);
        tree(-40, 96, -8, 20, 9, 57);
        // a few small floating apples off the course (red herrings, out of reach)
        apple(-36.5, 38.5, 3, 30, 1.0, false, true);
        apple(34.5, 48.5, 3, 20, 1.0, true, false);
    }

    private void tree(int cx, int cz, int baseY, int h, int crownR, long salt) {
        // island
        int ir = crownR - 1;
        for (int x = cx - ir; x <= cx + ir; x++) for (int z = cz - ir; z <= cz + ir; z++) {
            double dd = Math.hypot(x - cx, z - cz) / ir;
            if (dd <= 1 + 0.2 * Noise.fbm2(x * 0.2, z * 0.2, g.seed + salt)) g.set(x, baseY, z, grass.at(x, baseY, z, g.seed));
        }
        g.underside(cx - ir, cz - ir, cx + ir, cz + ir, baseY, ir + 4, rock, salt);
        // trunk with a slight lean, two branch forks
        g.tube(cx + 0.5, baseY + 1, cz + 0.5, cx + 1.5, baseY + h * 0.6, cz + 0.5, 1.6, bark);
        g.tube(cx + 1.5, baseY + h * 0.6, cz + 0.5, cx - crownR * 0.5, baseY + h * 0.85, cz + 2, 1.0, bark);
        g.tube(cx + 1.5, baseY + h * 0.6, cz + 0.5, cx + crownR * 0.5, baseY + h * 0.9, cz - 2, 1.0, bark);
        // crown: three lobes of leaves with apples
        g.ellipsoid(cx + 0.5, baseY + h, cz + 0.5, crownR, crownR * 0.6, crownR, leaves);
        g.ellipsoid(cx - crownR * 0.5, baseY + h - 2, cz + 3, crownR * 0.7, crownR * 0.5, crownR * 0.7, leaves);
        g.ellipsoid(cx + crownR * 0.6, baseY + h - 1, cz - 3, crownR * 0.7, crownR * 0.5, crownR * 0.7, leaves);
        // a ladder of fallen-apple dots on the island
        for (int i = 0; i < 5; i++) {
            int x = cx - ir + 2 + (int) (g.rnd(i, 1, 0, salt) * (2 * ir - 4)), z = cz - ir + 2 + (int) (g.rnd(i, 2, 0, salt) * (2 * ir - 4));
            if (g.air(x, baseY + 1, z)) g.set(x, baseY + 1, z, i % 2 == 0 ? "red_wool" : "lime_wool");
        }
    }
}
