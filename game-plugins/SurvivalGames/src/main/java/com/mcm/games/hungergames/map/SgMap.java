package com.mcm.games.hungergames.map;

/**
 * Contract for a procedural Survival Games map.
 */
public interface SgMap {
    String id();

    String displayName();

    boolean boatRecommended();

    int radius();

    void generate(SgCanvas canvas);
}
