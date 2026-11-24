package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.KiteCourse.Hole;
import com.mcm.games.tgttos.maps.b.KiteCourse.K;
import com.mcm.games.tgttos.maps.b.KiteCourse.Piece;
import com.mcm.games.tgttos.maps.b.KiteCourse.Screen;
import com.mcm.games.tgttos.maps.b.KiteCourse.Tube;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Noise;
import com.mcm.games.tgttos.maps.b.util.Noise.Pal;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapFeatures;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

import static com.mcm.games.tgttos.maps.b.KiteCourse.FIELD_Y;
import static com.mcm.games.tgttos.maps.b.KiteCourse.GANTRY_Y;
import static com.mcm.games.tgttos.maps.b.KiteCourse.HW;
import static com.mcm.games.tgttos.maps.b.KiteCourse.LA;
import static com.mcm.games.tgttos.maps.b.KiteCourse.LD;
import static com.mcm.games.tgttos.maps.b.KiteCourse.crestY;
import static com.mcm.games.tgttos.maps.b.KiteCourse.floorY;

/**
 * Kite Festival (elytra), v2. A ~1,400-block elytra course folded into the slot as a festival canyon: four legs
 * and three hairpins between tall striped windbreaks, descending from a launch gantry at local y 170 to a landing
 * meadow at y -30. Every gate is a giant kite banner stretched wall to wall with colour-coded openings (lime =
 * safe 9-11, orange = fast 6-7, red = needle 4-5), plus a tunnel split, a ring stack, a mast forest and a red-glass
 * finish frame. The canyon has a meadow floor: a racer who loses height lands instead of dying and relaunches with a
 * rocket, or climbs one of five wall ledges. Leaving the canyon (over a windbreak) is a hazard: back to the start.
 *
 * <p>Plan data (centre line, altitude profiles, screens, tubes) lives in {@link KiteCourse}; see the README section.
 */
public final class KiteFestival implements TgMap {

    @Override public String id() { return "kite_festival"; }
    @Override public String displayName() { return "Kite Festival"; }
    @Override public MapType type() { return MapType.ELYTRA; }
    @Override public Kit kit() { return Kit.ELYTRA_FIREWORKS; }

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    /** Extra rockets on top of the kit's {@code kit.fireworks} (2): 6 up front; gold refill rings add 2 each (playtest 2). */
    public static final int EXTRA_ROCKETS = 4;

    /** Decoy chickens on hay bales in the landing field (no name tag; punching them does nothing). */
    public static final List<int[]> DECOYS = List.of(new int[]{151, FIELD_Y + 2, 112}, new int[]{180, FIELD_Y + 2, 106},
            new int[]{149, FIELD_Y + 2, 90}, new int[]{181, FIELD_Y + 2, 88});

    /** Owner (playtest 2): decoys wander the later part of the map (runtime scatter); the hay bales stay as field props. */
    @Override public List<int[]> decoyChickens() { return List.of(); }

    /** Wall ledges (relaunch points): {x, z of the ladder, platform y, inward dx, inward dz}. Filled by generate. */
    public static final List<int[]> LEDGES = new ArrayList<>();
    /** Hazard boxes outside the canyon (filled by generate; also in TgMapData.hazardRegions). */
    public static final int PEN_X1 = 160, PEN_X2 = 171, PEN_Z1 = 72, PEN_Z2 = 83, CHOKE_Z = 96;

    static final Pal GRASS = new Pal("grass_block", 10, "moss_block", 1);
    static final Pal ROCK = new Pal("stone", 5, "andesite", 3, "tuff", 2, "cobblestone", 1);
    static final Pal ROCK_LOW = new Pal("deepslate", 3, "tuff", 3, "cobbled_deepslate", 2, "andesite", 1);
    /** Windbreak canvas stripe colours per centre-line piece. */
    static final String[][] CANVAS = {{"white_wool", "yellow_wool"}, {"white_wool", "yellow_wool"}, {"white_wool", "light_blue_wool"},
            {"white_wool", "light_blue_wool"}, {"white_wool", "pink_wool"}, {"white_wool", "pink_wool"}, {"white_wool", "purple_wool"}};
    static final String[] BUNTING = {"red_carpet", "yellow_carpet", "light_blue_carpet", "lime_carpet", "magenta_carpet", "orange_carpet"};
    /** Kite banner colour pairs (avoid the ring colours lime / orange / red). */
    static final String[][] KITE = {{"white_wool", "purple_wool"}, {"white_wool", "cyan_wool"}, {"white_wool", "magenta_wool"},
            {"light_blue_wool", "purple_wool"}, {"yellow_wool", "cyan_wool"}};

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        LEDGES.clear();
        boolean[][] outside = new boolean[501][501];
        canyon(g, outside);
        for (int i = 0; i < KiteCourse.SCREENS.size(); i++) screen(g, KiteCourse.SCREENS.get(i), i);
        for (Tube t : KiteCourse.TUBES) tube(g, t);
        gantry(g);
        ledges(g);
        field(g);
        g.scenery = true;
        floorProps(g);
        skyKites(g);
        g.scenery = false;
        hazards(d, outside);

