#!/usr/bin/env python3
"""Find openings in the hall's outer walls (x=-44 and x=163, z=-116 and z=60)."""
import gzip
import os
from collections import defaultdict

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
cols = defaultdict(set)
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        cols[(int(x), int(z))].add(int(y))

def gaps(label, cells, y0, y1):
    print("==", label)
    for c in cells:
        ys = sorted(y for y in cols.get(c, ()) if y0 <= y <= y1)
        # compress to ranges
        rng = []
        for y in ys:
            if rng and y == rng[-1][1] + 1:
                rng[-1][1] = y
            else:
                rng.append([y, y])
        print(f"  {c}: {rng}")

# west wall x=-44
gaps("west wall x=-44", [(-44, z) for z in range(-116, 61, 8)], 100, 200)
# east wall x=163
gaps("east wall x=163", [(163, z) for z in range(-116, 61, 8)], 100, 200)
# south wall z=0? and z=60
gaps("south edge z=60", [(x, 60) for x in range(-44, 164, 12)], 100, 130)
gaps("north edge z=-116", [(x, -116) for x in range(-44, 164, 12)], 100, 200)
