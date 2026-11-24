package com.mcm.games.skyblockle;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * loot.yml (Bukkit side): parses the file into a pure {@link LootPool} (items validated through {@link ItemSpec}),
 * builds item stacks and puts rolled stacks into inventories. Dealing is {@link LootDeal}.
 */
final class LootTables {
    private LootPool pool = new LootPool();

    static LootTables load(File file, Logger log) {
        LootTables lt = new LootTables();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> root = toMap(y);
        lt.pool = LootPool.parse(root, (raw, where) -> ItemSpec.parse(raw, log, where), msg -> log.warning("Sky Blockle: " + msg));
        for (MapModel.IslandType t : MapModel.IslandType.values()) {
            if (lt.pool.table(t.name().toLowerCase(Locale.ROOT)) == null)
                log.warning("Sky Blockle: loot.yml has no table for island type '" + t.name().toLowerCase(Locale.ROOT) + "' (its chests stay empty)");
        }
        return lt;
    }

    /** ConfigurationSection -> plain nested maps/lists (sections inside lists come back as maps already). */
    static Map<String, Object> toMap(ConfigurationSection s) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : s.getKeys(false)) out.put(k, plain(s.get(k)));
        return out;
    }

    private static Object plain(Object o) {
        if (o instanceof ConfigurationSection cs) return toMap(cs);
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), plain(e.getValue()));
            return out;
        }
        if (o instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) out.add(plain(x));
            return out;
        }
        return o;
    }

    LootPool pool() { return pool; }

    String tableFor(MapModel.IslandType type, String variant) { return LootDeal.tableFor(pool, type, variant); }

    LootPool.Table table(String name) { return pool.table(name); }

    Map<String, LootPool.Table> all() { return pool.all(); }

    static ItemStack build(LootPool.Roll r, Random rnd) {
        if (!(r.entry.handle instanceof ItemSpec spec)) return null;
        return spec.build(r.amount);
    }

    /**
     * Put rolled stacks into an inventory: each into its slot if that is empty, else into the first empty slot.
     * Existing items are kept (refills). Returns the number of stacks added.
     */
    static int put(Inventory inv, List<LootPool.Roll> rolls, Random rnd) {
        if (inv == null || rolls == null) return 0;
        int added = 0;
        List<ItemStack> overflow = new ArrayList<>();
        for (LootPool.Roll r : rolls) {
            ItemStack st = build(r, rnd);
            if (st == null) continue;
            if (r.slot >= 0 && r.slot < inv.getSize() && empty(inv.getItem(r.slot))) { inv.setItem(r.slot, st); added++; }
            else overflow.add(st);
        }
        for (ItemStack st : overflow) {
            for (int i = 0; i < inv.getSize(); i++) if (empty(inv.getItem(i))) { inv.setItem(i, st); added++; break; }
        }
        return added;
    }

    private static boolean empty(ItemStack s) { return s == null || s.getType().isAir(); }

    /** Fill one inventory with a single-chest roll of a table (/sbl loot, brewing stands). */
    int fill(Inventory inv, String tableName, Random r, boolean randomSlots) {
        if (inv == null || pool.table(tableName) == null) return 0;
        return put(inv, LootDeal.rollOne(pool, tableName, r, randomSlots), r);
    }
}
