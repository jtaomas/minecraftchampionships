#!/usr/bin/env python3
"""Scratch QA: pinhole perspective render of preview.mcmdump.gz (vectorised DDA)."""
import gzip
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "..", "..", "..", "..", "..", "tools"))
import render_preview as rp

DUMP = os.path.join(HERE, "..", "preview.mcmdump.gz")

# camera
CX, CY, CZ = 0.0, 124.0, 112.0
YAW = 0.0          # 0 = looking -z ; positive turns east
PITCH = 8.0
FOV = 100.0
W, H = 1280, 720

with gzip.open(DUMP, "rt") as f:
    hdr = f.readline().split()
    sx, sy, sz = int(hdr[2]), int(hdr[3]), int(hdr[4])
    grid = np.zeros((sx, sy, sz), np.uint16)
    names = ["air"]
    idx = {}
    for line in f:
        p = line.split()
        if len(p) != 4:
            continue
        b = p[3]
        i = idx.get(b)
        if i is None:
            i = idx[b] = len(names)
            names.append(b)
        grid[int(p[0]), int(p[1]), int(p[2])] = i

colors = np.zeros((len(names), 3), np.float32)
alpha = np.ones(len(names), np.float32)
solid = np.zeros(len(names), bool)
emit = np.zeros(len(names), np.float32)
for i, n in enumerate(names):
    sp = rp.resolve_block(n)
    if sp is None:
        colors[i] = 0.75
        alpha[i] = 0.0
        continue
    top, side, a, e, so, strip = sp
    colors[i] = np.array(side, np.float32) / 255.0
    alpha[i] = a
    solid[i] = so
    emit[i] = e

# rays
fx, fy = W / 2, H / 2
scale = fx / np.tan(np.radians(FOV) / 2)
xs, ys = np.meshgrid(np.arange(W) + 0.5, np.arange(H) + 0.5)
dx = (xs - fx) / scale
dy = -(ys - fy) / scale
dz = -np.ones_like(dx)
yaw = np.radians(YAW)
pitch = np.radians(PITCH)
# yaw about y, then pitch
dx, dz = dx * np.cos(yaw) - dz * np.sin(yaw), dx * np.sin(yaw) + dz * np.cos(yaw)
dy, dz = dy * np.cos(pitch) - dz * np.sin(pitch), dy * np.sin(pitch) + dz * np.cos(pitch)
norm = np.sqrt(dx * dx + dy * dy + dz * dz)
dx, dy, dz = dx / norm, dy / norm, dz / norm

ox, oy, oz = CX + 150, CY - 90, CZ + 150
px = np.full(W * H, ox, np.float32)
py = np.full(W * H, oy, np.float32)
pz = np.full(W * H, oz, np.float32)
ddx, ddy, ddz = dx.ravel(), dy.ravel(), dz.ravel()

hit_id = np.zeros(W * H, np.uint16)
hit_face = np.zeros(W * H, np.uint8)   # 1 x, 2 y, 3 z
active = np.ones(W * H, bool)
step = np.empty_like(px)
stepx = np.sign(ddx)
stepy = np.sign(ddy)
stepz = np.sign(ddz)
tmaxx = np.where(ddx != 0, ((np.floor(px) + (ddx > 0) - px) / np.where(ddx == 0, 1, ddx)), 1e9)
tmaxy = np.where(ddy != 0, ((np.floor(py) + (ddy > 0) - py) / np.where(ddy == 0, 1, ddy)), 1e9)
tmaxz = np.where(ddz != 0, ((np.floor(pz) + (ddz > 0) - pz) / np.where(ddz == 0, 1, ddz)), 1e9)
tdx = np.abs(1.0 / np.where(ddx == 0, 1e9, ddx))
tdy = np.abs(1.0 / np.where(ddy == 0, 1e9, ddy))
tdz = np.abs(1.0 / np.where(ddz == 0, 1e9, ddz))

for it in range(700):
    if not active.any():
        break
    ix = np.floor(px).astype(np.int32)
    iy = np.floor(py).astype(np.int32)
    iz = np.floor(pz).astype(np.int32)
    inside = (ix >= 0) & (ix < sx) & (iy >= 0) & (iy < sy) & (iz >= 0) & (iz < sz)
    bid = np.zeros(px.shape, np.uint16)
    bid[inside] = grid[ix[inside], iy[inside], iz[inside]]
    hit = active & (bid > 0)
    if hit.any():
        hit_id[hit] = bid[hit]
        # which axis crossed last
        m = np.minimum(np.minimum(tmaxx, tmaxy), tmaxz)
        hit_face[hit & (m == tmaxx)] = 1
        hit_face[hit & (m == tmaxy)] = 2
        hit_face[hit & (m == tmaxz)] = 3
        active &= ~hit
    m = np.minimum(np.minimum(tmaxx, tmaxy), tmaxz)
    gox = active & (m == tmaxx)
    px[gox] += stepx[gox]
    tmaxx[gox] += tdx[gox]
    goy = active & (m == tmaxy)
    py[goy] += stepy[goy]
    tmaxy[goy] += tdy[goy]
    goz = active & (m == tmaxz)
    pz[goz] += stepz[goz]
    tmaxz[goz] += tdz[goz]

col = colors[hit_id]
shade = np.where(hit_face == 2, 1.0, np.where(hit_face == 1, 0.80, 0.66))
col = col * shade[:, None]
col = col * (1 - 0.35 * (hit_id == 0))[:, None]
out = col.reshape(H, W, 3)
# sky where nothing hit
sky = np.zeros((H, W, 3), np.float32)
t = np.linspace(0, 1, H)[:, None, None]
sky[:] = np.array([104, 160, 226], np.float32) / 255 * (1 - t) + np.array([210, 232, 248], np.float32) / 255 * t
mask = (hit_id == 0).reshape(H, W)[..., None]
out = out * (1 - mask) + sky * mask
Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8)).save(os.path.join(HERE, "persp.png"))
print("wrote persp.png", W, H)
