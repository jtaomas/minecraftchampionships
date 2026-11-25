package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmGame;
import com.mcm.api.McmLobby;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class DecisionDomePlugin extends JavaPlugin implements Listener {
    private DomeArena arena;
    private DomeSettings settings;
    private DomeController controller;
    private final Random rng = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = DomeSettings.load(this);
        arena = new DomeArena(this);
        try {
            arena.ensureWorld();
        } catch (Throwable t) {
            getLogger().severe("Failed to build the fallback dome world now (will retry on first vote): " + t);
        }
        controller = new DomeController(this, arena, settings);
        getServer().getPluginManager().registerEvents(this, this);
        McmApi.setVoteProvider(controller);
        sweepStale();
        getLogger().info("Decision Dome registered as the MCM vote provider; "
                + controller.venueDescription() + ".");
    }

    @Override
    public void onDisable() {
        if (McmApi.voteProvider() == controller) McmApi.setVoteProvider(null);
        if (controller != null) controller.stopAll();
        if (arena != null) arena.removeStrayEntities();
    }

    private void sweepStale() {
        try {
            if (arena != null && arena.world() != null) ChickenService.sweepStale(arena.world(), this);
            McmLobby.Venue v = McmLobby.venue();
            World hub = v == null ? null : v.world();
            if (hub != null) ChickenService.sweepStale(hub, this);
        } catch (Throwable ignored) { }
    }

    // ---------------------------------------------------------------------------------------------
    // Command
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mcm.dome.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "test" -> {
                if (McmApi.isChampionshipRunning() || !"lobby".equals(McmApi.currentGameId())) {
                    sender.sendMessage("§cA game or championship is running; stop it before testing the dome.");
                    return true;
                }
                boolean hub = args.length > 1 ? "hub".equalsIgnoreCase(args[1]) : controller.venueDescription().contains("v2");
                if (hub && McmLobby.venue() == null) {
                    sender.sendMessage("§cThe MCC hub venue isn't available. Use /dome test dome.");
                    return true;
                }
                LinkedHashSet<String> ids = new LinkedHashSet<>();
                if (hub) {
                    for (McmGame g : McmApi.games()) ids.add(g.id().toLowerCase(Locale.ROOT));
                }
                for (String id : McmApi.builtInGameIds()) ids.add(id.toLowerCase(Locale.ROOT));
                for (McmGame g : McmApi.games()) ids.add(g.id().toLowerCase(Locale.ROOT));
                if (ids.isEmpty()) {
                    sender.sendMessage("§cNo games installed to vote on.");
                    return true;
                }
                List<String> list = new ArrayList<>(ids);
                if (list.size() > 12) {
                    java.util.Collections.shuffle(list, rng);
                    list = new ArrayList<>(list.subList(0, 12));
                }
                if (!controller.startTest(list, hub)) {
                    sender.sendMessage("§cA real championship vote is running.");
                } else {
                    sender.sendMessage("§aStarted a test vote with " + list.size() + " games in "
                            + (hub ? "the MCC hub dome" : "the fallback dome") + ".");
                }
            }
            case "cancel" -> {
                if (!controller.isActive()) sender.sendMessage("§7No vote is running.");
                else { controller.adminCancel(); sender.sendMessage("§aVote cancelled."); }
            }
            case "end" -> {
                if (controller.phase() != Phase.VOTING) sender.sendMessage("§7Not in the voting phase.");
                else { controller.endVoting(); sender.sendMessage("§aEnding the voting phase (chickens still settle)."); }
            }
            case "skipwait" -> {
                if (controller.skipWait()) sender.sendMessage("§aSkipped to the current result.");
                else sender.sendMessage("§7Nothing to skip.");
            }
            case "simulate" -> {
                int n = 200;
                if (args.length > 1) {
                    try { n = Math.max(1, Math.min(5000, Integer.parseInt(args[1]))); }
                    catch (NumberFormatException ignored) { }
                }
                VoteSimulator.Summary s = VoteSimulator.run(n, 0xD0DE5EEDL);
                sender.sendMessage("§bDome simulate §f" + n + " votes: " + s.describe());
            }
            case "tp" -> {
                if (!(sender instanceof Player p)) return true;
                String where = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "auto";
                String part = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "dial";
                DomeSession session = controller.session();
                if (session != null && session.venue() != null && session.venue().available()) {
                    if ("pod".equals(part)) {
                        TeamPod pod = null;
                        for (TeamPod candidate : session.pods()) {
                            if (candidate.members().contains(p.getUniqueId())) { pod = candidate; break; }
                        }
                        Location l = session.venue().podSpawn(pod == null ? 0 : pod.index());
                        if (l != null) p.teleport(l);
                    } else {
                        Location l = session.venue().center();
                        if (l != null) p.teleport(l.clone().add(0, 4, 0));
                    }
                    sender.sendMessage("§aTeleported to the active dome.");
                    return true;
                }
                if (!"dome".equals(where) && McmLobby.venue() != null) {
                    p.teleport(McmLobby.venue().interiorSpawn());
                } else if (arena.ensureWorld() != null) {
                    p.teleport(arena.hubSpot(rng));
                }
            }
            default -> {
                sender.sendMessage("§bDecision Dome §7— " + controller.status());
                sender.sendMessage("§7Venue: §f" + controller.venueDescription()
                        + " §7| provider: " + (McmApi.voteProvider() == controller));
                sender.sendMessage("§7/dome test [hub|dome] §8· §7end §8· §7skipwait §8· §7cancel §8· §7tp §8· §7simulate <n> §8· §7status");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("test", "end", "skipwait", "cancel", "status", "tp", "simulate"))
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        if (args.length == 2 && ("test".equalsIgnoreCase(args[0]) || "tp".equalsIgnoreCase(args[0]))) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("hub", "dome")) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        if (args.length == 3 && "tp".equalsIgnoreCase(args[0])) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("pod", "dial")) if (s.startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        return List.of();
    }

    // ---------------------------------------------------------------------------------------------
    // Interaction and rules
    // ---------------------------------------------------------------------------------------------

    private DomeSession session() { return controller == null ? null : controller.session(); }

    private boolean inVenue(World world) {
        if (arena != null && arena.isDomeWorld(world)) return true;
        DomeSession s = session();
        return s != null && s.isVenueWorld(world);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (settings != null && settings.debug) {
            getLogger().info("[dome-debug] event " + e.getAction() + " hand=" + e.getHand()
                    + " item=" + (e.getItem() == null ? "null" : e.getItem().getType())
                    + " cancelled=" + e.isCancelled() + " session=" + (session() != null));
        }
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        DomeSession s = session();
        if (s == null) return;
        if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            if (s.tryThrowEgg(e.getPlayer(), item)) { e.setCancelled(true); return; }
            if (s.usePowerUp(e.getPlayer(), item)) { e.setCancelled(true); }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        DomeSession s = session();
        if (s != null && s.ownsProjectile(e.getEgg())) e.setHatching(false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Projectile projectile)) return;
        DomeSession s = session();
        if (s == null || !s.ownsProjectile(projectile)) return;
        e.setCancelled(true);
        s.handleEggHit(projectile, e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        DomeSession s = session();
        if (s != null && s.isVoteEntity(e.getRightClicked())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (inVenue(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (inVenue(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (inVenue(e.getBlock().getWorld())
                && !(e.getPlayer().getGameMode() == GameMode.CREATIVE && !controller.isActive())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (inVenue(e.getBlock().getWorld())
                && !(e.getPlayer().getGameMode() == GameMode.CREATIVE && !controller.isActive())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (inVenue(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (controller.isActive() && inVenue(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (inVenue(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (inVenue(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to == null || !inVenue(to.getWorld())) return;
        DomeSession s = session();
        if (s == null) return;
        // just under the dial (above the hub's own pit rescue at floor - 6) so a voter goes back to their cage
        if (to.getY() < s.venue().geometry().floorY() - 5) {
            TeamPod pod = null;
            for (TeamPod candidate : s.pods()) {
                if (candidate.members().contains(e.getPlayer().getUniqueId())) { pod = candidate; break; }
            }
            if (pod == null) return;   // not a voter: the hub returns them to the terrace
            Location back = s.venue().podSpawn(pod.index());
            if (back != null) {
                e.getPlayer().setFallDistance(0f);
                e.getPlayer().teleport(back);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        DomeSession s = session();
        if (s != null) s.onJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        DomeSession s = session();
        if (s != null) s.onQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        DomeSession s = session();
        if (s != null) s.onRespawn(e.getPlayer());
    }
}

