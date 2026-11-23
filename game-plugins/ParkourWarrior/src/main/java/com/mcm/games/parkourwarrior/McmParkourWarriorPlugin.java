package com.mcm.games.parkourwarrior;

import com.mcm.api.McmApi;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class McmParkourWarriorPlugin extends JavaPlugin {
    private ParkourWarriorGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        game = new ParkourWarriorGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        for (String c : new String[]{"pw", "pwview"}) {
            PluginCommand cmd = getCommand(c);
            if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        }
        McmApi.registerGame(this, game);
        try { game.preload(); } catch (Throwable t) { getLogger().warning("Parkour Warrior: could not prepare the world/course: " + t); }
        getLogger().info("Parkour Warrior ready (map.generator=" + getConfig().getString("map.generator", "auto") + ", "
                + (CourseWorld.mapClassPresent(getClassLoader()) ? "CourseMap present" : "CourseMap missing - the stub course will be used") + ")");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Parkour Warrior: " + t); }
            try { game.course.shutdown(); } catch (Throwable t) { getLogger().warning("Error shutting down course: " + t); }
        }
        try { McmApi.unregisterAll(this); } catch (Throwable ignored) {}
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pwview")) return view(sender, args);
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Parkour Warrior admin: " + ChatColor.YELLOW + "/pw list" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw tp <id|start|fork|hub N|view N>" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw complete <id> [player]" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw time <seconds-left>" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw info" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw rebuild [seed]");
            return true;
        }
        CourseModel m = game.course.model();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (m == null) { sender.sendMessage(ChatColor.RED + "Course not built yet."); return true; }
                StringBuilder sb = new StringBuilder();
                String kind = "";
                for (Obstacle o : m.obstacles) {
                    String k = o.kind() + (o.kind() == CourseModel.Kind.ENDING ? "" : " " + o.stage());
                    if (!k.equals(kind)) { if (sb.length() > 0) { sender.sendMessage(sb.toString()); sb.setLength(0); } kind = k; sb.append(ChatColor.GOLD).append(k).append(": "); }
                    sb.append(ChatColor.YELLOW).append(o.id()).append(ChatColor.GRAY).append(" ").append(o.name()).append("  ");
                }
                if (sb.length() > 0) sender.sendMessage(sb.toString());
                sender.sendMessage(ChatColor.GRAY + "Timed groups: " + m.timed.size() + ", hubs: " + m.hubs.size() + ", overviews: " + m.overviews.size());
            }
            case "tp" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (m == null || game.course.world() == null) { sender.sendMessage(ChatColor.RED + "Course not built yet."); return true; }
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "/pw tp <id|start|fork|hub N|view N>"); return true; }
                String t = args[1].toLowerCase(Locale.ROOT);
                Progress pr = new Progress(m);
                switch (t) {
                    case "start" -> game.teleport(p, game.course.loc(pr.startSpawn(0)));
                    case "fork" -> game.teleport(p, game.course.loc(pr.respawn(Progress.Node.FORK)));
                    case "hub" -> {
                        int n = args.length > 2 ? parse(args[2], 1) : 1;
                        game.teleport(p, game.course.loc(pr.respawn(Progress.Node.hub(n))));
                    }
                    case "view" -> game.teleport(p, game.course.overview(args.length > 2 ? parse(args[2], 1) - 1 : 0));
                    default -> {
                        Obstacle o = m.obstacle(args[1]);
                        if (o == null) { sender.sendMessage(ChatColor.RED + "Unknown obstacle " + args[1] + " (see /pw list)"); return true; }
                        game.adminTpObstacle(p, o);
                        sender.sendMessage(ChatColor.GREEN + "Teleported to " + o.id() + " " + o.name()
                                + (game.inPlay() && game.run(p) != null ? ChatColor.GRAY + " (checkpoint set to its start; no medals given)" : ""));
                    }
                }
            }
            case "complete" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "/pw complete <id> [player]"); return true; }
                Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player p ? p : null;
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                StringBuilder msg = new StringBuilder();
                boolean ok = game.adminComplete(target, args[1], msg);
                sender.sendMessage((ok ? ChatColor.GREEN : ChatColor.RED) + msg.toString());
            }
            case "time" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "/pw time <seconds-left> (negative = into overtime)"); return true; }
                if (!game.inPlay()) { sender.sendMessage(ChatColor.RED + "No game in play."); return true; }
                int left;
                try { left = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Not a number."); return true; }
                game.setSecondsLeft(left);
                sender.sendMessage(ChatColor.GREEN + "Normal time left set to " + left + " s.");
            }
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "Parkour Warrior: " + ChatColor.WHITE + game.info());
                if (m != null) {
                    sender.sendMessage(ChatColor.GOLD + "Course: " + ChatColor.WHITE + "seed " + m.seed + ", " + m.blocks.size() + " blocks, "
                            + m.obstacles.size() + " obstacles, bounds x" + m.minX + ".." + m.maxX + " y" + m.minY + ".." + m.maxY + " z" + m.minZ + ".." + m.maxZ);
                    if (!m.notes.isEmpty()) sender.sendMessage(ChatColor.GOLD + "Notes: " + ChatColor.WHITE + m.notes);
                }
                if (sender instanceof Player p && m != null) {
                    var l = p.getLocation();
                    sender.sendMessage(ChatColor.GRAY + "You: local " + String.format(Locale.ROOT, "%.1f %.1f %.1f", game.course.lx(l), game.course.ly(l), game.course.lz(l)));
                }
            }
            case "rebuild" -> {
                if (game.running()) { sender.sendMessage(ChatColor.RED + "A game is running."); return true; }
                reloadConfig();
                if (args.length > 1) {
                    try { getConfig().set("map.seed", Long.parseLong(args[1])); } catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Bad seed."); return true; }
                }
                Settings s = Settings.load(getConfig(), getLogger());
                sender.sendMessage(ChatColor.YELLOW + "Rebuilding the course (seed " + s.seed + ")...");
                game.course.prepare(s, true, () -> {
                    game.course.removeLabels();
                    sender.sendMessage(ChatColor.GREEN + "Course rebuilt with " + game.course.generatorUsed() + ".");
                });
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. /pw for help.");
        }
        return true;
    }

    private boolean view(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (!game.running() || p.getGameMode() != GameMode.SPECTATOR || p.getWorld() != game.course.world()) {
            sender.sendMessage(ChatColor.GRAY + "Only spectators in Parkour Warrior can use this.");
            return true;
        }
        int n = args.length > 0 ? parse(args[0], 1) : 1;
        game.teleport(p, game.course.overview(n - 1));
        return true;
    }

    private static int parse(String s, int def) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; } }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("pwview")) return out;
        if (args.length == 1) {
            for (String s : List.of("list", "tp", "complete", "time", "info", "rebuild")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("complete"))) {
            CourseModel m = game.course.model();
            List<String> opts = new ArrayList<>();
            if (args[0].equalsIgnoreCase("tp")) opts.addAll(List.of("start", "fork", "hub", "view"));
            if (m != null) for (Obstacle o : m.obstacles) opts.add(o.id());
            for (String s : opts) if (s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("complete")) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}
