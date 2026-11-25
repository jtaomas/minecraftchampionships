#!/usr/bin/env python3
"""Height-coloured top-down: each column gets the Y of its topmost non-air block."""
import gzip
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

src, dst = sys.argv[1], sys.argv[2]
SCALE = int(sys.argv[3]) if len(sys.argv) > 3 else 2
SKIP = {"minecraft:barrier", "minecraft:structure_void", "minecraft:light",
        "minecraft:water", "minecraft:bubble_column", "minecraft:seagrass",
        "minecraft:tall_seagrass", "minecraft:vine", "minecraft:grass",
        "minecraft:tall_grass", "minecraft:oak_leaves", "minecraft:spruce_leaves",
        "minecraft:azalea_leaves", "minecraft:dark_oak_leaves", "minecraft:jungle_leaves",
        "minecraft:flowering_azalea_leaves"}

hdr = None
cols = {}
with gzip.open(src, "rt", encoding="utf-8") as fh:
    hdr = fh.readline().split()
    sx, sy, sz = int(hdr[2]), int(hdr[3]), int(hdr[4])
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4:
            continue
        x, y, z = int(p[0]), int(p[1]), int(p[2])
        name = p[3].strip()
        if name in SKIP:
            continue
        k = (x, z)
        if k not in cols or y > cols[k][0]:
            cols[k] = (y, name)

print("columns", len(cols), "of", sx * sz)
ys = np.array([v[0] for v in cols.values()])
print("y min/max", ys.min(), ys.max())

im = Image.new("RGB", (sx * SCALE, sz * SCALE), (14, 16, 22))
px = im.load()
for (x, z), (y, name) in cols.items():
    if y < 0:
        c = (20, 30, 70)
    else:
        t = (y + 64) / 384.0
        # blue -> cyan -> green -> yellow -> orange -> white
        stops = [(0.0, (20, 45, 110)), (0.2, (20, 140, 190)), (0.35, (40, 180, 110)),
                 (0.5, (150, 200, 70)), (0.65, (240, 200, 60)), (0.8, (240, 130, 50)),
                 (0.92, (240, 240, 230)), (1.0, (255, 255, 255))]
        for i in range(len(stops) - 1):
            a, ca = stops[i]
            b, cb = stops[i + 1]
            if a <= t <= b:
                f = (t - a) / (b - a)
                c = tuple(int(ca[j] + (cb[j] - ca[j]) * f) for j in range(3))
                break
        else:
            c = (255, 255, 255)
    for dy in range(SCALE):
        for dx in range(SCALE):
            px[x * SCALE + dx, z * SCALE + dy] = c

d = ImageDraw.Draw(im, "RGBA")
try:
    font = ImageFont.truetype("arial.ttf", 14)
except Exception:
    font = ImageFont.load_default()
for gx in range(0, sx, 50):
    d.line([(gx * SCALE, 0), (gx * SCALE, sz * SCALE)], fill=(255, 0, 0, 90))
    d.text((gx * SCALE + 2, 2), str(gx), fill=(255, 80, 80, 255), font=font)
for gz in range(0, sz, 50):
    d.line([(0, gz * SCALE), (sx * SCALE, gz * SCALE)], fill=(255, 0, 0, 90))
    d.text((2, gz * SCALE + 1), str(gz), fill=(255, 80, 80, 255), font=font)
im.save(dst)
print("wrote", dst, im.size)
