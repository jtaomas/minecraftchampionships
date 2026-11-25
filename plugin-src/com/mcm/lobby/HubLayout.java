package com.mcm.lobby;

import java.util.ArrayList;
import java.util.List;

/** Key coordinates of the generated hub (pure data, filled in by {@link HubGenerator}). World coordinates. */
public final class HubLayout {
    public static final int SEA = 62;          // top water block
    public static final int PAD_COUNT = 12;     // legacy max; the live dial is 8 (see dialCapacity)

    /** Pad accent kinds (recoloured at runtime). */
    public static final int K_CONCRETE = 0, K_GLASS = 1, K_WOOL = 2, K_LIGHT = 3, K_CARPET = 4, K_PANE = 5, K_TERRACOTTA = 6;
    /** Keep the built block (copper frames, dark boards); never dyed. */
    public static final int K_KEEP = 7;
    /** A permanent dark inlay/chevron; never dyed. */
    public static final int K_DARK = 8;

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

    // ---- Decision Dome v2 (Nocturne Orrery)
    /** Live wedge count: 8 (45 deg) or 6 (60 deg). */
    public int dialCapacity = 8;
    /** Bumped with the generator so the provider can invalidate caches. */
    public int dialGeneration = 1;
    /** Outer radius of the live wedge floor. */
    public double dialRadius = 12.0;
    /** Inner radius of the live wedge floor (chickens closer to the centre than this do not vote). */
    public double dialInner = 7.5;
    /** Block y of the repaintable wedge band cells in {@link #dialBand} (v6: the top course of the dial rim). */
    public int dialBandY = Integer.MIN_VALUE;
    /** Dial centre (x, standing y, z). */
    public double[] dialCentre = new double[3];
    /** Wedge centre / label anchor / accent cells, 8-game. */
    public final double[][] wedge8 = new double[8][4];       // x, y, z, yaw
    public final double[][] wedge6 = new double[6][4];
    public final double[][] wedgeLabels8 = new double[8][4]; // text anchor x,y,z,yaw
    public final double[][] wedgeLabels6 = new double[6][4];
    @SuppressWarnings("unchecked")
    public final List<int[]>[] wedgeAccents8 = new List[8];  // {x,y,z,kind}
    @SuppressWarnings("unchecked")
    public final List<int[]>[] wedgeAccents6 = new List[6];
    /** Shared outer band cells {x,z}; the band is repainted for 8 or 6 wedges. */
    public final List<int[]> dialBand = new ArrayList<>();

    /** Ten team pods (x standing, y, z, yaw facing the dial). */
    public final double[][] pods = new double[10][4];
    @SuppressWarnings("unchecked")
    public final List<int[]>[] podAccents = new List[10];    // {x,y,z,kind}
    @SuppressWarnings("unchecked")
    public final List<int[]>[] podGates = new List[10];      // closed gate blocks
    @SuppressWarnings("unchecked")
    public final List<int[]>[] podBars = new List[10];       // cage bars, air normally

    /** Divider teeth above the floor: {x,y,z}; the seam itself is permanent. */
    @SuppressWarnings("unchecked")
    public final List<int[]>[] divider8 = new List[8];
    @SuppressWarnings("unchecked")
    public final List<int[]>[] divider6 = new List[6];
    /** Floor seam inlays {x,z} for each dial mode; repainted with the band. */
    public final List<int[]> dividerSeam8 = new ArrayList<>();
    public final List<int[]> dividerSeam6 = new ArrayList<>();

    /** Four reveal face anchors around the central orrery. */
    public final double[][] revealSurfaces = new double[4][4]; // x,y,z,yaw
    /** Interior clock ring anchor (also the timer text position). */
    public final double[] clock = new double[3];

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

    public HubLayout() {
        for (int i = 0; i < PAD_COUNT; i++) padAccents[i] = new ArrayList<>();
        for (int i = 0; i < 8; i++) { wedgeAccents8[i] = new ArrayList<>(); divider8[i] = new ArrayList<>(); }
        for (int i = 0; i < 6; i++) { wedgeAccents6[i] = new ArrayList<>(); divider6[i] = new ArrayList<>(); }
        for (int i = 0; i < 10; i++) { podAccents[i] = new ArrayList<>(); podGates[i] = new ArrayList<>(); podBars[i] = new ArrayList<>(); }
    }

    public void label(double x, double y, double z, double yaw, String text) {
        labels.add(new double[]{x, y, z, yaw});
        labelText.add(text);
    }
}
