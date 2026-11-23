package com.mcm.games.tgttos.maps.b.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generator context for the B maps: wraps a {@link TgMapData}, tags blocks as scenery or route (for the
 * scenery-share report), and offers shape / palette / signage helpers. Pure Java.
 */
public final class G {
    public final TgMapData d;
    public final TgMapData.Blocks b;
    public final long seed;
    /** While true, placed blocks count as scenery (never on the route). */
    public boolean scenery;
    private final Set<Long> sceneryKeys = new HashSet<>();
    public final List<Stairs.Lane> lanes = new ArrayList<>();
    public PostPass.Result post;
    /** Landmark target points for the visibility ray-cast. */
    public final List<double[]> landmark = new ArrayList<>();

    public G(TgMapData d) {
        this.d = d;
        this.b = d.blocks;
        this.seed = d.seed;
    }

    // ------------------------------------------------------------------ basic

    public void set(int x, int y, int z, String s) {
        b.set(x, y, z, s == null ? null : Bs.full(s));
        long k = TgMapData.SparseBlocks.key(x, y, z);
        if (scenery && s != null) sceneryKeys.add(k); else sceneryKeys.remove(k);
    }

    /** Set only where the cell is air. */
    public void setIfAir(int x, int y, int z, String s) { if (b.get(x, y, z) == null) set(x, y, z, s); }

    public String get(int x, int y, int z) { return b.get(x, y, z); }

