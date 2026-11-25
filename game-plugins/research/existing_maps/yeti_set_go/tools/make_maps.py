#!/usr/bin/env python3
"""Custom annotated top-down maps of the Yeti Set Go dump (analysis only).

Reads data/yeti_full.mcmdump.gz and writes PNGs with world-coordinate
grids so route coordinates can be read directly. Colors follow broad
material families, not the shared renderer's palette.
"""
import gzip
import os
import sys
from collections import defaultdict

from PIL import Image, ImageDraw, ImageFont

COLOURS = {
    "water": (70, 120, 210), "bubble_column": (120, 170, 235),
    "ice": (170, 215, 245), "packed_ice": (150, 200, 240), "blue_ice": (120, 175, 235),
    "snow_block": (245, 248, 252), "snow": (238, 244, 250), "powder_snow": (228, 236, 245),
    "stone": (120, 120, 125), "andesite": (140, 140, 140), "cobblestone": (105, 105, 105),
    "stone_bricks": (115, 115, 118), "smooth_quartz": (235, 232, 225), "quartz": (230, 228, 222),
    "gray_concrete": (75, 78, 82), "black_concrete": (20, 20, 24), "white_concrete": (225, 226, 228),
    "yellow_glazed_terracotta": (240, 200, 60), "pink_glazed_terracotta": (235, 140, 170),
    "magenta_glazed_terracotta": (200, 80, 170), "red_glazed_terracotta": (190, 60, 50),
    "lime_wool": (120, 220, 60), "yellow_wool": (245, 210, 70), "yellow_carpet": (242, 205, 65),
    "lodestone": (90, 90, 95), "barrier": (255, 0, 0),
    "spruce_planks": (110, 80, 50), "mangrove_planks": (125, 65, 60), "dark_oak_planks": (75, 50, 30),
    "oak_planks": (160, 125, 75), "spruce_slab": (110, 80, 50), "stripped_spruce_wood": (140, 110, 75),
    "grass": (110, 160, 70), "dirt": (120, 90, 60), "podzol": (100, 75, 45),
    "leaves": (60, 120, 50), "spruce_leaves": (50, 100, 45),
    "glass": (200, 230, 240), "lantern": (255, 210, 120), "sea_lantern": (210, 240, 235),
    "red_concrete": (170, 40, 40), "blue_concrete": (50, 70, 170), "cyan_concrete": (30, 130, 150),
    "green_terracotta": (75, 90, 60), "terracotta": (150, 90, 65),
}


def colour_for(name):
    base = name.split("[")[0].replace("minecraft:", "")
    if base in COLOURS:
        return COLOURS[base]
    for key, col in COLOURS.items():
        if key in base:
            return col
    if "wool" in base or "carpet" in base or "concrete" in base or "terracotta" in base:
        return (180, 150, 160)
    if "leaves" in base or "grass" in base or "moss" in base:
        return (80, 140, 60)
    if "glass" in base or "pane" in base:
        return (190, 220, 235)
    if "planks" in base or "wood" in base or "log" in base:
        return (120, 90, 60)
    return (150, 150, 150)


def load(path):
    cols = {}
    with gzip.open(path, "rt", encoding="utf-8") as fh:
        header = fh.readline().split()
        sx, sy, sz = int(header[2]), int(header[3]), int(header[4])
        for line in fh:
            x, y, z, name = line.split(" ", 3)
            x, y, z = int(x), int(y), int(z)
            cur = cols.get((x, z))
            if cur is None or y > cur[0]:
                cols[(x, z)] = (y, name.strip())
    return sx, sy, sz, cols


def render(cols, x0, x1, z0, z1, scale, out, title, marks=None):
    w = (x1 - x0 + 1) * scale
    h = (z1 - z0 + 1) * scale
    img = Image.new("RGB", (w + 70, h + 50), (232, 236, 242))
    dr = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 13)
        small = ImageFont.truetype("arial.ttf", 10)
    except Exception:  # noqa: BLE001
        font = ImageFont.load_default()
        small = font
    ys = [v[0] for v in cols.values()]
    ymin, ymax = min(ys), max(ys)
    for (x, z), (y, name) in cols.items():
        if not (x0 <= x <= x1 and z0 <= z <= z1):
            continue
        r, g, b = colour_for(name)
        shade = 0.72 + 0.28 * ((y - ymin) / max(1, ymax - ymin))
        r, g, b = int(r * shade), int(g * shade), int(b * shade)
        px = 70 + (x - x0) * scale
        py = 30 + (z - z0) * scale
        dr.rectangle([px, py, px + scale - 1, py + scale - 1], fill=(r, g, b))
    # grid every 16 blocks, strong every 64
    for gx in range((x0 // 16) * 16, x1 + 1, 16):
        if gx < x0:
            continue
        px = 70 + (gx - x0) * scale
        strong = gx % 64 == 0
        dr.line([px, 30, px, 30 + h], fill=(0, 0, 0, 90) if strong else (120, 120, 120), width=1)
        if strong or scale >= 4:
            dr.text((px + 2, 4), str(gx), fill=(20, 20, 20), font=small)
    for gz in range((z0 // 16) * 16, z1 + 1, 16):
        if gz < z0:
            continue
        py = 30 + (gz - z0) * scale
        strong = gz % 64 == 0
        dr.line([70, py, 70 + w, py], fill=(0, 0, 0, 90) if strong else (120, 120, 120), width=1)
        if strong or scale >= 4:
            dr.text((4, py + 1), str(gz), fill=(20, 20, 20), font=small)
    if marks:
        for label, mx, mz, col in marks:
            px = 70 + (mx - x0) * scale
            py = 30 + (mz - z0) * scale
            dr.ellipse([px - 5, py - 5, px + 5, py + 5], outline=col, width=2)
            dr.text((px + 7, py - 6), label, fill=col, font=font)
    dr.text((70, h + 34), title, fill=(20, 20, 20), font=font)
    img.save(out)
    print(f"wrote {out} {img.size} range x {x0}..{x1} z {z0}..{z1}")


def main():
    sx, sy, sz, cols = load("data/yeti_full.mcmdump.gz")
    # global bounds are -182..77 x, -140..77 z; dump is offset, rebuild world coords
    # dump coordinates start at 0 -> add origin -182,-64,-140
    ox, oz = -182, -140
    world = {((x + ox), (z + oz)): v for (x, z), v in cols.items()}
    render(world, -182, 77, -140, 77, 4, "renders/yeti_top_grid.png",
           "Yeti Set Go - top-down, 4 px/block, labels = world x/z")
    render(world, 2, 45, -20, 25, 12, "renders/z_start.png",
           "Start cluster x 2..45 z -20..25")
    render(world, -175, -135, 10, 50, 10, "renders/z_checkpoint.png",
           "West cluster x -175..-135 z 10..50")
    render(world, -80, 0, -60, 20, 8, "renders/z_mid.png",
           "Middle x -80..0 z -60..20")
    render(world, -40, 77, -140, -70, 8, "renders/z_north.png",
           "North-east snow x -40..77 z -140..-70")


if __name__ == "__main__":
    main()
