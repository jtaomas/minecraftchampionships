package com.mcm.games.dodgebolt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;

import java.util.*;

/**
 * The Dodgebolt stadium, recreated from MCC's Season 6 "pool" arena (research: dodgebolt/db-1, render_s6_current.png,
 * 032137 / 032147 / 032201 / 024533 frames).
 *
 * Layout (block coordinates; deck blocks at y = FLOOR_Y, players stand at FLOOR_Y + 1):
 *  - Deck: x -12..12. Side A z -17..-1, side B z 1..17, the black centre line at z = 0. White deck with a team-coloured
 *    pixel pattern and one black/white 7x7 target pad per side (|x| <= 3, |z| 3..9), where the arrows drop.
 *  - Lava one block below the whole deck. Shrinking removes the deck ring by ring, so the lava ring grows.
 *  - Pool walls at |x| = 13 / |z| = 18: grey stone below deck level, team colour above, a white band on top,
 *    a quartz pillar where the centre line meets the wall. Invisible barrier walls and ceiling keep arrows in.
 *  - Spectator ring: a stone-brick walkway at rim level (|x| <= 17, |z| <= 22), four tiers of spruce seating,
 *    then a high outer wall carrying the physical "0 0" scoreboards, team crowns and big team banners.
 * Symmetric: block z mirrors to -z (side A is negative z).
 */
public final class Arena {
    public static final int FLOOR_Y = 64;
    public static final int HALF_X = 12;          // deck |x| <= 12
    public static final int BACK_Z = 17;          // deck |z| <= 17
    public static final int WALL_X = 13, WALL_Z = 18;
    public static final int RIM_Y = FLOOR_Y + 4;  // walkway top block (spectators stand at RIM_Y + 1)
    public static final int WALK_X = 17, WALK_Z = 22;
    public static final int TIERS = 4;
    public static final int OUTER_TOP = RIM_Y + 12;
    public static final int CEILING_Y = FLOOR_Y + 14;
    /** The deck never shrinks by more than this many rings (MCC 6+: "stops after 8 blocks total"). */
    public static final int MAX_SHRINK = 8;
    public static final int PAD_Z = 6;            // target pad centre |z|
    /** Side boundary in real coordinates: side A stays at z <= A_EDGE, side B at z >= B_EDGE (the centre line is between). */
    public static final double A_EDGE = 0.0, B_EDGE = 1.0;

    private final World world;
    private final Random random = new Random();
    private DyeColor dyeA = DyeColor.RED, dyeB = DyeColor.BLUE;
    private int shrunk;

    public Arena(World world) {
        this.world = world;
    }

    public World world() { return world; }

    // ------------------------------------------------------------------ geometry

    /** 0 = side A (negative z), 1 = side B. The centre line (z 0..1) counts as B; callers handle it. */
    public static int sideOfZ(double z) { return z < 0.5 ? 0 : 1; }

    public static boolean onCentreLine(double z) { return z >= 0.0 && z < 1.0; }

    public static boolean isDeck(int x, int z) { return Math.abs(x) <= HALF_X && Math.abs(z) <= BACK_Z; }

    /** Distance of a deck block from the outer edge: 0 = outermost ring. The centre line edge never shrinks. */
    public static int ringOf(int x, int z) {
        return Math.min(HALF_X - Math.abs(x), BACK_Z - Math.abs(z));
    }

    /** Inside the pool (below the barrier ceiling, inside the pool walls). */
    public static boolean inPool(Location l) {
        return Math.abs(l.getX() - 0.5) < WALL_X + 0.5 && Math.abs(l.getZ() - 0.5) < WALL_Z + 0.5
                && l.getY() > FLOOR_Y - 6 && l.getY() < CEILING_Y + 1;
    }

    /** Inside the whole stadium (spectators are kept in here). */
    public static boolean inStadium(Location l) {
        return Math.abs(l.getX() - 0.5) < WALK_X + TIERS + 1 && Math.abs(l.getZ() - 0.5) < WALK_Z + TIERS + 1
                && l.getY() > RIM_Y - 1 && l.getY() < OUTER_TOP + 30;
    }

    public int shrunk() { return shrunk; }

    /** Starting spot of player index of count on a side: the back of their half, facing the centre line. */
    public Location spawn(int side, int index, int count) {
        int n = Math.max(1, Math.min(count, 12));
        double x = -9 + 18.0 * ((index % n) + 0.5) / n;
        int bz = 14 + (index / 12) % 2;
        Location l = new Location(world, Math.floor(x) + 0.5, FLOOR_Y + 1, (side == 0 ? -bz : bz) + 0.5);
        l.setYaw(side == 0 ? 0 : 180);
        return l;
    }

