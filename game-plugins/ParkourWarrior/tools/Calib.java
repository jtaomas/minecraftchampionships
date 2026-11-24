package com.mcm.games.parkourwarrior.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Simulator calibration: synthetic jumps checked against the published 1.21.1 limits (research Part 1.2-1.4):
 * flat 4b possible / 5b impossible, 3+1 possible / 4+1 impossible, 4-1 possible / 5-1 not from a run-up,
 * single and double neos possible, triple neo impossible or near-impossible, 2bc head-hitter caps the gap.
 * Usage: java Calib  (prints worst/best windows).
 */
public final class Calib {
    static int fails = 0;

    public static void main(String[] a) {
        if (a.length > 0 && a[0].equals("trace")) {
            JumpSim.TRACE = true;
            JumpSim.Cfg c = new JumpSim.Cfg();
            c.aligns = 1;
            if (a.length > 1 && a[1].equals("vine")) { JumpSim.Res r = vineExit(2); System.out.println(r.worst + "/" + r.best); }
            else if (a.length > 2 && a[1].equals("st")) { JumpSim.Res r = straight(3, Integer.parseInt(a[2]), 0, 1, false, null); System.out.println(r.worst + "/" + r.best); }
            else if (a.length > 1) slime(Integer.parseInt(a[1]), Integer.parseInt(a[2])); else straight(3, 2, 0, 1, false, null);
            return;
        }
        System.out.println("== flat / up / down gaps (landing 1x1, W only, no late jump)");
        for (int L : new int[]{1, 3}) for (double dh : new double[]{0, 1, 0.5, -1, -2}) for (int g = 1; g <= 5; g++) {
            JumpSim.Res r = straight(L, g, dh, 1, false, null);
            System.out.printf(Locale.ROOT, "  takeoff %d long  gap %d  dh %+.1f  -> window %d/%d%n", L, g, dh, r.worst, r.best);
        }
        System.out.println("== with the late (coyote) jump allowed");
        for (double dh : new double[]{0, 1}) for (int g = 3; g <= 5; g++) {
            JumpSim.Res r = straight(3, g, dh, 1, true, null);
            System.out.printf(Locale.ROOT, "  takeoff 3 long  gap %d  dh %+.1f  -> window %d/%d%n", g, dh, r.worst, r.best);
        }
        System.out.println("== thin landings (gap 2 / 3 flat-ish, takeoff 3 long)");
        for (String s : new String[]{"glass_pane[east=false,north=false,south=false,west=false,waterlogged=false]",
                "glass_pane[east=true,north=false,south=false,west=true,waterlogged=false]",
                "oak_fence[east=false,north=false,south=false,west=false,waterlogged=false]",
                "cobblestone_wall[east=none,north=none,south=none,west=none,up=true,waterlogged=false]",
                "chain[axis=y,waterlogged=false]", "lantern[hanging=false,waterlogged=false]", "flower_pot",
                "end_rod[facing=up]", "oak_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]"})
            for (int g = 1; g <= 3; g++) {
                JumpSim.Res r = straight(3, g, 0, 1, false, s);
                System.out.printf(Locale.ROOT, "  %-28s gap %d -> window %d/%d%n", Bs.id(s), g, r.worst, r.best);
            }
        System.out.println("== neos (wall 3 high, landing 1x1 directly behind, takeoff 1 / 3 long)");
        for (int L : new int[]{1, 3}) for (int n = 1; n <= 3; n++) for (int dh : new int[]{0, -1}) {
            JumpSim.Res r = neo(L, n, dh, false);
            System.out.printf(Locale.ROOT, "  takeoff %d  %d-wall neo dh %d -> window %d/%d curved=%s %ddeg sw%d wall %d%n", L, n, dh, r.worst, r.best, r.curved, r.deg, r.sw, r.wallLen);
            JumpSim.Res r2 = neo(L, n, dh, true);
            System.out.printf(Locale.ROOT, "      (late allowed) -> window %d/%d%n", r2.worst, r2.best);
        }
        System.out.println("== head-hitter: ceiling at 2.0 / 2.5 bc over takeoff + gap");
        for (double bc : new double[]{2.0, 2.5}) for (int g = 1; g <= 3; g++) {
            JumpSim.Res r = hh(g, bc);
            System.out.printf(Locale.ROOT, "  bc %.1f gap %d -> window %d/%d%n", bc, g, r.worst, r.best);
        }
        System.out.println("== ladder catch (ladder on a wall, 3 rungs, bottom rung at takeoff level)");
        for (int g = 1; g <= 4; g++) {
            JumpSim.Res r = ladderCatch(g);
            System.out.printf(Locale.ROOT, "  gap %d -> window %d/%d%n", g, r.worst, r.best);
        }
        System.out.println("== ladder exit sideways to a block at the top rung level, gap g");
        for (int g = 1; g <= 3; g++) {
            JumpSim.Res r = ladderExit(g);
            System.out.printf(Locale.ROOT, "  gap %d -> window %d/%d %s%n", g, r.worst, r.best, r.note);
        }
        System.out.println("== vine exit sideways (vine on the block to the south)");
        for (int g = 1; g <= 3; g++) {
            JumpSim.Res r = vineExit(g);
            System.out.printf(Locale.ROOT, "  gap %d -> window %d/%d %s%n", g, r.worst, r.best, r.note);
        }
        System.out.println("== slime: drop d onto slime, bounce to a ledge h above the slime top, 2 forward");
        for (int d = 3; d <= 6; d++) for (int h = 1; h <= 4; h++) {
            JumpSim.Res r = slime(d, h);
            System.out.printf(Locale.ROOT, "  drop %d rise %d -> window %d/%d%n", d, h, r.worst, r.best);
        }
    }

