package com.mcm.games.skyblockle;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/** loot.yml: one weighted table per island type (start, generator, mid, fort, brewing, enchanting, castle). */
final class LootTables {
    static final class Table {
        final String name;
        final int minRolls, maxRolls;
        final List<ItemSpec> fixed;
        final List<ItemSpec> items;
        final Map<Integer, ItemSpec> slots;
        final int totalWeight;

        Table(String name, int minRolls, int maxRolls, List<ItemSpec> fixed, List<ItemSpec> items, Map<Integer, ItemSpec> slots) {
            this.name = name;
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.fixed = fixed;
            this.items = items;
            this.slots = slots;
            int t = 0;
            for (ItemSpec s : items) t += s.weight;
            this.totalWeight = t;
        }

        ItemSpec pick(Random r) {
            if (totalWeight <= 0) return null;
            int x = r.nextInt(totalWeight);
            for (ItemSpec s : items) { x -= s.weight; if (x < 0) return s; }
            return null;
        }
    }

    private final Map<String, Table> tables = new LinkedHashMap<>();

    static LootTables load(File file, Logger log) {
        LootTables lt = new LootTables();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) continue;
            String where = "loot.yml " + key;
            int[] rolls = ItemSpec.range(s.get("rolls"), 0);
            List<ItemSpec> fixed = ItemSpec.parseList(s.getList("fixed"), log, where + ".fixed");
            List<ItemSpec> items = ItemSpec.parseList(s.getList("items"), log, where + ".items");
            Map<Integer, ItemSpec> slots = new LinkedHashMap<>();
            ConfigurationSection ss = s.getConfigurationSection("slots");
            if (ss != null) for (String k : ss.getKeys(false)) {
                try {
                    int slot = slotOf(k);
                    Object raw = ss.get(k);
                    if (raw instanceof ConfigurationSection cs) raw = cs.getValues(false);
                    ItemSpec spec = ItemSpec.parse(raw, log, where + ".slots." + k);
                    if (spec != null) slots.put(slot, spec);
                } catch (NumberFormatException e) {
                    log.warning("Sky Blockle: " + where + ": slot '" + k + "' is not a number");
                }
            }
            lt.tables.put(key.toLowerCase(Locale.ROOT), new Table(key, Math.max(0, rolls[0]), Math.max(0, rolls[1]), fixed, items, slots));
        }
        for (MapModel.IslandType t : MapModel.IslandType.values()) {
            if (!lt.tables.containsKey(t.name().toLowerCase(Locale.ROOT)))
                log.warning("Sky Blockle: loot.yml has no table for island type '" + t.name().toLowerCase(Locale.ROOT) + "' (its chests stay empty)");
        }
        return lt;
    }

    /** "13" -> 13; "r2c5" (row 1-6, column 1-9, as in the video research) -> (r-1)*9 + (c-1). */
    static int slotOf(String k) {
        String s = k.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("r") && s.contains("c")) {
            int c = s.indexOf('c');
            int row = Integer.parseInt(s.substring(1, c)), col = Integer.parseInt(s.substring(c + 1));
            if (row < 1 || col < 1 || col > 9) throw new NumberFormatException(k);
            return (row - 1) * 9 + (col - 1);
        }
        return Integer.parseInt(s);
    }

    /** The table for a chest: {@code <type>_<variant>} if loot.yml has it, else {@code <type>}. */
    String tableFor(MapModel.IslandType type, String variant) {
        String base = type.name().toLowerCase(Locale.ROOT);
        if (variant != null && !variant.isBlank()) {
            String v = base + "_" + variant.toLowerCase(Locale.ROOT).trim();
            if (tables.containsKey(v)) return v;
        }
        return base;
    }

    Table table(String name) { return name == null ? null : tables.get(name.toLowerCase(Locale.ROOT)); }

    Map<String, Table> all() { return Collections.unmodifiableMap(tables); }

    /**
     * Fill an inventory from a table: fixed items + N weighted picks, put into random empty slots (or the first
     * empty slots). Existing items are kept (so it can also serve as a refill). Returns the number of stacks added.
     */
    int fill(Inventory inv, String tableName, Random r, boolean randomSlots) {
        Table t = table(tableName);
        if (t == null || inv == null) return 0;
        int added = 0;
        for (Map.Entry<Integer, ItemSpec> e : t.slots.entrySet()) {
            int slot = e.getKey();
            if (slot >= 0 && slot < inv.getSize()) {
                ItemStack cur = inv.getItem(slot);
                if (cur == null || cur.getType().isAir()) { inv.setItem(slot, e.getValue().build(r)); added++; }
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemSpec s : t.fixed) stacks.add(s.build(r));
        int rolls = t.minRolls + (t.maxRolls > t.minRolls ? r.nextInt(t.maxRolls - t.minRolls + 1) : 0);
        for (int i = 0; i < rolls; i++) {
            ItemSpec s = t.pick(r);
            if (s != null) stacks.add(s.build(r));
        }
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType().isAir()) empty.add(i);
        }
        if (randomSlots) Collections.shuffle(empty, r);
        int k = 0;
        for (ItemStack st : stacks) {
            if (k >= empty.size()) break;
            inv.setItem(empty.get(k++), st);
            added++;
        }
        return added;
    }
}
