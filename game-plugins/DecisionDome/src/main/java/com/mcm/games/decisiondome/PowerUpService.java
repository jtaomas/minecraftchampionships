package com.mcm.games.decisiondome;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-team power-ups for one vote. MVP: Glacia, Mitosis, Waxing/Waning, Swapping, Sticking,
 * Coin of Fate, Spellcheck, Swinging Watch, Frog/Mega eggs. Phase 2 (config): Levitas, Infernus,
 * Petrification, Transmutation, Cage, Freezing, Dunking.
 */
final class PowerUpService {
    interface Hooks {
        boolean throwExtraEgg(Player player, Egg.Kind kind);
        void grantEggItem(Player player, Egg.Kind kind);
        void announce(String message);
        void tell(Player player, String message);
    }

    private final Plugin plugin;
    private final String sessionId;
    private final DomeSettings settings;
    private final EggService eggs;
    private final VoteEngine engine;
    private final DomeVenue venue;
    private final DomeRng rng;
    private final Hooks hooks;
    private final Map<String, List<PowerUp>> byTeam = new HashMap<>();
    private final Map<UUID, TeamPod> podOf;
    private long spellLockUntil;

    PowerUpService(Plugin plugin, String sessionId, DomeSettings settings, EggService eggs,
                   VoteEngine engine, DomeVenue venue, DomeRng rng, Hooks hooks, Map<UUID, TeamPod> podOf) {
        this.plugin = plugin;
        this.sessionId = sessionId;
        this.settings = settings;
        this.eggs = eggs;
        this.engine = engine;
        this.venue = venue;
        this.rng = rng;
        this.hooks = hooks;
        this.podOf = podOf;
    }

