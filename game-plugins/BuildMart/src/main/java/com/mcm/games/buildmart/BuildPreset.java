package com.mcm.games.buildmart;

import org.bukkit.Material;

import java.util.*;

/**
 * A small target build (max 5x5x5). Layers are given bottom to top; each layer is a list of rows (z),
 * each row a string over x. '.' is air. The build is centred in the 5x5 plot footprint.
 */
final class BuildPreset {
    static final int SIZE = 5;

    /** Materials only obtainable at a shop counter (count double towards difficulty). */
    static final Set<Material> SHOP_MATERIALS = EnumSet.of(Material.GLASS, Material.STONE_BRICKS, Material.BRICKS, Material.GOLD_BLOCK);

    private static final Map<Character, Material> LEGEND = new HashMap<>();
    static {
        LEGEND.put('a', Material.OAK_PLANKS);
        LEGEND.put('L', Material.OAK_LOG);
        LEGEND.put('s', Material.SPRUCE_PLANKS);
        LEGEND.put('B', Material.BIRCH_PLANKS);
        LEGEND.put('c', Material.COBBLESTONE);
        LEGEND.put('C', Material.STONE_BRICKS);
        LEGEND.put('R', Material.BRICKS);
        LEGEND.put('#', Material.GLASS);
        LEGEND.put('i', Material.IRON_BLOCK);
        LEGEND.put('$', Material.GOLD_BLOCK);
        LEGEND.put('G', Material.GLOWSTONE);
        LEGEND.put('q', Material.QUARTZ_BLOCK);
        LEGEND.put('d', Material.SANDSTONE);
        LEGEND.put('x', Material.SNOW_BLOCK);
        LEGEND.put('w', Material.WHITE_WOOL);
        LEGEND.put('r', Material.RED_WOOL);
        LEGEND.put('o', Material.ORANGE_WOOL);
        LEGEND.put('y', Material.YELLOW_WOOL);
        LEGEND.put('l', Material.LIME_WOOL);
        LEGEND.put('g', Material.GREEN_WOOL);
        LEGEND.put('e', Material.LIGHT_BLUE_WOOL);
        LEGEND.put('b', Material.BLUE_WOOL);
        LEGEND.put('k', Material.BLACK_WOOL);
        LEGEND.put('p', Material.PINK_WOOL);
        LEGEND.put('n', Material.BROWN_WOOL);
        LEGEND.put('v', Material.OAK_LEAVES);
        LEGEND.put('u', Material.PUMPKIN);
        LEGEND.put('h', Material.HAY_BLOCK);
        LEGEND.put('m', Material.MELON);
        LEGEND.put('z', Material.CLAY);
    }

    final String name;
    final int sx, sy, sz;
    final int ox, oz;
    final int blocks;
    final int stars;
    final Map<Material, Integer> counts;
    private final Material[][][] grid; // [y][z][x], null = air

    private BuildPreset(String name, String[][] layers) {
        this.name = name;
        this.sy = layers.length;
        this.sz = layers[0].length;
        this.sx = layers[0][0].length();
        if (sy > SIZE || sz > SIZE || sx > SIZE) throw new IllegalStateException("Preset too big: " + name);
        this.ox = (SIZE - sx) / 2;
        this.oz = (SIZE - sz) / 2;
        grid = new Material[sy][sz][sx];
        Map<Material, Integer> c = new LinkedHashMap<>();
        int n = 0, shop = 0;
        for (int y = 0; y < sy; y++) {
            if (layers[y].length != sz) throw new IllegalStateException("Ragged layer in " + name);
            for (int z = 0; z < sz; z++) {
                String row = layers[y][z];
                if (row.length() != sx) throw new IllegalStateException("Ragged row in " + name);
                for (int x = 0; x < sx; x++) {
                    char ch = row.charAt(x);
                    if (ch == '.') continue;
                    Material m = LEGEND.get(ch);
                    if (m == null) throw new IllegalStateException("Unknown char '" + ch + "' in " + name);
                    grid[y][z][x] = m;
                    c.merge(m, 1, Integer::sum);
                    n++;
                    if (SHOP_MATERIALS.contains(m)) shop++;
                }
            }
        }
        this.blocks = n;
        this.counts = Collections.unmodifiableMap(c);
        int score = n + 2 * shop;
        this.stars = score < 12 ? 1 : score < 20 ? 2 : score < 30 ? 3 : score < 42 ? 4 : 5;
    }

    /** Material wanted at plot-local coordinates (0..4 each), AIR if none. */
    Material at(int x, int y, int z) {
        int gx = x - ox, gz = z - oz;
        if (y < 0 || y >= sy || gx < 0 || gx >= sx || gz < 0 || gz >= sz) return Material.AIR;
        Material m = grid[y][gz][gx];
        return m == null ? Material.AIR : m;
    }

    /** True if any block of this build sits in the column at plot-local x,z. */
    boolean columnUsed(int x, int z) {
        for (int y = 0; y < sy; y++) if (at(x, y, z) != Material.AIR) return true;
        return false;
    }

    String starString() {
        return "§6" + "★".repeat(stars) + "§8" + "★".repeat(5 - stars);
    }

