#!/usr/bin/env python3
"""
views.py - extra preview views for the TGTTOS B maps, using the colour table of render_preview.py.

  python views.py map.mcmdump.gz out_prefix
writes  <out_prefix>_side.png   orthographic side elevation (looking along +X, course runs left -> right)
        <out_prefix>_start.png  perspective view from the start-strip eye (camera file map.cam), towards the landmark

The camera file (written by PreviewMain) holds "ex ey ez tx ty tz" in dump coordinates.
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, r"C:\Users\USER\Desktop\Mcc\tools")
import render_preview as rp  # noqa: E402


def load(path):
    grid, names, _ = rp.load_dump(path)
    lut = rp.build_luts(names)
    lut["glass"] = np.array([("glass" in nm) for nm in names], bool)
    for i, nm in enumerate(names):   # thin blocks: draw them see-through
        if any(k in nm for k in ("chain", "bars", "fence", "pane", "_rod", "lantern", "rail", "wall", "banner")):
            lut["alpha"][i] *= 0.45
    return grid, lut


def sky(h, w):
    t = np.linspace(0, 1, h, dtype=np.float32)[:, None, None]
    top = np.array([96, 150, 220], np.float32) / 255
    bot = np.array([214, 232, 248], np.float32) / 255
    return np.broadcast_to(top * (1 - t) + bot * t, (h, w, 3)).copy()


def side(grid, lut, out, scale=4):
    X0 = grid.shape[0]
    grid = grid[int(X0 * 0.47):int(X0 * 0.53) + 1]          # central section through the course
    X, Y, Z = grid.shape
    img = np.zeros((Y, Z, 3), np.float32)
    acc = np.zeros((Y, Z), np.float32)
    # composite from the viewer (x = 0 side is nearest) inward, front-to-back with alpha
    for x in range(X):
        ids = grid[x]
        pres = ids > 0
        if not pres.any():
            continue
        a = lut["alpha"][ids] * pres
        depth = 1.0 - 0.45 * x / max(1, X - 1)
        col = lut["side"][ids] * depth
        w = (1 - acc) * a
        img += col * w[..., None]
        acc += w
        if (acc > 0.995).all():
            break
    bg = sky(Y, Z)[::-1]
    img = img + bg * (1 - acc[..., None])
    img = img[::-1]                       # y up
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    im = im.resize((Z * scale, Y * scale), Image.NEAREST)
    im.save(out)


def perspective(grid, lut, cam, out, w=1280, h=720, fov=75.0, far=260.0):
    X, Y, Z = grid.shape
    ex, ey, ez, tx, ty, tz = cam
    eye = np.array([ex, ey, ez], np.float64)
    fwd = np.array([tx - ex, (ty - ey), tz - ez], np.float64)
    fwd[1] = fwd[1] * 0.12                # keep the horizon in frame
    fwd /= np.linalg.norm(fwd)
    up0 = np.array([0, 1, 0], np.float64)
    right = np.cross(fwd, up0); right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = np.tan(np.radians(fov) / 2)
    xs = (np.arange(w) + 0.5) / w * 2 - 1
    ys = 1 - (np.arange(h) + 0.5) / h * 2
    U, V = np.meshgrid(xs * f, ys * f * h / w)
    d = fwd[None, None, :] + U[..., None] * right[None, None, :] + V[..., None] * up[None, None, :]
    d /= np.linalg.norm(d, axis=2, keepdims=True)
    d = d.reshape(-1, 3)
    n = d.shape[0]
    col = np.zeros((n, 3), np.float32)
    acc = np.zeros(n, np.float32)
    dist = np.full(n, far, np.float32)
    step = 0.18
    t = 0.3
    active = np.ones(n, bool)
    # per-axis entry detection for face shading
    prev = np.floor(eye[None, :] + d * t).astype(np.int64)
    while t < far and active.any():
        p = eye[None, :] + d * t
        c = np.floor(p).astype(np.int64)
        inb = (c[:, 0] >= 0) & (c[:, 0] < X) & (c[:, 1] >= 0) & (c[:, 1] < Y) & (c[:, 2] >= 0) & (c[:, 2] < Z)
        m = active & inb
        if m.any():
            idx = np.nonzero(m)[0]
            ids = grid[c[idx, 0], c[idx, 1], c[idx, 2]]
            hit = ids > 0
            if t < 4.0:
                hit &= lut["alpha"][ids] >= 0.99
            if t < 14.0:
                hit &= ~lut["glass"][ids]
            if hit.any():
                hi = idx[hit]
                hid = ids[hit]
                a = lut["alpha"][hid]
                moved = c[hi] != prev[hi]
                facey = moved[:, 1] & ~moved[:, 0] & ~moved[:, 2]
                facex = moved[:, 0] & ~moved[:, 1]
                base = np.where(facey[:, None], lut["top"][hid], lut["side"][hid])
                shade = np.where(facey, 1.0, np.where(facex, 0.78, 0.9))
                fog = np.clip(1 - t / far, 0.25, 1.0)
                cc = base * shade[:, None] * (0.75 + 0.25 * fog)
                wgt = (1 - acc[hi]) * a
                col[hi] += cc * wgt[:, None]
                acc[hi] += wgt
                first = dist[hi] == far
                dist[hi[first]] = t
                done = acc[hi] > 0.97
                active[hi[done]] = False
        prev = c
        # leave the grid: stop
        out_far = ((c[:, 1] < 0) & (d[:, 1] < 0)) | ((c[:, 1] >= Y) & (d[:, 1] > 0))
        active &= ~out_far
        t += step * (1 + t / 90.0)
    bg = sky(h, w).reshape(-1, 3)
    img = col + bg * (1 - acc[:, None])
    fogc = np.array([0.84, 0.90, 0.96], np.float32)
    fg = np.clip((dist - 120) / 160, 0, 0.55)[:, None] * (acc[:, None] > 0)
    img = img * (1 - fg) + fogc * fg
    img = img.reshape(h, w, 3)
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).save(out)


def main():
    src, prefix = sys.argv[1], sys.argv[2]
    grid, lut = load(src)
    side(grid, lut, prefix + "_side.png")
    camf = src.replace(".mcmdump.gz", ".cam")
    if os.path.exists(camf):
        cam = [float(v) for v in open(camf).read().split()]
        perspective(grid, lut, cam, prefix + "_start.png")
    print("views written:", prefix)


if __name__ == "__main__":
    main()
