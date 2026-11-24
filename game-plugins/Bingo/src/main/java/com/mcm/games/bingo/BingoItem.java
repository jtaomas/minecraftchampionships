package com.mcm.games.bingo;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.*;

import static org.bukkit.Material.*;
import static org.bukkit.block.Biome.*;

/**
 * One square on a bingo card: icon, accepted materials, difficulty tier (1 easy, 2 medium, 3 hard, 4 very hard),
 * exclusion groups (at most one item per group on a card, Fetchr-style categories), a pick weight, whether it can
 * be made from wood alone, and what the world must contain for it to be obtainable ({@link #needs}: every entry must
 * be met; an entry is met if the area has any one of its tags).
 *
 * <p>Pool: base list and groups from Fetchr (NeunEinser/bingo, MIT licence), tiers checked against
 * Extremelyd1/minecraft-bingo (GPL-3.0, reference only). See research/bingo_design.md.
 * Requirements are declared conservatively: only sources that certainly exist in the listed biomes or structures.
 * Nether and End items are excluded (portals are disabled). Mob spawning is on and time runs from sunrise, but a
 * 10-12 minute game ends around dusk, so surface-night-only sources (swamp slimes, phantoms) are not relied on;
 * cave mobs (zombies, skeletons, spiders, creepers, endermen) spawn in dark caves at any time of day, and slimes
 * spawn in slime chunks below y 40 at any time of day.
 */
public final class BingoItem {
    public static final String[] TIER_NAMES = {"", "Easy", "Medium", "Hard", "Very hard"};
    public static final String[] TIER_COLORS = {"", "§a", "§e", "§6", "§c"};

    public final Material icon;
    public Set<Material> accepted;
    public final int tier;
    public String name;
    public Set<String> groups = Set.of();
    public int weight = 1;
    public boolean woodOnly;
    public boolean anyVariant;
    public final List<Set<String>> needs = new ArrayList<>();
    public String hint;

    private BingoItem(int tier, Material icon) {
        this.tier = tier;
        this.icon = icon;
        this.accepted = Set.of(icon);
        this.name = pretty(icon.name());
    }

    public boolean matches(Set<Material> held) {
        for (Material m : accepted) if (held.contains(m)) return true;
        return false;
    }