        d.voidY = FIELD_Y - 20;
        d.playBounds = Region.of(-250, -60, -250, 250, 200, 250);
        d.extraItems.add("minecraft:firework_rocket:" + EXTRA_ROCKETS);
        d.overviewPoints.add(new Spawn(LA, GANTRY_Y + 6, -6.5, 0f, 18f));
        d.overviewPoints.add(new Spawn(0.5, 195, -40.5, 0f, 55f));
        d.notes.put("course", String.format("~%.0f blocks of centre line, gantry y %d -> meadow y %d, 4 legs + 3 hairpins, %d gates",
                KiteCourse.LEN, GANTRY_Y, FIELD_Y, KiteCourse.SCREENS.size()));
        d.notes.put("safeLine", "lime 9-11 openings, outside of the hairpins, 11x11 tunnel, low ring of the stack, slalom round the needles");
        d.notes.put("fastLine", "orange 6-7 openings on the inside, 5x5 tunnel with two bends, high red 5 of the stack, three 4x4 red needles");
        d.notes.put("rockets", "kit " + 2 + " + " + EXTRA_ROCKETS + " extra (TgMapData.extraItems); +" + REFILL + " per gold refill ring (" + String.join(", ", REFILL_SCREENS) + ")");
        d.notes.put("features", "6 movers (3 pendulums, 2 rotors, 1 shutter), " + feats().gates.size() + " bonus openings in 5 groups, 5 ledge checkpoints, " + feats().refills.size() + " refill openings");
        d.hazardBlocks.add("minecraft:fire");
        d.notes.put("failure", "landing on the meadow floor is not a fail: relaunch with a rocket or climb a wall ledge; leaving the canyon = back to start");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ canyon: floor + windbreak walls

    private void canyon(G g, boolean[][] outside) {
        for (int x = -250; x <= 250; x++)
            for (int z = -250; z <= 250; z++) {
                double[] n = KiteCourse.nearest(x + 0.5, z + 0.5);
                double s = n[0], dist = n[1], cap = n[3];
                Piece p = KiteCourse.PIECES.get((int) n[2]);
                double bw = p.arc() ? 1.6 : 1.0;
                boolean in = cap == 0 && dist <= HW;
                boolean wall = !in && (cap == 0 ? dist <= HW + bw : cap <= 3 && dist <= HW + bw);
                if (in) floorCol(g, x, z, s, dist);
                else if (wall) wallCol(g, x, z, s, (int) n[2]);
                else outside[x + 250][z + 250] = cap > 0 ? cap > 4 || dist > HW + 2.5 : dist > HW + 2.5;
            }
    }

    private void floorCol(G g, int x, int z, double s, double dist) {
        int f = floorY(s);
        g.set(x, f, z, GRASS.at(x, f, z, g.seed));
        double r = g.rnd(x, f, z, 3);
        if (r < 0.06) g.set(x, f + 1, z, r < 0.025 ? "short_grass" : r < 0.035 ? "poppy" : r < 0.045 ? "dandelion" : r < 0.053 ? "cornflower" : "oxeye_daisy");
        // the floor steps down along the course: fill the riser so the meadow never shows a crack
        int fn = floorY(s + 1.5);
        if (fn < f) g.set(x, f - 1, z, "dirt");
    }

    private void wallCol(G g, int x, int z, double s, int piece) {
        int f = floorY(s), c = crestY(s), fb = floorY(s + 4) - 2;
        int si = (int) Math.floor(s);
        double nz = Noise.fbm2(s * 0.08, piece * 7.0, g.seed + 41);
        int rockTop = f + 7 + (int) Math.round(nz * 6);
        String[] cv = CANVAS[piece];
        boolean post = Math.floorMod(si, 9) == 0;
        for (int y = fb; y < c; y++) {
            String b;
            if (y <= rockTop) b = y < f + 3 ? ROCK_LOW.at(x, y, z, g.seed) : y == rockTop ? "moss_block" : ROCK.at(x, y, z, g.seed);
            else if (post) b = Math.floorMod(y, 6) == 0 ? "shroomlight" : "spruce_log";
            else b = cv[Math.floorMod(si / 3, 2)];
            g.set(x, y, z, b);
        }
        g.set(x, c, z, post ? "stripped_dark_oak_log" : "dark_oak_planks");
        if (((x + z) & 1) == 0) g.set(x, c + 1, z, BUNTING[Math.floorMod(si / 2, BUNTING.length)]);
    }

    // ------------------------------------------------------------------ kite banner screens

