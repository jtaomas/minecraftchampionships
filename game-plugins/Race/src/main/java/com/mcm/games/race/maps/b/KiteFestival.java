package com.mcm.games.race.maps.b;

import com.mcm.games.race.maps.b.KiteCourse.Hole;
import com.mcm.games.race.maps.b.KiteCourse.K;
import com.mcm.games.race.maps.b.KiteCourse.Piece;
import com.mcm.games.race.maps.b.KiteCourse.Screen;
import com.mcm.games.race.maps.b.KiteCourse.Tube;
import com.mcm.games.race.maps.b.util.G;
import com.mcm.games.race.maps.b.util.Noise;
import com.mcm.games.race.maps.b.util.Noise.Pal;
import com.mcm.games.race.model.Kit;
import com.mcm.games.race.model.MapFeatures;
import com.mcm.games.race.model.MapType;
import com.mcm.games.race.model.TgMap;
import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.Pos;
import com.mcm.games.race.model.TgMapData.Region;
import com.mcm.games.race.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

import static com.mcm.games.race.maps.b.KiteCourse.FIELD_Y;
import static com.mcm.games.race.maps.b.KiteCourse.GANTRY_Y;
import static com.mcm.games.race.maps.b.KiteCourse.HW;
import static com.mcm.games.race.maps.b.KiteCourse.LA;
import static com.mcm.games.race.maps.b.KiteCourse.LD;
import static com.mcm.games.race.maps.b.KiteCourse.crestY;
import static com.mcm.games.race.maps.b.KiteCourse.floorY;

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
    /** Finish row (owner, feedback 3: a whole row of chickens across the field): x 148..182 at z 78. */
    public static final int PEN_X1 = 148, PEN_X2 = 182, PEN_Z1 = 78, PEN_Z2 = 78, CHOKE_Z = 96;

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
        for (int i = 0; i < KiteCourse.SCREENS.size(); i++) gateDeco(g, KiteCourse.SCREENS.get(i), i);
        crestStrings(g);
        skyDecor(g);
        signboard(g);
        g.scenery = false;
        hazards(d, outside);

        d.voidY = FIELD_Y - 20;
        d.playBounds = Region.of(-250, -60, -250, 250, 200, 250);
        d.extraItems.add("minecraft:firework_rocket:" + EXTRA_ROCKETS);
        d.overviewPoints.add(new Spawn(LA, GANTRY_Y + 6, -6.5, 0f, 18f));
        d.overviewPoints.add(new Spawn(0.5, 195, -40.5, 0f, 55f));
        d.notes.put("course", String.format("~%.0f blocks of centre line, gantry y %d -> meadow y %d, 4 legs + 3 hairpins, %d gates",
                KiteCourse.LEN, GANTRY_Y, FIELD_Y, KiteCourse.SCREENS.size()));
        d.notes.put("safeLine", "EASY: lime 9-11 openings, outside of the hairpins, 11x11 tunnel, lowest opening of every stack");
        d.notes.put("fastLine", "MEDIUM: orange 5-7 openings, inside of the hairpins, 5x5 tunnel with two bends; HARD: red 3x3 rings (fire from the ring stack on) and 1-wide mast slits");
        d.notes.put("lastWall", "the last wall over the meadow: one 1-wide slit, " + KiteCourse.LAST_H + " tall from the floor, for every line");
        d.notes.put("crash", "crash kills (owner, feedback 3): a wall slam, a nose dive or a big fall = back to the last checkpoint (config elytra.*)");
        d.notes.put("rockets", "kit " + 2 + " + " + EXTRA_ROCKETS + " extra (TgMapData.extraItems); +" + REFILL + " per gold refill ring (" + String.join(", ", REFILL_SCREENS) + ")");
        d.notes.put("features", feats().movers.size() + " movers (portcullises, gate pendulums, hairpin pendulums, windmills), " + feats().gates.size() + " bonus openings in 5 groups, 5 ledge checkpoints, " + feats().refills.size() + " refill openings");
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
            else { b = mural(s, y, f, c); if (b == null) b = cv[Math.floorMod(si / 3, 2)]; }
            g.set(x, y, z, b);
        }
        g.set(x, c, z, post ? "stripped_dark_oak_log" : "dark_oak_planks");
        if (((x + z) & 1) == 0) g.set(x, c + 1, z, BUNTING[Math.floorMod(si / 2, BUNTING.length)]);
    }

    /**
     * Wall murals (owner, feedback 3: "a lot of blank space"): a big painted diamond kite with a bow tail every 40
     * blocks of wall, on both walls, away from the gates. Returns null for plain canvas.
     */
    static String mural(double s, int y, int f, int c) {
        if (s < 70 || s > 1320) return null;
        int k = (int) Math.round((s - 20) / 40.0);
        double sc = 20 + 40.0 * k;
        for (Screen x : KiteCourse.SCREENS) if (Math.abs(x.s() - sc) < 9) return null;
        double yc = f + (c - f) * 0.62, hs = 6.5, hy = Math.min(10, (c - f) * 0.2);
        double u = (s - sc) / hs, v = (y + 0.5 - yc) / hy;
        double e = Math.abs(u) + Math.abs(v);
        String[] pair = KITE[Math.floorMod(k, KITE.length)];
        if (e <= 1) {
            if (e > 0.86) return "dark_oak_planks";
            if (Math.abs(s - sc) < 0.5 || Math.abs(y + 0.5 - yc) < 0.5) return "stripped_spruce_log";
            return (u >= 0) == (v >= 0) ? pair[0] : pair[1];
        }
        // tail: bows down the wall below the kite
        double ty = yc - hy - 1 - (y + 0.5);
        if (ty > 0 && ty < hy * 1.6) {
            double wob = Math.sin(ty * 0.6) * 1.5;
            if (Math.abs(s - sc - wob) < 0.6) return Math.floorMod((int) ty, 4) < 2 ? "red_wool" : "yellow_wool";
        }
        return null;
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
                int frame = 0;   // distance (Chebyshev) outside the nearest opening: 1 glow ring, 2 colour ring, 3 black outline
                for (Hole h : sc.holes()) {
                    int m = dist(h, x, y, f, c);
                    if (m == 0) { in = h; frame = 0; break; }
                    if (m <= 3 && (in == null || m < frame)) { frame = m; in = h; }
                }
                if (in != null && frame == 0) { g.set(x, y, z, null); continue; }
                boolean slotTop = in != null && in.slot() && y == c;
                if (in != null && !slotTop) {
                    b = frameBlock(sc, in, frame, x, y);
                } else if (y == c) {
                    b = sc.style() == 1 ? "stripped_spruce_log[axis=x]" : "dark_oak_planks";
                } else if (sc.style() == 1) {
                    int m3 = Math.floorMod(x, 3);
                    b = m3 < 2 ? (Math.floorMod(y, 7) == 0 ? "shroomlight" : (m3 == 0 ? "spruce_log" : "stripped_spruce_log"))
                            : new String[]{"purple_wool", "white_wool", "light_blue_wool"}[Math.floorMod(y / 2, 3)];
                } else if (sc.style() == 3) {
                    b = Math.floorMod(x + y, 4) < 2 ? "white_wool" : "black_wool";
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
        labels(g, sc, f, c);
    }

    /** Chebyshev distance of cell (x, y) outside opening h (0 = inside). */
    static int dist(Hole h, int x, int y, int f, int c) {
        int y0 = h.slot() ? f + 1 : h.y0(), y1 = h.slot() ? c - 1 : h.y0() + h.h() - 1;
        int dx = x < h.x0() ? h.x0() - x : x > h.x0() + h.w() - 1 ? x - (h.x0() + h.w() - 1) : 0;
        int dy = y < y0 ? y0 - y : y > y1 ? y - y1 : 0;
        return Math.max(dx, dy);
    }

    /** Line colours (feedback 3, one colour language everywhere): {wool, glow block, flag / glass}. */
    public static String[] tierCol(K k) {
        return switch (k) {
            case SAFE -> new String[]{"lime_wool", "verdant_froglight", "lime_terracotta"};
            case FAST -> new String[]{"orange_wool", "ochre_froglight", "orange_terracotta"};
            case NEEDLE, BRUTAL -> new String[]{"red_wool", "pearlescent_froglight", "red_terracotta"};
            case FINISH -> new String[]{"white_wool", "sea_lantern", "white_terracotta"};
        };
    }

    /**
     * Fire framing (owner, feedback 3: "start off with little amounts of fire"): none in the first half, the hard
     * rings from the ring stack on, and the medium rings too from swing 3 on.
     */
    public static boolean fireHole(Screen sc, Hole h) {
        if (h.slot() || h.k() == K.FINISH || h.k() == K.SAFE) return false;
        double s = sc.s();
        return (h.k().tier == 2 && s >= screen("ring stack").s()) || (h.k().tier == 1 && s >= screen("swing 3").s());
    }

    private String frameBlock(Screen sc, Hole h, int m, int x, int y) {
        String[] col = tierCol(h.k());
        if (m == 1) {
            if (fireHole(sc, h)) return "fire";
            boolean glow = h.k() == K.FINISH || h.w() <= 3 || ((x + y) & 1) == 0;
            return glow ? col[1] : col[0];
        }
        if (m == 2) return refillHole(sc, h) ? (((x + y) & 1) == 0 ? "gold_block" : "glowstone") : col[0];
        return h.k() == K.FINISH ? col[1] : "black_wool";
    }

    /**
     * Big pixel letters E / M / H in the line colour on a black plate next to every opening (above it if there is
     * room, else below), readable for a racer flying into the screen.
     */
    private void labels(G g, Screen sc, int f, int c) {
        if (sc.holes().size() < 2) return;
        int ux = sc.leg() == 0 || sc.leg() == 2 ? -1 : 1;   // a racer flying +Z has -X on the right
        for (Hole h : sc.holes()) {
            if (h.slot() || h.k() == K.FINISH) continue;
            String t = h.k().tier == 0 ? "E" : h.k().tier == 1 ? "M" : "H";
            int w = G.textWidth(t);
            int cx = (int) Math.floor(h.cx());
            int bx0 = cx - w / 2 - 1, bx1 = bx0 + w + 1;
            int[][] boxes = {{h.y0() + h.h() + 3, h.y0() + h.h() + 9}, {h.y0() - 10, h.y0() - 4}};
            for (int[] bb : boxes) {
                boolean ok = bb[0] >= f + 3 && bb[1] <= c - 1;
                for (int x = bx0; x <= bx1 && ok; x++)
                    for (int y = bb[0]; y <= bb[1] && ok; y++)
                        for (Hole o : sc.holes()) if (dist(o, x, y, f, c) <= 3) { ok = false; break; }
                if (!ok) continue;
                for (int x = bx0; x <= bx1; x++) for (int y = bb[0]; y <= bb[1]; y++) g.set(x, y, sc.z(), "black_wool");
                g.text(t, ux < 0 ? bx1 - 1 : bx0 + 1, bb[1] - 1, sc.z(), ux, 0, 1, tierCol(h.k())[1]);
                break;
            }
        }
    }

    /**
     * Gate framing (scenery): a crossbeam over the canyon 8 blocks before every gate with bunting, a lane flag in the
     * line colour hanging over each opening of a side-by-side gate (a tricolour of flags over a stacked one), and
     * lantern poles on the wall tops at both ends of the gate.
     */
    private void gateDeco(G g, Screen sc, int idx) {
        int dir = sc.leg() == 0 || sc.leg() == 2 ? -1 : 1;   // approach side of the plane
        int zb = sc.z() + dir * 8;
        double sb = KiteCourse.sOn(sc.leg(), zb);
        int cb = crestY(sb);
        double lx = KiteCourse.legX(sc.leg());
        double line = Math.max(KiteCourse.safeY(sb), KiteCourse.fastY(sb));
        for (Hole h : sc.holes()) if (!h.slot()) line = Math.max(line, h.y0() + h.h());
        if (cb - line < 10) return;
        int xa = (int) Math.floor(lx - HW - 1), xb = (int) Math.floor(lx + HW + 1);
        for (int x = xa; x <= xb; x++) {
            g.set(x, cb, zb, "stripped_dark_oak_log[axis=x]");
            if ((x & 1) == 0) g.set(x, cb + 1, zb, BUNTING[Math.floorMod(x / 2 + idx, BUNTING.length)]);
        }
        boolean side = true;   // side by side: no two openings overlap in x
        List<Hole> hs = sc.holes();
        for (int a = 0; a < hs.size(); a++)
            for (int b = a + 1; b < hs.size(); b++)
                if (hs.get(a).x0() <= hs.get(b).x0() + hs.get(b).w() - 1 && hs.get(b).x0() <= hs.get(a).x0() + hs.get(a).w() - 1) side = false;
        if (hs.size() >= 2) {
            List<double[]> flags = new ArrayList<>();   // {x, tier kind ordinal}
            if (side) for (Hole h : hs) flags.add(new double[]{h.cx(), h.k().ordinal()});
            else { flags.add(new double[]{lx - 7, K.SAFE.ordinal()}); flags.add(new double[]{lx, K.FAST.ordinal()}); flags.add(new double[]{lx + 7, K.NEEDLE.ordinal()}); }
            for (double[] fl : flags) {
                int fx = (int) Math.floor(fl[0]);
                String[] col = tierCol(K.values()[(int) fl[1]]);
                g.set(fx, cb - 1, zb, "chain");
                g.set(fx, cb - 2, zb, "chain");
                for (int y = cb - 7; y <= cb - 3; y++)
                    for (int x = fx - 1; x <= fx + 1; x++) {
                        boolean pt = y == cb - 7 && x != fx;   // swallow-tail
                        if (!pt) g.set(x, y, zb, (x == fx && y == cb - 5) ? col[1] : col[0]);
                    }
            }
        }
        // lantern poles on both wall tops at the gate itself
        int c = crestY(sc.s());
        if (c + 7 <= 180) for (int sd : new int[]{-1, 1}) {
            int px = (int) Math.floor(lx + sd * (HW + 0.6));
            for (int y = c + 1; y <= c + 6; y++) g.set(px, y, sc.z(), "spruce_fence");
            g.set(px, c + 7, sc.z(), "sea_lantern");
            for (int y = c + 4; y <= c + 6; y++) g.set(px - sd, y, sc.z(), BUNTING[Math.floorMod(idx + y, BUNTING.length)].replace("_carpet", "_wool"));
        }
    }

    /** Lantern strings across the canyon above the crest between the gates (scenery, never below the crest). */
    private void crestStrings(G g) {
        for (double s = 70; s < KiteCourse.sOn(3, 124) - 10; s += 22) {
            boolean near = false;
            for (Screen sc : KiteCourse.SCREENS) if (Math.abs(sc.s() - s) < 12) near = true;
            if (near) continue;
            double[] c0 = KiteCourse.at(s), c1 = KiteCourse.at(s + 1);
            double hx = c1[0] - c0[0], hz = c1[1] - c0[1], hl = Math.hypot(hx, hz);
            double nx = hz / hl, nz = -hx / hl;
            int cr = crestY(s);
            int n = 24;
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n, off = (t - 0.5) * 2 * (HW + 0.8);
                double sag = 2.2 * (1 - Math.pow(2 * t - 1, 2));
                int x = (int) Math.floor(c0[0] + nx * off), z = (int) Math.floor(c0[1] + nz * off);
                int y = Math.min(180, (int) Math.round(cr + 3.4 - sag));
                g.set(x, y, z, "chain");
                if (k % 4 == 2) g.set(x, y - 1, z, k % 8 == 2 ? "lantern[hanging=true]" : BUNTING[Math.floorMod(k / 4 + (int) s, BUNTING.length)].replace("_carpet", "_wool"));
            }
        }
    }

    /**
     * Dead air between the legs (feedback 3; the floating islands and light strings of Hylur, the balloons of a
     * festival): hot-air balloons and floating meadow islands with a tree, outside the canyon (scenery only).
     */
    private void skyDecor(G g) {
        int[][] balloons = {{-112, -170, 150}, {-118, -25, 138}, {-102, 95, 168}, {-6, -150, 95}, {8, 15, 75}, {-4, 150, 105},
                {110, -150, 40}, {104, 40, 25}, {116, 175, 55}, {-225, -120, 172}, {-228, 150, 150}, {228, -140, 30}, {226, 40, 10},
                {-110, 175, 128}, {0, -165, 66}, {110, 140, 12}};
        String[][] env = {{"red_wool", "yellow_wool"}, {"blue_wool", "white_wool"}, {"purple_wool", "yellow_wool"}, {"cyan_wool", "white_wool"},
                {"magenta_wool", "light_blue_wool"}, {"orange_wool", "white_wool"}};
        int i = 0;
        for (int[] b : balloons) {
            if (!clearOfCanyon(b[0], b[1], 9)) continue;
            String[] e = env[i++ % env.length];
            int cx = b[0], cz = b[1], cy = b[2];
            for (int dx = -6; dx <= 6; dx++)
                for (int dz = -6; dz <= 6; dz++)
                    for (int dy = -8; dy <= 7; dy++) {
                        double r = dy >= 0 ? 6 : 6 * Math.sqrt(Math.max(0, 1 - Math.pow(dy / 9.0, 2)));
                        double q = Math.hypot(dx, dz);
                        if (q > r + 0.01 || (dy >= 0 && dx * dx + dz * dz + dy * dy * 0.8 > 37)) continue;
                        if (q < r - 1.3 && dy > -7 && dy < 6) continue;   // hollow shell
                        double ang = Math.atan2(dz, dx);
                        g.set(cx + dx, cy + dy, cz + dz, Math.floorMod((int) Math.floor(ang / (Math.PI / 6)), 2) == 0 ? e[0] : e[1]);
                    }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) { g.set(cx + dx, cy - 14, cz + dz, "spruce_planks"); if (dx != 0 || dz != 0) g.set(cx + dx, cy - 13, cz + dz, "spruce_slab[type=bottom]"); }
            for (int[] k : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) g.tether(cx + k[0] + 0.5, cy - 13 + 0.5, cz + k[1] + 0.5, cx + k[0] * 2 + 0.5, cy - 8 + 0.5, cz + k[1] * 2 + 0.5, "chain");
            g.set(cx, cy - 12, cz, "campfire[lit=true]");
        }
        int[][] islands = {{-108, -120, 128}, {-110, 55, 156}, {2, -100, 60}, {-2, 95, 88}, {112, -70, 15}, {108, 105, 45}, {-226, 10, 142},
                {226, 150, -5}, {226, -60, 20}, {-2, -40, 100}};
        int k = 0;
        for (int[] is : islands) {
            if (!clearOfCanyon(is[0], is[1], 10)) continue;
            int cx = is[0], cz = is[1], top = is[2];
            double rad = 6 + (k++ % 3);
            for (int dx = -9; dx <= 9; dx++)
                for (int dz = -9; dz <= 9; dz++) {
                    double q = Math.hypot(dx, dz) + Noise.hash01(cx + dx, 0, cz + dz, g.seed) * 1.2;
                    if (q > rad) continue;
                    int depth = (int) Math.round((rad - q) * 1.3) + 1;
                    for (int y = top - depth; y <= top; y++) g.set(cx + dx, y, cz + dz, y == top ? "grass_block" : y >= top - 2 ? "dirt" : ROCK.at(cx + dx, y, cz + dz, g.seed));
                    double r = g.rnd(cx + dx, top, cz + dz, 9);
                    if (q < rad - 1 && r < 0.25) g.set(cx + dx, top + 1, cz + dz, r < 0.1 ? "short_grass" : r < 0.15 ? "poppy" : r < 0.2 ? "dandelion" : "cornflower");
                }
            // a small blossom tree and a lantern post
            for (int y = top + 1; y <= top + 5; y++) g.set(cx, y, cz, "cherry_log");
            g.ellipsoid(cx + 0.5, top + 7.5, cz + 0.5, 3.2, 2.4, 3.2, new Pal("cherry_leaves[persistent=true,distance=7]", 1));
            g.set(cx + 3, top + 1, cz + 2, "spruce_fence");
            g.set(cx + 3, top + 2, cz + 2, "lantern[hanging=false]");
        }
    }

    /** True if a column disc of radius r round (x, z) stays outside the canyon walls (+2). */
    private static boolean clearOfCanyon(int x, int z, int r) {
        for (int a = 0; a < 16; a++) {
            double t = a * Math.PI / 8;
            double[] n = KiteCourse.nearest(x + Math.cos(t) * r + 0.5, z + Math.sin(t) * r + 0.5);
            if (n[3] == 0 && n[1] <= HW + 4) return false;
            if (n[3] > 0 && n[3] <= 6 && n[1] <= HW + 4) return false;
        }
        double[] n = KiteCourse.nearest(x + 0.5, z + 0.5);
        return !(n[3] == 0 && n[1] <= HW + 4);
    }

    /**
     * Start signage (feedback 3): a board over the canyon ahead of the gantry (above the crest, behind drop 1) with
     * the colour language and the points: EASY in lime, MEDIUM +1 in orange, HARD +3 in red.
     */
    private void signboard(G g) {
        int z = 26;
        String[] t = {"EASY", "MEDIUM +1", "HARD +3"};
        String[] col = {"lime_wool", "orange_wool", "red_wool"};
        // one board each side of the canyon, outside the walls, facing the gantry (contract box: y <= 180)
        for (int x2 : new int[]{(int) Math.floor(LA - HW - 3), (int) Math.floor(LA + HW + 42)}) {
            int x1 = x2 - 39, top = 179, bot = 160;
            for (int x = x1; x <= x2; x++) for (int y = bot; y <= top; y++) g.set(x, y, z, (x == x1 || x == x2 || y == bot || y == top) ? "dark_oak_planks" : "black_wool");
            for (int x : new int[]{x1 + 2, x2 - 2}) for (int y = bot - 12; y < bot; y++) g.set(x, y, z, "chain[axis=y]");
            int mid = (x1 + x2) / 2;
            for (int i = 0; i < 3; i++) {
                int w = G.textWidth(t[i]);
                g.text(t[i], mid + w / 2, top - 2 - i * 6, z, -1, 0, 1, col[i]);   // a racer looks +Z: text runs towards -X
            }
        }
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
        // finish: a row of chickens across the whole field (racers arrive flying / running -Z)
        com.mcm.games.race.model.FinishRow.build(d, PEN_X1, PEN_X2, y, PEN_Z1, -1, "grass_block");
        g.scenery = true;
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
        return h.k() != K.FINISH;   // every line refills there (the hard line too: it burns the most rockets)
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
                if (h.k().tier > 0) f.gates.add(new MapFeatures.BonusGate(group(sc.seg()), sc.name() + " " + h.k().label().toLowerCase(), h.k().tier, holeBox(sc, h)));
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
        f.crashKills = true;
        f.floorBlocks.addAll(List.of("grass_block", "moss_block", "dirt", "short_grass", "poppy", "dandelion", "cornflower", "oxeye_daisy"));
        // the landing field after the last wall is always fine
        f.floorSafe.add(Region.of((int) Math.floor(LD - HW - 2), FIELD_Y - 3, -250, (int) Math.floor(LD + HW + 2), FIELD_Y + 4, 123));
        FEATURES = f;
        return f;
    }

    /**
     * Moving obstacles (owner, feedback 3: bigger, more, in the centre of the flight path, NOT faster): every gate after
     * the drop run has one, aimed at one line's opening and big enough to close it completely - a portcullis panel
     * (opening + 1 all round) that drops over a side-by-side opening, a pendulum whose ball covers a stacked opening
     * as it swings across it, or a windmill sweeping a whole stack - plus a big pendulum at every hairpin apex
     * swinging across both racing lines. Speeds are capped at the playtest-2 values: portcullis 0.8 blocks/tick,
     * pendulum tip at most L * amplitude / period = 12 * 50 / 90 (same as the old turn pendulums), windmill tips
     * 0.37 blocks/tick. KiteV checks that each one closes its line for >= 20% of the time (no decorative movers)
     * and leaves it open for >= 35% (easy line: >= 50%).
     */
    public static List<MapFeatures.Mover> movers() {
        List<MapFeatures.Mover> m = new ArrayList<>();
        String[][] plan = {
                {"drop 3", "P", "1"}, {"slalom 1", "P", "0"}, {"slalom 2", "P", "2"},
                {"lift 1", "P", "1"}, {"lift 2", "P", "2"}, {"lift 3", "P", "0"},
                {"ring stack", "P", "1"}, {"masts 2", "B", "1"},
                {"low arch", "P", "0"}, {"pull-up 1", "P", "1"}, {"pull-up 2", "P", "2"},
                {"needle 1", "B", "top"}, {"needle 2", "P", "1"}, {"needle 3", "B", "top"},
                {"swing 1", "P", "2"}, {"swing 2", "W", "-1"}, {"swing 3", "P", "0"}, {"swing 4", "B", "top"}};
        int n = 0;
        for (String[] pl : plan) {
            Screen sc = screen(pl[0]);
            int phase = (n++ * 23) % 100;
            if (pl[1].equals("W")) { m.add(windmill(sc, phase)); continue; }
            Hole h = null;
            if (pl[2].equals("top")) { for (Hole o : sc.holes()) if (h == null || o.y0() > h.y0()) h = o; }
            else for (Hole o : sc.holes()) if (o.k().tier == Integer.parseInt(pl[2])) h = o;
            if (h == null) throw new IllegalStateException(pl[0]);
            m.add(pl[1].equals("P") ? portcullis(sc, h, phase) : gatePendulum(sc, h, phase));
        }
        // hairpin apex pendulums: across both lines (inside r 44 / outside r 65), centre r 54.5
        double[][] apex = {{-109.5, 160 + 54.5, 168 + Math.PI * KiteCourse.R / 2}, {0.5, -160 - 54.5, 660.8 + Math.PI * KiteCourse.R / 2},
                {110.5, 160 + 54.5, 1154 + Math.PI * KiteCourse.R / 2}};
        String[] an = {"turn 1 wrecking ball", "turn 2 wrecking ball", "turn 3 wrecking ball"};
        for (int i = 0; i < 3; i++) {
            double s = apex[i][2], y = (KiteCourse.safeY(s) + KiteCourse.fastY(s)) / 2;
            m.add(MapFeatures.Mover.pendulum(an[i], apex[i][0], y + 0.3 + 14, apex[i][1], 'x', 60, 130, 31 * i, ball(14, 3.5)).withTier(-1));
        }
        return m;
    }

    /** Approach direction of a leg's racers along z: +1 for legs flown towards +Z (A, C). */
    static int travel(Screen sc) { return sc.leg() == 0 || sc.leg() == 2 ? 1 : -1; }

    /** A solid portcullis (opening + 1 all round) resting just above a side-by-side opening, 3 blocks in front of the screen; drops at 0.8 b/t. */
    static MapFeatures.Mover portcullis(Screen sc, Hole h, int phase) {
        int w = h.w() + 2, hh = h.h() + 2;
        double pz = sc.z() - travel(sc) * 3.0 + (travel(sc) > 0 ? 0 : 0.5);
        int move = (int) Math.ceil(hh / 0.8);
        boolean easy = h.k().tier == 0;
        int open = easy ? 60 : 45, closed = easy ? 20 : 25;
        String bars = "iron_bars";
        return MapFeatures.Mover.slider(h.k().label().toLowerCase() + " portcullis (" + sc.name() + ")", h.x0() - 1, h.y0() + h.h() + 1, pz, 0, -hh, 0,
                        open, move, closed, phase, List.of(new MapFeatures.Box(0, 0, 0, w, hh, 0.5, bars, true),
                                new MapFeatures.Box(0, hh - 1, -0.1, w, hh, 0.6, tierCol(h.k())[0], true)))
                .withSolid().withSound("block.iron_door.close").withTier(h.k().tier);
    }

    /** A pendulum in front of a stacked opening: the ball (opening + 1) hangs 12 below the pivot, swings sideways across it. */
    static MapFeatures.Mover gatePendulum(Screen sc, Hole h, int phase) {
        double len = 12, half = Math.min(h.w(), 9) / 2.0 + 0.5;
        double cy = h.slot() ? (h.k().tier == 0 ? KiteCourse.safeY(sc.s()) : KiteCourse.fastY(sc.s())) + 0.3 : h.y0() + h.h() / 2.0;
        double pz = sc.z() + 0.5 - travel(sc) * (half + 2);
        double need = h.w() / 2.0 + half + 1.2;
        double amp = Math.max(50, Math.toDegrees(Math.asin(Math.min(0.95, need / len))));
        int period = (int) Math.ceil(Math.max(90, len * amp / (12 * 50 / 90.0)));
        return MapFeatures.Mover.pendulum(h.k().label().toLowerCase() + " pendulum (" + sc.name() + ")", h.cx(), cy + len, pz, 'z', amp, period, phase, ball(len, half - 0.5))
                .withTier(h.k().tier);
    }

    /** A windmill beside a stack: hub 13 to the side level with the hard opening, four 4-wide sails 15 long sweep it; 12.5 s a turn. */
    static MapFeatures.Mover windmill(Screen sc, int phase) {
        Hole h = null;
        for (Hole o : sc.holes()) if (o.k().tier == 2) h = o;
        double lx = KiteCourse.legX(sc.leg());
        double side = h.cx() > lx ? -1 : 1;
        return MapFeatures.Mover.rotor("hard windmill (" + sc.name() + ")", h.cx() + side * 13, h.y0() + h.h() / 2.0, sc.z() + 0.5 - travel(sc) * 3.5, 'z', 20, 250, phase, blades(15, 2))
                .withSound("block.wood.break").withTier(2);
    }

    static Screen screen(String name) {
        for (Screen sc : KiteCourse.SCREENS) if (sc.name().equals(name)) return sc;
        throw new IllegalStateException(name);
    }

    /** Pendulum: chain from the pivot down to a (2r+1)-cube ball centred `len` below the pivot. */
    private static List<MapFeatures.Box> ball(double len, double r) {
        return List.of(new MapFeatures.Box(-0.15, -(len - r), -0.15, 0.15, 0, 0.15, "chain", false),
                new MapFeatures.Box(-r - 0.5, -len - r - 0.5, -r - 0.5, r + 0.5, -len + r + 0.5, r + 0.5, "polished_blackstone", true),
                new MapFeatures.Box(-r, -len - r - 0.7, -r, r, -len - r - 0.5, r, "red_wool", true));
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
