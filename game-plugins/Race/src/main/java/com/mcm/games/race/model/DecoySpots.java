package com.mcm.games.race.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wandering decoy chickens (owner, playtest 2): when a map's {@link TgMap#decoyChickens()} is empty, the runtime
 * scatters decoys over the later part of the map with this picker (pure Java, deterministic per seed, also used by
 * the offline validators). A spot is a feet block with air at feet and head height on a solid, non-hazard block,
 * on the walkable level nearest the pen floor (never a roof), with at least three walkable neighbours (no pillar tops), inside playBounds, outside every hazard box and the start
 * strip, at least 4 blocks outside the goal pen, nearer the pen than the start (horizontal distance to the pen at most
 * 55% of start-to-pen, vertical within 18 of the pen floor), and at least 6 from the other decoys.
 */
public final class DecoySpots {
    private DecoySpots() {}

    public static final int DEFAULT_COUNT = 6;

    /** The decoy spots the runtime uses for this map and build: the map's own list, else {@link #pick}. */
    public static List<int[]> effective(TgMap m, TgMapData d) {
        List<int[]> l = m.decoyChickens();
        if (l != null && !l.isEmpty()) return l;
        return m.scatterDecoys() ? pick(d, DEFAULT_COUNT, d.seed) : List.of();
    }

    public static List<int[]> pick(TgMapData d, int n, long seed) {
        List<int[]> out = new ArrayList<>();
        if (d.pen == null || d.playBounds == null || d.startSpawns.isEmpty()) return out;
        double sx = 0, sz = 0;
        for (TgMapData.Spawn s : d.startSpawns) { sx += s.x(); sz += s.z(); }
        sx /= d.startSpawns.size(); sz /= d.startSpawns.size();
        double px = d.pen.centerX(), pz = d.pen.centerZ();
        double dist = Math.hypot(px - sx, pz - sz), reach = Math.max(18, 0.55 * dist);
        TgMapData.Region b = d.playBounds;
        int yTop = Math.min(b.maxY() - 2, d.pen.minY() + 18), yLow = Math.max(b.minY() + 1, d.pen.minY() - 18);
        Random r = new Random(seed ^ 0x5DEC0115L);
        for (int tries = 0; tries < 8000 && out.size() < n; tries++) {
            double a = r.nextDouble() * 2 * Math.PI, rr = Math.sqrt(r.nextDouble()) * reach;
            int x = (int) Math.floor(px + Math.cos(a) * rr), z = (int) Math.floor(pz + Math.sin(a) * rr);
            if (x <= b.minX() || x >= b.maxX() || z <= b.minZ() || z >= b.maxZ()) continue;
            if (x >= d.pen.minX() - 4 && x <= d.pen.maxX() + 4 && z >= d.pen.minZ() - 4 && z <= d.pen.maxZ() + 4) continue;
            int found = Integer.MIN_VALUE;   // the walkable level nearest the pen floor (not a roof above it)
            for (int y = yTop; y >= yLow; y--)
                if (walkable(d, x, y, z) && (found == Integer.MIN_VALUE || Math.abs(y - d.pen.minY()) < Math.abs(found - d.pen.minY()))) found = y;
            if (found == Integer.MIN_VALUE) continue;
            int y = found;
            int nb = 0;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) if (walkable(d, x + o[0], y, z + o[1])) nb++;
            if (nb < 3) continue;
            boolean bad = d.startStrip != null && d.startStrip.contains(x + 0.5, y + 0.5, z + 0.5);
            for (TgMapData.Region h : d.hazardRegions) if (h.contains(x + 0.5, y + 0.5, z + 0.5) || h.contains(x + 0.5, y - 0.5, z + 0.5)) { bad = true; break; }
            for (int[] o : out) if (Math.abs(o[0] - x) + Math.abs(o[2] - z) < 6) bad = true;
            if (!bad) out.add(new int[]{x, y, z});
        }
        return out;
    }

    /** Feet cell (x, y, z): air at feet and head, standable solid non-hazard block below. */
    public static boolean walkable(TgMapData d, int x, int y, int z) {
        if (d.blocks.get(x, y, z) != null || d.blocks.get(x, y + 1, z) != null) return false;
        String under = d.blocks.get(x, y - 1, z);
        if (under == null) return false;
        String id = under.startsWith("minecraft:") ? under.substring(10) : under;
        int i = id.indexOf('['); if (i >= 0) id = id.substring(0, i);
        for (String h : d.hazardBlocks) {
            String hid = h.startsWith("minecraft:") ? h.substring(10) : h;
            int j = hid.indexOf('['); if (j >= 0) hid = hid.substring(0, j);
            if (hid.equals(id)) return false;
        }
        String[] no = {"water", "lava", "fire", "magma", "campfire", "cactus", "fence", "wall", "pane", "bars", "carpet", "snow", "ladder",
                "vine", "torch", "lantern", "chain", "rail", "button", "plate", "sign", "banner", "flower", "grass", "fern", "cobweb", "scaffolding",
                "ice", "glass", "slime", "powder", "trapdoor", "door", "sapling", "rose", "tulip", "poppy", "dandelion", "daisy", "cornflower", "petals", "head"};
        if (id.equals("grass_block") || id.equals("snow_block")) return true;
        for (String k : no) if (id.contains(k)) return false;   // ice: never in a boat lane
        return true;
    }
}