    private void screen(G g, Screen sc, int idx) {
        double s = sc.s();
        int f = floorY(s), c = crestY(s), z = sc.z();
        double lx = KiteCourse.legX(sc.leg());
        int xa = (int) Math.floor(lx - HW - 1), xb = (int) Math.floor(lx + HW + 1);
        String[] kite = KITE[idx % KITE.length];
        double cy = (f + c) / 2.0, ha = HW - 0.5, hb = (c - f) / 2.0 - 1;
        int door = (int) Math.floor(lx + 16);
        for (int x = xa; x <= xb; x++)
            for (int y = f + 1; y <= c; y++) {
                String b = null;
                Hole in = null;
                int frame = 0;   // 1 = inner ring, 2 = outer ring
                for (Hole h : sc.holes()) {
                    int y0 = h.slot() ? f + 1 : h.y0(), y1 = h.slot() ? c - 1 : h.y0() + h.h() - 1;
                    int dx = x < h.x0() ? h.x0() - x : x > h.x0() + h.w() - 1 ? x - (h.x0() + h.w() - 1) : 0;
                    int dy = y < y0 ? y0 - y : y > y1 ? y - y1 : 0;
                    int m = Math.max(dx, dy);
                    if (m == 0) { in = h; frame = 0; break; }
                    if (m <= 2 && (frame == 0 || m < frame)) { frame = m; in = h; }
                }
                if (in != null && frame == 0) { g.set(x, y, z, null); continue; }
                boolean fire = sc.name().equals("ring stack") && (in != null && (in.k() == K.NEEDLE || in.k() == K.BRUTAL));
                if (in != null && frame == 1 && !(in.slot() && (y == c))) {
                    b = fire ? "fire" : refillHole(sc, in) ? (ringBlock(in, x, y, f, c).contains("froglight") ? "glowstone" : "gold_block") : ringBlock(in, x, y, f, c);
                } else if (in != null && frame == 2 && in.k() != K.FINISH && sc.style() != 1) {
                    b = fire ? "netherrack" : "black_wool";
                } else if (y == c) {
                    b = sc.style() == 1 ? "stripped_spruce_log[axis=x]" : "dark_oak_planks";
                } else if (sc.style() == 1) {
                    int m3 = Math.floorMod(x, 3);
                    b = m3 < 2 ? (Math.floorMod(y, 7) == 0 ? "shroomlight" : (m3 == 0 ? "spruce_log" : "stripped_spruce_log"))
                            : new String[]{"red_wool", "white_wool", "yellow_wool"}[Math.floorMod(y / 2, 3)];
                } else if (sc.style() == 3) {
                    b = Math.floorMod(x + y, 4) < 2 ? "white_wool" : "red_wool";
                } else {
                    double u = (x + 0.5 - lx) / ha, v = (y + 0.5 - cy) / hb;
                    double e = Math.abs(u) + Math.abs(v);
                    if (e <= 1) {
                        if (e > 0.93) b = "dark_oak_planks";
                        else if (Math.abs(x + 0.5 - lx) < 0.6 || Math.abs(y + 0.5 - cy) < 0.6) b = "stripped_spruce_log";
                        else b = (u >= 0) == (v >= 0) ? kite[0] : kite[1];
                    } else b = sc.style() == 2 ? (Math.floorMod(y, 4) == 0 ? "light_blue_wool" : "blue_wool") : "blue_wool";
                }
                // walk-through door at the floor (for racers who landed)
                if (sc.style() != 1 && sc.style() != 3 && x == door && y <= f + 2) b = null;
                else if (sc.style() != 1 && sc.style() != 3 && x == door && y == f + 3 && in == null) b = "spruce_planks";
                g.set(x, y, z, b);
            }
        // bunting on the crest beam, a lantern each end of the frames
        g.scenery = true;
        for (int x = xa; x <= xb; x++) if (Math.floorMod(x, 2) == 0) g.set(x, c + 1, z, BUNTING[Math.floorMod(x / 2 + idx, BUNTING.length)]);
        g.scenery = false;
    }

    private String ringBlock(Hole h, int x, int y, int f, int c) {
        int y0 = h.slot() ? f + 1 : h.y0(), y1 = h.slot() ? c - 1 : h.y0() + h.h() - 1;
        boolean corner = (x == h.x0() - 1 || x == h.x0() + h.w()) && (y == y0 - 1 || y == y1 + 1);
        boolean glow = corner || (h.slot() && Math.floorMod(y, 5) == 0) || (!h.slot() && h.w() >= 9 && (x == Math.floor(h.cx()) || y == y0 + h.h() / 2));
        return switch (h.k()) {
            case SAFE -> glow ? "verdant_froglight" : "lime_wool";
            case FAST -> glow ? "ochre_froglight" : "orange_wool";
            case NEEDLE -> glow ? "pearlescent_froglight" : "red_wool";
            case BRUTAL -> glow || (h.w() == 1 && (x + y) % 2 == 0) ? "pearlescent_froglight" : "red_concrete";
            case FINISH -> corner || Math.floorMod(x + y, 3) == 0 ? "sea_lantern" : "red_stained_glass";
        };
    }

    // ------------------------------------------------------------------ tunnel tubes

    private void tube(G g, Tube t) {
        boolean safe = t.k() == K.SAFE;
        int th = safe ? 1 : 2;
        for (int z = 63; z <= 131; z++) {
            int x0 = t.x0(z), y0 = t.y0(z), w = t.inner();
            for (int x = x0 - th; x < x0 + w + th; x++)
                for (int y = y0 - th; y < y0 + w + th; y++) {
                    boolean inside = x >= x0 && x < x0 + w && y >= y0 && y < y0 + w;
                    if (inside) { g.set(x, y, z, null); continue; }
                    boolean ringIn = x >= x0 - 1 && x <= x0 + w && y >= y0 - 1 && y <= y0 + w;
                    String b;
                    if (safe) b = Math.floorMod(z, 6) == 0 ? "verdant_froglight" : (y == y0 - 1 ? "moss_block" : "lime_wool");
                    else b = ringIn ? (Math.floorMod(z, 4) == 0 ? "ochre_froglight" : "orange_wool") : "spruce_planks";
                    g.set(x, y, z, b);
                }
        }
    }