    static W world() { return new W(1); }

    static Ob.Node node(Ob.T t, int[]... c) {
        Ob.Node n = new Ob.Node(t);
        for (int[] p : c) n.cells.add(p);
        return n;
    }

    static JumpSim.Res eval(W w, Ob.Node a, List<Ob.Node> t, boolean late) {
        Phys.World wd = new Phys.World(w, new HashMap<>());
        JumpSim.Kill k = new JumpSim.Kill(80, new ArrayList<>(), new ArrayList<>());
        JumpSim.Cfg cfg = new JumpSim.Cfg();
        cfg.allowLate = late;
        return JumpSim.eval(wd, a, t, k, cfg);
    }

    /** Takeoff L blocks long along +z ending at z = 0, landing at z = g + 1 (1 or len long), dh 0 / 0.5 / +-n. */
    static JumpSim.Res straight(int L, int g, double dh, int len, boolean late, String landing) {
        W w = world();
        List<int[]> ac = new ArrayList<>();
        for (int z = -L + 1; z <= 0; z++) { w.set(0, 100, z, "stone"); ac.add(new int[]{0, 100, z}); }
        int by = 100 + (int) Math.floor(dh);
        String st = landing != null ? landing : (dh % 1 != 0 ? "stone_slab[type=bottom,waterlogged=false]" : "stone");
        if (dh % 1 != 0) by = 100 + (int) Math.ceil(dh);
        List<int[]> bc = new ArrayList<>();
        for (int z = g + 1; z < g + 1 + len; z++) {
            w.set(0, by, z, st);
            if (landing != null) w.set(0, by - 1, z, "stone");
            bc.add(new int[]{0, by, z});
        }
        Ob.Node a = node(Ob.T.AREA, ac.toArray(new int[0][]));
        Ob.Node b = node(Ob.T.AREA, bc.toArray(new int[0][]));
        return eval(w, a, List.of(b), late);
    }

    static JumpSim.Res neo(int L, int n, int dh, boolean late) {
        W w = world();
        List<int[]> ac = new ArrayList<>();
        for (int z = -L + 1; z <= 0; z++) { w.set(0, 100, z, "stone"); ac.add(new int[]{0, 100, z}); }
        // wall n long (z 1..n), extending from x = 0 to x = -6 (open on the +x side), 3 high above the takeoff
        for (int z = 1; z <= n; z++) for (int x = -6; x <= 0; x++) for (int y = 100; y <= 103; y++) w.set(x, y, z, "stone_bricks");
        w.set(0, 100 + dh, n + 1, "stone");
        Ob.Node a = node(Ob.T.AREA, ac.toArray(new int[0][]));
        Ob.Node b = node(Ob.T.AREA, new int[]{0, 100 + dh, n + 1});
        return eval(w, a, List.of(b), late);
    }

