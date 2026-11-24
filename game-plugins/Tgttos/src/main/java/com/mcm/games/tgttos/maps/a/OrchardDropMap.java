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

import java.util.ArrayList;
import java.util.List;

/**
 * Orchard Drop (drop map, snowballs only, lethal falls). Players start on the gallery of a white windmill 60 blocks
 * above a floating orchard and drop down through a giant apple tree in three stages. Every stage lands in a pool of
 * water 2 deep cut into the top of a giant apple (or a pond in the meadow at the bottom); a fall of
 * {@link #LETHAL} or more blocks onto anything else sends you back to the gallery.
 *
 * <ul>
 *   <li>SAFE line (left): big apples with 5-wide pools (gallery -> apple at y 40 -> apple at y 22 -> big pond).</li>
 *   <li>FAST line (right): small apples with 3-wide pools, further out and lower, each drop steered diagonally
 *       (gallery -> apple at y 34 -> apple at y 14 -> small pond), landing next to the hedge gaps that lead to the
 *       fallen-branch choke.</li>
 *   <li>Two dry apples (no pool) sit where a careless jumper aims: straight ahead of the gallery and under the
 *       tree. The middle canopy hides the lower stage from the gallery.</li>
 *   <li>Bottom: an orchard meadow split by two hedge rows (three gaps each), then three ways across the void to the
 *       cider yard: a 3-wide fallen branch (choke), a 5-wide plank bridge far left (long way), and a broken bridge
 *       (dead end). In the yard a barrel wall blocks the direct way; right is a 4-wide ledge along the void to the
 *       real pen under the cider tower, left is the inner yard with a decoy pen (unnamed chickens) and a lane round.
 *       A second decoy pen sits in a dead-end pocket at the far left.</li>
 * </ul>
 *
 * <p>Local frame: x across, z along the run. Gallery deck top y 59 (feet 60); meadow and yard top y 0 (feet 1).
 * Palette: apple red / leaf green / oak and white plaster.
 */
public final class OrchardDropMap extends AMap {

    /** Falls of this many blocks (highest point minus landing) onto anything but water fail the racer. */
    public static final int LETHAL = 8;

    @Override public String id() { return "orchard_drop"; }
    @Override public String displayName() { return "Orchard Drop"; }
    @Override public MapType type() { return MapType.DROP; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }
    @Override public int lethalFallHeight() { return LETHAL; }

    /** Decoy chickens (no name tag) in the two decoy pens. */
    private static final List<int[]> DECOYS = List.of(
            new int[]{-16, 1, 106}, new int[]{-12, 1, 109}, new int[]{-9, 1, 105}, new int[]{-14, 1, 112}, new int[]{-10, 1, 112},
            new int[]{-30, 1, 122}, new int[]{-27, 1, 125}, new int[]{-30, 1, 128}, new int[]{-26, 1, 129});

    /** Owner (playtest 2): the two decoy pens are gone; decoys wander the later part of the map (runtime scatter). */
    @Override public List<int[]> decoyChickens() { return List.of(); }

    private Gen g;
    private final Pal red = new Pal("red_terracotta", 45, "red_wool", 30, "red_nether_bricks", 10, "red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", 15);
    private final Pal green = new Pal("lime_terracotta", 45, "green_terracotta", 25, "moss_block", 20, "lime_wool", 10);
    private final Pal flesh = new Pal("white_terracotta", 50, "smooth_sandstone", 30, "birch_planks", 20);
    private final Pal plaster = new Pal("white_terracotta", 55, "calcite", 35, "diorite", 10);
    private final Pal rock = new Pal("stone", 35, "andesite", 25, "dirt", 20, "coarse_dirt", 10, "tuff", 10);
    private final Pal leaves = new Pal("oak_leaves[distance=1,persistent=true,waterlogged=false]", 70, "azalea_leaves[distance=1,persistent=true,waterlogged=false]", 22, "red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]", 8);
    private final Pal bark = new Pal("oak_log[axis=y]", 60, "dark_oak_log[axis=y]", 40);
    private final Pal grass = new Pal("grass_block[snowy=false]", 75, "moss_block", 15, "coarse_dirt", 10);
    private static final String LEAF = "oak_leaves[distance=1,persistent=true,waterlogged=false]";
    private static final String AZ = "azalea_leaves[distance=1,persistent=true,waterlogged=false]";
    private static final String FAZ = "flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]";
    private static final String LANTERN = "lantern[hanging=false,waterlogged=false]";

    /** Water pools placed (for the containment pass). */
    private final List<int[]> water = new ArrayList<>();

