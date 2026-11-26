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
        try {
            McmApi.registerMapEditable(this, game.id(), new PwEdit(this, game));   // core /mcm mapedit (Map Edit core; older cores skip it)
        } catch (Throwable t) {
            getLogger().info("Map Edit is not available on this core (" + t.getClass().getSimpleName() + ").");
        }
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
                    + ChatColor.YELLOW + "/pw ghost [on|off]" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw rebuild [seed]" + ChatColor.GRAY + ", "
                    + ChatColor.YELLOW + "/pw map [id|random]");
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
                                + (game.inPlay() && game.run(p) != null ? ChatColor.GRAY + " (checkpoint set to its start; no coins given)" : ""));
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
            case "ghost" -> {
                boolean cur = getConfig().getBoolean("ghost-players", getConfig().getBoolean("players.translucent", true));
                boolean on;
                if (args.length < 2) on = !cur;
                else if (args[1].equalsIgnoreCase("on")) on = true;
                else if (args[1].equalsIgnoreCase("off")) on = false;
                else { sender.sendMessage(ChatColor.RED + "/pw ghost [on|off]"); return true; }
                getConfig().set("ghost-players", on);
                saveConfig();
                game.setGhost(on);
                sender.sendMessage(ChatColor.GREEN + "Ghost players " + (on ? "ON (runners translucent to each other)" : "OFF (runners fully visible)")
                        + ChatColor.GRAY + "; player collision stays off" + (game.running() ? ", applied to the running game." : "."));
            }
            case "map" -> {
                reloadConfig();
                Settings st = Settings.load(getConfig(), getLogger());
                List<String> av = game.course.availableMaps(st);
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.GOLD + "Maps: " + ChatColor.WHITE + av + ChatColor.GRAY + "  built: " + game.course.mapId()
                            + (game.forcedMap != null ? "  forced for the next games: " + game.forcedMap : "  next game: random"));
                    sender.sendMessage(ChatColor.GRAY + "/pw map <id> forces that map for the following games, /pw map random goes back to a random pick.");
                    return true;
                }
                String id = args[1].toLowerCase(Locale.ROOT);
                if (id.equals("random") || id.equals("auto")) {
                    game.forcedMap = null;
                    sender.sendMessage(ChatColor.GREEN + "Map choice: random among " + av + ".");
                    return true;
                }
                if (!av.contains(id)) { sender.sendMessage(ChatColor.RED + "Unknown map '" + id + "'. Available: " + av); return true; }
                game.forcedMap = id;
                sender.sendMessage(ChatColor.GREEN + "Next games use the map '" + id + "'.");
                if (!game.running()) {
                    sender.sendMessage(ChatColor.YELLOW + "Building it now...");
                    game.course.prepare(st, id, false, () -> {
                        game.course.removeLabels();
                        sender.sendMessage(ChatColor.GREEN + "Map '" + game.course.mapId() + "' is built (" + game.course.generatorUsed() + ").");
                    });
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
                game.course.prepare(s, game.pickMap(s, false), true, () -> {
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
            for (String s : List.of("list", "tp", "complete", "time", "info", "ghost", "rebuild", "map")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("complete"))) {
            CourseModel m = game.course.model();
            List<String> opts = new ArrayList<>();
            if (args[0].equalsIgnoreCase("tp")) opts.addAll(List.of("start", "fork", "hub", "view"));
            if (m != null) for (Obstacle o : m.obstacles) opts.add(o.id());
            for (String s : opts) if (s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("map")) {
            List<String> ids = new ArrayList<>(game.course.availableMaps(Settings.load(getConfig(), getLogger())));
            ids.add("random");
            for (String s : ids) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ghost")) {
            for (String s : List.of("on", "off")) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("complete")) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}
