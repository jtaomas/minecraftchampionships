package com.mcm.games.tgttos;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.Set;

/**
 * No player collision + red karma glow. The core gives every player a fresh scoreboard each second, so the two
 * teams are (re)registered on each viewer's current scoreboard every few ticks. Collision is decided client-side
 * from the viewer's own scoreboard, so this must live on every viewer's board.
 */
final class ScoreTeams {
    static final String NORMAL = "tg_nc", RED = "tg_karma";

    private ScoreTeams() {}

    static void sync(Collection<? extends Player> viewers, Collection<String> entries, Set<String> red) {
        for (Player v : viewers) {
            Scoreboard sb = v.getScoreboard();
            if (sb == null) continue;
            Team n = team(sb, NORMAL, null);
            Team r = team(sb, RED, ChatColor.RED);
            for (String e : entries) {
                Team want = red.contains(e) ? r : n;
                if (!want.hasEntry(e)) want.addEntry(e);
            }
        }
    }

    private static Team team(Scoreboard sb, String name, ChatColor color) {
        Team t = sb.getTeam(name);
        if (t == null) {
            t = sb.registerNewTeam(name);
            t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            t.setCanSeeFriendlyInvisibles(false);
            t.setAllowFriendlyFire(true);
            if (color != null) t.setColor(color);
        }
        return t;
    }

    static void clear(Collection<? extends Player> viewers) {
        for (Player v : viewers) {
            Scoreboard sb = v.getScoreboard();
            if (sb == null) continue;
            for (String n : new String[]{NORMAL, RED}) {
                Team t = sb.getTeam(n);
                if (t != null) try { t.unregister(); } catch (IllegalStateException ignored) {}
            }
        }
    }
}
