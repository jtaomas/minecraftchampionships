package com.mcm.games.acerace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * The race course, generated in code. A cursor ({@link C}) walks the course: f = forward,
 * l = lateral (positive = right), u = up relative to the cursor's standing level (floor block is at u = -1).
 */
public final class Track {
    public static final int BASE_Y = 100;
    private static final Material UNDER = Material.SMOOTH_STONE;

    public enum PadType { SPEED, JUMP, JUMP_FAR, LAUNCH }

    public record Pad(PadType type, int dx, int dz) {}

    public record Checkpoint(String name, Region region, Location respawn, boolean trident, double voidY) {}

    public record Ring(Vector center, int dx, int dz, double radius) {}

    public static final class Region {
        final double x1, y1, z1, x2, y2, z2;

        Region(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1; this.x2 = x2; this.y2 = y2; this.z2 = z2;
        }

        boolean contains(Vector v) {
            return v.getX() >= x1 && v.getX() < x2 && v.getY() >= y1 && v.getY() < y2 && v.getZ() >= z1 && v.getZ() < z2;
        }

        Vector center() { return new Vector((x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2); }
    }

    /** Course cursor: origin block + horizontal facing direction. */
    record C(int x, int y, int z, int dx, int dz) {
        int wx(int f, int l) { return x + f * dx - l * dz; }
        int wz(int f, int l) { return z + f * dz + l * dx; }
        C adv(int n) { return new C(x + n * dx, y, z + n * dz, dx, dz); }
        C up(int n) { return new C(x, y + n, z, dx, dz); }
        C right() { return new C(x, y, z, -dz, dx); }
        C left() { return new C(x, y, z, dz, -dx); }
        float yaw() { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }
    }

    final World world;
    final Set<Long> placed = new LinkedHashSet<>();
    final List<Long> gate = new ArrayList<>();
    final Map<Long, Pad> pads = new HashMap<>();
    final Set<Long> hazards = new HashSet<>();
    final List<Checkpoint> checkpoints = new ArrayList<>();
    final List<Ring> rings = new ArrayList<>();
    final List<Location> gridSlots = new ArrayList<>();
    final List<Location> padParticles = new ArrayList<>();
    Region finish;
    Location spectatorSpot;
    Location gridCenter;

    Track(World world) { this.world = world; }

    // ------------------------------------------------------------------ keys

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    static int kx(long k) { return (int) (k >> 38); }
    static int ky(long k) { return (int) (k << 52 >> 52); }
    static int kz(long k) { return (int) (k << 26 >> 38); }

    // ------------------------------------------------------------------ primitives

    private long set(C c, int f, int l, int u, Material m) {
        int x = c.wx(f, l), y = c.y + u, z = c.wz(f, l);
        world.getBlockAt(x, y, z).setType(m, false);
        long k = key(x, y, z);
        placed.add(k);
        return k;
    }

    private void fill(C c, int f1, int f2, int l1, int l2, int u1, int u2, Material m) {
        for (int f = Math.min(f1, f2); f <= Math.max(f1, f2); f++)
            for (int l = Math.min(l1, l2); l <= Math.max(l1, l2); l++)
                for (int u = Math.min(u1, u2); u <= Math.max(u1, u2); u++)
                    set(c, f, l, u, m);
    }

    /** Floor you stand on at level u (block at u-1, support underneath at u-2). Edge stripe on the lateral sides. */
    private void floor(C c, int f1, int f2, int l1, int l2, int u, Material m, Material edge) {
        for (int f = f1; f <= f2; f++)
            for (int l = l1; l <= l2; l++) {
                set(c, f, l, u - 1, (l == l1 || l == l2) ? edge : m);
                set(c, f, l, u - 2, UNDER);
            }
    }

    private void padRow(C c, int f, int u, PadType type, Material m) {
        for (int l = -3; l <= 3; l++) {
            long k = set(c, f, l, u - 1, m);
            pads.put(k, new Pad(type, c.dx, c.dz));
            if (l % 2 == 0) padParticles.add(new Location(world, c.wx(f, l) + 0.5, c.y + u + 0.2, c.wz(f, l) + 0.5));
        }
    }

    private void hazard(C c, int f, int l, int u) {
        hazards.add(set(c, f, l, u - 1, Material.MAGMA_BLOCK));
    }

