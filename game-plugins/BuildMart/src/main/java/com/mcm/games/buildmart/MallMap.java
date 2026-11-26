package com.mcm.games.buildmart;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Axis;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;

import java.util.*;

/**
 * The Classic Mall (MCC 3 to 13) and the private team build zones, generated once into the void world.
 *
 * Reference images (open them before changing a part):
 * mall: bm-1/images/wiki_original_map_overview.png, wiki_original_halloween_topdown.png, bm-1/frames/021550..021950,
 * cart/frames/ref/18_old_mart_departments.png; build zone: bm-1/frames/021840, 021920, 022030, 022140, bm-2 build renders.
 *
 * Layout (x = east, z = south, floor top at y 64):
 *   plaza (spawn from the build zones, MCC store facade with the portal to your build zone, pool)
 *   -> main concourse north, LUMBER (west) and OUTDOOR (east) near the plaza, pitfalls mid-concourse
 *   -> cross hall with STONE (left), COLOURS (centre), ORES (right) along its back wall.
 *   -> overflow halls share the cross hall: WOOD DETAILS, GARDEN, BRICK & SLAB and REDSTONE & METAL open off its
 *      south wall, and GLASS & CLAY is the wing west of the cross hall. Each hall is sized so its 3-column bays fit.
 * Every walkable mall floor is FROSTED ICE (ages 0-3 are retextured by the pack into white tiles, red carpet, yellow tiles
 * and hazard stripes). Boats slide on it exactly like on ice, which is how MCC's trolleys were fast: see TROLLEY.md.
 */
final class MallMap {
    static final int FY = 64;              // floor block level (stand at 65)
    static final int VERSION = 14;          // bump to force regeneration
    static final int FALL_Y = 55;
    static final int ROOMS = 10;
    static final int ROOM_Z = 300;

    record Box(int x1, int z1, int x2, int z2) {
        boolean in(int x, int z) { return x >= x1 && x <= x2 && z >= z1 && z <= z2; }
    }
    record Label(double x, double y, double z, String text, float scale) { }
    record Counter(int x, int y, int z, Material item, String name) { }
    /** entrance: 'E' / 'W' = door in the east / west wall, 'S' / 'N' = door in the south / north wall. */
    record Dept(String name, Box box, int roofY, char entrance, int doorC, int doorHalf, Material colour, List<BlockData> stock) { }
    /** A status LED on a server rack (redstone lamps blink during the game). */
    final List<long[]> leds = new ArrayList<>();

    // halls (walls included)
    static final Box PLAZA = new Box(-31, 0, 31, 46);
    static final Box CORRIDOR = new Box(-13, -86, 13, 0);
    static final Box CROSS = new Box(-67, -102, 67, -86);
    static final Box LUMBER = new Box(-51, -42, -13, -2);
    static final Box WOOD_DETAILS = new Box(-67, -86, -41, -46);
    static final Box OUTDOOR = new Box(13, -42, 51, -2);
    static final Box GARDEN = new Box(-40, -86, -13, -46);
    static final Box STONE = new Box(-67, -142, -23, -102);
    static final Box BRICK_SLAB = new Box(41, -86, 67, -46);
    static final Box COLOURS = new Box(-21, -150, 21, -102);
    static final Box GLASS_CLAY = new Box(-114, -122, -67, -86);
    static final Box ORES = new Box(23, -142, 67, -102);
    static final Box REDSTONE_METAL = new Box(13, -86, 40, -46);
    static final Box[] PITS = {new Box(-3, -66, 1, -52), new Box(6, -40, 9, -34), new Box(-31, -97, -27, -90)};
    static final Box MALL_BOUNDS = new Box(-114, -150, 67, 46);

    /** The MCC store portal in the plaza (to your build zone). */
    static final Box MALL_PORTAL = new Box(-3, 46, 3, 46);
    static final double[] MALL_ARRIVE = {0.5, 65, 38.5, 180};

    final World w;
    final Map<Long, BlockData> stock = new HashMap<>();
    final List<Label> labels = new ArrayList<>();
    final List<Counter> counters = new ArrayList<>();
    final List<Dept> depts = new ArrayList<>();
    private static final Set<String> NEW_HALLS = Set.of(
            "WOOD DETAILS", "GARDEN", "BRICK & SLAB", "GLASS & CLAY", "REDSTONE & METAL");
    private long placed;

