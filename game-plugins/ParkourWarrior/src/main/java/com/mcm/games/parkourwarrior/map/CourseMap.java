package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Parkour Warrior course generator (pure Java, no Bukkit). One straight spine along +z: start island (= stage 1
 * hub) -> 3 main obstacles -> hub 2 -> ... -> stage 5 -> fork pavilion -> three endings with their own finish
 * shrines. Each hub has a 3-obstacle bonus spur leaving along +x. Every obstacle is its own hand-designed class
 * method (see the zone classes); this class only lays them out and fills the {@link CourseModel}.
 */
public final class CourseMap {
    private CourseMap() {}

    interface Mk { Ob make(W w, Ob.Slot s); }

    static final String[] ZONES = {"Toy Box", "Market Rooftops", "Shipwreck in the Sky", "Clockwork Workshop", "Crystal Mine"};
    static final String[] ZONE_BANNER = {"red", "orange", "blue", "yellow", "purple"};
    static final String[] ZONE_COLOUR = {"&c", "&6", "&9", "&e", "&5"};
    static final int BASE_Y = 100, HUB_R = 8, START_R = 10;

    static Mk[][] mains() {
        return new Mk[][]{
                {ToyBox::m1, ToyBox::m2, ToyBox::m3},
                {Market::m1, Market::m2, Market::m3},
                {Shipwreck::m1, Shipwreck::m2, Shipwreck::m3},
                {Clockwork::m1, Clockwork::m2, Clockwork::m3},
                {Crystal::m1, Crystal::m2, Crystal::m3}};
    }

    static Mk[][] bonuses() {
        return new Mk[][]{
                {ToyBox::b1, ToyBox::b2, ToyBox::b3},
                {Market::b1, Market::b2, Market::b3},
                {Shipwreck::b1, Shipwreck::b2, Shipwreck::b3},
                {Clockwork::b1, Clockwork::b2, Clockwork::b3},
                {Crystal::b2, Crystal::b3, Crystal::b1}};  // Dripstone (bronze), Crystal Curtain (silver), Runaway Rails (gold floor)
    }

    static Mk[][] endings() {
        return new Mk[][]{{Endings::easy1, Endings::easy2}, {Endings::med1, Endings::med2}, {Endings::hard1, Endings::hard2}};
    }

    /** Runtime entry point. */
    public static CourseModel generate(long seed) { return build(seed, null).model; }

    /**
     * Runtime entry point with imported sections: {@code importDir/course.txt} (see {@link ImportedCourse}); slots
     * without a usable section keep the built-in obstacle. A missing folder gives exactly the built-in course.
     */
    public static CourseModel generate(long seed, java.io.File importDir) {
        if (FullCourse.isLinear(importDir)) return FullCourse.load(seed, importDir);   // a replica imported 1:1 (format linear)
        return build(seed, ImportedCourse.load(importDir)).model;
    }

    static W build(long seed) { return build(seed, null); }

    /** Full build (model + generator bookkeeping, used by the offline tools); ic = imported sections or null. */
    static W build(long seed, ImportedCourse ic) {
        W w = buildOnce(seed, ic, null);
        if (ic == null) return w;
        // pass 2: leave out whole replica props that would clip through another structure (hub, spur end, shrine, classic
        // obstacle, a neighbouring section): nothing is cut, props are dropped entirely
        java.util.Map<String, java.util.Set<Long>> skip = w.decorConflicts();
        if (skip.isEmpty()) return w;
        W w2 = buildOnce(seed, ic, skip);
        w2.model.notes.put("dropped scenery", skip.values().stream().mapToInt(java.util.Set::size).sum() + " blocks of replica props that touched other structures");
        return w2;
    }

