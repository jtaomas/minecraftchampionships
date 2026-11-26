package com.mcm.games.hitw;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class HoleInTheWallPlugin extends JavaPlugin {
    private HoleInTheWallGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        game = new HoleInTheWallGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
        try {
            McmApi.registerMapEditable(this, game.id(), new HitwEdit(this, game, game.arena()));   // core /mcm mapedit (Map Edit core; older cores skip it)
        } catch (Throwable t) {
            getLogger().info("Map Edit is not available on this core (" + t.getClass().getSimpleName() + ").");
        }
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping game: " + t); }
        }
        McmApi.unregisterGame("hitw");
    }
}
