package com.mcm.games.battlebox.arena;

/** Block-state string helpers (pure Java). */
public final class States {
    private States() {}

    public static String full(String b) {
        if (b == null) return null;
        int s = b.indexOf('[');
        String head = s < 0 ? b : b.substring(0, s);
        return head.indexOf(':') >= 0 ? b : "minecraft:" + b;
    }

    /** Block id without namespace and states, e.g. "oak_stairs". */
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

    private static String swapDir(String w) {
        return switch (w) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            case "west" -> "east";
            default -> w;
        };
    }

    /** The same block rotated 180 degrees around the vertical axis. */
    public static String rot180(String b) {
        if (b == null) return null;
        int s = b.indexOf('[');
        if (s < 0) return b;
        String head = b.substring(0, s);
        String body = b.substring(s + 1, b.length() - 1);
        StringBuilder out = new StringBuilder(head).append('[');
        String[] parts = body.split(",");
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split("=", 2);
            String k = swapDir(kv[0]);
            String v = kv.length > 1 ? kv[1] : "";
            if (k.equals("rotation")) {
                try { v = String.valueOf((Integer.parseInt(v) + 8) % 16); } catch (NumberFormatException ignored) { }
            } else if (v.equals("east_west") || v.equals("north_south")) {
                // axis-like shapes are unchanged by a half turn
            } else {
                String[] ws = v.split("_");
                StringBuilder nv = new StringBuilder();
                for (int j = 0; j < ws.length; j++) { if (j > 0) nv.append('_'); nv.append(swapDir(ws[j])); }
                v = nv.toString();
            }
            if (i > 0) out.append(',');
            out.append(k).append('=').append(v);
        }
        return out.append(']').toString();
    }

    public static String stairs(String mat, String facing, boolean top) {
        return mat + "_stairs[facing=" + facing + ",half=" + (top ? "top" : "bottom") + "]";
    }

    public static String slab(String mat, boolean top) {
        return mat + "_slab[type=" + (top ? "top" : "bottom") + "]";
    }

    public static String trapdoor(String mat, String facing, boolean top, boolean open) {
        return mat + "_trapdoor[facing=" + facing + ",half=" + (top ? "top" : "bottom") + ",open=" + open + "]";
    }
}
