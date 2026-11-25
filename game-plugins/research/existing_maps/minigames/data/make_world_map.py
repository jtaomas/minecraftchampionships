#!/usr/bin/env python3
"""Labelled top-down world map: true-scale main cluster + schematic remote clusters."""
import csv
import os
from collections import defaultdict
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "renders", "world_map.png")
FB = "C:/Windows/Fonts/arialbd.ttf"
FR = "C:/Windows/Fonts/arial.ttf"

rows = list(csv.DictReader(open(os.path.join(ROOT, "data", "blocks_positions.csv"), encoding="utf-8")))
blocks = defaultdict(list)
for r in rows:
    blocks[int(r["island"])].append((int(r["x"]), int(r["z"])))

META = {
    0: ("MG-REB-02", "Rebecca's course", (60, 200, 190)),
    1: ("MG-REB-01", "Rebecca kiosk", (60, 200, 190)),
    2: ("MG-HUB-01", "Main lobby", (255, 205, 60)),
    3: ("MG-CMD-01", "Command room", (160, 160, 168)),
    4: ("MG-PT-01", "Parkour Tag", (245, 140, 40)),
    5: ("MG-SB-01", "Sky Battle", (230, 70, 60)),
    6: ("MG-SC-01", "Super Clash", (80, 130, 230)),
    7: ("MG-SR-HUB-01", "Sky Race hub", (240, 200, 60)),
    8: ("MG-SR-RED-01", "Sky Race Red", (220, 60, 60)),
    9: ("MG-SR-ORG-01", "Sky Race Orange", (235, 140, 50)),
}
BOUNDS = {
    0: "x -50005..-49871  z -51005..-50946",
    1: "x -50005..-49995  z -50005..-49995",
    2: "x -235..-173  z 904..984",
    3: "x 926..1004  z -18..16",
    4: "x 1035..1078  z 972..1017",
    5: "x 1948..2052  z -52..52",
    6: "x 4922..5012  z -30..6",
    7: "x 19995..20013  z 19992..20008",
    8: "x 20991..21070  z 19979..20021",
    9: "x 20991..21070  z 20979..21021",
}
ENTRY = {2: (-196, 927), 4: (1061, 995), 5: (2000, 0), 6: (5000, 0),
         7: (20000, 20000), 8: (21000, 20000), 9: (21000, 21000),
         1: (-50000, -50000), 0: (-50000, -51000)}