    // ------------------------------------------------------------------ launch gantry

    private void gantry(G g) {
        TgMapData d = g.d;
        int y = GANTRY_Y;
        int x1 = (int) Math.floor(LA - HW + 0.5), x2 = (int) Math.floor(LA + HW - 0.5);   // -183 .. -147
        for (int x = x1; x <= x2; x++)
            for (int z = 0; z <= 6; z++) {
                g.set(x, y, z, ((x + z) & 1) == 0 ? "spruce_planks" : "birch_planks");
                d.dropFloor.add(new Pos(x, y, z));
            }
        for (int yy = y + 1; yy <= y + 3; yy++)
            for (int x = x1; x <= x2; x++) { g.set(x, yy, 7, "glass"); d.startBarrier.add(new Pos(x, yy, 7)); }
        g.scenery = true;
        for (int yy = y + 1; yy <= y + 4; yy++) {
            for (int z = -1; z <= 7; z++) { g.set(x1 - 1, yy, z, "glass"); g.set(x2 + 1, yy, z, "glass"); }
            for (int x = x1 - 1; x <= x2 + 1; x++) g.set(x, yy, -1, yy == y + 4 ? "spruce_slab[type=bottom]" : "glass");
        }
        for (int x = x1 - 1; x <= x2 + 1; x++) g.set(x, y, -1, "dark_oak_planks");
        for (int z = -1; z <= 7; z++) { g.set(x1 - 1, y, z, "dark_oak_planks"); g.set(x2 + 1, y, z, "dark_oak_planks"); }
        int fl = floorY(0);
        // back legs are logs; the front legs are fences so the view down the course (and to the goal kite) stays open
        for (int[] l : new int[][]{{x1 - 2, -2}, {x2 + 2, -2}})
            for (int yy = fl - 2; yy <= y + 5; yy++) g.set(l[0], yy, l[1], Math.floorMod(yy, 6) == 0 ? "stripped_dark_oak_log" : "dark_oak_log");
        for (int[] l : new int[][]{{x1 - 2, 8}, {x2 + 2, 8}})
            for (int yy = fl - 2; yy <= y + 5; yy++) g.set(l[0], yy, l[1], "dark_oak_fence");
        for (int x = x1 - 3; x <= x2 + 3; x++)
            for (int z = -3; z <= 9; z++) g.set(x, y + 6, z, Math.floorMod(x / 3, 2) == 0 ? "red_wool" : "white_wool");
        for (int x = x1 - 3; x <= x2 + 3; x += 2) g.set(x, y + 7, 9, BUNTING[Math.floorMod(x / 2, BUNTING.length)]);
        for (int x = x1 + 1; x <= x2; x += 6) g.set(x, y + 5, 8, "lantern[hanging=true]");
        // sign behind the back rail
        g.text("KITE FEST", (int) Math.floor(LA) - 16, y + 5, -2, 1, 0, 1, "yellow_wool");
        g.scenery = false;
        g.startSpawns(x1, x2, 0, 6, y + 1, 15, 3);
        d.startStrip = Region.of(x1, y + 1, 0, x2, y + 4, 6);
    }

    // ------------------------------------------------------------------ wall ledges (relaunch + shoving spots)

    private void ledges(G g) {
        // turn mids (outer wall) and two on the long legs, each 1 below the safe line there
        for (int[] l : LEDGE_SPOTS) ledge(g, l[0], l[1], l[2], l[3]);
    }

    /** Ladder cell (lx, lz) in front of a tower against the wall; (ix, iz) = inward direction (towards the canyon). */
    private void ledge(G g, int lx, int lz, int ix, int iz) {
        double s = KiteCourse.nearest(lx + 0.5, lz + 0.5)[0];
        int f = floorY(s);
        int py = ledgePy(lx, lz);
        int tx = -iz, tz = ix;  // tangential
        String facing = ix > 0 ? "east" : ix < 0 ? "west" : iz > 0 ? "south" : "north";
        // tower: 3 wide (tangential) x 2 deep behind the ladder, floor to platform
        for (int t = -1; t <= 1; t++)
            for (int k = 1; k <= 2; k++)
                for (int y = f + 1; y <= py; y++)
                    g.set(lx + t * tx - k * ix, y, lz + t * tz - k * iz, Math.floorMod(y, 5) == 0 ? "stripped_spruce_log" : "spruce_planks");
        for (int y = f + 1; y <= py; y++) g.set(lx, y, lz, "ladder[facing=" + facing + "]");
        // platform 5 wide x 5 deep (towards the canyon), open edges: a shoving spot
        for (int t = -2; t <= 2; t++)
            for (int k = -2; k <= 2; k++) {
                int x = lx + t * tx + k * ix, z = lz + t * tz + k * iz;
                if (t == 0 && k == 0) continue;
                g.set(x, py, z, ((t + k) & 1) == 0 ? "spruce_planks" : "stripped_spruce_wood");
            }
        g.scenery = true;
        // beacon pole with a lamp and a flag at the back corner
        int bx = lx + 2 * tx - 2 * ix, bz = lz + 2 * tz - 2 * iz;
        for (int y = py + 1; y <= py + 6; y++) g.set(bx, y, bz, "spruce_fence");
        g.set(bx, py + 7, bz, "sea_lantern");
        g.set(bx + ix, py + 6, bz + iz, "lime_wool");
        g.set(bx + ix, py + 5, bz + iz, "lime_wool");
        g.scenery = false;
        LEDGES.add(new int[]{lx, lz, py, ix, iz});
    }

