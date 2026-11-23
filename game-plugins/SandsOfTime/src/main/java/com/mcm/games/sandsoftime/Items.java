package com.mcm.games.sandsoftime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Tagged game items. Every item the game hands out carries a "kind" tag. */
final class Items {
    static final String COIN = "coin", SAND = "sand", KEY = "key", GEAR = "gear", KIT = "kit";

    private static NamespacedKey KIND, VALUE, COLOR;

    private Items() {}

    static void init(Plugin p) {
        KIND = new NamespacedKey(p, "kind");
        VALUE = new NamespacedKey(p, "value");
        COLOR = new NamespacedKey(p, "color");
    }

    static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack make(Material m, int amount, String kind, String name, String lore) {
        ItemStack st = new ItemStack(m, Math.max(1, amount));
        ItemMeta meta = st.getItemMeta();
        meta.displayName(legacy(name));
        if (lore != null) meta.lore(List.of(legacy(lore)));
        meta.getPersistentDataContainer().set(KIND, PersistentDataType.STRING, kind);
        st.setItemMeta(meta);
        return st;
    }

    static ItemStack coin(Material m, int unitValue, int amount) {
        ItemStack st = make(m, amount, COIN, "§6Treasure §7(" + unitValue + " coins)", "§7Bank it at the Sphinx or carry it out.");
        ItemMeta meta = st.getItemMeta();
        meta.getPersistentDataContainer().set(VALUE, PersistentDataType.INTEGER, unitValue);
        st.setItemMeta(meta);
        return st;
    }

    static ItemStack sand(int amount) {
        return make(Material.SAND, amount, SAND, "§eSand of Time", "§7Right-click the hourglass: +10s. Also revives teammates.");
    }

    static ItemStack key(KeyColor c) {
        ItemStack st = make(Material.TRIPWIRE_HOOK, 1, KEY, c.keyName(), "§7Right-click the matching vault door.");
        ItemMeta meta = st.getItemMeta();
        meta.getPersistentDataContainer().set(COLOR, PersistentDataType.STRING, c.name());
        meta.setEnchantmentGlintOverride(true);
        st.setItemMeta(meta);
        return st;
    }

    static ItemStack gear(Material m) {
        return make(m, 1, GEAR, "§b" + pretty(m), null);
    }

    static ItemStack kit(Material m, int amount, String name) {
        return make(m, amount, KIT, name, null);
    }

    static ItemStack kitArmor(Material m, Color color, String name) {
        ItemStack st = kit(m, 1, name);
        if (st.getItemMeta() instanceof LeatherArmorMeta lm) {
            lm.setColor(color);
            st.setItemMeta(lm);
        }
        return st;
    }

    static String kind(ItemStack st) {
        if (st == null || !st.hasItemMeta()) return null;
        return st.getItemMeta().getPersistentDataContainer().get(KIND, PersistentDataType.STRING);
    }

    static int value(ItemStack st) {
        if (st == null || !st.hasItemMeta()) return 0;
        Integer v = st.getItemMeta().getPersistentDataContainer().get(VALUE, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    static KeyColor color(ItemStack st) {
        if (st == null || !st.hasItemMeta()) return null;
        String s = st.getItemMeta().getPersistentDataContainer().get(COLOR, PersistentDataType.STRING);
        if (s == null) return null;
        try { return KeyColor.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    static String pretty(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
