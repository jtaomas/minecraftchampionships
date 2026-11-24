package com.mcm.games.skyblockle;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/** One configurable item: {item, amount, weight, enchants, potion, name}. Shared by loot.yml and start-inventory. */
final class ItemSpec {
    final Material material;
    final int minAmount, maxAmount;
    final int weight;
    final Map<Enchantment, Integer> enchants;
    final PotionType potion;
    final String name;

    private ItemSpec(Material material, int minAmount, int maxAmount, int weight, Map<Enchantment, Integer> enchants,
                     PotionType potion, String name) {
        this.material = material;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.weight = weight;
        this.enchants = enchants;
        this.potion = potion;
        this.name = name;
    }

    /** Parse one entry; returns null (and logs) if it is unusable. */
    static ItemSpec parse(Object raw, Logger log, String where) {
        if (!(raw instanceof Map<?, ?> m)) {
            if (raw instanceof String s) return parse(Map.of("item", s), log, where);
            log.warning("Sky Blockle: " + where + ": item entry is not a map: " + raw);
            return null;
        }
        Object itemName = m.get("item");
        Material mat = itemName == null ? null : Material.matchMaterial(String.valueOf(itemName));
        if (mat == null || !mat.isItem() || mat.isAir()) {
            log.warning("Sky Blockle: " + where + ": unknown item '" + itemName + "'");
            return null;
        }
        int[] amt = range(m.get("amount"), 1);
        int max = Math.max(1, Math.min(mat.getMaxStackSize(), amt[1]));
        int min = Math.max(1, Math.min(max, amt[0]));
        int weight = 1;
        Object w = m.get("weight");
        if (w instanceof Number n) weight = Math.max(0, n.intValue());
        else if (w != null) try { weight = Math.max(0, Integer.parseInt(String.valueOf(w).trim())); } catch (NumberFormatException ignored) {}

        Map<Enchantment, Integer> ench = new LinkedHashMap<>();
        if (m.get("enchants") instanceof Map<?, ?> em) {
            for (Map.Entry<?, ?> e : em.entrySet()) {
                String key = String.valueOf(e.getKey()).toLowerCase(Locale.ROOT);
                NamespacedKey nk = key.contains(":") ? NamespacedKey.fromString(key) : NamespacedKey.minecraft(key);
                Enchantment en = nk == null ? null : Registry.ENCHANTMENT.get(nk);
                if (en == null) { log.warning("Sky Blockle: " + where + ": unknown enchantment '" + key + "'"); continue; }
                int lvl = 1;
                if (e.getValue() instanceof Number n) lvl = n.intValue();
                else try { lvl = Integer.parseInt(String.valueOf(e.getValue()).trim()); } catch (NumberFormatException ignored) {}
                ench.put(en, Math.max(1, lvl));
            }
        }
        PotionType pt = null;
        Object p = m.get("potion");
        if (p != null) {
            try { pt = PotionType.valueOf(String.valueOf(p).trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { log.warning("Sky Blockle: " + where + ": unknown potion '" + p + "'"); }
        }
        Object nm = m.get("name");
        return new ItemSpec(mat, min, max, weight, ench, pt, nm == null ? null : String.valueOf(nm));
    }

    static List<ItemSpec> parseList(Object raw, Logger log, String where) {
        List<ItemSpec> out = new ArrayList<>();
        if (raw instanceof List<?> l) for (Object o : l) {
            ItemSpec s = parse(o, log, where);
            if (s != null) out.add(s);
        }
        return out;
    }

    /** "3", 3, "1-4", [1, 4] -> {min, max}. */
    static int[] range(Object o, int def) {
        if (o == null) return new int[]{def, def};
        if (o instanceof Number n) return new int[]{n.intValue(), n.intValue()};
        if (o instanceof List<?> l && !l.isEmpty()) {
            int a = toInt(l.get(0), def), b = l.size() > 1 ? toInt(l.get(1), a) : a;
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        String s = String.valueOf(o).trim();
        int dash = s.indexOf('-', 1);
        if (dash > 0) {
            int a = toInt(s.substring(0, dash), def), b = toInt(s.substring(dash + 1), a);
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        int v = toInt(s, def);
        return new int[]{v, v};
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch (NumberFormatException e) { return def; }
    }

    ItemStack build(Random r) {
        return build(minAmount + (maxAmount > minAmount ? r.nextInt(maxAmount - minAmount + 1) : 0));
    }

    /** Build with a given amount (clamped to 1..max stack size). */
    @SuppressWarnings("deprecation")
    ItemStack build(int amount) {
        amount = Math.max(1, Math.min(material.getMaxStackSize(), amount));
        ItemStack it = new ItemStack(material, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (!enchants.isEmpty()) {
                if (meta instanceof EnchantmentStorageMeta esm) {
                    for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) esm.addStoredEnchant(e.getKey(), e.getValue(), true);
                } else {
                    for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) meta.addEnchant(e.getKey(), e.getValue(), true);
                }
            }
            if (potion != null && meta instanceof PotionMeta pm) pm.setBasePotionType(potion);
            if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            it.setItemMeta(meta);
        }
        return it;
    }
}
