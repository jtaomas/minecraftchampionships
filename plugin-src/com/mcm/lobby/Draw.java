package com.mcm.lobby;

/** Pure-Java drawing helpers over a {@link Vox}. Block strings may omit the "minecraft:" namespace. */
class Draw {
    final Vox v;
    final long seed;

    Draw(Vox v, long seed) { this.v = v; this.seed = seed; }

    // ---------------------------------------------------------------- basics
    void set(int x, int y, int z, String b) { v.set(x, y, z, b); }
    void setIfAir(int x, int y, int z, String b) { v.setIfAir(x, y, z, b); }
    void air(int x, int y, int z) { v.set(x, y, z, null); }
    boolean isAir(int x, int y, int z) { return v.air(x, y, z); }
    String id(int x, int y, int z) { return v.id(x, y, z); }

    double rnd(int x, int y, int z, int salt) { return HNoise.hash01(x, y, z, seed * 31 + salt); }

    /** Weighted pick: args "block", weight, "block", weight ... using a positional hash. */
    String pick(int x, int y, int z, int salt, Object... bw) {
        double tot = 0;
        for (int i = 1; i < bw.length; i += 2) tot += ((Number) bw[i]).doubleValue();
        double r = rnd(x, y, z, salt) * tot;
        for (int i = 0; i < bw.length; i += 2) {
            r -= ((Number) bw[i + 1]).doubleValue();
            if (r < 0) return (String) bw[i];
        }
        return (String) bw[bw.length - 2];
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) v.set(x, y, z, b);
    }

    interface Src { String at(int x, int y, int z); }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, Src s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    String b = s.at(x, y, z);
                    if (b != null) v.set(x, y, z, b.equals("air") ? null : b);
                }
    }

    void clear(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, (String) null); }

    /** Filled disc in the XZ plane (centre on block corners when cx/cz are .0 ; use .5 for block centres). */
    void disc(double cx, int y, double cz, double r, String b) {
        int r0 = (int) Math.ceil(r) + 1;
        for (int x = (int) Math.floor(cx - r0); x <= cx + r0; x++)
            for (int z = (int) Math.floor(cz - r0); z <= cz + r0; z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= r * r) v.set(x, y, z, b);
            }
    }

    void disc(double cx, int y, double cz, double r, Src s) {
        int r0 = (int) Math.ceil(r) + 1;
        for (int x = (int) Math.floor(cx - r0); x <= cx + r0; x++)
            for (int z = (int) Math.floor(cz - r0); z <= cz + r0; z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= r * r) { String b = s.at(x, y, z); if (b != null) v.set(x, y, z, b.equals("air") ? null : b); }
            }
    }

    /** Ring (annulus) rIn < d <= rOut. */
    void ring(double cx, int y, double cz, double rIn, double rOut, String b) {
        int r0 = (int) Math.ceil(rOut) + 1;
        for (int x = (int) Math.floor(cx - r0); x <= cx + r0; x++)
            for (int z = (int) Math.floor(cz - r0); z <= cz + r0; z++) {
                double dx = x + 0.5 - cx, dz = z + 0.5 - cz, d = Math.sqrt(dx * dx + dz * dz);
                if (d > rIn && d <= rOut) v.set(x, y, z, b);
            }
    }

    void cyl(double cx, int y1, int y2, double cz, double r, String b) { for (int y = y1; y <= y2; y++) disc(cx, y, cz, r, b); }

    void ellipsoid(double cx, double cy, double cz, double rx, double ry, double rz, Src s) {
        for (int x = (int) Math.floor(cx - rx - 1); x <= cx + rx + 1; x++)
            for (int y = (int) Math.floor(cy - ry - 1); y <= cy + ry + 1; y++)
                for (int z = (int) Math.floor(cz - rz - 1); z <= cz + rz + 1; z++) {
                    double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry, dz = (z + 0.5 - cz) / rz;
                    if (dx * dx + dy * dy + dz * dz <= 1.0) {
                        String b = s.at(x, y, z);
                        if (b != null) v.set(x, y, z, b.equals("air") ? null : b);
                    }
                }
    }

    void sphere(double cx, double cy, double cz, double r, String b) { ellipsoid(cx, cy, cz, r, r, r, (x, y, z) -> b); }

    void line(double x1, double y1, double z1, double x2, double y2, double z2, String b) {
        double len = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        int n = Math.max(1, (int) Math.ceil(len * 1.5));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            v.set((int) Math.floor(x1 + (x2 - x1) * t), (int) Math.floor(y1 + (y2 - y1) * t), (int) Math.floor(z1 + (z2 - z1) * t), b);
        }
    }

    void thickLine(double x1, double y1, double z1, double x2, double y2, double z2, double r, String b) {
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int n = Math.max(1, (int) Math.ceil(len * 2));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double px = x1 + (x2 - x1) * t, py = y1 + (y2 - y1) * t, pz = z1 + (z2 - z1) * t;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) for (int dy = -ri; dy <= ri; dy++) for (int dz = -ri; dz <= ri; dz++)
                if (dx * dx + dy * dy + dz * dz <= r * r + 0.3)
                    v.set((int) Math.floor(px) + dx, (int) Math.floor(py) + dy, (int) Math.floor(pz) + dz, b);
        }
    }

    // ---------------------------------------------------------------- block-state helpers
    static String stairs(String mat, String facing, boolean top) { return mat + "_stairs[facing=" + facing + ",half=" + (top ? "top" : "bottom") + "]"; }
    static String slab(String mat, boolean top) { return mat + "_slab[type=" + (top ? "top" : "bottom") + "]"; }
    static String log(String wood, char axis) { return wood + "_log[axis=" + axis + "]"; }
    static String leaves(String wood) { return wood + "_leaves[persistent=true]"; }

    /** Horizontal facing pointing from (x,z) toward (tx,tz). */
    static String facingToward(double x, double z, double tx, double tz) {
        double dx = tx - x, dz = tz - z;
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? "east" : "west";
        return dz > 0 ? "south" : "north";
    }

    static String opposite(String f) {
        return switch (f) { case "north" -> "south"; case "south" -> "north"; case "east" -> "west"; default -> "east"; };
    }

    /** Team colours in rainbow (TeamScoring) order, as dye names. */
    static final String[] TEAM_DYES = {"red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "pink"};
    /** 12 pad colours. */
    static final String[] PAD_DYES = {"red", "orange", "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink", "white"};

    // ---------------------------------------------------------------- props
    /** Lamp post: a wall/fence post with a lantern on top (or hanging). Returns top y. */
    void lampPost(int x, int y, int z, int h, String post, boolean hanging) {
        set(x, y, z, "polished_andesite");
        for (int i = 1; i <= h; i++) set(x, y + i, z, post);
        if (hanging) {
            set(x, y + h + 1, z, "polished_blackstone_slab[type=bottom]");
            set(x + 1, y + h, z, "chain[axis=y]");
            set(x + 1, y + h - 1, z, "lantern[hanging=true]");
            set(x - 1, y + h, z, "chain[axis=y]");
            set(x - 1, y + h - 1, z, "lantern[hanging=true]");
            set(x + 1, y + h + 1, z, "polished_blackstone_slab[type=bottom]");
            set(x - 1, y + h + 1, z, "polished_blackstone_slab[type=bottom]");
        } else {
            set(x, y + h + 1, z, "lantern[hanging=false]");
        }
    }

    /** Brazier: pedestal with a lit campfire on top. */
    void brazier(int x, int y, int z, boolean soul) {
        set(x, y, z, "polished_blackstone");
        set(x, y + 1, z, "chiseled_polished_blackstone");
        set(x, y + 2, z, "gold_block");
        set(x, y + 3, z, (soul ? "soul_campfire" : "campfire") + "[lit=true,signal_fire=false]");
        set(x + 1, y + 2, z, "polished_blackstone_stairs[facing=west,half=top]");
        set(x - 1, y + 2, z, "polished_blackstone_stairs[facing=east,half=top]");
        set(x, y + 2, z + 1, "polished_blackstone_stairs[facing=north,half=top]");
        set(x, y + 2, z - 1, "polished_blackstone_stairs[facing=south,half=top]");
    }

    /** Flag pole with a wool flag pointing +X (or -X when flip). */
    void flag(int x, int y, int z, int h, String dye, boolean alongX, boolean flip) {
        for (int i = 0; i < h; i++) set(x, y + i, z, "white_concrete".equals(dye) ? "iron_bars" : "end_rod[facing=up]");
        set(x, y + h, z, "gold_block");
        int fh = 3, fw = 5;
        for (int r = 0; r < fh; r++) for (int c = 1; c <= fw - (r == 1 ? 0 : 1); c++) {
            int dx = alongX ? (flip ? -c : c) : 0, dz = alongX ? 0 : (flip ? -c : c);
            set(x + dx, y + h - 1 - r, z + dz, dye + "_wool");
        }
    }
}
