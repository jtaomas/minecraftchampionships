package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A hedge maze (5x5 cells) with the goal in the middle. A ring corridor connects all doors to the maze entrances. */
public final class MazeRoom extends Room {
    private static final int N = 5;      // cells per side
    private static final int BASE = 2;   // maze occupies local 2..12
    private static final int GOAL = BASE + 1 + 2 * 2; // 7
    private final boolean[][] wall = new boolean[16][16];
    private final List<int[]> solution = new ArrayList<>();

    public MazeRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.MAZE, col, row, seed);
        generate();
    }

    @Override public String name() { return "Hedge Maze"; }
    @Override public String objective() { return "Reach the golden tile in the centre of the maze"; }
    @Override public String hintText() { return "Follow the green sparkles to the centre."; }

    @Override
    public Material floorAt(int lx, int lz) {
        if (lx == GOAL && lz == GOAL) return Material.GOLD_BLOCK;
        return Material.GRASS_BLOCK;
    }

    private void generate() {
        int size = 2 * N + 1; // 11
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                wall[BASE + i][BASE + j] = (i % 2 == 0) || (j % 2 == 0);
        boolean[][] seen = new boolean[N][N];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{2, 2});
        seen[2][2] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<int[]> opts = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx >= 0 && nx < N && nz >= 0 && nz < N && !seen[nx][nz]) opts.add(new int[]{nx, nz, d[0], d[1]});
            }
            if (opts.isEmpty()) { stack.pop(); continue; }
            int[] o = opts.get(rng.nextInt(opts.size()));
            int cx = BASE + 1 + 2 * c[0], cz = BASE + 1 + 2 * c[1];
            wall[cx + o[2]][cz + o[3]] = false;
            seen[o[0]][o[1]] = true;
            stack.push(new int[]{o[0], o[1]});
        }
        // Door-approach strips are always kept clear (Room.put skips them), so mirror that here.
        for (int x = 1; x <= 14; x++)
            for (int z = 1; z <= 14; z++)
                if (isDoorStrip(x, z)) wall[x][z] = false;
        // Two guaranteed entrances on the outer boundary (east and south side).
        wall[12][3] = false;
        wall[11][12] = false;
        computeSolution();
    }

    private void computeSolution() {
        int[][] prev = new int[16 * 16][];
        boolean[][] vis = new boolean[16][16];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{GOAL, GOAL});
        vis[GOAL][GOAL] = true;
        int[] end = null;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == 1 || c[0] >= 13 || c[1] == 1 || c[1] >= 13) { end = c; break; }
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx < 1 || nx > 14 || nz < 1 || nz > 14 || vis[nx][nz] || wall[nx][nz]) continue;
                vis[nx][nz] = true;
                prev[nx * 16 + nz] = c;
                q.add(new int[]{nx, nz});
            }
        }
        solution.clear();
        for (int[] c = end; c != null; c = prev[c[0] * 16 + c[1]]) solution.add(c);
        Collections.reverse(solution);
    }

    @Override
    public void buildContent() {
        Leaves leaves = (Leaves) Material.OAK_LEAVES.createBlockData();
        leaves.setPersistent(true);
        for (int x = 1; x <= 14; x++)
            for (int z = 1; z <= 14; z++)
                if (wall[x][z]) for (int y = 1; y <= 3; y++) put(x, y, z, leaves);
        put(GOAL, 1, GOAL, Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @Override
    public void onPlayerFast(Player p) {
        if (lx(p.getLocation()) == GOAL && lz(p.getLocation()) == GOAL) complete();
    }

    @Override
    public void secondTick() {
        world.spawnParticle(Particle.END_ROD, loc(GOAL + 0.5, 2.5, GOAL + 0.5), 3, 0.2, 0.5, 0.2, 0.01);
    }

    @Override
    public void hintTick() {
        for (int[] c : solution) world.spawnParticle(Particle.HAPPY_VILLAGER, loc(c[0] + 0.5, 1.4, c[1] + 0.5), 2, 0.15, 0.1, 0.15, 0);
    }
}
