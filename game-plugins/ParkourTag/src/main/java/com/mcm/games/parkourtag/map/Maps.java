package com.mcm.games.parkourtag.map;

import java.util.List;

/** Every MCC Parkour Tag map recreated so far, in build order. A game picks one finished map at random. */
public final class Maps {
    private Maps() {}

    public static List<PtMap> all() {
        return List.of(new DustbowlMap());
    }
}