    MallMap(World w) {
        this.w = w;
        depts.add(new Dept("LUMBER", LUMBER, 80, 'E', -22, 5, Material.ORANGE_CONCRETE, list(
                "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log",
                "stripped_oak_log", "stripped_spruce_log", "stripped_dark_oak_log",
                "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
                "bamboo_planks")));
        depts.add(new Dept("WOOD DETAILS", WOOD_DETAILS, 80, 'N', -54, 3, Material.BROWN_CONCRETE, list(
                "oak_slab", "spruce_slab", "birch_slab", "acacia_slab",
                "spruce_stairs", "birch_stairs", "acacia_stairs",
                "oak_fence", "spruce_fence", "birch_fence", "oak_door",
                "oak_trapdoor", "spruce_trapdoor", "dark_oak_trapdoor",
                "acacia_button", "bamboo_button", "spruce_button",
                "ladder", "barrel", "note_block", "campfire")));
        depts.add(new Dept("OUTDOOR", OUTDOOR, 80, 'W', -22, 5, Material.LIME_CONCRETE, list(
                "grass_block", "dirt", "coarse_dirt", "podzol", "sand", "gravel", "snow_block", "ice", "pumpkin", "cactus",
                "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves")));
        depts.add(new Dept("GARDEN", GARDEN, 80, 'N', -26, 3, Material.GREEN_CONCRETE, list(
                "dandelion", "poppy", "red_tulip", "orange_tulip", "oxeye_daisy", "lily_of_the_valley", "allium",
                "cornflower", "short_grass", "potted_red_tulip", "brown_mushroom_block", "mushroom_stem", "moss_block",
                "dried_kelp_block", "honey_block", "honeycomb_block", "slime_block")));
        depts.add(new Dept("STONE", STONE, 80, 'S', -45, 5, Material.CYAN_CONCRETE, list(
                "stone", "cobblestone", "smooth_stone", "andesite", "diorite", "granite", "sandstone",
                "quartz_block", "smooth_quartz", "end_stone", "purpur_block", "netherrack", "soul_soil", "obsidian",
                "blackstone", "calcite", "cobbled_deepslate", "polished_deepslate", "prismarine", "dark_prismarine",
                "polished_diorite", "smooth_sandstone", "purpur_pillar", "quartz_pillar",
                "furnace", "stonecutter", "smithing_table")));
        depts.add(new Dept("BRICK & SLAB", BRICK_SLAB, 80, 'N', 54, 3, Material.RED_CONCRETE, list(
                "bricks", "stone_bricks", "mossy_stone_bricks", "mud_bricks", "nether_bricks", "chiseled_stone_bricks",
                "cobblestone_slab", "stone_slab", "stone_brick_slab", "purpur_slab", "quartz_slab",
                "smooth_stone_slab", "smooth_quartz_slab", "stone_brick_stairs", "quartz_stairs",
                "andesite_wall", "stone_brick_wall", "nether_brick_wall", "end_stone_brick_wall")));
        List<String> col = new ArrayList<>();
        String[] cs = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "green", "cyan",
                "light_blue", "blue", "purple", "magenta", "pink"};
        for (String c : cs) col.add(c + "_concrete");
        for (String c : cs) col.add(c + "_wool");
        depts.add(new Dept("COLOURS", COLOURS, 80, 'S', 0, 6, Material.PURPLE_CONCRETE, list(col.toArray(String[]::new))));
        List<String> glass = new ArrayList<>();
        for (String c : cs) glass.add(c + "_stained_glass");
        glass.addAll(List.of("glass",
                "light_blue_glazed_terracotta", "lime_glazed_terracotta", "yellow_glazed_terracotta",
                "red_glazed_terracotta", "black_glazed_terracotta", "blue_glazed_terracotta",
                "green_glazed_terracotta", "terracotta", "orange_terracotta", "black_terracotta",
                "cyan_terracotta", "red_terracotta", "white_terracotta", "yellow_terracotta",
                "black_carpet", "magenta_carpet", "purple_carpet", "red_carpet", "yellow_carpet",
                "black_stained_glass_pane", "light_blue_stained_glass_pane", "orange_stained_glass_pane",
                "purple_stained_glass_pane", "glass_pane", "yellow_concrete_powder"));
        depts.add(new Dept("GLASS & CLAY", GLASS_CLAY, 80, 'E', -94, 3, Material.LIGHT_BLUE_CONCRETE,
                list(glass.toArray(String[]::new))));
        depts.add(new Dept("ORES", ORES, 80, 'S', 45, 5, Material.BLACK_CONCRETE, list(
                "coal_block", "coal_ore", "iron_block", "gold_block", "diamond_block", "emerald_block", "lapis_block",
                "redstone_block", "copper_block", "nether_wart_block", "glowstone",
                "bone_block", "warped_wart_block", "cut_copper", "oxidized_copper", "oxidized_cut_copper", "sea_lantern")));
        depts.add(new Dept("REDSTONE & METAL", REDSTONE_METAL, 80, 'N', 26, 3, Material.GRAY_CONCRETE, list(
                "iron_bars", "iron_door", "chain", "hopper", "cauldron", "rail", "lightning_rod",
                "lantern", "torch", "redstone_torch", "stone_button", "polished_blackstone_button",
                "stone_pressure_plate", "light_weighted_pressure_plate")));
    }

    private static List<BlockData> list(String... ids) {
        List<BlockData> out = new ArrayList<>();
        for (String id : ids) {
            BlockData bd = Bukkit.createBlockData("minecraft:" + id);
            if (bd instanceof Leaves l) l.setPersistent(true);
            out.add(bd);
        }
        return out;
    }

    // ------------------------------------------------------------------ palette

    static final BlockData AIR = Material.AIR.createBlockData();
    static final BlockData ICE0 = Material.FROSTED_ICE.createBlockData("[age=0]");   // white tile floor
    static final BlockData ICE1 = Material.FROSTED_ICE.createBlockData("[age=1]");   // red carpet runner
    static final BlockData ICE2 = Material.FROSTED_ICE.createBlockData("[age=2]");   // yellow tiles
    static final BlockData ICE3 = Material.FROSTED_ICE.createBlockData("[age=3]");   // hazard stripes
    private static BlockData b(Material m) { return m.createBlockData(); }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private void set(int x, int y, int z, BlockData d) {
        if (dry) return;
        Block bl = w.getBlockAt(x, y, z);
        if (d.getMaterial().isAir() && bl.getType().isAir()) return;
        bl.setBlockData(d, false);
        placed++;
    }
    private void set(int x, int y, int z, Material m) { set(x, y, z, b(m)); }

    // ------------------------------------------------------------------ generation

    long generate() {
        placed = 0;
        stock.clear(); labels.clear(); counters.clear(); leds.clear();
        for (int cx = (MALL_BOUNDS.x1 - 2) >> 4; cx <= (MALL_BOUNDS.x2 + 2) >> 4; cx++)
            for (int cz = (MALL_BOUNDS.z1 - 2) >> 4; cz <= (MALL_BOUNDS.z2 + 2) >> 4; cz++) w.getChunkAt(cx, cz).load(true);
        // clear the old mall volume (regeneration)
        for (int x = MALL_BOUNDS.x1 - 1; x <= MALL_BOUNDS.x2 + 1; x++)
            for (int z = MALL_BOUNDS.z1 - 1; z <= MALL_BOUNDS.z2 + 1; z++)
                for (int y = 30; y <= 92; y++) set(x, y, z, AIR);

        for (Dept d : depts) hall(d.box, d.roofY);
        hall(CROSS, 90);
        hall(CORRIDOR, 90);
        hall(PLAZA, 90);
        // openings between halls
        opening(-12, 0, 12, 0);          // plaza <-> concourse
        opening(-12, -86, 12, -86);      // concourse <-> cross hall
        opening(-13, -27, -13, -17);     // LUMBER
        opening(13, -27, 13, -17);       // OUTDOOR
        opening(-50, -102, -40, -102);   // STONE
        opening(-6, -102, 6, -102);      // COLOURS
        opening(40, -102, 50, -102);     // ORES
        opening(-57, -86, -51, -86);     // WOOD DETAILS
        opening(-29, -86, -23, -86);     // GARDEN
        opening(23, -86, 29, -86);       // REDSTONE & METAL
        opening(51, -86, 57, -86);       // BRICK & SLAB
        opening(-67, -97, -67, -91);     // GLASS & CLAY (west wing)

        floorDetails();
        concourse();
        for (Box p : PITS) pit(p);
        plazaDetails();
        signs();
        for (Dept d : depts) { entrance(d); racks(d); }
        counters();
        for (int i = 0; i < ROOMS; i++) room(i);
        return placed;
    }

    private Counter counter(int x, int z, Material item, String name) {
        set(x, FY + 1, z, Material.BARREL);
        labels.add(new Label(x + 0.5, FY + 2.3, z + 0.5, "§e" + name + "\n§7right-click", 1.0f));
        return new Counter(x, FY + 1, z, item, name);
    }

    /** Wall material by height: grey base, yellow band, red shopfront, white cornice, white upper floor, red/yellow bands. */
    private static Material wallAt(int y) {
        int r = y - FY;
        if (r <= 2) return Material.LIGHT_GRAY_CONCRETE;
        if (r == 3) return Material.GOLD_BLOCK;
        if (r <= 8) return Material.RED_CONCRETE;
        if (r == 9) return Material.QUARTZ_BLOCK;
        if (r == 14) return Material.RED_CONCRETE;
        if (r == 23) return Material.YELLOW_CONCRETE;
        return Material.WHITE_CONCRETE;
    }

    private void hall(Box b, int roofY) {
        for (int x = b.x1; x <= b.x2; x++) {
            for (int z = b.z1; z <= b.z2; z++) {
                set(x, FY - 1, z, Material.WHITE_CONCRETE);
                boolean edge = x == b.x1 || x == b.x2 || z == b.z1 || z == b.z2;
                if (edge) {
                    for (int y = FY; y <= roofY; y++) set(x, y, z, wallAt(y));
                } else {
                    set(x, FY, z, ICE0);
                    boolean beam = (x - b.x1) % 6 == 0 || (z - b.z1) % 6 == 0;
                    set(x, roofY, z, beam ? Material.WHITE_CONCRETE : Material.LIGHT_BLUE_STAINED_GLASS);
                }
            }
        }
    }

    private void opening(int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                set(x, FY - 1, z, Material.WHITE_CONCRETE);
                set(x, FY, z, ICE0);
                for (int y = FY + 1; y <= FY + 9; y++) set(x, y, z, AIR);
            }
    }

    private void floorDetails() {
        // red carpet runners hugging the concourse walls and looping the plaza (021550, 021640, 021950, Halloween still)
        for (int z = -85; z <= -1; z++) for (int x : new int[]{-11, -10, 10, 11}) set(x, FY, z, ICE1);
        for (int x = -29; x <= 29; x++) for (int z : new int[]{2, 3, 43, 44}) set(x, FY, z, ICE1);
        for (int z = 2; z <= 44; z++) for (int x : new int[]{-29, -28, 28, 29}) set(x, FY, z, ICE1);
        for (int z = 0; z <= 3; z++) for (int x : new int[]{-11, -10, 10, 11}) set(x, FY, z, ICE1);
        // yellow tile patch at the far junction (021550, 021730)
        for (int x = -7; x <= 7; x++) for (int z = -98; z <= -89; z++) set(x, FY, z, ICE2);
        // cross hall runner along the shopfronts
        for (int x = -66; x <= 66; x++) set(x, FY, -100, ICE1);
    }

    private void concourse() {
        // white columns with grey base and yellow ring (021620) every 6 blocks, upper balconies with glass rails + planters
        for (int z = -84; z <= -2; z += 6) {
            for (int x : new int[]{-12, 12}) {
                if (z >= -29 && z <= -15) continue;          // keep the LUMBER / OUTDOOR entrances clear
                set(x, FY + 1, z, Material.LIGHT_GRAY_CONCRETE);
                set(x, FY + 2, z, Material.QUARTZ_PILLAR);
                set(x, FY + 3, z, Material.GOLD_BLOCK);
                for (int y = FY + 4; y <= FY + 8; y++) set(x, y, z, Material.QUARTZ_PILLAR);
            }
        }
        for (int z = -85; z <= -1; z++) {
            for (int s : new int[]{-1, 1}) {
                for (int k = 10; k <= 12; k++) set(s * k, FY + 9, z, Material.SMOOTH_QUARTZ);
                set(s * 10, FY + 10, z, Material.WHITE_STAINED_GLASS_PANE);
                if (Math.floorMod(z, 8) == 3) set(s * 12, FY + 10, z, Material.POTTED_AZALEA_BUSH);
            }
        }
        // closed shopfronts with windows between the departments (yellow-framed windows, 021950)
        for (int z = -80; z <= -32; z += 8) {
            for (int s : new int[]{-1, 1}) {
                int x = s * 13;
                for (int dz = 0; dz < 5; dz++) {
                    set(x, FY + 5, z + dz, Material.GOLD_BLOCK);
                    set(x, FY + 8, z + dz, Material.GOLD_BLOCK);
                    for (int y = FY + 6; y <= FY + 7; y++)
                        set(x, y, z + dz, (dz == 0 || dz == 4) ? Material.GOLD_BLOCK : Material.BLACK_STAINED_GLASS);
                }
            }
        }
    }

    private void pit(Box p) {
        // black shaft with a red lip (021610, 021620): drop through the floor into the void
        for (int x = p.x1 - 1; x <= p.x2 + 1; x++)
            for (int z = p.z1 - 1; z <= p.z2 + 1; z++) {
                boolean inside = p.in(x, z);
                if (inside) { for (int y = 30; y <= FY; y++) set(x, y, z, AIR); }
                else {
                    for (int y = 40; y < FY; y++) set(x, y, z, Material.BLACK_CONCRETE);
                    set(x, FY, z, ICE1);
                }
            }
    }

    private void plazaDetails() {
        // pool (021700, 021710)
        for (int x = 14; x <= 24; x++)
            for (int z = 18; z <= 30; z++) {
                boolean rim = x == 14 || x == 24 || z == 18 || z == 30;
                set(x, FY - 1, z, rim ? Material.WHITE_CONCRETE : Material.LIGHT_BLUE_CONCRETE);
                set(x, FY, z, rim ? b(Material.QUARTZ_BLOCK) : b(Material.WATER));
            }
        // the MCC store facade over the build zone portal (021700, 021710): red panel, cream MCC letters, yellow band
        int z = PLAZA.z2;
        for (int x = -22; x <= 22; x++) {
            set(x, FY + 9, z, Material.GOLD_BLOCK);
            for (int y = FY + 10; y <= FY + 25; y++) set(x, y, z, Material.RED_CONCRETE);
            set(x, FY + 26, z, Material.ORANGE_CONCRETE);
            for (int y = FY + 1; y <= FY + 8; y++) set(x, y, z, Material.GRAY_CONCRETE);
        }
        int w2 = BlockFont.width("MCC") * 2;
        int x0 = -w2 / 2;
        BlockFont.draw("MCC", (c, r) -> {
            for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++)
                set(x0 + c * 2 + dx, FY + 24 - r * 2 - dy, z, Material.SMOOTH_SANDSTONE);
        });
        // portal: quartz frame, purple portal blocks
        for (int x = -4; x <= 4; x++) for (int y = FY; y <= FY + 8; y++) set(x, y, z, Material.QUARTZ_BLOCK);
        Orientable portal = (Orientable) Material.NETHER_PORTAL.createBlockData();
        portal.setAxis(Axis.X);
        for (int x = -3; x <= 3; x++) for (int y = FY + 1; y <= FY + 7; y++) set(x, y, z, portal);
        labels.add(new Label(0.5, FY + 8.6, z - 0.6, "§f§lBUILD ZONE", 1.6f));
    }

    private void signs() {
        // LUMBER (orange) on the west wall facing east, OUTDOOR (green) on the east wall facing west (021640, 021950)
        boardOnX(-12, -22, "LUMBER", Material.ORANGE_CONCRETE, false);
        boardOnX(12, -22, "OUTDOOR", Material.LIME_CONCRETE, true);
        // STONE (teal), COLOURS (purple + rainbow), ORES (black) at the far end facing the concourse (021620)
        boardOnZ(-45, "STONE", Material.CYAN_CONCRETE, false);
        boardOnZ(0, "COLOURS", Material.PURPLE_CONCRETE, true);
        boardOnZ(45, "ORES", Material.BLACK_CONCRETE, false);
        // the five overflow halls get floating name signs (BlockFont only contains the original department letters)
        for (Dept d : depts) if (NEW_HALLS.contains(d.name)) hallLabel(d);
    }

    /** Floating name sign over a new hall's door, readable from the hall or the cross hall. */
    private void hallLabel(Dept d) {
        double x = switch (d.entrance) {
            case 'E' -> d.box.x2 + 0.5;
            case 'W' -> d.box.x1 + 0.5;
            default -> d.doorC + 0.5;
        };
        double z = switch (d.entrance) {
            case 'N' -> d.box.z1 + 0.5;
            case 'S' -> d.box.z2 + 0.5;
            default -> d.doorC + 0.5;
        };
        String c = switch (d.colour) {
            case BROWN_CONCRETE -> "§6";
            case GREEN_CONCRETE -> "§a";
            case RED_CONCRETE -> "§c";
            case LIGHT_BLUE_CONCRETE -> "§b";
            case GRAY_CONCRETE -> "§7";
            default -> "§f";
        };
        labels.add(new Label(x, FY + 11.2, z, c + "● §f§l" + d.name, 1.0f));
    }

    /** Board in a constant-x plane. east = the board faces east (+x side readable). */
    private void boardOnX(int x, int zc, String text, Material bg, boolean faceWest) {
        int tw = BlockFont.width(text), h = BlockFont.H + 4, wd = tw + 4;
        int yTop = FY + 21;
        int s = faceWest ? -1 : 1;                 // letters stand out toward the viewer
        int zStart = faceWest ? zc - wd / 2 : zc + wd / 2;   // left edge as seen by the viewer
        int dir = faceWest ? 1 : -1;
        for (int i = 0; i < wd; i++)
            for (int y = yTop - h + 1; y <= yTop; y++) set(x, y, zStart + dir * i, bg);
        BlockFont.draw(text, (c, r) -> set(x + s, yTop - 2 - r, zStart + dir * (c + 2), Material.WHITE_CONCRETE));
        for (int i : new int[]{2, wd - 3}) for (int y = yTop + 1; y <= 89; y++) set(x, y, zStart + dir * i, Material.CHAIN);
    }

    /** Board in a constant-z plane on the cross hall's back wall, readable from the south. */
    private void boardOnZ(int xc, String text, Material bg, boolean rainbow) {
        int tw = BlockFont.width(text), h = BlockFont.H + 4 + (rainbow ? 4 : 0), wd = tw + 4;
        int z = CROSS.z1 + 1, yBot = FY + 11, yTop = yBot + h - 1;
        int xStart = xc - wd / 2;
        for (int i = 0; i < wd; i++) for (int y = yBot; y <= yTop; y++) set(xStart + i, y, z, bg);
        BlockFont.draw(text, (c, r) -> set(xStart + 2 + c, yBot + 1 + BlockFont.H - 1 - r + 1, z + 1, Material.WHITE_CONCRETE));
        if (rainbow) {
            Material[] rb = {Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                    Material.LIGHT_BLUE_CONCRETE};
            int yBase = yBot + BlockFont.H + 3;
            for (int k = 0; k < rb.length; k++) {
                int rad = 8 - k;
                for (int dx = -rad; dx <= rad; dx++) {
                    double hgt = Math.sqrt(Math.max(0, rad * rad - dx * dx)) * 0.45;
                    set(xc + dx, yBase + (int) Math.round(hgt) - 2 + 1, z + 1, rb[k]);
                }
            }
        }
    }

    // ------------------------------------------------------------------ departments: block racks

    /*
     * MCC's departments (bm-1/frames/021810, cart clip 116-134 s, 18_old_mart_departments.png) are warehouses packed with
     * tall grey racking: posts with ladders, thin shelves, every bay stacked full of one block, yellow/black hazard
     * stripes along the foot of every rack, narrow white aisles, glass roof. The owner asked for the racks to read as a
     * data centre, so each bay is a "server cabinet": 4 shelves of blocks slotted in like servers, a black cap with a
     * status light (redstone lamps blink in game), a wire cable tray on top, cable trays across the aisles under the roof,
     * and black end-of-row panels with green lights and a RACK label. Mall palette everywhere else.
     *
     * Local frame per department: u runs from the back wall (u0) to the door wall (u1), v runs across.
     *   u0 .. u0+3   back aisle (4 wide)
     *   rows of cabinets: post, 3 stock columns, post, ..., rows 2 deep (back to back), aisles 4 wide
     *   u1-5 .. u1   main aisle along the door wall (5 wide, trolleys)
     */
    private record Frame(Dept d, int u0, int u1, int v0, int v1) {
        int x(int u, int v) {
            return switch (d.entrance()) { case 'E' -> u; case 'W' -> d.box().x1() + d.box().x2() - u; default -> v; };
        }
        int z(int u, int v) {
            return switch (d.entrance()) {
                case 'S' -> u;
                case 'N' -> d.box().z1() + d.box().z2() - u;
                default -> v;
            };
        }
        BlockFace minusV() { return d.entrance() == 'S' || d.entrance() == 'N' ? BlockFace.WEST : BlockFace.NORTH; }
        BlockFace plusV() { return d.entrance() == 'S' || d.entrance() == 'N' ? BlockFace.EAST : BlockFace.SOUTH; }
        boolean uIsX() { return d.entrance() == 'E' || d.entrance() == 'W'; }
    }

    private Frame frame(Dept d) {
        Box b = d.box;
        if (d.entrance == 'S' || d.entrance == 'N') return new Frame(d, b.z1 + 1, b.z2 - 1, b.x1 + 1, b.x2 - 1);
        return new Frame(d, b.x1 + 1, b.x2 - 1, b.z1 + 1, b.z2 - 1);
    }

    private void put(Frame f, int u, int y, int v, BlockData d) { set(f.x(u, v), y, f.z(u, v), d); }
    private void put(Frame f, int u, int y, int v, Material m) { put(f, u, y, v, b(m)); }

    static final int AISLE = 4;                       // rack aisles: hazard stripe, 2 white tiles, hazard stripe (021810)
    static final int TIERS = 4;                       // shelves per cabinet, each 2 blocks of stock (racks reach the roof)
    static final int CAB_CAP = FY + 1 + TIERS * 3;    // black cap with the status light (y 77)
    private static final BlockData SHELF = Material.SMOOTH_STONE_SLAB.createBlockData("[type=bottom]");
    private static final BlockData TRAY = Material.SMOOTH_STONE_SLAB.createBlockData("[type=bottom]");

    /** Department door: quartz jambs and a lintel in the department colour (18_old_mart_departments: COLOURS door). */
    private void entrance(Dept d) {
        Box b = d.box;
        int lo = d.doorC - d.doorHalf - 1, hi = d.doorC + d.doorHalf + 1;
        for (int k = lo; k <= hi; k++) {
            boolean jamb = k == lo || k == hi;
            for (int y = FY + 1; y <= FY + 10; y++) {
                Material m = jamb ? (y == FY + 3 ? Material.GOLD_BLOCK : Material.QUARTZ_PILLAR)
                        : y == FY + 10 ? d.colour : null;
                if (m == null) continue;
                switch (d.entrance) {
                    case 'E' -> set(b.x2, y, k, m);
                    case 'W' -> set(b.x1, y, k, m);
                    case 'N' -> set(k, y, b.z1, m);
                    default -> set(k, y, b.z2, m);
                }
            }
        }
    }

    private void racks(Dept d) {
        Frame f = frame(d);
        int cw = 3;                                // every bay is the approved 3-column server cabinet
        int pitch = cw + 1;
        int uA = f.u0 + AISLE;                     // first post (back aisle is u0 .. u0+AISLE-1)
        int cabs = ((f.u1 - 5) - uA) / pitch;      // main aisle keeps the last 5 (or more)
        int uB = uA + cabs * pitch;                // last post
        int width = f.v1 - f.v0 + 1;
        int rowsN = (width - AISLE) / (AISLE + 2);
        int vStart = f.v0 + AISLE + (width - AISLE - rowsN * (AISLE + 2)) / 2;
        int total = rowsN * cabs;
        List<BlockData> st = d.stock;
        BlockData bars = Material.IRON_BARS.createBlockData(f.uIsX() ? "[east=true,west=true]" : "[north=true,south=true]");
        int idx = 0;
        for (int r = 0; r < rowsN; r++) {
            int v = vStart + r * (AISLE + 2);      // the row fills v and v+1
            for (int u = uA; u <= uB; u++) {
                put(f, u, FY, v - 1, ICE3);        // hazard stripes along both faces of the row
                put(f, u, FY, v + 2, ICE3);
                put(f, u, CAB_CAP + 1, v, bars);   // wire cable tray along the top
                put(f, u, CAB_CAP + 1, v + 1, bars);
            }
            for (int c = 0; c < cabs; c++) {
                BlockData item = st.get((int) ((long) idx * st.size() / total));
                idx++;
                cabinet(f, uA + c * pitch, v, cw, item);
            }
            for (int c = 0; c <= cabs; c++) post(f, uA + c * pitch, v, c == 0 || c == cabs, (c + r) % 2 == 0);
            // RACK label over the end facing the main aisle
            double lx = (f.x(uB + 1, v) + f.x(uB + 1, v + 1)) / 2.0 + 0.5, lz = (f.z(uB + 1, v) + f.z(uB + 1, v + 1)) / 2.0 + 0.5;
            labels.add(new Label(lx, CAB_CAP + 1.4, lz, "§a● §fRACK " + d.name.charAt(0) + (r + 1), 1.1f));
        }
        // cable trays across the aisles every other post, under the glass roof
        for (int c = 1; c < cabs; c += 2)
            for (int v = f.v0; v <= f.v1; v++) put(f, uA + c * pitch, CAB_CAP + 2, v, TRAY);
    }

    private static boolean plant(BlockData item) {
        return org.bukkit.Tag.SMALL_FLOWERS.isTagged(item.getMaterial());
    }

    /** One server cabinet: cw stock columns x 2 deep x 4 shelves of 2 blocks, shelf slabs, black cap with a status light. */
    private void cabinet(Frame f, int p, int v, int cw, BlockData item) {
        boolean plant = plant(item) || item.getMaterial() == Material.SHORT_GRASS,
                cactus = item.getMaterial() == Material.CACTUS;
        for (int t = 0; t < TIERS; t++) {
            int yb = FY + 1 + t * 3;
            for (int u = p + 1; u <= p + cw; u++)
                for (int vv = v; vv <= v + 1; vv++) {
                    int x = f.x(u, vv), z = f.z(u, vv);
                    if (plant || cactus) {
                        set(x, yb, z, cactus ? Material.SAND : Material.GRASS_BLOCK);
                        set(x, yb + 1, z, item);
                        stock.put(key(x, yb + 1, z), item);
                    } else {
                        for (int y = yb; y <= yb + 1; y++) { set(x, y, z, item); stock.put(key(x, y, z), item); }
                    }
                    set(x, yb + 2, z, SHELF);
                }
        }
        int h = Math.floorMod(p * 31 + v * 17 + item.getMaterial().ordinal() * 7, 6);
        Material led = h <= 2 ? Material.REDSTONE_LAMP : h <= 4 ? Material.VERDANT_FROGLIGHT : Material.SEA_LANTERN;
        int mid = p + 1 + (cw - 1) / 2;
        for (int vv = v; vv <= v + 1; vv++) {
            for (int u = p + 1; u <= p + cw; u++) put(f, u, CAB_CAP, vv, Material.BLACK_CONCRETE);
            if (led == Material.REDSTONE_LAMP) {
                put(f, mid, CAB_CAP, vv, Material.REDSTONE_LAMP.createBlockData("[lit=true]"));
                leds.add(new long[]{f.x(mid, vv), CAB_CAP, f.z(mid, vv)});
            } else put(f, mid, CAB_CAP, vv, led);
        }
        String name = "§f" + Canonical.nice(item.getMaterial());
        for (int face : new int[]{v - 1, v + 2}) {
            int x0 = f.x(p + 1, face), x1 = f.x(p + cw, face), z0 = f.z(p + 1, face), z1 = f.z(p + cw, face);
            labels.add(new Label((x0 + x1) / 2.0 + 0.5, FY + 3.35, (z0 + z1) / 2.0 + 0.5, name, 0.7f));
        }
    }

    /** Rack post: grey upright with a ladder (MCC), or a black end-of-row panel with green status lights. */
    private void post(Frame f, int u, int v, boolean end, boolean ladderMinus) {
        for (int vv = v; vv <= v + 1; vv++)
            for (int y = FY + 1; y <= CAB_CAP; y++) {
                Material m = !end ? Material.POLISHED_ANDESITE
                        : (y - FY) % 3 == 0 ? Material.VERDANT_FROGLIGHT : Material.BLACK_CONCRETE;
                put(f, u, y, vv, m);
            }
        if (end) return;
        int lv = ladderMinus ? v - 1 : v + 2;
        BlockData ladder = Material.LADDER.createBlockData();
        ((org.bukkit.block.data.Directional) ladder).setFacing(ladderMinus ? f.minusV() : f.plusV());
        for (int y = FY + 1; y <= CAB_CAP; y++) put(f, u, y, lv, ladder);
    }

    /** Service counters (right-click for an item): next to the department doors, out of the aisles. */
    private void counters() {
        counters.add(counter(14, -30, Material.WATER_BUCKET, "Water Bucket"));      // OUTDOOR, inside the door
        counters.add(counter(-14, -30, Material.SHEARS, "Shears"));                 // LUMBER, inside the door
        counters.add(counter(38, -103, Material.FLINT_AND_STEEL, "Flint and Steel")); // ORES, left of the door
        counters.add(counter(52, -103, Material.COAL, "Coal"));                     // ORES, right of the door
    }

    // ------------------------------------------------------------------ team build zones

    static int roomX(int i) { return -270 + 60 * i; }
    static int plotX(int room, int slot) { return roomX(room) + (slot - 1) * 14; }
    static int templateZ() { return ROOM_Z - 7; }
    static int copyZ() { return ROOM_Z + 7; }
    static final int PLOT_FLOOR_Y = FY + 2;     // 7x7 floor of a plot (themed by the build)
    static double[] roomSpawn(int i) { return new double[]{roomX(i) - 21.5, FY + 1, ROOM_Z + 0.5, -90}; }
    static Box roomBox(int i) { return new Box(roomX(i) - 25, ROOM_Z - 17, roomX(i) + 25, ROOM_Z + 17); }
    static Box roomPortal(int i) { return new Box(roomX(i) - 25, ROOM_Z - 2, roomX(i) - 25, ROOM_Z + 2); }

    final List<double[]> chestSpots = new ArrayList<>();

    private void room(int i) {
        int cx = roomX(i), cz = ROOM_Z;
        Box b = roomBox(i);
        for (int ch = (b.x1 - 1) >> 4; ch <= (b.x2 + 1) >> 4; ch++)
            for (int cc = (b.z1 - 1) >> 4; cc <= (b.z2 + 1) >> 4; cc++) w.getChunkAt(ch, cc).load(true);
        for (int x = b.x1; x <= b.x2; x++)
            for (int z = b.z1; z <= b.z2; z++) {
                for (int y = FY + 1; y <= FY + 22; y++) set(x, y, z, AIR);
                set(x, FY - 1, z, Material.WHITE_CONCRETE);
                boolean edge = x == b.x1 || x == b.x2 || z == b.z1 || z == b.z2;
                boolean corner = (x == b.x1 || x == b.x2) && (z == b.z1 || z == b.z2);
                if (edge) {
                    for (int y = FY; y <= ROOM_ROOF; y++) {
                        Material m = corner ? Material.BLACK_CONCRETE
                                : y == FY ? Material.GOLD_BLOCK : y <= FY + 8 ? Material.BLUE_CONCRETE
                                : y == FY + 9 || y == ROOM_ROOF - 2 ? Material.RED_CONCRETE : Material.QUARTZ_BLOCK;
                        set(x, y, z, m);
                    }
                } else {
                    boolean trim = x == b.x1 + 1 || x == b.x2 - 1 || z == b.z1 + 1 || z == b.z2 - 1;
                    set(x, FY, z, trim ? Material.GOLD_BLOCK : Material.RED_WOOL);
                }
            }
        // black wall pillars every 8 blocks (022030)
        for (int x = b.x1 + 8; x < b.x2; x += 8) for (int z : new int[]{b.z1, b.z2})
            for (int y = FY + 1; y <= ROOM_ROOF - 3; y++) set(x, y, z, Material.BLACK_CONCRETE);
        roof(b);
        for (int s = 0; s < 3; s++) {
            plinth(plotX(i, s), templateZ());
            plinth(plotX(i, s), copyZ());
            // white path between template and copy (022140)
            for (int z = templateZ() + 6; z <= copyZ() - 6; z++)
                for (int dx = -1; dx <= 1; dx++) set(plotX(i, s) + dx, FY, z, Material.WHITE_CONCRETE);
            set(plotX(i, s) - 6, FY + 1, copyZ() + 6, Material.CHEST.createBlockData("[facing=south]"));
        }
        // portal back to the mall (purple portal in a quartz frame, 021920)
        Box p = roomPortal(i);
        for (int z = p.z1 - 1; z <= p.z2 + 1; z++) for (int y = FY; y <= FY + 6; y++) set(p.x1, y, z, Material.QUARTZ_BLOCK);
        Orientable portal = (Orientable) Material.NETHER_PORTAL.createBlockData();
        portal.setAxis(Axis.Z);
        for (int z = p.z1; z <= p.z2; z++) for (int y = FY + 1; y <= FY + 5; y++) set(p.x1, y, z, portal);
        // crafting tables, stonecutters and furnaces along the east wall ("use the provided furnaces and crafting table")
        int ex = b.x2 - 1;
        Material[] st = {Material.CRAFTING_TABLE, Material.CRAFTING_TABLE, Material.STONECUTTER, Material.FURNACE,
                Material.FURNACE, Material.FURNACE, Material.CRAFTING_TABLE};
        for (int k = 0; k < st.length; k++) {
            BlockData bd = st[k].createBlockData();
            if (bd instanceof org.bukkit.block.data.Directional dir) dir.setFacing(BlockFace.WEST);
            set(ex, FY + 1, cz - 9 + k * 3, bd);
        }
        // block catalogue board on the back wall (021920 / zz_detail_orig_buildarea_block_catalogue)
        int zw = b.z1;
        int col = cx - 23;                         // full inner wall: ten departments need the width
        for (int x = cx - 24; x <= cx + 24; x++) for (int y = FY + 2; y <= FY + 10; y++) set(x, y, zw, Material.QUARTZ_BLOCK);
        int di = 0;
        for (Dept d : depts) {
            int n = 0;
            int startCol = col;
            for (BlockData bd : d.stock) {
                if (!bd.getMaterial().isOccluding()) continue;
                set(col + n / 9, FY + 10 - n % 9, zw, bd);
                n++;
            }
            int cols = (n + 8) / 9;
            labels.add(new Label(startCol + cols / 2.0, FY + 10.2 + (di++ % 2) * 1.4, zw + 1.05, "§0" + d.name, 0.7f));
            col += cols + 1;
        }
    }

    /** Top of the build-zone walls / the roof. Clear of the plot volume (clearVolume reaches FY + 14). */
    static final int ROOM_ROOF = FY + 16;

    /**
     * Build-zone roof (owner: "it has to have a roof"). 021840 shows the walls ending in a quartz cornice with a red band
     * under the ceiling and the same daylight as the mall; the roof is the mall's glazed module: white beams every 5
     * blocks, glass between them (skylight passes stained glass at full strength, so builds stay as bright as outdoors),
     * sea lanterns where the beams cross and a stepped quartz cornice round the edge.
     */
    private void roof(Box b) {
        for (int x = b.x1 + 1; x < b.x2; x++)
            for (int z = b.z1 + 1; z < b.z2; z++) {
                boolean ring1 = x == b.x1 + 1 || x == b.x2 - 1 || z == b.z1 + 1 || z == b.z2 - 1;
                boolean ring2 = !ring1 && (x == b.x1 + 2 || x == b.x2 - 2 || z == b.z1 + 2 || z == b.z2 - 2);
                if (ring1) { set(x, ROOM_ROOF - 1, z, Material.SMOOTH_QUARTZ); set(x, ROOM_ROOF, z, Material.QUARTZ_BLOCK); continue; }
                if (ring2) { set(x, ROOM_ROOF, z, Material.RED_CONCRETE); continue; }
                boolean bx = (x - b.x1) % 5 == 0, bz = (z - b.z1) % 5 == 0;
                set(x, ROOM_ROOF, z, bx && bz ? Material.SEA_LANTERN : bx || bz ? Material.WHITE_CONCRETE : Material.GLASS);
            }
    }

    private void plinth(int px, int pz) {
        // two white quartz steps with nether-brick notches on each side, 7x7 themed floor (bm-2 renders, 022140)
        for (int dx = -5; dx <= 5; dx++)
            for (int dz = -5; dz <= 5; dz++) {
                boolean notch1 = (dx == 0 && Math.abs(dz) == 5) || (dz == 0 && Math.abs(dx) == 5);
                set(px + dx, FY + 1, pz + dz, notch1 ? Material.NETHER_BRICKS : Material.QUARTZ_BLOCK);
                if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) {
                    boolean ring = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                    boolean notch2 = (dx == 0 && Math.abs(dz) == 4) || (dz == 0 && Math.abs(dx) == 4);
                    set(px + dx, FY + 2, pz + dz, !ring ? Material.SMOOTH_QUARTZ : notch2 ? Material.NETHER_BRICKS : Material.QUARTZ_BLOCK);
                }
            }
    }

    // ------------------------------------------------------------------ queries

    /** Per-department rack audit used by /bm stock: stock size, cw=3 bays and how many items are shown. */
    String stockSummary() {
        StringBuilder sb = new StringBuilder();
        for (Dept d : depts) {
            Frame f = frame(d);
            int cw = 3, pitch = cw + 1;
            int uA = f.u0 + AISLE;
            int cabs = ((f.u1 - 5) - uA) / pitch;
            int width = f.v1 - f.v0 + 1;
            int rowsN = (width - AISLE) / (AISLE + 2);
            int total = rowsN * cabs;
            Set<Integer> shown = new HashSet<>();
            for (int i = 0; i < total; i++) shown.add((int) ((long) i * d.stock.size() / total));
            sb.append(d.name).append(" stock=").append(d.stock.size())
                    .append(" cw=").append(cw).append(" bays=").append(total).append(" shown=").append(shown.size())
                    .append(shown.size() == d.stock.size() && total >= d.stock.size() ? " OK" : " MISSING")
                    .append(" | ");
        }
        return sb.toString();
    }

    static boolean inMall(double x, double z) { return MALL_BOUNDS.in((int) Math.floor(x), (int) Math.floor(z)); }

    static int roomAt(double x, double z) {
        for (int i = 0; i < ROOMS; i++) if (roomBox(i).in((int) Math.floor(x), (int) Math.floor(z))) return i;
        return -1;
    }

    Set<Long> chunks(int rooms) {
        Set<Long> out = new HashSet<>();
        Box m = MALL_BOUNDS;
        for (int cx = (m.x1 - 2) >> 4; cx <= (m.x2 + 2) >> 4; cx++)
            for (int cz = (m.z1 - 2) >> 4; cz <= (m.z2 + 2) >> 4; cz++) out.add(((long) cx << 32) | (cz & 0xFFFFFFFFL));
        for (int i = 0; i < Math.min(ROOMS, rooms); i++) {
            Box b = roomBox(i);
            for (int cx = (b.x1 - 1) >> 4; cx <= (b.x2 + 1) >> 4; cx++)
                for (int cz = (b.z1 - 1) >> 4; cz <= (b.z2 + 1) >> 4; cz++) out.add(((long) cx << 32) | (cz & 0xFFFFFFFFL));
        }
        return out;
    }

    /** Rebuild the stock map and labels without placing blocks (world already generated). */
    void index() {
        stock.clear(); labels.clear(); counters.clear(); leds.clear();
        dry = true;
        try {
            plazaDetails();
            for (Dept d : depts) racks(d);
            counters();
            for (int i = 0; i < ROOMS; i++) catalogueLabels(i);
        } finally { dry = false; }
    }

    private boolean dry;

    private void catalogueLabels(int i) {
        int cx = roomX(i), zw = roomBox(i).z1, col = cx - 23;
        int di = 0;
        for (Dept d : depts) {
            int n = 0;
            for (BlockData bd : d.stock) if (bd.getMaterial().isOccluding()) n++;
            int cols = (n + 8) / 9;
            labels.add(new Label(col + cols / 2.0, FY + 10.2 + (di++ % 2) * 1.4, zw + 1.05, "§0" + d.name, 0.7f));
            col += cols + 1;
        }
    }
}
