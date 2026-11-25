#!/usr/bin/env python3
"""Component graph: link solid components that are within jump range.

Prints the shortest component chain from the start area to candidate
finishes, with the gap (Chebyshev horizontal) and height difference that
each link requires. Used to infer the course order; not authoritative.
"""
import gzip
import math
import sys
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

seen = set(); comps = []
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

info = []
for i, comp in enumerate(comps):
    xs = [p[0] for p in comp]; ys = [p[1] for p in comp]; zs = [p[2] for p in comp]
    info.append({"id": i, "n": len(comp), "x": (min(xs), max(xs)), "y": (min(ys), max(ys)), "z": (min(zs), max(zs))})

# spatial hash of block positions by 8-block cells
cell = 8
grid = {}
for p in blocks:
    grid.setdefault((p[0] // cell, p[1] // cell, p[2] // cell), []).append(p)

comp_of = {}
for i, comp in enumerate(comps):
    for p in comp:
        comp_of[p] = i

# candidate pairs from bounding boxes expanded by 6
pairs = set()
for i, a in enumerate(info):
    for j in range(i + 1, min(i + 120, len(info))):
        b = info[j]
        if a["x"][0] - 6 <= b["x"][1] and b["x"][0] - 6 <= a["x"][1] and \
           a["y"][0] - 6 <= b["y"][1] and b["y"][0] - 6 <= a["y"][1] and \
           a["z"][0] - 6 <= b["z"][1] and b["z"][0] - 6 <= a["z"][1]:
            pairs.add((i, j))

links = {}
for i, j in pairs:
    a, b = comps[i], comps[j]
    best = None
    # search near the boundary of a
    for p in a:
        cx, cy, cz = p[0]//cell, p[1]//cell, p[2]//cell
        for dx in (-1,0,1):
            for dy in (-1,0,1):
                for dz in (-1,0,1):
                    for q in grid.get((cx+dx, cy+dy, cz+dz), ()):
                        if comp_of[q] != j:
                            continue
                        hgap = max(abs(q[0]-p[0]), abs(q[2]-p[2]))
                        vgap = abs(q[1]-p[1])
                        if hgap <= 6 and vgap <= 6:
                            cost = max(0, hgap-1) + 0.5*vgap
                            if best is None or cost < best[0]:
                                best = (cost, hgap, vgap, p, q)
    if best:
        links.setdefault(i, []).append((j, best))
        links.setdefault(j, []).append((i, best))

start_comp = comp_of.get((21, -1, 6))
print("start comp", start_comp, info[start_comp])
targets = {
    "west_station": comp_of.get((-165, 36, 30)),
    "north_tip": comp_of.get((15, -20, -130)),
    "north_lone": comp_of.get((-20, -19, -119)),
    "lodge_roof": comp_of.get((-17, 35, -36)),
    "start_platform": start_comp,
}
print("targets", {k: v for k, v in targets.items()})

# Dijkstra over components
import heapq
dist = {start_comp: 0}
prev = {}
pq = [(0, start_comp)]
while pq:
    d, c = heapq.heappop(pq)
    if d != dist.get(c):
        continue
    for nb, best in links.get(c, []):
        nd = d + best[0]
        if nd < dist.get(nb, 1e9):
            dist[nb] = nd; prev[nb] = (c, best); heapq.heappush(pq, (nd, nb))

for name, tid in targets.items():
    if tid is None or tid not in dist:
        print(f"{name}: unreachable")
        continue
    chain = []
    c = tid
    while c != start_comp:
        c0, best = prev[c]
        chain.append((c, best))
        c = c0
    chain.reverse()
    print(f"\n{name} comp {tid} dist {dist[tid]:.1f} hops {len(chain)}")
    for c, best in chain:
        i = info[c]
        print(f"  -> C{c:02d} n={i['n']} x{i['x']} y{i['y']} z{i['z']}  gap h{best[1]} v{best[2]}")
