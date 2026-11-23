package com.mcm.games.gridrunners;

import com.mcm.games.gridrunners.rooms.Room;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One team's copy of the course. */
public final class Lane {
    public final int index;
    public final int x0;
    public final int z0;
    public final DyeColor color;
    public final String label;
    public final List<String> sideKeys = new ArrayList<>();
    public final List<UUID> members = new ArrayList<>();
    public final Room[] rooms = new Room[9];

    public int cleared;
    public int skipped;
    public boolean finalOpen;
    public boolean finished;
    public int place;
    public int finishSeconds;

    public Lane(int index, DyeColor color, String label) {
        this.index = index;
        this.x0 = index * GridRunnersGame.LANE_SPACING;
        this.z0 = 0;
        this.color = color;
        this.label = label;
    }

    public int done() { return cleared + skipped; }

    public boolean allChallengesDone() {
        for (int i = 0; i < 8; i++) {
            Room r = rooms[i];
            if (r.state != Room.State.DONE && r.state != Room.State.SKIPPED) return false;
        }
        return true;
    }

    public Room room(int col, int row) {
        if (col < 0 || col > 2 || row < 0 || row > 2) return null;
        return rooms[row * 3 + col];
    }

    /** The room whose interior contains this location, or null (walls, doorways, hall, outside). */
    public Room roomAt(Location l) {
        if (l == null) return null;
        int dx = l.getBlockX() - x0;
        int dz = l.getBlockZ() - z0;
        int dy = l.getBlockY() - GridRunnersGame.Y0;
        if (dx < 1 || dx > 44 || dz < 1 || dz > 44 || dy < 0 || dy > 9) return null;
        if (dx % 15 == 0 || dz % 15 == 0) return null;
        return rooms[(dz / 15) * 3 + (dx / 15)];
    }

    public Location spawn(World w) {
        Location l = new Location(w, x0 - 4.5, GridRunnersGame.Y0 + 1, z0 + 8.0);
        l.setYaw(-90f);
        return l;
    }

    public Location overview(World w) {
        Location l = new Location(w, x0 + 23, GridRunnersGame.Y0 + 20, z0 + 23);
        l.setPitch(90f);
        return l;
    }
}
