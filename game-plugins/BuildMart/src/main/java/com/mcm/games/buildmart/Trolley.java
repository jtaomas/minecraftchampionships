package com.mcm.games.buildmart;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * The old Build Mart shopping trolley ("Shopping Cart", MCC 3 to 13). See TROLLEY.md for the footage evidence.
 *
 * It is a REAL vanilla boat (mangrove, which no other game here uses; the pack retextures it into the grey wire basket)
 * driven by the player's own WASD on a floor of frosted ice, so speed, drift, steering, wall bumps and boat-to-boat
 * pushing are exactly vanilla ice-boat physics, as in MCC. The "Shopping Cart" item sits in hotbar slot 5: use it on the
 * mall floor and you are seated in your trolley at once; hop out (sneak) and the trolley vanishes and the item is back
 * in your hotbar. One trolley per player, nobody else can sit in yours, it cannot be broken, and it only works in the mall.
 */
final class Trolley implements Listener {
    static final int SLOT = 4;                 // hotbar slot 5 (cart/frames/ref/12_cart_ride_firstperson.png)
    private final JavaPlugin plugin;
    private final BuildMartGame game;
    final NamespacedKey itemKey, boatKey;
    final Map<UUID, Boat> rides = new HashMap<>();
    boolean debug;

    Trolley(JavaPlugin plugin, BuildMartGame game) {
        this.plugin = plugin;
        this.game = game;
        itemKey = new NamespacedKey(plugin, "cart");
        boatKey = new NamespacedKey(plugin, "trolley");
    }

    ItemStack item() {
        ItemStack it = new ItemStack(Material.MANGROVE_BOAT);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Shopping Cart", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(Component.text("Use it on the mall floor to ride.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Faster than running, harder to steer.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Sneak to hop out.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(game.kitKey, PersistentDataType.BYTE, (byte) 1);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(m);
        return it;
    }

    boolean isCart(ItemStack it) {
        return it != null && it.getType() == Material.MANGROVE_BOAT && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    boolean isTrolley(Entity e) { return e instanceof Boat && e.getPersistentDataContainer().has(boatKey, PersistentDataType.STRING); }

    private UUID ownerOf(Entity e) {
        String s = e.getPersistentDataContainer().get(boatKey, PersistentDataType.STRING);
        try { return s == null ? null : UUID.fromString(s); } catch (IllegalArgumentException ex) { return null; }
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isCart(e.getItem())) return;
        Player p = e.getPlayer();
        if (debug) plugin.getLogger().info("[trolley] use " + e.getAction() + " allowed=" + game.trolleyAllowed(p) + " cancelled=" + e.useItemInHand());
        if (!game.trolleyAllowed(p)) {
            e.setCancelled(true);
            game.actionBar(p, "§cYour Shopping Cart only works in Build Mart!");
            return;
        }
        Boat old = rides.get(p.getUniqueId());
        if (old != null && old.isValid()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof Boat boat)) return;
        Player p = e.getPlayer();
        if (debug) plugin.getLogger().info("[trolley] place by " + (p == null ? "?" : p.getName()));
        if (p == null || !boat.getWorld().getName().equals(BuildMartGame.WORLD)) return;
        ItemStack hand = p.getInventory().getItem(e.getHand());
        if (!isCart(hand) || !game.trolleyAllowed(p)) { e.setCancelled(true); return; }
        boat.getPersistentDataContainer().set(boatKey, PersistentDataType.STRING, p.getUniqueId().toString());
        boat.setPersistent(false);
        boat.addScoreboardTag(BuildMartGame.ENT_TAG);
        rides.put(p.getUniqueId(), boat);
        // seat the shopper straight away
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (boat.isValid() && p.isOnline() && p.getVehicle() == null) boat.addPassenger(p);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnter(VehicleEnterEvent e) {
        if (!isTrolley(e.getVehicle())) return;
        if (!e.getEntered().getUniqueId().equals(ownerOf(e.getVehicle()))) e.setCancelled(true);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p) || !isTrolley(e.getDismounted())) return;
        Entity boat = e.getDismounted();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.getVehicle() == boat) return;
            boat.remove();
            if (rides.get(p.getUniqueId()) == boat) rides.remove(p.getUniqueId());
            giveBack(p);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(VehicleDamageEvent e) { if (isTrolley(e.getVehicle())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDestroy(VehicleDestroyEvent e) { if (isTrolley(e.getVehicle())) e.setCancelled(true); }

    // ------------------------------------------------------------------ helpers

    /** Remove the player's trolley (if any) and make sure the Shopping Cart item is back in their hotbar. */
    void reclaim(Player p, boolean giveItem) {
        Boat b = rides.remove(p.getUniqueId());
        if (p.getVehicle() != null && isTrolley(p.getVehicle())) { Entity v = p.getVehicle(); p.leaveVehicle(); v.remove(); }
        if (b != null && b.isValid()) { b.eject(); b.remove(); }
        if (giveItem) giveBack(p);
    }

    void giveBack(Player p) {
        if (!game.isParticipant(p)) return;
        for (ItemStack it : p.getInventory().getContents()) if (isCart(it)) return;
        if (p.getInventory().getItem(SLOT) == null) p.getInventory().setItem(SLOT, item());
        else p.getInventory().addItem(item());
    }

    void removeAll() {
        for (Boat b : rides.values()) if (b != null && b.isValid()) { b.eject(); b.remove(); }
        rides.clear();
    }

    int count() {
        int n = 0;
        for (Boat b : rides.values()) if (b != null && b.isValid()) n++;
        return n;
    }
}
