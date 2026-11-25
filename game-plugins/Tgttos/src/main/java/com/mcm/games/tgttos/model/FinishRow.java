package com.mcm.games.tgttos.model;

import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

/**
 * The finish of every TGTTOS map (owner, feedback 3): "The ending is always a whole row of chickens, not a small box
 * that's hidden ... everyone knows where the ending is, but it's hard to get there because everyone is punching each
 * other." A straight row of goal chickens across the full width of the finish line, one every 2 blocks, standing on
 * hay in front of a black / white chequered finish board, under an arch with a FINISH sign. Racers punch them from the
 * open strip in front. Pure Java (generators and validators).
 *
 * <p>The row runs along x at z = {@code z}; {@code approach} = +1 if racers arrive travelling +Z (they stand at
 * z - 1), -1 if travelling -Z (they stand at z + 1). {@code fy} = top block of the floor under the row.
 */
public final class FinishRow {
    private FinishRow() {}

    public static final int SPACING = 2;

    /** Walkable span along x at (z, fy) round cx: floor solid, two air cells above, at most maxHalf each way. */
    public static int[] span(TgMapData d, int cx, int fy, int z, int maxHalf) {
        int a = cx, b = cx;
        while (a - 1 >= cx - maxHalf && stand(d, a - 1, fy, z)) a--;
        while (b + 1 <= cx + maxHalf && stand(d, b + 1, fy, z)) b++;
        return new int[]{a, b};
    }

    private static boolean stand(TgMapData d, int x, int fy, int z) {
        return d.blocks.get(x, fy, z) != null && d.blocks.get(x, fy + 1, z) == null && d.blocks.get(x, fy + 2, z) == null;
    }

    /**
     * Builds the row on [xa, xb] (inclusive; the arch posts go at xa - 1 and xb + 1) and sets d.pen and
     * d.chickenSpawns. Clears the row and the strip in front (2 high), keeps a floor under both.
     */
    public static void build(TgMapData d, int xa, int xb, int fy, int z, int approach, String floorBlock) {
        TgMapData.Blocks b = d.blocks;
        int front = z - approach, back = z + approach;
        for (int x = xa; x <= xb; x++) {
            for (int y = fy + 1; y <= fy + 3; y++) { b.set(x, y, z, null); }
            for (int y = fy + 1; y <= fy + 2; y++) b.set(x, y, front, null);
            if (b.get(x, fy, front) == null) b.set(x, fy, front, floorBlock);
            b.set(x, fy, z, ((x - xa) % SPACING) == 0 ? "hay_block[axis=y]" : "yellow_terracotta");
        }
        // chequered finish board behind the row (stops runners, frames the chickens)
        for (int x = xa - 1; x <= xb + 1; x++)
            for (int y = fy + 1; y <= fy + 3; y++) b.set(x, y, back, ((x + y) & 1) == 0 ? "black_wool" : "white_wool");
        // arch: striped posts at both ends, chequered beam, hanging lanterns, FINISH sign over the beam
        int top = fy + 6;
        for (int x : new int[]{xa - 1, xb + 1}) {
            for (int y = fy + 1; y <= top; y++) b.set(x, y, z, (y & 1) == 0 ? "red_wool" : "white_wool");
            b.set(x, fy, z, "polished_blackstone");
        }
        for (int x = xa - 1; x <= xb + 1; x++) {
            b.set(x, top + 1, z, ((x + top) & 1) == 0 ? "black_wool" : "white_wool");
            b.set(x, top + 2, z, ((x + top + 1) & 1) == 0 ? "black_wool" : "white_wool");
        }
        for (int x = xa + 1; x <= xb - 1; x += 4) { b.set(x, top, z, "lantern[hanging=true]"); }
        String word = "FINISH";
        int w = textWidth(word);
        int mid = (xa + xb) / 2;
        if (xb - xa + 3 >= w + 2) {
            int y0 = top + 3, y1 = top + 9;
            for (int x = mid - w / 2 - 1; x <= mid - w / 2 + w; x++) for (int y = y0; y <= y1; y++) b.set(x, y, z, "black_wool");
            // readable for a racer arriving: travelling +Z the text runs towards -X
            int ux = approach > 0 ? -1 : 1;
            int sx = ux < 0 ? mid - w / 2 + w - 1 : mid - w / 2;
            text(b, word, sx, y1 - 1, z, ux, "gold_block");
        }
        d.pen = Region.of(xa, fy + 1, z, xb, fy + 3, z);
        d.chickenSpawns.clear();
        float yaw = approach > 0 ? 180f : 0f;   // the chickens face the racers
        for (int x = xa; x <= xb; x += SPACING) d.chickenSpawns.add(new Spawn(x + 0.5, fy + 1, z + 0.5, yaw));
    }

    // ------------------------------------------------------------------ validator

    /** Result of {@link #check}: ok + a one-line description. */
    public record Result(boolean ok, String text) {}

