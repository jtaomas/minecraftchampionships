package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Staircase helper (LESSONS: first step sits ON the floor at floor+1, last step flush with the upper level) and
 * the matching walk validator.
 */
final class Stairs {
    private Stairs() {}

    /** One flight: first step at (x, floorY + 1, z), ascending along (dx, dz) for n steps. */
    record Lane(String name, int x, int z, int floorY, int dx, int dz, int n) {
        String dir() { return dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west"; }

        Lane rotated(int q) {
            return new Lane(name, Bs.rx(x, z, q), Bs.rz(x, z, q), floorY, Bs.rx(dx, dz, q), Bs.rz(dx, dz, q), n);
        }
    }

    /**
     * Build a flight into {@code t} (template coordinates): bottom-half stairs facing up the flight, solid fill
     * under each step down to the floor, 3 blocks of air above each step. Registers the lane.
     */
    static Lane flight(Tpl t, String name, String mat, String fill, int x, int z, int floorY, int dx, int dz, int n) {
        Lane l = new Lane(name, x, z, floorY, dx, dz, n);
        for (int i = 1; i <= n; i++) {
            int cx = x + dx * (i - 1), cz = z + dz * (i - 1), cy = floorY + i;
            for (int y = floorY; y < cy; y++) t.set(cx, y, cz, fill);
            t.set(cx, cy, cz, Bs.stairs(mat, l.dir(), false));
            for (int y = cy + 1; y <= cy + 3; y++) t.set(cx, y, cz, null);
        }
        t.lanes.add(new int[]{x, z, floorY, dx, dz, n});
        return l;
    }

    private static boolean free(MapModel.Blocks b, int x, int y, int z) { return Bs.passable(b.get(x, y, z)); }

    /** Walk every flight bottom to top; returns the problems found (empty = all walkable). */
    static List<String> validate(MapModel.Blocks b, List<Lane> lanes) {
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
                double top;
                if (s != null && Bs.id(s).endsWith("_stairs")) {
                    if (!"bottom".equals(Bs.prop(s, "half")) || !dir.equals(Bs.prop(s, "facing")))
                        errs.add(where + ": step " + i + " wrong orientation: " + s);
                    top = cy + 1;
                } else {
                    errs.add(where + ": step " + i + " missing (" + s + ")");
                    top = cy + 1;
                }
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
