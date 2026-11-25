#!/usr/bin/env python3
"""Print a small block table: x0 x1 z0 z1 y0 y1 (world coords)."""
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def main():
    x0, x1, z0, z1, y0, y1 = map(int, sys.argv[1:7])
    cells = {}
    for line in gzip.open(DUMP, "rt", encoding="utf-8"):
        if line.startswith("MCMDUMP"):
            continue
        x, y, z, b = line.rstrip("\n").split(" ", 3)
        wx, wy, wz = int(x) + OX, int(y) + OY, int(z) + OZ
        if x0 <= wx <= x1 and z0 <= wz <= z1 and y0 <= wy <= y1:
            cells[(wx, wy, wz)] = b.split("[")[0].replace("minecraft:", "")
    for z in range(z0, z1 + 1):
        print(f"--- z={z} (rows y high->low) ---")
        for y in range(y1, y0 - 1, -1):
            print(f"y={y:4d} " + " ".join(f"{x}:{cells.get((x, y, z), 'air'):24s}" for x in range(x0, x1 + 1)))


if __name__ == "__main__":
    main()
