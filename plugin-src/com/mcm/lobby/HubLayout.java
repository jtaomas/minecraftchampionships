package com.mcm.lobby;

import java.util.ArrayList;
import java.util.List;

/** Key coordinates of the generated hub (pure data, filled in by {@link HubGenerator}). World coordinates. */
public final class HubLayout {
    public static final int SEA = 62;          // top water block
    public static final int PAD_COUNT = 12;

    /** Pad accent kinds (recoloured at runtime). */
    public static final int K_CONCRETE = 0, K_GLASS = 1, K_WOOL = 2, K_LIGHT = 3, K_CARPET = 4, K_PANE = 5, K_TERRACOTTA = 6;

    // ---- spawn / plaza
    public int spawnX, spawnY, spawnZ;          // spawn feet position (block coords); floor is spawnY-1
    public float spawnYaw = 180f;
    public double[] leaderboard = new double[4]; // x, y, z (text centre), yaw the text faces
    public final List<double[]> teamPads = new ArrayList<>(); // x, y(standing), z per team (TeamScoring order)
    public int teamPadRadius = 1;

    // ---- venue
    public int domeX, domeZ;                   // dome centre
    public int stageY;                          // stage floor block y (players stand at stageY+1)
    public int domeRadius;                      // drum outer radius
    public int innerRadius;                     // usable interior radius (stands back wall)
    public double[] entrance = new double[4];  // x, y, z, yaw (outside the doors, facing in)
    public double[] interiorSpawn = new double[4]; // where voters land (on the stage, facing the podium)
    public final double[][] pads = new double[PAD_COUNT][3]; // standing centre of each voting pad
    public double padRadius;
    @SuppressWarnings("unchecked")
    public final List<int[]>[] padAccents = new List[PAD_COUNT]; // {x,y,z,kind}
    public double[] podium = new double[3];    // top centre of the reveal podium (standing position)
    public int podiumBaseY;                     // block y of the podium's top surface
    public double[] timer = new double[3];     // interior timer text display position
    public final List<double[]> seats = new ArrayList<>(); // spectator standing spots in the stands (x,y,z,yaw)

    // ---- countdown display on the facade: digits drawn in the plane z = clockZ, columns increase +X
    public int clockX0, clockY0, clockZ;       // bottom-left pixel of the digit area
    public String clockOn = "minecraft:sea_lantern", clockOff = "minecraft:black_concrete", clockDim = "minecraft:gray_concrete";

    // ---- ambience
    public final List<double[]> searchlights = new ArrayList<>(); // x, y, z, phase
    public final List<double[]> fireworkSpots = new ArrayList<>(); // x, y, z (burst centre)
    public final List<double[]> labels = new ArrayList<>();       // decorative floating labels: x,y,z,yaw + text in labelText
    public final List<String> labelText = new ArrayList<>();

    // ---- extents
    public int minX, maxX, minZ, maxZ, minY, maxY;

    public HubLayout() { for (int i = 0; i < PAD_COUNT; i++) padAccents[i] = new ArrayList<>(); }

    public void label(double x, double y, double z, double yaw, String text) {
        labels.add(new double[]{x, y, z, yaw});
        labelText.add(text);
    }
}
