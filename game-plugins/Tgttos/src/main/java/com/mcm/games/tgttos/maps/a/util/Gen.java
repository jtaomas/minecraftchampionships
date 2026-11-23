package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData;

/**
 * Drawing helper over a {@link TgMapData} block store (pure Java): boxes, palettes, ellipsoids, cylinders,
 * lines, and the design record ({@link Design}) the offline validators read.
 */
public final class Gen {
    public final TgMapData d;
    public final TgMapData.Blocks b;
    public final long seed;
    public final Design design = new Design();

    public Gen(TgMapData d) {
        this.d = d;
        this.b = d.blocks;
        this.seed = d.seed;
    }

    // ------------------------------------------------------------------ basic

    public void set(int x, int y, int z, String s) { b.set(x, y, z, s); }

    public String get(int x, int y, int z) { return b.get(x, y, z); }

    public boolean air(int x, int y, int z) { return Bs.isAir(b.get(x, y, z)); }

    /** Set only where the cell is currently air. */
    public void soft(int x, int y, int z, String s) { if (air(x, y, z)) b.set(x, y, z, s); }

    public double rnd(int x, int y, int z) { return Noise.hash01(x, y, z, seed); }

    public double rnd(int x, int y, int z, long salt) { return Noise.hash01(x, y, z, seed * 31 + salt); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) b.set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Noise.Pal p) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) b.set(x, y, z, p.at(x, y, z, seed));
    }

    public void softFill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) soft(x, y, z, s);
    }

    /** Walls of a box (no top/bottom). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Noise.Pal p) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) { b.set(x, y, z1, p.at(x, y, z1, seed)); b.set(x, y, z2, p.at(x, y, z2, seed)); }
            for (int z = z1; z <= z2; z++) { b.set(x1, y, z, p.at(x1, y, z, seed)); b.set(x2, y, z, p.at(x2, y, z, seed)); }
        }
    }

    /** Solid ellipsoid. */
    public void ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz, Noise.Pal p) {
        for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++)
            for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++)
                for (int z = (int) Math.floor(cz - rz); z <= (int) Math.ceil(cz + rz); z++) {
                    double a = (x - cx) / rx, c = (y - cy) / ry, e = (z - cz) / rz;
                    if (a * a + c * c + e * e <= 1.0) b.set(x, y, z, p.at(x, y, z, seed));
                }
    }

    /** Vertical cylinder (filled). */
    public void cyl(double cx, double cz, double r, int y1, int y2, Noise.Pal p) {
        for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++)
            for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= r * r) for (int y = y1; y <= y2; y++) b.set(x, y, z, p.at(x, y, z, seed));
            }
    }

    /** Vertical ring (cylinder shell of thickness t). */
    public void ring(double cx, double cz, double r, double t, int y1, int y2, Noise.Pal p) {
        for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++)
            for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz, dd = Math.sqrt(dx * dx + dz * dz);
                if (dd <= r && dd > r - t) for (int y = y1; y <= y2; y++) b.set(x, y, z, p.at(x, y, z, seed));
            }
    }

    /** 3D line of blocks (thickness 1). */
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, String s) {
        double len = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        int n = Math.max(1, (int) Math.ceil(len * 2));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            b.set((int) Math.floor(x1 + (x2 - x1) * t), (int) Math.floor(y1 + (y2 - y1) * t), (int) Math.floor(z1 + (z2 - z1) * t), s);
        }
    }

    /** Thick 3D line: sphere of radius r swept along the segment. */
    public void tube(double x1, double y1, double z1, double x2, double y2, double z2, double r, Noise.Pal p) {
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int n = Math.max(1, (int) Math.ceil(len * 2));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double cx = x1 + (x2 - x1) * t, cy = y1 + (y2 - y1) * t, cz = z1 + (z2 - z1) * t;
            for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++)
                for (int y = (int) Math.floor(cy - r); y <= (int) Math.ceil(cy + r); y++)
                    for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
                        double a = x + 0.5 - cx, c = y + 0.5 - cy, e = z + 0.5 - cz;
                        if (a * a + c * c + e * e <= r * r) b.set(x, y, z, p.at(x, y, z, seed));
                    }
        }
    }

    /** Tapered rocky underside below a footprint: for each (x,z) in the box that has a block at y=top, hang rock down. */
    public void underside(int x1, int z1, int x2, int z2, int top, int depth, Noise.Pal p, long salt) {
        double cx = (x1 + x2) / 2.0, cz = (z1 + z2) / 2.0, hx = (x2 - x1) / 2.0 + 0.5, hz = (z2 - z1) / 2.0 + 0.5;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                double ex = Math.abs(x - cx) / hx, ez = Math.abs(z - cz) / hz;
                double e = Math.max(ex, ez);
                double n = Noise.fbm2(x * 0.18, z * 0.18, seed + salt);
                int dpt = (int) Math.round(depth * (1 - e * e) * (0.55 + 0.6 * n));
                for (int y = top - 1; y >= top - dpt; y--) soft(x, y, z, p.at(x, y, z, seed + salt));
            }
    }

    /**
     * Free-run breaker: along z in every column x1..x2, whenever more than {@code maxRun} consecutive flat
     * standable cells at feet level {@code feetY} occur (outside the exempt boxes), drop a prop on the next one.
     */
    public int breakRuns(int x1, int x2, int z1, int z2, int feetY, int maxRun, java.util.List<com.mcm.games.tgttos.model.TgMapData.Region> exempt, String... props) {
        int placed = 0;
        for (int x = x1; x <= x2; x++) {
            int run = 0;
            for (int z = z1; z <= z2; z++) {
                boolean ex = false;
                for (var r : exempt) if (r.contains(x, feetY, z)) { ex = true; break; }
                boolean st = !ex && air(x, feetY, z) && air(x, feetY + 1, z) && Bs.fullSolid(get(x, feetY - 1, z));
                if (!st) { run = 0; continue; }
                run++;
                if (run > maxRun) {
                    String p = props[(int) (rnd(x, feetY, z, 991) * props.length)];
                    set(x, feetY, z, p);
                    placed++;
                    run = 0;
                }
            }
        }
        return placed;
    }

    public static String st(String mat, String facing, boolean top) { return Bs.stairs(mat, facing, top); }

    public static String slab(String mat, boolean top) { return Bs.slab(mat, top); }
}
