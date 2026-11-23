package com.mcm.games.bingo;

import org.bukkit.Material;

import java.util.*;
import java.util.function.Predicate;

/**
 * One square on a bingo card: a display icon, the set of materials that satisfy it,
 * a difficulty tier (1 easy .. 3 hard) and an optional exclusion group so near-duplicate
 * squares (e.g. bucket / water bucket / milk bucket) never land on the same card.
 */
public final class BingoItem {
    public final String name;
    public final Material icon;
    public final Set<Material> accepted;
    public final int tier;
    public final String group;
    public final boolean anyVariant;

    private BingoItem(String name, Material icon, Set<Material> accepted, int tier, String group, boolean anyVariant) {
        this.name = name;
        this.icon = icon;
        this.accepted = accepted;
        this.tier = tier;
        this.group = group;
        this.anyVariant = anyVariant;
    }

    public boolean matches(Set<Material> held) {
        for (Material m : accepted) if (held.contains(m)) return true;
        return false;
    }

    // ------------------------------------------------------------------ pool

    public static final List<BingoItem> POOL = new ArrayList<>();

    private static void one(int tier, String name, Material m) { one(tier, name, m, null); }

    private static void one(int tier, String name, Material m, String group) {
        POOL.add(new BingoItem(name, m, Set.of(m), tier, group, false));
    }

    private static void many(int tier, String name, Material icon, String group, Material... ms) {
        POOL.add(new BingoItem(name, icon, Set.of(ms), tier, group, true));
    }

