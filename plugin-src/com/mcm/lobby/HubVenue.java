package com.mcm.lobby;

/** The Decision Dome venue: terrace, drum facade, glass dome, trophy topper, gatehouse with clock + logo, full interior. */
final class HubVenue {
    final HubGenerator g;
    final Draw d;
    final HubLayout L;
    static final int CX = HubGenerator.DOME_X, CZ = HubGenerator.DOME_Z;
    static final int TERRACE = 76;      // terrace / entrance floor block y
    static final int STAGE = 64;        // stage floor block y
    static final int RIM = 102;         // drum top
    static final double R_OUT = 48.5, R_IN = 45.5, R_TERRACE = 56.5;
    static final double PAD_RING = 19.5, PAD_R = 4.0;
    static final int FRONT_Z = CZ + 52;  // gatehouse front face (z = -10)

    HubVenue(HubGenerator g) { this.g = g; this.d = g.d; this.L = g.L; }

    static double r(int x, int z) { return Math.hypot(x - CX, z - CZ); }
    static double ang(int x, int z) { double a = Math.atan2(z - CZ, x - CX); return a < 0 ? a + Math.PI * 2 : a; }
    static double angDeg(int x, int z) { return Math.toDegrees(ang(x, z)); }
    /** Angular distance (deg) from the front (+Z = 90 deg). */
    static double fromFront(int x, int z) { return Math.abs(angDeg(x, z) - 90); }

    void build() {
        L.domeX = CX; L.domeZ = CZ; L.stageY = STAGE; L.domeRadius = 48; L.innerRadius = 45;
        foundation();
        drumAndFacade();
        interior();
        glassDome();
        topper();
        gatehouse();
        pylons();
        rimFlags();
        layout();
    }

    // ------------------------------------------------------------------------------------------------ foundation
    void foundation() {
        int R = (int) R_TERRACE + 2;
        for (int x = CX - R - 4; x <= CX + R + 4; x++) for (int z = CZ - R - 4; z <= CZ + R + 4; z++) {
            double rr = r(x, z);
            if (rr > R_TERRACE) continue;
            for (int y = 58; y < TERRACE; y++) d.set(x, y, z, y < 70 ? "stone" : terraceWall(x, y, z, rr));
            // terrace surface
            String top;
            if (rr > R_TERRACE - 1.2) top = "polished_andesite";
            else if (rr > 52.5 && rr <= 53.5) top = (Math.floorMod((int) Math.round(angDeg(x, z) * 2), 9) == 0) ? "ochre_froglight" : "gold_block";
            else top = d.pick(x, TERRACE, z, 11, "smooth_stone", 3, "polished_andesite", 2, "andesite", 1);
            d.set(x, TERRACE, z, top);
            g.paved(x, z, TERRACE);
            for (int y = TERRACE + 1; y < TERRACE + 6; y++) d.air(x, y, z);
            // parapet
            if (rr > R_TERRACE - 1.2 && fromFront(x, z) > 16) {
                d.set(x, TERRACE + 1, z, (Math.floorMod(x + z, 6) == 0) ? "chiseled_quartz_block" : "quartz_slab[type=bottom]");
            }
        }
        // the ground ring just outside the terrace: paved apron
        for (int x = CX - 64; x <= CX + 64; x++) for (int z = CZ - 64; z <= CZ + 64; z++) {
            double rr = r(x, z);
            if (rr <= R_TERRACE || rr > 61.5) continue;
            if (z > -8 && Math.abs(x) <= 31) continue; // approach handles this
            int gy = g.hAt(x, z);
            if (gy < HubGenerator.GROUND - 2 || gy > HubGenerator.GROUND + 2) continue;
            for (int y = gy; y < HubGenerator.GROUND; y++) d.set(x, y, z, "stone");
            for (int y = HubGenerator.GROUND + 1; y < HubGenerator.GROUND + 4; y++) d.air(x, y, z);
            d.set(x, HubGenerator.GROUND, z, rr > 60.5 ? "stone_bricks" : d.pick(x, 0, z, 12, "stone_bricks", 2, "polished_andesite", 2, "cracked_stone_bricks", 1, "mossy_stone_bricks", 1));
            g.paved(x, z, HubGenerator.GROUND);
        }
        // front steps: approach floor (y 73, z = -2) up to the landing (y 76, z = -6): stairs at z -3/-4/-5, y 74/75/76
        g.staircase("dome entrance", 0, HubGenerator.APPROACH_Y, CZ + 60, 0, TERRACE, CZ + 56, "north", 31,
                x -> Math.abs(x) <= 3 ? "red_nether_brick" : "quartz", x -> Math.abs(x) <= 3 ? "red_nether_bricks" : "quartz_block");
        for (int x = -15; x <= 15; x++) {
            // landing at the terrace edge row
            for (int z = CZ + 54; z <= CZ + 56; z++) {
                d.set(x, TERRACE, z, Math.abs(x) <= 3 ? "red_wool" : "smooth_quartz");
                for (int yy = TERRACE + 1; yy < TERRACE + 5; yy++) d.air(x, yy, z);
                g.paved(x, z, TERRACE);
            }
        }
        // carpet across the landing to the doors
        for (int z = FRONT_Z; z <= CZ + 56; z++) for (int x = -3; x <= 3; x++)
            d.set(x, TERRACE, z, Math.abs(x) == 3 ? "gold_block" : "red_wool");
        // braziers on the landing
        for (int sx : new int[]{-12, 12}) d.brazier(sx, TERRACE + 1, CZ + 55, false);
    }

    String terraceWall(int x, int y, int z, double rr) {
        if (rr < R_TERRACE - 1.2) return "stone_bricks";
        if (y == TERRACE - 1) return "smooth_quartz";
        if (y == 70) return "polished_blackstone";
        double a = angDeg(x, z);
        if (Math.floorMod((int) Math.floor(a / 6.0), 2) == 0) return y == 73 ? "ochre_froglight" : d.pick(x, y, z, 13, "stone_bricks", 4, "mossy_stone_bricks", 1, "cracked_stone_bricks", 1);
        return d.pick(x, y, z, 14, "polished_andesite", 3, "andesite", 1);
    }

