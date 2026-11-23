package com.mcm.games.sandsoftime;

import com.mcm.games.sandsoftime.Layout.ItemSpec;
import com.mcm.games.sandsoftime.Layout.Room;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mcm.games.sandsoftime.Layout.CELL;
import static com.mcm.games.sandsoftime.Layout.H;
import static com.mcm.games.sandsoftime.Layout.HUB_I;
import static com.mcm.games.sandsoftime.Layout.HUB_J;
import static com.mcm.games.sandsoftime.Layout.W;

/** One side's copy of the dungeon: block builder plus its runtime state (hourglass, doors, traps). */
final class Dungeon {
    static final int OY = 64;
    static final int SPACING = 100;
    static final int SIZE = W * CELL; // 60

    final int index;
    final String sideKey;
    final Layout L;
    final World w;
    final int ox, oy, oz;

    // runtime
    int time;
    boolean done;
    boolean ranOut;
    boolean started;

    final Set<Long> hourglass = new HashSet<>();
    final Set<Long> sphinx = new HashSet<>();
    final Set<Long> shrine = new HashSet<>();
    final Set<Long> exitPad = new HashSet<>();
    final Map<Long, KeyColor> doorBlocks = new HashMap<>();
    final Map<KeyColor, List<int[]>> doorsByColor = new EnumMap<>(KeyColor.class);
    final Set<KeyColor> opened = java.util.EnumSet.noneOf(KeyColor.class);
    final Set<Long> crumble = new HashSet<>();
    final Set<Long> crumbling = new HashSet<>();
    final Map<Long, Integer> plateRoom = new HashMap<>();
    final Map<Long, Integer> plateCooldown = new HashMap<>();
    final Map<Integer, List<Location>> dispensers = new HashMap<>();
    final boolean[] triggered = new boolean[W * H];
    final Map<Integer, List<Husk>> husks = new HashMap<>();
    final Map<KeyColor, Location> keySpawn = new EnumMap<>(KeyColor.class);
    boolean redReleased;
    final List<int[]> mobKeyBars = new ArrayList<>();
    final List<int[]> topSlots = new ArrayList<>();
    final List<int[]> bottomSlots = new ArrayList<>();
    final List<int[]> hubOpenings = new ArrayList<>();
    int lastTop = -1;

