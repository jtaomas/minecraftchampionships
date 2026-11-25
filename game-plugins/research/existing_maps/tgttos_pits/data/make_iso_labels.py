#!/usr/bin/env python3
"""Add a legend and approximate callouts to the course iso and finish iso."""
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


def add_legend(src, out, title, marks, note):
    img = Image.open(os.path.join(RENDERS, src)).convert("RGB")
    W, H = img.size
    canvas = Image.new("RGB", (max(W, 900), H + 60 + 24 * len(marks) + 24), (18, 20, 28))
    canvas.paste(img, (0, 0))
    d = ImageDraw.Draw(canvas)
    for n, x, y, label in marks:
        d.ellipse([x - 12, y - 12, x + 12, y + 12], fill=(200, 30, 30), outline=(255, 255, 255), width=2)
        f = font(16, True)
        bb = d.textbbox((0, 0), str(n), font=f)
        d.text((x - (bb[2] - bb[0]) / 2, y - (bb[3] - bb[1]) / 2 - 3), str(n), font=f, fill=(255, 255, 255))
    ly = H + 10
    d.text((12, ly), title, font=font(15, True), fill=(240, 240, 240))
    ly += 28
    for n, x, y, label in marks:
        d.text((14, ly), f"{n}.", font=font(13, True), fill=(255, 120, 120))
        d.text((38, ly), label, font=font(13), fill=(220, 220, 220))
        ly += 23
    d.text((14, ly), note, font=font(12), fill=(180, 180, 180))
    canvas.save(os.path.join(RENDERS, out))
    print("wrote", out, canvas.size)


def main():
    add_legend(
        "course_iso_angle0.png", "course_iso_angle0_labeled.png",
        "Course iso, angle 0 (south-east view) - numbers are approximate screen positions",
        [
            (1, 285, 268, "Start trench (narrow strip beside Road A)"),
            (2, 355, 305, "Road A, top -3"),
            (3, 515, 355, "Road B, top -7"),
            (4, 800, 360, "Finish area / chicken pool (partly hidden by buildings)"),
        ],
        "The render is a block dump of y -16..4; the blue background is the renderer, not water. See course_top_labeled.png for exact pit positions.",
    )
    add_legend(
        "finish_pool_iso.png", "finish_pool_iso_labeled.png",
        "Finish pool, iso angle 2 - the long sunken pool is the goal; chickens are entities and not drawn",
        [
            (1, 420, 390, "Chicken pool: x61-67, z19-52, floor y=-11"),
            (2, 250, 380, "Black-glass west lip: x59-60, y=-9"),
            (3, 640, 360, "East quartz plaza: x68-79, y=-9"),
            (4, 430, 210, "Quartz rim / pool wall"),
        ],
        "20 saved chickens stand on the pool floor at y=-10; punch any one to finish (damage_dealt_resisted).",
    )


if __name__ == "__main__":
    main()
