package com.mcm.games.buildmart;

import java.util.Map;

/** Chunky 5 x 7 block letters for the department signs and the MCC facade (MCC's signs use a pixel font). */
final class BlockFont {
    private BlockFont() { }

    static final int H = 7;
    private static final Map<Character, String[]> G = Map.ofEntries(
            Map.entry('B', new String[]{"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."}),
            Map.entry('C', new String[]{".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."}),
            Map.entry('D', new String[]{"####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."}),
            Map.entry('E', new String[]{"#####", "#....", "#....", "####.", "#....", "#....", "#####"}),
            Map.entry('L', new String[]{"#....", "#....", "#....", "#....", "#....", "#....", "#####"}),
            Map.entry('M', new String[]{"#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"}),
            Map.entry('N', new String[]{"#...#", "##..#", "#.#.#", "#.#.#", "#..##", "#...#", "#...#"}),
            Map.entry('O', new String[]{".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."}),
            Map.entry('R', new String[]{"####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"}),
            Map.entry('S', new String[]{".####", "#....", "#....", ".###.", "....#", "....#", "####."}),
            Map.entry('T', new String[]{"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."}),
            Map.entry('U', new String[]{"#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."}),
            Map.entry(' ', new String[]{"...", "...", "...", "...", "...", "...", "..."}));

    static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) w += glyph(c)[0].length() + 1;
        return Math.max(0, w - 1);
    }

    static String[] glyph(char c) { return G.getOrDefault(Character.toUpperCase(c), G.get(' ')); }

    /** Calls px(col, rowFromTop) for every lit pixel; col 0 = first letter's left column. */
    interface Px { void at(int col, int row); }

    static void draw(String s, Px px) {
        int x0 = 0;
        for (char c : s.toCharArray()) {
            String[] g = glyph(c);
            for (int r = 0; r < H; r++)
                for (int k = 0; k < g[r].length(); k++) if (g[r].charAt(k) == '#') px.at(x0 + k, r);
            x0 += g[0].length() + 1;
        }
    }
}
