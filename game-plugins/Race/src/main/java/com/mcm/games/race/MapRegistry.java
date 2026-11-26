package com.mcm.games.race;

import com.mcm.games.race.model.TgMap;
import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.stub.StubBridgeMap;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/** Loads the TgMap generators (config class list + jar auto-discovery + optional stub). */
final class MapRegistry {

    /** One loaded map with a fixed world slot. */
    static final class Entry {
        final TgMap map;
        final int slot;
        final boolean stub;
        TgMapData cached;      // fixed-seed build, validated at enable
        List<String> problems = List.of();
        boolean broken;        // build threw or core data missing -> not playable

        Entry(TgMap map, int slot, boolean stub) { this.map = map; this.slot = slot; this.stub = stub; }

        long fixedSeed() { return map.id().hashCode() * 2654435761L; }
    }

    /** Replaced as a whole on the main thread by {@link #install} (maps are built on the worker thread). */
    private volatile List<Entry> entries = new ArrayList<>();
    private volatile boolean ready;
    private final Logger log;

    MapRegistry(Logger log) { this.log = log; }

    /** Worker-safe: instantiate the map classes into a fresh list (no Bukkit calls). */
    List<Entry> scan(List<String> classNames, boolean autoDiscover, File jar, String stubMode, int rounds) {
        List<Entry> entries = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>(classNames);
        if (autoDiscover && jar != null && jar.isFile()) {
            try (JarFile jf = new JarFile(jar)) {
                Enumeration<JarEntry> en = jf.entries();
                List<String> found = new ArrayList<>();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (n.startsWith("com/mcm/games/race/maps/") && n.endsWith(".class") && !n.contains("$"))
                        found.add(n.substring(0, n.length() - 6).replace('/', '.'));
                }
                found.sort(String::compareTo);
                names.addAll(found);
            } catch (Exception e) {
                log.warning("Race: map auto-discovery failed: " + e);
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String cn : names) {
            boolean listed = classNames.contains(cn);
            try {
                Class<?> c = Class.forName(cn, false, getClass().getClassLoader());
                if (!TgMap.class.isAssignableFrom(c) || c.isInterface() || Modifier.isAbstract(c.getModifiers())
                        || !Modifier.isPublic(c.getModifiers())) {
                    if (listed) log.warning("Race: " + cn + " is not a public TgMap class; skipped.");
                    continue;
                }
                TgMap m = (TgMap) c.getDeclaredConstructor().newInstance();
                if (!ids.add(m.id().toLowerCase(Locale.ROOT))) { log.warning("Race: duplicate map id " + m.id() + " (" + cn + "); skipped."); continue; }
                entries.add(new Entry(m, entries.size(), false));
            } catch (ClassNotFoundException e) {
                if (listed) log.warning("Race: map class " + cn + " not found; skipped.");
            } catch (NoSuchMethodException e) {
                if (listed) log.warning("Race: map class " + cn + " has no public no-arg constructor; skipped.");
            } catch (Throwable t) {
                log.warning("Race: could not load map " + cn + ": " + t);
            }
        }
        String sm = stubMode == null ? "auto" : stubMode.toLowerCase(Locale.ROOT);
        if (sm.equals("always") || (sm.equals("auto") && entries.size() < rounds)) {
            entries.add(new Entry(new StubBridgeMap(), entries.size(), true));
        }
        return entries;
    }

    /**
     * Worker-safe: append maps imported from community worlds ({@code plugins/McmRace/imported/<id>/}, see
     * {@link com.mcm.games.race.imported.ImportedMaps}). No folder = nothing added. Ids already used are skipped.
     */
    void addImported(List<Entry> entries, File dir) {
        if (dir == null || !dir.isDirectory()) return;
        Set<String> ids = new LinkedHashSet<>();
        for (Entry e : entries) ids.add(e.map.id().toLowerCase(Locale.ROOT));
        for (com.mcm.games.race.imported.ImportedMap m : com.mcm.games.race.imported.ImportedMaps.loadAll(dir, log)) {
            if (!ids.add(m.id())) { log.warning("Race: imported map id " + m.id() + " is already used; skipped."); continue; }
            entries.add(new Entry(m, entries.size(), false));
        }
    }

    /** Main thread: make a scanned + validated list the live one. */
    void install(List<Entry> list) { entries = list; ready = true; }

    boolean ready() { return ready; }

    /** Worker-safe: build every map once with its fixed seed and run the contract check (pure Java). */
    void validate(List<Entry> entries) {
        for (Entry e : entries) {
            long t0 = System.nanoTime();
            try {
                TgMapData d = Overlays.apply(e.map.id(), e.map.build(e.fixedSeed()));   // Map Edit overlay on top (core /mcm mapedit)
                e.cached = d;
                e.problems = d.check(e.map.kit());
                // Imported community builds keep their concrete: only bridging kits hand out (and break) concrete.
                if (e.map instanceof com.mcm.games.race.imported.ImportedMap && !e.map.kit().bridging())
                    e.problems.removeIf(p -> p.startsWith("map uses dyed concrete"));
                for (int[] dp : e.map.decoyChickens())
                    if (dp == null || dp.length < 3) e.problems.add("decoy chicken entry is not {x,y,z}");
                    else if (d.playBounds != null && !d.playBounds.contains(dp[0] + 0.5, dp[1], dp[2] + 0.5))
                        e.problems.add("decoy chicken outside playBounds at " + dp[0] + " " + dp[1] + " " + dp[2]);
                if (e.map.lethalFallHeight() < 0) e.problems.add("lethalFallHeight < 0 (treated as off)");
                e.broken = d.startSpawns.isEmpty() || d.chickenSpawns.isEmpty() || d.pen == null
                        || d.playBounds == null || d.startStrip == null;
                long ms = (System.nanoTime() - t0) / 1_000_000;
                log.info("Race map " + e.map.id() + " (" + e.map.displayName() + ", " + e.map.type() + "): "
                        + d.blocks.size() + " blocks, " + d.variants.size() + " variant groups, built in " + ms + " ms"
                        + (e.broken ? " - NOT PLAYABLE" : ""));
                for (String p : e.problems) log.warning("Race map " + e.map.id() + ": " + p);
            } catch (Throwable t) {
                e.broken = true;
                log.severe("Race map " + e.map.id() + " failed to build: " + t);
            }
        }
    }

    List<Entry> all() { return entries; }

    List<Entry> playable() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (!e.broken) out.add(e);
        return out;
    }

    Entry byId(String id) {
        for (Entry e : entries) if (e.map.id().equalsIgnoreCase(id)) return e;
        return null;
    }

    List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Entry e : entries) out.add(e.map.id());
        return out;
    }
}