    private static void anyOf(int tier, String name, Material icon, String group, Predicate<String> namePred) {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.startsWith("LEGACY_")) continue;
            if (!m.isItem()) continue;
            if (namePred.test(n)) set.add(m);
        }
        set.add(icon);
        POOL.add(new BingoItem(name, icon, Collections.unmodifiableSet(set), tier, group, true));
    }

    static {
        // ---------- tier 1: a minute or two of normal survival
        one(1, "Furnace", Material.FURNACE);
        one(1, "Chest", Material.CHEST);
        anyOf(1, "Any Boat", Material.OAK_BOAT, null, n -> n.endsWith("_BOAT") || n.endsWith("_RAFT"));
        one(1, "Bowl", Material.BOWL);
        one(1, "Ladder", Material.LADDER);
        one(1, "Torch", Material.TORCH);
        one(1, "Flint", Material.FLINT, "flint");
        one(1, "Stone Hoe", Material.STONE_HOE);
        anyOf(1, "Any Sapling", Material.OAK_SAPLING, null,
                n -> n.endsWith("_SAPLING") || n.equals("MANGROVE_PROPAGULE"));
        one(1, "Wheat Seeds", Material.WHEAT_SEEDS);
        one(1, "Charcoal", Material.CHARCOAL);
        one(1, "Cobblestone Wall", Material.COBBLESTONE_WALL);
        anyOf(1, "Any Wooden Door", Material.OAK_DOOR, null,
                n -> n.endsWith("_DOOR") && !n.contains("IRON") && !n.contains("COPPER"));
        anyOf(1, "Any Wooden Trapdoor", Material.OAK_TRAPDOOR, null,
                n -> n.endsWith("_TRAPDOOR") && !n.contains("IRON") && !n.contains("COPPER"));
        anyOf(1, "Any Fence Gate", Material.OAK_FENCE_GATE, null, n -> n.endsWith("_FENCE_GATE"));
        one(1, "Barrel", Material.BARREL);
        one(1, "Composter", Material.COMPOSTER);
        one(1, "Lever", Material.LEVER);
        one(1, "Stone Button", Material.STONE_BUTTON);
        anyOf(1, "Any Sign", Material.OAK_SIGN, null, n -> n.endsWith("_SIGN") && !n.contains("WALL"));
        one(1, "Coal", Material.COAL);
        one(1, "Iron Ingot", Material.IRON_INGOT);
        one(1, "Copper Ingot", Material.COPPER_INGOT, "copper");
        anyOf(1, "Any Flower", Material.POPPY, null, n -> switch (n) {
            case "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET", "RED_TULIP", "ORANGE_TULIP",
                 "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY", "CORNFLOWER", "LILY_OF_THE_VALLEY",
                 "SUNFLOWER", "LILAC", "ROSE_BUSH", "PEONY" -> true;
            default -> false;
        });
        one(1, "Grindstone", Material.GRINDSTONE);
        one(1, "Smoker", Material.SMOKER);
        one(1, "Campfire", Material.CAMPFIRE);
        anyOf(1, "Any Wool", Material.WHITE_WOOL, "wool", n -> n.endsWith("_WOOL"));
        one(1, "Shears", Material.SHEARS);
        one(1, "Bucket", Material.BUCKET, "bucket");
        one(1, "Water Bucket", Material.WATER_BUCKET, "bucket");
        one(1, "Shield", Material.SHIELD);
        one(1, "Lantern", Material.LANTERN, "nugget");
        one(1, "Stonecutter", Material.STONECUTTER);
        one(1, "Fletching Table", Material.FLETCHING_TABLE, "flint");
        one(1, "Smithing Table", Material.SMITHING_TABLE);
        one(1, "Tripwire Hook", Material.TRIPWIRE_HOOK);
        one(1, "Heavy Pressure Plate", Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        one(1, "Flint and Steel", Material.FLINT_AND_STEEL, "flint");
        one(1, "Stone Bricks", Material.STONE_BRICKS);
        many(1, "Any Polished Stone", Material.POLISHED_ANDESITE, null,
                Material.POLISHED_ANDESITE, Material.POLISHED_DIORITE, Material.POLISHED_GRANITE);
        one(1, "Glass", Material.GLASS, "glass");
        one(1, "Feather", Material.FEATHER, "feather");

        // ---------- tier 2: needs a mob, a cave trip or a short crafting chain
        anyOf(2, "Any Bed", Material.RED_BED, "wool", n -> n.endsWith("_BED"));
        one(2, "Milk Bucket", Material.MILK_BUCKET, "bucket");
        many(2, "Cooked Fish", Material.COOKED_COD, "fish", Material.COOKED_COD, Material.COOKED_SALMON);
        one(2, "Fishing Rod", Material.FISHING_ROD, "string");
        one(2, "Bow", Material.BOW, "string");
        one(2, "Arrow", Material.ARROW, "feather");
        one(2, "Leather", Material.LEATHER, "leather");
        one(2, "Paper", Material.PAPER, "paper");
        one(2, "Sugar", Material.SUGAR, "paper");
        one(2, "Bread", Material.BREAD, "wheat");
        one(2, "Cooked Chicken", Material.COOKED_CHICKEN);
        one(2, "Cooked Porkchop", Material.COOKED_PORKCHOP);
        one(2, "Cooked Mutton", Material.COOKED_MUTTON);
        one(2, "Egg", Material.EGG);
        one(2, "Bone Meal", Material.BONE_MEAL, "bone");
        one(2, "String", Material.STRING, "string");
        one(2, "Rotten Flesh", Material.ROTTEN_FLESH);
        one(2, "Gold Ingot", Material.GOLD_INGOT, "gold");
        one(2, "Compass", Material.COMPASS, "compass");
        one(2, "Redstone Dust", Material.REDSTONE, "redstone");
        one(2, "Lapis Lazuli", Material.LAPIS_LAZULI);
        one(2, "Iron Helmet", Material.IRON_HELMET, "armor");
        one(2, "Iron Boots", Material.IRON_BOOTS, "armor");
        one(2, "Minecart", Material.MINECART, "rail");
        one(2, "Rail", Material.RAIL, "rail");
        one(2, "Hopper", Material.HOPPER);
        one(2, "Cauldron", Material.CAULDRON);
        one(2, "Chain", Material.CHAIN, "nugget");
        one(2, "Piston", Material.PISTON, "redstone");
        one(2, "Note Block", Material.NOTE_BLOCK, "redstone");
        one(2, "Flower Pot", Material.FLOWER_POT, "clay");
        one(2, "Brick", Material.BRICK, "clay");
        one(2, "Glass Bottle", Material.GLASS_BOTTLE, "glass");
        one(2, "Smooth Stone", Material.SMOOTH_STONE, "smooth");
        one(2, "Armor Stand", Material.ARMOR_STAND, "smooth");
        one(2, "Loom", Material.LOOM, "string");
        one(2, "Iron Bars", Material.IRON_BARS);
        one(2, "Painting", Material.PAINTING, "wool");
        one(2, "Item Frame", Material.ITEM_FRAME, "leather");
        one(2, "Redstone Torch", Material.REDSTONE_TORCH, "redstone");
        one(2, "Lightning Rod", Material.LIGHTNING_ROD, "copper");
        one(2, "Brush", Material.BRUSH, "copper");
        one(2, "Cobbled Deepslate", Material.COBBLED_DEEPSLATE);
        one(2, "Tuff", Material.TUFF);
        one(2, "Apple", Material.APPLE, "apple");
        one(2, "Leather Boots", Material.LEATHER_BOOTS, "leather");
        one(2, "Cartography Table", Material.CARTOGRAPHY_TABLE, "paper");
        one(2, "Sandstone", Material.SANDSTONE);
        one(2, "Bone", Material.BONE, "bone");

        // ---------- tier 3: a real detour; at most a handful per card
        one(3, "Diamond", Material.DIAMOND);
        one(3, "Golden Apple", Material.GOLDEN_APPLE, "apple");
        one(3, "Clock", Material.CLOCK, "gold");
        one(3, "Map", Material.MAP, "compass");
        one(3, "Book", Material.BOOK, "paper");
        one(3, "Bookshelf", Material.BOOKSHELF, "paper");
        one(3, "Gunpowder", Material.GUNPOWDER, "gunpowder");
        one(3, "TNT", Material.TNT, "gunpowder");
        one(3, "Firework Rocket", Material.FIREWORK_ROCKET, "gunpowder");
        one(3, "Spider Eye", Material.SPIDER_EYE);
        one(3, "Mushroom Stew", Material.MUSHROOM_STEW);
        one(3, "Amethyst Shard", Material.AMETHYST_SHARD);
        one(3, "Blast Furnace", Material.BLAST_FURNACE, "smooth");
        one(3, "Bone Block", Material.BONE_BLOCK, "bone");
        one(3, "Hay Bale", Material.HAY_BLOCK, "wheat");
        one(3, "Ink Sac", Material.INK_SAC);
        one(3, "Pumpkin", Material.PUMPKIN);
        one(3, "Crossbow", Material.CROSSBOW, "string");
    }

    /** Builds a 25-square card with the requested tier mix, never two squares from the same group. */
    public static List<BingoItem> generateCard(Random rnd, int easy, int medium, int hard) {
        Map<Integer, List<BingoItem>> byTier = new HashMap<>();
        for (BingoItem it : POOL) byTier.computeIfAbsent(it.tier, k -> new ArrayList<>()).add(it);
        byTier.values().forEach(l -> Collections.shuffle(l, rnd));

        List<BingoItem> card = new ArrayList<>(25);
        Set<String> usedGroups = new HashSet<>();
        int[] want = {0, easy, medium, hard};
        // Pick hard ones first so their groups win, then medium, then easy.
        for (int tier = 3; tier >= 1; tier--) {
            int got = 0;
            for (BingoItem it : byTier.getOrDefault(tier, List.of())) {
                if (got >= want[tier]) break;
                if (it.group != null && usedGroups.contains(it.group)) continue;
                card.add(it);
                if (it.group != null) usedGroups.add(it.group);
                got++;
            }
        }
        // Fill any shortfall (shouldn't happen with the pool sizes above) from whatever is left.
        if (card.size() < 25) {
            List<BingoItem> rest = new ArrayList<>(POOL);
            Collections.shuffle(rest, rnd);
            for (BingoItem it : rest) {
                if (card.size() >= 25) break;
                if (card.contains(it)) continue;
                if (it.group != null && usedGroups.contains(it.group)) continue;
                card.add(it);
                if (it.group != null) usedGroups.add(it.group);
            }
            for (BingoItem it : rest) { if (card.size() >= 25) break; if (!card.contains(it)) card.add(it); }
        }
        Collections.shuffle(card, rnd);
        return card;
    }

    /** The 12 lines of a 5x5 card: 5 rows, 5 columns, 2 diagonals (cell index = row*5+col). */
    public static final int[][] LINES;
    static {
        List<int[]> lines = new ArrayList<>();
        for (int r = 0; r < 5; r++) { int[] l = new int[5]; for (int c = 0; c < 5; c++) l[c] = r * 5 + c; lines.add(l); }
        for (int c = 0; c < 5; c++) { int[] l = new int[5]; for (int r = 0; r < 5; r++) l[r] = r * 5 + c; lines.add(l); }
        int[] d1 = new int[5], d2 = new int[5];
        for (int i = 0; i < 5; i++) { d1[i] = i * 5 + i; d2[i] = i * 5 + (4 - i); }
        lines.add(d1); lines.add(d2);
        LINES = lines.toArray(new int[0][]);
    }

    public static String lineName(int idx) {
        if (idx < 5) return "Row " + (idx + 1);
        if (idx < 10) return "Column " + (idx - 4);
        return idx == 10 ? "Diagonal \\" : "Diagonal /";
    }
}
