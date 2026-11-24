package com.mcm.games.bingo;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.*;

/**
 * Offline check (no server): for many random areas, plans the layout with a synthetic vanilla terrain (random oceans,
 * rivers and beaches), builds the env the game would use, generates Bingo and Lockout cards and verifies every card:
 * 25 distinct squares, all requirements met by the env, no two squares sharing a group, wood-only cap kept, tier mix
 * as configured (or filled from neighbouring tiers). Also checks every pool item is a real item.
 *
 * <p>Run: {@code java -cp target/classes;<paper-api and deps> com.mcm.games.bingo.CardCheck [areas]}
 */
public final class CardCheck {
    public static void main(String[] args) {
        int areas = args.length > 0 ? Integer.parseInt(args[0]) : 500;
        Settings cfg = new Settings();
        int failures = 0, cards = 0;
        int[] poolTier = new int[5];
        for (BingoItem it : BingoItem.POOL) {
            poolTier[it.tier]++;
            // Material.isItem() needs a running server's registry; block-only materials are caught by name instead.
            for (Material m : it.accepted) if (m.isLegacy() || m.name().startsWith("POTTED_") || m.name().endsWith("_WALL_SIGN")
                    || m.name().endsWith("_WALL_BANNER") || m.name().equals("KELP_PLANT")) { System.out.println("NOT AN ITEM: " + m); failures++; }
        }
        Set<Material> seen = new HashSet<>();
        for (BingoItem it : BingoItem.POOL) if (!seen.add(it.icon)) { System.out.println("DUPLICATE: " + it.icon); failures++; }
        System.out.printf("Pool: %d items (easy %d, medium %d, hard %d, very hard %d), wood-only %d%n", BingoItem.POOL.size(),
                poolTier[1], poolTier[2], poolTier[3], poolTier[4], BingoItem.POOL.stream().filter(i -> i.woodOnly).count());

        Random master = new Random(12345);
        long bingoPts = 0, lockPts = 0;
        int[] mixSeen = new int[5];
        Map<Integer, Integer> biomeCountHist = new TreeMap<>();
        Map<String, Integer> siteHist = new TreeMap<>();
        Map<Material, Integer> itemHist = new HashMap<>();
        for (int a = 0; a < areas; a++) {
            long seed = master.nextLong();
            AreaLayout.Sampler fake = fakeTerrain(seed, master.nextInt(4));
            AreaLayout l = AreaLayout.plan(1 + a, (1 + a) * BingoWorld.AREA_SPACING, 0, BingoWorld.BORDER / 2, seed, fake, true);
            Set<Biome> surfaces = EnumSet.noneOf(Biome.class);
            for (Biome b : l.biomes) if (!AreaLayout.isWater(b) && b != Biome.LUSH_CAVES && b != Biome.DRIPSTONE_CAVES) surfaces.add(b);
            biomeCountHist.merge(surfaces.size(), 1, Integer::sum);
            if (Math.abs(l.cx - (1 + a) * BingoWorld.AREA_SPACING) > 520 || Math.abs(l.cz) > 520) {
                System.out.println("CENTRE TOO FAR: " + l.describe()); failures++;
            }
            if (AreaLayout.isWater(fake.biome(l.cx, 64, l.cz))) { System.out.println("START IN WATER: " + l.describe()); failures++; }
            Set<String> structures = new LinkedHashSet<>(Env.ALWAYS_STRUCTURES);
            for (AreaLayout.Site s : l.sites) {
                structures.add(s.kind());
                siteHist.merge(s.kind(), 1, Integer::sum);
                if (Math.abs(s.x() - l.cx) + 16 * s.chunkRadius() > l.half || Math.abs(s.z() - l.cz) + 16 * s.chunkRadius() > l.half) {
                    System.out.println("SITE OUTSIDE BORDER: " + s + " in " + l.describe()); failures++;
                }
            }
            Env env = new Env(l.biomes, structures);
            for (int k = 0; k < 4; k++) {
                boolean lock = (k & 1) == 1;
                int[] mix = lock ? cfg.lockoutMix : cfg.bingoMix;
                List<BingoItem> card = BingoItem.generateCard(new Random(seed + k), env, mix, cfg.woodCap);
                cards++;
                String err = verify(card, env, mix, cfg.woodCap);
                if (err != null) { failures++; System.out.println("BAD CARD (" + err + ") env " + env); }
                int pts = 0;
                for (BingoItem it : card) {
                    pts += lock ? cfg.lockoutTierPoints[it.tier] : cfg.bingoTierPoints[it.tier];
                    mixSeen[it.tier]++;
                    itemHist.merge(it.icon, 1, Integer::sum);
                }
                if (lock) lockPts += pts; else bingoPts += pts;
            }
            if (a < 3) System.out.println(l.describe());
        }
        int half = cards / 2;
        System.out.printf("Areas %d, cards %d, failures %d%n", areas, cards, failures);
        System.out.printf("Surface biomes present per area: %s%n", biomeCountHist);
        System.out.printf("Structure sites planned: %s (of %d areas)%n", siteHist, areas);
        System.out.printf("Average full-card value: Bingo %.1f (all first finds), Lockout %.1f (all claims)%n",
                bingoPts / (double) half, lockPts / (double) half);
        System.out.printf("Tier mix over all cards: easy %d, medium %d, hard %d, very hard %d%n", mixSeen[1], mixSeen[2], mixSeen[3], mixSeen[4]);
        long never = BingoItem.POOL.stream().filter(i -> !itemHist.containsKey(i.icon)).count();
        System.out.printf("Pool items that never appeared: %d%n", never);
        BingoItem.POOL.stream().filter(i -> !itemHist.containsKey(i.icon)).forEach(i -> System.out.println("  never: " + i.name));
        System.exit(failures == 0 ? 0 : 1);
    }

