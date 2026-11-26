#!/usr/bin/env python3
"""Second pass: name-tag / sign text zooms."""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.abspath(os.path.join(HERE, "..", "reference"))
OUT = os.path.join(HERE, "crops")

JOBS = [
    ("StageAreaIntro.png", "txt_chat", (0, 900, 1050, 1010), 6),
    ("StageAreaIntro.png", "txt_names", (700, 470, 1320, 640), 6),
    ("MainHub-StageArea.png", "txt_choc", (1140, 375, 1330, 415), 10),
    ("MainHub-StageArea.png", "txt_mountain", (1080, 570, 1270, 610), 10),
    ("MainHub-StageArea.png", "txt_something", (760, 970, 1000, 1010), 10),
    ("MainHub-StageArea.png", "txt_olorzsy", (250, 590, 400, 630), 10),
    ("StageAreaBuilding.png", "txt_wibbade", (510, 295, 700, 335), 10),
    ("StageAreaBuilding.png", "txt_byrt", (1240, 305, 1400, 345), 10),
    ("StageAreaAnC.png", "txt_tomeroo", (1460, 695, 1640, 735), 10),
    ("StageAreaAnC.png", "txt_dwittyy", (540, 30, 700, 70), 10),
    ("StageAreaAnC.png", "txt_turnjus", (300, 285, 500, 325), 10),
    ("StageAreaAnC.png", "txt_oreo", (10, 570, 140, 610), 10),
    ("StageAreaPride.png", "txt_plantskid", (1050, 430, 1220, 470), 10),
    # plots / columns structure
    ("MCCHubSeason1.png", "s1_cols_zoom", (230, 660, 700, 1000), 3),
    ("MCCHubSeason1.png", "s1_cols_zoom2", (500, 620, 980, 1010), 3),
    ("MCCHubSeason1.png", "s1_east_zoom", (1150, 560, 1500, 820), 3),
    ("MCCHub-13.png", "h13_cols", (0, 600, 900, 1080), 2),
    ("MCCNewHub.jpg", "new_cols", (0, 0, 960, 600), 2),
]


def main():
    for src, name, box, scale in JOBS:
        im = Image.open(os.path.join(REF, src)).convert("RGB")
        w, h = im.size
        l, t, r, b = box
        c = im.crop((max(0, l), max(0, t), min(w, r), min(h, b)))
        c = c.resize((c.width * scale, c.height * scale), Image.LANCZOS)
        c.save(os.path.join(OUT, name + ".png"))
        print(name, c.size)


if __name__ == "__main__":
    main()
