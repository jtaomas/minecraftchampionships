package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmGame;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class DecisionDomePlugin extends JavaPlugin implements Listener {
    private DomeArena arena;
    private DomeVote vote;
    private final Random rng = new Random();

    @Override
    public void onEnable() {
        arena = new DomeArena(this);
        try {
            arena.ensureWorld();
        } catch (Throwable t) {
            getLogger().severe("Failed to build the Decision Dome world now (will retry on first vote): " + t);
        }
        vote = new DomeVote(this, arena);
        getServer().getPluginManager().registerEvents(this, this);
        McmApi.setVoteProvider(vote);
        getLogger().info("Decision Dome registered as the MCM vote provider.");
    }

    @Override
    public void onDisable() {
        if (McmApi.voteProvider() == vote) McmApi.setVoteProvider(null);
        if (vote != null) vote.shutdown();
        if (arena != null) arena.removeStrayEntities();
    }

    // ---------------- command ----------------

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
                // /dome test [hub|dome]: hub (MCC games, in the MCC hub's Decision Dome) when available, else the fallback dome
                String where = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : (HubStage.available() ? "hub" : "dome");
                boolean hub = "hub".equals(where);
                if (hub && !HubStage.available()) {
                    sender.sendMessage("§cThe MCC hub venue isn't available (hub disabled or still building). Use /dome test dome.");
                    return true;
                }
                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (McmGame g : McmApi.games()) ids.add(g.id().toLowerCase(Locale.ROOT));
                if (!hub || ids.size() < 2) ids.addAll(McmApi.builtInGameIds());
                if (ids.isEmpty()) {
                    sender.sendMessage("§cNo games installed to vote on.");
                    return true;
                }
                List<String> list = new ArrayList<>(ids);
                if (list.size() > DomeArena.MAX_WEDGES) {
                    Collections.shuffle(list, rng);
                    list = new ArrayList<>(list.subList(0, DomeArena.MAX_WEDGES));
                }
                if (!vote.startTest(list, hub)) {
                    sender.sendMessage("§cA real championship vote is running.");
                } else {
                    sender.sendMessage("§aStarted a test vote with " + list.size() + " games " + (hub ? "in the MCC hub dome." : "in the fallback dome."));
                }
            }
            case "cancel" -> {
                if (!vote.isActive()) sender.sendMessage("§7No vote is running.");
                else { vote.adminCancel(); sender.sendMessage("§aVote cancelled."); }
            }
            case "end" -> {
                if (vote.state() != DomeVote.State.VOTING) sender.sendMessage("§7Not in the voting phase.");
                else { vote.endVotingNow(); sender.sendMessage("§aEnding the voting phase."); }
            }
            case "skipwait" -> {
                if (vote.skipWait()) sender.sendMessage("§aSkipped the current vote phase.");
                else sender.sendMessage("§7Nothing to skip.");
            }
            case "tp" -> {
                if (!(sender instanceof Player p)) return true;
                boolean hub = args.length > 1 ? "hub".equalsIgnoreCase(args[1]) : HubStage.available();
                if (hub && HubStage.available()) p.teleport(vote.hubArrival(rng));
                else if (arena.ensureWorld() != null) p.teleport(arena.hubSpot(rng));
            }
            default -> {
                sender.sendMessage("§bDecision Dome: §f" + vote.state() + (vote.isTest() ? " (test)" : "")
                        + " §7| provider registered: " + (McmApi.voteProvider() == vote));
                sender.sendMessage("§7  stage: §f" + (vote.isActive() ? (vote.isHubMode() ? "MCC hub dome" : "fallback dome") : "-")
                        + " §7| hub venue available: " + HubStage.available());
                sender.sendMessage("§7/dome test [hub|dome] §f- demo vote  §7/dome end §f- end voting now  §7/dome skipwait §f- leader wins now  §7/dome cancel  §7/dome tp [hub|dome]");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("test", "end", "skipwait", "cancel", "status", "tp"))
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        if (args.length == 2 && ("test".equalsIgnoreCase(args[0]) || "tp".equalsIgnoreCase(args[0]))) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("hub", "dome")) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        return List.of();
    }

    // ---------------- rules inside mcc_dome ----------------

    /** The fallback dome world always; the MCC hub only while a vote runs there (the core owns the hub otherwise). */
    private boolean inDome(org.bukkit.World w) {
        return (arena != null && arena.isDomeWorld(w)) || (vote != null && vote.isHubMode() && vote.inStageWorld(w));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (inDome(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (inDome(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (inDome(e.getBlock().getWorld()) && !(e.getPlayer().getGameMode() == GameMode.CREATIVE && !vote.isActive()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (inDome(e.getBlock().getWorld()) && !(e.getPlayer().getGameMode() == GameMode.CREATIVE && !vote.isActive()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (inDome(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (inDome(e.getPlayer().getWorld()) && vote.isActive()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (inDome(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (!inDome(to.getWorld())) return;
        boolean hub = vote.isHubMode() && vote.inStageWorld(to.getWorld());
        double limit = hub ? vote.stage().voidY() : arena.floorY() - 6;
        if (to.getY() < limit) {
            e.getPlayer().setFallDistance(0f);
            e.getPlayer().teleport(hub ? vote.stage().arrival(rng) : arena.hubSpot(rng));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (vote != null) vote.onJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (vote != null) vote.onQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        if (vote != null && vote.isActive() && vote.stage() != null && vote.stage().world() != null) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(this, () -> { if (vote.isActive() && p.isOnline()) vote.bringIn(p); }, 2L);
        }
    }
}
