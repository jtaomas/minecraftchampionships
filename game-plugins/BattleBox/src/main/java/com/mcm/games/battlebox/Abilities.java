package com.mcm.games.battlebox;

import com.mcm.games.battlebox.Gear.Custom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Behaviour of the custom items: Charges (self / on-hit), Bursts (thrown area), Fuse Bursts (delayed area), Popper Crates. */
final class Abilities {
    private final Plugin plugin;
    private final BattleBoxGame game;
    final NamespacedKey throwKey;
    final NamespacedKey ownerKey;
    private final List<BukkitTask> tasks = new ArrayList<>();

    static final double BURST_RADIUS = 3.5;

    Abilities(Plugin plugin, BattleBoxGame game) {
        this.plugin = plugin;
        this.game = game;
        this.throwKey = new NamespacedKey(plugin, "bb_throw");
        this.ownerKey = new NamespacedKey(plugin, "bb_owner");
    }

    void cancelAll() {
        for (BukkitTask t : tasks) { try { t.cancel(); } catch (Throwable ignored) { } }
        tasks.clear();
    }

    private void consume(Player p, EquipmentSlot hand) {
        ItemStack it = hand == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        if (it == null) return;
        it.setAmount(it.getAmount() - 1);
        if (hand == EquipmentSlot.OFF_HAND) p.getInventory().setItemInOffHand(it.getAmount() <= 0 ? null : it);
        else p.getInventory().setItemInMainHand(it.getAmount() <= 0 ? null : it);
    }

    /** Right-click use by an alive fighter. Returns true if handled. */
    boolean use(Player p, Custom c, EquipmentSlot hand) {
        if (p.hasCooldown(c.mat)) return true;
        switch (c.kind) {
            case CHARGE -> {
                applyCharge(c, p, p, true);
                consume(p, hand);
                p.setCooldown(c.mat, 10);
            }
            case BURST, FUSE -> {
                Snowball s = p.launchProjectile(Snowball.class, p.getEyeLocation().getDirection().multiply(1.35));
                s.setItem(new ItemStack(c.mat));
                s.getPersistentDataContainer().set(throwKey, PersistentDataType.STRING, c.name());
                s.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 0.8f);
                consume(p, hand);
                p.setCooldown(c.mat, 15);
            }
            case THROWN_TNT -> {
                Location at = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.8));
                TNTPrimed t = p.getWorld().spawn(at, TNTPrimed.class, tnt -> {
                    tnt.setFuseTicks(30);
                    tnt.setSource(p);
                    tnt.setYield(2.6f);
                    tnt.setVelocity(p.getEyeLocation().getDirection().multiply(0.9).add(new Vector(0, 0.2, 0)));
                });
                t.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1.2f);
                consume(p, hand);
                p.setCooldown(c.mat, 20);
            }
            default -> { return false; }
        }
        return true;
    }

    /** Melee hit on an enemy while holding a Charge: apply it to them. */
    void chargeHit(Player attacker, Player victim, Custom c) {
        applyCharge(c, attacker, victim, false);
        consume(attacker, EquipmentSlot.HAND);
    }

    private void applyCharge(Custom c, Player user, Player target, boolean self) {
        switch (c) {
            case QUICKSTEP -> target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
            case MENDING -> target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            case UPDRAFT -> target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, self ? 30 : 40, 1));
            case VANISH -> target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0));
            case SPRING -> target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 160, 2));
            case SLIP -> {
                Vector d = self ? user.getLocation().getDirection() : target.getLocation().toVector().subtract(user.getLocation().toVector());
                d.setY(0);
                if (d.lengthSquared() < 1e-4) d = new Vector(0, 0, 1);
                target.setVelocity(d.normalize().multiply(1.3).setY(0.35));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2));
            }
            default -> { }
        }
        Location l = target.getLocation().add(0, 1, 0);
        l.getWorld().spawnParticle(Particle.DUST, l, 24, 0.4, 0.6, 0.4, new Particle.DustOptions(c.fx, 1.4f));
        l.getWorld().playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, self ? 1.4f : 0.8f);
    }

    /** A thrown Burst / Fuse Burst landed. */
    void landed(Projectile proj) {
        String s = proj.getPersistentDataContainer().get(throwKey, PersistentDataType.STRING);
        String o = proj.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (s == null || o == null) return;
        Custom c;
        UUID owner;
        try { c = Custom.valueOf(s); owner = UUID.fromString(o); } catch (IllegalArgumentException e) { return; }
        Player thrower = Bukkit.getPlayer(owner);
        Location at = proj.getLocation();
        if (thrower == null || !game.isAliveFighter(thrower)) return;
        if (c.kind == Gear.Kind.BURST) burst(c, thrower, at);
        else fuse(c, thrower, at);
    }

    private void ring(World w, Location at, double r, Particle.DustOptions dust, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            w.spawnParticle(Particle.DUST, at.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r), 1, 0, 0, 0, dust);
        }
    }

    private void burst(Custom c, Player thrower, Location at) {
        World w = at.getWorld();
        Particle.DustOptions dust = new Particle.DustOptions(c.fx, 1.6f);
        ring(w, at, BURST_RADIUS, dust, 36);
        w.spawnParticle(Particle.DUST, at.clone().add(0, 0.6, 0), 40, 1.2, 0.6, 1.2, dust);
        w.playSound(at, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 1.2f);
        for (Player t : nearbyFighters(at, BURST_RADIUS)) {
            boolean team = game.sameSide(thrower, t);
            switch (c) {
                case BLOOM -> { if (team) t.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); }
                case LIFT -> { if (!team) { t.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1)); game.markAttack(t, thrower); } }
                case FLARE -> { if (!team) { t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0)); game.markAttack(t, thrower); } }
                default -> { }
            }
        }
    }

    private void fuse(Custom c, Player thrower, Location at) {
        World w = at.getWorld();
        Particle.DustOptions dust = new Particle.DustOptions(c.fx, 1.5f);
        UUID owner = thrower.getUniqueId();
        final BukkitTask[] self = new BukkitTask[1];
        self[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int t = 0;
            @Override public void run() {
                if (!game.isActive()) { self[0].cancel(); return; }
                if (t < 30) {
                    ring(w, at, BURST_RADIUS * (1 - t / 40.0), dust, 24);
                    if (t % 5 == 0) w.playSound(at, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.0f + t / 30f);
                    t++;
                    return;
                }
                self[0].cancel();
                tasks.remove(self[0]);
                w.spawnParticle(Particle.EXPLOSION, at.clone().add(0, 0.5, 0), 2);
                w.playSound(at, c == Custom.STING ? Sound.ENTITY_GENERIC_EXPLODE : Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.6f);
                Player th = Bukkit.getPlayer(owner);
                for (Player v : nearbyFighters(at, BURST_RADIUS)) {
                    if (th != null && game.sameSide(th, v)) continue;
                    if (c == Custom.STING) {
                        if (th != null) v.damage(6.0, th); else v.damage(6.0);
                    } else {
                        v.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                        v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                        if (th != null) game.markAttack(v, th);
                    }
                }
            }
        }, 1L, 1L);
        tasks.add(self[0]);
    }

    private List<Player> nearbyFighters(Location at, double r) {
        List<Player> out = new ArrayList<>();
        for (Entity e : at.getWorld().getNearbyEntities(at, r, r, r)) {
            if (e instanceof Player p && game.isAliveFighter(p) && p.getLocation().distanceSquared(at) <= (r + 0.5) * (r + 0.5)) out.add(p);
        }
        return out;
    }
}
