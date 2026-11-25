#!/usr/bin/env python3
"""Height-coloured top-down map (world y of the top block) + contour bands."""
import gzip
from PIL import Image, ImageDraw, ImageFont

ORIGIN = (-182, -64, -140)
cols = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    wx, wy, wz = int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2]
    if (wx, wz) not in cols or wy > cols[(wx, wz)][0]:
        cols[(wx, wz)] = (wy, n.split("[")[0])

ramp = [
    (-64, (25, 35, 70)), (-40, (35, 75, 130)), (-24, (60, 120, 190)),
    (-16, (90, 160, 210)), (-8, (150, 200, 230)), (0, (235, 245, 250)),
    (8, (200, 215, 220)), (16, (170, 175, 160)), (24, (140, 120, 90)),
    (32, (110, 80, 60)), (46, (70, 50, 40)),
]


def col(y):
    prev = ramp[0]
    for yy, c in ramp:
        if y <= yy:
            if yy == prev[0]:
                return c
            t = (y - prev[0]) / (yy - prev[0])
            return tuple(int(prev[1][i] + t * (c[i] - prev[1][i])) for i in range(3))
        prev = (yy, c)
    return ramp[-1][1]


x0, x1, z0, z1, s = -182, 77, -140, 77, 5
img = Image.new("RGB", ((x1-x0+1)*s + 80, (z1-z0+1)*s + 55), (250, 250, 252))
dr = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 11)
    big = ImageFont.truetype("arial.ttf", 13)
except Exception:  # noqa: BLE001
    font = ImageFont.load_default(); big = font
for (x, z), (y, name) in cols.items():
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        continue
    px = 70 + (x - x0) * s
    py = 30 + (z - z0) * s
    dr.rectangle([px, py, px+s-1, py+s-1], fill=col(y))
# contour every 8 y: mark where floor(y/8) changes to a neighbour
for (x, z), (y, _) in cols.items():
    if not (x0 <= x <= x1 and z0 <= z <= z1):
        continue
    for dx, dz in ((1, 0), (0, 1)):
        nb = cols.get((x+dx, z+dz))
        if nb and nb[0] // 8 != y // 8:
            px = 70 + (x - x0) * s + (s if dx else 0)
            py = 30 + (z - z0) * s + (s if dz else 0)
            dr.line([px, py, px, py], fill=(20, 20, 20))
            dr.point([px, py], fill=(0, 0, 0))
            dr.line([px-1, py, px+1, py], fill=(0, 0, 0))
for gx in range(-176, 78, 32):
    px = 70 + (gx - x0) * s
    dr.line([px, 30, px, 30 + (z1-z0+1)*s], fill=(120, 120, 120))
    dr.text((px+2, 12), str(gx), fill=(30, 30, 30), font=font)
for gz in range(-136, 78, 32):
    py = 30 + (gz - z0) * s
    dr.line([70, py, 70 + (x1-x0+1)*s, py], fill=(120, 120, 120))
    dr.text((42, py+1), str(gz), fill=(30, 30, 30), font=font)
# legend
lx, ly = 80, 30 + (z1-z0+1)*s + 4
for i in range(0, 110, 2):
    yy = -64 + i
    dr.rectangle([lx + i*4, ly, lx + i*4 + 4, ly + 14], fill=col(yy))
dr.text((lx, ly + 18), "y -64", fill=(20, 20, 20), font=font)
dr.text((lx + 220, ly + 18), "y 46", fill=(20, 20, 20), font=font)
dr.text((lx - 4, ly - 16), "Height map: colour = top block world y; black ticks = 8-block contour", fill=(20, 20, 20), font=big)
img.save("renders/yeti_height.png")
print("wrote renders/yeti_height.png", img.size)
