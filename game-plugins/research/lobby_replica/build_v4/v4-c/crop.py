#!/usr/bin/env python3
"""Crop + upscale regions of the reference screenshots for detail study."""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.abspath(os.path.join(HERE, "..", "reference"))
OUT = os.path.join(HERE, "crops")

JOBS = [
    # (src, name, box(l,t,r,b), scale)
    ("MCCHub-S6.png", "s6_columns_left", (150, 760, 700, 1080), 3),
    ("MCCHub-S6.png", "s6_columns_right", (1250, 740, 1920, 1080), 3),
    ("MCCHub-SD.png", "sd_columns_left", (80, 700, 620, 1080), 3),
    ("MCCHub-SD.png", "sd_columns_right", (1330, 690, 1920, 1080), 3),
    ("MainHub.png", "mh_banners_left", (0, 0, 480, 700), 2),
    ("MainHub.png", "mh_banners_mid", (420, 0, 1000, 420), 2),
    ("MainHub.png", "mh_banners_right", (940, 0, 1920, 700), 2),
    ("MainHub-StageArea.png", "sa_plots_left", (150, 250, 800, 620), 2),
    ("MainHub-StageArea.png", "sa_plots_right", (1050, 330, 1920, 700), 2),
    ("MainHub-StageArea.png", "sa_blue_heads", (400, 300, 780, 560), 3),
    ("MainHub-StageArea.png", "sa_lime_plot", (1250, 300, 1750, 620), 3),
    ("MainHub-StageArea.png", "sa_centre_path", (760, 300, 1350, 700), 2),
    ("StageAreaAnC.png", "anc_heads_purple", (150, 530, 850, 830), 2),
    ("StageAreaAnC.png", "anc_heads_pink", (900, 630, 1750, 900), 2),
    ("StageAreaAnC.png", "anc_wall", (100, 250, 1300, 700), 1),
    ("StageAreaPride.png", "pride_heads", (300, 330, 1300, 900), 2),
    ("StageAreaPride.png", "pride_arch", (200, 0, 1300, 700), 1),
    ("StageAreaTwitch.png", "twitch_plot", (150, 100, 1150, 700), 2),
    ("StageAreaTwitch.png", "twitch_head", (250, 450, 420, 640), 5),
    ("StageAreaBuilding.png", "bld_heads", (780, 330, 1300, 720), 3),
    ("StageAreaBuilding.png", "bld_plot", (180, 120, 1700, 780), 1),
    ("MCCHubSeason1.png", "s1_foreground", (200, 640, 1000, 1000), 2),
    ("MCCHubSeason1.png", "s1_ring", (900, 600, 1500, 850), 2),
    ("MCCHubSeason1.png", "s1_east", (1150, 400, 1920, 800), 2),
    ("MCCHub-13.png", "h13_wide", (0, 0, 1920, 1080), 1),
    ("MCCNewHub.jpg", "new_wide", (0, 0, 1920, 1080), 1),
    ("StageAreaIntro.png", "intro_stage", (200, 60, 1500, 900), 1),
]


def main():
    os.makedirs(OUT, exist_ok=True)
    for src, name, box, scale in JOBS:
        path = os.path.join(REF, src)
        if not os.path.exists(path):
            print("missing", src)
            continue
        im = Image.open(path).convert("RGB")
        w, h = im.size
        l, t, r, b = box
        l, t = max(0, l), max(0, t)
        r, b = min(w, r), min(h, b)
        c = im.crop((l, t, r, b))
        if scale != 1:
            c = c.resize((c.width * scale, c.height * scale), Image.LANCZOS)
        dst = os.path.join(OUT, name + ".png")
        c.save(dst)
        print(f"{name}.png  {c.width}x{c.height}  <- {src}{box}")


if __name__ == "__main__":
    main()