    /** Where an arrow drops in for a side: just above the middle of that side's target pad. */
    public Location arrowSpot(int side) {
        int z = side == 0 ? -PAD_Z : PAD_Z;
        return new Location(world, 0.5, FLOOR_Y + 2.5, z + 0.5);
    }

    /** A seat in the stands (crowd players stand here), facing the pool. */
    public Location seat(int i) {
        Random r = new Random(i * 7919L + 13);
        int tier = 1 + r.nextInt(TIERS);
        boolean longSide = r.nextInt(3) != 0;
        int x, z;
        if (longSide) {
            x = (r.nextBoolean() ? 1 : -1) * (WALK_X + tier);
            z = -WALK_Z + 2 + r.nextInt(2 * WALK_Z - 3);
        } else {
            z = (r.nextBoolean() ? 1 : -1) * (WALK_Z + tier);
            x = -WALK_X + 2 + r.nextInt(2 * WALK_X - 3);
        }
        return facePool(new Location(world, x + 0.5, RIM_Y + tier + 1, z + 0.5));
    }

    /** A rim spot behind a side's half for eliminated finalists. */
    public Location rimSpot(int side, int i) {
        int x = -10 + Math.floorMod(i * 5, 21);
        int z = side == 0 ? -(WALL_Z + 2) : WALL_Z + 2;
        return facePool(new Location(world, x + 0.5, RIM_Y + 1, z + 0.5));
    }

    private static Location facePool(Location l) {
        double dx = 0.5 - l.getX(), dz = 0.5 - l.getZ();
        l.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        l.setPitch(25f);
        return l;
    }

    // ------------------------------------------------------------------ building

    public void setDyes(DyeColor a, DyeColor b) { dyeA = a; dyeB = b; }

    public DyeColor dye(int side) { return side == 0 ? dyeA : dyeB; }

    static Material mat(DyeColor d, String suffix) {
        Material m = Material.matchMaterial(d.name() + "_" + suffix);
        return m == null ? Material.matchMaterial("WHITE_" + suffix) : m;
    }

    private void set(int x, int y, int z, Material m) {
        world.getBlockAt(x, y, z).setType(m, false);
    }

    private void set(int x, int y, int z, BlockData d) {
        world.getBlockAt(x, y, z).setBlockData(d, false);
    }

    private static final int MIN_X = -(WALK_X + TIERS + 1), MIN_Z = -(WALK_Z + TIERS + 1);

