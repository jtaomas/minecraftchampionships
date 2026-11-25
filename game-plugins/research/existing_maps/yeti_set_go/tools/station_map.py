#!/usr/bin/env python3
"""Large annotated top-down of the west station and its approaches."""
import gzip
from PIL import Image, ImageDraw, ImageFont

ORIGIN = (-182, -64, -140)
blocks = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    blocks[(int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2])] = n

TOP = {}
for (x, y, z), n in blocks.items():
    if (x, z) not in TOP or y > TOP[(x, z)][0]:
        TOP[(x, z)] = (y, n)

COL = {
    "lodestone": (80, 80, 90), "yellow_glazed_terracotta": (235, 190, 20),
    "pink_glazed_terracotta": (240, 120, 175), "magenta_glazed_terracotta": (190, 30, 150),
    "red_glazed_terracotta": (210, 40, 35), "lime_wool": (100, 230, 45),
    "bubble_column": (90, 170, 250), "water": (60, 110, 200),
    "gray_concrete": (95, 95, 100), "white_concrete": (230, 230, 235),
    "black_concrete": (25, 25, 30), "mangrove_planks": (130, 65, 55),
    "spruce_planks": (110, 80, 50), "snow_block": (245, 245, 250),
    "stone": (120, 120, 125), "smooth_quartz": (235, 230, 220),
    "barrier": (255, 0, 0), "iron_trapdoor": (180, 180, 185),
}


def colour(name):
    b = name.split("[")[0].replace("minecraft:", "")
    if b in COL:
        return COL[b]
    for k, c in COL.items():
        if k in b:
            return c
    if "snow" in b:
        return (240, 242, 248)
    if "ice" in b:
        return (170, 210, 240)
    if "wood" in b or "plank" in b:
        return (120, 85, 55)
    return (160, 160, 165)


x0, x1, z0, z1, s = -180, -115, 15, 60, 14
img = Image.new("RGB", ((x1-x0+1)*s + 90, (z1-z0+1)*s + 40), (250, 250, 252))
dr = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 11)
except Exception:  # noqa: BLE001
    font = ImageFont.load_default()
for (x, z), (y, n) in TOP.items():
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        continue
    px = 70 + (x - x0) * s
    py = 30 + (z - z0) * s
    c = colour(n)
    dr.rectangle([px, py, px+s-1, py+s-1], fill=c)
    if n.split("[")[0] == "minecraft:lodestone":
        dr.text((px+1, py+1), "L", fill=(255, 255, 255), font=font)
for gx in range(-176, x1, 8):
    px = 70 + (gx - x0) * s
    dr.line([px, 30, px, 30 + (z1-z0+1)*s], fill=(190, 190, 190))
    dr.text((px+1, 12), str(gx), fill=(20, 20, 20), font=font)
for gz in range(16, z1, 8):
    py = 30 + (gz - z0) * s
    dr.line([70, py, 70 + (x1-x0+1)*s, py], fill=(190, 190, 190))
    dr.text((32, py+1), str(gz), fill=(20, 20, 20), font=font)
# mark notable block entities
for (x, y, z), n in blocks.items():
    b = n.split("[")[0]
    if x0 <= x <= x1 and z0 <= z <= z1 and b in ("minecraft:command_block", "minecraft:chain_command_block", "minecraft:sign", "minecraft:hopper", "minecraft:lever"):
        px = 70 + (x - x0) * s + s//2
        py = 30 + (z - z0) * s + s//2
        dr.ellipse([px-3, py-3, px+3, py+3], outline=(255, 0, 0), width=2)
dr.text((72, 30 + (z1-z0+1)*s + 6), "West station top-down: colour = top block; L = lodestone; red rings = command blocks/signs/hopper/lever", fill=(20, 20, 20), font=font)
img.save("renders/station_top.png")
print("wrote renders/station_top.png", img.size)
