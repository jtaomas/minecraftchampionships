package com.mcm.games.battlebox.arena;

/**
 * Rough vanilla block-shape knowledge for the pure-Java post-pass, the staircase validator and the offline
 * sightline check. Works on block-state strings only (no Bukkit).
 */
public final class Shapes {
    private Shapes() {}

    private static final String[] NOT_FULL = {"slab", "stairs", "carpet", "torch", "button", "trapdoor",
            "door", "rail", "sapling", "short_grass", "tall_grass", "fern", "sign", "banner", "chain", "rod", "potted",
            "candle", "water", "lava", "pressure_plate", "vine", "lily_pad", "lily_of", "tulip", "poppy", "orchid", "allium", "bluet",
            "daisy", "dandelion", "cornflower", "azalea", "leaves", "campfire", "bush", "dripleaf",
            "lever", "ladder", "cake", "lichen", "frame", "hopper", "cauldron", "brewing",
            "anvil", "grindstone", "lectern", "composter", "scaffolding", "dripstone", "cluster", "bud", "pickle",
            "coral_fan", "kelp_plant", "roots", "sprouts", "berry", "flower_pot", "end_rod", "wheat", "carrots", "potatoes",
            "beetroots", "farmland", "dirt_path", "chest", "snow", "cobweb", "flower", "decorated_pot", "daylight",
            "enchanting", "stonecutter", "carved_pumpkin", "jack_o_lantern", "melon", "pumpkin", "shulker", "glass_pane",
            "iron_bars", "_fence", "_wall", "gate", "conduit", "sea_pickle", "tripwire", "string", "moss_carpet",
            "pink_petals", "sugar_cane", "cactus", "honey_block", "soul_sand", "spore", "hanging"};

    /** True for a full, sturdy cube that fences, panes and walls connect to and carpets may sit on. */
    public static boolean fullSolid(String b) {
        if (States.isAir(b)) return false;
        String id = States.id(b);
        if (id.endsWith("_slab")) return b.contains("type=double");
        if (id.equals("snow_block") || id.equals("mud_bricks") || id.equals("packed_mud") || id.equals("glass")
                || id.endsWith("_stained_glass") || id.equals("tinted_glass") || id.equals("honeycomb_block")
                || id.equals("muddy_mangrove_roots")) return true;
        if (id.equals("lantern") || id.equals("soul_lantern") || id.equals("bell") || id.equals("barrier")
                || id.equals("light") || id.endsWith("_bed") || id.equals("bamboo") || id.equals("mud")
                || id.endsWith("_head") || id.endsWith("_skull") || id.equals("head") || id.equals("skull")) return false;
        if (id.equals("sea_lantern")) return true;
        for (String n : NOT_FULL) if (id.contains(n)) return false;
        return true;
    }

    /** 1 fence, 2 pane/bars, 3 wall, 4 fence gate, 0 other. */
    public static int kind(String id) {
        if (id.endsWith("_fence_gate")) return 4;
        if (id.endsWith("_fence")) return 1;
        if (id.endsWith("glass_pane") || id.equals("iron_bars")) return 2;
        if (id.endsWith("_wall") && !id.endsWith("_wall_sign") && !id.endsWith("_wall_banner")
                && !id.endsWith("_wall_hanging_sign") && !id.endsWith("_wall_head") && !id.endsWith("_wall_skull")
                && !id.endsWith("wall_torch") && !id.endsWith("_wall_fan")) return 3;
        return 0;
    }

    /** Value of a state property, or null. */
    public static String prop(String b, String key) {
        int s = b.indexOf('[');
        if (s < 0) return null;
        for (String kv : b.substring(s + 1, b.length() - 1).split(",")) {
            int e = kv.indexOf('=');
            if (e > 0 && kv.substring(0, e).equals(key)) return kv.substring(e + 1);
        }
        return null;
    }