    /** Wipe the whole build region. */
    public void clearAll() {
        for (int x = MIN_X - 2; x <= -MIN_X + 3; x++)
            for (int z = MIN_Z - 2; z <= -MIN_Z + 3; z++)
                for (int y = FLOOR_Y - 4; y <= OUTER_TOP + 16; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
    }

    /** Build the whole stadium for a match (team colours change per match), then a full deck. */
    public void buildAll() {
        clearAll();
        for (int x = MIN_X; x <= -MIN_X; x++)
            for (int z = MIN_Z; z <= -MIN_Z; z++) buildColumn(x, z);
        buildScoreboards();
        buildCrowns();
        buildBanners();
        setScore(0, 0);
        resetFloor();
    }

    private void buildColumn(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        int side = z < 0 ? 0 : 1;
        if (ax <= HALF_X && az <= BACK_Z) {                       // pool: base, lava (deck by resetFloor)
            set(x, FLOOR_Y - 3, z, Material.STONE);
            set(x, FLOOR_Y - 2, z, Material.STONE);
            set(x, FLOOR_Y - 1, z, Material.LAVA);
            set(x, CEILING_Y, z, Material.BARRIER);
        } else if (ax <= WALL_X && az <= WALL_Z) {                // pool wall
            for (int y = FLOOR_Y - 3; y <= RIM_Y; y++) {
                Material m;
                if (z == 0) m = Material.QUARTZ_PILLAR;
                else if (y <= FLOOR_Y) m = Material.STONE_BRICKS;
                else if (y == RIM_Y) m = Material.WHITE_CONCRETE;
                else m = mat(dye(side), "CONCRETE");
                set(x, y, z, m);
            }
            for (int y = RIM_Y + 1; y <= CEILING_Y; y++) set(x, y, z, Material.BARRIER);
        } else if (ax <= WALK_X && az <= WALK_Z) {                // walkway at rim level
            for (int y = RIM_Y - 3; y < RIM_Y; y++) set(x, y, z, Material.STONE_BRICKS);
            boolean edge = ax == WALK_X || az == WALK_Z || ax == WALL_X + 1 || az == WALL_Z + 1;
            set(x, RIM_Y, z, edge ? Material.POLISHED_ANDESITE : ((x + z) % 3 == 0 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS));
        } else {
            int e = Math.max(ax - WALK_X, az - WALK_Z);
            if (e <= TIERS) {                                     // stepped seating
                int top = RIM_Y + e;
                for (int y = RIM_Y - 3; y < top; y++) set(x, y, z, Material.STONE_BRICKS);
                int ex = ax - WALK_X, ez = az - WALK_Z;
                if (ex == ez) {
                    set(x, top, z, Material.SPRUCE_PLANKS);
                } else {
                    BlockFace back = ex > ez ? (x > 0 ? BlockFace.EAST : BlockFace.WEST) : (z > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
                    Stairs st = (Stairs) Material.SPRUCE_STAIRS.createBlockData();
                    st.setFacing(back);
                    set(x, top, z, st);
                }
            } else {                                              // outer wall
                for (int y = RIM_Y - 3; y <= OUTER_TOP; y++) set(x, y, z, y % 5 == 0 ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS);
                if (((x + z) & 1) == 0) set(x, OUTER_TOP + 1, z, Material.STONE_BRICKS);
            }
        }
    }

    // ------------------------------------------------------------------ deck

    /** Deck material at (x, z): centre line, team stripes, target pad, pixel pattern. Pure, so tools can render it. */
    public static char deckCode(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        if (z == 0) return 'K';                                    // black centre line
        if (az == 1) return 'T';                                   // team stripe along the line
        if (ax <= 3 && az >= PAD_Z - 3 && az <= PAD_Z + 3) {       // 7x7 target pad: black ring, white ring, black centre
            int ring = Math.max(ax, Math.abs(az - PAD_Z));
            return ring == 2 ? 'W' : 'K';
        }
        if (az == 11 && ax <= 9) return 'K';                        // thin black lane stripe behind the pad
        // Bold team-colour pixel art on a mostly white deck (024533, 032237): a diamond ring with a centre dot on
        // each back corner, a 2-wide diagonal step band from the pad outwards, and a lane stripe along each side.
        int dd = Math.abs(ax - 7) + Math.abs(az - 14);
        if (dd == 3 || dd == 4 || dd == 0) return 'T';
        int diag = ax - az;
        if (ax >= 5 && ax <= 10 && az >= 3 && az <= 9 && (diag == 1 || diag == 2)) return 'T';
        if (ax == HALF_X - 1 && az >= 2 && az <= 9) return 'T';
        if (az == BACK_Z - 1 && ax <= 2) return 'T';
        return 'W';
    }

    private Material deckMat(int x, int z) {
        char c = deckCode(x, z);
        return switch (c) {
            case 'K' -> Material.BLACK_CONCRETE;
            case 'T' -> mat(dye(z < 0 ? 0 : 1), "CONCRETE");
            default -> Material.WHITE_CONCRETE;
        };
    }

    /** Restore the full deck (start of every round). Lava under it is left as it is. */
    public void resetFloor() {
        shrunk = 0;
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -BACK_Z; z <= BACK_Z; z++) {
                set(x, FLOOR_Y, z, deckMat(x, z));
                if (world.getBlockAt(x, FLOOR_Y - 1, z).getType() != Material.LAVA) set(x, FLOOR_Y - 1, z, Material.LAVA);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 3; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
            }
    }

    /** The blocks of the next ring to go (empty when the deck is at its minimum). */
    private List<Block> ringBlocks(int r) {
        List<Block> out = new ArrayList<>();
        for (int x = -HALF_X; x <= HALF_X; x++)
            for (int z = -BACK_Z; z <= BACK_Z; z++)
                if (ringOf(x, z) == r) out.add(world.getBlockAt(x, FLOOR_Y, z));
        return out;
    }

    /** Warning stage: the outer ring turns red (MCC shows a red edge before it goes). False if no more shrinking. */
    public boolean markNextRing() {
        if (shrunk >= MAX_SHRINK) return false;
        for (Block b : ringBlocks(shrunk)) if (b.getType() != Material.AIR) b.setType(Material.RED_CONCRETE, false);
        return true;
    }

    /** Remove the marked ring: the deck is now one block smaller on every outer edge. */
    public void dropRing() {
        if (shrunk >= MAX_SHRINK) return;
        List<Block> ring = ringBlocks(shrunk);
        for (Block b : ring) {
            if (b.getType() == Material.AIR) continue;
            b.setType(Material.AIR, false);
            if (random.nextInt(6) == 0) world.spawnParticle(Particle.LAVA, b.getLocation().add(0.5, 0.2, 0.5), 2, 0.2, 0.1, 0.2, 0);
        }
        shrunk++;
    }

    // ------------------------------------------------------------------ physical scoreboard (032201: two white boards, red digits)

    private static final String[] DIGITS = {
            "01110,10001,10011,10101,11001,10001,01110", "00100,01100,00100,00100,00100,00100,01110",
            "01110,10001,00001,00010,00100,01000,11111", "11110,00001,00001,01110,00001,00001,11110",
            "00010,00110,01010,10010,11111,00010,00010", "11111,10000,11110,00001,00001,10001,01110",
            "00110,01000,10000,11110,10001,10001,01110", "11111,00001,00010,00100,01000,01000,01000",
            "01110,10001,10001,01110,10001,10001,01110", "01110,10001,10001,01111,00001,00010,01100"};
    private static final int BOARD_X = WALK_X + TIERS + 1;   // on the +x outer wall, read from the -x stands
    private static final int BOARD_BOTTOM = RIM_Y + TIERS + 3, BOARD_W = 12, BOARD_H = 16;

    private int boardZ0(int side) { return side == 0 ? -BOARD_W - 1 : 2; }

    private void buildScoreboards() {
        for (int side = 0; side < 2; side++) {
            int z0 = boardZ0(side);
            for (int dz = -1; dz <= BOARD_W; dz++) {
                set(BOARD_X, BOARD_BOTTOM + BOARD_H, z0 + dz, Material.DARK_OAK_PLANKS);
                set(BOARD_X - 1, BOARD_BOTTOM + BOARD_H, z0 + dz, Material.DARK_OAK_SLAB);
                set(BOARD_X + 1, BOARD_BOTTOM + BOARD_H + 1, z0 + dz, Material.DARK_OAK_PLANKS);
            }
            for (int dz = 0; dz < BOARD_W; dz++)
                for (int dy = 0; dy < BOARD_H; dy++) {
                    set(BOARD_X, BOARD_BOTTOM + dy, z0 + dz, Material.WHITE_CONCRETE);
                    set(BOARD_X + 1, BOARD_BOTTOM + dy, z0 + dz, Material.DARK_OAK_PLANKS);
                }
        }
    }

    /** Write the round score on the two boards (left board = side A as seen from the opposite stands). */
    public void setScore(int a, int b) {
        drawDigit(0, a);
        drawDigit(1, b);
    }

    private void drawDigit(int side, int value) {
        String[] rows = DIGITS[Math.max(0, Math.min(9, value))].split(",");
        int z0 = boardZ0(side);
        for (int dz = 1; dz < BOARD_W - 1; dz++)
            for (int dy = 1; dy < BOARD_H - 1; dy++) set(BOARD_X, BOARD_BOTTOM + dy, z0 + dz, Material.WHITE_CONCRETE);
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 5; c++) {
                if (rows[r].charAt(c) != '1') continue;
                for (int sz = 0; sz < 2; sz++)
                    for (int sy = 0; sy < 2; sy++)
                        set(BOARD_X, BOARD_BOTTOM + BOARD_H - 2 - 2 * r - sy, z0 + 1 + 2 * c + sz, Material.RED_CONCRETE);
            }
    }

