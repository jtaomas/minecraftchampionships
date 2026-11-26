package com.mcm.games.tgttos.maps.a;

import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Design;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Noise.Pal;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapFeatures;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

/**
 * Laundry Line (bridging + knock-off brawl). A long hop over a red-brick terrace town: rooftop to rooftop across
 * four streets, every street a void with washing lines, chimney stacks and balconies in it, every rooftop a place
 * where the whole field piles up beside a drop. Owner (FEEDBACK_1..3): the game is people punching and snowballing
 * each other off things, so there is void everywhere and a mover or a crumbling floor at every brawl spot. Length
 * is Bookshelf Run or more; the finish is a full-width row of chickens in front of the clock tower.
 *
 * <ol>
 *   <li>Roof A: start strip. Street 1 (24): FAST = chimney stacks left of centre with two swinging sheets over
 *       the hops; SAFE = bridge on the right between hanging sheets.</li>
 *   <li>Roof B: pitched roof, chimney-breast wall on the ridge with a 4-wide choke and a swinging sheet in it
 *       (checkpoint 1). Street 2 (20): chimney stacks right of centre, two swinging sheets.</li>
 *   <li>Roof C, the Drying Terrace (checkpoint 2): open on all sides, a rotary washing line (whirligig) turns in
 *       the middle. Street 3 (24): stacks left, balconies right, two swinging sheets.</li>
 *   <li>Roof D, the dye-works scaffold (checkpoint 3): a crumbling plank floor over nothing. Street 4 (28): stacks
 *       and a free-standing island with a turning rotary line in the middle, balconies on the right.</li>
 *   <li>Roof E1, the bleach yard (checkpoint 4): sheets hung in rows to slalom through. The Long Plank (18): a
 *       5-wide plank over the street with three swinging sheets that sweep players off.</li>
 *   <li>Roof E2, the mill yard (checkpoint 5): two laundry baskets slide across the run and back, then the row
 *       of goal chickens under the clock tower and its golden cockerel.</li>
 * </ol>
 *
 * <p>Local frame: x across (lane -21..20), z along the run, roof decks at y = 0 (feet y = 1).
 */
public final class LaundryLineMap extends AMap {

    @Override public String id() { return "laundry_line"; }
    @Override public String displayName() { return "Laundry Line"; }
    @Override public MapType type() { return MapType.BRIDGE; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }
    @Override public MapFeatures features() { return feats(); }

    static final int B_Z1 = 37, B_Z2 = 48;
    static final int C_Z1 = 69, C_Z2 = 96;
    static final int D_Z1 = 121, D_Z2 = 135;
    static final int E1_Z1 = 164, E1_Z2 = 178;
    static final int PL_Z1 = 179, PL_Z2 = 196;
    static final int E2_Z1 = 197, E2_Z2 = 216;
    static final int FIN_Z = 211;
    static final int TOWER_Z = 225;

    private Gen g;

    // palettes
    private final Pal brick = new Pal("bricks", 70, "mud_bricks", 6, "granite", 4, "terracotta", 6, "bricks", 14);
    private final Pal brick2 = new Pal("bricks", 30, "terracotta", 20, "red_terracotta", 10, "mud_bricks", 30, "granite", 10);
    private final Pal soot = new Pal("deepslate_bricks", 40, "cobbled_deepslate", 30, "deepslate_tiles", 20, "tuff", 10);
    private final Pal deck = new Pal("polished_deepslate", 40, "deepslate_tiles", 30, "cobbled_deepslate", 15, "tuff", 15);
    private final Pal slate = new Pal("deepslate_tiles", 60, "polished_deepslate", 25, "cobbled_deepslate", 15);
    private final Pal stone = new Pal("stone_bricks", 60, "cracked_stone_bricks", 15, "andesite", 15, "polished_andesite", 10);
    private final Pal rubble = new Pal("cobbled_deepslate", 30, "tuff", 30, "andesite", 20, "bricks", 10, "gravel", 10);
    private final Pal linen = new Pal("white_wool", 70, "light_gray_wool", 20, "white_wool", 10);
    private final Pal pastel = new Pal("white_wool", 50, "pink_wool", 25, "light_blue_wool", 25);
    private final Pal planks = new Pal("spruce_planks", 55, "dark_oak_planks", 30, "oak_planks", 15);
    private static final String[] SHIRTS = {"red_wool", "yellow_wool", "light_blue_wool", "lime_wool", "orange_wool", "pink_wool", "white_wool", "cyan_wool"};

    @Override
    protected void draw(Gen gen) {
        g = gen;
        TgMapData d = g.d;
        Design de = g.design;

        buildingA();
        roofB();
        roofC();
        roofD();
        roofE1();
        plank();
        roofE2();
        street1();
        street2();
        street3();
        street4();
        bunting();
        neighbours();
        skyline();
        startStrip(d);
        pen(d);
        tower(d, de);
        // break up long runs on the big roofs (flower tubs, crates, vents)
        List<Region> keep = List.of(Region.of(-22, 0, 69, 21, 9, 72));
        g.breakRuns(-22, 21, C_Z1, C_Z2, 1, 11, keep,
                "barrel[facing=up,open=false]", "potted_azalea_bush", "polished_deepslate_wall", "hay_block[axis=y]", "composter[level=5]");

        d.voidY = -8;
        d.playBounds = Region.of(-23, -14, -9, 22, 62, 236);
        d.buildBounds = Region.of(-21, -6, 8, 20, 14, 208);
        d.overviewPoints.add(new Spawn(0.5, 40, -22, 0f, 28f));
        d.overviewPoints.add(new Spawn(-62, 34, 110, -90f, 25f));
        d.overviewPoints.add(new Spawn(62, 34, 150, 90f, 25f));
        d.notes.put("fastLine", "chimney stacks and an island hop across four streets (gaps 2-4), swinging sheets over the hops, the ridge choke, the crumbling scaffold, the plank");
        d.notes.put("safeLine", "balconies and own bridge on the right, round the wide ridge gap, bridge again; every street 2-5 blocks of bridging");
        d.notes.put("brawlSpots", "ridge choke z 37-48, drying terrace z 69-96, scaffold z 121-135, bleach yard z 164-178, long plank z 179-196, mill yard z 197-216");
        d.notes.put("movers", "ridge sheet, 8 swinging sheets over the streets, whirligig on the terrace, island rotary line, 3 plank sheets, 2 sliding baskets before the row; crumbling scaffold floor");

        de.searchBox = Region.of(-23, -10, -2, 22, 20, 214);
        de.lane.add(Region.of(-22, -40, -8, 21, 70, 234));
        de.lazyCorridor = Region.of(-2, -6, 0, 2, 14, 214);
        de.lazyBuildBox = Region.of(-2, -1, 8, 2, 4, 208);
        de.sprintExempt.add(Region.of(-23, 4, 41, 22, 6, 44));   // ridge top runs across (x), not along the course
        de.voidReport = true;
        de.minVoidCrossings = 10;
        de.minFastSave = 0.0;
        de.maxExpert = 62;
        de.brawl.add(Region.of(-22, 0, 70, 21, 4, 95));
        de.brawl.add(Region.of(-22, 0, D_Z1, 21, 4, D_Z2));
        de.brawl.add(Region.of(-22, 0, E1_Z1, 21, 4, E1_Z2));
        de.brawl.add(Region.of(-2, 0, PL_Z1, 2, 4, PL_Z2));
        de.brawl.add(Region.of(-22, 0, E2_Z1, 21, 4, 209));

        Design.Line fast = de.line("FAST", false, true);
        fast.description = "chimney-stack hops (gaps 2-4) across four streets with swinging sheets over them, the ridge choke, round the whirligig, the crumbling scaffold, the island rotary line, the bleach-yard sheets, the plank";
        fast.leg("ridge choke", Region.of(-2, 5, 42, 1, 5, 43), Region.of(-11, -2, 7, 2, 8, 44))
            .leg("terrace", Region.of(-6, 1, 69, 8, 1, 73), Region.of(-4, -3, 42, 8, 8, 74))
            .leg("whirligig", Region.of(-12, 1, 95, 2, 1, 96), Region.of(-22, 0, 69, 21, 8, 97))
            .leg("scaffold", Region.of(-12, 1, D_Z1, 0, 1, D_Z1 + 1), Region.of(-12, -3, 93, 2, 8, D_Z1 + 2))
            .leg("island", Region.of(-7, 1, 150, 1, 1, 156), Region.of(-12, -3, D_Z1, 6, 8, 157))
            .leg("bleach yard", Region.of(-10, 1, E1_Z1, 4, 1, E1_Z1 + 1), Region.of(-12, -3, 146, 6, 8, E1_Z1 + 2))
            .leg("plank", Region.of(-2, 1, E2_Z1, 2, 2, E2_Z1 + 1), Region.of(-14, -3, E1_Z1, 14, 8, E2_Z1 + 2))
            .leg("pen", d.pen, Region.of(-16, -3, E2_Z1, 16, 8, FIN_Z + 1));
        Design.Line safe = de.line("SAFE", true, false);
        safe.maxGap = 2;
        safe.description = "own bridge on the right weaving between sheets, over the roof through the wide right ridge gap, then balconies on the right of every street (2-3 blocks of bridging each), the plank, the mill yard";
        safe.leg("right ridge gap", Region.of(11, 5, 42, 18, 5, 43), Region.of(5, -2, 7, 20, 3, 37), Region.of(5, -2, 37, 20, 6, 44))
            .leg("terrace", Region.of(6, 1, 69, 20, 1, 73), Region.of(5, -3, 42, 20, 6, 48), Region.of(5, -3, 48, 20, 3, 74))
            .leg("terrace back", Region.of(8, 1, 95, 20, 1, 96), Region.of(-22, 0, 69, 21, 8, 97))
            .leg("scaffold", Region.of(8, 1, D_Z1, 20, 1, D_Z1 + 1), Region.of(6, -3, 93, 21, 8, D_Z1 + 2))
            .leg("bleach yard", Region.of(8, 1, E1_Z1, 20, 1, E1_Z1 + 1), Region.of(6, -3, D_Z1, 21, 8, E1_Z1 + 2))
            .leg("plank", Region.of(-2, 1, E2_Z1, 2, 2, E2_Z1 + 1), Region.of(-14, -3, E1_Z1, 21, 8, E2_Z1 + 2))
            .leg("pen", d.pen, Region.of(-16, -3, E2_Z1, 16, 8, FIN_Z + 1));
    }

