package com.mcm.games.dodgebolt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * The generated Dodgebolt stadium.
 *
 * Layout (block coordinates, floor at y = FLOOR_Y):
 *  - Side A floor: x -10..10, z -14..-2.   Side B floor: x -10..10, z 2..14.
 *  - River (impassable gap): z -1..1, a glowing channel one block below the floor.
 *  - Glass walls at x = -11 / 11 and z = -15 / 15, barrier ceiling on top.
 *  - Four tiers of spectator stands outside the walls.
 * The arena is symmetric about z = 0.5 (block z mirrors to -z).
 *
 * Ring-collapse idea adapted from DevvyDont's MIT-licensed Dodgebolt recreation.
 */
public final class Arena {
    public static final int FLOOR_Y = 64;
    public static final int HALF_X = 10;     // floor x in [-10, 10]
    public static final int BACK_Z = 14;     // floor |z| up to 14
    public static final int RIVER_Z = 1;     // river blocks z in [-1, 1]
    public static final int WALL_X = 11, WALL_Z = 15, WALL_TOP = FLOOR_Y + 10;
    public static final int RINGS = 6;
    /** Side boundary edges in real coordinates: A must stay at z <= A_EDGE, B at z >= B_EDGE. */
    public static final double A_EDGE = -1.0, B_EDGE = 2.0;

    private final Plugin plugin;
    private final World world;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Random random = new Random();
    private DyeColor dyeA = DyeColor.RED, dyeB = DyeColor.BLUE;

