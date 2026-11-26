#!/usr/bin/env python3
"""Crop a region, upscale, and draw a labelled pixel grid so landmarks can be measured."""
import sys
from PIL import Image, ImageDraw

def main(path, x0, y0, x1, y1, sc, step, out):
    im = Image.open(path).convert("RGB").crop((x0, y0, x1, y1))
    im = im.resize((int(im.width * sc), int(im.height * sc)), Image.LANCZOS)
    d = ImageDraw.Draw(im, "RGBA")
    for px in range(x0 - x0 % step, x1, step):
        X = (px - x0) * sc
        d.line([(X, 0), (X, im.height)], fill=(255, 0, 255, 70), width=1)
        d.text((X + 2, 2), str(px), fill=(255, 255, 0, 255))
    for py in range(y0 - y0 % step, y1, step):
        Y = (py - y0) * sc
        d.line([(0, Y), (im.width, Y)], fill=(255, 0, 255, 70), width=1)
        d.text((2, Y + 1), str(py), fill=(255, 255, 0, 255))
    im.save(out)
    print(out, im.size)

if __name__ == "__main__":
    a = sys.argv
    main(a[1], int(a[2]), int(a[3]), int(a[4]), int(a[5]), float(a[6]), int(a[7]), a[8])
