#!/usr/bin/env python3
"""Labelled route overlay on a material-coloured top-down map."""
import gzip
import math
from PIL import Image, ImageDraw, ImageFont

ORIGIN = (-182, -64, -140)
TOP = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    wx, wy, wz = int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2]
    if (wx, wz) not in TOP or wy > TOP[(wx, wz)][0]:
        TOP[(wx, wz)] = (wy, n.split("[")[0].replace("minecraft:", ""))

COLOURS = {
    "water": (60, 105, 190), "bubble_column": (120, 170, 235),
    "ice": (175, 215, 245), "packed_ice": (150, 200, 240), "blue_ice": (120, 175, 235),
    "snow_block": (246, 248, 252), "snow": (235, 242, 250),
    "stone": (118, 118, 122), "andesite": (140, 140, 140), "cobblestone": (100, 100, 100),
    "stone_bricks": (110, 110, 114), "smooth_quartz": (236, 232, 224),
    "gray_concrete": (78, 80, 85), "black_concrete": (22, 22, 26), "white_concrete": (228, 229, 231),
    "yellow_glazed_terracotta": (240, 195, 40), "pink_glazed_terracotta": (238, 120, 170),
    "magenta_glazed_terracotta": (190, 40, 150), "red_glazed_terracotta": (200, 45, 40),
    "lime_wool": (110, 225, 55), "yellow_carpet": (245, 210, 70),
    "lodestone": (85, 85, 92), "barrier": (255, 0, 0),
    "spruce_planks": (105, 75, 48), "mangrove_planks": (125, 62, 55), "dark_oak_planks": (70, 48, 30),
    "stripped_spruce_wood": (140, 110, 75), "spruce_slab": (110, 80, 50),
    "grass": (105, 155, 65), "dirt": (118, 88, 58), "podzol": (95, 72, 44),
    "spruce_leaves": (48, 98, 44), "lantern": (255, 210, 120), "sea_lantern": (210, 240, 235),
}


def colour(name):
    if name in COLOURS:
        return COLOURS[name]
    for k, c in COLOURS.items():
        if k in name:
            return c
    if "wool" in name or "carpet" in name or "concrete" in name or "terracotta" in name:
        return (185, 155, 165)
    if "leaves" in name or "grass" in name or "moss" in name:
        return (80, 140, 60)
    if "glass" in name or "pane" in name:
        return (195, 220, 235)
    if "planks" in name or "wood" in name or "log" in name:
        return (115, 85, 58)
    return (150, 150, 155)


X0, X1, Z0, Z1, S = -182, 77, -140, 77, 6
W = (X1 - X0 + 1) * S + 120
H = (Z1 - Z0 + 1) * S + 250
img = Image.new("RGB", (W, H), (238, 242, 247))
dr = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 12)
    small = ImageFont.truetype("arial.ttf", 10)
    big = ImageFont.truetype("arial.ttf", 15)
except Exception:  # noqa: BLE001
    font = ImageFont.load_default(); small = font; big = font

for (x, z), (y, name) in TOP.items():
    px = 80 + (x - X0) * S
    py = 40 + (z - Z0) * S
    dr.rectangle([px, py, px + S - 1, py + S - 1], fill=colour(name))

for gx in range(-176, X1 + 1, 32):
    px = 80 + (gx - X0) * S
    dr.line([px, 40, px, 40 + (Z1 - Z0 + 1) * S], fill=(170, 175, 182))
    dr.text((px + 2, 22), str(gx), fill=(40, 40, 40), font=small)
for gz in range(-136, Z1 + 1, 32):
    py = 40 + (gz - Z0) * S
    dr.line([80, py, 80 + (X1 - X0 + 1) * S, py], fill=(170, 175, 182))
    dr.text((50, py + 1), str(gz), fill=(40, 40, 40), font=small)


def P(x, z):
    return (80 + (x - X0) * S, 40 + (z - Z0) * S)


def poly(points, fill, width=4, dash=None):
    pts = [P(x, z) for x, z in points]
    if dash:
        for i in range(len(pts) - 1):
            x1, y1 = pts[i]; x2, y2 = pts[i + 1]
            d = math.hypot(x2 - x1, y2 - y1)
            n = max(1, int(d / dash))
            for k in range(n):
                t0 = k / n; t1 = min(1, (k + 0.55) / n)
                dr.line([x1 + (x2-x1)*t0, y1 + (y2-y1)*t0, x1 + (x2-x1)*t1, y1 + (y2-y1)*t1],
                        fill=fill, width=width)
    else:
        dr.line(pts, fill=fill, width=width, joint="curve")


