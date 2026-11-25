#!/usr/bin/env python3
"""Labelled top-down route map and x/y route profile for the Badlands dossier."""
import gzip, os
from collections import defaultdict
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "world.mcmdump.gz")
OX, OY, OZ = 0, -64, -16


def hcol(y):
    t = max(0.0, min(1.0, (y + 64) / 76.0))
    stops = [(0.0, (40, 55, 75)), (0.25, (120, 80, 55)), (0.55, (190, 110, 55)),
             (0.8, (225, 160, 95)), (1.0, (245, 225, 190))]
    for i in range(len(stops) - 1):
        a, ca = stops[i]; b, cb = stops[i + 1]
        if a <= t <= b:
            f = (t - a) / (b - a)
            return tuple(int(ca[k] + (cb[k] - ca[k]) * f) for k in range(3))
    return stops[-1][1]


def main():
    tops = {}
    floors = {}
    with gzip.open(SRC, "rt") as f:
        f.readline()
        for line in f:
            p = line.split(" ", 3)
            x = int(p[0]); y = int(p[1]) + OY; z = int(p[2]) + OZ
            b = p[3].strip().split("[")[0].replace("minecraft:", "")
            key = (x, z)
            if key not in tops or y > tops[key][0]:
                tops[key] = (y, b)
            if b not in ("acacia_leaves", "oak_leaves", "cactus", "chain", "lantern", "barrier"):
                if key not in floors or y > floors[key][0]:
                    floors[key] = (y, b)

    # ---------------- labelled top-down ----------------
    S = 5
    x0, x1, z0, z1 = 0, 255, -8, 72
    W, H = (x1 - x0 + 1) * S + 300, (z1 - z0 + 1) * S + 90
    im = Image.new("RGB", (W, H), (20, 22, 27))
    d = ImageDraw.Draw(im)
    offx, offy = 70, 55
    for (x, z), (y, b) in tops.items():
        if not (x0 <= x <= x1 and z0 <= z <= z1):
            continue
        d.rectangle([offx + (x - x0) * S, offy + (z - z0) * S,
                     offx + (x - x0) * S + S - 1, offy + (z - z0) * S + S - 1], fill=hcol(y))
    for x in range(0, 256, 20):
        px = offx + (x - x0) * S
        d.line([px, offy, px, offy + (z1 - z0 + 1) * S], fill=(255, 255, 255))
        d.text((px + 2, offy - 14), str(x), fill=(235, 235, 235))
    for z in range(0, 72, 10):
        pz = offy + (z - z0) * S
        d.line([offx, pz, offx + (x1 - x0 + 1) * S, pz], fill=(255, 255, 255))
        d.text((offx - 30, pz - 4), str(z), fill=(235, 235, 235))
    # route line (schematic centre line)
    pts = [(10, 31), (30, 31), (62, 30), (90, 30), (120, 30), (145, 30), (160, 26), (185, 26), (200, 30), (225, 30), (240, 31)]
    pp = [(offx + (x - x0) * S, offy + (z - z0) * S) for x, z in pts]
    for i in range(len(pp) - 1):
        d.line([pp[i], pp[i + 1]], fill=(0, 220, 255), width=3)
    labels = [
        (10, 31, "START PEN\n10,-42,31", (2, 67)),
        (64, 30, "MINE WEST GATE\nMCC billboard x=64", (18, 2)),
        (100, 30, "MINE PLAZA\nx 82..118", (78, 63)),
        (137, 30, "BLACK RAIL / WALL\nx=137..139", (112, 57)),
        (160, 26, "CAVE MOUTH\n~158,-20,31", (142, 67)),
        (185, 26, "CAVE PILLARS\nx 182..189", (172, 62)),
        (210, 30, "FINISH CANYON\nfloor -21..-5", (196, 2)),
        (241, 31, "CHICKEN LEDGE\n241,-1, z 11..52", (214, 57)),
    ]
    for x, z, txt, (lx, lz) in labels:
        px = offx + (x - x0) * S + S // 2
        py = offy + (z - z0) * S + S // 2
        d.ellipse([px - 5, py - 5, px + 5, py + 5], fill=(255, 90, 40), outline=(255, 255, 255))
        tx, ty = offx + (lx - x0) * S, offy + (lz - z0) * S
        d.line([px, py, tx, ty], fill=(255, 90, 40), width=2)
        tw = d.multiline_textbbox((tx + 4, ty), txt, spacing=2)
        d.rectangle([tw[0]-3, tw[1]-2, tw[2]+3, tw[3]+2], fill=(10, 10, 14))
        d.multiline_text((tx + 4, ty), txt, fill=(255, 240, 210), spacing=2)
    d.text((10, 16), "TGTTOS BADLANDS - TOP NON-AIR BLOCK (5 px/block) + schematic centre line", fill=(255, 255, 255))
    # legend
    lx, ly = W - 210, 60
    d.text((lx, ly - 18), "height", fill=(240, 240, 240))
    for i in range(0, 77):
        yv = -64 + i
        d.rectangle([lx, ly + (76 - i) * 3, lx + 16, ly + (76 - i) * 3 + 3], fill=hcol(yv))
    d.text((lx + 22, ly + 76 * 3 - 12), "y=12", fill=(230, 230, 230))
    d.text((lx + 22, ly), "y=-64", fill=(230, 230, 230))
    im.save(os.path.join(HERE, "renders", "route_top_annotated.png"))
    print("wrote route_top_annotated.png", im.size)

    # ---------------- route profile at z=31 ----------------
    z = 31
    prof = []
    for x in range(0, 256):
        t = floors.get((x, z), tops.get((x, z)))
        prof.append(t[0] if t else None)
    W2, H2 = 1600, 520
    im2 = Image.new("RGB", (W2, H2), (18, 20, 25))
    d2 = ImageDraw.Draw(im2)
    left, right, top, bot = 70, W2 - 30, 40, H2 - 60
    ymin, ymax = -46, 6

    def px(x):
        return left + (x / 255.0) * (right - left)

    def py(y):
        return bot - ((y - ymin) / (ymax - ymin)) * (bot - top)

    # leg bands
    legs = [(4, 12, "START", (80, 80, 90)), (12, 62, "LEG 1  badlands slope", (120, 70, 45)),
            (62, 120, "LEG 2  mine / rail yard", (90, 90, 110)), (120, 150, "LEG 3  canyon", (110, 60, 60)),
            (150, 195, "LEG 4  cave under mesa", (60, 80, 110)), (195, 240, "LEG 5  finish canyon", (130, 80, 60)),
            (240, 250, "FIN", (60, 130, 60))]
    for a, b, name, col in legs:
        d2.rectangle([px(a), top, px(b), bot], fill=col)
        d2.text((px(a) + 4, top + 4), name, fill=(255, 255, 255))
    for y in range(-44, 7, 4):
        d2.line([left, py(y), right, py(y)], fill=(70, 70, 75))
        d2.text((8, py(y) - 5), str(y), fill=(220, 220, 220))
    for x in range(0, 256, 20):
        d2.line([px(x), top, px(x), bot], fill=(70, 70, 75))
        d2.text((px(x) - 8, bot + 6), str(x), fill=(220, 220, 220))
    # draw area under floor
    for x in range(0, 255):
        if prof[x] is None or prof[x + 1] is None:
            continue
        d2.polygon([(px(x), py(prof[x])), (px(x + 1), py(prof[x + 1])),
                    (px(x + 1), py(-46)), (px(x), py(-46))], fill=(160, 110, 70))
    # roof line (top non-air) thin
    for x in range(0, 255):
        a = tops.get((x, z)); b = tops.get((x + 1, z))
        if a and b:
            d2.line([px(x), py(a[0]), px(x + 1), py(b[0])], fill=(255, 230, 160), width=2)
    # markers
    for x, y, txt in [(10, -42, "start"), (64, -14, "MCC"), (137, -26, "rail"), (160, -20, "cave"),
                      (185, -20, "pillars"), (241, -1, "chickens")]:
        d2.ellipse([px(x) - 5, py(y) - 5, px(x) + 5, py(y) + 5], fill=(255, 80, 40), outline=(255, 255, 255))
        d2.text((px(x) + 7, py(y) - 14), txt, fill=(255, 240, 200))
    d2.text((10, 12), "ROUTE PROFILE at z=31: floor (brown fill) and top non-air/roof (yellow line), x 0..255, y -46..6", fill=(255, 255, 255))
    d2.text((left, H2 - 22), "x", fill=(230, 230, 230))
    d2.text((10, 24), "y", fill=(230, 230, 230))
    im2.save(os.path.join(HERE, "renders", "route_profile_z31.png"))
    print("wrote route_profile_z31.png", im2.size)


if __name__ == "__main__":
    main()
