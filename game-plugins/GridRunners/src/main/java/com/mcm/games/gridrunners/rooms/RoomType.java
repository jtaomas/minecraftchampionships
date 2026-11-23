package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;

public enum RoomType {
    MOB_PIT, PLATES, PARKOUR, SEARCH, LAMPS, SHEEP, HOPPER, MAZE, LEVERS, DEFEND, FINISH;

    public Room create(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        return switch (this) {
            case MOB_PIT -> new MobPitRoom(g, lane, col, row, seed);
            case PLATES -> new PlatesRoom(g, lane, col, row, seed);
            case PARKOUR -> new ParkourRoom(g, lane, col, row, seed);
            case SEARCH -> new SearchRoom(g, lane, col, row, seed);
            case LAMPS -> new LampGridRoom(g, lane, col, row, seed);
            case SHEEP -> new SheepRoom(g, lane, col, row, seed);
            case HOPPER -> new HopperRoom(g, lane, col, row, seed);
            case MAZE -> new MazeRoom(g, lane, col, row, seed);
            case LEVERS -> new LeverCodeRoom(g, lane, col, row, seed);
            case DEFEND -> new DefendRoom(g, lane, col, row, seed);
            case FINISH -> new FinishRoom(g, lane, col, row, seed);
        };
    }
}
