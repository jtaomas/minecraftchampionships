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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ticket Gates: the trap map (owner, playtest 4: "the really confusing trappy one"). A station hall, one wide lane
 * towards +Z, no side routes:
 * <ol>
 *   <li>Concourse with hidden pitfalls: single floor blocks that look like every other one and drop you into the void.</li>
 *   <li>The fake gates: 12 turnstiles, each OPEN, JAMMED or a DROP into the service tunnel (which rejoins).</li>
 *   <li>Three tracks: walk through the parked carriages or jump the coupling anvils, which drop into the pit.
 *       Swinging station lamps sweep platforms B and C.</li>
 *   <li>The tile hall: a TNT-run floor - every tile drops just after you step on it and grows back 2 s later.</li>
 *   <li>Lost Property: a plain maze with walls up to the roof, braided so there is more than one way (owner: no traps in it).</li>
 *   <li>The finish: a full-width row of chickens under the station clock.</li>
 * </ol>
 * Checkpoints at platform A, platform D and the maze entrance. Owner, playtest 4: the old scattered posts and the
 * staff corridor are gone ("too many random walls everywhere").
 *
 * <p>Local frame: floor y = 0, hall interior x -21..21, start strip x -18..17 / z 0..6, course towards +Z.
 */
public final class TicketGates implements TgMap {

    @Override public String id() { return "ticket_gates"; }
    @Override public String displayName() { return "Ticket Gates"; }
    @Override public MapType type() { return MapType.DOORS; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }

    /** Coupling anvil: creaks 0.4 s after a racer lands on it, then drops into the live pit; back 3 s later. */
    public static final int TRAP_DELAY = 8, TRAP_RESTORE = 60;
    /** Pitfall block: gives way almost at once (barely a sound), you drop into the void; back 2.5 s later. */
    public static final int PIT_DELAY = 2, PIT_RESTORE = 50;
    /** Tile hall TNT run: a tile drops 0.3 s after you step on it and grows back 2 s later (owner: "regenerate quickly"). */
    public static final int CRUMBLE_DELAY = 6, CRUMBLE_RESTORE = 40;

    public static final int GATE_Z = 29;           // first row of the gate wall (flaps), wall z 29..32
    public static final int CEIL = 12;
    public static final int LOW = -6;              // service tunnel floor
    public static final int T1 = 42, T2 = 56, T3 = 70;   // first z of each track pit (6 wide)
    /** Coupling gap centre x per track. */
    public static final int C1 = 0, C2 = 10, C3 = -8;
    /** Platform D (after track 3) z 76..80, tile hall z 81..92, strip z 93..95, maze walls z 96..138, plaza from 139. */
    public static final int FIELD_Z = 81, MAZE_Z = 96, MAZE_CELLS = 14, PLAZA_Z = MAZE_Z + 3 * MAZE_CELLS + 1;
    public static final int ROW_Z = PLAZA_Z + 10, END = PLAZA_Z + 19;

    /** Passage x of turnstile i (2 wide: gx, gx+1). */
    public static int gateX(int i) { return -18 + 3 * i; }

    /**
     * Pitfalls, owner playtest 4: single blocks (not patches) over the void. Picked by a fixed hash, never two touching,
     * never next to an obstacle: about 1 in 16 concourse cells and 1 in 10 tile-hall cells.
     */
    static boolean pitCell(int x, int z, int percent, long salt) {
        long h = (x * 73856093L) ^ (z * 19349663L) ^ salt;
        h ^= h >>> 13; h *= 0x5DEECE66DL; h ^= h >>> 17;
        return Math.floorMod(h, 100) < percent;
    }

    static boolean concourseFree(int x, int z) {
        if (z < 9 || z > 25 || Math.abs(x) > 19) return false;
        if (z >= 10 && z <= 12 || z >= 16 && z <= 18) return false;                  // ticket machine rows
        if (z >= 21 && z <= 24 && (Math.abs(x + 11.5) <= 2 || Math.abs(x - 0.5) <= 2 || Math.abs(x - 12.5) <= 2)) return false;   // pillars
        return true;
    }