    public Arena(Plugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    public World world() { return world; }

    // ------------------------------------------------------------------ geometry

    /** 0 = side A (negative z), 1 = side B. */
    public static int sideOfZ(double z) { return z < 0.5 ? 0 : 1; }

    public static boolean inRiver(double z) { return z >= A_EDGE && z < B_EDGE; }

    public static boolean isFloor(int x, int z) {
        return x >= -HALF_X && x <= HALF_X && Math.abs(z) >= RIVER_Z + 1 && Math.abs(z) <= BACK_Z;
    }

    /** Ring index of a floor block: 0 = outermost. */
    public static int ringOf(int x, int z) {
        int sideDist = Math.min(x + HALF_X, HALF_X - x);
        int backDist = BACK_Z - Math.abs(z);
        return Math.min(sideDist, backDist);
    }

    /** Inside the glass box (where arrows and players live). */
    public static boolean inBox(Location l) {
        return l.getX() > -WALL_X && l.getX() < WALL_X + 1 && l.getZ() > -WALL_Z && l.getZ() < WALL_Z + 1
                && l.getY() > FLOOR_Y - 12 && l.getY() < WALL_TOP + 2;
    }

    public Location spectatorSpot(int i) {
        // Top tier of the east / west stands, facing the arena.
        boolean east = i % 2 == 0;
        int z = -12 + (i * 5) % 25;
        int x = east ? WALL_X + 4 : -WALL_X - 4;
        Location l = new Location(world, x + 0.5, FLOOR_Y + 6, z + 0.5);
        l.setYaw(east ? 90 : -90);
        l.setPitch(20);
        return l;
    }

    public Location pad(int side, int index, int count) {
        int perRow = Math.min(Math.max(count, 1), 9);
        int row = (index / 9) % 2;
        double x = -8 + 16.0 * ((index % 9) + 0.5) / perRow + (row == 1 ? 1 : 0);
        int bx = (int) Math.floor(x);
        int bz = 11 + row * 2;
        if (side == 0) bz = -bz;
        Location l = new Location(world, bx + 0.5, FLOOR_Y + 1, bz + 0.5);
        l.setYaw(side == 0 ? 0 : 180);
        return l;
    }

    // ------------------------------------------------------------------ building

    public void setDyes(DyeColor a, DyeColor b) { dyeA = a; dyeB = b; }

    public DyeColor dye(int side) { return side == 0 ? dyeA : dyeB; }

    private static Material mat(DyeColor d, String suffix) {
        Material m = Material.matchMaterial(d.name() + "_" + suffix);
        return m == null ? Material.matchMaterial("WHITE_" + suffix) : m;
    }

    private void set(int x, int y, int z, Material m) {
        world.getBlockAt(x, y, z).setType(m, false);
    }

    /** Wipe the entire build region (about 25k blocks). */
    public void clearAll() {
        cancelTasks();
        for (int x = -WALL_X - 6; x <= WALL_X + 6; x++)
            for (int z = -WALL_Z - 6; z <= WALL_Z + 6; z++)
                for (int y = FLOOR_Y - 3; y <= WALL_TOP + 3; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
    }

    /** Build walls, river, stands and floor for a match. */
    public void buildAll() {
        clearAll();
        // River channel: glass over sea lanterns, one block below the floor.
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -RIVER_Z; z <= RIVER_Z; z++) {
                set(x, FLOOR_Y - 1, z, Material.LIGHT_BLUE_STAINED_GLASS);
                set(x, FLOOR_Y - 2, z, Material.SEA_LANTERN);
            }
        // Walls
        for (int y = FLOOR_Y - 2; y <= WALL_TOP; y++) {
            for (int z = -WALL_Z; z <= WALL_Z; z++) {
                Material m = wallMat(y, z);
                set(-WALL_X, y, z, m);
                set(WALL_X, y, z, m);
            }
            for (int x = -WALL_X; x <= WALL_X; x++) {
                set(x, y, -WALL_Z, wallMat(y, -WALL_Z));
                set(x, y, WALL_Z, wallMat(y, WALL_Z));
            }
        }
        // Ceiling (invisible) so arrows can't fly into the stands.
        for (int x = -WALL_X; x <= WALL_X; x++)
            for (int z = -WALL_Z; z <= WALL_Z; z++)
                set(x, WALL_TOP + 1, z, Material.BARRIER);
        // Stands
        for (int x = -WALL_X - 6; x <= WALL_X + 6; x++)
            for (int z = -WALL_Z - 6; z <= WALL_Z + 6; z++) {
                int ex = Math.max(-WALL_X - x, x - WALL_X);
                int ez = Math.max(-WALL_Z - z, z - WALL_Z);
                int d = Math.max(ex, ez);
                if (d >= 1 && d <= 4) {
                    int top = FLOOR_Y + 1 + d;
                    Material step;
                    if (z < -WALL_Z) step = mat(dyeA, "TERRACOTTA");
                    else if (z > WALL_Z) step = mat(dyeB, "TERRACOTTA");
                    else step = (d % 2 == 0) ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE;
                    set(x, top, z, step);
                    set(x, top - 1, z, Material.DEEPSLATE_BRICKS);
                    if (d == 4 && (x + z) % 6 == 0) set(x, top + 1, z, Material.LANTERN);
                } else if (d == 5) {
                    set(x, FLOOR_Y + 5, z, Material.DEEPSLATE_BRICKS);
                    set(x, FLOOR_Y + 6, z, Material.POLISHED_BLACKSTONE_WALL);
                    for (int y = FLOOR_Y + 7; y <= FLOOR_Y + 9; y++) set(x, y, z, Material.BARRIER);
                }
            }
        resetFloor();
    }

    private Material wallMat(int y, int z) {
        if (y <= FLOOR_Y) {
            if (Arena.inRiver(z)) return Material.WHITE_CONCRETE;
            return mat(sideOfZ(z) == 0 ? dyeA : dyeB, "CONCRETE");
        }
        if (y == WALL_TOP) return Material.SEA_LANTERN;
        return Material.GLASS;
    }

    /** Restore the full floor (start of every round). */
    public void resetFloor() {
        cancelTasks();
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -BACK_Z; z <= BACK_Z; z++) {
                if (!isFloor(x, z)) continue;
                DyeColor d = sideOfZ(z) == 0 ? dyeA : dyeB;
                Material m;
                if (Math.abs(z) == RIVER_Z + 1) m = Material.WHITE_CONCRETE;          // foul line
                else m = ((x + z) & 1) == 0 ? mat(d, "CONCRETE") : mat(d, "TERRACOTTA");
                set(x, FLOOR_Y, z, m);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 3; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
            }
    }

