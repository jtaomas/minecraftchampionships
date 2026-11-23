package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/** Final room: unlocks when all 8 rooms are done. Hurdle sprint to the checkered gold corner. */
public final class FinishRoom extends Room {
    public FinishRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.FINISH, col, row, seed);
    }

    @Override public String name() { return "Final Sprint"; }
    @Override public String objective() { return "Sprint over the hurdles to the gold finish!"; }
    @Override public String hintText() { return "Run to the far corner."; }
    @Override public String progress() { return "Reach the finish corner"; }
    @Override public String title() { return "Final Sprint"; }

    private static boolean finishZone(int lx, int lz) { return lx >= 11 && lz >= 11; }

    @Override
    public Material floorAt(int lx, int lz) {
        if (finishZone(lx, lz)) return ((lx + lz) & 1) == 0 ? Material.GOLD_BLOCK : Material.BLACK_CONCRETE;
        return ((lx + lz) & 1) == 0 ? Material.SMOOTH_QUARTZ : Material.QUARTZ_BRICKS;
    }

    @Override
    public void buildContent() {
        for (int i = 4; i <= 12; i++) {
            put(i, 1, 4, Material.WHITE_CONCRETE);
            put(4, 1, i, Material.WHITE_CONCRETE);
        }
        for (int i = 8; i <= 10; i++) {
            put(i, 1, 8, Material.YELLOW_CONCRETE);
            put(8, 1, i, Material.YELLOW_CONCRETE);
        }
        put(10, 1, 14, Material.GOLD_BLOCK);
        put(10, 2, 14, Material.GOLD_BLOCK);
        put(14, 1, 10, Material.GOLD_BLOCK);
        put(14, 2, 10, Material.GOLD_BLOCK);
    }

    @Override
    public void onPlayerFast(Player p) {
        if (finishZone(lx(p.getLocation()), lz(p.getLocation()))) game.laneFinished(lane);
    }

    @Override
    public void secondTick() {
        world.spawnParticle(Particle.END_ROD, loc(13, 1.5, 13), 12, 1.2, 0.6, 1.2, 0.01);
    }
}
