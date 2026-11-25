package com.mcm.games.tgttos.imported;

import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapFeatures;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;

import java.util.List;

/**
 * A map imported from a community world save (see {@link ImportedMaps}). The block data and metadata live in the
 * server's data folder ({@code plugins/McmRace/imported/<id>/}), never in the jar. Built once when loaded; every
 * {@link #build} call returns the same, already validated data (imported maps have no seed-dependent layout).
 */
public final class ImportedMap implements TgMap {
    final String id, name;
    final MapType type;
    final Kit kit;
    final String credit;
    final int lethalFall, roundSeconds;
    final boolean rotation;
    final List<int[]> decoys;
    final MapFeatures features;
    final TgMapData data;
    final String folder;

    ImportedMap(String id, String name, MapType type, Kit kit, String credit, int lethalFall, int roundSeconds, boolean rotation,
                List<int[]> decoys, MapFeatures features, TgMapData data, String folder) {
        this.id = id; this.name = name; this.type = type; this.kit = kit; this.credit = credit;
        this.lethalFall = lethalFall; this.roundSeconds = roundSeconds; this.rotation = rotation;
        this.decoys = decoys; this.features = features; this.data = data; this.folder = folder;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public MapType type() { return type; }
    @Override public Kit kit() { return kit; }
    @Override public TgMapData build(long seed) { return data; }
    @Override public int lethalFallHeight() { return lethalFall; }
    @Override public List<int[]> decoyChickens() { return decoys; }
    /** Imported maps never get scattered decoys (owner: no decoys, no fake endings). */
    @Override public boolean scatterDecoys() { return false; }
    @Override public MapFeatures features() { return features; }
    @Override public int roundSeconds() { return roundSeconds; }
    @Override public String credit() { return credit; }
    @Override public boolean inRotation() { return rotation; }

    /** Source folder (for /race info). */
    public String folder() { return folder; }
}
