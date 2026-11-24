package com.mcm.games.tgttos.model;

import com.mcm.games.tgttos.model.TgMapData.Placement;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive parts of a map (pure Java, local coordinates like {@link TgMapData}), returned by
 * {@link TgMap#features()}. Everything is optional; {@link #NONE} is the default for maps without features.
 * The pose / cell maths live here so the runtime and the offline validators use exactly the same model.
 *
 * <ul>
 *   <li>{@link Mover}: a moving hazard shown with block display entities (pendulum, rotor, slider). The runtime
 *       animates it from GO (frozen at its rest pose before), tests every racer against its hazard boxes each tick
 *       (a hit = the same as touching a hazard block: back to the last checkpoint), and optionally ({@link Mover#solid})
 *       keeps invisible barrier blocks under its current cells so boats collide with it for real.</li>
 *   <li>{@link Toggle}: timed real-block changes (gates, sweeping arms, sliding barges, ice turning to slush). Frame 0
 *       is the state the generator placed in {@link TgMapData#blocks}. Placed without physics; a solid block is never
 *       set into a cell a boat or player occupies (it waits until the cell is free). Indicator blocks switch to
 *       {@link Toggle#warnBlock} during the last {@link Toggle#warnTicks} before every frame marked {@code warn}.</li>
 *   <li>{@link BonusGate}: line bonus. Passing through the box pays the gate's tier once per racer per
 *       {@link BonusGate#group} (the best tier flown in that group counts), paid only on finishing, capped per round.</li>
 *   <li>{@link Checkpoint}: crossing the trigger box raises the racer's checkpoint (highest {@code order} counts).
 *       Void / hazard / mover / lethal-fall respawns go to a spawn of the highest checkpoint with a fresh kit.</li>
 *   <li>{@link Refill}: elytra rocket refill: passing through the box gives {@code rockets} fireworks once per attempt
 *       (an attempt ends at every respawn).</li>
 *   <li>{@link Depot}: boat depot: a racer on foot (not in a vehicle) inside the area gets a fresh boat at
 *       {@code boat} and is seated in it; their old boat is removed.</li>
 * </ul>
 */
public final class MapFeatures {
    public static final MapFeatures NONE = new MapFeatures();

    public final List<Mover> movers = new ArrayList<>();
    public final List<Toggle> toggles = new ArrayList<>();
    public final List<BonusGate> gates = new ArrayList<>();
    public final List<Checkpoint> checkpoints = new ArrayList<>();
    public final List<Refill> refills = new ArrayList<>();
    public final List<Depot> depots = new ArrayList<>();
    public final List<Trap> traps = new ArrayList<>();

    public boolean isEmpty() {
        return movers.isEmpty() && toggles.isEmpty() && gates.isEmpty() && checkpoints.isEmpty() && refills.isEmpty() && depots.isEmpty() && traps.isEmpty();
    }

    // ================================================================== bonus, checkpoints, refills, depots

    /** Tier: 1 = orange (fast), 2 = red (hard), 3 = brutal (1x1 pinholes, 1-wide slits, 1x1 fire rings). */
    public record BonusGate(String group, String name, int tier, Region box) {}

    public record Checkpoint(int order, String name, Region trigger, List<Spawn> spawns, List<Spawn> boats) {}

    public record Refill(String name, Region box, int rockets) {}

    public record Depot(String name, Region area, Spawn boat) {}

    /**
     * A block that gives way under a racer: when a racer's feet enter {@code trigger}, it creaks (telegraph sound),
     * {@code delayTicks} later the {@code gone} cells turn to air and {@code falling} drops as a falling block (no
     * damage, never lands); {@code restoreTicks} after that the cells get their generator blocks back (only once
     * nobody stands in them).
     */
    public record Trap(String name, Region trigger, List<Pos> gone, String falling, int delayTicks, int restoreTicks, String warnSound, String dropSound) {}

    // ================================================================== movers

    public enum Motion { PENDULUM, ROTOR, SLIDER }

    /** Axis-aligned box of a mover in its own frame (pivot = origin); {@code hazard} = part of the hit test. */
    public record Box(double x0, double y0, double z0, double x1, double y1, double z1, String block, boolean hazard) {}

    /**
     * One moving hazard. Pivot (px, py, pz) in local map coordinates. PENDULUM: swings {@code amplitude} degrees either
     * side about {@code axis} ('x' or 'z'), angle = amplitude * sin(2 pi u). ROTOR: turns 360 degrees per period about
     * {@code axis}, starting at {@code amplitude} degrees. SLIDER: moves by (dx, dy, dz) and back: rest (open)
     * {@code openTicks}, move {@code moveTicks}, extended (closed) {@code closedTicks}, move back {@code moveTicks}.
     * u = ((t + phaseTicks) mod period) / period, t = ticks since GO.
     */
    public record Mover(String name, double px, double py, double pz, Motion motion, char axis, double amplitude,
                        double dx, double dy, double dz, int periodTicks, int openTicks, int moveTicks, int closedTicks,
                        int phaseTicks, List<Box> boxes, boolean solid, String sound, int tier) {

        public static Mover pendulum(String name, double px, double py, double pz, char axis, double ampDeg, int period, int phase, List<Box> boxes) {
            return new Mover(name, px, py, pz, Motion.PENDULUM, axis, ampDeg, 0, 0, 0, period, 0, 0, 0, phase, boxes, false, "entity.player.attack.sweep", 0);
        }

        public static Mover rotor(String name, double px, double py, double pz, char axis, double startDeg, int period, int phase, List<Box> boxes) {
            return new Mover(name, px, py, pz, Motion.ROTOR, axis, startDeg, 0, 0, 0, period, 0, 0, 0, phase, boxes, false, "block.grindstone.use", 0);
        }

        public static Mover slider(String name, double px, double py, double pz, double dx, double dy, double dz,
                                   int open, int move, int closed, int phase, List<Box> boxes) {
            return new Mover(name, px, py, pz, Motion.SLIDER, 'y', 0, dx, dy, dz, open + 2 * move + closed, open, move, closed, phase, boxes, false, "block.piston.extend", 0);
        }

        public Mover withSolid() {
            return new Mover(name, px, py, pz, motion, axis, amplitude, dx, dy, dz, periodTicks, openTicks, moveTicks, closedTicks, phaseTicks, boxes, true, sound, tier);
        }

        public Mover withSound(String s) {
            return new Mover(name, px, py, pz, motion, axis, amplitude, dx, dy, dz, periodTicks, openTicks, moveTicks, closedTicks, phaseTicks, boxes, solid, s, tier);
        }

        /** Rotation angle (degrees) at tick t. */
        public double angle(double t) {
            double u = frac(t);
            return switch (motion) {
                case PENDULUM -> amplitude * Math.sin(2 * Math.PI * u);
                case ROTOR -> amplitude + 360 * u;
                case SLIDER -> 0;
            };
        }

        /** Slider extension 0 (rest) .. 1 (extended) at tick t. */
        public double extension(double t) {
            if (motion != Motion.SLIDER) return 0;
            double k = frac(t) * periodTicks;
            if (k < openTicks) return 0;
            k -= openTicks;
            if (k < moveTicks) return smooth(k / moveTicks);
            k -= moveTicks;
            if (k < closedTicks) return 1;
            k -= closedTicks;
            return 1 - smooth(Math.min(1, k / moveTicks));
        }

        /** Ticks until the slider next starts to extend (0 while moving / extended). */
        public double ticksToClose(double t) {
            double k = frac(t) * periodTicks;
            return k < openTicks ? openTicks - k : 0;
        }

        private double frac(double t) {
            double p = Math.max(1, periodTicks);
            double u = ((t + phaseTicks) % p + p) % p;
            return u / p;
        }

        private static double smooth(double x) { return x * x * (3 - 2 * x); }

        /** Local (mover frame) -> map: rotation (about axis) then slide. Returns {x, y, z}. */
        public double[] toMap(double t, double lx, double ly, double lz) {
            double[] r = rot(angle(t), lx, ly, lz, false);
            double e = extension(t);
            return new double[]{px + r[0] + dx * e, py + r[1] + dy * e, pz + r[2] + dz * e};
        }

        /** Map -> local (mover frame). */
        public double[] toLocal(double t, double x, double y, double z) {
            double e = extension(t);
            return rot(angle(t), x - px - dx * e, y - py - dy * e, z - pz - dz * e, true);
        }

        private double[] rot(double deg, double x, double y, double z, boolean inverse) {
            if (deg == 0) return new double[]{x, y, z};
            double a = Math.toRadians(inverse ? -deg : deg), c = Math.cos(a), s = Math.sin(a);
            return switch (axis) {
                case 'x' -> new double[]{x, y * c - z * s, y * s + z * c};
                case 'y' -> new double[]{x * c + z * s, y, -x * s + z * c};
                default -> new double[]{x * c - y * s, x * s + y * c, z};
            };
        }

        /** Unit quaternion {x, y, z, w} of the rotation at tick t (for display transformations). */
        public float[] quaternion(double t) {
            double a = Math.toRadians(angle(t)) / 2, s = Math.sin(a);
            return switch (axis) {
                case 'x' -> new float[]{(float) s, 0, 0, (float) Math.cos(a)};
                case 'y' -> new float[]{0, (float) s, 0, (float) Math.cos(a)};
                default -> new float[]{0, 0, (float) s, (float) Math.cos(a)};
            };
        }

        /** Largest distance from the pivot any box corner reaches (plus the slide), for a cheap distance pre-check. */
        public double reach() {
            double m = 0;
            for (Box b : boxes)
                for (double x : new double[]{b.x0(), b.x1()}) for (double y : new double[]{b.y0(), b.y1()}) for (double z : new double[]{b.z0(), b.z1()})
                    m = Math.max(m, Math.sqrt(x * x + y * y + z * z));
            return m + Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Does a sphere (centre x,y,z in map coordinates, radius r) touch a hazard box at tick t? */
        public boolean hits(double t, double x, double y, double z, double r) {
            double[] l = toLocal(t, x, y, z);
            for (Box b : boxes) {
                if (!b.hazard()) continue;
                double cx = Math.max(b.x0(), Math.min(b.x1(), l[0])), cy = Math.max(b.y0(), Math.min(b.y1(), l[1])), cz = Math.max(b.z0(), Math.min(b.z1(), l[2]));
                double ddx = l[0] - cx, ddy = l[1] - cy, ddz = l[2] - cz;
                if (ddx * ddx + ddy * ddy + ddz * ddz <= r * r) return true;
            }
            return false;
        }

        /** Map block cells whose centre lies inside a hazard box at tick t (solid barrier cells, previews). */
        public List<Pos> cells(double t) {
            List<Pos> out = new ArrayList<>();
            double rr = reach() + 1;
            java.util.Set<Long> seen = new java.util.HashSet<>();
            double[] c = toMap(t, 0, 0, 0);
            for (Box b : boxes) {
                if (!b.hazard()) continue;
                // sample the box in its own frame at 0.5 steps and collect the cells (robust under rotation)
                for (double x = b.x0() + 0.25; x < b.x1(); x += 0.5)
                    for (double y = b.y0() + 0.25; y < b.y1(); y += 0.5)
                        for (double z = b.z0() + 0.25; z < b.z1(); z += 0.5) {
                            double[] m = toMap(t, x, y, z);
                            int bx = (int) Math.floor(m[0]), by = (int) Math.floor(m[1]), bz = (int) Math.floor(m[2]);
                            if (Math.abs(bx - c[0]) > rr + 2) continue;
                            if (seen.add(TgMapData.SparseBlocks.key(bx, by, bz))) out.add(new Pos(bx, by, bz));
                        }
            }
            return out;
        }

        /** Every box drawn as blocks at tick t (preview only; rotated boxes are approximated by their sampled cells). */
        public Map<Pos, String> render(double t) {
            Map<Pos, String> out = new LinkedHashMap<>();
            for (Box b : boxes)
                for (double x = b.x0() + 0.25; x < b.x1(); x += 0.5)
                    for (double y = b.y0() + 0.25; y < b.y1(); y += 0.5)
                        for (double z = b.z0() + 0.25; z < b.z1(); z += 0.5) {
                            double[] m = toMap(t, x, y, z);
                            out.put(new Pos((int) Math.floor(m[0]), (int) Math.floor(m[1]), (int) Math.floor(m[2])), b.block());
                        }
            return out;
        }
    }

    // ================================================================== toggles

    /** One state of a toggle: lasts {@code ticks}; {@code warn} = its start is announced by the indicators. */
    public record Frame(int ticks, List<Placement> blocks, boolean warn) {}

    public record Toggle(String name, int phaseTicks, List<Frame> frames, List<Pos> indicators, String warnBlock, int warnTicks,
                         String sound, int tier) {
        public int period() { int p = 0; for (Frame f : frames) p += f.ticks(); return Math.max(1, p); }

        /** Frame index at tick t (t = ticks since GO). */
        public int frameAt(long t) {
            long k = Math.floorMod(t + phaseTicks, (long) period());
            for (int i = 0; i < frames.size(); i++) { if (k < frames.get(i).ticks()) return i; k -= frames.get(i).ticks(); }
            return frames.size() - 1;
        }

        /** True while the indicators should show the warning (the last warnTicks before a frame with warn). */
        public boolean warning(long t) {
            int i = frameAt(t);
            int next = (i + 1) % frames.size();
            if (!frames.get(next).warn()) return false;
            long k = Math.floorMod(t + phaseTicks, (long) period());
            long end = 0;
            for (int j = 0; j <= i; j++) end += frames.get(j).ticks();
            return end - k <= warnTicks;
        }

        /**
         * Build a toggle from complete states: each state maps cells to blocks (null = air); cells missing from a state
         * get {@code base} (what the map has there in frame 0 / the generator's block, or air). Every frame then sets
         * the whole union, so applying any frame alone gives a consistent picture.
         */
        public static Toggle ofStates(String name, int phase, List<Map<Pos, String>> states, int[] ticks, boolean[] warn,
                                      Map<Pos, String> base, List<Pos> indicators, String warnBlock, int warnTicks, String sound, int tier) {
            java.util.LinkedHashSet<Pos> union = new java.util.LinkedHashSet<>();
            for (Map<Pos, String> s : states) union.addAll(s.keySet());
            List<Frame> frames = new ArrayList<>();
            for (int i = 0; i < states.size(); i++) {
                List<Placement> pl = new ArrayList<>();
                for (Pos p : union) {
                    String b = states.get(i).containsKey(p) ? states.get(i).get(p) : base.get(p);
                    pl.add(new Placement(p.x(), p.y(), p.z(), b));
                }
                frames.add(new Frame(ticks[i], pl, warn[i]));
            }
            return new Toggle(name, phase, frames, indicators, warnBlock, warnTicks, sound, tier);
        }
    }
}