    /** Grant power-ups per team. Excluded on Game 1 and the audience vote by the caller/settings. */
    void grant(List<TeamPod> pods) {
        if (pods == null || pods.isEmpty()) return;
        List<PowerUpRegistry.Def> defs = new ArrayList<>(PowerUpRegistry.all(settings.phaseTwoPowerUps));
        int count = Math.min(settings.maxPowerUpsPerTeam, 1 + (engine.eventIndex() >= 4 ? 1 : 0));
        if (count <= 0 || defs.isEmpty()) return;
        for (TeamPod pod : pods) {
            if (pod.members().isEmpty()) continue;
            List<PowerUpRegistry.Def> pool = new ArrayList<>(defs);
            for (int i = 0; i < count && !pool.isEmpty(); i++) {
                PowerUpRegistry.Def def = pool.remove(rng.nextInt(pool.size()));
                if (def.egg()) {
                    pod.addExtraEggs(1);
                    Player p = firstOnline(pod);
                    if (p != null) hooks.grantEggItem(p, def.eggKind());
                    hooks.announce("§d[➤] §f" + teamName(pod) + " received a §d" + def.name()
                            + "§f for reaching Game " + engine.eventIndex() + "!");
                } else {
                    PowerUp instance = new PowerUp(def.id(), def.name(), def.category(), pod.teamKey(), null, 1);
                    byTeam.computeIfAbsent(pod.teamKey(), k -> new ArrayList<>()).add(instance);
                    for (UUID uuid : pod.members()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.getInventory().addItem(PowerUpRegistry.item(plugin, def, sessionId));
                        }
                    }
                    hooks.announce("§d[➤] §f" + teamName(pod) + " received the §d" + def.name()
                            + "§f for reaching Game " + engine.eventIndex() + "!");
                }
            }
        }
    }

    /** Right-click a power-up item. Returns true when the event was consumed. */
    boolean use(Player player, ItemStack item) {
        if (player == null || item == null) return false;
        if (!PowerUpRegistry.isPowerUp(item, sessionId)) return false;
        String id = PowerUpRegistry.idOf(item);
        PowerUpRegistry.Def def = PowerUpRegistry.byId(id);
        if (def == null) return false;
        TeamPod pod = podOf.get(player.getUniqueId());
        if (pod == null) {
            hooks.tell(player, "§cYou are not in a team pod.");
            return true;
        }
        if (pod.dunked()) {
            hooks.tell(player, "§cYour pod has been dunked; your power-ups are gone.");
            return true;
        }
        if (engine.phase() != Phase.VOTING && engine.phase() != Phase.SETTLE) {
            hooks.tell(player, "§7Power-ups are live only during the vote.");
            return true;
        }

        if (def.egg()) {
            if (hooks.throwExtraEgg(player, def.eggKind())) {
                removeFromTeam(pod, id);
            }
            return true;
        }

        if (def.category() != PowerUp.Category.RELIC && engine.totalTick() < spellLockUntil) {
            hooks.tell(player, "§dSpellcheck §7blocked that spell.");
            return true;
        }

        PowerUp instance = find(pod.teamKey(), id);
        if (instance == null || !instance.available()) {
            hooks.tell(player, "§7This power-up has already been used.");
            return true;
        }
        if (!instance.spend()) return true;

        boolean applied = apply(player, pod, def);
        if (applied) {
            removeFromTeam(pod, id);
            hooks.announce("§d[➤] §f" + player.getName() + " used §d" + def.name() + "§f for " + teamName(pod) + "!");
        }
        return true;
    }

    private boolean apply(Player player, TeamPod pod, PowerUpRegistry.Def def) {
        World w = venue.world();
        switch (def.id()) {
            case "glacia", "petrify" -> {
                Chicken target = engine.randomChicken(rng);
                if (target == null) return false;
                engine.freezeChicken(target.id(), 20L * 60);
                fx(w, target, def.id().equals("petrify") ? Particle.CRIT : Particle.SNOWFLAKE);
                return true;
            }
            case "mitosis" -> {
                Chicken target = engine.randomChicken(rng);
                if (target == null) return false;
                Chicken copy = engine.duplicateChicken(target.id());
                if (copy == null) return false;
                fx(w, target, Particle.HEART);
                return true;
            }
            case "waxing", "waning" -> {
                Chicken target = engine.randomChicken(rng);
                if (target == null) return false;
                target.scale(target.scalePercent() + (def.id().equals("waxing") ? 25 : -20));
                fx(w, target, Particle.ENCHANT);
                return true;
            }
            case "swapping" -> {
                Chicken a = engine.randomChicken(rng);
                Chicken b = engine.randomChicken(rng);
                if (a == null || b == null || a.id().equals(b.id())) return false;
                engine.swapChickens(a.id(), b.id());
                fx(w, a, Particle.PORTAL);
                fx(w, b, Particle.PORTAL);
                return true;
            }
            case "sticking" -> {
                int slot = randomLiveSlot();
                if (slot < 0) return false;
                engine.stickyWedge(slot, 200);
                Location l = venue.wedgeCenter(slot);
                if (l != null && l.getWorld() != null) {
                    l.getWorld().spawnParticle(Particle.ITEM_SLIME, l.clone().add(0, 1, 0), 60, 3, 0.4, 3, 0.02);
                    l.getWorld().playSound(l, Sound.BLOCK_SLIME_BLOCK_PLACE, 1f, 0.7f);
                }
                return true;
            }
            case "coin_of_fate" -> {
                boolean good = rng.nextBoolean();
                if (good) {
                    for (UUID uuid : pod.members()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) hooks.grantEggItem(p, Egg.Kind.CHICKEN);
                    }
                    hooks.announce("§6[Coin of Fate] §f" + teamName(pod) + "'s eggs were doubled!");
                } else {
                    dunk(pod);
                    hooks.announce("§6[Coin of Fate] §f" + teamName(pod) + " dunked themselves!");
                }
                return true;
            }
            case "spellcheck" -> {
                spellLockUntil = engine.totalTick() + 200;
                hooks.announce("§d[Spellcheck] §fSpells and curses are blocked for 10 seconds.");
                return true;
            }
            case "swinging_watch" -> {
                engine.freezeAll(200);
                for (Chicken c : engine.chickens()) fx(w, c, Particle.NOTE);
                return true;
            }
            case "levitas" -> {
                int slot = randomLiveSlot();
                if (slot < 0) return false;
                engine.levitateWedge(slot, 160);
                Location l = venue.wedgeCenter(slot);
                if (l != null && l.getWorld() != null) l.getWorld().spawnParticle(Particle.CLOUD, l.clone().add(0, 1, 0), 50, 3, 0.4, 3, 0.05);
                return true;
            }
            case "infernus" -> {
                int slot = randomLiveSlot();
                if (slot < 0) return false;
                engine.removeChickensInWedge(slot);
                Location l = venue.wedgeCenter(slot);
                if (l != null && l.getWorld() != null) {
                    l.getWorld().spawnParticle(Particle.FLAME, l.clone().add(0, 1, 0), 80, 3, 0.6, 3, 0.05);
                    l.getWorld().playSound(l, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.7f);
                }
                return true;
            }
            case "transmutation" -> {
                Chicken target = engine.randomChicken(rng);
                if (target == null) return false;
                engine.transmute(target.id());
                fx(w, target, Particle.WITCH);
                return true;
            }
            case "cage", "freezing", "dunking" -> {
                TeamPod target = randomEnemyPod(pod);
                if (target == null) return false;
                if (def.id().equals("cage")) {
                    target.cage(engine.totalTick() + 200);
                    venue.setPodGate(target.index(), false);
                    venue.setPodState(target.index(), "caged");
                } else if (def.id().equals("freezing")) {
                    target.freeze(engine.totalTick() + 200);
                    venue.setPodState(target.index(), "frozen");
                } else {
                    dunk(target);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void dunk(TeamPod pod) {
        pod.dunk();
        venue.setPodState(pod.index(), "dunked");
        venue.setPodGate(pod.index(), false);
        strip(pod);
        Location l = venue.podSpawn(pod.index());
        if (l != null && l.getWorld() != null) {
            World w = l.getWorld();
            w.spawnParticle(Particle.EXPLOSION, l.clone().add(0, 1, 0), 3, 1.2, 0.6, 1.2, 0);
            w.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 40, 1.6, 0.8, 1.6, 0.02);
            w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        }
    }

    /** A dunked team loses every remaining power-up. */
    void strip(TeamPod pod) {
        List<PowerUp> list = byTeam.get(pod.teamKey());
        if (list != null) {
            for (PowerUp p : list) p.cancel();
            list.clear();
        }
        for (UUID uuid : pod.members()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            for (ItemStack item : p.getInventory().getContents()) {
                if (PowerUpRegistry.isPowerUp(item, sessionId)) p.getInventory().removeItem(item);
            }
        }
    }

    void tick() {
        long now = engine.totalTick();
        for (TeamPod pod : new ArrayList<>(podOf.values())) {
            if (pod.caged(now) || "caged".equals(pod.state())) {
                if (!pod.caged(now)) {
                    pod.state("normal");
                    pod.gateOpen(true);
                    venue.setPodState(pod.index(), "normal");
                    venue.setPodGate(pod.index(), true);
                }
            }
            if (pod.frozen(now) || "frozen".equals(pod.state())) {
                if (!pod.frozen(now)) {
                    pod.state("normal");
                    venue.setPodState(pod.index(), "normal");
                }
            }
        }
        for (List<PowerUp> list : byTeam.values()) {
            for (PowerUp p : list) if (p.expired(now)) p.expire();
        }
    }

    private void removeFromTeam(TeamPod pod, String defId) {
        for (UUID uuid : pod.members()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            for (ItemStack item : p.getInventory().getContents()) {
                if (defId.equals(PowerUpRegistry.idOf(item)) && sessionId.equals(PowerUpRegistry.sessionOf(item))) {
                    p.getInventory().removeItem(item);
                }
            }
        }
    }

    private PowerUp find(String team, String defId) {
        List<PowerUp> list = byTeam.get(team);
        if (list == null) return null;
        for (PowerUp p : list) if (p.id().equals(defId) && p.available()) return p;
        return null;
    }

    private TeamPod randomEnemyPod(TeamPod own) {
        List<TeamPod> others = new ArrayList<>();
        for (TeamPod p : new java.util.LinkedHashSet<>(podOf.values())) {
            if (p != own && !p.members().isEmpty()) others.add(p);
        }
        return others.isEmpty() ? null : others.get(rng.nextInt(others.size()));
    }

    private int randomLiveSlot() {
        List<Wedge> live = engine.dial().liveWedges();
        if (live.isEmpty()) return -1;
        return live.get(rng.nextInt(live.size())).slot();
    }

    private Player firstOnline(TeamPod pod) {
        for (UUID uuid : pod.members()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) return p;
        }
        return null;
    }

    static String teamName(TeamPod pod) {
        return pod.teamKey() == null || pod.teamKey().isBlank() ? "Solos" : pod.teamKey();
    }

    private void fx(World w, Chicken c, Particle particle) {
        if (w == null || c == null) return;
        Location at = venue.at(c.x(), c.z(), 1.0);
        if (at == null) return;
        w.spawnParticle(particle, at, 12, 0.3, 0.4, 0.3, 0.02);
    }

    List<PowerUp> forTeam(String team) {
        return byTeam.getOrDefault(team, List.of());
    }

    int activeCount() {
        int n = 0;
        for (List<PowerUp> list : byTeam.values()) for (PowerUp p : list) if (p.state() == PowerUp.State.ACTIVE) n++;
        return n;
    }

    int heldCount() {
        int n = 0;
        for (List<PowerUp> list : byTeam.values()) for (PowerUp p : list) if (p.available()) n++;
        return n;
    }

    void clear() {
        for (List<PowerUp> list : byTeam.values()) for (PowerUp p : list) p.cancel();
        byTeam.clear();
    }
}
