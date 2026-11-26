package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Designer intent recorded by a map generator for the offline validators: the SAFE and FAST lines (legs of
 * target boxes inside corridors), landmark points for the visibility ray-cast, the lane (route envelope) used
 * for the scenery share, regions excluded from the free-sprint check, and the staircase flights.
 */
public final class Design {
    /** One leg of a line: reach any node inside {@code target}, moving only inside the corridor boxes. */
    public record Leg(String name, Region target, List<Region> corridor) {}

    public static final class Line {
        public final String name;
        public final boolean bridge;          // may place blocks
        public final boolean fast;            // fast (expert) movement parameters
        public final List<Leg> legs = new ArrayList<>();
        public String description = "";
        /** Cautious lines skip jumps with a wider edge-to-edge gap than this. */
        public double maxGap = 4.0;

        public Line(String name, boolean bridge, boolean fast) { this.name = name; this.bridge = bridge; this.fast = fast; }

        public Line leg(String name, Region target, Region... corridor) {
            legs.add(new Leg(name, target, List.of(corridor)));
            return this;
        }
    }

    public final List<Line> lines = new ArrayList<>();
    public final List<Pos> landmark = new ArrayList<>();
    /** Route envelope: blocks inside are "course", outside are "scenery". */
    public final List<Region> lane = new ArrayList<>();
    /** Excluded from the map-wide free-sprint check (start strip and pen are always excluded). */
    public final List<Region> sprintExempt = new ArrayList<>();
    public final List<Stairs.Flight> flights = new ArrayList<>();
    /** Corridor for the lazy straight bridging line (bridging maps). */
    public Region lazyCorridor;
    /** Optional smaller build box for the lazy-line search only (long maps: keeps the search inside its node budget). */
    public Region lazyBuildBox;
    /** Search box for map-wide reachability and the sprint check. */
    public Region searchBox;
    /** Bridging allowed for this map's kit. */
    public boolean bridgingKit;
    /** Respawn below this y (copied from data). */
    public int voidY;
    /** Wide brawl spots (crowd next to a drop), reported by {@link ExtraChecks}. */
    public final List<Region> brawl = new ArrayList<>();
    /** Report void crossings per line ({@link ExtraChecks}). */
    public boolean voidReport;
    /** Minimum void crossings per line when {@link #voidReport} is on. */
    public int minVoidCrossings = 0;
    /**
     * Unintended-hole scan ({@link ExtraChecks}): every cell a racer can walk to (no bridging, falls allowed) must not
     * stand next to a column that is empty down to the void, unless that column lies in one of {@link #voidByDesign}.
     */
    public boolean holeScan;
    /** FAST must beat SAFE by at least this share (expert). Feedback 3: a wall-to-wall finish row lets a side line finish at its own end. */
    public double minFastSave = 0.20;
    /** Longest allowed expert time of the best line (s); Bookshelf Run is longer on purpose (owner: "if not longer"). */
    public double maxExpert = 35.5;
    /** Columns where an open drop to the void is intended (map edges, deliberate gaps). */
    public final List<Region> voidByDesign = new ArrayList<>();

    public Line line(String name, boolean bridge, boolean fast) {
        Line l = new Line(name, bridge, fast);
        lines.add(l);
        return l;
    }
}
