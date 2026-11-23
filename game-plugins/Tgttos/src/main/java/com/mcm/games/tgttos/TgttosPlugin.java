package com.mcm.games.tgttos;

import com.mcm.api.McmApi;
import com.mcm.api.McmBuild;
import com.mcm.games.tgttos.model.TgMapData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class TgttosPlugin extends JavaPlugin {
    public static final String WORLD = "mcc_tgttos";

    private World world;
    private MapRegistry registry;
    private MapPlacer placer;
    private TgttosGame game;
    private McmBuild.BuildQueue adminQueue;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        resetWorldFolder();
        world = McmBuild.voidWorld(WORLD);
        if (world == null) getLogger().severe("Could not create world " + WORLD + "; the game cannot run.");
        else applyRules(world);
        placer = new MapPlacer(getLogger());
        registry = new MapRegistry(getLogger());
        loadMaps();
        game = new TgttosGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        PluginCommand cmd = getCommand("tg");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        try {
            if (game != null) game.stop();
            if (adminQueue != null) adminQueue.cancel();
        } catch (Throwable t) {
            getLogger().warning("Error while stopping TGTTOS: " + t);
        }
        McmApi.unregisterAll(this);
    }

    void loadMaps() {
        reloadConfig();
        registry.load(getConfig().getStringList("maps"), getConfig().getBoolean("auto-discover", true), getFile(),
                getConfig().getString("stub-map", "auto"), Math.max(1, getConfig().getInt("rounds", 6)));
        registry.validate();
        getLogger().info("TGTTOS: " + registry.playable().size() + " playable map(s): " + registry.ids());
    }

    /** The world is regenerated on every enable so no stale map/player blocks survive a restart. */
    private void resetWorldFolder() {
        if (Bukkit.getWorld(WORLD) != null) return;
        File dir = new File(Bukkit.getWorldContainer(), WORLD);
        if (!dir.exists()) return;
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException { Files.delete(f); return FileVisitResult.CONTINUE; }
                @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException { Files.delete(d); return FileVisitResult.CONTINUE; }
            });
        } catch (IOException e) {
            getLogger().warning("Could not delete old world folder " + dir + ": " + e);
        }
    }

    static void applyRules(World w) {
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.FIRE_DAMAGE, false);
        w.setGameRule(GameRule.DROWNING_DAMAGE, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        w.setGameRule(GameRule.DO_MOB_LOOT, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        w.setDifficulty(Difficulty.PEACEFUL);
    }

    World world() {
        if (world == null || Bukkit.getWorld(WORLD) == null) {
            world = McmBuild.voidWorld(WORLD);
            if (world != null) applyRules(world);
        }
        return world;
    }

    MapRegistry registry() { return registry; }
    MapPlacer placer() { return placer; }

    // ------------------------------------------------------------------ /tg

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                sender.sendMessage(ChatColor.GOLD + "TGTTOS maps (" + registry.playable().size() + " playable):");
                for (MapRegistry.Entry e : registry.all()) {
                    String st = e.broken ? ChatColor.RED + " BROKEN" : e.problems.isEmpty() ? ChatColor.GREEN + " ok" : ChatColor.YELLOW + " " + e.problems.size() + " warning(s)";
                    sender.sendMessage(ChatColor.YELLOW + e.map.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + e.map.displayName()
                            + ChatColor.GRAY + " [" + e.map.type() + ", " + e.map.kit() + ", slot " + e.slot + "]" + st
                            + (e.stub ? ChatColor.DARK_GRAY + " (stub)" : ""));
                }
            }
            case "map" -> {
                MapRegistry.Entry e = args.length > 1 ? registry.byId(args[1]) : null;
                if (e == null) { usage(sender, "map"); return true; }
                if (e.broken) { sender.sendMessage(ChatColor.RED + e.map.id() + " is not playable (see /tg info)."); return true; }
                game.forceNext(e);
                sender.sendMessage(ChatColor.GREEN + e.map.displayName() + " will be played as the "
                        + (game.isRunning() ? "next round" : "first round of the next game") + ".");
            }
            case "info" -> {
                MapRegistry.Entry e = args.length > 1 ? registry.byId(args[1]) : null;
                if (e == null) { usage(sender, "info"); return true; }
                TgMapData d = e.cached;
                sender.sendMessage(ChatColor.GOLD + e.map.displayName() + ChatColor.GRAY + " (" + e.map.id() + ", " + e.map.type() + ", kit " + e.map.kit() + ", slot " + e.slot + ")");
                if (d == null) { sender.sendMessage(ChatColor.RED + "Build failed."); return true; }
                sender.sendMessage(ChatColor.GRAY + "blocks " + d.blocks.size() + ", spawns " + d.startSpawns.size() + ", chickens " + d.chickenSpawns.size()
                        + ", barrier " + d.startBarrier.size() + ", drop floor " + d.dropFloor.size() + ", boats " + d.boatSpawns.size()
                        + ", variants " + d.variants.size() + ", voidY " + d.voidY + ", hazards " + d.hazardBlocks + " + " + d.hazardRegions.size() + " regions");
                if (!d.variants.isEmpty()) sender.sendMessage(ChatColor.GRAY + "sample roll: " + Variants.summary(d, Variants.pick(d, new Random())));
                for (String p : e.problems) sender.sendMessage(ChatColor.YELLOW + "! " + p);
                for (Map.Entry<String, String> n : d.notes.entrySet()) sender.sendMessage(ChatColor.DARK_AQUA + n.getKey() + ": " + ChatColor.WHITE + n.getValue());
            }
            case "tp", "build" -> {
                MapRegistry.Entry e = args.length > 1 ? registry.byId(args[1]) : null;
                if (e == null || e.cached == null) { usage(sender, sub); return true; }
                if (game.isRunning()) { sender.sendMessage(ChatColor.RED + "A game is running; use /tg map to queue a map."); return true; }
                World w = world();
                if (w == null) { sender.sendMessage(ChatColor.RED + "World missing."); return true; }
                if (adminQueue != null) adminQueue.cancel();
                Settings s = Settings.load(getConfig());
                placer.baseY = s.baseY; placer.spacing = s.spacing;
                adminQueue = new McmBuild.BuildQueue(this);
                TgMapData d = e.cached;
                placer.enqueue(adminQueue, w, e.slot, d, Variants.pick(d, new Random()));
                adminQueue.then(() -> {
                    sender.sendMessage(ChatColor.GREEN + "Built " + e.map.displayName() + " at slot " + e.slot + ".");
                    if (sub.equals("tp") && sender instanceof Player p && p.isOnline()) {
                        TgMapData.Spawn sp = !d.overviewPoints.isEmpty() ? d.overviewPoints.get(0) : d.startSpawns.get(0);
                        p.teleport(placer.loc(w, e.slot, sp));
                        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) p.setGameMode(GameMode.CREATIVE);
                    }
                });
                adminQueue.start();
            }
            case "reload" -> {
                if (game.isRunning()) { sender.sendMessage(ChatColor.RED + "A game is running."); return true; }
                loadMaps();
                sender.sendMessage(ChatColor.GREEN + "Reloaded config; " + registry.playable().size() + " playable maps.");
            }
            default -> sender.sendMessage(ChatColor.GOLD + "TGTTOS admin: " + ChatColor.YELLOW
                    + "/tg list | map <id> | info <id> | tp <id> | build <id> | reload");
        }
        return true;
    }

    private void usage(CommandSender s, String sub) {
        s.sendMessage(ChatColor.RED + "Usage: /tg " + sub + " <" + String.join("|", registry.ids()) + ">");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("list", "map", "info", "tp", "build", "reload")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2) {
            for (String s : registry.ids()) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
