package com.mcm.games.parkourwarrior.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tick-by-tick Java 1.21.1 player movement (no effects, sprint key held) against the real collision shapes of the
 * generated blocks. Port of {@code tools/sim/pksim.py} (research: parkourwarrior_design.md Part 3.1) extended to 3D:
 * vanilla order per tick (aiStep velocity cut-off, jump + sprint boost, moveRelative with ground/air speed,
 * climbable clamp, Entity.move with Y first then the larger horizontal axis, step-up 0.6, axis zeroing on collision,
 * slime bounce / default vertical stop, slime stepOn slow-down, honey / soul-sand speed factor, ladder climb 0.2 when
 * pushing into a wall or holding jump, gravity 0.08 and drag 0.98, horizontal drag friction*0.91 / 0.91).
 * Sprinting stops for a tick on a non-minor horizontal collision (LocalPlayer). Player box 0.6 x 1.8.
 * Pure Java; used only by the offline validators.
 */
final class Phys {
    private Phys() {}

    static final double HW = 0.3, PH = 1.8, EPS = 1.0E-7;
    static final double[][] NONE = new double[0][];
    static final double[][] FULL = {{0, 0, 0, 1, 1, 1}};

    // ------------------------------------------------------------------ shapes

    private static final Map<String, double[][]> SHAPES = new HashMap<>();

    private static double[] b16(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new double[]{x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16};
    }

    /** Collision boxes of a block state in cell-local coordinates (y may reach 1.5). */
    static double[][] shape(String s) {
        if (s == null) return NONE;
        double[][] r = SHAPES.get(s);
        if (r == null) { r = compute(s); SHAPES.put(s, r); }
        return r;
    }

    private static String p(String s, String k) { String v = Bs.prop(s, k); return v == null ? "" : v; }

