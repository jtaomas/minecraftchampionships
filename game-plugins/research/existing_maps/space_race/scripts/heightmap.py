#!/usr/bin/env python3
"""Top-down heightmap of the whole Space Race world (1 px per column)."""
import gzip
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
OUT = os.path.join(BASE, "renders")
os.makedirs(OUT, exist_ok=True)

top = {}
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        x, y, z = int(x), int(y), int(z)
        k = (x, z)
        cur = top.get(k)
        if cur is None or y > cur[0]:
            top[k] = (y, b)

xs = [k[0] for k in top]; zs = [k[1] for k in top]
x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
W, H = x1 - x0 + 1, z1 - z0 + 1
print("bounds", x0, x1, z0, z1, "size", W, H, "cols", len(top))

img = Image.new("RGB", (W, H), (18, 20, 28))
px = img.load()
ys = [v[0] for v in top.values()]
ylo, yhi = min(ys), max(ys)
for (x, z), (y, b) in top.items():
    spec = rp.resolve_block(b)
    if spec is None:
        continue
    col = spec[0]
    f = 0.55 + 0.45 * ((y - ylo) / max(1, yhi - ylo))
    px[x - x0, z - z0] = tuple(min(255, int(c * f)) for c in col)

img.save(os.path.join(OUT, "heightmap_raw.png"))
print("saved", os.path.join(OUT, "heightmap_raw.png"), "y", ylo, yhi)
