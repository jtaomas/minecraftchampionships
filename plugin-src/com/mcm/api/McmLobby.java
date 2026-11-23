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