    // ------------------------------------------------------------------ moving parts and checkpoints

    private static MapFeatures FEATURES;

    /** A bedsheet pegged on a line: hangs from the pivot and swings; its hem sweeps head height at the middle of the swing. */
    private static MapFeatures.Mover sheet(String name, char axis, double px, double py, double pz, double halfW, double len,
                                           double amp, int period, int phase, String col) {
        return MapFeatures.Mover.pendulum(name, px, py, pz, axis, amp, period, phase, List.of(
                new MapFeatures.Box(-0.06, -1.6, -0.06, 0.06, 0, 0.06, "chain[axis=y]", false),
                new MapFeatures.Box(axis == 'z' ? -halfW - 0.1 : -0.25, -1.75, axis == 'z' ? -0.25 : -halfW - 0.1,
                        axis == 'z' ? halfW + 0.1 : 0.25, -1.6, axis == 'z' ? 0.25 : halfW + 0.1, "spruce_planks", false),
                new MapFeatures.Box(axis == 'z' ? -halfW : -0.15, -len, axis == 'z' ? -0.15 : -halfW,
                        axis == 'z' ? halfW : 0.15, -1.75, axis == 'z' ? 0.15 : halfW, col, true),
                new MapFeatures.Box(axis == 'z' ? -halfW * 0.7 : -0.17, -len + 1.2, axis == 'z' ? -0.17 : -halfW * 0.7,
                        axis == 'z' ? halfW * 0.7 : 0.17, -len + 1.7, axis == 'z' ? 0.17 : halfW * 0.7, "red_wool", false)));
    }

    /** A rotary washing line: four arms with sheets hanging from them turn about a pole (pivot = top of the pole's collar). */
    private static MapFeatures.Mover whirl(String name, double px, double py, double pz, double r, int period, int phase, int arms) {
        List<MapFeatures.Box> b = new ArrayList<>();
        b.add(new MapFeatures.Box(-0.45, 1.5, -0.45, 0.45, 2.1, 0.45, "stripped_spruce_log", false));
        String[] col = {"white_wool", "pink_wool", "light_blue_wool", "yellow_wool"};
        for (int a = 0; a < arms; a++) {
            String c = col[a % col.length];
            boolean along = (a % 2 == 0);          // 0: +x, 1: +z, 2: -x, 3: -z
            double sgn = a < 2 ? 1 : -1;
            if (arms == 2) { along = true; sgn = a == 0 ? 1 : -1; }
            double a0 = 0.45 * sgn, a1 = r * sgn;
            double lo = Math.min(a0, a1), hi = Math.max(a0, a1);
            double s0 = Math.min(0.9 * sgn, (r - 0.4) * sgn), s1 = Math.max(0.9 * sgn, (r - 0.4) * sgn);
            if (along) {
                b.add(new MapFeatures.Box(lo, 1.7, -0.1, hi, 1.9, 0.1, "spruce_planks", false));
                b.add(new MapFeatures.Box(s0, 0, -0.08, s1, 1.7, 0.08, c, true));
            } else {
                b.add(new MapFeatures.Box(-0.1, 1.7, lo, 0.1, 1.9, hi, "spruce_planks", false));
                b.add(new MapFeatures.Box(-0.08, 0, s0, 0.08, 1.7, s1, c, true));
            }
        }
        return MapFeatures.Mover.rotor(name, px, py, pz, 'y', 0, period, phase, b);
    }

    private static MapFeatures.Mover basket(String name, double px, double py, double pz, double dx, int open, int move, int closed, int phase, boolean fromLeft) {
        double x0 = fromLeft ? 0 : -3.0, x1 = fromLeft ? 3.0 : 0;
        return MapFeatures.Mover.slider(name, px, py, pz, dx, 0, 0, open, move, closed, phase, List.of(
                new MapFeatures.Box(x0, 0, -1.2, x1, 2.2, 1.2, "jungle_planks", true),
                new MapFeatures.Box(x0 + 0.2, 2.2, -1.0, x1 - 0.2, 2.5, 1.0, "white_wool", false),
                new MapFeatures.Box(x0 + 0.3, 0.3, -1.3, x1 - 0.3, 0.7, 1.3, "stripped_jungle_log", false),
                new MapFeatures.Box(x0 + 0.3, 1.4, -1.3, x1 - 0.3, 1.8, 1.3, "stripped_jungle_log", false)));
    }

