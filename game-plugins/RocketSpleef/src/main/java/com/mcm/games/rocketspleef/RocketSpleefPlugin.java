package com.mcm.games.rocketspleef;

import com.mcm.api.McmApi;
import com.mcm.games.rocketspleef.map.MapGen;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RocketSpleefPlugin extends JavaPlugin {
    private RocketSpleefGame game;

    @Override
    public void onEnable() {
        RocketConfig config = new RocketConfig(this);
        World world = createVoidWorld(RocketSpleefGame.WORLD_NAME);
        game = new RocketSpleefGame(this, config, world);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
        getLogger().info("Rocket Spleef (old MCC) ready: maps " + game.maps() + ", rocket jump "
                + config.rocketJumpHeight + " blocks = " + String.format("%.3f", config.rocketJumpVelocity) + " blocks/tick");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error while stopping Rocket Spleef: " + t); }
        }
        McmApi.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length > 0 ? a[0].toLowerCase() : "help";
        switch (sub) {
            case "reload" -> {
                game.setConfig(new RocketConfig(this));
                s.sendMessage("§aRocket Spleef config reloaded. Rocket jump " + game.config().rocketJumpHeight + " blocks.");
            }
            case "show" -> {
                if (a.length < 2 || MapGen.byId(a[1]) == null) { s.sendMessage("§cUsage: /rs show <" + String.join("|", game.maps()) + ">"); return true; }
                PastedMap m = game.showMap(a[1]);
                if (m == null) { s.sendMessage("§cStop the running game first."); return true; }
                s.sendMessage("§aPasted " + m.gen.displayName() + " (y " + m.minWorldY + ".." + m.maxWorldY + ", " + m.spawns.size() + " spawns).");
                if (s instanceof Player p) p.teleport(new Location(m.world, 0.5, m.maxWorldY + 10, m.radius + 12, 180f, 30f));
            }
            case "clear" -> {
                if (game.phase() != RocketSpleefGame.Phase.IDLE) { s.sendMessage("§cA game is running."); return true; }
                game.clearMap();
                s.sendMessage("§aPreview map cleared.");
            }
            case "tnt" -> s.sendMessage(game.forceTntTime() ? "§cTNT Time forced." : "§cOnly during the first 2 minutes of a round.");
            case "debug" -> s.sendMessage("§7[rs] " + game.debugLine());
            default -> s.sendMessage("§e/rs show <map> §7| §e/rs clear §7| §e/rs reload §7| §e/rs tnt §7| §e/rs debug");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) return List.of("show", "clear", "reload", "tnt", "debug");
        if (a.length == 2 && a[0].equalsIgnoreCase("show")) return new ArrayList<>(game.maps());
        return List.of();
    }

    static World createVoidWorld(String name) {
        World existing = org.bukkit.Bukkit.getWorld(name);
        if (existing != null) { configureWorld(existing); return existing; }
        WorldCreator wc = new WorldCreator(name);
        wc.generator(new ChunkGenerator() {
            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                return createChunkData(world);
            }
        });
        World w = wc.createWorld();
        if (w != null) configureWorld(w);
        return w;
    }

    private static void configureWorld(World w) {
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setTime(6000L);
        w.setSpawnLocation(0, 100, 0);
    }
}
