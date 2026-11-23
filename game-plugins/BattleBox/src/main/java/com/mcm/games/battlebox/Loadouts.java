package com.mcm.games.battlebox;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

import static com.mcm.games.battlebox.Gear.Custom.*;

/**
 * Kit loadout tables per arena theme. Every kit additionally gets team leather armour, 16 centre blocks and the
 * centre tool (pickaxe, or shears in wool mode) - added by {@link BattleBoxGame}, not listed here.
 *
 * <pre>
 *            | Greenhouse                    | Toy Workshop                      | Ski Lodge
 * Medic      | stone sword, bow, 8 arrows,   | stone sword, bow, 6 arrows,       | stone sword, bow, 8 arrows,
 *            | 3 Bloom Bursts                | 2 Mending Charges, 1 Bloom Burst  | 2 Bloom Bursts, 1 Healing II splash
 * Marksman   | Power I bow, 16 arrows,       | Quick Charge I crossbow,          | Quick Charge I crossbow, 12 arrows,
 *            | 1 Updraft Charge              | 10 arrows, 1 Quickstep Charge     | 1 Slip Charge
 * Tinkerer   | Punch I bow, 6 arrows,        | Punch I bow, 4 arrows,            | Punch I bow, 5 arrows,
 *            | 1 Sting Fuse, 1 Spring Charge | 1 Smoke Fuse, 1 Lift Burst        | 1 Flare Burst, 1 Sting Fuse
 * Runner     | Knockback I stone axe,        | stone axe, 2 Quickstep Charges,   | Knockback I stone axe,
 *            | 3 Quickstep Charges           | 1 Popper Crate (TNT)              | 2 Quickstep Charges, 1 Vanish Charge
 * Brawler    | iron sword, 1 golden apple    | crossbow, 3 Party Rockets,        | bow, 4 arrows, 2 Harming splash,
 *            |                               | wooden sword                      | wooden sword
 * </pre>
 */
final class Loadouts {
    private Loadouts() {}

    static List<ItemStack> items(String theme, Kit kit) {
        List<ItemStack> l = new ArrayList<>();
        String t = theme == null ? "greenhouse" : theme;
        switch (t) {
            case "toyworkshop" -> toy(kit, l);
            case "skilodge" -> ski(kit, l);
            default -> greenhouse(kit, l);
        }
        return l;
    }

    private static ItemStack sword(Material m, String name) { return Gear.weapon(m, name, null, 0); }

    private static ItemStack bow(String name, Enchantment e, int lvl) { return Gear.weapon(Material.BOW, name, e, lvl); }

    private static ItemStack xbow(String name) { return Gear.weapon(Material.CROSSBOW, name, Enchantment.QUICK_CHARGE, 1); }

    private static ItemStack arrows(int n) { return Gear.plain(Material.ARROW, n); }

    private static void greenhouse(Kit k, List<ItemStack> l) {
        switch (k) {
            case MEDIC -> { l.add(sword(Material.STONE_SWORD, "Pruning Blade")); l.add(bow("Trellis Bow", null, 0)); l.add(arrows(8)); l.add(Gear.custom(BLOOM, 3)); }
            case MARKSMAN -> { l.add(bow("Greenhouse Longbow", Enchantment.POWER, 1)); l.add(arrows(16)); l.add(Gear.custom(UPDRAFT, 1)); }
            case TINKERER -> { l.add(bow("Sprinkler Bow", Enchantment.PUNCH, 1)); l.add(arrows(6)); l.add(Gear.custom(STING, 1)); l.add(Gear.custom(SPRING, 1)); }
            case RUNNER -> { l.add(Gear.weapon(Material.STONE_AXE, "Hedge Axe", Enchantment.KNOCKBACK, 1)); l.add(Gear.custom(QUICKSTEP, 3)); }
            case BRAWLER -> { l.add(sword(Material.IRON_SWORD, "Gardener's Cleaver")); l.add(Gear.plain(Material.GOLDEN_APPLE, 1)); }
        }
    }

    private static void toy(Kit k, List<ItemStack> l) {
        switch (k) {
            case MEDIC -> { l.add(sword(Material.STONE_SWORD, "Wooden Toy Sword")); l.add(bow("Slingshot Bow", null, 0)); l.add(arrows(6)); l.add(Gear.custom(MENDING, 2)); l.add(Gear.custom(BLOOM, 1)); }
            case MARKSMAN -> { l.add(xbow("Clockwork Crossbow")); l.add(arrows(10)); l.add(Gear.custom(QUICKSTEP, 1)); }
            case TINKERER -> { l.add(bow("Spring-Loaded Bow", Enchantment.PUNCH, 1)); l.add(arrows(4)); l.add(Gear.custom(SMOKE, 1)); l.add(Gear.custom(LIFT, 1)); }
            case RUNNER -> { l.add(Gear.weapon(Material.STONE_AXE, "Toy Hatchet", null, 0)); l.add(Gear.custom(QUICKSTEP, 2)); l.add(Gear.custom(POPPER, 1)); }
            case BRAWLER -> { l.add(xbow("Confetti Launcher")); l.add(Gear.rockets(3)); l.add(sword(Material.WOODEN_SWORD, "Ruler")); }
        }
    }

    private static void ski(Kit k, List<ItemStack> l) {
        switch (k) {
            case MEDIC -> { l.add(sword(Material.STONE_SWORD, "Ice Pick")); l.add(bow("Lodge Bow", null, 0)); l.add(arrows(8)); l.add(Gear.custom(BLOOM, 2));
                l.add(Gear.splash(PotionType.STRONG_HEALING, ChatColor.LIGHT_PURPLE + "Hot Cocoa Splash (team only)")); }
            case MARKSMAN -> { l.add(xbow("Biathlon Crossbow")); l.add(arrows(12)); l.add(Gear.custom(SLIP, 1)); }
            case TINKERER -> { l.add(bow("Snowcat Bow", Enchantment.PUNCH, 1)); l.add(arrows(5)); l.add(Gear.custom(FLARE, 1)); l.add(Gear.custom(STING, 1)); }
            case RUNNER -> { l.add(Gear.weapon(Material.STONE_AXE, "Woodcutter's Axe", Enchantment.KNOCKBACK, 1)); l.add(Gear.custom(QUICKSTEP, 2)); l.add(Gear.custom(VANISH, 1)); }
            case BRAWLER -> { l.add(bow("Avalanche Bow", null, 0)); l.add(arrows(4));
                l.add(Gear.splash(PotionType.HARMING, ChatColor.DARK_RED + "Icy Harming Splash (enemies only)"));
                l.add(Gear.splash(PotionType.HARMING, ChatColor.DARK_RED + "Icy Harming Splash (enemies only)"));
                l.add(sword(Material.WOODEN_SWORD, "Ski Pole")); }
        }
    }
}
