package com.mcm.api;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a game hands the Map Edit core once it has pasted a map for editing ({@link McmMapEditable#buildForEdit}):
 * where the map is, how far edits are tracked, where to put the editor, and the things the game's own logic needs
 * (so the core can warn when an edit removes them). Coordinates called "local" are relative to {@link #originX},
 * {@link #originY}, {@link #originZ}: the same origin the game adds when it pastes an overlay
 * ({@link McmMapOverlay}).
 * @since the Map Edit core update
 */
public final class McmEditSpace {
    /** What the game needs at a marker cell. */
    public enum Kind {
        /** The feet cell and the one above must not be solid (spawn points, chicken spawns). */
        FREE,
        /** Like FREE, and the block under the feet cell must be solid (a spawn that stands on something). */
        STAND,
        /** The block must stay (not air): a structure the game's logic relies on. */
        SOLID
    }

    public record Marker(String label, int lx, int ly, int lz, Kind kind) {}

    public final World world;
    public final int originX, originY, originZ;
    /** Inclusive local box in which edits are tracked (the map plus room to build around it). */
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    /** Where the editor first appears (world coordinates). */
    public final Location spawn;
    /** Named teleport targets inside the map (world coordinates), e.g. Parkour Warrior stages. In insertion order. */
    public final Map<String, Location> anchors = new LinkedHashMap<>();
    /** Things the game's logic needs; checked on save. */
    public final List<Marker> markers = new ArrayList<>();
    /** Extra lines shown to the editor on entering (what is game-managed, what is not saved...). */
    public final List<String> notes = new ArrayList<>();
    /** Free slot for the game's own bookkeeping (an edit slot number, an arena index). */
    public Object token;

    public McmEditSpace(World world, int originX, int originY, int originZ,
                        int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Location spawn) {
        this.world = world;
        this.originX = originX; this.originY = originY; this.originZ = originZ;
        this.minX = Math.min(minX, maxX); this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY); this.maxY = Math.max(minY, maxY);
        this.minZ = Math.min(minZ, maxZ); this.maxZ = Math.max(minZ, maxZ);
        this.spawn = spawn;
    }

    public McmEditSpace anchor(String id, Location at) { anchors.put(id, at); return this; }

    public McmEditSpace marker(String label, int lx, int ly, int lz, Kind kind) {
        markers.add(new Marker(label, lx, ly, lz, kind));
        return this;
    }

    public McmEditSpace note(String line) { notes.add(line); return this; }
}
