#!/usr/bin/env python3
"""
make_test_dump.py - generate sample MCMDUMP files for testing render_preview.py.

    python make_test_dump.py [out.mcmdump.gz]            # colourful floating island (default)
    python make_test_dump.py big.mcmdump.gz --big        # ~400x200x400 stress-test arena
"""
import argparse
import gzip
import math
import time

import numpy as np

COLOURS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
           "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")


class Vox:
    def __init__(self, sx, sy, sz):
        self.g = np.zeros((sx, sy, sz), np.int16)
        self.pal = ["air"]
        self.idx = {"air": 0}

    def id(self, name):
        if name not in self.idx:
            self.idx[name] = len(self.pal)
            self.pal.append(name)
        return self.idx[name]

    def set(self, x, y, z, name):
        if 0 <= x < self.g.shape[0] and 0 <= y < self.g.shape[1] and 0 <= z < self.g.shape[2]:
            self.g[x, y, z] = self.id(name)

    def get(self, x, y, z):
        return self.pal[self.g[x, y, z]]

    def write(self, path):
        xs, ys, zs = np.nonzero(self.g)
        ids = self.g[xs, ys, zs]
        pal = [n if ":" in n else "minecraft:" + n for n in self.pal]
        with gzip.open(path, "wt", compresslevel=5, newline="\n") as f:
            f.write("MCMDUMP 1 %d %d %d\n" % self.g.shape)
            step = 500_000
            for i in range(0, len(xs), step):
                f.write("".join(f"{x} {y} {z} {pal[b]}\n" for x, y, z, b in
                                zip(xs[i:i + step].tolist(), ys[i:i + step].tolist(),
                                    zs[i:i + step].tolist(), ids[i:i + step].tolist())))
        return len(xs)


def noise2(x, z, seed=0):
    return (np.sin(x * 0.21 + seed) * np.cos(z * 0.17 - seed * 1.3) +
            0.5 * np.sin(x * 0.53 + z * 0.41 + seed * 2.1) + 0.25 * np.cos(x * 1.1 - z * 0.9 + seed))


def island(v):
    sx, sy, sz = v.g.shape
    cx, cz, top = sx // 2, sz // 2, 52
    rng = np.random.default_rng(7)
    X, Z = np.meshgrid(np.arange(sx), np.arange(sz), indexing="ij")
    ang = np.arctan2(Z - cz, X - cx)
    R = 44 + 4 * np.sin(ang * 3 + 0.5) + 2.5 * np.sin(ang * 7)
    r = np.hypot(X - cx, Z - cz)
    inside = r < R
    hgt = np.round(top + 1.6 * noise2(X * 0.45, Z * 0.45, 1) + 3 * np.clip(1 - r / 25, 0, 1)).astype(int)
    edge = np.clip(R - r, 0, None)
    depth = np.round(edge * (0.85 + 0.25 * noise2(X, Z, 4)) + 2).astype(int)
    for x in range(sx):
        for z in range(sz):
            if not inside[x, z]:
                continue
            h, d = hgt[x, z], depth[x, z]
            beach = edge[x, z] < 3.5
            for y in range(max(0, h - d), h + 1):
                k = h - y
                if k == 0:
                    name = "sand" if beach else "grass_block"
                elif k <= 3:
                    name = "sand" if beach else "dirt"
                else:
                    rr = rng.random()
                    if y < h - d + 3:
                        name = "mossy_cobblestone" if rr < 0.5 else "cobblestone"
                    elif rr < 0.03:
                        name = "coal_ore"
                    elif rr < 0.045:
                        name = "iron_ore"
                    elif rr < 0.10:
                        name = "andesite"
                    elif rr < 0.14:
                        name = "granite"
                    elif y < top - 20:
                        name = "deepslate" if rr < 0.7 else "tuff"
                    else:
                        name = "stone"
                v.set(x, y, z, name)
            # hanging moss / roots below
            if d > 10 and rng.random() < 0.03:
                for y in range(h - d - 1, h - d - 1 - rng.integers(2, 6), -1):
                    v.set(x, y, z, "hanging_roots")
    return hgt, inside, edge


def surface(v, hgt, x, z):
    return int(hgt[x, z])


