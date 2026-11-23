package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Kill N monsters. Never more than 4 alive; lost mobs are simply replaced. */
public final class MobPitRoom extends Room {
    private static final double[][] SPAWNS = {{2.5, 2.5}, {13.5, 2.5}, {2.5, 13.5}, {13.5, 13.5}};
    private final Set<UUID> mobs = new LinkedHashSet<>();
    private int target = 6;
    private int kills;

    public MobPitRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.MOB_PIT, col, row, seed);
    }

    @Override public String name() { return "Monster Pit"; }
    @Override public String objective() { return "Defeat " + target + " monsters"; }
    @Override public String hintText() { return "Monsters spawn from the four corners - fight back to back. The rest now glow."; }
    @Override public String progress() { return "Monsters " + kills + "/" + target; }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx * 7 + lz * 13) % 5 == 0) ? Material.MOSSY_COBBLESTONE : Material.COARSE_DIRT;
    }

    @Override
    public void buildContent() {
        int[][] pillars = {{4, 4}, {11, 4}, {4, 11}, {11, 11}};
        for (int[] p : pillars) {
            put(p[0], 1, p[1], Material.MOSSY_STONE_BRICKS);
            put(p[0], 2, p[1], Material.MOSSY_STONE_BRICK_WALL);
        }
        for (double[] s : SPAWNS) put((int) s[0], 0, (int) s[1], Material.CRACKED_STONE_BRICKS);
    }

    @Override
    public void onActivate() {
        target = 5 + Math.min(5, Math.max(1, game.online(lane).size()));
        kills = 0;
    }

    @Override
    public void secondTick() {
        int alive = prune(mobs);
        while (alive < 4 && alive + kills < target) {
            spawnMob();
            alive++;
        }
        keepInside(mobs);
        if (hinted) for (UUID u : mobs) { Entity e = entity(u); if (e != null) e.setGlowing(true); }
    }

    private void spawnMob() {
        double[] s = SPAWNS[rng.nextInt(SPAWNS.length)];
        Location l = loc(s[0], 1, s[1]);
        int kind = rng.nextInt(3);
        Entity e;
        if (kind == 0) e = spawn(l, Zombie.class, z -> { z.setAdult(); z.setShouldBurnInDay(false); });
        else if (kind == 1) e = spawn(l, Husk.class, z -> { z.setAdult(); z.setShouldBurnInDay(false); });
        else e = spawn(l, Slime.class, sl -> sl.setSize(2));
        mobs.add(e.getUniqueId());
        world.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
    }

    @Override
    public void onEntityDeath(LivingEntity en) {
        if (!mobs.remove(en.getUniqueId())) return;
        entities.remove(en.getUniqueId());
        kills++;
        soundInside(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        if (kills >= target) complete();
    }

    @Override
    protected Location returnSpot() { return loc(7.5, 1, 7.5); }

    @Override
    protected void onCleanup() { mobs.clear(); }
}
