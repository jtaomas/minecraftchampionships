#!/usr/bin/env python3
"""Annotated top-down map: heightmap + grid + markers for commands/signs/special pads."""
import gzip
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
OUT = os.path.join(BASE, "renders")

top = {}
special = {}
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        x, y, z = int(x), int(y), int(z)
        k = (x, z)
        cur = top.get(k)
        if cur is None or y > cur[0]:
            top[k] = (y, b)
        base = b.split("[")[0]
        if base in ("minecraft:diamond_block", "minecraft:red_concrete", "minecraft:gray_concrete",
                    "minecraft:magenta_glazed_terracotta", "minecraft:orange_concrete",
                    "minecraft:red_sandstone", "minecraft:bedrock", "minecraft:lime_stained_glass",
                    "minecraft:water"):
            special.setdefault(base, []).append((x, y, z))

xs = [k[0] for k in top]; zs = [k[1] for k in top]
x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
S = 2
W, H = (x1 - x0 + 1) * S, (z1 - z0 + 1) * S

base = Image.new("RGB", (x1 - x0 + 1, z1 - z0 + 1), (14, 16, 24))
px = base.load()
ylo = min(v[0] for v in top.values()); yhi = max(v[0] for v in top.values())
for (x, z), (y, b) in top.items():
    spec = rp.resolve_block(b)
    if spec is None:
        continue
    col = spec[0]
    f = 0.55 + 0.45 * ((y - ylo) / max(1, yhi - ylo))
    px[x - x0, z - z0] = tuple(min(255, int(c * f)) for c in col)

img = base.resize((W, H), Image.NEAREST)
d = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 14)
except Exception:
    font = ImageFont.load_default()

for gx in range((x0 // 64) * 64, x1 + 1, 64):
    X = (gx - x0) * S
    d.line([(X, 0), (X, H)], fill=(70, 78, 92), width=1)
    d.text((X + 2, 2), f"x{gx}", fill=(150, 160, 180), font=font)
for gz in range((z0 // 64) * 64, z1 + 1, 64):
    Z = (gz - z0) * S
    d.line([(0, Z), (W, Z)], fill=(70, 78, 92), width=1)
    d.text((2, Z + 1), f"z{gz}", fill=(150, 160, 180), font=font)

T = json.load(open(os.path.join(BASE, "data_raw", "tile_entities.json"), encoding="utf-8"))
for t in T:
    if t["id"] == "minecraft:command_block":
        X, Z = (t["x"] - x0) * S, (t["z"] - z0) * S
        d.ellipse([X - 5, Z - 5, X + 5, Z + 5], outline=(0, 255, 255), width=2)
    else:
        X, Z = (t["x"] - x0) * S, (t["z"] - z0) * S
        d.rectangle([X - 4, Z - 4, X + 4, Z + 4], outline=(255, 255, 0), width=2)

for x, y, z in special.get("minecraft:diamond_block", []):
    X, Z = (x - x0) * S, (z - z0) * S
    d.point((X, Z), fill=(80, 240, 255))
for x, y, z in special.get("minecraft:magenta_glazed_terracotta", []):
    X, Z = (x - x0) * S, (z - z0) * S
    d.point((X, Z), fill=(255, 0, 255))
for x, y, z in special.get("minecraft:orange_concrete", []):
    X, Z = (x - x0) * S, (z - z0) * S
    d.point((X, Z), fill=(255, 140, 0))
for x, y, z in special.get("minecraft:red_concrete", []):
    X, Z = (x - x0) * S, (z - z0) * S
    d.point((X, Z), fill=(255, 60, 60))

img.save(os.path.join(OUT, "heightmap_grid.png"))
print("saved", os.path.join(OUT, "heightmap_grid.png"), img.size)

# material extents for route reasoning
for name, pts in sorted(special.items()):
    xs2 = [p[0] for p in pts]; zs2 = [p[2] for p in pts]; ys2 = [p[1] for p in pts]
    print(name, len(pts), "x", min(xs2), max(xs2), "y", min(ys2), max(ys2), "z", min(zs2), max(zs2))
