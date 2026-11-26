#!/usr/bin/env python3
"""Crop + upscale regions of the reference images for close study (V4-B)."""
import os
import sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.join(os.path.dirname(HERE), "reference")
OUT = os.path.join(HERE, "crops")

CROPS = [
    # name, file, box (l, t, r, b), scale
    ("s1_biosphere", "MCCHubSeason1.png", (200, 500, 600, 820), 2.4),
    ("s1_mountain", "MCCHubSeason1.png", (390, 360, 640, 640), 3.4),
    ("s1_wide_west", "MCCHubSeason1.png", (140, 360, 720, 900), 1.7),
    ("new_mountain", "MCCNewHub.jpg", (540, 80, 820, 420), 3.0),
    ("new_biosphere", "MCCNewHub.jpg", (440, 230, 740, 520), 3.2),
    ("new_rainbow", "MCCNewHub.jpg", (600, 180, 1050, 420), 2.4),
    ("h13_globe", "MCCHub-13.png", (1120, 420, 1600, 900), 2.1),
    ("h13_biosphere", "MCCHub-13.png", (520, 580, 900, 950), 2.6),
    ("h13_mountain", "MCCHub-13.png", (200, 420, 560, 780), 3.0),
    ("hof_sign", "HallOfFame.png", (300, 20, 1250, 780), 1.4),
    ("champs_front", "TheChamps.png", (330, 0, 1550, 760), 1.2),
    ("sd_left", "MCCHub-SD.png", (0, 240, 420, 700), 2.4),
    ("s4_west", "MCCHub-S4.png", (150, 80, 900, 560), 1.9),
    ("s6_west", "MCCHub-S6.png", (0, 0, 800, 700), 1.8),
    # second pass: tighter studies
    ("g_base", "MCCHub-13.png", (1150, 700, 1560, 940), 3.0),
    ("g_castle", "MCCHub-13.png", (1230, 540, 1520, 830), 3.6),
    ("rain_left", "MCCNewHub.jpg", (620, 180, 880, 400), 4.2),
    ("rain_right", "MCCNewHub.jpg", (860, 200, 1120, 380), 4.2),
    ("s1_gate", "MCCHubSeason1.png", (455, 520, 620, 640), 6.0),
    ("hof_letters", "HallOfFame.png", (700, 150, 1300, 700), 2.0),
    ("sd_champs_sign", "MCCHub-SD.png", (0, 100, 210, 470), 5.0),
    ("s1_dome_edge", "MCCHubSeason1.png", (900, 300, 1250, 560), 3.0),
]

def main():
    os.makedirs(OUT, exist_ok=True)
    only = sys.argv[1:] or None
    for name, fn, box, sc in CROPS:
        if only and name not in only:
            continue
        p = os.path.join(REF, fn)
        im = Image.open(p).convert("RGB")
        w, h = im.size
        l, t, r, b = box
        l, t = max(0, l), max(0, t)
        r, b = min(w, r), min(h, b)
        c = im.crop((l, t, r, b))
        nw, nh = int(c.width * sc), int(c.height * sc)
        c = c.resize((nw, nh), Image.LANCZOS)
        o = os.path.join(OUT, name + ".png")
        c.save(o)
        print(f"{name:16s} {fn:20s} src {im.size} box {(l,t,r,b)} -> {nw}x{nh}")

if __name__ == "__main__":
    main()