    static String verify(List<BingoItem> card, Env env, int[] mix, int woodCap) {
        if (card.size() != 25) return "size " + card.size();
        if (new HashSet<>(card).size() != 25) return "duplicate squares";
        Set<String> groups = new HashSet<>();
        int wood = 0;
        int[] tiers = new int[5];
        for (BingoItem it : card) {
            if (!env.satisfies(it)) return "unsatisfiable " + it.name + " needs " + it.needs;
            for (String g : it.groups) if (!groups.add(g)) return "group clash " + g;
            if (it.woodOnly) wood++;
            tiers[it.tier]++;
        }
        if (wood > woodCap) return "wood " + wood;
        // Hard tiers may be short in a poor area (filled from lower tiers), never over-filled beyond what was asked plus shortfall.
        int asked = 0, have = 0;
        for (int t = 4; t >= 2; t--) { asked += mix[t]; have += tiers[t]; }
        if (have > asked + 0) return "too many hard items " + Arrays.toString(tiers);
        return null;
    }

    /** Synthetic vanilla: smooth noise decides ocean (deep/shallow), coast beaches and river valleys. */
    static AreaLayout.Sampler fakeTerrain(long seed, int wetness) {
        Random r = new Random(seed);
        double ox = r.nextDouble() * 10000, oz = r.nextDouble() * 10000, rx = r.nextDouble() * 10000, rz = r.nextDouble() * 10000;
        double oceanLevel = -0.75 + wetness * 0.2;
        return (x, y, z) -> {
            double c = smooth((x + ox) / 700.0, (z + oz) / 700.0);
            if (c < oceanLevel - 0.25) return Biome.DEEP_OCEAN;
            if (c < oceanLevel) return Biome.OCEAN;
            if (c < oceanLevel + 0.04) return Biome.BEACH;
            double w = smooth((x + rx) / 260.0, (z + rz) / 260.0);
            if (Math.abs(w) < 0.04) return Biome.RIVER;
            return Biome.PLAINS;
        };
    }

    private static double smooth(double x, double z) {
        return 0.6 * Math.sin(x * 1.7 + Math.cos(z * 1.3)) * Math.cos(z * 1.1 - Math.sin(x * 0.7)) + 0.4 * Math.sin((x + z) * 2.3);
    }
}