    // ------------------------------------------------------------------------------------------------ drum
    void drumAndFacade() {
        int R = 50;
        for (int x = CX - R; x <= CX + R; x++) for (int z = CZ - R; z <= CZ + R; z++) {
            double rr = r(x, z);
            if (rr <= R_IN || rr > R_OUT + 2.0) continue;
            double a = angDeg(x, z);
            for (int y = TERRACE + 1; y <= RIM + 1; y++) {
                if (rr <= R_OUT) {
                    boolean outer = rr > R_OUT - 1.0;
                    d.set(x, y, z, outer ? facade(x, y, z, a) : innerWall(x, y, z, a));
                } else if (rr <= R_OUT + 1.0) {
                    // cornice + base plinth ring
                    if (y == RIM - 2) d.set(x, y, z, "smooth_quartz");
                    else if (y == RIM - 1) d.set(x, y, z, Math.floorMod((int) Math.round(a * 1.5), 6) == 0 ? "sea_lantern" : "quartz_block");
                    else if (y == RIM) d.set(x, y, z, "quartz_block");
                    else if (y == RIM + 1) d.set(x, y, z, Math.floorMod((int) Math.round(a), 4) < 2 ? "quartz_slab[type=bottom]" : "air");
                    else if (y == TERRACE + 1) d.set(x, y, z, "polished_blackstone");
                } else {
                    if (y == RIM) d.set(x, y, z, "quartz_slab[type=top]");
                }
            }
        }
    }

    String facade(int x, int y, int z, double a) {
        double panel = a / 10.0;
        double frac = panel - Math.floor(panel);
        boolean seam = frac < 0.11 || frac > 0.95;
        int pi = (int) Math.floor(panel);
        if (y <= TERRACE + 2) return y == TERRACE + 1 ? "polished_blackstone" : "gray_concrete";
        if (y == TERRACE + 3) return Math.floorMod((int) Math.round(a * 2), 3) == 0 ? "yellow_concrete" : "gold_block";
        if (y >= RIM - 4 && y <= RIM - 3) return y == RIM - 3 ? "gold_block" : "yellow_concrete";
        if (seam) return y % 4 == 0 ? "smooth_quartz" : "white_concrete";
        if (y == 86 || y == 95) return "blue_concrete";
        if (y >= 88 && y <= 92 && frac > 0.3 && frac < 0.78) return (pi % 2 == 0) ? "light_blue_stained_glass" : "blue_stained_glass";
        if (y == 87 || y == 93) return (frac > 0.25 && frac < 0.82) ? "white_concrete" : "red_concrete";
        if (y < 83) return y == TERRACE + 4 ? "red_nether_bricks" : "red_terracotta";
        return "red_concrete";
    }

    String innerWall(int x, int y, int z, double a) {
        if (y >= 88 && y <= 92) {
            double frac = a / 10.0 - Math.floor(a / 10.0);
            if (frac > 0.3 && frac < 0.78) return (((int) Math.floor(a / 10.0)) % 2 == 0) ? "light_blue_stained_glass" : "blue_stained_glass";
        }
        // LED ribbon above the top tier: rainbow team panels between lit trims (keeps the back wall from reading as a black void)
        if (y == 81 || y == 84) return Math.floorMod((int) Math.round(a * 2), 4) == 0 ? "sea_lantern" : "white_concrete";
        if (y == 82 || y == 83) {
            int seg = Math.floorMod((int) Math.floor(a / 12.0), Draw.TEAM_DYES.length);
            double f = a / 12.0 - Math.floor(a / 12.0);
            if (f < 0.08) return "black_concrete";
            return Draw.TEAM_DYES[seg] + "_concrete";
        }
        if (y == 80 || y == 96) return "white_concrete";
        if (y == 85 || y == 99) return "red_concrete";
        if (Math.floorMod((int) Math.round(a), 15) == 0) return "polished_blackstone_bricks";
        if (y > 96) return "black_concrete";
        return d.pick(x, y, z, 21, "gray_concrete", 3, "black_concrete", 2, "polished_deepslate", 1);
    }

    // ------------------------------------------------------------------------------------------------ interior
    /** Floor height of the bowl at radius r (top solid block y). */
    static int bowl(double rr) {
        int k = (int) Math.floor(rr);
        if (rr <= 28.5) return STAGE;
        if (k <= 30) return 66;                // pit wall
        if (k <= 34) return 67 + (k - 31);     // tier 1: 67..70
        if (k <= 36) return 71;                // walkway 1
        if (k <= 40) return 72 + (k - 37);     // tier 2: 72..75
        if (k <= 42) return 76;                // walkway 2 (entrance level)
        return 77 + (k - 43);                  // tier 3: 77..79
    }

    void interior() {
        int R = 46;
        for (int x = CX - R; x <= CX + R; x++) for (int z = CZ - R; z <= CZ + R; z++) {
            double rr = r(x, z);
            if (rr > R_IN) continue;
            double a = angDeg(x, z);
            int top = bowl(rr);
            boolean tunnel = Math.abs(x) <= 5 && z > CZ && rr > 40.5; // entrance cut through tier 3
            if (tunnel) top = 76;
            for (int y = 58; y <= 146; y++) {
                if (y > top) { d.air(x, y, z); continue; }
                if (y < top - 3) { d.set(x, y, z, "stone"); continue; }
                d.set(x, y, z, bowlBlock(x, y, z, rr, a, top, tunnel));
            }
            // seats on tiers
            int k = (int) Math.floor(rr);
            boolean seatRow = (k >= 31 && k <= 34) || (k >= 37 && k <= 40) || (k >= 43 && k <= 45 && rr <= R_IN - 0.3);
            boolean aisle = Math.floorMod((int) Math.floor(a / 2.0), 18) == 0;
            if (seatRow && !aisle && !tunnel && k != 31 && k != 37 && k != 43) {
                int sector = Math.floorMod((int) Math.floor((a + 90) / 36.0), 10);
                boolean gap = Math.floorMod((int) Math.floor(a * rr / 57.3), 3) == 2;
                d.set(x, top + 1, z, gap ? null : Draw.TEAM_DYES[sector] + "_carpet");
            }
            if (seatRow && aisle && !tunnel) d.set(x, top, z, "quartz_block");
            g.paved(x, z, top);
        }
        stage();
        pads();
        podium();
        columns();
        truss();
        chandelier();
        tunnel();
        aisles();
    }

