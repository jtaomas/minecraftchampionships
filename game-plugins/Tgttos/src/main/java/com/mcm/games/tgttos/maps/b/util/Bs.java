package com.mcm.games.tgttos.maps.b.util;

/**
 * Block-state string helpers for the pure-Java generator (no Bukkit): ids, properties, quarter-turn rotation
 * and rough vanilla shape knowledge for the post-pass and the walk/stair validators.
 */
public final class Bs {
    private Bs() {}

    public static String full(String b) {
        if (b == null) return null;
        int s = b.indexOf('[');
        String head = s < 0 ? b : b.substring(0, s);
        return head.indexOf(':') >= 0 ? b : "minecraft:" + b;
    }

    /** Id without namespace and states, e.g. "oak_stairs". */
    public static String id(String b) {
        if (b == null) return "air";
        int c = b.indexOf(':');
        int s = b.indexOf('[');
        if (s < 0) s = b.length();
        if (c < 0 || c > s) c = -1;
        return b.substring(c + 1, s);
    }

    public static boolean isAir(String b) {
        if (b == null) return true;
        String i = id(b);
        return i.equals("air") || i.equals("cave_air") || i.equals("void_air");
    }

    public static String head(String b) {
        int s = b.indexOf('[');
        return s < 0 ? b : b.substring(0, s);
    }

    public static String prop(String b, String key) {
        if (b == null) return null;
        int s = b.indexOf('[');
        if (s < 0) return null;
        for (String kv : b.substring(s + 1, b.length() - 1).split(",")) {
            int e = kv.indexOf('=');
            if (e > 0 && kv.substring(0, e).equals(key)) return kv.substring(e + 1);
        }
        return null;
    }

    public static String withProp(String b, String key, String value) {
        String h = head(b);
        int s = b.indexOf('[');
        StringBuilder sb = new StringBuilder(h).append('[');
        boolean found = false, first = true;
        if (s >= 0) {
            for (String kv : b.substring(s + 1, b.length() - 1).split(",")) {
                int e = kv.indexOf('=');
                if (!first) sb.append(',');
                first = false;
                if (e > 0 && kv.substring(0, e).equals(key)) { sb.append(key).append('=').append(value); found = true; }
                else sb.append(kv);
            }
        }
        if (!found) { if (!first) sb.append(','); sb.append(key).append('=').append(value); }
        return sb.append(']').toString();
    }

    /** Same state with its properties in sorted order (for comparisons). */
    public static String norm(String b) {
        if (b == null) return null;
        int s = b.indexOf('[');
        if (s < 0) return b;
        String[] parts = b.substring(s + 1, b.length() - 1).split(",");
        java.util.Arrays.sort(parts);
        return b.substring(0, s) + "[" + String.join(",", parts) + "]";
    }

    // ------------------------------------------------------------------ rotation

    private static final String[] DIRS = {"north", "east", "south", "west"};

    private static int dirIndex(String d) {
        for (int i = 0; i < 4; i++) if (DIRS[i].equals(d)) return i;
        return -1;
    }

    /** Rotate (x, z) by q clockwise quarter turns seen from above (+x east, +z south): north -> east. */
    public static int rx(int x, int z, int q) {
        switch (q & 3) { case 1: return -z; case 2: return -x; case 3: return z; default: return x; }
    }

    public static int rz(int x, int z, int q) {
        switch (q & 3) { case 1: return x; case 2: return -z; case 3: return -x; default: return z; }
    }

