package com.mcm.games.tgttos.maps.b.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared validators for the B maps (start capacity, landmark ray-cast, post-pass idempotency, overlays). */
public final class Check {
    private Check() {}

    /** Text report with an error counter. */
    public static final class Report {
        public final StringBuilder text = new StringBuilder();
        public int errors;

        public void line(String s) { text.append(s).append('\n'); }
        public void ok(String s) { line("  ok    " + s); }
        public void err(String s) { errors++; line("  ERROR " + s); }
        public void check(boolean good, String s) { if (good) ok(s); else err(s); }
    }

    /** Blocks view with per-position overrides (variant options) on top of a base store. */
    public static final class Overlay implements TgMapData.Blocks {
        private final TgMapData.Blocks base;
        private final Map<Long, String> over = new HashMap<>();
        private static final String AIR = "\u0000air";

        public Overlay(TgMapData.Blocks base) { this.base = base; }

        @Override public void set(int x, int y, int z, String s) {
            String f = TgMapData.SparseBlocks.normalize(s);
            over.put(TgMapData.SparseBlocks.key(x, y, z), f == null ? AIR : f);
        }
        @Override public String get(int x, int y, int z) {
            String o = over.get(TgMapData.SparseBlocks.key(x, y, z));
            if (o == null) return base.get(x, y, z);
            return o == AIR ? null : o;
        }
        @Override public void forEach(TgMapData.Visitor v) {
            base.forEach((x, y, z, s) -> { if (!over.containsKey(TgMapData.SparseBlocks.key(x, y, z))) v.block(x, y, z, s); });
            for (Map.Entry<Long, String> e : over.entrySet()) {
                if (e.getValue() == AIR) continue;
                long k = e.getKey();
                v.block(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k), e.getValue());
            }
        }
        @Override public int size() { int[] n = {0}; forEach((x, y, z, s) -> n[0]++); return n[0]; }
    }

    /** Copy of the map data viewing a different block store (same metadata). */
    public static TgMapData view(TgMapData d, TgMapData.Blocks blocks) {
        TgMapData v = new TgMapData(d.mapId, d.seed, blocks);
        v.voidY = d.voidY;
        v.hazardBlocks.addAll(d.hazardBlocks);
        v.hazardRegions.addAll(d.hazardRegions);
        v.playBounds = d.playBounds;
        v.buildBounds = d.buildBounds;
        v.pen = d.pen;
        v.startStrip = d.startStrip;
        v.startSpawns.addAll(d.startSpawns);
        return v;
    }

    /** Apply one option of every variant group (by name -> option) onto an overlay. */
    public static Overlay withOptions(TgMapData d, Map<String, String> pick) {
        Overlay o = new Overlay(d.blocks);
        for (TgMapData.VariantGroup g : d.variants) {
            String opt = pick.getOrDefault(g.name, g.options.keySet().iterator().next());
            for (TgMapData.Placement p : g.options.get(opt)) o.set(p.x(), p.y(), p.z(), p.block());
        }
        return o;
    }

    /** The world after GO: barrier and drop floor removed. */
    public static Overlay afterGo(TgMapData d, TgMapData.Blocks base) {
        Overlay o = new Overlay(base);
        for (TgMapData.Pos p : d.startBarrier) o.set(p.x(), p.y(), p.z(), null);
        for (TgMapData.Pos p : d.dropFloor) o.set(p.x(), p.y(), p.z(), null);
        return o;
    }

    // ------------------------------------------------------------------ common checks

    public static void start(TgMapData d, Report r) {
        int n = d.startSpawns.size(), inside = 0, standing = 0;
        double minGap = 99;
        for (int i = 0; i < n; i++) {
            Spawn s = d.startSpawns.get(i);
            if (d.startStrip != null && d.startStrip.contains(s.x(), s.y(), s.z())) inside++;
            int bx = (int) Math.floor(s.x()), by = (int) Math.floor(s.y()), bz = (int) Math.floor(s.z());
            if (Bs.passable(d.blocks.get(bx, by, bz)) && Bs.passable(d.blocks.get(bx, by + 1, bz))
                    && Bs.standTop(d.blocks.get(bx, by - 1, bz)) > 0) standing++;
            for (int j = i + 1; j < n; j++) {
                Spawn t = d.startSpawns.get(j);
                minGap = Math.min(minGap, Math.hypot(s.x() - t.x(), s.z() - t.z()));
            }
        }
        r.check(n >= 40 && inside == n && standing == n && minGap >= 0.75,
                String.format("start holds %d players (%d inside the strip, %d on solid floor with headroom, closest pair %.2f apart)", n, inside, standing, minGap));
        if (d.startStrip != null) {
            Region s = d.startStrip;
            r.line(String.format("        start strip %d x %d (x %d..%d, z %d..%d), barrier %d blocks, drop floor %d blocks",
                    s.maxX() - s.minX() + 1, s.maxZ() - s.minZ() + 1, s.minX(), s.maxX(), s.minZ(), s.maxZ(), d.startBarrier.size(), d.dropFloor.size()));
        }
        int missing = 0;
        for (TgMapData.Pos p : d.startBarrier) if (d.blocks.get(p.x(), p.y(), p.z()) == null) missing++;
        for (TgMapData.Pos p : d.dropFloor) if (d.blocks.get(p.x(), p.y(), p.z()) == null) missing++;
        r.check(missing == 0, "every barrier / drop-floor position holds a real block (" + missing + " missing)");
    }

    /** Does a ray from the eye see the target point? Transparent / thin blocks don't block. */
    public static boolean sees(TgMapData.Blocks b, double x1, double y1, double z1, double x2, double y2, double z2) {
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int steps = (int) Math.ceil(len / 0.2);
        for (int i = 1; i < steps - 3; i++) {
            double t = i / (double) steps;
            int x = (int) Math.floor(x1 + (x2 - x1) * t), y = (int) Math.floor(y1 + (y2 - y1) * t), z = (int) Math.floor(z1 + (z2 - z1) * t);
            String s = b.get(x, y, z);
            if (s == null) continue;
            String id = Bs.id(s);
            if (Bs.passable(s) || id.contains("glass") || id.equals("iron_bars") || id.equals("chain") || Bs.kind(id) != 0
                    || id.equals("barrier") || id.contains("lantern") || id.endsWith("_rod")) continue;
            return false;
        }
        return true;
    }

    /** Landmark visible from the start: rays from spawn eyes to each target point (any target counts per eye). */
    public static void landmark(TgMapData d, TgMapData.Blocks b, List<double[]> targets, Report r) {
        int eyes = 0, seeing = 0;
        for (int i = 0; i < d.startSpawns.size(); i += 4) {
            Spawn s = d.startSpawns.get(i);
            eyes++;
            for (double[] t : targets)
                if (sees(b, s.x(), s.y() + 1.62, s.z(), t[0], t[1], t[2])) { seeing++; break; }
        }
        r.check(seeing * 10 >= eyes * 8, String.format("landmark visible from the start: %d of %d sampled spawn eyes see it (ray-cast)", seeing, eyes));
    }

    /** Post-pass ran and is idempotent (a second run changes nothing). */
    public static void postPass(TgMapData d, PostPass.Result res, Report r) {
        Map<Long, String> before = new HashMap<>();
        d.blocks.forEach((x, y, z, s) -> { if (Bs.kind(Bs.id(s)) != 0) before.put(TgMapData.SparseBlocks.key(x, y, z), s); });
        PostPass.run(d.blocks);
        int changed = 0;
        for (Map.Entry<Long, String> e : before.entrySet()) {
            long k = e.getKey();
            String now = d.blocks.get(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k));
            if (!e.getValue().equals(now)) changed++;
        }
        r.check(changed == 0, "connection post-pass: " + res.connections() + " fence/pane/wall/bar states set, " + res.leaves() + " leaves, "
                + res.carpetsDropped() + " unsupported carpets dropped; re-run changes " + changed);
    }

    public static void stairs(G g, Report r) {
        List<String> e = Stairs.validate(g.b, g.lanes);
        int cols = 0;
        for (Stairs.Lane l : g.lanes) cols += l.w();
        r.check(e.isEmpty(), "staircase helper: " + g.lanes.size() + " flights (" + cols + " columns) walked bottom to top" + (e.isEmpty() ? "" : ": " + e.size() + " problems"));
        for (int i = 0; i < Math.min(8, e.size()); i++) r.line("        " + e.get(i));
    }

    public static void contract(TgMapData d, com.mcm.games.tgttos.model.Kit kit, Report r) {
        List<String> p = d.check(kit);
        r.check(p.isEmpty(), "contract check (TgMapData.check): " + (p.isEmpty() ? "clean" : String.join("; ", p)));
        int[] team = {0};
        String[] reserved = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "cyan", "purple", "blue", "green", "red"};
        d.blocks.forEach((x, y, z, s) -> {
            String id = Bs.id(s);
            if (id.endsWith("_concrete") || id.endsWith("_concrete_powder")) team[0]++;
        });
        for (TgMapData.VariantGroup g : d.variants)
            for (List<TgMapData.Placement> l : g.options.values())
                for (TgMapData.Placement pl : l) if (pl.block() != null && Bs.id(pl.block()).contains("concrete")) team[0]++;
        r.check(team[0] == 0, "no concrete of any colour in blocks or variants (" + team[0] + "); reserved team colours: " + String.join(",", reserved));
    }

    public static List<Region> list(Region... rs) { return new ArrayList<>(List.of(rs)); }
}
