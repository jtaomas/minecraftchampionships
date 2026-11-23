package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The world being generated: the model's block store plus generator bookkeeping (registered obstacles, frame
 * walkways, staircases, timed-block preview states). Everything in world coordinates.
 */
final class W {
    /** Material used ONLY for checkpoint pads (validated). */
    static final String PAD = "minecraft:pearlescent_froglight";

    final CourseModel model;
    final CourseModel.Blocks b;
    final Random rnd;
    final List<Ob> obs = new ArrayList<>();
    /** Cells (standing blocks) of hubs, start, fork, shrines: legit walkable, not decoys. */
    final Set<Long> frameWalk = new HashSet<>();
    final List<Stairs.Lane> lanes = new ArrayList<>();
    /** World cells of every pad block placed. */
    final Set<Long> padCells = new HashSet<>();

    W(long seed) {
        model = new CourseModel(seed);
        b = model.blocks;
        rnd = new Random(seed);
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0xFFFFF) << 40) | ((long) (z & 0xFFFFF) << 20) | (y & 0xFFFFF);
    }

    static int kx(long k) { return (int) (k >> 40) << 12 >> 12; }
    static int kz(long k) { return (int) ((k >> 20) & 0xFFFFF) << 12 >> 12; }
    static int ky(long k) { return (int) (k & 0xFFFFF) << 12 >> 12; }

    void set(int x, int y, int z, String s) {
        if (s != null && Bs.id(s).equals("pearlescent_froglight")) padCells.add(key(x, y, z));
        else padCells.remove(key(x, y, z));
        b.set(x, y, z, s == null ? null : Bs.full(s));
    }

    String get(int x, int y, int z) { return b.get(x, y, z); }

    boolean air(int x, int y, int z) { return Bs.isAir(b.get(x, y, z)); }

    /** Set only if currently air. */
    void soft(int x, int y, int z, String s) { if (air(x, y, z)) set(x, y, z, s); }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    void walk(int x, int y, int z) { frameWalk.add(key(x, y, z)); }

    void walkRect(int x0, int y, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) walk(x, y, z);
    }

    /** Pick from a weighted palette: pairs of (state, weight). */
    String pick(Object... pw) {
        int tot = 0;
        for (int i = 1; i < pw.length; i += 2) tot += (Integer) pw[i];
        int r = rnd.nextInt(tot);
        for (int i = 0; i < pw.length; i += 2) {
            r -= (Integer) pw[i + 1];
            if (r < 0) return (String) pw[i];
        }
        return (String) pw[0];
    }

    /** Deterministic hash noise in [0,1). */
    static double h(int x, int y, int z, int salt) {
        long v = x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ salt * 2654435761L;
        v ^= (v >>> 13); v *= 0x5bd1e995L; v ^= (v >>> 15);
        return ((v & 0xFFFFFF) / (double) 0x1000000);
    }
}