    static W buildOnce(long seed, ImportedCourse ic, java.util.Map<String, java.util.Set<Long>> skip) {
        W w = new W(seed);
        w.skipOwner = skip;
        CourseModel m = w.model;
        Mk[][] mains = mains(), bon = bonuses(), ends = endings();
        int cx = 0, cy = BASE_Y, cz = 0;
        List<int[]> hubs = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            int r = s == 1 ? START_R : HUB_R;
            hubs.add(new int[]{cx, cy, cz, r});
            // bonus spur along +x (frame q = 3)
            int bx = cx + r - 1, by = cy, bz = cz;
            for (int i = 1; i <= 3; i++) {
                Ob o = make(w, ic, bon[s - 1][i - 1], new Ob.Slot(CourseModel.bonusId(s, i), CourseModel.Kind.BONUS, s, i, null, bx, by, bz, 3));
                int nx = o.wx(o.endX, o.endZ), nz = o.wz(o.endX, o.endZ);
                bx = nx; by = o.wy(o.endY); bz = nz;
            }
            w.cur = "spur-end" + s;
            w.softMode = ic != null;
            Frame.spurEnd(w, bx, by, bz, 1, 0);
            w.softMode = false;
            // main line along +z
            int mx = cx, my = cy, mz = cz + r - 1;
            for (int i = 1; i <= 3; i++) {
                Ob o = make(w, ic, mains[s - 1][i - 1], new Ob.Slot(CourseModel.mainId(s, i), CourseModel.Kind.MAIN, s, i, null, mx, my, mz, 0));
                mx = o.wx(o.endX, o.endZ); my = o.wy(o.endY); mz = o.wz(o.endX, o.endZ);
            }
            cx = mx; cy = my; cz = mz + (s < 5 ? HUB_R - 1 : 7);
            if (ic != null) {
                // imported sections bring their own scenery: push the hub (or the fork) forward until its box is clear of it
                // and join it to the last pad with a short walkway, so no structure cuts into another
                int xr = s < 5 ? HUB_R + 2 : 22, zr = s < 5 ? HUB_R + 2 : 10;
                int d = 0;
                while (d < 40 && occupied(w, mx, my, mz, cz + d, xr, zr)) d += 2;
                if (d > 0) {
                    int zEdge = cz + d - (s < 5 ? HUB_R - 1 : 9) + 1;
                    for (int z = mz + 2; z <= zEdge; z++) for (int x = mx - 1; x <= mx + 1; x++) {
                        w.cur = "walkway" + s;
                        if (w.air(x, my, z)) { w.set(x, my, z, x == mx ? "polished_andesite" : "stone_bricks"); w.frameWalk.add(W.key(x, my, z)); }
                    }
                    cz += d;
                }
            }
        }
        // hubs are built after the obstacles so their decks fit around the pads
        for (int s = 1; s <= 5; s++) {
            int[] h = hubs.get(s - 1);
            w.cur = "hub" + s;
            Frame.hub(w, h[0], h[1], h[2], h[3], ZONE_BANNER[s - 1], s == 1);
        }
        int fx = cx, fy = cy, fz = cz;
        w.cur = "fork";
        Frame.fork(w, fx, fy, fz);
        CourseModel.Difficulty[] D = CourseModel.Difficulty.values();
        int[][] finish = new int[3][];
        for (int d = 0; d < 3; d++) {
            int ex = fx + Frame.GATES[d], ey = fy, ez = fz + 7;
            for (int i = 1; i <= 2; i++) {
                Ob o = make(w, ic, ends[d][i - 1], new Ob.Slot(CourseModel.endingId(D[d], i), CourseModel.Kind.ENDING, 0, i, D[d], ex, ey, ez, 0));
                ex = o.wx(o.endX, o.endZ); ey = o.wy(o.endY); ez = o.wz(o.endX, o.endZ);
            }
            finish[d] = new int[]{ex, ey, ez};
            w.cur = "shrine" + d;
            w.softMode = ic != null;
            Frame.shrine(w, ex, ey, ez, Frame.GATE_COL[d]);
            w.softMode = false;
        }
        w.cur = "backdrop";
        Backdrop.build(w, hubs, new int[]{fx, fy, fz}, finish);

