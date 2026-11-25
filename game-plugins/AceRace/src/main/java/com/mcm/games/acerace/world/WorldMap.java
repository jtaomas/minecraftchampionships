package com.mcm.games.acerace.world;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One Ace Race course that lives in an already imported world. The course keeps its own blocks; this file only says
 * where the pen, gate and finish are and carries the map's command-block pad language as a per-player rule table.
 */
public final class WorldMap {
    /** Inclusive block box. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(double x, double y, double z) {
            int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
            return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
        }
    }

    /** kind: effect | uneffect | item | clear | sound | checkpoint | kill. */
    public record Action(String kind, String arg, int n1, int n2) { }

    /** "If the block at the player's position plus this offset is {@code block}, run the actions on that player." */
    public record Rule(Set<String> banks, double dx, double dy, double dz, Material block, List<Action> actions) { }

    public final String id, name, worldName;
    public final int laps, timeLimit, countdown, voidY, bankRange, riptide, unbreaking;
    public final boolean trident;
    public final double startX, startY, startZ;
    public final float startYaw;
    public final Box pen, gate, finish;
    public final List<Box> open = new ArrayList<>();
    public final List<Box> waypoints = new ArrayList<>();
    public final Map<String, int[]> banks = new LinkedHashMap<>();
    public final List<Rule> rules = new ArrayList<>();
    public final List<Action> always = new ArrayList<>();
    public final Material hoop;
    /** Flight rings that boost a gliding player forward: x, y, z, radius. */
    public final List<double[]> boostRings = new ArrayList<>();
    public final double boostStrength;
    /** Boxes that put an elytra on whoever is inside (rings and launch columns), independent of the block underfoot. */
    public final List<Box> elytraZones = new ArrayList<>();
    /**
     * Arrow pads: every {@code arrowFrom} block in {@code arrowArea} is swapped for {@code arrowTo} (the pack draws it as
     * a green arrow) while a race runs, facing along {@link #route} ("route") or keeping its own facing ("keep").
     * Rules written for {@code arrowFrom} keep working on the swapped blocks.
     */
    public final Material arrowFrom, arrowTo;
    public final boolean arrowKeepFacing;
    public final int[] arrowArea;
    /** The racing line as x, z points in order; arrow pads point along its nearest segment. */
    public final List<double[]> route = new ArrayList<>();
    public final List<String> problems = new ArrayList<>();

