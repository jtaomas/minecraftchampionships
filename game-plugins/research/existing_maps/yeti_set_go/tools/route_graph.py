#!/usr/bin/env python3
"""Surface connectivity probe for the Yeti Set Go map.

Builds a walkable top-surface grid (1 block per column), floods from a
seed, and writes a PNG of reached columns. Used only to trace the course
by eye; not a physics simulation.
"""
import gzip
import sys
from collections import deque

from PIL import Image, ImageDraw, ImageFont

ORIGIN = (-182, -64, -140)
NOT_WALK = {
    "minecraft:water", "minecraft:bubble_column", "minecraft:lava",
    "minecraft:air", "minecraft:cave_air", "minecraft:void_air",
    "minecraft:glass", "minecraft:glass_pane", "minecraft:ice",
    "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow",
    "minecraft:leaves", "minecraft:spruce_leaves", "minecraft:barrier",
    "minecraft:iron_trapdoor", "minecraft:ladder",
}
# treat as walkable surfaces anyway
WALK_ANYWAY = {"minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow",
               "minecraft:water", "minecraft:bubble_column"}

blocks = {}
columns = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    key = (int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2])
    blocks[key] = n
    name = n.split("[")[0]
    col = (key[0], key[2])
    if col not in columns or key[1] > columns[col][0]:
        columns[col] = (key[1], name)


def base(name):
    return name.split("[")[0]


def top_at(x, z):
    best = None
    for (bx, by, bz), name in blocks.items():
        if bx == x and bz == z and (best is None or by > best[0]):
            best = (by, base(name))
    return best


def build():
    cols = {}
    for (x, z), (y, name) in columns.items():
        if name in NOT_WALK and name not in WALK_ANYWAY:
            continue
        # need 2 air above the surface
        if (x, y + 1, z) in blocks or (x, y + 2, z) in blocks:
            continue
        cols[(x, z)] = y
    return cols


def flood(cols, seeds, step_up=1, step_down=3, jumps=0):
    seen = set()
    q = deque()
    for s in seeds:
        if s in cols:
            seen.add(s); q.append(s)
    while q:
        x, z = q.popleft()
        y = cols[(x, z)]
        for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, nz = x + dx, z + dz
            if (nx, nz) in cols and (nx, nz) not in seen:
                ny = cols[(nx, nz)]
                if y - step_down <= ny <= y + step_up:
                    seen.add((nx, nz)); q.append((nx, nz))
    return seen


def draw(cols, seen, out, title):
    xs = [p[0] for p in cols]; zs = [p[1] for p in cols]
    x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
    s = 4
    img = Image.new("RGB", ((x1-x0+1)*s + 70, (z1-z0+1)*s + 50), (245, 247, 250))
    dr = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 11)
    except Exception:  # noqa: BLE001
        font = ImageFont.load_default()
    for (x, z), y in cols.items():
        px = 70 + (x - x0) * s
        py = 30 + (z - z0) * s
        col = (140, 190, 140) if (x, z) in seen else (210, 210, 214)
        dr.rectangle([px, py, px+s-1, py+s-1], fill=col)
    for gx in range(-176, 78, 32):
        px = 70 + (gx - x0) * s
        dr.line([px, 30, px, 30 + (z1-z0+1)*s], fill=(180, 180, 180))
        dr.text((px+2, 12), str(gx), fill=(40, 40, 40), font=font)
    for gz in range(-136, 78, 32):
        py = 30 + (gz - z0) * s
        dr.line([70, py, 70 + (x1-x0+1)*s, py], fill=(180, 180, 180))
        dr.text((36, py+1), str(gz), fill=(40, 40, 40), font=font)
    dr.text((72, 30 + (z1-z0+1)*s + 6), title, fill=(20, 20, 20), font=font)
    img.save(out)
    print(f"{out}: seen {len(seen)} of {len(cols)} cols")


if __name__ == "__main__":
    cols = build()
    print(f"walkable columns: {len(cols)}")
    seed = (int(sys.argv[1]), int(sys.argv[2]))
    seen = flood(cols, [seed])
    draw(cols, seen, sys.argv[3], f"flood from world {sys.argv[1]},{sys.argv[2]} - {len(seen)} cols")
