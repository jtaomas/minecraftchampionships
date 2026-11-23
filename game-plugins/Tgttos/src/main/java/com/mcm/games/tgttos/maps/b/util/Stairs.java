package com.mcm.games.tgttos.maps.b.util;

import com.mcm.games.tgttos.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Staircase helper (LESSONS: the first step sits ON the floor at floor+1, the last step is flush with the upper
 * level) and the matching walk validator. Adapted from the Sky Blockle helper.
 */
public final class Stairs {
    private Stairs() {}

    /** One flight: first step at (x, floorY + 1, z), ascending along (dx, dz) for n steps, w wide (to the right). */
    public record Lane(String name, int x, int z, int floorY, int dx, int dz, int n, int w) {
        public String dir() { return dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west"; }
        /** Lateral unit (perpendicular to the flight) used for the width. */
        int lx() { return dz != 0 ? 1 : 0; }
        int lz() { return dx != 0 ? 1 : 0; }
    }

    /**
     * Build a flight: bottom-half stairs facing up the flight, solid fill under each step down to {@code fillDown}
     * (or the floor), 3 blocks of air above each step. Registers the lane in {@code g}.
     */
    public static Lane flight(G g, String name, String mat, String fill, int x, int z, int floorY,
                              int dx, int dz, int n, int w, int fillDown) {
        Lane l = new Lane(name, x, z, floorY, dx, dz, n, w);
        for (int k = 0; k < w; k++) {
            int ox = l.lx() * k, oz = l.lz() * k;
            for (int i = 1; i <= n; i++) {
                int cx = x + ox + dx * (i - 1), cz = z + oz + dz * (i - 1), cy = floorY + i;
                for (int y = Math.min(fillDown, floorY); y < cy; y++) g.set(cx, y, cz, fill);
                g.set(cx, cy, cz, Bs.stairs(mat, l.dir(), false));
                for (int y = cy + 1; y <= cy + 3; y++) g.set(cx, y, cz, null);
            }
        }
        g.lanes.add(l);
        return l;
    }

    private static boolean free(TgMapData.Blocks b, int x, int y, int z) { return Bs.passable(b.get(x, y, z)); }

    /** Walk every flight bottom to top along each of its columns; returns the problems found (empty = all OK). */
    public static List<String> validate(TgMapData.Blocks b, List<Lane> lanes) {
        List<String> errs = new ArrayList<>();
        for (Lane l : lanes) {
            String dir = l.dir();
            for (int k = 0; k < l.w(); k++) {
                int x0 = l.x() + l.lx() * k, z0 = l.z() + l.lz() * k;
                String where = l.name() + "#" + k + " @(" + x0 + "," + (l.floorY() + 1) + "," + z0 + ")" + dir;
                int ax = x0 - l.dx(), az = z0 - l.dz();
                double approach = Bs.standTop(b.get(ax, l.floorY(), az));
                if (approach < 0.99 || approach > 1.01) errs.add(where + ": no floor to step from at (" + ax + "," + l.floorY() + "," + az + ")");
                if (!free(b, ax, l.floorY() + 1, az) || !free(b, ax, l.floorY() + 2, az)) errs.add(where + ": approach blocked");
                if (Bs.standTop(b.get(x0, l.floorY(), z0)) < 0.99) errs.add(where + ": first step not standing on the floor");
                double feet = l.floorY() + 1;
                for (int i = 1; i <= l.n(); i++) {
                    int cx = x0 + l.dx() * (i - 1), cz = z0 + l.dz() * (i - 1), cy = l.floorY() + i;
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
                int lx = x0 + l.dx() * l.n(), lz = z0 + l.dz() * l.n(), ly = l.floorY() + l.n();
                double land = Bs.standTop(b.get(lx, ly, lz));
                if (land < 0.99 || land > 1.01) errs.add(where + ": no landing flush with the last step at (" + lx + "," + ly + "," + lz + ")");
                if (!free(b, lx, ly + 1, lz) || !free(b, lx, ly + 2, lz)) errs.add(where + ": no headroom on the landing");
            }
        }
        return errs;
    }
}
