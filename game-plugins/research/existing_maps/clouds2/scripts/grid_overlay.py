#!/usr/bin/env python3
"""Overlay a labelled world-coordinate grid on a --top render.

Matches render_preview.py's compose(): content is trimmed to the dump bbox,
then padded by m = max(24, int(0.05 * max(content_h, content_w))) pixels.
"""
import sys
from PIL import Image, ImageDraw, ImageFont

src, dst = sys.argv[1], sys.argv[2]
x0, z0 = int(sys.argv[3]), int(sys.argv[4])
sx, sz = int(sys.argv[5]), int(sys.argv[6])
scale = int(sys.argv[7])

im = Image.open(src).convert("RGB")
W, H = im.size
cw, ch = sx * scale, sz * scale
m = max(24, int(0.05 * max(ch, cw)))
print("image", W, H, "content", cw, ch, "margin", m, "-> expected", cw + 2 * m, ch + 2 * m)
assert abs(W - (cw + 2 * m)) <= 3 and abs(H - (ch + 2 * m)) <= 3, (W, H, cw + 2 * m, ch + 2 * m)

d = ImageDraw.Draw(im, "RGBA")
try:
    font = ImageFont.truetype("arial.ttf", 14)
    small = ImageFont.truetype("arial.ttf", 12)
except Exception:
    font = small = ImageFont.load_default()

px = lambda wx: m + (wx - x0) * scale
py = lambda wz: m + (wz - z0) * scale

step = 50 if sx >= 200 else 25 if sx >= 100 else 10
for gx in range((x0 // step) * step, x0 + sx + 1, step):
    X = px(gx)
    if m <= X <= W - m:
        d.line([(X, m), (X, H - m)], fill=(255, 0, 0, 60), width=1)
        d.text((X + 2, m + 3), str(gx), fill=(200, 0, 0, 255), font=small)
for gz in range((z0 // step) * step, z0 + sz + 1, step):
    Y = py(gz)
    if m <= Y <= H - m:
        d.line([(m, Y), (W - m, Y)], fill=(255, 0, 0, 60), width=1)
        d.text((m + 3, Y + 1), str(gz), fill=(200, 0, 0, 255), font=small)
im.save(dst)
print("wrote", dst, im.size)
