#!/usr/bin/env python3
"""Connected components of solid blocks (6-neighbour), water excluded."""
import gzip
from collections import deque

ORIGIN = (-182, -64, -140)
WATER = {"minecraft:water", "minecraft:bubble_column"}
blocks = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    b = n.split("[")[0]
    if b in WATER:
        continue
    blocks[(int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2])] = b

seen = set()
comps = []
for c in blocks:
    if c in seen:
        continue
    q = deque([c]); seen.add(c); comp = []
    while q:
        p = q.popleft(); comp.append(p)
        x, y, z = p
        for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n = (x+d[0], y+d[1], z+d[2])
            if n in blocks and n not in seen:
                seen.add(n); q.append(n)
    comps.append(comp)

comps.sort(key=len, reverse=True)
print(f"{len(comps)} components")
for i, comp in enumerate(comps[:25]):
    xs = [p[0] for p in comp]; ys = [p[1] for p in comp]; zs = [p[2] for p in comp]
    print(f"C{i+1:02d} n={len(comp):5d} x {min(xs):4d}..{max(xs):4d} y {min(ys):3d}..{max(ys):3d} z {min(zs):4d}..{max(zs):4d}")
