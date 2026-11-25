package com.mcm.games.acerace;

import com.mcm.api.McmApi;
import com.mcm.games.acerace.world.WorldMap;
import com.mcm.games.acerace.world.WorldRace;
import com.mcm.games.acerace.world.WorldRaceListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** McmAceRace: Ace Race on imported course worlds, on top of MinecraftMondayCore. */
public final class AceRacePlugin extends JavaPlugin {
    private static final String[] DEFAULT_WORLDS = {"clouds2", "spacerace", "yetisetgo"};   // City Marathon removed (owner, 2026-11-25: "too rubbish")

    private RaceRegistry registry;
    private RaceWorld world;
    private RaceGame game;
    private WorldRace worldRace;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Owner (2026-11-25): the generated maps (Sunset Skyway, Kessler Run, test grid) are parked outside the repo.
        // The generated-map runtime stays for the admin command but has no maps and no world of its own.
        registry = new RaceRegistry();
        world = new RaceWorld(this);
        game = new RaceGame(this, registry, world);

        worldRace = new WorldRace(this);
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
                worldRace.register(m);
                getLogger().info("Ace Race map " + id + " -> world " + m.worldName + " (" + m.rules.size() + " pad rules, "
                        + m.waypoints.size() + " waypoints, " + m.laps + " lap(s))");
            }
        }
        getServer().getPluginManager().registerEvents(new WorldRaceListener(worldRace), this);
        // Find the arrow pads of every map once the worlds are loaded, so no race waits on the scan.
        getServer().getScheduler().runTaskLater(this, worldRace::prescan, 200L);

        AceRaceCommand command = new AceRaceCommand(this);
        PluginCommand cmd = getCommand("acerace");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        McmApi.registerGame(this, worldRace);
        getLogger().info("McmAceRace enabled with " + worldRace.mapCount() + " map(s).");
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