def pond(v, hgt, cx, cz, rad):
    sx, _, sz = v.g.shape
    lvl = int(hgt[cx, cz])
    for x in range(cx - rad - 2, cx + rad + 3):
        for z in range(cz - rad - 2, cz + rad + 3):
            r = math.hypot(x - cx, z - cz)
            if r < rad:
                dep = 1 + int(3 * (1 - r / rad))
                for y in range(lvl - dep, lvl + 3):
                    v.set(x, y, z, "air")
                v.set(x, lvl - dep - 1, z, "sand" if r > rad * 0.5 else "clay")
                for y in range(lvl - dep, lvl):
                    v.set(x, y, z, "water")
                if (x * 7 + z * 3) % 17 == 0:
                    v.set(x, lvl, z, "lily_pad")
            elif r < rad + 1.5:
                for y in range(lvl, lvl + 4):
                    v.set(x, y, z, "air")
                v.set(x, lvl - 1, z, "sand")
    return lvl


def waterfall(v, hgt, edge, x0, z0, dx, dz, lvl):
    """channel from (x0,z0) towards the rim, then a falling column."""
    sx, sy, sz = v.g.shape
    x, z = x0, z0
    while 0 <= x < sx and 0 <= z < sz and edge[x, z] > 0.5:
        for w in (-1, 0, 1):
            xx, zz = (x + w * dz, z + w * dx)
            for y in range(lvl, lvl + 4):
                v.set(xx, y, zz, "air")
            v.set(xx, lvl - 1, zz, "water")
            v.set(xx, lvl - 2, zz, "stone")
        x, z = x + dx, z + dz
    for y in range(lvl - 1, max(0, lvl - 34), -1):
        for w in (-1, 0, 1):
            v.set(x + w * dz, y, z + w * dx, "water")


def tower_round(v, cx, cz, base, rad, height, wall_fn, window, roof):
    for y in range(base, base + height):
        for x in range(cx - rad - 1, cx + rad + 2):
            for z in range(cz - rad - 1, cz + rad + 2):
                r = math.hypot(x - cx, z - cz)
                if r <= rad + 0.5:
                    if r > rad - 0.7:
                        ang = math.atan2(z - cz, x - cx)
                        win = (y - base) % 6 in (2, 3) and int((ang + math.pi) / (math.pi / 4)) % 2 == 0
                        v.set(x, y, z, window if win and y > base + 1 else wall_fn(y - base, x, z))
                    else:
                        v.set(x, y, z, "air" if (y - base) % 6 else "spruce_planks")
    top = base + height
    roof(v, cx, cz, top, rad)


def dome(v, cx, cz, y0, rad, name, rim="glowstone"):
    for x in range(cx - rad - 1, cx + rad + 2):
        for z in range(cz - rad - 1, cz + rad + 2):
            r = math.hypot(x - cx, z - cz)
            if r <= rad + 0.5:
                v.set(x, y0, z, rim if r > rad - 0.7 else "smooth_quartz")
            for y in range(y0 + 1, y0 + rad + 2):
                d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + (y - y0) ** 2)
                if rad - 0.5 <= d <= rad + 0.5:
                    v.set(x, y, z, name)
    v.set(cx, y0 + 1, cz, "sea_lantern")


def cone(v, cx, cz, y0, rad, name, tip="gold_block"):
    h = rad * 2 + 2
    for y in range(y0, y0 + h):
        rr = (rad + 1) * (1 - (y - y0) / h)
        for x in range(cx - rad - 2, cx + rad + 3):
            for z in range(cz - rad - 2, cz + rad + 3):
                if math.hypot(x - cx, z - cz) <= rr + 0.3:
                    v.set(x, y, z, name)
    v.set(cx, y0 + h, cz, tip)
    v.set(cx, y0 + h + 1, cz, "end_rod")


