package com.mcm.games.meltdown;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * The generated frozen facility: a 7x7 grid of rooms separated by mineable ice walls,
 * connected by doorways (a random spanning tree plus extra openings).
 */
public final class Arena {
    public static final int N = 7;
    public static final int PITCH = 16;
    public static final int OX = 0;
    public static final int OZ = 0;
    public static final int SIZE = N * PITCH;          // wall lines at 0..112
    public static final int FLOOR_Y = 64;
    public static final int WALL_MIN_Y = 65;
    public static final int WALL_MAX_Y = 69;
    public static final int CEIL_Y = 70;
    public static final int CENTER_X = OX + SIZE / 2;
    public static final int CENTER_Z = OZ + SIZE / 2;
    public static final int SPECTATOR_Y = 82;
    private static final int BUILD_MIN_Y = 60;
    private static final int BUILD_MAX_Y = 76;
    private static final int MARGIN = 3;
    private static final int COLUMNS_PER_TICK = 6;

    private static final Set<Material> BREAKABLE_WALL = EnumSet.of(
            Material.PACKED_ICE, Material.SNOW_BLOCK, Material.BLUE_ICE, Material.LIGHT_BLUE_STAINED_GLASS);

    public static final Material CRATE = Material.RAW_GOLD_BLOCK;
    public static final Material HEATER = Material.CAMPFIRE;

    private final World world;
    private final Random rng;
    private final long seed;
    public final Room[][] rooms = new Room[N][N];
    /** doorX[i][j]: opening between room (i,j) and (i+1,j). */
    private final boolean[][] doorX = new boolean[N - 1][N];
    /** doorZ[i][j]: opening between room (i,j) and (i,j+1). */
    private final boolean[][] doorZ = new boolean[N][N - 1];
    /** Extra blocks placed on top of the base layout (pads, decor, crates). */
    private final Map<Long, Material> overrides = new HashMap<>();
    private final Set<Long> crates = new HashSet<>();

    public Arena(World world, Random rng) {
        this.world = world;
        this.rng = rng;
        this.seed = rng.nextLong();
        int c = N / 2;
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                rooms[i][j] = new Room(i, j, Math.max(Math.abs(i - c), Math.abs(j - c)));
        generateDoors();
    }

    public World world() { return world; }

    // ------------------------------------------------------------------ layout