    public boolean air(int x, int y, int z) { return b.get(x, y, z) == null; }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Noise.Pal p) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, p.at(x, y, z, seed));
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, (String) null); }

    /** Hollow box walls (no floor/roof). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Noise.Pal p) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) { set(x, y, z1, p.at(x, y, z1, seed)); set(x, y, z2, p.at(x, y, z2, seed)); }
            for (int z = z1; z <= z2; z++) { set(x1, y, z, p.at(x1, y, z, seed)); set(x2, y, z, p.at(x2, y, z, seed)); }
        }
    }

    public double rnd(int x, int y, int z) { return Noise.hash01(x, y, z, seed); }

    public double rnd(int x, int y, int z, long salt) { return Noise.hash01(x, y, z, seed + salt); }

    /** Filled vertical cylinder. */
    public void cyl(double cx, double cz, double r, int y1, int y2, Noise.Pal p) {
        int r0 = (int) Math.ceil(r);
        for (int x = (int) Math.floor(cx) - r0; x <= (int) Math.floor(cx) + r0; x++)
            for (int z = (int) Math.floor(cz) - r0; z <= (int) Math.floor(cz) + r0; z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= r * r) for (int y = y1; y <= y2; y++) set(x, y, z, p.at(x, y, z, seed));
            }
    }

    /** Filled ellipsoid. */
    public void ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz, Noise.Pal p) {
        for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++)
            for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++)
                for (int z = (int) Math.floor(cz - rz); z <= (int) Math.ceil(cz + rz); z++) {
                    double a = (x + 0.5 - cx) / rx, c = (y + 0.5 - cy) / ry, e = (z + 0.5 - cz) / rz;
                    if (a * a + c * c + e * e <= 1) set(x, y, z, p.at(x, y, z, seed));
                }
    }

    /** 3D line of blocks (DDA, every cell touched once). */
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, String s) {
        double len = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        int n = Math.max(1, (int) Math.ceil(len * 2));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            set((int) Math.floor(x1 + (x2 - x1) * t), (int) Math.floor(y1 + (y2 - y1) * t), (int) Math.floor(z1 + (z2 - z1) * t), s);
        }
    }

    /** A hanging chain / rope between two points: chain blocks with the dominant axis per cell. */
    public void tether(double x1, double y1, double z1, double x2, double y2, double z2, String mat) {
        double dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
        String axis = dy >= dx && dy >= dz ? "y" : dx >= dz ? "x" : "z";
        line(x1, y1, z1, x2, y2, z2, mat + "[axis=" + axis + "]");
    }

    // ------------------------------------------------------------------ common pieces

    /** 40+ start spawns in rows on a strip [x1..x2] x [z1..z2] with feet at y, facing +Z. */
    public void startSpawns(int x1, int x2, int z1, int z2, int y, int perRow, int rows) {
        double w = x2 - x1 + 1, dep = z2 - z1 + 1;
        for (int r = 0; r < rows; r++)
            for (int i = 0; i < perRow; i++) {
                double x = x1 + (i + 0.5) * w / perRow;
                double z = z1 + (r + 0.5) * dep / rows;
                d.startSpawns.add(new Spawn(x, y, z, 0f));
            }
    }

    /** Chicken spawns on a grid inside the pen interior. */
    public void chickens(Region pen, int n) {
        int cols = (int) Math.ceil(Math.sqrt(n));
        double w = pen.maxX() - pen.minX() + 1 - 2, dep = pen.maxZ() - pen.minZ() + 1 - 2;
        for (int i = 0; i < n; i++) {
            int c = i % cols, r = i / cols;
            d.chickenSpawns.add(new Spawn(pen.minX() + 1 + (c + 0.5) * w / cols, pen.minY(),
                    pen.minZ() + 1 + (r + 0.5) * dep / cols, 180f));
        }
    }

    /** Run the connection post-pass and drop any concrete (call once at the end of a build). */
    public void finish() {
        scrubConcrete();
        post = PostPass.run(b);
    }

    /** Team-colour concrete is reserved for players: replace any stray one (safety net) and count them. */
    public int scrubConcrete() {
        List<int[]> bad = new ArrayList<>();
        b.forEach((x, y, z, s) -> { if (Bs.id(s).endsWith("_concrete")) bad.add(new int[]{x, y, z}); });
        for (int[] p : bad) set(p[0], p[1], p[2], "minecraft:smooth_stone");
        return bad.size();
    }

    /** Share of final blocks that were placed in scenery mode (0..1). */
    public double sceneryShare() {
        int[] n = {0, 0};
        b.forEach((x, y, z, s) -> {
            n[0]++;
            if (sceneryKeys.contains(TgMapData.SparseBlocks.key(x, y, z))) n[1]++;
        });
        return n[0] == 0 ? 0 : n[1] / (double) n[0];
    }

    // ------------------------------------------------------------------ signage

    private static final String[] FONT = {
            "A:010101111101101", "B:110101110101110", "C:011100100100011", "D:110101101101110", "E:111100110100111",
            "F:111100110100100", "G:011100101101011", "H:101101111101101", "I:111010010010111", "K:101101110101101",
            "L:100100100100111", "N:110101101101101", "O:010101101101010", "P:110101110100100", "Q:010101101110011",
            "R:110101110101101", "S:011100010001110", "T:111010010010010", "U:101101101101111", "V:101101101101010",
            "Y:101101010010010", "1:010110010010111", "2:110001010100111", "3:110001010001110", "0:010101101101010",
            "!:010010010000010", "-:000000111000000"};

    /** 5-wide glyphs (M, W). */
    private static final String[] WIDE = {"M:1000111011101011000110001", "W:1000110001101011101110001"};

    private static String glyph(char c) {
        for (String f : FONT) if (f.charAt(0) == c) return f.substring(2);
        return null;
    }

    private static String wide(char c) {
        for (String f : WIDE) if (f.charAt(0) == c) return f.substring(2);
        return null;
    }

    /** Width in pixels of a text (glyph width + 1 spacing; space = 2). */
    public static int textWidth(String t) {
        int w = 0;
        for (char c : t.toCharArray()) w += c == ' ' ? 2 : (wide(c) != null ? 6 : 4);
        return Math.max(0, w - 1);
    }

    /**
     * Pixel letters in a vertical plane. Text runs along +ux (ux,uz is a horizontal unit vector, e.g. (1,0) or
     * (-1,0)); top-left pixel at (x, y, z); pixel size s.
     */
    public void text(String t, int x, int y, int z, int ux, int uz, int s, String block) {
        int cx = 0;
        for (char c : t.toCharArray()) {
            if (c == ' ') { cx += 2; continue; }
            String g = wide(c);
            int gw = 5;
            if (g == null) { g = glyph(c); gw = 3; }
            if (g == null) { cx += 4; continue; }
            for (int r = 0; r < 5; r++)
                for (int k = 0; k < gw; k++)
                    if (g.charAt(r * gw + k) == '1')
                        for (int a = 0; a < s; a++)
                            for (int e = 0; e < s; e++) {
                                int px = (cx + k) * s + a;
                                set(x + ux * px, y - r * s - e, z + uz * px, block);
                            }
            cx += gw + 1;
        }
    }
}
