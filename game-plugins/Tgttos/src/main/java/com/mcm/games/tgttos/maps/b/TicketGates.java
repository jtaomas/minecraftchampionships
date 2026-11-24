package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Noise.Pal;
import com.mcm.games.tgttos.maps.b.util.Stairs;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapFeatures;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.List;

/**
 * Ticket Gates (doors / choice, indoor). A station concourse: weave through ticket machines and queue rails to a
 * wall of 12 turnstiles hidden behind red flaps. Each round every turnstile is OPEN, JAMMED (blocked one block in)
 * or a DROP (floor gives way into the service tunnel, which rejoins by the side stairs). The always-open staff
 * corridor round the left wall is the safe, slow line. Then two parked trains: walk through the offset carriage
 * doors (safe) or jump the coupling gaps over the live track pit (fast, 5-wide crowd choke, a fall = respawn).
 * Goal: the clock plaza pen.
 *
 * <p>Local frame: floor y = 0, hall interior x -21..21, start strip x -18..17 / z 0..6, course towards +Z.
 */
public final class TicketGates implements TgMap {

    @Override public String id() { return "ticket_gates"; }
    @Override public String displayName() { return "Ticket Gates"; }
    @Override public MapType type() { return MapType.DOORS; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }

    /** Coupling anvil: creaks 0.4 s after a racer lands on it, then drops (with its iron-bar stand) into the live pit; back 3 s later. */
    public static final int TRAP_DELAY = 8, TRAP_RESTORE = 60;
    private static MapFeatures FEATURES;

    @Override public MapFeatures features() { return feats(); }

    public static synchronized MapFeatures feats() {
        if (FEATURES != null) return FEATURES;
        MapFeatures f = new MapFeatures();
        int[][] cz = {{C1, T1}, {C2, T2}, {C3, T3}};
        for (int i = 0; i < cz.length; i++) {
            int cx = cz[i][0], z = cz[i][1] + 2;
            f.traps.add(new MapFeatures.Trap("track " + (i + 1) + " coupling anvil", Region.of(cx, 0, z, cx, 1, z),
                    List.of(new TgMapData.Pos(cx, 0, z), new TgMapData.Pos(cx, -1, z)), "anvil[facing=north]",
                    TRAP_DELAY, TRAP_RESTORE, "block.anvil.land", "block.anvil.fall"));
        }
        FEATURES = f;
        return f;
    }

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    public static final int GATE_Z = 29;           // first row of the gate wall (flaps), wall z 29..32
    public static final int CEIL = 12;
    public static final int LOW = -6;              // service tunnel floor
    public static final int T1 = 42, T2 = 56;      // first z of each track pit (6 wide)
    /** Owner (playtest 2): the map was too short. A third track (T3) and a queue-rail hall were added before the plaza, which moved DZ along. */
    public static final int DZ = 56, T3 = 70;
    /** Queue-rail rows (z): wall to wall, a 3-wide gap at alternating ends (crowd) and a 1-wide ticket barrier in the middle (fast). */
    public static final int[] QUEUE = {84, 94, 104, 114};
    public static final int END = 92 + DZ;

    /** Passage x of turnstile i (2 wide: gx, gx+1). */
    public static int gateX(int i) { return -18 + 3 * i; }

    /** Coupling gap centre x for track 1 / 2. */
    public static final int C1 = 0, C2 = 10, C3 = -8;

