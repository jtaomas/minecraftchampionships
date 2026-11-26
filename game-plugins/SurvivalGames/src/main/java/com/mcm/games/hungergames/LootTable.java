package com.mcm.games.hungergames;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tiered chest & Coin Crate loot generator for 1.21.1 MCC Survival Games.
 * Follows MCC 31 (no iron chestplates/axes; boosted splash harming & armor variety),
 * MCC TCC (wind charges, chorus fruit, no Fire Aspect books, 1-enchant crossbow cap),
 * MCC 14/19 (boats, snowballs), plus an ultra-rare nerfed 1.21 Mace.
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
            case AIRDROP -> 6 + rng.nextInt(3);
        };

        List<Integer> slots = new ArrayList<>(inv.getSize());
        for (int s = 0; s < inv.getSize(); s++) slots.add(s);
        Collections.shuffle(slots, rng);

        int idx = 0;
        if (tier == Tier.CORNUCOPIA && rng.nextDouble() < 0.04 && idx < slots.size()) {
            inv.setItem(slots.get(idx++), heavyMace());
        } else if (tier == Tier.AIRDROP && rng.nextDouble() < 0.15 && idx < slots.size()) {
            inv.setItem(slots.get(idx++), heavyMace());
        }

        for (; idx < count && idx < slots.size() && !pool.isEmpty(); idx++) {
            ItemStack item = pool.get(rng.nextInt(pool.size())).clone();
            inv.setItem(slots.get(idx), item);
        }
    }

    /**
     * Generates a 5-6 item burst of 1.21 combat supplies dropped when a Coin Crate breaks open.
     */
    public static List<ItemStack> createCrateBurst() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<ItemStack> pool = airdropPool(rng);
        int count = 5 + rng.nextInt(2);
        List<ItemStack> burst = new ArrayList<>(count);
        if (rng.nextDouble() < 0.15) {
            burst.add(heavyMace());
        }
        while (burst.size() < count && !pool.isEmpty()) {
            burst.add(pool.get(rng.nextInt(pool.size())).clone());
        }
        return burst;
    }

    private static List<ItemStack> outerPool(ThreadLocalRandom rng) {
        List<ItemStack> list = new ArrayList<>();
        // Weapons & 1.21 ranged options (no iron axes per MCC 31)
        list.add(new ItemStack(Material.WOODEN_SWORD));
        list.add(new ItemStack(Material.STONE_SWORD));
        list.add(new ItemStack(Material.STONE_AXE));
        list.add(new ItemStack(Material.BOW));
        list.add(new ItemStack(Material.CROSSBOW));
        list.add(enchantedCrossbow(Enchantment.QUICK_CHARGE, 1));
        list.add(enchantedCrossbow(Enchantment.PIERCING, 1));
        list.add(new ItemStack(Material.ARROW, 6 + rng.nextInt(7)));
        list.add(new ItemStack(Material.SPECTRAL_ARROW, 3 + rng.nextInt(4)));
        list.add(tippedArrow(PotionType.SLOWNESS, 2 + rng.nextInt(3)));
        list.add(tippedArrow(PotionType.WEAKNESS, 2 + rng.nextInt(3)));
        list.add(new ItemStack(Material.SNOWBALL, 4 + rng.nextInt(5))); // MCC 19
        list.add(new ItemStack(Material.SHIELD));

        // Armor variety (no iron chestplates per MCC 31)
        list.add(new ItemStack(Material.LEATHER_HELMET));
        list.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        list.add(new ItemStack(Material.LEATHER_LEGGINGS));
        list.add(new ItemStack(Material.GOLDEN_HELMET));
        list.add(new ItemStack(Material.GOLDEN_CHESTPLATE));
        list.add(new ItemStack(Material.GOLDEN_BOOTS));
        list.add(new ItemStack(Material.CHAINMAIL_HELMET));
        list.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        list.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        list.add(new ItemStack(Material.CHAINMAIL_BOOTS));
        list.add(new ItemStack(Material.IRON_HELMET));
        list.add(new ItemStack(Material.IRON_LEGGINGS));
        list.add(new ItemStack(Material.IRON_BOOTS));

        // 1.21 & MCC utility + food
        list.add(new ItemStack(Material.WIND_CHARGE, 3 + rng.nextInt(4)));
        list.add(new ItemStack(Material.CHORUS_FRUIT, 2 + rng.nextInt(2)));
        list.add(new ItemStack(Material.OAK_BOAT));
        list.add(new ItemStack(Material.COBWEB, 2 + rng.nextInt(2)));
        list.add(new ItemStack(Material.COOKED_BEEF, 2 + rng.nextInt(3)));
        list.add(new ItemStack(Material.BREAD, 3 + rng.nextInt(3)));
        list.add(new ItemStack(Material.GOLDEN_CARROT, 2 + rng.nextInt(3)));
        list.add(new ItemStack(Material.OAK_PLANKS, 6 + rng.nextInt(5)));
        list.add(new ItemStack(Material.COBBLESTONE, 6 + rng.nextInt(5)));
        list.add(new ItemStack(Material.LAPIS_LAZULI, 4 + rng.nextInt(5)));

        // Potions & books (no Fire Aspect)
        list.add(splashPotion(PotionType.HEALING));
        list.add(splashPotion(PotionType.HARMING));
        list.add(splashPotion(PotionType.SWIFTNESS));
        list.add(enchantedBook(Enchantment.SHARPNESS, 1));
        list.add(enchantedBook(Enchantment.PROTECTION, 1));
        list.add(enchantedBook(Enchantment.POWER, 1));
        list.add(enchantedBook(Enchantment.FEATHER_FALLING, 1));
        list.add(enchantedBook(Enchantment.QUICK_CHARGE, 1));
        return list;
    }

    private static List<ItemStack> cornucopiaPool(ThreadLocalRandom rng) {
        List<ItemStack> list = new ArrayList<>();
        // Strong center combat gear (still no iron chestplates or iron axes)
        list.add(new ItemStack(Material.IRON_SWORD));
        list.add(new ItemStack(Material.DIAMOND_AXE));
        list.add(new ItemStack(Material.BOW));
        list.add(enchantedCrossbow(Enchantment.QUICK_CHARGE, 2));
        list.add(enchantedCrossbow(Enchantment.MULTISHOT, 1));
        list.add(new ItemStack(Material.SHIELD));
        list.add(new ItemStack(Material.ARROW, 8 + rng.nextInt(7)));
        list.add(new ItemStack(Material.SPECTRAL_ARROW, 6));
        list.add(tippedArrow(PotionType.HARMING, 3 + rng.nextInt(2)));
        list.add(tippedArrow(PotionType.SLOWNESS, 3 + rng.nextInt(2)));
        list.add(new ItemStack(Material.SNOWBALL, 8));

        list.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        list.add(new ItemStack(Material.IRON_HELMET));
        list.add(new ItemStack(Material.IRON_LEGGINGS));
        list.add(new ItemStack(Material.IRON_BOOTS));
        list.add(new ItemStack(Material.DIAMOND_HELMET));
        list.add(new ItemStack(Material.DIAMOND_BOOTS));

        list.add(new ItemStack(Material.GOLDEN_APPLE));
        list.add(new ItemStack(Material.ENDER_PEARL, 1 + rng.nextInt(2)));
        list.add(new ItemStack(Material.WIND_CHARGE, 4 + rng.nextInt(5)));
        list.add(new ItemStack(Material.CHORUS_FRUIT, 2 + rng.nextInt(3)));
        list.add(tacticalTnt(1 + rng.nextInt(2)));
        list.add(new ItemStack(Material.COBWEB, 3));
        list.add(new ItemStack(Material.OAK_PLANKS, 10));
        list.add(new ItemStack(Material.COOKED_BEEF, 4));

        list.add(splashPotion(PotionType.HARMING));
        list.add(splashPotion(PotionType.HARMING)); // MCC 31: boosted splash harming
        list.add(splashPotion(PotionType.STRONG_HEALING));
        list.add(splashPotion(PotionType.SWIFTNESS));
        list.add(splashPotion(PotionType.REGENERATION));
        list.add(splashPotion(PotionType.WIND_CHARGED));

        list.add(enchantedBook(Enchantment.SHARPNESS, 2));
        list.add(enchantedBook(Enchantment.PROTECTION, 2));
        list.add(enchantedBook(Enchantment.POWER, 2));
        list.add(enchantedBook(Enchantment.PROJECTILE_PROTECTION, 2));
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
        list.add(enchantedCrossbow(Enchantment.MULTISHOT, 1));
        list.add(enchantedCrossbow(Enchantment.QUICK_CHARGE, 2));
        list.add(new ItemStack(Material.ARROW, 16));
        list.add(tippedArrow(PotionType.HARMING, 4));
        list.add(new ItemStack(Material.ENDER_PEARL, 2 + rng.nextInt(2)));
        list.add(new ItemStack(Material.WIND_CHARGE, 6 + rng.nextInt(5)));
        list.add(new ItemStack(Material.CHORUS_FRUIT, 3 + rng.nextInt(2)));
        list.add(tacticalTnt(2));
        list.add(splashPotion(PotionType.STRONG_HEALING));
        list.add(splashPotion(PotionType.HARMING));
        list.add(splashPotion(PotionType.SWIFTNESS));
        list.add(splashPotion(PotionType.WIND_CHARGED));
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

    private static ItemStack tippedArrow(PotionType type, int amount) {
        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW, amount);
        if (arrow.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            arrow.setItemMeta(meta);
        }
        return arrow;
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

    private static ItemStack enchantedCrossbow(Enchantment singleEnch, int level) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        crossbow.addUnsafeEnchantment(singleEnch, level);
        return crossbow;
    }

    private static ItemStack tacticalTnt(int amount) {
        ItemStack tnt = new ItemStack(Material.TNT, amount);
        ItemMeta meta = tnt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Tactical TNT");
            meta.setLore(List.of(ChatColor.GRAY + "Auto-ignites on place • No terrain grief"));
            tnt.setItemMeta(meta);
        }
        return tnt;
    }

    private static ItemStack heavyMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Heavy Mace");
            meta.setLore(List.of(ChatColor.GRAY + "Ultra-rare 1.21 smash weapon (capped damage)"));
            mace.setItemMeta(meta);
        }
        return mace;
    }
}
