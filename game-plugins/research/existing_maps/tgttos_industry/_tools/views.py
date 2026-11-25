#!/usr/bin/env python3
"""Draw labelled elevations / floor plans of the Industry world from blocks_xyz.csv."""
import csv
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp

DATA = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data\blocks_xyz.csv"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\renders"

SKIP = {"minecraft:barrier"}

rows = []
with open(DATA, encoding="utf-8") as f:
    for d in csv.DictReader(f):
        if d["block"].split("[")[0] in SKIP:
            continue
        rows.append((int(d["x"]), int(d["y"]), int(d["z"]), d["block"]))

COLS = {}


def colour(block):
    if block not in COLS:
        spec = rp.resolve_block(block)
        COLS[block] = (spec[1] if spec else (0, 0, 0))
    return COLS[block]


def font(sz):
    for name in ("consola.ttf", "arial.ttf"):
        try:
            return ImageFont.truetype(name, sz)
        except Exception:
            pass
    return ImageFont.load_default()


def draw_axes(d, ox, oy, w, h, s, xlabel, ylabel, x0, x1, y0, y1, step=10):
    f = font(13)
    # grid
    for gx in range(x0 - x0 % step, x1 + 1, step):
        px = ox + (gx - x0) * s
        d.line([(px, oy), (px, oy + h)], fill=(255, 255, 255, 40), width=1)
        d.text((px + 2, oy + h + 2), str(gx), fill=(30, 40, 55), font=f)
    for gy in range(y0 - y0 % step, y1 + 1, step):
        py = oy + h - (gy - y0) * s
        d.line([(ox, py), (ox + w, py)], fill=(255, 255, 255, 40), width=1)
        d.text((2, py - 7), str(gy), fill=(30, 40, 55), font=f)
    d.text((ox + w // 2, oy + h + 20), xlabel, fill=(20, 25, 35), font=font(15))
    d.text((3, 3), ylabel, fill=(20, 25, 35), font=font(15))


def elevation(axis, direction, path, title, step=10):
    """axis 'z': plane y/z, frontmost by x. axis 'x': plane x/y, frontmost by z."""
    if axis == "z":
        key = lambda r: (r[2], r[1])
        depth = lambda r: r[0]
        x0 = min(r[2] for r in rows); x1 = max(r[2] for r in rows)
        y0 = min(r[1] for r in rows); y1 = max(r[1] for r in rows)
        xlabel, ylabel = "z", "y"
    else:
        key = lambda r: (r[0], r[1])
        depth = lambda r: r[2]
        x0 = min(r[0] for r in rows); x1 = max(r[0] for r in rows)
        y0 = min(r[1] for r in rows); y1 = max(r[1] for r in rows)
        xlabel, ylabel = "x", "y"
    best = {}
    for r in rows:
        k = key(r)
        v = depth(r)
        if k not in best or (v > best[k][0] if direction > 0 else v < best[k][0]):
            best[k] = (v, r[3])
    s = 8
    w = (x1 - x0 + 1) * s
    h = (y1 - y0 + 1) * s
    ml, mt = 46, 30
    im = Image.new("RGB", (w + ml + 20, h + mt + 40), (244, 247, 250))
    d = ImageDraw.Draw(im, "RGBA")
    for (a, b), (_, block) in best.items():
        px = ml + (a - x0) * s
        py = mt + (y1 - b) * s
        d.rectangle([px, py, px + s - 1, py + s - 1], fill=colour(block))
    draw_axes(d, ml, mt, w, h, s, xlabel, ylabel, x0, x1, y0, y1, step)
    d.text((ml, 6), title, fill=(10, 20, 30), font=font(16))
    im.save(path)
    print("wrote", path, im.size)


def plan(y0, y1, path, title, step=10):
    sel = [r for r in rows if y0 <= r[1] <= y1]
    best = {}
    for r in sel:
        k = (r[0], r[2])
        if k not in best or r[1] > best[k][0]:
            best[k] = (r[1], r[3])
    xs = [r[0] for r in sel]; zs = [r[2] for r in sel]
    x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
    s = 8
    ml, mt = 46, 30
    w = (x1 - x0 + 1) * s
    h = (z1 - z0 + 1) * s
    im = Image.new("RGB", (w + ml + 20, h + mt + 40), (244, 247, 250))
    d = ImageDraw.Draw(im, "RGBA")
    for (x, z), (y, block) in best.items():
        px = ml + (x - x0) * s
        py = mt + (z - z0) * s
        d.rectangle([px, py, px + s - 1, py + s - 1], fill=colour(block))
    # axes: x right, z down
    f = font(13)
    for gx in range(x0 - x0 % step, x1 + 1, step):
        px = ml + (gx - x0) * s
        d.line([(px, mt), (px, mt + h)], fill=(255, 255, 255, 40))
        d.text((px + 2, mt + h + 2), str(gx), fill=(30, 40, 55), font=f)
    for gz in range(z0 - z0 % step, z1 + 1, step):
        py = mt + (gz - z0) * s
        d.line([(ml, py), (ml + w, py)], fill=(255, 255, 255, 40))
        d.text((2, py - 7), str(gz), fill=(30, 40, 55), font=f)
    d.text((ml + w // 2, mt + h + 20), "x", fill=(20, 25, 35), font=font(15))
    d.text((3, 3), "z (down)", fill=(20, 25, 35), font=font(15))
    d.text((ml, 6), title, fill=(10, 20, 30), font=font(16))
    im.save(path)
    print("wrote", path, im.size)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    elevation("z", +1, os.path.join(OUT, "elev_z_y.png"), "Elevation: z vs y (looking from +x, frontmost = max x)")
    elevation("x", +1, os.path.join(OUT, "elev_x_y.png"), "Elevation: x vs y (looking from +z, frontmost = max z)")
    plan(-40, -20, os.path.join(OUT, "plan_y-40_-20.png"), "Plan y=-40..-20 (lowest occupied level)")
    plan(-19, -7, os.path.join(OUT, "plan_y-19_-7.png"), "Plan y=-19..-7 (middle void level)")
    plan(-6, 1, os.path.join(OUT, "plan_y-6_1.png"), "Plan y=-6..1")
    plan(2, 7, os.path.join(OUT, "plan_y2_7.png"), "Plan y=2..7 (upper level)")
    plan(8, 11, os.path.join(OUT, "plan_y8_11.png"), "Plan y=8..11 (roof)")
