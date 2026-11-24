package com.mcm.games.acerace.map;

import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

/**
 * Pure geometry and data types for the Ace Race map contract. No Bukkit world access here:
 * a map builds these values off-thread and the runtime pastes them.
 */
public final class RaceGeo {
    private RaceGeo() {}

    public record Spawn(double x, double y, double z, float yaw) {}

    public record Pos(int x, int y, int z) {}

    public record ItemSpec(String material, int amount, String name,
                           List<String> enchants, boolean unbreakable, int slot) {}

    public record EffectSpec(String type, int durationTicks, int amplifier) {}

    public record Loadout(boolean clearInventory, List<ItemSpec> items, List<EffectSpec> effects) {
        public static final Loadout EMPTY = new Loadout(false, List.of(), List.of());
    }

    public enum PadKind { SPEED, JUMP, LAUNCH, RIPTIDE }

    public record BoostPad(int id, Region trigger, PadKind kind,
                           double dx, double dy, double dz, double speed,
                           int cooldownTicks, Loadout grant) {}

    public record BoostRing(int id, double x, double y, double z,
                            double nx, double ny, double nz,
                            double radius, double strength,
                            boolean requireGlide, int cooldownTicks) {}

    public record Hazard(Set<String> blockTypes, List<Region> regions, int killY, String message) {}

    /** Inclusive axis-aligned box in local map coordinates. */
    public static final class Region {
        public final double minX, minY, minZ, maxX, maxY, maxZ;

        public Region(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.minX = Math.min(x1, x2);
            this.minY = Math.min(y1, y2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxY = Math.max(y1, y2);
            this.maxZ = Math.max(z1, z2);
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public boolean contains(Vector v) {
            return contains(v.getX(), v.getY(), v.getZ());
        }

        public Vector center() {
            return new Vector((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        }
    }

    public enum Dir { EAST, WEST, NORTH, SOUTH, UP, DOWN }

    /**
     * A directional checkpoint gate. The player must cross the entry plane in the gate's forward
     * direction inside the box. This catches fast crossings and rejects side/back entries.
     */
    public record Gate(Region box, Dir dir) {
        public boolean crossed(Vector prev, Vector cur) {
            if (prev == null || cur == null) return false;
            double px = prev.getX(), py = prev.getY(), pz = prev.getZ();
            double cx = cur.getX(), cy = cur.getY(), cz = cur.getZ();
            double plane, t;
            switch (dir) {
                case EAST -> {
                    plane = box.minX;
                    if (px > plane || cx < plane) return false;
                    if (cx == px) return false;
                    t = (plane - px) / (cx - px);
                }
                case WEST -> {
                    plane = box.maxX;
                    if (px < plane || cx > plane) return false;
                    if (cx == px) return false;
                    t = (plane - px) / (cx - px);
                }
                case NORTH -> {
                    plane = box.maxZ;
                    if (pz < plane || cz > plane) return false;
                    if (cz == pz) return false;
                    t = (plane - pz) / (cz - pz);
                }
                case SOUTH -> {
                    plane = box.minZ;
                    if (pz > plane || cz < plane) return false;
                    if (cz == pz) return false;
                    t = (plane - pz) / (cz - pz);
                }
                case UP -> {
                    plane = box.minY;
                    if (py > plane || cy < plane) return false;
                    if (cy == py) return false;
                    t = (plane - py) / (cy - py);
                }
                case DOWN -> {
                    plane = box.maxY;
                    if (py < plane || cy > plane) return false;
                    if (cy == py) return false;
                    t = (plane - py) / (cy - py);
                }
                default -> {
                    return false;
                }
            }
            if (t < 0 || t > 1) return false;
            double ix = px + t * (cx - px);
            double iy = py + t * (cy - py);
            double iz = pz + t * (cz - pz);
            return box.contains(ix, iy, iz);
        }
    }

    /** One ordered course section: its entry gate, respawn, loadout and local kill plane. */
    public record Section(int index, String name, Gate gate, Spawn respawn,
                          Loadout onEnter, Loadout onRespawn, int killY,
                          List<Integer> padIds, List<Integer> ringIds) {}
}
