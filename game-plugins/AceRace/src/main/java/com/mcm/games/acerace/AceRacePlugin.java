package com.mcm.games.acerace;

import com.mcm.api.McmApi;
import com.mcm.games.acerace.map.maps.SkyCircuitMap;
import com.mcm.games.acerace.world.WorldMap;
import com.mcm.games.acerace.world.WorldRace;
import com.mcm.games.acerace.world.WorldRaceListener;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** McmAceRace: Ace Race on imported course worlds (or built-in procedural fallback when no imported world is installed). */
public final class AceRacePlugin extends JavaPlugin {
    // City Marathon removed (owner, 2026-11-25: "too rubbish"); Space Race and Yeti Set Go parked (owner, 2026-11-25:
    // "we'll just have Clouds 2 for now"). Their world files stay in resources/worlds/ for later.
    private static final String[] DEFAULT_WORLDS = {"clouds2"};

    private RaceRegistry registry;
    private RaceWorld world;
    private RaceGame game;
    private WorldRace worldRace;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registry = new RaceRegistry();
        registry.register(new SkyCircuitMap(), 0);
        world = new RaceWorld(this);
        game = new RaceGame(this, registry, world);

        worldRace = new WorldRace(this);
        int loadedWorlds = 0;
        File dir = new File(getDataFolder(), "worlds");
        for (String id : DEFAULT_WORLDS) {
            if (!new File(dir, id + ".yml").isFile()) saveResource("worlds/" + id + ".yml", false);
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File f : files) {
                String id = f.getName().substring(0, f.getName().length() - 4);
                WorldMap m = new WorldMap(id, YamlConfiguration.loadConfiguration(f));
                if (!m.problems.isEmpty()) {
                    getLogger().warning("Ace Race map " + id + " skipped: " + m.problems);
                    continue;
                }
                if (Bukkit.getWorld(m.worldName) == null) {
                    File wf = new File(Bukkit.getWorldContainer(), m.worldName);
                    if (new File(wf, "level.dat").isFile()) {
                        getLogger().info("Loading Ace Race world folder " + m.worldName + "...");
                        new WorldCreator(m.worldName).createWorld();
                    }
                }
                if (Bukkit.getWorld(m.worldName) != null) {
                    worldRace.register(m);
                    loadedWorlds++;
                    getLogger().info("Ace Race map " + id + " -> world " + m.worldName + " (" + m.rules.size() + " pad rules, "
                            + m.waypoints.size() + " waypoints, " + m.laps + " lap(s))");
                } else {
                    getLogger().warning("Ace Race imported world '" + m.worldName + "' for map '" + id + "' is not present on disk.");
                }
            }
        }
        getServer().getPluginManager().registerEvents(new WorldRaceListener(worldRace), this);
        getServer().getPluginManager().registerEvents(new RaceListener(this, game), this);

        AceRaceCommand command = new AceRaceCommand(this);
        PluginCommand cmd = getCommand("acerace");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        if (loadedWorlds > 0) {
            // Find the arrow pads of every map once the worlds are loaded, so no race waits on the scan.
            getServer().getScheduler().runTaskLater(this, worldRace::prescan, 200L);
            McmApi.registerGame(this, worldRace);
            getLogger().info("McmAceRace enabled with " + worldRace.mapCount() + " imported world map(s).");
        } else {
            McmApi.registerGame(this, game);
            getLogger().info("McmAceRace enabled using built-in procedural course(s): " + registry.ids()
                    + " (drop mcc_clouds2 world folder in server root to switch to Clouds 2).");
        }
    }

    @Override
    public void onDisable() {
        if (worldRace != null) {
            try { worldRace.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Ace Race: " + t); }
        }
        if (world != null) {
            try { world.disable(); } catch (Throwable t) { getLogger().warning("Error releasing Ace Race world: " + t); }
        }
        McmApi.unregisterAll(this);
    }

    public RaceRegistry registry() { return registry; }
    public RaceWorld world() { return world; }
    public RaceGame game() { return game; }
    public WorldRace worldRace() { return worldRace; }
}
