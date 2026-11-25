#!/usr/bin/env python3
"""Group scanned chunks into connected islands and print their extents/palettes."""
import csv
import sys
from collections import Counter

src = sys.argv[1] if len(sys.argv) > 1 else "data/chunk_scan.csv"
rows = list(csv.DictReader(open(src, encoding="utf-8")))
pts = {(int(r["chunk_x"]), int(r["chunk_z"])): r for r in rows}
seen = set()
comps = []
for p in pts:
    if p in seen:
        continue
    stack = [p]
    comp = []
    seen.add(p)
    while stack:
        c = stack.pop()
        comp.append(c)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                q = (c[0] + dx, c[1] + dz)
                if q in pts and q not in seen:
                    seen.add(q)
                    stack.append(q)
    comps.append(comp)
comps.sort(key=len, reverse=True)

print(f"chunks={len(rows)} islands={len(comps)}")
for i, c in enumerate(comps):
    x0 = min(p[0] for p in c)
    x1 = max(p[0] for p in c)
    z0 = min(p[1] for p in c)
    z1 = max(p[1] for p in c)
    ymin = min(int(pts[p]["ymin"]) for p in c)
    ymax = max(int(pts[p]["ymax"]) for p in c)
    cc = Counter()
    for p in c:
        for item in pts[p]["palette_top"].split("|"):
            k, v = item.rsplit(":", 1)
            cc[k] += int(v)
    pal = ", ".join(f"{k.replace('minecraft:', '')}({v})" for k, v in cc.most_common(10))
    print(f"C{i:02d} chunks={len(c)} chunkbox=({x0},{z0})-({x1},{z1}) "
          f"blocks=({x0*16},{z0*16})-({x1*16+15},{z1*16+15}) y=({ymin},{ymax})")
    print(f"    {pal}")
