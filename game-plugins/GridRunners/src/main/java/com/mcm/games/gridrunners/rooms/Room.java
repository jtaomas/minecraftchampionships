package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import com.mcm.games.gridrunners.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One challenge room. Local coordinates: walls at 0 and 15, interior 1..14, floor at ly=0,
 * interior air ly=1..8, glass ceiling at ly=9. Doors are 4 wide (6..9) and 3 tall (1..3) in the middle of each wall.
 */
public abstract class Room {
    public enum State { LOCKED, OPEN, ACTIVE, DONE, SKIPPED }

    public final GridRunnersGame game;
    public final Lane lane;
    public final RoomType type;
    public final int col, row, index;
    public final String id;
    protected final World world;
    protected final int ox, oy, oz;
    protected final Random rng;

    public State state = State.LOCKED;
    public int activeSeconds;
    public boolean hinted;
    protected final Set<UUID> entities = new LinkedHashSet<>();

    protected Room(GridRunnersGame game, Lane lane, RoomType type, int col, int row, long seed) {
        this.game = game;
        this.lane = lane;
        this.type = type;
        this.col = col;
        this.row = row;
        this.index = row * 3 + col;
        this.id = lane.index + ":" + index;
        this.world = game.world();
        this.ox = lane.x0 + 15 * col;
        this.oz = lane.z0 + 15 * row;
        this.oy = GridRunnersGame.Y0;
        // Same seed for the same room slot in every lane -> identical layouts.
        this.rng = new Random(seed * 31L + index * 7919L + type.ordinal());
    }

    // ---- description ----
    public abstract String name();
    public abstract String objective();
    public abstract String hintText();
    public String progress() { return ""; }

    public String title() { return index < 8 ? "Room " + (index + 1) + ": " + name() : name(); }

    public Material floorAt(int lx, int lz) { return Material.SMOOTH_STONE; }

    // ---- lifecycle hooks ----
    public void buildContent() {}
    public void onActivate() {}
    public void secondTick() {}
    public void fastTick() {}
    public void onPlayerFast(Player p) {}
    public void onHint() {}
    public void hintTick() {}
    public void onInteractBlock(Player p, Block b, PlayerInteractEvent e) {}
    public void onInteractEntity(Player p, Entity en) {}
    public void onEntityDeath(LivingEntity en) {}
    public void onProtectedLethal(LivingEntity en) {}
    protected void onCleanup() {}

    /** Where escaped entities are put back. */
    protected Location returnSpot() { return loc(2.5, 1, 2.5); }

    protected void complete() { game.roomCompleted(this, false); }

    public void cleanup() {
        for (UUID u : entities) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) e.remove();
        }
        entities.clear();
        onCleanup();
    }

    // ---- helpers ----
    public static boolean isDoorStrip(int lx, int lz) {
        boolean midX = lx >= 6 && lx <= 9;
        boolean midZ = lz >= 6 && lz <= 9;
        return (midX && (lz <= 2 || lz >= 13)) || (midZ && (lx <= 2 || lx >= 13));
    }

    protected Block block(int lx, int ly, int lz) { return world.getBlockAt(ox + lx, oy + ly, oz + lz); }

    private static boolean placeable(int lx, int ly, int lz) {
        if (ly >= 1 && ly <= 3 && isDoorStrip(lx, lz)) return false;
        return lx >= 1 && lx <= 14 && lz >= 1 && lz <= 14 && ly >= 0 && ly <= 8;
    }

    /** Place content, never blocking a doorway approach. */
    protected void put(int lx, int ly, int lz, Material m) {
        if (placeable(lx, ly, lz)) block(lx, ly, lz).setType(m, false);
    }

    protected void put(int lx, int ly, int lz, BlockData d) {
        if (placeable(lx, ly, lz)) block(lx, ly, lz).setBlockData(d, false);
    }

    protected boolean is(Block b, int lx, int ly, int lz) {
        return b.getX() == ox + lx && b.getY() == oy + ly && b.getZ() == oz + lz;
    }

    protected Location loc(double lx, double ly, double lz) { return new Location(world, ox + lx, oy + ly, oz + lz); }

    public int lx(Location l) { return l.getBlockX() - ox; }
    public int ly(Location l) { return l.getBlockY() - oy; }
    public int lz(Location l) { return l.getBlockZ() - oz; }

    public boolean contains(Location l) {
        if (l == null || l.getWorld() != world) return false;
        double dx = l.getX() - ox, dy = l.getY() - oy, dz = l.getZ() - oz;
        return dx >= 1 && dx < 15 && dz >= 1 && dz < 15 && dy >= -0.5 && dy < 9;
    }

    protected <T extends Entity> T spawn(Location l, Class<T> cls, Consumer<T> cfg) {
        T ent = world.spawn(l, cls, e -> {
            game.tag(e, id);
            e.setPersistent(true);
            if (e instanceof LivingEntity le) le.setRemoveWhenFarAway(false);
            if (e instanceof Mob m) m.setCanPickupItems(false);
            if (cfg != null) cfg.accept(e);
        });
        entities.add(ent.getUniqueId());
        return ent;
    }

    protected static Entity entity(UUID u) { return u == null ? null : Bukkit.getEntity(u); }

    protected static boolean alive(UUID u) {
        Entity e = entity(u);
        return e != null && e.isValid() && !e.isDead();
    }

    /** Remove dead/despawned/unloaded entities from the set; returns the new size. */
    protected int prune(Set<UUID> set) {
        set.removeIf(u -> !alive(u));
        return set.size();
    }

    /** Teleport tracked entities that wandered out of the room back in. */
    protected void keepInside(Collection<UUID> ids) {
        for (UUID u : ids) {
            Entity e = entity(u);
            if (e != null && e.isValid() && !contains(e.getLocation())) {
                e.teleport(returnSpot());
            }
        }
    }

    public List<Player> playersInside() {
        List<Player> out = new ArrayList<>();
        for (Player p : game.online(lane)) if (contains(p.getLocation())) out.add(p);
        return out;
    }

    protected void tellInside(String s) {
        for (Player p : playersInside()) Msg.send(p, s);
    }

    protected void soundInside(Sound s, float vol, float pitch) {
        for (Player p : playersInside()) p.playSound(p.getLocation(), s, vol, pitch);
    }

    protected void barInside(String s) {
        for (Player p : playersInside()) Msg.bar(p, s);
    }

    protected void marker(double lx, double ly, double lz) {
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc(lx, ly, lz), 6, 0.25, 0.25, 0.25, 0);
    }
}
