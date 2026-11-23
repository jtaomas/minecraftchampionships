package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

/** Grab crystals from the pedestals and toss them (Q) into the hopper pit. Pedestals refill forever. */
public final class HopperRoom extends Room {
    private static final int[][] PEDESTALS = {{3, 3}, {12, 3}, {3, 12}, {12, 12}};
    private static final int TARGET = 10;
    private final UUID[] pedestalItems = new UUID[PEDESTALS.length];
    private final int[] cooldown = new int[PEDESTALS.length];
    private int deposited;

    public HopperRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.HOPPER, col, row, seed);
    }

    @Override public String name() { return "Hopper Feed"; }
    @Override public String objective() { return "Drop " + TARGET + " crystals into the hoppers"; }
    @Override public String hintText() { return "Walk up to a corner pedestal to grab its crystal, then stand at the middle pit and press Q to drop it onto the hoppers."; }
    @Override public String progress() { return "Crystals " + deposited + "/" + TARGET; }

    @Override
    public Material floorAt(int lx, int lz) {
        if (lx >= 6 && lx <= 9 && lz >= 6 && lz <= 9) return Material.WAXED_OXIDIZED_CUT_COPPER;
        return ((lx + lz) % 3 == 0) ? Material.WAXED_COPPER_BLOCK : Material.WAXED_CUT_COPPER;
    }

    @Override
    public void buildContent() {
        for (int x = 7; x <= 8; x++)
            for (int z = 7; z <= 8; z++) put(x, 0, z, Material.HOPPER);
        for (int[] p : PEDESTALS) {
            put(p[0], 1, p[1], Material.WAXED_COPPER_BLOCK);
            put(p[0], 0, p[1], Material.AMETHYST_BLOCK);
        }
        // little lightning-rod posts marking the pit corners
        put(6, 1, 6, Material.LIGHTNING_ROD);
        put(9, 1, 6, Material.LIGHTNING_ROD);
        put(6, 1, 9, Material.LIGHTNING_ROD);
        put(9, 1, 9, Material.LIGHTNING_ROD);
    }

    @Override
    public void onActivate() {
        for (int i = 0; i < PEDESTALS.length; i++) spawnCrystal(i);
    }

    private void spawnCrystal(int i) {
        Location l = loc(PEDESTALS[i][0] + 0.5, 2.05, PEDESTALS[i][1] + 0.5);
        ItemStack stack = game.crystal(1);
        Item item = world.dropItem(l, stack, it -> {
            it.setVelocity(new Vector(0, 0, 0));
            it.setUnlimitedLifetime(true);
            it.setCanMobPickup(false);
            it.setPickupDelay(10);
            game.tag(it, id);
        });
        pedestalItems[i] = item.getUniqueId();
        entities.add(item.getUniqueId());
        world.spawnParticle(Particle.END_ROD, l.clone().add(0, 0.3, 0), 6, 0.2, 0.2, 0.2, 0.01);
    }

    @Override
    public void secondTick() {
        for (int i = 0; i < PEDESTALS.length; i++) {
            if (alive(pedestalItems[i])) continue;
            if (pedestalItems[i] != null) {
                entities.remove(pedestalItems[i]);
                pedestalItems[i] = null;
                cooldown[i] = 2;
            }
            if (cooldown[i] > 0) cooldown[i]--;
            else spawnCrystal(i);
        }
    }

    @Override
    public void fastTick() {
        Location c = loc(8, 1, 8);
        for (Entity e : world.getNearbyEntities(c, 2.5, 2, 2.5)) {
            if (!(e instanceof Item item) || !game.isCrystal(item.getItemStack())) continue;
            Location l = item.getLocation();
            double dx = l.getX() - ox, dz = l.getZ() - oz, dy = l.getY() - oy;
            if (dx >= 7 && dx < 9 && dz >= 7 && dz < 9 && dy < 1.8) {
                int amount = item.getItemStack().getAmount();
                entities.remove(item.getUniqueId());
                item.remove();
                deposited += amount;
                world.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1f);
                world.spawnParticle(Particle.WAX_ON, c.clone().add(0, 0.2, 0), 15, 0.6, 0.2, 0.6, 0.05);
                if (deposited >= TARGET) {
                    complete();
                    return;
                }
            }
        }
    }

    @Override
    public void hintTick() {
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc(8, 1.3, 8), 10, 0.6, 0.2, 0.6, 0);
        for (int i = 0; i < PEDESTALS.length; i++) marker(PEDESTALS[i][0] + 0.5, 2.8, PEDESTALS[i][1] + 0.5);
    }

    @Override
    protected void onCleanup() {
        for (int i = 0; i < pedestalItems.length; i++) pedestalItems[i] = null;
        // Strip leftover crystals from the team.
        for (Player p : game.online(lane)) {
            for (ItemStack it : p.getInventory().getContents()) {
                if (game.isCrystal(it)) p.getInventory().remove(it);
            }
        }
        for (Entity e : world.getNearbyEntities(loc(8, 4, 8), 8, 6, 8)) {
            if (e instanceof Item item && game.isCrystal(item.getItemStack())) item.remove();
        }
    }
}
