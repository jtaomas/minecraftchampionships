#!/usr/bin/env python3
"""Height/surface maps of the Badlands dump. Read-only analysis; writes to this folder."""
import gzip, os
from collections import Counter, defaultdict
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "world.mcmdump.gz")
OX, OY, OZ = 0, -64, -16

# y -> colour ramp (deep to high)
def hcol(y):
    # -64..12
    t = max(0.0, min(1.0, (y + 64) / 76.0))
    # blue-grey low, orange mid, pale high
    stops = [(0.0, (40, 55, 75)), (0.25, (120, 80, 55)), (0.55, (190, 110, 55)),
             (0.8, (225, 160, 95)), (1.0, (245, 225, 190))]
    for i in range(len(stops) - 1):
        a, ca = stops[i]; b, cb = stops[i + 1]
        if a <= t <= b:
            f = (t - a) / (b - a)
            return tuple(int(ca[k] + (cb[k] - ca[k]) * f) for k in range(3))
    return stops[-1][1]

MATCOL = {
    "terracotta": (150, 90, 65), "stone": (125, 125, 125), "granite": (160, 110, 90),
    "andesite": (140, 140, 140), "red_sand": (200, 110, 60), "dirt": (120, 85, 55),
    "brown_concrete_powder": (150, 100, 75), "white_terracotta": (225, 215, 200),
    "red_terracotta": (150, 60, 45), "gray_terracotta": (110, 95, 85),
    "diorite": (210, 210, 205), "polished_granite": (160, 110, 90),
    "soul_sand": (90, 65, 50), "smooth_stone": (170, 170, 170),
    "light_gray_terracotta": (170, 155, 145), "cyan_terracotta": (90, 120, 120),
    "quartz_block": (240, 235, 225), "stone_bricks": (110, 110, 110),
    "gravel": (140, 130, 125), "red_concrete": (170, 40, 35), "black_wool": (35, 35, 40),
    "chain": (60, 60, 65), "barrier": (255, 0, 0), "acacia_leaves": (70, 120, 40),
    "cactus": (60, 160, 40), "jungle_planks": (150, 110, 75),
    "nether_brick_wall": (60, 35, 40), "blackstone": (45, 40, 45),
    "red_wool": (160, 45, 45), "nether_brick_slab": (60, 35, 40),
    "white_concrete": (235, 235, 235), "smooth_red_sandstone": (190, 100, 55),
    "black_terracotta": (50, 40, 40), "gray_concrete": (90, 95, 100),
    "sea_lantern": (200, 230, 220), "light_gray_concrete": (160, 160, 160),
    "cyan_concrete_powder": (40, 120, 130), "white_wool": (235, 235, 235),
    "iron_block": (200, 200, 200), "iron_trapdoor": (170, 170, 170),
    "daylight_detector": (160, 130, 100), "black_concrete": (20, 20, 25),
    "iron_bars": (120, 120, 120), "spruce_planks": (110, 80, 50),
    "cyan_concrete": (20, 120, 140), "cracked_stone_bricks": (100, 100, 100),
    "lantern": (200, 170, 100), "gold_block": (240, 200, 60),
}


def main():
    tops = {}
    blocks = defaultdict(set)
    allb = defaultdict(Counter)
    with gzip.open(SRC, "rt") as f:
        f.readline()
        for line in f:
            p = line.split(" ", 3)
            x = int(p[0]); y = int(p[1]) + OY; z = int(p[2]) + OZ
            b = p[3].strip().split("[")[0].replace("minecraft:", "")
            key = (x, z)
            if key not in tops or y > tops[key][0]:
                tops[key] = (y, b)
            allb[key][b] += 1
            blocks[key].add(b)

    xs = [k[0] for k in tops]; zs = [k[1] for k in tops]
    x0, x1 = min(xs), max(xs); z0, z1 = min(zs), max(zs)
    print("columns", len(tops), "x", x0, x1, "z", z0, z1)
    S = 2
    W, H = (x1 - x0 + 1) * S + 120, (z1 - z0 + 1) * S + 60
    im = Image.new("RGB", (W, H), (24, 26, 32))
    d = ImageDraw.Draw(im)
    offx, offy = 90, 40
    for (x, z), (y, b) in tops.items():
        d.rectangle([offx + (x - x0) * S, offy + (z - z0) * S,
                     offx + (x - x0) * S + S - 1, offy + (z - z0) * S + S - 1], fill=hcol(y))
    # grid every 10
    for x in range((x0 // 10) * 10, x1 + 1, 10):
        px = offx + (x - x0) * S
        d.line([px, offy, px, offy + (z1 - z0 + 1) * S], fill=(255, 255, 255, 40))
        d.text((px + 1, offy - 12), str(x), fill=(230, 230, 230))
    for z in range((z0 // 10) * 10, z1 + 1, 10):
        pz = offy + (z - z0) * S
        d.line([offx, pz, offx + (x1 - x0 + 1) * S, pz], fill=(255, 255, 255, 40))
        d.text((offx - 28, pz - 3), str(z), fill=(230, 230, 230))
    # legend
    d.text((6, 6), "TOP NON-AIR Y  (x horizontal, z vertical)", fill=(255, 255, 255))
    for i in range(0, 77):
        yv = -64 + i
        d.rectangle([8, H - 30 - i * 2, 20, H - 30 - i * 2 + 2], fill=hcol(yv))
    d.text((24, H - 90), "y=12", fill=(220, 220, 220))
    d.text((24, H - 30), "y=-64", fill=(220, 220, 220))
    im.save(os.path.join(HERE, "renders", "heightmap_top.png"))
    print("wrote renders/heightmap_top.png", im.size)

    # surface material map
    im2 = Image.new("RGB", (W, H), (24, 26, 32))
    d2 = ImageDraw.Draw(im2)
    for (x, z), (y, b) in tops.items():
        c = MATCOL.get(b, (100, 100, 100))
        d2.rectangle([offx + (x - x0) * S, offy + (z - z0) * S,
                      offx + (x - x0) * S + S - 1, offy + (z - z0) * S + S - 1], fill=c)
    for x in range((x0 // 10) * 10, x1 + 1, 10):
        px = offx + (x - x0) * S
        d2.text((px + 1, offy - 12), str(x), fill=(230, 230, 230))
    for z in range((z0 // 10) * 10, z1 + 1, 10):
        pz = offy + (z - z0) * S
        d2.text((offx - 28, pz - 3), str(z), fill=(230, 230, 230))
    d2.text((6, 6), "TOP BLOCK MATERIAL", fill=(255, 255, 255))
    im2.save(os.path.join(HERE, "renders", "surface_top.png"))
    print("wrote renders/surface_top.png", im2.size)

    # coarse ASCII: 5x5 columns with max height and dominant material initial
    for z in range(z0, z1 + 1, 5):
        row = []
        for x in range(x0, x1 + 1, 5):
            cell = [tops[(xx, zz)] for xx in range(x, x + 5) for zz in range(z, z + 5) if (xx, zz) in tops]
            if not cell:
                row.append("    ")
                continue
            ymax = max(c[0] for c in cell)
            ymin = min(c[0] for c in cell)
            row.append(f"{ymax:3d}{'.' if ymax-ymin<3 else '^'}")
        print(f"z{z:4d} " + " ".join(row))


if __name__ == "__main__":
    main()
