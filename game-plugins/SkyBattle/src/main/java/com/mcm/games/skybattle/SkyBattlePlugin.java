package com.mcm.games.skybattle;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyBattlePlugin extends JavaPlugin {
    private SkyBattleGame game;

    @Override
    public void onEnable() {
        game = new SkyBattleGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
        getServer().getScheduler().runTask(this, () -> {
            try { game.prepareWorld(); } catch (Throwable t) { getLogger().warning("Could not prepare Sky Battle world: " + t); }
        });
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Sky Battle: " + t); }
        }
        McmApi.unregisterAll(this);
    }
}
