package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import com.mcm.games.gridrunners.Msg;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Search barrels for hidden keys. Opening is virtual: no inventories, so nothing can be lost. */
public final class SearchRoom extends Room {
    private static final int[][] BARRELS = {
            {1, 1, 1}, {3, 1, 1}, {1, 1, 3}, {14, 1, 1}, {12, 1, 1}, {14, 1, 3},
            {1, 1, 14}, {3, 1, 14}, {1, 1, 12}, {14, 1, 14}, {12, 1, 14}, {14, 1, 12},
            {4, 3, 5}, {11, 3, 5}, {4, 3, 10}, {11, 3, 10}, {7, 1, 7}, {8, 1, 8}
    };
    private static final int KEYS = 5;
    private final boolean[] hasKey = new boolean[BARRELS.length];
    private final boolean[] searched = new boolean[BARRELS.length];
    private int found;

    public SearchRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.SEARCH, col, row, seed);
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < BARRELS.length; i++) idx.add(i);
        Collections.shuffle(idx, rng);
        for (int i = 0; i < KEYS; i++) hasKey[idx.get(i)] = true;
    }

    @Override public String name() { return "Library Search"; }
    @Override public String objective() { return "Search the barrels and find " + KEYS + " keys"; }
    @Override public String hintText() { return "Right-click barrels to search them. The ones still hiding keys are now sparkling."; }
    @Override public String progress() { return "Keys " + found + "/" + KEYS; }

    @Override
    public Material floorAt(int lx, int lz) {
        return (lx % 4 == 0) ? Material.DARK_OAK_PLANKS : Material.SPRUCE_PLANKS;
    }

    @Override
    public void buildContent() {
        for (int z : new int[]{5, 10}) {
            for (int x = 4; x <= 11; x++) {
                if (x == 7 || x == 8) continue;
                put(x, 1, z, Material.BOOKSHELF);
                put(x, 2, z, Material.BOOKSHELF);
            }
        }
        for (int[] b : BARRELS) {
            Barrel data = (Barrel) Material.BARREL.createBlockData();
            data.setFacing(BlockFace.UP);
            data.setOpen(false);
            put(b[0], b[1], b[2], data);
        }
        put(1, 1, 5, Material.LANTERN);
        put(14, 1, 10, Material.LANTERN);
    }

    @Override
    public void onInteractBlock(Player p, Block b, PlayerInteractEvent e) {
        for (int i = 0; i < BARRELS.length; i++) {
            int[] bp = BARRELS[i];
            if (!is(b, bp[0], bp[1], bp[2])) continue;
            e.setCancelled(true);
            if (searched[i]) {
                Msg.bar(p, "§7Already searched.");
                return;
            }
            searched[i] = true;
            if (b.getBlockData() instanceof Barrel data) {
                data.setOpen(true);
                b.setBlockData(data, false);
            }
            if (hasKey[i]) {
                found++;
                world.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.8f);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 1, 0.5), 15, 0.2, 0.3, 0.2, 0.1);
                tellInside("§a" + p.getName() + " found a key! §7(" + found + "/" + KEYS + ")");
                if (found >= KEYS) complete();
            } else {
                world.playSound(b.getLocation(), Sound.BLOCK_BARREL_OPEN, 1f, 1f);
                Msg.bar(p, "§7Empty...");
            }
            return;
        }
    }

    @Override
    public void hintTick() {
        for (int i = 0; i < BARRELS.length; i++) {
            if (hasKey[i] && !searched[i]) marker(BARRELS[i][0] + 0.5, BARRELS[i][1] + 1.3, BARRELS[i][2] + 0.5);
        }
    }
}