primary = [
    (21, 6), (21, 9), (21, 14), (26, 20), (28, 28), (29, 38), (26, 48),
    (20, 57), (10, 64), (-3, 69), (-13, 62), (-16, 53), (-19, 44), (-25, 39),
    (-34, 35), (-45, 31), (-55, 29), (-70, 27), (-85, 28), (-100, 33),
    (-112, 40), (-120, 36), (-127, 30), (-127, 27), (-135, 29), (-145, 31),
    (-155, 30), (-165, 31),
]
alt = [
    (-34, 35), (-35, 30), (-38, 24), (-42, 20), (-47, 16), (-55, 10),
    (-65, 3), (-78, -4), (-95, -10), (-115, -18), (-140, -20), (-160, -19),
    (-172, -16), (-176, -12), (-168, -24), (-150, -26), (-132, -22),
    (-115, -14), (-100, -5), (-85, -2), (-70, -4), (-52, -10), (-40, -14),
    (-30, -18), (-24, -26), (-20, -32), (-17, -36),
]

# outline then fill
for pts, col in ((primary, (255, 255, 255)), (alt, (255, 165, 40))):
    poly(pts, (0, 0, 0), width=7)
    poly(pts, col, width=4)
# elytra legs (dashed)
poly([(-163, 31), (15, -132)], (0, 0, 0), width=7, dash=18)
poly([(-163, 31), (15, -132)], (0, 230, 255), width=4, dash=18)
poly([(-17, -36), (15, -132)], (0, 0, 0), width=7, dash=18)
poly([(-17, -36), (15, -132)], (0, 230, 255), width=4, dash=18)
# return leg
ret = [(15, -132), (28, -118), (48, -104), (36, -92), (58, -82), (52, -62),
       (40, -58), (21, -50), (21, -40), (21, -25), (21, -15), (21, -11), (21, 6)]
poly(ret, (0, 0, 0), width=7)
poly(ret, (255, 60, 220), width=4)

wp = {
    1: (21, 6), 2: (21, 14), 3: (28, 32), 4: (12, 63), 5: (-16, 50),
    6: (-30, 37), 7: (-60, 28), 8: (-95, 30), 9: (-120, 37), 10: (-127, 29),
    11: (-150, 31), 12: (-165, 31), 13: (15, -132), 14: (48, -104),
    15: (21, -48), 16: (21, -25), 17: (21, -12), 18: (21, 6),
}
for n, (x, z) in wp.items():
    px, py = P(x, z)
    dr.ellipse([px-10, py-10, px+10, py+10], fill=(0, 0, 0))
    dr.ellipse([px-8, py-8, px+8, py+8], fill=(255, 255, 255))
    txt = str(n)
    tb = dr.textbbox((0, 0), txt, font=font)
    dr.text((px - (tb[2]-tb[0])/2, py - (tb[3]-tb[1])/2 - 1), txt, fill=(0, 0, 0), font=font)

legend = [
    ("white line  = primary racing line (station / checkpoint / elytra)", (255, 255, 255)),
    ("orange line = pool / river / lodge alternate line", (255, 165, 40)),
    ("cyan dashed = elytra flight to the north landing", (0, 230, 255)),
    ("magenta line = north chain -> tunnel -> waterfall -> finish", (255, 60, 220)),
]
ly = 40 + (Z1 - Z0 + 1) * S + 8
for i, (text, col) in enumerate(legend):
    dr.rectangle([84, ly + i*18, 104, ly + i*18 + 10], fill=col, outline=(0, 0, 0))
    dr.text((110, ly + i*18 - 1), text, fill=(20, 20, 20), font=small)
dr.text((80, ly + 82), "Yeti Set Go - route overlay (world x/z grid). Numbers = leg waypoints; see data/route.md for y and mechanics.",
        fill=(20, 20, 20), font=big)
img.save("renders/route_overlay.png")
print("wrote renders/route_overlay.png", img.size)
