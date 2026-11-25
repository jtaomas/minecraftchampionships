#!/usr/bin/env python3
"""Cluster occupied columns into structures (2D connectivity with a gap tolerance),
print bbox, block count, dominant materials, and nearby special blocks."""
import gzip
import os
from collections import Counter, deque

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")

rows = []
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        rows.append((int(x), int(y), int(z), b.split("[")[0]))

cols = {}
for x, y, z, b in rows:
    cols.setdefault((x, z), []).append((y, b))

# 2D components with tolerance: treat columns within 3 blocks as connected
GAP = 3
keys = set(cols)
seen = set()
comps = []
for k in keys:
    if k in seen:
        continue
    q = deque([k]); seen.add(k)
    comp = []
    while q:
        cx, cz = q.popleft()
        comp.append((cx, cz))
        for dx in range(-GAP, GAP + 1):
            for dz in range(-GAP, GAP + 1):
                n = (cx + dx, cz + dz)
                if n in keys and n not in seen:
                    seen.add(n); q.append(n)
    comps.append(comp)

comps.sort(key=len, reverse=True)
print("components:", len(comps))
for i, comp in enumerate(comps[:40]):
    cs = set(comp)
    xs = [c[0] for c in comp]; zs = [c[1] for c in comp]
    ys = [y for c in comp for y, b in cols[c]]
    mats = Counter(b for c in comp for y, b in cols[c])
    total = sum(mats.values())
    top = ", ".join(f"{m.split(':')[1]} {n}" for m, n in mats.most_common(6))
    print(f"\n[{i}] cols={len(comp)} x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)} "
          f"y {min(ys)}..{max(ys)} blocks={total}")
    print("    ", top)
