package com.mcm.games.meltdown;

/** One room of the facility grid. Interior is 15x15 blocks between wall lines. */
public final class Room {
    public enum State { SAFE, WARNING, FROZEN }

    public final int i;
    public final int j;
    public final int ring;
    public final String label;
    public State state = State.SAFE;
    /** Elapsed game second at which the warning starts / the room becomes deadly. */
    public int warnAt;
    public int freezeAt;
    /** Elapsed game second at which the room actually froze (valid when state == FROZEN). */
    public int frozenSince = -1;

    Room(int i, int j, int ring) {
        this.i = i;
        this.j = j;
        this.ring = ring;
        this.label = String.valueOf((char) ('A' + i)) + (j + 1);
    }

    /** First interior x/z coordinate (inclusive). */
    public int minX() { return Arena.OX + i * Arena.PITCH + 1; }
    public int minZ() { return Arena.OZ + j * Arena.PITCH + 1; }
    public int maxX() { return minX() + Arena.PITCH - 2; }
    public int maxZ() { return minZ() + Arena.PITCH - 2; }
    public int centerX() { return Arena.OX + i * Arena.PITCH + Arena.PITCH / 2; }
    public int centerZ() { return Arena.OZ + j * Arena.PITCH + Arena.PITCH / 2; }

    public static String ringName(int ring) {
        return switch (ring) {
            case 0 -> "Core";
            case 1 -> "Inner Ring";
            case 2 -> "Middle Ring";
            default -> "Outer Ring";
        };
    }
}
