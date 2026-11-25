package com.mcm.games.decisiondome;

/**
 * Geometry of one dial, in a local coordinate system where the dial centre is (0, 0).
 * Wedge 0 is centred on {@code baseAngle} (radians); wedge boundaries sit half a sector either side.
 */
public record DialGeometry(double floorY,
                           double innerRadius,
                           double outerRadius,
                           double baseAngle) {

    public DialGeometry {
        if (outerRadius <= innerRadius) outerRadius = innerRadius + 1.0;
    }

    public static DialGeometry of(double floorY, double innerRadius, double outerRadius, double baseAngle) {
        return new DialGeometry(floorY, innerRadius, outerRadius, baseAngle);
    }

    /** Default for the Nocturne Orrery dial: podium r 7.5, rim r 12, wedge 1 on the 90-degree entrance axis. */
    public static DialGeometry orrery() {
        return new DialGeometry(65.0, 7.5, 12.0, Math.PI / 2.0);
    }

    /** Default for the mcc_dome fallback: hub r 6, rim r 20. */
    public static DialGeometry fallback(int floorY) {
        return new DialGeometry(floorY, 6.0, 20.0, Math.PI / 2.0);
    }

    public double step(int wedges) { return Math.PI * 2.0 / Math.max(1, wedges); }

    public double centreAngle(int index, int wedges) {
        return baseAngle + index * step(wedges);
    }

    public double boundaryAngle(int index, int wedges) {
        return baseAngle - step(wedges) / 2.0 + index * step(wedges);
    }

    public double radius(double x, double z) { return Math.hypot(x, z); }

    public boolean inside(double x, double z) {
        double r = radius(x, z);
        return r >= innerRadius && r <= outerRadius;
    }

    /**
     * Slot at a local point. Returns -1 on the centre podium, outside the rim, or if the point is
     * exactly on a boundary and the caller asks for a strict read.
     */
    public int wedgeAt(double x, double z, int wedges) {
        if (wedges <= 0) return -1;
        double r = radius(x, z);
        if (r < innerRadius || r > outerRadius + 0.001) return -1;
        double angle = Math.atan2(z, x);
        double step = step(wedges);
        double rel = normalise(angle - boundaryAngle(0, wedges));
        int index = (int) Math.floor(rel / step);
        if (index < 0) index = 0;
        if (index >= wedges) index = wedges - 1;
        return index;
    }

    /** The sector centre in local coordinates. */
    public double[] centre(int index, int wedges, double radius) {
        double a = centreAngle(index, wedges);
        return new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
    }

    public static double normalise(double angle) {
        double a = angle % (Math.PI * 2.0);
        return a < 0 ? a + Math.PI * 2.0 : a;
    }
}
