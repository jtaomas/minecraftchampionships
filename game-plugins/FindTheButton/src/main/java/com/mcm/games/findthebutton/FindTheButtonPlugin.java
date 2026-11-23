package com.mcm.games.findthebutton;

import com.mcm.api.McmApi;
import com.mcm.api.McmBuild;
import com.mcm.games.findthebutton.map.FtbMap;
import com.mcm.games.findthebutton.map.Maps;
import com.mcm.games.findthebutton.map.Reach;
import com.mcm.games.findthebutton.map.Spot;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FindTheButtonPlugin extends JavaPlugin {
    public static final String WORLD = "mcc_findthebutton";

    private final List<FtbMap> maps = new ArrayList<>(Maps.all());
    /** Spots that passed the reachability validation, per map id. */
    private final Map<String, List<Spot>> validSpots = new LinkedHashMap<>();
    private World world;
    private FindTheButtonGame game;
    private McmBuild.BuildQueue adminQueue;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        world = McmBuild.voidWorld(WORLD);
        if (world == null) getLogger().severe("Could not create world " + WORLD + "; the game cannot run.");
        else applyRules(world);
        validateMaps();
        game = new FindTheButtonGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(this, game), this);
        PluginCommand cmd = getCommand("ftbmap");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        try {
            if (game != null) game.stop();
            if (adminQueue != null) adminQueue.cancel();
        } catch (Throwable t) {
            getLogger().warning("Error while stopping Find the Button: " + t);
        }
        McmApi.unregisterAll(this);
    }

    static void applyRules(World w) {
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);       // no leaf decay, grass spread, crop growth
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        w.setDifficulty(Difficulty.PEACEFUL);
    }

    /** Offline-style reachability validation of every hiding spot (same code as tools/PreviewMain). */
    private void validateMaps() {
        for (FtbMap m : maps) {
            List<Reach.Result> results = new ArrayList<>();
            List<String> problems = Reach.validate(m, results);
            List<Spot> ok = new ArrayList<>();
            for (Reach.Result r : results) if (r.ok()) ok.add(r.spot());
            validSpots.put(m.id(), ok);
            if (problems.isEmpty()) getLogger().info("Map " + m.displayName() + ": " + ok.size() + "/" + m.spots().size() + " hiding spots validated.");
            else for (String p : problems) getLogger().warning("Map " + m.displayName() + ": " + p);
        }
    }

    World world() {
        if (world == null || world.getName() == null || getServer().getWorld(WORLD) == null) {
            world = McmBuild.voidWorld(WORLD);
            if (world != null) applyRules(world);
        }
        return world;
    }

    List<FtbMap> maps() { return maps; }

    int indexOf(FtbMap m) { return maps.indexOf(m); }

    List<Spot> validSpots(FtbMap m) { return validSpots.getOrDefault(m.id(), List.of()); }

    FtbMap mapById(String id) {
        for (FtbMap m : maps) if (m.id().equalsIgnoreCase(id)) return m;
        return null;
    }

    // ------------------------------------------------------------------ admin command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                for (FtbMap m : maps)
                    sender.sendMessage(ChatColor.YELLOW + m.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + m.displayName()
                            + ChatColor.GRAY + " (" + validSpots(m).size() + "/" + m.spots().size() + " spots valid)");
            }
            case "build", "tp", "dump", "spots" -> {
                if (game.isRunning()) { sender.sendMessage(ChatColor.RED + "A game is running."); return true; }
                FtbMap m = args.length > 1 ? mapById(args[1]) : null;
                if (m == null) { sender.sendMessage(ChatColor.RED + "Usage: /ftbmap " + sub + " <" + String.join("|", ids()) + ">"); return true; }
                World w = world();
                if (w == null) { sender.sendMessage(ChatColor.RED + "World missing."); return true; }
                int idx = indexOf(m);
                switch (sub) {
                    case "build" -> {
                        if (adminQueue != null) adminQueue.cancel();
                        adminQueue = new McmBuild.BuildQueue(this);
                        MapPlacer.enqueue(adminQueue, w, idx, m, getLogger());
                        adminQueue.then(() -> sender.sendMessage(ChatColor.GREEN + "Built " + m.displayName() + "."));
                        adminQueue.start();
                    }
                    case "tp" -> {
                        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                        p.teleport(MapPlacer.at(w, idx, (m.padX1() + m.padX2()) / 2.0 + 0.5, 1, (m.padZ1() + m.padZ2()) / 2.0 + 0.5));
                        if (p.getGameMode() == GameMode.SURVIVAL) p.setGameMode(GameMode.CREATIVE);
                    }
                    case "spots" -> {
                        for (Spot s : m.spots()) {
                            boolean ok = validSpots(m).contains(s);
                            sender.sendMessage((ok ? ChatColor.GREEN : ChatColor.RED) + s.name() + ChatColor.GRAY + " (" + s.difficulty() + ") at "
                                    + (MapPlacer.originX(idx) + s.x()) + " " + (MapPlacer.BASE_Y + s.y()) + " " + (MapPlacer.originZ(idx) + s.z()));
                        }
                    }
                    default -> {
                        File dir = new File(getDataFolder(), "dumps");
                        dir.mkdirs();
                        File out = new File(dir, m.id() + ".mcmdump.gz");
                        Location a = MapPlacer.at(w, idx, -1, -1, -1), b = MapPlacer.at(w, idx, 22, m.height + 3, 22);
                        McmBuild.dumpRegion(w, a, b, out, r -> sender.sendMessage(r.error() == null
                                ? ChatColor.GREEN + "Dumped " + r.blocks() + " blocks to " + r.file()
                                : ChatColor.RED + "Dump failed: " + r.error()));
                    }
                }
            }
            default -> sender.sendMessage(ChatColor.GOLD + "Find the Button admin: " + ChatColor.YELLOW
                    + "/ftbmap list | build <map> | tp <map> | spots <map> | dump <map>");
        }
        return true;
    }

    private List<String> ids() {
        List<String> out = new ArrayList<>();
        for (FtbMap m : maps) out.add(m.id());
        return out;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("list", "build", "tp", "spots", "dump")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2) {
            for (String s : ids()) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
