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
        return build(seed, ImportedCourse.load(importDir)).model;
    }

    static W build(long seed) { return build(seed, null); }

    /** Full build (model + generator bookkeeping, used by the offline tools); ic = imported sections or null. */
    static W build(long seed, ImportedCourse ic) {
        W w = new W(seed);
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
            Frame.spurEnd(w, bx, by, bz, 1, 0);
            // main line along +z
            int mx = cx, my = cy, mz = cz + r - 1;
            for (int i = 1; i <= 3; i++) {
                Ob o = make(w, ic, mains[s - 1][i - 1], new Ob.Slot(CourseModel.mainId(s, i), CourseModel.Kind.MAIN, s, i, null, mx, my, mz, 0));
                mx = o.wx(o.endX, o.endZ); my = o.wy(o.endY); mz = o.wz(o.endX, o.endZ);
            }
            cx = mx; cy = my; cz = mz + (s < 5 ? HUB_R - 1 : 7);
        }
        // hubs are built after the obstacles so their decks fit around the pads
        for (int s = 1; s <= 5; s++) {
            int[] h = hubs.get(s - 1);
            Frame.hub(w, h[0], h[1], h[2], h[3], ZONE_BANNER[s - 1], s == 1);
        }
        int fx = cx, fy = cy, fz = cz;
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
            Frame.shrine(w, ex, ey, ez, Frame.GATE_COL[d]);
        }
        Backdrop.build(w, hubs, new int[]{fx, fy, fz}, finish);

        // ---- model
        for (int s = 1; s <= 5; s++) {
            int[] h = hubs.get(s - 1);
            CourseModel.Pos resp = CourseModel.Pos.block(h[0], h[1] + 1, h[2] + 4, 0f);
            CourseModel.Pos bonus = CourseModel.Pos.block(h[0] + h[3] - 1, h[1] + 1, h[2], 270f);
            m.hubs.add(new CourseModel.Hub(s, resp, bonus, ZONES[s - 1]));
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(h[0] + 0.5, h[1] + 5.2, h[2] + h[3] - 1.5, 0f),
                    ZONE_COLOUR[s - 1] + "&lStage " + s + ": " + ZONES[s - 1]));
            m.labels.add(new CourseModel.Label(new CourseModel.Pos(h[0] + h[3] + 0.5, h[1] + 8.3, h[2] + 0.5, 270f), "&d&lBONUS PATH &7>>"));
        }
        // start spawns: a ring on the start island avoiding the statue and the columns
        int[] h1 = hubs.get(0);
        for (int dx = -6; dx <= 6; dx += 2) for (int dz = -6; dz <= 6; dz += 2) {
            double d = Math.hypot(dx, dz);
            if (d < 3.5 || d > 7.2 || (Math.abs(dx) == 6 && Math.abs(dz) == 6)) continue;
            if (Math.abs(Math.abs(dx) - 5) <= 1 && Math.abs(Math.abs(dz) - 5) <= 1) continue;
            m.spawns.add(CourseModel.Pos.block(h1[0] + dx, h1[1] + 1, h1[2] + dz, 0f));
        }
        m.fork = CourseModel.Pos.block(fx, fy + 1, fz + 2, 0f);
        for (int d = 0; d < 3; d++) {
            Ob last = find(w, CourseModel.endingId(D[d], 2));
            m.finishes.put(D[d], last.padBox);
            m.finishStands.put(D[d], CourseModel.Pos.block(finish[d][0], finish[d][1] + 1, finish[d][2] - 2, 180f));
            String[] txt = {"&a&lEASY &a★ &fMeadow & Beehive", "&6&lMEDIUM &6★★ &fFrozen Harbour", "&c&lHARD &c★★★ &fLibrary Tower"};
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
        Ob o = ic == null ? null : ic.make(w, s);
        return o != null ? o : builtIn.make(w, s);
    }

    static Ob find(W w, String id) {
        for (Ob o : w.obs) if (o.id.equals(id)) return o;
        return null;
    }
}
