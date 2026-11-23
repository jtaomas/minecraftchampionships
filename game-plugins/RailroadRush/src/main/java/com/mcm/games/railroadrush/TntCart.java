package com.mcm.games.railroadrush;

import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.TextDisplay;

/** A scripted TNT cart riding a lane's track. */
final class TntCart {
    Pos prev;
    Pos cur;
    int power;
    boolean warned;
    BlockDisplay body;
    TextDisplay label;

    void remove() {
        if (body != null && body.isValid()) body.remove();
        if (label != null && label.isValid()) label.remove();
    }
}
