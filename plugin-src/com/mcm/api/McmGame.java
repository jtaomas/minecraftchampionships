package com.mcm.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * A minigame that the MinecraftMondayCore tournament can run as one event in its rotation.
 *
 * Register it from your plugin's onEnable with {@link McmApi#registerGame}. The core calls
 * {@link #start} on the main thread with every online player as a participant; the game must
 * call {@link McmGameContext#finish} exactly once when it is over. If an admin skips or stops
 * the event, the core calls {@link #stop} instead, and the game must clean up without calling finish.
 */
public interface McmGame {
    /** Short lowercase id used in /mcm play &lt;id&gt; and config playlists, e.g. "buildmart". */
    String id();

    /** Name shown in titles, the boss bar and the sidebar, e.g. "Build Mart". */
    String displayName();

    /** Icon for the admin GUI. */
    default Material icon() { return Material.COMMAND_BLOCK; }

    /** One-line description for the admin GUI. */
    default String description() { return ""; }

    /** Start the game. Teleport players into your arena and run it. */
    void start(McmGameContext ctx);

    /** Force-stop: cancel tasks, restore the arena. Do NOT call ctx.finish(). The core returns players to the lobby. */
    void stop();

    /**
     * Return true if the core should keep drawing its sidebar and boss bar during this game
     * (fill them via ctx.setSidebarLines / ctx.setTimer). Return false if your game sets its own scoreboard.
     */
    default boolean useCoreScoreboard() { return true; }

    /** A player joined the server mid-game. Default: the core puts them in spectator mode. Return true if you handled it. */
    default boolean onLateJoin(Player player, McmGameContext ctx) { return false; }

    // ---------------------------------------------------------------------------------------------
    // Admin testing hooks (core 3.2.0+). All optional; the defaults mean "not supported".
    // ---------------------------------------------------------------------------------------------

    /**
     * Admin ran {@code /mcm skipwait}: end whatever waiting phase is running right now (countdown, kit select,
     * intermission, results) and go straight to the next phase. Return true if something was skipped.
     */
    default boolean skipWait() { return false; }

    /**
     * Admin ran {@code /mcm skip}: end the current round/map immediately with no points for it and move to the
     * next one (if it was the last one, end the game normally via finish). Return true if handled; games with a
     * single round return false.
     */
    default boolean skipRound() { return false; }

    /** Map/arena ids this game can play, for {@code /mcm map}. Empty = no selectable maps. */
    default java.util.List<String> maps() { return java.util.List.of(); }

    /** Admin ran {@code /mcm map <id>}: the next round uses that map. Return false if unknown/unsupported. */
    default boolean forceNextMap(String id) { return false; }
}
