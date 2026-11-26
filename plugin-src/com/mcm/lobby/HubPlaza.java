package com.mcm.lobby;

/** Arrival plaza (emblem mosaic, rings, hedges, lamps), leaderboard wall, team pads, banner gate, red-carpet approach, statues. */
final class HubPlaza {
    final HubGenerator g;
    final Draw d;
    final HubLayout L;
    static final int PX = HubGenerator.PLAZA_X, PZ = HubGenerator.PLAZA_Z, PR = HubGenerator.PLAZA_R;
    static final int Y = HubGenerator.GROUND;          // 70
    static final int AY = HubGenerator.APPROACH_Y;     // 73
    static final int GATE_Z = 33;

    HubPlaza(HubGenerator g) { this.g = g; this.d = g.d; this.L = g.L; }

    static double pr(int x, int z) { return Math.hypot(x + 0.5 - (PX + 0.5), z + 0.5 - (PZ + 0.5)); }
    static double pa(int x, int z) { double a = Math.toDegrees(Math.atan2(z - PZ, x - PX)); return a < 0 ? a + 360 : a; }

    /** Openings in the hedge ring (degrees, half-width). 270 = north (gate). */
    static final double[][] OPENINGS = {{270, 34}, {90, 7}, {0, 6}, {180, 6}, {45, 5}, {135, 5}};

    static boolean opening(double a) {
        for (double[] o : OPENINGS) {
            double diff = Math.abs(((a - o[0]) % 360 + 540) % 360 - 180);
            if (diff < o[1]) return true;
        }
        return false;
    }

    void build() {
        plaza();
        leaderboard();
        teamPads();
        approach();
        gate();
        statues();
        mascot();
        L.spawnX = PX; L.spawnY = Y + 1; L.spawnZ = PZ + 28; L.spawnYaw = 180f;
        L.label(PX + 0.5, Y + 5.2, PZ + 17.5, 0,
                "§6§l✦ MINECRAFT CHAMPIONSHIPS ✦\n§e← LEADERBOARD   §8┃   §d↑ DECISION DOME   §8┃   §bTEAM SELECTION →");
        L.fireworkSpots.add(new double[]{PX + 0.5, Y + 40, PZ + 0.5});
        L.fireworkSpots.add(new double[]{PX - 25.5, Y + 35, GATE_Z});
        L.fireworkSpots.add(new double[]{PX + 25.5, Y + 35, GATE_Z});
    }

