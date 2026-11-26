package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One player's run (kept after a quit so a rejoin resumes and the score split still counts them). */
final class Run {
    enum Status { ACTIVE, FINISHED, OUT }

    final UUID id;
    final String name;
    final String sideKey;
    final int spawnIndex;
    Status status = Status.ACTIVE;
    boolean online = true;

    Progress.Node node;       // current checkpoint
    Progress.Node mainNode;   // last main-line / ending-phase checkpoint (scroll + auto-return target)
    final Set<String> completed = new HashSet<>();
    int coins;                // coins earned this game (shown on the HUD)
    Difficulty ending;        // ending finished (null = none)
    int falls;
    int cp;                   // linear (replica) course: last checkpoint reached (0 = start)
    long finishTick = -1;

    // transient
    long noFallUntilTick;
    long lastMessageMs;
    long lastItemUseMs;
    boolean returning;
    double[] lastPos;        // previous tick's position in course coordinates (moving-trap sweep)

    Run(UUID id, String name, String sideKey, int spawnIndex, Progress.Node start) {
        this.id = id;
        this.name = name;
        this.sideKey = sideKey;
        this.spawnIndex = spawnIndex;
        this.node = start;
        this.mainNode = start;
    }

}