        // ---- model
        for (int s = 1; s <= 5; s++) {
            int[] h = hubs.get(s - 1);
            CourseModel.Pos resp = CourseModel.Pos.block(h[0], h[1] + 1, h[2] + 4, 0f);
            CourseModel.Pos bonus = CourseModel.Pos.block(h[0] + h[3] - 1, h[1] + 1, h[2], 270f);
            String zn = ic != null && ic.zones.containsKey(s) ? ic.zones.get(s) : ZONES[s - 1];
            m.hubs.add(new CourseModel.Hub(s, resp, bonus, zn));
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(h[0] + 0.5, h[1] + 5.2, h[2] + h[3] - 1.5, 0f),
                    ZONE_COLOUR[s - 1] + "&lStage " + s + ": " + zn));
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(h[0] + h[3] + 0.5, h[1] + 8.3, h[2] + 0.5, 270f), "&d&lBONUS PATH &7>>"));
        }
        // start spawns: a ring on the start island avoiding the statue and the columns
        int[] h1 = hubs.get(0);
        // three rings (radius 4.5 / 6.5 / 8.2, 12 + 18 + 26 spots) so a full 40-player lobby never shares a spot; the first
        // 40 are spread over all rings by a fixed shuffle
        java.util.List<CourseModel.Pos> ring = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        double[] radii = {4.5, 6.5, 8.2};
        int[] counts = {12, 18, 26};
        for (int k = 0; k < radii.length; k++) for (int i = 0; i < counts[k]; i++) {
            double a = 2 * Math.PI * (i + (k == 1 ? 0.5 : 0)) / counts[k];
            int dx = (int) Math.round(radii[k] * Math.cos(a)), dz = (int) Math.round(radii[k] * Math.sin(a));
            if (Math.hypot(dx, dz) < 3.5 || Math.abs(dx) > 8 || Math.abs(dz) > 8) continue;
            if (Math.abs(Math.abs(dx) - 5) <= 1 && Math.abs(Math.abs(dz) - 5) <= 1) continue;   // quartz columns
            if (!seen.add(((long) dx << 32) ^ (dz & 0xffffffffL))) continue;
            ring.add(CourseModel.Pos.block(h1[0] + dx, h1[1] + 1, h1[2] + dz, 0f));
        }
        java.util.Collections.shuffle(ring, new java.util.Random(7));
        m.spawns.addAll(ring);
        m.fork = CourseModel.Pos.block(fx, fy + 1, fz + 2, 0f);
        if (ic != null) {
            m.mapId = ic.mapId;
            m.credit = ic.mapCredit;
            if (!ic.mapCredit.isEmpty())
                m.labels.add(new CourseModel.Label(new CourseModel.Pos(h1[0] + 0.5, h1[1] + 3.2, h1[2] - 6.5, 180f), "&7Course sections: &f" + ic.mapCredit));
        }
        for (int d = 0; d < 3; d++) {
            Ob last = find(w, CourseModel.endingId(D[d], 2));
            m.finishes.put(D[d], last.padBox);
            m.finishStands.put(D[d], CourseModel.Pos.block(finish[d][0], finish[d][1] + 1, finish[d][2] - 2, 180f));
            String[] txt = {"&a&lEASY &a★ &f" + endName(ic, "EASY", "Meadow & Beehive"), "&6&lMEDIUM &6★★ &f" + endName(ic, "MEDIUM", "Frozen Harbour"),
                    "&c&lHARD &c★★★ &f" + endName(ic, "HARD", "Library Tower")};
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(fx + Frame.GATES[d] + 0.5, fy + 10.3, fz + 7.5, 0f), txt[d]));
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(finish[d][0] + 0.5, finish[d][1] + 3.5, finish[d][2] + 0.5, 180f),
                    new String[]{"&a", "&6", "&c"}[d] + "&lFINISH"));
        }
        for (Ob o : w.obs) {
            o.killY = o.minFeet() - o.killDepth;
            // kill column: the route WITHOUT its start area. The start area is the previous pad / hub deck, and
            // including it let an obstacle that starts above another (the hard ending's roof start above the book
            // spines) kill players on the lower one (the "Spine Scramble" instant-death bug).
            int[] bb = Validate.bboxNoStart(o);
            CourseModel.Box area = new CourseModel.Box(bb[0] - 1, o.killY - 20, bb[2] - 1, bb[3] + 1, bb[4] + 6, bb[5] + 1);
            o.built = new CourseModel.Obstacle(o.id, o.name, o.kind, o.stage, o.index, o.ending,
                    o.pos(0, 1, 0, 0f), o.padBox, o.killY, o.pos(0, 4, 1, 0f), area);
            m.obstacles.add(o.built);
            // the name + medal label is spawned by the runtime at o.built.label() (with the id code); here only the
            // difficulty sign and the credit, in ONE label below it (two labels at one spot was the overlapping text)
            String sub = "";
            if (!o.difficultyTag.isEmpty()) {
                String c = o.difficultyTag.equals("EASY") ? "&a" : o.difficultyTag.equals("MEDIUM") ? "&6" : "&c";
                sub = c + "&l" + o.difficultyTag;
            }
            if (!o.credit.isEmpty()) sub += (sub.isEmpty() ? "" : "\n") + "&7Section from &f" + o.credit;
            if (!sub.isEmpty()) {
                CourseModel.Pos p = o.pos(0, 3, 1, 0f);
                m.labels.add(new CourseModel.Label(new CourseModel.Pos(p.x(), p.y() - (sub.indexOf('\n') >= 0 ? 0.4 : 0), p.z(), p.yaw()), sub));
            }
            m.timed.addAll(o.timed);
            m.movers.addAll(o.movers);
            m.crumbles.addAll(o.crumbles);
            m.belts.addAll(o.belts);
        }
        int midZ = (hubs.get(0)[2] + fz) / 2;
        m.overviews.add(new CourseModel.Overview("Spine (top-down)", new CourseModel.Pos(20.5, BASE_Y + 70, midZ + 0.5, 0f), 90f));
        m.overviews.add(new CourseModel.Overview("Fork", new CourseModel.Pos(fx + 0.5, fy + 16, fz - 14.5, 0f), 35f));
        m.overviews.add(new CourseModel.Overview("Start", new CourseModel.Pos(h1[0] + 0.5, h1[1] + 14, h1[2] - 16.5, 0f), 35f));
        PostPass.Result pr = PostPass.run(w.b);
        m.recomputeBounds();
        m.notes.put("generator", "CourseMap v1 (hand-designed obstacles, seed only varies scenery noise)");
        m.notes.put("postpass", "connections " + pr.connections() + ", leaves " + pr.leaves() + ", carpets dropped " + pr.carpetsDropped());
        m.notes.put("pad", W.PAD + " (only used for checkpoint pads)");
        if (ic != null) {
            StringBuilder sb = new StringBuilder();
            for (Ob o : w.obs) if (o.imported) sb.append(sb.length() == 0 ? "" : ", ").append(o.id);
            m.notes.put("imported", sb.length() == 0 ? "none" : sb.toString());
            if (!ic.problems.isEmpty()) m.notes.put("import problems", String.join("; ", ic.problems));
        }
        return w;
    }

    static Ob make(W w, ImportedCourse ic, Mk builtIn, Ob.Slot s) {
        Ob o = make0(w, ic, builtIn, s);
        if (ic != null) o.imported = true;   // inside an imported map every obstacle sits among foreign scenery: decoy / object-size / neo-tier findings are warnings
        return o;
    }

    private static Ob make0(W w, ImportedCourse ic, Mk builtIn, Ob.Slot s) {
        if (ic != null) {
            String[] b = ic.builtins.get(s.id());
            if (b != null) {
                Ob o = Mechanics.make(b[0], w, s, Mechanics.theme(b[1]), Integer.parseInt(b[2]));
                if (o != null) {
                    if (s.kind() == CourseModel.Kind.BONUS) o.difficultyTag = s.index() == 1 ? "MEDIUM" : "HARD";
                    return o;
                }
                ic.problems.add(s.id() + ": unknown builtin " + b[0]);
            }
            String c = ic.classics.get(s.id());
            if (c != null) {
                Mk k = classic(c);
                if (k != null) return k.make(w, s);
                ic.problems.add(s.id() + ": unknown classic slot " + c);
            }
        }
        Ob o = ic == null ? null : ic.make(w, s);
        return o != null ? o : builtIn.make(w, s);
    }

    /** The classic course's obstacle maker of a slot id (M1-1 .. B5-3, E-EASY-1 ..), or null. */
    static Mk classic(String id) {
        try {
            if (id.startsWith("M")) { String[] p = id.substring(1).split("-"); return mains()[Integer.parseInt(p[0]) - 1][Integer.parseInt(p[1]) - 1]; }
            if (id.startsWith("B")) { String[] p = id.substring(1).split("-"); return bonuses()[Integer.parseInt(p[0]) - 1][Integer.parseInt(p[1]) - 1]; }
            if (id.startsWith("E-")) {
                String[] p = id.split("-");
                return endings()[CourseModel.Difficulty.valueOf(p[1]).ordinal()][Integer.parseInt(p[2]) - 1];
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    static String endName(ImportedCourse ic, String d, String def) {
        return ic != null && ic.endNames.containsKey(d) ? ic.endNames.get(d) : def;
    }

    /** Any block in the hub / fork box around (mx, cz), ignoring the last pad's own corner (padZ). */
    static boolean occupied(W w, int mx, int y, int padZ, int cz, int xr, int zr) {
        // generous box (5 blocks of air all round, 30 up and down): the hub must not be reachable by a jump from the last
        // section's islands, only along the walkway that starts at the checkpoint pad, so the pad cannot be skipped
        for (int x = mx - xr - 5; x <= mx + xr + 5; x++) for (int z = cz - zr - 5; z <= cz + zr + 5; z++) for (int yy = y - 30; yy <= y + 30; yy++) {
            if (Math.abs(x - mx) <= 2 && z <= padZ + 2) continue;
            if (!w.air(x, yy, z)) return true;
        }
        return false;
    }

    static Ob find(W w, String id) {
        for (Ob o : w.obs) if (o.id.equals(id)) return o;
        return null;
    }
}
