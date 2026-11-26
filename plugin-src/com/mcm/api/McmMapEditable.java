package com.mcm.api;

import org.bukkit.Material;

import java.util.List;
import java.util.function.Consumer;

/**
 * Optional: a game whose maps ops can edit in creative with {@code /mcm mapedit} (Map Edit core). The core does
 * everything generic (permissions, one editor per map, creative/flight, the edit session, the block diff, the
 * overlay file, backups, warnings); the game only builds its map and, when it builds a map for play, applies the
 * saved {@link McmMapOverlay} on top.
 *
 * <p>Register it from onEnable, either by letting your {@link McmGame} implement this interface (discovered by
 * {@link McmApi#registerGame}) or with {@link McmApi#registerMapEditable}. To stay loadable on a core without Map Edit, put the implementation in its own class and register it inside {@code try { ... } catch (Throwable t) { }}.
 * @since the Map Edit core update
 */
public interface McmMapEditable {
    /** Ids of the maps that can be edited. Stable, lowercase, [a-z0-9_-]; used as the overlay file name. */
    List<String> editableMaps();

    default String editableMapName(String mapId) { return mapId; }

    default Material editableMapIcon(String mapId) { return null; }

    /**
     * Non-null = this map cannot be edited right now, with the reason (shown to the op). The core already refuses
     * while any game is running; use this for "not built yet", "map is broken" and the like.
     */
    default String editBlockedReason(String mapId) { return null; }

    /**
     * Build the map exactly as the game would paste it for play, <b>without</b> the saved overlay (the core applies the
     * existing overlay itself and diffs against what you built, so the overlay is always "all changes from the
     * original"), with <b>no game logic running</b>: no countdowns, kills, scoreboards, roofs or barriers that exist
     * only for play. Put it in a region no other editor or game uses (a dedicated edit origin), in the game's own world
     * so its world rules apply. Call {@code done} exactly once on the main thread when every block is placed and the
     * chunks are loaded; {@code done.accept(null)} reports failure (log why).
     * Hold your own chunk tickets only if you need them; the core keeps the edit region's chunks loaded.
     */
    void buildForEdit(String mapId, Consumer<McmEditSpace> done);

    /** The saved overlay of a map changed (saved or reset): drop cached derived data so the next build uses it. */
    default void overlayChanged(String mapId) { }

    /** The edit session ended (after the save finished). Clear the edit region / release what {@link #buildForEdit} acquired. */
    default void editClosed(String mapId, McmEditSpace space) { }

    /**
     * Game-specific warnings about the edited world, computed on save from the live blocks (e.g. "the goal pen has no
     * floor"). The core already checks {@link McmEditSpace#markers}. Main thread.
     */
    default List<String> editWarnings(String mapId, McmEditSpace space) { return List.of(); }
}
