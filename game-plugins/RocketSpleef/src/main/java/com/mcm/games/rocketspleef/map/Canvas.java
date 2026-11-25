package com.mcm.games.rocketspleef.map;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Sparse pure-Java voxel canvas (no Bukkit). Blocks are "minecraft:id" strings at local coordinates; the game
 * pastes it with the local origin at a world anchor. Also writes MCMDUMP files for the offline renderer.
 */
public final class Canvas {
    public record Pos(int x, int y, int z) {}

    private final Map<Long, String> blocks = new HashMap<>();
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    private static long key(int x, int y, int z) {
        return ((long) (x + 2048) << 40) | ((long) (y + 2048) << 20) | (long) (z + 2048);
    }

    private static Pos unkey(long k) {
        return new Pos((int) (k >> 40) - 2048, (int) ((k >> 20) & 0xFFFFF) - 2048, (int) (k & 0xFFFFF) - 2048);
    }

    public void set(int x, int y, int z, String id) {
        if (id == null || id.equals("minecraft:air")) { blocks.remove(key(x, y, z)); return; }
        blocks.put(key(x, y, z), id);
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
    }

    public String get(int x, int y, int z) { return blocks.get(key(x, y, z)); }
    public boolean solid(int x, int y, int z) { return blocks.containsKey(key(x, y, z)); }
    public int count() { return blocks.size(); }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    /** Largest horizontal distance of any block from the local origin. */
    public int maxHorizRadius() {
        int m = Math.max(Math.max(Math.abs(minX), Math.abs(maxX)), Math.max(Math.abs(minZ), Math.abs(maxZ)));
        return m + 1;
    }

    public void forEach(BlockVisitor v) {
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            Pos p = unkey(e.getKey());
            v.visit(p.x(), p.y(), p.z(), e.getValue());
        }
    }

    public interface BlockVisitor { void visit(int x, int y, int z, String id); }

    // ---------------------------------------------------------------- shapes

    /** Filled blocky ellipsoid. */
    public void ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz, String id) {
        for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++)
            for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++)
                for (int z = (int) Math.floor(cz - rz); z <= (int) Math.ceil(cz + rz); z++) {
                    double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry, dz = (z + 0.5 - cz) / rz;
                    if (dx * dx + dy * dy + dz * dz <= 1.0) set(x, y, z, id);
                }
    }

    public void sphere(double cx, double cy, double cz, double r, String id) { ellipsoid(cx, cy, cz, r, r, r, id); }

    /**
     * A one-block-thick, face-connected (walkable "zig-zag") line between two points: each step moves one block along
     * a single axis, so the line reads as the stepped links in the reference images.
     */
    public void stepLine(double x0, double y0, double z0, double x1, double y1, double z1, String id) {
        int x = (int) Math.floor(x0), y = (int) Math.floor(y0), z = (int) Math.floor(z0);
        set(x, y, z, id);
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int steps = Math.max(1, (int) Math.ceil(len * 4));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            int nx = (int) Math.floor(x0 + (x1 - x0) * t);
            int ny = (int) Math.floor(y0 + (y1 - y0) * t);
            int nz = (int) Math.floor(z0 + (z1 - z0) * t);
            // move one axis at a time so every block touches the previous one by a face
            while (x != nx) { x += Integer.signum(nx - x); set(x, y, z, id); }
            while (z != nz) { z += Integer.signum(nz - z); set(x, y, z, id); }
            while (y != ny) { y += Integer.signum(ny - y); set(x, y, z, id); }
        }
    }

    public void cylinder(double cx, int y0, int y1, double cz, double r, String id) {
        for (int y = y0; y <= y1; y++)
            for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++)
                for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
                    double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                    if (dx * dx + dz * dz <= r * r) set(x, y, z, id);
                }
    }

    // ---------------------------------------------------------------- spawns

    /**
     * Standing spots: a solid block whose 4 side neighbours are solid too and with 2 air blocks above. Then picks
     * {@code n} of them spread apart (farthest-point sampling, deterministic).
     */
    public List<Pos> pickSpawns(int n) {
        List<Pos> cand = new ArrayList<>();
        for (long k : blocks.keySet()) {
            Pos p = unkey(k);
            if (solid(p.x(), p.y() + 1, p.z()) || solid(p.x(), p.y() + 2, p.z())) continue;
            if (!solid(p.x() + 1, p.y(), p.z()) || !solid(p.x() - 1, p.y(), p.z())
                    || !solid(p.x(), p.y(), p.z() + 1) || !solid(p.x(), p.y(), p.z() - 1)) continue;
            cand.add(p);
        }
        cand.sort((a, b) -> a.x() != b.x() ? Integer.compare(a.x(), b.x())
                : a.z() != b.z() ? Integer.compare(a.z(), b.z()) : Integer.compare(a.y(), b.y()));
        List<Pos> out = new ArrayList<>();
        if (cand.isEmpty()) return out;
        double[] best = new double[cand.size()];
        java.util.Arrays.fill(best, Double.MAX_VALUE);
        int pick = 0;
        for (int i = 0; i < cand.size(); i++) if (cand.get(i).y() > cand.get(pick).y()) pick = i; // start at the top
        while (out.size() < n && out.size() < cand.size()) {
            Pos p = cand.get(pick);
            out.add(p);
            int next = -1;
            double far = -1;
            for (int i = 0; i < cand.size(); i++) {
                Pos c = cand.get(i);
                double dx = c.x() - p.x(), dy = (c.y() - p.y()) * 1.5, dz = c.z() - p.z();
                double d = dx * dx + dy * dy + dz * dz;
                if (d < best[i]) best[i] = d;
                if (best[i] > far) { far = best[i]; next = i; }
            }
            if (next < 0 || far <= 0) break;
            pick = next;
        }
        return out;
    }

    // ---------------------------------------------------------------- dump

    /** Writes an MCMDUMP gz file (coords relative to the region minimum), optionally only blocks with y <= cutY. */
    public void writeDump(File f, int cutY) throws IOException {
        Files.createDirectories(f.toPath().toAbsolutePath().getParent());
        int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(f.toPath())), StandardCharsets.UTF_8)) {
            w.write("MCMDUMP 1 " + sx + " " + sy + " " + sz + "\n");
            StringBuilder sb = new StringBuilder();
            forEach((x, y, z, id) -> {
                if (y > cutY) return;
                sb.append(x - minX).append(' ').append(y - minY).append(' ').append(z - minZ).append(' ').append(id).append('\n');
            });
            w.write(sb.toString());
        }
    }
}
