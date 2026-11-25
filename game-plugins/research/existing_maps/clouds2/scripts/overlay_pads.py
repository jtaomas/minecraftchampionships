#!/usr/bin/env python3
"""Draw pad/checkpoint/elytra markers on the top-down overview."""
import json
from collections import defaultdict
from PIL import Image, ImageDraw, ImageFont

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2"
pads = json.load(open(D + r"\data\pads.json"))
im = Image.open(D + r"\renders\overview_top.png").convert("RGB")
d = ImageDraw.Draw(im, "RGBA")
X0, Z0, SCALE, MARGIN = -258, -316, 2, 60
px = lambda x: MARGIN + (x - X0) * SCALE
py = lambda z: MARGIN + (z - Z0) * SCALE
try:
    font = ImageFont.truetype("arialbd.ttf", 13)
    small = ImageFont.truetype("arial.ttf", 11)
except Exception:
    font = small = ImageFont.load_default()

# merge component blocks into pads by rounding centroid to 3 blocks
pads2 = defaultdict(lambda: {"n": 0, "types": set(), "x": 0, "y": 0, "z": 0, "w": 0})
for p in pads:
    k = (round(p["cx"] / 4), round(p["cy"] / 4), round(p["cz"] / 4))
    q = pads2[k]
    q["n"] += p["n"]; q["types"].add(p["type"])
    q["x"] += p["cx"] * p["n"]; q["y"] += p["cy"] * p["n"]; q["z"] += p["cz"] * p["n"]; q["w"] += p["n"]
for q in pads2.values():
    q["x"] /= q["w"]; q["y"] /= q["w"]; q["z"] /= q["w"]

COL = {"checkpoint": (255, 215, 0), "jump": (120, 255, 90), "megajump": (255, 120, 0),
       "jump+speed": (255, 60, 60), "slow": (80, 160, 80), "elytra": (190, 120, 255)}
for q in pads2.values():
    if q["n"] < 4:
        continue
    t = max(q["types"], key=lambda s: q["n"])
    c = COL.get(t, (255, 255, 255))
    x, z = px(q["x"]), py(q["z"])
    r = 5 if t == "checkpoint" else 4
    d.ellipse([x - r, z - r, x + r, z + r], fill=c + (255,), outline=(0, 0, 0, 220))
    if t in ("checkpoint", "elytra", "megajump"):
        d.text((x + 6, z - 6), f"{t[0].upper()}{int(round(q['y']))}", fill=(0, 0, 0, 255), font=small)
        d.text((x + 5, z - 7), f"{t[0].upper()}{int(round(q['y']))}", fill=c + (255,), font=small)
# start
sx, sz = px(5), py(24)
d.rectangle([sx - 7, sz - 7, sx + 7, sz + 7], outline=(0, 255, 255, 255), width=3)
d.text((sx + 10, sz), "START", fill=(0, 255, 255, 255), font=font)
im.save(D + r"\renders\route_pads.png")
print("wrote", D + r"\renders\route_pads.png", im.size, len([q for q in pads2.values() if q["n"] >= 4]), "pads")