    static final Pal FLOOR = new Pal("smooth_sandstone", 5, "white_terracotta", 4, "smooth_quartz", 1);
    static final Pal WALL = new Pal("smooth_sandstone", 6, "sandstone", 2, "white_terracotta", 2);
    static final Pal GREEN = new Pal("waxed_oxidized_cut_copper", 6, "waxed_oxidized_copper", 3, "waxed_weathered_cut_copper", 1);
    static final Pal MAZE = new Pal("deepslate_tiles", 5, "polished_deepslate", 3, "deepslate_bricks", 2);

    // ================================================================== maze model (fixed, so features() can know the cells)

    /** Openings of the maze: east[cx][r] = no wall between cell (cx, r) and (cx + 1, r); south[cx][r] = no wall to (cx, r + 1). */
    public static final class Maze {
        public final boolean[][] east = new boolean[MAZE_CELLS][MAZE_CELLS], south = new boolean[MAZE_CELLS][MAZE_CELLS];
        /** Entrance columns (front wall) and exit columns (back wall). */
        public final int[] in = {1, 5, 8, 12}, out = {3, 10};

        Maze(long seed) {
            Random rnd = new Random(seed);
            boolean[][] seen = new boolean[MAZE_CELLS][MAZE_CELLS];
            int[][] stack = new int[MAZE_CELLS * MAZE_CELLS][];
            int sp = 0;
            stack[sp++] = new int[]{MAZE_CELLS / 2, 0};
            seen[MAZE_CELLS / 2][0] = true;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (sp > 0) {
                int[] c = stack[sp - 1];
                List<int[]> next = new ArrayList<>();
                for (int[] dd : dirs) {
                    int nx = c[0] + dd[0], nr = c[1] + dd[1];
                    if (nx >= 0 && nx < MAZE_CELLS && nr >= 0 && nr < MAZE_CELLS && !seen[nx][nr]) next.add(new int[]{nx, nr});
                }
                if (next.isEmpty()) { sp--; continue; }
                int[] n = next.get(rnd.nextInt(next.size()));
                open(c[0], c[1], n[0], n[1]);
                seen[n[0]][n[1]] = true;
                stack[sp++] = n;
            }
            // braid: knock out ~1 in 10 of the remaining inner walls, so the crowd splits and meets again
            for (int cx = 0; cx < MAZE_CELLS; cx++)
                for (int r = 0; r < MAZE_CELLS; r++) {
                    if (cx + 1 < MAZE_CELLS && !east[cx][r] && rnd.nextDouble() < 0.10) east[cx][r] = true;
                    if (r + 1 < MAZE_CELLS && !south[cx][r] && rnd.nextDouble() < 0.10) south[cx][r] = true;
                }
        }

        private void open(int ax, int ar, int bx, int br) {
            if (ax != bx) east[Math.min(ax, bx)][ar] = true;
            else south[ax][Math.min(ar, br)] = true;
        }

        /** Fewest cells from any entrance to any exit. */
        public int shortest() {
            int best = Integer.MAX_VALUE;
            for (int i : in) {
                int[][] dist = new int[MAZE_CELLS][MAZE_CELLS];
                for (int[] row : dist) java.util.Arrays.fill(row, -1);
                java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                dist[i][0] = 0;
                q.add(new int[]{i, 0});
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    int cx = c[0], r = c[1];
                    int[][] nb = {{cx + 1, r, cx < MAZE_CELLS - 1 && east[cx][r] ? 1 : 0}, {cx - 1, r, cx > 0 && east[cx - 1][r] ? 1 : 0},
                            {cx, r + 1, r < MAZE_CELLS - 1 && south[cx][r] ? 1 : 0}, {cx, r - 1, r > 0 && south[cx][r - 1] ? 1 : 0}};
                    for (int[] n : nb) if (n[2] == 1 && dist[n[0]][n[1]] < 0) { dist[n[0]][n[1]] = dist[cx][r] + 1; q.add(new int[]{n[0], n[1]}); }
                }
                for (int o : out) if (dist[o][MAZE_CELLS - 1] >= 0) best = Math.min(best, dist[o][MAZE_CELLS - 1]);
            }
            return best;
        }

