package com.mcm;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/**
 * The MCC centre-screen "accomplishment graphic" (crossed swords + dark box with a red title and the victim line), built as
 * ONE subtitle component in the pack fonts {@code mcc:callout}, {@code mcc:callout_t1} and {@code mcc:callout_t2}
 * (drawn by {@code _candidates/pack/callout_pack.py}; the numbers below must match that file).
 *
 * <p>Everything is placed with negative-space fonts: the subtitle is 2x scaled, so one font unit is 2 GUI px and all positions
 * here are in GUI px (half units). The string starts at x = 0 and its total advance is brought back to 0, so Minecraft centres
 * it exactly on the crosshair.
 */
final class Callout {
    private Callout() {}

    static final Key GRAPHICS = Key.key("mcc", "callout");
    static final Key LINE1 = Key.key("mcc", "callout_t1");
    static final Key LINE2 = Key.key("mcc", "callout_t2");

    static final char SINGLE = '\uE000', DOUBLE = '\uE001', TRIPLE = '\uE002', FREEZE = '\uE003', LAP = '\uE004', ROOM = '\uE005',
            OBJECTIVE = '\uE006';
    private static final char SILHOUETTE = '\uE020', DOT = '\uE010', STAR = '\uE011', RING = '\uE012', BOX = '\uE030';
    private static final char TEAM_BASE = '\uE900', NO_TEAM = '\uE90A';

    /** Glyph advances in GUI px (bitmap advance = int(0.5 + cellWidth * height / cellHeight) + 1 font units, x2). */
    private static final int ICON_W = 72, ICON_ADV = 74, BOX_W = 114, BOX_ADV = 116, TEAM_ADV = 20;
    /** Widest line 2 that still sits inside the box. */
    private static final int MAX_LINE = 104;

    static final TextColor RED = TextColor.color(0xFF5555), CYAN = TextColor.color(0x55FFFF), GREEN = TextColor.color(0x55FF55);

    /** What a banner shows. {@code team}: team index for the team icon, -1 = head only, -2 = no icon (objectives). */
    record Spec(char icon, TextColor titleColor, String title, String line2, int team) {
        /** Plain-text version for players without the pack. */
        Component plain() {
            return Component.text(title, titleColor).append(Component.text(" ")).append(Component.text(line2, NamedTextColor.WHITE));
        }
    }

    /** Frames 0-5 are the pop-in (dot, star, ring burst, white silhouette, icon, icon), 6 is the full banner. */
    static final int FULL = 6;

    /** The banner at {@code stage}; {@code withText} false gives the box + icon only (the last 0.13 s). */
    static Component frame(Spec s, int stage, boolean withText) {
        Pen pen = new Pen();
        char slot = switch (stage) {
            case 0 -> DOT;
            case 1 -> STAR;
            case 2 -> RING;
            case 3 -> (char) (SILHOUETTE + (s.icon - SINGLE));
            default -> s.icon;
        };
        pen.moveTo(-ICON_W / 2);
        pen.glyph(GRAPHICS, slot, ICON_ADV);
        if (stage >= 1) {
            pen.moveTo(-BOX_W / 2);
            pen.glyph(GRAPHICS, BOX, BOX_ADV);
        }
        if (withText && stage >= FULL) {
            String t = s.title.toUpperCase(Locale.ROOT);
            int w1 = McmHud.hudWidth(t);
            pen.moveTo(-w1 / 2);
            pen.text(LINE1, t, s.titleColor, w1);
            String name = fit(s.line2.toUpperCase(Locale.ROOT), s.team == -2 ? MAX_LINE : MAX_LINE - TEAM_ADV);
            int w2 = McmHud.hudWidth(name) + (s.team == -2 ? 0 : TEAM_ADV);
            pen.moveTo(-w2 / 2);
            if (s.team != -2) pen.glyph(LINE2, s.team >= 0 ? (char) (TEAM_BASE + s.team) : NO_TEAM, TEAM_ADV);
            pen.text(LINE2, name, NamedTextColor.WHITE, McmHud.hudWidth(name));
        }
        pen.moveTo(0);
        return Component.empty().children(pen.parts);
    }

    private static String fit(String s, int max) {
        if (McmHud.hudWidth(s) <= max) return s;
        String cut = s;
        while (cut.length() > 1 && McmHud.hudWidth(cut + "...") > max) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }

    /** Space chars of the fonts: U+E800+k = +2^k px, U+E820+k = -2^k px (the pack declares 0.5 * 2^k font units each). */
    private static final class Pen {
        final java.util.List<Component> parts = new java.util.ArrayList<>();
        int x;

        void moveTo(int target) {
            int d = target - x;
            if (d == 0) return;
            int base = d > 0 ? 0xE800 : 0xE820;
            int n = Math.abs(d);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 11; k++) if ((n & (1 << k)) != 0) sb.append((char) (base + k));
            parts.add(Component.text(sb.toString(), Style.style().font(GRAPHICS).build()));
            x = target;
        }

        void glyph(Key font, char c, int advance) {
            parts.add(Component.text(String.valueOf(c), Style.style(NamedTextColor.WHITE).font(font)));
            x += advance;
        }

        void text(Key font, String s, TextColor color, int width) {
            parts.add(Component.text(s, Style.style(color).font(font)));
            x += width;
        }
    }
}
