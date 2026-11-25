#!/usr/bin/env python3
"""Vertical section renders (x-y at fixed z) and route elevation profile. Read-only analysis."""
import gzip, os, sys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "world.mcmdump.gz")
OX, OY, OZ = 0, -64, -16
S = 4

COL = {
 'terracotta': (150, 90, 65), 'stone': (125, 125, 125), 'granite': (160, 110, 90),
 'andesite': (140, 140, 140), 'red_sand': (200, 110, 60), 'dirt': (120, 85, 55),
 'brown_concrete_powder': (150, 100, 75), 'white_terracotta': (225, 215, 200),
 'red_terracotta': (150, 60, 45), 'gray_terracotta': (110, 95, 85),
 'diorite': (210, 210, 205), 'polished_granite': (160, 110, 90),
 'soul_sand': (90, 65, 50), 'smooth_stone': (170, 170, 170),
 'light_gray_terracotta': (170, 155, 145), 'cyan_terracotta': (90, 120, 120),
 'quartz_block': (240, 235, 225), 'stone_bricks': (110, 110, 110),
 'gravel': (140, 130, 125), 'red_concrete': (170, 40, 35), 'black_wool': (30, 30, 35),
 'chain': (60, 60, 65), 'barrier': (255, 0, 0), 'acacia_leaves': (70, 120, 40),
 'cactus': (60, 160, 40), 'jungle_planks': (150, 110, 75),
 'nether_brick_wall': (60, 35, 40), 'blackstone': (45, 40, 45),
 'red_wool': (160, 45, 45), 'nether_brick_slab': (60, 35, 40),
 'white_concrete': (235, 235, 235), 'smooth_red_sandstone': (190, 100, 55),
 'black_terracotta': (50, 40, 40), 'gray_concrete': (90, 95, 100),
 'sea_lantern': (200, 230, 220), 'light_gray_concrete': (160, 160, 160),
 'cyan_concrete_powder': (40, 120, 130), 'white_wool': (235, 235, 235),
 'iron_block': (200, 200, 200), 'iron_trapdoor': (170, 170, 170),
 'daylight_detector': (160, 130, 100), 'black_concrete': (20, 20, 25),
 'iron_bars': (120, 120, 120), 'spruce_planks': (110, 80, 50),
 'cyan_concrete': (20, 120, 140), 'cracked_stone_bricks': (100, 100, 100),
 'lantern': (200, 170, 100), 'oak_leaves': (60, 130, 45), 'oak_wall_sign': (220, 200, 140),
}


def main():
    # load only the needed slices
    zs = [0, 8, 15, 31, 45, 60]
    cols = {z: {} for z in zs}
    with gzip.open(SRC, "rt") as f:
        f.readline()
        for line in f:
            p = line.split(" ", 3)
            x = int(p[0]); y = int(p[1]) + OY; z = int(p[2]) + OZ
            if z not in cols or not (0 <= x <= 255 and -64 <= y <= 16):
                continue
            b = p[3].strip().split('[')[0].replace('minecraft:', '')
            cols[z][(x, y)] = b
    x0, x1, y0, y1 = 0, 255, -64, 16
    for z in zs:
        W = (x1 - x0 + 1) * S + 60
        H = (y1 - y0 + 1) * S + 50
        im = Image.new("RGB", (W, H), (18, 20, 24))
        d = ImageDraw.Draw(im)
        offx, offy = 45, 35
        for (x, y), b in cols[z].items():
            px = offx + (x - x0) * S
            py = offy + (y1 - y) * S
            d.rectangle([px, py, px + S - 1, py + S - 1], fill=COL.get(b, (100, 100, 100)))
        # 10-block grid
        for x in range(0, 256, 10):
            px = offx + x * S
            d.line([px, offy, px, H - 15], fill=(255, 255, 255))
            if x % 20 == 0:
                d.text((px - 6, H - 14), str(x), fill=(230, 230, 230))
        for y in range(-64, 17, 10):
            py = offy + (y1 - y) * S
            d.line([offx, py, W - 5, py], fill=(255, 255, 255))
            d.text((4, py - 4), str(y), fill=(230, 230, 230))
        d.text((6, 6), f"SECTION z={z}: x horizontal (0-255), y vertical (-64..16), 4 px/block", fill=(255, 255, 255))
        im.save(os.path.join(HERE, "renders", f"section_z{z}.png"))
        print("wrote", f"section_z{z}.png", im.size)


if __name__ == "__main__":
    main()
