import com.mcm.games.skyblockle.LootDeal;
import com.mcm.games.skyblockle.LootPool;
import com.mcm.games.skyblockle.MapModel;
import com.mcm.games.skyblockle.map.SkyMap;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Offline loot check (no Bukkit): loads loot.yml, deals the loot of real SkyMap maps for 2..10 wedges over many
 * seeds and checks: (1) every wedge's chests hold the identical multiset (and every shared slot's), (2) no chest has
 * two pieces for one armour slot, (3) the castle area has at least one helmet, chestplate, leggings and boots,
 * (4) tier ordering: no item above its chest's tier (diamond gear and level-II+ books castle only, iron armour
 * fort and up, golden apples tier 3+), and each island type's table tier matches the ring order.
 * Usage: java LootCheck path/to/loot.yml [seeds]. Exit code 2 on any failure.
 */
public final class LootCheck {
    static final Map<MapModel.IslandType, Integer> TYPE_TIER = new EnumMap<>(MapModel.IslandType.class);
    static {
        TYPE_TIER.put(MapModel.IslandType.START, 0);
        TYPE_TIER.put(MapModel.IslandType.GENERATOR, 0);
        TYPE_TIER.put(MapModel.IslandType.MID, 1);
        TYPE_TIER.put(MapModel.IslandType.FORT, 2);
        TYPE_TIER.put(MapModel.IslandType.BREWING, 3);
        TYPE_TIER.put(MapModel.IslandType.ENCHANTING, 3);
        TYPE_TIER.put(MapModel.IslandType.CASTLE, 4);
    }

    /** Lowest tier an item may appear in. */
    static int itemTier(LootPool.Entry e) {
        String i = e.item;
        if (i.startsWith("netherite_")) return 5;
        if (i.startsWith("diamond_")) return 4;
        if (i.equals("enchanted_book")) {
            int max = 0;
            for (int l : e.enchants.values()) max = Math.max(max, l);
            return max >= 2 ? 4 : max == 1 ? 3 : 0;
        }
        if (i.equals("golden_apple") || i.equals("enchanted_golden_apple")) return 3;
        if (i.startsWith("iron_") && e.armourSlot() != null) return 2;
        if (i.equals("iron_sword") || i.equals("iron_axe")) return 2;
        return 0;
    }

    static int errors = 0;
    static void err(String s) { errors++; if (errors <= 30) System.out.println("  ERROR " + s); }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "src/main/resources/loot.yml";
        int seeds = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        Map<?, ?> root;
        try (InputStream in = new FileInputStream(path)) { root = new Yaml().load(in); }
        LootPool pool = LootPool.parse(root, (raw, where) -> raw.get("item") == null ? null : Boolean.TRUE, w -> err("parse: " + w));

        // static tier check over every entry of every table
        for (Map.Entry<String, LootPool.Table> te : pool.all().entrySet()) {
            LootPool.Table t = te.getValue();
            if (te.getKey().equals("brewing_stand")) continue;
            if (t.tier < 0) { err("table " + te.getKey() + " has no tier"); continue; }
            for (LootPool.Entry e : t.allEntries())
                if (itemTier(e) > t.tier) err("table " + te.getKey() + " (tier " + t.tier + ") has " + e.key() + " (tier " + itemTier(e) + ")");
        }