    private static double[][] compute(String s) {
        String id = Bs.id(s);
        if (Bs.isAir(s)) return NONE;
        if (id.equals("ladder")) {
            switch (p(s, "facing")) {
                case "north": return new double[][]{b16(0, 0, 13, 16, 16, 16)};
                case "south": return new double[][]{b16(0, 0, 0, 16, 16, 3)};
                case "west": return new double[][]{b16(13, 0, 0, 16, 16, 16)};
                default: return new double[][]{b16(0, 0, 0, 3, 16, 16)};
            }
        }
        if (id.equals("powder_snow") || id.equals("cobweb") || id.equals("scaffolding") && false) return NONE;
        if (Bs.passable(s)) return NONE;
        if (id.endsWith("_slab")) {
            String t = p(s, "type");
            if (t.equals("bottom")) return new double[][]{{0, 0, 0, 1, 0.5, 1}};
            if (t.equals("top")) return new double[][]{{0, 0.5, 0, 1, 1, 1}};
            return FULL;
        }
        if (id.endsWith("_stairs")) {
            boolean top = p(s, "half").equals("top");
            double[] slab = top ? new double[]{0, 0.5, 0, 1, 1, 1} : new double[]{0, 0, 0, 1, 0.5, 1};
            double y0 = top ? 0 : 0.5, y1 = top ? 0.5 : 1;
            double[] step;
            switch (p(s, "facing")) {
                case "north": step = new double[]{0, y0, 0, 1, y1, 0.5}; break;
                case "south": step = new double[]{0, y0, 0.5, 1, y1, 1}; break;
                case "east": step = new double[]{0.5, y0, 0, 1, y1, 1}; break;
                default: step = new double[]{0, y0, 0, 0.5, y1, 1};
            }
            return new double[][]{slab, step};
        }
        if (id.endsWith("_carpet")) return new double[][]{{0, 0, 0, 1, 1 / 16.0, 1}};
        if (id.equals("snow")) {
            int n = 1;
            try { n = Integer.parseInt(p(s, "layers")); } catch (NumberFormatException ignored) { }
            return n <= 1 ? NONE : new double[][]{{0, 0, 0, 1, (n - 1) * 2 / 16.0, 1}};
        }
        int k = Bs.kind(id);
        if (k == 1 || k == 2) {
            double a = k == 1 ? 6 : 7, b = k == 1 ? 10 : 9, h = k == 1 ? 24 : 16;
            List<double[]> l = new ArrayList<>();
            l.add(b16(a, 0, a, b, h, b));
            if (p(s, "north").equals("true")) l.add(b16(a, 0, 0, b, h, a));
            if (p(s, "south").equals("true")) l.add(b16(a, 0, b, b, h, 16));
            if (p(s, "west").equals("true")) l.add(b16(0, 0, a, a, h, b));
            if (p(s, "east").equals("true")) l.add(b16(b, 0, a, 16, h, b));
            return l.toArray(new double[0][]);
        }
        if (k == 3) {
            List<double[]> l = new ArrayList<>();
            if (!p(s, "up").equals("false")) l.add(b16(4, 0, 4, 12, 24, 12));
            if (!p(s, "north").equals("none") && !p(s, "north").isEmpty()) l.add(b16(5, 0, 0, 11, 24, 11));
            if (!p(s, "south").equals("none") && !p(s, "south").isEmpty()) l.add(b16(5, 0, 5, 11, 24, 16));
            if (!p(s, "west").equals("none") && !p(s, "west").isEmpty()) l.add(b16(0, 0, 5, 11, 24, 11));
            if (!p(s, "east").equals("none") && !p(s, "east").isEmpty()) l.add(b16(5, 0, 5, 16, 24, 11));
            if (l.isEmpty()) l.add(b16(4, 0, 4, 12, 24, 12));
            return l.toArray(new double[0][]);
        }
        if (k == 4) {
            if (p(s, "open").equals("true")) return NONE;
            String f = p(s, "facing");
            return f.equals("north") || f.equals("south") ? new double[][]{b16(0, 0, 6, 16, 24, 10)} : new double[][]{b16(6, 0, 0, 10, 24, 16)};
        }
        if (id.equals("chain")) {
            switch (p(s, "axis")) {
                case "x": return new double[][]{b16(0, 6.5, 6.5, 16, 9.5, 9.5)};
                case "z": return new double[][]{b16(6.5, 6.5, 0, 9.5, 9.5, 16)};
                default: return new double[][]{b16(6.5, 0, 6.5, 9.5, 16, 9.5)};
            }
        }
        if (id.equals("end_rod") || id.equals("lightning_rod")) {
            String f = p(s, "facing");
            if (f.equals("north") || f.equals("south")) return new double[][]{b16(6, 6, 0, 10, 10, 16)};
            if (f.equals("east") || f.equals("west")) return new double[][]{b16(0, 6, 6, 16, 10, 10)};
            return new double[][]{b16(6, 0, 6, 10, 16, 10)};
        }
        if (id.equals("lantern") || id.equals("soul_lantern")) {
            if (p(s, "hanging").equals("true")) return new double[][]{b16(5, 1, 5, 11, 8, 11), b16(6, 8, 6, 10, 10, 10)};
            return new double[][]{b16(5, 0, 5, 11, 7, 11), b16(6, 7, 6, 10, 9, 10)};
        }
        if (id.endsWith("_trapdoor")) {
            if (p(s, "open").equals("true")) {
                switch (p(s, "facing")) {
                    case "north": return new double[][]{b16(0, 0, 13, 16, 16, 16)};
                    case "south": return new double[][]{b16(0, 0, 0, 16, 16, 3)};
                    case "west": return new double[][]{b16(13, 0, 0, 16, 16, 16)};
                    default: return new double[][]{b16(0, 0, 0, 3, 16, 16)};
                }
            }
            return p(s, "half").equals("top") ? new double[][]{b16(0, 13, 0, 16, 16, 16)} : new double[][]{b16(0, 0, 0, 16, 3, 16)};
        }
        if (id.equals("chest") || id.equals("trapped_chest") || id.equals("ender_chest")) return new double[][]{b16(1, 0, 1, 15, 14, 15)};
        if (id.equals("flower_pot") || id.startsWith("potted_")) return new double[][]{b16(5, 0, 5, 11, 6, 11)};
        if ((id.endsWith("_head") || id.endsWith("_skull")) && !id.contains("wall")) return new double[][]{b16(4, 0, 4, 12, 8, 12)};
        if (id.equals("enchanting_table")) return new double[][]{b16(0, 0, 0, 16, 12, 16)};
        if (id.endsWith("_bed")) return new double[][]{b16(0, 0, 0, 16, 9, 16)};
        if (id.equals("honey_block")) return new double[][]{b16(1, 0, 1, 15, 15, 15)};
        if (id.equals("soul_sand") || id.equals("mud")) return new double[][]{b16(0, 0, 0, 16, 14, 16)};
        if (id.equals("dirt_path") || id.equals("farmland")) return new double[][]{b16(0, 0, 0, 16, 15, 16)};
        if (id.endsWith("campfire")) return new double[][]{b16(0, 0, 0, 16, 7, 16)};
        if (id.startsWith("daylight_detector")) return new double[][]{b16(0, 0, 0, 16, 6, 16)};
        if (id.equals("cake")) return new double[][]{b16(1, 0, 1, 15, 8, 15)};
        if (id.endsWith("candle")) return new double[][]{b16(7, 0, 7, 9, 6, 9)};
        if (id.equals("sea_pickle")) return new double[][]{b16(6, 0, 6, 10, 6, 10)};
        if (id.equals("lily_pad")) return new double[][]{b16(1, 0, 1, 15, 1.5, 15)};
        if (id.equals("decorated_pot")) return new double[][]{b16(1, 0, 1, 15, 16, 15)};
        if (id.equals("conduit")) return new double[][]{b16(5, 5, 5, 11, 11, 11)};
        if (id.equals("bamboo")) return new double[][]{b16(6.5, 0, 6.5, 9.5, 16, 9.5)};
        if (id.equals("amethyst_cluster") || id.endsWith("amethyst_bud")) {
            switch (p(s, "facing")) {
                case "down": return new double[][]{b16(3, 9, 3, 13, 16, 13)};
                case "east": return new double[][]{b16(0, 3, 3, 7, 13, 13)};
                case "west": return new double[][]{b16(9, 3, 3, 16, 13, 13)};
                case "north": return new double[][]{b16(3, 3, 9, 13, 13, 16)};
                case "south": return new double[][]{b16(3, 3, 0, 13, 13, 7)};
                default: return new double[][]{b16(3, 0, 3, 13, 7, 13)};
            }
        }
        if (id.equals("pointed_dripstone")) return new double[][]{b16(5, 0, 5, 11, 16, 11)};
        if (id.equals("big_dripleaf") || id.equals("small_dripleaf") || id.equals("big_dripleaf_stem")) return NONE;
        return FULL;
    }

