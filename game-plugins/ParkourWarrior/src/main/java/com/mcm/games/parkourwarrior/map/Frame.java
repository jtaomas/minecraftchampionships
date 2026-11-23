package com.mcm.games.parkourwarrior.map;

/**
 * The fixed frame architecture, one original style used everywhere ("lantern rotunda"): round white quartz decks
 * with a copper inlay ring and a weathered-copper rim, a pale diorite balustrade with lanterns, quartz columns
 * carrying a stepped copper parasol roof with a lightning-rod finial, a chandelier of lanterns, and a stone owl
 * statue (the course mascot) on a plinth. Hubs, the start island, the fork pavilion and the finish shrines all use
 * it. World coordinates; the spine runs along +z.
 */
final class Frame {
    private Frame() {}

    static final String RIM = "waxed_weathered_cut_copper";
    static final String INLAY = "waxed_cut_copper";
    static final String DECK = "smooth_quartz";
    static final String SPOKE = "quartz_bricks";
    static final String RAIL = "diorite_wall";
    static final String[] ROCK = {"stone", "andesite", "tuff", "cobbled_deepslate", "deepslate"};

    static boolean inOct(int dx, int dz, int r) {
        return Math.abs(dx) <= r && Math.abs(dz) <= r && Math.abs(dx) + Math.abs(dz) <= r + r / 2;
    }

    static boolean isExit(int dx, int dz, int r, boolean n, boolean s, boolean e, boolean wst) {
        if (n && dz <= -r + 1 && Math.abs(dx) <= 1) return true;
        if (s && dz >= r - 1 && Math.abs(dx) <= 1) return true;
        if (e && dx >= r - 1 && Math.abs(dz) <= 1) return true;
        return wst && dx <= -r + 1 && Math.abs(dz) <= 1;
    }