    static final Pal FLOOR = new Pal("smooth_sandstone", 5, "white_terracotta", 4, "smooth_quartz", 1);
    static final Pal WALL = new Pal("smooth_sandstone", 6, "sandstone", 2, "white_terracotta", 2);
    static final Pal GREEN = new Pal("waxed_oxidized_cut_copper", 6, "waxed_oxidized_copper", 3, "waxed_weathered_cut_copper", 1);

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        shell(g);
        start(g);
        concourse(g);
        staffCorridor(g);
        gates(g);
        tunnel(g);
        tracks(g);
        plaza(g);
        g.scenery = true;
        decor(g);
        g.scenery = false;
        // no straight free run > 14 on the hall floor (posts = copper-capped bollards)
        g.breakRuns(-21, 21, 8, END - 1, 0, 13, p -> (p[1] >= GATE_Z - 2 && p[1] <= GATE_Z + 3) || (p[1] == GATE_Z + 4 && p[0] >= -18 && p[0] <= 17 && (p[0] + 18) % 3 != 2)
                || (p[0] <= -21 && p[1] >= 33 && p[1] <= 34)
                || (p[1] >= T1 && p[1] <= T1 + 5) || (p[1] >= T2 && p[1] <= T2 + 5)
                || (Math.abs(p[0]) >= 18 && p[1] >= GATE_Z + 4 && p[1] <= GATE_Z + 13)
                || (p[0] <= -21 && (p[1] >= 12 && p[1] <= 13))
                || (p[1] >= T3 && p[1] <= T3 + 5) || isQueue(p[1])
                || (p[0] >= -8 && p[0] <= 7 && p[1] >= 72 + DZ && p[1] <= 87 + DZ), "polished_blackstone_wall", null);
        g.set(-19, 1, 34, "barrel[facing=up,open=false]");
        g.set(-19, 1, 12, "barrel[facing=up,open=false]");
        g.breakRuns(-34, -24, 5, 39, 0, 11, p -> (p[0] >= -24 && (p[1] == 12 || p[1] == 13 || p[1] == 33 || p[1] == 34)), "barrel[facing=up,open=false]", null);

        d.voidY = LOW - 8;
        d.playBounds = Region.of(-35, LOW - 10, -3, 22, CEIL + 8, END);
        d.hazardRegions.add(Region.of(-21, -5, T1, 21, -1, T1 + 5));     // live track pits
        d.hazardRegions.add(Region.of(-21, -5, T2, 21, -1, T2 + 5));
        d.hazardRegions.add(Region.of(-21, -5, T3, 21, -1, T3 + 5));
        d.overviewPoints.add(new Spawn(0.5, CEIL - 2, 6.5, 0f, 40f));
        d.overviewPoints.add(new Spawn(0.5, CEIL - 2, 66.5, 0f, 45f));
        d.overviewPoints.add(new Spawn(0.5, CEIL - 2, 110.5, 0f, 40f));
        d.notes.put("safeLine", "staff corridor round the left wall (always open), then through the carriage doors");
        d.notes.put("fastLine", "gamble on a turnstile (OPEN/JAMMED/DROP, >= 4 of 12 open), jump the coupling gaps over the live track (the anvils drop!), single-file ticket barriers through the queue hall");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ hall

    private void shell(G g) {
        g.scenery = true;
        // floor slab (single layer over the tunnel / pits), walls, roof with trusses and skylights
        for (int x = -23; x <= 23; x++)
            for (int z = -4; z <= END + 1; z++) {
                boolean wall = x <= -22 || x >= 22 || z == -4 || z == END + 1;
                g.set(x, -1, z, "stone_bricks");
                g.set(x, 0, z, wall ? "polished_deepslate" : FLOOR.at(x, 0, z, g.seed));
                if ((x + 40) % 6 == 0 && !wall) g.set(x, 0, z, "polished_deepslate");
                int top = z >= 66 + DZ ? CEIL + 6 : CEIL;
                if (wall) for (int y = 1; y <= top; y++) g.set(x, y, z, wallBlock(g, x, y, z));
                // roof: barrel vault in 6-block bays
                double arch = Math.sqrt(Math.max(0, 1 - (x / 23.0) * (x / 23.0)));
                int ry = top + (int) Math.round(arch * 5);
                g.set(x, ry, z, (z % 6 == 0) ? "waxed_oxidized_cut_copper" : (Math.abs(x) < 15 && z % 6 >= 2 && z % 6 <= 4) ? "glass" : "dark_oak_planks");
                if (z % 6 == 0 && !wall) {
                    g.set(x, ry - 1, z, "waxed_oxidized_copper_grate");
                }
            }
        // the 'inside' of the roof vault above the ceiling line is open (vault), ceiling level marked by tie beams
        for (int z = 0; z <= END; z += 6)
            for (int x = -21; x <= 21; x++) g.set(x, z >= 66 + DZ ? CEIL + 6 : CEIL, z, "waxed_oxidized_cut_copper_slab[type=top]");
        // windows in the side walls (high), pilasters
        for (int z = -2; z <= END; z++) {
            boolean win = z % 6 >= 2 && z % 6 <= 4;
            for (int x : new int[]{-22, 22})
                for (int y = 6; y <= CEIL - 2; y++) if (win) g.set(x, y, z, "light_blue_stained_glass_pane");
            if (z % 6 == 0) for (int y = 1; y <= CEIL; y++) { g.set(-21, y, z, GREEN.at(-21, y, z, g.seed)); g.set(21, y, z, GREEN.at(21, y, z, g.seed)); }
        }
        // hanging lamps
        for (int z = 3; z <= END; z += 6)
            for (int x = -15; x <= 15; x += 10) {
                for (int y = 8; y < CEIL; y++) g.set(x, y, z, "chain[axis=y]");
                g.set(x, 7, z, "lantern[hanging=true]");
            }
        g.scenery = false;
    }