    // ------------------------------------------------------------------ landing field, choke, pen, landmark

    private void field(G g) {
        TgMapData d = g.d;
        int y = FIELD_Y;
        int xa = (int) Math.floor(LD - HW + 0.5), xb = (int) Math.floor(LD + HW - 0.5);   // 147 .. 183
        // choke: a 2-high hedge across the field with one 3-wide gap in the middle and 2-wide gaps at the walls
        for (int x = xa; x <= xb; x++) {
            boolean gap = (x >= 164 && x <= 166) || x <= xa + 1 || x >= xb - 1;
            if (gap) continue;
            for (int yy = y + 1; yy <= y + 2; yy++) g.set(x, yy, CHOKE_Z, "oak_leaves[persistent=true,distance=7]");
            if (Math.floorMod(x, 6) == 0) g.set(x, y + 3, CHOKE_Z, "lantern[hanging=false]");
        }
        // pen: low hedge ring, two gates facing the field
        int x1 = PEN_X1 - 1, x2 = PEN_X2 + 1, z1 = PEN_Z1 - 1, z2 = PEN_Z2 + 1;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                if (!(x == x1 || x == x2 || z == z1 || z == z2)) continue;
                boolean gate = z == z2 && ((x >= 161 && x <= 163) || (x >= 168 && x <= 170));
                if (!gate) g.set(x, y + 1, z, "oak_leaves[persistent=true,distance=7]");
            }
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) if (x > x1 && x < x2 && z > z1 && z < z2) g.set(x, y, z, "grass_block");
        d.pen = Region.of(PEN_X1, y + 1, PEN_Z1, PEN_X2, y + 3, PEN_Z2);
        g.chickens(d.pen, 12);
        g.scenery = true;
        for (int[] dc : DECOYS) g.set(dc[0], dc[1] - 1, dc[2], "hay_block[axis=y]");
        int[][] hay = {{150, 118}, {181, 114}, {148, 86}, {182, 76}, {152, 64}, {178, 62}};
        for (int[] h : hay) { g.set(h[0], y + 1, h[1], "hay_block[axis=x]"); g.set(h[0], y + 1, h[1] + 1, "hay_block[axis=x]"); }
        // picnic blankets
        int[][] pic = {{153, 108, 0}, {175, 102, 1}, {150, 74, 2}};
        String[] cp = {"red_carpet", "white_carpet", "yellow_carpet", "white_carpet"};
        for (int[] p : pic) for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++) g.set(p[0] + dx, y + 1, p[1] + dz, cp[(dx + dz + p[2]) & 3]);
        // landmark: a giant kite on a mast behind the pen (visible from the gantry over the low end of the course)
        int mx = 165, mz = 58;
        for (int yy = y + 1; yy <= 150; yy++) { g.set(mx, yy, mz, "stripped_spruce_log"); g.set(mx + 1, yy, mz, "stripped_spruce_log"); }
        int ky = 160, hw = 9, hh = 13;
        for (int dx = -hw; dx <= hw + 1; dx++)
            for (int dy = -hh; dy <= hh; dy++) {
                double e = Math.abs(dx - 0.5) / (hw + 0.5) + Math.abs(dy) / (double) hh;
                if (e > 1.0001) continue;
                String b = e > 0.88 ? "yellow_wool" : (dx > 0) == (dy >= 0) ? "red_wool" : "white_wool";
                g.set(mx + dx, ky + dy, mz + 1, b);
            }
        for (int dy = -hh; dy <= hh; dy++) g.set(mx, ky + dy, mz + 2, "spruce_fence");
        for (int i = 0; i < 14; i++) g.set(mx + (int) Math.round(Math.sin(i * 0.7) * 2), ky - hh - 1 - i, mz + 1, i % 3 == 2 ? "yellow_wool" : "red_wool");
        g.text("GOAL", mx - 6, ky + 3, mz + 3, 1, 0, 1, "black_wool");
        // a second sail across it (plane x = mx - 1) faces the gantry, far up the course to the west: a box-kite cross
        for (int dz = -hw; dz <= hw; dz++)
            for (int dy = -hh; dy <= hh; dy++) {
                double e = Math.abs(dz) / (hw + 0.5) + Math.abs(dy) / (double) hh;
                if (e > 1.0001 || dz == 1 || dz == 2) continue;
                String b = e > 0.88 ? "yellow_wool" : (dz > 0) == (dy >= 0) ? "white_wool" : "red_wool";
                g.set(mx - 1, ky + dy, mz + 1 + dz, b);
            }
        g.landmark.add(new double[]{mx - 1.4, ky + 0.5, mz + 1.5 - 5});
        g.landmark.add(new double[]{mx - 1.4, ky - 6.5, mz + 1.5 - 4});
        g.scenery = false;
    }

    // ------------------------------------------------------------------ props

    private boolean nearGate(double s, double r) {
        for (Screen sc : KiteCourse.SCREENS) if (Math.abs(sc.s() - s) < r) return true;
        double t0 = KiteCourse.sOn(1, 132), t1 = KiteCourse.sOn(1, 62);
        return s > t0 - 3 && s < t1 + 3;
    }

    /** Festival stalls and lantern posts on the canyon floor, near the walls, far below the flight lines. */
    private void floorProps(G g) {
        String[][] roofs = {{"red_wool", "white_wool"}, {"blue_wool", "white_wool"}, {"yellow_wool", "white_wool"}, {"purple_wool", "white_wool"}};
        int k = 0;
        for (double s = 24; s < KiteCourse.sOn(3, 110); s += 9) {
            double[] c = KiteCourse.at(s), c2 = KiteCourse.at(s + 1);
            double hx = c2[0] - c[0], hz = c2[1] - c[1], hl = Math.hypot(hx, hz);
            hx /= hl; hz /= hl;
            double nx = hz, nz = -hx;
            int side = (k & 1) == 0 ? 1 : -1;
            k++;
            int f = floorY(s);
            if (floorY(s - 4) != f || floorY(s + 4) != f) continue;
            // lantern post every slot
            int px = (int) Math.floor(c[0] + side * 17 * nx), pz = (int) Math.floor(c[1] + side * 17 * nz);
            if (nearGate(s, 3)) continue;
            g.set(px, f + 1, pz, "spruce_fence");
            g.set(px, f + 2, pz, "spruce_fence");
            g.set(px, f + 3, pz, "lantern[hanging=false]");
            if (Math.floorMod(k, 5) != 0 || nearGate(s, 9)) continue;
            // stall: 4 x 3 with a striped roof, open towards the canyon
            int sx = (int) Math.floor(c[0] + side * 12 * nx), sz = (int) Math.floor(c[1] + side * 12 * nz);
            String[] rf = roofs[Math.floorMod(k / 5, roofs.length)];
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    if (corner) for (int yy = f + 1; yy <= f + 3; yy++) g.set(sx + dx, yy, sz + dz, "spruce_fence");
                    g.set(sx + dx, f + 4, sz + dz, Math.floorMod(dx, 2) == 0 ? rf[0] : rf[1]);
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) g.set(sx + dx, f + 5, sz + dz, Math.floorMod(dx, 2) == 0 ? rf[0] : rf[1]);
                }
            g.set(sx, f + 1, sz, "barrel[facing=up]");
            g.set(sx + 1, f + 1, sz, "hay_block[axis=y]");
            g.set(sx, f + 3, sz, "lantern[hanging=true]");
        }
    }

    /** Big decorative kites flying over the gaps between the legs, tethered to the windbreak crests (scenery). */
    private void skyKites(G g) {
        int[][] ks = {{-110, 40, 150}, {-110, -90, 118}, {0, 60, 110}, {0, -60, 96}, {110, -20, 74}, {110, 100, 60},
                {-215, 80, 160}, {-215, -60, 140}, {218, 120, 70}, {218, -40, 66}, {-60, 215, 140}, {60, -215, 100}};
        int i = 0;
        for (int[] k : ks) {
            int x0 = k[0], z0 = k[1], y0 = Math.min(168, k[2]);
            String a = KITE[i % KITE.length][0], b = i % 2 == 0 ? "red_wool" : "orange_wool";
            i++;
            for (int dx = -4; dx <= 4; dx++)
                for (int dy = -6; dy <= 6; dy++) {
                    double e = Math.abs(dx) / 4.0 + Math.abs(dy) / 6.0;
                    if (e > 1.0001) continue;
                    g.set(x0 + dx, y0 + dy, z0, e > 0.8 ? "dark_oak_planks" : (dx >= 0) == (dy >= 0) ? a : b);
                }
            for (int t = 1; t <= 8; t++) g.set(x0 + ((t & 1) == 0 ? 0 : 1), y0 - 6 - t, z0, t % 2 == 0 ? "white_wool" : b);
            // tether to the nearest windbreak crest
            double best = 1e9; int tx = x0, tz = z0;
            for (int dx = -40; dx <= 40; dx += 2)
                for (int dz = -30; dz <= 30; dz += 2) {
                    double[] n = KiteCourse.nearest(x0 + dx + 0.5, z0 + dz + 0.5);
                    if (n[3] != 0 || n[1] <= HW || n[1] > HW + 1) continue;
                    double dd = dx * dx + dz * dz;
                    if (dd < best) { best = dd; tx = x0 + dx; tz = z0 + dz; }
                }
            if (best < 1e9) {
                int cy = crestY(KiteCourse.nearest(tx + 0.5, tz + 0.5)[0]);
                if (cy + 1 < y0 - 6) g.tether(x0 + 0.5, y0 - 6, z0 + 0.5, tx + 0.5, cy + 1.5, tz + 0.5, "chain");
            }
        }
    }

    // ------------------------------------------------------------------ hazard boxes outside the canyon

    private void hazards(TgMapData d, boolean[][] outside) {
        int cs = 5, n = 500 / cs;
        boolean[][] cell = new boolean[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                boolean all = true;
                for (int a = 0; a < cs && all; a++) for (int b = 0; b < cs && all; b++) all = outside[i * cs + a][j * cs + b];
                cell[i][j] = all;
            }
        // greedy rectangles: runs along x per row, extended along z while identical
        boolean[][] used = new boolean[n][n];
        for (int j = 0; j < n; j++)
            for (int i = 0; i < n; i++) {
                if (!cell[i][j] || used[i][j]) continue;
                int i2 = i;
                while (i2 + 1 < n && cell[i2 + 1][j] && !used[i2 + 1][j]) i2++;
                int j2 = j;
                outer:
                while (j2 + 1 < n) {
                    for (int a = i; a <= i2; a++) if (!cell[a][j2 + 1] || used[a][j2 + 1]) break outer;
                    j2++;
                }
                for (int a = i; a <= i2; a++) for (int b = j; b <= j2; b++) used[a][b] = true;
                d.hazardRegions.add(Region.of(i * cs - 250, -64, j * cs - 250, (i2 + 1) * cs - 251, 320, (j2 + 1) * cs - 251));
            }
    }

    // ------------------------------------------------------------------ interactive features (playtest 2)

    /** Wall ledges {ladder x, ladder z, inward dx, inward dz}: relaunch points and the 5 checkpoints. */
    static final int[][] LEDGE_SPOTS = {{-110, 231, 0, -1}, {0, -232, 0, 1}, {110, 231, 0, -1}, {-39, -49, -1, 0}, {39, -20, 1, 0}};
    static final String[] LEDGE_NAMES = {"Turn 1 ledge", "Turn 2 ledge", "Turn 3 ledge", "Tether Forest ledge", "Needle ledge"};

    /** Ledge platform y (1 below the safe line there - 1). */
    static int ledgePy(int lx, int lz) { return (int) Math.round(KiteCourse.safeY(KiteCourse.nearest(lx + 0.5, lz + 0.5)[0])) - 2; }

    /** Screens whose openings refill rockets (gold frames): +REFILL each, once per opening per attempt. */
    public static final List<String> REFILL_SCREENS = List.of("tunnel exit", "ring stack", "low arch", "swing 2");
    public static final int REFILL = 2;

    /** Bonus group of a segment (about five per map, so the orange line earns ~5 and the red / brutal line hits the cap). */
    public static String group(String seg) {
        return switch (seg) {
            case "Drop Gate Run", "Canyon Slalom" -> "opening";
            case "Tunnel Split", "Lift Hill" -> "tunnel & lift";
            case "Ring Stack", "Tether Forest" -> "stack & masts";
            case "Dive & Pull-up", "Needle" -> "dive & needle";
            default -> "S-canyon";
        };
    }

    public static boolean refillHole(Screen sc, Hole h) {
        if (!REFILL_SCREENS.contains(sc.name())) return false;
        return !sc.name().equals("ring stack") || h.k() == K.SAFE || h.k() == K.FAST;
    }

    /** Opening cells as a region at the screen plane (slots: floor + 1 .. crest - 1). */
    public static Region holeBox(Screen sc, Hole h) {
        int f = floorY(sc.s()), c = crestY(sc.s());
        int y0 = h.slot() ? f + 1 : h.y0(), y1 = h.slot() ? c - 1 : h.y0() + h.h() - 1;
        return Region.of(h.x0(), y0, sc.z(), h.x0() + h.w() - 1, y1, sc.z());
    }

    private static MapFeatures FEATURES;

    @Override public MapFeatures features() { return feats(); }

    public static synchronized MapFeatures feats() {
        if (FEATURES != null) return FEATURES;
        MapFeatures f = new MapFeatures();
        for (Screen sc : KiteCourse.SCREENS)
            for (Hole h : sc.holes()) {
                if (h.k().tier > 0) f.gates.add(new MapFeatures.BonusGate(group(sc.seg()), sc.name() + " " + h.k().name().toLowerCase(), h.k().tier, holeBox(sc, h)));
                if (refillHole(sc, h)) f.refills.add(new MapFeatures.Refill(sc.name() + " ring", holeBox(sc, h), REFILL));
            }
        // the fast tube itself is an orange choice (its mouth is in the gate list above as a FAST hole)
        // checkpoints: the five wall ledges, ordered along the course; crossing the canyon at a ledge saves it
        List<int[]> ord = new ArrayList<>();
        for (int i = 0; i < LEDGE_SPOTS.length; i++) ord.add(new int[]{i, (int) Math.round(KiteCourse.nearest(LEDGE_SPOTS[i][0] + 0.5, LEDGE_SPOTS[i][1] + 0.5)[0])});
        ord.sort((a, b) -> Integer.compare(a[1], b[1]));
        int order = 0;
        for (int[] o : ord) {
            int[] l = LEDGE_SPOTS[o[0]];
            double s = o[1];
            double[] c = KiteCourse.at(s), c2 = KiteCourse.at(s + 1);
            boolean alongZ = Math.abs(c2[1] - c[1]) > Math.abs(c2[0] - c[0]);
            int fl = floorY(s), cr = crestY(s);
            Region trig = alongZ
                    ? Region.of((int) Math.floor(c[0] - HW), fl, (int) Math.floor(c[1]) - 1, (int) Math.floor(c[0] + HW), cr, (int) Math.floor(c[1]) + 1)
                    : Region.of((int) Math.floor(c[0]) - 1, fl, (int) Math.floor(c[1] - HW), (int) Math.floor(c[0]) + 1, cr, (int) Math.floor(c[1] + HW));
            int py = ledgePy(l[0], l[1]);
            int tx = -l[3], tz = l[2];
            float yaw = (float) Math.toDegrees(Math.atan2(-(c2[0] - c[0]), c2[1] - c[1]));
            List<Spawn> sp = new ArrayList<>();
            for (int t = -1; t <= 1; t++) sp.add(new Spawn(l[0] + t * tx + l[2] + 0.5, py + 1, l[1] + t * tz + l[3] + 0.5, yaw, 0f));
            f.checkpoints.add(new MapFeatures.Checkpoint(order++, LEDGE_NAMES[o[0]], trig, sp, List.of()));
        }
        f.movers.addAll(movers());
        FEATURES = f;
        return f;
    }

    /**
     * Six moving hazards, all on the orange / red side of the canyon and clear of the lime line at every phase
     * (KiteV proves it): two pendulum wrecking balls over the hairpin insides and one after the slalom, a fan rotor in
     * front of the fast lift 6s, a windmill rotor below the swing-3 orange ring, and a portcullis shutter on needle 3.
     */
    public static List<MapFeatures.Mover> movers() {
        List<MapFeatures.Mover> m = new ArrayList<>();
        // wrecking ball after slalom 2 (leg A, +Z): swings along the course over the fast line (x -152), 5 s, +-65 deg
        m.add(MapFeatures.Mover.pendulum("wrecking ball", -152, KiteCourse.fastY(KiteCourse.sOn(0, 150)) + 12.3, 150, 'x', 65, 100, 0, ball(12, 1.5)));
        // turn 1 / turn 2 pendulums at the hairpin apexes, over the inside line (r 44), swinging radially (r 34..54)
        double s1 = 168 + Math.PI * KiteCourse.R / 2, s2 = 660.8 + Math.PI * KiteCourse.R / 2;
        m.add(MapFeatures.Mover.pendulum("turn 1 pendulum", -109, KiteCourse.fastY(s1) + 12.3, 160 + KiteCourse.R_FAST, 'x', 50, 90, 20, ball(12, 2)));
        m.add(MapFeatures.Mover.pendulum("turn 2 pendulum", 1, KiteCourse.fastY(s2) + 12.3, -160 - KiteCourse.R_FAST, 'x', 50, 90, 45, ball(12, 2)));
        // lift 2 fan: 4 blades 2 x 10 round a hub 7 above the fast 6 (leg B, -Z, z 10), 8 s per turn
        Hole lf = screen("lift 2").get(K.FAST);
        m.add(MapFeatures.Mover.rotor("lift fan", lf.cx(), lf.y0() + lf.h() / 2.0 + 7, 13.5, 'z', 0, 160, 0, blades(10, 1)).withSound("block.grindstone.use"));
        // windmill below the swing 3 orange ring (leg C, +Z, z 84): 4 arms 2 x 12, hub 8 below the ring, 10 s per turn
        Hole sw = screen("swing 3").get(K.FAST);
        m.add(MapFeatures.Mover.rotor("windmill", sw.cx(), sw.y0() + sw.h() / 2.0 - 8, 80.5, 'z', 45, 200, 0, blades(12, 1)).withSound("block.wood.break"));
        // needle 3 shutter: a 4 x 4 portcullis in front of the red 4x4, open 2 s, closes in 0.25 s, shut 1 s
        Hole nd = screen("needle 3").get(K.NEEDLE);
        m.add(MapFeatures.Mover.slider("needle shutter", nd.x0(), nd.y0() + nd.h(), -2.6, 0, -nd.h(), 0, 40, 5, 20, 0,
                List.of(new MapFeatures.Box(0, 0, 0, nd.w(), nd.h(), 0.5, "iron_bars", true))).withSound("block.iron_door.close"));
        return m;
    }

    private static Screen screen(String name) {
        for (Screen sc : KiteCourse.SCREENS) if (sc.name().equals(name)) return sc;
        throw new IllegalStateException(name);
    }

    /** Pendulum: chain from the pivot down to a (2r+1)-cube ball centred `len` below the pivot. */
    private static List<MapFeatures.Box> ball(double len, double r) {
        return List.of(new MapFeatures.Box(-0.15, -(len - r), -0.15, 0.15, 0, 0.15, "chain", false),
                new MapFeatures.Box(-r - 0.5, -len - r - 0.5, -r - 0.5, r + 0.5, -len + r + 0.5, r + 0.5, "polished_blackstone", true),
                new MapFeatures.Box(-r, -len - r - 0.7, -r, r, -len - r - 0.5, r, "red_concrete", true));
    }

    /** Rotor: hub + four blades `len` long, 2*hw wide (in the rotor plane), 1 thick. */
    private static List<MapFeatures.Box> blades(double len, double hw) {
        return List.of(new MapFeatures.Box(-1.5, -1.5, -0.6, 1.5, 1.5, 0.6, "yellow_concrete", true),
                new MapFeatures.Box(-hw, 1.5, -0.5, hw, len, 0.5, "white_concrete", true),
                new MapFeatures.Box(-hw, -len, -0.5, hw, -1.5, 0.5, "orange_concrete", true),
                new MapFeatures.Box(1.5, -hw, -0.5, len, hw, 0.5, "white_concrete", true),
                new MapFeatures.Box(-len, -hw, -0.5, -1.5, hw, 0.5, "orange_concrete", true));
    }
}
