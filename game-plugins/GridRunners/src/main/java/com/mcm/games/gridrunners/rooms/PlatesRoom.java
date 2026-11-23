package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Every pressure plate must be lit at the same time; each stays lit for a few seconds after being stepped on. */
public final class PlatesRoom extends Room {
    /** {plate x, plate y, plate z}; the support block is directly below. */
    private static final int[][] PLATES = {
            {3, 1, 3}, {12, 2, 3}, {3, 2, 12}, {12, 3, 12}, {5, 1, 9}, {10, 4, 6}
    };
    private static final int LIT_SECONDS = 14;
    private final long[] litUntil = new long[PLATES.length];
    private long now; // in fast ticks (5 game ticks)

    public PlatesRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.PLATES, col, row, seed);
    }

    @Override public String name() { return "Pressure Plates"; }
    @Override public String objective() { return "Light all " + PLATES.length + " plates at the same time"; }
    @Override public String hintText() { return "Each plate stays lit for " + LIT_SECONDS + "s. Split up and step on them together - unlit ones are marked."; }

    @Override
    public String progress() { return "Lit " + litCount() + "/" + PLATES.length; }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx + lz) & 1) == 0 ? Material.POLISHED_ANDESITE : Material.ANDESITE;
    }

    @Override
    public void buildContent() {
        // raised supports
        put(12, 1, 3, Material.RED_CONCRETE);
        put(3, 1, 12, Material.RED_CONCRETE);
        put(12, 1, 12, Material.POLISHED_ANDESITE);
        put(11, 1, 12, Material.POLISHED_ANDESITE); // step
        put(10, 1, 6, Material.POLISHED_ANDESITE);
        put(10, 2, 6, Material.POLISHED_ANDESITE);
        put(8, 1, 6, Material.POLISHED_ANDESITE);   // step 1
        put(9, 2, 6, Material.POLISHED_ANDESITE);   // step 2
        for (int i = 0; i < PLATES.length; i++) {
            int[] p = PLATES[i];
            put(p[0], p[1] - 1, p[2], Material.RED_CONCRETE);
            put(p[0], p[1], p[2], Material.STONE_PRESSURE_PLATE);
        }
    }

    private int litCount() {
        int n = 0;
        for (long t : litUntil) if (t > now) n++;
        return n;
    }

    @Override
    public void fastTick() { now++; }

    @Override
    public void onPlayerFast(Player p) {
        int x = lx(p.getLocation()), y = ly(p.getLocation()), z = lz(p.getLocation());
        for (int i = 0; i < PLATES.length; i++) {
            int[] pl = PLATES[i];
            if (pl[0] == x && pl[1] == y && pl[2] == z) {
                boolean wasLit = litUntil[i] > now;
                litUntil[i] = now + LIT_SECONDS * 4L;
                if (!wasLit) {
                    put(pl[0], pl[1] - 1, pl[2], Material.LIME_CONCRETE);
                    world.playSound(loc(pl[0] + 0.5, pl[1], pl[2] + 0.5), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f + 0.2f * litCount());
                    world.spawnParticle(Particle.HAPPY_VILLAGER, loc(pl[0] + 0.5, pl[1] + 0.5, pl[2] + 0.5), 10, 0.3, 0.3, 0.3, 0);
                }
            }
        }
        if (litCount() == PLATES.length) complete();
    }

    @Override
    public void secondTick() {
        for (int i = 0; i < PLATES.length; i++) {
            int[] pl = PLATES[i];
            if (litUntil[i] != 0 && litUntil[i] <= now) {
                litUntil[i] = 0;
                put(pl[0], pl[1] - 1, pl[2], Material.RED_CONCRETE);
                world.playSound(loc(pl[0] + 0.5, pl[1], pl[2] + 0.5), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            } else if (litUntil[i] > now && litUntil[i] - now <= 12) {
                world.spawnParticle(Particle.SMOKE, loc(pl[0] + 0.5, pl[1] + 0.3, pl[2] + 0.5), 3, 0.2, 0.1, 0.2, 0);
            }
        }
    }

    @Override
    public void hintTick() {
        for (int i = 0; i < PLATES.length; i++) {
            if (litUntil[i] <= now) marker(PLATES[i][0] + 0.5, PLATES[i][1] + 0.8, PLATES[i][2] + 0.5);
        }
    }
}
