package com.mcm.games.parkourtag.map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/** In-memory block grid. Air entries are dropped. Used for the runtime placement list and offline dumps. */
public final class MapGrid implements BlockSink {
    public record Block(int x, int y, int z, String data) {}

    public interface Filter { boolean keep(int x, int y, int z, String data); }

    private final Map<Long, String> blocks = new LinkedHashMap<>();

    private static long key(int x, int y, int z) {
        return ((long) (x & 0xFFFFF) << 40) | ((long) (y & 0xFFFFF) << 20) | (long) (z & 0xFFFFF);
    }
    private static int unpack(long v) { int i = (int) (v & 0xFFFFF); return (i & 0x80000) != 0 ? i - 0x100000 : i; }

    public static String norm(String d) {
        if (d == null) return null;
        return d.startsWith("minecraft:") ? d : "minecraft:" + d;
    }

    @Override
    public void set(int x, int y, int z, String data) {
        long k = key(x, y, z);
        if (data == null || data.equals("air") || data.equals("minecraft:air")) blocks.remove(k);
        else blocks.put(k, norm(data));
    }

    public String get(int x, int y, int z) { return blocks.get(key(x, y, z)); }

    public int count() { return blocks.size(); }

    public static MapGrid of(PtMap map) {
        MapGrid g = new MapGrid();
        map.generate(g);
        return g;
    }

    public List<Block> list() {
        List<Block> out = new ArrayList<>(blocks.size());
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            long k = e.getKey();
            out.add(new Block(unpack(k >> 40), unpack(k >> 20), unpack(k), e.getValue()));
        }
        return out;
    }

    /** Writes the MCMDUMP format read by tools/render_preview.py. */
    public void writeDump(File f, Filter keep) throws IOException {
        List<Block> bs = new ArrayList<>();
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (Block b : list()) {
            if (keep != null && !keep.keep(b.x(), b.y(), b.z(), b.data())) continue;
            bs.add(b);
            x0 = Math.min(x0, b.x()); y0 = Math.min(y0, b.y()); z0 = Math.min(z0, b.z());
            x1 = Math.max(x1, b.x()); y1 = Math.max(y1, b.y()); z1 = Math.max(z1, b.z());
        }
        if (bs.isEmpty()) { x0 = y0 = z0 = 0; x1 = y1 = z1 = 0; }
        try (Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8)) {
            w.write("MCMDUMP 1 " + (x1 - x0 + 1) + " " + (y1 - y0 + 1) + " " + (z1 - z0 + 1) + "\n");
            for (Block b : bs) w.write((b.x() - x0) + " " + (b.y() - y0) + " " + (b.z() - z0) + " " + b.data() + "\n");
        }
    }
}
