#!/usr/bin/env python3
"""Full-height reachability probe for TGTTOS Industry (read-only)."""
import csv
import os
from collections import deque

from PIL import Image, ImageDraw, ImageFont

DATA = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data\blocks_xyz.csv"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\renders"

cols = {}
with open(DATA, encoding="utf-8") as f:
    for d in csv.DictReader(f):
        if d["block"].split("[")[0] == "minecraft:barrier":
            continue
        x, y, z = int(d["x"]), int(d["y"]), int(d["z"])
        cols.setdefault((x, z), []).append((y, d["block"]))


def solid(name):
    n = name.split("[")[0].replace("minecraft:", "")
    if "glass_pane" in n or n.endswith("_pane"):
        return False
    if any(w in n for w in ("trapdoor", "fence", "wall", "bars", "sign", "ladder")):
        return False
    if "glass" in n:
        return False  # glass is solid but we treat as non-walkable for now
    return True


solid_at = {}
for (x, z), v in cols.items():
    for y, b in v:
        if solid(b):
            solid_at[(x, y, z)] = b

stand = {}
for (x, z), v in cols.items():
    tops = [y for y, b in v if solid(b)]
    if not tops:
        continue
    top = max(tops)
    if (x, top + 1, z) not in solid_at and (x, top + 2, z) not in solid_at:
        stand[(x, z)] = top + 1
print("standable", len(stand))


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
            if nh <= h + 1 and (h - nh) <= 30:
                yield nx, nz


start = (73, 11)
print("start stand", start, stand.get(start))
seen = {start}
q = deque([start])
while q:
    x, z = q.popleft()
    for n in jump_neighbours(x, z):
        if n not in seen:
            seen.add(n)
            q.append(n)
print("reachable", len(seen), "z", min(z for x, z in seen), max(z for x, z in seen),
      "y", min(stand[p] for p in seen), max(stand[p] for p in seen))
# cells near the finish (chicken pen x55..90 z86..99)
near = [p for p in seen if 55 <= p[0] <= 90 and 86 <= p[1] <= 99]
print("reachable in finish pen:", len(near), near[:10])


def font(sz):
    for name in ("consola.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, sz)
        except Exception:
            pass
    return ImageFont.load_default()


xs = [x for x, z in stand]; zs = [z for x, z in stand]
x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
s = 7; ml, mt = 44, 30
w = (x1 - x0 + 1) * s; h = (z1 - z0 + 1) * s
im = Image.new("RGB", (w + ml + 20, h + mt + 40), (245, 248, 251))
d = ImageDraw.Draw(im, "RGBA")
for (x, z), top in stand.items():
    px = ml + (x - x0) * s
    py = mt + (z - z0) * s
    # colour by height
    if (x, z) in seen:
        band = max(-40, min(8, top))
        t = (band + 40) / 48
        c = (int(40 + 180 * t), int(160 - 60 * t), int(90 + 100 * (1 - t)))
    else:
        c = (185, 190, 198)
    d.rectangle([px, py, px + s - 1, py + s - 1], fill=c)
for (px0, pz0, col, label) in [(73, 11, (200, 20, 20), "start"), (72, 92, (20, 60, 200), "finish pen")]:
    px = ml + (px0 - x0) * s; py = mt + (pz0 - z0) * s
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
d.text((ml, 6), "All-height reachability from start (colour = standing y, grey = unreachable)", fill=(10, 20, 30), font=font(15))
im.save(os.path.join(OUT, "reach_all.png"))
print("wrote reach_all.png", im.size)
