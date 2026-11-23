#!/usr/bin/env python3
"""
view.py - extra preview modes for MCMDUMP files (builder-A TGTTOS maps), reusing the colour table of
C:/Users/USER/Desktop/Mcc/tools/render_preview.py:
  persp : first-person perspective view from a camera (eye position + look-at point, local coords via cam.json)
  side  : orthographic side elevation looking along +x (the course runs left->right along z)
Usage:
  python view.py persp dump.mcmdump.gz cam.json out.png [--w 960 --h 540 --fov 80]
  python view.py side  dump.mcmdump.gz out.png [--scale 4]
"""
import argparse
import json
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, "C:/Users/USER/Desktop/Mcc/tools")
import render_preview as rp  # noqa: E402

SKY_TOP = np.array([0.52, 0.70, 0.95], np.float32)
SKY_BOT = np.array([0.80, 0.88, 0.98], np.float32)


def load(path):
    grid, names, _ = rp.load_dump(path)
    lut = rp.build_luts(names)
    return grid, lut


def side(grid, lut, out, scale):
    X, Y, Z = grid.shape
    occ = grid > 0
    # first non-air from x = 0 (viewer stands at -x looking +x)
    any_ = occ.any(axis=0)                     # [Y, Z]
    first = np.argmax(occ, axis=0)             # [Y, Z]
    ids = np.take_along_axis(grid, first[None, :, :], axis=0)[0]
    col = lut["side"][ids]
    depth = first / max(1, X - 1)
    shade = (1.0 - 0.55 * depth)[..., None]
    emit = lut["emit"][ids][..., None]
    col = col * (shade * (1 - emit) + emit)
    img = np.zeros((Y, Z, 3), np.float32)
    t = np.linspace(1, 0, Y)[:, None, None]
    sky = SKY_BOT * (1 - t) + SKY_TOP * t
    img[:] = sky
    img[any_] = col[any_]
    img = img[::-1]                            # y up
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    im = im.resize((Z * scale, Y * scale), Image.NEAREST)
    im.save(out)


def persp(grid, lut, cam, out, W, H, fov):
    X, Y, Z = grid.shape
    mn = np.array(cam["min"], np.float64)
    eye = np.array(cam["start"], np.float64) - mn + np.array([0.0, 0.0, 0.0])
    look = np.array(cam["look"], np.float64) - mn
    fwd = look - eye
    fwd[1] = fwd[1] * cam.get("pitchScale", 0.35)   # mostly level gaze, slightly toward the goal (drop maps look down)
    fwd /= np.linalg.norm(fwd)
    up0 = np.array([0, 1.0, 0])
    right = np.cross(fwd, up0); right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = 1.0 / np.tan(np.radians(fov) / 2)
    u = (np.arange(W) + 0.5) / W * 2 - 1
    v = (np.arange(H) + 0.5) / H * 2 - 1
    uu, vv = np.meshgrid(u, -v * H / W)
    dirs = fwd[None, None, :] * f + right[None, None, :] * uu[..., None] + up[None, None, :] * vv[..., None]
    dirs = dirs.reshape(-1, 3)
    dirs /= np.linalg.norm(dirs, axis=1, keepdims=True)
    n = dirs.shape[0]
    col = np.zeros((n, 3), np.float32)
    alpha_acc = np.zeros(n, np.float32)
    pos = np.repeat(eye[None, :], n, 0)
    cell = np.floor(pos).astype(np.int64)
    step = np.sign(dirs).astype(np.int64)
    with np.errstate(divide="ignore", invalid="ignore"):
        tdelta = np.abs(1.0 / dirs)
        nxt = np.where(dirs > 0, cell + 1 - pos, pos - cell)
        tmax = nxt * tdelta
    tmax[~np.isfinite(tmax)] = 1e30
    tdelta[~np.isfinite(tdelta)] = 1e30
    active = np.ones(n, bool)
    face = np.zeros(n, np.int64)
    dist = np.zeros(n)
    solid = lut["solid"]
    for _ in range(int(X + Y + Z) * 2):
        idx = np.nonzero(active)[0]
        if idx.size == 0:
            break
        tm = tmax[idx]
        ax = np.argmin(tm, axis=1)
        r = np.arange(idx.size)
        dist[idx] = tm[r, ax]
        cell[idx, ax] += step[idx, ax]
        tmax[idx, ax] += tdelta[idx, ax]
        face[idx] = ax
        c = cell[idx]
        inside = (c[:, 0] >= 0) & (c[:, 0] < X) & (c[:, 1] >= 0) & (c[:, 1] < Y) & (c[:, 2] >= 0) & (c[:, 2] < Z)
        # rays that left the box going away are done
        gone = ~inside & (((c[:, 0] < 0) & (step[idx, 0] < 0)) | ((c[:, 0] >= X) & (step[idx, 0] > 0)) |
                          ((c[:, 1] < 0) & (step[idx, 1] < 0)) | ((c[:, 1] >= Y) & (step[idx, 1] > 0)) |
                          ((c[:, 2] < 0) & (step[idx, 2] < 0)) | ((c[:, 2] >= Z) & (step[idx, 2] > 0)) | (dist[idx] > 400))
        active[idx[gone]] = False
        ins = idx[inside]
        if ins.size == 0:
            continue
        ci = cell[ins]
        bid = grid[ci[:, 0], ci[:, 1], ci[:, 2]]
        hit = bid > 0
        if not hit.any():
            continue
        hi = ins[hit]
        b = bid[hit]
        fa = face[hi]
        base = np.where((fa == 1)[:, None], lut["top"][b], lut["side"][b])
        sh = np.where(fa == 1, 1.0, np.where(fa == 0, 0.82, 0.66)).astype(np.float32)
        em = lut["emit"][b]
        fog = np.clip(dist[hi] / 260.0, 0, 0.6).astype(np.float32)
        c3 = base * (sh * (1 - em) + em * 1.15)[:, None]
        c3 = c3 * (1 - fog[:, None]) + SKY_BOT * fog[:, None]
        a = lut["alpha"][b]
        a = np.where(solid[b], 1.0, np.maximum(a, 0.35)).astype(np.float32)
        w = (1 - alpha_acc[hi]) * a
        col[hi] += c3 * w[:, None]
        alpha_acc[hi] += w
        done = alpha_acc[hi] > 0.97
        active[hi[done]] = False
    t = (dirs[:, 1] * 0.5 + 0.5)[:, None].astype(np.float32)
    sky = SKY_BOT * (1 - t) + SKY_TOP * t
    col += sky * (1 - alpha_acc)[:, None]
    img = (np.clip(col.reshape(H, W, 3), 0, 1) * 255).astype(np.uint8)
    Image.fromarray(img).save(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["persp", "side"])
    ap.add_argument("dump")
    ap.add_argument("a")
    ap.add_argument("b", nargs="?")
    ap.add_argument("--w", type=int, default=960)
    ap.add_argument("--h", type=int, default=540)
    ap.add_argument("--fov", type=float, default=80)
    ap.add_argument("--scale", type=int, default=4)
    o = ap.parse_args()
    grid, lut = load(o.dump)
    if o.mode == "side":
        side(grid, lut, o.a, o.scale)
    else:
        with open(o.a) as fh:
            cam = json.load(fh)
        persp(grid, lut, cam, o.b, o.w, o.h, o.fov)


if __name__ == "__main__":
    main()