def tower_square(v, cx, cz, base, half, height):
    for y in range(base, base + height):
        for x in range(cx - half, cx + half + 1):
            for z in range(cz - half, cz + half + 1):
                edge_x, edge_z = abs(x - cx) == half, abs(z - cz) == half
                if edge_x and edge_z:
                    v.set(x, y, z, "quartz_pillar[axis=y]")
                elif edge_x or edge_z:
                    win = (y - base) % 5 in (2, 3) and (x + z) % 3 == 0
                    band = (y - base) % 5 == 0
                    v.set(x, y, z, "light_blue_stained_glass_pane[east=true]" if win
                          else "chiseled_quartz_block" if band else "quartz_block")
                else:
                    v.set(x, y, z, "air" if (y - base) % 5 else "birch_planks")
    # copper pyramid roof with stairs edges (block states on purpose)
    y = base + height
    for k in range(half + 2):
        for x in range(cx - half - 1 + k, cx + half + 2 - k):
            for z in range(cz - half - 1 + k, cz + half + 2 - k):
                ring = max(abs(x - cx), abs(z - cz)) == half + 1 - k
                stage = ["copper_block", "exposed_copper", "weathered_copper", "oxidized_copper"][min(3, k // 2)]
                v.set(x, y + k, z, f"minecraft:waxed_{stage}" if not ring else
                      f"minecraft:{'' if stage == 'copper_block' else stage.replace('_copper', '_') }cut_copper_stairs[facing=north,half=bottom]")
    v.set(cx, y + half + 2, cz, "sea_lantern")
    v.set(cx, y + half + 3, cz, "lightning_rod")


def ruin(v, cx, cz, base):
    rng = np.random.default_rng(3)
    for y in range(base, base + 16):
        for x in range(cx - 4, cx + 5):
            for z in range(cz - 4, cz + 5):
                if max(abs(x - cx), abs(z - cz)) == 4:
                    if y - base > 6 + 6 * math.sin(x * 0.9 + z * 1.3) + 3:
                        continue
                    rr = rng.random()
                    v.set(x, y, z, "cracked_polished_blackstone_bricks" if rr < 0.25 else
                          "gilded_blackstone" if rr < 0.29 else "crying_obsidian" if rr < 0.33
                          else "polished_blackstone_bricks")
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            v.set(x, base, z, "magma_block" if (x + z) % 4 == 0 else "blackstone")
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            v.set(x, base, z, "lava")
    v.set(cx + 3, base + 1, cz + 3, "soul_lantern")
    v.set(cx - 3, base + 1, cz + 3, "shroomlight")
    v.set(cx + 3, base + 1, cz - 3, "lantern[hanging=false]")


def tree(v, x, z, y, kind="oak", height=6):
    for k in range(height):
        v.set(x, y + k, z, f"{kind}_log[axis=y]")
    top = y + height
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(-2, 3):
                d = math.sqrt(dx * dx + dz * dz + (dy * 1.4) ** 2)
                if d <= 2.9 and (dx or dz or dy > 0 or dy < -1):
                    if v.get(x + dx, top + dy, z + dz) == "air":
                        v.set(x + dx, top + dy, z + dz, f"{kind}_leaves[persistent=true]")


def make_island(path):
    v = Vox(128, 100, 128)
    hgt, inside, edge = island(v)
    rng = np.random.default_rng(11)
    lvl = pond(v, hgt, 78, 62, 9)
    waterfall(v, hgt, edge, 78, 71, 0, 1, lvl)

    def ring(t, x, z):
        return f"{COLOURS[(t // 2) % 16]}_concrete"
    tower_round(v, 44, 42, surface(v, hgt, 44, 42) + 1, 5, 30, ring, "light_blue_stained_glass",
                lambda vv, cx, cz, y, r: dome(vv, cx, cz, y, r, "magenta_stained_glass"))

    tc = ["white_terracotta", "orange_terracotta", "yellow_terracotta", "red_terracotta", "brown_terracotta"]
    tower_round(v, 40, 84, surface(v, hgt, 40, 84) + 1, 4, 20, lambda t, x, z: tc[(t // 3) % len(tc)],
                "glass", lambda vv, cx, cz, y, r: cone(vv, cx, cz, y, r, "red_glazed_terracotta"))

    tower_square(v, 88, 36, surface(v, hgt, 88, 36) + 1, 5, 24)
    ruin(v, 84, 90, surface(v, hgt, 84, 90))

    # wool + powder plaza with a beacon
    px, pz = 60, 60
    py = surface(v, hgt, px, pz)
    for x in range(px - 6, px + 7):
        for z in range(pz - 6, pz + 7):
            for y in range(py + 1, py + 6):
                v.set(x, y, z, "air")
            c = COLOURS[(abs(x - px) + abs(z - pz)) % 16]
            v.set(x, py, z, f"{c}_wool" if (x + z) % 2 else f"{c}_concrete_powder")
            for y in range(py - 3, py):
                if v.get(x, y, z) == "air":
                    v.set(x, y, z, "stone_bricks")
    v.set(px, py + 1, pz, "beacon")
    for c in (-6, 6):
        for k in range(-6, 7):
            v.set(px + c, py + 1, pz + k, "stone_brick_wall[up=true]")
            v.set(px + k, py + 1, pz + c, "stone_brick_wall[up=true]")
    for cx_, cz_ in ((-6, -6), (-6, 6), (6, -6), (6, 6)):
        v.set(px + cx_, py + 2, pz + cz_, "ochre_froglight" if cx_ < 0 else "pearlescent_froglight")
    v.set(px + 3, py + 1, pz - 3, "mymod:ruby_block")          # unknown modded id -> hash/keyword
    v.set(px - 3, py + 1, pz + 3, "mymod:crimson_crate")       # keyword "crimson"
    v.set(px + 3, py + 1, pz + 3, "amethyst_block")
    v.set(px - 3, py + 1, pz - 3, "diamond_block")

    # trees
    for (x, z, kind) in ((30, 60, "oak"), (52, 30, "birch"), (68, 96, "cherry"), (98, 60, "oak"),
                         (24, 74, "spruce"), (62, 80, "dark_oak"), (100, 76, "jungle")):
        y = surface(v, hgt, x, z)
        if v.get(x, y, z) == "grass_block":
            tree(v, x, z, y + 1, kind, height=int(rng.integers(5, 8)))

    # flowers / grass
    flowers = ["poppy", "dandelion", "cornflower", "allium", "oxeye_daisy", "short_grass", "short_grass",
               "short_grass", "red_tulip", "azure_bluet", "fern"]
    for _ in range(600):
        x, z = int(rng.integers(0, 128)), int(rng.integers(0, 128))
        if not inside[x, z]:
            continue
        y = int(hgt[x, z])
        if v.get(x, y, z) == "grass_block" and v.get(x, y + 1, z) == "air":
            v.set(x, y + 1, z, flowers[int(rng.integers(len(flowers)))])

    # gravel / path from plaza to towers
    for (tx, tz) in ((44, 42), (40, 84), (88, 36), (84, 90)):
        n = int(max(abs(tx - px), abs(tz - pz)))
        for i in range(n):
            x = round(px + (tx - px) * i / n)
            z = round(pz + (tz - pz) * i / n)
            for w in (0, 1):
                y = surface(v, hgt, x + w, z)
                if v.get(x + w, y, z) == "grass_block":
                    v.set(x + w, y, z, "dirt_path")
    return v.write(path)


def make_big(path):
    """~400x200x400 arena-ish stress test with a few million blocks."""
    sx, sy, sz = 400, 200, 400
    v = Vox(sx, sy, sz)
    g = v.g
    X, Z = np.meshgrid(np.arange(sx), np.arange(sz), indexing="ij")
    h = (40 + 12 * noise2(X * 0.5, Z * 0.5, 2) + 8 * noise2(X * 0.13, Z * 0.11, 5)).astype(int)
    stone, dirt, grass, water, sand = (v.id(n) for n in ("stone", "dirt", "grass_block", "water", "sand"))
    Y = np.arange(sy)[None, :, None]
    H = h[:, None, :]
    g[...] = np.where(Y < H - 3, stone, np.where(Y < H, dirt, np.where(Y == H, grass, 0)))
    sea = 38
    g[(Y > H) & (Y <= sea)] = water
    g[(Y == H) & (H <= sea + 1)] = sand
    # concrete pillars grid
    for i, c in enumerate(COLOURS):
        cid = v.id(f"{c}_concrete")
        gx, gz = 40 + (i % 4) * 100, 40 + (i // 4) * 100
        g[gx - 6:gx + 6, 30:140, gz - 6:gz + 6] = cid
        g[gx - 4:gx + 4, 60:130, gz - 4:gz + 4] = 0
        g[gx - 7:gx + 7, 140, gz - 7:gz + 7] = v.id(f"{c}_stained_glass")
    return v.write(path)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("out", nargs="?", default="sample.mcmdump.gz")
    ap.add_argument("--big", action="store_true")
    a = ap.parse_args()
    t = time.time()
    n = make_big(a.out) if a.big else make_island(a.out)
    print(f"wrote {a.out}: {n:,} blocks in {time.time() - t:.1f}s")
