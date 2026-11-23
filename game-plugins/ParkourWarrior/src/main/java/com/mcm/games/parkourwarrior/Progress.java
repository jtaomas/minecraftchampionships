package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import com.mcm.games.parkourwarrior.CourseModel.Kind;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import com.mcm.games.parkourwarrior.CourseModel.Pos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Course graph on top of a {@link CourseModel} (pure Java). A player's checkpoint is a {@link Node}:
 * a hub, "just after obstacle X" or the fork. From a node, {@link #candidates} are the obstacles the player
 * may be attempting; only their pads count and only their kill heights apply, so a respawn is never ahead.
 */
final class Progress {

    enum Type { HUB, AFTER, FORK }

    /** Checkpoint node. {@code stage} for HUB, {@code obstacleId} for AFTER. */
    record Node(Type type, int stage, String obstacleId) {
        static Node hub(int s) { return new Node(Type.HUB, s, null); }
        static Node after(String id) { return new Node(Type.AFTER, 0, id); }
        static final Node FORK = new Node(Type.FORK, 0, null);
    }

    final CourseModel m;
    final int mainCount;

    Progress(CourseModel m) {
        this.m = m;
        this.mainCount = Math.max(1, m.ofKind(Kind.MAIN).size());
    }

    Obstacle main(int s, int i) { return m.obstacle(CourseModel.mainId(s, i)); }
    Obstacle bonus(int s, int i) { return m.obstacle(CourseModel.bonusId(s, i)); }
    Obstacle ending(Difficulty d, int i) { return m.obstacle(CourseModel.endingId(d, i)); }

    int lastStage() {
        int s = 0;
        for (Obstacle o : m.obstacles) if (o.kind() == Kind.MAIN) s = Math.max(s, o.stage());
        return Math.max(1, s);
    }

    int lastIndex(Kind k, int stage, Difficulty d) {
        int n = 0;
        for (Obstacle o : m.obstacles) {
            if (o.kind() != k) continue;
            if (k == Kind.ENDING ? o.ending() == d : o.stage() == stage) n = Math.max(n, o.index());
        }
        return n;
    }

    Node start() { return Node.hub(1); }

    private static void add(java.util.Collection<Obstacle> c, Obstacle o) { if (o != null) c.add(o); }

    /** Node reached by completing {@code o}; null = bonus path done (auto-return) or ending done (finish). */
    Node afterCompletion(Obstacle o) {
        switch (o.kind()) {
            case MAIN -> {
                if (o.index() >= lastIndex(Kind.MAIN, o.stage(), null)) {
                    return o.stage() >= lastStage() ? Node.FORK : Node.hub(o.stage() + 1);
                }
                return Node.after(o.id());
            }
            case BONUS -> { return o.index() >= lastIndex(Kind.BONUS, o.stage(), null) ? null : Node.after(o.id()); }
            default -> { return o.index() >= lastIndex(Kind.ENDING, 0, o.ending()) ? null : Node.after(o.id()); }
        }
    }

    /** Obstacles a player at this node may be attempting (null entries removed). */
    List<Obstacle> candidates(Node n) {
        List<Obstacle> out = new ArrayList<>();
        if (n == null) return out;
        switch (n.type()) {
            case HUB -> { add(out, main(n.stage(), 1)); add(out, bonus(n.stage(), 1)); }
            case FORK -> { for (Difficulty d : Difficulty.values()) add(out, ending(d, 1)); }
            case AFTER -> {
                Obstacle o = m.obstacle(n.obstacleId());
                if (o != null) {
                    switch (o.kind()) {
                        case MAIN -> add(out, main(o.stage(), o.index() + 1));
                        case BONUS -> add(out, bonus(o.stage(), o.index() + 1));
                        case ENDING -> add(out, ending(o.ending(), o.index() + 1));
                    }
                }
            }
        }
        return out;
    }

    /** Pads that count for a player: own node, main-line node, the current stage's bonus start, and (ending phase) every ending start. */
    List<Obstacle> padCandidates(Node node, Node mainNode, boolean switchingAllowed) {
        Set<Obstacle> s = new LinkedHashSet<>(candidates(node));
        if (mainNode != null && !mainNode.equals(node)) s.addAll(candidates(mainNode));
        int st = stageOf(mainNode);
        if (st > 0) add(s, bonus(st, 1));
        if (switchingAllowed && isEndingPhase(mainNode)) for (Difficulty d : Difficulty.values()) add(s, ending(d, 1));
        return new ArrayList<>(s);
    }

    /** Obstacles whose kill heights apply. */
    List<Obstacle> fallCandidates(Node node, Node mainNode) {
        Set<Obstacle> s = new LinkedHashSet<>(candidates(node));
        if (mainNode != null && !mainNode.equals(node)) s.addAll(candidates(mainNode));
        if (isEndingPhase(node)) for (Obstacle o : m.obstacles) if (o.kind() == Kind.ENDING) s.add(o);
        return new ArrayList<>(s);
    }

    /** Stage the node belongs to on the main line (0 in the ending phase). */
    int stageOf(Node n) {
        if (n == null) return 0;
        return switch (n.type()) {
            case HUB -> n.stage();
            case FORK -> 0;
            case AFTER -> {
                Obstacle o = m.obstacle(n.obstacleId());
                yield o == null || o.kind() == Kind.ENDING ? 0 : o.stage();
            }
        };
    }

    boolean isEndingPhase(Node n) {
        if (n == null) return false;
        if (n.type() == Type.FORK) return true;
        if (n.type() == Type.AFTER) {
            Obstacle o = m.obstacle(n.obstacleId());
            return o != null && o.kind() == Kind.ENDING;
        }
        return false;
    }

    boolean isBonus(Node n) {
        if (n == null || n.type() != Type.AFTER) return false;
        Obstacle o = m.obstacle(n.obstacleId());
        return o != null && o.kind() == Kind.BONUS;
    }

    /** Ending difficulty the node is on (null at the fork / elsewhere). */
    Difficulty endingOf(Node n) {
        if (n == null || n.type() != Type.AFTER) return null;
        Obstacle o = m.obstacle(n.obstacleId());
        return o != null && o.kind() == Kind.ENDING ? o.ending() : null;
    }

    /** Respawn position for a node. */
    Pos respawn(Node n) {
        if (n == null) return startSpawn(0);
        switch (n.type()) {
            case HUB -> {
                CourseModel.Hub h = m.hub(n.stage());
                if (h != null && h.respawn() != null) return h.respawn();
                Obstacle o = main(n.stage(), 1);
                if (o != null) return o.entry();
                return startSpawn(0);
            }
            case FORK -> {
                if (m.fork != null) return m.fork;
                Obstacle o = ending(Difficulty.EASY, 1);
                return o != null ? o.entry() : startSpawn(0);
            }
            default -> {
                Obstacle o = m.obstacle(n.obstacleId());
                if (o == null) return startSpawn(0);
                List<Obstacle> next = candidates(n);
                if (!next.isEmpty()) return next.get(0).entry();
                return new Pos(o.pad().centerX(), o.pad().minY() + 1, o.pad().centerZ(), o.entry().yaw());
            }
        }
    }

    Pos startSpawn(int i) {
        if (m.spawns.isEmpty()) {
            CourseModel.Hub h = m.hub(1);
            return h != null ? h.respawn() : new Pos(0.5, 100, 0.5, 0);
        }
        return m.spawns.get(Math.floorMod(i, m.spawns.size()));
    }
}
