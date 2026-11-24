package com.mcm.games.battlebox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Chest GUIs: the kit selection menu and the read-only kit preview. */
final class KitMenu implements InventoryHolder {
    /** Set by the game from config fight-seconds (false = no round time limit). */
    static boolean timeLimit = false;

    static final int[] SLOTS = {11, 12, 13, 14, 15};

    final boolean inspect;
    final String theme;
    final boolean wool;
    boolean preview;
    private Inventory inv;

    private KitMenu(boolean inspect, String theme, boolean wool) {
        this.inspect = inspect;
        this.theme = theme;
        this.wool = wool;
    }

    @Override public Inventory getInventory() { return inv; }

    Kit kitAt(int slot) {
        if (preview) return null;
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == slot) return Kit.values()[i];
        return null;
    }

    /**
     * Open the menu. takenBy: kit -> teammate names who hold it (null map for inspect mode); mine = current pick.
     */
    static void open(Player p, String theme, String themeName, boolean wool, DyeColor team, Map<Kit, String> takenBy,
                     Kit mine, boolean inspect) {
        KitMenu h = new KitMenu(inspect, theme, wool);
        String title = (inspect ? "Kits: " : "Pick a kit - ") + themeName;
        h.inv = Bukkit.createInventory(h, 27, title.length() > 32 ? title.substring(0, 32) : title);
        ItemStack pane = named(team == null ? Material.GRAY_STAINED_GLASS_PANE
                : Material.valueOf(team.name() + "_STAINED_GLASS_PANE"), " ", null);
        for (int i = 0; i < 27; i++) h.inv.setItem(i, pane);
        for (Kit k : Kit.values()) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + k.role);
            lore.add(ChatColor.GRAY + k.blurb);
            lore.add("");
            for (ItemStack it : Loadouts.items(theme, k)) lore.add(ChatColor.DARK_GRAY + "- " + Gear.describe(it));
            lore.add(ChatColor.DARK_GRAY + "+ team armour, 16 centre blocks, " + (wool ? "shears" : "pickaxe"));
            lore.add("");
            String taken = takenBy == null ? null : takenBy.get(k);
            if (inspect) lore.add(ChatColor.YELLOW + "Click to see the items");
            else if (k == mine) lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your kit");
            else if (taken != null) lore.add(ChatColor.RED + "Taken by " + taken);
            else lore.add(ChatColor.YELLOW + "Click to pick");
            ItemStack icon = named(k.icon, k.color + "" + ChatColor.BOLD + k.display, lore);
            if (k == mine) {
                ItemMeta m = icon.getItemMeta();
                m.setEnchantmentGlintOverride(true);
                icon.setItemMeta(m);
            }
            if (taken != null && k != mine) icon.setType(Material.BARRIER);
            h.inv.setItem(SLOTS[k.ordinal()], icon);
        }
        h.inv.setItem(22, named(Material.WHITE_CONCRETE, ChatColor.WHITE + "Centre: fill the 3x3!",
                List.of(ChatColor.GRAY + "Full 3x3 in your colour = instant win.",
                        ChatColor.GRAY + (timeLimit ? "At time-out, more centre blocks wins." : "No time limit: fill it to win."))));
        p.openInventory(h.inv);
    }

    /** Read-only view of one kit's actual items. */
    static void openPreview(Player p, String theme, String themeName, boolean wool, Kit k) {
        KitMenu h = new KitMenu(true, theme, wool);
        h.preview = true;
        h.inv = Bukkit.createInventory(h, 27, k.display + " - " + themeName);
        int slot = 0;
        for (ItemStack it : Loadouts.items(theme, k)) h.inv.setItem(slot++, it);
        h.inv.setItem(slot++, Gear.centreBlocks(DyeColor.RED, wool, 16));
        h.inv.setItem(slot, Gear.centreTool(wool));
        h.inv.setItem(18, Gear.leather(Material.LEATHER_HELMET, DyeColor.RED));
        h.inv.setItem(19, Gear.leather(Material.LEATHER_CHESTPLATE, DyeColor.RED));
        h.inv.setItem(20, Gear.leather(Material.LEATHER_LEGGINGS, DyeColor.RED));
        h.inv.setItem(21, Gear.leather(Material.LEATHER_BOOTS, DyeColor.RED));
        h.inv.setItem(26, named(Material.ARROW, ChatColor.YELLOW + "Back to kits", null));
        p.openInventory(h.inv);
    }

    private static ItemStack named(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }
}
