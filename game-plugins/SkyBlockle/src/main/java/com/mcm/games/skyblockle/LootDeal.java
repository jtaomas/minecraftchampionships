package com.mcm.games.skyblockle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deals one game's chest contents (pure Java; used by the runtime and by the offline loot check).
 *
 * <p><b>Areas.</b> The chests are split into areas: each team wedge (wedge >= 0), each shared between-team slot
 * (wedge -1, by angle) and the castle (all CASTLE chests). Within one area every category pick is "least dealt
 * first": the entry handed out the fewest times so far in that area wins, ties broken by weight. So the four castle
 * chests share out the four diamond armour pieces before any piece repeats, and the chests of one wedge avoid
 * repeating each other's picks. Inside one chest no item and no armour slot (helmet/chestplate/leggings/boots) is
 * picked twice.
 *
 * <p><b>Fairness.</b> The first wedge is rolled and every other wedge gets an exact copy (same chest, same items,
 * same amounts, same slots: "same order", not mirrored). The shared between-team slots likewise: slot 0 is rolled
 * and copied to every slot, so both neighbours of any shared island see the same thing. Only the castle (one shared
 * area at the centre) is random per game with no copy. Everything changes from game to game.
 */
public final class LootDeal {
    private LootDeal() {}

    public static final int CHEST_SIZE = 27;

    /** Result: rolls per chest index (into {@code chests}) plus the table each chest used. */
    public static final class Result {
        public final Map<Integer, List<LootPool.Roll>> rolls = new HashMap<>();
        public final Map<Integer, String> tables = new HashMap<>();
        /** Area key per chest ("w3", "s1", "castle", "solo"). */
        public final Map<Integer, String> areas = new HashMap<>();
        /** Areas whose chests did not line up with the template area, so they were rolled on their own. */
        public final List<String> unmatched = new ArrayList<>();
    }

    /** The table for a chest: {@code <type>_<variant>} if the pool has it, else {@code <type>}. */
    public static String tableFor(LootPool pool, MapModel.IslandType type, String variant) {
        String base = type.name().toLowerCase(Locale.ROOT);
        if (variant != null && !variant.isBlank()) {
            String v = base + "_" + variant.toLowerCase(Locale.ROOT).trim();
            if (pool.table(v) != null) return v;
        }
        return base;
    }

    public static String tableFor(LootPool pool, MapModel m, MapModel.ChestSpot c) {
        return tableFor(pool, c.type(), c.variant() != null ? c.variant() : inferVariant(m, c));
    }

    /**
     * Variant for a chest without one: MID chests on a red concrete/wool column = "pillar", on sand/sandstone/
     * terracotta/granite = "spike", on grass/dirt/moss = "grass". Other types: null (plain table).
     */
    public static String inferVariant(MapModel m, MapModel.ChestSpot c) {
        if (c.type() != MapModel.IslandType.MID) return null;
        for (int dy = 1; dy <= 2; dy++) {
            String b = m.blocks.get(c.x(), c.y() - dy, c.z());
            if (b == null) continue;
            if (b.contains("red_concrete") || b.contains("red_wool") || b.contains("red_nether_brick") || b.contains("tnt")) return "pillar";
            if (b.contains("sand") || b.contains("terracotta") || b.contains("granite")) return "spike";
            if (b.contains("grass_block") || b.contains("dirt") || b.contains("moss")) return "grass";
            return null;
        }
        return null;
    }

    /** Shared slot of a neutral chest: slot i lies between wedge i and wedge i+1 (angle from +Z towards -X). */
    static int sharedSlot(MapModel.ChestSpot c, int wedges) {
        double a = Math.toDegrees(Math.atan2(-c.x(), c.z()));
        a = ((a % 360) + 360) % 360;
        return Math.floorMod((int) Math.floor(a / (360.0 / Math.max(1, wedges))), Math.max(1, wedges));
    }

    /** Deal every chest of a map. */
    public static Result deal(LootPool pool, MapModel m, Random rnd, boolean randomSlots) {
        Result res = new Result();
        int n = Math.max(1, m.wedges);
        // group chests into areas, keeping list order
        Map<String, List<Integer>> wedgeAreas = new LinkedHashMap<>(), slotAreas = new LinkedHashMap<>();
        List<Integer> castle = new ArrayList<>();
        for (int i = 0; i < m.chests.size(); i++) {
            MapModel.ChestSpot c = m.chests.get(i);
            res.tables.put(i, tableFor(pool, m, c));
            String area;
            if (c.type() == MapModel.IslandType.CASTLE) { castle.add(i); area = "castle"; }
            else if (c.wedge() >= 0) { area = "w" + c.wedge(); wedgeAreas.computeIfAbsent(area, k -> new ArrayList<>()).add(i); }
            else { area = "s" + sharedSlot(c, n); slotAreas.computeIfAbsent(area, k -> new ArrayList<>()).add(i); }
            res.areas.put(i, area);
        }
        dealCopied(pool, wedgeAreas, res, rnd, randomSlots);
        dealCopied(pool, slotAreas, res, rnd, randomSlots);
        if (!castle.isEmpty()) put(res, castle, rollArea(pool, tablesOf(res, castle), rnd, randomSlots));
        return res;
    }