    /** The same block state turned q clockwise quarter turns. */
    public static String rot(String b, int q) {
        q &= 3;
        if (b == null || q == 0) return b;
        int s = b.indexOf('[');
        if (s < 0) return b;
        String h = b.substring(0, s);
        String[] parts = b.substring(s + 1, b.length() - 1).split(",");
        StringBuilder out = new StringBuilder(h).append('[');
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            String k = kv[0];
            String v = kv.length > 1 ? kv[1] : "";
            int ki = dirIndex(k);
            if (ki >= 0) k = DIRS[(ki + q) & 3];
            if (kv[0].equals("facing")) {
                int vi = dirIndex(v);
                if (vi >= 0) v = DIRS[(vi + q) & 3];
            } else if (kv[0].equals("axis") && (q & 1) == 1) {
                if (v.equals("x")) v = "z"; else if (v.equals("z")) v = "x";
            } else if (kv[0].equals("rotation")) {
                try { v = String.valueOf((Integer.parseInt(v) + 4 * q) % 16); } catch (NumberFormatException ignored) { }
            }
            if (i > 0) out.append(',');
            out.append(k).append('=').append(v);
        }
        return out.append(']').toString();
    }

    public static String stairs(String mat, String facing, boolean top) {
        return mat + "_stairs[facing=" + facing + ",half=" + (top ? "top" : "bottom") + ",shape=straight]";
    }

    public static String slab(String mat, boolean top) {
        return mat + "_slab[type=" + (top ? "top" : "bottom") + "]";
    }

    // ------------------------------------------------------------------ shapes

    private static final String[] NOT_FULL = {"slab", "stairs", "carpet", "torch", "button", "trapdoor",
            "door", "rail", "sapling", "short_grass", "tall_grass", "fern", "sign", "banner", "chain", "rod", "potted",
            "candle", "water", "lava", "pressure_plate", "vine", "lily_pad", "lily_of", "tulip", "poppy", "orchid", "allium", "bluet",
            "daisy", "dandelion", "cornflower", "azalea", "leaves", "campfire", "bush", "dripleaf",
            "lever", "ladder", "cake", "lichen", "frame", "hopper", "cauldron", "brewing",
            "anvil", "grindstone", "lectern", "composter", "scaffolding", "dripstone", "cluster", "bud", "pickle",
            "roots", "sprouts", "berry", "flower_pot", "end_rod", "chest", "snow", "cobweb", "flower", "decorated_pot",
            "enchanting", "glass_pane", "iron_bars", "_fence", "_wall", "gate", "pink_petals", "lantern", "bell", "head", "skull"};

    /** Full sturdy cube (fences/panes/walls connect to it, carpets may sit on it, it blocks chest lids). */
    public static boolean fullSolid(String b) {
        if (isAir(b)) return false;
        String id = id(b);
        if (id.endsWith("_slab")) return "double".equals(prop(b, "type"));
        if (id.equals("glass") || id.endsWith("_stained_glass") || id.equals("sea_lantern") || id.equals("moss_block")
                || id.equals("mud_bricks")) return true;
        if (id.equals("azalea_leaves") || id.equals("flowering_azalea_leaves")) return false;
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

    /** A player can occupy this cell (no collision box). Lava counts as blocked. */
    public static boolean passable(String b) {
        if (isAir(b)) return true;
        String id = id(b);
        if (id.equals("water") || id.equals("light")) return true;
        if (id.equals("lava")) return false;
        String[] open = {"torch", "sign", "banner", "rail", "button", "pressure_plate", "vine", "ladder", "flower",
                "tulip", "poppy", "orchid", "allium", "bluet", "daisy", "dandelion", "cornflower", "sapling",
                "short_grass", "tall_grass", "fern", "lily_of_the_valley", "cobweb", "lichen", "pink_petals",
                "roots", "sprouts", "bush", "lever", "dead_bush", "glow_lichen"};
        if (id.equals("azalea") || id.equals("flowering_azalea") || id.contains("leaves")) return false;
        if (id.contains("potted") || id.equals("flower_pot")) return false;
        for (String n : open) if (id.contains(n)) return true;
        return false;
    }

    public static boolean isLadder(String b) { return b != null && id(b).equals("ladder"); }

    /** Height of the surface on top of this block (0..1.5), or -1 when it has no top to stand on. */
    public static double standTop(String b) {
        if (passable(b)) return -1;
        String id = id(b);
        if (id.equals("lava")) return -1;
        if (id.endsWith("_slab")) return "bottom".equals(prop(b, "type")) ? 0.5 : 1.0;
        if (id.endsWith("_carpet")) return 0.0625;
        if (kind(id) == 1 || kind(id) == 3 || kind(id) == 4) return 1.5;
        if (id.equals("chest")) return 0.875;
        return 1.0;
    }
}