    /** Round deck with rim, inlay ring, spokes, balustrade (gaps at the exits) and rock underside. */
    static void deck(W w, int cx, int y, int cz, int r, boolean exN, boolean exS, boolean exE, boolean exW, int depth) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (!inOct(dx, dz, r)) continue;
            boolean rim = !inOct(dx + 1, dz, r) || !inOct(dx - 1, dz, r) || !inOct(dx, dz + 1, r) || !inOct(dx, dz - 1, r);
            double d = Math.sqrt(dx * dx + dz * dz);
            String s;
            if (rim) s = RIM;
            else if (Math.abs(d - (r - 2.5)) < 0.55) s = INLAY;
            else if (d < 1.6) s = "chiseled_quartz_block";
            else if (dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz)) s = SPOKE;
            else s = DECK;
            if (!w.padCells.contains(W.key(cx + dx, y, cz + dz))) w.set(cx + dx, y, cz + dz, s);
            w.walk(cx + dx, y, cz + dz);
            w.set(cx + dx, y - 1, cz + dz, rim ? "polished_andesite" : "stone_bricks");
            if (rim && !isExit(dx, dz, r, exN, exS, exE, exW)) {
                boolean post = (Math.abs(dx) + Math.abs(dz)) % 4 == 0;
                w.set(cx + dx, y + 1, cz + dz, post ? "chiseled_quartz_block" : RAIL);
                w.walk(cx + dx, y + 1, cz + dz);
                if (post) w.set(cx + dx, y + 2, cz + dz, "lantern[hanging=false,waterlogged=false]");
            }
            if (rim) {
                // little hanging trim under the rim
                if ((dx + dz & 1) == 0) w.soft(cx + dx, y - 2, cz + dz, "polished_andesite_slab[type=top,waterlogged=false]");
            }
        }
        // rock underside
        for (int k = 2; k <= depth + 1; k++) {
            double rr = (r - 0.5) * (1.0 - (k - 1.0) / (depth + 1)) ;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                double n = W.h(cx + dx, y - k, cz + dz, 7);
                if (dx * dx + dz * dz > (rr + n * 1.2 - 0.4) * (rr + n * 1.2 - 0.4)) continue;
                int idx = Math.min(ROCK.length - 1, (int) (k * ROCK.length / (double) (depth + 2) + n * 1.3));
                w.soft(cx + dx, y - k, cz + dz, ROCK[idx]);
                if (k == 2 && n > 0.8) w.soft(cx + dx, y - k, cz + dz, "moss_block");
            }
        }
        // a few hanging vines/roots under the rim
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (!inOct(dx, dz, r) || W.h(cx + dx, 1, cz + dz, 11) < 0.93) continue;
            for (int k = 2; k < 2 + 1 + (int) (W.h(dx, 2, dz, 3) * 4); k++)
                if (w.air(cx + dx, y - k, cz + dz) && !w.air(cx + dx, y - k + 1, cz + dz) || Bs.id(w.get(cx + dx, y - k + 1, cz + dz)).equals("hanging_roots"))
                    w.set(cx + dx, y - k, cz + dz, "hanging_roots[waterlogged=false]");
        }
    }

    /** Four quartz columns and the stepped copper parasol roof with finial and chandelier. */
    static void rotunda(W w, int cx, int y, int cz, int colR, int roofY, String banner) {
        int[][] cols = {{colR, colR}, {-colR, colR}, {colR, -colR}, {-colR, -colR}};
        for (int[] c : cols) {
            int x = cx + c[0], z = cz + c[1];
            w.set(x, y + 1, z, "chiseled_quartz_block");
            for (int yy = y + 2; yy < roofY; yy++) w.set(x, yy, z, "quartz_pillar[axis=y]");
            w.set(x, roofY - 1, z, INLAY);
            // zone banner on the outward faces
            if (banner != null) {
                String fx = c[0] > 0 ? "east" : "west";
                w.set(x + (c[0] > 0 ? 1 : -1), roofY - 2, z, banner + "_wall_banner[facing=" + fx + "]");
            }
        }
        int rr = colR + 2;
        for (int dx = -rr - 1; dx <= rr + 1; dx++) for (int dz = -rr - 1; dz <= rr + 1; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d <= rr + 0.6 && d > rr - 0.9) {
                String f = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "west" : "east") : (dz > 0 ? "north" : "south");
                w.set(cx + dx, roofY, cz + dz, "waxed_weathered_cut_copper_stairs[facing=" + f + ",half=bottom,shape=straight,waterlogged=false]");
                w.set(cx + dx, roofY - 1, cz + dz, "waxed_weathered_cut_copper_slab[type=top,waterlogged=false]");
            } else if (d <= rr - 0.9) {
                w.set(cx + dx, roofY, cz + dz, d < rr - 2.5 ? "waxed_oxidized_cut_copper" : RIM);
            }
            if (d <= rr - 1.9) w.set(cx + dx, roofY + 1, cz + dz, d > rr - 3 ? "waxed_weathered_cut_copper_stairs[facing="
                    + (Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "west" : "east") : (dz > 0 ? "north" : "south")) + ",half=bottom,shape=straight,waterlogged=false]"
                    : "waxed_oxidized_cut_copper");
            if (d <= rr - 3.6) w.set(cx + dx, roofY + 2, cz + dz, "waxed_oxidized_copper");
        }
        w.set(cx, roofY + 3, cz, "waxed_oxidized_copper");
        w.set(cx, roofY + 4, cz, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        // chandelier
        chandelier(w, cx, roofY - 1, cz);
    }

    /** The course mascot: a stone owl on a plinth (gold for finishes). facing = direction it looks. */
    static void owl(W w, int x, int y, int z, String facing, boolean gold) {
        String body = gold ? "gold_block" : "polished_diorite", belly = gold ? "raw_gold_block" : "calcite";
        String eye = gold ? "emerald_block" : "gold_block", beak = "orange_terracotta";
        String st = gold ? "smooth_sandstone_stairs" : "diorite_stairs";
        // local frame: front = facing; side axis
        int fx = 0, fz = 0;
        switch (facing) { case "north": fz = -1; break; case "south": fz = 1; break; case "east": fx = 1; break; default: fx = -1; }
        int sx = -fz, sz = fx;
        // plinth 3x3
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            w.set(x + a * sx + b * fx, y, z + a * sz + b * fz, "chiseled_stone_bricks");
            w.set(x + a * sx + b * fx, y + 1, z + a * sz + b * fz, Math.abs(a) + Math.abs(b) == 2 ? "polished_andesite" : "stone_bricks");
        }
        // feet + body 3 wide, 2 deep, 3 tall
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 0; b++) for (int h = 2; h <= 4; h++) {
            String s = (b == 0 && a == 0 && h <= 3) ? belly : body;
            w.set(x + a * sx + b * fx, y + h, z + a * sz + b * fz, s);
        }
        // wings
        for (int h = 2; h <= 4; h++) for (int sgn = -1; sgn <= 1; sgn += 2) {
            String out = sgn > 0 ? dirOf(sx, sz) : dirOf(-sx, -sz);
            w.set(x + sgn * 2 * sx, y + h, z + sgn * 2 * sz, h == 2 ? null : Bs.stairs(st.replace("_stairs", ""), opposite(out), h == 4));
        }
        // head 3x3x2
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 0; b++) for (int h = 5; h <= 6; h++)
            w.set(x + a * sx + b * fx, y + h, z + a * sz + b * fz, body);
        w.set(x - sx, y + 6, z - sz, eye);
        w.set(x + sx, y + 6, z + sz, eye);
        w.set(x + fx, y + 5, z + fz, beak.equals("orange_terracotta") ? "orange_glazed_terracotta" : beak);
        // brow and ear tufts
        w.set(x + fx, y + 6, z + fz, Bs.stairs(st.replace("_stairs", ""), opposite(facing), true));
        w.set(x - sx - fx, y + 7, z - sz - fz, Bs.stairs(st.replace("_stairs", ""), facing, false));
        w.set(x + sx - fx, y + 7, z + sz - fz, Bs.stairs(st.replace("_stairs", ""), facing, false));
    }

    static String dirOf(int dx, int dz) { return dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west"; }

    static String opposite(String f) {
        switch (f) { case "north": return "south"; case "south": return "north"; case "east": return "west"; default: return "east"; }
    }

    /** Ring chandelier hanging from yTop: chain, a copper ring with lanterns, a centre lantern. */
    static void chandelier(W w, int cx, int yTop, int cz) {
        w.set(cx, yTop, cz, "chain[axis=y,waterlogged=false]");
        w.set(cx, yTop - 1, cz, "chain[axis=y,waterlogged=false]");
        w.set(cx, yTop - 2, cz, "waxed_copper_bulb[lit=true,powered=false]");
        int[][] ring = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : ring) {
            w.set(cx + d[0], yTop - 2, cz + d[1], "waxed_cut_copper_slab[type=top,waterlogged=false]");
            w.set(cx + 2 * d[0], yTop - 2, cz + 2 * d[1], "waxed_cut_copper_slab[type=top,waterlogged=false]");
            w.set(cx + 2 * d[0], yTop - 3, cz + 2 * d[1], "lantern[hanging=true,waterlogged=false]");
        }
    }

    /** Stage hub: deck, rotunda, owl statue and the bonus-path arch on the east side. */
    static void hub(W w, int cx, int y, int cz, int r, String banner, boolean start) {
        deck(w, cx, y, cz, r, !start, true, true, false, start ? 12 : 9);
        rotunda(w, cx, y, cz, 5, y + 7, banner);
        owl(w, cx, y, cz, "east", false);
        bonusArch(w, cx + r, y, cz);
        // planters along the inside of the balustrade
        for (int dx = -r + 2; dx <= r - 2; dx++) for (int dz = -r + 2; dz <= r - 2; dz++) {
            if (!inOct(dx, dz, r - 1) || inOct(dx, dz, r - 2)) continue;
            if (isExit(dx, dz, r, !start, true, true, false) || Math.abs(dx) <= 2 || Math.abs(dz) <= 2) continue;
            if (W.h(cx + dx, y, cz + dz, 5) < 0.45) continue;
            w.set(cx + dx, y + 1, cz + dz, "flowering_azalea_leaves[distance=7,persistent=true,waterlogged=false]");
        }
    }

    /** Arch over the east exit with an orange/yellow arrow board pointing out along +x. */
    static void bonusArch(W w, int x, int y, int cz) {
        for (int s = -2; s <= 2; s += 4) {
            for (int yy = y + 1; yy <= y + 5; yy++) w.set(x, yy, cz + s, yy == y + 1 ? "chiseled_quartz_block" : "quartz_pillar[axis=y]");
            w.set(x, y + 6, cz + s, INLAY);
        }
        for (int s = -2; s <= 2; s++) w.set(x, y + 7, cz + s, "waxed_weathered_cut_copper_slab[type=bottom,waterlogged=false]");
        // arrow board facing west/east: a yellow band with an orange chevron, hung under the lintel
        for (int s = -1; s <= 1; s++) w.set(x, y + 6, cz + s, s == 0 ? "orange_concrete" : "yellow_concrete");
        w.set(x + 1, y + 6, cz, "orange_concrete");
        w.set(x + 2, y + 6, cz, "orange_glazed_terracotta");
        w.set(x, y + 5, cz, "lantern[hanging=true,waterlogged=false]");
    }

    static final int[] GATES = {-14, 0, 14};
    static final String[] GATE_COL = {"lime", "orange", "red"};

    static boolean inFork(int dx, int dz) {
        return Math.abs(dx) <= 20 && Math.abs(dz) <= 8 && (Math.abs(dx) - 14) + (Math.abs(dz) - 4) <= 6;
    }

    /** Fork pavilion: a long deck with three rotundas and three colour-coded gates on the south (+z) side. */
    static void fork(W w, int cx, int y, int cz) {
        for (int dx = -20; dx <= 20; dx++) for (int dz = -8; dz <= 8; dz++) {
            if (!inFork(dx, dz)) continue;
            boolean rim = !inFork(dx + 1, dz) || !inFork(dx - 1, dz) || !inFork(dx, dz + 1) || !inFork(dx, dz - 1);
            String st = rim ? RIM : (Math.abs(dz) == 5 && Math.abs(dx) <= 17) ? INLAY : (Math.floorMod(dx, 7) == 0 || dz == 0) ? SPOKE : DECK;
            int x = cx + dx, z = cz + dz;
            if (!w.padCells.contains(W.key(x, y, z))) w.set(x, y, z, st);
            w.walk(x, y, z);
            w.set(x, y - 1, z, rim ? "polished_andesite" : "stone_bricks");
            boolean exit = (dz <= -7 && Math.abs(dx) <= 1);
            for (int g : GATES) if (dz >= 7 && Math.abs(dx - g) <= 1) exit = true;
            if (rim && !exit) {
                boolean post = Math.floorMod(dx + dz, 4) == 0;
                w.set(x, y + 1, z, post ? "chiseled_quartz_block" : RAIL);
                w.walk(x, y + 1, z);
                if (post) w.set(x, y + 2, z, "lantern[hanging=false,waterlogged=false]");
            }
        }
        for (int k = 2; k <= 12; k++) {
            double fx = 19.5 * (1.0 - (k - 1.0) / 13), fz = 7.5 * (1.0 - (k - 1.0) / 13);
            for (int dx = -20; dx <= 20; dx++) for (int dz = -8; dz <= 8; dz++) {
                double n = W.h(cx + dx, y - k, cz + dz, 9);
                double e = (dx * dx) / (fx * fx + 0.01) + (dz * dz) / (fz * fz + 0.01);
                if (e > 1 + n * 0.25) continue;
                w.soft(cx + dx, y - k, cz + dz, ROCK[Math.min(ROCK.length - 1, (int) (k * 0.45 + n * 1.3))]);
            }
        }
        for (int g : GATES) rotunda(w, cx + g, y, cz - 1, 4, y + 8, null);
        for (int i = 0; i < 3; i++) gate(w, cx + GATES[i], y, cz + 7, GATE_COL[i], i + 1);
        owl(w, cx, y, cz - 2, "south", false);
    }

    /** Gate: two thick pillars in the route colour, a lintel and 1-3 gold stars on top. */
    static void gate(W w, int gx, int y, int z, String col, int stars) {
        for (int s = -2; s <= 2; s += 4) {
            for (int yy = y + 1; yy <= y + 6; yy++) {
                w.set(gx + s, yy, z, yy == y + 1 || yy == y + 6 ? col + "_glazed_terracotta" : col + "_concrete");
                w.set(gx + s + (s > 0 ? 1 : -1), yy, z, yy % 2 == 0 ? col + "_concrete" : "chiseled_quartz_block");
            }
            w.set(gx + s, y + 7, z, "chiseled_quartz_block");
        }
        for (int s = -3; s <= 3; s++) w.set(gx + s, y + 7, z, s == -3 || s == 3 ? "chiseled_quartz_block" : col + "_concrete");
        for (int s = -1; s <= 1; s++) w.set(gx + s, y + 6, z, col + "_wool");
        int[][] pos = stars == 1 ? new int[][]{{0}} : stars == 2 ? new int[][]{{-1}, {1}} : new int[][]{{-2}, {0}, {2}};
        for (int[] p : pos) {
            w.set(gx + p[0], y + 8, z, "gold_block");
            w.set(gx + p[0], y + 9, z, "end_rod[facing=up]");
        }
        w.set(gx, y + 5, z, "lantern[hanging=true,waterlogged=false]");
    }

    /** Finish shrine around the final pad (centre px, py, pz), facing +z: floor, portal, thick pillars, gold owl. */
    static void shrine(W w, int px, int py, int pz, String col) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 4; dz++) {
            int x = px + dx, z = pz + dz;
            boolean rim = Math.abs(dx) == 3 || dz == 4;
            if (!w.padCells.contains(W.key(x, py, z))) w.set(x, py, z, rim ? col + "_concrete" : DECK);
            w.walk(x, py, z);
            w.set(x, py - 1, z, "stone_bricks");
            if (dz == -2 && !w.padCells.contains(W.key(x, py, z))) w.set(x, py, z, rim ? col + "_concrete" : SPOKE);
        }
        for (int k = 2; k <= 7; k++) {
            int r = Math.max(0, 3 - k / 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) w.soft(px + dx, py - k, pz + 1 + dz, ROCK[Math.min(4, k / 2)]);
        }
        int bz = pz + 5;
        for (int dx = -3; dx <= 3; dx++) for (int yy = py; yy <= py + 7; yy++) {
            boolean frame = Math.abs(dx) >= 2 || yy == py || yy >= py + 6;
            w.set(px + dx, yy, bz, frame ? (Math.abs(dx) == 3 ? "quartz_pillar[axis=y]" : "chiseled_quartz_block") : col + "_stained_glass");
        }
        for (int s = -1; s <= 1; s += 2) {
            for (int yy = py - 2; yy <= py + 8; yy++) for (int a = 4; a <= 5; a++) for (int dz = 3; dz <= 4; dz++)
                w.set(px + s * a, yy, pz + dz, yy >= py + 8 ? col + "_glazed_terracotta" : (yy + a + dz) % 3 == 0 ? "chiseled_quartz_block" : col + "_concrete");
            w.set(px + s * 4, py + 9, pz + 3, "end_rod[facing=up]");
            w.set(px + s * 5, py + 9, pz + 4, "lantern[hanging=false,waterlogged=false]");
        }
        owl(w, px, py + 7, bz - 1, "north", true);
    }

    /** End of a bonus path: gold trim around the last pad and two lantern posts. */
    static void spurEnd(W w, int px, int py, int pz, int fx, int fz) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
            w.set(px + dx, py, pz + dz, "gold_block");
            w.walk(px + dx, py, pz + dz);
        }
        int sx = -fz, sz = fx;
        for (int s = -1; s <= 1; s += 2) {
            int x = px + 2 * fx + 2 * s * sx, z = pz + 2 * fz + 2 * s * sz;
            w.set(x, py, z, "chiseled_quartz_block");
            w.set(x, py + 1, z, "chiseled_quartz_block");
            w.set(x, py + 2, z, "quartz_pillar[axis=y]");
            w.set(x, py + 3, z, "gold_block");
            w.set(x, py + 4, z, "lantern[hanging=false,waterlogged=false]");
        }
    }
}
