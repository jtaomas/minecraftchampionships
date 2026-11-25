package com.mcm.games.parkourwarrior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java result of the Parkour Warrior course generator (no Bukkit types). Contract between the game
 * mechanics (package {@code com.mcm.games.parkourwarrior}) and the map generator
 * ({@code com.mcm.games.parkourwarrior.map.CourseMap}).
 *
 * <p><b>Generator entry point:</b> {@code public static CourseModel com.mcm.games.parkourwarrior.map.CourseMap.generate(long seed)}.
 * The runtime finds it by reflection; when absent it uses {@code stub.StubCourse}.
 *
 * <p><b>Coordinates</b> are integer block coordinates (doubles for player positions). The runtime places
 * local (x,y,z) at world (x + offsetX, y + offsetY, z + offsetZ) with offsets from config ({@code map.offset-*},
 * default 0/0/0), so in practice local == world. The world is a void world (min y -64, max y 319); keep the
 * course within y 40 .. 250. Place the start island near x = 0, z = 0.
 *
 * <p><b>Blocks</b> are vanilla block-state strings like {@code minecraft:oak_stairs[facing=north,half=top]}
 * ("minecraft:" optional). null / "air" clears. Do NOT put timed blocks ({@link #timed}) in {@link #blocks}
 * (the runtime toggles them; their positions must be air in the static store).
 *
 * <p><b>Positions ({@link Pos})</b> are player feet positions: use block centre (x + 0.5, y, z + 0.5) standing
 * on top of the block at y - 1. Yaw is Minecraft yaw (0 = +Z south, 90 = -X west, 180 = -Z north, -90/270 = +X east).
 *
 * <p><b>Boxes ({@link Box})</b> are inclusive integer block boxes. A player is "in" a box when the block
 * containing their feet ({@code floor(x), floor(y)} or {@code floor(y - 0.01)}, {@code floor(z)}) is inside it.
 * For a pad region use the pad block's x/z extent and minY = pad block y, maxY = pad block y + 2: a player
 * standing on the pad (feet at pad y + 1.0) is then inside.
 *
 * <p><b>Flow the runtime assumes:</b>
 * <ul>
 *   <li>Players spawn at {@link #spawns} on the start island. Stage s hub ({@link Hub}, s = 1..5) comes right
 *   before main obstacle Ms-1 (hub 1 may be the start island itself). Each hub has a bonus path entry.</li>
 *   <li>Obstacles: main {@code M<s>-<i>} (s 1..5, i 1..3), bonus {@code B<s>-<i>} (bonus path of hub s), endings
 *   {@code E-EASY-1, E-EASY-2, E-MEDIUM-1, ...}. {@link Obstacle#entry} = respawn point while attempting it
 *   (normally the previous obstacle's pad / the hub / the fork gate). Stepping into {@link Obstacle#pad}
 *   completes it; the runtime then uses the NEXT obstacle's entry on that line as the new checkpoint
 *   (for Ms-3: hub s+1 respawn, or the fork for M5-3; for Bs-3: hub s respawn by auto-return;
 *   for E-x-1: E-x-2's entry). So Ms-3's pad normally IS hub s+1.</li>
 *   <li>Falls: a player is "fallen" when below the kill-Y of the obstacle they are attempting, or inside an
 *   obstacle's optional {@link Obstacle#area} box column (x/z) below its kill-Y, or below {@code minY - 8}.</li>
 *   <li>After M5-3 the player reaches the fork pavilion ({@link #fork}); three gates lead to E-EASY-1 /
 *   E-MEDIUM-1 / E-HARD-1. Entering {@link #finishes} of a difficulty finishes that ending
 *   (the E-x-2 pad may be the same box as the finish).</li>
 * </ul>
 */
public final class CourseModel {

    public enum Kind { MAIN, BONUS, ENDING }

    public enum Difficulty { EASY, MEDIUM, HARD }

    /** Feet position + facing. */
    public record Pos(double x, double y, double z, float yaw) {
        public static Pos block(int bx, int by, int bz, float yaw) { return new Pos(bx + 0.5, by, bz + 0.5, yaw); }
    }

    public record BlockPos(int x, int y, int z) {}

    /** Inclusive integer block box. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Box {
            if (minX > maxX) { int t = minX; minX = maxX; maxX = t; }
            if (minY > maxY) { int t = minY; minY = maxY; maxY = t; }
            if (minZ > maxZ) { int t = minZ; minZ = maxZ; maxZ = t; }
        }
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
        /** True if the feet position is inside (tests floor(y) and floor(y - 0.01) so standing on a top face counts). */
        public boolean containsFeet(double x, double y, double z) {
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            return contains(bx, (int) Math.floor(y), bz) || contains(bx, (int) Math.floor(y - 0.01), bz);
        }
        public boolean containsColumn(double x, double z) {
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            return bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ;
        }
        public double centerX() { return (minX + maxX + 1) / 2.0; }
        public double centerZ() { return (minZ + maxZ + 1) / 2.0; }
    }

    /**
     * One named obstacle.
     * @param id          "M3-2", "B3-2", "E-HARD-1" ...
     * @param name        display name, e.g. "Domino Run"
     * @param kind        MAIN / BONUS / ENDING
     * @param stage       1..5 (0 for endings)
     * @param index       1..3 within its stage/bonus path (1..2 for endings)
     * @param ending      ending difficulty, null unless kind == ENDING
     * @param entry       respawn point while attempting this obstacle
     * @param pad         completion region (stepping in completes it)
     * @param killY       fall height: feet y below this = fall (lowest foothold - 6 by default)
     * @param label       where the floating name label goes (feet-style position; text display is spawned there)
     * @param area        x/z extent of the obstacle (y ignored); may be null but strongly recommended: without it a fall is
     *                    only detected below the LOWEST kill-Y of all obstacles attempted from the same checkpoint
     *                    (e.g. main + bonus at a hub), and the respawn uses the hub respawn instead of this entry
     */
    public record Obstacle(String id, String name, Kind kind, int stage, int index, Difficulty ending,
                           Pos entry, Box pad, int killY, Pos label, Box area) {}

    /**
     * Hub at the start of stage {@code stage} (1..5).
     * @param respawn    where players stand / respawn at this hub (and the scroll destination)
     * @param bonusEntry start of this hub's bonus path (respawn while attempting Bs-1)
     * @param zoneName   e.g. "Toy Box" (used in "You completed the Toy Box zone!")
     */
    public record Hub(int stage, Pos respawn, Pos bonusEntry, String zoneName) {}

    /**
     * Blocks the runtime toggles on a cycle. Tick t (game ticks since the run started) is "on" when
     * {@code (t + phaseTicks) % periodTicks < onTicks}. The runtime shows an indicator during the
     * {@code warnTicks} before turning on: particles at each position and, if {@code warnState} is non-null,
     * that block state placed there (e.g. a glass/carpet ghost). When off, {@code offState} (null = air) is placed.
     * @param obstacleId obstacle this belongs to (informational)
     */
    public record Timed(String obstacleId, List<BlockPos> positions, String blockState, int periodTicks,
                        int onTicks, int phaseTicks, int warnTicks, String warnState, String offState) {
        public Timed {
            positions = List.copyOf(positions);
            if (periodTicks <= 0) periodTicks = 40;
            if (onTicks <= 0 || onTicks > periodTicks) onTicks = periodTicks / 2;
            if (warnTicks < 0) warnTicks = 0;
        }
        public Timed(String obstacleId, List<BlockPos> positions, String blockState, int periodTicks, int onTicks, int phaseTicks) {
            this(obstacleId, positions, blockState, periodTicks, onTicks, phaseTicks, 15, null, null);
        }
        public boolean onAt(long tick) { return Math.floorMod(tick + phaseTicks, periodTicks) < onTicks; }
        /** True during the warning window before switching on. */
        public boolean warnAt(long tick) {
            if (onAt(tick)) return false;
            long m = Math.floorMod(tick + phaseTicks, periodTicks);
            return m >= periodTicks - warnTicks;
        }
    }

    /** Extra floating text (hub signs, gate names, zone banners). Text may use legacy '&' colour codes. */
    public record Label(Pos pos, String text) {}

    /** Named overview teleport for spectators; pitch 90 = looking straight down. */
    public record Overview(String name, Pos pos, float pitch) {}

    /** Sparse block store; also the sink the generator writes into. */
    public interface Blocks {
        void set(int x, int y, int z, String blockData);
        String get(int x, int y, int z);
        void forEach(Visitor v);
        int size();
    }

    @FunctionalInterface
    public interface Visitor { void block(int x, int y, int z, String blockData); }

    public static final class SparseBlocks implements Blocks {
        private final Map<Long, String> map = new HashMap<>();
        private final Map<String, String> intern = new HashMap<>();

        static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }
        static int kx(long k) { return (int) (k >> 38) << 6 >> 6; }
        static int kz(long k) { return (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6; }
        static int ky(long k) { return (int) (k & 0xFFF) << 20 >> 20; }

        @Override public void set(int x, int y, int z, String b) {
            long k = key(x, y, z);
            if (b == null || b.isEmpty() || b.equals("air") || b.equals("minecraft:air")) { map.remove(k); return; }
            String f = b.indexOf(':') < 0 ? "minecraft:" + b : b;
            map.put(k, intern.computeIfAbsent(f, s -> s));
        }
        @Override public String get(int x, int y, int z) { return map.get(key(x, y, z)); }
        @Override public void forEach(Visitor v) {
            for (Map.Entry<Long, String> e : map.entrySet()) {
                long k = e.getKey();
                v.block(kx(k), ky(k), kz(k), e.getValue());
            }
        }
        @Override public int size() { return map.size(); }
    }

    public final long seed;
    public final Blocks blocks;
    /** Start spawns on the start island (players are spread over them round-robin). At least one. */
    public final List<Pos> spawns = new ArrayList<>();
    /** All obstacles in course order: M1-1..M1-3 interleaved with B1-1..B1-3 etc. is fine; ids are what matter. */
    public final List<Obstacle> obstacles = new ArrayList<>();
    /** Hubs 1..5 (index 0 = stage 1). */
    public final List<Hub> hubs = new ArrayList<>();
    /** Fork pavilion standing point (respawn after M5-3 and when switching endings). */
    public Pos fork;
    /** Finish region per ending difficulty. */
    public final Map<Difficulty, Box> finishes = new EnumMap<>(Difficulty.class);
    /** Where finished players are sent before spectating (optional; defaults to the first overview). */
    public final Map<Difficulty, Pos> finishStands = new EnumMap<>(Difficulty.class);
    public final List<Timed> timed = new ArrayList<>();
    public final List<Label> labels = new ArrayList<>();
    /** Moving hazards (block displays): a touch sends the runner back like a fall. See {@link Mover}. */
    public final List<Mover> movers = new ArrayList<>();
    /** Dropping ledges: standing on a group drops it after {@code delayTicks}; it comes back later. */
    public final List<Crumble> crumbles = new ArrayList<>();
    /** Conveyor belts: standing on a belt cell adds a fixed horizontal velocity every tick. */
    public final List<Belt> belts = new ArrayList<>();
    /** Course credit shown on the start island and in /pw info (imported maps), may be empty. */
    public String credit = "";
    /** Map id this course was built for (classic, mcc6, mcc10, ...). */
    public String mapId = "classic";

    // ================================================================== moving parts
    // Pose maths ported from TGTTOS (model/MapFeatures.Mover, read-only copy): the runtime and the offline
    // validator use exactly this code, so every moving trap is checked against the same model players see.

    public enum Motion { PENDULUM, ROTOR, SLIDER }

    /** Axis-aligned box of a mover in its own frame (pivot = origin); {@code hazard} = part of the hit test. */
    public record MBox(double x0, double y0, double z0, double x1, double y1, double z1, String block, boolean hazard) {}

    /**
     * One moving hazard. Pivot (px, py, pz) in course coordinates. PENDULUM: swings {@code amplitude} degrees either
     * side about {@code axis} ('x' or 'z'), angle = amplitude * sin(2 pi u). ROTOR: turns 360 degrees per period about
     * {@code axis}, starting at {@code amplitude} degrees. SLIDER: moves by (dx, dy, dz) and back: rest (open)
     * {@code openTicks}, move {@code moveTicks}, extended (closed) {@code closedTicks}, move back {@code moveTicks}.
     * u = ((t + phaseTicks) mod period) / period, t = game ticks (the same clock as {@link Timed}).
     */
    public record Mover(String obstacleId, String name, double px, double py, double pz, Motion motion, char axis, double amplitude,
                        double dx, double dy, double dz, int periodTicks, int openTicks, int moveTicks, int closedTicks,
                        int phaseTicks, List<MBox> boxes, String sound) {
        public Mover { boxes = List.copyOf(boxes); }

        public static Mover pendulum(String ob, String name, double px, double py, double pz, char axis, double ampDeg, int period, int phase, List<MBox> boxes) {
            return new Mover(ob, name, px, py, pz, Motion.PENDULUM, axis, ampDeg, 0, 0, 0, period, 0, 0, 0, phase, boxes, "entity.player.attack.sweep");
        }

        public static Mover rotor(String ob, String name, double px, double py, double pz, char axis, double startDeg, int period, int phase, List<MBox> boxes) {
            return new Mover(ob, name, px, py, pz, Motion.ROTOR, axis, startDeg, 0, 0, 0, period, 0, 0, 0, phase, boxes, "block.grindstone.use");
        }

        public static Mover slider(String ob, String name, double px, double py, double pz, double dx, double dy, double dz,
                                   int open, int move, int closed, int phase, List<MBox> boxes) {
            return new Mover(ob, name, px, py, pz, Motion.SLIDER, 'y', 0, dx, dy, dz, open + 2 * move + closed, open, move, closed, phase, boxes, "block.piston.extend");
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

        /** Mover frame -> course: rotation (about axis) then slide. Returns {x, y, z}. */
        public double[] toMap(double t, double lx, double ly, double lz) {
            double[] r = rot(angle(t), lx, ly, lz, false);
            double e = extension(t);
            return new double[]{px + r[0] + dx * e, py + r[1] + dy * e, pz + r[2] + dz * e};
        }

        /** Course -> mover frame. */
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

        /** Unit quaternion {x, y, z, w} of the rotation at tick t (display transformations). */
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
            for (MBox b : boxes)
                for (double x : new double[]{b.x0(), b.x1()}) for (double y : new double[]{b.y0(), b.y1()}) for (double z : new double[]{b.z0(), b.z1()})
                    m = Math.max(m, Math.sqrt(x * x + y * y + z * z));
            return m + Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Does a sphere (centre x,y,z in course coordinates, radius r) touch a hazard box at tick t? */
        public boolean hits(double t, double x, double y, double z, double r) {
            double[] l = toLocal(t, x, y, z);
            for (MBox b : boxes) {
                if (!b.hazard()) continue;
                double cx = Math.max(b.x0(), Math.min(b.x1(), l[0])), cy = Math.max(b.y0(), Math.min(b.y1(), l[1])), cz = Math.max(b.z0(), Math.min(b.z1(), l[2]));
                double ddx = l[0] - cx, ddy = l[1] - cy, ddz = l[2] - cz;
                if (ddx * ddx + ddy * ddy + ddz * ddz <= r * r) return true;
            }
            return false;
        }

        /** Does a standing/jumping player (feet x,y,z; three spheres up the body, radius 0.35) touch it at tick t? */
        public boolean hitsPlayer(double t, double x, double y, double z) {
            return hits(t, x, y + 0.35, z, 0.35) || hits(t, x, y + 0.9, z, 0.35) || hits(t, x, y + 1.45, z, 0.35);
        }

        /** Every box drawn as cells at tick t (preview renders only). */
        public Map<BlockPos, String> render(double t) {
            Map<BlockPos, String> out = new LinkedHashMap<>();
            for (MBox b : boxes)
                for (double x = b.x0() + 0.25; x < b.x1(); x += 0.5)
                    for (double y = b.y0() + 0.25; y < b.y1(); y += 0.5)
                        for (double z = b.z0() + 0.25; z < b.z1(); z += 0.5) {
                            double[] m = toMap(t, x, y, z);
                            out.put(new BlockPos((int) Math.floor(m[0]), (int) Math.floor(m[1]), (int) Math.floor(m[2])), b.block());
                        }
            return out;
        }
    }

    /**
     * Dropping ledge group: when a runner stands on any of {@code cells} it creaks (particles + sound) and
     * {@code delayTicks} later every cell turns to air; {@code restoreTicks} after dropping the blocks come back once
     * nobody stands in them. {@code state} = the block placed (the static store has air there).
     */
    public record Crumble(String obstacleId, List<BlockPos> cells, String state, int delayTicks, int restoreTicks) {
        public Crumble { cells = List.copyOf(cells); }
    }

    /** Conveyor belt: feet supported by one of {@code cells} = (vx, vz) blocks/tick added every tick. */
    public record Belt(String obstacleId, List<BlockPos> cells, double vx, double vz) {
        public Belt { cells = List.copyOf(cells); }
    }
    /** Spectator overview points, e.g. "Spine (top-down)", "Fork". At least one. */
    public final List<Overview> overviews = new ArrayList<>();
    /** Bounds of every block (inclusive). Call {@link #recomputeBounds()} after generating. */
    public int minX, minY, minZ, maxX, maxY, maxZ;
    /** Free-form generator notes / validator results. Informational. */
    public final Map<String, String> notes = new LinkedHashMap<>();

    public CourseModel(long seed, Blocks blocks) { this.seed = seed; this.blocks = blocks; }

    public CourseModel(long seed) { this(seed, new SparseBlocks()); }

    public void recomputeBounds() {
        final int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        blocks.forEach((x, y, z, s) -> {
            if (x < b[0]) b[0] = x; if (y < b[1]) b[1] = y; if (z < b[2]) b[2] = z;
            if (x > b[3]) b[3] = x; if (y > b[4]) b[4] = y; if (z > b[5]) b[5] = z;
        });
        for (Timed t : timed) for (BlockPos p : t.positions()) {
            if (p.x() < b[0]) b[0] = p.x(); if (p.y() < b[1]) b[1] = p.y(); if (p.z() < b[2]) b[2] = p.z();
            if (p.x() > b[3]) b[3] = p.x(); if (p.y() > b[4]) b[4] = p.y(); if (p.z() > b[5]) b[5] = p.z();
        }
        if (b[0] == Integer.MAX_VALUE) { minX = minY = minZ = maxX = maxY = maxZ = 0; return; }
        minX = b[0]; minY = b[1]; minZ = b[2]; maxX = b[3]; maxY = b[4]; maxZ = b[5];
    }

    // ---- lookups used by the runtime ----

    public Obstacle obstacle(String id) {
        for (Obstacle o : obstacles) if (o.id().equalsIgnoreCase(id)) return o;
        return null;
    }

    public List<Obstacle> ofKind(Kind k) {
        List<Obstacle> out = new ArrayList<>();
        for (Obstacle o : obstacles) if (o.kind() == k) out.add(o);
        return Collections.unmodifiableList(out);
    }

    public Hub hub(int stage) {
        for (Hub h : hubs) if (h.stage() == stage) return h;
        return null;
    }

    public static String mainId(int stage, int index) { return "M" + stage + "-" + index; }
    public static String bonusId(int stage, int index) { return "B" + stage + "-" + index; }
    public static String endingId(Difficulty d, int index) { return "E-" + d.name() + "-" + index; }
}
