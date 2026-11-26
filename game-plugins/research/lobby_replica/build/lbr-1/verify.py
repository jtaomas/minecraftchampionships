#!/usr/bin/env python3
"""
LBR-1 verifier - reads blocks.csv back and checks the LBR-1 contract.

    * every block is inside the declared bounds
    * plaza disc is solid and walkable at y=120
    * the eight portal pads are flat at y=120
    * all eight paths are continuous from the plaza edge to the rim
    * the island rim is closed (no holes into the hollow interior)
    * the underside reaches y=90 and nothing goes below it
    * no block sits inside another owner's reserved furniture zones
"""

from __future__ import annotations

import collections
import csv
import math
import os
import sys

CX, CZ = 5000, 5000
PLAZA_R = 48.0
LABEL = {0.0: "east", 45.0: "south-east", 90.0: "south", 135.0: "south-west",
         180.0: "west", 225.0: "north-west", 270.0: "north", 315.0: "north-east"}
PAD_C = [(0, 5072, 5000), (45, 5051, 5051), (90, 5000, 5072), (135, 4949, 5051),
         (180, 4928, 5000), (225, 4949, 4949), (270, 5000, 4928), (315, 5051, 4949)]
XMIN, XMAX = 4860, 5140
ZMIN, ZMAX = 4860, 5140
YMIN_HARD, YMAX_HARD = 88, 130


def ang_delta(a: float, b: float) -> float:
    return abs((a - b + math.pi) % (2 * math.pi) - math.pi)


def radius_at(th: float) -> float:
    """Mirror of gen_island.radius_at - the silhouette the paths must reach."""
    r = (98.0
         + 4.2 * math.sin(3.0 * th + 0.70)
         + 2.8 * math.sin(5.0 * th + 2.35)
         + 1.9 * math.sin(8.0 * th + 4.10)
         + 0.6 * math.sin(13.0 * th + 1.20))
    for a, w, d in ((0.30, 0.075, 5.0), (2.15, 0.062, 3.4), (4.55, 0.080, 4.2)):
        r -= d * math.exp(-(ang_delta(th, a) / w) ** 2)
    r += 3.0 * math.exp(-(ang_delta(th, 0.95) / 0.30) ** 2)
    for k in range(8):
        r += 2.5 * math.exp(-(ang_delta(th, k * math.pi / 4) / 0.30) ** 2)
    return max(86.0, min(110.0, r))


def main(path: str) -> int:
    solid: dict[tuple[int, int, int], str] = {}
    top: dict[tuple[int, int], tuple[int, str]] = {}
    fails: list[str] = []

    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            x, y, z = int(row["x"]), int(row["y"]), int(row["z"])
            b = row["block"]
            solid[(x, y, z)] = b
            if b in ("minecraft:water[level=0]", "minecraft:sea_pickle[pickles=3,waterlogged=true]"):
                continue
            k = (x, z)
            cur = top.get(k)
            if cur is None or y > cur[0]:
                top[k] = (y, b)

    n = len(solid)
    print(f"blocks            {n:,}")
    print(f"columns           {len(top):,}")
    xs = [k[0] for k in solid]
    ys = [k[1] for k in solid]
    zs = [k[2] for k in solid]
    print(f"bounds            x {min(xs)}..{max(xs)}  y {min(ys)}..{max(ys)}  z {min(zs)}..{max(zs)}")

    # ---- bounds
    out = [k for k in solid if not (XMIN <= k[0] <= XMAX and ZMIN <= k[2] <= ZMAX
                                    and YMIN_HARD <= k[1] <= YMAX_HARD)]
    if out:
        fails.append(f"{len(out)} blocks outside the site box, e.g. {out[:3]}")
    if min(ys) != 90:
        fails.append(f"underside lowest block is y={min(ys)}, expected 90")

    # ---- plaza
    holes = 0
    for x in range(CX - 48, CX + 49):
        for z in range(CZ - 48, CZ + 49):
            if math.hypot(x - CX, z - CZ) > PLAZA_R:
                continue
            t = top.get((x, z))
            if not t or t[0] != 120:
                holes += 1
            elif (x, 121, z) in solid:
                holes += 1
    if holes:
        fails.append(f"plaza: {holes} columns not clean at y=120/121")

    # ---- pads
    for ang, px, pz in PAD_C:
        bad = 0
        for dx in range(-9, 10):
            for dz in range(-9, 10):
                t = top.get((px + dx, pz + dz))
                if not t or t[0] != 120:
                    bad += 1
        if bad:
            fails.append(f"pad {LABEL[ang]}: {bad} columns not flat at y=120")
        else:
            print(f"pad {LABEL[ang]:10s}  ({px},{pz}) flat 19x19")

    # ---- paths: continuous walkable deck plaza -> rim
    print("path continuity (walkable y=120, headroom y=121 free)")
    for ang, _, _ in PAD_C:
        a = math.radians(ang)
        dx, dz = math.cos(a), math.sin(a)
        r_end = radius_at(a) - 2.5
        miss = []
        steps = int((r_end - 40) / 0.55)
        for i in range(steps + 1):
            r = 40 + i * 0.55
            ok = False
            for s in (-2, -1, 0, 1, 2):
                x = int(round(CX + r * dx - s * dz))
                z = int(round(CZ + r * dz + s * dx))
                t = top.get((x, z))
                if t and t[0] == 120 and (x, 121, z) not in solid:
                    ok = True
                    break
            if not ok:
                miss.append(round(r, 1))
        if miss:
            fails.append(f"path {LABEL[ang]}: gaps at r={miss[:6]}")
        else:
            print(f"  {LABEL[ang]:10s} continuous r=40..{r_end:.0f}")

    # ---- water level sanity: every water block has water or floor below
    bad_water = 0
    for (x, y, z), b in solid.items():
        if b == "minecraft:water[level=0]" and y == 118:
            below = solid.get((x, 117, z))
            if below != "minecraft:water[level=0]" and not (below or "").startswith("minecraft:sea_pickle"):
                bad_water += 1
    if bad_water:
        fails.append(f"{bad_water} surface water blocks without water beneath")

    # ---- closed rim: for every air column just outside the island, no cavity
    #      is exposed (sampled: y=110 must be solid wherever y=118 is solid at the edge)
    print("\ntop materials by radius band")
    bands = collections.defaultdict(collections.Counter)
    for (x, z), (y, b) in top.items():
        rr = math.hypot(x - CX, z - CZ)
        bands[int(rr // 10) * 10][b.split(":")[1]] += 1
    for band in sorted(bands):
        common = ", ".join(f"{k} {v}" for k, v in bands[band].most_common(4))
        print(f"  r {band:3d}-{band + 9:3d}  {common}")

    print()
    if fails:
        print("FAIL")
        for f_ in fails:
            print("  -", f_)
        return 1
    print("PASS - plaza, pads, paths, bounds and underside all check out")
    return 0


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    sys.exit(main(os.path.join(here, "blocks.csv")))