    /** Blocks that cannot be reached by face-adjacent placement from the floor (for validation). */
    List<String> unsupported() {
        boolean[][][] ok = new boolean[sy][sz][sx];
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) {
                if (grid[y][z][x] == null || ok[y][z][x]) continue;
                boolean s = y == 0
                        || (y > 0 && ok[y - 1][z][x]) || (y + 1 < sy && ok[y + 1][z][x])
                        || (x > 0 && ok[y][z][x - 1]) || (x + 1 < sx && ok[y][z][x + 1])
                        || (z > 0 && ok[y][z - 1][x]) || (z + 1 < sz && ok[y][z + 1][x]);
                if (s) { ok[y][z][x] = true; changed = true; }
            }
        }
        List<String> bad = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++)
            if (grid[y][z][x] != null && !ok[y][z][x]) bad.add(x + "," + y + "," + z);
        return bad;
    }

    private static String[] L(String... rows) { return rows; }

    private static BuildPreset p(String name, String[]... layers) { return new BuildPreset(name, layers); }

    static final List<BuildPreset> ALL = List.of(
            p("Pixel Heart", L("..r.."), L(".rrr."), L("rrrrr"), L("rr.rr")),
            p("Pumpkin Patch", L("g.g", ".h.", "g.g"), L("u.u", "...", "u.u")),
            p("Traffic Light", L(".k."), L(".k."), L("klk"), L("kyk"), L("krk")),
            p("Snowman", L(".x.", "xxx", ".x."), L(".x.", "xxx", ".x."), L("...", ".u.", "...")),
            p("Flag", L("L...."), L("L...."), L("Lbbbb"), L("Lwwww"), L("L....")),
            p("Small Tree", L("...", ".L.", "..."), L("...", ".L.", "..."), L("vvv", "vLv", "vvv"), L(".v.", "vvv", ".v.")),
            p("Mushroom", L("...", ".q.", "..."), L("...", ".q.", "..."), L("rrr", "rrr", "rrr"), L(".r.", "rwr", ".r.")),
            p("Cactus", L("..g.."), L("ggggg"), L("g.g.g"), L("g.g.g"), L("..y..")),
            p("Fruit Stand", L("L.L", "L.L"), L("aaa", "aaa"), L("mum", "umu")),
            p("Brick Chimney", L("RR", "RR"), L("RR", "RR"), L("cc", "cc"), L("w.", "..")),
            p("Stone Well", L("ccc", "c.c", "ccc"), L("L.L", "...", "L.L"), L("aaa", "aaa", "aaa")),
            p("Tiny Hut", L("LaL", "a.a", "L.L"), L("LaL", "a.a", "L.L"), L("aaa", "aaa", "aaa")),
            p("Smiley", L(".yyy."), L("ykkky"), L("yyyyy"), L("ykyky"), L(".yyy.")),
            p("Rocket", L("o.o", ".w.", "o.o"), L(".w.", "w#w", ".w."), L(".w.", "www", ".w."), L("...", ".w.", "..."), L("...", ".r.", "...")),
            p("Golden Trophy", L("qqq", "qqq", "qqq"), L("...", ".$.", "..."), L(".$.", "$$$", ".$."), L(".$.", "$.$", ".$.")),
            p("Penguin", L("o.o", "kkk"), L("kwk", "kkk"), L("kwk", "kkk"), L("kok", "kkk")),
            p("Bumble Bee", L("ykyk", "ykyk"), L("ykyk", "ykyk"), L(".ww.", ".ww.")),
            p("Piggy", L("p..p", "....", "p..p"), L("pppp", "pppp", "pppp"), L("pp..", "pp..", "pp..")),
            p("Fountain", L("ccccc", "c...c", "c.q.c", "c...c", "ccccc"), L(".....", ".....", "..q..", ".....", "....."),
                    L(".....", "..e..", ".eee.", "..e..", ".....")),
            p("Creeper Head", L("klk", "lll", "lll"), L("lkl", "lll", "lll"), L("klk", "lll", "lll")),
            p("Watchtower", L("c.c", "...", "c.c"), L("c.c", "...", "c.c"), L("c.c", "...", "c.c"), L("sss", "sss", "sss"), L("s.s", "...", "s.s")),
            p("Desert Pyramid", L("ddddd", "ddddd", "ddddd", "ddddd", "ddddd"), L(".....", ".ddd.", ".ddd.", ".ddd.", "....."),
                    L(".....", ".....", "..$..", ".....", ".....")),
            p("Igloo", L(".xxx.", "x...x", "x...x", "x...x", ".x.x."), L(".xxx.", "x...x", "x...x", "x...x", ".xxx."),
                    L("..x..", ".xxx.", "xxxxx", ".xxx.", "..x..")),
            p("Lighthouse", L("rrr", "r.r", "rrr"), L("www", "w.w", "www"), L("rrr", "r.r", "rrr"), L("###", "#G#", "###"), L(".r.", "rrr", ".r.")),
            p("Cottage", L("LaaaL", "a...a", "a...a", "a...a", "La.aL"), L("La#aL", "#...#", "a...a", "#...#", "La.aL"),
                    L("rrrrr", "rrrrr", "rrrrr", "rrrrr", "rrrrr"))
    );
}