    /** Aisle floor height at radius rr: the bowl profile, but stepping 64 -> 65 -> 66 down the pit wall. */
    static int aisleTop(double rr) {
        int k = (int) Math.floor(rr);
        if (rr <= 28.5 || k <= 28) return STAGE;
        if (k == 29) return STAGE + 1;
        if (k == 30) return STAGE + 2;
        return bowl(rr);
    }

    /**
     * The ten radial aisles (stage -> walkway 1 -> walkway 2 -> top row) as walkable stair runs: a 4-connected
     * path along each aisle ray, every rising cell a bottom stair facing the walking direction, flat cells full
     * blocks; registered as walks for the validator.
     */
    void aisles() {
        for (int k = 0; k < 10; k++) {
            double a = Math.toRadians(k * 36.0 + 1.0);
            java.util.List<int[]> cells = new java.util.ArrayList<>();
            for (double t = 26.8; t <= 45.3; t += 0.02) {
                int x = (int) Math.floor(CX + Math.cos(a) * t), z = (int) Math.floor(CZ + Math.sin(a) * t);
                if (r(x, z) > R_IN - 0.2) break;
                if (!cells.isEmpty()) {
                    int[] p = cells.get(cells.size() - 1);
                    if (p[0] == x && p[1] == z) continue;
                    if (p[0] != x && p[1] != z) { // diagonal move: insert the corner cell nearer the ray
                        double ex = Math.abs(-Math.sin(a) * (x + 0.5 - CX) + Math.cos(a) * (p[1] + 0.5 - CZ));
                        double ez = Math.abs(-Math.sin(a) * (p[0] + 0.5 - CX) + Math.cos(a) * (z + 0.5 - CZ));
                        cells.add(ex <= ez ? new int[]{x, p[1]} : new int[]{p[0], z});
                    }
                }
                cells.add(new int[]{x, z});
            }
            int[] xs = new int[cells.size()], zs = new int[cells.size()];
            int prev = STAGE;
            for (int i = 0; i < cells.size(); i++) {
                int x = cells.get(i)[0], z = cells.get(i)[1];
                xs[i] = x; zs[i] = z;
                double rr = r(x, z);
                int top = Math.max(prev, aisleTop(rr)); // never step down while walking outwards
                for (int y = top + 1; y <= top + 3; y++) d.air(x, y, z);
                int kk = (int) Math.floor(rr);
                boolean walkway = kk == 35 || kk == 36 || kk == 41 || kk == 42;
                if (i > 0 && top > prev) {
                    String f = Draw.facingToward(xs[i - 1], zs[i - 1], x, z);
                    d.set(x, top, z, Draw.stairs("quartz", f, false));
                    d.set(x, top - 1, z, "quartz_block");
                } else if (top > STAGE && !walkway) d.set(x, top, z, "quartz_block");
                else if (top == STAGE && rr > 28.5) d.set(x, top, z, "smooth_quartz");
                prev = top;
                g.paved(x, z, top);
            }
            g.walks.add(new HubGenerator.Walk("dome aisle " + (k * 36) + "deg", xs, zs, STAGE, prev));
        }
    }

    String bowlBlock(int x, int y, int z, double rr, double a, int top, boolean tunnel) {
        int k = (int) Math.floor(rr);
        if (tunnel) return y == top ? (Math.abs(x) <= 2 ? "red_wool" : Math.abs(x) == 3 ? "gold_block" : "polished_andesite") : "stone_bricks";
        if (rr <= 28.5) return "white_concrete"; // replaced by stage()
        if (k <= 30) { // pit wall
            if (y == top) return "smooth_quartz";
            if (y == top - 1) return "red_concrete";
            return d.pick(x, y, z, 31, "black_concrete", 3, "gray_concrete", 1);
        }
        if (k == 35 || k == 36 || k == 41 || k == 42) { // walkways
            if (y != top) return "gray_concrete";
            boolean inner = k == 35 || k == 41;
            return inner ? "polished_andesite" : d.pick(x, y, z, 32, "smooth_stone", 2, "polished_andesite", 1);
        }
        // seat rows: the top block colours the riser. sector colour by angle (10 team sectors)
        if (y == top) {
            if (k == 31 || k == 37 || k == 43) // tier front row: light strip
                return Math.floorMod((int) Math.round(a * 1.2), 5) == 0 ? "sea_lantern" : "white_concrete";
            int sector = Math.floorMod((int) Math.floor((a + 90) / 36.0), 10);
            if (k == 34 || k == 40) return Draw.TEAM_DYES[sector] + "_concrete"; // coloured riser under each walkway
            return k % 2 == 0 ? "gray_concrete" : "black_concrete";
        }
        return "gray_concrete";
    }

    void stage() {
        for (int x = CX - 29; x <= CX + 29; x++) for (int z = CZ - 29; z <= CZ + 29; z++) {
            double rr = r(x, z);
            if (rr > 28.5) continue;
            double a = angDeg(x, z);
            String b;
            if (rr > 26.5) b = (Math.floorMod((int) Math.round(a / 5.0), 2) == 0) ? "black_concrete" : "white_concrete";
            else if (rr > 25.5) b = Math.floorMod((int) Math.round(a), 10) == 0 ? "sea_lantern" : "light_gray_concrete";
            else if (rr > 9.5) {
                boolean check = (Math.floorMod((int) Math.floor(rr / 2.0) + (int) Math.floor(a / 7.5), 2) == 0);
                b = check ? "white_concrete" : "smooth_quartz";
                if (rr > 13.5 && rr < 14.5) b = "light_gray_concrete";
            } else if (rr > 7.5) b = Math.floorMod((int) Math.round(a / 7.5), 2) == 0 ? "gold_block" : "yellow_glazed_terracotta";
            else b = "polished_blackstone";
            d.set(x, STAGE, z, b);
        }
    }

