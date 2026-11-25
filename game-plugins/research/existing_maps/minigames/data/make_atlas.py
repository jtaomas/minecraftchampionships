#!/usr/bin/env python3
"""Compose the labelled top-down arena atlas from render_preview top views."""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
R = os.path.join(ROOT, "renders")
OUT = os.path.join(R, "arena_atlas.png")

FONT_B = "C:/Windows/Fonts/arialbd.ttf"
FONT_R = "C:/Windows/Fonts/arial.ttf"

ARENAS = [
    # id, name, bounds, entry, image, note, colour
    ("MG-HUB-01", "Main lobby — 'crown' hub", "x -235..-173  y 8..52  z 904..984",
     "spawn -196 24 927", "lobby_top_big.png", "game pads at y=10, z=961..982", (255, 205, 60)),
    ("MG-SB-01", "Sky Battle", "x 1948..2052  y 0..80  z -52..52",
     "entry 2000 78 0", "sb_top_big.png", "4 gift-box team islands + central ring", (230, 70, 60)),
    ("MG-PT-01", "Parkour Tag", "x 1035..1078  y 0..58  z 972..1017",
     "entry 1061 28 995", "pt_top_big.png", "stadium bowl + course; banner at west", (245, 140, 40)),
    ("MG-SC-01", "Super Clash", "x 4922..5012  y 17..36  z -30..6",
     "entry 5000 21 0", "sc_top_big.png", "tinted-glass roof hides interior", (80, 120, 220)),
    ("MG-SR-HUB-01", "Sky Race — level select", "x 19995..20013  y 39..49  z 19992..20008",
     "entry 20000 41 20000", "woolhub_top_big.png", "6 colour pillars; only red/orange wired", (240, 200, 60)),
    ("MG-SR-RED-01", "Sky Race — Red course", "x 20991..21070  y 0..82  z 19979..20021",
     "entry 21000 31 20000", "woolred_top_big.png", "red glass gates over void", (220, 60, 60)),
    ("MG-SR-ORG-01", "Sky Race — Orange course", "x 20991..21070  y 0..82  z 20979..21021",
     "entry 21000 31 21000", "woolorg_top_big.png", "same layout as Red, orange palette", (235, 140, 50)),
    ("MG-CMD-01", "Sky Battle command room", "x 926..1004  y 9..23  z -18..16",
     "no player entry", "cmd_top_big.png", "474 command blocks, 57 signs; Sky Battle logic only", (150, 150, 150)),
    ("MG-REB-01", "'Rebecca's Minigames' platform", "x -50005..-49995  y 97..104  z -50005..-49995",
     "entry -50000 100 -50000", "rebecca_top_big.png", "small kiosk; pad teleports to course", (40, 170, 160)),
    ("MG-REB-02", "Rebecca's course (event/test)", "x -50005..-49871  y 99..131  z -51005..-50946",
     "entry -50000 100 -51000", "sky_top_big.png", "linear white-concrete course over lava", (40, 170, 160)),
]


def trim_bg(img, tol=28):
    img = img.convert("RGB")
    w, h = img.size
    bg = img.getpixel((1, 1))
    px = img.load()
    minx, miny, maxx, maxy = w, h, -1, -1
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            r, g, b = px[x, y]
            if abs(r - bg[0]) + abs(g - bg[1]) + abs(b - bg[2]) > tol:
                if x < minx: minx = x
                if y < miny: miny = y
                if x > maxx: maxx = x
                if y > maxy: maxy = y
    if maxx < 0:
        return img
    return img.crop((max(0, minx - 6), max(0, miny - 6), min(w, maxx + 7), min(h, maxy + 7)))


def fit(img, box):
    bw, bh = box
    iw, ih = img.size
    s = min(bw / iw, bh / ih)
    return img.resize((max(1, int(iw * s)), max(1, int(ih * s))), Image.LANCZOS)


def main():
    cols, rows = 2, 5
    cw, ch = 700, 450
    canvas = Image.new("RGB", (cols * cw, rows * ch + 50), (18, 22, 30))
    draw = ImageDraw.Draw(canvas)
    fb = ImageFont.truetype(FONT_B, 21)
    fr = ImageFont.truetype(FONT_R, 15)
    fs = ImageFont.truetype(FONT_R, 13)
    for i, (aid, name, bounds, entry, fn, note, col) in enumerate(ARENAS):
        x0 = (i % cols) * cw
        y0 = (i // cols) * ch
        draw.rectangle([x0 + 6, y0 + 6, x0 + cw - 6, y0 + ch - 6], outline=(50, 58, 72), width=2)
        draw.rectangle([x0 + 6, y0 + 6, x0 + 12, y0 + ch - 6], fill=col)
        img = Image.open(os.path.join(R, fn))
        img = trim_bg(img)
        img = fit(img, (cw - 40, 300))
        canvas.paste(img, (x0 + (cw - img.width) // 2, y0 + 14))
        ty = y0 + 324
        draw.text((x0 + 22, ty), f"{aid}  {name}", font=fb, fill=(255, 255, 255))
        draw.text((x0 + 22, ty + 30), bounds, font=fr, fill=(190, 210, 235))
        draw.text((x0 + 22, ty + 52), f"entry: {entry}", font=fr, fill=(170, 220, 170))
        draw.text((x0 + 22, ty + 74), note, font=fs, fill=(160, 165, 180))
        draw.text((x0 + cw - 58, y0 + 16), f"{i+1:02d}", font=fb, fill=(90, 100, 120))
    draw.text((22, rows * ch + 14), "MCC research — mcc_minigames / daniellaur's minigames v3 — labelled top-down atlas (north = up)", font=fr, fill=(150, 160, 180))
    canvas.save(OUT)
    print("wrote", OUT, canvas.size)


if __name__ == "__main__":
    main()
