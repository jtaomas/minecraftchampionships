#!/usr/bin/env python3
"""Walkability / reachability probe for the lower course of TGTTOS Industry.

Builds a per-column standing height (top of the highest solid block below the
catwalk level), then BFS from the start with walk / step / sprint-jump moves.
Read-only analysis; writes a labelled PNG + CSV.
"""
import csv
import os
from collections import deque

import numpy as np
from PIL import Image, ImageDraw, ImageFont

DATA = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data\blocks_xyz.csv"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\renders"

# blocks that cannot be stood on / block movement
NON_SOLID_WORDS = ("air", "barrier", "water", "lava", "glass_pane", "iron_bars", "fence", "wall",
                   "trapdoor", "sign", "ladder", "torch", "button", "lever", "rail", "pane",
                   "carpet", "flower", "grass", "sapling")
SOLID_WORDS = ("stone", "andesite", "diorite", "blackstone", "bricks", "terracotta", "wool",
               "concrete", "quartz", "planks", "slab", "stairs", "iron_block", "sea_lantern",
               "glass")


def solid(name):
    n = name.split("[")[0].replace("minecraft:", "")
    if n in ("minecraft:air",):
        return False
    if "glass_pane" in n or n.endswith("_pane"):
        return False
    if any(w in n for w in ("trapdoor", "fence", "wall", "bars", "sign", "ladder")):
        return False
    if "glass" in n:
        return True
    return any(w in n for w in SOLID_WORDS)


cols = {}  # (x,z) -> sorted list of (y, block)
with open(DATA, encoding="utf-8") as f:
    for d in csv.DictReader(f):
        if d["block"].split("[")[0] == "minecraft:barrier":
            continue
        x, y, z = int(d["x"]), int(d["y"]), int(d["z"])
        if not (-41 <= y <= -19):
            continue
        cols.setdefault((x, z), []).append((y, d["block"]))

ys = {}
for k, v in cols.items():
    v.sort()
    top = None
    for y, b in v:
        if solid(b):
            top = y
    if top is not None:
        ys[k] = top

# A standable column needs 2 air above its top within the play band.
stand = {}
for (x, z), top in ys.items():
    above = [b for (yy, b) in cols.get((x, z), []) if yy > top]
    if len(above) == 0:  # nothing above at all -> open
        stand[(x, z)] = top + 1
    else:
        # check the two blocks directly above are not solid
        pass

# simpler: stand if no solid block at top+1 or top+2
solid_at = {}
for (x, z), v in cols.items():
    for y, b in v:
        if solid(b):
            solid_at[(x, y, z)] = b

for (x, z), top in list(ys.items()):
    if (x, top + 1, z) not in solid_at and (x, top + 2, z) not in solid_at:
        stand[(x, z)] = top + 1

print("standable columns", len(stand))


def neighbours(x, z):
    """walk/step moves: 4-neighbour within +-1 rise, or fall any amount."""
    h = stand.get((x, z))
    if h is None:
        return
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, nz = x + dx, z + dz
        nh = stand.get((nx, nz))
        if nh is None:
            continue
        if nh <= h + 1:
            yield nx, nz


# BFS with jumps: allow up to 4 horizontal gap and rise <= 1, fall <= 8
def jump_neighbours(x, z):
    h = stand.get((x, z))
    if h is None:
        return
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            d = max(abs(dx), abs(dz))
            if d == 0 or d > 4:
                continue
            nx, nz = x + dx, z + dz
            nh = stand.get((nx, nz))
            if nh is None:
                continue
            if nh <= h + 1 and (h - nh) <= 8:
                yield nx, nz


start = (73, 11)
assert start in stand, (start, stand.get(start))
seen = {start}
q = deque([start])
while q:
    x, z = q.popleft()
    for nx, nz in list(neighbours(x, z)) + list(jump_neighbours(x, z)):
        if (nx, nz) not in seen:
            seen.add((nx, nz))
            q.append((nx, nz))
print("reachable columns", len(seen), "z range", min(z for x, z in seen), max(z for x, z in seen))


def font(sz):
    for name in ("consola.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, sz)
        except Exception:
            pass
    return ImageFont.load_default()


xs = [x for x, z in stand]
zs = [z for x, z in stand]
x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
s = 7
ml, mt = 44, 30
w = (x1 - x0 + 1) * s
h = (z1 - z0 + 1) * s
im = Image.new("RGB", (w + ml + 20, h + mt + 40), (245, 248, 251))
d = ImageDraw.Draw(im, "RGBA")
for (x, z), top in stand.items():
    px = ml + (x - x0) * s
    py = mt + (z - z0) * s
    if (x, z) in seen:
        c = (60, 160, 90)
    else:
        c = (170, 176, 185)
    d.rectangle([px, py, px + s - 1, py + s - 1], fill=c)
# mark start + finish chicken
for (px0, pz0, col, label) in [(73, 11, (220, 40, 40), "start"), (62, 92, (30, 90, 220), "finish")]:
    px = ml + (px0 - x0) * s
    py = mt + (pz0 - z0) * s
    d.ellipse([px - 5, py - 5, px + 5, py + 5], fill=col)
    d.text((px + 7, py - 8), label, fill=col, font=font(14))
f = font(13)
for gx in range(x0 - x0 % 10, x1 + 1, 10):
    px = ml + (gx - x0) * s
    d.line([(px, mt), (px, mt + h)], fill=(255, 255, 255, 50))
    d.text((px + 2, mt + h + 2), str(gx), fill=(30, 40, 55), font=f)
for gz in range(z0 - z0 % 10, z1 + 1, 10):
    py = mt + (gz - z0) * s
    d.line([(ml, py), (ml + w, py)], fill=(255, 255, 255, 50))
    d.text((2, py - 7), str(gz), fill=(30, 40, 55), font=f)
d.text((ml, 6), "Lower-level reachability from start (green = reachable with <=4 gap, <=1 rise)", fill=(10, 20, 30), font=font(15))
d.text((ml + w // 2, mt + h + 20), "x", fill=(20, 25, 35), font=font(15))
d.text((3, 3), "z (down)", fill=(20, 25, 35), font=font(15))
im.save(os.path.join(OUT, "reach_lower.png"))
print("wrote", os.path.join(OUT, "reach_lower.png"), im.size)

with open(os.path.join(OUT, "..", "data", "reachable_lower.csv"), "w", newline="", encoding="utf-8") as fh:
    wtr = csv.writer(fh)
    wtr.writerow(["x", "z", "stand_y", "reachable"])
    for (x, z), top in sorted(stand.items()):
        wtr.writerow([x, z, top, int((x, z) in seen)])
