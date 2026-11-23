import com.mcm.lobby.HubGenerator;
import com.mcm.lobby.Vox;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Close-up dumps with real block shapes. Every block is expanded into an R x R x R grid of micro-voxels following
 * its state (stairs facing/half, slabs, fence/pane/bar arms, wall posts + low/tall sides, carpets, lanterns...),
 * so the cube renderer (render_preview.py) shows sunk steps, missing steps and disconnected fences.
 * States are read exactly as the server would place them (missing properties = vanilla defaults, i.e. a fence
 * without states is an unconnected post).
 * Usage: java -cp out HubCloseup outDir R name:x1:y1:z1:x2:y2:z2 ...
 */
public class HubCloseup {
    public static void main(String[] args) throws Exception {
        Vox v = new Vox();
        new HubGenerator(v, 20261123L).generate();
        int R = Integer.parseInt(args[1]);
        for (int i = 2; i < args.length; i++) {
            String[] p = args[i].split(":");
            dump(v, new File(args[0], p[0] + ".mcmdump.gz"), R, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]));
        }
    }

    static Map<String, String> props(String s) {
        Map<String, String> m = new HashMap<>();
        int b = s.indexOf('['), e = s.lastIndexOf(']');
        if (b < 0) return m;
        for (String kv : s.substring(b + 1, e).split(",")) { int q = kv.indexOf('='); if (q > 0) m.put(kv.substring(0, q), kv.substring(q + 1)); }
        return m;
    }

    static String base(String s) {
        int b = s.indexOf('[');
        String id = b < 0 ? s : s.substring(0, b);
        return id.substring(id.indexOf(':') + 1);
    }

    interface Cell { boolean in(int x, int y, int z); }

    /** Micro-voxel shape (coords 0..R-1) of a block state. */
    static Cell shape(String st, int R) {
        String id = base(st);
        Map<String, String> p = props(st);
        int h = R / 2, c0 = R * 3 / 8, c1 = R - 1 - c0;           // post band (6/16 .. 10/16)
        int t0 = R * 7 / 16, t1 = R - 1 - t0;                       // thin band (7/16 .. 9/16) for panes/arms
        if (id.endsWith("_stairs")) {
            boolean top = "top".equals(p.getOrDefault("half", "bottom"));
            String f = p.getOrDefault("facing", "north");
            return (x, y, z) -> {
                boolean slabPart = top ? y >= h : y < h;
                boolean back = switch (f) { case "north" -> z < h; case "south" -> z >= h; case "east" -> x >= h; default -> x < h; };
                return slabPart || back;
            };
        }
        if (id.endsWith("_slab")) {
            String t = p.getOrDefault("type", "bottom");
            return (x, y, z) -> t.equals("double") || (t.equals("top") ? y >= h : y < h);
        }
        if (id.endsWith("_carpet")) return (x, y, z) -> y == 0;
        if (id.endsWith("_fence")) {
            boolean n = "true".equals(p.get("north")), s = "true".equals(p.get("south")), e = "true".equals(p.get("east")), w = "true".equals(p.get("west"));
            int a1 = R * 6 / 16, a2 = R * 12 / 16;                  // arm heights
            return (x, y, z) -> {
                if (x >= c0 && x <= c1 && z >= c0 && z <= c1) return true;
                boolean armY = y == a1 || y == a1 + Math.max(0, R / 8 - 1) || y == a2 || y == a2 + Math.max(0, R / 8 - 1);
                if (!armY) return false;
                boolean inX = x >= t0 && x <= t1, inZ = z >= t0 && z <= t1;
                return (inX && ((n && z < c0) || (s && z > c1))) || (inZ && ((w && x < c0) || (e && x > c1)));
            };
        }
        if (id.endsWith("_pane") || id.equals("iron_bars")) {
            boolean n = "true".equals(p.get("north")), s = "true".equals(p.get("south")), e = "true".equals(p.get("east")), w = "true".equals(p.get("west"));
            return (x, y, z) -> {
                boolean inX = x >= t0 && x <= t1, inZ = z >= t0 && z <= t1;
                if (inX && inZ) return true;
                return (inX && ((n && z < t0) || (s && z > t1))) || (inZ && ((w && x < t0) || (e && x > t1)));
            };
        }
        if (id.endsWith("_wall")) {
            boolean up = !"false".equals(p.get("up"));
            String n = p.getOrDefault("north", "none"), s = p.getOrDefault("south", "none"), e = p.getOrDefault("east", "none"), w = p.getOrDefault("west", "none");
            int p0 = R / 4, p1 = R - 1 - p0;                       // post 4/16 .. 12/16
            int lowTop = R * 14 / 16 - 1;
            return (x, y, z) -> {
                if (up && x >= p0 && x <= p1 && z >= p0 && z <= p1) return true;
                boolean inX = x >= c0 - 1 && x <= c1 + 1, inZ = z >= c0 - 1 && z <= c1 + 1;
                java.util.function.Predicate<String> sideY = sd -> sd.equals("tall") || (sd.equals("low") && y <= lowTop);
                if (inX && inZ && (!n.equals("none") || !s.equals("none") || !e.equals("none") || !w.equals("none")) && y <= lowTop) return true;
                return (inX && ((z < h && sideY.test(n)) || (z >= h && sideY.test(s)))) || (inZ && ((x < h && sideY.test(w)) || (x >= h && sideY.test(e))));
            };
        }
        if (id.equals("lantern") || id.equals("soul_lantern")) {
            int l0 = R * 5 / 16, l1 = R - 1 - l0, lh = R * 9 / 16;
            boolean hang = "true".equals(p.get("hanging"));
            return (x, y, z) -> x >= l0 && x <= l1 && z >= l0 && z <= l1 && (hang ? y >= R - lh - 1 && y < R - 1 : y < lh);
        }
        if (id.equals("chain") || id.equals("end_rod") || id.equals("lightning_rod")) return (x, y, z) -> x >= t0 && x <= t1 && z >= t0 && z <= t1;
        if (id.contains("campfire")) return (x, y, z) -> y < R * 7 / 16;
        if (id.equals("snow")) { int L = Integer.parseInt(p.getOrDefault("layers", "1")); return (x, y, z) -> y < Math.max(1, L * R / 8); }
        if (id.equals("water") || id.equals("seagrass") || id.equals("short_grass") || id.equals("tall_grass") || id.equals("fern")
                || id.endsWith("_tulip") || id.equals("poppy") || id.equals("dandelion") || id.equals("cornflower") || id.equals("allium")
                || id.equals("azure_bluet") || id.equals("oxeye_daisy") || id.equals("lily_of_the_valley") || id.equals("dead_bush"))
            return id.equals("water") ? (x, y, z) -> true : (x, y, z) -> (x == z || x == R - 1 - z) && y < R * 3 / 4;
        return (x, y, z) -> true;
    }

    static void dump(Vox v, File f, int R, int x1, int y1, int z1, int x2, int y2, int z2) throws IOException {
        int sx = (x2 - x1 + 1) * R, sy = (y2 - y1 + 1) * R, sz = (z2 - z1 + 1) * R;
        Map<String, Cell> cache = new HashMap<>();
        long n = 0;
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f), 1 << 16), "UTF-8"), 1 << 16)) {
            w.write("MCMDUMP 1 " + sx + " " + sy + " " + sz + "\n");
            for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = y1; y <= y2; y++) {
                String st = v.get(x, y, z);
                if (st == null) continue;
                Cell c = cache.computeIfAbsent(st, s -> shape(s, R));
                String name = st.contains("[") ? st.substring(0, st.indexOf('[')) : st;
                for (int a = 0; a < R; a++) for (int b = 0; b < R; b++) for (int e = 0; e < R; e++) {
                    if (!c.in(a, b, e)) continue;
                    w.write(((x - x1) * R + a) + " " + ((y - y1) * R + b) + " " + ((z - z1) * R + e) + " " + name + "\n");
                    n++;
                }
            }
        }
        System.out.println("  " + f.getName() + ": " + n + " micro-voxels (R=" + R + ")");
    }
}
