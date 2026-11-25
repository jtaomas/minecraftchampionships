#!/usr/bin/env python3
"""Create filtered MCMDUMP crops of the Pits world for the shared renderer.

Usage: python filter_dump.py <out> [--min x y z] [--max x y z] [--drop material,...]
Coordinates are world coordinates. The source dump stays untouched.
"""
import argparse
import gzip
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--min", nargs=3, type=int, default=[-999, -999, -999])
    ap.add_argument("--max", nargs=3, type=int, default=[999, 999, 999])
    ap.add_argument("--drop", default="")
    args = ap.parse_args()
    drop = {d.strip() for d in args.drop.split(",") if d.strip()}
    lo, hi = args.min, args.max
    rows = []
    for line in gzip.open(SRC, "rt", encoding="utf-8"):
        if line.startswith("MCMDUMP"):
            continue
        x, y, z, b = line.rstrip("\n").split(" ", 3)
        x, y, z = int(x) + OX, int(y) + OY, int(z) + OZ
        if not (lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]):
            continue
        mat = b.split("[")[0].replace("minecraft:", "")
        if mat in drop:
            continue
        rows.append((x, y, z, b))
    x0 = min(r[0] for r in rows); x1 = max(r[0] for r in rows)
    y0 = min(r[1] for r in rows); y1 = max(r[1] for r in rows)
    z0 = min(r[2] for r in rows); z1 = max(r[2] for r in rows)
    with gzip.open(os.path.join(HERE, args.out), "wt", encoding="utf-8", compresslevel=6) as f:
        f.write(f"MCMDUMP 1 {x1 - x0 + 1} {y1 - y0 + 1} {z1 - z0 + 1}\n")
        for x, y, z, b in rows:
            f.write(f"{x - x0} {y - y0} {z - z0} {b}\n")
    print(f"{args.out}: {len(rows)} blocks, world {x0},{y0},{z0} -> {x1},{y1},{z1}, "
          f"size {x1-x0+1}x{y1-y0+1}x{z1-z0+1}")


if __name__ == "__main__":
    main()
