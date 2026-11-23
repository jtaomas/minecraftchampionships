package com.mcm.games.meltdown;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

final class AttributeHelper {
    private AttributeHelper() {}

    static double maxHealth(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return a == null ? 20.0 : a.getValue();
    }

    static void resetHealth(Player p) {
        p.setHealth(Math.max(1.0, maxHealth(p)));
    }
}