    static JumpSim.Res hh(int g, double bc) {
        W w = world();
        List<int[]> ac = new ArrayList<>();
        for (int z = -2; z <= 0; z++) { w.set(0, 100, z, "stone"); ac.add(new int[]{0, 100, z}); }
        w.set(0, 100, g + 1, "stone");
        w.set(0, 100, g + 2, "stone");
        for (int z = -2; z <= g + 2; z++) {
            if (bc == 2.0) w.set(0, 103, z, "stone");
            else w.set(0, 103, z, "stone_slab[type=bottom,waterlogged=false]");
        }
        if (bc == 2.5) for (int z = -2; z <= g + 2; z++) { w.set(0, 103, z, null); w.set(0, 103, z, "stone_slab[type=top,waterlogged=false]"); }
        Ob.Node a = node(Ob.T.AREA, ac.toArray(new int[0][]));
        Ob.Node b = node(Ob.T.AREA, new int[]{0, 100, g + 1}, new int[]{0, 100, g + 2});
        return eval(w, a, List.of(b), false);
    }

    static JumpSim.Res ladderCatch(int g) {
        W w = world();
        List<int[]> ac = new ArrayList<>();
        for (int z = -2; z <= 0; z++) { w.set(0, 100, z, "stone"); ac.add(new int[]{0, 100, z}); }
        int lz = g + 1;
        for (int y = 98; y <= 106; y++) w.set(0, y, lz + 1, "stone_bricks");
        Ob.Node b = new Ob.Node(Ob.T.LADDER);
        for (int y = 101; y <= 103; y++) { w.set(0, y, lz, "ladder[facing=north,waterlogged=false]"); b.cells.add(new int[]{0, y, lz}); }
        Ob.Node a = node(Ob.T.AREA, ac.toArray(new int[0][]));
        return eval(w, a, List.of(b), false);
    }

    static JumpSim.Res ladderExit(int g) {
        W w = world();
        for (int y = 98; y <= 105; y++) w.set(0, y, 1, "stone_bricks");
        Ob.Node a = new Ob.Node(Ob.T.LADDER);
        for (int y = 100; y <= 104; y++) { w.set(0, y, 0, "ladder[facing=north,waterlogged=false]"); a.cells.add(new int[]{0, y, 0}); }
        // target to the side (+x) at the top rung level
        w.set(g + 1, 104, 0, "stone");
        w.set(g + 2, 104, 0, "stone");
        Ob.Node b = node(Ob.T.AREA, new int[]{g + 1, 104, 0}, new int[]{g + 2, 104, 0});
        return eval(w, a, List.of(b), false);
    }

    static JumpSim.Res vineExit(int g) {
        W w = world();
        for (int y = 98; y <= 105; y++) w.set(0, y, 1, "stone_bricks");
        Ob.Node a = new Ob.Node(Ob.T.LADDER);
        for (int y = 100; y <= 104; y++) { w.set(0, y, 0, "vine[east=false,north=false,south=true,up=false,west=false]"); a.cells.add(new int[]{0, y, 0}); }
        w.set(g + 1, 104, 0, "stone");
        w.set(g + 2, 104, 0, "stone");
        Ob.Node b = node(Ob.T.AREA, new int[]{g + 1, 104, 0}, new int[]{g + 2, 104, 0});
        return eval(w, a, List.of(b), false);
    }

    static JumpSim.Res slime(int d, int h) {
        W w = world();
        List<int[]> ac = new ArrayList<>();
        for (int z = -2; z <= 0; z++) { w.set(0, 100, z, "stone"); ac.add(new int[]{0, 100, z}); }
        Ob.Node s = new Ob.Node(Ob.T.SLIME);
        for (int z = 2; z <= 4; z++) for (int x = -1; x <= 1; x++) { w.set(x, 100 - d, z, "slime_block"); s.cells.add(new int[]{x, 100 - d, z}); }
        int by = 100 - d + h;
        w.set(0, by, 7, "stone"); w.set(0, by, 8, "stone");
        Ob.Node b = node(Ob.T.AREA, new int[]{0, by, 7}, new int[]{0, by, 8});
        Ob.Node a = node(Ob.T.AREA, ac.toArray(new int[0][]));
        return eval(w, a, List.of(s, b), false);
    }
}
