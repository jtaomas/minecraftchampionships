package com.mcm.games.skyblockle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Pure-Java model of loot.yml (no Bukkit types, so the offline loot check can use it). One {@link Table} per island
 * type / variant. A table has fixed slot items ({@code slots}), items that are always added ({@code fixed}) and
 * {@code categories} (armour, weapons, blocks, food, utility, books...) that each roll a number of distinct picks.
 * The dealing (who gets what, fairness between wedges) is {@link LootDeal}.
 */
public final class LootPool {

    /** One configurable item. {@code handle} is the runtime's built form (an ItemSpec), null offline. */
    public static final class Entry {
        public final String item;
        public final int min, max, weight;
        public final Map<String, Integer> enchants;
        public final String potion, name;
        public final Object handle;

        Entry(String item, int min, int max, int weight, Map<String, Integer> enchants, String potion, String name, Object handle) {
            this.item = item; this.min = min; this.max = max; this.weight = weight;
            this.enchants = enchants; this.potion = potion; this.name = name; this.handle = handle;
        }

        /** Identity for "same item" (material + stored enchantments + potion). */
        public String key() {
            StringBuilder sb = new StringBuilder(item);
            if (!enchants.isEmpty()) sb.append(new TreeMap<>(enchants));
            if (potion != null) sb.append('/').append(potion);
            return sb.toString();
        }

        /** helmet / chestplate / leggings / boots for armour pieces, else null. */
        public String armourSlot() { return LootPool.armourSlot(item); }

        @Override public String toString() { return key(); }
    }

    /** A category: {@code picks} distinct entries per chest (min..max). */
    public static final class Category {
        public final String name;
        public final int minPicks, maxPicks;
        public final List<Entry> entries;

        Category(String name, int minPicks, int maxPicks, List<Entry> entries) {
            this.name = name; this.minPicks = minPicks; this.maxPicks = maxPicks; this.entries = entries;
        }
    }

    public static final class Table {
        public final String name;
        /** Power tier (0 start/generator .. 4 castle), -1 if not given. */
        public final int tier;
        public final Map<Integer, Entry> slots;
        public final List<Entry> fixed;
        public final List<Category> categories;

        Table(String name, int tier, Map<Integer, Entry> slots, List<Entry> fixed, List<Category> categories) {
            this.name = name; this.tier = tier; this.slots = slots; this.fixed = fixed; this.categories = categories;
        }

        /** Every entry this table can produce. */
        public List<Entry> allEntries() {
            List<Entry> out = new ArrayList<>(slots.values());
            out.addAll(fixed);
            for (Category c : categories) out.addAll(c.entries);
            return out;
        }

        /** True if the table only has fixed contents (identical in every chest). */
        public boolean isFixed() { return categories.isEmpty(); }
    }

    /** One rolled stack: entry, amount and target slot (-1 = any empty slot). */
    public static final class Roll {
        public final Entry entry;
        public final int amount;
        public int slot;

        public Roll(Entry entry, int amount, int slot) { this.entry = entry; this.amount = amount; this.slot = slot; }

        Roll copy() { return new Roll(entry, amount, slot); }

        @Override public String toString() { return entry.key() + "x" + amount + "@" + slot; }
    }

    final Map<String, Table> tables = new LinkedHashMap<>();

    public Table table(String name) { return name == null ? null : tables.get(name.toLowerCase(Locale.ROOT)); }

    public Map<String, Table> all() { return Collections.unmodifiableMap(tables); }

    // ------------------------------------------------------------------ parsing

