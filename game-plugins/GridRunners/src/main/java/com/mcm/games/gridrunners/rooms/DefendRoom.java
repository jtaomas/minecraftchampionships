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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Protect the villager through three zombie waves. If it falls, the current wave restarts. */
public final class DefendRoom extends Room {
    private static final double[][] SPAWNS = {{2.5, 2.5}, {13.5, 2.5}, {2.5, 13.5}, {13.5, 13.5}};
    private static final int WAVES = 3;
    private final Set<UUID> mobs = new LinkedHashSet<>();
    private UUID villager;
    private int wave;
    private int delay;
    private boolean waveSpawned;

    public DefendRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.DEFEND, col, row, seed);
    }

    @Override public String name() { return "Villager Guard"; }
    @Override public String objective() { return "Protect the villager through " + WAVES + " zombie waves"; }
    @Override public String hintText() { return "Stand between the corners and the villager - zombies go for it first. Remaining zombies now glow."; }

    @Override
    public String progress() {
        if (!waveSpawned) return "Wave " + (wave + 1) + "/" + WAVES + " incoming...";
        return "Wave " + (wave + 1) + "/" + WAVES + " §7| zombies left " + mobs.size();
    }

    @Override
    public Material floorAt(int lx, int lz) {
        if (lx >= 6 && lx <= 9 && lz >= 6 && lz <= 9) return Material.CUT_SANDSTONE;
        return ((lx + lz) % 4 == 0) ? Material.CHISELED_SANDSTONE : Material.SMOOTH_SANDSTONE;
    }

    @Override
    public void buildContent() {
        put(7, 0, 7, Material.HAY_BLOCK);
        put(8, 0, 8, Material.HAY_BLOCK);
        put(7, 0, 8, Material.HAY_BLOCK);
        put(8, 0, 7, Material.HAY_BLOCK);
        for (double[] s : SPAWNS) put((int) s[0], 0, (int) s[1], Material.SOUL_SAND);
        int[][] posts = {{5, 5}, {10, 5}, {5, 10}, {10, 10}};
        for (int[] p : posts) { put(p[0], 1, p[1], Material.SANDSTONE_WALL); put(p[0], 2, p[1], Material.LANTERN); }
    }

    @Override
    public void onActivate() {
        wave = 0;
        waveSpawned = false;
        delay = 3;
        spawnVillager();
    }

    private void spawnVillager() {
        Villager v = spawn(loc(8, 1, 8), Villager.class, vi -> {
            vi.setAI(false);
            vi.setAdult();
            game.protect(vi);
        });
        villager = v.getUniqueId();
    }

    private int waveSize() {
        return 2 + wave + Math.min(2, Math.max(0, game.online(lane).size() - 1));
    }

    @Override
    public void secondTick() {
        if (!alive(villager)) {
            if (villager != null) entities.remove(villager);
            spawnVillager();
        }
        Entity v = entity(villager);
        if (!waveSpawned) {
            if (--delay <= 0) {
                int n = waveSize();
                for (int i = 0; i < n; i++) {
                    double[] s = SPAWNS[i % SPAWNS.length];
                    Location l = loc(s[0], 1, s[1]);
                    Entity m = (rng.nextInt(3) == 0)
                            ? spawn(l, Husk.class, z -> { z.setAdult(); z.setShouldBurnInDay(false); })
                            : spawn(l, Zombie.class, z -> { z.setAdult(); z.setShouldBurnInDay(false); });
                    mobs.add(m.getUniqueId());
                    world.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
                }
                waveSpawned = true;
                soundInside(Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
                barInside("§cWave " + (wave + 1) + "/" + WAVES + "!");
            }
            return;
        }
        prune(mobs);
        keepInside(mobs);
        // Make sure idle zombies go for the villager.
        if (v instanceof LivingEntity target) {
            for (UUID u : mobs) {
                if (entity(u) instanceof Mob m && m.getTarget() == null) m.setTarget(target);
            }
        }
        if (hinted) for (UUID u : mobs) { Entity e = entity(u); if (e != null) e.setGlowing(true); }
        if (mobs.isEmpty()) {
            wave++;
            waveSpawned = false;
            if (wave >= WAVES) {
                complete();
                return;
            }
            delay = 3;
            soundInside(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
            tellInside("§aWave cleared! §7Next wave in 3 seconds...");
        }
    }

    @Override
    public void onEntityDeath(LivingEntity en) {
        if (mobs.remove(en.getUniqueId())) {
            entities.remove(en.getUniqueId());
            soundInside(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    @Override
    public void onProtectedLethal(LivingEntity en) {
        if (!en.getUniqueId().equals(villager)) return;
        en.setHealth(en.getMaxHealth());
        for (UUID u : mobs) { Entity e = entity(u); if (e != null) e.remove(); entities.remove(u); }
        mobs.clear();
        waveSpawned = false;
        delay = 4;
        world.playSound(en.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 1f, 1f);
        tellInside("§cThe villager was knocked out! §7Wave " + (wave + 1) + " restarts in 4 seconds.");
    }

    @Override
    protected Location returnSpot() { return loc(2.5, 1, 2.5); }

    @Override
    protected void onCleanup() { mobs.clear(); villager = null; }
}
