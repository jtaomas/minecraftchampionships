package com.mcm.games.decisiondome;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Snapshot and restore every voter around the dome, including inventory and effects. */
final class PlayerStateService {
    private record Snapshot(Location location, GameMode mode, ItemStack[] contents, double health,
                            int food, float saturation, float exp, int level, boolean allowFlight,
                            boolean flying, List<PotionEffect> effects) { }

    private final Map<UUID, Snapshot> snapshots = new HashMap<>();

    void capture(Player player) {
        if (player == null) return;
        ItemStack[] raw = player.getInventory().getContents();
        ItemStack[] contents = new ItemStack[raw.length];
        for (int i = 0; i < raw.length; i++) contents[i] = raw[i] == null ? null : raw[i].clone();
        snapshots.put(player.getUniqueId(), new Snapshot(
                player.getLocation().clone(), player.getGameMode(), contents,
                player.getHealth(), player.getFoodLevel(), player.getSaturation(),
                player.getExp(), player.getLevel(), player.getAllowFlight(), player.isFlying(),
                new ArrayList<>(player.getActivePotionEffects())));
    }

    void prepare(Player player, Location spawn) {
        if (player == null) return;
        if (player.isInsideVehicle()) player.leaveVehicle();
        if (spawn != null && spawn.getWorld() != null) player.teleport(spawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setFallDistance(0f);
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setAllowFlight(false);
        player.setFlying(false);
        AttributeInstance max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (max != null) player.setHealth(Math.max(1.0, max.getValue()));
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
    }

    void restore(UUID uuid) { restore(uuid, true); }

    void restore(UUID uuid, boolean restoreLocation) {
        Snapshot s = snapshots.remove(uuid);
        if (s == null) return;
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        try {
            player.getInventory().setContents(s.contents());
            player.setGameMode(s.mode());
            if (restoreLocation && s.location() != null && s.location().getWorld() != null) player.teleport(s.location());
            AttributeInstance max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double hp = max == null ? s.health() : Math.min(s.health(), max.getValue());
            player.setHealth(Math.max(1.0, hp));
            player.setFoodLevel(s.food());
            player.setSaturation(s.saturation());
            player.setExp(s.exp());
            player.setLevel(s.level());
            player.setAllowFlight(s.allowFlight());
            player.setFlying(s.flying() && s.allowFlight());
            for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
            for (PotionEffect effect : s.effects()) player.addPotionEffect(effect);
        } catch (Throwable ignored) {
            // A player can quit during cleanup; never fail the result callback because of a restore.
        }
    }

    void restoreAll() { restoreAll(true); }

    void restoreAll(boolean restoreLocation) {
        for (UUID uuid : new ArrayList<>(snapshots.keySet())) restore(uuid, restoreLocation);
    }

    void clear() { snapshots.clear(); }

    boolean has(UUID uuid) { return snapshots.containsKey(uuid); }

    Collection<UUID> captured() { return new ArrayList<>(snapshots.keySet()); }
}
