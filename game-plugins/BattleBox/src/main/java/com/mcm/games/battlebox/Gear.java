package com.mcm.games.battlebox;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/** Item factory for Battle Box: weapons, team gear and the custom Charges / Bursts / Fuse Bursts. */
final class Gear {
    private Gear() {}

    static NamespacedKey ITEM_KEY;

    static void init(Plugin plugin) { ITEM_KEY = new NamespacedKey(plugin, "bb_item"); }

    enum Kind { CHARGE, BURST, FUSE, THROWN_TNT, MENU }

    /**
     * Custom items. custom_model_data hooks: {@link #model} is 0 for now.
     * TODO(resource pack): map these to constants in McmGlyphs once the pack ships Battle Box item models.
     */
    enum Custom {
        QUICKSTEP(Kind.CHARGE, Material.SUGAR, ChatColor.AQUA, "Quickstep Charge", Color.fromRGB(0x7FE8FF),
                "Speed II for 8 s.", 0),
        MENDING(Kind.CHARGE, Material.GLISTERING_MELON_SLICE, ChatColor.LIGHT_PURPLE, "Mending Charge", Color.fromRGB(0xFF6FD0),
                "Regeneration II for 5 s.", 0),
        UPDRAFT(Kind.CHARGE, Material.FEATHER, ChatColor.WHITE, "Updraft Charge", Color.fromRGB(0xF0F0FF),
                "Levitation for 1.5 s (2 s on an enemy).", 0),
        VANISH(Kind.CHARGE, Material.ECHO_SHARD, ChatColor.DARK_AQUA, "Vanish Charge", Color.fromRGB(0x2E6E7E),
                "Invisibility for 6 s.", 0),
        SLIP(Kind.CHARGE, Material.RABBIT_FOOT, ChatColor.GOLD, "Slip Charge", Color.fromRGB(0xFFB347),
                "Dash forward + Speed III for 3 s.", 0),
        SPRING(Kind.CHARGE, Material.SLIME_BALL, ChatColor.GREEN, "Spring Charge", Color.fromRGB(0x8BEA5A),
                "Jump Boost III for 8 s.", 0),
        BLOOM(Kind.BURST, Material.HEART_OF_THE_SEA, ChatColor.LIGHT_PURPLE, "Bloom Burst", Color.fromRGB(0xFF7AD9),
                "Throw: Regeneration II to teammates nearby.", 0),
        LIFT(Kind.BURST, Material.PHANTOM_MEMBRANE, ChatColor.WHITE, "Lift Burst", Color.fromRGB(0xE0E8FF),
                "Throw: enemies nearby float up.", 0),
        FLARE(Kind.BURST, Material.GLOW_INK_SAC, ChatColor.YELLOW, "Flare Burst", Color.fromRGB(0xFFF27A),
                "Throw: enemies nearby glow for 10 s.", 0),
        STING(Kind.FUSE, Material.FIRE_CHARGE, ChatColor.RED, "Sting Fuse Burst", Color.fromRGB(0xD12A2A),
                "Throw: after 1.5 s, hurts enemies nearby.", 0),
        SMOKE(Kind.FUSE, Material.INK_SAC, ChatColor.DARK_GRAY, "Smoke Fuse Burst", Color.fromRGB(0x303040),
                "Throw: after 1.5 s, blinds enemies nearby.", 0),
        POPPER(Kind.THROWN_TNT, Material.TNT, ChatColor.RED, "Popper Crate", Color.fromRGB(0xE04020),
                "Throw: explodes after 1.5 s. Breaks no blocks.", 0),
        MENU(Kind.MENU, Material.NETHER_STAR, ChatColor.GOLD, "Kit Menu", Color.WHITE,
                "Right-click to choose your kit.", 0);

        final Kind kind;
        final Material mat;
        final ChatColor color;
        final String name;
        final Color fx;
        final String desc;
        final int model;

        Custom(Kind kind, Material mat, ChatColor color, String name, Color fx, String desc, int model) {
            this.kind = kind; this.mat = mat; this.color = color; this.name = name; this.fx = fx; this.desc = desc; this.model = model;
        }

        String colored() { return color + name; }
    }

