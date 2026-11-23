package com.mcm.lobby;

/** Procedural trees and bushes (pure Java). (x,y,z) = first block above the ground. */
final class Trees {
    private Trees() {}

    static void blob(Draw d, double cx, double cy, double cz, double rx, double ry, double rz, String leaf, String leaf2, int salt) {
        d.ellipsoid(cx, cy, cz, rx, ry, rz, (x, y, z) -> {
            double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry, dz = (z + 0.5 - cz) / rz;
            double dd = dx * dx + dy * dy + dz * dz;
            if (dd > 0.72 && d.rnd(x, y, z, salt) < 0.35) return null; // ragged edge
            if (!d.isAir(x, y, z) && !d.id(x, y, z).endsWith("leaves")) return null;
            return leaf2 != null && d.rnd(x, y, z, salt + 1) < 0.3 ? leaf2 : leaf;
        });
    }

    static void oak(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(5 + 3 * s);
        for (int i = 0; i < h; i++) d.set(x, y + i, z, "oak_log[axis=y]");
        // a couple of branches
        for (int b = 0; b < 3; b++) {
            double a = d.rnd(x, b, z, salt + 5) * Math.PI * 2;
            int by = y + h - 3 + b;
            int bx = x + (int) Math.round(Math.cos(a) * 2), bz = z + (int) Math.round(Math.sin(a) * 2);
            d.line(x + 0.5, by, z + 0.5, bx + 0.5, by + 1, bz + 0.5, "oak_log[axis=y]");
            blob(d, bx + 0.5, by + 2, bz + 0.5, 2.6 + s, 2.0 + s * 0.6, 2.6 + s, Draw.leaves("oak"), Draw.leaves("azalea"), salt + b);
        }
        blob(d, x + 0.5, y + h + 0.5, z + 0.5, 3.0 + s * 1.3, 2.4 + s * 0.6, 3.0 + s * 1.3, Draw.leaves("oak"), null, salt + 9);
    }

    static void birch(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(7 + 4 * s);
        for (int i = 0; i < h; i++) d.set(x, y + i, z, "birch_log[axis=y]");
        blob(d, x + 0.5, y + h - 1.5, z + 0.5, 2.2 + s, 3.6 + s, 2.2 + s, Draw.leaves("birch"), null, salt);
    }

    static void spruce(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(10 + 8 * s);
        for (int i = 0; i < h; i++) d.set(x, y + i, z, "spruce_log[axis=y]");
        double maxR = 3.2 + 1.8 * s;
        for (int i = 2; i <= h + 1; i++) {
            double t = (i - 2) / (double) (h - 1);
            double r = maxR * (1 - t) * (0.75 + 0.25 * (((i + salt) % 3 == 0) ? 1 : 0.55));
            if (r < 0.6) r = 0.6;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz > r * r + 0.5) continue;
                if (dx == 0 && dz == 0 && i < h) continue;
                d.setIfAir(x + dx, y + i, z + dz, Draw.leaves("spruce"));
            }
        }
        d.setIfAir(x, y + h + 2, z, Draw.leaves("spruce"));
    }

    static void darkOak(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(6 + 3 * s);
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++)
            for (int i = 0; i < h; i++) d.set(x + dx, y + i, z + dz, "dark_oak_log[axis=y]");
        blob(d, x + 1, y + h + 0.5, z + 1, 4.5 + s * 1.5, 2.6 + s * 0.5, 4.5 + s * 1.5, Draw.leaves("dark_oak"), null, salt);
    }

    static void cherry(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(4 + 2 * s);
        for (int i = 0; i < h; i++) d.set(x, y + i, z, "cherry_log[axis=y]");
        for (int b = 0; b < 2; b++) {
            double a = d.rnd(x, b, z, salt) * Math.PI * 2 + b * Math.PI;
            double ex = x + 0.5 + Math.cos(a) * (3 + s), ez = z + 0.5 + Math.sin(a) * (3 + s);
            d.line(x + 0.5, y + h - 1, z + 0.5, ex, y + h + 2, ez, "cherry_log[axis=y]");
            blob(d, ex, y + h + 3.2, ez, 3.4 + s, 2.0 + s * 0.4, 3.4 + s, Draw.leaves("cherry"), null, salt + b + 3);
        }
        // pink petals underneath
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (d.rnd(x + dx, 0, z + dz, salt + 7) < 0.25 && d.isAir(x + dx, y, z + dz) && !d.isAir(x + dx, y - 1, z + dz)
                    && "grass_block".equals(d.id(x + dx, y - 1, z + dz)))
                d.set(x + dx, y, z + dz, "pink_petals[flower_amount=" + (1 + (int) (d.rnd(dx, 1, dz, salt) * 4)) + ",facing=north]");
        }
    }

    /** Palm: leaning curved trunk with drooping fronds. */
    static void palm(Draw d, int x, int y, int z, double s, int salt) {
        int h = (int) Math.round(7 + 4 * s);
        double a = d.rnd(x, 3, z, salt) * Math.PI * 2;
        double lx = Math.cos(a), lz = Math.sin(a);
        double px = x + 0.5, pz = z + 0.5;
        int ty = y;
        for (int i = 0; i < h; i++) {
            double t = i / (double) h;
            px = x + 0.5 + lx * t * t * 3.5;
            pz = z + 0.5 + lz * t * t * 3.5;
            ty = y + i;
            d.set((int) Math.floor(px), ty, (int) Math.floor(pz), i % 3 == 2 ? "stripped_jungle_log[axis=y]" : "jungle_log[axis=y]");
        }
        int cx = (int) Math.floor(px), cz = (int) Math.floor(pz), cy = ty + 1;
        d.set(cx, cy, cz, Draw.leaves("jungle"));
        d.set(cx, cy - 1, cz + 1, "cocoa[age=2,facing=north]");
        int fronds = 7;
        for (int f = 0; f < fronds; f++) {
            double fa = f * Math.PI * 2 / fronds + d.rnd(f, 1, salt, 2) * 0.4;
            double len = 4.5 + 1.5 * s;
            for (int k = 1; k <= len; k++) {
                double t = k / len;
                int fx = cx + (int) Math.round(Math.cos(fa) * k), fz = cz + (int) Math.round(Math.sin(fa) * k);
                int fy = cy + (int) Math.round(1.2 * Math.sin(t * Math.PI * 0.8) - 2.2 * t * t);
                d.setIfAir(fx, fy, fz, Draw.leaves("jungle"));
                if (k > 2 && k < len - 1) d.setIfAir(fx, fy - 1, fz, Draw.leaves(k % 2 == 0 ? "jungle" : "acacia"));
            }
        }
    }

    static void bush(Draw d, int x, int y, int z, double s, int salt) {
        String leaf = d.rnd(x, y, z, salt) < 0.5 ? Draw.leaves("azalea") : d.rnd(x, y, z, salt + 1) < 0.5 ? Draw.leaves("flowering_azalea") : Draw.leaves("oak");
        blob(d, x + 0.5, y + 0.3, z + 0.5, 1.4 + s, 1.0 + s * 0.5, 1.4 + s, leaf, Draw.leaves("flowering_azalea"), salt);
    }

    /** Random tree by biome flavour. */
    static void forestTree(Draw d, int x, int y, int z, double kind, double s, int salt) {
        if (kind < 0.30) oak(d, x, y, z, s, salt);
        else if (kind < 0.50) birch(d, x, y, z, s, salt);
        else if (kind < 0.68) darkOak(d, x, y, z, s, salt);
        else spruce(d, x, y, z, s, salt);
    }
}