        Map<String, Integer> castleSeen = new TreeMap<>();
        Map<String, Set<String>> variety = new TreeMap<>();
        int deals = 0, chestsChecked = 0;
        long maps = 0;
        // one map per wedge count (map loot does not depend on the map seed; the deal seed varies)
        for (int n = 2; n <= 10; n++) {
            MapModel m = SkyMap.generate(n, 20261123L + n);
            maps++;
            // table tier vs island ring order
            for (MapModel.ChestSpot c : m.chests) {
                String tn = LootDeal.tableFor(pool, m, c);
                LootPool.Table t = pool.table(tn);
                if (t == null) { err(n + " wedges: no table " + tn); continue; }
                if (t.tier != TYPE_TIER.get(c.type())) err("table " + tn + " tier " + t.tier + " but island type " + c.type() + " is tier " + TYPE_TIER.get(c.type()));
            }
            for (int s = 0; s < seeds; s++) {
                Random rnd = new Random(1000L * n + s);
                LootDeal.Result r = LootDeal.deal(pool, m, rnd, true);
                deals++;
                if (!r.unmatched.isEmpty()) err(n + " wedges seed " + s + ": unmatched areas " + r.unmatched);
                Map<String, List<String>> areaMultiset = new HashMap<>();
                Set<String> castleSlots = new HashSet<>();
                for (int i = 0; i < m.chests.size(); i++) {
                    MapModel.ChestSpot c = m.chests.get(i);
                    List<LootPool.Roll> rolls = r.rolls.get(i);
                    if (rolls == null) { err("chest " + i + " not dealt"); continue; }
                    chestsChecked++;
                    LootPool.Table t = pool.table(r.tables.get(i));
                    Set<String> arm = new HashSet<>(), slots = new HashSet<>();
                    List<String> ms = new ArrayList<>();
                    for (LootPool.Roll ro : rolls) {
                        String a = ro.entry.armourSlot();
                        if (a != null && !arm.add(a)) err(n + "w seed " + s + " chest " + r.tables.get(i) + " has two " + a + " pieces");
                        if (!slots.add(String.valueOf(ro.slot))) err("chest " + i + " two stacks in slot " + ro.slot);
                        if (ro.slot < 0 || ro.slot >= LootDeal.CHEST_SIZE) err("chest " + i + " bad slot " + ro.slot);
                        if (itemTier(ro.entry) > t.tier) err("rolled " + ro.entry.key() + " in tier " + t.tier + " chest " + r.tables.get(i));
                        ms.add(ro.entry.key() + "x" + ro.amount);
                        if (c.type() == MapModel.IslandType.CASTLE) {
                            if (a != null) castleSlots.add(a);
                            castleSeen.merge(ro.entry.key(), 1, Integer::sum);
                        }
                        if (n == 10) variety.computeIfAbsent(r.tables.get(i), k -> new HashSet<>()).add(ro.entry.key());
                    }
                    Collections.sort(ms);
                    String area = r.areas.get(i);
                    if (!area.equals("castle")) areaMultiset.computeIfAbsent(area, k -> new ArrayList<>()).add(r.tables.get(i) + ms);
                }
                // identical multisets per wedge, and per shared slot
                String wRef = null, sRef = null;
                for (Map.Entry<String, List<String>> e : areaMultiset.entrySet()) {
                    List<String> l = new ArrayList<>(e.getValue());
                    Collections.sort(l);
                    String v = l.toString();
                    if (e.getKey().startsWith("w")) { if (wRef == null) wRef = v; else if (!wRef.equals(v)) err(n + "w seed " + s + ": wedge " + e.getKey() + " differs"); }
                    else { if (sRef == null) sRef = v; else if (!sRef.equals(v)) err(n + "w seed " + s + ": shared " + e.getKey() + " differs"); }
                }
                long wAreas = areaMultiset.keySet().stream().filter(k -> k.startsWith("w")).count();
                if (wAreas != n) err(n + "w: " + wAreas + " wedge areas");
                for (String a : new String[]{"helmet", "chestplate", "leggings", "boots"})
                    if (!castleSlots.contains(a)) err(n + "w seed " + s + ": castle has no " + a);
            }
        }
        System.out.println("loot check: " + maps + " maps (2..10 wedges) x " + seeds + " deals = " + deals + " deals, " + chestsChecked + " chests");
        System.out.println("castle items over all deals: " + castleSeen);
        for (Map.Entry<String, Set<String>> e : variety.entrySet())
            System.out.println("  " + e.getKey() + ": " + e.getValue().size() + " different items seen");
        System.out.println(errors == 0 ? "RESULT: all loot checks passed" : "RESULT: " + errors + " problem(s)");
        if (errors > 0) System.exit(2);
    }
}
