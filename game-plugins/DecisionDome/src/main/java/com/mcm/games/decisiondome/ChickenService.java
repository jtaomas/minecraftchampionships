package com.mcm.games.decisiondome;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real chicken/frog entities slaved to the logical votes in {@link VoteEngine}. AI, collision and
 * gravity are off; the engine owns the path and the tally reads the resulting wedge.
 */
final class ChickenService {
    private final Plugin plugin;
    private final String sessionId;
    private final NamespacedKey keySession;
    private final NamespacedKey keyOwner;
    private final NamespacedKey keyTeam;
    private final NamespacedKey keyKind;
    private final NamespacedKey keyWeight;
    private final Map<UUID, Entity> visuals = new HashMap<>();

    ChickenService(Plugin plugin, String sessionId) {
        this.plugin = plugin;
        this.sessionId = sessionId;
        this.keySession = new NamespacedKey(plugin, "dome_session");
        this.keyOwner = new NamespacedKey(plugin, "dome_owner");
        this.keyTeam = new NamespacedKey(plugin, "dome_team");
        this.keyKind = new NamespacedKey(plugin, "dome_kind");
        this.keyWeight = new NamespacedKey(plugin, "dome_weight");
    }

    Entity spawn(Chicken logical, DomeVenue venue) {
        if (logical == null || venue == null || venue.world() == null) return null;
        Location at = venue.at(logical.x(), logical.z(), logical.levitating(0) ? 1.6 : 0.08);
        if (at == null) return null;
        Entity entity;
        try {
            if (logical.frog()) {
                entity = venue.world().spawn(at, org.bukkit.entity.Frog.class);
            } else {
                entity = venue.world().spawn(at, org.bukkit.entity.Chicken.class);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not spawn vote mob: " + t);
            return null;
        }
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setGravity(false);
        if (entity instanceof org.bukkit.entity.LivingEntity living) living.setCollidable(false);
        entity.setFallDistance(0f);
        entity.addScoreboardTag(DomeArena.TAG);
        entity.addScoreboardTag(DomeArena.TAG + ":" + sessionId + ":chicken");
        entity.addScoreboardTag(DomeArena.TAG + ":" + sessionId + ":vote:" + logical.id());
        if (entity instanceof Mob mob) {
            mob.setAware(false);
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
        }
        if (entity instanceof org.bukkit.entity.Chicken chicken) {
            chicken.setAdult();
            chicken.setAgeLock(true);
            try { chicken.setEggLayTime(2_000_000); } catch (Throwable ignored) { }
        }
        if (entity instanceof org.bukkit.entity.LivingEntity living) {
            AttributeInstance scale = living.getAttribute(Attribute.GENERIC_SCALE);
            if (scale != null) {
                double s = Math.max(0.5, Math.min(2.5, logical.scalePercent() / 100.0));
                scale.setBaseValue(s);
            }
        }
        entity.getPersistentDataContainer().set(keySession, PersistentDataType.STRING, sessionId);
        if (logical.owner() != null) entity.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, logical.owner().toString());
        entity.getPersistentDataContainer().set(keyTeam, PersistentDataType.STRING, logical.team());
        entity.getPersistentDataContainer().set(keyKind, PersistentDataType.STRING, logical.kind().name());
        entity.getPersistentDataContainer().set(keyWeight, PersistentDataType.INTEGER, logical.value());

        World w = venue.world();
        w.spawnParticle(Particle.CRIT, at.clone().add(0, 0.35, 0), 8, 0.25, 0.25, 0.25, 0.03);
        w.playSound(at, Sound.ENTITY_CHICKEN_EGG, 0.6f, 1.2f);
        visuals.put(logical.id(), entity);
        return entity;
    }

    /** Mirror the engine's logical positions onto the real entities. */
    void sync(VoteEngine engine, DomeVenue venue) {
        if (engine == null || venue == null) return;
        for (Chicken logical : engine.chickens()) {
            Entity entity = visuals.get(logical.id());
            if (!logical.alive()) {
                if (entity != null) removeVisual(logical.id(), venue.world());
                continue;
            }
            if (entity == null || !entity.isValid()) {
                entity = spawn(logical, venue);
                if (entity == null) continue;
            }
            double extra = logical.levitating(engine.totalTick()) ? 1.6 : 0.08;
            Location target = venue.at(logical.x(), logical.z(), extra);
            if (target == null || target.getWorld() == null) continue;
            target.setYaw((float) ((logical.id().hashCode() % 360)));
            if (entity.getLocation().distanceSquared(target) > 0.0025) {
                entity.teleport(target);
                entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            }
            AttributeInstance scale = entity instanceof org.bukkit.entity.LivingEntity living
                    ? living.getAttribute(Attribute.GENERIC_SCALE) : null;
            if (scale != null) {
                double want = Math.max(0.5, Math.min(2.5, logical.scalePercent() / 100.0));
                if (Math.abs(scale.getBaseValue() - want) > 0.01) scale.setBaseValue(want);
            }
        }
        // Remove visuals whose logical chicken has disappeared (removed wedge, cleanup).
        for (Map.Entry<UUID, Entity> e : new HashMap<>(visuals).entrySet()) {
            Chicken logical = engine.chicken(e.getKey());
            if (logical == null || !logical.alive()) removeVisual(e.getKey(), venue.world());
        }
    }

    void removeVisual(UUID chickenId, World world) {
        Entity entity = visuals.remove(chickenId);
        if (entity == null) return;
        if (world != null) {
            world.spawnParticle(Particle.POOF, entity.getLocation().add(0, 0.3, 0), 6, 0.2, 0.2, 0.2, 0.02);
            world.playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.5f);
        }
        entity.remove();
    }

    void removeAll(World world) {
        for (Entity entity : visuals.values()) if (entity != null && entity.isValid()) entity.remove();
        visuals.clear();
        if (world != null) {
            String tag = DomeArena.TAG + ":" + sessionId + ":chicken";
            for (Entity e : world.getEntities()) {
                if (e.getScoreboardTags().contains(tag)) e.remove();
            }
        }
    }

    boolean isVoteEntity(Entity e) {
        if (e == null) return false;
        if (e.getScoreboardTags().contains(DomeArena.TAG + ":" + sessionId + ":chicken")) return true;
        String s = e.getPersistentDataContainer().get(keySession, PersistentDataType.STRING);
        return sessionId.equals(s);
    }

    Entity entity(UUID chickenId) { return visuals.get(chickenId); }

    int visualCount() { return visuals.size(); }

    /** Called on plugin enable: remove any stale vote mobs from a previous server run. */
    static void sweepStale(World world, Plugin plugin) {
        if (world == null) return;
        for (Entity e : world.getEntities()) {
            boolean stale = false;
            for (String tag : e.getScoreboardTags()) {
                if (tag.startsWith(DomeArena.TAG + ":") && (tag.endsWith(":chicken") || tag.endsWith(":egg") || tag.endsWith(":ui"))) {
                    stale = true;
                    break;
                }
            }
            if (stale) e.remove();
        }
    }
}
