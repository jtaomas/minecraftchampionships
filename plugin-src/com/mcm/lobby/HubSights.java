package com.mcm.lobby;

/** Side attractions (aquarium dome, volcano, lighthouse, rainbow arch, champions' shrine, beach), paths, balloons. */
final class HubSights {
    final HubGenerator g;
    final Draw d;
    final HubLayout L;

    /** Flatten pads used by the terrain: {x, z, radius, targetTopY}. */
    static final double[][] PADS = {
            {-92, 56, 20, 69},     // aquarium
            {118, 26, 24, 69},     // volcano foot
            {116, 118, 8, 67},     // lighthouse
            {-136, -14, 10, 86},   // champions' shrine
            {0, 140, 8, 66},       // rainbow arch
            {-50, 100, 9, 69},     // mascot
    };

    HubSights(HubGenerator g) { this.g = g; this.d = g.d; this.L = g.L; }

    void build() {
        aquarium(-92, 56);
        volcano(118, 26);
        lighthouse(116, 118);
        shrine(-136, -14);
        rainbow(0, 140);
        beach();
        paths();
    }

    int ground(int x, int z) { return g.hAt(x, z); }

    // ------------------------------------------------------------------------------------------------ aquarium
    void aquarium(int cx, int cz) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        int by = 69, R = 16;
        // base drum
        for (int x = cx - R - 3; x <= cx + R + 3; x++) for (int z = cz - R - 3; z <= cz + R + 3; z++) {
            double rr = Math.hypot(x + 0.5 - x0, z + 0.5 - z0);
            if (rr > R + 2.5) continue;
            for (int y = by - 4; y <= by; y++) d.set(x, y, z, "stone");
            if (rr > R + 0.5) {
                d.set(x, by + 1, z, rr > R + 1.5 ? "prismarine_brick_slab[type=bottom]" : "dark_prismarine");
                d.set(x, by, z, "prismarine_bricks");
            } else d.set(x, by, z, "sand");
            g.paved(x, z, by);
        }
        // glass dome with prismarine ribs
        for (int x = cx - R - 1; x <= cx + R + 1; x++) for (int z = cz - R - 1; z <= cz + R + 1; z++) for (int y = by + 1; y <= by + R + 2; y++) {
            double dx = x + 0.5 - x0, dy = y - (by + 1) + 0.5, dz = z + 0.5 - z0;
            double e = Math.sqrt(dx * dx + dy * dy * 1.3 + dz * dz);
            if (e > R + 0.5) continue;
            double a = Math.toDegrees(Math.atan2(dz, dx));
            boolean rib = Math.abs(((a % 30) + 30) % 30 - 15) > 15 - 25.0 / Math.max(3, Math.hypot(dx, dz));
            if (e > R - 0.8) {
                if (y <= by + 2) d.set(x, y, z, "dark_prismarine");
                else d.set(x, y, z, rib ? "prismarine_bricks" : (y > by + 11 ? "glass" : "light_blue_stained_glass"));
            } else if (y <= by + 9) {
                // water tank with a dry walk-through glass tunnel along X
                boolean tunnelShell = Math.abs(dz) <= 2.6 && y <= by + 4;
                boolean tunnelAir = Math.abs(dz) <= 1.6 && y <= by + 3;
                if (tunnelAir) d.set(x, y, z, null);
                else if (tunnelShell) d.set(x, y, z, "glass");
                else d.set(x, y, z, "water");
            } else d.set(x, y, z, null);
        }
        // tunnel floor + entrances
        for (int x = cx - R - 3; x <= cx + R + 3; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            d.set(x, by, z, z == cz ? "sea_lantern" : "prismarine_bricks");
            double rr = Math.abs(x + 0.5 - x0);
            if (rr > R - 1.0) for (int y = by + 1; y <= by + 3; y++) d.set(x, y, z, null);
        }
        // reef: coral, sea lanterns, kelp-like seagrass
        for (int x = cx - R; x <= cx + R; x++) for (int z = cz - R; z <= cz + R; z++) {
            double rr = Math.hypot(x + 0.5 - x0, z + 0.5 - z0);
            if (rr > R - 2 || Math.abs(z - cz) <= 3) continue;
            double r = d.rnd(x, 0, z, 81);
            String[] coral = {"brain_coral_block", "tube_coral_block", "fire_coral_block", "horn_coral_block", "bubble_coral_block"};
            if (r < 0.18) {
                int hgt = 1 + (int) (d.rnd(x, 1, z, 82) * 4);
                for (int y = by + 1; y <= by + hgt; y++) d.set(x, y, z, coral[(int) (d.rnd(x, y, z, 83) * 5)]);
            } else if (r < 0.22) d.set(x, by + 1, z, "sea_lantern");
            else if (r < 0.42) d.set(x, by + 1, z, "seagrass");
            else if (r < 0.46) { for (int y = by + 1; y <= by + 6; y++) d.set(x, y, z, y == by + 6 ? "kelp[age=25]" : "kelp_plant"); }
        }
        // a big block-fish swimming in the tank
        int fx = cx + 6, fy = by + 7, fz = cz + 7;
        d.fill(fx - 2, fy - 1, fz, fx + 2, fy + 1, fz, "orange_concrete");
        d.fill(fx - 1, fy - 2, fz, fx + 1, fy + 2, fz, "orange_concrete");
        d.set(fx + 3, fy, fz, "white_concrete"); d.set(fx - 3, fy + 1, fz, "orange_concrete"); d.set(fx - 3, fy - 1, fz, "orange_concrete");
        d.set(fx + 1, fy + 1, fz, "black_concrete"); d.set(fx, fy, fz, "white_concrete");
        L.label(x0, by + R + 5, z0, 0, "§b§lCORAL DOME §7aquarium");
    }

    // ------------------------------------------------------------------------------------------------ volcano
    void volcano(int cx, int cz) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        int base = 69, H = 46;
        double R = 34;
        HNoise nz = new HNoise(g.seed * 3 + 1);
        for (int x = cx - 40; x <= cx + 40; x++) for (int z = cz - 40; z <= cz + 40; z++) {
            double dx = x + 0.5 - x0, dz = z + 0.5 - z0;
            double rr = Math.hypot(dx, dz);
            double a = Math.atan2(dz, dx);
            double rj = R * (1 + 0.12 * nz.noise2(Math.cos(a) * 2, Math.sin(a) * 2)) + 3 * nz.noise2(x * 0.1, z * 0.1);
            if (rr > rj) continue;
            double t = rr / rj;
            int top = base + (int) Math.round(H * Math.pow(1 - t, 1.35) + 2 * nz.noise2(x * 0.15, z * 0.15));
            double crater = 7.5;
            boolean inCrater = rr < crater;
            int craterFloor = base + H - 8;
            if (inCrater) top = craterFloor;
            if (rr >= crater && rr < crater + 2.5) top = Math.max(top, base + H - 2);
            int gTop = g.hAt(x, z);
            if (top <= gTop && !inCrater) continue;
            for (int y = Math.min(gTop, base) - 1; y <= top; y++) {
                String b;
                // glowing lava streaks down two flanks
                double wig = a + 0.35 * nz.noise2(rr * 0.12, a * 2) ;
                double streak = Math.abs(Math.sin(wig * 2.5 + 0.7)) * (0.9 + 0.1 * nz.noise2(x * 0.3, z * 0.3));
                boolean inReach = rr > crater + 2 && rr < rj * (0.55 + 0.45 * Math.abs(Math.sin(a * 1.7)));
                boolean surface = y >= top - 2;               // also paint the exposed side of steep steps
                boolean core = inReach && surface && streak > 0.945 - 0.03 * (1 - t);
                boolean edge = inReach && surface && streak > 0.885 - 0.03 * (1 - t);
                if (core) b = y == top && d.rnd(x, y, z, 94) < 0.18 ? "shroomlight" : "orange_concrete";
                else if (edge) b = d.pick(x, y, z, 95, "magma_block", 3, "orange_terracotta", 1);
                else if (y == top) b = d.pick(x, y, z, 91, "basalt[axis=y]", 3, "blackstone", 2, "tuff", 1, "smooth_basalt", 1, "magma_block", 0.3);
                else b = d.pick(x, y, z, 92, "blackstone", 2, "basalt[axis=y]", 1, "stone", 1);
                d.set(x, y, z, b);
            }
            if (inCrater) {
                d.set(x, craterFloor, z, "magma_block");
                d.set(x, craterFloor + 1, z, rr < crater - 1.2 ? "lava" : "magma_block");
                for (int y = craterFloor + 2; y <= base + H + 2; y++) d.air(x, y, z);
                if (rr > crater - 2 && d.rnd(x, 0, z, 93) < 0.2) d.set(x, craterFloor + 2, z, "campfire[lit=true,signal_fire=true]");
            }
            g.paved(x, z, top);
        }
        // shroomlight glow vents on the flanks
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6 + 0.3;
            double rr = 12 + (k % 3) * 6;
            int x = (int) Math.floor(x0 + Math.cos(a) * rr), z = (int) Math.floor(z0 + Math.sin(a) * rr);
            int t = d.v.top(x, z, 140, 60);
            if (t > 60) d.set(x, t, z, "shroomlight");
        }
        L.label(x0, base + H + 10, z0, 0, "§6§lMOUNT EMBER");
        L.fireworkSpots.add(new double[]{x0, base + H + 20, z0});
    }

    // ------------------------------------------------------------------------------------------------ lighthouse
    void lighthouse(int cx, int cz) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        int by = 67;
        // rocky base
        for (int x = cx - 9; x <= cx + 9; x++) for (int z = cz - 9; z <= cz + 9; z++) {
            double rr = Math.hypot(x + 0.5 - x0, z + 0.5 - z0);
            if (rr > 8.5) continue;
            int top = by + (rr < 6.5 ? 1 : 0);
            for (int y = 58; y <= top; y++) d.set(x, y, z, d.pick(x, y, z, 95, "stone", 2, "cobblestone", 2, "mossy_cobblestone", 1, "andesite", 1));
            for (int y = top + 1; y <= top + 3; y++) d.air(x, y, z);
            g.paved(x, z, top);
        }
        int y0 = by + 2, y1 = by + 36;
        for (int y = y0; y <= y1; y++) {
            double rr = 4.6 - (y - y0) * 0.04;
            String b = ((y - y0) / 4) % 2 == 0 ? "red_concrete" : "white_concrete";
            final String bb = b;
            d.ring(x0, y, z0, rr - 1.2, rr, bb);
            if ((y - y0) % 8 == 5) { d.set(cx + 3, y, cz, "glass_pane"); d.set(cx - 4, y, cz, "glass_pane"); }
        }
        // gallery
        d.disc(x0, y1 + 1, z0, 5.6, "polished_blackstone");
        d.ring(x0, y1 + 2, z0, 4.6, 5.6, "iron_bars");
        // lamp room
        for (int y = y1 + 2; y <= y1 + 6; y++) {
            d.ring(x0, y, z0, 2.0, 3.1, y == y1 + 2 || y == y1 + 6 ? "white_concrete" : "glass");
            if (y > y1 + 2 && y < y1 + 6) d.disc(x0, y, z0, 1.6, y == y1 + 4 ? "sea_lantern" : "glowstone");
        }
        for (int y = y1 + 7; y <= y1 + 10; y++) d.disc(x0, y, z0, 3.4 - (y - y1 - 7) * 1.0, "red_concrete");
        d.set(cx, y1 + 11, cz, "lightning_rod");
        L.searchlights.add(new double[]{x0, y1 + 4, z0, 4.0});
        L.label(x0, y1 + 14, z0, 0, "§c§lHARBOUR LIGHT");
    }

    // ------------------------------------------------------------------------------------------------ shrine
    void shrine(int cx, int cz) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        int by = 86;
        for (int x = cx - 8; x <= cx + 8; x++) for (int z = cz - 8; z <= cz + 8; z++) {
            double rr = Math.hypot(x + 0.5 - x0, z + 0.5 - z0);
            if (rr > 7.5) continue;
            int gt = g.hAt(x, z);
            for (int y = Math.min(gt, by) - 2; y <= by; y++) d.set(x, y, z, "stone_bricks");
            for (int y = by + 1; y <= by + 12; y++) d.air(x, y, z);
            d.set(x, by, z, rr > 6.5 ? "polished_andesite" : rr < 2.6 ? "gold_block" : (Math.floorMod(x + z, 2) == 0 ? "smooth_quartz" : "quartz_bricks"));
            g.paved(x, z, by);
        }
        // pillars + domed roof
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            int x = (int) Math.floor(x0 + Math.cos(a) * 5.5), z = (int) Math.floor(z0 + Math.sin(a) * 5.5);
            for (int y = by + 1; y <= by + 7; y++) d.set(x, y, z, y == by + 1 || y == by + 7 ? "chiseled_quartz_block" : "quartz_pillar[axis=y]");
            d.set(x, by + 4, z + (z > cz ? 1 : -1), "lantern[hanging=false]".replace("false", "false"));
        }
        for (int y = by + 8; y <= by + 12; y++) {
            double rr = 6.5 - (y - by - 8) * 1.4;
            if (rr < 0.5) rr = 0.5;
            final double r2 = rr;
            d.disc(x0, y, z0, r2, y == by + 8 ? "gold_block" : (y % 2 == 0 ? "blue_concrete" : "blue_terracotta"));
        }
        d.set(cx, by + 13, cz, "gold_block");
        d.set(cx, by + 14, cz, "end_rod[facing=up]");
        // plinth with a golden trophy
        d.disc(x0, by + 1, z0, 1.6, "polished_blackstone");
        d.set(cx, by + 2, cz, "gold_block");
        d.set(cx, by + 3, cz, "gold_block");
        d.ring(x0, by + 4, z0, 0.5, 1.6, "gold_block");
        d.set(cx, by + 4, cz, "ochre_froglight");
        L.label(x0, by + 17, z0, 0, "§6§lHALL OF CHAMPIONS");
        L.fireworkSpots.add(new double[]{x0, by + 25, z0});
    }

    // ------------------------------------------------------------------------------------------------ rainbow arch
    void rainbow(int cx, int cz) {
        int by = 66;
        String[] bands = Draw.TEAM_DYES;
        for (int x = cx - 24; x <= cx + 24; x++) for (int y = by - 3; y <= by + 25; y++) {
            double rr = Math.hypot(x + 0.5 - (cx + 0.5), y - by);
            if (rr < 13.5 || rr > 23.5 || y < by - 2) continue;
            int bi = (int) Math.floor(23.5 - rr);
            bi = Math.max(0, Math.min(9, bi));
            for (int z = cz - 1; z <= cz + 1; z++) d.set(x, y, z, bands[bi] + (z == cz ? "_concrete" : "_terracotta"));
        }
        // cloud feet
        for (int s : new int[]{-1, 1}) {
            double fx = cx + 0.5 + s * 18.5;
            d.ellipsoid(fx, by, cz + 0.5, 6, 3.5, 4, (x, y, z) -> d.rnd(x, y, z, 97) < 0.15 ? "white_concrete_powder" : "white_wool");
            for (int x = (int) fx - 6; x <= fx + 6; x++) for (int z = cz - 4; z <= cz + 4; z++) g.reserve(x, z);
        }
        L.label(cx + 0.5, by + 27, cz + 0.5, 0, "§d§lR§c§lA§6§lI§e§lN§a§lB§b§lO§9§lW §5§lROAD");
    }

    // ------------------------------------------------------------------------------------------------ beach
    void beach() {
        // pier
        int px = 34, pz0 = 150;
        int y = HubLayout.SEA + 1;
        for (int z = pz0; z <= pz0 + 46; z++) for (int x = px - 2; x <= px + 2; x++) {
            int gt = g.hAt(x, z);
            if (gt > y) continue;
            d.set(x, y, z, Math.abs(x - px) == 2 ? "stripped_spruce_log[axis=z]" : "spruce_planks");
            if (Math.abs(x - px) == 2 && z % 5 == 0) {
                for (int yy = gt; yy < y; yy++) d.set(x, yy, z, "spruce_log[axis=y]");
                d.set(x, y + 1, z, "spruce_fence");
                d.set(x, y + 2, z, z % 10 == 0 ? "lantern[hanging=false]" : "spruce_fence");
            } else if (Math.abs(x - px) == 2) d.set(x, y + 1, z, "spruce_fence");
            g.paved(x, z, y);
        }
        // pier head deck
        for (int x = px - 5; x <= px + 5; x++) for (int z = pz0 + 46; z <= pz0 + 54; z++) {
            d.set(x, y, z, (x + z) % 3 == 0 ? "stripped_spruce_wood[axis=y]" : "spruce_planks");
            boolean rim = Math.abs(x - px) == 5 || z == pz0 + 54;
            if (rim) d.set(x, y + 1, z, "spruce_fence");
            if (rim && (x + z) % 4 == 0) d.set(x, y + 2, z, "lantern[hanging=false]");
            g.paved(x, z, y);
        }
        for (int yy = y + 1; yy <= y + 4; yy++) d.set(px, yy, pz0 + 50, "spruce_log[axis=y]");
        d.set(px, y + 5, pz0 + 50, "sea_lantern");
        // umbrellas + towels on the sand
        String[][] cols = {{"red", "white"}, {"blue", "yellow"}, {"orange", "white"}, {"lime", "light_blue"}, {"pink", "white"}, {"cyan", "yellow"}};
        int k = 0;
        for (int x = -90; x <= 90; x += 3) for (int z = 120; z <= 185; z += 3) {
            if (!g.inMap(x, z)) continue;
            if (g.surf[g.idx(x, z)] != 1 || g.isReserved(x, z)) continue;
            if (d.rnd(x, 0, z, 98) > 0.07) continue;
            int gt = g.hAt(x, z);
            if (gt < HubLayout.SEA + 1) continue;
            String[] c = cols[(k++) % cols.length];
            for (int yy = gt + 1; yy <= gt + 3; yy++) d.set(x, yy, z, "birch_fence");
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                boolean edge = Math.abs(dx) + Math.abs(dz) == 3 || Math.abs(dx) == 2 || Math.abs(dz) == 2;
                d.set(x + dx, gt + (edge ? 3 : 4), z + dz, ((dx + dz) & 1) == 0 ? c[0] + "_wool" : c[1] + "_wool");
            }
            // towel: each carpet on the sand of its own column (the beach slopes), never in mid-air or buried
            for (int dz = 1; dz <= 2; dz++) {
                int ty = g.hAt(x + 1, z + dz);
                if (Shapes.fullTop(d.v.get(x + 1, ty, z + dz)) && d.isAir(x + 1, ty + 1, z + dz)) d.set(x + 1, ty + 1, z + dz, c[0] + "_carpet");
            }
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) g.reserve(x + dx, z + dz);
        }
        // a beach bonfire ring
        int bx = -30, bz = 158;
        if (g.surf[g.idx(bx, bz)] == 1) {
            int gt = g.hAt(bx, bz);
            d.set(bx, gt + 1, bz, "campfire[lit=true,signal_fire=false]");
            for (int k2 = 0; k2 < 8; k2++) {
                double a = k2 * Math.PI / 4;
                int lx = (int) Math.floor(bx + 0.5 + Math.cos(a) * 3.2), lz = (int) Math.floor(bz + 0.5 + Math.sin(a) * 3.2);
                d.set(lx, g.hAt(lx, lz) + 1, lz, "stripped_oak_log[axis=" + (Math.abs(Math.cos(a)) > 0.7 ? "z" : "x") + "]");
            }
            g.reserveDisc(bx + 0.5, bz + 0.5, 4);
        }
    }

    // ------------------------------------------------------------------------------------------------ paths
    void paths() {
        // west: plaza -> aquarium -> shrine
        g.path(new double[]{-40, 80, -58, 74, -70, 60, -74, 57}, 4, "stone", 12);
        g.path(new double[]{-110, 56, -122, 40, -118, 20, -128, 4, -134, -6}, 3, "dirt", 14);
        // east: plaza -> cherry grove -> volcano
        g.path(new double[]{40, 80, 56, 76, 70, 62, 84, 46, 92, 38}, 4, "stone", 12);
        // south-east: plaza -> lighthouse
        g.path(new double[]{28, 109, 44, 122, 66, 128, 88, 124, 108, 120}, 3, "dirt", 14);
        // south: plaza -> rainbow -> beach -> pier
        g.path(new double[]{0, 121, 0, 132, 2, 148, 14, 158, 32, 156}, 5, "sand", 10);
        // south-west: plaza -> lagoon shore
        g.path(new double[]{-28, 109, -44, 118, -58, 112, -70, 104}, 3, "dirt", 14);
        // venue apron around to the back of the dome (service ring path)
        g.path(new double[]{-52, -6, -66, -40, -62, -84, -40, -112, 0, -122, 40, -112, 62, -84, 66, -40, 52, -6}, 3, "stone", 16);
    }

    // ------------------------------------------------------------------------------------------------ balloons
    void balloons() {
        double[][] B = {
                {-150, 140, -150, 0}, {170, 150, 40, 1}, {-60, 170, 210, 2}, {110, 132, -210, 3},
                {-230, 128, 70, 4}, {60, 160, 120, 5}, {-110, 175, -40, 6}, {220, 138, -110, 7}};
        String[][] cols = {
                {"red", "white"}, {"blue", "yellow"}, {"orange", "purple"}, {"lime", "white"},
                {"cyan", "pink"}, {"yellow", "red"}, {"magenta", "light_blue"}, {"green", "orange"}};
        for (double[] b : B) balloon((int) b[0], (int) b[1], (int) b[2], cols[(int) b[3]]);
    }

    void balloon(int cx, int cy, int cz, String[] c) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        double rx = 10.0, ry = 12.0;
        d.ellipsoid(x0, cy, z0, rx, ry, rx, (x, y, z) -> {
            double dx = x + 0.5 - x0, dy = (y + 0.5 - cy), dz = z + 0.5 - z0;
            // taper the bottom into a teardrop
            double taper = dy < 0 ? 1 + (-dy / ry) * 0.9 : 1;
            double e = Math.sqrt((dx * dx + dz * dz) * taper * taper / (rx * rx) + dy * dy / (ry * ry));
            if (e > 1.0) return null;
            if (e < 0.8) return null; // hollow
            double a = Math.toDegrees(Math.atan2(dz, dx));
            int gore = Math.floorMod((int) Math.floor(a / 30.0), 2);
            if (y == cy + 3 || y == cy - 3) return "white_wool";
            return gore == 0 ? c[0] + "_wool" : c[1] + "_wool";
        });
        // skirt + ropes + basket
        int sy = cy - 12;
        for (int k = 0; k < 4; k++) {
            int sx = cx + (k % 2 == 0 ? -1 : 1), sz = cz + (k < 2 ? -1 : 1);
            for (int y = sy - 4; y <= sy + 1; y++) d.set(sx, y, sz, "chain[axis=y]");
        }
        d.set(cx, sy - 2, cz, "campfire[lit=true,signal_fire=false]");
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            d.set(x, sy - 6, z, "spruce_planks");
            if (x != cx || z != cz) d.set(x, sy - 5, z, "barrel[facing=up]");
        }
        d.set(cx, sy - 5, cz, "air");
    }
}
