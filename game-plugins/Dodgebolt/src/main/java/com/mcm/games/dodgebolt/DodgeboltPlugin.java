package com.mcm.games.dodgebolt;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class DodgeboltPlugin extends JavaPlugin {
    private DodgeboltGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        game = new DodgeboltGame(this);
        game.prepareWorld();
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) game.stop();
    }
}