    public void markPad(Location pad, int side) {
        world.getBlockAt(pad.getBlockX(), FLOOR_Y, pad.getBlockZ()).setType(mat(dye(side), "WOOL"), false);
    }

    // ------------------------------------------------------------------ collapse

    public void cancelTasks() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
    }

    /** Flash then remove every floor block of ring r. */
    public void dropRing(int r) {
        List<Block> blocks = new ArrayList<>();
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -BACK_Z; z <= BACK_Z; z++)
                if (isFloor(x, z) && ringOf(x, z) == r) {
                    Block b = world.getBlockAt(x, FLOOR_Y, z);
                    if (b.getType() != Material.AIR) blocks.add(b);
                }
        collapse(blocks, 6, true);
    }

    /** Overtime finale: crumble n random remaining floor blocks. Returns false if nothing is left. */
    public boolean shatter(int n) {
        List<Block> left = new ArrayList<>();
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -BACK_Z; z <= BACK_Z; z++)
                if (isFloor(x, z)) {
                    Block b = world.getBlockAt(x, FLOOR_Y, z);
                    Material t = b.getType();
                    if (t != Material.AIR && t != Material.RED_STAINED_GLASS) left.add(b);
                }
        if (left.isEmpty()) return false;
        Collections.shuffle(left, random);
        collapse(left.subList(0, Math.min(n, left.size())), 3, false);
        return true;
    }

    private void collapse(List<Block> blocks, int flashes, boolean loud) {
        if (blocks.isEmpty()) return;
        final List<Block> list = new ArrayList<>(blocks);
        final Map<Block, Material> orig = new HashMap<>();
        for (Block b : list) orig.put(b, b.getType());
        final Location sound = new Location(world, 0.5, FLOOR_Y + 2, 0.5);
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int step = 0;
            @Override public void run() {
                if (step >= flashes) {
                    for (Block b : list) b.setType(Material.AIR, false);
                    world.playSound(sound, Sound.BLOCK_GLASS_BREAK, loud ? 2f : 0.6f, 0.6f);
                    if (loud) world.playSound(sound, Sound.BLOCK_BEACON_DEACTIVATE, 2f, 0.6f);
                    holder[0].cancel();
                    tasks.remove(holder[0]);
                    return;
                }
                boolean red = step % 2 == 0;
                for (Block b : list) {
                    Material now = b.getType();
                    if (now == Material.AIR) continue;
                    b.setType(red ? Material.RED_STAINED_GLASS : orig.get(b), false);
                }
                world.playSound(red ? list.get(0).getLocation() : sound, Sound.BLOCK_LEVER_CLICK, loud ? 1.5f : 0.6f, red ? 1.4f : 1.2f);
                step++;
            }
        }, 1L, 5L);
        tasks.add(holder[0]);
    }

    /** Where an arrow should appear for a side: its centre spot, or any remaining floor block on that side. */
    public Location arrowSpot(int side) {
        int z = side == 0 ? -7 : 7;
        Block b = world.getBlockAt(0, FLOOR_Y, z);
        if (b.getType().isSolid()) return b.getLocation().add(0.5, 1.2, 0.5);
        List<Block> opts = new ArrayList<>();
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int zz = RIVER_Z + 2; zz <= BACK_Z; zz++) {
                Block c = world.getBlockAt(x, FLOOR_Y, side == 0 ? -zz : zz);
                if (c.getType().isSolid() && c.getType() != Material.RED_STAINED_GLASS) opts.add(c);
            }
        if (opts.isEmpty()) return null;
        return opts.get(random.nextInt(opts.size())).getLocation().add(0.5, 1.2, 0.5);
    }
}
