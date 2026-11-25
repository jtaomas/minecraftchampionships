#!/usr/bin/env python3
"""Spatial overview: coarse XZ occupancy grid, Y histogram, cluster extents."""
import gzip
import os
from collections import Counter, defaultdict

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")

pts = []
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        pts.append((int(x), int(y), int(z), b))

print("blocks", len(pts))
xs = [p[0] for p in pts]; ys = [p[1] for p in pts]; zs = [p[2] for p in pts]
print("bounds", min(xs), max(xs), min(ys), max(ys), min(zs), max(zs))

# Y histogram in 8-block bands
h = Counter((p[1] // 8) * 8 for p in pts)
print("\nY bands (band: count)")
for k in sorted(h):
    print(f"{k:5d}: {h[k]}")

# XZ occupancy 64-block cells
cell = Counter((p[0] // 64, p[2] // 64) for p in pts)
print("\n64x64 cells with >0 blocks:", len(cell))
for (cx, cz), c in sorted(cell.items(), key=lambda kv: -kv[1])[:60]:
    print(f"cell x={cx*64}..{cx*64+63} z={cz*64}..{cz*64+63}: {c}")

# Column heightmap cluster: columns with blocks
cols = set((p[0], p[2]) for p in pts)
print("\noccupied columns", len(cols))
