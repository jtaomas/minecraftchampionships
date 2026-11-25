#!/usr/bin/env python3
"""Draw a labelled top-down route schematic and label the course render.

Outputs:
  renders/route_schematic.png
  renders/course_top_labeled.png
"""
import json
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)  # tgttos_pits
RENDERS = os.path.join(ROOT, "renders")

FONT = "C:/Windows/Fonts/segoeui.ttf"
FONTB = "C:/Windows/Fonts/segoeuib.ttf"


def font(size, bold=False):
    try:
        return ImageFont.truetype(FONTB if bold else FONT, size)
    except Exception:
        return ImageFont.load_default()


def route_schematic():
    S = 8
    X0, X1 = -8, 84
    Z0, Z1 = -14, 84
    W = (X1 - X0) * S
    H = (Z1 - Z0) * S
    LM, TM = 130, 80
    canvas = Image.new("RGB", (W + LM + 330, H + TM + 90), (18, 20, 28))
    d = ImageDraw.Draw(canvas)

    def px(x, z):
        return (LM + (x - X0) * S, TM + (z - Z0) * S)

    def rect(x0, z0, x1, z1, fill, outline=None, width=1):
        d.rectangle([px(x0, z0), px(x1, z1)], fill=fill, outline=outline, width=width)

    # outer shell
    rect(-3, -9, 80, 80, None, (40, 100, 150), 2)

    # context buildings (coarse masses from the surface map)
    context = (44, 38, 36)
    rect(-2, -8, 79, 1, context, (70, 60, 56))
    rect(-2, 69, 79, 79, context, (70, 60, 56))
    rect(70, 2, 79, 60, context, (70, 60, 56))
    rect(-2, 15, 14, 56, (46, 74, 38), (72, 120, 60))
    # building rows around the course
    rect(14, -8, 45, 1, context)
    rect(50, -8, 68, 1, context)
    rect(28, 69, 45, 79, context)
    rect(60, 69, 68, 79, context)

    # pits (course gaps)
    rect(26, 17, 37, 54, (8, 10, 16), (90, 40, 40), 2)
    rect(47, 17, 58, 54, (8, 10, 16), (90, 40, 40), 2)

    # road A (x13..25) and its start trench
    rect(13, 17, 25, 54, (78, 78, 82), (150, 150, 150))
    rect(19, 17, 19, 54, (206, 170, 40))
    rect(25, 17, 25, 54, (206, 170, 40))
    # road A centre line: 2 quartz, 1 dark
    for z in range(17, 55):
        if (z - 17) % 3 != 2:
            rect(22, z, 22, z, (235, 235, 225))
    # start trench + cage
    rect(13, 25, 17, 46, (40, 40, 44), (220, 60, 60), 2)
    rect(15, 26, 16, 45, (120, 120, 125))

    # road B
    rect(38, 17, 46, 54, (72, 72, 76), (150, 150, 150))
    rect(38, 17, 38, 54, (206, 170, 40))
    rect(46, 17, 46, 54, (206, 170, 40))
    for z in range(17, 55):
        if (z - 17) % 3 != 0:
            rect(42, z, 42, z, (235, 235, 225))

    # finish plaza and lip/pool
    rect(59, 17, 79, 54, (110, 108, 104), (170, 168, 160))
    rect(59, 19, 60, 52, (20, 255, 255), (20, 255, 255))
    rect(61, 19, 67, 52, (12, 14, 20), (220, 220, 220), 2)
    # chickens
    try:
        ents = json.load(open(os.path.join(HERE, "entities_raw.json"), encoding="utf-8"))
        for e in ents:
            if e["id"] == "minecraft:chicken":
                x, z = e["x"], e["z"]
                cx, cy = px(x, z)
                d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 235, 90))
    except Exception as exc:
        print("chicken dots skipped", exc)

    # route arrows
    def arrow(x0, z0, x1, z1, color=(255, 140, 30), width=3, dashed=False):
        a = px(x0, z0); b = px(x1, z1)
        if dashed:
            n = 12
            for i in range(n):
                if i % 2 == 0:
                    t0 = i / n; t1 = (i + 1) / n
                    d.line([(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0),
                            (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)],
                           fill=color, width=width)
        else:
            d.line([a, b], fill=color, width=width)
        # arrowhead
        import math
        ang = math.atan2(b[1] - a[1], b[0] - a[0])
        for s in (-0.5, 0.5):
            d.line([b, (b[0] - 10 * math.cos(ang + s), b[1] - 10 * math.sin(ang + s))],
                   fill=color, width=width)

    arrow(15, 36, 24.4, 36)
    arrow(25, 36, 38, 36, dashed=True)
    arrow(38, 36, 46, 36)
    arrow(46, 36, 59, 36, dashed=True)
    arrow(59, 36, 64, 36)

    # numbered markers
    marks = [
        (1, 15, 36, "Start cage / sign"),
        (2, 21.5, 48, "Road A"),
        (3, 31.5, 48, "Pit 1"),
        (4, 42, 48, "Road B"),
        (5, 52.5, 48, "Pit 2"),
        (6, 59.5, 48, "Glass lip"),
        (7, 64, 48, "Chicken pool"),
        (8, 73, 48, "East plaza"),
    ]
    for n, x, z, label in marks:
        cx, cy = px(x, z)
        d.ellipse([cx - 11, cy - 11, cx + 11, cy + 11], fill=(200, 30, 30), outline=(255, 255, 255), width=2)
        f = font(15, True)
        bb = d.textbbox((0, 0), str(n), font=f)
        d.text((cx - (bb[2] - bb[0]) / 2, cy - (bb[3] - bb[1]) / 2 - 2), str(n), font=f, fill=(255, 255, 255))

    # title + legend
    d.text((LM, 22), "TGTTOS Pits - route schematic (world coords, 8 px/block)", font=font(24, True), fill=(240, 240, 240))
    d.text((LM, 52), "Course runs west -> east across three elevated streets; two 12-block pits are bridged with the offhand concrete.",
           font=font(14), fill=(200, 200, 200))

    lx = W + LM + 18
    ly = TM
    d.text((lx, ly), "Legend", font=font(20, True), fill=(255, 255, 255))
    ly += 34
    legend = [
        ((78, 78, 82), "Road platform (top y=-3 / -7 / -9)"),
        ((206, 170, 40), "Yellow edge line"),
        ((235, 235, 225), "Quartz centre dashes (2:1)"),
        ((8, 10, 16), "Pit (fall below y=-41 -> respawn)"),
        ((20, 255, 255), "Black-glass finish lip (y=-9)"),
        ((12, 14, 20), "Chicken pool floor (y=-11)"),
        ((255, 235, 90), "Chicken (20)"),
        ((255, 140, 30), "Route / bridge"),
        ((46, 74, 38), "Park"),
    ]
    for col, text in legend:
        d.rectangle([lx, ly, lx + 20, ly + 14], fill=col, outline=(120, 120, 120))
        d.text((lx + 28, ly - 1), text, font=font(13), fill=(220, 220, 220))
        ly += 26
    ly += 10
    d.text((lx, ly), "Key coordinates", font=font(18, True), fill=(255, 255, 255))
    ly += 28
    lines = [
        "Spawn: 15, -2, 36  (yaw -90 / east)",
        "Start sign: 14, -1, 36 (click line 1)",
        "Cage: x13..17, z25..46, barrier y -2..1",
        "Road A: x18..25, z17..54, top -3",
        "Pit 1: x26..37 (12 wide), z17..54",
        "Road B: x38..46, z17..54, top -7",
        "Pit 2: x47..58 (12 wide), z17..54",
        "Finish lip: x59..60, z19..52, top -9",
        "Chicken pool: x61..67, z19..52, floor -11",
        "East plaza: x68..79, z17..54, top -9",
        "Kill plane: feet y <= -41",
        "Ceiling: black glass y29; walls light blue wool",
    ]
    for line in lines:
        d.text((lx, ly), line, font=font(13), fill=(210, 210, 210))
        ly += 19
    # scale bar
    sx, sy = LM, H + TM + 30
    d.line([sx, sy, sx + 10 * S, sy], fill=(255, 255, 255), width=3)
    d.text((sx, sy - 22), "10 blocks", font=font(13), fill=(255, 255, 255))
    d.text((sx + 10 * S + 16, sy - 10), "x -> east, z -> south", font=font(13), fill=(180, 180, 180))

    out = os.path.join(RENDERS, "route_schematic.png")
    canvas.save(out)
    print("wrote", out, canvas.size)


