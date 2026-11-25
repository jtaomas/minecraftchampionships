package com.mcm.games.hungergames;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tiered chest loot generator for modern 1.21 MCC Survival Games.
 */
public final class LootTable {
    public enum Tier {
        OUTER,
        CORNUCOPIA,
        AIRDROP
    }

    private LootTable() {}

    public static void fill(Inventory inv, Tier tier) {
        inv.clear();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<ItemStack> pool = switch (tier) {
            case OUTER -> outerPool(rng);
            case CORNUCOPIA -> cornucopiaPool(rng);
            case AIRDROP -> airdropPool(rng);
        };
        int count = switch (tier) {
            case OUTER -> 4 + rng.nextInt(4);
            case CORNUCOPIA -> 6 + rng.nextInt(4);
            case AIRDROP -> 7 + rng.nextInt(3);
        };

        List<Integer> slots = new ArrayList<>(inv.getSize());
        for (int s = 0; s < inv.getSize(); s++) slots.add(s);
        Collections.shuffle(slots, rng);

        for (int i = 0; i < count && i < slots.size() && !pool.isEmpty(); i++) {
            ItemStack item = pool.get(rng.nextInt(pool.size())).clone();
            inv.setItem(slots.get(i), item);
        }
    }

    private static List<ItemStack> outerPool(ThreadLocalRandom rng) {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(Material.WOODEN_SWORD));
        list.add(new ItemStack(Material.STONE_SWORD));
        list.add(new ItemStack(Material.STONE_AXE));
        list.add(new ItemStack(Material.IRON_AXE));
        list.add(new ItemStack(Material.BOW));
        list.add(new ItemStack(Material.CROSSBOW));
        list.add(new ItemStack(Material.ARROW, 6 + rng.nextInt(8)));
        list.add(new ItemStack(Material.SHIELD));
        list.add(new ItemStack(Material.LEATHER_HELMET));
        list.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        list.add(new ItemStack(Material.CHAINMAIL_HELMET));
        list.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        list.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        list.add(new ItemStack(Material.CHAINMAIL_BOOTS));
        list.add(new ItemStack(Material.IRON_HELMET));
        list.add(new ItemStack(Material.IRON_BOOTS));
        list.add(new ItemStack(Material.COOKED_BEEF, 2 + rng.nextInt(4)));
        list.add(new ItemStack(Material.BREAD, 3 + rng.nextInt(4)));
        list.add(new ItemStack(Material.GOLDEN_CARROT, 2 + rng.nextInt(3)));
        list.add(new ItemStack(Material.LAPIS_LAZULI, 4 + rng.nextInt(5)));
        list.add(new ItemStack(Material.COBWEB, 2 + rng.nextInt(2)));
        list.add(new ItemStack(Material.OAK_PLANKS, 8 + rng.nextInt(9)));
        list.add(new ItemStack(Material.COBBLESTONE, 8 + rng.nextInt(9)));
        return list;
    }

    private static List<ItemStack> cornucopiaPool(ThreadLocalRandom rng) {
        List<ItemStack> list = new ArrayList<>(outerPool(rng));
        list.add(new ItemStack(Material.IRON_SWORD));
        list.add(new ItemStack(Material.IRON_AXE));
        list.add(new ItemStack(Material.DIAMOND_AXE));
        list.add(new ItemStack(Material.IRON_HELMET));
        list.add(new ItemStack(Material.IRON_CHESTPLATE));
        list.add(new ItemStack(Material.IRON_LEGGINGS));
        list.add(new ItemStack(Material.IRON_BOOTS));
        list.add(new ItemStack(Material.DIAMOND_BOOTS));
        list.add(new ItemStack(Material.GOLDEN_APPLE));
        list.add(new ItemStack(Material.ENDER_PEARL, 1 + rng.nextInt(2)));
        list.add(new ItemStack(Material.ARROW, 12));
        list.add(new ItemStack(Material.LAPIS_LAZULI, 8));
        list.add(splashPotion(PotionType.HEALING));
        list.add(splashPotion(PotionType.SWIFTNESS));
        list.add(enchantedBook(Enchantment.SHARPNESS, 1));
        list.add(enchantedBook(Enchantment.PROTECTION, 1));
        return list;
    }

    private static List<ItemStack> airdropPool(ThreadLocalRandom rng) {
        List<ItemStack> list = new ArrayList<>();
        list.add(enchantedWeapon(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 1));
        list.add(new ItemStack(Material.DIAMOND_AXE));
        list.add(new ItemStack(Material.DIAMOND_HELMET));
        list.add(new ItemStack(Material.DIAMOND_CHESTPLATE));
        list.add(new ItemStack(Material.DIAMOND_LEGGINGS));
        list.add(new ItemStack(Material.DIAMOND_BOOTS));
        list.add(new ItemStack(Material.GOLDEN_APPLE, 2));
        list.add(enchantedWeapon(Material.BOW, Enchantment.POWER, 2));
        list.add(new ItemStack(Material.ARROW, 16));
        list.add(new ItemStack(Material.ENDER_PEARL, 2 + rng.nextInt(2)));
        list.add(splashPotion(PotionType.STRONG_HEALING));
        list.add(splashPotion(PotionType.SWIFTNESS));
        list.add(new ItemStack(Material.COOKED_BEEF, 6));
        return list;
    }

    private static ItemStack splashPotion(PotionType type) {
        ItemStack pot = new ItemStack(Material.SPLASH_POTION);
        if (pot.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            pot.setItemMeta(meta);
        }
        return pot;
    }

    private static ItemStack enchantedBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        if (book.getItemMeta() instanceof EnchantmentStorageMeta meta) {
            meta.addStoredEnchant(ench, level, true);
            book.setItemMeta(meta);
        }
        return book;
    }

    private static ItemStack enchantedWeapon(Material mat, Enchantment ench, int level) {
        ItemStack item = new ItemStack(mat);
        item.addUnsafeEnchantment(ench, level);
        return item;
    }
}
