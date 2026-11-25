package com.mcm.lobby;

import java.util.ArrayList;
import java.util.List;

/**
 * The Decision Dome venue. The outside is our own build: terrace, drum facade, ribbed glass dome, brass armillary
 * topper, gatehouse with clock + marquee and the entrance tunnel. The inside (v6) is the downloaded "Decision
 * Dome - Recreation" placed 1:1 from {@link HubRecreation}: its drum and terrace, the 42-deep pit, the ten team
 * cage towers, the floating eight-wedge dial and the gold crown. Between the recreation's terrace (r 28) and our
 * inner wall (r 37) a paved ring with two low spectator tiers joins the two builds, and the red-carpet entrance
 * corridor runs straight from the tunnel onto the recreation's terrace.
 * The v4/v5 interior methods (dial, pods, stands, crown ...) are kept below but are no longer called.
 */
final class HubVenue {
    final HubGenerator g;
    final Draw d;
    final HubLayout L;
    static final int CX = HubGenerator.DOME_X, CZ = HubGenerator.DOME_Z;
    static final int TERRACE = 76;      // terrace / entrance floor block y
    static final int STAGE = 64;        // dial floor block y (top y 65)
    static final int POD_FLOOR = 65;    // pod floor block y (top y 66)
    static final int RIM = 102;         // drum top
    static final double R_OUT = 48.5, R_IN = 45.5, R_TERRACE = 56.5;
    static final double DIAL_R = 12.0;
    static final double POD_IN = 13.4, POD_OUT = 19.4, POD_R = 14.2;
    static final double CONC_IN = 19.6, CONC_OUT = 23.4;
    static final double STAND_IN = 23.6, STAND_OUT = 35.4;
    static final double WALL_R = 37.0;
    static final int FRONT_Z = CZ + 52;  // gatehouse front face (z = -10)

    /** First block of the tower shell above the pod roof (the room's glass roof is at POD_FLOOR + 5). */
    static final int TOWER_Y0 = POD_FLOOR + 6;   // 71
    /** Gold/copper belt under the tower's glass collar. */
    static final int TOWER_BELT = 80;
    /** Flared crown band of the tower. */
    static final int TOWER_CROWN = 83;
    /** Gold cap + finial of the tower. */
    static final int TOWER_CAP = 86;
    /** Base block of the floating crown truss. */
    static final int CROWN_Y = 82;
    /** Yellow top ring of the crown. */
    static final int CROWN_RING = 91;

    /** Ten pod centres, degrees; the 36 degree gap at 90 is the stair. */
    static final int[] POD_ANGLE = {18, 54, 72, 108, 126, 162, 198, 234, 270, 306};
    /** The ten team jewel colours, in pod order. */
    static final String[] POD_DYES = Draw.TEAM_DYES;
    static final String[] WEDGE_DYES8 = {"red", "orange", "yellow", "lime", "cyan", "blue", "purple", "pink"};
    static final String[] WEDGE_DYES6 = {"red", "yellow", "lime", "cyan", "blue", "pink"};
    static final int[] AISLE_ANGLES = {0, 45, 135, 180, 225, 315};

    HubVenue(HubGenerator g) { this.g = g; this.d = g.d; this.L = g.L; }

    static double r(int x, int z) { return Math.hypot(x - CX, z - CZ); }
    static double ang(int x, int z) { double a = Math.atan2(z - CZ, x - CX); return a < 0 ? a + Math.PI * 2 : a; }
    static double angDeg(int x, int z) { return Math.toDegrees(ang(x, z)); }
    /** Angular distance (deg) from the front (+Z = 90 deg). */
    static double fromFront(int x, int z) { return Math.abs(angDeg(x, z) - 90); }

    // ------------------------------------------------------------------------------------------------ v6 constants
    /** Translation from the recreation's coordinates: centre (-3, 16) -> (CX, CZ), its terrace y68 -> TERRACE. */
    static final int REC_DX = CX + 3, REC_DY = TERRACE - 68, REC_DZ = CZ - 16;
    /** Dial floor block y (bottom smooth-quartz slabs, standing surface DIAL_Y + 0.5). */
    static final int DIAL_Y = 58 + REC_DY;
    static final double DIAL_SURFACE = DIAL_Y + 0.5;
    /** Live wedge floor: inside the recreation's r9 rim wall; only the converging divider lines are neutral. */
    static final double DIAL_OUT = 8.3, DIAL_IN = 1.0;
    /** Top course of the dial rim wall; repainted per wedge at runtime (the floor itself stays 1:1). */
    static final int DIAL_RIM_TOP = 60 + REC_DY;
    /** Cage floor block y inside each team tower (players stand at CAGE_FLOOR + 1). */
    static final int CAGE_FLOOR = 64 + REC_DY;
    /** Columns cleared for the recreation's pit (its drum shell sits at r 26). */
    static final double PIT_R = 26.0;
    /** Ring tiers between the recreation terrace and the inner wall: y77 from r 31, y78 (top walk) from r 33. */
    static final double TIER1 = 31.0, TIER2 = 33.0;
    /** Wedge 0 centre angles: the recreation's dividers lie on 0/45/90..., so the 8-game wedges are centred on 22.5 + 45k. */
    static final double WEDGE_BASE8 = 67.5, WEDGE_BASE6 = 90.0;
    /** Recreation cage centres (source x, z) in team order red .. pink ({@link Draw#TEAM_DYES}). */
    static final int[][] CAGE_SRC = {{-18, 21}, {-18, 11}, {-12, 3}, {-3, 0}, {6, 3}, {12, 11}, {12, 21}, {6, 29}, {-3, 32}, {-12, 29}};

