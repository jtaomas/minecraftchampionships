package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;
import com.mcm.games.parkourwarrior.CourseModel.MBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Moving-mechanic obstacles for the imported maps (PLAN_v3 4.3), built from the reference frames:
 * <ul>
 *   <li>Conveyor Deck: striped belts with arrow tiles push sideways (TGTTOS Industry conveyor tables, maps.md 5 #7)</li>
 *   <li>Monkey Bars: crosswise bars hanging on chains under a timber gantry (original_000108_hanging_beams, pw-2 hanging chains)</li>
 *   <li>Dropping Ledges: ledges on a cliff wall that creak and drop (Survivor [1-4], pw-2 000036)</li>
 *   <li>Revolving Doors: turnstiles with door panels turning round a post you stand on (MCC SB silver, pw-1 current_000105)</li>
 *   <li>Swinging Barrels: barrels on chains swing across the gaps between barrel-topped cobble posts (Dojo [M1-2], pw-2 000048)</li>
 *   <li>Pincer Press: piston crushers with hazard stripes slam into the gaps (MCC SB "Pincer Parkour", RACE_FEATURES 7)</li>
 *   <li>Blinking Beams: two sets of lime beams on chains blink in turn (Dojo [3-3] 000512, SB current_000267)</li>
 *   <li>Checkerboard Swap: a black and white tile grid that swaps colour (Survivor final 000542)</li>
 * </ul>
 * Every moving part is telegraphed (glow / particles / ghost blocks / sound, at least 1 s) and every one is checked by
 * {@link Validate} for every start timing (mover sweep, timed phase windows, dropping-ledge deadlines, belts in Phys).
 * Hazard stripes (yellow / black concrete) mark every moving part's frame, as in the Dojo frames.
 */
final class Mechanics {
    private Mechanics() {}

    /** Zone palette: footholds warm/light against darker structure (Clouds 2 readability). */
    record Theme(String name, String body, String top, String trim, String accent, String log, String planks, String slab, String under) {}

    static final Theme MESA = new Theme("mesa", "terracotta", "smooth_red_sandstone", "cut_red_sandstone", "orange_terracotta",
            "stripped_dark_oak_log", "dark_oak_planks", "smooth_red_sandstone_slab", "red_terracotta");
    static final Theme GARDEN = new Theme("garden", "mossy_stone_bricks", "stone_bricks", "spruce_planks", "pink_terracotta",
            "stripped_spruce_log", "spruce_planks", "stone_brick_slab", "mossy_cobblestone");
    static final Theme ICE = new Theme("ice", "packed_ice", "snow_block", "light_blue_concrete", "blue_ice",
            "stripped_spruce_log", "spruce_planks", "smooth_quartz_slab", "packed_ice");
    static final Theme JUNGLE = new Theme("jungle", "mossy_cobblestone", "moss_block", "jungle_planks", "lime_terracotta",
            "stripped_jungle_log", "jungle_planks", "mossy_cobblestone_slab", "mossy_cobblestone");

    static Theme theme(String n) {
        return switch (n == null ? "" : n.toLowerCase(java.util.Locale.ROOT)) {
            case "garden" -> GARDEN;
            case "ice" -> ICE;
            case "jungle" -> JUNGLE;
            default -> MESA;
        };
    }

    static final String Y = "yellow_concrete", K = "black_concrete";

    /** Hazard stripe along a run: alternating yellow / black by position. */
    static String stripe(int a, int b) { return Math.floorMod(a + b, 2) == 0 ? Y : K; }

    // ------------------------------------------------------------------ world transforms for movers

    /** World point of a continuous local point (local cell [x, x+1) maps onto its world cell). */
    static double[] wp(Ob o, double x, double y, double z) {
        double dx = x - 0.5, dz = z - 0.5, rx, rz;
        switch (o.q) {
            case 1 -> { rx = -dz; rz = dx; }
            case 2 -> { rx = -dx; rz = -dz; }
            case 3 -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        return new double[]{o.ox + 0.5 + rx, o.oy + y, o.oz + 0.5 + rz};
    }

    /** Local axis-aligned box (relative to a pivot, local axes) -> world-axes box. */
    static MBox wbox(Ob o, double x0, double y0, double z0, double x1, double y1, double z1, String block, boolean hazard) {
        double ax, az, bx, bz;
        switch (o.q) {
            case 1 -> { ax = -z0; az = x0; bx = -z1; bz = x1; }
            case 2 -> { ax = -x0; az = -z0; bx = -x1; bz = -z1; }
            case 3 -> { ax = z0; az = -x0; bx = z1; bz = -x1; }
            default -> { ax = x0; az = z0; bx = x1; bz = z1; }
        }
        return new MBox(Math.min(ax, bx), y0, Math.min(az, bz), Math.max(ax, bx), y1, Math.max(az, bz), Bs.full(Bs.rot(Bs.full(block), o.q)), hazard);
    }

    /** Local rotation axis -> world axis. */
    static char wAxis(Ob o, char a) { return a == 'y' || (o.q & 1) == 0 ? a : (a == 'x' ? 'z' : 'x'); }

    /** Local horizontal vector -> world. */
    static double[] wv(Ob o, double x, double z) {
        return switch (o.q) {
            case 1 -> new double[]{-z, x};
            case 2 -> new double[]{-x, -z};
            case 3 -> new double[]{z, -x};
            default -> new double[]{x, z};
        };
    }

    static CourseModel.BlockPos bp(Ob o, int x, int y, int z) { return new CourseModel.BlockPos(o.wx(x, z), o.wy(y), o.wz(x, z)); }

    static void endPad(Ob o, Theme t, int z, int y) {
        o.end(0, y, z);
        Sh.padBase(o, 0, y, z, t.trim(), t.body(), t.under(), 3);
    }

    /** A plain support pillar under a foothold so nothing floats. */
    static void pillar(Ob o, int x, int z, int yTop, int depth, String s) {
        for (int y = yTop; y >= yTop - depth; y--) o.soft(x, y, z, s);
    }

    // ================================================================== Conveyor Deck (BELT)

    /**
     * Belts 3 wide and 3 long, alternately pushing left and right (arrow tiles show the way), a striped wall on the
     * downstream side catches anyone who stands still; sprint-jump from belt to belt. lvl 1..3 = gap and push.
     */
    static Ob conveyor(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Conveyor Deck", "BELT");
        double push = lvl == 1 ? 0.05 : lvl == 2 ? 0.07 : 0.09;
        int[][] belts = lvl == 1 ? new int[][]{{4, 0}, {9, 0}, {14, 1}, {19, 1}} : new int[][]{{4, 0}, {10, 0}, {16, 1}, {22, 1}};
        int len = 3;
        for (int i = 0; i < belts.length; i++) {
            int z0 = belts[i][0], y = belts[i][1], dir = i % 2 == 0 ? 1 : -1;
            List<CourseModel.BlockPos> cells = new ArrayList<>();
            String arrow = "magenta_glazed_terracotta[facing=" + (dir > 0 ? "west" : "east") + "]";
            for (int z = z0; z < z0 + len; z++) {
                for (int x = -1; x <= 1; x++) {
                    o.set(x, y, z, arrow);
                    o.set(x, y - 1, z, "smooth_stone");
                    cells.add(bp(o, x, y, z));
                }
                // guard walls on both sides (striped, 3 high: nobody can walk along the rails), frame below
                for (int h = 0; h <= 3; h++) { o.set(-2, y + h, z, h == 3 ? "iron_block" : stripe(z, h)); o.set(2, y + h, z, h == 3 ? "iron_block" : stripe(z, h + 1)); }
                o.set(-2, y - 1, z, "iron_block"); o.set(2, y - 1, z, "iron_block");
            }
            pillar(o, 0, z0 + 1, y - 2, 6, t.body());
            pillar(o, -2, z0 + 1, y - 2, 3, "iron_bars");
            pillar(o, 2, z0 + 1, y - 2, 3, "iron_bars");
            o.set(0, y - 9, z0 + 1, t.under());
            o.area(-1, y, z0, 1, z0 + len - 1);
            double[] v = wv(o, push * dir, 0);
            o.belts.add(new CourseModel.Belt(o.id, cells, v[0], v[1]));
        }
        int last = belts[belts.length - 1][0] + len - 1;
        endPad(o, t, last + 4, belts[belts.length - 1][1] + 1);
        return o;
    }

    // ================================================================== Monkey Bars (HANG)

    /** Crosswise bars on chains under a timber gantry; jump bar to bar (3-gaps, ups and downs). */
    static Ob monkeyBars(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Monkey Bars", "BARS");
        int[][] bars = lvl <= 1 ? new int[][]{{4, 0}, {8, 0}, {12, 1}, {16, 1}, {20, 1}}
                : lvl == 2 ? new int[][]{{4, 0}, {8, 1}, {12, 1}, {16, 2}, {20, 2}, {24, 2}}
                : new int[][]{{4, 0}, {8, 1}, {13, 1}, {17, 2}, {22, 2}};
        int top = 8;
        String log = t.log();
        int zEnd = bars[bars.length - 1][0];
        // gantry: two side rails on posts, crossbeams over each bar
        for (int z = 2; z <= zEnd + 2; z++) for (int sx = -3; sx <= 3; sx += 6) o.set(sx, top, z, log + "[axis=z]");
        for (int z : new int[]{2, zEnd + 2}) for (int sx = -3; sx <= 3; sx += 6) for (int y = -6; y < top; y++) o.set(sx, y, z, log + "[axis=y]");
        for (int z : new int[]{2, zEnd + 2}) for (int sx = -3; sx <= 3; sx += 6) o.set(sx, -7, z, t.body());
        for (int[] b : bars) {
            int z = b[0], y = b[1];
            for (int x = -3; x <= 3; x++) o.set(x, top, z, log + "[axis=x]");
            for (int x = -1; x <= 1; x++) o.set(x, y, z, log + "[axis=x]");
            o.set(-2, y, z, t.planks()); o.set(2, y, z, t.planks());
            Sh.chain(o, -2, y + 1, top - 1, z);
            Sh.chain(o, 2, y + 1, top - 1, z);
            o.area(-1, y, z, 1, z);
            o.safe(-2, y, z, -2, z); o.safe(2, y, z, 2, z);
        }
        o.set(0, top + 1, (2 + zEnd + 2) / 2, "lantern[hanging=false,waterlogged=false]");
        int ly = bars[bars.length - 1][1];
        endPad(o, t, zEnd + 4, ly + (lvl >= 3 ? 1 : 0));
        return o;
    }

    // ================================================================== Dropping Ledges (CRUMBLE)

    /** Ledges on a cliff wall; each drops 1 s (lvl 3: 0.6 s) after a runner lands and comes back 3 s later. */
    static Ob droppingLedges(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Dropping Ledges", "CRUMBLE");
        int delay = lvl >= 4 ? 12 : 20;
        int[][] led = lvl <= 1 ? new int[][]{{4, 0, 0}, {8, 0, 0}, {12, 1, 0}, {16, 1, 0}, {20, 1, 0}}
                : lvl == 2 ? new int[][]{{4, 0, 0}, {8, 1, 0}, {12, 1, 1}, {16, 2, 0}, {20, 2, 1}, {24, 2, 0}}
                : new int[][]{{4, 0, 0}, {9, 1, 0}, {14, 1, 1}, {19, 2, 0}, {24, 2, 1}, {29, 3, 0}};
        int zEnd = led[led.length - 1][0] + 1;
        // the cliff: a thick wall on the left side, taller than any foothold
        for (int z = 1; z <= zEnd + 2; z++) for (int y = -9; y <= 9; y++) for (int x = -5; x <= -3; x++) {
            double n = W.h(o.wx(x, z), y, o.wz(x, z), 17);
            if (x == -5 && n < 0.4) continue;
            if (y < -6 && x == -3 && n < 0.5) continue;
            o.set(x, y, z, n < 0.25 ? t.under() : t.body());
        }
        for (int z = 1; z <= zEnd + 2; z += 3) o.set(-3, 9, z, "flowering_azalea_leaves[distance=1,persistent=true,waterlogged=false]");
        for (int[] l : led) {
            int z = l[0], y = l[1], x0 = -2 + l[2];
            List<CourseModel.BlockPos> cells = new ArrayList<>();
            String st = t.slab() + "[type=top,waterlogged=false]";
            for (int x = -2; x <= x0 + 1; x++) for (int zz = z; zz <= z + 1; zz++) {
                o.set(x, y, zz, st);
                cells.add(bp(o, x, y, zz));
            }
            o.area(-2, y, z, x0 + 1, z + 1);
            o.crumbles.add(new CourseModel.Crumble(o.id, cells, Bs.full(st), delay, 60));
            // hanging vines below each ledge mark them as loose
            o.set(x0, y - 1, z, "vine[east=false,north=false,south=false,up=true,west=false]");
        }
        endPad(o, t, zEnd + 3, led[led.length - 1][1] + 1);
        return o;
    }

    // ================================================================== Revolving Doors (ROTOR)

    /**
     * Turnstiles: four door panels turn round a central post; jump onto the post between two panels (the post top is
     * inside the panels' inner radius, so it is a safe waiting spot), then jump off when the next gap comes round.
     */
    static Ob revolvingDoors(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Revolving Doors", "DOORS");
        int period = lvl <= 1 ? 200 : lvl == 2 ? 160 : 120;
        int gap = lvl <= 1 ? 2 : 3;
        int z = 2;
        // platform A, post 1, platform B, post 2, end pad
        int pa = z + gap + 1;                      // platform A z..z+1
        for (int x = -1; x <= 1; x++) for (int zz = pa; zz <= pa + 1; zz++) { o.set(x, 0, zz, t.top()); o.set(x, -1, zz, t.body()); }
        pillar(o, 0, pa, -2, 5, t.body());
        o.area(-1, 0, pa, 1, pa + 1);
        int zz = pa + 1;
        for (int d = 0; d < 2; d++) {
            int post = zz + gap + 1;
            door(o, t, post, period, d * period / 8);
            int nb = post + gap + 1;
            if (d == 0) {
                for (int x = -1; x <= 1; x++) for (int q = nb; q <= nb + 1; q++) { o.set(x, 0, q, t.top()); o.set(x, -1, q, t.body()); }
                pillar(o, 0, nb, -2, 5, t.body());
                o.area(-1, 0, nb, 1, nb + 1);
                zz = nb + 1;
            } else {
                endPad(o, t, nb + 1, 0);
            }
        }
        return o;
    }

    static void door(Ob o, Theme t, int post, int period, int phase) {
        // the post: 1x1 standing top, a column to a base below
        o.set(0, 0, post, "chiseled_stone_bricks");
        for (int y = -1; y >= -6; y--) o.set(0, y, post, y == -6 ? t.under() : "polished_andesite");
        o.area(0, 0, post, 0, post);
        // overhead bearing frame outside the door radius (4.2), carried on posts
        int top = 6;
        for (int x = -5; x <= 5; x++) o.set(x, top, post, t.log() + "[axis=x]");
        for (int sx = -5; sx <= 5; sx += 10) for (int y = -6; y < top; y++) o.set(sx, y, post, t.log() + "[axis=y]");
        for (int sx = -5; sx <= 5; sx += 10) o.set(sx, -7, post, t.under());
        o.set(0, top - 1, post, "lightning_rod[facing=down,powered=false,waterlogged=false]");
        double[] pv = wp(o, 0.5, 1.0, post + 0.5);
        List<MBox> boxes = new ArrayList<>();
        for (int a = 0; a < 4; a++) {
            // arm a points along +x, +z, -x, -z of the mover frame (the rotor turns about y)
            double[] r = switch (a) {
                case 0 -> new double[]{1.25, 3.35, -0.15, 0.15};
                case 1 -> new double[]{-0.15, 0.15, 1.25, 3.35};
                case 2 -> new double[]{-3.35, -1.25, -0.15, 0.15};
                default -> new double[]{-0.15, 0.15, -3.35, -1.25};
            };
            boxes.add(new MBox(r[0], 0.0, r[2], r[1], 2.0, r[3], "stripped_spruce_wood[axis=y]", true));
            // a yellow edge strip on the outer end (the door's push edge) and a spoke up to the axle
            double ex0 = r[0], ex1 = r[1], ez0 = r[2], ez1 = r[3];
            if (a == 0) { ex0 = 3.15; } else if (a == 1) { ez0 = 3.15; } else if (a == 2) { ex1 = -3.15; } else { ez1 = -3.15; }
            boxes.add(new MBox(ex0 - 0.02, 0.0, ez0 - 0.02, ex1 + 0.02, 2.02, ez1 + 0.02, Y, true));
            double sx0 = Math.min(0, r[0]), sx1 = Math.max(0, r[1]), sz0 = Math.min(0, r[2]), sz1 = Math.max(0, r[3]);
            boxes.add(new MBox(sx0, 2.9, sz0, sx1, 3.1, sz1, "dark_oak_planks", false));
        }
        boxes.add(new MBox(-0.12, 3.1, -0.12, 0.12, 5.0, 0.12, "chain[axis=y,waterlogged=false]", false));
        o.movers.add(CourseModel.Mover.rotor(o.id, "Revolving Doors", pv[0], pv[1], pv[2], 'y', 0, period, phase, boxes));
    }

    // ================================================================== Swinging Barrels (PENDULUM)

    /**
     * Barrel-topped cobblestone posts (the Dojo frame) with a barrel on chains swinging across every gap; the posts
     * are outside the swing, so each is a safe waiting spot.
     */
    static Ob swingingBarrels(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Swinging Barrels", "SWING");
        int[][] posts = lvl >= 3 ? new int[][]{{4, 0}, {8, 1}, {12, 1}, {16, 2}, {20, 2}, {25, 2}}
                : new int[][]{{4, 0}, {8, 0}, {12, 1}, {16, 1}, {20, 1}};
        int period = lvl >= 3 ? 50 : 70;
        int beam = 9;
        for (int i = 0; i < posts.length; i++) {
            int z = posts[i][0], y = posts[i][1];
            o.set(0, y, z, "barrel[facing=up,open=false]");
            for (int yy = y - 1; yy >= y - 7; yy--) o.set(0, yy, z, yy == y - 1 ? Y : yy == y - 2 ? K : "cobblestone");
            o.set(0, y - 8, z, "cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
            o.area(0, y, z, 0, z);
            if (i == 0) continue;
            // the swing between this post and the previous one
            int zp = posts[i - 1][0];
            double zm = (zp + z) / 2.0 + 0.5;
            int ybar = Math.max(posts[i - 1][1], y);
            for (int x = -4; x <= 4; x++) o.set(x, beam, (int) Math.floor(zm), t.log() + "[axis=x]");
            for (int sx = -4; sx <= 4; sx += 8) for (int yy = -6; yy < beam; yy++) o.set(sx, yy, (int) Math.floor(zm), t.log() + "[axis=y]");
            for (int sx = -4; sx <= 4; sx += 8) o.set(sx, -7, (int) Math.floor(zm), t.under());
            double len = beam - (ybar + 1) - 0.4;   // chain length down to the barrel centre ~ 1.4 above the jump line
            double[] pv = wp(o, 0.5, beam, zm);
            List<MBox> boxes = new ArrayList<>();
            boxes.add(wbox(o, -0.1, -len + 0.5, -0.1, 0.1, 0.0, 0.1, "chain[axis=y,waterlogged=false]", false));
            boxes.add(wbox(o, -0.5, -len - 0.5, -0.5, 0.5, -len + 0.5, 0.5, "barrel[facing=up,open=false]", true));
            o.movers.add(CourseModel.Mover.pendulum(o.id, "Swinging Barrels", pv[0], pv[1], pv[2], wAxis(o, 'z'), 62, period, (i * period) / 3, boxes));
        }
        int last = posts[posts.length - 1][0];
        endPad(o, t, last + 4, posts[posts.length - 1][1] + (lvl >= 3 ? 1 : 0));
        return o;
    }

    // ================================================================== Pincer Press (SLIDER)

    /** Piston crushers drop into each gap; their heads glow red for a second before they close. */
    static Ob pincerPress(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Pincer Press", "PRESS");
        int[][] pads = lvl <= 1 ? new int[][]{{4, 0}, {8, 0}, {12, 1}, {16, 1}, {20, 1}} : new int[][]{{4, 0}, {8, 1}, {12, 1}, {16, 2}, {20, 2}};
        int open = lvl <= 1 ? 44 : 34, closed = 14, move = 5;
        int top = 8;
        for (int i = 0; i < pads.length; i++) {
            int z = pads[i][0], y = pads[i][1];
            for (int x = -1; x <= 1; x++) { o.set(x, y, z, t.top()); o.set(x, y - 1, z, t.body()); }
            o.set(-1, y, z, stripe(z, 0)); o.set(1, y, z, stripe(z, 1));
            pillar(o, 0, z, y - 2, 5, t.body());
            o.area(-1, y, z, 1, z);
            if (i == 0) continue;
            int zp = pads[i - 1][0];
            int zg = (zp + z) / 2;          // the gap's middle cell
            int ytop = Math.max(y, pads[i - 1][1]);
            // housing: a striped beam across on two posts, the piston body above the gap
            for (int x = -3; x <= 3; x++) o.set(x, top, zg, x == 0 ? "piston[extended=false,facing=down]" : stripe(x, zg));
            for (int sx = -3; sx <= 3; sx += 6) for (int yy = -6; yy < top; yy++) o.set(sx, yy, zg, "iron_block");
            for (int sx = -3; sx <= 3; sx += 6) o.set(sx, -7, zg, t.under());
            double[] pv = wp(o, 0.5, top, zg + 0.5);
            double drop = top - (ytop + 1) + 0.2;     // the head comes down to just below the jump line
            List<MBox> boxes = new ArrayList<>();
            boxes.add(wbox(o, -1.5, -1.0, -0.75, 1.5, 0.0, 0.75, "piston_head[facing=down,short=false,type=normal]", true));
            boxes.add(wbox(o, -0.15, 0.0, -0.15, 0.15, 0.0001, 0.15, "iron_bars", false));
            o.movers.add(CourseModel.Mover.slider(o.id, "Pincer Press", pv[0], pv[1], pv[2], 0, -drop, 0, open, move, closed, i * 17, boxes));
        }
        int last = pads[pads.length - 1][0];
        endPad(o, t, last + 4, pads[pads.length - 1][1] + 1);
        return o;
    }

    // ================================================================== Blinking Beams (TIMED, two phases)

    /** Two sets of lime beams on chains alternate on and off; hop diagonally to the set that is about to stay. */
    static Ob blinkingBeams(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Blinking Beams", "BLINK");
        int half = lvl <= 1 ? 40 : lvl == 2 ? 30 : 22;
        int warn = Math.max(20, half / 2);
        int sp = lvl >= 3 ? 4 : 3;
        int[][] beams = {{-1, 4, 0}, {1, 4 + sp, 0}, {-1, 4 + 2 * sp, 1}, {1, 4 + 3 * sp, 1}, {-1, 4 + 4 * sp, 1}, {1, 4 + 5 * sp, 2}};
        int top = 8;
        for (int i = 0; i < beams.length; i++) {
            int x = beams[i][0], z = beams[i][1], y = beams[i][2];
            int[][] cells = {{x, y, z}, {x, y, z + 1}};
            o.timed("lime_concrete", 2 * half, half, i % 2 == 0 ? 0 : half, warn, "lime_stained_glass", cells);
            // chains to the gantry from the beam's outer side, a hanger bracket under the gantry
            Sh.chain(o, x * 2, y + 1, top - 1, z);
            Sh.chain(o, x * 2, y + 1, top - 1, z + 1);
            o.set(x * 2, y, z, "lime_terracotta"); o.set(x * 2, y, z + 1, "lime_terracotta");
            o.safe(x * 2, y, z, x * 2, z + 1);
        }
        int zEnd = 4 + 5 * sp + 2;
        for (int z = 3; z <= zEnd; z++) for (int sx = -3; sx <= 3; sx += 6) o.set(sx, top, z, t.log() + "[axis=z]");
        for (int z = 3; z <= zEnd; z += 2) for (int x = -2; x <= 2; x++) o.set(x, top, z, t.log() + "[axis=x]");
        for (int z : new int[]{3, zEnd}) for (int sx = -3; sx <= 3; sx += 6) { for (int y = -6; y < top; y++) o.set(sx, y, z, t.log() + "[axis=y]"); o.set(sx, -7, z, t.under()); }
        endPad(o, t, zEnd + 3, 3);
        return o;
    }

    // ================================================================== Checkerboard Swap (TIMED grid)

    /** A spaced black and white tile grid; every few seconds the colours swap (the vanishing colour flickers first). */
    static Ob checkerboard(W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = new Ob(w, s, "Checkerboard Swap", "CHECKER");
        int half = lvl >= 4 ? 22 : lvl >= 3 ? 26 : 36;
        int warn = 20;
        // tiles on a 2-cell grid (1-block gaps between them); the route zigzags across it, alternating colours. Only
        // the route tiles exist (other tiles would be alternative routes with the same timing).
        int z0 = 4, rows = 8;
        int step = lvl >= 4 ? 4 : lvl >= 3 ? 3 : 2;
        int zMax = z0 + (rows - 1) * step;
        for (int[] c : new int[][]{{-4, z0 - 1}, {4, z0 - 1}, {-4, zMax + 1}, {4, zMax + 1}})
            for (int y = -7; y <= 11; y++) o.set(c[0], y, c[1], y == 11 ? "lantern[hanging=false,waterlogged=false]" : "polished_blackstone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        // route: one tile per row, alternating colour
        int[] path = lvl >= 3 ? new int[]{0, 2, 0, -2, 0, 2, 0, -2} : new int[]{0, 2, 0, -2, 0, 2, 0, -2};
        for (int r = 0; r < rows; r++) {
            int c = path[r];
            int z = z0 + r * step, y = r / 3;
            boolean isBlack = r % 2 == 0;
            o.timed(isBlack ? "black_concrete" : "white_concrete", 2 * half, half, isBlack ? 0 : half, warn,
                    isBlack ? "black_stained_glass" : "white_stained_glass", new int[][]{{c, y, z}});
        }
        endPad(o, t, zMax + 4, (rows - 1) / 3 + 1);
        return o;
    }

    // ------------------------------------------------------------------ registry for course.txt "builtin" lines

    /** Build a mechanic by key (conveyor, bars, ledges, doors, barrels, press, beams, checker); null if unknown. */
    static Ob make(String key, W w, Ob.Slot s, Theme t, int lvl) {
        Ob o = make0(key, w, s, t, lvl);
        if (o != null) o.imported = true;     // scenery-rich built objects: decoy / object-size findings are warnings, bypass stays an error
        return o;
    }

    private static Ob make0(String key, W w, Ob.Slot s, Theme t, int lvl) {
        return switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "conveyor" -> conveyor(w, s, t, lvl);
            case "bars", "monkeybars" -> monkeyBars(w, s, t, lvl);
            case "ledges", "droppingledges" -> droppingLedges(w, s, t, lvl);
            case "doors", "revolvingdoors" -> revolvingDoors(w, s, t, lvl);
            case "barrels", "swingingbarrels" -> swingingBarrels(w, s, t, lvl);
            case "press", "pincerpress", "crushers" -> pincerPress(w, s, t, lvl);
            case "beams", "blinkingbeams" -> blinkingBeams(w, s, t, lvl);
            case "checker", "checkerboard" -> checkerboard(w, s, t, lvl);
            default -> null;
        };
    }
}
