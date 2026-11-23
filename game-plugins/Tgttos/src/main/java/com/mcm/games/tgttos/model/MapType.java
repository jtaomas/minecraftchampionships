package com.mcm.games.tgttos.model;

/** Crossing type of a TGTTOS map. Informational for HUD/README; the kit is chosen by {@link TgMap#kit()}. */
public enum MapType {
    BRIDGE, CLIMB, PARKOUR, DROP, ELYTRA, BOAT, DOORS, PUSH_BRIDGE;

    /** Display word for title cards. */
    public String label() {
        return switch (this) {
            case BRIDGE -> "Bridging";
            case CLIMB -> "Climb";
            case PARKOUR -> "Parkour";
            case DROP -> "Drop";
            case ELYTRA -> "Glide";
            case BOAT -> "Boats";
            case DOORS -> "Doors";
            case PUSH_BRIDGE -> "Push & Bridge";
        };
    }
}