    void pads() {
        for (int i = 0; i < HubLayout.PAD_COUNT; i++) {
            double th = Math.toRadians(i * 30.0 + 15.0);
            double px = CX + 0.5 + Math.cos(th) * PAD_RING, pz = CZ + 0.5 + Math.sin(th) * PAD_RING;
            String dye = Draw.PAD_DYES[i];
            L.pads[i][0] = px; L.pads[i][1] = STAGE + 2; L.pads[i][2] = pz;
            var acc = L.padAccents[i];
            // runway stripe from the podium ring to the pad
            for (double t = 10.0; t <= PAD_RING - PAD_R + 0.2; t += 0.35) {
                for (double w = -0.9; w <= 0.9; w += 0.45) {
                    int x = (int) Math.floor(CX + 0.5 + Math.cos(th) * t - Math.sin(th) * w);
                    int z = (int) Math.floor(CZ + 0.5 + Math.sin(th) * t + Math.cos(th) * w);
                    if (!"white_concrete".equals(d.id(x, STAGE, z)) && !"smooth_quartz".equals(d.id(x, STAGE, z)) && !"light_gray_concrete".equals(d.id(x, STAGE, z))) continue;
                    d.set(x, STAGE, z, dye + "_concrete");
                    acc.add(new int[]{x, STAGE, z, HubLayout.K_CONCRETE});
                }
            }
            // pad body
            for (int x = (int) Math.floor(px - 5); x <= px + 5; x++) for (int z = (int) Math.floor(pz - 5); z <= pz + 5; z++) {
                double dd = Math.hypot(x + 0.5 - px, z + 0.5 - pz);
                if (dd > PAD_R) continue;
                d.set(x, STAGE, z, "sea_lantern");
                if (dd > PAD_R - 0.75) { d.set(x, STAGE + 1, z, dye + "_stained_glass"); acc.add(new int[]{x, STAGE + 1, z, HubLayout.K_GLASS}); }
                else if (dd < 1.3) { d.set(x, STAGE + 1, z, "ochre_froglight"); acc.add(new int[]{x, STAGE + 1, z, HubLayout.K_LIGHT}); }
                else { d.set(x, STAGE + 1, z, dye + "_concrete"); acc.add(new int[]{x, STAGE + 1, z, HubLayout.K_CONCRETE}); }
            }
            // corner glass posts with wool caps
            for (int c = 0; c < 4; c++) {
                double ca = th + Math.PI / 4 + c * Math.PI / 2;
                int x = (int) Math.floor(px + Math.cos(ca) * 3.0), z = (int) Math.floor(pz + Math.sin(ca) * 3.0);
                for (int y = STAGE + 2; y <= STAGE + 3; y++) { d.set(x, y, z, dye + "_stained_glass"); acc.add(new int[]{x, y, z, HubLayout.K_GLASS}); }
                d.set(x, STAGE + 4, z, dye + "_wool"); acc.add(new int[]{x, STAGE + 4, z, HubLayout.K_WOOL});
            }
            // backdrop fin (tangential) behind the pad
            double fx = CX + 0.5 + Math.cos(th) * 25.2, fz = CZ + 0.5 + Math.sin(th) * 25.2;
            for (double w = -3.2; w <= 3.2; w += 0.4) {
                int x = (int) Math.floor(fx - Math.sin(th) * w), z = (int) Math.floor(fz + Math.cos(th) * w);
                double aw = Math.abs(w);
                int hgt = (int) Math.round(9 - aw * 1.1);
                for (int y = STAGE + 1; y <= STAGE + hgt; y++) {
                    boolean frame = aw > 2.7 || y == STAGE + hgt || y == STAGE + 1;
                    if (frame) { d.set(x, y, z, dye + "_wool"); acc.add(new int[]{x, y, z, HubLayout.K_WOOL}); }
                    else { d.set(x, y, z, dye + "_stained_glass"); acc.add(new int[]{x, y, z, HubLayout.K_GLASS}); }
                }
                d.set(x, STAGE + hgt + 1, z, "end_rod[facing=up]");
            }
        }
        L.padRadius = PAD_R;
        // de-duplicate accents (later entries win; keep order)
        for (int i = 0; i < HubLayout.PAD_COUNT; i++) {
            java.util.LinkedHashMap<Long, int[]> m = new java.util.LinkedHashMap<>();
            for (int[] a : L.padAccents[i]) m.put(((long) a[0] << 40) ^ ((long) a[1] << 20) ^ (a[2] & 0xFFFFFL), a);
            L.padAccents[i].clear();
            L.padAccents[i].addAll(m.values());
        }
    }

