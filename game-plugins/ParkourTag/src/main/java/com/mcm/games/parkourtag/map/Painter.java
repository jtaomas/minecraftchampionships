package com.mcm.games.parkourtag.map;

/**
 * Drawing helpers over a {@link BlockSink} in arena-local coordinates: (ax, az) is the pit (0,0 = north-west
 * street corner), y is absolute map height. The map origin is offset by {@code margin} on x and z so the stands
 * and walls around the pit stay at positive coordinates.
 */
public final class Painter {
    private final BlockSink s;
    public final int margin;

    public Painter(BlockSink s, int margin) { this.s = s; this.margin = margin; }

    public void set(int ax, int y, int az, String b) { s.set(ax + margin, y, az + margin, b); }

    public void fill(int ax0, int y0, int az0, int ax1, int y1, int az1, String b) {
        for (int x = Math.min(ax0, ax1); x <= Math.max(ax0, ax1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(az0, az1); z <= Math.max(az0, az1); z++) set(x, y, z, b);
    }

    /** Walls only (the four sides of the box). */
    public void walls(int ax0, int y0, int az0, int ax1, int y1, int az1, String b) {
        for (int x = ax0; x <= ax1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = az0; z <= az1; z++)
                    if (x == ax0 || x == ax1 || z == az0 || z == az1) set(x, y, z, b);
    }

    public void clear(int ax0, int y0, int az0, int ax1, int y1, int az1) { fill(ax0, y0, az0, ax1, y1, az1, "air"); }

    /** 5x7 block letters for signage. Returns the glyph rows top to bottom; '#' = filled. */
    public static String[] glyph(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'A' -> new String[]{".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"};
            case 'C' -> new String[]{".####", "#....", "#....", "#....", "#....", "#....", ".####"};
            case 'G' -> new String[]{".####", "#....", "#....", "#.###", "#...#", "#...#", ".###."};
            case 'K' -> new String[]{"#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"};
            case 'M' -> new String[]{"#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"};
            case 'O' -> new String[]{".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."};
            case 'P' -> new String[]{"####.", "#...#", "#...#", "####.", "#....", "#....", "#...."};
            case 'R' -> new String[]{"####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"};
            case 'T' -> new String[]{"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."};
            case 'U' -> new String[]{"#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."};
            case 'E' -> new String[]{"#####", "#....", "#....", "####.", "#....", "#....", "#####"};
            case 'N' -> new String[]{"#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"};
            case 'B' -> new String[]{"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."};
            case 'L' -> new String[]{"#....", "#....", "#....", "#....", "#....", "#....", "#####"};
            case 'S' -> new String[]{".####", "#....", "#....", ".###.", "....#", "....#", "####."};
            case 'Y' -> new String[]{"#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."};
            case 'J' -> new String[]{"..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##.."};
            case 'F' -> new String[]{"#####", "#....", "#....", "####.", "#....", "#....", "#...."};
            case 'I' -> new String[]{"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"};
            default -> new String[]{".....", ".....", ".....", ".....", ".....", ".....", "....."};
        };
    }

    /**
     * Writes text on a vertical plane. The text starts at (ax, top, az) and advances by (dx, dz) per column;
     * {@code shadow} (nullable) is drawn one block below and one column further on, behind the letter.
     */
    public void text(String t, int ax, int top, int az, int dx, int dz, String ink, String shadow) {
        int col = 0;
        for (char c : t.toCharArray()) {
            String[] g = glyph(c);
            for (int r = 0; r < 7; r++)
                for (int k = 0; k < 5; k++)
                    if (g[r].charAt(k) == '#') {
                        int x = ax + dx * (col + k), z = az + dz * (col + k);
                        if (shadow != null) set(x + dx, top - r - 1, z + dz, shadow);
                    }
            for (int r = 0; r < 7; r++)
                for (int k = 0; k < 5; k++)
                    if (g[r].charAt(k) == '#') set(ax + dx * (col + k), top - r, az + dz * (col + k), ink);
            col += 6;
        }
    }
}
