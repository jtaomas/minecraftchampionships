package com.mcm.games.skyblockle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Lootable graves (video research: a dead player's whole inventory goes into "&lt;name&gt;'s Grave", a 5-row
 * container in a chest-like block with a headstone at the death spot). The chest block itself stays empty; the
 * items live in a 45-slot virtual inventory opened by right-clicking the grave.
 */
final class Graves {
    private final MapWorld map;
    private final Map<Long, Inventory> graves = new HashMap<>();
    private final Map<Long, int[]> headstones = new HashMap<>();

    Graves(MapWorld map) { this.map = map; }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static long key(Block b) { return key(b.getX(), b.getY(), b.getZ()); }

    boolean isGrave(Block b) { return b != null && b.getWorld() == map.world() && graves.containsKey(key(b)); }

    boolean isHeadstone(Block b) {
        if (b == null || b.getWorld() != map.world()) return false;
        for (int[] h : headstones.values()) if (h[0] == b.getX() && h[1] == b.getY() && h[2] == b.getZ()) return true;
        return false;
    }

    /** Move the player's whole inventory into a grave at (or just above) {@code at}. Returns false if no spot. */
    @SuppressWarnings("deprecation")
    boolean create(Player p, Location at, int maxY) {
        World w = map.world();
        if (w == null || at.getWorld() != w) return false;
        Block b = null;
        for (int dy = 0; dy <= 4; dy++) {
            Block c = w.getBlockAt(at.getBlockX(), at.getBlockY() + dy, at.getBlockZ());
            if (c.getY() >= Math.min(maxY, w.getMaxHeight() - 2)) break;
            if ((c.getType().isAir() || c.isLiquid() || c.isReplaceable()) && !graves.containsKey(key(c))) { b = c; break; }
        }
        if (b == null) return false;
        PlayerInventory pi = p.getInventory();
        Inventory inv = Bukkit.createInventory(null, 45, p.getName() + "'s Grave");
        ItemStack[] contents = pi.getContents(); // 0-35 storage, 36-39 armour, 40 offhand
        boolean any = false;
        for (int i = 0; i < contents.length && i < 45; i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir()) { inv.setItem(i, it.clone()); any = true; }
        }
        if (!any) return true; // nothing to keep: no grave block needed
        b.setType(Material.CHEST, false);
        map.markDirty(b);
        long k = key(b);
        graves.put(k, inv);
        Block top = b.getRelative(0, 1, 0);
        if (top.getType().isAir()) {
            top.setBlockData(map.data("minecraft:stone_brick_wall[up=true]"), false);
            headstones.put(k, new int[]{top.getX(), top.getY(), top.getZ()});
        }
        return true;
    }

    /** Open a grave's contents for a player; true if {@code b} is a grave. */
    boolean open(Player viewer, Block b) {
        Inventory inv = isGrave(b) ? graves.get(key(b)) : null;
        if (inv == null) return false;
        viewer.openInventory(inv);
        return true;
    }

    /** The grave block was broken: spill its contents and drop the record (the chest block itself drops nothing). */
    void broken(Block b) {
        long k = key(b);
        Inventory inv = graves.remove(k);
        if (inv == null) return;
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack it : inv.getContents()) if (it != null && !it.getType().isAir()) b.getWorld().dropItemNaturally(l, it);
        inv.clear();
        int[] h = headstones.remove(k);
        if (h != null) {
            Block hb = b.getWorld().getBlockAt(h[0], h[1], h[2]);
            if (hb.getType() == Material.STONE_BRICK_WALL) hb.setType(Material.AIR, false);
        }
    }

    /** Forget all graves (the blocks are wiped with the map). Closes open grave views. */
    void clear() {
        for (Inventory inv : graves.values()) {
            for (var v : new java.util.ArrayList<>(inv.getViewers())) v.closeInventory();
            inv.clear();
        }
        graves.clear();
        headstones.clear();
    }

    int count() { return graves.size(); }
}
