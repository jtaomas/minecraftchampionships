package com.mcm.games.sandsoftime;

import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A procedurally generated dungeon plan: a W x H grid of rooms joined by doorways (a randomized
 * Prim maze plus a few loops), with room types and loot positions. The same plan is built once per
 * side so every team raids an identical dungeon.
 *
 * Cell (i, j) spans local coords 0..CELL on both axes; walls are shared and sit at 0 and CELL,
 * so the interior is 1..CELL-1. j grows "deeper" (+Z) away from the hub.
 */
final class Layout {
    static final int W = 5, H = 5, CELL = 12;
    static final int HUB_I = 2, HUB_J = 0;
    /** Directions: 0 = +X (east), 1 = +Z (deeper), 2 = -X, 3 = -Z (towards the exit). */
    static final int[] DX = {1, 0, -1, 0};
    static final int[] DZ = {0, 1, 0, -1};

    enum Room { HUB, TREASURE, SAND, MOB, MOB_KEY, TRAP_ARROW, TRAP_PIT, PARKOUR_KEY, VAULT }

    enum Kind { COIN, SAND, KEY, GEAR }

    /** A loot item to spawn. lx/lz are local to the cell, ly is height above the floor block. */
    record ItemSpec(int ci, int cj, double lx, double ly, double lz, Kind kind, Material mat,
                    int unitValue, int amount, KeyColor key) {}

    final Room[][] room = new Room[W][H];
    final int[][] depth = new int[W][H];
    final boolean[][] doorE = new boolean[W][H];
    final boolean[][] doorN = new boolean[W][H];
    final KeyColor[][] vault = new KeyColor[W][H];
    final List<ItemSpec> items = new ArrayList<>();
    /** Pressure plates: {ci, cj, lx, lz}. */
    final List<int[]> plates = new ArrayList<>();
    final Set<KeyColor> vaults = EnumSet.noneOf(KeyColor.class);
    int mobKeyI = -1, mobKeyJ = -1;
    private final Random rnd;

    Layout(Random rnd) {
        this.rnd = rnd;
        generate();
    }

    // ------------------------------------------------------------------ doors

    static boolean inGrid(int i, int j) { return i >= 0 && j >= 0 && i < W && j < H; }

    boolean hasDoor(int i, int j, int d) {
        return switch (d) {
            case 0 -> i < W - 1 && doorE[i][j];
            case 2 -> i > 0 && doorE[i - 1][j];
            case 1 -> j < H - 1 && doorN[i][j];
            default -> j > 0 && doorN[i][j - 1];
        };
    }

    private void setDoor(int i, int j, int d) {
        switch (d) {
            case 0 -> doorE[i][j] = true;
            case 2 -> doorE[i - 1][j] = true;
            case 1 -> doorN[i][j] = true;
            default -> doorN[i][j - 1] = true;
        }
    }

    int degree(int i, int j) {
        int n = 0;
        for (int d = 0; d < 4; d++) if (hasDoor(i, j, d)) n++;
        return n;
    }

    /** The only door direction of a dead-end cell (or the first door found). */
    int firstDoor(int i, int j) {
        for (int d = 0; d < 4; d++) if (hasDoor(i, j, d)) return d;
        return 3;
    }

    // ------------------------------------------------------------------ generation

