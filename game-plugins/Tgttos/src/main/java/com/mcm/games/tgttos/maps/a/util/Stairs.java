package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Staircase helper (LESSONS: first step sits ON the floor at floor+1, last step flush with the upper level)
 * and the matching walk validator.
 */
public final class Stairs {
    private Stairs() {}

    /** One flight: first step at (x, floorY + 1, z), ascending along (dx, dz) for n steps, w wide (to the right of travel). */
    public record Flight(String name, int x, int z, int floorY, int dx, int dz, int n, int w) {
        public String dir() { return dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west"; }
        /** Sideways unit vector for the width. */
        int sx() { return dz != 0 ? 1 : 0; }
        int sz() { return dx != 0 ? 1 : 0; }
    }

    /**
     * Build a flight: bottom-half stairs facing up the flight, solid fill under each step down to the floor,
     * 3 blocks of air above each step. Registers it in the design.
     */
    public static Flight flight(Gen g, String name, String mat, String fill, int x, int z, int floorY, int dx, int dz, int n, int w) {
        Flight f = new Flight(name, x, z, floorY, dx, dz, n, w);
        for (int k = 0; k < w; k++) {
            int ox = x + f.sx() * k, oz = z + f.sz() * k;
            for (int i = 1; i <= n; i++) {
                int cx = ox + dx * (i - 1), cz = oz + dz * (i - 1), cy = floorY + i;
                for (int y = floorY; y < cy; y++) if (fill != null) g.set(cx, y, cz, fill);
                g.set(cx, cy, cz, Bs.stairs(mat, f.dir(), false));
                for (int y = cy + 1; y <= cy + 3; y++) g.set(cx, y, cz, null);
            }
        }
        g.design.flights.add(f);
        return f;
    }

    private static boolean free(TgMapData.Blocks b, int x, int y, int z) { return Bs.passable(b.get(x, y, z)); }

    /** Walk every flight (each column of its width) bottom to top; returns the problems found. */
    public static List<String> validate(TgMapData.Blocks b, List<Flight> flights) {
        List<String> errs = new ArrayList<>();
        for (Flight f : flights) {
            String dir = f.dir();
            for (int k = 0; k < f.w(); k++) {
                int ox = f.x() + f.sx() * k, oz = f.z() + f.sz() * k;
                String where = f.name() + "[" + k + "] @(" + ox + "," + (f.floorY() + 1) + "," + oz + ")" + dir;
                int ax = ox - f.dx(), az = oz - f.dz();
                double approach = Bs.standTop(b.get(ax, f.floorY(), az));
                if (approach < 0.99 || approach > 1.01) errs.add(where + ": no floor to step from at (" + ax + "," + f.floorY() + "," + az + ")");
                if (!free(b, ax, f.floorY() + 1, az) || !free(b, ax, f.floorY() + 2, az)) errs.add(where + ": approach blocked");
                if (Bs.standTop(b.get(ox, f.floorY(), oz)) < 0.99) errs.add(where + ": first step not standing on the floor");
                double feet = f.floorY() + 1;
                for (int i = 1; i <= f.n(); i++) {
                    int cx = ox + f.dx() * (i - 1), cz = oz + f.dz() * (i - 1), cy = f.floorY() + i;
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
                int lx = ox + f.dx() * f.n(), lz = oz + f.dz() * f.n(), ly = f.floorY() + f.n();
                double land = Bs.standTop(b.get(lx, ly, lz));
                if (land < 0.99 || land > 1.01) errs.add(where + ": no landing flush with the last step at (" + lx + "," + ly + "," + lz + ")");
                if (!free(b, lx, ly + 1, lz) || !free(b, lx, ly + 2, lz)) errs.add(where + ": no headroom on the landing");
            }
        }
        return errs;
    }
}