    // ------------------------------------------------------------------ crowns and banners (render_s6: crown emblems, huge team banners)

    private static final String[] CROWN = {"1000100010", "1101110110", "1111111111", "1111111111", "0111111110"};

    private void buildCrowns() {
        for (int side = 0; side < 2; side++) {
            int z = side == 0 ? MIN_Z : -MIN_Z;
            Material bg = mat(dye(side), "CONCRETE");
            int y0 = RIM_Y + TIERS + 2;
            for (int dx = -7; dx <= 6; dx++)
                for (int dy = 0; dy < 9; dy++) set(dx, y0 + dy, z, bg);
            for (int r = 0; r < CROWN.length; r++)
                for (int c = 0; c < 10; c++)
                    if (CROWN[r].charAt(c) == '1') set(c - 5, y0 + 7 - r, z, Material.YELLOW_CONCRETE);
        }
    }

    private void buildBanners() {
        int x = MIN_X;   // the -x outer wall, opposite the scoreboards
        for (int side = 0; side < 2; side++) {
            Material team = mat(dye(side), "CONCRETE");
            int zc = side == 0 ? -12 : 12;
            int top = OUTER_TOP + 4;
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = top - 15; y <= top; y++) {
                    boolean notch = y == top - 15 && Math.abs(dz) <= 1;
                    if (notch) continue;
                    boolean white = (dz == 0 && (y - top) % 4 != 0) || (Math.abs(dz) == 2 && (y + dz) % 5 == 0);
                    set(x, y, zc + dz, white ? Material.WHITE_CONCRETE : team);
                }
                set(x, top + 1, zc + dz, Material.DARK_OAK_PLANKS);
            }
            set(x, top + 1, zc - 4, Material.DARK_OAK_PLANKS);
            set(x, top + 1, zc + 4, Material.DARK_OAK_PLANKS);
        }
    }
}