    /** Roll the first area of the group and copy it to every area with the same table sequence. */
    private static void dealCopied(LootPool pool, Map<String, List<Integer>> areas, Result res, Random rnd, boolean randomSlots) {
        List<String> keys = new ArrayList<>(areas.keySet());
        keys.sort((a, b) -> Integer.compare(Integer.parseInt(a.substring(1)), Integer.parseInt(b.substring(1))));
        List<String> tplTables = null;
        List<List<LootPool.Roll>> tpl = null;
        for (String k : keys) {
            List<Integer> idx = areas.get(k);
            List<String> t = tablesOf(res, idx);
            if (tpl == null) {
                tpl = rollArea(pool, t, rnd, randomSlots);
                tplTables = t;
                put(res, idx, tpl);
            } else if (t.equals(tplTables)) {
                List<List<LootPool.Roll>> copy = new ArrayList<>();
                for (List<LootPool.Roll> l : tpl) {
                    List<LootPool.Roll> cl = new ArrayList<>();
                    for (LootPool.Roll r : l) cl.add(r.copy());
                    copy.add(cl);
                }
                put(res, idx, copy);
            } else {
                res.unmatched.add(k);
                put(res, idx, rollArea(pool, t, rnd, randomSlots));
            }
        }
    }

    private static List<String> tablesOf(Result res, List<Integer> idx) {
        List<String> out = new ArrayList<>();
        for (int i : idx) out.add(res.tables.get(i));
        return out;
    }

    private static void put(Result res, List<Integer> idx, List<List<LootPool.Roll>> rolls) {
        for (int k = 0; k < idx.size(); k++) res.rolls.put(idx.get(k), rolls.get(k));
    }

    /** One chest on its own (for /sbl loot and brewing stands). */
    public static List<LootPool.Roll> rollOne(LootPool pool, String table, Random rnd, boolean randomSlots) {
        return rollArea(pool, List.of(table), rnd, randomSlots).get(0);
    }

    /** Roll the chests of one area (tables in chest order), sharing the "least dealt first" counts. */
    static List<List<LootPool.Roll>> rollArea(LootPool pool, List<String> tableNames, Random rnd, boolean randomSlots) {
        Map<String, Integer> dealt = new HashMap<>();
        List<List<LootPool.Roll>> out = new ArrayList<>();
        for (String tn : tableNames) {
            LootPool.Table t = pool.table(tn);
            List<LootPool.Roll> chest = new ArrayList<>();
            out.add(chest);
            if (t == null) continue;
            Set<String> keys = new HashSet<>(), armour = new HashSet<>();
            Set<Integer> used = new HashSet<>();
            for (Map.Entry<Integer, LootPool.Entry> e : t.slots.entrySet()) {
                chest.add(new LootPool.Roll(e.getValue(), amount(e.getValue(), rnd), e.getKey()));
                used.add(e.getKey());
                note(e.getValue(), keys, armour, dealt);
            }
            List<LootPool.Roll> loose = new ArrayList<>();
            for (LootPool.Entry e : t.fixed) {
                loose.add(new LootPool.Roll(e, amount(e, rnd), -1));
                note(e, keys, armour, dealt);
            }
            for (LootPool.Category c : t.categories) {
                int picks = c.minPicks + (c.maxPicks > c.minPicks ? rnd.nextInt(c.maxPicks - c.minPicks + 1) : 0);
                for (int p = 0; p < picks; p++) {
                    LootPool.Entry e = draw(c, keys, armour, dealt, rnd);
                    if (e == null) break;
                    loose.add(new LootPool.Roll(e, amount(e, rnd), -1));
                    note(e, keys, armour, dealt);
                }
            }
            List<Integer> free = new ArrayList<>();
            for (int s = 0; s < CHEST_SIZE; s++) if (!used.contains(s)) free.add(s);
            if (randomSlots) Collections.shuffle(free, rnd);
            for (int k = 0; k < loose.size() && k < free.size(); k++) {
                loose.get(k).slot = free.get(k);
                chest.add(loose.get(k));
            }
        }
        return out;
    }

    private static void note(LootPool.Entry e, Set<String> keys, Set<String> armour, Map<String, Integer> dealt) {
        keys.add(e.key());
        String a = e.armourSlot();
        if (a != null) armour.add(a);
        dealt.merge(e.key(), 1, Integer::sum);
    }

    /**
     * Least-dealt-first weighted draw. Never an armour slot the chest already has; avoids an item the chest already
     * has unless the category has nothing else left.
     */
    private static LootPool.Entry draw(LootPool.Category c, Set<String> keys, Set<String> armour, Map<String, Integer> dealt, Random rnd) {
        List<LootPool.Entry> cand = new ArrayList<>();
        for (LootPool.Entry e : c.entries)
            if (e.weight > 0 && !keys.contains(e.key()) && (e.armourSlot() == null || !armour.contains(e.armourSlot()))) cand.add(e);
        if (cand.isEmpty())
            for (LootPool.Entry e : c.entries)
                if (e.weight > 0 && e.armourSlot() == null) cand.add(e);
        if (cand.isEmpty()) return null;
        int min = Integer.MAX_VALUE;
        for (LootPool.Entry e : cand) min = Math.min(min, dealt.getOrDefault(e.key(), 0));
        List<LootPool.Entry> best = new ArrayList<>();
        int total = 0;
        for (LootPool.Entry e : cand) if (dealt.getOrDefault(e.key(), 0) == min) { best.add(e); total += e.weight; }
        int x = rnd.nextInt(total);
        for (LootPool.Entry e : best) { x -= e.weight; if (x < 0) return e; }
        return best.get(best.size() - 1);
    }

    private static int amount(LootPool.Entry e, Random r) {
        return e.min + (e.max > e.min ? r.nextInt(e.max - e.min + 1) : 0);
    }
}
