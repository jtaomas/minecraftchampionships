#!/usr/bin/env python3
"""Label the start/finish top views and draw a pit cross-section schematic."""
import json
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RENDERS = os.path.join(ROOT, "renders")
FONT = "C:/Windows/Fonts/segoeui.ttf"
FONTB = "C:/Windows/Fonts/segoeuib.ttf"


def font(size, bold=False):
    try:
        return ImageFont.truetype(FONTB if bold else FONT, size)
    except Exception:
        return ImageFont.load_default()


def label_image(src, out, bounds, scale, margin, marks, title, extra_lines):
    img = Image.open(os.path.join(RENDERS, src)).convert("RGB")
    W, H = img.size
    legend_h = 40 + 22 * len(marks) + 24 + 18 * len(extra_lines)
    canvas = Image.new("RGB", (max(W, 760), H + legend_h), (18, 20, 28))
    canvas.paste(img, (0, 0))
    d = ImageDraw.Draw(canvas)
    x0, z0 = bounds

    def p(x, z):
        return (margin + (x - x0) * scale, margin + (z - z0) * scale)

    for n, x, z, label in marks:
        cx, cy = p(x, z)
        d.ellipse([cx - 11, cy - 11, cx + 11, cy + 11], fill=(200, 30, 30), outline=(255, 255, 255), width=2)
        f = font(15, True)
        bb = d.textbbox((0, 0), str(n), font=f)
        d.text((cx - (bb[2] - bb[0]) / 2, cy - (bb[3] - bb[1]) / 2 - 2), str(n), font=f, fill=(255, 255, 255))
    ly = H + 10
    d.text((12, ly), title, font=font(15, True), fill=(240, 240, 240))
    ly += 26
    for n, x, z, label in marks:
        d.text((14, ly), f"{n}.", font=font(13, True), fill=(255, 120, 120))
        d.text((38, ly), label, font=font(13), fill=(220, 220, 220))
        ly += 21
    ly += 4
    for line in extra_lines:
        d.text((14, ly), line, font=font(12), fill=(180, 180, 180))
        ly += 17
    canvas.save(os.path.join(RENDERS, out))
    print("wrote", out, canvas.size)


def start_detail():
    marks = [
        (1, 15, 36, "Start spawn (15,-2,36), yaw -90 (east)"),
        (2, 14, 36, "Wall sign (14,-1,36): click line 1 to start"),
        (3, 17, 36, "East barrier gate -> Road A (y=-3)"),
        (4, 15, 24, "North 2-high corridor -> park (z21-24)"),
        (5, 15, 47, "South 2-high corridor -> south plaza"),
        (6, 22, 36, "Road A lane + quartz centre dashes"),
        (7, 10, 30, "Park / tree (decorative, up to y=17)"),
    ]
    label_image("start_top.png", "start_top_labeled.png", (4, 12), 12, 29, marks,
                "Start area - top view, dump_start (x4..25, z12..60)",
                ["The cage is 4 wide x 22 long; barrier walls at x13/x17 and z25/z46.",
                 "All three exits open at GO; the east exit is the road, north/south are 2-high side passages.",
                 "Top view is dominated by the park canopy; the cage floor is at y=-3 under it."])


def finish_detail():
    try:
        ents = json.load(open(os.path.join(HERE, "entities_raw.json"), encoding="utf-8"))
        chickens = [(e["x"], e["z"]) for e in ents if e["id"] == "minecraft:chicken"]
    except Exception:
        chickens = []
    src = Image.open(os.path.join(RENDERS, "finish_top.png")).convert("RGB")
    W, H = src.size
    canvas = Image.new("RGB", (max(W, 940), H + 240), (18, 20, 28))
    canvas.paste(src, (0, 0))
    d = ImageDraw.Draw(canvas)
    x0, z0, scale, margin = 59, 12, 12, 29

    def p(x, z):
        return (margin + (x - x0) * scale, margin + (z - z0) * scale)

    for x, z in chickens:
        cx, cy = p(x, z)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(255, 235, 90), outline=(80, 60, 0))
    marks = [
        (1, 60, 36, "Black-glass finish lip (x59-60, y=-9)"),
        (2, 64, 36, "Chicken pool (x61-67, z19-52, floor y=-11)"),
        (3, 70, 36, "East quartz plaza (x68-79, y=-9)"),
        (4, 64, 20, "Pool north end (z19)"),
        (5, 64, 52, "Pool south end (z52)"),
    ]
    for n, x, z, label in marks:
        cx, cy = p(x, z)
        d.ellipse([cx - 11, cy - 11, cx + 11, cy + 11], fill=(200, 30, 30), outline=(255, 255, 255), width=2)
        f = font(15, True)
        bb = d.textbbox((0, 0), str(n), font=f)
        d.text((cx - (bb[2] - bb[0]) / 2, cy - (bb[3] - bb[1]) / 2 - 2), str(n), font=f, fill=(255, 255, 255))
    ly = H + 10
    d.text((12, ly), "Finish pool - top view, dump_finish (x59..80, z12..60); yellow dots = 20 saved chickens",
           font=font(14, True), fill=(240, 240, 240))
    ly += 26
    for n, x, z, label in marks:
        d.text((14, ly), f"{n}.", font=font(13, True), fill=(255, 120, 120))
        d.text((38, ly), label, font=font(13), fill=(220, 220, 220))
        ly += 21
    d.text((14, ly), "The pool is 7 wide x 34 long; chickens are spread over the whole length, not a single row.", font=font(12), fill=(180, 180, 180))
    ly += 18
    d.text((14, ly), "Chickens are entities and do not appear in the block renderer; dots are their exact saved X/Z.", font=font(12), fill=(180, 180, 180))
    canvas.save(os.path.join(RENDERS, "finish_top_labeled.png"))
    print("wrote finish_top_labeled.png", canvas.size)


