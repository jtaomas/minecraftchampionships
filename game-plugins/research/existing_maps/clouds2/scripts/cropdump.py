#!/usr/bin/env python3
"""Crop an MCMDUMP by absolute world coordinates.

usage: cropdump.py in.gz out.gz x0 x1 y0 y1 z0 z1 [--origin x y z]
The input dump's origin is given explicitly (absolute coord of dump 0,0,0).
"""
import gzip
import sys

args = [a for a in sys.argv[1:] if not a.startswith("--")]
flags = [a for a in sys.argv[1:] if a.startswith("--")]
src, dst = args[0], args[1]
x0, x1, y0, y1, z0, z1 = map(int, args[2:8])
ox, oy, oz = -258, -64, -316
if "--origin" in flags:
    i = sys.argv.index("--origin")
    ox, oy, oz = map(int, sys.argv[i + 1:i + 4])

rows = []
with gzip.open(src, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4:
            continue
        x, y, z = int(p[0]) + ox, int(p[1]) + oy, int(p[2]) + oz
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            rows.append((x, y, z, p[3].rstrip("\n")))

if not rows:
    raise SystemExit("empty crop")
mnx = min(r[0] for r in rows); mny = min(r[1] for r in rows); mnz = min(r[2] for r in rows)
mxx = max(r[0] for r in rows); mxy = max(r[1] for r in rows); mxz = max(r[2] for r in rows)
with gzip.open(dst, "wt", encoding="utf-8", compresslevel=6) as fh:
    fh.write(f"MCMDUMP 1 {mxx-mnx+1} {mxy-mny+1} {mxz-mnz+1}\n")
    for x, y, z, n in rows:
        fh.write(f"{x-mnx} {y-mny} {z-mnz} {n}\n")
print(f"{dst}: {len(rows)} blocks, abs {mnx},{mny},{mnz} -> {mxx},{mxy},{mxz}")
