package com.mcm.games.findthebutton.map;

import java.util.List;

/** All Find the Button maps. */
public final class Maps {
    private Maps() {}

    public static List<FtbMap> all() {
        return List.of(new LibraryMap(), new KitchenMap(), new ShedMap(), new MineshaftMap(), new ToyRoomMap(), new ShipCabinMap());
    }
}