def pit_section():
    # x 10..82, y -66..8; 10 px per block
    S = 10
    X0, X1, Y0, Y1 = 10, 82, -66, 8
    W = (X1 - X0) * S
    H = (Y1 - Y0) * S
    LM, TM = 90, 70
    canvas = Image.new("RGB", (W + LM + 380, H + TM + 80), (18, 20, 28))
    d = ImageDraw.Draw(canvas)

    def p(x, y):
        return (LM + (x - X0) * S, TM + (Y1 - y) * S)

    def block(x0, x1, y0, y1, fill, outline=None):
        d.rectangle([p(x0, y1), p(x1, y0)], fill=fill, outline=outline)

    # void background
    block(X0, X1, Y0, Y1, (10, 12, 18))
    # road A slab (varies; draw a 6-thick slab at x13..25)
    block(13, 25, -9, -3, (90, 90, 94), (150, 150, 150))
    block(19, 19, -3, -3, (206, 170, 40))
    block(25, 25, -3, -3, (206, 170, 40))
    block(22, 22, -3, -3, (235, 235, 225))
    # road B solid tower
    block(38, 46, -64, -7, (82, 82, 86), (150, 150, 150))
    block(38, 38, -7, -7, (206, 170, 40))
    block(46, 46, -7, -7, (206, 170, 40))
    block(42, 42, -7, -7, (235, 235, 225))
    # finish lip tower + glass cap
    block(59, 60, -64, -9, (100, 100, 100), (170, 170, 170))
    block(59, 60, -9, -9, (20, 220, 220))
    # pool tower + floor
    block(61, 67, -64, -11, (90, 60, 50), (150, 100, 80))
    block(61, 67, -11, -11, (120, 120, 124))
    # chickens as yellow heads at y=-10
    for i in range(7):
        x = 61.5 + i * 0.8
        cx, cy = p(x, -10)
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(255, 235, 90))
    # east plaza thin slab
    block(68, 79, -9, -9, (150, 148, 140))
    # pit floor + glass layers
    block(12, 69, -64, -64, (230, 180, 80))
    for yy in range(-62, -43, 2):
        block(12, 69, yy, yy, (25, 25, 30))
    # kill plane
    ky = p(X0 + 1, -41)[1]
    for xx in range(LM, LM + W, 24):
        d.line([(xx, ky), (xx + 14, ky)], fill=(230, 50, 50), width=3)
    d.text((LM + 8, ky - 22), "kill plane y=-41 (feet <= -41 -> tp to start)", font=font(14, True), fill=(255, 90, 90))
    # labels
    d.text((LM, 22), "Pits - vertical section at z=36 (schematic, 10 px/block, y compressed to the section)", font=font(18, True), fill=(240, 240, 240))
    labels = [
        (21, -1, "Road A top -3", (255, 255, 255)),
        (31.5, -20, "Pit 1\n12 wide", (255, 120, 120)),
        (42, -5, "Road B top -7", (255, 255, 255)),
        (42, -20, "Road B tower\n(down to -64)", (170, 170, 190)),
        (52.5, -20, "Pit 2\n12 wide", (255, 120, 120)),
        (57.5, -5, "Glass lip\n-9", (20, 240, 240)),
        (64, -30, "Pool tower\nfloor -11", (255, 235, 120)),
        (64, -15, "20 chickens\n(y=-10)", (255, 235, 120)),
        (74, -12, "East plaza\nthin slab -9", (255, 255, 255)),
        (28, -64.5, "Shroomlight floor y=-64", (255, 200, 80)),
        (30, -45, "10 black-glass layers -44..-62", (180, 180, 220)),
        (21, -12, "Road A\nslab", (190, 190, 190)),
    ]
    for x, y, text, col in labels:
        tx, ty = p(x, y)
        d.multiline_text((tx, ty), text, font=font(12, True), fill=col, anchor="mm", align="center")
    d.text((LM, H + TM + 18), "Road A and the east plaza are thin floating slabs; Road B, the glass lip and the pool are solid towers from y=-64.",
           font=font(13), fill=(200, 200, 200))
    d.text((LM, H + TM + 38), "The pit is 61 blocks deep to the lit floor; the kill plane 3 blocks above the top glass layer hides the bottom.",
           font=font(13), fill=(200, 200, 200))
    out = os.path.join(RENDERS, "pit_section.png")
    canvas.save(out)
    print("wrote pit_section.png", canvas.size)


if __name__ == "__main__":
    start_detail()
    finish_detail()
    pit_section()
