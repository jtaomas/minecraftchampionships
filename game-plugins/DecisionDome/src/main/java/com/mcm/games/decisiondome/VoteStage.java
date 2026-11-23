package com.mcm.games.decisiondome;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Random;

/** Where a vote happens: the core's MCC hub venue ({@link HubStage}) or the fallback dome world ({@link DomeArena}). */
interface VoteStage {
    /** Max options this stage can show. */
    int maxOptions();
    /** Set up for these options (pads/labels). Returns false if the stage is unavailable. */
    boolean prepare(List<String> ids);
    World world();
    boolean isStageWorld(World w);
    /** Arrival spot for a voter. */
    Location arrival(Random rng);
    /** Option index under a location or -1. */
    int optionAt(Location l);
    void updateLabel(int idx, String id, int votes, boolean winner);
    /** Called every tick while a vote runs (spinning icons etc.). */
    void tickVisuals(int tick);
    /** Seconds left in the voting phase (-1 = idle). */
    void setCountdown(int seconds);
    /** Spin highlight on/off (fallback stage). */
    void highlight(int idx, boolean on);
    /** Knock an option out (with effects). */
    void eliminate(int idx, String id);
    /** Mark the winner. */
    void winner(int idx, String id, int votes);
    /** Location for effects of option idx. */
    Location optionLocation(int idx);
    /** Centre of the reveal (podium or hub). */
    Location revealLocation();
    /** Remove labels/highlights, restore the stage. */
    void clear();
    /** Below this y a voter in the stage world is brought back. */
    double voidY();
}
