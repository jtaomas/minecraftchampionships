#!/usr/bin/env python3
"""Plan of water blocks at chosen Y, and diamond/bedrock/pad overlays."""
import gzip
import os
import sys

from PIL import Image, ImageDraw, ImageFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
OUT = os.path.join(BASE, "renders", "water_map.png")

rows = []
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        rows.append((int(x), int(y), int(z), b.split("[")[0]))

x0, x1, z0, z1 = -180, 180, -130, 450
S = 3
img = Image.new("RGB", ((x1 - x0) * S, (z1 - z0) * S), (10, 12, 18))
d = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 13)
except Exception:
    font = ImageFont.load_default()

COL = {
    "minecraft:water": (40, 90, 255),
    "minecraft:diamond_block": (0, 255, 230),
    "minecraft:bedrock": (90, 70, 60),
    "minecraft:red_concrete": (255, 40, 40),
    "minecraft:orange_concrete": (255, 140, 0),
    "minecraft:magenta_glazed_terracotta": (255, 0, 255),
    "minecraft:lime_stained_glass": (80, 255, 80),
    "minecraft:sea_lantern": (255, 250, 200),
}
for x, y, z, b in rows:
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        continue
    c = COL.get(b)
    if c is None:
        continue
    if b == "minecraft:water" and not (140 <= y <= 200):
        continue
    if b == "minecraft:lime_stained_glass" and y > 110:
        continue
    if b == "minecraft:sea_lantern" and y < 110:
        continue
    d.rectangle([(x - x0) * S, (z - z0) * S, (x - x0) * S + S - 1, (z - z0) * S + S - 1], fill=c)

for gx in range(x0, x1 + 1, 32):
    X = (gx - x0) * S
    d.line([(X, 0), (X, img.height)], fill=(50, 55, 68))
    d.text((X + 1, 1), str(gx), fill=(170, 180, 200), font=font)
for gz in range(z0, z1 + 1, 32):
    Z = (gz - z0) * S
    d.line([(0, Z), (img.width, Z)], fill=(50, 55, 68))
    d.text((1, Z + 1), str(gz), fill=(170, 180, 200), font=font)
img.save(OUT)
print("saved", OUT, img.size)