    public WorldMap(String id, YamlConfiguration c) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.name = c.getString("name", id);
        this.worldName = c.getString("world", "");
        this.laps = Math.max(1, c.getInt("laps", 1));
        this.timeLimit = Math.max(60, c.getInt("time-limit", 600));
        this.countdown = Math.max(1, c.getInt("countdown", 10));
        this.voidY = c.getInt("void-y", -64);
        this.bankRange = c.getInt("bank-range", 160);
        this.trident = c.getBoolean("trident.give", true);
        this.riptide = c.getInt("trident.riptide", 1);
        this.unbreaking = c.getInt("trident.unbreaking", 3);
        List<Double> s = c.getDoubleList("start");
        if (s.size() < 3) problems.add("start needs x, y, z[, yaw]");
        this.startX = s.size() > 0 ? s.get(0) : 0;
        this.startY = s.size() > 1 ? s.get(1) : 0;
        this.startZ = s.size() > 2 ? s.get(2) : 0;
        this.startYaw = s.size() > 3 ? s.get(3).floatValue() : 0f;
        this.pen = box(c.getIntegerList("pen"), "pen");
        this.gate = box(c.getIntegerList("gate"), "gate");
        this.finish = box(c.getIntegerList("finish"), "finish");
        for (Object o : c.getList("open", List.of())) open.add(box(ints(o), "open"));
        for (Object o : c.getList("waypoints", List.of())) waypoints.add(box(ints(o), "waypoint"));
        ConfigurationSection b = c.getConfigurationSection("banks");
        if (b != null) for (String k : b.getKeys(false)) {
            List<Integer> xz = b.getIntegerList(k);
            if (xz.size() == 2) banks.put(k, new int[]{xz.get(0), xz.get(1)});
        }
        for (Object o : c.getList("boost-rings", List.of())) {
            if (o instanceof List<?> l && l.size() == 4) {
                double[] r = new double[4];
                for (int i = 0; i < 4; i++) r[i] = ((Number) l.get(i)).doubleValue();
                boostRings.add(r);
            } else problems.add("boost-rings entries need x, y, z, radius");
        }
        this.boostStrength = c.getDouble("boost-strength", 2.0);
        for (Object o : c.getList("elytra-zones", List.of())) elytraZones.add(box(ints(o), "elytra-zone"));
        ConfigurationSection ap = c.getConfigurationSection("arrow-pads");
        if (ap != null) {
            this.arrowFrom = Material.matchMaterial(ap.getString("from", "yellow_glazed_terracotta"));
            this.arrowTo = Material.matchMaterial(ap.getString("to", "purple_glazed_terracotta"));
            this.arrowKeepFacing = ap.getString("facing", "route").equalsIgnoreCase("keep");
            List<Integer> a = ap.getIntegerList("area");
            this.arrowArea = a.size() == 6 ? new int[]{Math.min(a.get(0), a.get(3)), Math.min(a.get(1), a.get(4)), Math.min(a.get(2), a.get(5)),
                    Math.max(a.get(0), a.get(3)), Math.max(a.get(1), a.get(4)), Math.max(a.get(2), a.get(5))} : null;
            if (arrowFrom == null || arrowTo == null || arrowArea == null) problems.add("arrow-pads needs from, to and a six-number area");
        } else {
            this.arrowFrom = null; this.arrowTo = null; this.arrowKeepFacing = true; this.arrowArea = null;
        }
        for (Object o : c.getList("route", List.of())) {
            if (o instanceof List<?> l && l.size() == 2) route.add(new double[]{((Number) l.get(0)).doubleValue(), ((Number) l.get(1)).doubleValue()});
            else problems.add("route points need x, z");
        }
        if (arrowFrom != null && !arrowKeepFacing && route.size() < 2) problems.add("arrow-pads facing: route needs a route of two or more points");
        String h = c.getString("hoop", "");
        this.hoop = h.isBlank() ? null : Material.matchMaterial(h);
        for (String line : c.getStringList("always")) {
            Action a = action(line);
            if (a != null) always.add(a);
        }
        for (String line : c.getStringList("rules")) rule(line);
        if (worldName.isBlank()) problems.add("no world");
    }

    private static List<Integer> ints(Object o) {
        List<Integer> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object v : l) if (v instanceof Number n) out.add(n.intValue());
        return out;
    }

    private Box box(List<Integer> v, String what) {
        if (v.size() != 6) {
            problems.add(what + " needs six numbers");
            return new Box(0, 0, 0, 0, 0, 0);
        }
        return new Box(Math.min(v.get(0), v.get(3)), Math.min(v.get(1), v.get(4)), Math.min(v.get(2), v.get(5)),
                Math.max(v.get(0), v.get(3)), Math.max(v.get(1), v.get(4)), Math.max(v.get(2), v.get(5)));
    }

    /** "banks | dx dy dz | block | action ; action". */
    private void rule(String line) {
        String[] f = line.split("\\|");
        if (f.length != 4) { problems.add("bad rule: " + line); return; }
        Set<String> bankSet = null;
        String bs = f[0].trim();
        if (!bs.equals("*")) {
            bankSet = new HashSet<>();
            for (String k : bs.split(",")) {
                String key = k.trim();
                if (!banks.containsKey(key)) problems.add("rule names unknown bank " + key);
                bankSet.add(key);
            }
        }
        String[] off = f[1].trim().split("\\s+");
        Material m = Material.matchMaterial(f[2].trim());
        if (off.length != 3 || m == null) { problems.add("bad rule: " + line); return; }
        List<Action> acts = new ArrayList<>();
        for (String a : f[3].split(";")) {
            Action act = action(a);
            if (act != null) acts.add(act);
        }
        try {
            rules.add(new Rule(bankSet, Double.parseDouble(off[0]), Double.parseDouble(off[1]), Double.parseDouble(off[2]), m, acts));
        } catch (NumberFormatException e) {
            problems.add("bad rule offset: " + line);
        }
    }

    private Action action(String text) {
        String[] p = text.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return null;
        try {
            switch (p[0]) {
                case "effect": {
                    int seconds = p[2].startsWith("inf") ? -1 : Integer.parseInt(p[2]);
                    return new Action("effect", p[1], seconds, Integer.parseInt(p[3]));
                }
                case "uneffect": case "clear": case "sound": return new Action(p[0], p[1], 0, 0);
                case "item": return new Action("item", p[1] + " " + p[2], 0, 0);
                case "checkpoint": case "kill": return new Action(p[0], "", 0, 0);
                case "launch": {   // launch <up> <forward>: velocity, no jumping needed
                    Double.parseDouble(p[1]); Double.parseDouble(p[2]);
                    return new Action("launch", p[1] + " " + p[2], 0, 0);
                }
                default: problems.add("unknown action: " + text); return null;
            }
        } catch (RuntimeException e) {
            problems.add("bad action: " + text);
            return null;
        }
    }
}