    /**
     * The row rule: >= 8 chickens in a single straight row (pen 1 deep) covering the row, one every 1.5..2.5 blocks,
     * each with an open standing cell (floor + 2 air) right in front of it, and >= 75% of them in clear sight from one
     * open spot of the approach 8..16 blocks in front of the row (centre or quarter points) at eye height.
     */
    public static Result check(TgMapData d) {
        List<String> bad = new ArrayList<>();
        Region p = d.pen;
        if (p == null) return new Result(false, "no finish row");
        int n = d.chickenSpawns.size();
        boolean row = p.minZ() == p.maxZ() || p.minX() == p.maxX();
        if (!row) bad.add("pen is not a single row (" + (p.maxX() - p.minX() + 1) + " x " + (p.maxZ() - p.minZ() + 1) + ")");
        boolean alongX = p.minZ() == p.maxZ();
        int width = alongX ? p.maxX() - p.minX() + 1 : p.maxZ() - p.minZ() + 1;
        if (n < 8) bad.add(n + " chickens (< 8)");
        List<Double> xs = new ArrayList<>();
        for (Spawn s : d.chickenSpawns) {
            if (!p.contains(s.x(), s.y(), s.z())) bad.add("chicken outside the row at " + s.x() + "," + s.z());
            xs.add(alongX ? s.x() : s.z());
        }
        xs.sort(Double::compare);
        double maxGap = 0, minGap = 99;
        for (int i = 1; i < xs.size(); i++) { maxGap = Math.max(maxGap, xs.get(i) - xs.get(i - 1)); minGap = Math.min(minGap, xs.get(i) - xs.get(i - 1)); }
        if (n > 1 && (maxGap > 2.5 || minGap < 1.5)) bad.add(String.format("spacing %.1f..%.1f", minGap, maxGap));
        // the front side: where the standing cells are
        int fy = p.minY() - 1;
        int stands = 0, front = 0;
        for (int side : new int[]{-1, 1}) {
            int c = 0;
            for (Spawn s : d.chickenSpawns) {
                int x = (int) Math.floor(s.x()) + (alongX ? 0 : side), z = (int) Math.floor(s.z()) + (alongX ? side : 0);
                if (d.blocks.get(x, fy, z) != null && d.blocks.get(x, fy + 1, z) == null && d.blocks.get(x, fy + 2, z) == null) c++;
            }
            if (c > stands) { stands = c; front = side; }
        }
        if (stands < n) bad.add((n - stands) + " chickens without a standing cell in front");
        // sight: from the approach, 8..16 blocks out in front of the row (centre or a quarter of the way in from either
        // end), at eye height, to the top of each chicken; the best open spot counts
        double cx = (p.minX() + p.maxX() + 1) / 2.0, cz = (p.minZ() + p.maxZ() + 1) / 2.0, ey = fy + 2.62;
        double q = width / 4.0;
        int seen = 0, dist = 12;
        for (int dd = 8; dd <= 16; dd++)
            for (double lat : new double[]{0, -q, q}) {
                double ex = alongX ? cx + lat : cx + front * dd, ez = alongX ? cz + front * dd : cz + lat;
                if (d.blocks.get((int) Math.floor(ex), (int) Math.floor(ey), (int) Math.floor(ez)) != null
                        || d.blocks.get((int) Math.floor(ex), (int) Math.floor(ey) - 1, (int) Math.floor(ez)) != null) continue;
                int c = 0;
                for (Spawn s : d.chickenSpawns) if (clear(d, ex, ey, ez, s.x(), s.y() + 0.65, s.z())) c++;
                if (c > seen) { seen = c; dist = dd; }
            }
        if (seen * 4 < n * 3) bad.add(seen + "/" + n + " chickens in sight from " + dist + " in front");
        String txt = String.format("finish row: %d chickens in one row %d wide (pen %d x %d), spacing %.1f..%.1f, %d/%d with a standing cell in front, %d/%d in sight from %d blocks out%s",
                n, width, p.maxX() - p.minX() + 1, p.maxZ() - p.minZ() + 1, minGap == 99 ? 0 : minGap, maxGap, stands, n, seen, n, dist,
                bad.isEmpty() ? "" : " - " + String.join("; ", bad));
        return new Result(bad.isEmpty(), txt);
    }

    private static boolean clear(TgMapData d, double x0, double y0, double z0, double x1, double y1, double z1) {
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int k = (int) Math.ceil(len / 0.25);
        for (int i = 1; i < k - 1; i++) {
            double t = i / (double) k;
            String b = d.blocks.get((int) Math.floor(x0 + (x1 - x0) * t), (int) Math.floor(y0 + (y1 - y0) * t), (int) Math.floor(z0 + (z1 - z0) * t));
            if (b != null && opaque(b)) return false;
        }
        return true;
    }

    private static boolean opaque(String b) {
        String id = b.startsWith("minecraft:") ? b.substring(10) : b;
        for (String k : new String[]{"glass", "pane", "bars", "fence", "_wall", "chain", "lantern", "carpet", "torch", "flower", "grass", "rail", "button", "sign", "banner", "leaves"})
            if (id.contains(k)) return false;
        return true;
    }

    // ------------------------------------------------------------------ tiny pixel font (same glyphs as the B maps' G.text)

    private static final String[] FONT = {"F:111100110100100", "I:111010010010111", "N:110101101101101", "S:011100010001110", "H:101101111101101"};

    static int textWidth(String t) { return t.length() * 4 - 1; }

    private static void text(TgMapData.Blocks b, String t, int x, int y, int z, int ux, String block) {
        int cx = 0;
        for (char c : t.toCharArray()) {
            String g = null;
            for (String f : FONT) if (f.charAt(0) == c) g = f.substring(2);
            if (g != null)
                for (int r = 0; r < 5; r++)
                    for (int k = 0; k < 3; k++)
                        if (g.charAt(r * 3 + k) == '1') b.set(x + ux * (cx + k), y - r, z, block);
            cx += 4;
        }
    }
}
