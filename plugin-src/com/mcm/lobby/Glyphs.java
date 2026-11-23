package com.mcm.lobby;

import java.util.HashMap;
import java.util.Map;

/** Block-pixel glyphs: a 5x9 seven-segment clock font and a 5x7 logo font. Rows are top to bottom. */
public final class Glyphs {
    private Glyphs() {}

    public static final int SEG_W = 5, SEG_H = 9;
    /** Width of "MM:SS" in pixels: 4 digits of 5, gaps of 1, colon column with a gap each side. */
    public static final int CLOCK_W = 25;

    // segments a b c d e f g
    private static final int[] SEGS = {
            0b1111110, 0b0110000, 0b1101101, 0b1111001, 0b0110011,
            0b1011011, 0b1011111, 0b1110000, 0b1111111, 0b1111011
    };

    /** Pixel of a seven-segment char: '0'..'9', '-', ' ' . c in [0,5), r in [0,9) from the top. */
    public static boolean seg(char ch, int c, int r) {
        int m;
        if (ch >= '0' && ch <= '9') m = SEGS[ch - '0'];
        else if (ch == '-') m = 0b0000001;
        else return false;
        boolean a = (m & 0b1000000) != 0, b = (m & 0b0100000) != 0, cc = (m & 0b0010000) != 0, d = (m & 0b0001000) != 0,
                e = (m & 0b0000100) != 0, f = (m & 0b0000010) != 0, g = (m & 0b0000001) != 0;
        boolean left = c == 0, right = c == 4, mid = !left && !right;
        if (r == 0) return mid ? a : left ? (a || f) : (a || b);
        if (r == 4) return mid ? g : left ? (g || f || e) : (g || b || cc);
        if (r == 8) return mid ? d : left ? (d || e) : (d || cc);
        if (r < 4) return left ? f : right && b;
        return left ? e : right && cc;
    }

    /** Pixel of "MM:SS"-style text at column x (0..CLOCK_W-1), row r from the top. */
    public static boolean clockPixel(String text, int x, int r) {
        // columns: d0 0-4, d1 6-10, colon 12, d2 14-18, d3 20-24
        if (text.length() < 5) return false;
        if (x == 12) return text.charAt(2) == ':' && (r == 2 || r == 3 || r == 5 || r == 6);
        int[] starts = {0, 6, 14, 20};
        int[] idx = {0, 1, 3, 4};
        for (int i = 0; i < 4; i++) {
            int c = x - starts[i];
            if (c >= 0 && c < 5) return seg(text.charAt(idx[i]), c, r);
        }
        return false;
    }

    /** "MM:SS" for a number of seconds (clamped 0..5999); negative = "--:--". */
    public static String clockText(int seconds) {
        if (seconds < 0) return "--:--";
        seconds = Math.min(5999, seconds);
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static final Map<Character, String[]> FONT = new HashMap<>();
    static {
        FONT.put('B', new String[]{"1111.", "1...1", "1...1", "1111.", "1...1", "1...1", "1111."});
        FONT.put('L', new String[]{"1....", "1....", "1....", "1....", "1....", "1....", "11111"});
        FONT.put('O', new String[]{".111.", "1...1", "1...1", "1...1", "1...1", "1...1", ".111."});
        FONT.put('C', new String[]{".1111", "1....", "1....", "1....", "1....", "1....", ".1111"});
        FONT.put('K', new String[]{"1...1", "1..1.", "1.1..", "11...", "1.1..", "1..1.", "1...1"});
        FONT.put('S', new String[]{".1111", "1....", "1....", ".111.", "....1", "....1", "1111."});
        FONT.put('H', new String[]{"1...1", "1...1", "1...1", "11111", "1...1", "1...1", "1...1"});
        FONT.put('W', new String[]{"1...1", "1...1", "1...1", "1.1.1", "1.1.1", "11.11", "1...1"});
        FONT.put('D', new String[]{"1111.", "1...1", "1...1", "1...1", "1...1", "1...1", "1111."});
        FONT.put('N', new String[]{"1...1", "11..1", "1.1.1", "1.1.1", "1..11", "1...1", "1...1"});
        FONT.put('E', new String[]{"11111", "1....", "1....", "1111.", "1....", "1....", "11111"});
        FONT.put('A', new String[]{".111.", "1...1", "1...1", "11111", "1...1", "1...1", "1...1"});
        FONT.put('M', new String[]{"1...1", "11.11", "1.1.1", "1.1.1", "1...1", "1...1", "1...1"});
        FONT.put('T', new String[]{"11111", "..1..", "..1..", "..1..", "..1..", "..1..", "..1.."});
        FONT.put('I', new String[]{"111", ".1.", ".1.", ".1.", ".1.", ".1.", "111"});
        FONT.put('R', new String[]{"1111.", "1...1", "1...1", "1111.", "1.1..", "1..1.", "1...1"});
        FONT.put('P', new String[]{"1111.", "1...1", "1...1", "1111.", "1....", "1....", "1...."});
        FONT.put('U', new String[]{"1...1", "1...1", "1...1", "1...1", "1...1", "1...1", ".111."});
        FONT.put('V', new String[]{"1...1", "1...1", "1...1", "1...1", ".1.1.", ".1.1.", "..1.."});
        FONT.put(' ', new String[]{"...", "...", "...", "...", "...", "...", "..."});
    }

    public static int textWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            String[] g = FONT.getOrDefault(s.charAt(i), FONT.get(' '));
            w += g[0].length() + (i < s.length() - 1 ? 1 : 0);
        }
        return w;
    }

    /** Calls sink(col, rowFromTop) for every lit pixel of the text in the 5x7 font. */
    public static void text(String s, PixelSink sink) {
        int x = 0;
        for (int i = 0; i < s.length(); i++) {
            String[] g = FONT.getOrDefault(s.charAt(i), FONT.get(' '));
            for (int r = 0; r < g.length; r++)
                for (int c = 0; c < g[r].length(); c++) if (g[r].charAt(c) == '1') sink.px(x + c, r);
            x += g[0].length() + 1;
        }
    }

    public interface PixelSink { void px(int col, int row); }
}