def main():
    W, H = 1460, 1180
    img = Image.new("RGB", (W, H), (12, 15, 22))
    d = ImageDraw.Draw(img)
    fb = ImageFont.truetype(FB, 26)
    fh = ImageFont.truetype(FB, 18)
    fr = ImageFont.truetype(FR, 15)
    fs = ImageFont.truetype(FR, 12)
    d.text((24, 14), "mcc_minigames — labelled top-down world map", font=fb, fill=(255, 255, 255))
    d.text((24, 48), "daniellaur's minigames v3 · 1.17.1 · panel A is true scale; panel B is schematic (distances compressed)",
           font=fs, fill=(150, 160, 180))

    # ---------- Panel A: true-scale main cluster ----------
    ax0, ay0, aw, ah = 24, 88, 1408, 620
    scale = 0.48
    ox, oz = -320, -160
    d.rectangle([ax0, ay0, ax0 + aw, ay0 + ah], fill=(16, 20, 28), outline=(45, 55, 70))
    d.text((ax0 + 10, ay0 + 6), "A — main cluster (true scale, 0.48 px/block)", font=fh, fill=(255, 255, 255))

    def P(wx, wz):
        return (ax0 + (wx - ox) * scale, ay0 + (wz - oz) * scale)

    for gx in range(-250, 2700, 500):
        X, _ = P(gx, 0)
        if ax0 <= X <= ax0 + aw:
            d.line([(X, ay0 + 30), (X, ay0 + ah)], fill=(27, 33, 45))
            d.text((X + 2, ay0 + ah - 14), str(gx), font=fs, fill=(70, 80, 100))
    for gz in range(0, 1200, 500):
        _, Z = P(0, gz)
        if ay0 <= Z <= ay0 + ah:
            d.line([(ax0, Z), (ax0 + aw, Z)], fill=(27, 33, 45))
            d.text((ax0 + 3, Z + 1), str(gz), font=fs, fill=(70, 80, 100))

    for a, b in [((-200, 968), (1061, 995)), ((-200, 968), (2000, 0))]:
        p1, p2 = P(*a), P(*b)
        for t in range(0, 100, 4):
            x = p1[0] + (p2[0] - p1[0]) * t / 100
            y = p1[1] + (p2[1] - p1[1]) * t / 100
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(110, 190, 250))
        mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
        d.text((mx + 4, my - 14), "teleport", font=fs, fill=(110, 190, 250))

    labels_a = {2: (-315, 780), 3: (760, 60), 4: (1150, 990), 5: (1560, 90)}
    for isl in (2, 3, 4, 5):
        xs = [p[0] for p in blocks[isl]]
        zs = [p[1] for p in blocks[isl]]
        x0, x1, z0, z1 = min(xs), max(xs), min(zs), max(zs)
        col = META[isl][2]
        p0, p1 = P(x0, z0), P(x1 + 1, z1 + 1)
        d.rectangle([p0[0], p0[1], p1[0], p1[1]], fill=tuple(c // 4 for c in col), outline=col, width=2)
        lx, ly = labels_a[isl]
        L = P(lx, ly)
        aid, name, _ = META[isl]
        tw = 235
        d.rectangle([L[0], L[1], L[0] + tw, L[1] + 54], fill=(10, 13, 18), outline=(50, 60, 80))
        d.text((L[0] + 6, L[1] + 2), aid, font=ImageFont.truetype(FB, 16), fill=col)
        d.text((L[0] + 6, L[1] + 20), name, font=fr, fill=(215, 220, 230))
        d.text((L[0] + 6, L[1] + 37), BOUNDS[isl], font=fs, fill=(130, 140, 160))
        above = ly < z0
        d.line([(L[0] + tw / 2, L[1] + (0 if above else 54)), ((p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2)],
               fill=(80, 90, 110))
        if isl in ENTRY:
            ex, ez = ENTRY[isl]
            X, Y = P(ex, ez)
            d.rectangle([X - 3, Y - 3, X + 3, Y + 3], fill=(255, 255, 255))
    d.text((ax0 + aw - 370, ay0 + ah - 40), "→ Super Clash, Sky Race, Rebecca: see panel B",
           font=fs, fill=(150, 160, 180))

    # ---------- Panel B: schematic wider world ----------
    bx0, by0, bw, bh = 24, 724, 1408, 350
    d.rectangle([bx0, by0, bx0 + bw, by0 + bh], fill=(16, 20, 28), outline=(45, 55, 70))
    d.text((bx0 + 10, by0 + 6), "B — wider world (schematic; node positions compressed, coordinates exact)", font=fh, fill=(255, 255, 255))

    # node boxes relative to panel B
    nodes = {
        "reb": (60, 170, 210, 1, "MG-REB-01 / 02", "Rebecca kiosk + course", "x -50005..-49871  z -51005..-49995"),
        "hub": (390, 170, 210, 2, "MG-HUB-01", "Main lobby", "x -235..-173  z 904..984"),
        "cmd": (390, 60, 210, 3, "MG-CMD-01", "Command room", "x 926..1004  z -18..16"),
        "sc": (680, 60, 210, 6, "MG-SC-01", "Super Clash", "x 4922..5012  z -30..6"),
        "srh": (980, 170, 210, 7, "MG-SR-HUB-01", "Sky Race hub", "x 19995..20013  z 19992..20008"),
        "red": (1190, 90, 210, 8, "MG-SR-RED-01", "Sky Race Red", "x 20991..21070  z 19979..20021"),
        "org": (1190, 250, 210, 9, "MG-SR-ORG-01", "Sky Race Orange", "x 20991..21070  z 20979..21021"),
    }
    box = {}
    for k, (x, y, w, isl, aid, name, bnd) in nodes.items():
        X, Y = bx0 + x, by0 + y
        col = META[isl][2]
        box[k] = (X, Y, w, 62)
        d.rectangle([X, Y, X + w, Y + 62], fill=tuple(c // 5 for c in col), outline=col, width=2)
        d.text((X + 6, Y + 2), aid, font=ImageFont.truetype(FB, 14), fill=col)
        d.text((X + 6, Y + 20), name, font=fr, fill=(220, 225, 235))
        d.text((X + 6, Y + 39), bnd, font=fs, fill=(135, 145, 165))

    def edge(a, b, label="tp"):
        p1 = (box[a][0] + box[a][2] / 2, box[a][1] + box[a][3] / 2)
        p2 = (box[b][0] + box[b][2] / 2, box[b][1] + box[b][3] / 2)
        for t in range(0, 100, 4):
            x = p1[0] + (p2[0] - p1[0]) * t / 100
            y = p1[1] + (p2[1] - p1[1]) * t / 100
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(110, 190, 250))
        mx, my = (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2
        d.text((mx - 8, my - 16), label, font=fs, fill=(110, 190, 250))

    edge("hub", "sc")
    edge("hub", "srh")
    edge("srh", "red")
    edge("srh", "org")
    edge("reb", "hub")
    d.text((bx0 + 366, by0 + 322), "· · · compressed · · ·", font=fs, fill=(80, 90, 110))
    d.text((bx0 + 700, by0 + 322), "· · · compressed · · ·", font=fs, fill=(80, 90, 110))
    d.text((bx0 + 1020, by0 + 322), "· · · compressed · · ·", font=fs, fill=(80, 90, 110))

    ly = 1096
    d.text((24, ly), "white square = spawn / teleport entry      dotted line = teleport link (no walkway)      arenas sit in separate chunk clusters surrounded by void",
           font=fr, fill=(160, 170, 190))

    img.save(OUT)
    print("wrote", OUT, img.size)


if __name__ == "__main__":
    main()
