#!/usr/bin/env python3
"""Elevation slice: max block along a thin slab, drawn as (horizontal, y)."""
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
    ap.add_argument("--axis", choices=["x", "z"], required=True, help="axis held constant (slab normal)")
    ap.add_argument("--at", type=int, required=True, help="coordinate of the slab centre")
    ap.add_argument("--thick", type=int, default=3)
    ap.add_argument("--hmin", type=int, required=True)
    ap.add_argument("--hmax", type=int, required=True)
    ap.add_argument("--ymin", type=int, default=0)
    ap.add_argument("--ymax", type=int, default=256)
    ap.add_argument("--px", type=int, default=6)
    args = ap.parse_args()

    best = {}
    with gzip.open(P, "rt", encoding="utf-8") as fh:
        next(fh)
        for line in fh:
            x, y, z, b = line.rstrip("\n").split(",", 3)
            x, y, z = int(x), int(y), int(z)
            if not (args.ymin <= y <= args.ymax):
                continue
            if args.axis == "x":
                if abs(x - args.at) > args.thick // 2:
                    continue
                h = z
            else:
                if abs(z - args.at) > args.thick // 2:
                    continue
                h = x
            if not (args.hmin <= h <= args.hmax):
                continue
            b = b.split("[")[0]
            if b in ("minecraft:light", "minecraft:structure_void", "minecraft:barrier"):
                continue
            # keep the block nearest the slab centre, else highest
            d = abs((x if args.axis == "x" else z) - args.at)
            key = (h, y)
            cur = best.get(key)
            if cur is None or d < cur[0] or (d == cur[0] and y > cur[1]):
                best[key] = (d, y, b)

    W = args.hmax - args.hmin + 1
    H = args.ymax - args.ymin + 1
    S = args.px
    img = Image.new("RGB", (W * S, H * S), (10, 12, 18))
    px = img.load()
    for (h, y), (d, yy, b) in best.items():
        spec = rp.resolve_block(b)
        col = (255, 0, 255) if spec is None else spec[0]
        rgb = tuple(int(c * 0.85) for c in col)
        for a in range(S):
            for c in range(S):
                px[(h - args.hmin) * S + a, (args.ymax - y) * S + c] = rgb
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 12)
    except Exception:
        font = ImageFont.load_default()
    for gx in range((args.hmin // 16) * 16, args.hmax + 1, 16):
        X = (gx - args.hmin) * S
        d.line([(X, 0), (X, H * S)], fill=(48, 54, 66))
        d.text((X + 1, 1), str(gx), fill=(150, 160, 180), font=font)
    for gy in range((args.ymin // 16) * 16, args.ymax + 1, 16):
        Y = (args.ymax - gy) * S
        d.line([(0, Y), (W * S, Y)], fill=(48, 54, 66))
        d.text((1, Y + 1), str(gy), fill=(150, 160, 180), font=font)
    img.save(args.out)
    print("saved", args.out, img.size, "blocks", len(best))


if __name__ == "__main__":
    main()