    private String wallBlock(G g, int x, int y, int z) {
        if (y == 1) return "polished_deepslate";
        if (y == 5) return "waxed_oxidized_cut_copper";
        if (y < 5) return (y + z) % 2 == 0 ? "white_terracotta" : "smooth_sandstone";
        return WALL.at(x, y, z, g.seed);
    }

    // ------------------------------------------------------------------ start

    private void start(G g) {
        TgMapData d = g.d;
        for (int x = -18; x <= 17; x++)
            for (int z = 0; z <= 6; z++) g.set(x, 0, z, ((x + 40) / 3 + z) % 2 == 0 ? "polished_blackstone" : "polished_deepslate");
        for (int x = -18; x <= 17; x++) g.set(x, 0, 6, (x & 1) == 0 ? "red_terracotta" : "white_terracotta");
        for (int y = 1; y <= 3; y++) {
            for (int z = -1; z <= 7; z++) { g.set(-19, y, z, "glass"); g.set(18, y, z, "glass"); }
            for (int x = -19; x <= 18; x++) g.set(x, y, -1, "glass");
            for (int x = -18; x <= 17; x++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new TgMapData.Pos(x, y, 7)); }
        }
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        g.startSpawns(-18, 17, 0, 6, 1, 15, 3);
        // entrance doors behind the start
        g.scenery = true;
        for (int x = -6; x <= 5; x++) for (int y = 1; y <= 4; y++) g.set(x, y, -4, (x % 3 == 0) ? "dark_oak_planks" : "glass_pane");
        g.text("MCM", -7, CEIL - 1, -3, 1, 0, 1, "red_wool");
        g.scenery = false;
    }

    // ------------------------------------------------------------------ concourse

    private void machine(G g, int x, int z) {
        g.set(x, 1, z, "polished_deepslate");
        g.set(x, 2, z, "black_stained_glass");
        g.set(x, 3, z, "red_terracotta");
        g.set(x + 1, 1, z, "polished_deepslate");
        g.set(x + 1, 2, z, "waxed_oxidized_copper_bulb[lit=true,powered=false]");
        g.set(x + 1, 3, z, "red_terracotta");
    }

    private void concourse(G g) {
        g.scenery = true;
        // two rows of ticket machines, pillars, queue rails with gaps (the crowd has to choose a lane)
        for (int x = -16; x <= 14; x += 6) machine(g, x, 11);
        for (int x = -13; x <= 17; x += 6) machine(g, x, 17);
        for (int x : new int[]{-12, 0, 12})
            for (int y = 1; y < CEIL; y++) for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) g.set(x + i, y, 22 + j, y == 1 || y == CEIL - 1 ? "polished_deepslate" : GREEN.at(x + i, y, 22 + j, g.seed));
        // queue rails in front of the gates
        for (int x = -20; x <= 20; x++) if ((x + 21) % 7 >= 2) g.set(x, 1, 26, "dark_oak_fence");
        for (int x = -20; x <= 20; x += 7) { g.set(x, 1, 26, "polished_blackstone_wall"); g.set(x, 2, 26, "waxed_oxidized_copper_bulb[lit=true,powered=false]"); }
        // benches by the walls
        for (int z = 9; z <= 24; z += 5) { g.set(-20, 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight]"); g.set(20, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]"); }
        g.scenery = false;
    }

    private void staffCorridor(G g) {
        // Staff corridor: door in the left wall at z 12..13, a loop round a block of staff offices
        // (lane 1 x -26..-24 back towards the entrance, lane 2 x -34..-32 all the way up, back across), in at z 33..34.
        g.scenery = true;
        for (int z = 4; z <= 40; z++)
            for (int x = -35; x <= -23; x++) {
                boolean outer = x == -35 || x == -23 || z == 4 || z == 40;
                boolean core = x >= -31 && x <= -27 && z >= 9 && z <= 35;
                g.set(x, -1, z, "stone_bricks");
                g.set(x, 0, z, outer || core ? "polished_deepslate" : ((x + z) & 1) == 0 ? "polished_andesite" : "andesite");
                for (int y = 1; y <= 5; y++) g.set(x, y, z, outer || core ? wallBlock(g, x, y, z) : null);
                g.set(x, 6, z, "dark_oak_planks");
            }
        // lane 1 is closed at z 15..16 (store room wall): you must go round the offices
        for (int x = -26; x <= -24; x++) for (int y = 1; y <= 5; y++) { g.set(x, y, 15, wallBlock(g, x, y, 15)); g.set(x, y, 16, wallBlock(g, x, y, 16)); }
        // office windows in the core, lamps
        for (int z = 11; z <= 33; z += 4) { g.set(-27, 2, z, "glass_pane"); g.set(-31, 2, z, "glass_pane"); }
        for (int z = 7; z <= 38; z += 6) { g.set(-25, 5, z, "lantern[hanging=true]"); g.set(-33, 5, z, "lantern[hanging=true]"); }
        g.text("STAFF", -21, 9, 18, 0, -1, 1, "red_wool");
        g.scenery = false;
        for (int z = 12; z <= 13; z++) for (int x = -23; x <= -22; x++) for (int y = 1; y <= 3; y++) g.set(x, y, z, null);
        for (int z = 33; z <= 34; z++) for (int x = -23; x <= -22; x++) for (int y = 1; y <= 3; y++) g.set(x, y, z, null);
    }

    // ------------------------------------------------------------------ gates

    private void gates(G g) {
        TgMapData d = g.d;
        int z0 = GATE_Z;
        g.scenery = true;
        // gate wall z 29..32: solid below y 4, screen of panes above, sign band
        for (int x = -21; x <= 21; x++)
            for (int z = z0; z <= z0 + 3; z++) {
                for (int y = 1; y <= 4; y++) g.set(x, y, z, z == z0 || z == z0 + 3 ? (y == 4 ? "waxed_oxidized_cut_copper" : "white_terracotta") : "polished_deepslate");
                if (z == z0 + 1) for (int y = 5; y < CEIL; y++) g.set(x, y, z, y == 5 || y == CEIL - 1 ? "waxed_oxidized_cut_copper" : (x % 4 == 0 ? "waxed_oxidized_copper" : "glass_pane"));
            }
        // sign band facing the start
        for (int x = -12; x <= 11; x++) for (int y = 5; y <= 10; y++) g.set(x, y, z0, "black_wool");
        g.text("GATES", 9, 9, z0 - 1, -1, 0, 1, "yellow_wool");
        g.scenery = false;
        for (int i = 0; i < 12; i++) {
            int gx = gateX(i);
            // passage 2 wide, 2 high, 4 deep; floor tiles; red flap (standing banners) at the entrance row
            for (int x = gx; x <= gx + 1; x++)
                for (int z = z0; z <= z0 + 3; z++) {
                    g.set(x, 0, z, "smooth_quartz");
                    g.set(x, 1, z, null);
                    g.set(x, 2, z, null);
                }
            for (int x = gx; x <= gx + 1; x++) g.set(x, 1, z0, "red_banner[rotation=0]");
            g.set(gx - 1, 3, z0 - 1, "waxed_oxidized_copper_bulb[lit=true,powered=false]");
            // the variant cells: z0+1 and z0+2 (floor + body)
            TgMapData.VariantGroup v = d.variant(String.format("gate_%02d", i + 1), "turnstile");
            for (int x = gx; x <= gx + 1; x++)
                for (int z = z0 + 1; z <= z0 + 2; z++) {
                    v.add("OPEN", x, 0, z, "smooth_quartz");
                    v.add("OPEN", x, 1, z, null);
                    v.add("OPEN", x, 2, z, null);
                    boolean jam = z == z0 + 1;
                    v.add("JAMMED", x, 0, z, "smooth_quartz");
                    v.add("JAMMED", x, 1, z, jam ? "iron_block" : null);
                    v.add("JAMMED", x, 2, z, jam ? "iron_bars" : null);
                    v.add("DROP", x, 0, z, null);
                    v.add("DROP", x, 1, z, null);
                    v.add("DROP", x, 2, z, null);
                }
        }
        d.variantRules.add(new TgMapData.VariantRule("turnstile", "OPEN", 4, -1));
    }

    // ------------------------------------------------------------------ service tunnel under the gate line

    private void tunnel(G g) {
        int z1 = GATE_Z - 2, z2 = GATE_Z + 5;
        g.scenery = true;
        for (int x = -23; x <= 23; x++)
            for (int z = z1 - 1; z <= z2 + 7; z++)
                for (int y = LOW - 1; y <= -1; y++) {
                    boolean inside = x >= -21 && x <= 21 && z >= z1 && z <= z2 && y > LOW && y <= -1;
                    boolean well = (x <= -18 || x >= 18) && x >= -21 && x <= 21 && z > z2 && z <= z2 + 6 && y > LOW;
                    if (inside || well) continue;
                    g.set(x, y, z, y <= LOW ? "polished_deepslate" : "stone_bricks");
                }
        g.scenery = false;
        for (int x = -21; x <= 21; x++)
            for (int z = z1; z <= z2; z++) {
                g.set(x, LOW, z, ((x + z) & 1) == 0 ? "polished_andesite" : "andesite");
                for (int y = LOW + 1; y <= -1; y++) g.set(x, y, z, null);
            }
        g.scenery = true;
        for (int x = -19; x <= 19; x += 6) g.set(x, -1, z1 + 3, "waxed_oxidized_copper_bulb[lit=true,powered=false]");
        for (int x = -20; x <= 20; x += 4) { g.set(x, LOW + 1, z1, "barrel[facing=up,open=false]"); }
        g.scenery = false;
        // stairwells at both sides back up to platform A (x -21..-19 and 19..21), 6 steps +z
        for (int side = -1; side <= 1; side += 2) {
            int x0 = side < 0 ? -21 : 19;
            for (int x = x0; x <= x0 + 2; x++)
                for (int z = z2 + 1; z <= z2 + 6; z++)
                    for (int y = LOW + 1; y <= 3; y++) g.set(x, y, z, null);
            Stairs.flight(g, side < 0 ? "tunnelL" : "tunnelR", "deepslate_brick", "deepslate_bricks", x0, z2 + 1, LOW, 0, 1, 6, 3, LOW);
            // railing on the platform side of the well
            int rx = side < 0 ? -18 : 18;
            for (int z = z2 + 1; z <= z2 + 5; z++) g.set(rx, 1, z, "dark_oak_fence");
        }
    }

    // ------------------------------------------------------------------ tracks

    private void car(G g, int x1, int x2, int z, int[] doors) {
        // carriage body z..z+5 (6 deep, flush with both platform edges), floor y 0, interior y 1..4, roof y 5
        for (int x = x1; x <= x2; x++)
            for (int k = 0; k <= 5; k++) {
                int cz = z + k;
                boolean side = k == 0 || k == 5;
                boolean end = x == x1 || x == x2;
                g.set(x, 0, cz, side || end ? "dark_oak_planks" : "spruce_planks");
                for (int y = 1; y <= 4; y++) {
                    String b;
                    if (side || end) b = y == 1 ? "waxed_oxidized_cut_copper" : y == 2 || y == 3 ? ((x & 3) == 0 ? "waxed_oxidized_cut_copper" : "glass_pane") : "white_terracotta";
                    else b = null;
                    g.set(x, y, cz, b);
                }
                g.set(x, 5, cz, side ? "waxed_oxidized_cut_copper_slab[type=bottom]" : "smooth_stone_slab[type=bottom]");
                // undercarriage + rails in the pit
                g.set(x, -1, cz, k == 1 || k == 4 ? "iron_bars" : null);
                g.set(x, -2, cz, (x % 7 == 0 && (k == 1 || k == 4)) ? "black_wool" : null);
            }
        // seats: rows of stairs between doors, facing the aisle
        for (int x = x1 + 2; x <= x2 - 2; x++) {
            boolean nearDoor = false;
            for (int dx : doors) if (x >= dx - 1 && x <= dx + 2) nearDoor = true;
            if (nearDoor) continue;
            if ((x - x1) % 3 == 0) { g.set(x, 1, z + 1, "red_wool"); g.set(x, 1, z + 4, "red_wool"); }
            else { g.set(x, 1, z + 1, "spruce_slab[type=bottom]"); g.set(x, 1, z + 4, "spruce_slab[type=bottom]"); }
        }
        // doors (2 wide, 3 high) on both sides, straight through
        // doors (2 wide, 3 high); the far-side door is staggered 3 along, so you turn inside the carriage
        for (int dx : doors) {
            int fx = dx + 3 <= x2 - 2 ? dx + 3 : dx - 3;
            for (int x = dx; x <= dx + 1; x++) for (int y = 1; y <= 3; y++) g.set(x, y, z, null);
            for (int x = fx; x <= fx + 1; x++) for (int y = 1; y <= 3; y++) g.set(x, y, z + 5, null);
            for (int x = Math.min(dx, fx) - 1; x <= Math.max(dx, fx) + 2; x++) { g.set(x, 1, z + 1, null); g.set(x, 1, z + 4, null); }
        }
        for (int dx : doors) { g.set(dx - 1, 4, z, "red_terracotta"); g.set(dx + 2, 4, z, "red_terracotta"); }
        for (int x = x1 + 3; x < x2; x += 8) g.set(x, 4, z + 2, "waxed_oxidized_copper_bulb[lit=true,powered=false]");
    }

    private void pit(G g, int z) {
        for (int x = -21; x <= 21; x++)
            for (int k = 0; k <= 5; k++) {
                g.set(x, 0, z + k, null);
                for (int y = -3; y <= -1; y++) g.set(x, y, z + k, null);
                g.set(x, -4, z + k, k == 1 || k == 4 ? "polished_andesite" : "gravel");
                if (k == 1 || k == 4) g.set(x, -3, z + k, "rail[shape=east_west]");
                else if (k == 2 || k == 3) g.set(x, -3, z + k, (x & 1) == 0 ? "redstone_wire[east=side,west=side,north=none,south=none,power=0]" : null);
                g.set(x, -5, z + k, "stone_bricks");
            }
        // pit walls
        for (int x = -21; x <= 21; x++) for (int y = -4; y <= -1; y++) { g.set(x, y, z - 1, "stone_bricks"); g.set(x, y, z + 6, "stone_bricks"); }
        // yellow platform edges
        for (int x = -21; x <= 21; x++) { g.set(x, 0, z - 1, "yellow_terracotta"); g.set(x, 0, z + 6, "yellow_terracotta"); }
    }

    private void coupling(G g, int cx, int z) {
        // buffers on the car ends and a single coupler block in the middle of the pit (z+2): jumps of 2 then 3
        g.set(cx, -1, z + 2, "iron_bars");
        g.set(cx, 0, z + 2, "anvil[facing=north]");
        g.set(cx - 2, 1, z + 2, "polished_blackstone_button[face=wall,facing=east,powered=false]");
        g.set(cx + 2, 1, z + 2, "polished_blackstone_button[face=wall,facing=west,powered=false]");
    }

    private void tracks(G g) {
        pit(g, T1);
        car(g, -21, C1 - 3, T1, new int[]{-17, -9});
        car(g, C1 + 3, 21, T1, new int[]{6, 14});
        coupling(g, C1, T1);
        pit(g, T2);
        car(g, -21, C2 - 3, T2, new int[]{-13, -4, 3});
        car(g, C2 + 3, 21, T2, new int[]{17});
        coupling(g, C2, T2);
        pit(g, T3);
        car(g, -21, C3 - 3, T3, new int[]{-15});
        car(g, C3 + 3, 21, T3, new int[]{-1, 9, 16});
        coupling(g, C3, T3);
        queue(g);
        // platform B kiosk and columns (the crowd squeezes between them)
        g.scenery = true;
        int zb = T1 + 6;
        g.fill(-6, 1, zb + 2, -2, 3, zb + 5, new Pal("red_terracotta", 3, "red_nether_bricks", 1));
        g.fill(-6, 4, zb + 1, -2, 4, zb + 6, "waxed_oxidized_cut_copper_slab[type=bottom]");
        g.fill(-5, 2, zb + 2, -3, 2, zb + 2, "glass_pane");
        for (int x : new int[]{-15, 6, 16})
            for (int y = 1; y < CEIL; y++) { g.set(x, y, zb + 3, GREEN.at(x, y, zb + 3, g.seed)); g.set(x, y, zb + 4, GREEN.at(x, y, zb + 4, g.seed)); }
        // platform numbers hanging over the tracks
        for (int[] t : new int[][]{{T1, 1}, {T2, 2}, {T3, 3}}) {
            for (int x = -20; x <= -16; x++) for (int y = 8; y <= 10; y++) g.set(x, y, t[0] - 1, "black_wool");
            g.text(String.valueOf(t[1]), -17, 10, t[0] - 2, -1, 0, 1, "white_wool");
        }
        g.scenery = false;
    }

    static boolean isQueue(int z) { for (int q : QUEUE) if (z == q) return true; return false; }

    /** Queue hall (platform C to the plaza): rails wall to wall with alternating 3-wide end gaps and a 1-wide barrier in the middle. */
    private void queue(G g) {
        for (int k = 0; k < QUEUE.length; k++) {
            int z = QUEUE[k];
            boolean gapRight = (k & 1) == 0;
            for (int x = -21; x <= 21; x++) {
                boolean endGap = gapRight ? x >= 19 : x <= -19;
                boolean barrier = x == (gapRight ? -3 : 2);
                if (endGap) continue;
                if (barrier) { g.set(x, 1, z, null); g.set(x, 2, z, null); continue; }
                g.set(x, 1, z, (x & 3) == 0 ? "polished_blackstone_wall" : "iron_bars");
                g.set(x, 2, z, (x & 3) == 0 ? "lantern[hanging=false]" : null);
            }
            g.scenery = true;
            // barrier frame over the 1-wide ticket gate: a yellow / black lintel (reads as "single file")
            int bx = gapRight ? -3 : 2;
            for (int dx = -1; dx <= 1; dx++) g.set(bx + dx, 3, z, (dx & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
            g.set(bx - 1, 2, z, "polished_blackstone_wall"); g.set(bx + 1, 2, z, "polished_blackstone_wall");
            g.scenery = false;
        }
        // luggage carts parked between the rails (weave obstacles, not on the barrier line)
        int[][] carts = {{-12, 89}, {10, 99}, {-9, 109}, {14, 89}, {-16, 99}};
        for (int[] c : carts) for (int dx = 0; dx <= 2; dx++) { g.set(c[0] + dx, 1, c[1], "barrel[facing=up,open=false]"); g.set(c[0] + dx, 1, c[1] + 1, dx == 1 ? "chest[facing=north]" : "spruce_planks"); }
    }

    // ------------------------------------------------------------------ plaza + pen

    private void plaza(G g) {
        TgMapData d = g.d;
        int z1 = 73 + DZ, z2 = 86 + DZ;
        for (int x = -7; x <= 6; x++)
            for (int z = z1; z <= z2; z++) {
                boolean ring = x == -7 || x == 6 || z == z1 || z == z2;
                g.set(x, 0, z, ring ? "polished_deepslate" : ((x + z) & 1) == 0 ? "smooth_quartz" : "white_terracotta");
                if (ring) g.set(x, 1, z, ((x + z) & 1) == 0 ? "waxed_oxidized_cut_copper" : "red_terracotta");
            }
        for (int x = -2; x <= 1; x++) g.set(x, 1, z1, null);
        for (int z = 78 + DZ; z <= 79 + DZ; z++) { g.set(-7, 1, z, null); g.set(6, 1, z, null); }
        d.pen = Region.of(-6, 1, z1 + 1, 5, 3, z2 - 1);
        g.chickens(d.pen, 12);
        g.set(-5, 1, z2 - 1, "hay_block");
        g.set(4, 1, z2 - 1, "hay_block");
        // station clock tower behind the pen, reaching into the vault
        g.scenery = true;
        int cz = 89 + DZ;
        for (int y = 1; y <= CEIL + 9; y++)
            for (int x = -2; x <= 1; x++)
                for (int z = cz; z <= cz + 2; z++) g.set(x, y, z, y > CEIL + 6 ? "waxed_oxidized_cut_copper" : (x == -2 || x == 1) ? "dark_oak_planks" : "smooth_sandstone");
        // clock face (radius 4) on the front
        for (int x = -5; x <= 4; x++)
            for (int y = CEIL - 1; y <= CEIL + 8; y++) {
                double dx = x + 0.5, dy = y - (CEIL + 3.5);
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r <= 4.9) g.set(x, y, cz - 1, r > 4.1 ? "waxed_oxidized_cut_copper" : "quartz_block");
            }
        for (int y = CEIL + 3; y <= CEIL + 6; y++) g.set(-1, y, cz - 2, "black_wool");
        for (int x = 0; x <= 2; x++) g.set(x, CEIL + 3, cz - 2, "black_wool");
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            g.set((int) Math.round(-0.5 + Math.sin(a) * 3.6), (int) Math.round(CEIL + 3 + Math.cos(a) * 3.6), cz - 2, "red_terracotta");
        }
        // flower planters and benches round the plaza
        for (int[] p : new int[][]{{-14, 75 + DZ}, {12, 75 + DZ}, {-14, 84 + DZ}, {12, 84 + DZ}}) {
            g.fill(p[0], 1, p[1], p[0] + 2, 1, p[1] + 2, "waxed_oxidized_cut_copper");
            g.fill(p[0], 2, p[1], p[0] + 2, 2, p[1] + 2, "azalea_leaves");
            g.set(p[0] + 1, 3, p[1] + 1, "flowering_azalea");
        }
        g.scenery = false;
    }

    private void decor(G g) {
        // departure boards over the concourse
        for (int x = -8; x <= 7; x++) for (int y = 8; y <= 10; y++) g.set(x, y, 14, y == 9 && (x & 1) == 0 ? "orange_wool" : "black_wool");
        for (int x = -8; x <= 7; x += 3) { g.set(x, 11, 14, "chain[axis=y]"); }
        // posters/banners along the walls
        for (int z = 4; z <= END; z += 12) {
            g.set(-21, 3, z + 2, "red_wall_banner[facing=east]");
            g.set(21, 3, z + 2, "red_wall_banner[facing=west]");
        }
        // potted plants on platform A / C
        for (int x = -14; x <= 14; x += 7) { g.set(x, 1, 39, "potted_fern"); g.set(x, 1, 66, "potted_azalea_bush"); g.set(x + 3, 1, T3 + 8, "potted_fern"); }
    }
}