    void podium() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        d.disc(cx, STAGE + 1, cz, 7.5, (x, y, z) -> {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            return rr > 6.6 ? (Math.floorMod(x + z, 2) == 0 ? "gold_block" : "polished_blackstone_bricks") : "polished_blackstone";
        });
        d.disc(cx, STAGE + 2, cz, 6.2, (x, y, z) -> {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr > 5.3) return "gold_block";
            if (rr < 1.2) return "ochre_froglight";
            return Math.floorMod(x + z, 2) == 0 ? "yellow_concrete" : "gold_block";
        });
        // corner light posts on the lower ring
        for (int c = 0; c < 8; c++) {
            double a = c * Math.PI / 4 + Math.PI / 8;
            int x = (int) Math.floor(cx + Math.cos(a) * 7.0), z = (int) Math.floor(cz + Math.sin(a) * 7.0);
            d.set(x, STAGE + 2, z, "end_rod[facing=up]");
        }
        L.podiumBaseY = STAGE + 2;
        L.podium[0] = cx; L.podium[1] = STAGE + 3; L.podium[2] = cz;
    }

    void columns() {
        for (int c = 0; c < 8; c++) {
            double a = Math.toRadians(22.5 + c * 45);
            double px = CX + 0.5 + Math.cos(a) * 44.0, pz = CZ + 0.5 + Math.sin(a) * 44.0;
            for (int y = 77; y <= 106; y++) {
                final int yy = y;
                String b;
                if (y <= 78) b = "polished_blackstone";
                else if (y >= 104) b = "gold_block";
                else {
                    int band = (y - 79) % 5;
                    double t = (y - 79) / 24.0;
                    String stripe = t < 0.25 ? "red_terracotta" : t < 0.45 ? "orange_concrete" : t < 0.65 ? "orange_terracotta" : t < 0.85 ? "yellow_concrete" : "gold_block";
                    b = band == 0 ? stripe : (band == 3 && y > 85 ? "ochre_froglight" : "red_concrete");
                }
                final String bb = b;
                d.disc(px, yy, pz, 2.1, (x, y2, z) -> bb);
            }
            // capital flare
            d.disc(px, 103, pz, 2.9, "gold_block");
            d.disc(px, 79, pz, 2.7, "polished_blackstone_bricks");
        }
    }

    void truss() {
        // dark steel ring truss hanging inside the dome
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int y = 104; y <= 107; y++) {
            final int yy = y;
            if (y == 104 || y == 107) d.ring(cx, yy, cz, 34.0, 35.0, y == 104 ? "polished_blackstone_bricks" : "polished_blackstone_slab[type=bottom]");
            else for (int x = CX - 36; x <= CX + 36; x++) for (int z = CZ - 36; z <= CZ + 36; z++) {
                double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (rr > 34.0 && rr <= 35.0) {
                    double aa = Math.toDegrees(Math.atan2(z + 0.5 - cz, x + 0.5 - cx));
                    int seg = Math.floorMod((int) Math.floor(aa / 4.0), 3);
                    d.set(x, yy, z, seg == 0 ? "polished_blackstone_wall" : (y == 105 ? "red_stained_glass_pane" : "air"));
                }
            }
            if (y == 104) d.ring(cx, 103, cz, 34.0, 35.0, "air");
        }
        // spokes from the column capitals to the truss ring, and chains up to the dome
        for (int c = 0; c < 8; c++) {
            double a = Math.toRadians(22.5 + c * 45);
            d.line(cx + Math.cos(a) * 44, 105, cz + Math.sin(a) * 44, cx + Math.cos(a) * 34.5, 105, cz + Math.sin(a) * 34.5, "polished_blackstone_bricks");
            double a2 = Math.toRadians(c * 45);
            int x = (int) Math.floor(cx + Math.cos(a2) * 34.5), z = (int) Math.floor(cz + Math.sin(a2) * 34.5);
            for (int y = 108; y <= 125; y++) d.set(x, y, z, "chain[axis=y]");
            for (int k = 0; k < 4; k++) {
                double a3 = a2 + Math.toRadians(11.25 + k * 0);
                int lx = (int) Math.floor(cx + Math.cos(a3) * 34.5), lz = (int) Math.floor(cz + Math.sin(a3) * 34.5);
                d.set(lx, 103, lz, "lantern[hanging=true]");
            }
            int sx = (int) Math.floor(cx + Math.cos(a2 + 0.1) * 34.5), sz = (int) Math.floor(cz + Math.sin(a2 + 0.1) * 34.5);
            d.set(sx, 103, sz, "sea_lantern");
        }
        // hanging emblem boards (both faces show the logo cube)
        board(CX - 31, CZ, true);
        board(CX + 31, CZ, true);
        board(CX, CZ - 31, false);
        board(CX, CZ + 31, false);
    }

    /** Hanging logo board in the plane x=const (alongZ) or z=const. */
    void board(int bx, int bz, boolean planeX) {
        int W = 19, H = 17;
        int top = 103;
        for (int c = 0; c < W; c++) for (int r = 0; r < H; r++) {
            int x = planeX ? bx : bx - W / 2 + c, z = planeX ? bz - W / 2 + c : bz;
            int y = top - r;
            String b;
            if (c == 0 || r == 0 || c == W - 1 || r == H - 1) b = "gold_block";
            else if (c == 1 || r == 1 || c == W - 2 || r == H - 2) b = "black_concrete";
            else {
                String e = emblem(c - 2, r - 2, 15);
                b = e != null ? e : ((c + r) % 2 == 0 ? "blue_concrete" : "blue_terracotta");
            }
            d.set(x, y, z, b);
        }
        // hangers
        for (int y = top + 1; y <= 104; y++) {
            int o = W / 2 - 2;
            d.set(planeX ? bx : bx - o, y, planeX ? bz - o : bz, "chain[axis=y]");
            d.set(planeX ? bx : bx + o, y, planeX ? bz + o : bz, "chain[axis=y]");
        }
    }

    /**
     * Pixel art of the Block Showdown emblem: an isometric cube (gold top, red left, blue right) on a square
     * of size s x s. Returns null for background.
     */
    static String emblem(int c, int r, int s) {
        double cx = (s - 1) / 2.0;
        double x = c - cx;                 // horizontal offset
        double half = s / 2.0 - 0.5;
        double q = half / 2.0;             // vertical size of the top rhombus half
        double y = r;                      // from the top
        double topY = 0.5, midY = topY + q, botTop = topY + 2 * q;
        double bottom = s - 1.0;
        // top rhombus centred at (0, midY), half-width half, half-height q
        double ax = Math.abs(x) / half, ay = Math.abs(y - midY) / q;
        if (Math.abs(x) > half + 0.01) return null;
        if (ax + ay <= 1.0) {
            if (ax + ay > 0.86) return "orange_concrete";
            return y < midY - q * 0.2 ? "yellow_concrete" : "gold_block";
        }
        // sides: below the top rhombus edge, above the bottom edge
        double upperEdge = midY + q * (1 - ax);               // lower edge of the rhombus
        double lowerEdge = bottom - q * ax;                    // bottom V of the cube
        if (y >= upperEdge - 0.01 && y <= lowerEdge + 0.01) {
            boolean outline = Math.abs(x) > half - 0.6 || y > lowerEdge - 0.9 || Math.abs(x) < 0.4;
            if (x < 0) return outline ? "red_nether_bricks" : (y > (upperEdge + lowerEdge) / 2 + 1 ? "red_terracotta" : "red_concrete");
            return outline ? "black_concrete" : (y > (upperEdge + lowerEdge) / 2 + 1 ? "blue_terracotta" : "blue_concrete");
        }
        return null;
    }

    void chandelier() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int y = 124; y <= 145; y++) d.set(CX, y, CZ, "chain[axis=y]");
        d.ring(cx, 123, cz, 3.5, 5.6, "gold_block");
        d.ring(cx, 122, cz, 4.5, 5.6, "sea_lantern");
        d.ring(cx, 121, cz, 4.5, 5.6, "polished_blackstone");
        d.disc(cx, 123, cz, 2.6, "polished_blackstone");
        d.disc(cx, 122, cz, 1.6, "gold_block");
        for (int y = 115; y <= 121; y++) d.set(CX, y, CZ, y % 2 == 0 ? "gold_block" : "ochre_froglight");
        d.ring(cx, 118, cz, 1.5, 2.6, "gold_block");
        d.ring(cx, 117, cz, 1.5, 2.6, "end_rod[facing=down]");
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            int x = (int) Math.floor(cx + Math.cos(a) * 5.0), z = (int) Math.floor(cz + Math.sin(a) * 5.0);
            d.set(x, 120, z, "chain[axis=y]");
            d.set(x, 119, z, "lantern[hanging=true]");
            int x2 = (int) Math.floor(cx + Math.cos(a) * 9.0), z2 = (int) Math.floor(cz + Math.sin(a) * 9.0);
            d.line(cx + Math.cos(a) * 5.5, 123, cz + Math.sin(a) * 5.5, x2 + 0.5, 125, z2 + 0.5, "chain[axis=y]");
            d.set(x2, 124, z2, "end_rod[facing=down]");
        }
    }

    void tunnel() {
        for (int z = FRONT_Z; z >= CZ + 40; z--) for (int x = -6; x <= 6; x++) {
            if (r(x, z) < 40.5) continue;
            int ax = Math.abs(x);
            int ceil = 86 - (ax >= 5 ? 1 : 0) - (ax >= 6 ? 1 : 0);
            for (int y = TERRACE + 1; y <= ceil; y++) d.air(x, y, z);
            if (ax <= 2) d.set(x, TERRACE, z, "red_wool");
            else if (ax == 3) d.set(x, TERRACE, z, "gold_block");
            else d.set(x, TERRACE, z, "polished_andesite");
            d.set(x, ceil + 1, z, ax <= 4 && z % 3 == 0 ? "sea_lantern" : "quartz_block");
            if (ax == 6) for (int y = TERRACE + 1; y <= ceil; y++) d.set(x + (x > 0 ? 1 : -1), y, z, y % 4 == 0 ? "ochre_froglight" : "white_concrete");
        }
    }

    // ------------------------------------------------------------------------------------------------ glass dome
    void glassDome() {
        double cx = CX + 0.5, cz = CZ + 0.5, rx = 48.0, ry = 44.0;
        for (int x = CX - 49; x <= CX + 49; x++) for (int z = CZ - 49; z <= CZ + 49; z++) {
            for (int y = RIM + 1; y <= RIM + 45; y++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - RIM) / ry, dz = (z + 0.5 - cz) / rz(rx);
                double e = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (e > 1.0 || e < 1.0 - 1.6 / 46.0) continue;
                double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (rr < 5.5 && y > RIM + 40) continue; // oculus
                double a = Math.toDegrees(Math.atan2(z + 0.5 - cz, x + 0.5 - cx));
                double arc = Math.abs(((a % 22.5) + 22.5) % 22.5 - 11.25); // 0 at the middle between ribs
                boolean rib = (11.25 - arc) * Math.PI / 180.0 * rr < 0.55;
                boolean latRing = y == 112 || y == 124 || y == 135;
                String b;
                if (y <= RIM + 1) b = "quartz_block";
                else if (rib || latRing) b = (latRing && rib) ? "sea_lantern" : (rib ? "white_concrete" : "light_gray_concrete");
                else if (y < 112) b = d.pick(x, y, z, 51, "light_blue_stained_glass", 4, "cyan_stained_glass", 1, "glass", 1);
                else if (y < 124) b = d.pick(x, y, z, 52, "light_blue_stained_glass", 3, "glass", 2);
                else b = d.pick(x, y, z, 53, "glass", 3, "light_blue_stained_glass", 2);
                d.set(x, y, z, b);
            }
        }
        // oculus ring
        d.ring(cx, RIM + 42, cz, 4.5, 7.5, "gold_block");
        d.ring(cx, RIM + 43, cz, 4.5, 6.5, "polished_blackstone");
    }

    static double rz(double rx) { return rx; }

    void topper() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        int b = RIM + 43;
        d.disc(cx, b + 1, cz, 5.5, "polished_blackstone_bricks");
        d.disc(cx, b + 2, cz, 4.5, "polished_blackstone");
        d.disc(cx, b + 3, cz, 3.6, "gold_block");
        d.disc(cx, b + 4, cz, 3.0, "yellow_concrete");
        for (int y = b + 5; y <= b + 9; y++) d.disc(cx, y, cz, y == b + 7 ? 2.0 : 1.5, y == b + 7 ? "gold_block" : "yellow_concrete");
        // cup
        int c0 = b + 10;
        for (int y = c0; y <= c0 + 9; y++) {
            double t = (y - c0) / 9.0;
            double rr = 2.2 + 4.6 * Math.sqrt(t);
            double rin = y == c0 ? 0 : rr - 1.1;
            final int yy = y;
            d.ring(cx, yy, cz, rin, rr, y == c0 + 9 ? "gold_block" : (y % 3 == 0 ? "raw_gold_block" : "gold_block"));
        }
        // beacon inside the cup (base of gold blocks)
        d.fill(CX - 1, c0, CZ - 1, CX + 1, c0, CZ + 1, "gold_block");
        d.set(CX, c0 + 1, CZ, "beacon");
        for (int y = c0 + 2; y <= c0 + 9; y++) d.disc(cx, y, cz, Math.max(0, 2.2 + 4.6 * Math.sqrt((y - c0) / 9.0) - 1.15), "air");
        d.set(CX, c0 + 2, CZ, "yellow_stained_glass");
        // handles
        for (int s : new int[]{-1, 1}) {
            for (int k = 0; k <= 10; k++) {
                double t = k / 10.0 * Math.PI;
                double hx = cx + s * (5.5 + 2.6 * Math.sin(t)), hy = c0 + 2 + 5 * (1 - Math.cos(t)) / 2.0 * 1.4;
                d.set((int) Math.floor(hx), (int) Math.floor(hy), CZ, "gold_block");
            }
        }
        // a floating emblem cube above the trophy
        int ey = c0 + 15;
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) for (int z = -2; z <= 2; z++) {
            String m = y == 4 ? "gold_block" : (x + z < 0 ? "red_concrete" : "blue_concrete");
            if (y == 4 && (Math.abs(x) == 2 || Math.abs(z) == 2)) m = "yellow_concrete";
            d.set(CX + x, ey + y, CZ + z, m);
        }
        L.fireworkSpots.add(new double[]{cx, ey + 18, cz});
    }

    // ------------------------------------------------------------------------------------------------ gatehouse
    void gatehouse() {
        int zf = FRONT_Z;        // -10
        int zb = CZ + 38;        // -24
        int top = 104;
        for (int x = -27; x <= 27; x++) for (int z = zb; z <= zf; z++) {
            if (r(x, z) < R_IN + 0.2) continue; // never intrude into the bowl
            for (int y = TERRACE + 1; y <= top; y++) {
                boolean face = z == zf;
                String b;
                if (!face) b = (y % 6 == 0) ? "quartz_block" : "white_concrete";
                else b = gateFace(x, y);
                d.set(x, y, z, b);
            }
        }
        // entrance opening through the gatehouse
        for (int z = zb; z <= zf; z++) for (int x = -6; x <= 6; x++) {
            if (r(x, z) < 40.5) continue;
            int ax = Math.abs(x);
            int ceil = 86 - (ax >= 5 ? 1 : 0) - (ax >= 6 ? 1 : 0);
            for (int y = TERRACE + 1; y <= ceil; y++) d.air(x, y, z);
            d.set(x, TERRACE, z, ax <= 2 ? "red_wool" : ax == 3 ? "gold_block" : "polished_andesite");
            if (ax == 6 || ax == 5) d.set(x, ceil + 1, z, "gold_block");
        }
        // marquee (logo sign) above, 3 deep
        int mTop = 126, mBot = 105;
        for (int x = -28; x <= 28; x++) for (int y = mBot; y <= mTop; y++) for (int z = zf - 2; z <= zf; z++) {
            boolean frame = Math.abs(x) >= 27 || y == mBot || y == mTop;
            boolean frame2 = Math.abs(x) == 26 || y == mBot + 1 || y == mTop - 1;
            String b = frame ? "gold_block" : frame2 ? (z == zf ? "yellow_concrete" : "gold_block") : (z == zf ? logoPixel(x, y) : "blue_concrete");
            d.set(x, y, z, b);
        }
        // bulb lights around the marquee frame
        for (int x = -28; x <= 28; x += 2) { d.set(x, mTop + 1, zf, "ochre_froglight"); d.set(x, mBot - 1 + 0, zf + 1, "end_rod[facing=south]"); }
        for (int y = mBot; y <= mTop; y += 2) { d.set(-29, y, zf, "ochre_froglight"); d.set(29, y, zf, "ochre_froglight"); }
        // canopy over the doors
        for (int x = -9; x <= 9; x++) for (int z = zf + 1; z <= zf + 3; z++) d.set(x, 88, z, Math.abs(x) == 9 || z == zf + 3 ? "gold_block" : "red_concrete");
        for (int x = -8; x <= 8; x += 4) d.set(x, 87, zf + 3, "lantern[hanging=true]");
        // side towers
        for (int s : new int[]{-1, 1}) tower(s * 31, -14, 70, 130, true);
        // clock
        L.clockX0 = -12; L.clockY0 = 92; L.clockZ = zf;
        drawClock("--:--");
        L.label(0.5, 104.2, zf + 1.6, 0, "§e§lNEXT GAME");
        L.entrance[0] = 0.5; L.entrance[1] = TERRACE + 1; L.entrance[2] = zf + 4.5; L.entrance[3] = 180;
    }

    void drawClock(String text) {
        for (int c = -3; c < Glyphs.CLOCK_W + 3; c++) for (int r = -2; r < Glyphs.SEG_H + 2; r++) {
            int x = L.clockX0 + c, y = L.clockY0 + (Glyphs.SEG_H - 1 - r);
            boolean in = c >= 0 && c < Glyphs.CLOCK_W && r >= 0 && r < Glyphs.SEG_H;
            String b = in && Glyphs.clockPixel(text, c, r) ? L.clockOn : "black_concrete";
            d.set(x, y, L.clockZ, b);
        }
        // protruding frame
        for (int c = -4; c <= Glyphs.CLOCK_W + 3; c++) for (int r = -3; r <= Glyphs.SEG_H + 2; r++) {
            boolean edge = c == -4 || c == Glyphs.CLOCK_W + 3 || r == -3 || r == Glyphs.SEG_H + 2;
            if (!edge) continue;
            int x = L.clockX0 + c, y = L.clockY0 + (Glyphs.SEG_H - 1 - r);
            d.set(x, y, L.clockZ, "gold_block");
            d.set(x, y, L.clockZ + 1, (r == -3 || r == Glyphs.SEG_H + 2) ? "polished_blackstone_slab[type=" + (r == -3 ? "bottom" : "top") + "]" : "polished_blackstone_wall");
        }
    }

    String gateFace(int x, int y) {
        int ax = Math.abs(x);
        // clock zone handled by drawClock
        if (y <= TERRACE + 1) return "polished_blackstone";
        if (y == 89 || y == 103) return "gold_block";
        if (ax >= 25) return y % 3 == 0 ? "gold_block" : "white_concrete";
        if (ax >= 20 && ax <= 21) return "blue_concrete";
        if (y < 89) {
            if (ax == 7 || ax == 8) return "gold_block";
            if (ax == 9 || ax == 10) return y % 2 == 0 ? "blue_concrete" : "light_blue_concrete";
            return (y + ax) % 8 == 0 ? "red_terracotta" : "red_concrete";
        }
        if (y > 89 && y < 103) return ax > 17 ? ((y % 4 == 0) ? "smooth_quartz" : "red_concrete") : "black_concrete";
        return "white_concrete";
    }

    /** Logo pixel for the marquee face at (x, y): emblem cube, "BLOCK" in gold, "SHOWDOWN" in white. */
    String logoPixel(int x, int y) {
        String bg = (Math.floorMod(x + y, 2) == 0) ? "blue_concrete" : "blue_terracotta";
        // SHOWDOWN: rows y 107..113 (top 113), width 47 centred
        int sw = Glyphs.textWidth("SHOWDOWN");
        int sx0 = -sw / 2;
        if (y >= 107 && y <= 113) {
            int col = x - sx0, row = 113 - y;
            if (hit("SHOWDOWN", col, row)) return row >= 5 ? "light_blue_concrete" : "white_concrete";
        }
        // BLOCK: rows 116..122, starts at x=-7
        if (y >= 116 && y <= 122) {
            int col = x - (-6), row = 122 - y;
            if (hit("BLOCK", col, row)) return row <= 1 ? "yellow_concrete" : row <= 4 ? "gold_block" : "orange_concrete";
        }
        // emblem 13x13 at x -23..-11, y 113..125 -> rows from top 125
        if (x >= -24 && x <= -10 && y >= 111 && y <= 125) {
            String e = emblem(x + 24, 125 - y, 15);
            if (e != null) return e;
        }
        if (x >= 20 && x <= 24 && y >= 116 && y <= 122) { // little star accent on the right
            int cx = 22, cy = 119;
            if (Math.abs(x - cx) + Math.abs(y - cy) <= 2 || x == cx || y == cy && Math.abs(x - cx) <= 2) return "yellow_concrete";
        }
        return bg;
    }

    static boolean hit(String text, int col, int row) {
        final boolean[] f = {false};
        if (col < 0 || row < 0) return false;
        Glyphs.text(text, (c, r) -> { if (c == col && r == row) f[0] = true; });
        return f[0];
    }

    /** Square banner tower (also used by the plaza gate). */
    void tower(int cx, int cz, int y0, int y1, boolean searchlight) {
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) for (int y = y0; y <= y1; y++) {
            boolean corner = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
            boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
            String b;
            int band = (y - y0) % 8;
            if (corner) b = y % 2 == 0 ? "quartz_pillar[axis=y]" : "quartz_block";
            else if (!edge) b = "white_concrete";
            else if (band == 0) b = "gold_block";
            else if (band == 4) b = "blue_concrete";
            else if (band >= 5 && (x == cx || z == cz)) b = "ochre_froglight";
            else b = y > y1 - 10 ? "white_concrete" : "red_concrete";
            d.set(x, y, z, b);
        }
        // crown cap
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
            d.set(x, y1 + 1, z, (Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) ? "gold_block" : "polished_blackstone");
            if ((Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) && (x + z) % 2 == 0) d.set(x, y1 + 2, z, "quartz_slab[type=bottom]");
        }
        if (searchlight) {
            d.fill(cx - 1, y1 + 1, cz - 1, cx + 1, y1 + 1, cz + 1, "iron_block");
            d.set(cx, y1 + 2, cz, "beacon");
            L.searchlights.add(new double[]{cx + 0.5, y1 + 3, cz + 0.5, (cx * 0.37 + cz * 0.11)});
        }
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) g.paved(x, z, y1 + 1);
    }

    // ------------------------------------------------------------------------------------------------ pylons
    void pylons() {
        for (int k = 0; k < 12; k++) {
            double deg = k * 30.0;
            if (Math.abs(deg - 90) < 35) continue;
            double a = Math.toRadians(deg);
            double ux = Math.cos(a), uz = Math.sin(a), tx = -uz, tz = ux;
            for (int y = 70; y <= 100; y++) {
                double outR = 58.5 - (y - 70) * 0.31;
                for (double rr = R_OUT - 0.5; rr <= outR; rr += 0.4) for (double w = -1.4; w <= 1.4; w += 0.4) {
                    int x = (int) Math.floor(CX + 0.5 + ux * rr + tx * w), z = (int) Math.floor(CZ + 0.5 + uz * rr + tz * w);
                    boolean mid = Math.abs(w) < 0.5;
                    boolean outerEdge = rr > outR - 0.8;
                    String b = mid && outerEdge ? (y % 6 < 3 ? "orange_concrete" : "orange_terracotta") : mid ? "orange_concrete" : (y % 5 == 0 ? "smooth_quartz" : "white_concrete");
                    if (y == 70) b = "polished_blackstone";
                    d.set(x, y, z, b);
                }
            }
            // light at the foot
            int fx = (int) Math.floor(CX + 0.5 + ux * 59.5), fz = (int) Math.floor(CZ + 0.5 + uz * 59.5);
            d.set(fx, 71, fz, "ochre_froglight");
        }
    }

    void rimFlags() {
        for (int k = 0; k < 30; k++) {
            double deg = k * 12.0 + 6;
            if (Math.abs(deg - 90) < 30) continue;
            double a = Math.toRadians(deg);
            int x = (int) Math.floor(CX + 0.5 + Math.cos(a) * 49.6), z = (int) Math.floor(CZ + 0.5 + Math.sin(a) * 49.6);
            String dye = Draw.TEAM_DYES[k % 10];
            boolean alongX = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a));
            boolean flip = alongX ? Math.cos(a) < 0 : Math.sin(a) < 0;
            d.flag(x, RIM + 1, z, 7, dye, !alongX, alongX ? Math.sin(a) < 0 : Math.cos(a) > 0);
        }
    }

    // ------------------------------------------------------------------------------------------------ layout
    void layout() {
        L.interiorSpawn[0] = CX + 0.5; L.interiorSpawn[1] = STAGE + 1; L.interiorSpawn[2] = CZ + 0.5 + 12; L.interiorSpawn[3] = 180;
        L.timer[0] = CX + 0.5; L.timer[1] = 84; L.timer[2] = CZ + 0.5;
        for (int k = 0; k < 120; k++) {
            double deg = k * 3.0;
            if (Math.abs(deg - 90) < 12) continue;
            double a = Math.toRadians(deg);
            double rr = (k % 2 == 0) ? 35.8 : 41.8;
            double y = (k % 2 == 0) ? 72 : 77;
            double x = CX + 0.5 + Math.cos(a) * rr, z = CZ + 0.5 + Math.sin(a) * rr;
            float yaw = (float) Math.toDegrees(Math.atan2(-(CX + 0.5 - x), CZ + 0.5 - z));
            L.seats.add(new double[]{x, y, z, yaw});
        }
        L.searchlights.add(new double[]{CX + 0.5 - 40, TERRACE + 1, CZ + 0.5 + 33, 1.3});
        L.searchlights.add(new double[]{CX + 0.5 + 40, TERRACE + 1, CZ + 0.5 + 33, 2.1});
        L.searchlights.add(new double[]{CX + 0.5 - 46, TERRACE + 1, CZ + 0.5 - 22, 0.4});
        L.searchlights.add(new double[]{CX + 0.5 + 46, TERRACE + 1, CZ + 0.5 - 22, 3.3});
        for (double[] s : L.searchlights.subList(L.searchlights.size() - 4, L.searchlights.size())) {
            int x = (int) Math.floor(s[0]), z = (int) Math.floor(s[2]);
            d.fill(x - 1, TERRACE, z - 1, x + 1, TERRACE, z + 1, "iron_block");
            d.set(x, TERRACE + 1, z, "beacon");
            d.set(x, TERRACE + 2, z, "white_stained_glass");
            s[1] = TERRACE + 2;
        }
        L.fireworkSpots.add(new double[]{CX - 30, 150, CZ});
        L.fireworkSpots.add(new double[]{CX + 30, 150, CZ});
        L.fireworkSpots.add(new double[]{CX, 140, CZ + 40});
    }
}
