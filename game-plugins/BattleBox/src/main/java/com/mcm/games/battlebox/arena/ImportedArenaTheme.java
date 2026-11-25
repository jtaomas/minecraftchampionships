package com.mcm.games.battlebox.arena;

/**
 * Wraps an {@link ImportedBbArena} as a first-class {@link ArenaTheme} so it works seamlessly with
 * {@code ArenaPlacer}, {@code Match}, {@code BattleBoxGame}, and {@code GameListener}.
 */
public final class ImportedArenaTheme extends ArenaTheme {
    private static final int[][] SPAWN_OFFSETS = {
            {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
    };

    private final ImportedBbArena arena;

    public ImportedArenaTheme(ImportedBbArena arena) {
        this.arena = arena;
    }

    public ImportedBbArena arena() { return arena; }

    @Override public boolean isImported() { return true; }
    @Override public String id() { return arena.id; }
    @Override public String displayName() { return arena.name; }
    @Override public String tagline() { return "Imported arena (" + arena.sizeX + "x" + arena.sizeY + "x" + arena.sizeZ + ")"; }
    @Override public String layoutNotes() {
        return "Imported arena " + arena.name + " (" + arena.blockCount + " blocks, centre at "
                + arena.centreX + "," + arena.centreY + "," + arena.centreZ + ").";
    }

    @Override public int centreX() { return arena.centreX; }
    @Override public int centreY(ArenaGeo geo) { return arena.centreY; }
    @Override public int centreZ() { return arena.centreZ; }

    @Override
    public boolean isCentreCell(ArenaGeo geo, int x, int y, int z) {
        return y == arena.centreY && Math.abs(x - arena.centreX) <= 1 && Math.abs(z - arena.centreZ) <= 1;
    }

    @Override
    public double[] spawn(ArenaGeo geo, boolean sideB, int i) {
        int sx = sideB ? arena.spawnBX : arena.spawnAX;
        int sy = sideB ? arena.spawnBY : arena.spawnAY;
        int sz = sideB ? arena.spawnBZ : arena.spawnAZ;
        int[] off = SPAWN_OFFSETS[Math.floorMod(i, SPAWN_OFFSETS.length)];
        double px = sx + off[0] + 0.5;
        double pz = sz + off[1] + 0.5;
        double yaw = Math.toDegrees(Math.atan2(-(arena.centreX - sx), (arena.centreZ - sz)));
        return new double[]{px, sy, pz, yaw};
    }

    @Override
    public double[] overview(ArenaGeo geo, boolean sideB, int i) {
        double dx = (Math.floorMod(i, 3) - 1) * 3;
        int sx = sideB ? arena.spawnBX : arena.spawnAX;
        int sz = sideB ? arena.spawnBZ : arena.spawnAZ;
        double ox = (sx + arena.centreX) / 2.0 + dx + 0.5;
        double oz = (sz + arena.centreZ) / 2.0 + 0.5;
        double oy = Math.max(arena.maxY - 2, arena.centreY + 12);
        double ddx = arena.centreX + 0.5 - ox;
        double ddz = arena.centreZ + 0.5 - oz;
        double dist = Math.max(1.0, Math.hypot(ddx, ddz));
        double yaw = Math.toDegrees(Math.atan2(-ddx, ddz));
        double pitch = Math.toDegrees(Math.atan2(oy + 1.62 - arena.centreY, dist));
        return new double[]{ox, oy, oz, yaw, pitch};
    }

    @Override
    public boolean inSpectatorBox(ArenaGeo geo, double x, double y, double z) {
        return x >= arena.minX - 4 && x <= arena.maxX + 5
                && z >= arena.minZ - 4 && z <= arena.maxZ + 5
                && y >= arena.minY - 4 && y <= arena.maxY + 14;
    }

    @Override public int voidY() { return arena.minY - 8; }

    @Override
    public double[] previewLocation(ArenaGeo geo) {
        double[] sp = spawn(geo, false, 0);
        return new double[]{sp[0], sp[1], sp[2], sp[3], 5.0};
    }

    @Override
    public int[] minBounds(ArenaGeo geo) {
        return new int[]{
                Math.min(arena.minX, Math.min(arena.centreX - 1, Math.min(arena.spawnAX - 1, arena.spawnBX - 1))),
                Math.min(arena.minY, Math.min(arena.centreY - 1, Math.min(arena.spawnAY - 1, arena.spawnBY - 1))),
                Math.min(arena.minZ, Math.min(arena.centreZ - 1, Math.min(arena.spawnAZ - 1, arena.spawnBZ - 1)))
        };
    }

    @Override
    public int[] maxBounds(ArenaGeo geo) {
        return new int[]{
                Math.max(arena.maxX, Math.max(arena.centreX + 1, Math.max(arena.spawnAX + 1, arena.spawnBX + 1))),
                Math.max(arena.maxY, Math.max(arena.centreY + 3, Math.max(arena.spawnAY + 2, arena.spawnBY + 2))),
                Math.max(arena.maxZ, Math.max(arena.centreZ + 1, Math.max(arena.spawnAZ + 1, arena.spawnBZ + 1)))
        };
    }

    @Override
    public Grid generate(ArenaGeo geo, long seed, String teamA, String teamB, String neutralCentre) {
        String neutral = (neutralCentre == null || neutralCentre.isBlank()) ? "white_concrete" : neutralCentre;
        String ta = (teamA == null || teamA.isBlank()) ? "red" : teamA;
        String tb = (teamB == null || teamB.isBlank()) ? "blue" : teamB;

        int[] mn = minBounds(geo);
        int[] mx = maxBounds(geo);
        Grid grid = new Grid(mn[0], mn[1], mn[2], mx[0], mx[1], mx[2]);
        arena.writeTo(grid);

        // Ensure a clean 3x3 neutral centre pad at (cx + dx, cy, cz + dz) with 3 air blocks above
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = arena.centreX + dx;
                int z = arena.centreZ + dz;
                if (Shapes.standTop(grid.get(x, arena.centreY - 1, z)) < 0.49) {
                    grid.set(x, arena.centreY - 1, z, "sea_lantern");
                }
                grid.set(x, arena.centreY, z, neutral);
                for (int dy = 1; dy <= 3; dy++) {
                    grid.set(x, arena.centreY + dy, z, null);
                }
            }
        }

        // Ensure spawn_a and spawn_b sit on solid 3x3 pads with 3-block air headroom
        prepareSpawn(grid, arena.spawnAX, arena.spawnAY, arena.spawnAZ, ta);
        prepareSpawn(grid, arena.spawnBX, arena.spawnBY, arena.spawnBZ, tb);

        return grid;
    }

    private static void prepareSpawn(Grid grid, int sx, int sy, int sz, String team) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = sx + dx;
                int z = sz + dz;
                if (Shapes.standTop(grid.get(x, sy - 1, z)) < 0.49) {
                    grid.set(x, sy - 1, z, team + "_concrete");
                }
                for (int dy = 0; dy <= 2; dy++) {
                    grid.set(x, sy + dy, z, null);
                }
            }
        }
    }

    // Unused template hooks (generate is overridden completely)
    @Override protected String floorAt(int x, int z) { return null; }
    @Override protected String wallAt(Face f, int u, int y) { return null; }
    @Override protected String ceilingAt(int x, int z) { return null; }
    @Override protected String[] roomPalette() { return new String[0]; }
    @Override protected void layout() {}
}
