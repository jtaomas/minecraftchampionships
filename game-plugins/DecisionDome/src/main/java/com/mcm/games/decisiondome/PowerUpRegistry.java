package com.mcm.games.decisiondome;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static catalogue of the Decision Dome power-ups shipped by this build. */
final class PowerUpRegistry {
    record Def(String id, String name, PowerUp.Category category, Material icon, String lore,
               boolean egg, Egg.Kind eggKind, boolean phaseTwo) { }

    private static final NamespacedKey KEY = new NamespacedKey("mcmdecisiondome", "dome_powerup");
    private static final NamespacedKey SESSION = new NamespacedKey("mcmdecisiondome", "dome_session");
    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    private static void add(Def d) { DEFS.put(d.id(), d); }

    static {
        add(new Def("glacia", "Spell of Glacia", PowerUp.Category.SPELL, Material.PACKED_ICE,
                "Freeze one chicken for the rest of the vote.", false, null, false));
        add(new Def("mitosis", "Spell of Mitosis", PowerUp.Category.SPELL, Material.SLIME_BALL,
                "Duplicate one chicken. The copy votes too.", false, null, false));
        add(new Def("waxing", "Spell of Waxing", PowerUp.Category.SPELL, Material.GLOWSTONE_DUST,
                "Make one chicken larger.", false, null, false));
        add(new Def("waning", "Spell of Waning", PowerUp.Category.SPELL, Material.GUNPOWDER,
                "Make one chicken smaller.", false, null, false));
        add(new Def("swapping", "Spell of Swapping", PowerUp.Category.SPELL, Material.ENDER_PEARL,
                "Swap two chickens on the dial.", false, null, false));
        add(new Def("sticking", "Spell of Sticking", PowerUp.Category.SPELL, Material.SLIME_BLOCK,
                "Make one wedge sticky for ten seconds.", false, null, false));
        add(new Def("coin_of_fate", "Coin of Fate", PowerUp.Category.RELIC, Material.GOLD_INGOT,
                "50/50: double your team's eggs or dunk your own pod.", false, null, false));
        add(new Def("spellcheck", "Spellcheck", PowerUp.Category.RELIC, Material.BOOK,
                "Block all spells and curses for ten seconds.", false, null, false));
        add(new Def("swinging_watch", "Swinging Watch", PowerUp.Category.RELIC, Material.CLOCK,
                "Hypnotise every chicken in the dome for ten seconds.", false, null, false));
        add(new Def("frog_egg", "Frog Egg", PowerUp.Category.EGG, Material.EGG,
                "Spawns a frog: -1 vote.", true, Egg.Kind.FROG, false));
        add(new Def("mega_chicken_egg", "Mega Chicken Egg", PowerUp.Category.EGG, Material.EGG,
                "Spawns a mega chicken: +3 votes.", true, Egg.Kind.MEGA_CHICKEN, false));
        add(new Def("mega_frog_egg", "Mega Frog Egg", PowerUp.Category.EGG, Material.EGG,
                "Spawns a mega frog: -3 votes.", true, Egg.Kind.MEGA_FROG, false));

        add(new Def("levitas", "Spell of Levitas", PowerUp.Category.SPELL, Material.FEATHER,
                "Lift every chicken in one wedge into the air.", false, null, true));
        add(new Def("infernus", "Spell of Infernus", PowerUp.Category.SPELL, Material.BLAZE_POWDER,
                "Set one wedge on fire; its chickens burn.", false, null, true));
        add(new Def("petrify", "Spell of Petrification", PowerUp.Category.SPELL, Material.STONE,
                "Turn one chicken to stone.", false, null, true));
        add(new Def("transmutation", "Spell of Transmutation", PowerUp.Category.SPELL, Material.FERMENTED_SPIDER_EYE,
                "Swap a chicken's type: chicken <-> frog, mega inverts too.", false, null, true));
        add(new Def("cage", "Curse of Entrapment", PowerUp.Category.CURSE, Material.IRON_BARS,
                "Cage another team's pod.", false, null, true));
        add(new Def("freezing", "Curse of Freezing", PowerUp.Category.CURSE, Material.BLUE_ICE,
                "Freeze another team's pod for ten seconds.", false, null, true));
        add(new Def("dunking", "Curse of Dunking", PowerUp.Category.CURSE, Material.TNT,
                "Dunk another team's pod and strip its remaining power-ups.", false, null, true));
    }

    private PowerUpRegistry() {}

    static List<Def> all(boolean phaseTwo) {
        List<Def> out = new ArrayList<>();
        for (Def d : DEFS.values()) if (!d.phaseTwo() || phaseTwo) out.add(d);
        return out;
    }

    static Def byId(String id) { return id == null ? null : DEFS.get(id.toLowerCase(java.util.Locale.ROOT)); }

    static ItemStack item(Plugin plugin, Def def, String sessionId) {
        ItemStack item = new ItemStack(def.icon(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§l" + def.name());
            meta.setLore(List.of("§7" + def.lore(), "§8Right-click to use.", "§8Team power-up, this vote only."));
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, def.id());
            meta.getPersistentDataContainer().set(SESSION, PersistentDataType.STRING, sessionId);
            item.setItemMeta(meta);
        }
        return item;
    }

    static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    static String sessionOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(SESSION, PersistentDataType.STRING);
    }

    static boolean isPowerUp(ItemStack item, String sessionId) {
        String id = idOf(item);
        return id != null && sessionId != null && sessionId.equals(sessionOf(item));
    }
}
