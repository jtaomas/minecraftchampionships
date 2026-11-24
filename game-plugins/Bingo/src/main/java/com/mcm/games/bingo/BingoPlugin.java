package com.mcm.games.bingo;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BingoPlugin extends JavaPlugin {
    private BingoWorld world;
    private BingoGame bingo;
    private BingoGame lockout;
    private NamespacedKey cardKey, toolKey;

    @Override
    public void onEnable() {
        cardKey = new NamespacedKey(this, "bingo_card");
        toolKey = new NamespacedKey(this, "bingo_tool");
        saveDefaultConfig();
        world = new BingoWorld(this);
        world.settings(loadSettings());
        // A leftover folder from a crash/shutdown is not loaded at this point; clear it out and generate a fresh
        // world now (server start). Games never create worlds: each one uses a new area of this world.
        if (world.prepare() == null) getLogger().severe("Could not generate " + BingoWorld.NAME + "; Bingo games will be skipped.");

        bingo = new BingoGame(this, world, false);
        lockout = new BingoGame(this, world, true);
        Bukkit.getPluginManager().registerEvents(bingo, this);
        Bukkit.getPluginManager().registerEvents(lockout, this);
        McmApi.registerGame(this, bingo);
        McmApi.registerGame(this, lockout);

        PluginCommand top = getCommand("top");
        if (top != null) {
            top.setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (bingo.handleTop(p) || lockout.handleTop(p)) return true;
                p.sendMessage(BingoGame.txt("§c/top only works during a Bingo game."));
                return true;
            });
        }
    }

    /** Re-reads config.yml (each game start). */
    Settings loadSettings() {
        try {
            reloadConfig();
            Settings s = Settings.load(getConfig());
            if (world != null) world.settings(s);
            return s;
        } catch (Throwable t) {
            getLogger().warning("Bad config.yml, using defaults: " + t.getMessage());
            return new Settings();
        }
    }

    @Override
    public void onDisable() {
        if (bingo != null) bingo.shutdown();
        if (lockout != null) lockout.shutdown();
        if (world != null) {
            try { world.unloadNow(); } catch (Throwable t) { getLogger().warning("Unload on disable failed: " + t.getMessage()); }
        }
    }

    public NamespacedKey cardKey() { return cardKey; }

    public NamespacedKey toolKey() { return toolKey; }
}
