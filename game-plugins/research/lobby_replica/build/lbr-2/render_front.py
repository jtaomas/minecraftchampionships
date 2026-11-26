#!/usr/bin/env python3
"""Straight-on south elevation of the LBR-2 Decision Dome.

Reads blocks.csv and projects from +z toward -z (the clock/entrance face),
with front-to-back glass compositing.  This is a verification view, not a
perspective render: x increases to the right, y increases upward, exactly as a
viewer standing south of the build sees it.

Usage:
    python render_front.py [blocks.csv] [preview_front.png]
"""
from __future__ import annotations

import csv
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "..", "tools"))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)
import render_preview as rp  # noqa: E402

SCALE = 10          # pixels per block
MARGIN_X = 24
MARGIN_Y = 24


def load_blocks(path):
    blocks = {}
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            blocks[(int(row["x"]), int(row["y"]), int(row["z"]))] = row["block"]
    return blocks


def front_image(blocks):
    xs = [p[0] for p in blocks]
    ys = [p[1] for p in blocks]
    zs = [p[2] for p in blocks]
    x0, x1 = min(xs), max(xs)
    y0, y1 = min(ys), max(ys)
    z0, z1 = min(zs), max(zs)
    W, H = x1 - x0 + 1, y1 - y0 + 1

    # sky gradient, brighter near the horizon
    top = (104, 160, 226)
    bot = (222, 236, 248)
    img = Image.new("RGB", (W, H), bot)
    px = img.load()
    for yy in range(H):
        t = yy / max(H - 1, 1)
        c = tuple(round(top[k] * (1 - t) + bot[k] * t) for k in range(3))
        for xx in range(W):
            px[xx, yy] = c

    # blocks
    for xx in range(W):
        x = x0 + xx
        for yy in range(H):
            y = y1 - yy
            layers = []
            for z in range(z1, z0 - 1, -1):
                b = blocks.get((x, y, z))
                if b is None:
                    continue
                spec = rp.resolve_block("minecraft:" + b)
                if spec is None:
                    continue
                colour, alpha, emit = spec[1], spec[2], spec[3]
                boost = 1.12 if emit > 0 else 1.0
                col = tuple(min(1.0, v / 255.0 * boost) for v in colour)
                layers.append((col, alpha))
                if alpha >= 0.999:
                    break
            if not layers:
                continue
            # layers are front-to-back; composite back-to-front. The frontmost
            # layer is brightest, deeper structure drops a little in value.
            out = [c / 255.0 for c in px[xx, yy]]
            n = len(layers)
            for li, (col, alpha) in enumerate(reversed(layers)):
                depth = 1.0 - 0.035 * min(n - 1 - li, 4)
                out = [(col[k] * depth) * alpha + out[k] * (1 - alpha) for k in range(3)]
            px[xx, yy] = tuple(round(max(0, min(1, v)) * 255) for v in out)

    big = img.resize((W * SCALE, H * SCALE), Image.NEAREST)
    canvas = Image.new("RGB", (big.width + MARGIN_X * 2, big.height + MARGIN_Y * 2 + 28),
                       (238, 242, 246))
    canvas.paste(big, (MARGIN_X, MARGIN_Y))
    d = ImageDraw.Draw(canvas)
    d.text((MARGIN_X, 6), f"LBR-2  south elevation   x {x0}..{x1}   y {y0}..{y1}   (from +Z)",
           fill=(30, 40, 55))
    d.text((MARGIN_X, canvas.height - 20),
           "clock 00:42  |  MCC CHAMPIONSHIP  |  straight-on, x increases east",
           fill=(60, 70, 85))
    return canvas


def main():
    blocks_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "blocks.csv")
    out_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "preview_front.png")
    blocks = load_blocks(blocks_path)
    img = front_image(blocks)
    img.save(out_path)
    print(f"wrote {out_path} {img.size[0]}x{img.size[1]}")


if __name__ == "__main__":
    main()
