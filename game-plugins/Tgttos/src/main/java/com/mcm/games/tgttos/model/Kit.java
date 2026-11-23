package com.mcm.games.tgttos.model;

/**
 * Starting kit of a map. The runtime builds the items (counts are config defaults, listed in the README) and
 * refills them on every respawn.
 * <ul>
 *   <li>{@link #CONCRETE_PICKAXE_SNOWBALLS}: 64 team-colour concrete + iron pickaxe + 5 snowballs, SURVIVAL
 *       (may place concrete; may break only player-placed concrete).</li>
 *   <li>{@link #SNOWBALLS_ONLY}: 5 snowballs, ADVENTURE (no placing/breaking).</li>
 *   <li>{@link #BOAT}: player starts seated in a boat at a {@link TgMapData#boatSpawns} point, ADVENTURE,
 *       plus 5 snowballs. Leaving the boat is allowed; a respawn puts the player back in a fresh boat.</li>
 *   <li>{@link #ELYTRA_FIREWORKS}: elytra worn + 2 firework rockets (config), ADVENTURE.</li>
 * </ul>
 * Extra per-map items go in {@link TgMapData#extraItems}.
 */
public enum Kit {
    CONCRETE_PICKAXE_SNOWBALLS, SNOWBALLS_ONLY, BOAT, ELYTRA_FIREWORKS;

    public boolean bridging() { return this == CONCRETE_PICKAXE_SNOWBALLS; }
}