    /** A hazard row of magma from f1..f2 across the track, leaving l in [gap1, gap2] safe. */
    private void hazardRow(C c, int f1, int f2, int gap1, int gap2, int u) {
        for (int f = f1; f <= f2; f++)
            for (int l = -3; l <= 3; l++)
                if (l < gap1 || l > gap2) hazard(c, f, l, u);
    }

    private Region region(C c, int f1, int f2, int l1, int l2, int u1, int u2) {
        int ax = c.wx(f1, l1), az = c.wz(f1, l1), bx = c.wx(f2, l2), bz = c.wz(f2, l2);
        return new Region(Math.min(ax, bx), c.y + Math.min(u1, u2), Math.min(az, bz),
                Math.max(ax, bx) + 1, c.y + Math.max(u1, u2) + 1, Math.max(az, bz) + 1);
    }

    private Location loc(C c, int f, int l, int u) {
        return new Location(world, c.wx(f, l) + 0.5, c.y + u, c.wz(f, l) + 0.5, c.yaw(), 0f);
    }

    private void ring(C c, int f, int uc, double r, Material m) {
        Set<Long> done = new HashSet<>();
        for (int deg = 0; deg < 360; deg += 4) {
            double a = Math.toRadians(deg);
            int l = (int) Math.round(r * Math.cos(a));
            int u = (int) Math.round(r * Math.sin(a));
            if (done.add(((long) l << 32) ^ (u & 0xffffffffL))) set(c, f, l, uc + u, m);
        }
        rings.add(new Ring(new Vector(c.wx(f, 0) + 0.5, c.y + uc + 0.5, c.wz(f, 0) + 0.5), c.dx, c.dz, r - 0.6));
    }

    /** Checkered arch over the track at f. */
    private void arch(C c, int f) {
        fill(c, f, f, -4, -4, -1, 5, Material.BLACK_CONCRETE);
        fill(c, f, f, 4, 4, -1, 5, Material.BLACK_CONCRETE);
        for (int l = -4; l <= 4; l++)
            for (int u = 5; u <= 6; u++)
                set(c, f, l, u, ((l + u) & 1) == 0 ? Material.WHITE_WOOL : Material.BLACK_WOOL);
    }

    /** 7x7 turn platform; returns the cursor for the next leg. */
    private C corner(C c, boolean right, Material m, Material edge) {
        floor(c, 0, 6, -3, 3, 0, m, edge);
        set(c, 3, 0, -1, Material.SEA_LANTERN);
        int cx = c.wx(3, 0), cz = c.wz(3, 0);
        C nd = right ? c.right() : c.left();
        return new C(cx + 4 * nd.dx, c.y, cz + 4 * nd.dz, nd.dx, nd.dz);
    }

    // ------------------------------------------------------------------ course

