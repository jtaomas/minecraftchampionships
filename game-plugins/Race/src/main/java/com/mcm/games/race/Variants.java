package com.mcm.games.race;

import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.VariantGroup;
import com.mcm.games.race.model.TgMapData.VariantRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Picks one option per variant group, honouring the map's min/max rules (e.g. >= 4 of 12 turnstiles OPEN). */
final class Variants {
    private Variants() {}

    /** Fallback guarantee if a turnstile map declares no OPEN rule (spec: >= 4 of 12 open). */
    static final int MIN_OPEN_TURNSTILES = 4;

    static Map<String, String> pick(TgMapData d, Random rnd) {
        Map<String, String> out = new LinkedHashMap<>();
        for (VariantGroup g : d.variants) {
            if (g.options.isEmpty()) continue;
            int total = 0;
            for (String o : g.options.keySet()) total += Math.max(0, g.weights.getOrDefault(o, 1));
            String chosen = g.options.keySet().iterator().next();
            if (total > 0) {
                int r = rnd.nextInt(total);
                for (String o : g.options.keySet()) {
                    r -= Math.max(0, g.weights.getOrDefault(o, 1));
                    if (r < 0) { chosen = o; break; }
                }
            }
            out.put(g.name, chosen);
        }
        List<VariantRule> rules = new ArrayList<>(d.variantRules);
        boolean hasTurnstileRule = false;
        for (VariantRule r : rules) if (r.category().equalsIgnoreCase("turnstile") && r.option().equalsIgnoreCase("OPEN")) hasTurnstileRule = true;
        if (!hasTurnstileRule) {
            for (VariantGroup g : d.variants) {
                if (g.category.equalsIgnoreCase("turnstile") && g.options.containsKey("OPEN")) {
                    rules.add(new VariantRule(g.category, "OPEN", MIN_OPEN_TURNSTILES, -1));
                    break;
                }
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (VariantRule rule : rules) {
                List<VariantGroup> with = new ArrayList<>(), without = new ArrayList<>();
                for (VariantGroup g : d.variants) {
                    if (!rule.category().equals(g.category) || !g.options.containsKey(rule.option())) continue;
                    if (rule.option().equals(out.get(g.name))) with.add(g); else without.add(g);
                }
                Collections.shuffle(without, rnd);
                Collections.shuffle(with, rnd);
                while (with.size() < rule.minCount() && !without.isEmpty()) {
                    VariantGroup g = without.remove(without.size() - 1);
                    out.put(g.name, rule.option());
                    with.add(g);
                }
                while (rule.maxCount() >= 0 && with.size() > rule.maxCount()) {
                    VariantGroup g = with.remove(with.size() - 1);
                    List<String> others = new ArrayList<>(g.options.keySet());
                    others.remove(rule.option());
                    if (others.isEmpty()) break;
                    out.put(g.name, others.get(rnd.nextInt(others.size())));
                }
            }
        }
        return out;
    }

    /** Count of groups per option in a category, e.g. "OPEN=5 JAMMED=4 DROP=3". */
    static String summary(TgMapData d, Map<String, String> choice) {
        Map<String, Map<String, Integer>> byCat = new LinkedHashMap<>();
        for (VariantGroup g : d.variants) {
            String o = choice.get(g.name);
            if (o == null) continue;
            byCat.computeIfAbsent(g.category, k -> new LinkedHashMap<>()).merge(o, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Integer>> e : byCat.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append(": ");
            e.getValue().forEach((k, v) -> sb.append(k).append('=').append(v).append(' '));
        }
        return sb.toString().trim();
    }
}