    // ------------------------------------------------------------------------------------------------ plaza floor
    void plaza() {
        for (int x = PX - PR - 2; x <= PX + PR + 2; x++) for (int z = PZ - PR - 2; z <= PZ + PR + 2; z++) {
            double rr = pr(x, z);
            if (rr > PR + 0.5) continue;
            double a = pa(x, z);
            for (int y = Y - 4; y < Y; y++) d.set(x, y, z, "stone");
            for (int y = Y + 1; y <= Y + 12; y++) d.air(x, y, z);
            d.set(x, Y, z, floor(x, z, rr, a));
            g.paved(x, z, Y);
            // hedge ring with openings
            if (rr > PR - 2.6 && !opening(a)) {
                boolean curb = rr <= PR - 1.9;
                d.set(x, Y, z, "stone_bricks");
                if (curb) { d.set(x, Y + 1, z, "stone_brick_slab[type=bottom]"); continue; }
                d.set(x, Y + 1, z, Draw.leaves(d.rnd(x, 1, z, 61) < 0.3 ? "azalea" : "oak"));
                d.set(x, Y + 2, z, Draw.leaves(d.rnd(x, 2, z, 62) < 0.2 ? "flowering_azalea" : d.rnd(x, 2, z, 63) < 0.3 ? "azalea" : "oak"));
                if (d.rnd(x, 3, z, 64) < 0.35) d.set(x, Y + 3, z, Draw.leaves("flowering_azalea"));
            }
        }
        // lamp posts
        for (int k = 0; k < 24; k++) {
            double a = k * 15.0 + 7.5;
            if (opening(a) || opening(a + 4) || opening(a - 4)) continue;
            int x = (int) Math.floor(PX + 0.5 + Math.cos(Math.toRadians(a)) * (PR - 4.0));
            int z = (int) Math.floor(PZ + 0.5 + Math.sin(Math.toRadians(a)) * (PR - 4.0));
            d.set(x, Y, z, "polished_andesite");
            d.set(x, Y + 1, z, "polished_blackstone_wall");
            d.set(x, Y + 2, z, "polished_blackstone_wall");
            d.set(x, Y + 3, z, "polished_blackstone_wall");
            d.set(x, Y + 4, z, "gold_block");
            d.set(x, Y + 5, z, "lantern[hanging=false]");
        }
        // planters with flowers at r=22 between spokes
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45 + 22.5);
            int cx = (int) Math.floor(PX + 0.5 + Math.cos(a) * 25.5), cz = (int) Math.floor(PZ + 0.5 + Math.sin(a) * 25.5);
            if (k == 5 || k == 6) continue; // keep the north side open toward the gate
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
                boolean rim = x != cx || z != cz;
                d.set(x, Y + 1, z, rim ? "stone_brick_wall" : "moss_block");
                if (!rim) {
                    d.set(x, Y + 1, z, "rooted_dirt");
                    Trees.bush(d, x, Y + 2, z, 0.5, x * 3 + z);
                }
            }
            String[] fl = {"red_tulip", "orange_tulip", "allium", "cornflower"};
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++)
                if ((x != cx || z != cz) && (x + z) % 2 == 0) { d.set(x, Y + 1, z, "moss_block"); d.set(x, Y + 2, z, fl[(k + x) & 3]); }
        }
    }

    String floor(int x, int z, double rr, double a) {
        // emblem mosaic (seen from above: gold top at the north)
        int S = 27;
        int c = x - PX + S / 2, r = z - PZ + S / 2;
        if (c >= 0 && c < S && r >= 0 && r < S) {
            String e = HubVenue.emblem(c, r, S);
            if (e != null) return e;
        }
        if (rr <= 15.0) return rr > 14.0 ? "gold_block" : (Math.floorMod(x + z, 2) == 0 ? "blue_terracotta" : "blue_concrete");
        if (rr <= 16.5) return Math.floorMod((int) Math.round(a / 7.5), 2) == 0 ? "polished_blackstone" : "ochre_froglight";
        if (rr <= 33.5) {
            // flush guide runners: South spawn -> centre, West -> leaderboard, East -> team booths
            if (x == PX && z >= PZ + 15 && z <= PZ + 27) {
                return Math.floorMod(z, 4) == 0 ? "ochre_froglight" : "gold_block";
            }
            if (z == PZ && ((x >= PX - 29 && x <= PX - 17) || (x >= PX + 17 && x <= PX + 26))) {
                return Math.floorMod(x, 4) == 0 ? "sea_lantern" : "gold_block";
            }
            // light points
            if ((rr > 21.4 && rr < 22.6 || rr > 29.4 && rr < 30.6) && Math.floorMod((int) Math.round(a / 2.5), 12) == 0) return "ochre_froglight";
            // radial spokes every 30 deg
            double spoke = Math.abs(((a % 30) + 30) % 30 - 15);
            if (spoke > 15 - 18.0 / Math.max(rr, 1)) return "smooth_stone";
            if (rr > 24.5 && rr < 25.5) return "polished_granite";
            // herringbone-ish brick/terracotta field
            int hb = Math.floorMod((x + 2 * z) / 2 + (Math.floorMod(z, 2) == 0 ? 0 : 1), 4);
            return switch (hb) {
                case 0 -> "bricks";
                case 1 -> d.pick(x, Y, z, 63, "terracotta", 3, "red_terracotta", 1);
                case 2 -> "granite";
                default -> d.pick(x, Y, z, 64, "orange_terracotta", 1, "terracotta", 2, "bricks", 1);
            };
        }
        if (rr <= 34.5) return "gold_block";
        return d.pick(x, Y, z, 65, "smooth_sandstone", 2, "cut_sandstone", 2, "stone_bricks", 1);
    }

    // ------------------------------------------------------------------------------------------------ leaderboard
    void leaderboard() {
        int wx = PX - 32; // wall plane, facing +X
        int z0 = PZ - 10, z1 = PZ + 10;
        for (int z = z0; z <= z1; z++) for (int y = Y + 1; y <= Y + 13; y++) {
            boolean frame = z == z0 || z == z1 || y == Y + 1 || y == Y + 13;
            boolean frame2 = z == z0 + 1 || z == z1 - 1 || y == Y + 2 || y == Y + 12;
            d.set(wx, y, z, frame ? "gold_block" : frame2 ? "polished_blackstone" : "black_concrete");
            d.set(wx - 1, y, z, frame ? "quartz_block" : "white_concrete");
            d.set(wx - 2, y, z, "stone_bricks");
        }
        // crest with emblem on top
        for (int c = 0; c < 11; c++) for (int r = 0; r < 11; r++) {
            String e = HubVenue.emblem(c, r, 11);
            d.set(wx, Y + 24 - r, PZ - 5 + c, e != null ? e : null);
        }
        for (int z = PZ - 6; z <= PZ + 6; z++) d.set(wx, Y + 14, z, "gold_block");
        // pilasters + lights
        for (int z : new int[]{z0 - 1, z1 + 1}) for (int y = Y + 1; y <= Y + 15; y++) {
            d.set(wx, y, z, y % 3 == 0 ? "ochre_froglight" : "quartz_pillar[axis=y]");
            d.set(wx - 1, y, z, "quartz_block");
        }
        d.set(wx, Y + 16, z0 - 1, "gold_block"); d.set(wx, Y + 16, z1 + 1, "gold_block");
        // light bar in front at floor
        for (int z = z0; z <= z1; z++) d.set(wx + 1, Y, z, Math.floorMod(z, 3) == 0 ? "sea_lantern" : "polished_blackstone");
        L.leaderboard[0] = wx + 1.2; L.leaderboard[1] = Y + 7.5; L.leaderboard[2] = PZ + 0.5; L.leaderboard[3] = -90;
        L.label(wx + 1.35, Y + 13.7, PZ + 0.5, -90, "§6§l★ MCC LEADERBOARD ★");
        for (int x = wx - 2; x <= wx + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) g.reserve(x, z);
    }

    // ------------------------------------------------------------------------------------------------ team pads
    void teamPads() {
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-49.5 + i * 11.0);
            double cx = PX + 0.5 + Math.cos(a) * 29.5, cz = PZ + 0.5 + Math.sin(a) * 29.5;
            int bx = (int) Math.floor(cx), bz = (int) Math.floor(cz);
            String dye = Draw.TEAM_DYES[i];
            for (int x = bx - 1; x <= bx + 1; x++) for (int z = bz - 1; z <= bz + 1; z++) {
                boolean center = x == bx && z == bz;
                boolean edge = x == bx || z == bz;
                d.set(x, Y, z, center ? "sea_lantern" : edge ? dye + "_concrete" : "gold_block");
            }
            // Framed glowing team booth behind each pad
            int ox = (int) Math.floor(PX + 0.5 + Math.cos(a) * 32.8), oz = (int) Math.floor(PZ + 0.5 + Math.sin(a) * 32.8);
            int tx = Math.abs(Math.cos(a)) >= Math.abs(Math.sin(a)) ? 0 : 1;
            int tz = 1 - tx;
            for (int s = -1; s <= 1; s++) {
                int px = ox + s * tx, pz = oz + s * tz;
                if (s == 0) {
                    d.set(px, Y + 1, pz, dye + "_wool");
                    d.set(px, Y + 2, pz, dye + "_stained_glass");
                    d.set(px, Y + 3, pz, dye + "_stained_glass");
                    d.set(px, Y + 4, pz, dye + "_wool");
                    d.set(px, Y + 5, pz, "gold_block");
                    d.set(px, Y + 6, pz, "end_rod[facing=up]");
                } else {
                    for (int y = Y + 1; y <= Y + 4; y++) d.set(px, y, pz, dye + "_concrete");
                    d.set(px, Y + 5, pz, "gold_block");
                    d.set(px, Y + 6, pz, "lantern[hanging=false]");
                }
            }
            L.teamPads.add(new double[]{bx + 0.5, Y + 1, bz + 0.5});
        }
        L.label(PX + 29.5, Y + 9.2, PZ + 0.5, 90, "§b§l▼ JOIN A TEAM ▼\n§fStep onto any booth or type §e/team");
    }

    // ------------------------------------------------------------------------------------------------ approach
    void approach() {
        int zA = HubVenue.CZ + 60; // -2 (just past the venue steps)
        for (int x = -31; x <= 31; x++) for (int z = zA; z <= GATE_Z + 4; z++) {
            int ax = Math.abs(x);
            for (int y = AY - 5; y < AY; y++) d.set(x, y, z, "stone");
            for (int y = AY + 1; y <= AY + 10; y++) d.air(x, y, z);
            String b;
            if (ax <= 2) b = "red_wool";
            else if (ax == 3) b = "gold_block";
            else if (ax == 4) b = "polished_blackstone";
            else if (ax >= 29) b = "stone_bricks";
            else if (Math.floorMod(z, 8) == 0 && ax % 4 == 1) b = "ochre_froglight";
            else b = (Math.floorMod(x / 2 + z / 2, 2) == 0) ? "polished_andesite" : d.pick(x, AY, z, 71, "stone_bricks", 3, "andesite", 1);
            d.set(x, AY, z, b);
            g.paved(x, z, AY);
        }
        // braziers and flags along the carpet
        int k = 0;
        for (int z = zA + 4; z <= GATE_Z - 4; z += 7, k++) {
            d.brazier(-7, AY + 1, z, k % 3 == 2);
            d.brazier(7, AY + 1, z, k % 3 == 2);
            d.flag(-13, AY + 1, z, 9, Draw.TEAM_DYES[(k * 2) % 10], true, true);
            d.flag(13, AY + 1, z, 9, Draw.TEAM_DYES[(k * 2 + 1) % 10], true, false);
        }
        // hedge planters between flags
        for (int z = zA + 2; z <= GATE_Z - 2; z++) for (int x : new int[]{-21, -20, 20, 21}) {
            if (Math.floorMod(z - zA, 7) == 4) continue;
            d.set(x, AY + 1, z, Draw.leaves(d.rnd(x, 0, z, 72) < 0.2 ? "flowering_azalea" : "oak"));
            if (Math.abs(x) == 20) d.set(x, AY, z, "moss_block");
        }
        // retaining walls with hedges on top (terraces at y 77 behind)
        for (int s : new int[]{-1, 1}) {
            for (int z = zA - 8; z <= GATE_Z + 4; z++) {
                for (int w = 30; w <= 31; w++) {
                    int x = s * w;
                    for (int y = AY; y <= 77; y++) {
                        String b = (Math.floorMod(z, 6) == 0) ? (y == 76 ? "chiseled_stone_bricks" : "stone_bricks")
                                : d.pick(x, y, z, 73, "stone_bricks", 5, "mossy_stone_bricks", 2, "cracked_stone_bricks", 1);
                        if (y == 77) b = "polished_andesite";
                        d.set(x, y, z, b);
                    }
                    d.set(x, 78, z, Draw.leaves(d.rnd(x, 3, z, 74) < 0.2 ? "azalea" : "oak"));
                    if (w == 31) d.set(x, 79, z, Draw.leaves("oak"));
                    g.paved(x, z, 79);
                }
                // buttress + lantern facing the approach
                if (Math.floorMod(z, 6) == 0) {
                    int x = s * 29;
                    for (int y = AY + 1; y <= 75; y++) d.set(x, y, z, "stone_bricks");
                    d.set(x, 76, z, "stone_brick_wall");
                    d.set(x, 77, z, "lantern[hanging=false]");
                }
            }
            // terrace surface behind the walls
            for (int x = 32; x <= 50; x++) for (int z = zA - 10; z <= GATE_Z + 4; z++) {
                int xx = s * x;
                if (g.hAt(xx, z) < 74) continue;
                int top = g.hAt(xx, z);
                if (top >= 76 && top <= 78) {
                    for (int y = top + 1; y <= 77; y++) d.set(xx, y, z, "dirt");
                    d.set(xx, 77, z, "grass_block[snowy=false]");
                    for (int y = 78; y <= 80; y++) if (d.id(xx, y, z) != null && !d.id(xx, y, z).contains("leaves")) d.air(xx, y, z);
                    g.h[g.idx(xx, z)] = 77;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ gate
    void gate() {
        // gate floor + steps down to the plaza
        for (int x = -27; x <= 27; x++) {
            for (int z = GATE_Z - 3; z <= GATE_Z + 4; z++) {
                d.set(x, AY, z, Math.abs(x) <= 2 ? "red_wool" : Math.abs(x) == 3 ? "gold_block" : "smooth_quartz");
                g.paved(x, z, AY);
            }
            if (Math.abs(x) <= 22) d.set(x, Y, GATE_Z + 8, Math.abs(x) <= 2 ? "red_wool" : "smooth_quartz");
        }
        // steps down to the plaza: plaza floor (y 70, z 41) up to the gate floor (y 73, z 37)
        g.staircase("plaza gate", 0, Y, GATE_Z + 8, 0, AY, GATE_Z + 4, "north", 45,
                x -> Math.abs(x) <= 2 ? "red_nether_brick" : "quartz", x -> Math.abs(x) <= 2 ? "red_nether_bricks" : "quartz_block");
        // towers
        HubVenue hv = new HubVenue(g);
        for (int s : new int[]{-1, 1}) {
            int tx = s * 25;
            for (int x = tx - 2; x <= tx + 2; x++) for (int z = GATE_Z - 2; z <= GATE_Z + 2; z++) for (int y = Y - 2; y < AY; y++) d.set(x, y, z, "stone_bricks");
            hv.tower(tx, GATE_Z, AY, 104, true);
            L.fireworkSpots.add(new double[]{tx + 0.5, 125, GATE_Z + 0.5});
        }
        // low white wall / stage segments either side of the passage
        for (int x = -22; x <= 22; x++) {
            int ax = Math.abs(x);
            if (ax <= 8) continue;
            for (int z = GATE_Z - 2; z <= GATE_Z + 2; z++) for (int y = AY + 1; y <= AY + 6; y++) {
                boolean face = z == GATE_Z + 2 || z == GATE_Z - 2;
                String b = y == AY + 6 ? "quartz_block"
                        : y == AY + 5 ? "gold_block" : face && ax % 4 == 0 ? "quartz_pillar[axis=y]" : face ? (y == AY + 1 ? "polished_blackstone" : "calcite") : "white_concrete";
                d.set(x, y, z, b);
            }
            if (ax % 4 == 2) d.set(x, AY + 7, GATE_Z + 2, "lantern[hanging=false]");
            if (ax % 4 == 0) d.set(x, AY + 7, GATE_Z - 2, "end_rod[facing=up]");
        }
        // cross beam between towers
        for (int x = -23; x <= 23; x++) for (int z = GATE_Z - 1; z <= GATE_Z + 1; z++) for (int y = 101; y <= 103; y++) {
            boolean mid = z == GATE_Z;
            String b = y == 102 && !mid ? (Math.floorMod(x, 3) == 0 ? "ochre_froglight" : "gold_block") : "polished_blackstone";
            if (y == 103) b = mid ? "polished_blackstone" : "polished_blackstone_slab[type=bottom]";
            d.set(x, y, z, b);
        }
        // hanging team banners in rainbow order (10 x 3 wide), bottom pointed
        for (int i = 0; i < 10; i++) {
            int x0 = -19 + i * 4;
            String dye = Draw.TEAM_DYES[i];
            for (int c = 0; c < 3; c++) {
                int x = x0 + c;
                int bottom = 86 + (c == 1 ? 0 : 1);
                for (int y = bottom; y <= 100; y++) {
                    String b;
                    if (y == 100) b = "gold_block";
                    else if (y == 99) b = dye + "_wool";
                    else if (y == 96 || y == 92) b = "white_wool";
                    else if (y == 94 && c == 1) b = "yellow_wool";
                    else b = dye + "_wool";
                    d.set(x, y, GATE_Z, b);
                }
            }
            d.set(x0 + 1, 85, GATE_Z, "end_rod[facing=down]");
        }
        L.label(0.5, 108.5, GATE_Z + 0.5, 0, "§6§lMINECRAFT §e§lCHAMPIONSHIPS");
    }

    // ------------------------------------------------------------------------------------------------ statues
    void statues() {
        // giant trophy on the west terrace
        trophy(-42, 77, 8, 1.0);
        // giant emblem cube on the east terrace (floating above a pedestal)
        int cx = 42, cz = 8, by = 77;
        for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++) {
            int m = Math.max(Math.abs(x - cx), Math.abs(z - cz));
            for (int y = by + 1; y <= by + (m >= 4 ? 1 : m >= 3 ? 2 : 3); y++) d.set(x, y, z, m >= 4 ? "polished_blackstone_bricks" : m == 3 ? "quartz_block" : "gold_block");
            g.reserve(x, z);
        }
        d.set(cx, by + 4, cz, "end_rod[facing=up]");
        int cy = by + 7, s = 4;
        for (int x = -s; x <= s; x++) for (int y = -s; y <= s; y++) for (int z = -s; z <= s; z++) {
            boolean edge = (Math.abs(x) == s ? 1 : 0) + (Math.abs(y) == s ? 1 : 0) + (Math.abs(z) == s ? 1 : 0) >= 2;
            String b;
            if (y == s) b = edge ? "orange_concrete" : (Math.abs(x) + Math.abs(z) < 3 ? "yellow_concrete" : "gold_block");
            else if (edge) b = y == -s ? "black_concrete" : "polished_blackstone";
            else if (Math.abs(x) == s) b = x < 0 ? "red_concrete" : "blue_concrete";
            else if (Math.abs(z) == s) b = z > 0 ? "red_concrete" : "blue_concrete";
            else b = y == -s ? "black_concrete" : "black_concrete";
            d.set(cx + x, cy + s + y, cz + z, b);
        }
        L.label(cx + 0.5, cy + 2 * s + 3, cz + 0.5, 0, "§e§lTHE MCC CROWN CUBE");
    }

    void trophy(int cx, int by, int cz, double sc) {
        double x0 = cx + 0.5, z0 = cz + 0.5;
        for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++) {
            int m = Math.max(Math.abs(x - cx), Math.abs(z - cz));
            for (int y = by + 1; y <= by + (m >= 4 ? 1 : 2); y++) d.set(x, y, z, m >= 4 ? "polished_blackstone_bricks" : (m == 3 ? "quartz_block" : "smooth_quartz"));
            g.reserve(x, z);
        }
        int b = by + 3;
        d.disc(x0, b, z0, 3.0, "polished_blackstone");
        d.disc(x0, b + 1, z0, 2.5, "gold_block");
        for (int y = b + 2; y <= b + 5; y++) d.disc(x0, y, z0, y == b + 4 ? 1.6 : 1.0, "gold_block");
        int c0 = b + 6;
        for (int y = c0; y <= c0 + 7; y++) {
            double t = (y - c0) / 7.0;
            double rr = 1.6 + 3.2 * Math.sqrt(t);
            d.ring(x0, y, z0, y == c0 ? -1 : rr - 1.05, rr, y % 2 == 0 ? "gold_block" : "raw_gold_block");
        }
        for (int s : new int[]{-1, 1}) for (int k = 0; k <= 8; k++) {
            double t = k / 8.0 * Math.PI;
            d.set((int) Math.floor(x0 + s * (4.1 + 2.0 * Math.sin(t))), (int) Math.floor(c0 + 1 + 4.0 * (1 - Math.cos(t)) / 2.0 * 1.3), cz, "gold_block");
        }
        d.set(cx, c0 + 1, cz, "ochre_froglight");
        L.label(x0, c0 + 11, z0, 0, "§6§lCHAMPIONS' CUP");
    }

    /** Original mascot "Bloxy": a cube-headed showman with a red jacket, blue trousers and a gold star. */
    void mascot() {
        int cx = PX - 50, cz = PZ + 20, by = Y;
        // pedestal
        for (int x = cx - 5; x <= cx + 5; x++) for (int z = cz - 5; z <= cz + 5; z++) {
            int m = Math.max(Math.abs(x - cx), Math.abs(z - cz));
            int top = g.hAt(x, z);
            for (int y = Math.min(top, by) - 2; y <= by + (m >= 5 ? 0 : 1); y++) d.set(x, y, z, m >= 5 ? "stone_bricks" : m == 4 ? "polished_andesite" : "smooth_quartz");
            g.paved(x, z, by + 1);
        }
        int y0 = by + 2;
        // legs (blue) + shoes (black)
        for (int sx : new int[]{-2, 1}) {
            d.fill(cx + sx, y0, cz - 1, cx + sx + 1, y0, cz + 1, "black_concrete");
            d.fill(cx + sx, y0 + 1, cz - 1, cx + sx + 1, y0 + 5, cz, "blue_concrete");
        }
        // body (red jacket, gold buttons, white collar)
        d.fill(cx - 3, y0 + 6, cz - 2, cx + 3, y0 + 12, cz + 1, "red_concrete");
        d.fill(cx - 3, y0 + 6, cz - 2, cx + 3, y0 + 6, cz + 1, "black_concrete");
        d.fill(cx - 3, y0 + 7, cz - 2, cx + 3, y0 + 7, cz + 1, "gold_block");
        for (int y = y0 + 9; y <= y0 + 11; y += 2) d.set(cx, y, cz + 2, "gold_block");
        d.fill(cx - 2, y0 + 12, cz - 2, cx + 2, y0 + 12, cz + 1, "white_concrete");
        // arms: left down, right raised waving
        d.fill(cx - 5, y0 + 8, cz - 1, cx - 4, y0 + 12, cz, "red_concrete");
        d.fill(cx - 5, y0 + 7, cz - 1, cx - 4, y0 + 7, cz, "white_concrete");
        d.fill(cx + 4, y0 + 11, cz - 1, cx + 5, y0 + 16, cz, "red_concrete");
        d.fill(cx + 4, y0 + 17, cz - 1, cx + 5, y0 + 18, cz, "white_concrete");
        // star in the raised hand
        int sx = cx + 5, sy = y0 + 21;
        for (int k = -2; k <= 2; k++) { d.set(sx + k, sy, cz, "gold_block"); d.set(sx, sy + k, cz, "gold_block"); }
        d.set(sx, sy, cz + 1, "ochre_froglight");
        d.set(sx + 1, sy + 1, cz, "yellow_concrete"); d.set(sx - 1, sy - 1, cz, "yellow_concrete");
        d.set(sx + 1, sy - 1, cz, "yellow_concrete"); d.set(sx - 1, sy + 1, cz, "yellow_concrete");
        // head: the cube emblem as a head with a face on the front (+Z)
        int hy = y0 + 13;
        for (int x = -4; x <= 4; x++) for (int y = 0; y <= 8; y++) for (int z = -4; z <= 4; z++) {
            boolean shell = Math.abs(x) == 4 || y == 0 || y == 8 || Math.abs(z) == 4;
            if (!shell) continue;
            String b = y == 8 ? (Math.abs(x) == 4 || Math.abs(z) == 4 ? "orange_concrete" : "gold_block") : x < 0 ? "red_concrete" : "blue_concrete";
            if (z == 4 && y > 0 && y < 8) b = "white_concrete";
            d.set(cx + x, hy + y, cz + z, b);
        }
        // face
        for (int ex : new int[]{-2, 2}) { d.set(cx + ex, hy + 5, cz + 4, "black_concrete"); d.set(cx + ex, hy + 4, cz + 4, "black_concrete"); }
        d.set(cx - 1, hy + 2, cz + 4, "black_concrete"); d.set(cx, hy + 2, cz + 4, "black_concrete"); d.set(cx + 1, hy + 2, cz + 4, "black_concrete");
        d.set(cx - 2, hy + 3, cz + 4, "black_concrete"); d.set(cx + 2, hy + 3, cz + 4, "black_concrete");
        d.set(cx - 3, hy + 3, cz + 4, "pink_concrete"); d.set(cx + 3, hy + 3, cz + 4, "pink_concrete");
        L.label(cx + 0.5, by + 3.2, cz + 6.2, 0, "§c§lBLOXY §7the MCC mascot");
    }
}