    void build() {
        // ---------------- S0: start straight (east)
        C c0 = new C(0, BASE_Y, 0, 1, 0);
        floor(c0, 0, 29, -3, 3, 0, Material.WHITE_CONCRETE, Material.BLACK_CONCRETE);
        for (int f = 0; f <= 11; f++)
            for (int l = -3; l <= 3; l++)
                set(c0, f, l, -1, ((f + l) & 1) == 0 ? Material.WHITE_CONCRETE : Material.LIGHT_GRAY_CONCRETE);
        fill(c0, -1, -1, -4, 4, -2, 3, Material.WHITE_STAINED_GLASS);
        fill(c0, 0, 11, -4, -4, -2, 2, Material.WHITE_STAINED_GLASS);
        fill(c0, 0, 11, 4, 4, -2, 2, Material.WHITE_STAINED_GLASS);
        arch(c0, 12);
        for (int l = -3; l <= 3; l++)
            for (int u = 0; u <= 3; u++)
                gate.add(set(c0, 12, l, u, Material.LIGHT_BLUE_STAINED_GLASS));
        padRow(c0, 18, 0, PadType.SPEED, Material.MAGENTA_GLAZED_TERRACOTTA);
        padRow(c0, 25, 0, PadType.SPEED, Material.MAGENTA_GLAZED_TERRACOTTA);
        checkpoints.add(new Checkpoint("Start", null, loc(c0, 15, 0, 0), false, c0.y - 14));
        int[] lanes = {0, -1, 1, -2, 2, -3, 3};
        for (int row = 0; row < 6; row++)
            for (int l : lanes) gridSlots.add(loc(c0, 11 - row * 2, l, 0));
        gridCenter = loc(c0, 6, 0, 0);

        // ---------------- S1: jump pads, climbing two ledges
        C c1 = c0.adv(30);
        checkpoints.add(new Checkpoint("Jump Pads", region(c1, 0, 2, -3, 3, 0, 4), loc(c1, 1, 0, 0), false, c1.y - 14));
        floor(c1, 0, 8, -3, 3, 0, Material.LIME_CONCRETE, Material.GREEN_CONCRETE);
        padRow(c1, 5, 0, PadType.JUMP, Material.EMERALD_BLOCK);
        fill(c1, 9, 9, -3, 3, 0, 3, Material.GREEN_CONCRETE);
        floor(c1, 9, 17, -3, 3, 6, Material.LIME_CONCRETE, Material.GREEN_CONCRETE);
        padRow(c1, 14, 6, PadType.JUMP, Material.EMERALD_BLOCK);
        fill(c1, 18, 18, -3, 3, 6, 9, Material.GREEN_CONCRETE);
        floor(c1, 18, 27, -3, 3, 12, Material.LIME_CONCRETE, Material.GREEN_CONCRETE);

        // ---------------- S2: parkour
        C c2 = c1.adv(28).up(12);
        checkpoints.add(new Checkpoint("Parkour", region(c2, 0, 2, -3, 3, 0, 4), loc(c2, 1, 0, 0), false, c2.y - 14));
        floor(c2, 0, 4, -3, 3, 0, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE);
        int[][] plats = {{7, 9, 0, 0}, {12, 14, 2, 1}, {17, 19, -1, 1}, {23, 24, 0, 0}, {27, 29, 2, 0}};
        for (int[] p : plats) floor(c2, p[0], p[1], p[2] - 1, p[2] + 1, p[3], Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE);
        floor(c2, 32, 36, -3, 3, 0, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE);
        C c3 = corner(c2.adv(37), true, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE); // -> south

        // ---------------- S3: elytra launch + glide through rings
        checkpoints.add(new Checkpoint("Elytra Launch", region(c3, 0, 2, -3, 3, 0, 4), loc(c3, 1, 0, 0), false, c3.y - 26));
        floor(c3, 0, 9, -3, 3, 0, Material.LIGHT_BLUE_CONCRETE, Material.WHITE_CONCRETE);
        padRow(c3, 6, 0, PadType.LAUNCH, Material.PURPUR_BLOCK);
        padRow(c3, 7, 0, PadType.LAUNCH, Material.PURPUR_BLOCK);
        ring(c3, 20, 10, 3.5, Material.GOLD_BLOCK);
        ring(c3, 34, 4, 3.5, Material.GOLD_BLOCK);
        ring(c3, 47, -3, 3.5, Material.GOLD_BLOCK);
        floor(c3, 60, 77, -7, 7, -12, Material.CYAN_CONCRETE, Material.WHITE_CONCRETE);
        checkpoints.add(new Checkpoint("Landing", region(c3, 60, 77, -7, 7, -12, 16), loc(c3, 63, 0, -12), false, c3.y - 12 - 14));
        C cl = c3.adv(78).up(-12);
        C c4 = corner(cl, false, Material.CYAN_CONCRETE, Material.WHITE_CONCRETE); // -> east
        fill(cl, 7, 7, -7, 7, -2, 8, Material.LIGHT_BLUE_STAINED_GLASS);
        fill(c3, 60, 85, 8, 8, -14, -4, Material.LIGHT_BLUE_STAINED_GLASS);

        // ---------------- S4: the drop + hazard run
        floor(c4, 0, 7, -3, 3, 0, Material.RED_CONCRETE, Material.WHITE_CONCRETE);
        for (int l = -3; l <= 3; l++) set(c4, 7, l, -1, (l & 1) == 0 ? Material.YELLOW_CONCRETE : Material.BLACK_CONCRETE);
        C d = c4.up(-12);
        floor(d, 5, 51, -3, 3, 0, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS);
        floor(d, 5, 16, -4, 4, 0, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS);
        checkpoints.add(new Checkpoint("Hazard Run", region(d, 5, 14, -4, 4, 0, 5), loc(d, 12, 0, 0), false, d.y - 14));
        hazardRow(d, 20, 22, 2, 3, 0);
        hazardRow(d, 28, 30, -3, -2, 0);
        hazardRow(d, 36, 38, 0, 1, 0);
        hazardRow(d, 44, 46, -2, -1, 0);
        C c5 = corner(d.adv(52), true, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS); // -> south

        // ---------------- S5: riptide pool
        checkpoints.add(new Checkpoint("Riptide Pool", region(c5, 0, 2, -3, 3, 0, 4), loc(c5, 1, 0, 0), true, c5.y - 14));
        floor(c5, 0, 3, -3, 3, 0, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
        fill(c5, 3, 50, -4, -4, -5, 4, Material.LIGHT_BLUE_STAINED_GLASS);
        fill(c5, 3, 50, 4, 4, -5, 4, Material.LIGHT_BLUE_STAINED_GLASS);
        fill(c5, 4, 49, -3, 3, -5, -5, Material.DARK_PRISMARINE);
        fill(c5, 3, 3, -3, 3, -5, -2, Material.PRISMARINE);
        fill(c5, 50, 50, -3, 3, -5, -2, Material.PRISMARINE);
        fill(c5, 4, 49, -3, 3, -4, -1, Material.WATER);
        for (int f = 6; f < 49; f += 5) {
            set(c5, f, -3, -5, Material.SEA_LANTERN);
            set(c5, f, 3, -5, Material.SEA_LANTERN);
        }
        fill(c5, 16, 17, -1, 1, -4, 2, Material.PRISMARINE_BRICKS);
        fill(c5, 29, 30, -3, -2, -4, 2, Material.PRISMARINE_BRICKS);
        fill(c5, 29, 30, 2, 3, -4, 2, Material.PRISMARINE_BRICKS);
        fill(c5, 41, 42, 0, 1, -4, 2, Material.PRISMARINE_BRICKS);
        floor(c5, 50, 56, -3, 3, 0, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
        C c6 = corner(c5.adv(57), false, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE); // -> east

        // ---------------- S6: ring jump + finish straight
        checkpoints.add(new Checkpoint("Ring Jump", region(c6, 0, 3, -3, 3, 0, 6), loc(c6, 1, 0, 0), false, c6.y - 14));
        floor(c6, 0, 12, -3, 3, 0, Material.YELLOW_CONCRETE, Material.WHITE_CONCRETE);
        padRow(c6, 5, 0, PadType.SPEED, Material.MAGENTA_GLAZED_TERRACOTTA);
        padRow(c6, 11, 0, PadType.JUMP_FAR, Material.EMERALD_BLOCK);
        padRow(c6, 12, 0, PadType.JUMP_FAR, Material.EMERALD_BLOCK);
        ring(c6, 17, 3, 3.5, Material.GOLD_BLOCK);
        floor(c6, 22, 45, -3, 3, 0, Material.YELLOW_CONCRETE, Material.WHITE_CONCRETE);
        for (int f = 38; f <= 39; f++)
            for (int l = -3; l <= 3; l++)
                set(c6, f, l, -1, ((f + l) & 1) == 0 ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE);
        arch(c6, 38);
        fill(c6, 46, 46, -4, 4, -2, 3, Material.WHITE_STAINED_GLASS);
        finish = region(c6, 38, 39, -3, 3, 0, 5);
        // grandstand / spectator area beside the finish
        for (int i = 0; i < 5; i++)
            fill(c6, 30, 46, 5 + i, 5 + i, -2, i - 1, i % 2 == 0 ? Material.QUARTZ_BLOCK : Material.YELLOW_CONCRETE);
        C cs = c6.left();
        spectatorSpot = new Location(world, c6.wx(40, 9) + 0.5, c6.y + 5, c6.wz(40, 9) + 0.5, cs.yaw(), 10f);
    }

    Location gridSlot(int i) { return gridSlots.get(i % gridSlots.size()).clone(); }

    void colorSlot(int i, Material m) {
        if (i >= gridSlots.size()) return;
        Location s = gridSlots.get(i);
        world.getBlockAt(s.getBlockX(), s.getBlockY() - 1, s.getBlockZ()).setType(m, false);
    }

    void openGate() {
        for (long k : gate) world.getBlockAt(kx(k), ky(k), kz(k)).setType(Material.AIR, false);
    }

    void clear() {
        for (long k : placed) world.getBlockAt(kx(k), ky(k), kz(k)).setType(Material.AIR, false);
        placed.clear();
    }
}
