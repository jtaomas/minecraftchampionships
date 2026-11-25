#!/usr/bin/env python3
"""Plan-view slice: topmost block per column within a Y range, with grid + labels."""
import argparse
import gzip
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--min", nargs=3, type=int, required=True)
    ap.add_argument("--max", nargs=3, type=int, required=True)
    ap.add_argument("--px", type=int, default=4)
    ap.add_argument("--labels", default="")
    args = ap.parse_args()
    lo, hi = args.min, args.max
    top = {}
    with gzip.open(P, "rt", encoding="utf-8") as fh:
        next(fh)
        for line in fh:
            x, y, z, b = line.rstrip("\n").split(",", 3)
            x, y, z = int(x), int(y), int(z)
            if not (lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]):
                continue
            b = b.split("[")[0]
            if b in ("minecraft:light", "minecraft:structure_void", "minecraft:barrier"):
                continue
            k = (x, z)
            if k not in top or y > top[k][0]:
                top[k] = (y, b)
    if not top:
        raise SystemExit("empty slice")
    W = hi[0] - lo[0] + 1
    H = hi[2] - lo[2] + 1
    S = args.px
    img = Image.new("RGB", (W * S, H * S), (12, 14, 20))
    px = img.load()
    for (x, z), (y, b) in top.items():
        spec = rp.resolve_block(b)
        col = (255, 0, 255) if spec is None else spec[0]
        f = 0.65 + 0.35 * ((y - lo[1]) / max(1, hi[1] - lo[1]))
        rgb = tuple(min(255, int(c * f)) for c in col)
        for dx in range(S):
            for dz in range(S):
                px[(x - lo[0]) * S + dx, (z - lo[2]) * S + dz] = rgb
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 13)
    except Exception:
        font = ImageFont.load_default()
    step = 16
    for gx in range((lo[0] // step) * step, hi[0] + 1, step):
        X = (gx - lo[0]) * S
        d.line([(X, 0), (X, H * S)], fill=(60, 66, 80))
        d.text((X + 1, 1), str(gx), fill=(160, 170, 190), font=font)
    for gz in range((lo[2] // step) * step, hi[2] + 1, step):
        Z = (gz - lo[2]) * S
        d.line([(0, Z), (W * S, Z)], fill=(60, 66, 80))
        d.text((1, Z + 1), str(gz), fill=(160, 170, 190), font=font)
    if args.labels:
        for item in args.labels.split(";"):
            try:
                name, xs, zs = item.split(",")
                X = (int(xs) - lo[0]) * S
                Z = (int(zs) - lo[2]) * S
                d.ellipse([X - 3, Z - 3, X + 3, Z + 3], fill=(255, 40, 40))
                d.text((X + 5, Z - 6), name, fill=(255, 220, 120), font=font)
            except Exception:
                pass
    img.save(args.out)
    print("saved", args.out, img.size, "cols", len(top))


if __name__ == "__main__":
    main()
