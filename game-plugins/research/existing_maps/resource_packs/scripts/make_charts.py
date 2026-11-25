#!/usr/bin/env python3
"""Two evidence charts: audio durations pack vs vanilla, and pack byte composition."""
import csv
import os
from PIL import Image, ImageDraw, ImageFont

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
DATA = os.path.join(HERE, "data")
OUT = os.path.join(HERE, "frames")
FONT = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 13)
FONT_B = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 19)
BG = (22, 22, 26)
FG = (235, 235, 235)
MUT = (145, 145, 155)
PACK = (255, 180, 60)
VAN = (110, 160, 235)


def audio():
    rows = list(csv.DictReader(open(os.path.join(DATA, "audio.csv"), encoding="utf-8")))
    sel = [r for r in rows if r["vanilla_counterpart"]]
    W, H = 1400, 90 + len(sel) * 58
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((16, 16), "Audio duration: pack file (yellow) vs vanilla counterpart (blue), seconds", font=FONT_B, fill=PACK)
    maxv = max(max(float(r["seconds"]), float(r["vanilla_counterpart"].split("(")[1].split("s")[0])) for r in sel)
    x0, x1 = 480, W - 160
    for i, r in enumerate(sel):
        y = 72 + i * 58
        v = float(r["seconds"])
        vv = float(r["vanilla_counterpart"].split("(")[1].split("s")[0])
        d.text((16, y), r["pack_file"].replace("assets/minecraft/sounds/", ""), font=FONT, fill=FG)
        d.text((16, y + 18), r["event"][:56], font=FONT, fill=MUT)
        w1 = int((x1 - x0) * v / maxv)
        w2 = int((x1 - x0) * vv / maxv)
        d.rectangle([x0, y + 2, x0 + max(2, w1), y + 18], fill=PACK)
        d.rectangle([x0, y + 26, x0 + max(2, w2), y + 42], fill=VAN)
        d.text((x0 + max(2, w1) + 6, y + 2), f"{v:.2f}s", font=FONT, fill=FG)
        d.text((x0 + max(2, w2) + 6, y + 26), f"{vv:.2f}s", font=FONT, fill=FG)
    im.save(os.path.join(OUT, "audio_durations.png"))
    print("audio", im.size)


def composition():
    rows = list(csv.DictReader(open(os.path.join(DATA, "inventory.csv"), encoding="utf-8")))
    packs = ["clouds2", "space_race", "yeti_set_go"]
    cats = {"sound": "sounds", "texture": "textures", "language": "language", "model": "models",
            "metadata": "meta/icon", "pack_icon": "meta/icon", "source_file": "source/PSD", "animation_meta": "anim meta"}
    W, H = 1200, 360
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((16, 16), "Pack composition by bytes", font=FONT_B, fill=PACK)
    colors = [(255, 180, 60), (110, 160, 235), (120, 210, 140), (220, 120, 200), (160, 160, 170), (230, 90, 90), (90, 200, 210)]
    x0, x1 = 180, W - 40
    for i, p in enumerate(packs):
        sub = [r for r in rows if r["pack"] == p]
        totals = {}
        for r in sub:
            cat = cats.get(r["kind"], "other")
            totals[cat] = totals.get(cat, 0) + int(r["bytes"])
        total = sum(totals.values())
        y = 90 + i * 90
        d.text((16, y + 20), p, font=FONT_B, fill=FG)
        d.text((16, y + 44), f"{total/1048576:.2f} MB", font=FONT, fill=MUT)
        x = x0
        for j, (cat, b) in enumerate(sorted(totals.items(), key=lambda kv: -kv[1])):
            w = int((x1 - x0) * b / total)
            d.rectangle([x, y, x + w, y + 46], fill=colors[j % len(colors)])
            if w > 70:
                d.text((x + 6, y + 8), cat, font=FONT, fill=(20, 20, 20))
                d.text((x + 6, y + 26), f"{b/1048576:.1f} MB", font=FONT, fill=(20, 20, 20))
            x += w
        d.text((x0, y + 52), " | ".join(f"{c}:{b/1048576:.1f}MB" for c, b in sorted(totals.items(), key=lambda kv: -kv[1])), font=FONT, fill=MUT)
    im.save(os.path.join(OUT, "pack_composition.png"))
    print("comp", im.size)


if __name__ == "__main__":
    audio()
    composition()
