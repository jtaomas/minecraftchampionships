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

    /** Overlap tracking: who wrote each cell (current owner = {@link #cur}, set by Ob.set / the frame builders). */
    String cur = "?";
    private final java.util.Map<Long, Integer> own = new java.util.HashMap<>();
    private final java.util.List<String> ownerNames = new java.util.ArrayList<>();
    private final java.util.Map<String, int[]> overlapAt = new java.util.TreeMap<>();   // "A x B" -> {count, x, y, z}

    private int ownerId(String n) {
        int i = ownerNames.indexOf(n);
        if (i < 0) { ownerNames.add(n); i = ownerNames.size() - 1; }
        return i;
    }

    /** "ownerA x ownerB" -> {blocks of A overwritten by B, first x, y, z}. */
    java.util.Map<String, int[]> overlaps() { return overlapAt; }

    /**
     * Replica scenery that touches (26-neighbourhood) a block of another owner: such a prop would clip through it. Returns
     * cell -> owner for every connected scenery component of a replica obstacle that holds no route cell and touches another
     * owner; pass 2 leaves those out, so props are dropped whole, never cut. Route components are kept (junction platforms).
     */
    java.util.Map<String, java.util.Set<Long>> decorConflicts() {
        java.util.Map<String, java.util.Set<Long>> out = new java.util.HashMap<>();
        java.util.Map<Integer, java.util.List<Long>> byOwner = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, Integer> e : own.entrySet()) byOwner.computeIfAbsent(e.getValue(), k -> new java.util.ArrayList<>()).add(e.getKey());
        for (Ob o : obs) {
            if (!o.replica) continue;
            int id = ownerNames.indexOf(o.id);
            if (id < 0) continue;
            java.util.List<Long> cells = byOwner.getOrDefault(id, java.util.List.of());
            java.util.Set<Long> mine = new HashSet<>(cells);
            java.util.Set<Long> route = new HashSet<>(o.safe);
            for (Ob.Node n : o.route) for (int[] c : n.cells) route.add(key(c[0], c[1], c[2]));
            java.util.Set<Long> seen = new HashSet<>();
            for (long start : cells) {
                if (!seen.add(start)) continue;
                java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<>();
                java.util.List<Long> comp = new ArrayList<>();
                q.add(start);
                boolean hasRoute = false, touches = false;
                while (!q.isEmpty()) {
                    long c = q.poll();
                    comp.add(c);
                    int cx = kx(c), cy = ky(c), cz = kz(c);
                    // route cells stand on the block below: protect the block under and the two above
                    for (int dy = -1; dy <= 2 && !hasRoute; dy++) if (route.contains(key(cx, cy + dy, cz))) hasRoute = true;
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        long n = key(cx + dx, cy + dy, cz + dz);
                        if (mine.contains(n)) { if (seen.add(n)) q.add(n); continue; }
                        Integer ow = own.get(n);
                        if (ow != null && ow != id && !Bs.isAir(b.get(cx + dx, cy + dy, cz + dz))) touches = true;
                    }
                }
                if (!hasRoute && touches) for (long c : comp) out.computeIfAbsent(o.id, k -> new HashSet<>()).add(c);
            }
            // route protection: route cells +-1 and the support column under them stay
            java.util.Set<Long> prot = new HashSet<>();
            for (long rc : route) for (int dx = -1; dx <= 1; dx++) for (int dy = -8; dy <= 2; dy++) for (int dz = -1; dz <= 1; dz++)
                prot.add(key(kx(rc) + dx, ky(rc) + dy, kz(rc) + dz));
            // cell-level yield: where this replica obstacle and another owner wrote different blocks into one cell
            for (java.util.Map.Entry<Long, String[]> e : conflicts.entrySet()) {
                String prev = e.getValue()[0], curO = e.getValue()[1];
                boolean prevRep = isReplica(prev), curRep = isReplica(curO);
                boolean skip = (o.id.equals(curO) && curRep) || (o.id.equals(prev) && prevRep && !curRep);
                String other = o.id.equals(curO) ? prev : curO;
                boolean bypass = !isObstacle(other) || o.id.equals(curO);   // a frame provides the floor itself / the earlier section wins the shared platform
                if (skip && (bypass || !prot.contains(e.getKey()))) out.computeIfAbsent(o.id, k -> new HashSet<>()).add(e.getKey());
            }
            // halo: a replica block next to a frame block (hub, fork, shrine, spur end, backdrop) is left out too
            for (java.util.Map.Entry<Long, Integer> e : own.entrySet()) {
                String on = ownerNames.get(e.getValue());
                if (isObstacle(on) || on.equals("?")) continue;
                int fx = kx(e.getKey()), fy = ky(e.getKey()), fz = kz(e.getKey());
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    long n = key(fx + dx, fy + dy, fz + dz);
                    if (mine.contains(n) && !prot.contains(n)) out.computeIfAbsent(o.id, k -> new HashSet<>()).add(n);
                }
            }
        }
        return out;
    }

    private boolean isReplica(String id) { for (Ob o : obs) if (o.id.equals(id)) return o.replica; return false; }
    private boolean isObstacle(String id) { for (Ob o : obs) if (o.id.equals(id)) return true; return false; }

    /** Pass 2 of an imported-map build: cells of replica scenery that touched another structure in pass 1 are not written. */
    java.util.Map<String, java.util.Set<Long>> skipOwner;
    /** Frame decor that must not cut into what is already built (spur ends, shrines): only air is written. */
    boolean softMode;
    private final java.util.Map<Long, String[]> conflicts = new java.util.HashMap<>();

    void set(int x, int y, int z, String s) {
        if (softMode && s != null && !Bs.isAir(s) && !Bs.isAir(b.get(x, y, z))) return;
        if (skipOwner != null && s != null && !Bs.isAir(s)) { java.util.Set<Long> so = skipOwner.get(cur); if (so != null && so.contains(key(x, y, z))) return; }
        if (s != null && Bs.id(s).equals("pearlescent_froglight")) padCells.add(key(x, y, z));
        else padCells.remove(key(x, y, z));
        if (s != null && !Bs.isAir(s) && !Bs.id(s).equals("pearlescent_froglight")) {
            long k = key(x, y, z);
            int me = ownerId(cur);
            Integer prev = own.put(k, me);
            if (prev != null && prev != me) {
                String old = b.get(x, y, z);
                if (old != null && !Bs.isAir(old) && !Bs.id(old).equals("pearlescent_froglight") && !Bs.full(s).equals(old)) {
                    conflicts.put(key(x, y, z), new String[]{ownerNames.get(prev), cur});
                    String pair = ownerNames.get(prev) + " x " + cur;
                    int[] v = overlapAt.get(pair);
                    if (v == null) overlapAt.put(pair, new int[]{1, x, y, z}); else v[0]++;
                }
            }
        }
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
