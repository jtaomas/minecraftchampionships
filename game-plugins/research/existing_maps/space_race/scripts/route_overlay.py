#!/usr/bin/env python3
"""Labelled route overlay for Space Race (top-down)."""
import gzip
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
OUT = os.path.join(BASE, "renders", "route_overlay.png")

X0, X1, Z0, Z1 = -190, 240, -150, 470
S = 2

top = {}
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        x, y, z = int(x), int(y), int(z)
        if not (X0 <= x <= X1 and Z0 <= z <= Z1):
            continue
        b = b.split("[")[0]
        if b in ("minecraft:light", "minecraft:structure_void", "minecraft:barrier"):
            continue
        k = (x, z)
        if k not in top or y > top[k][0]:
            top[k] = (y, b)

img = Image.new("RGB", ((X1 - X0 + 1) * S, (Z1 - Z0 + 1) * S), (12, 14, 22))
px = img.load()
ylo = min(v[0] for v in top.values()); yhi = max(v[0] for v in top.values())
for (x, z), (y, b) in top.items():
    spec = rp.resolve_block(b)
    if spec is None:
        continue
    f = 0.45 + 0.55 * ((y - ylo) / max(1, yhi - ylo))
    rgb = tuple(min(255, int(c * f)) for c in spec[0])
    for a in range(S):
        for c in range(S):
            px[(x - X0) * S + a, (z - Z0) * S + c] = rgb

d = ImageDraw.Draw(img, "RGBA")
try:
    font = ImageFont.truetype("arial.ttf", 15)
    fontb = ImageFont.truetype("arialbd.ttf", 17)
    fonts = ImageFont.truetype("arial.ttf", 12)
except Exception:
    font = fontb = fonts = ImageFont.load_default()


def pt(x, z):
    return ((x - X0) * S, (z - Z0) * S)


