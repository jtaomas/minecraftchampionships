package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Staircase helper (LESSONS: first step sits ON the floor at floor+1, last step flush with the upper level) and
 * the matching walk validator. Lanes are stored in world coordinates.
 */
final class Stairs {
    private Stairs() {}

    /** One flight: first step at (x, floorY + 1, z), ascending along (dx, dz) for n steps; width w to the right (+side). */
    record Lane(String name, int x, int z, int floorY, int dx, int dz, int n) {
        String dir() { return dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west"; }
    }

    /**
     * World-space flight: bottom-half stairs facing up the flight, solid fill under each step down to the floor,
     * air above each step. {@code wide} lanes side by side along the perpendicular (+x for z flights, +z for x flights).
     */
    static void flight(W w, String name, String mat, String fill, int x, int z, int floorY, int dx, int dz, int n, int wide) {
        int px = dz != 0 ? 1 : 0, pz = dx != 0 ? 1 : 0;
        for (int k = 0; k < wide; k++) {
            int lx = x + px * k, lz = z + pz * k;
            Lane l = new Lane(name + (wide > 1 ? "#" + k : ""), lx, lz, floorY, dx, dz, n);
            for (int i = 1; i <= n; i++) {
                int cx = lx + dx * (i - 1), cz = lz + dz * (i - 1), cy = floorY + i;
                for (int y = floorY; y < cy; y++) w.set(cx, y, cz, fill);
                w.set(cx, cy, cz, Bs.stairs(mat, l.dir(), false));
                for (int y = cy + 1; y <= cy + 3; y++) w.set(cx, y, cz, null);
                w.walk(cx, cy, cz);
            }
            w.lanes.add(l);
        }
    }

    private static boolean free(CourseModel.Blocks b, int x, int y, int z) { return Bs.passable(b.get(x, y, z)); }

    /** Walk every flight bottom to top; returns the problems found (empty = all walkable). */
    static List<String> validate(CourseModel.Blocks b, List<Lane> lanes) {
        List<String> errs = new ArrayList<>();
        for (Lane l : lanes) {
            String dir = l.dir();
            String where = l.name() + " @(" + l.x() + "," + (l.floorY() + 1) + "," + l.z() + ")" + dir;
            int ax = l.x() - l.dx(), az = l.z() - l.dz();
            double approach = Bs.standTop(b.get(ax, l.floorY(), az));
            if (approach < 0.99 || approach > 1.01) errs.add(where + ": no floor to step from at (" + ax + "," + l.floorY() + "," + az + ")");
            if (!free(b, ax, l.floorY() + 1, az) || !free(b, ax, l.floorY() + 2, az)) errs.add(where + ": approach blocked");
            if (Bs.standTop(b.get(l.x(), l.floorY(), l.z())) < 0.99) errs.add(where + ": first step not standing on the floor");
            double feet = l.floorY() + 1;
            for (int i = 1; i <= l.n(); i++) {
                int cx = l.x() + l.dx() * (i - 1), cz = l.z() + l.dz() * (i - 1), cy = l.floorY() + i;
                String s = b.get(cx, cy, cz);
                if (s != null && Bs.id(s).endsWith("_stairs")) {
                    if (!"bottom".equals(Bs.prop(s, "half")) || !dir.equals(Bs.prop(s, "facing")))
                        errs.add(where + ": step " + i + " wrong orientation: " + s);
                } else errs.add(where + ": step " + i + " missing (" + s + ")");
                double top = cy + 1;
                if (top - feet > 1.0001) errs.add(where + ": step " + i + " needs a jump");
                if (!free(b, cx, cy + 1, cz) || !free(b, cx, cy + 2, cz)) errs.add(where + ": no headroom above step " + i);
                feet = top;
            }
            int lx = l.x() + l.dx() * l.n(), lz = l.z() + l.dz() * l.n(), ly = l.floorY() + l.n();
            double land = Bs.standTop(b.get(lx, ly, lz));
            if (land < 0.99 || land > 1.01) errs.add(where + ": no landing flush with the last step at (" + lx + "," + ly + "," + lz + ")");
            if (!free(b, lx, ly + 1, lz) || !free(b, lx, ly + 2, lz)) errs.add(where + ": no headroom on the landing");
        }
        return errs;
    }
}
