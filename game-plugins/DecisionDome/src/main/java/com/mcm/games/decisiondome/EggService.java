package com.mcm.games.decisiondome;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Egg items, projectile launching and the projectile-to-logical-egg map for one session. */
final class EggService {
    private final Plugin plugin;
    private final String sessionId;
    private final NamespacedKey keySession;
    private final NamespacedKey keyKind;
    private final NamespacedKey keyOwner;
    private final Map<UUID, Egg> byProjectile = new HashMap<>();

    EggService(Plugin plugin, String sessionId) {
        this.plugin = plugin;
        this.sessionId = sessionId;
        this.keySession = new NamespacedKey(plugin, "dome_session");
        this.keyKind = new NamespacedKey(plugin, "dome_egg_kind");
        this.keyOwner = new NamespacedKey(plugin, "dome_owner");
    }

    ItemStack makeEgg(Egg.Kind kind, UUID owner) {
        Material m = Material.EGG;
        ItemStack item = new ItemStack(m, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = switch (kind) {
                case CHICKEN -> "§e§lDecision Egg";
                case FROG -> "§a§lFrog Egg §7(-1 vote)";
                case MEGA_CHICKEN -> "§6§lMega Chicken Egg §7(+3 votes)";
                case MEGA_FROG -> "§2§lMega Frog Egg §7(-3 votes)";
            };
            meta.setDisplayName(name);
            meta.setLore(List.of(
                    "§7Right-click to throw at a game.",
                    "§7The chicken's wedge at tally is the vote.",
                    "§8One egg, one chicken."));
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keySession, PersistentDataType.STRING, sessionId);
            pdc.set(keyKind, PersistentDataType.STRING, kind.name());
            if (owner != null) pdc.set(keyOwner, PersistentDataType.STRING, owner.toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    boolean isEgg(ItemStack item) {
        if (item == null || item.getType() != Material.EGG || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String s = meta.getPersistentDataContainer().get(keySession, PersistentDataType.STRING);
        return sessionId.equals(s);
    }

    Egg.Kind kindOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Egg.Kind.CHICKEN;
        String s = item.getItemMeta().getPersistentDataContainer().get(keyKind, PersistentDataType.STRING);
        if (s == null) return Egg.Kind.CHICKEN;
        try { return Egg.Kind.valueOf(s); } catch (IllegalArgumentException e) { return Egg.Kind.CHICKEN; }
    }

    UUID ownerOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** Launch a tagged egg projectile for an already-created logical egg. Returns null on failure. */
    org.bukkit.entity.Egg launch(Player player, Egg logical) {
        try {
            Vector direction = player.getEyeLocation().getDirection().normalize();
            Vector velocity = direction.multiply(1.55).add(player.getVelocity().multiply(0.45));
            if (velocity.length() > 2.1) velocity = velocity.normalize().multiply(2.1);
            org.bukkit.entity.Egg projectile = player.launchProjectile(org.bukkit.entity.Egg.class, velocity);
            projectile.setPersistent(false);
            projectile.setGravity(true);
            projectile.setItem(makeEgg(logical.kind(), logical.owner()));
            projectile.addScoreboardTag(DomeArena.TAG);
            projectile.addScoreboardTag(DomeArena.TAG + ":" + sessionId + ":egg");
            byProjectile.put(projectile.getUniqueId(), logical);
            player.playSound(player.getLocation(), Sound.ENTITY_EGG_THROW, 0.8f, 0.8f);
            return projectile;
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not launch Decision Egg: " + t);
            return null;
        }
    }

    Egg logical(Projectile projectile) {
        return projectile == null ? null : byProjectile.get(projectile.getUniqueId());
    }

    boolean owns(Projectile projectile) {
        return projectile != null && byProjectile.containsKey(projectile.getUniqueId());
    }

    Egg remove(Projectile projectile) {
        return projectile == null ? null : byProjectile.remove(projectile.getUniqueId());
    }

    boolean isTagged(Projectile projectile) {
        return projectile != null && projectile.getScoreboardTags().contains(DomeArena.TAG + ":" + sessionId + ":egg");
    }

    void clear() {
        byProjectile.clear();
    }
}
