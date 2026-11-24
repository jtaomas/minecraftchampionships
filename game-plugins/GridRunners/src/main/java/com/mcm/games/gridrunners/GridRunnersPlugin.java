package com.mcm.games.gridrunners;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class GridRunnersPlugin extends JavaPlugin {
    private GridRunnersGame game;

    @Override
    public void onEnable() {
        // Create the world now (server start), never lazily at game start: world creation stalls the main thread.
        if (Arena.ensureWorld() == null) getLogger().severe("Could not create world " + GridRunnersGame.WORLD);
        game = new GridRunnersGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error stopping Grid Runners: " + t);
            }
            game.shutdown();
        }
    }
}