    static String pretty(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String w : enumName.toLowerCase(Locale.ROOT).split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (w.equals("of") || w.equals("the") || w.equals("a") || w.equals("on")) sb.append(w);
            else sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ fluent setup (static init only)

    private BingoItem g(String... gs) { groups = Set.of(gs); return this; }
    private BingoItem w(int weight) { this.weight = weight; return this; }
    private BingoItem wood() { woodOnly = true; return this; }
    private BingoItem n(String name) { this.name = name; return this; }
    private BingoItem need(Set<String> anyOf, String hint) {
        needs.add(anyOf);
        if (this.hint == null) this.hint = hint;
        return this;
    }
    private BingoItem need(Req r) { return need(r.tags, r.hint); }
    private BingoItem alt(Material... more) {
        Set<Material> s = new LinkedHashSet<>(accepted);
        s.addAll(Arrays.asList(more));
        accepted = Collections.unmodifiableSet(s);
        anyVariant = true;
        return this;
    }

    /** A requirement: any one of these tags, with a short player-facing hint. */
    record Req(Set<String> tags, String hint) {
        Req or(Req other) {
            Set<String> t = new HashSet<>(tags);
            t.addAll(other.tags);
            return new Req(Set.copyOf(t), hint + " or " + other.hint);
        }
    }

    static Req biomes(String hint, Biome... bs) {
        Set<String> t = new HashSet<>();
        for (Biome b : bs) t.add(Env.biomeTag(b));
        return new Req(Set.copyOf(t), hint);
    }

    static Req structure(String id, String hint) { return new Req(Set.of("s:" + id), hint); }

    // ------------------------------------------------------------------ requirement vocabulary

    static final Req OAK = biomes("Oak trees", FOREST, FLOWER_FOREST, PLAINS, SUNFLOWER_PLAINS, DARK_FOREST, SWAMP,
            SAVANNA, SAVANNA_PLATEAU, WOODED_BADLANDS, MEADOW, WINDSWEPT_HILLS, WINDSWEPT_FOREST);
    static final Req APPLE_TREES = biomes("Oak or dark oak trees", FOREST, FLOWER_FOREST, PLAINS, SUNFLOWER_PLAINS,
            DARK_FOREST, SWAMP, WOODED_BADLANDS, MEADOW, WINDSWEPT_FOREST);
    static final Req BIRCH = biomes("Birch trees", BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST, FOREST, FLOWER_FOREST, DARK_FOREST, MEADOW);
    static final Req SPRUCE = biomes("Spruce trees (taiga)", TAIGA, SNOWY_TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA,
            GROVE, WINDSWEPT_HILLS, WINDSWEPT_FOREST, WINDSWEPT_GRAVELLY_HILLS);
    static final Req DARK_OAK = biomes("Dark Forest", DARK_FOREST);
    static final Req ACACIA = biomes("Savanna", SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA);
    static final Req JUNGLE_B = biomes("Jungle", JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE);
    static final Req BAMBOO_B = biomes("Jungle / Bamboo Jungle", JUNGLE, BAMBOO_JUNGLE);
    static final Req CHERRY = biomes("Cherry Grove", CHERRY_GROVE);
    static final Req MANGROVE = biomes("Mangrove Swamp", MANGROVE_SWAMP);
    static final Req SWAMP_B = biomes("Swamp", SWAMP);
    static final Req VINES = biomes("Jungle or swamp", JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE, SWAMP, MANGROVE_SWAMP);
    static final Req DESERT_B = biomes("Desert", DESERT);
    static final Req CACTUS_B = biomes("Desert or badlands", DESERT, BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS);
    static final Req BADLANDS_B = biomes("Badlands", BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS);
    static final Req SNOWY = biomes("Snowy biome", SNOWY_PLAINS, ICE_SPIKES, SNOWY_TAIGA, GROVE, SNOWY_SLOPES);
    static final Req POWDER = biomes("Grove (powder snow)", GROVE, SNOWY_SLOPES);
    static final Req BERRIES = biomes("Taiga", TAIGA, SNOWY_TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA);
    static final Req FERNS = biomes("Taiga or jungle", TAIGA, SNOWY_TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA,
            JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE);
    static final Req PLAINS_FLOWERS = biomes("Plains, meadow or flower forest", PLAINS, SUNFLOWER_PLAINS, FLOWER_FOREST, MEADOW);
    static final Req ALLIUM_B = biomes("Flower forest or meadow", FLOWER_FOREST, MEADOW);
    static final Req FLOWER_F = biomes("Flower Forest", FLOWER_FOREST);
    static final Req TALL_FLOWERS = biomes("Forest or flower forest", FOREST, FLOWER_FOREST);
    static final Req SUNFLOWER_B = biomes("Sunflower Plains", SUNFLOWER_PLAINS);
    static final Req FARM = biomes("Cows, sheep, pigs, chickens", PLAINS, SUNFLOWER_PLAINS, FOREST, FLOWER_FOREST, BIRCH_FOREST,
            DARK_FOREST, TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, SWAMP, SAVANNA, SAVANNA_PLATEAU,
            WINDSWEPT_HILLS, WINDSWEPT_FOREST);
    static final Req SHEEP = FARM.or(biomes("meadow", MEADOW));
    static final Req MILK = FARM.or(biomes("mooshrooms", MUSHROOM_FIELDS));
    static final Req RABBITS = biomes("Rabbits (desert, snow, taiga, meadow, cherry)", DESERT, SNOWY_PLAINS, ICE_SPIKES,
            FLOWER_FOREST, MEADOW, CHERRY_GROVE, TAIGA, SNOWY_TAIGA, GROVE, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA);
    static final Req ARMADILLO = biomes("Armadillos (savanna, badlands)", SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA,
            BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS);
    static final Biome[] OCEANS = {OCEAN, DEEP_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN,
            WARM_OCEAN, FROZEN_OCEAN, DEEP_FROZEN_OCEAN};
    static final Req OCEAN_ANY = biomes("Ocean", OCEANS);
    static final Req RIVER_ANY = biomes("River", RIVER, FROZEN_RIVER);
    static final Req WATER = OCEAN_ANY.or(RIVER_ANY).or(biomes("swamp/beach water", SWAMP, MANGROVE_SWAMP, BEACH, SNOWY_BEACH));
    static final Req SQUID = OCEAN_ANY.or(RIVER_ANY);
    static final Req COD = biomes("Ocean (cod)", OCEAN, DEEP_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN);
    static final Req SALMON = biomes("River or cold ocean", RIVER, FROZEN_RIVER, COLD_OCEAN, DEEP_COLD_OCEAN, FROZEN_OCEAN, DEEP_FROZEN_OCEAN);
    static final Req TROPICAL = biomes("Warm ocean or mangrove", WARM_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN, MANGROVE_SWAMP);
    static final Req PUFFER = biomes("Warm ocean", WARM_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN);
    static final Req KELP_B = biomes("Ocean (kelp)", OCEAN, DEEP_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN, LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN);
    static final Req PICKLE = biomes("Warm Ocean", WARM_OCEAN);
    static final Req SAND = biomes("Sand (desert, beach, river)", DESERT, BEACH, SNOWY_BEACH, RIVER, OCEAN, DEEP_OCEAN,
            LUKEWARM_OCEAN, DEEP_LUKEWARM_OCEAN, WARM_OCEAN, COLD_OCEAN, DEEP_COLD_OCEAN);
    static final Req GLASS_SRC = SAND.or(BADLANDS_B);
    static final Req CLAY = WATER.or(biomes("lush caves", LUSH_CAVES));
    static final Req LUSH = biomes("Lush Caves", LUSH_CAVES);
    static final Req DRIP = biomes("Dripstone Caves", DRIPSTONE_CAVES);
    static final Req MUSHROOM_F = biomes("Mushroom Fields", MUSHROOM_FIELDS);
    static final Req VILLAGE = structure("village", "Village");
    static final Req EMERALD_SRC = VILLAGE.or(biomes("mountain biome", WINDSWEPT_HILLS, WINDSWEPT_GRAVELLY_HILLS,
            WINDSWEPT_FOREST, MEADOW, GROVE, CHERRY_GROVE, SNOWY_SLOPES, JAGGED_PEAKS, FROZEN_PEAKS, STONY_PEAKS));
    static final Req PYRAMID = structure("desert_pyramid", "Desert Pyramid");
    static final Req PORTAL = structure("ruined_portal", "Ruined Portal");
    static final Req TREASURE = structure("buried_treasure", "Buried Treasure");
    static final Req MINESHAFT = structure("mineshaft", "Mineshaft");

    // ------------------------------------------------------------------ pool

    public static final List<BingoItem> POOL = new ArrayList<>();

    private static BingoItem add(int tier, Material m) {
        BingoItem it = new BingoItem(tier, m);
        POOL.add(it);
        return it;
    }

    static {
        // ======================= tier 1: easy (biome-specific gathering, short crafts)
        add(1, OAK_SAPLING).g("oak").need(OAK);
        add(1, BIRCH_SAPLING).g("birch").need(BIRCH);
        add(1, SPRUCE_SAPLING).g("taiga").need(SPRUCE);
        add(1, DARK_OAK_SAPLING).g("dark_oak").need(DARK_OAK);
        add(1, ACACIA_SAPLING).g("savanna").need(ACACIA);
        add(1, CHERRY_SAPLING).g("cherry_grove").need(CHERRY);
        add(1, MANGROVE_PROPAGULE).g("mangrove").need(MANGROVE);
        add(1, PINK_PETALS).g("cherry_grove").need(CHERRY);
        add(1, SWEET_BERRIES).g("taiga").need(BERRIES);
        add(1, COCOA_BEANS).g("jungle").need(JUNGLE_B);
        add(1, MELON_SLICE).g("jungle").need(JUNGLE_B);
        add(1, BAMBOO).g("bamboo").need(BAMBOO_B);
        add(1, CACTUS).g("warm_biome").need(CACTUS_B);
        add(1, RED_SAND).g("badlands").need(BADLANDS_B);
        add(1, ORANGE_TERRACOTTA).g("badlands").need(BADLANDS_B);
        add(1, YELLOW_TERRACOTTA).g("badlands").need(BADLANDS_B);
        add(1, RED_SANDSTONE).g("badlands").need(BADLANDS_B);
        add(1, SNOWBALL).g("snowy_biomes").need(SNOWY);
        add(1, SNOW_BLOCK).g("snowy_biomes").need(SNOWY);
        add(1, BLUE_ORCHID).g("swamp").need(SWAMP_B);
        add(1, LILY_PAD).g("swamp").need(SWAMP_B);
        add(1, MANGROVE_ROOTS).g("mangrove").need(MANGROVE);
        add(1, AZURE_BLUET).g("flower_white").need(PLAINS_FLOWERS);
        add(1, OXEYE_DAISY).g("flower_white").need(PLAINS_FLOWERS);
        add(1, CORNFLOWER).g("flower_blue").need(PLAINS_FLOWERS);
        add(1, ALLIUM).g("flower_magenta").need(ALLIUM_B);
        add(1, LILY_OF_THE_VALLEY).g("flower_white").need(FLOWER_F);
        add(1, ORANGE_TULIP).g("tulip").need(FLOWER_F);
        add(1, PINK_TULIP).g("tulip").need(FLOWER_F);
        add(1, LILAC).g("tall_flower").need(TALL_FLOWERS);
        add(1, PEONY).g("tall_flower").need(TALL_FLOWERS);
        add(1, ROSE_BUSH).g("tall_flower").need(TALL_FLOWERS);
        add(1, SUNFLOWER).g("tall_flower").need(SUNFLOWER_B);
        add(1, BROWN_MUSHROOM).g("mushroom");
        add(1, RED_MUSHROOM).g("mushroom");
        add(1, MUSHROOM_STEW).g("mushroom");
        add(1, EGG).g("egg").need(FARM);
        add(1, FEATHER).g("chicken").need(FARM);
        add(1, LEATHER).g("leather").need(FARM);
        add(1, MILK_BUCKET).g("milk").need(MILK);
        add(1, FLINT).g("flint");
        add(1, FLINT_AND_STEEL).g("flint");
        add(1, ARROW).g("skeleton_drops");
        add(1, BONE).g("skeleton_drops");
        add(1, STRING).g("spider");
        add(1, ROTTEN_FLESH).g("zombie");
        add(1, BOW).g("bow");
        add(1, COPPER_INGOT).g("copper");
        add(1, LIGHTNING_ROD).g("copper");
        add(1, COBBLED_DEEPSLATE).g("deepslate");
        add(1, DEEPSLATE).g("deepslate").n("Deepslate (smelted)");
        add(1, TUFF).g("deepslate");
        add(1, LANTERN).g("basic_iron");
        add(1, CHAIN).g("basic_iron");
        add(1, STONECUTTER).g("basic_iron");
        add(1, IRON_TRAPDOOR).g("basic_iron");
        add(1, SMITHING_TABLE).g("basic_iron");
        add(1, TRIPWIRE_HOOK).g("basic_iron");
        add(1, GLASS).g("sand").need(GLASS_SRC);
        add(1, GLASS_BOTTLE).g("sand").need(GLASS_SRC);
        add(1, SMOOTH_SANDSTONE).g("sand").need(SAND);
        add(1, SUGAR_CANE).g("paper").need(WATER);
        add(1, CARTOGRAPHY_TABLE).g("paper").need(WATER);
        add(1, SMOOTH_STONE).g("smooth");
        add(1, CRACKED_STONE_BRICKS).g("stone_bricks");
        add(1, POLISHED_GRANITE).g("stone_variant");
        add(1, POLISHED_DIORITE).g("stone_variant");
        add(1, NOTE_BLOCK).g("redstone");
        add(1, RAIL).g("rail");
        add(1, FLETCHING_TABLE).g("flint");
        add(1, LOOM).g("spider");
        add(1, GRINDSTONE).g("grindstone");
        add(1, PAINTING).g("wool").need(SHEEP);
        add(1, ITEM_FRAME).g("leather").need(FARM);
        add(1, ORANGE_DYE).g("dye_mix").need(FARM);
        add(1, LIGHT_BLUE_DYE).g("lapis");
        add(1, DRIED_KELP).g("kelp").need(KELP_B);
        // wood-only crafts: at most a couple per card (owner: "about 10 items crafted from wood alone")
        add(1, BIRCH_BOAT).g("birch").wood().need(BIRCH);
        add(1, SPRUCE_TRAPDOOR).g("taiga").wood().need(SPRUCE);
        add(1, DARK_OAK_FENCE).g("dark_oak").wood().need(DARK_OAK);
        add(1, ACACIA_FENCE_GATE).g("savanna").wood().need(ACACIA);
        add(1, JUNGLE_DOOR).g("jungle").wood().need(JUNGLE_B);
        add(1, CHERRY_CHEST_BOAT).g("cherry_grove").wood().need(CHERRY);
        add(1, MANGROVE_TRAPDOOR).g("mangrove").wood().need(MANGROVE);
        add(1, BAMBOO_MOSAIC).g("bamboo").wood().need(BAMBOO_B);
        add(1, BAMBOO_RAFT).g("bamboo").wood().need(BAMBOO_B);
        add(1, STRIPPED_CHERRY_LOG).g("cherry_grove").wood().need(CHERRY);
        add(1, STRIPPED_ACACIA_LOG).g("savanna").wood().need(ACACIA);
        add(1, CHISELED_BOOKSHELF).g("bookshelf").wood();
        add(1, COMPOSTER).g("composter").wood();
        add(1, LADDER).g("ladder").wood();

        // ======================= tier 2: medium (a mob, a cave trip, a crafting chain, a specific biome product)
        add(2, JUNGLE_SAPLING).g("jungle").need(JUNGLE_B);
        add(2, MELON).g("jungle").n("Melon (block)").need(JUNGLE_B);
        add(2, COOKIE).g("jungle", "wheat").need(JUNGLE_B);
        add(2, BROWN_DYE).g("jungle").need(JUNGLE_B);
        add(2, SCAFFOLDING).g("bamboo", "spider").need(BAMBOO_B);
        add(2, APPLE).g("oak").need(APPLE_TREES);
        add(2, CHERRY_HANGING_SIGN).g("cherry_grove", "basic_iron").need(CHERRY);
        add(2, ACACIA_HANGING_SIGN).g("savanna", "basic_iron").need(ACACIA);
        add(2, MUDDY_MANGROVE_ROOTS).g("mangrove").need(MANGROVE);
        add(2, MUD_BRICKS).g("mud", "wheat").need(GLASS_SRC.or(MANGROVE));
        add(2, PACKED_MUD).g("mud", "wheat").need(GLASS_SRC.or(MANGROVE));
        add(2, DEAD_BUSH).g("warm_biome", "shearable").need(CACTUS_B);
        add(2, GREEN_DYE).g("warm_biome").need(CACTUS_B);
        add(2, LIME_DYE).g("warm_biome").need(CACTUS_B);
        add(2, CYAN_DYE).g("warm_biome", "lapis").need(CACTUS_B);
        add(2, CHISELED_RED_SANDSTONE).g("badlands").need(BADLANDS_B);
        add(2, CHISELED_SANDSTONE).g("sand").need(DESERT_B);
        add(2, SNOW).g("snowy_biomes").n("Snow (layer)").need(SNOWY);
        add(2, FERN).g("taiga", "shearable").need(FERNS);
        add(2, VINE).g("shearable").need(VINES);
        add(2, SEAGRASS).g("shearable").need(WATER);
        add(2, GLOW_LICHEN).g("shearable");
        add(2, SUSPICIOUS_STEW).g("mushroom");
        add(2, RABBIT_HIDE).g("rabbit").need(RABBITS);
        add(2, COOKED_RABBIT).g("rabbit").need(RABBITS);
        add(2, RED_BED).g("wool").need(SHEEP);
        add(2, PINK_WOOL).g("wool").need(SHEEP);
        add(2, LIGHT_GRAY_WOOL).g("wool").need(SHEEP);
        add(2, LEATHER_HORSE_ARMOR).g("leather").need(FARM);
        add(2, BOOK).g("paper", "leather").need(FARM).need(WATER);
        add(2, WRITABLE_BOOK).g("paper", "ink").need(FARM).need(SQUID);
        add(2, INK_SAC).g("ink").need(SQUID);
        add(2, GRAY_DYE).g("ink").need(SQUID);
        add(2, BLACK_DYE).g("ink").need(SQUID);
        add(2, PURPLE_DYE).g("lapis").need(FARM);
        add(2, MAGENTA_DYE).g("lapis").need(FARM);
        add(2, LAPIS_LAZULI).g("lapis");
        add(2, COOKED_COD).g("fish").need(WATER);
        add(2, COOKED_SALMON).g("fish").need(WATER);
        add(2, COD_BUCKET).g("fish").need(COD);
        add(2, SALMON_BUCKET).g("fish").need(SALMON);
        add(2, DRIED_KELP_BLOCK).g("kelp").need(KELP_B);
        add(2, BREAD).g("wheat");
        add(2, HAY_BLOCK).g("wheat");
        add(2, CARROT).g("village").need(VILLAGE);
        add(2, BAKED_POTATO).g("village").need(VILLAGE);
        add(2, BEETROOT).g("village").need(VILLAGE);
        add(2, CARROT_ON_A_STICK).g("village", "spider").need(VILLAGE);
        add(2, BRICK).g("clay").need(CLAY);
        add(2, FLOWER_POT).g("clay").need(CLAY);
        add(2, DECORATED_POT).g("clay").need(CLAY);
        add(2, TERRACOTTA).g("clay").need(CLAY.or(BADLANDS_B));
        add(2, ORANGE_CONCRETE).g("sand", "dye_mix").need(SAND).need(FARM);
        add(2, WHITE_STAINED_GLASS).g("sand", "skeleton_drops").need(GLASS_SRC);
        add(2, BONE_BLOCK).g("skeleton_drops");
        add(2, SPIDER_EYE).g("spider");
        add(2, GUNPOWDER).g("gunpowder");
        add(2, FIREWORK_ROCKET).g("gunpowder", "paper").need(WATER);
        add(2, CROSSBOW).g("bow", "basic_iron");
        add(2, DISPENSER).g("bow", "redstone");
        add(2, FISHING_ROD).g("spider").need(WATER);
        add(2, GOLD_INGOT).g("gold");
        add(2, GOLDEN_SHOVEL).g("gold");
        add(2, GOLDEN_SWORD).g("gold");
        add(2, CLOCK).g("gold_redstone");
        add(2, POWERED_RAIL).g("gold_redstone", "rail");
        add(2, COMPASS).g("compass");
        add(2, MAP).g("compass", "paper").need(WATER);
        add(2, REPEATER).g("redstone");
        add(2, PISTON).g("piston");
        add(2, REDSTONE_BLOCK).g("redstone");
        add(2, DETECTOR_RAIL).g("rail");
        add(2, ACTIVATOR_RAIL).g("rail");
        add(2, CHEST_MINECART).g("chest_iron");
        add(2, FURNACE_MINECART).g("furnace");
        add(2, BLAST_FURNACE).g("furnace", "smooth");
        add(2, ARMOR_STAND).g("smooth");
        add(2, CAULDRON).g("basic_iron");
        add(2, HOPPER).g("chest_iron");
        add(2, IRON_BLOCK).g("basic_iron");
        add(2, RAW_IRON_BLOCK).g("basic_iron");
        add(2, COPPER_BLOCK).g("copper");
        add(2, CUT_COPPER).g("copper");
        add(2, CHISELED_COPPER).g("copper");
        add(2, COPPER_GRATE).g("copper");
        add(2, RAW_COPPER_BLOCK).g("copper");
        add(2, BRUSH).g("copper", "chicken").need(FARM);
        add(2, CRACKED_DEEPSLATE_BRICKS).g("deepslate");
        add(2, DEEPSLATE_TILE_WALL).g("deepslate");
        add(2, TUFF_BRICKS).g("deepslate");
        add(2, CHISELED_TUFF).g("deepslate");
        add(2, MOSSY_COBBLESTONE).g("moss").need(VINES.or(LUSH));
        add(2, MOSSY_STONE_BRICKS).g("moss").need(VINES.or(LUSH));
        add(2, AMETHYST_SHARD).g("amethyst");
        add(2, CALCITE).g("amethyst");
        add(2, AMETHYST_BLOCK).g("amethyst");
        add(2, MOSS_BLOCK).g("lush_cave").need(LUSH);
        add(2, FLOWERING_AZALEA).g("lush_cave").need(LUSH);
        add(2, BIG_DRIPLEAF).g("lush_cave").need(LUSH);
        add(2, ROOTED_DIRT).g("lush_cave").need(LUSH);
        add(2, HANGING_ROOTS).g("lush_cave", "shearable").need(LUSH);
        add(2, GLOW_BERRIES).g("lush_cave").need(LUSH);
        add(2, POINTED_DRIPSTONE).g("dripstone_cave").need(DRIP);
        add(2, DRIPSTONE_BLOCK).g("dripstone_cave").need(DRIP);
        add(2, SMOKER).g("furnace");
        add(2, CAMPFIRE).g("campfire");

        // ======================= tier 3: hard (a real detour: structure, rare drop, long chain)
        add(3, DIAMOND).g("diamond");
        add(3, JUKEBOX).g("diamond");
        add(3, EMERALD).g("village").need(EMERALD_SRC);
        add(3, BELL).g("village").need(VILLAGE);
        add(3, GOLDEN_CARROT).g("village", "gold").need(VILLAGE);
        add(3, BEETROOT_SOUP).g("village").need(VILLAGE);
        add(3, RABBIT_STEW).g("rabbit", "village").need(RABBITS).need(VILLAGE);
        add(3, RABBIT_FOOT).g("rabbit").need(RABBITS);
        add(3, GOLDEN_APPLE).g("oak", "gold").need(APPLE_TREES);
        add(3, GLISTERING_MELON_SLICE).g("jungle", "gold").need(JUNGLE_B);
        add(3, PUMPKIN).g("pumpkin").need(FARM);
        add(3, JACK_O_LANTERN).g("pumpkin").need(FARM);
        add(3, PUMPKIN_PIE).g("pumpkin", "egg").need(FARM).need(WATER);
        add(3, CAKE).g("milk", "egg").need(FARM).need(WATER);
        add(3, ARMADILLO_SCUTE).g("armadillo").need(ARMADILLO);
        add(3, POWDER_SNOW_BUCKET).g("snowy_biomes").need(POWDER);
        add(3, SPORE_BLOSSOM).g("lush_cave").need(LUSH);
        add(3, TROPICAL_FISH_BUCKET).g("fish").need(TROPICAL);
        add(3, PUFFERFISH_BUCKET).g("fish").need(PUFFER);
        add(3, SEA_PICKLE).g("warm_biome").need(PICKLE);
        add(3, NETHERRACK).g("ruined_portal").need(PORTAL);
        add(3, NETHER_BRICK).g("ruined_portal").n("Nether Brick (item)").need(PORTAL);
        add(3, COBWEB).g("mineshaft", "shearable").need(MINESHAFT);
        add(3, BOOKSHELF).g("paper", "leather").need(FARM).need(WATER);
        add(3, LECTERN).g("paper", "leather").need(FARM).need(WATER);
        add(3, TNT).g("gunpowder").need(SAND.or(PYRAMID));
        add(3, TNT_MINECART).g("gunpowder").need(SAND.or(PYRAMID));
        add(3, FIREWORK_STAR).g("gunpowder");
        add(3, FERMENTED_SPIDER_EYE).g("spider", "paper").need(WATER);
        add(3, TARGET).g("wheat", "redstone");
        // re-admitted with mob spawning on: slimes in slime chunks underground (any light, any time of day)
        add(3, SLIME_BALL).g("slime").n("Slimeball (cave slimes)");
        add(3, STICKY_PISTON).g("slime", "piston");
        add(4, LEAD).g("slime", "spider");
        add(3, CRAFTER).g("basic_iron", "redstone");
        add(3, ANVIL).g("basic_iron");
        add(3, HOPPER_MINECART).g("chest_iron");
        add(3, LAPIS_BLOCK).g("lapis");
        add(3, GOLD_BLOCK).g("gold");
        add(3, RAW_GOLD_BLOCK).g("gold");
        add(3, SPYGLASS).g("amethyst", "copper");
        add(3, TINTED_GLASS).g("amethyst", "sand").need(GLASS_SRC);
        add(3, PINK_GLAZED_TERRACOTTA).g("clay", "dye_mix").need(CLAY.or(BADLANDS_B)).need(FARM);
        add(3, BROWN_BANNER).g("wool", "jungle").need(SHEEP).need(JUNGLE_B);
        add(3, PINK_CONCRETE_POWDER).g("sand", "dye_mix").need(SAND).need(FARM);

        // ======================= tier 4: very hard (worth a big detour)
        add(4, ENDER_PEARL).g("nighttime_mob_drops");
        add(4, AXOLOTL_BUCKET).g("lush_cave").need(LUSH);
        add(4, HEART_OF_THE_SEA).g("shipwreck").need(TREASURE);
        add(4, OBSIDIAN).g("diamond");
        add(4, ARCHER_POTTERY_SHERD).g("desert_pyramid").n("Desert Pyramid Sherd")
                .alt(MINER_POTTERY_SHERD, PRIZE_POTTERY_SHERD, SKULL_POTTERY_SHERD).need(PYRAMID);
        add(4, WOLF_ARMOR).g("armadillo").need(ARMADILLO);
        add(4, DIAMOND_PICKAXE).g("diamond");
        add(4, DIAMOND_HOE).g("diamond");
        add(4, ENCHANTING_TABLE).g("diamond", "paper").need(FARM).need(WATER);
        add(4, EMERALD_BLOCK).g("village").need(EMERALD_SRC);
    }

    /** Points per tier, index 1..4 (filled from config by the game). */
    static int clampTier(int t) { return Math.max(1, Math.min(4, t)); }

    /**
     * Builds a 25-square card: {@code perTier[t]} items of tier t (t = 1..4), only items {@code env} satisfies, never
     * two items sharing a group, at most {@code woodCap} wood-only items. Inside a tier, picks are weighted (item weight,
     * doubled for items with a biome/structure requirement). Shortfalls in a tier are filled from neighbouring tiers.
     */
    public static List<BingoItem> generateCard(Random rnd, Env env, int[] perTier, int woodCap) {
        List<BingoItem> card = new ArrayList<>(25);
        Set<String> usedGroups = new HashSet<>();
        int[] woodLeft = {woodCap};
        int[] want = new int[5];
        for (int t = 1; t <= 4; t++) want[t] = t < perTier.length ? Math.max(0, perTier[t]) : 0;
        int total = 0;
        for (int t = 1; t <= 4; t++) total += want[t];
        if (total != 25) { // keep the mix proportions sane if config is off
            want[1] += 25 - total;
            if (want[1] < 0) { want[2] += want[1]; want[1] = 0; }
        }
        int shortfall = 0;
        for (int t = 4; t >= 1; t--) {
            int got = pick(rnd, env, t, want[t] + shortfall, card, usedGroups, woodLeft);
            shortfall = want[t] + shortfall - got;
        }
        // Still short (tiny env): try every tier again, hardest first, then relax groups.
        for (int t = 4; t >= 1 && card.size() < 25; t--) pick(rnd, env, t, 25 - card.size(), card, usedGroups, woodLeft);
        if (card.size() < 25) {
            for (BingoItem it : POOL) {
                if (card.size() >= 25) break;
                if (!card.contains(it) && env.satisfies(it)) card.add(it);
            }
        }
        Collections.shuffle(card, rnd);
        return card;
    }

    private static int pick(Random rnd, Env env, int tier, int count, List<BingoItem> card, Set<String> usedGroups, int[] woodLeft) {
        int got = 0;
        while (got < count && card.size() < 25) {
            List<BingoItem> cands = new ArrayList<>();
            int totalW = 0;
            for (BingoItem it : POOL) {
                if (it.tier != tier || card.contains(it) || !env.satisfies(it)) continue;
                if (it.woodOnly && woodLeft[0] <= 0) continue;
                boolean clash = false;
                for (String g : it.groups) if (usedGroups.contains(g)) { clash = true; break; }
                if (clash) continue;
                cands.add(it);
                totalW += effWeight(it);
            }
            if (cands.isEmpty()) break;
            int r = rnd.nextInt(totalW);
            BingoItem chosen = cands.get(cands.size() - 1);
            for (BingoItem it : cands) { r -= effWeight(it); if (r < 0) { chosen = it; break; } }
            card.add(chosen);
            usedGroups.addAll(chosen.groups);
            if (chosen.woodOnly) woodLeft[0]--;
            got++;
        }
        return got;
    }

    static int effWeight(BingoItem it) { return it.weight * (it.needs.isEmpty() ? 1 : 2); }

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