    private void generateDoors() {
        // randomized DFS spanning tree so every room is reachable without mining
        boolean[][] seen = new boolean[N][N];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{N / 2, N / 2});
        seen[N / 2][N / 2] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] d : dirs) {
                int ni = cur[0] + d[0], nj = cur[1] + d[1];
                if (ni >= 0 && nj >= 0 && ni < N && nj < N && !seen[ni][nj]) options.add(new int[]{ni, nj});
            }
            if (options.isEmpty()) { stack.pop(); continue; }
            int[] nxt = options.get(rng.nextInt(options.size()));
            openBetween(cur[0], cur[1], nxt[0], nxt[1]);
            seen[nxt[0]][nxt[1]] = true;
            stack.push(nxt);
        }
        // extra openings so it is not a pure maze
        for (int i = 0; i < N - 1; i++) for (int j = 0; j < N; j++) if (rng.nextDouble() < 0.30) doorX[i][j] = true;
        for (int i = 0; i < N; i++) for (int j = 0; j < N - 1; j++) if (rng.nextDouble() < 0.30) doorZ[i][j] = true;
        // the core always opens on all four sides
        int c = N / 2;
        doorX[c - 1][c] = doorX[c][c] = true;
        doorZ[c][c - 1] = doorZ[c][c] = true;
    }

    private void openBetween(int i1, int j1, int i2, int j2) {
        if (i1 != i2) doorX[Math.min(i1, i2)][j1] = true;
        else doorZ[i1][Math.min(j1, j2)] = true;
    }

    /** Outer ring rooms in angular order around the centre (for even spawn spreading). */
    public List<Room> outerRooms() {
        List<Room> list = new ArrayList<>();
        for (Room[] col : rooms) for (Room r : col) if (r.ring == N / 2) list.add(r);
        list.sort(Comparator.comparingDouble(r -> Math.atan2(r.centerZ() - CENTER_Z, r.centerX() - CENTER_X)));
        return list;
    }

    public List<Room> allRooms() {
        List<Room> list = new ArrayList<>();
        for (Room[] col : rooms) list.addAll(Arrays.asList(col));
        return list;
    }

    /** Places a 3x3 coloured start pad around (cx, cz) and returns the spawn location. */
    public Location addPad(int cx, int cz, Material pad) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                overrides.put(key(cx + dx, FLOOR_Y, cz + dz), pad);
        return new Location(world, cx + 0.5, FLOOR_Y + 1, cz + 0.5);
    }

    /** Random decor and coin crates. Call after pads are placed. */
    public void decorate() {
        for (Room r : allRooms()) {
            boolean core = r.ring == 0;
            if (core) {
                // warm core: a glowing reactor column surrounded by crates
                int cx = r.centerX(), cz = r.centerZ();
                for (int y = WALL_MIN_Y; y <= WALL_MIN_Y + 2; y++) overrides.put(key(cx, y, cz), Material.OCHRE_FROGLIGHT);
                overrides.put(key(cx, WALL_MIN_Y + 3, cz), Material.LIGHTNING_ROD);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    if (dx != 0 || dz != 0) overrides.put(key(cx + dx, FLOOR_Y, cz + dz), Material.WAXED_CUT_COPPER);
                int[][] ring = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {0, -4}, {0, 4}, {-4, 0}, {4, 0}, {-5, -5}, {5, 5}, {-5, 5}, {5, -5}};
                for (int[] p : ring) addCrate(cx + p[0], cz + p[1]);
                continue;
            }
            // pillars / snow piles
            int obstacles = 2 + rng.nextInt(3);
            for (int k = 0; k < obstacles; k++) {
                int x = r.minX() + 2 + rng.nextInt(11);
                int z = r.minZ() + 2 + rng.nextInt(11);
                if (Math.abs(x - r.centerX()) <= 2 && Math.abs(z - r.centerZ()) <= 2) continue;
                if (overrides.containsKey(key(x, WALL_MIN_Y, z))) continue;
                int h = 1 + rng.nextInt(3);
                Material m = rng.nextBoolean() ? Material.PACKED_ICE : Material.SNOW_BLOCK;
                for (int y = WALL_MIN_Y; y < WALL_MIN_Y + h; y++) overrides.put(key(x, y, z), m);
            }
            // a few snow layers and a sneaky powder-snow patch
            for (int k = 0; k < 6; k++) {
                int x = r.minX() + rng.nextInt(15), z = r.minZ() + rng.nextInt(15);
                overrides.putIfAbsent(key(x, WALL_MIN_Y, z), Material.SNOW);
            }
            // lights in the floor
            overrides.putIfAbsent(key(r.minX() + 3, FLOOR_Y, r.minZ() + 3), Material.SEA_LANTERN);
            overrides.putIfAbsent(key(r.maxX() - 3, FLOOR_Y, r.maxZ() - 3), Material.SEA_LANTERN);
            overrides.putIfAbsent(key(r.minX() + 3, FLOOR_Y, r.maxZ() - 3), Material.SEA_LANTERN);
            overrides.putIfAbsent(key(r.maxX() - 3, FLOOR_Y, r.minZ() + 3), Material.SEA_LANTERN);
            int crateCount = r.ring == N / 2 ? 2 : 3;
            int tries = 0;
            while (crateCount > 0 && tries++ < 40) {
                int x = r.minX() + 1 + rng.nextInt(13), z = r.minZ() + 1 + rng.nextInt(13);
                if (Math.abs(x - r.centerX()) <= 1 && Math.abs(z - r.centerZ()) <= 1) continue;
                Material at = overrides.get(key(x, WALL_MIN_Y, z));
                if (at != null && at != Material.SNOW) continue;
                addCrate(x, z);
                crateCount--;
            }
        }
    }

    private void addCrate(int x, int z) {
        long k = key(x, WALL_MIN_Y, z);
        overrides.put(k, CRATE);
        crates.add(k);
    }

    public int crateCount() { return crates.size(); }

    public boolean isCrate(Block b) { return b.getType() == CRATE && crates.contains(key(b.getX(), b.getY(), b.getZ())); }

    public void removeCrate(Block b) { crates.remove(key(b.getX(), b.getY(), b.getZ())); }

    /** Encases all crates of a room in ice; returns how many were lost. */
    public int freezeCrates(Room r) {
        int lost = 0;
        Iterator<Long> it = crates.iterator();
        while (it.hasNext()) {
            long k = it.next();
            int x = keyX(k), z = keyZ(k);
            if (x >= r.minX() && x <= r.maxX() && z >= r.minZ() && z <= r.maxZ()) {
                Block b = world.getBlockAt(x, keyY(k), z);
                if (b.getType() == CRATE) b.setType(Material.BLUE_ICE, false);
                it.remove();
                lost++;
            }
        }
        return lost;
    }

    /** Material the fresh arena has at this position. */
    private Material desired(int x, int y, int z) {
        if (x < OX || z < OZ || x > OX + SIZE || z > OZ + SIZE) return Material.AIR;
        Material o = overrides.get(key(x, y, z));
        if (o != null) return o;
        int lx = Math.floorMod(x - OX, PITCH), lz = Math.floorMod(z - OZ, PITCH);
        boolean lineX = lx == 0, lineZ = lz == 0;
        boolean perimeter = x == OX || z == OZ || x == OX + SIZE || z == OZ + SIZE;
        if (y == FLOOR_Y) {
            if (lineX || lineZ) return Material.POLISHED_DIORITE;
            if (lx == 1 || lx == 15 || lz == 1 || lz == 15) return Material.LIGHT_GRAY_CONCRETE;
            return ((lx + lz) & 1) == 0 ? Material.WHITE_CONCRETE : Material.SMOOTH_QUARTZ;
        }
        if (y == CEIL_Y) {
            if (lineX || lineZ) return Material.WHITE_CONCRETE;
            return Material.GLASS;
        }
        if (y < WALL_MIN_Y || y > WALL_MAX_Y) return Material.AIR;
        if (lineX && lineZ) return Material.QUARTZ_PILLAR;
        if (perimeter) {
            if (y == WALL_MAX_Y) return Material.PACKED_ICE;
            if (y == 67 && ((lx + lz) % 4 == 2)) return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            return Material.QUARTZ_BRICKS;
        }
        if (lineX || lineZ) {
            boolean door;
            int along;
            if (lineX) {
                int i = (x - OX) / PITCH - 1;          // wall between room i and i+1
                int j = (z - OZ) / PITCH;
                door = doorX[i][j];
                along = lz;
            } else {
                int i = (x - OX) / PITCH;
                int j = (z - OZ) / PITCH - 1;
                door = doorZ[i][j];
                along = lx;
            }
            if (door && along >= 7 && along <= 9) {
                if (y <= 67) return Material.AIR;
                if (y == 68 && along == 8) return Material.SEA_LANTERN;
            }
            if (y == 67 && along % 4 == 2) return Material.LIGHT_BLUE_STAINED_GLASS;
            long h = hash(x, y, z);
            int roll = (int) Math.floorMod(h, 100);
            if (roll < 60) return Material.PACKED_ICE;
            if (roll < 90) return Material.SNOW_BLOCK;
            return Material.BLUE_ICE;
        }
        return Material.AIR;
    }

    private long hash(int x, int y, int z) {
        long h = seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return h;
    }

    /** True if a player may mine this block: an interior wall block of ice/snow/glass. */
    public boolean isBreakableWall(Block b) {
        if (b.getWorld() != world) return false;
        int x = b.getX(), y = b.getY(), z = b.getZ();
        if (y < WALL_MIN_Y || y > WALL_MAX_Y) return false;
        if (x <= OX || z <= OZ || x >= OX + SIZE || z >= OZ + SIZE) return false;
        int lx = Math.floorMod(x - OX, PITCH), lz = Math.floorMod(z - OZ, PITCH);
        boolean lineX = lx == 0, lineZ = lz == 0;
        if (lineX == lineZ) return false; // pillar or room interior
        return BREAKABLE_WALL.contains(b.getType());
    }

    /** True if the position is inside the facility's playable volume (rooms + wall lines, below the ceiling). */
    public boolean isInsidePlayable(Block b) {
        if (b.getWorld() != world) return false;
        int x = b.getX(), y = b.getY(), z = b.getZ();
        return x > OX && z > OZ && x < OX + SIZE && z < OZ + SIZE && y >= WALL_MIN_Y && y <= WALL_MAX_Y;
    }

    public Room roomAt(Location loc) {
        if (loc == null || loc.getWorld() != world) return null;
        double x = loc.getX() - OX, z = loc.getZ() - OZ;
        if (x < 0 || z < 0 || x > SIZE + 1 || z > SIZE + 1) return null;
        int i = Math.min(N - 1, Math.max(0, (int) Math.floor(x / PITCH)));
        int j = Math.min(N - 1, Math.max(0, (int) Math.floor(z / PITCH)));
        return rooms[i][j];
    }

    // ------------------------------------------------------------------ building

    /** Rebuilds the whole arena from scratch over several ticks, then runs onDone. */
    public BukkitTask build(Plugin plugin, Runnable onDone) {
        return new BukkitRunnable() {
            int x = OX - MARGIN;

            @Override
            public void run() {
                for (int c = 0; c < COLUMNS_PER_TICK; c++) {
                    if (x > OX + SIZE + MARGIN) {
                        cancel();
                        onDone.run();
                        return;
                    }
                    for (int z = OZ - MARGIN; z <= OZ + SIZE + MARGIN; z++) {
                        for (int y = BUILD_MIN_Y; y <= BUILD_MAX_Y; y++) {
                            Block b = world.getBlockAt(x, y, z);
                            Material m = desired(x, y, z);
                            if (b.getType() != m) b.setType(m, false);
                        }
                    }
                    x++;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ------------------------------------------------------------------ freezing visuals

    public void setCeiling(Room r, Material m) {
        for (int x = r.minX(); x <= r.maxX(); x++)
            for (int z = r.minZ(); z <= r.maxZ(); z++)
                world.getBlockAt(x, CEIL_Y, z).setType(m, false);
    }

    /** Warning visuals: frosted ceiling and snow creeping in along the walls. */
    public void applyWarning(Room r) {
        setCeiling(r, Material.LIGHT_BLUE_STAINED_GLASS);
        for (int x = r.minX(); x <= r.maxX(); x++)
            for (int z = r.minZ(); z <= r.maxZ(); z++) {
                boolean edge = x == r.minX() || x == r.maxX() || z == r.minZ() || z == r.maxZ();
                if (!edge || rng.nextDouble() > 0.6) continue;
                Block b = world.getBlockAt(x, WALL_MIN_Y, z);
                if (b.getType() == Material.AIR) b.setType(Material.SNOW, false);
            }
    }

    public void applyFrozen(Room r) {
        setCeiling(r, Material.BLUE_STAINED_GLASS);
    }

    /** One second of a frozen room's floor turning to ice and powder snow. */
    public void freezeStep(Room r) {
        for (int k = 0; k < 14; k++) {
            int x = r.minX() + rng.nextInt(15), z = r.minZ() + rng.nextInt(15);
            Block f = world.getBlockAt(x, FLOOR_Y, z);
            Material m = switch (rng.nextInt(3)) {
                case 0 -> Material.ICE;
                case 1 -> Material.PACKED_ICE;
                default -> Material.BLUE_ICE;
            };
            if (f.getType() != m) f.setType(m, false);
        }
        for (int k = 0; k < 3; k++) {
            int x = r.minX() + rng.nextInt(15), z = r.minZ() + rng.nextInt(15);
            Block b = world.getBlockAt(x, WALL_MIN_Y, z);
            if (b.getType() == Material.AIR || b.getType() == Material.SNOW) b.setType(Material.POWDER_SNOW, false);
        }
    }

    // ------------------------------------------------------------------ keys

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static int keyX(long k) { return (int) (k >> 38) << 6 >> 6; }
    static int keyZ(long k) { return (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6; }
    static int keyY(long k) { return (int) (k & 0xFFF) << 20 >> 20; }
}