    /**
     * Parse the loot.yml root map. {@code resolve(raw, where)} validates an item entry and returns its runtime handle
     * (null = unusable, the entry is dropped); offline it can return any non-null object.
     */
    public static LootPool parse(Map<?, ?> root, BiFunction<Map<?, ?>, String, Object> resolve, Consumer<String> warn) {
        LootPool p = new LootPool();
        for (Map.Entry<?, ?> te : root.entrySet()) {
            String key = String.valueOf(te.getKey());
            if (!(te.getValue() instanceof Map<?, ?> s)) continue;
            String where = "loot.yml " + key;
            int tier = -1;
            if (s.get("tier") != null) tier = range(s.get("tier"), -1)[0];
            Map<Integer, Entry> slots = new LinkedHashMap<>();
            if (s.get("slots") instanceof Map<?, ?> ss) for (Map.Entry<?, ?> e : ss.entrySet()) {
                try {
                    int slot = slotOf(String.valueOf(e.getKey()));
                    Entry en = entry(e.getValue(), resolve, warn, where + ".slots." + e.getKey());
                    if (en != null) slots.put(slot, en);
                } catch (NumberFormatException ex) {
                    warn.accept(where + ": slot '" + e.getKey() + "' is not a number");
                }
            }
            List<Entry> fixed = list(s.get("fixed"), resolve, warn, where + ".fixed");
            List<Category> cats = new ArrayList<>();
            if (s.get("categories") instanceof Map<?, ?> cs) for (Map.Entry<?, ?> ce : cs.entrySet()) {
                if (!(ce.getValue() instanceof Map<?, ?> c)) continue;
                int[] picks = range(c.get("picks"), 1);
                List<Entry> items = list(c.get("items"), resolve, warn, where + ".categories." + ce.getKey());
                if (!items.isEmpty()) cats.add(new Category(String.valueOf(ce.getKey()), Math.max(0, picks[0]), Math.max(0, picks[1]), items));
            }
            // old format: rolls + items = one category
            if (s.get("items") != null) {
                int[] rolls = range(s.get("rolls"), 0);
                List<Entry> items = list(s.get("items"), resolve, warn, where + ".items");
                if (!items.isEmpty()) cats.add(new Category("items", Math.max(0, rolls[0]), Math.max(0, rolls[1]), items));
            }
            p.tables.put(key.toLowerCase(Locale.ROOT), new Table(key, tier, slots, fixed, cats));
        }
        return p;
    }

    private static List<Entry> list(Object raw, BiFunction<Map<?, ?>, String, Object> resolve, Consumer<String> warn, String where) {
        List<Entry> out = new ArrayList<>();
        if (raw instanceof List<?> l) for (Object o : l) {
            Entry e = entry(o, resolve, warn, where);
            if (e != null) out.add(e);
        }
        return out;
    }

    static Entry entry(Object raw, BiFunction<Map<?, ?>, String, Object> resolve, Consumer<String> warn, String where) {
        Map<?, ?> m;
        if (raw instanceof Map<?, ?> mm) m = mm;
        else if (raw instanceof String str) m = Map.of("item", str);
        else { warn.accept(where + ": item entry is not a map: " + raw); return null; }
        Object handle = resolve.apply(m, where);
        if (handle == null) return null;
        String item = String.valueOf(m.get("item")).trim().toLowerCase(Locale.ROOT);
        if (item.startsWith("minecraft:")) item = item.substring(10);
        int[] amt = range(m.get("amount"), 1);
        int weight = Math.max(0, range(m.get("weight"), 1)[0]);
        Map<String, Integer> ench = new LinkedHashMap<>();
        if (m.get("enchants") instanceof Map<?, ?> em)
            for (Map.Entry<?, ?> e : em.entrySet()) ench.put(String.valueOf(e.getKey()).toLowerCase(Locale.ROOT), Math.max(1, range(e.getValue(), 1)[0]));
        Object pot = m.get("potion"), nm = m.get("name");
        return new Entry(item, Math.max(1, amt[0]), Math.max(1, amt[1]), weight, ench,
                pot == null ? null : String.valueOf(pot).trim().toLowerCase(Locale.ROOT), nm == null ? null : String.valueOf(nm), handle);
    }

    /** "13" -> 13; "r2c5" (row 1-6, column 1-9, as in the video research) -> (r-1)*9 + (c-1). */
    public static int slotOf(String k) {
        String s = k.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("r") && s.contains("c")) {
            int c = s.indexOf('c');
            int row = Integer.parseInt(s.substring(1, c)), col = Integer.parseInt(s.substring(c + 1));
            if (row < 1 || col < 1 || col > 9) throw new NumberFormatException(k);
            return (row - 1) * 9 + (col - 1);
        }
        return Integer.parseInt(s);
    }

    /** "3", 3, "1-4", [1, 4] -> {min, max}. */
    public static int[] range(Object o, int def) {
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

    /** helmet / chestplate / leggings / boots for an armour item id, else null. */
    public static String armourSlot(String item) {
        if (item == null) return null;
        if (item.endsWith("_helmet")) return "helmet";
        if (item.endsWith("_chestplate") || item.equals("elytra")) return "chestplate";
        if (item.endsWith("_leggings")) return "leggings";
        if (item.endsWith("_boots")) return "boots";
        return null;
    }
}
