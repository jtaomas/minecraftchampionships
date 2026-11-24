package com.mcm.games.acerace;

import com.mcm.api.McmApi;
import com.mcm.games.acerace.map.maps.TestGridMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** McmAceRace v2: map-driven Ace Race on top of MinecraftMondayCore. */
public final class AceRacePlugin extends JavaPlugin {
    private RaceRegistry registry;
    private RaceWorld world;
    private RaceGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registry = new RaceRegistry();
        registry.register(new TestGridMap(), 0);

        world = new RaceWorld(this);
        world.ensureWorld();

        game = new RaceGame(this, registry, world);
        getServer().getPluginManager().registerEvents(new RaceListener(this, game), this);

        AceRaceCommand command = new AceRaceCommand(this);
        PluginCommand cmd = getCommand("acerace");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        McmApi.registerGame(this, game);
        getLogger().info("McmAceRace v2.0.0 enabled with " + registry.ids().size() + " map(s).");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Ace Race: " + t); }
        }
        if (world != null) {
            try { world.disable(); } catch (Throwable t) { getLogger().warning("Error releasing Ace Race world: " + t); }
        }
        McmApi.unregisterAll(this);
    }

    public RaceRegistry registry() { return registry; }
    public RaceWorld world() { return world; }
    public RaceGame game() { return game; }
}