    Dungeon(int index, String sideKey, Layout layout, World w) {
        this.index = index;
        this.sideKey = sideKey;
        this.L = layout;
        this.w = w;
        this.ox = (index % 8) * SPACING;
        this.oy = OY;
        this.oz = (index / 8) * SPACING;
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    // ------------------------------------------------------------------ locations

    int hx() { return ox + HUB_I * CELL; }
    int hz() { return oz + HUB_J * CELL; }

    Location hubSpawn() { return new Location(w, hx() + 6.5, oy + 1, hz() + 2.5, 0f, 0f); }
    Location cage() { return new Location(w, hx() + 2.5, oy + 1, hz() - 1.5, 0f, 0f); }
    Location viewPos() { return new Location(w, hx() + 6.5, oy + 12, hz() + 6.5, 0f, 60f); }

    Room roomAt(int i, int j) { return L.room[i][j]; }

    /** Cell under a location, or null if outside the grid or on a wall line. */
    int[] cellOf(Location l) {
        int rx = l.getBlockX() - ox, rz = l.getBlockZ() - oz;
        if (rx <= 0 || rz <= 0 || rx >= SIZE || rz >= SIZE) return null;
        if (rx % CELL == 0 || rz % CELL == 0) return null;
        return new int[]{rx / CELL, rz / CELL};
    }

    boolean contains(Location l) {
        return l.getWorld() == w && l.getX() >= ox - 2 && l.getX() <= ox + SIZE + 2
                && l.getZ() >= oz - 6 && l.getZ() <= oz + SIZE + 2;
    }

    // ------------------------------------------------------------------ building

    private void set(int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    private void setData(int x, int y, int z, BlockData d) {
        w.getBlockAt(x, y, z).setBlockData(d, false);
    }

    /** Local → absolute for cell (i, j). */
    private int ax(int i, int lx) { return ox + i * CELL + lx; }
    private int az(int j, int lz) { return oz + j * CELL + lz; }

    void addTickets(Plugin plugin) {
        for (int cx = (ox - 8) >> 4; cx <= (ox + SIZE + 8) >> 4; cx++)
            for (int cz = (oz - 8) >> 4; cz <= (oz + SIZE + 8) >> 4; cz++)
                w.addPluginChunkTicket(cx, cz, plugin);
    }

    void removeTickets(Plugin plugin) {
        for (int cx = (ox - 8) >> 4; cx <= (ox + SIZE + 8) >> 4; cx++)
            for (int cz = (oz - 8) >> 4; cz <= (oz + SIZE + 8) >> 4; cz++)
                w.removePluginChunkTicket(cx, cz, plugin);
    }

    /** Remove every non-player entity in this dungeon's chunks. */
    void purgeEntities() {
        for (int cx = (ox - 8) >> 4; cx <= (ox + SIZE + 8) >> 4; cx++)
            for (int cz = (oz - 8) >> 4; cz <= (oz + SIZE + 8) >> 4; cz++) {
                Chunk c = w.getChunkAt(cx, cz);
                for (Entity e : c.getEntities()) if (!(e instanceof Player)) e.remove();
            }
    }

    /** Clear half of the dungeon volume (part 0 = lower, 1 = upper). */
    void clearPart(int part) {
        int y0 = part == 0 ? oy - 4 : oy + 3;
        int y1 = part == 0 ? oy + 2 : oy + 8;
        for (int x = ox - 1; x <= ox + SIZE + 1; x++)
            for (int z = oz - 6; z <= oz + SIZE + 1; z++)
                for (int y = y0; y <= y1; y++) {
                    var b = w.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
    }

    void build(Plugin plugin) {
        addTickets(plugin);
        int x1 = ox + SIZE, z1 = oz + SIZE;
        for (int x = ox; x <= x1; x++)
            for (int z = oz; z <= z1; z++) {
                set(x, oy, z, Material.SANDSTONE);
                set(x, oy + 7, z, Material.SMOOTH_SANDSTONE);
            }
        for (int k = 0; k <= W; k++) {
            int x = ox + k * CELL;
            for (int z = oz; z <= z1; z++) for (int y = 1; y <= 6; y++) set(x, oy + y, z, wallMat(y));
        }
        for (int k = 0; k <= H; k++) {
            int z = oz + k * CELL;
            for (int x = ox; x <= x1; x++) for (int y = 1; y <= 6; y++) set(x, oy + y, z, wallMat(y));
        }
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) buildRoom(i, j);
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) {
            if (L.doorE[i][j]) buildDoor(i, j, 0);
            if (L.doorN[i][j]) buildDoor(i, j, 1);
        }
        buildHub();
        buildOutside();
    }

    private static Material wallMat(int y) {
        return switch (y) {
            case 1 -> Material.SMOOTH_SANDSTONE;
            case 5 -> Material.CUT_SANDSTONE;
            default -> Material.SANDSTONE;
        };
    }

    /** Blocks of a 3x3 doorway between (i,j) and its neighbour in direction d (0 or 1 only). */
    private List<int[]> opening(int i, int j, int d, int latFrom, int latTo, int yFrom, int yTo) {
        List<int[]> out = new ArrayList<>();
        for (int lat = latFrom; lat <= latTo; lat++)
            for (int y = yFrom; y <= yTo; y++) {
                if (d == 0) out.add(new int[]{ox + (i + 1) * CELL, oy + y, oz + j * CELL + lat});
                else out.add(new int[]{ox + i * CELL + lat, oy + y, oz + (j + 1) * CELL});
            }
        return out;
    }

    private void buildDoor(int i, int j, int d) {
        int ni = i + Layout.DX[d], nj = j + Layout.DZ[d];
        KeyColor lock = L.vault[i][j] != null ? L.vault[i][j] : L.vault[ni][nj];
        List<int[]> blocks = opening(i, j, d, 5, 7, 1, 3);
        if (lock == null) {
            for (int[] b : blocks) set(b[0], b[1], b[2], Material.AIR);
            return;
        }
        for (int[] b : blocks) {
            set(b[0], b[1], b[2], lock.door);
            doorBlocks.put(key(b[0], b[1], b[2]), lock);
        }
        doorsByColor.computeIfAbsent(lock, c -> new ArrayList<>()).addAll(blocks);
        // Coloured frame marks the vault from the corridor side.
        for (int[] b : opening(i, j, d, 4, 4, 1, 4)) set(b[0], b[1], b[2], lock.frame);
        for (int[] b : opening(i, j, d, 8, 8, 1, 4)) set(b[0], b[1], b[2], lock.frame);
        for (int[] b : opening(i, j, d, 5, 7, 4, 4)) set(b[0], b[1], b[2], lock.frame);
    }

    private void floor(int i, int j, Material edge, Material inner) {
        for (int lx = 1; lx < CELL; lx++)
            for (int lz = 1; lz < CELL; lz++) {
                boolean border = lx == 1 || lz == 1 || lx == CELL - 1 || lz == CELL - 1;
                set(ax(i, lx), oy, az(j, lz), border ? edge : inner);
            }
    }

    private void lights(int i, int j, Material m) {
        int[][] spots = {{6, 3}, {6, 9}, {3, 6}, {9, 6}};
        for (int[] s : spots) set(ax(i, s[0]), oy + 7, az(j, s[1]), m);
    }

    private void pillar(int i, int j, int lx, int lz, Material m, Material cap) {
        for (int y = 1; y <= 6; y++) set(ax(i, lx), oy + y, az(j, lz), y == 6 || y == 1 ? cap : m);
    }

    private void buildRoom(int i, int j) {
        Room r = L.room[i][j];
        int idx = i * H + j;
        switch (r) {
            case HUB -> { }
            case TREASURE -> {
                floor(i, j, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE);
                lights(i, j, Material.SHROOMLIGHT);
                pillar(i, j, 3, 3, Material.CUT_SANDSTONE, Material.CHISELED_SANDSTONE);
                pillar(i, j, 3, 9, Material.CUT_SANDSTONE, Material.CHISELED_SANDSTONE);
                pillar(i, j, 9, 3, Material.CUT_SANDSTONE, Material.CHISELED_SANDSTONE);
                pillar(i, j, 9, 9, Material.CUT_SANDSTONE, Material.CHISELED_SANDSTONE);
                set(ax(i, 2), oy + 1, az(j, 2), Material.POTTED_DEAD_BUSH);
                set(ax(i, 10), oy + 1, az(j, 10), Material.POTTED_DEAD_BUSH);
            }
            case SAND -> {
                floor(i, j, Material.SMOOTH_SANDSTONE, Material.SANDSTONE);
                lights(i, j, Material.SHROOMLIGHT);
                int[][] piles = {{1, 1}, {1, 2}, {2, 1}, {11, 11}, {10, 11}, {11, 10}};
                for (int[] p : piles) set(ax(i, p[0]), oy + 1, az(j, p[1]), Material.SAND);
                set(ax(i, 1), oy + 2, az(j, 1), Material.DEAD_BUSH);
                set(ax(i, 11), oy + 2, az(j, 11), Material.DEAD_BUSH);
            }
            case MOB, MOB_KEY -> {
                floor(i, j, Material.CUT_RED_SANDSTONE, Material.SMOOTH_RED_SANDSTONE);
                lights(i, j, Material.SHROOMLIGHT);
                pillar(i, j, 2, 2, Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE);
                pillar(i, j, 2, 10, Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE);
                pillar(i, j, 10, 2, Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE);
                pillar(i, j, 10, 10, Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE);
                set(ax(i, 1), oy + 6, az(j, 1), Material.COBWEB);
                set(ax(i, 11), oy + 6, az(j, 11), Material.COBWEB);
                if (r == Room.MOB_KEY) {
                    set(ax(i, 6), oy + 1, az(j, 6), Material.CHISELED_RED_SANDSTONE);
                    for (int lx = 5; lx <= 7; lx++)
                        for (int lz = 5; lz <= 7; lz++) {
                            if (lx == 6 && lz == 6) continue;
                            for (int y = 1; y <= 3; y++) {
                                set(ax(i, lx), oy + y, az(j, lz), Material.IRON_BARS);
                                mobKeyBars.add(new int[]{ax(i, lx), oy + y, az(j, lz)});
                            }
                        }
                    for (int lx = 5; lx <= 7; lx++)
                        for (int lz = 5; lz <= 7; lz++) set(ax(i, lx), oy + 4, az(j, lz), Material.CUT_RED_SANDSTONE);
                }
            }
            case TRAP_ARROW -> {
                floor(i, j, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE);
                lights(i, j, Material.SHROOMLIGHT);
                for (int[] p : L.plates) {
                    if (p[0] != i || p[1] != j) continue;
                    int x = ax(i, p[2]), z = az(j, p[3]);
                    set(x, oy, z, Material.CHISELED_SANDSTONE);
                    set(x, oy + 1, z, Material.STONE_PRESSURE_PLATE);
                    plateRoom.put(key(x, oy + 1, z), idx);
                }
                List<Location> disp = new ArrayList<>();
                for (int lat : new int[]{3, 9}) {
                    disp.add(dispenser(ax(i, 0), az(j, lat), BlockFace.EAST));
                    disp.add(dispenser(ax(i, CELL), az(j, lat), BlockFace.WEST));
                    disp.add(dispenser(ax(i, lat), az(j, 0), BlockFace.SOUTH));
                    disp.add(dispenser(ax(i, lat), az(j, CELL), BlockFace.NORTH));
                }
                dispensers.put(idx, disp);
            }
            case TRAP_PIT -> {
                floor(i, j, Material.CUT_SANDSTONE, Material.CUT_SANDSTONE);
                lights(i, j, Material.SHROOMLIGHT);
                for (int lx = 2; lx <= 10; lx++)
                    for (int lz = 2; lz <= 10; lz++) {
                        int x = ax(i, lx), z = az(j, lz);
                        boolean wall = lx == 2 || lz == 2 || lx == 10 || lz == 10;
                        if (wall) {
                            set(x, oy - 1, z, Material.SANDSTONE);
                            set(x, oy - 2, z, Material.SANDSTONE);
                            set(x, oy - 3, z, Material.SANDSTONE);
                            if (lx == 2 || lx == 10) { if (lz % 2 == 0) set(x, oy, z, Material.CHISELED_SANDSTONE); }
                        } else {
                            set(x, oy, z, Material.SMOOTH_SANDSTONE);
                            crumble.add(key(x, oy, z));
                            set(x, oy - 1, z, Material.AIR);
                            set(x, oy - 2, z, Material.LAVA);
                            set(x, oy - 3, z, Material.SANDSTONE);
                        }
                    }
            }
            case PARKOUR_KEY -> {
                floor(i, j, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE);
                lights(i, j, Material.GLOWSTONE);
                int d = L.firstDoor(i, j);
                for (int u = 1; u <= 11; u++)
                    for (int v = 3; v <= 9; v++) {
                        int[] l = Layout.uvBlock(d, u, v);
                        set(ax(i, l[0]), oy - 1, az(j, l[1]), Material.SANDSTONE);
                        set(ax(i, l[0]), oy, az(j, l[1]), Material.LAVA);
                    }
                int[][] stones = {{6, 4}, {4, 6}, {5, 6}, {7, 8}, {6, 8}};
                for (int[] s : stones) {
                    int[] l = Layout.uvBlock(d, s[0], s[1]);
                    set(ax(i, l[0]), oy + 1, az(j, l[1]), Material.CHISELED_SANDSTONE);
                }
                int[] ped = Layout.uvBlock(d, 6, 11);
                set(ax(i, ped[0]), oy + 1, az(j, ped[1]), Material.GOLD_BLOCK);
                int[] a = Layout.uvBlock(d, 5, 11), b = Layout.uvBlock(d, 7, 11);
                set(ax(i, a[0]), oy + 1, az(j, a[1]), Material.YELLOW_GLAZED_TERRACOTTA);
                set(ax(i, b[0]), oy + 1, az(j, b[1]), Material.YELLOW_GLAZED_TERRACOTTA);
            }
            case VAULT -> {
                KeyColor c = L.vault[i][j];
                floor(i, j, c.accent, Material.SMOOTH_SANDSTONE);
                for (int lx = 5; lx <= 7; lx++) for (int lz = 5; lz <= 7; lz++) set(ax(i, lx), oy, az(j, lz), Material.GOLD_BLOCK);
                lights(i, j, Material.SHROOMLIGHT);
                // Coloured band on every wall of the vault cell: visible from neighbouring rooms too.
                for (int l = 0; l <= CELL; l++) {
                    set(ax(i, l), oy + 5, az(j, 0), c.accent);
                    set(ax(i, l), oy + 5, az(j, CELL), c.accent);
                    set(ax(i, 0), oy + 5, az(j, l), c.accent);
                    set(ax(i, CELL), oy + 5, az(j, l), c.accent);
                }
            }
        }
    }

    private Location dispenser(int x, int z, BlockFace facing) {
        BlockData bd = Material.DISPENSER.createBlockData();
        if (bd instanceof Directional dir) dir.setFacing(facing);
        setData(x, oy + 2, z, bd);
        return new Location(w, x + 0.5 + facing.getModX() * 0.8, oy + 2.4, z + 0.5 + facing.getModZ() * 0.8);
    }

    private void buildHub() {
        int hx = hx(), hz = hz();
        for (int lx = 1; lx < CELL; lx++)
            for (int lz = 1; lz < CELL; lz++) {
                boolean ring = (lx == 2 || lx == 10 || lz == 2 || lz == 10) && lx >= 2 && lx <= 10 && lz >= 2 && lz <= 10;
                set(hx + lx, oy, hz + lz, ring ? Material.CHISELED_SANDSTONE : Material.SMOOTH_SANDSTONE);
            }
        int[][] hubLights = {{2, 2}, {10, 2}, {2, 10}, {10, 10}, {6, 2}, {6, 10}, {2, 6}, {10, 6}};
        for (int[] s : hubLights) set(hx + s[0], oy + 7, hz + s[1], Material.SHROOMLIGHT);

        // Hourglass: 5x5 footprint at local 4..8.
        for (int lx = 4; lx <= 8; lx++)
            for (int lz = 4; lz <= 8; lz++) {
                boolean edge = lx == 4 || lx == 8 || lz == 4 || lz == 8;
                boolean corner = (lx == 4 || lx == 8) && (lz == 4 || lz == 8);
                int x = hx + lx, z = hz + lz;
                set(x, oy + 1, z, corner ? Material.CHISELED_SANDSTONE : Material.SMOOTH_SANDSTONE);
                set(x, oy + 4, z, (lx == 6 && lz == 6) ? Material.GLASS : (corner ? Material.CHISELED_SANDSTONE : Material.CUT_SANDSTONE));
                for (int y : new int[]{2, 3, 5, 6}) {
                    if (corner) set(x, oy + y, z, Material.CUT_SANDSTONE);
                    else if (edge) set(x, oy + y, z, Material.GLASS);
                    else set(x, oy + y, z, Material.AIR);
                }
                for (int y = 1; y <= 6; y++) hourglass.add(key(x, oy + y, z));
            }
        for (int y : new int[]{2, 3})
            for (int lx = 5; lx <= 7; lx++) for (int lz = 5; lz <= 7; lz++) bottomSlots.add(new int[]{hx + lx, oy + y, hz + lz});
        for (int y : new int[]{5, 6})
            for (int lx = 5; lx <= 7; lx++) for (int lz = 5; lz <= 7; lz++) topSlots.add(new int[]{hx + lx, oy + y, hz + lz});

        // Sphinx (bank) in the deep-east corner.
        set(hx + 10, oy + 1, hz + 10, Material.SANDSTONE);
        set(hx + 10, oy + 2, hz + 10, Material.CHISELED_SANDSTONE);
        set(hx + 10, oy + 3, hz + 10, Material.SANDSTONE_SLAB);
        set(hx + 9, oy + 1, hz + 10, Material.SANDSTONE_SLAB);
        set(hx + 10, oy + 1, hz + 9, Material.SANDSTONE_SLAB);
        set(hx + 9, oy + 1, hz + 9, Material.GOLD_BLOCK);
        for (int[] b : new int[][]{{10, 1, 10}, {10, 2, 10}, {10, 3, 10}, {9, 1, 10}, {10, 1, 9}, {9, 1, 9}})
            sphinx.add(key(hx + b[0], oy + b[1], hz + b[2]));

        // Revive shrine in the deep-west corner.
        set(hx + 2, oy + 1, hz + 10, Material.CHISELED_RED_SANDSTONE);
        set(hx + 2, oy + 2, hz + 10, Material.SOUL_LANTERN);
        set(hx + 3, oy + 1, hz + 10, Material.RED_SANDSTONE_SLAB);
        set(hx + 2, oy + 1, hz + 9, Material.RED_SANDSTONE_SLAB);
        for (int[] b : new int[][]{{2, 1, 10}, {2, 2, 10}, {3, 1, 10}, {2, 1, 9}})
            shrine.add(key(hx + b[0], oy + b[1], hz + b[2]));

        // Doorways out of the hub (barred until the game starts).
        for (int d = 0; d < 4; d++) {
            if (!L.hasDoor(HUB_I, HUB_J, d)) continue;
            int vi = HUB_I + Layout.DX[d], vj = HUB_J + Layout.DZ[d];
            if (Layout.inGrid(vi, vj) && L.vault[vi][vj] != null) continue; // locked vault door stays
            int i = d == 2 ? HUB_I - 1 : HUB_I, j = d == 3 ? HUB_J - 1 : HUB_J;
            hubOpenings.addAll(opening(i, j, d % 2 == 0 ? 0 : 1, 5, 7, 1, 3));
        }
    }

    /** Exit alcove and the downed-player cage, built just outside the hub's outer wall. */
    private void buildOutside() {
        int hx = hx(), hz = hz();
        for (int lx = 0; lx <= 8; lx++)
            for (int z = hz - 4; z <= hz - 1; z++)
                for (int y = 0; y <= 4; y++) set(hx + lx, oy + y, z, Material.SANDSTONE);
        // Cage interior + glass front.
        for (int lx = 1; lx <= 3; lx++) {
            for (int z = hz - 3; z <= hz - 1; z++) for (int y = 1; y <= 3; y++) set(hx + lx, oy + y, z, Material.AIR);
            for (int y = 1; y <= 3; y++) set(hx + lx, oy + y, hz, Material.GLASS);
        }
        set(hx + 2, oy + 4, hz - 2, Material.SHROOMLIGHT);
        // Exit alcove.
        for (int lx = 5; lx <= 7; lx++) {
            for (int z = hz - 3; z <= hz - 1; z++) for (int y = 1; y <= 3; y++) set(hx + lx, oy + y, z, Material.AIR);
            for (int y = 1; y <= 3; y++) set(hx + lx, oy + y, hz, Material.AIR);
            set(hx + lx, oy, hz - 3, Material.CRYING_OBSIDIAN);
            exitPad.add(key(hx + lx, oy, hz - 3));
            hubOpenings.add(new int[]{hx + lx, oy + 1, hz});
            hubOpenings.add(new int[]{hx + lx, oy + 2, hz});
            hubOpenings.add(new int[]{hx + lx, oy + 3, hz});
        }
        for (int y = 1; y <= 4; y++) {
            set(hx + 4, oy + y, hz, Material.CHISELED_SANDSTONE);
            set(hx + 8, oy + y, hz, Material.CHISELED_SANDSTONE);
        }
        for (int lx = 4; lx <= 8; lx++) set(hx + lx, oy + 4, hz, Material.CHISELED_SANDSTONE);
        set(hx + 6, oy + 4, hz - 2, Material.SHROOMLIGHT);
    }

    void setHubBarriers(boolean closed) {
        for (int[] b : hubOpenings) set(b[0], b[1], b[2], closed ? Material.BARRIER : Material.AIR);
    }

    // ------------------------------------------------------------------ entities & runtime

    void spawnEntities(SandsOfTimeGame game) {
        int hx = hx(), hz = hz();
        game.spawnText(new Location(w, hx + 6.5, oy + 3.2, hz + 3.3), "§e§lHourglass\n§7Right-click with §eSand §7(+10s)");
        game.spawnText(new Location(w, hx + 10.5, oy + 4.3, hz + 10.5), "§6§lSphinx\n§7Right-click to bank coins\n§c20% fee");
        game.spawnText(new Location(w, hx + 2.5, oy + 3.5, hz + 10.5), "§d§lRevive Shrine\n§7Offer 1 §eSand §7to free\n§7a fallen teammate");
        game.spawnText(new Location(w, hx + 6.5, oy + 4.6, hz + 1.0), "§a§lEXIT\n§7Leave with all your coins\n§7(you can't come back)");
        game.spawnText(new Location(w, hx + 2.5, oy + 4.6, hz + 1.0), "§cFallen");
        for (ItemSpec s : L.items) {
            Location loc = new Location(w, ox + s.ci() * CELL + s.lx(), oy + s.ly(), oz + s.cj() * CELL + s.lz());
            ItemStack st = switch (s.kind()) {
                case COIN -> Items.coin(s.mat(), s.unitValue(), s.amount());
                case SAND -> Items.sand(s.amount());
                case KEY -> Items.key(s.key());
                case GEAR -> Items.gear(s.mat());
            };
            if (s.kind() == Layout.Kind.KEY) keySpawn.put(s.key(), loc.clone());
            game.spawnItem(loc, st);
        }
    }

    /** Update the physical hourglass: top bulb holds the remaining fraction of MAX time. */
    void updateHourglass(int maxTime) {
        int slots = topSlots.size();
        int top = (int) Math.round(slots * Math.max(0, Math.min(1.0, time / (double) maxTime)));
        if (top == lastTop) return;
        lastTop = top;
        for (int n = 0; n < slots; n++) {
            int[] t = topSlots.get(n);
            set(t[0], t[1], t[2], n < top ? Material.SAND : Material.AIR);
            int[] b = bottomSlots.get(n);
            set(b[0], b[1], b[2], n < slots - top ? Material.SAND : Material.AIR);
        }
    }

    void openDoor(KeyColor c) {
        List<int[]> blocks = doorsByColor.remove(c);
        if (blocks == null) return;
        for (int[] b : blocks) {
            set(b[0], b[1], b[2], Material.AIR);
            doorBlocks.remove(key(b[0], b[1], b[2]));
        }
        opened.add(c);
    }

    void removeMobKeyBars() {
        for (int[] b : mobKeyBars) set(b[0], b[1], b[2], Material.AIR);
    }

    Location mobKeyPedestal() {
        return new Location(w, ox + L.mobKeyI * CELL + 6.5, oy + 2.2, oz + L.mobKeyJ * CELL + 6.5);
    }

    Location roomPoint(int i, int j, double lx, double ly, double lz) {
        return new Location(w, ox + i * CELL + lx, oy + ly, oz + j * CELL + lz);
    }

    void setFloor(long k, Material m, boolean physics) {
        int x = (int) (k >> 38), z = (int) ((k << 26) >> 38), y = (int) (k & 0xFFF);
        w.getBlockAt(x, y, z).setType(m, physics);
    }
}