    /** Height of the top of the shape (0 when it has none). */
    static double top(String s) {
        double t = 0;
        for (double[] b : shape(s)) t = Math.max(t, b[4]);
        return t;
    }

    static boolean climbable(String s) {
        if (s == null) return false;
        String id = Bs.id(s);
        return id.equals("ladder") || id.equals("vine") || id.equals("scaffolding") || id.startsWith("weeping_vines")
                || id.startsWith("twisting_vines") || id.startsWith("cave_vines");
    }

    static double friction(String s) {
        if (s == null) return 0.6f;
        String id = Bs.id(s);
        if (id.equals("slime_block")) return 0.8f;
        if (id.equals("ice") || id.equals("packed_ice") || id.equals("frosted_ice")) return 0.98f;
        if (id.equals("blue_ice")) return 0.989f;
        return 0.6f;
    }

    static double speedFactor(String s) {
        if (s == null) return 1;
        String id = Bs.id(s);
        return id.equals("honey_block") || id.equals("soul_sand") ? 0.4f : 1;
    }

    static double jumpFactor(String s) {
        if (s == null) return 1;
        return Bs.id(s).equals("honey_block") ? 0.5f : 1;
    }

    static boolean tall(String s) {
        int k = Bs.kind(Bs.id(s));
        return k == 1 || k == 3 || k == 4;
    }

    // ------------------------------------------------------------------ world

    /** Collision world over the generator's block store (+ timed blocks treated as present). */
    static final class World {
        final W w;
        final Map<Long, String> timed;
        private final HashMap<Long, double[][]> cache = new HashMap<>();
        /** Cells forced empty (e.g. for "what if" queries). */
        final java.util.Set<Long> removed = new java.util.HashSet<>();

        World(W w, Map<Long, String> timed) { this.w = w; this.timed = timed; }

        String state(int x, int y, int z) {
            long k = W.key(x, y, z);
            if (removed.contains(k)) return null;
            String t = timed.get(k);
            return t != null ? t : w.get(x, y, z);
        }

        /** World-space boxes of a cell (cached). */
        double[][] boxes(int x, int y, int z) {
            long k = W.key(x, y, z);
            double[][] r = cache.get(k);
            if (r != null) return r;
            double[][] sh = shape(state(x, y, z));
            r = new double[sh.length][];
            for (int i = 0; i < sh.length; i++) {
                double[] b = sh[i];
                r[i] = new double[]{x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]};
            }
            cache.put(k, r);
            return r;
        }

