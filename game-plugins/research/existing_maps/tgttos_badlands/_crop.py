#!/usr/bin/env python3
"""Crop an MCMDUMP to original world coordinates (read-only helper)."""
import argparse, gzip, sys

ap = argparse.ArgumentParser()
ap.add_argument("src"); ap.add_argument("dst")
ap.add_argument("--min", nargs=3, type=int, required=True)
ap.add_argument("--max", nargs=3, type=int, required=True)
a = ap.parse_args()

# The dump is relative to the source dump's own min. We need the origin.
# Origin is recoverable from the header only as sizes; we pass it explicitly.
ap.add_argument("--origin", nargs=3, type=int, default=None,
                help="world coord of dump 0,0,0 (default 0 -64 -16 for this map)")
a = ap.parse_args()

ox, oy, oz = a.origin or (0, -64, -16)
lo, hi = a.min, a.max
rows = []
with gzip.open(a.src, "rt") as f:
    hdr = f.readline()
    for line in f:
        p = line.split(" ", 3)
        x = int(p[0]) + ox; y = int(p[1]) + oy; z = int(p[2]) + oz
        if lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]:
            rows.append((x, y, z, p[3].strip()))

if not rows:
    raise SystemExit("empty crop")
bx0 = min(r[0] for r in rows); by0 = min(r[1] for r in rows); bz0 = min(r[2] for r in rows)
bx1 = max(r[0] for r in rows); by1 = max(r[1] for r in rows); bz1 = max(r[2] for r in rows)
with gzip.open(a.dst, "wt") as f:
    f.write(f"MCMDUMP 1 {bx1-bx0+1} {by1-by0+1} {bz1-bz0+1}\n")
    for x, y, z, b in rows:
        f.write(f"{x-bx0} {y-by0} {z-bz0} {b}\n")
print(f"{a.dst}: {len(rows)} blocks, {bx0},{by0},{bz0} -> {bx1},{by1},{bz1}")