        public int openings(int cx, int r) {
            int n = 0;
            if (cx > 0 && east[cx - 1][r]) n++;
            if (cx < MAZE_CELLS - 1 && east[cx][r]) n++;
            if (r > 0 && south[cx][r - 1]) n++;
            if (r < MAZE_CELLS - 1 && south[cx][r]) n++;
            return n;
        }

        /** Block x / z of a cell's first column / row (cells are 2 x 2, walls 1 thick). */
        public static int cellX(int cx) { return -20 + 3 * cx; }
        public static int cellZ(int r) { return MAZE_Z + 1 + 3 * r; }
    }

    private static Maze MAZE_MODEL;
    public static synchronized Maze maze() {
        // the first fixed seed whose quickest way through winds at least twice the straight distance
        if (MAZE_MODEL == null)
            for (long s = 0x7_1C4E7L; MAZE_MODEL == null || MAZE_MODEL.shortest() < 2 * MAZE_CELLS; s++) MAZE_MODEL = new Maze(s);
        return MAZE_MODEL;
    }

    // ================================================================== features

    private static MapFeatures FEATURES;

    @Override public MapFeatures features() { return feats(); }

    private static List<int[]> PITS;

    /** Every pitfall cell {x, z}: the concourse only (the tile hall is a TNT-run floor, none in the maze). */
    public static synchronized List<int[]> pitfalls() {
        if (PITS != null) return PITS;
        List<int[]> out = new ArrayList<>();
        java.util.Set<Long> taken = new java.util.HashSet<>();
        for (int z = 9; z <= 25; z++)
            for (int x = -19; x <= 19; x++)
                if (concourseFree(x, z) && pitCell(x, z, 6, 11) && free(taken, x, z)) { out.add(new int[]{x, z}); taken.add(k(x, z)); }
        // the tile hall is a TNT-run floor now (owner): no single pitfalls there
        PITS = out;
        return out;
    }

    private static long k(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }
    private static boolean free(java.util.Set<Long> t, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (t.contains(k(x + dx, z + dz))) return false;
        return true;
    }

    /** Pendulums (swinging station lamps): pivot x, z. */
    static final double[][] LAMPS = {{-14, 52}, {0, 52}, {14, 52}, {-16, 66}, {-2, 66}, {12, 66}};
    static final int LAMP_PERIOD = 52;

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
        // the tile hall, the ending before the maze: a TNT-run floor that regrows quickly (owner)
        f.crumbles.add(new MapFeatures.Crumble("tile hall", Region.of(-21, 0, FIELD_Z, 21, 0, FIELD_Z + 11),
                CRUMBLE_DELAY, CRUMBLE_RESTORE, "block.stone.break"));
        int n = 0;
        for (int[] p : pitfalls()) {
            f.traps.add(new MapFeatures.Trap("pitfall " + (++n), Region.of(p[0], 1, p[1], p[0], 1, p[1]),
                    List.of(new TgMapData.Pos(p[0], 0, p[1])), null, PIT_DELAY, PIT_RESTORE, "block.gravel.step", "block.stone.break"));
        }
        // swinging lamps: pivot under the roof, a chain rod and a heavy lamp head that sweeps the platform side to side
        int k = 0;
        for (double[] l : LAMPS) {
            double py = CEIL - 0.5;
            List<MapFeatures.Box> boxes = List.of(
                    new MapFeatures.Box(-0.12, -8.4, -0.12, 0.12, 0, 0.12, "chain[axis=y]", false),
                    new MapFeatures.Box(-0.9, -10.1, -1.0, 0.9, -8.4, 1.0, "waxed_oxidized_copper", true),
                    new MapFeatures.Box(-0.6, -10.45, -0.6, 0.6, -10.1, 0.6, "redstone_lamp[lit=true]", true));
            f.movers.add(MapFeatures.Mover.pendulum("swinging lamp " + (k + 1), l[0] + 0.5, py, l[1] + 0.5, 'z', 52, LAMP_PERIOD,
                    (k * 17) % LAMP_PERIOD, boxes));
            k++;
        }
        f.checkpoints.add(new MapFeatures.Checkpoint(1, "Platform A", Region.of(-21, 0, 36, 21, 4, 36), spawns(38), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(2, "Platform D", Region.of(-21, 0, 77, 21, 4, 77), spawns(78), List.of()));
        f.checkpoints.add(new MapFeatures.Checkpoint(3, "Lost Property", Region.of(-21, 0, 94, 21, 4, 94), spawns(94), List.of()));
        FEATURES = f;
        return f;
    }

    private static List<Spawn> spawns(int z) {
        List<Spawn> s = new ArrayList<>();
        for (int x = -15; x <= 15; x += 3) s.add(new Spawn(x + 0.5, 1, z + 0.5, 0f, 0f));
        return s;
    }

    // ================================================================== build

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        shell(g);
        start(g);
        concourse(g);
        gates(g);
        tunnel(g);
        tracks(g);
        tileHall(g);
        maze(g);
        plaza(g);
        for (int[] p : pitfalls()) g.set(p[0], -1, p[1], null);   // nothing under the floor block but the void
        for (int x = -21; x <= 21; x++) for (int z = FIELD_Z; z < FIELD_Z + 12; z++) g.set(x, -1, z, null);   // TNT-run hall: void below
        g.scenery = true;
        decor(g);
        g.scenery = false;

        d.voidY = LOW - 8;
        d.playBounds = Region.of(-24, LOW - 10, -5, 24, CEIL + 12, END + 1);
        d.hazardRegions.add(Region.of(-21, -5, T1, 21, -1, T1 + 5));     // live track pits
        d.hazardRegions.add(Region.of(-21, -5, T2, 21, -1, T2 + 5));
        d.hazardRegions.add(Region.of(-21, -5, T3, 21, -1, T3 + 5));
        d.overviewPoints.add(new Spawn(0.5, CEIL - 2, 6.5, 0f, 40f));
        d.overviewPoints.add(new Spawn(0.5, CEIL - 2, 66.5, 0f, 45f));
        d.overviewPoints.add(new Spawn(0.5, CEIL + 4, 100.5, 0f, 55f));
        d.notes.put("safeLine", "a jammed turnstile costs a few seconds; carriage doors instead of the anvils; walk round the lamps' swing");
        d.notes.put("fastLine", "an open turnstile, the coupling anvils, timing the lamps, guessing the safe tiles, the short way through the maze");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ hall

    private void shell(G g) {
        g.scenery = true;
        for (int x = -23; x <= 23; x++)
            for (int z = -4; z <= END + 1; z++) {
                boolean wall = x <= -22 || x >= 22 || z == -4 || z == END + 1;
                g.set(x, -1, z, "stone_bricks");
                g.set(x, 0, z, wall ? "polished_deepslate" : FLOOR.at(x, 0, z, g.seed));
                if ((x + 40) % 6 == 0 && !wall) g.set(x, 0, z, "polished_deepslate");
                int top = z >= PLAZA_Z ? CEIL + 6 : CEIL;
                if (wall) for (int y = 1; y <= top; y++) g.set(x, y, z, wallBlock(g, x, y, z));
                double arch = Math.sqrt(Math.max(0, 1 - (x / 23.0) * (x / 23.0)));
                int ry = top + (int) Math.round(arch * 5);
                g.set(x, ry, z, (z % 6 == 0) ? "waxed_oxidized_cut_copper" : (Math.abs(x) < 15 && z % 6 >= 2 && z % 6 <= 4) ? "glass" : "dark_oak_planks");
                if (z % 6 == 0 && !wall) g.set(x, ry - 1, z, "waxed_oxidized_copper_grate");
            }
        for (int z = 0; z <= END; z += 6)
            for (int x = -21; x <= 21; x++) g.set(x, z >= PLAZA_Z ? CEIL + 6 : CEIL, z, "waxed_oxidized_cut_copper_slab[type=top]");
        for (int z = -2; z <= END; z++) {
            boolean win = z % 6 >= 2 && z % 6 <= 4;
            for (int x : new int[]{-22, 22})
                for (int y = 6; y <= CEIL - 2; y++) if (win) g.set(x, y, z, "light_blue_stained_glass_pane");
            if (z % 6 == 0) for (int y = 1; y <= CEIL; y++) { g.set(-21, y, z, GREEN.at(-21, y, z, g.seed)); g.set(21, y, z, GREEN.at(21, y, z, g.seed)); }
        }
        // hanging lamps (not over the maze: its walls reach the tie beams)
        for (int z = 3; z <= END; z += 6) {
            if (z >= MAZE_Z - 1 && z <= PLAZA_Z) continue;
            for (int x = -15; x <= 15; x += 10) {
                for (int y = 8; y < CEIL; y++) g.set(x, y, z, "chain[axis=y]");
                g.set(x, 7, z, "lantern[hanging=true]");
            }
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
        g.scenery = true;
        for (int x = -6; x <= 5; x++) for (int y = 1; y <= 4; y++) g.set(x, y, -4, (x % 3 == 0) ? "dark_oak_planks" : "glass_pane");
        g.text("MCM", -7, CEIL - 1, -3, 1, 0, 1, "red_wool");
        g.scenery = false;
    }

    // ------------------------------------------------------------------ concourse (open floor, hidden pitfalls)

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
        for (int x = -16; x <= 14; x += 6) machine(g, x, 11);
        for (int x = -13; x <= 17; x += 6) machine(g, x, 17);
        for (int x : new int[]{-12, 0, 12})
            for (int y = 1; y < CEIL; y++) for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++)
                g.set(x + i, y, 22 + j, y == 1 || y == CEIL - 1 ? "polished_deepslate" : GREEN.at(x + i, y, 22 + j, g.seed));
        for (int z = 9; z <= 24; z += 5) { g.set(-20, 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight]"); g.set(20, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]"); }
        g.scenery = false;
    }

    // ------------------------------------------------------------------ the fake gates

    private void gates(G g) {
        TgMapData d = g.d;
        int z0 = GATE_Z;
        g.scenery = true;
        for (int x = -21; x <= 21; x++)
            for (int z = z0; z <= z0 + 3; z++) {
                for (int y = 1; y <= 4; y++) g.set(x, y, z, z == z0 || z == z0 + 3 ? (y == 4 ? "waxed_oxidized_cut_copper" : "white_terracotta") : "polished_deepslate");
                if (z == z0 + 1) for (int y = 5; y < CEIL; y++) g.set(x, y, z, y == 5 || y == CEIL - 1 ? "waxed_oxidized_cut_copper" : (x % 4 == 0 ? "waxed_oxidized_copper" : "glass_pane"));
            }
        for (int x = -12; x <= 11; x++) for (int y = 5; y <= 10; y++) g.set(x, y, z0, "black_wool");
        g.text("GATES", 9, 9, z0 - 1, -1, 0, 1, "yellow_wool");
        g.scenery = false;
        for (int i = 0; i < 12; i++) {
            int gx = gateX(i);
            for (int x = gx; x <= gx + 1; x++)
                for (int z = z0; z <= z0 + 3; z++) {
                    g.set(x, 0, z, "smooth_quartz");
                    g.set(x, 1, z, null);
                    g.set(x, 2, z, null);
                }
            for (int x = gx; x <= gx + 1; x++) g.set(x, 1, z0, "red_banner[rotation=0]");
            g.set(gx - 1, 3, z0 - 1, "waxed_oxidized_copper_bulb[lit=true,powered=false]");
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
        for (int x = -20; x <= 20; x += 4) g.set(x, LOW + 1, z1, "barrel[facing=up,open=false]");
        g.scenery = false;
        for (int side = -1; side <= 1; side += 2) {
            int x0 = side < 0 ? -21 : 19;
            for (int x = x0; x <= x0 + 2; x++)
                for (int z = z2 + 1; z <= z2 + 6; z++)
                    for (int y = LOW + 1; y <= 3; y++) g.set(x, y, z, null);
            Stairs.flight(g, side < 0 ? "tunnelL" : "tunnelR", "deepslate_brick", "deepslate_bricks", x0, z2 + 1, LOW, 0, 1, 6, 3, LOW);
            int rx = side < 0 ? -18 : 18;
            for (int z = z2 + 1; z <= z2 + 5; z++) g.set(rx, 1, z, "dark_oak_fence");
        }
    }

    // ------------------------------------------------------------------ tracks

    private void car(G g, int x1, int x2, int z, int[] doors) {
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
                g.set(x, -1, cz, k == 1 || k == 4 ? "iron_bars" : null);
                g.set(x, -2, cz, (x % 7 == 0 && (k == 1 || k == 4)) ? "black_wool" : null);
            }
        for (int x = x1 + 2; x <= x2 - 2; x++) {
            boolean nearDoor = false;
            for (int dx : doors) if (x >= dx - 1 && x <= dx + 2) nearDoor = true;
            if (nearDoor) continue;
            if ((x - x1) % 3 == 0) { g.set(x, 1, z + 1, "red_wool"); g.set(x, 1, z + 4, "red_wool"); }
            else { g.set(x, 1, z + 1, "spruce_slab[type=bottom]"); g.set(x, 1, z + 4, "spruce_slab[type=bottom]"); }
        }
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
        for (int x = -21; x <= 21; x++) for (int y = -4; y <= -1; y++) { g.set(x, y, z - 1, "stone_bricks"); g.set(x, y, z + 6, "stone_bricks"); }
        for (int x = -21; x <= 21; x++) { g.set(x, 0, z - 1, "yellow_terracotta"); g.set(x, 0, z + 6, "yellow_terracotta"); }
    }

    private void coupling(G g, int cx, int z) {
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
        g.scenery = true;
        // lamp mounts under the roof (the swinging lamps hang from these) and a hazard stripe under each swing
        for (double[] l : LAMPS) {
            int x = (int) l[0], z = (int) l[1];
            g.fill(x - 1, CEIL - 1, z - 1, x + 1, CEIL - 1, z + 1, "waxed_oxidized_cut_copper");
            for (int dx = -7; dx <= 7; dx++) if (Math.abs(x + dx) <= 21 && Math.abs(dx) % 2 == 0) g.set(x + dx, 0, z - 2, "yellow_terracotta");
        }
        for (int[] t : new int[][]{{T1, 1}, {T2, 2}, {T3, 3}}) {
            for (int x = -20; x <= -16; x++) for (int y = 8; y <= 10; y++) g.set(x, y, t[0] - 1, "black_wool");
            g.text(String.valueOf(t[1]), -17, 10, t[0] - 2, -1, 0, 1, "white_wool");
        }
        g.scenery = false;
    }

    // ------------------------------------------------------------------ tile hall

    private void tileHall(G g) {
        for (int i = 0; i < 14; i++)
            for (int j = 0; j < 4; j++) {
                String b = ((i + j) & 1) == 0 ? "smooth_quartz" : "white_terracotta";
                for (int x = -20 + 3 * i; x < -17 + 3 * i; x++) for (int z = FIELD_Z + 3 * j; z < FIELD_Z + 3 * j + 3; z++) g.set(x, 0, z, b);
            }
        for (int z = FIELD_Z; z < FIELD_Z + 12; z++) g.set(-21, 0, z, "polished_deepslate");
        // a black border either side of the hall so it reads as one board
        for (int x = -21; x <= 21; x++) { g.set(x, 0, FIELD_Z - 1, "black_terracotta"); g.set(x, 0, FIELD_Z + 12, "black_terracotta"); }
    }

    // ------------------------------------------------------------------ Lost Property maze

    private void maze(G g) {
        Maze m = maze();
        int z0 = MAZE_Z, z1 = MAZE_Z + 3 * MAZE_CELLS;
        // every wall cell solid up to the tie beams, then carve the cells and the openings
        for (int x = -21; x <= 21; x++)
            for (int z = z0; z <= z1; z++) {
                g.set(x, 0, z, ((x + z) & 1) == 0 ? "polished_andesite" : "andesite");
                for (int y = 1; y <= CEIL; y++) g.set(x, y, z, y == 5 ? "waxed_oxidized_cut_copper" : MAZE.at(x, y, z, g.seed));
            }
        for (int cx = 0; cx < MAZE_CELLS; cx++)
            for (int r = 0; r < MAZE_CELLS; r++) {
                int x = Maze.cellX(cx), z = Maze.cellZ(r);
                carve(g, x, z, x + 1, z + 1);
                if (cx < MAZE_CELLS - 1 && m.east[cx][r]) carve(g, x + 2, z, x + 2, z + 1);
                if (r < MAZE_CELLS - 1 && m.south[cx][r]) carve(g, x, z + 2, x + 1, z + 2);
            }
        g.scenery = true;
        for (int i : m.in) { int x = Maze.cellX(i); for (int dx = 0; dx <= 1; dx++) for (int y = 1; y <= 3; y++) g.set(x + dx, y, z0, null); }
        for (int i : m.out) { int x = Maze.cellX(i); for (int dx = 0; dx <= 1; dx++) for (int y = 1; y <= 3; y++) g.set(x + dx, y, z1, null); }
        // invisible lights in the corridors (the walls block the hall lamps)
        for (int cx = 0; cx < MAZE_CELLS; cx++)
            for (int r = 0; r < MAZE_CELLS; r++) if (((cx + r) & 1) == 0) g.set(Maze.cellX(cx), 4, Maze.cellZ(r), "light[level=13]");
        // sign over the entrances
        for (int x = -12; x <= 11; x++) for (int y = 6; y <= 10; y++) g.set(x, y, z0 - 1, "black_wool");
        g.text("LOST", 4, 9, z0 - 2, -1, 0, 1, "yellow_wool");
        g.scenery = false;
    }

    private void carve(G g, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = 1; y <= CEIL; y++) g.set(x, y, z, null);
    }

    // ------------------------------------------------------------------ plaza + finish row

    private void plaza(G g) {
        TgMapData d = g.d;
        for (int x = -21; x <= 21; x++)
            for (int z = PLAZA_Z; z <= END; z++) g.set(x, 0, z, ((x + z) & 1) == 0 ? "smooth_quartz" : "white_terracotta");
        com.mcm.games.tgttos.model.FinishRow.build(d, -19, 19, 0, ROW_Z, 1, "smooth_quartz");
        g.scenery = true;
        int cz = ROW_Z + 4;
        for (int y = 1; y <= CEIL + 9; y++)
            for (int x = -2; x <= 1; x++)
                for (int z = cz; z <= cz + 2; z++) g.set(x, y, z, y > CEIL + 6 ? "waxed_oxidized_cut_copper" : (x == -2 || x == 1) ? "dark_oak_planks" : "smooth_sandstone");
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
        g.scenery = false;
    }

    private void decor(G g) {
        for (int x = -8; x <= 7; x++) for (int y = 8; y <= 10; y++) g.set(x, y, 14, y == 9 && (x & 1) == 0 ? "orange_wool" : "black_wool");
        for (int x = -8; x <= 7; x += 3) g.set(x, 11, 14, "chain[axis=y]");
        for (int z = 4; z <= MAZE_Z - 4; z += 12) {
            g.set(-21, 3, z + 2, "red_wall_banner[facing=east]");
            g.set(21, 3, z + 2, "red_wall_banner[facing=west]");
        }
        for (int x = -14; x <= 14; x += 7) g.set(x, 1, 39, "potted_fern");
    }
}
