#!/usr/bin/env python3
"""Scratch: flat front elevation from preview.mcmdump.gz (nearest block along z)."""
import gzip
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "..", "..", "tools"))
import render_preview as rp
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "..", "preview.mcmdump.gz")

H = W = None
grid = {}
with gzip.open(DUMP, "rt") as f:
    hdr = f.readline().split()
    sx, sy, sz = int(hdr[2]), int(hdr[3]), int(hdr[4])
    for line in f:
        p = line.split()
        if len(p) == 4:
            x, y, z = int(p[0]), int(p[1]), int(p[2])
            grid[(x, y, z)] = p[3]

W, H = sx, sy
img = np.zeros((H, W, 3), np.float32)
alpha = np.zeros((H, W), bool)
for (x, y, z), b in sorted(grid.items(), key=lambda kv: -kv[0][2]):
    if alpha[y, x]:
        continue
    spec = rp.resolve_block(b)
    if spec is None:
        continue
    t, s, a, e, solid, strip = spec
    if a < 0.35:
        continue
    col = np.array(s, np.float32) / 255.0
    # shading by z-ish: nearer = brighter
    col = col * (0.72 + 0.28 * (z / max(1, sz)))
    img[y, x] = col
    alpha[y, x] = True

# sky
t = np.linspace(0, 1, H)[:, None, None]
bg = np.array([104, 160, 226], np.float32) / 255 * (1 - t) + np.array([222, 236, 248], np.float32) / 255 * t
out = img + bg * (1 - alpha[..., None])
out = out[::-1]  # y up
im = Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGB")
im = im.resize((W * 5, H * 5), Image.NEAREST)
im.save(os.path.join(HERE, "front_elevation.png"))
print(im.size)