    @Override
    protected void draw(Gen gen) {
        g = gen;
        water.clear();
        TgMapData d = g.d;
        Design de = g.design;
        windmill(d);
        meadow();
        cideryard(d);
        // ---- the tree and its apples. Stage 1 tops y 40 (safe) / 34 (fast); stage 2 tops y 22 / 14.
        bigTree();
        int[] s1 = apple(-11.5, 19.5, 6.5, 40, 2.6, false, +1);     // S1 big, 5-wide pool
        int[] f1 = apple(21.5, 19.5, 3.6, 34, 1.5, true, +1);       // F1 small, 3-wide pool, out right of the gallery
        int[] x1 = apple(7.5, 15.5, 4.0, 37, -1, false, -1);        // X1 dry apple straight ahead (a trap)
        int[] s2 = apple(-19.5, 34.5, 6.5, 22, 2.6, true, +1);      // S2 big
        int[] f2 = apple(13.5, 31.5, 3.4, 14, 1.5, false, -1);      // F2 small
        int[] x2 = apple(-1.5, 41.5, 4.5, 18, -1, true, +1);        // X2 dry apple under the tree
        // stage-1 apples hang from the upper limbs (y ~44-47), stage-2 apples from the lower limbs (y ~27-30)
        double tx = 4.5, tz = 26.5;
        limb(tx, 47, tz, s1, 46, false);
        limb(tx, 46, tz, f1, 45, false);
        limb(tx, 45, tz, x1, 44, false);
        limb(tx, 30, tz, s2, 29, true);
        limb(tx, 29, tz, f2, 27, true);
        limb(tx, 29, tz, x2, 28, true);
        crossings();
        ciderTower(de);
        scenery();
        contain();
        List<Region> keep = List.of(d.pen, Region.of(4, 0, 83, 8, 3, 97),
                Region.of(-27, 0, 62, -25, 3, 63), Region.of(-9, 0, 62, -7, 3, 63), Region.of(15, 0, 62, 17, 3, 63),
                Region.of(-12, 0, 74, -10, 3, 75), Region.of(6, 0, 74, 8, 3, 75), Region.of(22, 0, 74, 24, 3, 75),
                Region.of(15, 0, 101, 18, 3, 117), Region.of(-18, 0, 104, -7, 3, 114), Region.of(-32, 0, 120, -24, 3, 130));
        g.breakRuns(-34, 30, 22, 142, 1, 8, keep, "barrel[facing=up,open=false]", "hay_block[axis=y]", "composter[level=6]",
                "red_wool", "lime_terracotta", "oak_log[axis=y]");

        d.voidY = -20;
        d.playBounds = Region.of(-40, -24, -6, 34, 70, 146);
        d.overviewPoints.add(new Spawn(40, 52, -6, 40f, 30f));
        d.overviewPoints.add(new Spawn(0.5, 64, 4, 0f, 45f));
        d.notes.put("fastLine", "right: small apples (3-wide pools) at y 34 and y 14, steer diagonally, small pond, hedge gaps, 3-wide fallen branch, ledge along the void to the pen");
        d.notes.put("safeLine", "left: big apples (5-wide pools) at y 40 and y 22, big pond, hedge gaps, 5-wide plank bridge far left, round the yard to the pen");
        d.notes.put("lethalFall", LETHAL + " blocks: every drop must end in water");

        de.searchBox = Region.of(-40, -22, -6, 34, 66, 146);
        de.lane.add(Region.of(-34, -40, -1, 30, 63, 133));

        Region startBox = Region.of(-19, 59, -1, 18, 63, 10);
        Design.Line fast = de.line("FAST", false, true);
        fast.description = "right: sprint-jump diagonally off the gallery corner (26-block fall) into the 3-wide pool of the small apple, steer 8 out "
                + "into the next small apple (21 blocks), then 7 out into the small pond; hedge gaps right/middle, the 3-wide fallen branch over the void, "
                + "the 4-wide ledge along the void edge to the pen";
        fast.leg("small apple 1", Region.of(19, 33, 18, 24, 34, 23), startBox, Region.of(2, 32, -1, 27, 63, 25))
            .leg("small apple 2", Region.of(11, 13, 30, 16, 14, 35), Region.of(8, 12, 16, 27, 37, 37))
            .leg("small pond", Region.of(9, -1, 42, 14, 0, 47), Region.of(6, -2, 28, 18, 16, 48))
            .leg("pen", d.pen, Region.of(-6, -2, 40, 28, 8, 133));
        Design.Line safe = de.line("SAFE", false, false);
        safe.maxGap = 2;
        safe.description = "left: jump out 7 into the 5-wide pool of the big apple (20-block fall), cross it, 8 out into the next big apple (19), "
                + "drop into the big pond; hedge gaps left, the 5-wide plank bridge far left, up the west side and round the yard to the pen";
        safe.leg("big apple 1", Region.of(-15, 39, 16, -8, 40, 23), startBox, Region.of(-20, 37, -1, 0, 63, 27))
            .leg("big apple 2", Region.of(-23, 21, 31, -16, 22, 38), Region.of(-28, 20, 12, -2, 42, 42))
            .leg("big pond", Region.of(-20, -1, 44, -11, 0, 53), Region.of(-30, -2, 30, -6, 24, 54))
            .leg("pen", d.pen, Region.of(-34, -2, 40, -12, 8, 142), Region.of(-34, -2, 114, 16, 8, 135));
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
        g.set(-19, 64, 9, LANTERN);
        g.set(18, 64, 9, LANTERN);
        // flower boxes along the railing bottoms
        for (int z = 1; z <= 8; z += 2) { g.set(-20, 59, z, "red_terracotta"); g.set(-20, 60, z, "poppy"); g.set(19, 59, z, "red_terracotta"); g.set(19, 60, z, "poppy"); }
        // barrier (glass) and start strip
        for (int x = -18; x <= 17; x++) for (int y = 60; y <= 62; y++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new Pos(x, y, 7)); }
        d.startStrip = Region.of(-18, 60, 0, 17, 63, 6);
        for (int row = 0; row < 4; row++)
            for (int k = 0; k < 13; k++) d.startSpawns.add(new Spawn(-17.5 + k * 2.75 + (row % 2) * 0.6, 60, 0.6 + row * 1.6, 0f));
    }

    // ------------------------------------------------------------------ apples

    /**
     * Giant apple cut flat at {@code top}: skin shell, flesh on top, a pool of water 2 deep in the core (radius
     * {@code wr}; {@code wr < 0} = a dry apple with a bruised brown core and no water), and a stem on the far rim
     * towards {@code side} (+1 = +x) that the tree's branches hang it from.
     */
    private int[] apple(double cx, double cz, double R, int top, double wr, boolean isGreen, int side) {
        Pal skin = isGreen ? green : red;
        double cy = top - R * 0.3, ry = R * 0.85;
        double rim = R * Math.sqrt(Math.max(0, 1 - Math.pow((top + 0.5 - cy) / ry, 2)));
        for (int x = (int) Math.floor(cx - R) - 1; x <= (int) Math.ceil(cx + R) + 1; x++)
            for (int z = (int) Math.floor(cz - R) - 1; z <= (int) Math.ceil(cz + R) + 1; z++)
                for (int y = (int) Math.floor(cy - ry) - 1; y <= top; y++) {
                    double a = (x + 0.5 - cx) / R, c = (y + 0.5 - cy) / ry, e = (z + 0.5 - cz) / R;
                    double q = a * a + c * c + e * e;
                    if (q > 1) continue;
                    double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                    boolean pool = wr > 0 && rr <= wr;
                    String s;
                    if (y == top) {
                        if (pool) { s = "water"; water.add(new int[]{x, y, z}); }
                        else if (rr >= rim - 1.0) s = skin.at(x, y, z, g.seed);
                        else if (wr < 0 && rr <= 1.6) s = (rr <= 0.8) ? "brown_terracotta" : "brown_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]";
                        else if (rr <= wr + 0.9 && R > 4) s = "brown_terracotta";      // the core around the pool
                        else s = flesh.at(x, y, z, g.seed);
                    } else if (y == top - 1) {
                        if (pool) { s = "water"; water.add(new int[]{x, y, z}); }
                        else s = q > 0.62 ? skin.at(x, y, z, g.seed) : flesh.at(x, y, z, g.seed);
                    } else if (y == top - 2 && rr <= wr + 1.0) s = "brown_terracotta";     // pool floor
                    else if (q > 0.62) s = skin.at(x, y, z, g.seed);
                    else s = "air";
                    if (s.equals("air")) continue;
                    g.set(x, y, z, s);
                }
        // calyx at the bottom
        int by = (int) Math.floor(cy - ry);
        g.set((int) Math.floor(cx), by, (int) Math.floor(cz), "brown_terracotta");
        g.set((int) Math.floor(cx), by - 1, (int) Math.floor(cz), "dark_oak_log[axis=y]");
        // stem on the far rim, towards the tree
        int sx = (int) Math.floor(cx + side * R * 0.42), sz = (int) Math.floor(cz + R * 0.52);
        g.set(sx, top + 1, sz, "dark_oak_log[axis=y]");
        g.set(sx, top + 2, sz, "dark_oak_log[axis=y]");
        return new int[]{sx, top + 3, sz};
    }

    // ------------------------------------------------------------------ the giant apple tree

    private void bigTree() {
        double tx = 4.5, tz = 26.5;
        // trunk with a root flare on the meadow, slight taper
        for (int y = 1; y <= 52; y++) {
            double r = 3.4 - 0.9 * y / 52.0 + (y <= 3 ? (4 - y) * 0.9 : 0);
            g.cyl(tx + 0.3 * Math.sin(y * 0.09), tz, r, y, y, bark);
        }
        for (int k = 0; k < 6; k++) {          // surface roots
            double ang = k * Math.PI / 3 + 0.4;
            g.tube(tx, 2, tz, tx + Math.cos(ang) * 7, 0.6, tz + Math.sin(ang) * 7, 0.9, new Pal("dark_oak_log[axis=y]", 60, "rooted_dirt", 40));
        }
        // crown: lobes of leaves high above, behind the stage-1 apples (the stage-1 pools stay open to the sky)
        g.ellipsoid(tx, 50, tz + 3, 9.5, 4.0, 8.5, leaves);
        g.ellipsoid(tx - 6, 48, tz + 7, 6.0, 3.5, 6.0, leaves);
        g.ellipsoid(tx + 7, 48, tz + 6, 6.0, 3.0, 6.0, leaves);
        // apples dotted in the crown
        for (int i = 0; i < 40; i++) {
            int x = (int) Math.floor(tx - 12 + g.rnd(i, 0, 0, 61) * 24), z = (int) Math.floor(tz - 6 + g.rnd(i, 0, 0, 62) * 22);
            int y = 44 + (int) (g.rnd(i, 0, 0, 63) * 11);
            if (!g.air(x, y, z) && g.air(x, y - 1, z)) g.set(x, y, z, i % 3 == 0 ? "lime_wool" : "red_wool");
        }
    }

    /** One limb: vertical stem from the apple top (feet y {@code sy}) up to {@code ly}, then a tapering branch to the trunk at {@code ty}. */
    private void limb(double tx, int ty, double tz, int[] stem, int ly, boolean canopy) {
        int sx = stem[0], sy = stem[1], sz = stem[2];
        for (int y = sy; y <= ly; y++) g.set(sx, y, sz, "dark_oak_log[axis=y]");
        g.tube(sx + 0.5, ly + 0.5, sz + 0.5, tx, ty, tz, 0.8, bark);
        if (canopy) {
            double mx = (sx + 0.5 + tx) / 2, mz = (sz + 0.5 + tz) / 2;
            g.ellipsoid(mx, ly + 1.5, mz, 4.5, 1.6, 3.5, leaves);
            g.ellipsoid(sx + 0.5 + (tx - sx) * 0.15, ly + 2, sz + 0.5 + (tz - sz) * 0.15, 3.0, 1.4, 3.0, leaves);
        }
    }

    // ------------------------------------------------------------------ meadow (bottom of the drop)

    private void meadow() {
        // island top (y 0) under the tree, ragged edge, tapered rocky underside with roots
        for (int x = -33; x <= 29; x++) for (int z = 18; z <= 84; z++) {
            double ex = Math.max(0, Math.abs(x + 2) - 26) / 5.0, ez = z < 28 ? (28 - z) / 8.0 : 0;
            double e = ex * ex + ez * ez;
            double n = 0.35 * Noise.fbm2(x * 0.15, z * 0.15, g.seed + 21);
            if (e > 1 + n) continue;
            if (z == 84 || (z >= 82 && Math.abs(x + 2) > 27)) continue;
            g.set(x, 0, z, grass.at(x, 0, z, g.seed));
        }
        under(-33, 18, 29, 84, 18, 23);
        for (int i = 0; i < 18; i++) {            // hanging roots
            int x = -28 + (int) (g.rnd(i, 0, 0, 31) * 54), z = 26 + (int) (g.rnd(i, 0, 0, 32) * 54);
            int len = 3 + (int) (g.rnd(i, 0, 0, 33) * 6);
            int y = -1;
            while (y > -30 && !g.air(x, y, z)) y--;
            if (y <= -30) continue;
            for (int k = 0; k < len; k++) g.set(x, y - k, z, k == len - 1 ? "hanging_roots" : "rooted_dirt");
        }
        // landing ponds: big (safe), small (fast), and a middle pond in the maze
        pond(-15.5, 48.5, 3.6);
        pond(11.5, 43.5, 2.1);
        pond(-2.5, 58.5, 2.4);
        // hedge rows (3 high, unjumpable) with three gaps each, offset row to row
        hedgeRow(62, new int[][]{{-27, -25}, {-9, -7}, {15, 17}});
        hedgeRow(74, new int[][]{{-12, -10}, {6, 8}, {22, 24}});
        // spurs: a dead-end pocket on the left, a loop in the middle
        for (int z = 64; z <= 70; z++) hedge(-17, z);
        for (int z = 64; z <= 69; z++) hedge(9, z);
        for (int x = -6; x <= 2; x++) hedge(x, 68);
        // small apple trees and crates between the ponds and the first row
        smallTree(-27, 40, 71);
        smallTree(-4, 35, 72);
        smallTree(21, 53, 73);
        smallTree(-8, 50, 74);
        crate(-24, 52); crate(4, 52); crate(18, 40); crate(-11, 56);
        cart(-30, 66);
        fallenApple(-20, 70, true); fallenApple(16, 68, false); fallenApple(-3, 79, true);
        // lantern posts marking the way to the fallen branch (the real way on)
        post(3, 82); post(9, 82); post(10, 77);
        smallTree(6, 79, 75); post(14, 64); post(18, 60);
    }

    /** Tapered rocky underside, only below columns that have a top block at y 0 (no hidden rims beside the edges). */
    private void under(int x1, int z1, int x2, int z2, int depth, long salt) {
        double cx = (x1 + x2) / 2.0, cz = (z1 + z2) / 2.0, hx = (x2 - x1) / 2.0 + 0.5, hz = (z2 - z1) / 2.0 + 0.5;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                if (g.air(x, 0, z)) continue;
                int edge = 0;
                for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (g.air(x + n[0], 0, z + n[1])) edge++;
                double e = Math.max(Math.abs(x - cx) / hx, Math.abs(z - cz) / hz);
                double n = Noise.fbm2(x * 0.18, z * 0.18, g.seed + salt);
                int dpt = Math.max(edge > 0 ? 1 : 2, (int) Math.round(depth * (1 - e * e) * (0.55 + 0.6 * n)));
                for (int y = -1; y >= -dpt; y--) g.soft(x, y, z, rock.at(x, y, z, g.seed + salt));
            }
    }

    private void pond(double cx, double cz, double r) {
        for (int x = (int) Math.floor(cx - r) - 1; x <= (int) Math.ceil(cx + r) + 1; x++)
            for (int z = (int) Math.floor(cz - r) - 1; z <= (int) Math.ceil(cz + r) + 1; z++) {
                double dd = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (dd <= r) {
                    g.set(x, 0, z, "water"); g.set(x, -1, z, "water"); g.set(x, -2, z, "clay");
                    g.set(x, 1, z, null); g.set(x, 2, z, null);
                    water.add(new int[]{x, 0, z}); water.add(new int[]{x, -1, z});
                } else if (dd <= r + 1.2) {
                    g.set(x, 0, z, g.rnd(x, 0, z, 7) < 0.5 ? "mud" : "moss_block");
                    g.set(x, 1, z, null); g.set(x, 2, z, null);
                }
            }
    }

    private void hedge(int x, int z) {
        if (g.air(x, 0, z)) return;
        for (int y = 1; y <= 3; y++) g.set(x, y, z, y == 3 && Math.floorMod(x * 7 + z, 5) == 0 ? FAZ : (Math.floorMod(x + z * 3, 4) == 0 ? AZ : LEAF));
        if (Math.floorMod(x * 3 + z, 6) == 0) g.set(x, 4, z, LEAF);
        if (Math.floorMod(x * 5 + z, 9) == 0) g.set(x, 2, z, "red_mushroom_block[down=true,east=true,north=true,south=true,up=true,west=true]");
    }

    private void hedgeRow(int z, int[][] gaps) {
        for (int x = -34; x <= 30; x++) {
            boolean gap = false;
            for (int[] gp : gaps) if (x >= gp[0] && x <= gp[1]) gap = true;
            if (gap) continue;
            hedge(x, z); hedge(x, z + 1);
        }
    }

    private void smallTree(int x, int z, long salt) {
        if (g.air(x, 0, z)) return;
        for (int y = 1; y <= 4; y++) g.set(x, y, z, "oak_log[axis=y]");
        g.ellipsoid(x + 0.5, 6, z + 0.5, 3.2, 2.2, 3.2, leaves);
        for (int i = 0; i < 4; i++) {
            int ax = x - 2 + (int) (g.rnd(i, 0, 0, salt) * 5), az = z - 2 + (int) (g.rnd(i, 1, 0, salt) * 5);
            if (g.air(ax, 1, az) && !g.air(ax, 0, az) && !(ax == x && az == z)) g.set(ax, 1, az, i % 2 == 0 ? "red_wool" : "lime_wool");
        }
    }

    private void post(int x, int z) {
        g.set(x, 1, z, "stripped_dark_oak_log[axis=y]");
        g.set(x, 2, z, "oak_fence");
        g.set(x, 3, z, LANTERN);
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

    // ------------------------------------------------------------------ crossings over the void

    private void crossings() {
        // C1: fallen branch, 3 wide (x -1..1), over the void z 84..96: the choke
        for (int z = 83; z <= 97; z++) for (int x = 5; x <= 7; x++) {
            g.set(x, 0, z, (z + x) % 5 == 0 ? "dark_oak_wood[axis=y]" : "oak_wood[axis=y]");
            g.set(x, -1, z, "dark_oak_wood[axis=y]");
            if (x == 6 && z >= 86 && z <= 94) g.set(x, -2, z, "dark_oak_wood[axis=y]");
        }
        g.set(7, 1, 88, "oak_leaves[distance=1,persistent=true,waterlogged=false]");     // twigs with leaves: 2 wide there
        g.set(5, 1, 92, "oak_leaves[distance=1,persistent=true,waterlogged=false]");
        g.set(7, 2, 88, "red_wool");
        g.set(6, 1, 90, "spruce_slab[type=bottom,waterlogged=false]");      // a knot half-way: a half step up
        // C2: plank bridge far left, 5 wide (x -30..-26), the long way
        for (int z = 83; z <= 97; z++) for (int x = -30; x <= -26; x++) {
            boolean beam = x == -30 || x == -26;
            g.set(x, 0, z, beam ? "stripped_spruce_log[axis=z]" : (z % 4 == 0 ? "spruce_planks" : "oak_planks"));
            if (beam && z % 4 == 1) { g.set(x, -1, z, "spruce_fence"); g.set(x, -2, z, "spruce_fence"); g.set(x, -3, z, "lantern[hanging=true,waterlogged=false]"); }
        }
        for (int z = 85; z <= 95; z += 5) { g.set(-30, 1, z, "barrel[facing=up,open=false]"); }
        g.set(-26, 1, 90, "hay_block[axis=y]");
        for (int z = 86; z <= 95; z += 3) g.set(-29 + Math.floorMod(z / 3, 3), 1, z, z % 2 == 0 ? "hay_block[axis=y]" : "barrel[facing=up,open=false]");
        // C3: broken bridge on the right (dead end: 6 blocks of void before the yard)
        for (int z = 83; z <= 88; z++) for (int x = 20; x <= 22; x++)
            if (z < 88 || x != 21) g.set(x, 0, z, x == 21 ? "oak_planks" : "stripped_oak_log[axis=z]");
        g.set(21, 0, 88, Gen.st("oak", "south", true));                   // splintered end
        for (int z = 95; z <= 96; z++) for (int x = 20; x <= 22; x++) if (g.air(x, 0, z)) g.set(x, 0, z, "oak_planks");
    }

    // ------------------------------------------------------------------ cider yard, pens

    private void cideryard(TgMapData d) {
        // island top (y 0): x -33..22, z 96..142; narrower along the east ledge (x <= 18 for z 101..117)
        for (int x = -34; x <= 23; x++) for (int z = 96; z <= 143; z++) {
            double ez = z > 136 ? (z - 136) / 7.0 : 0, ex = x < -29 ? (-29 - x) / 4.5 : 0;
            double n = 0.3 * Noise.fbm2(x * 0.17, z * 0.17, g.seed + 37);
            if (ex * ex + ez * ez > 1 + n) continue;
            if (x > 18 && z >= 101 && z <= 117) continue;
            if (x > 22) continue;
            g.set(x, 0, z, grass.at(x, 0, z, g.seed));
        }
        under(-34, 96, 23, 143, 18, 29);
        // barrel walls (3 high + heaped apples): Y1 along z 101 (x -20..14), Y2 along x 14 (z 101..117),
        // Y3 along x -21 (z 101..117) with a gap at z 108..110, Y4 along z 118 west of the pen lane gap
        for (int x = -20; x <= 14; x++) wall(x, 101);
        for (int z = 102; z <= 117; z++) wall(14, z);
        for (int z = 102; z <= 117; z++) if (z < 108 || z > 110) wall(-21, z);
        for (int x = -20; x <= -6; x++) wall(x, 117);      // inner yard north wall, exit gap x -5..-1
        for (int x = 0; x <= 13; x++) wall(x, 117);
        // decoy pen D1 in the inner yard (unnamed chickens)
        // decoy pen D1 removed after playtest 2: the same footprint is now a solid apple-crate heap (no pen, same obstacle)
        for (int x = -18; x <= -7; x++) for (int z = 104; z <= 114; z++) {
            g.set(x, 1, z, ((x + z) & 1) == 0 ? "barrel[facing=up,open=false]" : "oak_planks");
            if (g.rnd(x, 2, z, 7) < 0.45 && x > -18 && x < -7 && z > 104 && z < 114) g.set(x, 2, z, g.rnd(x, 2, z, 8) < 0.5 ? "red_wool" : "hay_block[axis=y]");
        }
        // decoy pen D2 in the dead-end pocket at the far left (behind a wall that closes the west strip)
        for (int x = -33; x <= -22; x++) wall(x, 132);
        for (int z = 119; z <= 131; z++) wall(-22, z);
        // decoy pen D2 removed after playtest 2: a cider-barrel store fills the pocket instead (no pen)
        for (int x = -32; x <= -24; x++) for (int z = 120; z <= 130; z++) {
            boolean edge = x == -32 || x == -24 || z == 120 || z == 130;
            if (edge) g.set(x, 1, z, ((x + z) & 1) == 0 ? "barrel[facing=up,open=false]" : "spruce_planks");
            else if (((x + z) % 4) == 0) g.set(x, 1, z, "barrel[facing=east,open=false]");
        }
        // lane north of the inner yard to the pen, with staggered barrels (no free sprint)
        int[][] lane = {{-18, 120}, {-12, 122}, {-7, 119}, {-2, 121}, {-15, 125}, {-9, 127}, {-4, 125}};
        for (int[] b : lane) { g.set(b[0], 1, b[1], "barrel[facing=up,open=false]"); g.set(b[0] + 1, 1, b[1], "hay_block[axis=x]"); }
        // yard dressing: cider press, barrel racks, crates
        press(-12, 133);
        for (int z = 121; z <= 131; z += 5) { g.set(20, 1, z, "barrel[facing=east,open=false]"); g.set(21, 1, z, "barrel[facing=east,open=false]"); g.set(20, 2, z, "barrel[facing=east,open=false]"); }
        crate(-4, 106); crate(4, 110); crate(-1, 113); crate(8, 104); crate(16, 136); crate(-20, 138);
        // the real pen: low ring of logs and planks with flower pots, interior x 3..13, z 121..131
        int x1 = 2, x2 = 14, z1 = 120, z2 = 132;
        penRing(x1, z1, x2, z2, true);
        g.set(4, 1, 130, "hay_block[axis=y]");
        g.set(12, 1, 122, "composter[level=4]");
        d.pen = Region.of(x1 + 1, 1, z1 + 1, x2 - 1, 3, z2 - 1);
        for (int i = 0; i < 10; i++) d.chickenSpawns.add(new Spawn(4.5 + (i % 5) * 1.8, 1, 123.5 + (i / 5) * 5, 180f));
        // staggered stacks on the east ledge: 2-wide squeezes along the void edge
        int[][] ledge = {{17, 102}, {18, 102}, {15, 106}, {16, 106}, {17, 110}, {18, 110}, {15, 114}, {16, 114}};
        for (int[] b : ledge) { g.set(b[0], 1, b[1], "barrel[facing=up,open=false]"); g.set(b[0], 2, b[1], b[0] % 2 == 0 ? "red_wool" : "hay_block[axis=y]"); }
        // lantern posts along the real way: strip by the branch, the east ledge, the pen corner
        post(4, 99); post(10, 99); post(17, 100); post(18, 106); post(18, 112); post(18, 116); post(16, 119);
        // bunting over the ledge
        for (int z = 101; z <= 117; z++) if (z % 2 == 0) g.set(18, 4 + (z % 4 == 0 ? 0 : 1) - 1, z, z % 4 == 0 ? "red_wool" : "lime_wool");
    }

    private void wall(int x, int z) {
        if (g.air(x, 0, z)) return;
        for (int y = 1; y <= 3; y++) {
            double r = g.rnd(x, y, z, 41);
            String s = Math.floorMod(x + z, 7) == 0 ? "stripped_oak_log[axis=y]"
                    : r < 0.45 ? "barrel[facing=up,open=false]" : r < 0.7 ? "oak_planks" : r < 0.85 ? "hay_block[axis=y]" : "spruce_planks";
            g.set(x, y, z, s);
        }
        if (Math.floorMod(x + z, 7) == 0) { g.set(x, 4, z, "oak_fence"); g.set(x, 5, z, LANTERN); }
        else g.set(x, 4, z, g.rnd(x, 4, z, 42) < 0.5 ? "red_wool" : "lime_terracotta");   // heaped apples on the stacks
    }

    /** Low chicken pen ring (1 high + flower pots), mossy floor inside. */
    private void penRing(int x1, int z1, int x2, int z2, boolean real) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (edge) {
                g.set(x, 1, z, ((x + z) & 1) == 0 ? "stripped_oak_log[axis=y]" : "oak_planks");
                g.set(x, 2, z, (x + z) % 3 == 0 ? "potted_red_tulip" : null);
            } else {
                g.set(x, 0, z, g.rnd(x, 0, z, 3) < 0.7 ? "moss_block" : "grass_block[snowy=false]");
                g.set(x, 1, z, g.rnd(x, 1, z, 4) < 0.07 ? "short_grass" : null);
                g.set(x, 2, z, null);
            }
        }
        if (!real) { g.set(x1, 2, z1, "lantern[hanging=false,waterlogged=false]"); }
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

    // ------------------------------------------------------------------ cider-house tower (landmark, behind the real pen)

    private void ciderTower(Design de) {
        int x1 = 3, x2 = 13, z1 = 134, z2 = 142;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = 1; y <= 40; y++) {
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
        g.set(8, 1, 134, "spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]");
        g.set(8, 2, 134, "spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]");
        // red roof pyramid
        for (int k = 0; k <= 6; k++) for (int x = x1 - 1 + k; x <= x2 + 1 - k; x++) for (int z = z1 - 1 + k; z <= z2 + 1 - k; z++) {
            boolean edge = x == x1 - 1 + k || x == x2 + 1 - k || z == z1 - 1 + k || z == z2 + 1 - k;
            if (edge || k == 6) g.set(x, 41 + k, z, (k & 1) == 0 ? "red_nether_bricks" : "red_terracotta");
        }
        for (int y = 48; y <= 51; y++) g.set(8, y, 138, "dark_oak_log[axis=y]");
        // the giant apple on the spike, with stem and leaf
        g.ellipsoid(8.5, 56.5, 138.5, 5.2, 4.8, 5.2, red);
        for (int y = 61; y <= 64; y++) g.set(8, y, 138, "dark_oak_log[axis=y]");
        g.ellipsoid(10.5, 64.5, 138.5, 2.6, 0.7, 1.4, new Pal("moss_block", 60, "lime_terracotta", 40));
        de.landmark.add(new Pos(8, 57, 134));
        de.landmark.add(new Pos(8, 60, 136));
        de.landmark.add(new Pos(8, 63, 138));
        de.landmark.add(new Pos(8, 64, 138));
        de.landmark.add(new Pos(4, 58, 137));
        de.landmark.add(new Pos(12, 58, 137));
        de.landmark.add(new Pos(10, 65, 138));
        // bunting from the tower down to the pen corners
        for (int i = 0; i <= 14; i++) {
            double t = i / 14.0;
            int y = (int) Math.round(38 + (4 - 38) * t - 3 * Math.sin(Math.PI * t));
            g.soft(3, y, (int) Math.round(134 + (120 - 134) * t), (i & 1) == 0 ? "red_wool" : "lime_wool");
            g.soft(13, y, (int) Math.round(134 + (120 - 134) * t), (i & 1) == 0 ? "lime_wool" : "red_wool");
        }
    }

    // ------------------------------------------------------------------ scenery: orchard islands, trees

    private void scenery() {
        tree(-48, 14, 6, 24, 11, 51);
        tree(46, 26, 10, 22, 10, 52);
        tree(-52, 64, -2, 26, 12, 53);
        tree(48, 74, 2, 24, 11, 54);
        tree(-54, 116, -4, 28, 12, 55);
        tree(42, 124, 0, 26, 11, 56);
        tree(-16, 166, -8, 20, 9, 57);
        tree(30, 164, -6, 22, 10, 58);
        // two small dry apples off the course (out of reach)
        apple(-42.5, 40.5, 3, 30, -1, false, 1);
        apple(40.5, 50.5, 3, 20, -1, true, -1);
    }

    private void tree(int cx, int cz, int baseY, int h, int crownR, long salt) {
        int ir = crownR - 1;
        for (int x = cx - ir; x <= cx + ir; x++) for (int z = cz - ir; z <= cz + ir; z++) {
            double dd = Math.hypot(x - cx, z - cz) / ir;
            if (dd <= 1 + 0.2 * Noise.fbm2(x * 0.2, z * 0.2, g.seed + salt)) g.set(x, baseY, z, grass.at(x, baseY, z, g.seed));
        }
        g.underside(cx - ir, cz - ir, cx + ir, cz + ir, baseY, ir + 4, rock, salt);
        g.tube(cx + 0.5, baseY + 1, cz + 0.5, cx + 1.5, baseY + h * 0.6, cz + 0.5, 1.6, bark);
        g.tube(cx + 1.5, baseY + h * 0.6, cz + 0.5, cx - crownR * 0.5, baseY + h * 0.85, cz + 2, 1.0, bark);
        g.tube(cx + 1.5, baseY + h * 0.6, cz + 0.5, cx + crownR * 0.5, baseY + h * 0.9, cz - 2, 1.0, bark);
        g.ellipsoid(cx + 0.5, baseY + h, cz + 0.5, crownR, crownR * 0.6, crownR, leaves);
        g.ellipsoid(cx - crownR * 0.5, baseY + h - 2, cz + 3, crownR * 0.7, crownR * 0.5, crownR * 0.7, leaves);
        g.ellipsoid(cx + crownR * 0.6, baseY + h - 1, cz - 3, crownR * 0.7, crownR * 0.5, crownR * 0.7, leaves);
        for (int i = 0; i < 5; i++) {
            int x = cx - ir + 2 + (int) (g.rnd(i, 1, 0, salt) * (2 * ir - 4)), z = cz - ir + 2 + (int) (g.rnd(i, 2, 0, salt) * (2 * ir - 4));
            if (g.air(x, baseY + 1, z)) g.set(x, baseY + 1, z, i % 2 == 0 ? "red_wool" : "lime_wool");
        }
    }

    // ------------------------------------------------------------------ water containment

    /** Every water block gets a solid neighbour wherever it would otherwise touch air at its sides or below. */
    private void contain() {
        for (int[] w : water) {
            int x = w[0], y = w[1], z = w[2];
            if (!"water".equals(com.mcm.games.tgttos.maps.a.util.Bs.id(g.get(x, y, z)))) continue;
            int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};
            for (int[] n : nb) if (g.air(x + n[0], y + n[1], z + n[2])) g.set(x + n[0], y + n[1], z + n[2], y > 5 ? "brown_terracotta" : "clay");
        }
    }
}
