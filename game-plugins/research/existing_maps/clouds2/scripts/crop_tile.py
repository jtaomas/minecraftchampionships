#!/usr/bin/env python3
"""Crop the scale-N top overview by world coordinates and label a grid.

usage: crop_tile.py <overview.png> <out.png> <scale> <dump sx> <dump sz>
                    <x0> <x1> <z0> <z1> [upscale]
dump origin fixed at (-258,-316).
"""
import sys
from PIL import Image, ImageDraw, ImageFont

src, dst = sys.argv[1], sys.argv[2]
scale = int(sys.argv[3])
sx, sz = int(sys.argv[4]), int(sys.argv[5])
x0, x1, z0, z1 = map(int, sys.argv[6:10])
up = float(sys.argv[10]) if len(sys.argv) > 10 else 1.0
OX, OZ = -258, -316
im = Image.open(src)
W, H = im.size
m = max(24, int(0.05 * max(sz * scale, sx * scale)))
px = lambda wx: m + (wx - OX) * scale
py = lambda wz: m + (wz - OZ) * scale
box = (int(px(x0)), int(py(z0)), int(px(x1 + 1)), int(py(z1 + 1)))
crop = im.crop(box)
if up != 1.0:
    crop = crop.resize((int(crop.width * up), int(crop.height * up)), Image.LANCZOS)
d = ImageDraw.Draw(crop, "RGBA")
try:
    font = ImageFont.truetype("arial.ttf", 13)
except Exception:
    font = ImageFont.load_default()
w, h = crop.size
step = 10
for gx in range((x0 // step) * step, x1 + 1, step):
    X = (px(gx) - box[0]) * up
    d.line([(X, 0), (X, h)], fill=(255, 0, 0, 55))
    if gx % 50 == 0:
        d.text((X + 1, 2), str(gx), fill=(200, 0, 0, 255), font=font)
for gz in range((z0 // step) * step, z1 + 1, step):
    Y = (py(gz) - box[1]) * up
    d.line([(0, Y), (w, Y)], fill=(255, 0, 0, 55))
    if gz % 50 == 0:
        d.text((2, Y + 1), str(gz), fill=(200, 0, 0, 255), font=font)
crop.save(dst)
print("wrote", dst, crop.size)