    /** True if a player can occupy this cell (no collision box). */
    public static boolean passable(String b) {
        if (States.isAir(b)) return true;
        String id = States.id(b);
        if (id.equals("snow")) { String l = prop(b, "layers"); return l == null || l.equals("1"); }
        if (id.equals("water") || id.equals("lava") || id.equals("light")) return true;
        if (id.endsWith("_carpet") || id.equals("moss_carpet")) return false;   // 1/16 tall: still collides
        String[] open = {"torch", "sign", "banner", "rail", "button", "pressure_plate", "vine", "ladder", "flower",
                "tulip", "poppy", "orchid", "allium", "bluet", "daisy", "dandelion", "cornflower", "sapling",
                "short_grass", "tall_grass", "fern", "lily_of_the_valley", "cobweb", "tripwire", "string", "lichen",
                "pink_petals", "wheat", "carrots", "potatoes", "beetroots", "sugar_cane", "kelp", "seagrass", "roots",
                "sprouts", "bush", "redstone_wire", "lever", "frame", "dead_bush", "cave_vines", "weeping", "twisting",
                "small_dripleaf", "spore_blossom", "hanging_roots", "glow_lichen"};
        if (id.equals("azalea") || id.equals("flowering_azalea") || id.contains("leaves")) return false;
        if (id.contains("potted") || id.equals("flower_pot")) return false;
        for (String n : open) if (id.contains(n)) return true;
        return false;
    }

    /**
     * Height (0..1.5) of the surface a player stands on when standing on top of this block, or -1 if it has
     * no top to stand on (passable or a ceiling-only shape).
     */
    public static double standTop(String b) {
        if (passable(b)) return -1;
        String id = States.id(b);
        if (id.endsWith("_slab")) { String t = prop(b, "type"); return "bottom".equals(t) ? 0.5 : 1.0; }
        if (id.endsWith("_stairs")) return 1.0;
        if (id.endsWith("_carpet") || id.equals("moss_carpet")) return 0.0625;
        if (id.equals("snow")) { String l = prop(b, "layers"); int n = l == null ? 1 : Integer.parseInt(l); return (n - 1) / 8.0; }
        if (kind(id) == 1 || kind(id) == 3 || kind(id) == 4) return 1.5;
        if (id.equals("farmland") || id.equals("dirt_path")) return 0.9375;
        if (id.equals("soul_sand") || id.equals("mud")) return 0.875;
        return 1.0;
    }

    /**
     * Opaque vertical band [lo, hi] (fractions of the cell) for the sightline check, or null if a player can see
     * through it. Conservative: glass, panes, bars, fences, walls, leaves, plants and small props are see-through.
     */
    public static double[] sightBand(String b) {
        if (States.isAir(b)) return null;
        String id = States.id(b);
        if (id.contains("glass") || id.equals("iron_bars") || kind(id) != 0 || id.contains("leaves")
                || id.equals("barrier") || id.equals("light") || id.equals("water") || id.equals("lava")) return null;
        if (id.endsWith("_slab")) {
            String t = prop(b, "type");
            return "bottom".equals(t) ? new double[]{0, 0.5} : "top".equals(t) ? new double[]{0.5, 1} : new double[]{0, 1};
        }
        if (id.endsWith("_stairs")) return "top".equals(prop(b, "half")) ? new double[]{0.5, 1} : new double[]{0, 0.5};
        if (id.equals("snow")) { String l = prop(b, "layers"); int n = l == null ? 1 : Integer.parseInt(l); return new double[]{0, n / 8.0}; }
        if (id.contains("carpet") || id.contains("trapdoor") || id.contains("door") || id.contains("rail")
                || id.contains("pressure_plate") || id.contains("button")) return null;
        if (!fullSolid(b)) {
            // chunky props that really block a view
            if (id.equals("composter") || id.equals("cauldron") || id.equals("water_cauldron") || id.equals("hopper")
                    || id.endsWith("chest") || id.equals("lectern") || id.equals("honey_block") || id.equals("soul_sand")
                    || id.equals("mud") || id.equals("farmland") || id.equals("dirt_path") || id.equals("carved_pumpkin")
                    || id.equals("jack_o_lantern") || id.equals("pumpkin") || id.equals("melon") || id.contains("shulker_box")) return new double[]{0, 0.9};
            return null;
        }
        return new double[]{0, 1};
    }
}