    static ItemStack custom(Custom c, int amount) {
        ItemStack it = new ItemStack(c.mat, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(c.color + "" + ChatColor.BOLD + c.name);
        List<String> lore = new ArrayList<>();
        String kind = switch (c.kind) {
            case CHARGE -> "Charge - right-click: you, hit: enemy";
            case BURST -> "Burst - thrown, area effect";
            case FUSE -> "Fuse Burst - thrown, delayed";
            case THROWN_TNT -> "Thrown explosive";
            case MENU -> "";
        };
        if (!kind.isEmpty()) lore.add(ChatColor.DARK_GRAY + kind);
        lore.add(ChatColor.GRAY + c.desc);
        m.setLore(lore);
        if (c.model > 0) m.setCustomModelData(c.model);
        m.setEnchantmentGlintOverride(c.kind != Kind.THROWN_TNT);
        m.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, c.name());
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return it;
    }

    static Custom customOf(ItemStack it) {
        if (it == null || ITEM_KEY == null || !it.hasItemMeta()) return null;
        String s = it.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
        if (s == null) return null;
        try { return Custom.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    // ------------------------------------------------------------------ weapons

    static ItemStack weapon(Material mat, String name, Enchantment ench, int level) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (name != null) m.setDisplayName(ChatColor.WHITE + name);
        if (ench != null) m.addEnchant(ench, level, true);
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        it.setItemMeta(m);
        return it;
    }

    static ItemStack plain(Material mat, int amount) { return new ItemStack(mat, amount); }

    static ItemStack splash(PotionType type, String name) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION, 1);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setBasePotionType(type);
        m.setDisplayName(name);
        it.setItemMeta(m);
        return it;
    }

    static ItemStack rockets(int n) {
        ItemStack it = new ItemStack(Material.FIREWORK_ROCKET, n);
        FireworkMeta m = (FireworkMeta) it.getItemMeta();
        m.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL).withColor(Color.ORANGE, Color.RED).build());
        m.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.YELLOW).build());
        m.setPower(1);
        m.setDisplayName(ChatColor.GOLD + "Party Rocket");
        it.setItemMeta(m);
        return it;
    }

    // ------------------------------------------------------------------ team gear

    /** Plain iron armour piece (unbreakable), for the non-Runner kits. */
    static ItemStack iron(Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setUnbreakable(true); m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); it.setItemMeta(m); }
        return it;
    }

    static ItemStack leather(Material mat, DyeColor dye) {
        ItemStack it = new ItemStack(mat);
        LeatherArmorMeta m = (LeatherArmorMeta) it.getItemMeta();
        m.setColor(dye.getColor());
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
        it.setItemMeta(m);
        return it;
    }

    static ItemStack centreBlocks(DyeColor dye, boolean wool, int n) {
        ItemStack it = new ItemStack(Side.centreBlock(dye, wool), n);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Side.chatFor(dye) + "" + ChatColor.BOLD + "Centre Blocks");
        m.setLore(List.of(ChatColor.GRAY + "Place on the centre pad.", ChatColor.GRAY + "Fill every cell to win the round!"));
        it.setItemMeta(m);
        return it;
    }

    /** Efficiency level on the centre tool (config centre-tool-efficiency; default 0 = plain vanilla tool). */
    static int centreToolEfficiency = 0;

    static ItemStack centreTool(boolean wool) {
        ItemStack it = new ItemStack(wool ? Material.SHEARS : Material.IRON_PICKAXE);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + (wool ? "Centre Shears" : "Centre Pick"));
        m.setLore(List.of(ChatColor.GRAY + "Breaks centre blocks only."));
        if (centreToolEfficiency > 0) m.addEnchant(Enchantment.EFFICIENCY, centreToolEfficiency, true);
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    static boolean isCentreTool(ItemStack it) {
        return it != null && (it.getType() == Material.IRON_PICKAXE || it.getType() == Material.SHEARS);
    }

    /** Readable one-line description of an item stack for menus. */
    static String describe(ItemStack it) {
        String name;
        Custom c = customOf(it);
        if (c != null) name = c.colored();
        else if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) name = it.getItemMeta().getDisplayName();
        else {
            String n = it.getType().name().toLowerCase().replace('_', ' ');
            name = ChatColor.WHITE + "" + Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
        StringBuilder sb = new StringBuilder(name);
        if (it.hasItemMeta()) {
            it.getItemMeta().getEnchants().forEach((e, l) -> sb.append(ChatColor.GRAY).append(" (")
                    .append(niceEnchant(e)).append(" ").append(roman(l)).append(")"));
        }
        if (it.getAmount() > 1) sb.append(ChatColor.GRAY).append(" x").append(it.getAmount());
        return sb.toString();
    }

    private static String niceEnchant(Enchantment e) {
        String k = e.getKey().getKey().replace('_', ' ');
        return Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }

    private static String roman(int l) {
        return switch (l) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(l); };
    }
}
