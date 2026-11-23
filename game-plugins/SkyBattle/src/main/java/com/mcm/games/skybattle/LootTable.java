package com.mcm.games.skybattle;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/** Weighted loot tables for the three chest tiers. */
public final class LootTable {
    public enum Tier { ISLAND, MID, CENTER }

    private record Entry(int weight, Supplier<ItemStack> item) {}

    private final List<Entry> island = new ArrayList<>();
    private final List<Entry> mid = new ArrayList<>();
    private final List<Entry> center = new ArrayList<>();
    private final Random random;

    public LootTable(Random random) {
        this.random = random;

        // Tier 1: starting islands
        add(island, 10, () -> item(Material.STONE_SWORD, 1));
        add(island, 6, () -> item(Material.WOODEN_AXE, 1));
        add(island, 8, () -> item(Material.LEATHER_HELMET, 1));
        add(island, 8, () -> item(Material.LEATHER_LEGGINGS, 1));
        add(island, 6, () -> item(Material.CHAINMAIL_HELMET, 1));
        add(island, 6, () -> item(Material.CHAINMAIL_BOOTS, 1));
        add(island, 4, () -> item(Material.CHAINMAIL_LEGGINGS, 1));
        add(island, 4, () -> item(Material.BOW, 1));
        add(island, 6, () -> item(Material.ARROW, 4 + random.nextInt(5)));
        add(island, 10, () -> item(Material.SNOWBALL, 8 + random.nextInt(9)));
        add(island, 14, () -> item(Material.COBBLESTONE, 32));
        add(island, 12, () -> item(Material.OAK_PLANKS, 32));
        add(island, 8, () -> item(Material.BREAD, 3 + random.nextInt(3)));
        add(island, 8, () -> item(Material.COOKED_BEEF, 2 + random.nextInt(3)));
        add(island, 5, () -> item(Material.STONE_PICKAXE, 1));
        add(island, 3, () -> potion(PotionType.HEALING));
        add(island, 2, () -> item(Material.ENDER_PEARL, 1));

        // Tier 2: mid islands
        add(mid, 10, () -> item(Material.IRON_SWORD, 1));
        add(mid, 6, () -> item(Material.STONE_SWORD, 1));
        add(mid, 7, () -> item(Material.IRON_HELMET, 1));
        add(mid, 6, () -> item(Material.IRON_CHESTPLATE, 1));
        add(mid, 6, () -> item(Material.IRON_LEGGINGS, 1));
        add(mid, 7, () -> item(Material.IRON_BOOTS, 1));
        add(mid, 6, () -> item(Material.CHAINMAIL_CHESTPLATE, 1));
        add(mid, 7, () -> item(Material.BOW, 1));
        add(mid, 9, () -> item(Material.ARROW, 8 + random.nextInt(9)));
        add(mid, 8, () -> item(Material.SNOWBALL, 16));
        add(mid, 7, () -> item(Material.ENDER_PEARL, 1));
        add(mid, 6, () -> item(Material.GOLDEN_APPLE, 1));
        add(mid, 7, () -> potion(PotionType.HEALING));
        add(mid, 10, () -> item(Material.COBBLESTONE, 64));
        add(mid, 4, () -> item(Material.COBWEB, 2 + random.nextInt(3)));
        add(mid, 5, () -> item(Material.IRON_PICKAXE, 1));
        add(mid, 6, () -> item(Material.COOKED_PORKCHOP, 4));

        // Tier 3: centre island
        add(center, 6, () -> item(Material.DIAMOND_SWORD, 1));
        add(center, 6, () -> enchanted(Material.IRON_SWORD, Enchantment.SHARPNESS, 1));
        add(center, 5, () -> item(Material.DIAMOND_HELMET, 1));
        add(center, 4, () -> item(Material.DIAMOND_CHESTPLATE, 1));
        add(center, 4, () -> item(Material.DIAMOND_LEGGINGS, 1));
        add(center, 5, () -> enchanted(Material.DIAMOND_BOOTS, Enchantment.FEATHER_FALLING, 2));
        add(center, 6, () -> enchanted(Material.IRON_CHESTPLATE, Enchantment.PROTECTION, 1));
        add(center, 5, () -> enchanted(Material.BOW, Enchantment.POWER, 1));
        add(center, 8, () -> item(Material.ARROW, 16));
        add(center, 8, () -> item(Material.ENDER_PEARL, 2));
        add(center, 7, () -> item(Material.GOLDEN_APPLE, 2));
        add(center, 6, () -> potion(PotionType.STRONG_HEALING));
        add(center, 6, () -> potion(PotionType.HEALING));
        add(center, 8, () -> item(Material.SNOWBALL, 16));
        add(center, 6, () -> item(Material.COBWEB, 4));
        add(center, 6, () -> item(Material.STONE_BRICKS, 64));
        add(center, 4, () -> item(Material.COOKED_BEEF, 6));
    }

    private static void add(List<Entry> list, int weight, Supplier<ItemStack> s) { list.add(new Entry(weight, s)); }

    private static ItemStack item(Material m, int amount) { return new ItemStack(m, Math.max(1, Math.min(amount, m.getMaxStackSize()))); }

    private static ItemStack enchanted(Material m, Enchantment ench, int level) {
        ItemStack it = new ItemStack(m);
        it.addUnsafeEnchantment(ench, level);
        return it;
    }

    private static ItemStack potion(PotionType type) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION);
        if (it.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack roll(List<Entry> list) {
        int total = 0;
        for (Entry e : list) total += e.weight();
        int r = random.nextInt(total);
        for (Entry e : list) {
            r -= e.weight();
            if (r < 0) return e.item().get();
        }
        return list.get(0).item().get();
    }

    /** Fill an inventory with random items of a tier in random slots. */
    public void fill(Inventory inv, Tier tier) {
        inv.clear();
        List<Entry> list = switch (tier) {
            case ISLAND -> island;
            case MID -> mid;
            case CENTER -> center;
        };
        int rolls = switch (tier) {
            case ISLAND -> 4 + random.nextInt(3);
            case MID -> 5 + random.nextInt(3);
            case CENTER -> 6 + random.nextInt(3);
        };
        int size = inv.getSize();
        for (int i = 0; i < rolls; i++) {
            ItemStack it = roll(list);
            for (int tries = 0; tries < 10; tries++) {
                int slot = random.nextInt(size);
                if (inv.getItem(slot) == null) { inv.setItem(slot, it); break; }
            }
        }
    }
}