def dashed(points, color, width=4, dash=9):
    for i in range(len(points) - 1):
        a, b = points[i], points[i + 1]
        dx, dy = b[0] - a[0], b[1] - a[1]
        L = max(1.0, (dx * dx + dy * dy) ** 0.5)
        n = int(L // dash)
        for j in range(n + 1):
            t0 = j / (n + 1); t1 = (j + 0.55) / (n + 1)
            d.line([(a[0] + dx * t0, a[1] + dy * t0), (a[0] + dx * t1, a[1] + dy * t1)],
                   fill=color, width=width)


def arrow(p, q, color):
    import math
    ang = math.atan2(q[1] - p[1], q[0] - p[0])
    L = 12
    for s in (0.5, -0.5):
        d.line([q, (q[0] - L * math.cos(ang - s), q[1] - L * math.sin(ang - s))], fill=color, width=4)


# route legs: (points, colour)
legs = [
    ([(17, 27), (17, 8), (0, -6), (-20, -18)], (255, 220, 0, 255)),           # start -> cavern floor
    ([(-20, -18), (-26, -30), (-14, -58), (-30, -44), (-44, -44)], (255, 220, 0, 255)),  # cavern ascent
    ([(-44, -44), (-80, -44), (-110, -30), (-121, -18)], (120, 220, 255, 255)),  # ring A -> west plat
    ([(-121, -18), (-124, 3), (-130, 40), (-140, 82), (-140, 105)], (120, 220, 255, 255)),  # ring B -> island A
    ([(-140, 105), (-141, 120), (-141, 175), (-132, 190), (-120, 200)], (80, 160, 255, 255)),  # slide
    ([(-120, 200), (-100, 228), (-60, 228), (-30, 228)], (80, 160, 255, 255)),  # bridge
    ([(-30, 228), (-36, 250), (-36, 270), (-31, 288), (-31, 300), (-32, 325), (-32, 340)], (255, 140, 255, 255)),
    ([(-32, 340), (-28, 355), (-20, 380), (0, 400), (20, 408), (41, 419)], (255, 120, 60, 255)),  # launch to F
    ([(41, 419), (70, 425), (100, 428), (117, 428)], (255, 60, 60, 255)),     # orange launch to finish
    ([(117, 428), (120, 400), (124, 380), (120, 360), (126, 320), (118, 285), (112, 266), (112, 246)], (255, 90, 160, 255)),
    ([(112, 246), (90, 220), (60, 180), (35, 140), (20, 100), (17, 70), (17, 30)], (255, 90, 160, 255)),
]

for pts, col in legs:
    pp = [pt(*p) for p in pts]
    dashed(pp, (0, 0, 0, 200), 8)
    dashed(pp, col, 4)
    for i in range(len(pp) - 1):
        arrow(pp[i], pp[i + 1], col)

stops = [
    ("1 START / FINISH", 17, 27, (255, 220, 0), 14, 4),
    ("2 LUMINOUS CAVERN", -20, -18, (255, 220, 0), -12, 22),
    ("3 CAVERN ASCENT PADS", -26, -33, (255, 220, 0), -14, -20),
    ("4 ELYTRA RING A", -46, -44, (200, 255, 120), 14, -22),
    ("5 WEST PLATFORM", -112, -22, (120, 220, 255), 14, -18),
    ("6 ELYTRA RING B", -122, 6, (200, 255, 120), 14, 0),
    ("7 POOL ISLAND", -140, 95, (80, 160, 255), 14, 0),
    ("8 WATER SLIDE", -150, 145, (80, 160, 255), 14, 0),
    ("9 POOL ISLAND B + SIGN WALL", -136, 185, (80, 160, 255), 14, -6),
    ("10 BRIDGE", -70, 220, (80, 160, 255), -10, 22),
    ("11 PLATFORM C", -30, 210, (255, 160, 80), 14, -4),
    ("12 TRIDENT RINGS", -10, 285, (255, 140, 255), 14, 0),
    ("13 LAUNCH PAD", -20, 345, (255, 120, 60), 14, 0),
    ("14 ELYTRA ISLANDS", -10, 395, (255, 120, 60), 12, 28),
    ("15 ORANGE MEGA PAD", 34, 432, (255, 60, 60), -12, 26),
    ("16 RIGHT-CHAIN TURN", 112, 448, (255, 60, 60), 14, 0),
    ("17 RETURN GLIDE TO START", 55, 120, (255, 90, 160), 14, 0),
]
for label, x, z, col, dx, dy in stops:
    cx, cz = pt(x, z)
    d.ellipse([cx - 9, cz - 9, cx + 9, cz + 9], fill=(0, 0, 0, 230), outline=col, width=3)
    tw = d.textlength(label, font=font)
    tx = cx + dx if dx >= 0 else cx + dx - tw
    d.text((tx, cz + dy), label, fill=(255, 255, 255), font=font, stroke_width=3, stroke_fill=(0, 0, 0))

# grid + axes
for gx in range(-160, X1, 40):
    X = (gx - X0) * S
    d.line([(X, 0), (X, img.height)], fill=(60, 66, 80, 90))
    d.text((X + 2, img.height - 16), f"x{gx}", fill=(180, 190, 210), font=fonts)
for gz in range(-120, Z1, 40):
    Z = (gz - Z0) * S
    d.line([(0, Z), (img.width, Z)], fill=(60, 66, 80, 90))
    d.text((2, Z + 1), f"z{gz}", fill=(180, 190, 210), font=fonts)

# title + legend
d.rectangle([8, 8, 500, 108], fill=(0, 0, 0, 205))
d.text((18, 14), "SPACE RACE - RECONSTRUCTED LAP ROUTE", fill=(255, 255, 255), font=fontb)
d.text((18, 38), "Agnor MCC Ace Race recreation, 1.18.1", fill=(200, 210, 230), font=font)
d.text((18, 62), "Dotted = launch / glide.  Labels are stops, not gates.", fill=(200, 210, 230), font=fonts)

img.save(OUT)
print("saved", OUT, img.size)
