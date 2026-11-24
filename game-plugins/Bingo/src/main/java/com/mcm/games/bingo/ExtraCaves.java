package com.mcm.games.bingo;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * Extra worm caves for mcc_bingo (owner: "more caves"). Vanilla cave density lives in the noise settings, which only a
 * datapack can change, so this world populator digs additional tunnels after vanilla decoration.
 *
 * <p>Deterministic from the world seed and chunk position: each chunk may start one tunnel system (chance
 * {@code chance}); a share of them start above ground and dig down (cave entrances). Every chunk replays the tunnels
 * that start within {@link #REACH} chunks and carves only the blocks inside itself, so results agree across chunk
 * borders whatever order chunks generate in. Only natural ground blocks are replaced, never logs, leaves, structure
 * blocks, ores or geodes, and a sphere is skipped when water or lava is next to it.
 * Runs on world-gen threads; holds no state.
 */
final class ExtraCaves extends BlockPopulator {
    private static final int MAX_LEN = 150;
    private static final int REACH = (MAX_LEN + 8) / 16 + 1;

    private static final Set<Material> CARVABLE = EnumSet.of(
            Material.STONE, Material.DEEPSLATE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.TUFF,
            Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.GRASS_BLOCK, Material.PODZOL,
            Material.MYCELIUM, Material.SNOW_BLOCK, Material.MUD, Material.CLAY, Material.DRIPSTONE_BLOCK,
            Material.MOSS_BLOCK, Material.SANDSTONE, Material.RED_SANDSTONE, Material.TERRACOTTA,
            Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.RED_TERRACOTTA,
            Material.BROWN_TERRACOTTA, Material.LIGHT_GRAY_TERRACOTTA, Material.PACKED_ICE);
    private static final Set<Material> LIQUID = EnumSet.of(Material.WATER, Material.LAVA, Material.SEAGRASS,
            Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT, Material.BUBBLE_COLUMN);

    private final double chance;
    private final double entranceShare;

    ExtraCaves(double chance, double entranceShare) {
        this.chance = Math.max(0, Math.min(0.5, chance));
        this.entranceShare = Math.max(0, Math.min(1, entranceShare));
    }

    @Override
    public void populate(WorldInfo info, Random unused, int chunkX, int chunkZ, LimitedRegion region) {
        if (chance <= 0) return;
        long seed = info.getSeed();
        int minX = chunkX << 4, minZ = chunkZ << 4;
        int minY = Math.max(info.getMinHeight() + 6, -52);
        for (int ox = -REACH; ox <= REACH; ox++) {
            for (int oz = -REACH; oz <= REACH; oz++) {
                int sx = chunkX + ox, sz = chunkZ + oz;
                Random r = new Random(seed ^ (sx * 341873128712L) ^ (sz * 132897987541L) ^ 0x5DEECE66DL);
                if (r.nextDouble() >= chance) continue;
                worm(r, (sx << 4) + r.nextInt(16), (sz << 4) + r.nextInt(16), region, minX, minZ, minY);
            }
        }
    }

    private void worm(Random r, double x, double z, LimitedRegion region, int minX, int minZ, int minY) {
        boolean entrance = r.nextDouble() < entranceShare;
        double y = entrance ? 72 + r.nextInt(24) : -36 + r.nextInt(76);
        double yaw = r.nextDouble() * Math.PI * 2;
        double pitch = entrance ? -0.6 : (r.nextDouble() - 0.5) * 0.4;
        int len = 80 + r.nextInt(MAX_LEN - 80);
        double baseR = entrance ? 1.8 + r.nextDouble() * 1.0 : 1.6 + r.nextDouble() * 1.8;
        int branchAt = r.nextDouble() < 0.5 ? len / 3 + r.nextInt(len / 3) : -1;
        double bx = 0, by = 0, bz = 0, byaw = 0;
        for (int i = 0; i < len; i++) {
            double rad = baseR * (0.65 + 0.55 * Math.sin(Math.PI * (i + 1) / (len + 1)));
            x += Math.cos(yaw) * Math.cos(pitch);
            z += Math.sin(yaw) * Math.cos(pitch);
            y += Math.sin(pitch);
            if (entrance && i < 45) pitch = pitch * 0.9 + (-0.55) * 0.1 + (r.nextDouble() - 0.5) * 0.08;
            else pitch = pitch * 0.85 + (r.nextDouble() - 0.5) * 0.18;
            yaw += (r.nextDouble() - 0.5) * 0.35;
            if (y < minY + 2) pitch = Math.abs(pitch) + 0.05;
            if (!(entrance && i < 60) && y > 40) pitch = -Math.abs(pitch) - 0.05;
            if (i == branchAt) { bx = x; by = y; bz = z; byaw = yaw + (r.nextBoolean() ? 1.2 : -1.2); }
            if (x + rad + 1 >= minX && x - rad - 1 < minX + 16 && z + rad + 1 >= minZ && z - rad - 1 < minZ + 16) {
                sphere(region, x, y, z, rad, minX, minZ, minY);
            }
        }
        if (branchAt >= 0) {
            // A short side tunnel; uses its own random stream derived from the parent so replay stays identical.
            Random br = new Random(r.nextLong());
            double yw = byaw, p = (br.nextDouble() - 0.5) * 0.3;
            int blen = 30 + br.nextInt(40);
            for (int i = 0; i < blen; i++) {
                double rad = (1.4 + 0.4 * br.nextDouble()) * (0.6 + 0.5 * Math.sin(Math.PI * (i + 1) / (blen + 1)));
                bx += Math.cos(yw) * Math.cos(p);
                bz += Math.sin(yw) * Math.cos(p);
                by += Math.sin(p);
                p = p * 0.85 + (br.nextDouble() - 0.5) * 0.18;
                yw += (br.nextDouble() - 0.5) * 0.35;
                if (by < minY + 2) p = Math.abs(p) + 0.05;
                if (by > 40) p = -Math.abs(p) - 0.05;
                if (bx + rad + 1 >= minX && bx - rad - 1 < minX + 16 && bz + rad + 1 >= minZ && bz - rad - 1 < minZ + 16) {
                    sphere(region, bx, by, bz, rad, minX, minZ, minY);
                }
            }
        }
    }

    private static void sphere(LimitedRegion region, double cx, double cy, double cz, double rad, int minX, int minZ, int minY) {
        double ry = rad * 0.8;
        int x0 = Math.max(minX, (int) Math.floor(cx - rad)), x1 = Math.min(minX + 15, (int) Math.ceil(cx + rad));
        int z0 = Math.max(minZ, (int) Math.floor(cz - rad)), z1 = Math.min(minZ + 15, (int) Math.ceil(cz + rad));
        int y0 = Math.max(minY, (int) Math.floor(cy - ry)), y1 = Math.min(250, (int) Math.ceil(cy + ry));
        if (x0 > x1 || z0 > z1 || y0 > y1) return;
        // Liquid check one block around the carved part.
        for (int x = x0 - 1; x <= x1 + 1; x++)
            for (int z = z0 - 1; z <= z1 + 1; z++)
                for (int y = y0 - 1; y <= y1 + 1; y++) {
                    if (!region.isInRegion(x, y, z)) continue;
                    if (LIQUID.contains(region.getType(x, y, z))) return;
                }
        for (int x = x0; x <= x1; x++) {
            double dx = (x + 0.5 - cx) / rad;
            for (int z = z0; z <= z1; z++) {
                double dz = (z + 0.5 - cz) / rad;
                double h = dx * dx + dz * dz;
                if (h >= 1) continue;
                for (int y = y0; y <= y1; y++) {
                    double dy = (y + 0.5 - cy) / ry;
                    if (h + dy * dy >= 1) continue;
                    if (!region.isInRegion(x, y, z)) continue;
                    if (CARVABLE.contains(region.getType(x, y, z))) region.setType(x, y, z, y > 50 ? Material.AIR : Material.CAVE_AIR);
                }
            }
        }
    }
}
