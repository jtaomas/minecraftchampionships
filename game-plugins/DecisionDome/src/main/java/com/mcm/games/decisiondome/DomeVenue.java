package com.mcm.games.decisiondome;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The port between the vote runtime and the physical dome. Implemented by {@link HubDialView} for
 * the core's v2 venue and {@link FallbackDialView} for the plugin-owned {@code mcc_dome} world.
 *
 * <p>The plugin never hardcodes a dome coordinate: it asks for the dial geometry, pod spawns and
 * wedge centres, and it asks the venue to perform every runtime block mutation.</p>
 */
interface DomeVenue {
    String id();

    int apiVersion();

    World world();

    boolean available();

    /** Local dial geometry (centre = 0,0 for wedge maths; floorY is an absolute world Y). */
    DialGeometry geometry();

    /** Switch the venue to an n-wedge dial before the geometry is read (wedge 0 may sit elsewhere for 6 vs 8). */
    default void configure(int capacity) { }

    /** Absolute centre of the dial at floor level. */
    Location center();

    int capacity();

    int podCount();

    /** Reconcile the physical dial with this session's board. Returns false if the venue vanished. */
    boolean prepare(Dial dial);

    Location podSpawn(int pod);

    Location wedgeCenter(int slot);

    /** Convert a local dial point to a world location at {@code floorY + dy}. */
    Location at(double x, double z, double dy);

    default Location at(double x, double z) { return at(x, z, 0.05); }

    void setWedge(int slot, Wedge.State state, String gameId, int colorIndex);

    /** Show the game name; when {@code showCount} is true append the tally line beneath it. */
    void updateLabel(int slot, String gameId, int count, boolean showCount);

    void setDividers(double progress);

    void setPodGate(int pod, boolean open);

    void setPodState(int pod, String state);

    void setPodColor(int pod, int colorIndex);

    void clearRuntime();

    void reveal(int slot, String gameId, VoteTally tally);

    Location revealLocation();

    void showCountdown(int seconds);

    void tick(int totalTick);

    boolean isVenueWorld(World w);
}
