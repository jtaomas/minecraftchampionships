package com.mcm.games.railroadrush;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;

import java.util.ArrayList;
import java.util.List;

/** Follows vanilla rail shapes block by block. */
final class Track {
    private static final int[][] NONE = new int[0][];

    private Track() {}

    static Material type(World w, Pos p) { return w.getBlockAt(p.x(), p.y(), p.z()).getType(); }

    static boolean isRail(World w, Pos p) {
        Material m = type(w, p);
        return m == Material.RAIL || m == Material.POWERED_RAIL;
    }

    static int[][] conns(World w, Pos p) {
        BlockData d = w.getBlockAt(p.x(), p.y(), p.z()).getBlockData();
        if (!(d instanceof Rail r)) return NONE;
        return switch (r.getShape()) {
            case NORTH_SOUTH -> new int[][]{{0, 0, -1}, {0, 0, 1}};
            case EAST_WEST -> new int[][]{{1, 0, 0}, {-1, 0, 0}};
            case ASCENDING_EAST -> new int[][]{{1, 1, 0}, {-1, 0, 0}};
            case ASCENDING_WEST -> new int[][]{{-1, 1, 0}, {1, 0, 0}};
            case ASCENDING_NORTH -> new int[][]{{0, 1, -1}, {0, 0, 1}};
            case ASCENDING_SOUTH -> new int[][]{{0, 1, 1}, {0, 0, -1}};
            case SOUTH_EAST -> new int[][]{{0, 0, 1}, {1, 0, 0}};
            case SOUTH_WEST -> new int[][]{{0, 0, 1}, {-1, 0, 0}};
            case NORTH_WEST -> new int[][]{{0, 0, -1}, {-1, 0, 0}};
            case NORTH_EAST -> new int[][]{{0, 0, -1}, {1, 0, 0}};
            default -> NONE;
        };
    }

    private static Pos resolve(World w, Pos p, int[] c) {
        Pos t = p.add(c[0], c[1], c[2]);
        if (isRail(w, t)) return t;
        if (c[1] == 0) {
            Pos d = t.down();
            if (isRail(w, d)) return d;
        }
        return null;
    }

    private static boolean linksTo(World w, Pos q, Pos p) {
        for (int[] c : conns(w, q)) {
            Pos t = q.add(c[0], c[1], c[2]);
            if (t.equals(p)) return true;
            if (c[1] == 0 && t.down().equals(p)) return true;
        }
        return false;
    }

    static List<Pos> neighbours(World w, Pos p) {
        List<Pos> out = new ArrayList<>(2);
        for (int[] c : conns(w, p)) {
            Pos n = resolve(w, p, c);
            if (n != null && linksTo(w, n, p)) out.add(n);
        }
        return out;
    }

    /** Next rail after cur when arriving from prev (null = start: prefer the forward/south neighbour). */
    static Pos next(World w, Pos prev, Pos cur) {
        Pos best = null;
        for (Pos n : neighbours(w, cur)) {
            if (n.equals(prev)) continue;
            if (best == null || n.z() > best.z()) best = n;
        }
        return best;
    }
}