        void invalidate() { cache.clear(); }

        void collect(double x0, double y0, double z0, double x1, double y1, double z1, List<double[]> out) {
            out.clear();
            int ax = (int) Math.floor(x0), bx = (int) Math.floor(x1), ay = (int) Math.floor(y0) - 1, by = (int) Math.floor(y1),
                    az = (int) Math.floor(z0), bz = (int) Math.floor(z1);
            for (int x = ax; x <= bx; x++) for (int y = ay; y <= by; y++) for (int z = az; z <= bz; z++)
                for (double[] b : boxes(x, y, z))
                    if (b[3] > x0 && b[0] < x1 && b[4] > y0 && b[1] < y1 && b[5] > z0 && b[2] < z1) out.add(b);
        }
    }

    // ------------------------------------------------------------------ player

    static final class P {
        double x, y, z, vx, vy, vz;
        boolean onGround, sprint = true, hColl, minor, jumped;
        int jumpDelay;
        /** Main supporting block cell (key) when on the ground, else Long.MIN_VALUE. */
        long sup = Long.MIN_VALUE;
        P copy() {
            P q = new P();
            q.x = x; q.y = y; q.z = z; q.vx = vx; q.vy = vy; q.vz = vz; q.onGround = onGround; q.sprint = sprint;
            q.hColl = hColl; q.minor = minor; q.jumpDelay = jumpDelay; q.sup = sup; q.jumped = jumped;
            return q;
        }
    }

    private static final ThreadLocal<List<double[]>> TMP = ThreadLocal.withInitial(ArrayList::new);

    /** Feet block of the player. */
    static String feetState(World wd, P p) {
        return wd.state((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
    }

    static boolean onClimbable(World wd, P p) { return climbable(feetState(wd, p)); }

    /** Block whose friction applies (getBlockPosBelowThatAffectsMyMovement, offset 0.5). */
    private static String belowState(World wd, P p, double off) {
        if (p.sup != Long.MIN_VALUE) {
            int sx = W.kx(p.sup), sy = W.ky(p.sup), sz = W.kz(p.sup);
            String s = wd.state(sx, sy, sz);
            if (off <= 0.5 && s != null && tall(s)) return s;
            return wd.state(sx, (int) Math.floor(p.y - off), sz);
        }
        return wd.state((int) Math.floor(p.x), (int) Math.floor(p.y - off), (int) Math.floor(p.z));
    }

    /**
     * One game tick. {@code yawDeg} = facing (Minecraft yaw: 0 = +z, 90 = -x), the W key is held with input
     * magnitude {@code mag} (0.98 W only, 0 = no key), {@code jump} = jump key held this tick.
     */
    static void tick(World wd, P p, double yawDeg, double mag, boolean jump) {
        // LocalPlayer: sprint key held -> sprinting unless the last move hit a wall head-on
        p.sprint = mag > 0.8 && !(p.hColl && !p.minor);
        if (Math.abs(p.vx) < 0.003) p.vx = 0;
        if (Math.abs(p.vy) < 0.003) p.vy = 0;
        if (Math.abs(p.vz) < 0.003) p.vz = 0;
        if (p.jumpDelay > 0) p.jumpDelay--;
        double r = Math.toRadians(yawDeg), sin = Math.sin(r), cos = Math.cos(r);
        p.jumped = false;
        if (jump) {
            if (p.onGround && p.jumpDelay == 0) {
                double jf = jumpFactor(feetState(wd, p));
                if (jf == 1) jf = jumpFactor(belowState(wd, p, 0.5000001));
                p.vy = 0.42f * jf;
                if (p.sprint) { p.vx += -sin * 0.2f; p.vz += cos * 0.2f; }
                p.jumpDelay = 10;
                p.jumped = true;
            }
        } else p.jumpDelay = 0;
        boolean og0 = p.onGround;
        double f2 = friction(belowState(wd, p, 0.5000001));
        double speed = og0 ? (p.sprint ? 0.13f : 0.1f) * (0.21600002f / (f2 * f2 * f2)) : (p.sprint ? 0.025999999f : 0.02f);
        double m = Math.min(mag, 1.0);
        p.vx += (-sin * m) * speed;
        p.vz += (cos * m) * speed;
        if (onClimbable(wd, p)) {
            p.vx = Math.max(-0.15f, Math.min(0.15f, p.vx));
            p.vz = Math.max(-0.15f, Math.min(0.15f, p.vz));
            p.vy = Math.max(p.vy, -0.15f);
        }
        move(wd, p, og0, sin, cos, mag);
        if ((p.hColl || jump) && onClimbable(wd, p)) p.vy = 0.2;
        double f3 = og0 ? f2 * 0.91f : 0.91f;
        p.vy = (p.vy - 0.08) * 0.98f;
        p.vx *= f3;
        p.vz *= f3;
    }

    private static void move(World wd, P p, boolean og0, double sin, double cos, double mag) {
        double mx = p.vx, my = p.vy, mz = p.vz;
        List<double[]> bx = TMP.get();
        double ex = Math.abs(mx) + 1, ey = Math.abs(my) + 1, ez = Math.abs(mz) + 1;
        wd.collect(p.x - HW - ex, p.y - ey, p.z - HW - ez, p.x + HW + ex, p.y + PH + ey + 0.6, p.z + HW + ez, bx);
        double[] box = {p.x - HW, p.y, p.z - HW, p.x + HW, p.y + PH, p.z + HW};
        double[] v = collideAll(box, mx, my, mz, bx);
        boolean cx = !eq(mx, v[0]), cy = my != v[1], cz = !eq(mz, v[2]);
        if ((og0 || (cy && my < 0)) && (cx || cz)) {
            double[] v1 = collideAll(box, mx, 0.6, mz, bx);
            double[] grown = {Math.min(box[0], box[0] + mx), box[1], Math.min(box[2], box[2] + mz), Math.max(box[3], box[3] + mx), box[4], Math.max(box[5], box[5] + mz)};
            double[] v2 = collideAll(grown, 0, 0.6, 0, bx);
            if (v2[1] < 0.6) {
                double[] b2 = off(box, 0, v2[1], 0);
                double[] v3 = collideAll(b2, mx, 0, mz, bx);
                v3[1] = v2[1];
                if (hd(v3) > hd(v1)) v1 = v3;
            }
            if (hd(v1) > hd(v)) {
                double[] b4 = off(box, v1[0], v1[1], v1[2]);
                double[] v4 = collideAll(b4, 0, -v1[1] + my, 0, bx);
                v = new double[]{v1[0], v1[1] + v4[1], v1[2]};
                cx = !eq(mx, v[0]); cy = my != v[1]; cz = !eq(mz, v[2]);
            }
        }
        p.x += v[0]; p.y += v[1]; p.z += v[2];
        p.hColl = cx || cz;
        p.minor = false;
        if (p.hColl && mag > 0) {
            double d4 = 1, d5 = v[0] * v[0] + v[2] * v[2];
            if (d5 >= 1.0E-5f) {
                double d6 = -sin * v[0] + cos * v[2];
                p.minor = Math.acos(Math.max(-1, Math.min(1, d6 / Math.sqrt(d4 * d5)))) < 0.13962634f;
            }
        }
        boolean below = cy && my < 0;
        p.onGround = below;
        if (cx) p.vx = 0;
        if (cz) p.vz = 0;
        // main supporting block
        p.sup = Long.MIN_VALUE;
        if (p.onGround) p.sup = support(wd, p);
        if (cy) {
            String sup = p.sup != Long.MIN_VALUE ? wd.state(W.kx(p.sup), W.ky(p.sup), W.kz(p.sup))
                    : wd.state((int) Math.floor(p.x), (int) Math.floor(p.y - 0.2), (int) Math.floor(p.z));
            if (below && sup != null && Bs.id(sup).equals("slime_block") && p.vy < 0) p.vy = -p.vy;
            else if (below && sup != null && Bs.id(sup).endsWith("_bed") && p.vy < 0) p.vy = -p.vy * 0.66f;
            else p.vy = 0;
        }
        if (p.onGround && p.sup != Long.MIN_VALUE) {
            String sup = wd.state(W.kx(p.sup), W.ky(p.sup), W.kz(p.sup));
            if (sup != null && Bs.id(sup).equals("slime_block") && Math.abs(p.vy) < 0.1) {
                double d1 = 0.4 + Math.abs(p.vy) * 0.2;
                p.vx *= d1; p.vz *= d1;
            }
        }
        double f = speedFactor(feetState(wd, p));
        if (f == 1) f = speedFactor(belowState(wd, p, 0.5000001));
        p.vx *= f; p.vz *= f;
    }

    private static boolean eq(double a, double b) { return Math.abs(b - a) < 1.0E-5f; }

    private static double hd(double[] v) { return v[0] * v[0] + v[2] * v[2]; }

    private static double[] off(double[] b, double dx, double dy, double dz) {
        return new double[]{b[0] + dx, b[1] + dy, b[2] + dz, b[3] + dx, b[4] + dy, b[5] + dz};
    }

    /** Vanilla collideWithShapes: Y first, then the larger horizontal axis first. */
    static double[] collideAll(double[] box, double mx, double my, double mz, List<double[]> bx) {
        double[] b = box.clone();
        if (my != 0) { my = axis(1, b, my, bx); if (my != 0) { b[1] += my; b[4] += my; } }
        boolean zFirst = Math.abs(mx) < Math.abs(mz);
        if (zFirst && mz != 0) { mz = axis(2, b, mz, bx); if (mz != 0) { b[2] += mz; b[5] += mz; } }
        if (mx != 0) { mx = axis(0, b, mx, bx); if (!zFirst && mx != 0) { b[0] += mx; b[3] += mx; } }
        if (!zFirst && mz != 0) mz = axis(2, b, mz, bx);
        return new double[]{mx, my, mz};
    }

    private static double axis(int a, double[] b, double d, List<double[]> bx) {
        int o1 = (a + 1) % 3, o2 = (a + 2) % 3;
        for (double[] c : bx) {
            if (c[o1 + 3] <= b[o1] + EPS || c[o1] >= b[o1 + 3] - EPS) continue;
            if (c[o2 + 3] <= b[o2] + EPS || c[o2] >= b[o2 + 3] - EPS) continue;
            if (d > 0) { if (c[a] >= b[a + 3] - EPS) { double m = c[a] - b[a + 3]; if (m < d) d = m; } }
            else { if (c[a + 3] <= b[a] + EPS) { double m = c[a + 3] - b[a]; if (m > d) d = m; } }
        }
        return Math.abs(d) < EPS ? 0 : d;
    }

    /** Cell (key) of the supporting block nearest to the player centre, or Long.MIN_VALUE. */
    static long support(World wd, P p) {
        List<double[]> bx = new ArrayList<>();
        wd.collect(p.x - HW, p.y - 1.0E-3, p.z - HW, p.x + HW, p.y + 1.0E-3, p.z + HW, bx);
        long best = Long.MIN_VALUE;
        double bd = 1e9;
        for (double[] c : bx) {
            if (Math.abs(c[4] - p.y) > 1.0E-4) continue;
            if (c[3] <= p.x - HW + EPS || c[0] >= p.x + HW - EPS || c[5] <= p.z - HW + EPS || c[2] >= p.z + HW - EPS) continue;
            int cx = (int) Math.floor((c[0] + c[3]) / 2), cy = (int) Math.floor(c[1] + 1e-6), cz = (int) Math.floor((c[2] + c[5]) / 2);
            double d = Math.pow(cx + 0.5 - p.x, 2) + Math.pow(cz + 0.5 - p.z, 2);
            if (d < bd) { bd = d; best = W.key(cx, cy, cz); }
        }
        return best;
    }

    /** All supporting cells under the player box (keys). */
    static List<Long> supports(World wd, P p) {
        List<double[]> bx = new ArrayList<>();
        wd.collect(p.x - HW, p.y - 1.0E-3, p.z - HW, p.x + HW, p.y + 1.0E-3, p.z + HW, bx);
        List<Long> out = new ArrayList<>();
        for (double[] c : bx) {
            if (Math.abs(c[4] - p.y) > 1.0E-4) continue;
            if (c[3] <= p.x - HW + EPS || c[0] >= p.x + HW - EPS || c[5] <= p.z - HW + EPS || c[2] >= p.z + HW - EPS) continue;
            int cx = (int) Math.floor((c[0] + c[3]) / 2), cy = (int) Math.floor(c[1] + 1e-6), cz = (int) Math.floor((c[2] + c[5]) / 2);
            long k = W.key(cx, cy, cz);
            if (!out.contains(k)) out.add(k);
        }
        return out;
    }

    /** True if the player box at (x, y, z) intersects any collision box. */
    static boolean blocked(World wd, double x, double y, double z) {
        List<double[]> bx = new ArrayList<>();
        wd.collect(x - HW + 1e-4, y + 1e-4, z - HW + 1e-4, x + HW - 1e-4, y + PH - 1e-4, z + HW - 1e-4, bx);
        return !bx.isEmpty();
    }
}
