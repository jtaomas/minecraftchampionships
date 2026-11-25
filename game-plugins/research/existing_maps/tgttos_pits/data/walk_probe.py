#!/usr/bin/env python3
"""Walk-space probe: per column, highest floor <= -1 and the open run above it."""
import gzip
import os
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def load():
    cols = {}
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        f.readline()
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            x, y, z = int(x) + OX, int(y) + OY, int(z) + OZ
            cols.setdefault((x, z), {})[y] = b.split("[")[0].replace("minecraft:", "")
    return cols


def main():
    cols = load()
    if sys.argv[1] == "x":
        x, z0, z1 = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
        for z in range(z0, z1 + 1):
            c = cols.get((x, z), {})
            floor = max([y for y in c if y <= -1] or [-99])
            run = []
            y = floor + 1
            while y not in c and y < 30:
                run.append(y)
                y += 1
            print(f"z={z:4d} floor={floor:4d} {c.get(floor, ''):18s} open={run}")
    elif sys.argv[1] == "reach":
        # flood fill standing cells at a fixed feet Y, allowing 1-up steps, in a box
        fy = int(sys.argv[2])
        x0, x1, z0, z1 = map(int, sys.argv[3:7])
        start = None
        def solid(x, y, z):
            return y in cols.get((x, z), {}) and cols[(x, z)][y] != "air"
        def stand(x, z, y):
            return solid(x, y - 1, z) and not solid(x, y, z) and not solid(x, y + 1, z)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if stand(x, z, fy):
                    start = (x, z)
                    break
            if start:
                break
        if not start:
            print("no start")
            return
        seen = {start}
        q = deque([start])
        while q:
            x, z = q.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                if not (x0 <= nx <= x1 and z0 <= nz <= z1) or (nx, nz) in seen:
                    continue
                if stand(nx, nz, fy) or (stand(nx, nz, fy + 1) and not solid(nx, fy + 2, nz)):
                    seen.add((nx, nz))
                    q.append((nx, nz))
        print(f"reachable standing cells at feet y={fy}: {len(seen)} bbox "
              f"x {min(p[0] for p in seen)}..{max(p[0] for p in seen)} "
              f"z {min(p[1] for p in seen)}..{max(p[1] for p in seen)}")
        for z in range(z0, z1 + 1, 2):
            print(f"z={z:4d} " + "".join("#" if (x, z) in seen else "." for x in range(x0, x1 + 1, 2)))


if __name__ == "__main__":
    main()