    void build() {
        L.domeX = CX; L.domeZ = CZ; L.stageY = DIAL_Y; L.domeRadius = 48; L.innerRadius = 45;
        L.dialCapacity = 8;
        L.dialGeneration = HubGenerator.VERSION;
        L.dialRadius = DIAL_OUT;
        L.dialInner = DIAL_IN;
        L.dialBandY = DIAL_RIM_TOP;
        L.dialCentre = new double[]{CX + 0.5, DIAL_SURFACE, CZ + 0.5};
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

    // ================================================================================================ palette helpers
    /** Runtime accent block for a kind + dye. Kept in one place so the build and the venue runtime agree. */
    static String accentBlock(int kind, String dye, boolean lit) {
        boolean off = !lit || dye == null;
        return switch (kind) {
            case HubLayout.K_CONCRETE -> dye == null ? "polished_blackstone" : dye + "_concrete";
            case HubLayout.K_GLASS -> dye == null ? "tinted_glass" : dye + "_stained_glass";
            case HubLayout.K_WOOL -> dye == null ? "black_wool" : dye + "_wool";
            case HubLayout.K_LIGHT -> off ? "black_concrete" : "ochre_froglight";
            case HubLayout.K_CARPET -> dye == null ? "black_carpet" : dye + "_carpet";
            case HubLayout.K_PANE -> dye == null ? "tinted_glass_pane" : dye + "_stained_glass_pane";
            case HubLayout.K_TERRACOTTA -> dye == null ? "deepslate_tiles" : dye + "_terracotta";
            case HubLayout.K_DARK -> "polished_blackstone"; default -> "polished_blackstone";
        };
    }

    private void accent(List<int[]> list, int x, int y, int z, int kind) {
        list.add(new int[]{x, y, z, kind});
    }

    private void accentSet(List<int[]> list, int x, int y, int z, int kind, String dye, boolean lit) {
        accent(list, x, y, z, kind);
        d.set(x, y, z, accentBlock(kind, dye, lit));
    }

    // ================================================================================================ foundation
    void foundation() {
        int R = (int) R_TERRACE + 2;
        for (int x = CX - R - 4; x <= CX + R + 4; x++) for (int z = CZ - R - 4; z <= CZ + R + 4; z++) {
            double rr = r(x, z);
            if (rr > R_TERRACE) continue;
            for (int y = 58; y < TERRACE; y++) d.set(x, y, z, y < 70 ? "deepslate_tiles" : terraceWall(x, y, z, rr));
            // terrace surface: dark basalt with cream bands and copper seams
            String top;
            if (rr > R_TERRACE - 1.2) top = "polished_blackstone";
            else if (rr > 52.5 && rr <= 53.5) top = (Math.floorMod((int) Math.round(angDeg(x, z) * 2), 9) == 0) ? "ochre_froglight" : "waxed_oxidized_copper";
            else if (rr > 50.5 && rr <= 51.1) top = "calcite";
            else top = d.pick(x, TERRACE, z, 11, "polished_deepslate", 4, "deepslate_tiles", 2, "polished_blackstone", 2, "smooth_basalt", 1);
            d.set(x, TERRACE, z, top);
            g.paved(x, z, TERRACE);
            for (int y = TERRACE + 1; y < TERRACE + 6; y++) d.air(x, y, z);
            // parapet: blackstone posts and calcite rails
            if (rr > R_TERRACE - 1.2 && fromFront(x, z) > 16) {
                d.set(x, TERRACE + 1, z, (Math.floorMod(x + z, 6) == 0) ? "calcite" : "polished_blackstone_slab[type=bottom]");
            }
        }
        // the paved apron just outside the terrace
        for (int x = CX - 64; x <= CX + 64; x++) for (int z = CZ - 64; z <= CZ + 64; z++) {
            double rr = r(x, z);
            if (rr <= R_TERRACE || rr > 61.5) continue;
            if (z > -8 && Math.abs(x) <= 31) continue;
            int gy = g.hAt(x, z);
            if (gy < HubGenerator.GROUND - 2 || gy > HubGenerator.GROUND + 2) continue;
            for (int y = gy; y < HubGenerator.GROUND; y++) d.set(x, y, z, "stone");
            for (int y = HubGenerator.GROUND + 1; y < HubGenerator.GROUND + 4; y++) d.air(x, y, z);
            d.set(x, HubGenerator.GROUND, z, rr > 60.5 ? "stone_bricks" : d.pick(x, 0, z, 12, "deepslate_tiles", 4, "polished_deepslate", 2, "stone_bricks", 2, "tuff", 1));
            g.paved(x, z, HubGenerator.GROUND);
        }
        // front steps from the approach to the terrace (kept from the original hub)
        g.staircase("dome entrance", 0, HubGenerator.APPROACH_Y, CZ + 60, 0, TERRACE, CZ + 56, "north", 31,
                x -> Math.abs(x) <= 3 ? "red_nether_brick" : "quartz", x -> Math.abs(x) <= 3 ? "red_nether_bricks" : "quartz_block");
        for (int x = -15; x <= 15; x++) {
            for (int z = CZ + 54; z <= CZ + 56; z++) {
                d.set(x, TERRACE, z, Math.abs(x) <= 3 ? "red_wool" : "polished_blackstone");
                for (int yy = TERRACE + 1; yy < TERRACE + 5; yy++) d.air(x, yy, z);
                g.paved(x, z, TERRACE);
            }
        }
        // carpet across the landing to the doors
        for (int z = FRONT_Z; z <= CZ + 56; z++) for (int x = -3; x <= 3; x++)
            d.set(x, TERRACE, z, Math.abs(x) == 3 ? "gold_block" : "red_wool");
        for (int sx : new int[]{-12, 12}) d.brazier(sx, TERRACE + 1, CZ + 55, false);
    }

    String terraceWall(int x, int y, int z, double rr) {
        if (rr < R_TERRACE - 1.2) return "stone_bricks";
        if (y == TERRACE - 1) return "calcite";
        if (y == 70) return "polished_blackstone";
        double a = angDeg(x, z);
        if (Math.floorMod((int) Math.floor(a / 6.0), 2) == 0) return y == 73 ? "ochre_froglight" : d.pick(x, y, z, 13, "basalt[axis=y]", 3, "polished_basalt", 2, "blackstone", 1);
        return d.pick(x, y, z, 14, "polished_deepslate", 3, "deepslate_tiles", 2, "tuff", 1);
    }

    // ================================================================================================ drum
    void drumAndFacade() {
        int R = 50;
        for (int x = CX - R; x <= CX + R; x++) for (int z = CZ - R; z <= CZ + R; z++) {
            double rr = r(x, z);
            if (rr <= R_IN || rr > R_OUT + 2.0) continue;
            double a = angDeg(x, z);
            for (int y = TERRACE + 1; y <= RIM + 2; y++) {
                if (rr <= R_OUT) {
                    boolean outer = rr > R_OUT - 1.0;
                    d.set(x, y, z, outer ? facade(x, y, z, a) : innerWall(x, y, z, a));
                } else if (rr <= R_OUT + 1.0) {
                    if (y == RIM - 2) d.set(x, y, z, "calcite");
                    else if (y == RIM - 1) d.set(x, y, z, Math.floorMod((int) Math.round(a * 2), 6) == 0 ? "ochre_froglight" : "smooth_quartz");
                    else if (y == RIM) d.set(x, y, z, "calcite");
                    else if (y == RIM + 1) d.set(x, y, z, Math.floorMod((int) Math.round(a), 4) < 2 ? "calcite" : "air");
                    else if (y == TERRACE + 1) d.set(x, y, z, "polished_blackstone");
                } else if (y == RIM) d.set(x, y, z, "smooth_quartz_slab[type=top]");
            }
        }
        // copper ribs every 15 degrees on the outer face
        for (int k = 0; k < 24; k++) {
            double a = Math.toRadians(k * 15.0);
            for (int y = TERRACE + 3; y <= RIM - 3; y++) {
                for (double t = -0.5; t <= 0.5; t += 0.35) {
                    int x = (int) Math.floor(CX + 0.5 + Math.cos(a) * (R_OUT - 0.2) + Math.cos(a + Math.PI / 2) * t);
                    int z = (int) Math.floor(CZ + 0.5 + Math.sin(a) * (R_OUT - 0.2) + Math.sin(a + Math.PI / 2) * t);
                    d.set(x, y, z, y % 9 == 0 ? "gold_block" : (y > 88 && y < 96 ? "waxed_oxidized_copper" : "oxidized_copper"));
                }
            }
        }
        // base plinth and cream cornice rings
        d.ring(CX + 0.5, TERRACE + 1, CZ + 0.5, R_OUT - 1.2, R_OUT + 1.2, "polished_blackstone");
        d.ring(CX + 0.5, TERRACE + 2, CZ + 0.5, R_OUT - 1.2, R_OUT + 1.1, "chiseled_polished_blackstone");
        d.ring(CX + 0.5, RIM, CZ + 0.5, R_OUT - 1.3, R_OUT + 1.3, "calcite");
        d.ring(CX + 0.5, RIM + 1, CZ + 0.5, R_OUT - 1.0, R_OUT + 1.0, "smooth_quartz");
    }

    String facade(int x, int y, int z, double a) {
        double panel = a / 10.0;
        double frac = panel - Math.floor(panel);
        boolean seam = frac < 0.09 || frac > 0.96;
        int pi = (int) Math.floor(panel);
        if (y <= TERRACE + 2) return y == TERRACE + 1 ? "polished_blackstone" : "deepslate_tiles";
        if (y == TERRACE + 3) return Math.floorMod((int) Math.round(a * 2), 3) == 0 ? "gold_block" : "waxed_oxidized_copper";
        // glazed clerestory band so the lit interior reads outside
        if (y >= 88 && y <= 95) {
            if (seam || y == 88 || y == 95) return "cut_copper"; if (frac > 0.25 && frac < 0.8) return (pi % 2 == 0) ? "light_blue_stained_glass" : "cyan_stained_glass";
            return "polished_blackstone";
        }
        if (y == 87 || y == 96) return "calcite";
        if (y >= 97 && y <= 100) {
            if (seam) return "smooth_quartz";
            return pi % 2 == 0 ? "polished_basalt" : "deepslate_tiles";
        }
        if (y >= RIM - 2) return y == RIM - 1 ? "calcite" : "gold_block";
        if (seam) return y % 4 == 0 ? "calcite" : "smooth_quartz";
        if (y < 83) return y == TERRACE + 4 ? "chiseled_polished_blackstone" : (frac > 0.35 && frac < 0.7 ? "deepslate_tiles" : "polished_basalt");
        return "polished_basalt";
    }

    String innerWall(int x, int y, int z, double a) {
        // upper wall: dark cathedral with copper ribs, team banners and a planetarium band
        if (y >= 97) {
            if (y == 102) return "calcite";
            if (Math.floorMod((int) Math.round(a), 12) == 0) return "waxed_oxidized_copper";
            double star = d.rnd(x, y, z, 921);
            if (star < 0.018) return "sea_lantern";
            if (star < 0.035) return "light_blue_stained_glass";
            return d.pick(x, y, z, 922, "black_concrete", 5, "blue_concrete", 1, "polished_blackstone", 1);
        }
        if (y == 96) return "calcite";
        if (y >= 89 && y <= 95) {
            // cream flute panels between dark recesses
            double frac = a / 12.0 - Math.floor(a / 12.0);
            if (frac < 0.12) return "polished_blackstone";
            if (Math.floorMod((int) Math.round(a / 3.0), 2) == 0) return d.pick(x, y, z, 923, "calcite", 4, "smooth_quartz", 2, "bone_block", 1);
            return d.pick(x, y, z, 924, "polished_deepslate", 4, "deepslate_tiles", 2);
        }
        if (y == 88) return "waxed_oxidized_copper";
        if (y >= 82 && y <= 87) {
            // ten team banners, one per pod, separated by copper ribs
            int seg = Math.floorMod((int) Math.floor((a + 90) / 36.0), 10);
            double f = (a + 90) / 36.0 - Math.floor((a + 90) / 36.0);
            if (f < 0.14 || f > 0.92) return "polished_blackstone";
            if (y == 87) return "gold_block";
            if (y == 82) return "waxed_oxidized_copper";
            return POD_DYES[seg] + (y == 84 ? "_wool" : (y == 86 ? "_concrete" : "_terracotta"));
        }
        if (y >= 78 && y <= 81) {
            if (Math.floorMod((int) Math.round(a), 15) == 0) return "oxidized_copper";
            return d.pick(x, y, z, 925, "polished_basalt", 3, "deepslate_tiles", 2, "blackstone", 1);
        }
        return d.pick(x, y, z, 926, "polished_deepslate", 3, "deepslate_tiles", 2, "blackstone", 1);
    }

    // ================================================================================================ interior
    /** Stand surface height at radius rr (top block y). */
    static int standTop(double rr) {
        int k = (int) Math.floor(rr);
        if (k < 24) return STAGE;
        if (k <= 26) return 67 + (k - 24);      // tier 1: 67..69
        if (k == 27) return 70;                 // landing
        if (k <= 30) return 71 + (k - 28);      // tier 2: 71..73
        if (k == 31) return 74;                 // landing
        if (k <= 34) return 75 + (k - 32);      // tier 3: 75..77
        if (k == 35) return 78;                 // top walk
        return 78;
    }

    void interior() {
        int R = 46;
        for (int x = CX - R; x <= CX + R; x++) for (int z = CZ - R; z <= CZ + R; z++) {
            double rr = r(x, z);
            if (rr > R_IN) continue;
            if (rr <= PIT_R) {
                // the recreation's pit: clear the terrain/foundation down to its base disk (placed by recreation())
                for (int y = 35; y <= 146; y++) d.air(x, y, z);
                g.paved(x, z, 34);
                continue;
            }
            boolean corridor = Math.abs(x) <= 6 && z > CZ;
            int top = corridor ? TERRACE : ringTop(rr);
            for (int y = 58; y <= 146; y++) {
                if (y > top) {
                    // the shell beyond the inner wall is hollow; the outer drum wall carries the ceiling
                    if (rr > WALL_R - 1.0 && y > 78) continue;
                    d.air(x, y, z);
                    continue;
                }
                d.set(x, y, z, y < top - 3 ? "stone" : y < top ? "deepslate_tiles" : ringBlock(x, z, rr, top, corridor));
            }
            g.paved(x, z, top);
        }
        recreation();
        recreationSafety();
        ringSeats();
        innerWallShell();
        recreationAnchors();
        recreationLights();
        // two white/copper boards on the east and west flanks, framing the crown from the ring
        hangingScreen(0, 29.0, 86, 11, 5);
        hangingScreen(180, 29.0, 86, 11, 5);
        tunnel();
    }

    // ================================================================================================ v6: recreation
    /** Place the downloaded recreation 1:1 (every block state unchanged), translated onto the hub. */
    void recreation() {
        HubRecreation.forEach((x, y, z, state) -> d.set(x + REC_DX, y + REC_DY, z + REC_DZ, state));
    }

    /** Ring floor height: flat walk to r 31, tier 1 to r 33, then the top walk up to the inner wall. */
    static int ringTop(double rr) {
        if (rr < TIER1) return TERRACE;
        if (rr < TIER2) return TERRACE + 1;
        return TERRACE + 2;
    }

    /** Top block of the joining ring: white quartz paving like the recreation's rim, copper rays, warm lamps. */
    String ringBlock(int x, int z, double rr, int top, boolean corridor) {
        double a = angDeg(x, z);
        if (corridor) {
            // the red carpet runs from the tunnel straight onto the recreation's terrace
            int ax = Math.abs(x);
            return ax <= 2 ? "red_wool" : ax == 3 ? "gold_block" : ax == 6 ? "calcite" : "polished_deepslate";
        }
        // risers: one stair ring per tier, so every tier is walked, not jumped
        if (rr >= TIER1 && rr < TIER1 + 1.0) return Draw.stairs("smooth_quartz", Draw.facingToward(CX, CZ, x, z), false);
        if (rr >= TIER2 && rr < TIER2 + 1.0) return Draw.stairs("polished_deepslate", Draw.facingToward(CX, CZ, x, z), false);
        if (top == TERRACE) {
            if (Math.abs(rr - 29.6) < 0.32) return "gray_concrete";               // echo of the recreation's grey rim
            if (rr > 30.0 && Math.abs(angDiff(a, Math.round(a / 36.0) * 36.0)) * Math.PI / 180.0 * rr < 0.5)
                return "ochre_froglight";                                        // warm floor lamp every 36 degrees
            if (Math.abs(angDiff(a, Math.round(a / 18.0) * 18.0)) * Math.PI / 180.0 * rr < 0.45) return "cut_copper";
            return d.pick(x, top, z, 961, "smooth_quartz", 9, "calcite", 4, "quartz_block", 3, "white_concrete", 2);
        }
        if (top == TERRACE + 1) return d.pick(x, top, z, 962, "calcite", 5, "smooth_quartz", 3, "polished_diorite", 1);
        if (Math.abs(angDiff(a, Math.round(a / 15.0) * 15.0)) * Math.PI / 180.0 * rr < 0.5) return "cut_copper";
        return d.pick(x, top, z, 963, "polished_deepslate", 4, "deepslate_tiles", 3, "polished_blackstone", 2);
    }

    /**
     * The recreation's own safety: a white glass kerb on the terrace (r 25-26) and an invisible barrier ring at
     * r 24-25. Where its outside pieces were left out the kerb has gaps; close every one so nobody falls 42 blocks.
     */
    void recreationSafety() {
        int ky = 69 + REC_DY, by = 71 + REC_DY;
        for (int x = CX - 28; x <= CX + 28; x++) for (int z = CZ - 28; z <= CZ + 28; z++) {
            double rr = r(x, z);
            if (rr >= 24.9 && rr <= 26.6 && d.isAir(x, ky, z) && !d.isAir(x, ky - 1, z))
                d.set(x, ky, z, "white_stained_glass_pane");
            if (rr >= 23.9 && rr <= 25.4 && d.isAir(x, by, z) && d.isAir(x, ky - 1, z))
                d.set(x, by, z, "barrier");
        }
    }

    /** 120 spectator spots on the two tiers, never in the entrance corridor. */
    void ringSeats() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        double[] rows = {32.0, 35.0};
        for (double rr : rows) {
            for (int s = 0; s < 60; s++) {
                double deg = 3.0 + s * 6.0;
                double off = angDiff(deg, 90);
                if (Math.abs(off) < 13) deg = 90 + (off < 0 ? -13 - (13 + off) : 13 + (13 - off));
                double a = Math.toRadians(deg);
                double x = cx + Math.cos(a) * rr, z = cz + Math.sin(a) * rr;
                float yaw = (float) Math.toDegrees(Math.atan2(-(cx - x), cz - z));
                L.seats.add(new double[]{x, ringTop(rr) + 1.0, z, yaw});
            }
        }
    }

