#!/usr/bin/env python3
"""Vertical cross-sections (x/y or z/y) coloured with render_preview's palette."""
import gzip
import sys
import importlib.util
from collections import defaultdict

import numpy as np
from PIL import Image, ImageDraw, ImageFont

spec = importlib.util.spec_from_file_location("rp", r"C:\Users\LeonS\Desktop\Mcc\tools\render_preview.py")
rp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rp)

SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
SCALE = 2

def main():
    axis = sys.argv[1]          # "z" slice at fixed z, or "x" slice at fixed x
    fixed = int(sys.argv[2])
    a0, a1 = int(sys.argv[3]), int(sys.argv[4])
    y0, y1 = int(sys.argv[5]), int(sys.argv[6])
    dst = sys.argv[7]
    blocks = {}
    with gzip.open(SRC, "rt", encoding="utf-8") as fh:
        fh.readline()
        for line in fh:
            p = line.split(" ", 3)
            if len(p) < 4: continue
            x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
            if axis == "z":
                if z != fixed or not (a0 <= x <= a1 and y0 <= y <= y1): continue
                key = (x, y)
            else:
                if x != fixed or not (a0 <= z <= a1 and y0 <= y <= y1): continue
                key = (z, y)
            # later blocks overwrite; iterate y ascending so higher wins? keep highest
            if key not in blocks or y > blocks[key][0]:
                blocks[key] = (y, p[3].strip())

    W = (a1 - a0 + 1) * SCALE
    H = (y1 - y0 + 1) * SCALE
    im = Image.new("RGB", (W, H), (18, 20, 28))
    px = im.load()
    for (a, y), (yy, name) in blocks.items():
        spec = rp.resolve_block(name)
        if spec is None:
            continue
        top, side, alpha, emit, solid, strip = spec
        c = top if y == 255 else side
        if alpha < 0.9:
            c = tuple(int(c[i] * alpha + 18 * (1 - alpha)) for i in range(3))
        ix = (a - a0) * SCALE
        iy = (y1 - y) * SCALE
        for dy in range(SCALE):
            for dx in range(SCALE):
                if 0 <= ix + dx < W and 0 <= iy + dy < H:
                    px[ix + dx, iy + dy] = c
    d = ImageDraw.Draw(im, "RGBA")
    try:
        font = ImageFont.truetype("arial.ttf", 12)
    except Exception:
        font = ImageFont.load_default()
    for a in range(a0 - a0 % 20, a1 + 1, 20):
        X = (a - a0) * SCALE
        d.line([(X, 0), (X, H)], fill=(255, 0, 0, 60))
        d.text((X + 1, 2), str(a), fill=(255, 90, 90, 255), font=font)
    for y in range(y0 - y0 % 10, y1 + 1, 10):
        Y = (y1 - y) * SCALE
        d.line([(0, Y), (W, Y)], fill=(255, 0, 0, 60))
        d.text((2, Y + 1), str(y), fill=(255, 90, 90, 255), font=font)
    im.save(dst)
    print("wrote", dst, im.size, "blocks", len(blocks))

if __name__ == "__main__":
    main()
