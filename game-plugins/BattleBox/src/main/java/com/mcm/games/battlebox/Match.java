package com.mcm.games.battlebox;

import org.bukkit.DyeColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One side-vs-side match in one arena copy for one round. */
final class Match {
    final int arena;
    final Side a;
    final Side b;
    final DyeColor dyeA;
    final DyeColor dyeB;
    /** Colour of the neutral blocks the centre starts filled with (white unless a side is white). */
    final DyeColor neutral;
    final Set<UUID> alive = new HashSet<>();
    boolean done;
    Side winner;      // null with done = draw / no result
    String result = "";

    Match(int arena, Side a, Side b) {
        this.arena = arena;
        this.a = a;
        this.b = b;
        this.dyeA = a.dye;
        DyeColor db = b.dye;
        if (db == dyeA) {
            // two sides share a colour (more squads than colours): pick another for this match
            for (DyeColor c : new DyeColor[]{DyeColor.BLACK, DyeColor.BROWN, DyeColor.MAGENTA, DyeColor.GRAY,
                    DyeColor.LIGHT_GRAY, DyeColor.RED, DyeColor.BLUE}) {
                if (c != dyeA) { db = c; break; }
            }
        }
        this.dyeB = db;
        DyeColor n = DyeColor.WHITE;
        for (DyeColor c : new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY}) {
            if (c != dyeA && c != dyeB) { n = c; break; }
        }
        this.neutral = n;
    }

    boolean contains(UUID id) { return a.members.contains(id) || b.members.contains(id); }

    Side sideOf(UUID id) {
        if (a.members.contains(id)) return a;
        if (b.members.contains(id)) return b;
        return null;
    }

    Side opponent(Side s) { return s == a ? b : a; }

    DyeColor dyeOf(Side s) { return s == a ? dyeA : dyeB; }

    boolean isB(Side s) { return s == b; }

    int aliveOf(Side s) {
        int c = 0;
        for (UUID u : alive) if (s.members.contains(u)) c++;
        return c;
    }
}