    /** Runtime anchors over the recreation: pods, wedges, band, dividers, reveal faces, clock, podium. */
    void recreationAnchors() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // ten team cages: spawn on the cage floor, one step toward the dial, facing it
        for (int i = 0; i < 10; i++) {
            int px = CAGE_SRC[i][0] + REC_DX, pz = CAGE_SRC[i][1] + REC_DZ;
            double ox = px + 0.5, oz = pz + 0.5;
            double dx = cx - ox, dz = cz - oz, len = Math.hypot(dx, dz);
            double[] p = L.pods[i];
            p[0] = ox + dx / len * 1.2; p[1] = CAGE_FLOOR + 1; p[2] = oz + dz / len * 1.2;
            p[3] = Math.toDegrees(Math.atan2(-(cx - p[0]), cz - p[2]));
            String glass = POD_DYES[i] + "_stained_glass";
            for (int ddx = -4; ddx <= 4; ddx++) for (int ddz = -4; ddz <= 4; ddz++) {
                int x = px + ddx, z = pz + ddz;
                // the full team-glass band of the cage follows the pod state (frozen / caged / dunked)
                for (int y = CAGE_FLOOR + 1; y <= CAGE_FLOOR + 12; y++)
                    if (glass.equals(d.id(x, y, z))) accent(L.podAccents[i], x, y, z, HubLayout.K_GLASS);
                // the eye-level slit above the first pane row: iron bars when the pod is caged
                String pane = d.id(x, CAGE_FLOOR + 1, z);
                if (pane != null && pane.endsWith("_pane") && d.isAir(x, CAGE_FLOOR + 2, z))
                    L.podBars[i].add(new int[]{x, CAGE_FLOOR + 2, z});
            }
        }
        // wedges: 8 centred on 22.5 + 45k (between the recreation's dividers), 6 centred on 90 + 60k
        for (int n : new int[]{8, 6}) {
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(wedgeBase(n) + i * (360.0 / n));
                double[] w = n == 8 ? L.wedge8[i] : L.wedge6[i];
                w[0] = cx + Math.cos(a) * 5.0; w[1] = DIAL_SURFACE; w[2] = cz + Math.sin(a) * 5.0; w[3] = 0;
                double[] lb = n == 8 ? L.wedgeLabels8[i] : L.wedgeLabels6[i];
                lb[0] = w[0]; lb[1] = DIAL_SURFACE + 5.0; lb[2] = w[2]; lb[3] = 0;
                // divider teeth on the boundary ray (for 8 these sit exactly on the recreation's grey lines)
                double b = Math.toRadians(wedgeBase(n) - 180.0 / n + i * (360.0 / n));
                List<int[]> cells = n == 8 ? L.divider8[i] : L.divider6[i];
                java.util.Set<Long> seen = new java.util.HashSet<>();
                for (double t = 0.0; t <= 8.6; t += 0.2) {
                    int x = (int) Math.floor(cx + Math.cos(b) * t), z = (int) Math.floor(cz + Math.sin(b) * t);
                    if (r(x, z) > 8.6 || !seen.add(((long) x << 32) ^ (z & 0xffffffffL))) continue;
                    for (int h = 1; h <= 3; h++) if (d.isAir(x, DIAL_Y + h, z)) cells.add(new int[]{x, DIAL_Y + h, z});
                }
            }
        }
        // the repaintable wedge band: the top course of the grey rim wall around the dial
        for (int x = CX - 11; x <= CX + 11; x++) for (int z = CZ - 11; z <= CZ + 11; z++) {
            double rr = r(x, z);
            if (rr < 8.4 || rr > 10.0) continue;
            if ("gray_concrete".equals(d.id(x, DIAL_RIM_TOP, z))) L.dialBand.add(new int[]{x, z, HubLayout.K_CONCRETE});
        }
        // the dial floor and its lines stay exactly as built: no seam repaint
        L.dividerSeam8.clear();
        L.dividerSeam6.clear();
        // reveal faces just outside the crown, high enough to clear the cage towers; clock inside the crown
        double[][] faces = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            double[] f = faces[i];
            L.revealSurfaces[i][0] = cx + f[0] * 10.5;
            L.revealSurfaces[i][1] = 86;
            L.revealSurfaces[i][2] = cz + f[1] * 10.5;
            L.revealSurfaces[i][3] = Math.toDegrees(Math.atan2(-f[0], f[1]));
        }
        L.timer[0] = cx; L.timer[1] = 83.6; L.timer[2] = cz;
        L.clock[0] = cx; L.clock[1] = 83.6; L.clock[2] = cz;
        // the closing ceremony podium stands on the dial (its blocks are restored afterwards)
        L.podiumBaseY = DIAL_Y;
        L.podium[0] = cx; L.podium[1] = DIAL_Y + 1; L.podium[2] = cz;
    }

    /** Invisible light blocks so the 1:1 build reads at night without changing a visible block. */
    void recreationLights() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45.0);
            for (int y : new int[]{48, 62}) {
                int x = (int) Math.floor(cx + Math.cos(a) * 22.5), z = (int) Math.floor(cz + Math.sin(a) * 22.5);
                d.setIfAir(x, y, z, "light[level=12]");
            }
            // over the dial, between the crown pillars
            int x = (int) Math.floor(cx + Math.cos(a) * 4.0), z = (int) Math.floor(cz + Math.sin(a) * 4.0);
            d.setIfAir(x, DIAL_Y + 6, z, "light[level=15]");
        }
        for (int i = 0; i < 10; i++) {
            int px = CAGE_SRC[i][0] + REC_DX, pz = CAGE_SRC[i][1] + REC_DZ;
            d.setIfAir(px, CAGE_FLOOR + 4, pz, "light[level=13]");
        }
    }

    static int topAt(double rr) {
        if (rr <= 13.2) return STAGE;            // dial
        if (rr <= 19.4) return POD_FLOOR;        // pod floor block
        if (rr <= 23.4) return STAGE;            // concourse
        return standTop(rr);
    }

    String bowlBlock(int x, int y, int z, double rr, double a, int top, boolean tunnel) {
        int k = (int) Math.floor(rr);
        if (tunnel) return y == top ? (Math.abs(x) <= 2 ? "red_wool" : Math.abs(x) == 3 ? "gold_block" : "polished_deepslate") : "deepslate_tiles";
        if (rr <= 13.2) return "polished_blackstone"; // dial() paints the top
        if (rr <= 19.4) return "polished_deepslate";  // pods() paint their floor
        if (rr <= 23.4) {
            if (y != top) return "deepslate_tiles";
            return d.pick(x, y, z, 931, "polished_deepslate", 4, "deepslate_tiles", 2, "polished_blackstone", 1);
        }
        if (y != top) {
            if (y == top - 1) return k == 24 || k == 28 || k == 32 ? "polished_blackstone" : "deepslate_tiles";
            return "deepslate_tiles";
        }
        if (k == 27 || k == 31 || k == 35) return "polished_blackstone";
        if (k == 24 || k == 28 || k == 32) return "polished_basalt"; // tier nosing
        return d.pick(x, y, z, 932, "polished_deepslate", 4, "deepslate_tiles", 3, "blackstone", 2);
    }

    // ------------------------------------------------------------------------------------------------ dial
    void dial() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // White quartz plates on a grey concrete divider grid, as in the downloaded Decision Dome dial: eight
        // 45 degree wedge plates, a grey rim, one chunky diamond inlay per wedge and a small hub medallion.
        for (int x = CX - 13; x <= CX + 13; x++) for (int z = CZ - 13; z <= CZ + 13; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr > DIAL_R) continue;
            double a = Math.toDegrees(Math.atan2(z + 0.5 - cz, x + 0.5 - cx));
            if (a < 0) a += 360;
            String b;
            if (rr > 11.45) b = "gray_concrete";                     // grey rim band
            else if (rr > 11.15) b = "light_gray_concrete";          // bright rim lip
            else if (rr > 1.55) {
                b = null;
                // the eight radial dividers sit on the wedge boundaries (not the wedge centres)
                for (int k = 0; k < 8; k++) {
                    double boundary = 22.5 + k * 45.0;
                    if (Math.abs(angDiff(a, boundary)) * Math.PI / 180.0 * rr < 0.5) { b = "gray_concrete"; break; }
                }
                // one chunky diamond inlay per wedge, as on the reference floor
                if (b == null) for (int k = 0; k < 8; k++) {
                    double wa = Math.toRadians(90 + k * 45.0);
                    double wx = Math.floor(cx + Math.cos(wa) * 6.5) + 0.5;
                    double wz = Math.floor(cz + Math.sin(wa) * 6.5) + 0.5;
                    if (Math.abs(x + 0.5 - wx) + Math.abs(z + 0.5 - wz) < 1.6) { b = "gray_concrete"; break; }
                }
                if (b == null) b = d.pick(x, STAGE, z, 933, "smooth_quartz", 13, "quartz_block", 2, "calcite", 1, "quartz_bricks", 1);
            } else b = "smooth_quartz";
            d.set(x, STAGE, z, b);
        }
        // the repaintable jewel inlay: a thin outer team ring and an inner light strip, not a solid pie
        for (int x = CX - 12; x <= CX + 12; x++) for (int z = CZ - 12; z <= CZ + 12; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            boolean outer = rr >= 10.45 && rr <= 11.45;
            boolean inner = rr >= 8.02 && rr <= 8.34;
            if (!outer && !inner) continue;
            double a = angDeg(x, z);
            int idx = wedgeIndex(a, 8);
            int kind = inner ? HubLayout.K_LIGHT : HubLayout.K_CONCRETE;
            L.dialBand.add(new int[]{x, z, kind});
            d.set(x, STAGE, z, accentBlock(kind, WEDGE_DYES8[idx], true));
        }
        // flat gold/sea-lantern medallion at the hub (the reveal plinth is the crown above, not a dais)
        d.ring(cx, STAGE, cz, 1.05, 1.75, "gold_block");
        for (int dxx = -1; dxx <= 1; dxx++) for (int dzz = -1; dzz <= 1; dzz++)
            if (Math.abs(dxx) + Math.abs(dzz) <= 1) d.set(CX + dxx, STAGE, CZ + dzz, dxx == 0 && dzz == 0 ? "ochre_froglight" : "gold_block");
        // (the physical divider seam is laid in dividers(), after the colour field)
        // a low brass rim kerb, so chickens cannot roll off the dial
        d.ring(cx, STAGE, cz, 11.55, 12.35, "cut_copper");
        d.ring(cx, STAGE + 1, cz, 11.55, 12.10, "cut_copper");
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(90 + k * 45.0);
            int x = (int) Math.floor(cx + Math.cos(a) * 11.85);
            int z = (int) Math.floor(cz + Math.sin(a) * 11.85);
            d.set(x, STAGE + 1, z, k % 2 == 0 ? "gold_block" : "smooth_quartz");
        }
        // the shadow moat: the plate hangs one block proud of a dark ring, so it reads as a floating disc.
        // The outer lip is dropped one block so a chicken that hops the kerb can always hop back out.
        for (int x = CX - 14; x <= CX + 14; x++) for (int z = CZ - 14; z <= CZ + 14; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr <= 12.30 || rr > 13.30) continue;
            d.air(x, STAGE, z);
            d.set(x, STAGE - 1, z, "deepslate_tiles");
            d.set(x, STAGE - 2, z, "polished_blackstone");
        }
        for (int x = CX - 14; x <= CX + 14; x++) for (int z = CZ - 14; z <= CZ + 14; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr <= 13.30 || rr > 13.45) continue;
            d.air(x, STAGE, z);
            d.air(x, STAGE + 1, z);
            d.set(x, STAGE - 1, z, "gray_concrete");
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45.0);
            int x = (int) Math.floor(cx + Math.cos(a) * 12.85), z = (int) Math.floor(cz + Math.sin(a) * 12.85);
            d.set(x, STAGE - 1, z, "ochre_froglight");
        }
        // eight game totems (wedge 1 on 90). The six-game set is stored for runtime.
        totemSet(8);
        totemSet(6);
        L.dialGeneration = HubGenerator.VERSION;
    }

    /** Centre angle (degrees) of wedge 0 for an n-wedge dial. */
    static double wedgeBase(int n) { return n == 6 ? WEDGE_BASE6 : WEDGE_BASE8; }

    /** Index of the wedge containing angle a (degrees) for n wedges, wedge 0 centred on {@link #wedgeBase}. */
    static int wedgeIndex(double aDeg, int n) {
        double step = 360.0 / n;
        double rel = (aDeg - wedgeBase(n) + step * 0.5);
        rel = ((rel % 360) + 360) % 360;
        return (int) Math.floor(rel / step);
    }

    void totemSet(int n) {
        String[] dyes = n == 8 ? WEDGE_DYES8 : WEDGE_DYES6;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(90 + i * (360.0 / n));
            double rad = 10.55;
            double px = CX + 0.5 + Math.cos(a) * rad, pz = CZ + 0.5 + Math.sin(a) * rad;
            double ux = Math.cos(a), uz = Math.sin(a), tx = -uz, tz = ux;
            List<int[]> acc = n == 8 ? L.wedgeAccents8[i] : L.wedgeAccents6[i];
            boolean build = n == 8;
            String dye = dyes[i];
            // low orrery totem: a 3x3 pedestal, a 2x2 jewelled body and a wool cap; the game name is a hologram
            // floating above it (so it never blocks the sightline across the dial)
            for (int dr = -1; dr <= 1; dr++) for (int dt = -1; dt <= 1; dt++) {
                int x = (int) Math.floor(px + ux * dr + tx * dt), z = (int) Math.floor(pz + uz * dr + tz * dt);
                accent(acc, x, STAGE + 1, z, build ? HubLayout.K_KEEP : HubLayout.K_CONCRETE);
                if (build) d.set(x, STAGE + 1, z, dr == 0 && dt == 0 ? "cut_copper" : "polished_blackstone");
            }
            for (int dr = -1; dr <= 1; dr++) for (int dt = -1; dt <= 1; dt++) {
                if (Math.abs(dr) == 1 && Math.abs(dt) == 1) continue;
                int x = (int) Math.floor(px + ux * dr + tx * dt), z = (int) Math.floor(pz + uz * dr + tz * dt);
                accent(acc, x, STAGE + 2, z, HubLayout.K_CONCRETE);
                if (build) d.set(x, STAGE + 2, z, accentBlock(HubLayout.K_CONCRETE, dye, true));
                accent(acc, x, STAGE + 3, z, HubLayout.K_WOOL);
                if (build) d.set(x, STAGE + 3, z, accentBlock(HubLayout.K_WOOL, dye, true));
            }
            int lx = (int) Math.floor(px), lz = (int) Math.floor(pz);
            accent(acc, lx, STAGE + 2, lz, HubLayout.K_LIGHT);
            if (build) d.set(lx, STAGE + 2, lz, "ochre_froglight");

            double[] w = n == 8 ? L.wedge8[i] : L.wedge6[i];
            w[0] = CX + 0.5 + Math.cos(a) * 8.5; w[1] = STAGE + 1; w[2] = CZ + 0.5 + Math.sin(a) * 8.5; w[3] = 0;
            double[] lb = n == 8 ? L.wedgeLabels8[i] : L.wedgeLabels6[i];
            lb[0] = CX + 0.5 + Math.cos(a) * 11.9; lb[1] = STAGE + 10.5; lb[2] = CZ + 0.5 + Math.sin(a) * 11.9; lb[3] = 0;
        }
    }

    // ------------------------------------------------------------------------------------------------ podium island
    void podiumIsland() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // flat brass inlay around the reveal plinth (the old 7.5-radius dais would hide the new wedge plates)
        d.ring(cx, STAGE, cz, 2.55, 3.15, "cut_copper");
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45.0 + 22.5);
            int x = (int) Math.floor(cx + Math.cos(a) * 2.85), z = (int) Math.floor(cz + Math.sin(a) * 2.85);
            d.set(x, STAGE, z, "gold_block");
        }
        // a low 3x3 reveal plinth at the hub: white quartz, gold corners and a warm core
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
            d.set(CX + dx, STAGE + 1, CZ + dz, dx == 0 && dz == 0 ? "ochre_froglight" : corner ? "gold_block" : "smooth_quartz");
        }
        L.podiumBaseY = STAGE + 1;
        L.podium[0] = cx; L.podium[1] = STAGE + 2; L.podium[2] = cz;
    }

    // ------------------------------------------------------------------------------------------------ pods
    void pods() {
        for (int i = 0; i < 10; i++) pod(i);
    }

    /** Half angular width of pod i (degrees): the two pods either side of the stair are the narrow heroes. */
    static double podHalf(int i) {
        int prev = POD_ANGLE[(i + 9) % 10], next = POD_ANGLE[(i + 1) % 10];
        double a = POD_ANGLE[i];
        double gapPrev = ((a - prev) % 360 + 360) % 360;
        double gapNext = ((next - a) % 360 + 360) % 360;
        double half = Math.min(gapPrev, gapNext) * 0.5 - 1.9;
        return Math.max(6.6, Math.min(15.4, half));
    }

    void pod(int i) {
        double ac = Math.toRadians(POD_ANGLE[i]);
        double half = Math.toRadians(podHalf(i));
        String dye = POD_DYES[i];
        List<int[]> acc = L.podAccents[i];
        List<int[]> gate = L.podGates[i];
        List<int[]> bars = L.podBars[i];
        double cx = CX + 0.5, cz = CZ + 0.5;
        int minX = (int) Math.floor(cx - POD_OUT - 2), maxX = (int) Math.ceil(cx + POD_OUT + 2);
        int minZ = (int) Math.floor(cz - POD_OUT - 2), maxZ = (int) Math.ceil(cz + POD_OUT + 2);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < POD_IN || rr > POD_OUT) continue;
            double a = ang(x, z);
            double diff = a - ac;
            while (diff > Math.PI) diff -= Math.PI * 2;
            while (diff < -Math.PI) diff += Math.PI * 2;
            double ad = Math.abs(diff);
            if (ad > half) continue;
            double tw = ad * rr;               // tangential distance from the pod centre (blocks)
            double side = Math.sin(half) * rr; // usable half width at this radius
            boolean wall = tw > side - 1.15;
            boolean front = rr < POD_IN + 0.85;
            boolean rear = rr > POD_OUT - 0.85;
            // floor: solid block at y65, team carpet at y66
            d.set(x, POD_FLOOR - 1, z, "polished_deepslate");
            d.set(x, POD_FLOOR, z, d.pick(x, 0, z, 941, "polished_deepslate", 3, "deepslate_tiles", 2, "polished_blackstone", 1));
            if (!wall && !front && !rear) {
                // a low rear step, so the pod reads as tiered rows like the reference suites
                if (rr > 16.2 && rr < 18.6) {
                    d.set(x, POD_FLOOR + 1, z, "polished_blackstone");
                    accentSet(acc, x, POD_FLOOR + 2, z, HubLayout.K_CARPET, dye, true);
                } else {
                    accentSet(acc, x, POD_FLOOR + 1, z, HubLayout.K_CARPET, dye, true);
                }
            }
            // walls
            if (front) {
                d.set(x, POD_FLOOR + 1, z, "chiseled_polished_blackstone");
                boolean window = tw < 1.55;
                for (int y = POD_FLOOR + 2; y <= POD_FLOOR + 3; y++) {
                    if (window) {
                        d.air(x, y, z);
                        bars.add(new int[]{x, y, z});
                    } else {
                        accentSet(acc, x, y, z, HubLayout.K_GLASS, dye, true);
                    }
                }
                accentSet(acc, x, POD_FLOOR + 4, z, HubLayout.K_GLASS, dye, true); } else if (rear) {
                boolean gateOpen = tw < 1.55;
                d.set(x, POD_FLOOR + 1, z, "chiseled_polished_blackstone");
                for (int y = POD_FLOOR + 2; y <= POD_FLOOR + 4; y++) {
                    if (gateOpen && y <= POD_FLOOR + 3) {
                        gate.add(new int[]{x, y, z});
                        d.air(x, y, z);
                    } else {
                        accentSet(acc, x, y, z, HubLayout.K_GLASS, dye, true);
                    }
                }
                if (gateOpen) {
                    // team crest above the rear gate
                    accentSet(acc, x, POD_FLOOR + 5, z, HubLayout.K_WOOL, dye, true);
                }
            } else if (wall) {
                for (int y = POD_FLOOR + 1; y <= POD_FLOOR + 4; y++) {
                    if (y == POD_FLOOR + 4 || y == POD_FLOOR + 1) d.set(x, y, z, "cut_copper");
                    else accentSet(acc, x, y, z, HubLayout.K_GLASS, dye, true);
                }
            }
            // glass/copper roof, ribs every other block
            if (!front && !rear) {
                boolean rib = Math.floorMod((int) Math.floor(tw), 2) == 0;
                if (rib) d.set(x, POD_FLOOR + 5, z, "cut_copper");
                else accentSet(acc, x, POD_FLOOR + 5, z, HubLayout.K_GLASS, dye, true);
            }
        }
        // four seats on the pod floor, facing the dial (narrow hero pods get a tighter pair)
        for (int row = 0; row < 2; row++) {
            double sr = row == 0 ? 15.2 : 17.2;
            double maxOff = Math.sin(half) * sr - 0.95;
            double off = Math.max(0.55, Math.min(1.15, maxOff));
            for (int s = -1; s <= 1; s += 2) {
                int x = (int) Math.floor(cx + Math.cos(ac) * sr - Math.sin(ac) * off * s);
                int z = (int) Math.floor(cz + Math.sin(ac) * sr + Math.cos(ac) * off * s);
                double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (rr < POD_IN + 0.8 || rr > POD_OUT - 0.8) continue;
                int seatY = row == 0 ? POD_FLOOR + 1 : POD_FLOOR + 2; String f = Draw.facingToward(x, z, CX, CZ); d.set(x, seatY, z, "polished_blackstone_slab[type=bottom]"); accent(acc, x, seatY, z, HubLayout.K_KEEP);
            }
        }
        // team lamp under the roof
        int lx = (int) Math.floor(cx + Math.cos(ac) * 18.2), lz = (int) Math.floor(cz + Math.sin(ac) * 18.2);
        accentSet(acc, lx, POD_FLOOR + 4, lz, HubLayout.K_LIGHT, dye, true);
        // concourse-side colour crest
        int bx = (int) Math.floor(cx + Math.cos(ac) * (POD_OUT + 0.35)), bz = (int) Math.floor(cz + Math.sin(ac) * (POD_OUT + 0.35));
        for (int dt = -1; dt <= 1; dt++) for (int y = POD_FLOOR + 2; y <= POD_FLOOR + 4; y++) {
            int x = (int) Math.floor(bx + 0.5 - Math.sin(ac) * dt), z = (int) Math.floor(bz + 0.5 + Math.cos(ac) * dt);
            accentSet(acc, x, y, z, HubLayout.K_WOOL, dye, true);
        }
        // gate threshold stair from the concourse
        int gx = (int) Math.floor(cx + Math.cos(ac) * (POD_OUT + 0.6)), gz = (int) Math.floor(cz + Math.sin(ac) * (POD_OUT + 0.6));
        d.set(gx, STAGE + 1, gz, "polished_blackstone_slab[type=bottom]");
        for (int y = STAGE + 2; y <= STAGE + 3; y++) d.air(gx, y, gz);
        double[] p = L.pods[i];
        // Keep the nominal spawn radius, but nudge out of the 0.85-thick front sill when the cell rounding puts
        // the spawn inside it (the cardinal pods land on r=14.0, the diagonals on r=14.5).
        double spawnR = POD_R;
        for (int k = 0; k < 4; k++) {
            int ix = (int) Math.floor(cx + Math.cos(ac) * spawnR);
            int iz = (int) Math.floor(cz + Math.sin(ac) * spawnR);
            String at = d.id(ix, POD_FLOOR + 1, iz);
            if (at == null || at.contains("carpet") || at.contains("slab")) break;
            spawnR += 0.35;
        }
        p[0] = cx + Math.cos(ac) * spawnR; p[1] = POD_FLOOR + 1; p[2] = cz + Math.sin(ac) * spawnR;
        p[3] = Math.toDegrees(Math.atan2(-(cx - p[0]), cz - p[2]));
        podTower(i);
    }

    /**
     * The tall cage tower above pod i's room, matching the downloaded Decision Dome pod silhouette: a white glass
     * base band, a hollow team cage over the pod roof, a copper/gold belt, a glass collar, a flared 7x7 crown band
     * and a 3x3 gold cap. It starts one block above the room roof (y71) and tops out at y89, so the room, the rear
     * gate and the seats below are untouched.
     */
    void podTower(int i) {
        double ac = Math.toRadians(POD_ANGLE[i]);
        double half = Math.toRadians(podHalf(i));
        double crownHalf = half + Math.toRadians(1.6);
        String dye = POD_DYES[i];
        double cx = CX + 0.5, cz = CZ + 0.5;
        int minX = (int) Math.floor(cx - POD_OUT - 2), maxX = (int) Math.ceil(cx + POD_OUT + 2);
        int minZ = (int) Math.floor(cz - POD_OUT - 2), maxZ = (int) Math.ceil(cz + POD_OUT + 2);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < POD_IN - 1.1 || rr > POD_OUT + 1.1) continue;
            double a = ang(x, z);
            double diff = a - ac;
            while (diff > Math.PI) diff -= Math.PI * 2;
            while (diff < -Math.PI) diff += Math.PI * 2;
            double ad = Math.abs(diff);
            if (ad > crownHalf) continue;
            double tw = ad * rr;
            double side = Math.sin(half) * rr;
            boolean body = rr >= POD_IN && rr <= POD_OUT && ad <= half;
            boolean wall = body && tw > side - 1.15;
            boolean front = body && rr < POD_IN + 0.85;
            boolean rear = body && rr > POD_OUT - 0.85;
            if (body) {
                // close the one-block slot between the room's wall top (y69/70) and the tower base, without
                // overwriting the glass roof or the team crest above the rear gate
                d.setIfAir(x, POD_FLOOR + 5, z, wall ? "cut_copper" : "white_concrete");
                // y71-72: white glass base band, the bright ring just above the room roof
                for (int y = TOWER_Y0; y <= TOWER_Y0 + 1; y++)
                    if (wall) d.set(x, y, z, y == TOWER_Y0 ? dye + "_terracotta" : dye + "_concrete");
                    else if (front || rear) d.set(x, y, z, "white_stained_glass");
                // y73-79: hollow cage; team verticals on the sides and the concourse face, open toward the dial
                for (int y = TOWER_Y0 + 2; y <= TOWER_BELT - 1; y++) {
                    boolean lintel = y == TOWER_Y0 + 2 || y == TOWER_BELT - 1;
                    if (wall) d.set(x, y, z, y % 3 == 0 ? dye + "_terracotta" : dye + "_concrete");
                    else if (rear) d.set(x, y, z, lintel ? "cut_copper"
                            : tw < 0.7 ? "white_concrete"
                            : tw < 1.7 ? dye + "_terracotta" : dye + "_concrete");
                    else if (front) {
                        if (lintel) d.set(x, y, z, "cut_copper");
                        else if (tw < 0.6) d.set(x, y, z, dye + "_concrete");
                    }
                }
                // y80: copper/gold belt between the cage and the glass collar (perimeter only)
                if (front || rear || wall) d.set(x, TOWER_BELT, z, wall ? "gold_block" : "cut_copper");
                // y81-82: glass collar (recorded as a pod accent, so it follows the team / pod state)
                for (int y = TOWER_BELT + 1; y <= TOWER_BELT + 2; y++) {
                    if (wall) d.set(x, y, z, dye + "_concrete");
                    else if (front || rear) accentSet(L.podAccents[i], x, y, z, HubLayout.K_GLASS, dye, true);
                }
                // close the tower top: a team deck under the 3x3 cap, so the hollow shaft is not open to the dome
                d.set(x, TOWER_CROWN + 2, z, dye + "_concrete");
            }
            // y83-85: flared crown band (the 7x7 ring of the reference pod), a one-block wider shell
            if (ad <= crownHalf) {
                boolean rim = rr <= POD_IN - 0.05 || rr >= POD_OUT - 0.15 || tw > Math.sin(crownHalf) * rr - 1.05;
                if (rim) for (int y = TOWER_CROWN; y <= TOWER_CROWN + 2; y++)
                    d.set(x, y, z, y == TOWER_CROWN ? "gold_block"
                            : y == TOWER_CROWN + 1 ? dye + "_terracotta"
                            : "white_concrete");
            }
        }
        // the 3x3 cap: team concrete, white quartz, a gold/glow top and a copper spire
        double rMid = (POD_IN + POD_OUT) * 0.5;
        int px = (int) Math.floor(cx + Math.cos(ac) * rMid);
        int pz = (int) Math.floor(cz + Math.sin(ac) * rMid);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
            d.set(px + dx, TOWER_CAP, pz + dz, corner ? "gold_block" : dye + "_concrete");
            d.set(px + dx, TOWER_CAP + 1, pz + dz, corner ? "gold_block" : "white_concrete");
            d.set(px + dx, TOWER_CAP + 2, pz + dz, corner ? "gold_block"
                    : dx == 0 && dz == 0 ? "ochre_froglight" : "yellow_concrete");
        }
        d.set(px, TOWER_CAP + 3, pz, "end_rod[facing=up]");
        // a warm lamp in the hollow shaft, shining down through the room's glass roof
        int mx = (int) Math.floor(cx + Math.cos(ac) * POD_R), mz = (int) Math.floor(cz + Math.sin(ac) * POD_R);
        d.set(mx, TOWER_Y0, mz, "ochre_froglight");
    }

    // ------------------------------------------------------------------------------------------------ concourse
    void concourse() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int x = CX - 25; x <= CX + 25; x++) for (int z = CZ - 25; z <= CZ + 25; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < CONC_IN || rr > CONC_OUT) continue;
            double a = angDeg(x, z);
            if (Math.abs(x) <= 4 && z > CZ) continue; // main stair corridor
            // white calcite paving with grey seams and thin copper rays, matching the dial's plate language
            String b = d.pick(x, STAGE, z, 931, "smooth_quartz", 9, "calcite", 4, "quartz_block", 3, "gray_concrete", 1);
            if (Math.abs(rr - 21.4) < 0.32) b = "gray_concrete";
            if (Math.abs(rr - 23.1) < 0.25) b = "smooth_quartz";
            if (Math.abs(a - Math.round(a / 18.0) * 18.0) < 0.30) b = "cut_copper";
            d.set(x, STAGE, z, b);
            for (int y = STAGE + 1; y <= STAGE + 4; y++) d.air(x, y, z);
        }
        // low rail at the concourse / stand edge, with stair and aisle openings
        for (int x = CX - 25; x <= CX + 25; x++) for (int z = CZ - 25; z <= CZ + 25; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < CONC_OUT - 0.7 || rr > CONC_OUT + 0.2) continue;
            double a = angDeg(x, z);
            if (aisleGap(a, 3.6)) continue;
            d.set(x, STAGE, z, "smooth_quartz");
            d.set(x, STAGE + 1, z, "cut_copper");
            d.set(x, STAGE + 2, z, "gold_block");
        }
        // front parapet at the pod backs, with ten gate gaps
        for (int x = CX - 22; x <= CX + 22; x++) for (int z = CZ - 22; z <= CZ + 22; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < CONC_IN - 0.4 || rr > CONC_IN + 0.1) continue;
            double a = angDeg(x, z);
            boolean gap = false;
            for (int i = 0; i < 10; i++) if (Math.abs(angDiff(a, POD_ANGLE[i])) < podHalf(i) * 0.55) gap = true;
            if (gap) continue;
            d.set(x, STAGE + 1, z, "cut_copper");
            d.set(x, STAGE + 2, z, "smooth_quartz");
        }
        // ten white quartz floor medallions, one in front of each gate, cored with the team colour
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(POD_ANGLE[i]);
            int gx = (int) Math.floor(cx + Math.cos(a) * 21.7), gz = (int) Math.floor(cz + Math.sin(a) * 21.7);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                String b = corner ? "gold_block" : dx == 0 && dz == 0 ? POD_DYES[i] + "_concrete" : "smooth_quartz";
                d.set(gx + dx, STAGE, gz + dz, b);
            }
        }
    }

    static double angDiff(double a, double b) {
        double d = (a - b) % 360;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    static boolean aisleGap(double a, double halfWidthDeg) {
        for (int x : AISLE_ANGLES) if (Math.abs(angDiff(a, x)) < halfWidthDeg) return true;
        return Math.abs(angDiff(a, 90)) < 5.5;
    }

    // ------------------------------------------------------------------------------------------------ stands
    void stands() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int x = CX - 37; x <= CX + 37; x++) for (int z = CZ - 37; z <= CZ + 37; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < STAND_IN || rr > STAND_OUT) continue;
            if (Math.abs(x) <= 4 && z > CZ) continue; // stair corridor
            double a = angDeg(x, z);
            if (aisleGap(a, 2.4)) continue;
            int top = standTop(rr);
            int k = (int) Math.floor(rr);
            for (int y = top + 1; y <= top + 3; y++) d.air(x, y, z);
            // calcite nosing on every tier front and copper accents on the landings
            if (k == 24 || k == 28 || k == 32) {
                boolean lamp = Math.abs(angDiff(a, Math.round(a / 72.0) * 72.0)) < 2.2;
                d.set(x, top, z, lamp ? "ochre_froglight" : Math.floorMod(x + z, 7) == 0 ? "cut_copper" : "polished_basalt");
            }
            else if (k == 27 || k == 31 || k == 35) d.set(x, top, z, Math.floorMod(x + z, 12) == 0 ? "cut_copper" : "calcite");
            g.paved(x, z, top);
        }
        // 120 seats: 12 rows x 10 angles offset 30 deg, so no seat sits in an aisle or the grand stair
        for (int row = 0; row < 12; row++) {
            double rr = 24.5 + row;
            int top = standTop(rr);
            for (int s = 0; s < 10; s++) {
                double a = Math.toRadians(30.0 + s * 36.0);
                double x = cx + Math.cos(a) * rr, z = cz + Math.sin(a) * rr;
                int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
                String f = Draw.facingToward(bx, bz, CX, CZ);
                d.set(bx, top + 1, bz, "polished_blackstone_slab[type=bottom]");
                float yaw = (float) Math.toDegrees(Math.atan2(-(cx - x), cz - z));
                L.seats.add(new double[]{x, top + 1.5, z, yaw});
            }
        }
        // top walkway and a 2-high parapet before the inner wall
        d.ring(cx, 78, cz, STAND_OUT, STAND_OUT + 1.2, "polished_blackstone");
        d.ring(cx, 79, cz, STAND_OUT, STAND_OUT + 0.35, "calcite");
    }

    // ------------------------------------------------------------------------------------------------ stair
    void grandStair() {
        int footZ = CZ + 26;     // r 26
        int headZ = CZ + 39;     // r 39
        // clear the 9-wide ceremonial corridor through the stand bowl and the tunnel mouth
        for (int x = -4; x <= 4; x++) for (int z = CZ + 22; z <= CZ + 41; z++)
            for (int y = STAGE + 1; y <= TERRACE + 4; y++) d.air(x, y, z);
        g.staircase("dome grand stair", 0, STAGE, footZ, 0, TERRACE, headZ, "south", 7,
                x -> Math.abs(x) <= 1 ? "red_nether_brick" : "polished_deepslate",
                x -> Math.abs(x) <= 1 ? "red_nether_bricks" : "chiseled_polished_blackstone");
        // 7-wide flight, 1-block copper/black balustrades at x = +-4
        for (int s : new int[]{-1, 1}) {
            int x = s * 4;
            for (int z = CZ + 23; z <= CZ + 41; z++) {
                int y = STAGE + Math.max(0, Math.min(12, (z - (CZ + 26))));
                for (int yy = Math.max(STAGE, y); yy <= TERRACE + 1; yy++) {
                    String b = yy == TERRACE + 1 ? "gold_block" : (yy % 4 == 0 ? "oxidized_copper" : "polished_blackstone");
                    d.set(x, yy, z, b);
                }
                for (int yy = TERRACE + 2; yy <= TERRACE + 4; yy++) d.air(x, yy, z);
            }
        }
        // landing at the tunnel mouth
        for (int x = -5; x <= 5; x++) for (int z = CZ + 39; z <= CZ + 41; z++) {
            d.set(x, TERRACE, z, Math.abs(x) == 5 ? "calcite" : "polished_deepslate");
            for (int y = TERRACE + 1; y <= TERRACE + 4; y++) d.air(x, y, z);
        }
    }

    // ------------------------------------------------------------------------------------------------ aisles
    void aisles() {
        for (int ang : AISLE_ANGLES) {
            double a = Math.toRadians(ang);
            List<int[]> cells = new ArrayList<>();
            // a flat foot on the concourse, so the first stair has proper headroom
            HubGenerator.addCell(cells, (int) Math.floor(CX + 0.5 + Math.cos(a) * 21.0), (int) Math.floor(CZ + 0.5 + Math.sin(a) * 21.0));
            for (double t = 22.0; t <= 35.4; t += 0.35) {
                int x = (int) Math.floor(CX + 0.5 + Math.cos(a) * t), z = (int) Math.floor(CZ + 0.5 + Math.sin(a) * t);
                if (!cells.isEmpty()) {
                    int[] p = cells.get(cells.size() - 1);
                    if (p[0] == x && p[1] == z) continue;
                    if (p[0] != x && p[1] != z) {
                        double ex = Math.abs(-Math.sin(a) * (x + 0.5 - CX) + Math.cos(a) * (p[1] + 0.5 - CZ));
                        double ez = Math.abs(-Math.sin(a) * (p[0] + 0.5 - CX) + Math.cos(a) * (z + 0.5 - CZ));
                        cells.add(ex <= ez ? new int[]{x, p[1]} : new int[]{p[0], z});
                    }
                }
                cells.add(new int[]{x, z});
            }
            // centre keys: the 3-wide flanking pavement must never overwrite the walk line
            java.util.Set<Long> centreKeys = new java.util.HashSet<>();
            for (int[] c : cells) centreKeys.add(((long) c[0] << 32) ^ (c[1] & 0xffffffffL));
            int[] xs = new int[cells.size()], zs = new int[cells.size()];
            int prev = STAGE;
            for (int i = 0; i < cells.size(); i++) {
                int x = cells.get(i)[0], z = cells.get(i)[1];
                xs[i] = x; zs[i] = z;
                double rr = Math.hypot(x - CX, z - CZ);
                int top = STAGE + (int) Math.round((rr - 22.0));
                top = Math.max(prev, Math.min(top, 78));
                top = Math.min(top, prev + 1);
                // centre line first: a stair for every rise so the whole aisle is walked, not jumped
                if (top > prev) d.set(x, top, z, Draw.stairs("polished_deepslate", Draw.facingToward(xs[i - 1], zs[i - 1], x, z), false));
                else d.set(x, top, z, "polished_blackstone");
                d.set(x, STAGE, z, "polished_blackstone");
                for (int y = top + 1; y <= top + 3; y++) d.air(x, y, z);
                // flanking pavement one block below the walk line, never on a centre cell
                for (int w = -1; w <= 1; w++) {
                    if (w == 0) continue;
                    int ax = (int) Math.floor(CX + 0.5 + Math.cos(a) * rr - Math.sin(a) * w);
                    int az = (int) Math.floor(CZ + 0.5 + Math.sin(a) * rr + Math.cos(a) * w);
                    if (centreKeys.contains(((long) ax << 32) ^ (az & 0xffffffffL))) continue;
                    d.set(ax, Math.max(STAGE, top - 1), az, "deepslate_tiles");
                    for (int y = top + 1; y <= top + 3; y++) d.air(ax, y, az);
                }
                prev = top;
                g.paved(x, z, top);
            }
            g.walks.add(new HubGenerator.Walk("dome aisle " + ang + "deg", xs, zs, STAGE, prev));
        }
    }

    // ------------------------------------------------------------------------------------------------ inner wall
    void innerWallShell() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        for (int x = CX - 38; x <= CX + 38; x++) for (int z = CZ - 38; z <= CZ + 38; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < WALL_R - 1.0 || rr > WALL_R + 0.6) continue;
            double a = angDeg(x, z);
            boolean tunnel = Math.abs(x) <= 6 && z > CZ;
            for (int y = 79; y <= RIM; y++) {
                if (tunnel && y <= 87) { d.air(x, y, z); continue; }
                String b;
                if (y == RIM) b = "calcite";
                else if (y >= 97) {
                    if (Math.floorMod((int) Math.round(a), 15) == 0) b = "oxidized_copper";
                    else if (d.rnd(x, y, z, 951) < 0.02) b = "sea_lantern";
                    else if (d.rnd(x, y, z, 952) < 0.035) b = "light_blue_stained_glass";
                    else b = d.pick(x, y, z, 953, "black_concrete", 4, "blue_concrete", 1, "polished_blackstone", 2);
                } else if (y == 96) b = "calcite";
                else if (y >= 89) {
                    double frac = a / 12.0 - Math.floor(a / 12.0);
                    int seg = Math.floorMod((int) Math.round(a / 3.0), 3);
                    b = frac < 0.12 ? "polished_blackstone" : seg == 0 ? "calcite" : seg == 1 ? "polished_deepslate" : "smooth_quartz";
                } else if (y == 88) b = "cut_copper";
                else if (y >= 82) {
                    int seg = Math.floorMod((int) Math.floor((a + 90) / 36.0), 10);
                    double f = (a + 90) / 36.0 - Math.floor((a + 90) / 36.0);
                    if (f < 0.14 || f > 0.92) b = "polished_blackstone";
                    else if (y == 87) b = "gold_block";
                    else if (y == 82) b = "cut_copper";
                    else b = POD_DYES[seg] + (y == 84 ? "_wool" : (y == 86 ? "_concrete" : "_terracotta"));
                } else if (y == 81) b = Math.floorMod((int) Math.round(a), 15) == 0 ? "gold_block" : "calcite";
                else if (y == 80) b = "smooth_quartz";
                else if (y == 79) b = Math.floorMod((int) Math.round(a), 15) == 0 ? "gold_block" : "cut_copper";
                else b = "deepslate_tiles";
                d.set(x, y, z, b);
            }
        }
        // dark soffit ring above the stands, tying the inner wall to the drum
        for (int x = CX - 46; x <= CX + 46; x++) for (int z = CZ - 46; z <= CZ + 46; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr < WALL_R + 0.4 || rr > R_IN) continue;
            d.set(x, RIM + 1, z, d.pick(x, 0, z, 955, "black_concrete", 5, "polished_deepslate", 2, "deepslate_tiles", 1));
        }
        // service door at the back (lore)
        int zx = (int) Math.floor(cx + Math.cos(Math.toRadians(270)) * (WALL_R - 0.2));
        int zz = (int) Math.floor(cz + Math.sin(Math.toRadians(270)) * (WALL_R - 0.2));
        for (int dt = -2; dt <= 2; dt++) for (int y = 79; y <= 84; y++) {
            int x = zx + dt;
            d.set(x, y, zz, y <= 81 ? "cut_copper" : "black_concrete");
        }
        d.set(zx, 82, zz - 1, "gold_block");
        d.set(zx, 83, zz - 1, "ochre_froglight");
    }

    // ------------------------------------------------------------------------------------------------ dividers
    void dividers() {
        for (int n : new int[]{8, 6}) {
            String[] dyes = n == 8 ? WEDGE_DYES8 : WEDGE_DYES6;
            for (int k = 0; k < n; k++) {
                double a = Math.toRadians((n == 8 ? 22.5 : 30.0) + k * (360.0 / n));
                List<int[]> cells = n == 8 ? L.divider8[k] : L.divider6[k];
                List<int[]> seam = n == 8 ? L.dividerSeam8 : L.dividerSeam6;
                double ux = Math.cos(a), uz = Math.sin(a);
                double boundaryDeg = (n == 8 ? 22.5 : 30.0) + k * (360.0 / n);
                for (double t = 7.6; t <= 11.7; t += 0.22) {
                    int x = (int) Math.floor(CX + 0.5 + ux * t), z = (int) Math.floor(CZ + 0.5 + uz * t);
                    // seam inlay (built for the 8-game floor; the 6-game seam is painted at runtime)
                    seam.add(new int[]{x, z});
                    if (n == 8) d.set(x, STAGE, z, "deepslate_tiles");
                    int step = (int) Math.round(t * 2);
                    for (int h = 1; h <= 3; h++) {
                        if (n == 6 && h == 3 && Math.floorMod(step, 3) == 1) continue;
                        if (n == 8 && h == 3 && Math.floorMod(step, 3) == 1) continue;
                        cells.add(new int[]{x, STAGE + h, z});
                        // leave the teeth down at build: the venue opens on the flat seam grid and the runtime
                        // raises them with setDividers(progress) during a session
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ crown truss
    /**
     * Gold/white/yellow crown above the dial, replacing the brass orrery as the reveal centrepiece: eight gold
     * pillars on black/gold plinths, a white cross/truss, a yellow top ring and eight red jewel caps. Roughly a
     * 17x17 plan, 12 blocks tall, centred on CX/CZ and fully inside the glass dome.
     */
    void crown() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // yellow top ring: 17x17 plan, two blocks tall, gold along the eight pillar lines
        for (int x = CX - 9; x <= CX + 9; x++) for (int z = CZ - 9; z <= CZ + 9; z++) {
            double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
            if (rr <= 6.7 || rr > 8.5) continue;
            double a = angDeg(x, z);
            boolean pillar = Math.abs(angDiff(a, Math.round(a / 45.0) * 45.0)) < 3.6;
            for (int y = CROWN_RING; y <= CROWN_RING + 1; y++)
                d.set(x, y, z, pillar ? "gold_block"
                        : d.pick(x, y, z, 971, "yellow_concrete", 8, "gold_block", 2, "yellow_terracotta", 1));
            if (rr <= 8.0) d.set(x, CROWN_RING + 2, z, pillar ? "gold_block" : "yellow_concrete");
        }
        // eight gold pillars on 3x3 black/gold plinths, capped with red jewels
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45.0);
            int px = (int) Math.floor(cx + Math.cos(a) * 7.4), pz = (int) Math.floor(cz + Math.sin(a) * 7.4);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                d.set(px + dx, CROWN_Y, pz + dz, corner ? "gold_block" : "polished_blackstone");
                d.set(px + dx, CROWN_Y + 1, pz + dz, corner ? "gold_block" : "smooth_quartz");
            }
            for (int y = CROWN_Y + 2; y <= CROWN_RING - 1; y++)
                d.set(px, y, pz, (y - CROWN_Y) % 4 == 0 ? "cut_copper" : "gold_block");
            d.set(px, CROWN_RING, pz, "gold_block");
            d.set(px, CROWN_RING + 1, pz, "gold_block");
            d.set(px, CROWN_RING + 3, pz, "red_concrete");
            d.set(px, CROWN_RING + 4, pz, "red_stained_glass");
        }
        // white cross/truss below the ring: four axis beams and four diagonal braces on a gold hub
        for (int x = CX - 8; x <= CX + 8; x++) for (int z = CZ - 8; z <= CZ + 8; z++) {
            double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
            double rr = Math.hypot(dx, dz);
            if (rr > 7.9) continue;
            boolean axis = Math.abs(dx) < 0.55 || Math.abs(dz) < 0.55;
            boolean diag = Math.abs(dx - dz) < 0.78 || Math.abs(dx + dz) < 0.78;
            if (!axis && !diag) continue;
            String b = rr > 6.9 ? "gold_block" : Math.floorMod(x + z, 6) == 0 ? "calcite" : "smooth_quartz";
            d.set(x, CROWN_RING - 5, z, b);
            if (axis) d.set(x, CROWN_RING - 4, z, b);
        }
        // gold hub with a warm core, and four lanterns under the truss arms
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            d.set(CX + dx, CROWN_RING - 6, CZ + dz, dx == 0 && dz == 0 ? "ochre_froglight" : "gold_block");
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(90 + k * 90.0);
            int x = (int) Math.floor(cx + Math.cos(a) * 3.3), z = (int) Math.floor(cz + Math.sin(a) * 3.3);
            d.set(x, CROWN_RING - 6, z, "sea_lantern");
        }
        // eight warm rods hanging from the ring, lighting the dial below
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45.0);
            int x = (int) Math.floor(cx + Math.cos(a) * 6.9), z = (int) Math.floor(cz + Math.sin(a) * 6.9);
            d.set(x, CROWN_RING - 1, z, "end_rod[facing=down]");
        }
        // four reveal face anchors, just outside the yellow ring (the title floats on the crown faces)
        double[][] faces = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            double[] f = faces[i];
            L.revealSurfaces[i][0] = cx + f[0] * 9.3;
            L.revealSurfaces[i][1] = CROWN_RING - 5;
            L.revealSurfaces[i][2] = cz + f[1] * 9.3;
            L.revealSurfaces[i][3] = Math.toDegrees(Math.atan2(-f[0], f[1]));
        }
        // two white/copper nebula boards on the east and west flanks, framing the crown from the stands
        hangingScreen(0, 29.0, 86, 11, 5);
        hangingScreen(180, 29.0, 86, 11, 5);
    }

    void armillaryRing(double cx, double cy, double cz, double rad, double tiltA, double tiltB) {
        for (int i = 0; i < 360; i++) {
            double a = Math.toRadians(i);
            double x = Math.cos(a) * rad, y = Math.sin(a) * rad;
            // rotate around X by tiltA, then around Z by tiltB
            double y1 = y * Math.cos(tiltA), z1 = y * Math.sin(tiltA);
            double x2 = x * Math.cos(tiltB) - y1 * Math.sin(tiltB);
            double y2 = x * Math.sin(tiltB) + y1 * Math.cos(tiltB);
            int bx = (int) Math.floor(cx + x2), by = (int) Math.floor(cy + y2), bz = (int) Math.floor(cz + z1);
            if (i % 45 == 0) d.set(bx, by, bz, "gold_block"); else d.set(bx, by, bz, (i % 3 == 0) ? "cut_copper" : "copper_block");
        }
    }

    void hangingScreen(double angleDeg, double rad, int y, int w, int h) {
        double a = Math.toRadians(angleDeg);
        double cx = CX + 0.5 + Math.cos(a) * rad, cz = CZ + 0.5 + Math.sin(a) * rad;
        double tx = -Math.sin(a), tz = Math.cos(a);
        for (int c = -w / 2; c <= w / 2; c++) for (int r = 0; r < h; r++) {
            int x = (int) Math.floor(cx + tx * c), z = (int) Math.floor(cz + tz * c);
            boolean frame = Math.abs(c) == w / 2 || r == 0 || r == h - 1;
            // white quartz frame with a gold top rail, a dark nebula field and a team-colour strip row
            String b = frame ? (r == h - 1 ? "smooth_quartz" : r == 0 ? "gold_block" : "smooth_quartz")
                    : r == 1 ? Draw.TEAM_DYES[Math.floorMod(c + w, 10)] + "_concrete"
                    : ((c + r) % 5 == 0 ? "sea_lantern" : "tinted_glass");
            d.set(x, y + r, z, b);
        }
        for (int c = -2; c <= 2; c += 2) {
            int x = (int) Math.floor(cx + tx * c), z = (int) Math.floor(cz + tz * c);
            for (int yy = y + h; yy <= 102; yy++) d.set(x, yy, z, "chain[axis=y]");
        }
    }

    void interiorClockRing() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // The countdown hangs in the open middle of the crown, framed by the white truss below and the yellow
        // ring above; both anchors stay valid and are read from every stand and from the entrance.
        L.timer[0] = cx; L.timer[1] = CROWN_RING - 2.2; L.timer[2] = cz;
        L.clock[0] = cx; L.clock[1] = CROWN_RING - 2.2; L.clock[2] = cz;
    }

    /** Warm/team lighting and the white/copper fill ring that bridges the cage towers and the stands. */
    void interiorFill() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        // eight white quartz pilasters on the concourse at the pod boundaries (the stair at 90 and the
        // opposite aisle at 180 stay clear), tying the cage ring to the top of the stands
        int[] boundary = {36, 63, 117, 144, 216, 252, 288, 342};
        for (int k = 0; k < boundary.length; k++) {
            double a = Math.toRadians(boundary[k]);
            double ux = Math.cos(a), uz = Math.sin(a);
            int px = (int) Math.floor(cx + ux * 20.1), pz = (int) Math.floor(cz + uz * 20.1);
            String dye = POD_DYES[k % 10];
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                d.set(px + dx, STAGE + 1, pz + dz,
                        Math.abs(dx) == 1 && Math.abs(dz) == 1 ? "cut_copper" : "smooth_quartz");
            for (int y = STAGE + 2; y <= STAGE + 12; y++)
                d.set(px, y, pz, y == STAGE + 6 ? "calcite" : y == STAGE + 9 ? dye + "_concrete" : "smooth_quartz");
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                d.set(px + dx, STAGE + 13, pz + dz, corner ? "gold_block" : "cut_copper");
                d.set(px + dx, STAGE + 14, pz + dz,
                        corner ? "gold_block" : dx == 0 && dz == 0 ? "sea_lantern" : "smooth_quartz");
            }
            d.set(px, STAGE + 15, pz, "gold_block");
            // the same rib continues on the drum wall above the top walk
            int wx = (int) Math.floor(cx + ux * 36.2), wz = (int) Math.floor(cz + uz * 36.2);
            for (int y = 79; y <= 87; y++)
                d.set(wx, y, wz, y == 87 ? "gold_block" : y == 82 ? "cut_copper" : y == 84 ? dye + "_concrete" : "smooth_quartz");
        }
        // warm team uplights at the foot of every cage tower, on the concourse parapet
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(POD_ANGLE[i]);
            double ux = Math.cos(a), uz = Math.sin(a), tx = -uz, tz = ux;
            for (int s = -1; s <= 1; s += 2) {
                int x = (int) Math.floor(cx + ux * 20.0 + tx * s * 1.4);
                int z = (int) Math.floor(cz + uz * 20.0 + tz * s * 1.4);
                d.set(x, STAGE + 1, z, "ochre_froglight");
            }
        }
        // keep the observatory's faint star clusters high on the upper wall
        for (int c = 0; c < 4; c++) {
            double base = Math.toRadians(45 + c * 90);
            int px = (int) Math.floor(cx + Math.cos(base) * (WALL_R - 0.5));
            int pz = (int) Math.floor(cz + Math.sin(base) * (WALL_R - 0.5));
            for (int k = 0; k < 6; k++) {
                int x = px + (k * 2 % 5) - 2;
                int y = 93 + (k % 3) * 2;
                d.set(x, y, pz, k % 2 == 0 ? "sea_lantern" : "light_blue_stained_glass");
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ tunnel
    void tunnel() {
        for (int z = FRONT_Z; z >= CZ + 36; z--) for (int x = -6; x <= 6; x++) {
            if (r(x, z) < 36.3) continue;
            int ax = Math.abs(x);
            int ceil = 86 - (ax >= 5 ? 1 : 0) - (ax >= 6 ? 1 : 0);
            for (int y = TERRACE + 1; y <= ceil; y++) d.air(x, y, z);
            if (ax <= 2) d.set(x, TERRACE, z, "red_wool");
            else if (ax == 3) d.set(x, TERRACE, z, "gold_block");
            else d.set(x, TERRACE, z, "polished_deepslate");
            d.set(x, ceil + 1, z, ax <= 4 && z % 3 == 0 ? "sea_lantern" : "deepslate_tiles");
            if (ax == 6) for (int y = TERRACE + 1; y <= ceil; y++) d.set(x + (x > 0 ? 1 : -1), y, z, y % 4 == 0 ? "ochre_froglight" : "polished_blackstone");
        }
        // copper door frame at the tunnel mouth
        for (int x = -5; x <= 5; x++) {
            d.set(x, TERRACE + 6, FRONT_Z, "cut_copper");
            d.set(x, TERRACE + 5, FRONT_Z, x == 0 ? "gold_block" : "black_concrete");
        }
        for (int y = TERRACE + 1; y <= TERRACE + 6; y++) {
            d.set(-6, y, FRONT_Z, "cut_copper");
            d.set(6, y, FRONT_Z, "cut_copper");
        }
    }

    // ================================================================================================ glass dome
    void glassDome() {
        double cx = CX + 0.5, cz = CZ + 0.5, rx = 48.0, ry = 44.0;
        for (int x = CX - 49; x <= CX + 49; x++) for (int z = CZ - 49; z <= CZ + 49; z++) {
            for (int y = RIM + 1; y <= RIM + 45; y++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - RIM) / ry, dz = (z + 0.5 - cz) / rx;
                double e = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (e > 1.0 || e < 1.0 - 1.7 / 46.0) continue;
                double rr = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (rr < 5.5 && y > RIM + 40) continue; // oculus
                double a = Math.toDegrees(Math.atan2(z + 0.5 - cz, x + 0.5 - cx));
                // 12 meridian ribs (every 30 deg) and 4 latitude rings
                double arc = Math.abs(((a % 30.0) + 30.0) % 30.0 - 15.0);
                boolean rib = (15.0 - arc) * Math.PI / 180.0 * Math.max(1, rr) < 0.55;
                boolean latRing = y == RIM + 8 || y == RIM + 18 || y == RIM + 28 || y == RIM + 37;
                String b;
                if (y <= RIM + 1) b = "calcite";
                else if (rib || latRing) b = latRing && rib ? "gold_block" : (rib ? "copper_block" : "cut_copper"); else if (y < RIM + 12) b = d.pick(x, y, z, 51, "light_blue_stained_glass", 5, "cyan_stained_glass", 2, "glass", 2, "tinted_glass", 1); else if (y < RIM + 24) b = d.pick(x, y, z, 52, "light_blue_stained_glass", 4, "glass", 3, "cyan_stained_glass", 1); else b = d.pick(x, y, z, 53, "glass", 4, "light_blue_stained_glass", 3, "tinted_glass", 1);
                d.set(x, y, z, b);
            }
        }
        // oculus ring at the apex
        d.ring(cx, RIM + 42, cz, 5.2, 7.4, "waxed_oxidized_copper");
        d.ring(cx, RIM + 43, cz, 5.2, 6.6, "gold_block");
        d.ring(cx, RIM + 44, cz, 5.2, 6.0, "calcite");
        // tension ring at the drum top
        d.ring(cx, RIM + 1, cz, 47.4, 48.6, "waxed_oxidized_copper");
        d.ring(cx, RIM + 2, cz, 47.8, 48.2, "gold_block");
    }

    // ================================================================================================ topper: armillary
    void topper() {
        double cx = CX + 0.5, cz = CZ + 0.5;
        int b = RIM + 44;
        d.disc(cx, b + 1, cz, 5.5, "polished_blackstone_bricks");
        d.disc(cx, b + 2, cz, 4.5, "polished_blackstone");
        d.disc(cx, b + 3, cz, 3.5, "cut_copper");
        d.disc(cx, b + 4, cz, 2.6, "gold_block");
        for (int y = b + 5; y <= b + 8; y++) d.disc(cx, y, cz, y == b + 6 ? 1.6 : 1.1, "cut_copper");
        // vertical axis
        for (int y = b + 9; y <= b + 23; y++) d.set(CX, y, CZ, "copper_block");
        // three brass rings around a small gold planet
        armillaryRing(cx, b + 16, cz, 7.5, 0, 0);
        armillaryRing(cx, b + 16, cz, 8.5, Math.PI / 2, 0);
        armillaryRing(cx, b + 16, cz, 9.3, 0, Math.PI / 2);
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            int m = Math.abs(x) + Math.abs(y) + Math.abs(z);
            if (m > 2) continue;
            d.set(CX + x, b + 16 + y, CZ + z, m == 0 ? "ochre_froglight" : "gold_block");
        }
        d.set(CX, b + 24, CZ, "end_rod[facing=up]");
        L.fireworkSpots.add(new double[]{cx, b + 26, cz});
    }

    // ================================================================================================ gatehouse
    void gatehouse() {
        int zf = FRONT_Z;        // -10
        int zb = CZ + 38;        // -24
        int top = 104;
        for (int x = -27; x <= 27; x++) for (int z = zb; z <= zf; z++) {
            if (r(x, z) < R_IN + 0.2) continue;
            for (int y = TERRACE + 1; y <= top; y++) {
                boolean face = z == zf;
                String b;
                if (!face) b = (y % 6 == 0) ? "calcite" : (y % 4 == 0 ? "deepslate_tiles" : "polished_basalt");
                else b = gateFace(x, y);
                d.set(x, y, z, b);
            }
        }
        // entrance opening through the gatehouse
        for (int z = zb; z <= zf; z++) for (int x = -6; x <= 6; x++) {
            if (r(x, z) < 39.0) continue;
            int ax = Math.abs(x);
            int ceil = 86 - (ax >= 5 ? 1 : 0) - (ax >= 6 ? 1 : 0);
            for (int y = TERRACE + 1; y <= ceil; y++) d.air(x, y, z);
            d.set(x, TERRACE, z, ax <= 2 ? "red_wool" : ax == 3 ? "gold_block" : "polished_deepslate");
            if (ax == 6 || ax == 5) d.set(x, ceil + 1, z, "cut_copper");
        }
        // grand copper portal around the doors (kept open; the tunnel is the threshold)
        for (int y = TERRACE + 1; y <= TERRACE + 9; y++) {
            d.set(-8, y, zf, "calcite");
            d.set(8, y, zf, "calcite");
            d.set(-7, y, zf, "cut_copper");
            d.set(7, y, zf, "cut_copper");
        }
        for (int x = -9; x <= 9; x++) {
            d.set(x, TERRACE + 10, zf, Math.abs(x) >= 7 ? "calcite" : "gold_block");
            d.set(x, TERRACE + 9, zf, Math.abs(x) >= 7 ? "cut_copper" : "waxed_oxidized_copper");
        }
        for (int x = -4; x <= 4; x++) for (int y = TERRACE + 7; y <= TERRACE + 8; y++)
            d.set(x, y, zf, (x + y) % 3 == 0 ? "sea_lantern" : "tinted_glass");
        for (int s : new int[]{-1, 1}) {
            d.set(s * 9, TERRACE + 1, zf + 1, "polished_blackstone");
            d.set(s * 9, TERRACE + 2, zf + 1, "gold_block");
            d.set(s * 9, TERRACE + 3, zf + 1, "lantern[hanging=false]");
        }
        // marquee: black field, cream BLOCK SHOWDOWN, copper frame and orrery emblem
        int mTop = 126, mBot = 105;
        for (int x = -28; x <= 28; x++) for (int y = mBot; y <= mTop; y++) for (int z = zf - 2; z <= zf; z++) {
            boolean frame = Math.abs(x) >= 27 || y == mBot || y == mTop;
            boolean frame2 = Math.abs(x) == 26 || y == mBot + 1 || y == mTop - 1;
            String b = frame ? "copper_block" : frame2 ? "gold_block" : (z == zf ? logoPixel(x, y) : "black_concrete");
            d.set(x, y, z, b);
        }
        for (int x = -28; x <= 28; x += 2) { d.set(x, mTop + 1, zf, "ochre_froglight"); d.set(x, mBot - 1, zf + 1, "end_rod[facing=south]"); }
        for (int y = mBot; y <= mTop; y += 2) { d.set(-29, y, zf, "ochre_froglight"); d.set(29, y, zf, "ochre_froglight"); }
        // canopy
        for (int x = -9; x <= 9; x++) for (int z = zf + 1; z <= zf + 3; z++) d.set(x, 88, z, Math.abs(x) == 9 || z == zf + 3 ? "waxed_oxidized_copper" : "polished_blackstone");
        for (int x = -8; x <= 8; x += 4) d.set(x, 87, zf + 3, "lantern[hanging=true]");
        for (int s : new int[]{-1, 1}) tower(s * 31, -14, 70, 130, true);
        // clock
        L.clockX0 = -12; L.clockY0 = 92; L.clockZ = zf;
        drawClock("--:--");
        L.label(0.5, 104.2, zf + 1.6, 0, "§e§lDECISION DOME");
        L.entrance[0] = 0.5; L.entrance[1] = TERRACE + 1; L.entrance[2] = zf + 4.5; L.entrance[3] = 180;
    }

    void drawClock(String text) {
        for (int c = -3; c < Glyphs.CLOCK_W + 3; c++) for (int r = -2; r < Glyphs.SEG_H + 2; r++) {
            int x = L.clockX0 + c, y = L.clockY0 + (Glyphs.SEG_H - 1 - r);
            boolean in = c >= 0 && c < Glyphs.CLOCK_W && r >= 0 && r < Glyphs.SEG_H;
            String b = in && Glyphs.clockPixel(text, c, r) ? L.clockOn : "black_concrete";
            d.set(x, y, L.clockZ, b);
        }
        for (int c = -4; c <= Glyphs.CLOCK_W + 3; c++) for (int r = -3; r <= Glyphs.SEG_H + 2; r++) {
            boolean edge = c == -4 || c == Glyphs.CLOCK_W + 3 || r == -3 || r == Glyphs.SEG_H + 2;
            if (!edge) continue;
            int x = L.clockX0 + c, y = L.clockY0 + (Glyphs.SEG_H - 1 - r);
            d.set(x, y, L.clockZ, "waxed_oxidized_copper");
            d.set(x, y, L.clockZ + 1, (r == -3 || r == Glyphs.SEG_H + 2) ? "polished_blackstone_slab[type=" + (r == -3 ? "bottom" : "top") + "]" : "polished_blackstone_wall");
        }
    }

    String gateFace(int x, int y) {
        int ax = Math.abs(x);
        if (y <= TERRACE + 1) return "polished_blackstone";
        if (y == 89 || y == 103) return "calcite";
        if (ax >= 25) return y % 3 == 0 ? "gold_block" : "polished_blackstone";
        if (ax >= 20 && ax <= 21) return "oxidized_copper";
        if (y < 89) {
            if (ax == 7 || ax == 8) return "waxed_oxidized_copper";
            if (ax == 9 || ax == 10) return y % 2 == 0 ? "deepslate_tiles" : "polished_basalt";
            return (y + ax) % 8 == 0 ? "waxed_oxidized_copper" : "polished_basalt";
        }
        if (y > 89 && y < 103) return ax > 17 ? ((y % 4 == 0) ? "calcite" : "deepslate_tiles") : "black_concrete";
        return "calcite";
    }

    /** Marquee face: copper orrery emblem on the left, BLOCK in gold, SHOWDOWN in cream. */
    String logoPixel(int x, int y) {
        String bg = "black_concrete";
        int sw = Glyphs.textWidth("SHOWDOWN");
        int sx0 = -sw / 2 + 3;
        if (y >= 107 && y <= 113) {
            int col = x - sx0, row = 113 - y;
            if (hit("SHOWDOWN", col, row)) return row >= 5 ? "smooth_quartz" : "calcite";
        }
        if (y >= 116 && y <= 122) {
            int col = x - (-9), row = 122 - y;
            if (hit("BLOCK", col, row)) return row <= 1 ? "gold_block" : (row <= 4 ? "yellow_concrete" : "copper_block");
        }
        // copper orrery emblem 15x15 at x -25..-11, y 111..125
        if (x >= -25 && x <= -11 && y >= 111 && y <= 125) {
            String e = orreryEmblem(x + 25, 125 - y, 15);
            if (e != null) return e;
        }
        return bg;
    }

    /** Pixel art of the Block Showdown emblem: an isometric cube (gold top, red left, blue right). Used by the plaza. */
    static String emblem(int c, int r, int s) {
        double cx = (s - 1) / 2.0;
        double x = c - cx;
        double half = s / 2.0 - 0.5;
        double q = half / 2.0;
        double y = r;
        double topY = 0.5, midY = topY + q, botTop = topY + 2 * q;
        double bottom = s - 1.0;
        double ax = Math.abs(x) / half, ay = Math.abs(y - midY) / q;
        if (Math.abs(x) > half + 0.01) return null;
        if (ax + ay <= 1.0) {
            if (ax + ay > 0.86) return "orange_concrete";
            return y < midY - q * 0.2 ? "yellow_concrete" : "gold_block";
        }
        double upperEdge = midY + q * (1 - ax);
        double lowerEdge = bottom - q * ax;
        if (y >= upperEdge - 0.01 && y <= lowerEdge + 0.01) {
            boolean outline = Math.abs(x) > half - 0.6 || y > lowerEdge - 0.9 || Math.abs(x) < 0.4;
            if (x < 0) return outline ? "red_nether_bricks" : (y > (upperEdge + lowerEdge) / 2 + 1 ? "red_terracotta" : "red_concrete");
            return outline ? "black_concrete" : (y > (upperEdge + lowerEdge) / 2 + 1 ? "blue_terracotta" : "blue_concrete");
        }
        return null;
    }

    /** Pixel art of a copper orrery: three rings and a gold planet. Returns null for background. */
    static String orreryEmblem(int c, int r, int s) {
        double cx = (s - 1) / 2.0, cy = (s - 1) / 2.0;
        double dx = c - cx, dy = r - cy;
        double d = Math.hypot(dx, dy);
        if (Math.abs(d - 6.2) < 0.7 || Math.abs(d - 4.2) < 0.55) return "oxidized_copper";
        if (Math.abs(dy) < 0.6 && Math.abs(dx) < 6.4) return "gold_block";
        if (Math.abs(dx) < 0.6 && Math.abs(dy) < 6.4) return "gold_block";
        if (d < 1.6) return "ochre_froglight";
        if (d < 2.3) return "gold_block";
        if (Math.abs(dx + dy) < 0.6 && d < 5.8) return "copper_block";
        return null;
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
            if (corner) b = y % 2 == 0 ? "polished_blackstone_wall" : "polished_blackstone";
            else if (!edge) b = "polished_basalt";
            else if (band == 0) b = "gold_block";
            else if (band == 4) b = "calcite";
            else if (band >= 5 && (x == cx || z == cz)) b = "ochre_froglight";
            else b = y > y1 - 10 ? "calcite" : "deepslate_tiles";
            d.set(x, y, z, b);
        }
        for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
            d.set(x, y1 + 1, z, (Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) ? "gold_block" : "polished_blackstone");
            if ((Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3) && (x + z) % 2 == 0) d.set(x, y1 + 2, z, "smooth_quartz_slab[type=bottom]");
        }
        if (searchlight) {
            d.fill(cx - 1, y1 + 1, cz - 1, cx + 1, y1 + 1, cz + 1, "iron_block");
            d.set(cx, y1 + 2, cz, "beacon");
            L.searchlights.add(new double[]{cx + 0.5, y1 + 3, cz + 0.5, (cx * 0.37 + cz * 0.11)});
        }
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) g.paved(x, z, y1 + 1);
    }

    // ================================================================================================ pylons
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
                    String b = mid && outerEdge ? (y % 6 < 3 ? "calcite" : "waxed_oxidized_copper") : mid ? "oxidized_copper" : (y % 5 == 0 ? "calcite" : "polished_blackstone");
                    if (y == 70) b = "polished_blackstone";
                    d.set(x, y, z, b);
                }
            }
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

    // ================================================================================================ layout
    void layout() {
        // visitors arrive on the red carpet at the end of the tunnel, looking across the pit at the dial
        L.interiorSpawn[0] = CX + 0.5; L.interiorSpawn[1] = TERRACE + 1; L.interiorSpawn[2] = CZ + 31.5; L.interiorSpawn[3] = 180;
        for (int k = 0; k < 120 && L.seats.size() < 120; k++) {
            // seats are filled by stands(); this is a fallback ring if a row was clipped
            double deg = k * 3.0;
            if (Math.abs(deg - 90) < 12) continue;
            double a = Math.toRadians(deg);
            double rr = (k % 2 == 0) ? 34.8 : 33.5;
            double x = CX + 0.5 + Math.cos(a) * rr, z = CZ + 0.5 + Math.sin(a) * rr;
            float yaw = (float) Math.toDegrees(Math.atan2(-(CX + 0.5 - x), CZ + 0.5 - z));
            L.seats.add(new double[]{x, ringTop(rr) + 1.0, z, yaw});
        }
        L.searchlights.add(new double[]{CX + 0.5 - 40, TERRACE + 1, CZ + 0.5 + 33, 1.3});
        L.searchlights.add(new double[]{CX + 0.5 + 40, TERRACE + 1, CZ + 0.5 + 33, 2.1});
        L.searchlights.add(new double[]{CX + 0.5 - 46, TERRACE + 1, CZ + 0.5 - 22, 0.4});
        L.searchlights.add(new double[]{CX + 0.5 + 46, TERRACE + 1, CZ + 0.5 - 22, 3.3});
        for (double[] s : L.searchlights.subList(Math.max(0, L.searchlights.size() - 4), L.searchlights.size())) {
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
