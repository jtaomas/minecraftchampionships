package com.mcm.games.race;

import com.mcm.api.McmApi;
import com.mcm.api.McmBuild;
import com.mcm.games.race.model.TgMapData;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RacePlugin extends JavaPlugin {
    public static final String WORLD = "mcc_race";

    private World world;
    private MapRegistry registry;
    private MapPlacer placer;
    private RaceGame game;
    private Paster paster;
    private Paster.Job adminJob;
    /** One worker thread for everything heavy and Bukkit-free: map generation + paste preparation (serialised). */
    private ExecutorService worker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        resetWorldFolder();
        world = McmBuild.voidWorld(WORLD);
        if (world == null) getLogger().severe("Could not create world " + WORLD + "; the game cannot run.");
        else applyRules(world);
        placer = new MapPlacer(getLogger());
        worker = Executors.newSingleThreadExecutor(r -> { Thread th = new Thread(r, "Race-builder"); th.setDaemon(true); return th; });
        paster = new Paster(this, placer, worker);
        registry = new MapRegistry(getLogger());
        loadMaps();
        game = new RaceGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        PluginCommand cmd = getCommand("race");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        McmApi.registerGame(this, game);
        Overlays.init(this);
        try {
            McmApi.registerMapEditable(this, game.id(), new RaceEdit(this));   // core /mcm mapedit (Map Edit core; older cores skip it)
        } catch (Throwable t) {
            getLogger().info("Map Edit is not available on this core (" + t.getClass().getSimpleName() + ").");
        }
    }

    @Override
    public void onDisable() {
        try {
            if (game != null) game.stop();
            if (paster != null) { paster.cancelAll(); paster.releaseAll(); }
            if (worker != null) worker.shutdownNow();
        } catch (Throwable t) {
            getLogger().warning("Error while stopping Race: " + t);
        }
        McmApi.unregisterAll(this);
    }

    /**
     * Instantiates and builds every map on the worker thread (the maps are pure Java), then installs the result on
     * the main thread. Never blocks a tick; a game started before this finishes waits for it ({@link MapRegistry#ready}).
     */
    void loadMaps() { loadMaps(null); }

    void loadMaps(Runnable then) {
        reloadConfig();
        applyDebug();
        List<String> classes = getConfig().getStringList("maps");
        boolean auto = getConfig().getBoolean("auto-discover", true);
        String stub = getConfig().getString("stub-map", "auto");
        int rounds = Math.max(1, getConfig().getInt("rounds", 6));
        File jar = getFile();
        File importedDir = new File(getDataFolder(), "imported");
        long t0 = System.nanoTime();
        worker.execute(() -> {
            List<MapRegistry.Entry> list;
            try {
                list = registry.scan(classes, auto, jar, stub, rounds);
                try {
                    registry.addImported(list, importedDir);   // absent folder: nothing happens
                } catch (Throwable t) {
                    getLogger().warning("Race: imported maps skipped: " + t);
                }
                registry.validate(list);
            } catch (Throwable t) {
                getLogger().severe("Race: loading maps failed: " + t);
                list = new ArrayList<>();
            }
            List<MapRegistry.Entry> done = list;
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Bukkit.getScheduler().runTask(this, () -> {
                registry.install(done);
                getLogger().info("Race: " + registry.playable().size() + " playable map(s), built off-thread in " + ms + " ms: " + registry.ids());
                if (then != null) then.run();
            });
        });
    }

    void applyDebug() { if (paster != null) paster.debugTiming = getConfig().getBoolean("debug-timing", true); }

    Paster paster() { return paster; }
    ExecutorService worker() { return worker; }

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
        w.setGameRule(GameRule.DO_FIRE_TICK, false);        // fire rings / braziers stay put
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
                sender.sendMessage(ChatColor.GOLD + "Race maps (" + registry.playable().size() + " playable):");
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
                if (e.broken) { sender.sendMessage(ChatColor.RED + e.map.id() + " is not playable (see /race info)."); return true; }
                game.forceNext(e);
                sender.sendMessage(ChatColor.GREEN + e.map.displayName() + " will be played as the "
                        + (game.isRunning() ? "next round" : "first round of the next game") + ".");
            }
            case "chickens" -> { for (String l : game.liveStatus()) sender.sendMessage(l); }
            case "info" -> {
                if (args.length == 1) { for (String l : game.liveStatus()) sender.sendMessage(l); return true; }
                MapRegistry.Entry e = registry.byId(args[1]);
                if (e == null) { usage(sender, "info"); return true; }
                TgMapData d = e.cached;
                sender.sendMessage(ChatColor.GOLD + e.map.displayName() + ChatColor.GRAY + " (" + e.map.id() + ", " + e.map.type() + ", kit " + e.map.kit() + ", slot " + e.slot + ")");
                if (d == null) { sender.sendMessage(ChatColor.RED + "Build failed."); return true; }
                sender.sendMessage(ChatColor.GRAY + "blocks " + d.blocks.size() + ", spawns " + d.startSpawns.size() + ", chickens " + d.chickenSpawns.size()
                        + ", barrier " + d.startBarrier.size() + ", drop floor " + d.dropFloor.size() + ", boats " + d.boatSpawns.size()
                        + ", variants " + d.variants.size() + ", voidY " + d.voidY + ", hazards " + d.hazardBlocks + " + " + d.hazardRegions.size() + " regions");
                if (!d.variants.isEmpty()) sender.sendMessage(ChatColor.GRAY + "sample roll: " + Variants.summary(d, Variants.pick(d, new Random())));
                sender.sendMessage(ChatColor.GRAY + "lethal fall: " + (e.map.lethalFallHeight() > 0 ? e.map.lethalFallHeight() + " blocks (water only)" : "off")
                        + ", decoy chickens " + e.map.decoyChickens().size() + ", pen " + d.pen);
                if (e.map.credit() != null || e.map.roundSeconds() > 0 || !e.map.inRotation())
                    sender.sendMessage(ChatColor.GRAY + "map by " + e.map.credit() + (e.map.roundSeconds() > 0 ? ", round " + e.map.roundSeconds() + " s" : "")
                            + (e.map.inRotation() ? "" : ", only when forced (/race map)")
                            + (e.map instanceof com.mcm.games.race.imported.ImportedMap im ? ", imported from folder " + im.folder() : ""));
                for (String p : e.problems) sender.sendMessage(ChatColor.YELLOW + "! " + p);
                for (Map.Entry<String, String> n : d.notes.entrySet()) sender.sendMessage(ChatColor.DARK_AQUA + n.getKey() + ": " + ChatColor.WHITE + n.getValue());
                if (game.isRunning() && game.current() == e) for (String l : game.liveStatus()) sender.sendMessage(l);
            }
            case "tp", "build" -> {
                MapRegistry.Entry e = args.length > 1 ? registry.byId(args[1]) : null;
                if (e == null || e.cached == null) { usage(sender, sub); return true; }
                if (game.isRunning()) { sender.sendMessage(ChatColor.RED + "A game is running; use /race map to queue a map."); return true; }
                World w = world();
                if (w == null) { sender.sendMessage(ChatColor.RED + "World missing."); return true; }
                if (adminJob != null) paster.cancel(adminJob);
                Settings s = Settings.load(getConfig());
                placer.baseY = s.baseY; placer.spacing = s.spacing;
                TgMapData d = e.cached;
                sender.sendMessage(ChatColor.GRAY + "Pasting " + e.map.displayName() + " (spread over ticks)...");
                adminJob = paster.paste("admin " + e.map.id(), w, e.slot, d, Variants.pick(d, new Random()));
                adminJob.then(() -> {
                    adminJob = null;
                    sender.sendMessage(ChatColor.GREEN + "Built " + e.map.displayName() + " at slot " + e.slot + ".");
                    if (sub.equals("tp") && sender instanceof Player p && p.isOnline()) {
                        TgMapData.Spawn sp = !d.overviewPoints.isEmpty() ? d.overviewPoints.get(0) : d.startSpawns.get(0);
                        p.teleport(placer.loc(w, e.slot, sp));
                        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) p.setGameMode(GameMode.CREATIVE);
                    }
                });
            }
            case "reload" -> {
                if (game.isRunning()) { sender.sendMessage(ChatColor.RED + "A game is running."); return true; }
                sender.sendMessage(ChatColor.GRAY + "Reloading config and rebuilding maps off-thread...");
                loadMaps(() -> sender.sendMessage(ChatColor.GREEN + "Reloaded config; " + registry.playable().size() + " playable maps."));
            }
            default -> sender.sendMessage(ChatColor.GOLD + "Race admin: " + ChatColor.YELLOW
                    + "/race list | map <id> | info [id] | chickens | tp <id> | build <id> | reload");
        }
        return true;
    }

    private void usage(CommandSender s, String sub) {
        s.sendMessage(ChatColor.RED + "Usage: /race " + sub + " <" + String.join("|", registry.ids()) + ">");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("list", "map", "info", "chickens", "tp", "build", "reload")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2) {
            for (String s : registry.ids()) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
