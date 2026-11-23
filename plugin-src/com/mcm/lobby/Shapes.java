package com.mcm.lobby;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java block-shape knowledge used by the hub post-pass ({@link BlockFix}) and the offline validator:
 * which block states are full solid cubes, which faces are sturdy (for fence/pane/wall connections), what a
 * carpet can rest on, and which blocks a player can walk through. Ids may carry a namespace and states.
 */
final class Shapes {
    private Shapes() {}

    // ------------------------------------------------------------------------------------------------ parsing
    /** "minecraft:oak_stairs[facing=north]" -> "oak_stairs"; null stays null. */
    static String base(String s) {
        if (s == null) return null;
        int b = s.indexOf('[');
        String id = b < 0 ? s : s.substring(0, b);
        int c = id.indexOf(':');
        return c < 0 ? id : id.substring(c + 1);
    }

    static Map<String, String> props(String s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null) return m;
        int b = s.indexOf('['), e = s.lastIndexOf(']');
        if (b < 0 || e < b) return m;
        for (String kv : s.substring(b + 1, e).split(",")) {
            int q = kv.indexOf('=');
            if (q > 0) m.put(kv.substring(0, q).trim(), kv.substring(q + 1).trim());
        }
        return m;
    }

    static String prop(String s, String key, String def) {
        String v = props(s).get(key);
        return v == null ? def : v;
    }

    static String state(String base, Map<String, String> p) {
        StringBuilder sb = new StringBuilder("minecraft:").append(base);
        if (!p.isEmpty()) {
            sb.append('[');
            boolean first = true;
            for (var e : p.entrySet()) { if (!first) sb.append(','); first = false; sb.append(e.getKey()).append('=').append(e.getValue()); }
            sb.append(']');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------ directions
    /** N, E, S, W as {dx, dz}. */
    static final String[] DIRS = {"north", "east", "south", "west"};
    static final int[][] DXZ = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    static int dirIndex(String f) {
        for (int i = 0; i < 4; i++) if (DIRS[i].equals(f)) return i;
        return -1;
    }

    static int opposite(int d) { return (d + 2) & 3; }

    // ------------------------------------------------------------------------------------------------ classes
    static boolean isAir(String id) {
        return id == null || id.equals("air") || id.equals("cave_air") || id.equals("void_air") || id.equals("light") || id.equals("structure_void");
    }

    static boolean isStairs(String id) { return id != null && id.endsWith("_stairs"); }
    static boolean isSlab(String id) { return id != null && id.endsWith("_slab"); }
    static boolean isFenceGate(String id) { return id != null && id.endsWith("_fence_gate"); }
    static boolean isFence(String id) { return id != null && id.endsWith("_fence"); }
    static boolean isWall(String id) { return id != null && id.endsWith("_wall"); }
    /** Glass panes and iron bars (vanilla IronBarsBlock). */
    static boolean isPane(String id) { return id != null && (id.endsWith("_pane") || id.equals("iron_bars")); }
    static boolean isCarpet(String id) { return id != null && id.endsWith("_carpet"); }
    static boolean isLeaves(String id) { return id != null && id.endsWith("_leaves"); }

    static boolean isWoodenFence(String id) { return isFence(id) && !id.equals("nether_brick_fence"); }

    /** Vanilla Block.isExceptionForConnection: never connect to these even though they are full cubes. */
    static boolean isConnectionException(String id) {
        return isLeaves(id) || id.equals("barrier") || id.equals("pumpkin") || id.equals("carved_pumpkin")
                || id.equals("jack_o_lantern") || id.equals("melon") || id.endsWith("shulker_box");
    }

    private static final Set<String> NON_FULL_EXACT = Set.of(
            "iron_bars", "lantern", "soul_lantern", "chain", "end_rod", "lightning_rod", "campfire", "soul_campfire",
            "flower_pot", "candle", "ladder", "vine", "snow", "seagrass", "tall_seagrass", "kelp", "kelp_plant",
            "short_grass", "grass", "tall_grass", "fern", "large_fern", "dead_bush", "sugar_cane", "cactus", "bamboo",
            "cobweb", "scaffolding", "water", "lava", "bubble_column", "dirt_path", "farmland", "chest", "ender_chest",
            "trapped_chest", "lectern", "anvil", "chipped_anvil", "damaged_anvil", "bell", "brewing_stand", "cauldron",
            "water_cauldron", "lava_cauldron", "powder_snow_cauldron", "hopper", "enchanting_table", "end_portal_frame",
            "daylight_detector", "conduit", "stonecutter", "grindstone", "cake", "sea_pickle", "lily_pad", "turtle_egg",
            "sniffer_egg", "pink_petals", "glow_lichen", "sculk_vein", "hanging_roots", "spore_blossom",
            "pointed_dripstone", "amethyst_cluster", "big_dripleaf", "big_dripleaf_stem", "small_dripleaf",
            "sweet_berry_bush", "tripwire", "tripwire_hook", "redstone_wire", "lever", "repeater", "comparator",
            "decorated_pot", "frogspawn", "chorus_plant", "chorus_flower", "dragon_egg", "piston_head", "moving_piston",
            "mangrove_roots", "mangrove_propagule", "heavy_core", "azalea", "flowering_azalea", "torch", "soul_torch",
            "redstone_torch", "wall_torch", "soul_wall_torch", "redstone_wall_torch", "poppy", "dandelion", "cornflower",
            "oxeye_daisy", "azure_bluet", "allium", "lily_of_the_valley", "blue_orchid", "sunflower", "lilac", "rose_bush",
            "peony", "wither_rose", "torchflower", "pitcher_plant", "pitcher_pod", "brown_mushroom", "red_mushroom",
            "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots", "nether_sprouts", "twisting_vines",
            "twisting_vines_plant", "weeping_vines", "weeping_vines_plant", "cave_vines", "cave_vines_plant", "rail",
            "powered_rail", "detector_rail", "activator_rail", "beetroots", "carrots", "potatoes", "wheat",
            "melon_stem", "pumpkin_stem", "attached_melon_stem", "attached_pumpkin_stem", "nether_wart", "cocoa",
            "fire", "soul_fire", "nether_portal", "end_portal", "end_gateway", "barrier", "bed", "head", "skull",
            "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "calibrated_sculk_sensor", "sculk_sensor",
            "sculk_shrieker");

    private static final String[] NON_FULL_SUFFIX = {
            "_stairs", "_fence", "_fence_gate", "_wall", "_pane", "_carpet", "_button", "_pressure_plate", "_sign",
            "_banner", "_door", "_trapdoor", "_bed", "_torch", "_candle", "_sapling", "_coral", "_coral_fan",
            "_coral_wall_fan", "_tulip", "_head", "_skull", "_rail", "_cake", "_amethyst_bud", "_propagule"};

    /** True for blocks that occupy the whole cube (incl. glass and leaves, double slabs). */
    static boolean isFullCube(String state) {
        String id = base(state);
        if (isAir(id)) return false;
        if (isSlab(id)) return "double".equals(prop(state, "type", "bottom"));
        if (NON_FULL_EXACT.contains(id)) return false;
        if (id.startsWith("potted_")) return false;
        for (String s : NON_FULL_SUFFIX) if (id.endsWith(s)) return false;
        return true;
    }

    /** Vanilla isFaceSturdy for the face of {@code state} pointing in direction {@code face} (0..3 = N,E,S,W). */
    static boolean sturdyFace(String state, int face) {
        String id = base(state);
        if (isAir(id)) return false;
        if (isStairs(id)) return dirIndex(prop(state, "facing", "north")) == face; // the full back of a stair
        return isFullCube(state);
    }

    /** Can a carpet rest flat on top of this block (full top face)? */
    static boolean fullTop(String state) {
        String id = base(state);
        if (isAir(id) || id.equals("barrier")) return false;
        if (isSlab(id)) return !"bottom".equals(prop(state, "type", "bottom"));
        if (isStairs(id)) return "top".equals(prop(state, "half", "bottom"));
        if (id.equals("snow")) return "8".equals(prop(state, "layers", "1"));
        return isFullCube(state);
    }

    private static final Set<String> NO_COLLISION_EXACT = Set.of(
            "short_grass", "grass", "tall_grass", "fern", "large_fern", "dead_bush", "seagrass", "tall_seagrass",
            "poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "lily_of_the_valley",
            "blue_orchid", "sunflower", "lilac", "rose_bush", "peony", "wither_rose", "torchflower", "pitcher_plant",
            "brown_mushroom", "red_mushroom", "torch", "soul_torch", "redstone_torch", "wall_torch", "soul_wall_torch",
            "redstone_wall_torch", "vine", "glow_lichen", "sculk_vein", "hanging_roots", "spore_blossom", "light",
            "rail", "powered_rail", "detector_rail", "activator_rail", "tripwire", "redstone_wire", "lever",
            "sugar_cane", "kelp", "kelp_plant", "water", "lava", "bubble_column", "fire", "soul_fire", "nether_sprouts",
            "crimson_roots", "warped_roots", "pink_petals", "cobweb", "sweet_berry_bush");

    /** True when a player can stand inside this block (air, plants, torches, carpets, thin snow, ...). */
    static boolean passable(String state) {
        String id = base(state);
        if (isAir(id)) return true;
        if (NO_COLLISION_EXACT.contains(id)) return true;
        if (isCarpet(id)) return true; // 1/16 high: you stand on it, it does not block the head
        if (id.equals("snow")) return "1".equals(prop(state, "layers", "1"));
        for (String s : new String[]{"_sapling", "_tulip", "_button", "_pressure_plate", "_sign", "_banner", "_coral", "_coral_fan", "_coral_wall_fan"})
            if (id.endsWith(s)) return true;
        return false;
    }
}