def label_top():
    src = os.path.join(RENDERS, "course_top.png")
    img = Image.open(src).convert("RGB")
    W, H = img.size
    # dump_course bounds: x -2..79, z -8..79; scale 8; margin 35
    scale = 8
    margin = 35

    def p(x, z):
        return (margin + (x + 2) * scale, margin + (z + 8) * scale)

    # grow canvas for legend
    legend_h = 250
    out = Image.new("RGB", (W, H + legend_h), (18, 20, 28))
    out.paste(img, (0, 0))
    d = ImageDraw.Draw(out)
    marks = [
        (1, 15, 36, "Start cage + sign (15,-2,36)"),
        (2, 21, 48, "Road A  top -3"),
        (3, 31, 48, "Pit 1  x26-37"),
        (4, 42, 48, "Road B  top -7"),
        (5, 52, 48, "Pit 2  x47-58"),
        (6, 59, 48, "Glass lip  top -9"),
        (7, 64, 48, "Chicken pool  floor -11"),
        (8, 73, 48, "East plaza/backstop"),
    ]
    for n, x, z, label in marks:
        cx, cy = p(x, z)
        d.ellipse([cx - 11, cy - 11, cx + 11, cy + 11], fill=(200, 30, 30), outline=(255, 255, 255), width=2)
        f = font(15, True)
        bb = d.textbbox((0, 0), str(n), font=f)
        d.text((cx - (bb[2] - bb[0]) / 2, cy - (bb[3] - bb[1]) / 2 - 2), str(n), font=f, fill=(255, 255, 255))
    # route arrows
    def arrow(x0, z0, x1, z1, dashed=False):
        a, b = p(x0, z0), p(x1, z1)
        if dashed:
            for i in range(0, 10, 2):
                t0, t1 = i / 10, (i + 1) / 10
                d.line([(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0),
                        (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)],
                       fill=(255, 140, 30), width=3)
        else:
            d.line([a, b], fill=(255, 140, 30), width=3)
    for seg in [(15, 36, 25, 36, False), (25, 36, 38, 36, True), (38, 36, 46, 36, False),
                (46, 36, 59, 36, True), (59, 36, 64, 36, False)]:
        arrow(*seg)
    # legend
    ly = H + 12
    d.text((12, ly), "TGTTOS Pits - labelled top view; orange = route, dashed = bridge.",
           font=font(15, True), fill=(240, 240, 240))
    ly += 28
    for n, x, z, label in marks:
        d.text((14, ly), f"{n}.", font=font(14, True), fill=(255, 120, 120))
        d.text((40, ly), label, font=font(14), fill=(220, 220, 220))
        ly += 22
    ly += 6
    d.text((14, ly), "Course runs west -> east; start yaw -90 (east). Pit kill plane y<=-41 returns to start.",
           font=font(13), fill=(190, 190, 190))
    out2 = os.path.join(RENDERS, "course_top_labeled.png")
    out.save(out2)
    print("wrote", out2, out.size)


if __name__ == "__main__":
    route_schematic()
    label_top()
