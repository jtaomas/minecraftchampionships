package com.mcm.games.railroadrush;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One side's private canyon course. */
final class Lane {
    final String sideKey;
    final int index;
    final int ox;
    final Material concrete;
    final int goldMult;
    final String label;
    final List<UUID> members = new ArrayList<>();

    int gold;
    int doorsOpened;
    int nextCartSec;

    /** Result of the last track walk. */
    List<Pos> chain = new ArrayList<>();
    int rawMaxZ;
    int rawProgress;
    int reach;
    int reachIndex;

    final List<TntCart> carts = new ArrayList<>();

    Lane(String sideKey, int index, int ox, Material concrete, int goldMult, String label) {
        this.sideKey = sideKey;
        this.index = index;
        this.ox = ox;
        this.concrete = concrete;
        this.goldMult = goldMult;
        this.label = label;
    }
}
