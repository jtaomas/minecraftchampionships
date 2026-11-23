package com.mcm.games.findthebutton.map;

/**
 * One hand-placed hiding spot: the cell the button occupies (map-local coordinates) and the direction from that
 * cell to the block the button is attached to. Pure Java.
 */
public record Spot(String name, String difficulty, int x, int y, int z, Dir support) {

    public enum Dir {
        DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);
        public final int dx, dy, dz;
        Dir(int dx, int dy, int dz) { this.dx = dx; this.dy = dy; this.dz = dz; }
    }

    /** Position of the supporting block. */
    public int sx() { return x + support.dx; }
    public int sy() { return y + support.dy; }
    public int sz() { return z + support.dz; }

    /** Vanilla block state of the stone button for this spot (not powered). */
    public String blockState() {
        String face, facing;
        switch (support) {
            case DOWN -> { face = "floor"; facing = "north"; }
            case UP -> { face = "ceiling"; facing = "north"; }
            case NORTH -> { face = "wall"; facing = "south"; }
            case SOUTH -> { face = "wall"; facing = "north"; }
            case WEST -> { face = "wall"; facing = "east"; }
            default -> { face = "wall"; facing = "west"; }
        }
        return "minecraft:stone_button[face=" + face + ",facing=" + facing + ",powered=false]";
    }

    /** Centre of the button's hit box, in map-local block coordinates (x, y, z). */
    public double[] centre() {
        double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;
        final double t = 1.0 / 16.0;   // buttons are 2/16 thick: centre 1/16 off the support face
        switch (support) {
            case DOWN -> cy = y + t;
            case UP -> cy = y + 1 - t;
            case NORTH -> cz = z + t;
            case SOUTH -> cz = z + 1 - t;
            case WEST -> cx = x + t;
            case EAST -> cx = x + 1 - t;
        }
        return new double[]{cx, cy, cz};
    }
}