    public static synchronized MapFeatures feats() {
        if (FEATURES != null) return FEATURES;
        MapFeatures f = new MapFeatures();
        String[] col = {"white_wool", "pink_wool", "light_blue_wool", "yellow_wool", "lime_wool", "orange_wool", "white_wool", "cyan_wool", "light_gray_wool", "magenta_wool"};
        // roof B: a sheet hung in the ridge choke swings fore and aft through the gap
        f.movers.add(sheet("swinging sheet", 'x', 0.0, 10.4, 42.5, 1.75, 4.6, 50, 64, 0, "white_wool"));
        // street 1: over the chimney hops
        f.movers.add(sheet("swinging sheet", 'z', -6.5, 9.5, 22.5, 2.0, 7.5, 52, 66, 0, col[0]));
        f.movers.add(sheet("swinging sheet", 'z', -4.5, 9.5, 31.5, 2.0, 7.5, 52, 66, 33, col[1]));
        // street 2
        f.movers.add(sheet("swinging sheet", 'z', 1.5, 9.5, 59.5, 2.0, 7.5, 52, 66, 12, col[2]));
        f.movers.add(sheet("swinging sheet", 'z', 3.5, 9.5, 64.5, 2.0, 7.5, 52, 66, 45, col[3]));
        // roof C: the whirligig
        f.movers.add(whirl("whirligig", 0.5, 2.0, 83.5, 8.0, 120, 0, 4));
        // street 3
        f.movers.add(sheet("swinging sheet", 'z', -7.5, 9.5, 103.5, 2.0, 7.5, 52, 66, 20, col[4]));
        f.movers.add(sheet("swinging sheet", 'z', -6.5, 9.5, 113.5, 2.0, 7.5, 52, 66, 55, col[5]));
        // street 4: the island's rotary line (two long arms)
        f.movers.add(whirl("island line", -2.5, 1.2, 153.5, 4.6, 84, 0, 2));
        // the long plank
        for (int i = 0; i < 3; i++)
            f.movers.add(sheet("plank sheet", 'z', 0.5, 9.5, 183.5 + 5 * i, 2.5, 7.5, 54, 66, i * 22, col[(6 + i) % col.length]));
        // mill yard: baskets slide across the run and back (the return trip carries you off the edge)
        f.movers.add(basket("sliding basket", -25.0, 1.0, 203.5, 24, 50, 45, 14, 0, true));
        f.movers.add(basket("sliding basket", 25.0, 1.0, 207.5, -24, 50, 45, 14, 78, false));
        f.pushers.add("swinging sheet");
        f.pushers.add("whirligig");
        f.pushers.add("island line");
        f.pushers.add("plank sheet");
        f.pushers.add("sliding basket");
        // the scaffold floor gives way under every step (TNT-run floor): the rim at both ends and the sides stays
        f.crumbles.add(new MapFeatures.Crumble("scaffold floor", Region.of(-19, 0, D_Z1 + 3, 18, 0, D_Z2 - 1), 8, 50, "block.wood.break"));
        // checkpoints at the brawl spots
        f.checkpoints.add(new MapFeatures.Checkpoint(1, "The Ridge", Region.of(-24, 0, B_Z1, 23, 5, B_Z1 + 1), spawns(-20, 20, 1, B_Z1 + 0.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(2, "Drying Terrace", Region.of(-22, 0, C_Z1, 21, 5, C_Z1 + 1), spawns(-20, 20, 1, C_Z1 + 1.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(3, "The Scaffold", Region.of(-22, 0, D_Z1, 21, 5, D_Z1 + 1), spawns(-20, 20, 1, D_Z1 + 1.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(4, "Bleach Yard", Region.of(-22, 0, E1_Z1, 21, 5, E1_Z1 + 1), spawns(-20, 20, 1, E1_Z1 + 1.5), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(5, "Mill Yard", Region.of(-22, 0, E2_Z1, 21, 5, E2_Z1 + 1), spawns(-20, 20, 1, E2_Z1 + 1.5), List.of()));
        FEATURES = f;
        return f;
    }

    private static List<Spawn> spawns(int x1, int x2, double y, double z) {
        List<Spawn> s = new ArrayList<>();
        for (int x = x1; x <= x2; x += 2) s.add(new Spawn(x + 0.5, y, z, 0f));
        return s;
    }

    // ------------------------------------------------------------------ buildings in the lane

    /** Hollow terrace block: facade walls from bottom to top-1 with windows, deck at top, rubble underside. */
    private void block(int x1, int x2, int z1, int z2, int top, int bottom, Pal wall, Pal roof, boolean deckRoof) {
        for (int y = bottom; y < top; y++) {
            for (int x = x1; x <= x2; x++) { facade(x, y, z1, x, y, true); facade(x, y, z2, x, y, true); }
            for (int z = z1; z <= z2; z++) { facade(x1, y, z, z, y, false); facade(x2, y, z, z, y, false); }
        }
        // inner layer: lit rooms / curtains behind the windows, brick elsewhere
        for (int y = bottom; y < top; y++) {
            for (int x = x1 + 1; x <= x2 - 1; x++) { inner(x, y, z1 + 1, g.get(x, y, z1)); inner(x, y, z2 - 1, g.get(x, y, z2)); }
            for (int z = z1 + 1; z <= z2 - 1; z++) { inner(x1 + 1, y, z, g.get(x1, y, z)); inner(x2 - 1, y, z, g.get(x2, y, z)); }
        }
        if (deckRoof) g.fill(x1, top, z1, x2, top, z2, roof);
        // floor slab + rubble underside
        g.fill(x1, bottom - 1, z1, x2, bottom - 1, z2, soot);
        g.underside(x1, z1, x2, z2, bottom - 1, 7, rubble, 11);
        // interior floors (seen through the windows) and storey bands
        for (int y = bottom + 5; y < top - 1; y += 6) g.fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, "spruce_planks");
        for (int y = bottom + 5; y < top; y += 6) {
            for (int x = x1; x <= x2; x++) { g.set(x, y, z1, "stone_bricks"); g.set(x, y, z2, "stone_bricks"); }
            for (int z = z1; z <= z2; z++) { g.set(x1, y, z, "stone_bricks"); g.set(x2, y, z, "stone_bricks"); }
        }
    }

    private void inner(int x, int y, int z, String outer) {
        if (outer != null && outer.contains("glass_pane")) {
            double r = g.rnd(x, y / 2, z, 41);
            g.set(x, y, z, r < 0.22 ? "shroomlight" : r < 0.32 ? "ochre_froglight" : r < 0.6 ? "white_wool" : r < 0.8 ? "brown_wool" : "dark_oak_planks");
        } else g.set(x, y, z, "bricks");
    }

    private void facade(int x, int y, int z, int u, int v, boolean alongX) {
        facade0(x, y, z, u, v);
    }

    private void facade0(int x, int y, int z, int u, int v) {
        int bottom = -24;
        int fl = Math.floorMod(v - bottom, 6);   // storey position
        int col = Math.floorMod(u, 4);
        String s;
        if ((fl == 2 || fl == 3) && col == 1) s = "glass_pane";
        else if (fl == 4 && col == 1) s = "calcite";
        else if (fl == 1 && col == 1) s = "smooth_stone";
        else s = brick.at(x, y, z, g.seed);
        g.set(x, y, z, s);
    }

    private void buildingA() {
        block(-22, 21, -8, 12, 0, -24, brick, deck, true);
        // parapets along the sides of the lane on roof A and the back
        for (int z = -8; z <= 12; z++) { parapet(-22, z); parapet(21, z); }
        for (int x = -22; x <= 21; x++) parapet(x, -8);
        // front cornice under the edge (decor, below the bridge level)
        for (int x = -22; x <= 21; x++) { g.set(x, -1, 12, "stone_bricks"); g.set(x, -2, 12, "polished_deepslate"); }
        // roof clutter behind the start strip (scenery): water tank, stair hut, chimney stacks
        tank(-12, -5);
        hut(6, -6, 12, -3);
        stack(-20, -21 + 1, -6, -5, 0, 6, true);
        stack(16, 17, -6, -5, 0, 5, true);
        // roof A front (z 8..14): skylights and vents break it up
        skylight(-16, 9);
        skylight(9, 9);
        vent(-1, 11);
        vent(-10, 12);
        vent(16, 11);
        g.set(4, 1, 13, "barrel[facing=up]");
        g.set(-6, 1, 9, "barrel[facing=up]");
    }

    private void parapet(int x, int z) {
        g.set(x, 1, z, brick.at(x, 1, z, g.seed));
        if (Math.floorMod(z, 6) == 0) {
            g.set(x, 2, z, "stone_bricks");
            g.set(x, 3, z, "stone_bricks");
            g.set(x, 4, z, (Math.floorMod(z, 12) == 0) ? "lantern[hanging=false,waterlogged=false]" : "flower_pot");
        } else g.set(x, 2, z, "deepslate_tile_slab[type=bottom]");
    }

    private void tank(int x, int z) {
        for (int[] c : new int[][]{{x, z}, {x + 4, z}, {x, z + 4}, {x + 4, z + 4}}) g.fill(c[0], 1, c[1], c[0], 3, c[1], "spruce_fence");
        g.fill(x, 4, z, x + 4, 4, z + 4, "spruce_planks");
        g.cyl(x + 2.5, z + 2.5, 2.6, 5, 9, new Pal("spruce_planks", 60, "stripped_spruce_log[axis=y]", 40));
        g.cyl(x + 2.5, z + 2.5, 1.6, 6, 9, new Pal("air", 1));
        for (int xx = x; xx <= x + 4; xx++) for (int zz = z; zz <= z + 4; zz++) if (!g.air(xx, 9, zz)) g.set(xx, 10, zz, "spruce_slab[type=bottom]");
        g.set(x + 2, 10, z + 2, "spruce_planks");
        g.set(x + 2, 11, z + 2, "spruce_slab[type=bottom]");
    }

    private void hut(int x1, int z1, int x2, int z2) {
        g.walls(x1, 1, z1, x2, 4, z2, stone);
        g.fill(x1, 5, z1, x2, 5, z2, "deepslate_tile_slab[type=bottom]");
        g.set(x1 + 2, 1, z2, "spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]");
        g.set(x1 + 2, 2, z2, "spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]");
        g.set(x1 + 4, 3, z2 + 1, "lantern[hanging=false]");
        g.set(x1 + 4, 2, z2 + 1, "air");
        g.set(x1 + 4, 3, z2, "stone_bricks");
        g.set(x1 + 4, 3, z2 + 1, "wall_torch[facing=south]");
        for (int x = x1 + 1; x < x2; x++) for (int z = z1 + 1; z < z2; z++) for (int y = 1; y <= 4; y++) g.set(x, y, z, null);
    }

    /** Chimney stack from y=from to top; pots on top for scenery stacks. */
    private void stack(int x1, int x2, int z1, int z2, int from, int top, boolean pots) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            for (int y = from; y < top; y++) g.set(x, y, z, brick2.at(x, y, z, g.seed));
            g.set(x, top, z, "stone_bricks");
            if (pots) g.set(x, top + 1, z, (x + z) % 2 == 0 ? "decorated_pot[facing=south,waterlogged=false,cracked=false]" : "flower_pot");
        }
        if (pots) g.set(x1, top + 1, z1, "campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]");
    }

    private void skylight(int x, int z) {
        g.fill(x, 1, z, x + 3, 1, z + 2, "polished_andesite");
        g.fill(x, 2, z, x + 3, 2, z + 2, "glass");
        g.fill(x + 1, 3, z, x + 2, 3, z + 2, "glass");
        g.fill(x + 1, 2, z + 1, x + 2, 2, z + 1, "glass");
    }

    private void vent(int x, int z) {
        g.set(x, 1, z, "polished_deepslate_wall");
        g.set(x, 2, z, "polished_deepslate_wall");
        g.set(x, 3, z, "lightning_rod[facing=up,powered=false,waterlogged=false]");
    }

    private void roofB() {
        int x1 = -24, x2 = 23, z1 = 37, z2 = 48;
        block(x1, x2, z1, z2, 0, -24, brick, deck, false);
        for (int x = x1; x <= x2; x++) {
            g.set(x, 0, z1, "smooth_stone");
            g.set(x, 0, z2, "smooth_stone");
            g.set(x, -1, z1, "stone_bricks");    // flush cornice band (no ledge to stand on)
            g.set(x, -1, z2, "stone_bricks");
            for (int i = 1; i <= 4; i++) {
                int ya = i;                       // ascent from z1 side
                String sa = (x == x1 || x == x2) ? brick.at(x, ya, z1 + i, g.seed) : (i % 2 == 0 ? Gen.st("deepslate_tile", "south", false) : Gen.st("deepslate_tile", "south", false));
                g.set(x, ya, z1 + i, sa);
                g.fill(x, 0, z1 + i, x, ya - 1, z1 + i, slate);
                String sb = (x == x1 || x == x2) ? brick.at(x, ya, z2 - i, g.seed) : Gen.st("deepslate_tile", "north", false);
                g.set(x, ya, z2 - i, sb);
                g.fill(x, 0, z2 - i, x, ya - 1, z2 - i, slate);
            }
            g.fill(x, 0, 42, x, 4, 43, slate);
            g.set(x, 4, 42, "polished_deepslate");
            g.set(x, 4, 43, "polished_deepslate");
        }
        // gable ends rise as parapets
        for (int z = z1; z <= z2; z++) {
            int top = Math.min(z - z1, z2 - z) + 1;
            if (z >= 42 && z <= 43) top = 6;
            for (int y = 1; y <= top + 1; y++) { g.set(x1, y, z, brick.at(x1, y, z, g.seed)); g.set(x2, y, z, brick.at(x2, y, z, g.seed)); }
        }
        // chimney-breast wall on the ridge with three gaps: centre (choke, x -2..1) and two wide ones
        int[][] seg = {{-23, -20}, {-11, -3}, {2, 10}, {19, 22}};
        for (int[] s : seg) {
            for (int x = s[0]; x <= s[1]; x++) for (int z = 42; z <= 43; z++) {
                for (int y = 5; y <= 10; y++) g.set(x, y, z, brick2.at(x, y, z, g.seed));
                g.set(x, 11, z, "stone_brick_slab[type=bottom]");
            }
            // pots along the cap, stone band
            for (int x = s[0]; x <= s[1]; x++) {
                g.set(x, 8, 41, "stone_brick_slab[type=top]");
                g.set(x, 8, 44, "stone_brick_slab[type=top]");
                if ((x & 1) == 0) { g.set(x, 11, 42, "decorated_pot[facing=north,waterlogged=false,cracked=false]"); }
                else g.set(x, 11, 43, "flower_pot");
            }
        }
        // gap jambs: stone quoins and a hanging lantern over the choke
        for (int y = 5; y <= 10; y++) for (int z = 42; z <= 43; z++) {
            g.set(-3, y, z, "stone_bricks"); g.set(2, y, z, "stone_bricks");
            g.set(10, y, z, "stone_bricks"); g.set(19, y, z, "stone_bricks");
            g.set(-11, y, z, "stone_bricks"); g.set(-20, y, z, "stone_bricks");
        }
        for (int x = -2; x <= 1; x++) { g.set(x, 10, 42, "stone_brick_slab[type=top]"); g.set(x, 10, 43, "stone_brick_slab[type=top]"); }
        g.fill(-2, 9, 42, 1, 9, 43, "air");
        // dormers on both pitches (between the gaps) - scenery bumps that also break up the slope
        for (int x : new int[]{-16, -7, 6, 14}) { dormer(x, 39, "north"); dormer(x, 46, "south"); }
    }

    private void dormer(int x, int z, String face) {
        int dz = face.equals("north") ? -1 : 1;
        int y = face.equals("north") ? 3 : 3;
        g.fill(x, 2, z, x + 2, y + 1, z, brick);
        g.set(x + 1, 3, z, "glass_pane");
        g.set(x + 1, 2, z, "glass_pane");
        g.set(x, y + 2, z, Gen.st("deepslate_tile", "east", false));
        g.set(x + 1, y + 2, z, "deepslate_tile_slab[type=bottom]");
        g.set(x + 2, y + 2, z, Gen.st("deepslate_tile", "west", false));
    }

    // ------------------------------------------------------------------ roof C: the drying terrace

    private void roofC() {
        block(-22, 21, C_Z1, C_Z2, 0, -24, brick, deck, true);
        for (int x = -22; x <= 21; x++) {
            g.set(x, -1, C_Z1, "stone_bricks"); g.set(x, -2, C_Z1, "polished_deepslate");
            g.set(x, -1, C_Z2, "stone_bricks"); g.set(x, -2, C_Z2, "polished_deepslate");
        }
        // flower beds along the front (the checkpoint rows stay clear), tank, hut, greenhouse, benches
        bed(-14, 72, -10, 74);
        bed(9, 72, 13, 74);
        greenhouse(11, 80, 17, 87);
        tank(-19, 77);
        hut(-21, 88, -16, 93);
        bench(-12, 84);
        bench(6, 91);
        skylight(-8, 93);
        skylight(15, 74);
        stack(-13, -12, 91, 92, 0, 5, true);
        stack(18, 19, 90, 91, 0, 5, true);
        vent(-17, 74);
        vent(19, 93);
        g.set(-12, 1, 75, "barrel[facing=up,open=false]");
        g.set(-11, 1, 75, "barrel[facing=up,open=false]");
        g.set(-11, 2, 75, "barrel[facing=up,open=false]");
        // the whirligig: pole, collar and a ring of tubs round its foot (the arms are a mover)
        for (int y = 1; y <= 3; y++) g.set(0, y, 83, "spruce_fence");
        g.set(0, 4, 83, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        for (int x = -1; x <= 1; x++) for (int z = 82; z <= 84; z++) if (x != 0 || z != 83) g.set(x, 1, z, (x + z) % 2 == 0 ? "barrel[facing=up,open=false]" : "moss_block");
        // drying lines on the left with a couple of sheets
        for (int z : new int[]{80, 94}) {
            g.set(-21, 1, z, "spruce_fence"); g.set(-21, 2, z, "spruce_fence"); g.set(-21, 3, z, "spruce_fence");
            g.set(-9, 1, z, "spruce_fence"); g.set(-9, 2, z, "spruce_fence"); g.set(-9, 3, z, "spruce_fence");
            for (int x = -20; x <= -10; x++) g.set(x, 3, z, "chain[axis=x]");
            for (int x = -19; x <= -16; x++) g.fill(x, 1, z, x, 2, z, linen);
            for (int x = -14; x <= -12; x++) g.set(x, 2, z, pastel.at(x, 2, z, g.seed));
        }
        bench(8, 76);
    }

    // ------------------------------------------------------------------ roof D: the dye-works scaffold (crumbling floor)

    private void roofD() {
        g.fill(-22, 0, D_Z1, 21, 0, D_Z2, planks);
        // the floor that never crumbles: front rows, back row and the sides; beams and legs under it
        for (int x = -22; x <= 21; x++) {
            for (int z : new int[]{D_Z1, D_Z1 + 1, D_Z1 + 2, D_Z2}) g.set(x, 0, z, z == D_Z1 ? "stripped_spruce_log[axis=x]" : stone.at(x, 0, z, g.seed));
        }
        for (int z = D_Z1; z <= D_Z2; z++) for (int x : new int[]{-22, -21, -20, 19, 20, 21}) g.set(x, 0, z, x == -22 || x == 21 ? "stripped_spruce_log[axis=z]" : stone.at(x, 0, z, g.seed));
        for (int x = -22; x <= 21; x++) { g.set(x, -1, D_Z1, "stripped_dark_oak_log[axis=x]"); g.set(x, -1, D_Z2, "stripped_dark_oak_log[axis=x]"); }
        for (int z = D_Z1; z <= D_Z2; z++) for (int x : new int[]{-22, 21}) g.set(x, -1, z, "stripped_dark_oak_log[axis=z]");
        for (int x = -22; x <= 21; x += 7) for (int z : new int[]{D_Z1, D_Z2}) {
            for (int y = -30; y <= -2; y++) g.set(x, y, z, y % 6 == 0 ? "stripped_spruce_log[axis=y]" : "spruce_fence");
        }
        for (int z = D_Z1 + 7; z < D_Z2; z += 7) for (int x : new int[]{-22, 21}) for (int y = -30; y <= -2; y++) g.set(x, y, z, "spruce_fence");
        // gantry over the floor: posts at the corners, a beam, chains and dye vats on them
        for (int x : new int[]{-22, 21}) for (int z : new int[]{D_Z1 + 1, D_Z2 - 1}) for (int y = 1; y <= 12; y++) g.set(x, y, z, y % 4 == 0 ? "stripped_spruce_log[axis=y]" : "spruce_fence");
        for (int z : new int[]{D_Z1 + 1, D_Z2 - 1}) for (int x = -22; x <= 21; x++) g.set(x, 13, z, "stripped_spruce_log[axis=x]");
        for (int x = -22; x <= 21; x++) g.set(x, 13, (D_Z1 + D_Z2) / 2, "chain[axis=x]");
        String[] dye = {"red_terracotta", "blue_terracotta", "yellow_terracotta", "green_terracotta", "purple_terracotta", "orange_terracotta"};
        for (int i = 0, x = -18; x <= 18; x += 6, i++) {
            for (int y = 9; y <= 12; y++) g.set(x, y, (D_Z1 + D_Z2) / 2, "chain[axis=y]");
            g.set(x, 8, (D_Z1 + D_Z2) / 2, "cauldron");
            g.set(x, 9, (D_Z1 + D_Z2) / 2, "chain[axis=y]");
        }
        // dye vats on the side rim (outside the crumbling field)
        for (int z : new int[]{D_Z1 + 4, D_Z1 + 9}) { g.set(-22, 1, z, "cauldron"); g.set(21, 1, z, "cauldron"); }
        for (int x = -20; x <= 19; x += 8) g.set(x, 1, D_Z2, "lantern[hanging=false,waterlogged=false]");
        // dyed cloth hung from the beam above the back rim (decor, clear of the walking height)
        for (int i = 0, x = -20; x <= 18; x += 4, i++) for (int y = 9; y <= 12; y++) g.set(x, y, D_Z2 - 1, SHIRTS[i % SHIRTS.length]);
    }

    // ------------------------------------------------------------------ roof E1: the bleach yard

    private void roofE1() {
        block(-22, 21, E1_Z1, E1_Z2, 0, -24, brick, deck, true);
        for (int x = -22; x <= 21; x++) {
            g.set(x, -1, E1_Z1, "stone_bricks"); g.set(x, -2, E1_Z1, "polished_deepslate");
        }
        // two rows of pegged sheets (slalom: gaps are in different places; the centre line stays open on the right of x 1)
        int[][] r1 = {{-21, -17}, {-12, -8}, {-6, -3}, {6, 10}, {15, 19}};
        int[][] r2 = {{-14, -10}, {-8, -4}, {4, 8}, {10, 14}};
        sheetRow(169, r1, 0);
        sheetRow(174, r2, 1);
        tank(-21, 171);
        hut(16, 171, 21, 175);
        g.set(-6, 1, 166, "barrel[facing=up,open=false]");
        g.set(5, 1, 166, "barrel[facing=up,open=false]");
        g.set(12, 1, 177, "flower_pot");
        bench(-4, 177);
        vent(-20, 167);
        vent(19, 166);
    }

    private void sheetRow(int z, int[][] segs, int phase) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int[] s : segs) { lo = Math.min(lo, s[0] - 1); hi = Math.max(hi, s[1] + 1); }
        for (int[] s : segs) {
            g.set(s[0] - 1, 1, z, "spruce_fence"); g.set(s[0] - 1, 2, z, "spruce_fence"); g.set(s[0] - 1, 3, z, "spruce_fence"); g.set(s[0] - 1, 4, z, "spruce_fence");
            g.set(s[1] + 1, 1, z, "spruce_fence"); g.set(s[1] + 1, 2, z, "spruce_fence"); g.set(s[1] + 1, 3, z, "spruce_fence"); g.set(s[1] + 1, 4, z, "spruce_fence");
            for (int x = s[0] - 1; x <= s[1] + 1; x++) g.set(x, 5, z, "chain[axis=x]");
            for (int x = s[0]; x <= s[1]; x++) for (int y = 2; y <= 4; y++) g.set(x, y, z, SHIRTS[Math.floorMod(s[0] + phase, SHIRTS.length)].replace("red_wool", "white_wool"));
            for (int x = s[0]; x <= s[1]; x++) g.set(x, 1, z, "air");
        }
    }

    // ------------------------------------------------------------------ the long plank (street over the void)

    private void plank() {
        for (int z = PL_Z1; z <= PL_Z2; z++) for (int x = -2; x <= 2; x++) {
            g.set(x, 0, z, "stripped_spruce_log[axis=z]");
            // stairs up from the yard, then a flat plank with one raised joint half way (breaks the free sprint)
            g.set(x, 1, z, z == PL_Z1 ? Gen.st("spruce", "south", false) : (x == -2 || x == 2) ? "stripped_spruce_log[axis=z]" : planks.at(x, 0, z, g.seed));
            if (z == 187) g.set(x, 2, z, "spruce_slab[type=bottom,waterlogged=false]");
        }
        for (int z = PL_Z1 + 1; z <= PL_Z2; z += 3) for (int x = -3; x <= 3; x++) g.set(x, -1, z, "stripped_dark_oak_log[axis=x]");
        for (int z : new int[]{182, 188, 194}) for (int x : new int[]{-3, 3}) for (int y = -30; y <= -2; y++) g.set(x, y, z, y % 6 == 0 ? "stripped_spruce_log[axis=y]" : "spruce_fence");
        // overhead lines for the swinging sheets (pivot lines), posts on both sides, laundry on the lines out of the way
        for (int i = 0; i < 3; i++) {
            int z = 183 + 5 * i;
            lineX(-9, 9, z, 9);
            post(-9, z, 9, false);
            post(9, z, 9, false);
            for (int x = -8; x <= -4; x++) for (int y = 5; y <= 8; y++) g.set(x, y, z, SHIRTS[(i * 3 + 1) % SHIRTS.length]);
            for (int x = 4; x <= 8; x++) for (int y = 5; y <= 8; y++) g.set(x, y, z, SHIRTS[(i * 3 + 4) % SHIRTS.length]);
        }
        // lamps on short posts at the plank ends
        for (int x : new int[]{-3, 3}) { g.set(x, 0, PL_Z1, "stripped_spruce_log[axis=y]"); g.set(x, 1, PL_Z1, "iron_bars"); g.set(x, 2, PL_Z1, "iron_bars"); g.set(x, 3, PL_Z1, "lantern[hanging=false,waterlogged=false]"); }
        for (int x : new int[]{-3, 3}) { g.set(x, 3, PL_Z2, "iron_bars"); g.set(x, 4, PL_Z2, "iron_bars"); g.set(x, 5, PL_Z2, "lantern[hanging=false,waterlogged=false]"); }
    }

    // ------------------------------------------------------------------ roof E2: the mill yard and the finish

    private void roofE2() {
        block(-22, 21, E2_Z1, E2_Z2, 0, -24, brick, deck, true);
        for (int x = -22; x <= 21; x++) { g.set(x, -1, E2_Z1, "stone_bricks"); g.set(x, -2, E2_Z1, "polished_deepslate"); }
        // chimney stacks and vents at the sides (the baskets run between them), flower tubs, lanterns
        stack(-21, -20, 200, 201, 0, 5, true);
        stack(20, 21, 205, 206, 0, 5, true);
        skylight(-20, 207);
        skylight(17, 199);
        vent(-12, 199);
        vent(12, 208);
        for (int z = 199; z <= 209; z += 5) { g.set(-22, 1, z, "lantern[hanging=false,waterlogged=false]"); g.set(21, 1, z, "lantern[hanging=false,waterlogged=false]"); }
        // the basket rails: a washing pole each side, a chain across the top (decor, well above heads)
        for (int z : new int[]{203, 207}) {
            for (int y = 1; y <= 8; y++) { g.set(-22, y, z, "spruce_fence"); g.set(21, y, z, "spruce_fence"); }
            for (int x = -22; x <= 21; x++) g.set(x, 9, z, "chain[axis=x]");
        }
        for (int x = -20; x <= 20; x += 5) for (int y = 6; y <= 8; y++) { g.set(x, y, 203, SHIRTS[Math.floorMod(x, 8)]); g.set(x + 1, y, 207, SHIRTS[Math.floorMod(x + 3, 8)]); }
        g.set(-8, 1, 199, "barrel[facing=up,open=false]");
        g.set(9, 1, 200, "composter[level=5]");
    }

    // ------------------------------------------------------------------ streets 3 and 4

    /** A balcony: stone slab at y=top on iron brackets, flower tubs on the outer corners, a striped awning over it. */
    private void balcony(int x1, int x2, int z1, int z2, int top) {
        g.fill(x1, top, z1, x2, top, z2, stone);
        for (int x = x1; x <= x2; x++) { g.set(x, top - 1, z1, "stone_bricks"); g.set(x, top - 1, z2, "stone_bricks"); }
        for (int[] c : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) for (int y = -30; y < top - 1; y++) g.set(c[0], y, c[1], y < -26 ? "polished_deepslate" : "iron_bars");
        g.set(x2, top + 1, z1, "flower_pot");
        g.set(x1, top + 1, z2, "potted_azalea_bush");
        for (int x = x1 - 1; x <= x2 + 1; x++) for (int z = z1 - 1; z <= z2 + 1; z++) g.set(x, top + 6, z, ((x + z) & 1) == 0 ? "red_wool" : "white_wool");
        for (int y = top + 1; y <= top + 5; y++) { g.set(x1 - 1, y, z1 - 1, "spruce_fence"); g.set(x2 + 1, y, z2 + 1, "spruce_fence"); }
    }

    private void street3() {
        // FAST: stacks left of centre (tops 0, 1, 0, 1), gaps 3
        stack(-9, -8, 100, 101, -30, 0, false);
        stack(-7, -6, 105, 106, -30, 1, false);
        stack(-8, -7, 110, 111, -30, 0, false);
        stack(-7, -6, 115, 116, -30, 1, false);
        stack(-27, -26, 104, 105, -30, 3, true);
        stack(25, 26, 112, 113, -30, 4, true);
        // SAFE: balconies on the right
        balcony(10, 13, 99, 101, 0);
        balcony(9, 12, 106, 108, 0);
        balcony(11, 14, 114, 116, 0);
        balcony(10, 13, 118, 119, 0);
        post(-21, 100, 6, true);
        post(20, 100, 6, true);
        post(-21, 112, 6, true);
        post(20, 112, 6, true);
        // lines for the two swinging sheets, laundry along them
        for (int z : new int[]{103, 113}) {
            lineX(-22, 21, z, 9);
            post(-22, z, 9, false);
            post(21, z, 9, false);
        }
        lineX(-20, 20, 109, 6);
        sheetX(-19, -15, 109, 1, 6, pastel);
        sheetX(3, 6, 109, 2, 6, linen);
        sheetX(16, 19, 109, 1, 6, linen);
        lineX(-20, 20, 118, 6);
        sheetX(-18, -14, 118, 1, 6, linen);
        sheetX(-3, 0, 118, 3, 6, pastel);
        sheetX(8, 11, 118, 1, 6, pastel);
        post(-21, 109, 6, false);
        post(20, 109, 6, false);
        post(-21, 118, 6, false);
        post(20, 118, 6, false);
    }

    private void street4() {
        stack(-4, -3, 139, 140, -30, 0, false);
        stack(-5, -4, 144, 145, -30, 1, false);
        stack(-3, -2, 160, 161, -30, 1, false);
        island();
        stack(-27, -26, 146, 147, -30, 4, true);
        stack(25, 26, 155, 156, -30, 3, true);
        balcony(9, 12, 139, 141, 0);
        balcony(8, 11, 146, 148, 0);
        balcony(10, 13, 153, 155, 0);
        balcony(9, 12, 160, 162, 0);
        post(-21, 141, 6, true);
        post(20, 141, 6, true);
        post(-21, 158, 6, true);
        post(20, 158, 6, true);
        lineX(-20, 20, 150, 7);
        sheetX(-19, -15, 150, 2, 7, linen);
        sheetX(14, 19, 150, 2, 7, pastel);
        lineX(-20, 20, 143, 6);
        sheetX(-19, -16, 143, 1, 6, pastel);
        sheetX(5, 8, 143, 1, 6, linen);
        lineX(-20, 20, 157, 6);
        sheetX(-18, -14, 157, 1, 6, pastel);
        sheetX(2, 6, 157, 1, 6, linen);
        post(-21, 143, 6, false);
        post(20, 143, 6, false);
        post(-21, 150, 7, false);
        post(20, 150, 7, false);
        post(-21, 157, 6, false);
        post(20, 157, 6, false);
    }

    /** The free-standing island of street 4: a chimney-top platform (x -7..1, z 150..156) with the rotary line's pole in the middle. */
    private void island() {
        stack(-5, -1, 151, 155, -30, -1, false);
        g.fill(-7, 0, 150, 1, 0, 156, deck);
        for (int x = -7; x <= 1; x++) { g.set(x, -1, 150, "stone_bricks"); g.set(x, -1, 156, "stone_bricks"); }
        for (int z = 150; z <= 156; z++) { g.set(-7, -1, z, "stone_bricks"); g.set(1, -1, z, "stone_bricks"); }
        for (int y = 1; y <= 3; y++) g.set(-3, y, 153, "spruce_fence");
        g.set(-3, 4, 153, "lantern[hanging=false,waterlogged=false]");
        for (int[] c : new int[][]{{-7, 150}, {1, 150}, {-7, 156}, {1, 156}}) g.set(c[0], 1, c[1], "barrel[facing=up,open=false]");
    }

    /** Bunting over the streets: a chain from wall to wall with coloured pennants. */
    private void bunting() {
        for (int z : new int[]{18, 26, 55, 62, 100, 108, 117, 142, 154, 161}) {
            for (int x = -26; x <= 25; x++) {
                if (g.air(x, 14, z)) g.set(x, 14, z, "chain[axis=x]");
                if (x % 2 == 0 && g.air(x, 13, z)) g.set(x, 13, z, SHIRTS[Math.floorMod(x / 2 + z, SHIRTS.length)]);
            }
        }
    }

    private void bed(int x1, int z1, int x2, int z2) {
        g.fill(x1, 1, z1, x2, 1, z2, "spruce_planks");
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            g.set(x, 1, z, edge ? "stripped_spruce_log[axis=" + (x == x1 || x == x2 ? "z" : "x") + "]" : "moss_block");
            if (!edge) {
                double r = g.rnd(x, 2, z);
                g.set(x, 2, z, r < 0.35 ? "flowering_azalea" : r < 0.6 ? "azalea" : r < 0.8 ? "red_tulip" : "cornflower");
            }
        }
    }

    private void greenhouse(int x1, int z1, int x2, int z2) {
        g.fill(x1, 1, z1, x2, 1, z2, "polished_andesite");
        for (int y = 2; y <= 4; y++) {
            for (int x = x1; x <= x2; x++) { g.set(x, y, z1, "glass_pane"); g.set(x, y, z2, "glass_pane"); }
            for (int z = z1; z <= z2; z++) { g.set(x1, y, z, "glass_pane"); g.set(x2, y, z, "glass_pane"); }
        }
        for (int x = x1; x <= x2; x += 3) for (int y = 2; y <= 4; y++) { g.set(x, y, z1, "spruce_fence"); g.set(x, y, z2, "spruce_fence"); }
        g.fill(x1, 5, z1, x2, 5, z2, "glass");
        g.fill(x1 + 1, 6, z1 + 1, x2 - 1, 6, z2 - 1, "glass");
        for (int x = x1 + 1; x < x2; x++) for (int z = z1 + 1; z < z2; z++) g.set(x, 2, z, g.rnd(x, 2, z) < 0.5 ? "potted_fern" : "air");
        g.set(x1, 2, z1 + 3, "air");
        g.set(x1, 3, z1 + 3, "air");
    }

    private void bench(int x, int z) {
        g.set(x, 1, z, Gen.st("spruce", "north", false));
        g.set(x + 1, 1, z, Gen.st("spruce", "north", false));
        g.set(x + 2, 1, z, Gen.st("spruce", "north", false));
        g.set(x - 1, 1, z, "spruce_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]");
        g.set(x + 3, 1, z, "spruce_trapdoor[facing=east,half=bottom,open=true,powered=false,waterlogged=false]");
    }

    // ------------------------------------------------------------------ the streets (gaps)

    /** Sheet hanging along x at row z, from the line at lineY down to hem (inclusive). */
    private void sheetX(int x1, int x2, int z, int hem, int lineY, Pal p) {
        for (int x = x1; x <= x2; x++) {
            int h = hem + (int) Math.round(g.rnd(x, 0, z, 77) * 0.6) * ((x == x1 || x == x2) ? 1 : 0);
            for (int y = h; y < lineY; y++) g.set(x, y, z, p.at(x, y, z, g.seed));
        }
        g.set(x1, lineY - 1, z, "white_wool");
    }

    private void lineX(int x1, int x2, int z, int y) { for (int x = x1; x <= x2; x++) if (g.air(x, y, z)) g.set(x, y, z, "chain[axis=x]"); }

    private void lineZ(int x, int z1, int z2, int y) { for (int z = z1; z <= z2; z++) if (g.air(x, y, z)) g.set(x, y, z, "chain[axis=z]"); }

    /** Iron street lamp post rising from the street far below to the top y, with a lantern arm. */
    private void post(int x, int z, int top, boolean lamp) {
        for (int y = -30; y <= top; y++) g.set(x, y, z, y < -26 ? "polished_deepslate" : "iron_bars");
        g.set(x, -31, z, "polished_deepslate_wall");
        g.set(x, top + 1, z, "polished_deepslate");
        if (lamp) {
            g.set(x, top + 2, z, "lantern[hanging=false,waterlogged=false]");
        }
    }

    private void street1() {
        // FAST: chimney stacks left of centre (top y = standing block)
        stack(-6, -5, 15, 16, -30, 0, false);
        stack(-7, -6, 20, 21, -30, 0, false);
        stack(-5, -4, 24, 25, -30, 1, false);
        stack(-5, -4, 29, 30, -30, 1, false);
        stack(-3, -2, 33, 34, -30, 0, false);
        // scenery chimneys further out (pots, not on a line)
        stack(-27, -26, 24, 25, -30, 3, true);
        stack(25, 26, 30, 31, -30, 4, true);
        // posts that carry the lines (iron, from below)
        post(-11, 15, 5, false);
        post(-11, 36, 5, false);
        post(-2, 21, 5, false);
        post(-2, 29, 5, false);
        post(-21, 21, 6, true);
        post(20, 21, 6, true);
        post(20, 29, 6, true);
        // line along z on the left (separates the chimney line from the far left)
        sheetZ(-11, 17, 22, 1, 5, linen);
        sheetZ(-11, 27, 33, 1, 5, pastel);
        // cross lines
        lineX(-1, 19, 21, 5);
        lineX(-20, -12, 21, 6);
        sheetX(0, 3, 21, 1, 5, linen);
        sheetX(7, 10, 21, 1, 5, pastel);
        sheetX(14, 17, 21, 1, 5, linen);
        sheetX(18, 19, 21, 3, 5, pastel);                 // towels: pass underneath
        sheetX(-19, -15, 21, 2, 6, linen);
        lineX(-1, 19, 29, 5);
        sheetX(-1, 1, 29, 1, 5, pastel);
        sheetX(5, 8, 29, 3, 5, linen);                    // towel
        sheetX(9, 12, 29, 1, 5, linen);
        sheetX(16, 19, 29, 1, 5, pastel);
        lineX(3, 19, 25, 5);
        sheetX(4, 6, 25, 1, 5, linen);
        sheetX(11, 15, 25, 1, 5, pastel);
        lineX(5, 20, 33, 5);
        sheetX(6, 9, 33, 1, 5, linen);
        sheetX(13, 16, 33, 1, 5, linen);
        sheetX(18, 20, 33, 1, 5, pastel);
        post(3, 25, 5, false);
        post(5, 33, 5, false);
        // head-hitter towel over the flat 3-gap of the chimney line (feet 2, hem 5)
        lineX(-10, -3, 27, 6);
        sheetX(-6, -4, 27, 5, 6, pastel);
        post(-10, 27, 5, false);
        // pivot lines of the two swinging sheets over the chimney hops
        for (int z : new int[]{22, 31}) { lineX(-22, 21, z, 9); post(-22, z, 9, false); post(21, z, 9, false); }
    }

    private void sheetZ(int x, int z1, int z2, int hem, int lineY, Pal p) {
        for (int z = z1; z <= z2; z++) for (int y = hem; y < lineY; y++) g.set(x, y, z, p.at(x, y, z, g.seed));
        lineZ(x, z1, z2, lineY);
    }

    private void street2() {
        // FAST: chimney stacks right of centre, with the 4-gap between T2 and T3
        stack(0, 1, 51, 52, -30, -1, false);
        stack(1, 2, 56, 57, -30, -1, false);
        stack(2, 3, 62, 63, -30, -1, false);
        stack(4, 5, 66, 67, -30, 0, false);
        stack(-27, -26, 58, 59, -30, 4, true);
        stack(25, 26, 65, 66, -30, 3, true);
        post(-21, 54, 6, true);
        post(20, 54, 6, true);
        post(-21, 62, 6, true);
        post(20, 62, 6, true);
        post(-6, 58, 4, true);                            // street lamp in the middle of the centre lane
        lineX(-20, 19, 54, 6);
        sheetX(-20, -16, 54, 1, 6, linen);
        sheetX(-12, -8, 54, 1, 6, pastel);
        sheetX(-5, -3, 54, 1, 6, linen);
        sheetX(-1, 2, 54, 3, 6, pastel);                  // head-hitter towel over the chimney 3-gap (feet 0)
        sheetX(7, 10, 54, 1, 6, linen);
        sheetX(14, 18, 54, 1, 6, pastel);
        lineX(-20, 19, 60, 6);
        sheetX(-18, -14, 60, 1, 6, pastel);
        sheetX(-10, -7, 60, 1, 6, linen);
        sheetX(9, 13, 60, 1, 6, linen);
        sheetX(16, 19, 60, 2, 6, pastel);
        lineX(8, 19, 57, 5);
        sheetX(11, 15, 57, 1, 5, linen);
        post(8, 57, 5, false);
        lineX(5, 19, 65, 5);
        sheetX(6, 9, 65, 1, 5, pastel);
        sheetX(14, 17, 65, 1, 5, linen);
        post(5, 64, 5, false);
        post(-11, 49, 5, false);
        post(-11, 68, 5, false);
        sheetZ(-11, 50, 52, 2, 6, linen);
        sheetZ(-11, 63, 66, 1, 6, pastel);
        // pivot lines of the two swinging sheets over the chimney hops
        for (int z : new int[]{59, 64}) { lineX(-22, 21, z, 9); post(-22, z, 9, false); post(21, z, 9, false); }
    }

    // ------------------------------------------------------------------ scenery

    private void neighbours() {
        // {z1, z2, touching}: row of houses beside each roof; the new roofs stand free (5 blocks of void to each side)
        int[][] rows = {{-8, 12, 0}, {37, 48, 1}, {C_Z1, C_Z2, 0}, {D_Z1, D_Z2, 0}, {E1_Z1, E1_Z2, 0}, {E2_Z1, E2_Z2, 0}};
        for (int r = 0; r < rows.length; r++) {
            int z1 = rows[r][0], z2 = rows[r][1];
            for (int side = -1; side <= 1; side += 2) {
                int x = side < 0 ? -27 : 26;
                if (rows[r][2] == 1) x = side < 0 ? -25 : 24;
                int i = 0;
                while (Math.abs(x) < 46) {
                    int w = 6 + (int) (g.rnd(i, r, side, 5) * 5);
                    int xa = side < 0 ? x - w + 1 : x, xb = side < 0 ? x : x + w - 1;
                    int top = 2 + (int) (g.rnd(i, r, side, 6) * 9) + (Math.abs(x) < 35 ? 3 : 0);
                    house(xa, xb, z1, z2, top, i + r * 10 + (side + 1) * 100, Math.abs(x) < 34 ? -8 : top - 6);
                    x += side * (w + (g.rnd(i, r, side, 9) < 0.2 ? 1 : 0));
                    i++;
                }
            }
        }
        // back rows behind the start and beyond the goal (massing only)
        for (int z : new int[]{-26, 238}) {
            int x = -64;
            int i = 0;
            while (x < 64) {
                int w = 7 + (int) (g.rnd(i, z, 0, 15) * 6);
                int top = -2 + (int) (g.rnd(i, z, 1, 15) * 10) + (z > 100 ? 6 : 0);
                house(x, x + w - 1, z, z + 11, top, i + z * 7, top - 5);
                x += w;
                i++;
            }
        }
    }

    /** Scenery house: hollow brick box with windows, a flat or pitched slate roof and chimneys. */
    private void house(int x1, int x2, int z1, int z2, int top, int salt, int bottom) {
        Pal w = g.rnd(salt, 1, 1, 3) < 0.25 ? brick2 : g.rnd(salt, 1, 2, 3) < 0.2 ? stone : brick;
        for (int y = bottom; y < top; y++) {
            for (int x = x1; x <= x2; x++) { wallCell(x, y, z1, x, w); wallCell(x, y, z2, x, w); }
            for (int z = z1; z <= z2; z++) { wallCell(x1, y, z, z, w); wallCell(x2, y, z, z, w); }
        }
        g.fill(x1, bottom - 1, z1, x2, bottom - 1, z2, soot);
        if (bottom < -6) g.underside(x1, z1, x2, z2, bottom - 1, 3, rubble, salt);
        boolean pitched = g.rnd(salt, 2, 2, 3) < 0.6;
        if (pitched) {
            // ridge along x (over the depth z)
            int depth = z2 - z1 + 1, half = depth / 2;
            for (int x = x1; x <= x2; x++) for (int i = 0; i < half; i++) {
                int y = top + i / 2;
                boolean stairRow = i % 2 == 0;
                g.set(x, y, z1 + i, stairRow ? Gen.st("deepslate_tile", "south", false) : "deepslate_tiles");
                g.set(x, y, z2 - i, stairRow ? Gen.st("deepslate_tile", "north", false) : "deepslate_tiles");
                if (i > 0) { g.fill(x, top, z1 + i, x, y - 1, z1 + i, "bricks"); g.fill(x, top, z2 - i, x, y - 1, z2 - i, "bricks"); }
            }
            int ry = top + (half - 1) / 2;
            for (int x = x1; x <= x2; x++) for (int z = z1 + half; z <= z2 - half; z++) g.set(x, ry, z, "polished_deepslate");
        } else {
            g.fill(x1, top, z1, x2, top, z2, deck);
            for (int x = x1; x <= x2; x++) { g.set(x, top + 1, z1, "deepslate_tile_slab[type=bottom]"); g.set(x, top + 1, z2, "deepslate_tile_slab[type=bottom]"); }
        }
        // chimneys on the ridge line
        int cz = (z1 + z2) / 2;
        int ch = top + 3 + (int) (g.rnd(salt, 3, 3, 3) * 3);
        if (g.rnd(salt, 4, 4, 3) < 0.8) stack(x1 + 1, x1 + 2, cz, cz + 1, top, ch, true);
        if (x2 - x1 > 8 && g.rnd(salt, 5, 5, 3) < 0.6) stack(x2 - 2, x2 - 1, cz, cz + 1, top, ch - 1, true);
        if (g.rnd(salt, 6, 6, 3) < 0.35) g.set(x1 + 3, top + 1, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        // washing out of the windows: a pole and a few sheets on the street-facing side of the taller houses
        if (top > 6 && g.rnd(salt, 7, 7, 3) < 0.6) {
            int x = x1 + 2;
            for (int y = top - 5; y <= top - 2; y++) g.set(x, y, z1 - 1, SHIRTS[Math.floorMod(salt + y, SHIRTS.length)]);
        }
    }

    private void wallCell(int x, int y, int z, int u, Pal w) {
        int fl = Math.floorMod(y + 22, 6);
        int col = Math.floorMod(u, 4);
        String s;
        if ((fl == 2 || fl == 3) && col == 1) s = "glass_pane";
        else if (fl == 4 && col == 1) s = "calcite";
        else if (fl == 1 && col == 1) s = "smooth_stone";
        else if (fl == 5) s = "stone_bricks";
        else s = w.at(x, y, z, g.seed);
        g.set(x, y, z, s);
    }

    private void skyline() {
        // church spire far to the right of the finish
        int cx = 62, cz = 190;
        g.fill(cx - 4, -20, cz - 4, cx + 4, 22, cz + 4, stone);
        g.fill(cx - 3, 23, cz - 3, cx + 3, 26, cz + 3, "stone_bricks");
        for (int y = 27; y <= 52; y++) {
            double r = 3.6 * (1 - (y - 27) / 26.0);
            g.cyl(cx + 0.5, cz + 0.5, Math.max(0.6, r), y, y, slate);
        }
        g.set(cx, 53, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        for (int y = 14; y <= 18; y++) g.set(cx, y, cz - 5, y == 16 ? "glowstone" : "calcite");
        // gasholder frame on the left
        double gx = -62.5, gz = 150.5;
        g.ring(gx, gz, 11, 1, -10, 18, new Pal("air", 7, "iron_bars", 3));
        for (int a = 0; a < 12; a++) {
            double t = a * Math.PI / 6;
            int x = (int) Math.floor(gx + 10.5 * Math.cos(t)), z = (int) Math.floor(gz + 10.5 * Math.sin(t));
            g.fill(x, -14, z, x, 20, z, "polished_deepslate_wall");
            g.set(x, 21, z, "lantern[hanging=false,waterlogged=false]");
        }
        g.cyl(gx, gz, 10.2, 6, 6, new Pal("deepslate_tiles", 1));
        // a few factory chimneys far out
        for (int[] c : new int[][]{{-78, 60}, {78, 30}, {-78, 200}, {82, 120}}) {
            g.cyl(c[0] + 0.5, c[1] + 0.5, 2.2, -20, 34, brick2);
            g.ring(c[0] + 0.5, c[1] + 0.5, 2.6, 1, 30, 31, new Pal("stone_bricks", 1));
        }
    }

    // ------------------------------------------------------------------ start, finish, landmark

    private void startStrip(TgMapData d) {
        for (int x = -18; x <= 17; x++) for (int z = 0; z <= 6; z++)
            g.set(x, 0, z, ((x + z) & 1) == 0 ? "polished_blackstone" : "polished_deepslate");
        for (int x = -18; x <= 17; x++) {
            g.set(x, 0, 7, (x & 1) == 0 ? "smooth_quartz" : "polished_blackstone");
            for (int y = 1; y <= 3; y++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new Pos(x, y, 7)); }
        }
        for (int z = -1; z <= 7; z++) for (int y = 1; y <= 3; y++) {
            g.set(-19, y, z, y == 3 ? "deepslate_tile_slab[type=bottom]" : brick.at(-19, y, z, g.seed));
            g.set(18, y, z, y == 3 ? "deepslate_tile_slab[type=bottom]" : brick.at(18, y, z, g.seed));
        }
        for (int z = -1; z <= 7; z++) { g.set(-19, 3, z, "bricks"); g.set(18, 3, z, "bricks"); g.set(-19, 4, z, "deepslate_tile_slab[type=bottom]"); g.set(18, 4, z, "deepslate_tile_slab[type=bottom]"); }
        for (int x = -19; x <= 18; x++) for (int y = 1; y <= 3; y++) g.set(x, y, -1, y == 3 ? "stone_bricks" : brick.at(x, y, -1, g.seed));
        for (int x = -19; x <= 18; x++) g.set(x, 4, -1, "deepslate_tile_slab[type=bottom]");
        // lanterns on the back wall
        for (int x = -16; x <= 16; x += 8) g.set(x, 4, -1, "lantern[hanging=false,waterlogged=false]");
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        for (int row = 0; row < 3; row++)
            for (int i = 0; i < 20; i++) d.startSpawns.add(new Spawn(-17.5 + i * 1.8 + (row % 2) * 0.6, 1, 1.5 + row * 2, 0f));
    }

    /** Feedback 3: the finish is a row of goal chickens across the whole width of the mill yard, under the clock tower. */
    private void pen(TgMapData d) {
        com.mcm.games.tgttos.model.FinishRow.build(d, -15, 15, 0, FIN_Z, 1, "polished_deepslate");
    }

    private void tower(TgMapData d, Design de) {
        int zc = TOWER_Z, z1 = zc - 5, z2 = zc + 5, x1 = -5, x2 = 5;
        // the clock house: its own roof behind the finish board
        block(-9, 9, z1 - 2, z2 + 3, 0, -24, brick, deck, true);
        for (int y = 1; y <= 30; y++) for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            if (!edge) continue;
            boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
            g.set(x, y, z, corner ? "stone_bricks" : (y % 7 == 0 ? "stone_bricks" : brick.at(x, y, z, g.seed)));
        }
        // clock face toward the start
        for (int x = -3; x <= 3; x++) for (int y = 17; y <= 23; y++) {
            double dx = x, dy = y - 20;
            double rr = Math.sqrt(dx * dx + dy * dy);
            if (rr <= 3.4) g.set(x, y, z1, rr > 2.6 ? "polished_deepslate" : "smooth_quartz");
        }
        g.set(0, 20, z1 - 1, "polished_deepslate_wall");
        g.set(0, 21, z1 - 1, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        g.set(1, 20, z1 - 1, "lightning_rod[facing=east,powered=false,waterlogged=false]");
        g.set(0, 20, z1, "gold_block");
        // windows lit
        for (int y = 4; y <= 10; y += 6) for (int x = -2; x <= 2; x += 2) { g.set(x, y, z1, "glass_pane"); g.set(x, y + 1, z1, "glass_pane"); g.set(x, y, z1 + 1, "glowstone"); g.set(x, y + 1, z1 + 1, "glowstone"); }
        // cornice + copper cupola
        for (int x = x1 - 1; x <= x2 + 1; x++) for (int z = z1 - 1; z <= z2 + 1; z++) {
            boolean edge = x == x1 - 1 || x == x2 + 1 || z == z1 - 1 || z == z2 + 1;
            g.set(x, 31, z, edge ? "stone_brick_slab[type=bottom]" : "stone_bricks");
        }
        for (int y = 32; y <= 39; y++) {
            double r = 5.2 - (y - 32) * 0.62;
            g.cyl(0.5, zc + 0.5, r, y, y, new Pal("oxidized_cut_copper", 50, "weathered_cut_copper", 30, "oxidized_copper", 20));
        }
        // open belfry: arches through the cupola base
        g.fill(-4, 32, zc, 4, 33, zc, "air");
        g.fill(0, 32, zc - 4, 0, 33, zc + 4, "air");
        g.set(0, 34, zc, "chain[axis=y]");
        g.set(0, 33, zc, "bell[attachment=ceiling,facing=north,powered=false]");
        // vane pole and the golden cockerel (profile faces the start)
        for (int y = 40; y <= 46; y++) g.set(0, y, zc, "polished_deepslate_wall");
        for (int x = -3; x <= 3; x++) g.set(x, 43, zc, "chain[axis=x]");
        for (int z = zc - 3; z <= zc + 3; z++) if (z != zc) g.set(0, 43, z, "chain[axis=z]");
        String[] cock = {
                "....RR....",
                "...RGGR...",
                "...GGGGO..",
                "GG.GGGG...",
                "GGGGGGGG..",
                ".GGGGGGG..",
                "..GGGGG...",
                "....G.....",
                "...GGG...."};
        int top = 56;
        for (int r = 0; r < cock.length; r++) for (int c = 0; c < cock[r].length(); c++) {
            char ch = cock[r].charAt(c);
            if (ch == '.') continue;
            int x = 5 - c, y = top - r;
            g.set(x, y, zc, ch == 'R' ? "red_terracotta" : ch == 'O' ? "orange_terracotta" : "gold_block");
            if (ch == 'G' && r == 4) de.landmark.add(new Pos(x, y, zc));
        }
        for (int y = 47; y <= 48; y++) g.set(0, y, zc, "polished_deepslate_wall");
        de.landmark.add(new Pos(0, 39, zc));
        de.landmark.add(new Pos(0, 20, z1));
    }
}
