package com.mcm.games.acerace.map;

import com.mcm.games.acerace.map.RaceGeo.BoostPad;
import com.mcm.games.acerace.map.RaceGeo.BoostRing;
import com.mcm.games.acerace.map.RaceGeo.Gate;
import com.mcm.games.acerace.map.RaceGeo.Hazard;
import com.mcm.games.acerace.map.RaceGeo.Pos;
import com.mcm.games.acerace.map.RaceGeo.Region;
import com.mcm.games.acerace.map.RaceGeo.Section;
import com.mcm.games.acerace.map.RaceGeo.Spawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Everything the runtime needs from one map. Pure data; checked before it is allowed to run. */
public final class RaceMapData {
    public final String mapId;
    public final long seed;
    public final RaceBlocks blocks;
    public final int laps;
    public final int timeLimitSeconds;
    public final int countdownSeconds;

    public final List<Spawn> startGrid;
    public final Region startStrip;
    public final List<Pos> startBarrier;

    public final List<Section> sections;
    public final Gate finishGate;
    public final boolean loop;
    public final Spawn lapSpawn;

    public final List<BoostPad> pads;
    public final List<BoostRing> rings;
    public final List<Hazard> hazards;

    public final Region playBounds;
    public final List<Spawn> overviews;
    public final Region spectatorBox;
    public final int voidY;
    public final int blockBudget;

    public RaceMapData(String mapId, long seed, RaceBlocks blocks,
                       int laps, int timeLimitSeconds, int countdownSeconds,
                       List<Spawn> startGrid, Region startStrip, List<Pos> startBarrier,
                       List<Section> sections, Gate finishGate, boolean loop, Spawn lapSpawn,
                       List<BoostPad> pads, List<BoostRing> rings, List<Hazard> hazards,
                       Region playBounds, List<Spawn> overviews, Region spectatorBox,
                       int voidY, int blockBudget) {
        this.mapId = mapId;
        this.seed = seed;
        this.blocks = blocks;
        this.laps = laps;
        this.timeLimitSeconds = timeLimitSeconds;
        this.countdownSeconds = countdownSeconds;
        this.startGrid = List.copyOf(startGrid);
        this.startStrip = startStrip;
        this.startBarrier = List.copyOf(startBarrier);
        this.sections = List.copyOf(sections);
        this.finishGate = finishGate;
        this.loop = loop;
        this.lapSpawn = lapSpawn;
        this.pads = List.copyOf(pads);
        this.rings = List.copyOf(rings);
        this.hazards = List.copyOf(hazards);
        this.playBounds = playBounds;
        this.overviews = List.copyOf(overviews);
        this.spectatorBox = spectatorBox;
        this.voidY = voidY;
        this.blockBudget = blockBudget;
    }

    /** Contract errors; empty means playable. Called at registry load and by /acerace validate. */
    public List<String> check() {
        List<String> errors = new ArrayList<>();
        if (mapId == null || mapId.isBlank()) errors.add("map id is blank");
        if (blocks == null || blocks.size() == 0) errors.add("no blocks");
        if (laps < 1 || laps > 4) errors.add("laps must be 1-4");
        if (timeLimitSeconds < 30) errors.add("time limit must be at least 30 s");
        if (countdownSeconds < 1) errors.add("countdown must be at least 1 s");
        if (startGrid.size() < 40) errors.add("start grid has " + startGrid.size() + " slots, need 40");
        if (startStrip == null) errors.add("no start strip");
        if (startBarrier.isEmpty()) errors.add("no start barrier");
        if (sections.size() < 2) errors.add("need at least two sections");
        if (finishGate == null) errors.add("no finish gate");
        if (playBounds == null) errors.add("no play bounds");
        if (overviews.isEmpty()) errors.add("no spectator overview");
        if (spectatorBox == null) errors.add("no spectator box");
        if (blockBudget <= 0) errors.add("block budget must be positive");
        if (blocks != null && blocks.size() > blockBudget) {
            errors.add("block count " + blocks.size() + " exceeds budget " + blockBudget);
        }

        Set<String> unique = new HashSet<>();
        for (Spawn s : startGrid) {
            String k = fmt(s.x()) + "," + fmt(s.y()) + "," + fmt(s.z());
            if (!unique.add(k)) errors.add("duplicate start spawn " + k);
            if (startStrip != null && !startStrip.contains(s.x(), s.y(), s.z())) {
                errors.add("start spawn outside start strip: " + k);
            }
            if (playBounds != null && !playBounds.contains(s.x(), s.y(), s.z())) {
                errors.add("start spawn outside play bounds: " + k);
            }
        }
        for (Section s : sections) {
            if (s.gate() == null) errors.add("section " + s.index() + " has no gate");
            if (s.respawn() == null) errors.add("section " + s.index() + " has no respawn");
            else if (playBounds != null && !playBounds.contains(s.respawn().x(), s.respawn().y(), s.respawn().z())) {
                errors.add("section " + s.index() + " respawn outside play bounds");
            }
        }
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).index() != i) errors.add("section indices must be sequential (found " + sections.get(i).index() + " at " + i + ")");
        }
        if (playBounds != null) {
            for (Spawn s : overviews) {
                if (spectatorBox != null && !spectatorBox.contains(s.x(), s.y(), s.z())) {
                    errors.add("overview outside spectator box: " + fmt(s.x()) + "," + fmt(s.y()) + "," + fmt(s.z()));
                }
            }
        }
        return errors;
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    public int laps() { return laps; }
    public int timeLimitSeconds() { return timeLimitSeconds; }
    public int countdownSeconds() { return countdownSeconds; }
    public RaceBlocks blocks() { return blocks; }
}
