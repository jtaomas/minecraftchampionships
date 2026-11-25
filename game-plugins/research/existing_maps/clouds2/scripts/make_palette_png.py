#!/usr/bin/env python3
"""Palette evidence: top-24 block swatches + share bars."""
import csv, importlib.util, sys
from PIL import Image, ImageDraw, ImageFont

spec = importlib.util.spec_from_file_location("rp", r"C:\Users\LeonS\Desktop\Mcc\tools\render_preview.py")
rp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rp)

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2"
rows = list(csv.DictReader(open(D + r"\data\blocks.csv", encoding="utf-8-sig")))
rows = rows[:24]
rows.sort(key=lambda r: -int(r["count"]))
W, H = 900, 40 + 26 * len(rows) + 20
im = Image.new("RGB", (W, H), (18, 21, 28))
d = ImageDraw.Draw(im)
try:
    font = ImageFont.truetype("arialbd.ttf", 14)
    small = ImageFont.truetype("arial.ttf", 12)
    title = ImageFont.truetype("arialbd.ttf", 17)
except Exception:
    font = small = title = ImageFont.load_default()
d.text((14, 8), "Clouds 2 block palette - top 24 by count (150,414 non-air blocks)", fill=(240, 245, 255), font=title)
y = 38
mx = max(int(r["count"]) for r in rows)
for r in rows:
    spec_ = rp.resolve_block(r["block"]) or ((120, 120, 120),) * 6
    c = spec_[0]
    d.rectangle([14, y, 54, y + 20], fill=c, outline=(0, 0, 0))
    d.text((62, y + 2), r["block"], fill=(235, 240, 250), font=font)
    barw = int(420 * int(r["count"]) / mx)
    d.rectangle([330, y + 3, 330 + barw, y + 17], fill=c)
    d.text((760, y + 2), f'{r["count"]}  {r["share_pct"]}%', fill=(200, 210, 225), font=small)
    y += 26
im.save(D + r"\renders\palette_bars.png")
print("wrote palette_bars.png", im.size)
