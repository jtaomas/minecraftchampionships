package com.mcm.api;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

/**
 * The MCC hub (world {@code mcc_lobby}) and its Decision Dome venue, built by MinecraftMondayCore when
 * {@code mcc-hub.enabled: true}. The hub is the lobby for MCC (external, McmApi-registered) games only; the
 * Minecraft Monday lobby (the server's default world) is unchanged.
 *
 * <p>{@link #venue()} is null until the hub has been built and loaded. All methods: main thread only.</p>
 */
public final class McmLobby {
    private McmLobby() {}

    /** The dome venue inside the MCC hub. Locations are copies; safe to modify. */
    public interface Venue {
        /** The hub world ({@code mcc_lobby}). */
        World world();
        /** Hub spawn (arrival plaza). */
        Location hubSpawn();
        /** Outside the dome doors, facing in. */
        Location entrance();
        /** Where voters arrive: on the stage between the podium and the pad ring, facing the podium. */
        Location interiorSpawn();
        /** Centre of the dome (stage floor level + 1). */
        Location domeCenter();
        /** Number of voting pads (12). */
        int padCount();
        /** Standing centre of pad i (on top of the pad). */
        Location padCenter(int i);
        /** Horizontal radius around {@link #padCenter} that counts as standing on the pad. */
        double padRadius();
        /**
         * Recolour pad i's accent blocks (concrete, glass, wool, runway stripe) to {@code color}; {@code lit}
         * switches its light core on/off. {@code color == null} = neutral dark pad (unused/eliminated).
         */
        void setPad(int i, DyeColor color, boolean lit);
        /** Restore every pad to its built (rainbow, lit) look. */
        void resetPads();
        /** Top centre of the gold reveal podium (standing position). */
        Location revealPodium();
        /** Where an interior countdown/timer text display should float (above the podium). */
        Location interiorTimer();
        /** Bottom-left block of the facade countdown digits (digits run toward +X, face +Z). */
        Location countdownOrigin();
        /** Draw MM:SS on the facade countdown display; negative = "--:--". */
        void showCountdown(int seconds);
        /** Spectator spots in the stands (for ceremonies). */
        List<Location> standSpots();

        // -------------------------------------------------------------------------------------
        // Venue v2 (Decision Dome rebuild). Defaults keep every v1 venue working.
        // -------------------------------------------------------------------------------------

        /** 1 = original pads, 2 = dial + pods + dividers + reveal. */
        default int apiVersion() { return 1; }

        /** Number of live wedges: 8 (45 degrees) or 6 (60 degrees). */
        default int dialCapacity() { return padCount(); }

        /** Reconfigure the dial to 8 or 6 games. No-op in v1 venues. */
        default void setDial(int games) { }

        /** Centre of wedge i (the floor area where chickens gather). */
        default Location wedgeCenter(int i) { return padCenter(i); }

        /** Outer radius of a wedge. */
        default double wedgeRadius() { return padRadius(); }

        /** Recolour/re-light a wedge; the v1 implementation forwards to the pad. */
        default void setWedge(int i, DyeColor color, boolean lit) { setPad(i, color, lit); }

        /** Number of team pods. Ten in the reference; solos share/are grouped by the provider. */
        default int podCount() { return 10; }

        /** Spawn point inside pod i, facing the dial. */
        default Location podSpawn(int i) { return interiorSpawn(); }

        /** Open/close a pod gate. */
        default void setPodGate(int i, boolean open) { }

        /** Optional visual pod state: "normal", "caged", "dunked", "frozen", "invaded". */
        default void setPodState(int i, String state) { }

        /** 0 = flush floor seam, 1 = fully raised dividers. */
        default void setDividers(double progress) { }

        /** Clear per-vote labels, overlays and divider/pod state; keep the permanent build. */
        default void clearRuntime() { resetPads(); }

        /** Reveal the winning wedge (label, light, podium surface). */
        default void revealWedge(int i, String gameId) { }

        /** Bumped whenever the permanent venue geometry changes; the provider can invalidate caches. */
        default int dialGeneration() { return 0; }

        /** Reveal surface index 0..3 (cube faces/screens), or the podium for v1. */
        default Location revealSurface(int face) { return revealPodium(); }

        /** Interior clock/timer anchor (above the reveal podium). */
        default Location interiorClock() { return interiorTimer(); }
    }

    /** Implemented by the core. */
    public interface Bridge {
        Venue venue();
        boolean isMccEvent();
    }

    private static Bridge bridge;

    /** The dome venue, or null when the hub is disabled / not built yet. */
    public static Venue venue() {
        try { return bridge == null ? null : bridge.venue(); } catch (Throwable t) { return null; }
    }

    public static boolean isAvailable() { return venue() != null; }

    /**
     * True when the running (or just finished) event belongs to the MCC side: an MCC championship
     * ({@code /mcm start mcc}) or a single MCC game. Votes and lobby returns for these use the hub.
     */
    public static boolean isMccEvent() { return bridge != null && bridge.isMccEvent(); }

    /** Internal: set by MinecraftMondayCore. */
    public static void setBridge(Bridge b) { bridge = b; }
}
