#!/usr/bin/env python3
"""Connected platform analysis of the Pits course surfaces."""
import collections
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def main():
    surf = {}
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        f.readline()
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            x, y, z = int(x) + OX, int(y) + OY, int(z) + OZ
            if not (-16 <= y <= 5):
                continue
            m = b.split("[")[0].replace("minecraft:", "")
            if m == "barrier":
                continue
            k = (x, z)
            if k not in surf or y > surf[k][0]:
                surf[k] = (y, m)
    # drop the outer light-blue box rim from consideration
    cells = {k: v for k, v in surf.items() if v[1] != "light_blue_wool"}
    print("surface cells", len(cells))

    # connected components at the same height (4-neighbour)
    seen = set()
    comps = []
    for k in cells:
        if k in seen:
            continue
        h = cells[k][0]
        q = [k]
        seen.add(k)
        pts = []
        while q:
            p = q.pop()
            pts.append(p)
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (p[0] + d[0], p[1] + d[1])
                if n in cells and n not in seen and cells[n][0] == h:
                    seen.add(n)
                    q.append(n)
        comps.append((h, pts))
    comps.sort(key=lambda c: -len(c[1]))
    print("components", len(comps))
    import csv
    with open(os.path.join(HERE, "platforms.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["id", "y", "cells", "x0", "x1", "z0", "z1", "materials"])
        for i, (h, pts) in enumerate(comps):
            xs = [p[0] for p in pts]; zs = [p[1] for p in pts]
            mats = collections.Counter(cells[p][1] for p in pts)
            w.writerow([f"c{i:03d}", h, len(pts), min(xs), max(xs), min(zs), max(zs),
                        "; ".join(f"{k}={v}" for k, v in mats.most_common(4))])
    for i, (h, pts) in enumerate(comps[:60]):
        xs = [p[0] for p in pts]; zs = [p[1] for p in pts]
        mats = collections.Counter(cells[p][1] for p in pts)
        print(f"c{i:03d} y={h:3d} n={len(pts):4d} x {min(xs):3d}..{max(xs):3d} z {min(zs):3d}..{max(zs):3d} "
              f"{mats.most_common(3)}")

    # adjacency by nearest Chebyshev gap between components (sample-based exact grid)
    print("\nadjacency (gap<=3, |dy|<=2):")
    cellset = {}
    for i, (h, pts) in enumerate(comps):
        for p in pts:
            cellset[p] = i
    adj = collections.defaultdict(set)
    for (x, z), i in cellset.items():
        h = comps[i][0]
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if dx == 0 and dz == 0:
                    continue
                j = cellset.get((x + dx, z + dz))
                if j is not None and j != i:
                    hj = comps[j][0]
                    if abs(hj - h) <= 2 and max(abs(dx), abs(dz)) <= 3:
                        adj[i].add((j, max(abs(dx), abs(dz)), hj - h))
    for i in sorted(adj)[:40]:
        hs = comps[i][0]
        links = sorted(adj[i], key=lambda t: (t[1], t[2]))[:8]
        print(f"c{i:02d} y={hs:3d} -> " + ", ".join(f"c{j}(gap{g},dy{d:+d})" for j, g, d in links))


if __name__ == "__main__":
    main()