    private void generate() {
        List<int[]> deadEnds = new ArrayList<>();
        for (int attempt = 0; attempt < 200; attempt++) {
            for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) { doorE[i][j] = false; doorN[i][j] = false; }
            prim();
            deadEnds.clear();
            for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) {
                if (i == HUB_I && j == HUB_J) continue;
                if (degree(i, j) == 1) deadEnds.add(new int[]{i, j});
            }
            if (deadEnds.size() >= 4) break;
        }
        computeDepth();
        Collections.shuffle(deadEnds, rnd);
        deadEnds.sort(Comparator.comparingInt((int[] c) -> -depth[c[0]][c[1]]));

        room[HUB_I][HUB_J] = Room.HUB;
        int idx = 0;
        int[] parkour = null;
        if (deadEnds.size() > idx) setVault(deadEnds.get(idx++), KeyColor.GOLD);
        if (deadEnds.size() > idx) setVault(deadEnds.get(idx++), KeyColor.RED);
        if (deadEnds.size() > idx) { parkour = deadEnds.get(idx++); room[parkour[0]][parkour[1]] = Room.PARKOUR_KEY; }
        if (deadEnds.size() > idx) setVault(deadEnds.get(idx++), KeyColor.GREEN);

        // A few extra doors so the dungeon has loops (never into vaults / the parkour room).
        int added = 0;
        for (int tries = 0; tries < 100 && added < 4; tries++) {
            int i = rnd.nextInt(W), j = rnd.nextInt(H), d = rnd.nextInt(4);
            int ni = i + DX[d], nj = j + DZ[d];
            if (!inGrid(ni, nj) || hasDoor(i, j, d)) continue;
            if (isSpecial(i, j) || isSpecial(ni, nj)) continue;
            setDoor(i, j, d);
            added++;
        }
        computeDepth();

        // Remaining rooms.
        List<int[]> free = new ArrayList<>();
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) if (room[i][j] == null) free.add(new int[]{i, j});
        Collections.shuffle(free, rnd);
        for (int[] c : free) {
            if (depth[c[0]][c[1]] >= 2) { room[c[0]][c[1]] = Room.MOB_KEY; mobKeyI = c[0]; mobKeyJ = c[1]; break; }
        }
        if (mobKeyI < 0 && !free.isEmpty()) {
            int[] c = free.get(free.size() - 1);
            room[c[0]][c[1]] = Room.MOB_KEY; mobKeyI = c[0]; mobKeyJ = c[1];
        }
        List<Room> bag = new ArrayList<>();
        for (int n = 0; n < 4; n++) bag.add(Room.MOB);
        for (int n = 0; n < 3; n++) bag.add(Room.TRAP_ARROW);
        for (int n = 0; n < 3; n++) bag.add(Room.TRAP_PIT);
        for (int n = 0; n < 2; n++) bag.add(Room.SAND);
        while (bag.size() < free.size()) bag.add(Room.TREASURE);
        Collections.shuffle(bag, rnd);
        int b = 0;
        for (int[] c : free) {
            if (room[c[0]][c[1]] != null) continue;
            room[c[0]][c[1]] = depth[c[0]][c[1]] <= 1 ? Room.TREASURE : bag.get(b++ % bag.size());
        }

        // Loot.
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) fillRoom(i, j);

        // Keys that are not tied to a special room.
        if (vaults.contains(KeyColor.GREEN)) placeLooseKey(KeyColor.GREEN);
        if (vaults.contains(KeyColor.GOLD) && parkour == null) placeLooseKey(KeyColor.GOLD);
        if (vaults.contains(KeyColor.RED) && mobKeyI < 0) placeLooseKey(KeyColor.RED);
    }

    private boolean isSpecial(int i, int j) {
        return room[i][j] == Room.VAULT || room[i][j] == Room.PARKOUR_KEY;
    }

    private void setVault(int[] c, KeyColor color) {
        room[c[0]][c[1]] = Room.VAULT;
        vault[c[0]][c[1]] = color;
        vaults.add(color);
    }

    private void prim() {
        boolean[][] in = new boolean[W][H];
        List<int[]> frontier = new ArrayList<>();
        in[HUB_I][HUB_J] = true;
        addFrontier(frontier, HUB_I, HUB_J);
        while (!frontier.isEmpty()) {
            int[] e = frontier.remove(rnd.nextInt(frontier.size()));
            int ni = e[0] + DX[e[2]], nj = e[1] + DZ[e[2]];
            if (in[ni][nj]) continue;
            setDoor(e[0], e[1], e[2]);
            in[ni][nj] = true;
            addFrontier(frontier, ni, nj);
        }
    }

    private void addFrontier(List<int[]> frontier, int i, int j) {
        for (int d = 0; d < 4; d++) {
            int ni = i + DX[d], nj = j + DZ[d];
            if (inGrid(ni, nj)) frontier.add(new int[]{i, j, d});
        }
    }

    private void computeDepth() {
        for (int[] row : depth) java.util.Arrays.fill(row, -1);
        Deque<int[]> q = new ArrayDeque<>();
        depth[HUB_I][HUB_J] = 0;
        q.add(new int[]{HUB_I, HUB_J});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int d = 0; d < 4; d++) {
                if (!hasDoor(c[0], c[1], d)) continue;
                int ni = c[0] + DX[d], nj = c[1] + DZ[d];
                if (depth[ni][nj] >= 0) continue;
                depth[ni][nj] = depth[c[0]][c[1]] + 1;
                q.add(new int[]{ni, nj});
            }
        }
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) if (depth[i][j] < 0) depth[i][j] = 1;
    }

    // ------------------------------------------------------------------ loot

    static double mult(int depth) { return 1.0 + 0.25 * depth; }

    private void coin(int i, int j, double[] p, Material m, int base, int amount) {
        int unit = Math.max(1, (int) Math.round(base * mult(depth[i][j])));
        items.add(new ItemSpec(i, j, p[0], 1.1, p[1], Kind.COIN, m, unit, amount, null));
    }

    private void sand(int i, int j, double[] p, int amount) {
        items.add(new ItemSpec(i, j, p[0], 1.1, p[1], Kind.SAND, Material.SAND, 0, amount, null));
    }

    private void nuggets(int i, int j, int count) {
        for (int n = 0; n < count; n++) coin(i, j, pos(i, j), Material.GOLD_NUGGET, 1, 1 + rnd.nextInt(2));
    }

    private void ingots(int i, int j, int count) {
        for (int n = 0; n < count; n++) coin(i, j, pos(i, j), Material.GOLD_INGOT, 5, 1);
    }

    private static final Material[] GEAR = {
            Material.IRON_SWORD, Material.IRON_HELMET, Material.CHAINMAIL_CHESTPLATE,
            Material.IRON_BOOTS, Material.GOLDEN_APPLE, Material.SHIELD
    };

    private void fillRoom(int i, int j) {
        Room r = room[i][j];
        int dep = depth[i][j];
        switch (r) {
            case HUB -> { }
            case TREASURE -> {
                nuggets(i, j, 6 + rnd.nextInt(4));
                ingots(i, j, 1 + rnd.nextInt(2));
                if (rnd.nextDouble() < 0.7) sand(i, j, pos(i, j), 1);
                if (dep >= 2 && rnd.nextDouble() < 0.35) {
                    double[] p = pos(i, j);
                    items.add(new ItemSpec(i, j, p[0], 1.1, p[1], Kind.GEAR, GEAR[rnd.nextInt(GEAR.length)], 0, 1, null));
                }
            }
            case SAND -> {
                for (int n = 0; n < 4 + rnd.nextInt(2); n++) sand(i, j, pos(i, j), 1);
                nuggets(i, j, 3);
            }
            case MOB -> {
                nuggets(i, j, 5);
                ingots(i, j, 2);
                sand(i, j, pos(i, j), 1);
                if (rnd.nextDouble() < 0.4) {
                    double[] p = pos(i, j);
                    items.add(new ItemSpec(i, j, p[0], 1.1, p[1], Kind.GEAR, GEAR[rnd.nextInt(GEAR.length)], 0, 1, null));
                }
            }
            case MOB_KEY -> {
                nuggets(i, j, 3);
                ingots(i, j, 2);
                sand(i, j, pos(i, j), 1);
            }
            case TRAP_ARROW -> {
                for (int n = 0; n < 8; n++) {
                    int lx = 2 + rnd.nextInt(9), lz = 2 + rnd.nextInt(9);
                    plates.add(new int[]{i, j, lx, lz});
                }
                nuggets(i, j, 6);
                ingots(i, j, 2);
                sand(i, j, pos(i, j), 1);
                sand(i, j, pos(i, j), 1);
            }
            case TRAP_PIT -> {
                // Treasure sits on the crumbling middle (3..9); the 2-wide ring around it is safe.
                for (int n = 0; n < 3; n++) coin(i, j, pitPos(), Material.GOLD_INGOT, 5, 1);
                for (int n = 0; n < 5; n++) coin(i, j, pitPos(), Material.GOLD_NUGGET, 1, 1 + rnd.nextInt(2));
                sand(i, j, pitPos(), 1);
                sand(i, j, new double[]{1.5 + rnd.nextInt(2) * 9, 1.5 + rnd.nextInt(11)}, 1);
            }
            case PARKOUR_KEY -> {
                int d = firstDoor(i, j);
                double[] key = uv(d, 6, 11);
                items.add(new ItemSpec(i, j, key[0], 2.2, key[1], Kind.KEY, Material.TRIPWIRE_HOOK, 0, 1, KeyColor.GOLD));
                coin(i, j, uv(d, 3, 10), Material.GOLD_INGOT, 5, 2);
                coin(i, j, uv(d, 9, 10), Material.GOLD_INGOT, 5, 2);
                double[] s = uv(d, 5, 6);
                items.add(new ItemSpec(i, j, s[0], 2.1, s[1], Kind.SAND, Material.SAND, 0, 1, null));
            }
            case VAULT -> {
                KeyColor c = vault[i][j];
                int blocks = c == KeyColor.GOLD ? 5 : 3;
                for (int n = 0; n < blocks; n++) coin(i, j, pos(i, j), Material.GOLD_BLOCK, 25, 1);
                for (int n = 0; n < 5; n++) coin(i, j, pos(i, j), Material.GOLD_INGOT, 5, 1);
                sand(i, j, pos(i, j), 2);
            }
        }
    }

    private void placeLooseKey(KeyColor c) {
        List<int[]> cands = new ArrayList<>();
        for (int i = 0; i < W; i++) for (int j = 0; j < H; j++) {
            if ((room[i][j] == Room.TREASURE || room[i][j] == Room.SAND || room[i][j] == Room.MOB) && depth[i][j] >= 2)
                cands.add(new int[]{i, j});
        }
        if (cands.isEmpty()) {
            for (int i = 0; i < W; i++) for (int j = 0; j < H; j++)
                if (room[i][j] != Room.HUB && room[i][j] != Room.VAULT) cands.add(new int[]{i, j});
        }
        if (cands.isEmpty()) return;
        int[] c0 = cands.get(rnd.nextInt(cands.size()));
        double[] p = room[c0[0]][c0[1]] == Room.TRAP_PIT ? new double[]{1.5, 6.5} : pos(c0[0], c0[1]);
        items.add(new ItemSpec(c0[0], c0[1], p[0], 1.1, p[1], Kind.KEY, Material.TRIPWIRE_HOOK, 0, 1, c));
    }

    /** Random free spot in a room (centre of a block). */
    private double[] pos(int i, int j) {
        Room r = room[i][j];
        for (int tries = 0; tries < 40; tries++) {
            int lx = 2 + rnd.nextInt(9), lz = 2 + rnd.nextInt(9);
            if (isFree(r, lx, lz)) return new double[]{lx + 0.5, lz + 0.5};
        }
        return new double[]{6.5, 2.5};
    }

    private double[] pitPos() {
        return new double[]{3.5 + rnd.nextInt(7), 3.5 + rnd.nextInt(7)};
    }

    static boolean isFree(Room r, int lx, int lz) {
        return switch (r) {
            case TREASURE -> !((lx == 3 || lx == 9) && (lz == 3 || lz == 9)) && !(lx == 2 && lz == 2) && !(lx == 10 && lz == 10);
            case MOB -> !((lx == 2 || lx == 10) && (lz == 2 || lz == 10));
            case MOB_KEY -> !((lx == 2 || lx == 10) && (lz == 2 || lz == 10)) && !(lx >= 5 && lx <= 7 && lz >= 5 && lz <= 7);
            case SAND -> !(lx <= 2 && lz <= 2) && !(lx >= 10 && lz >= 10);
            default -> true;
        };
    }

    /**
     * Converts room-relative (u = sideways 1..11, v = distance from the door wall 1..11) into
     * local (lx, lz) for a room whose single door faces direction d. Returns block-centre coords.
     */
    static double[] uv(int d, int u, int v) {
        int[] l = uvBlock(d, u, v);
        return new double[]{l[0] + 0.5, l[1] + 0.5};
    }

    static int[] uvBlock(int d, int u, int v) {
        return switch (d) {
            case 3 -> new int[]{u, v};
            case 1 -> new int[]{CELL - u, CELL - v};
            case 2 -> new int[]{v, u};
            default -> new int[]{CELL - v, u};
        };
    }
}
