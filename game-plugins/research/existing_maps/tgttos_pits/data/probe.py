#!/usr/bin/env python3
"""Probe vertical columns / rows in the Pits dump (world coordinates)."""
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def load():
    columns = {}
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        f.readline()
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            wx, wy, wz = int(x) + OX, int(y) + OY, int(z) + OZ
            columns.setdefault((wx, wz), []).append((wy, b.split("[")[0].replace("minecraft:", "")))
    return columns


def main():
    cols = load()
    mode = sys.argv[1]
    if mode == "x":  # x z  -> column along y
        x, z = int(sys.argv[2]), int(sys.argv[3])
        print(f"column x={x} z={z}")
        for y, b in sorted(cols.get((x, z), [])):
            print(f"  y={y:4d} {b}")
    elif mode == "row":  # z x0 x1 [maxy] -> list surface height along x at fixed z
        z, x0, x1 = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
        maxy = int(sys.argv[5]) if len(sys.argv) > 5 else 5
        print(f"row z={z}, surface height/material along x {x0}..{x1} (y<={maxy})")
        for x in range(x0, x1 + 1):
            c = [p for p in cols.get((x, z), []) if p[0] <= maxy]
            if not c:
                print(f"  x={x:4d} EMPTY")
                continue
            top = max(c)
            print(f"  x={x:4d} top y={top[0]:4d} {top[1]:24s} stack={len(c)}")
    elif mode == "zrow":  # x z0 z1
        x, z0, z1 = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
        print(f"row x={x}, surface height/material along z {z0}..{z1}")
        for z in range(z0, z1 + 1):
            c = cols.get((x, z), [])
            if not c:
                print(f"  z={z:4d} EMPTY")
                continue
            top = max(c)
            print(f"  z={z:4d} top y={top[0]:4d} {top[1]:24s} stack={len(c)}")


if __name__ == "__main__":
    main()
