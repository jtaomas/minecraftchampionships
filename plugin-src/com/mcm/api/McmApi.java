package com.mcm.api;

import org.bukkit.plugin.Plugin;

import java.util.*;

/** Static entry point. Depend on MinecraftMondayCore in your plugin.yml and call these from onEnable. */
public final class McmApi {
    private static final Map<String, McmGame> GAMES = new LinkedHashMap<>();
    private static final Map<String, Plugin> OWNERS = new HashMap<>();
    private static McmVoteProvider voteProvider;
    private static Bridge bridge;
    private static volatile boolean fastTest;

    private McmApi() {}

    /** Implemented by the core. */
    public interface Bridge {
        String displayNameOf(String gameId);
        List<String> builtInGameIds();
        boolean isChampionshipRunning();
        String currentGameId();
    }

    public static void registerGame(Plugin owner, McmGame game) {
        String id = game.id().toLowerCase(Locale.ROOT);
        GAMES.put(id, game);
        OWNERS.put(id, owner);
        owner.getLogger().info("Registered MCM game '" + id + "' (" + game.displayName() + ")");
    }

    public static void unregisterGame(String id) {
        GAMES.remove(id.toLowerCase(Locale.ROOT));
        OWNERS.remove(id.toLowerCase(Locale.ROOT));
    }

    /** Remove every game registered by a plugin (called automatically when it disables). */
    public static void unregisterAll(Plugin owner) {
        OWNERS.entrySet().removeIf(e -> {
            if (e.getValue().equals(owner)) { GAMES.remove(e.getKey()); return true; }
            return false;
        });
    }

    public static McmGame game(String id) { return id == null ? null : GAMES.get(id.toLowerCase(Locale.ROOT)); }

    public static Collection<McmGame> games() { return Collections.unmodifiableCollection(GAMES.values()); }

    public static void setVoteProvider(McmVoteProvider provider) { voteProvider = provider; }

    public static McmVoteProvider voteProvider() { return voteProvider; }

    /** Display name for any game id, built-in or registered. */
    public static String displayName(String id) { return bridge == null ? id : bridge.displayNameOf(id); }

    public static List<String> builtInGameIds() { return bridge == null ? List.of() : bridge.builtInGameIds(); }

    public static boolean isChampionshipRunning() { return bridge != null && bridge.isChampionshipRunning(); }

    /** Id of the game running now, or "lobby". */
    public static String currentGameId() { return bridge == null ? "lobby" : bridge.currentGameId(); }

    /** Internal: set by MinecraftMondayCore. */
    public static void setBridge(Bridge b) { bridge = b; }

    /**
     * True while the admin's fast-test mode is on ({@code /mcm fast on|off}). While true every wait a game
     * controls (pre-game/pre-round countdowns, kit selection, intermissions, results, celebrations, title
     * cards) should be cut to at most 1 second. Gameplay timers (round length) are not shortened.
     * See also {@link Waits}.
     * @since 3.2.0
     */
    public static boolean isFastTest() { return fastTest; }

    /** Internal: set by MinecraftMondayCore ({@code /mcm fast}). */
    public static void setFastTest(boolean on) { fastTest = on; }
}
