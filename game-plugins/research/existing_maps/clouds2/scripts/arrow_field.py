#!/usr/bin/env python3
"""Arrow flow field: dominant yellow-glazed facing per 32x32 world cell."""
import gzip, re
from collections import Counter, defaultdict
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
CELL = 32
cells = defaultdict(Counter)
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        n = p[3].strip()
        if not n.startswith("minecraft:yellow_glazed_terracotta"):
            continue
        m = re.search(r"facing=(\w+)", n)
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        cells[(x // CELL, z // CELL)][m.group(1) if m else "?"] += 1
# travel = opposite of facing
opp = {"north": "SOUTH", "south": "NORTH", "east": "WEST", "west": "EAST", "?": "?"}
rows = []
for (cx, cz), c in cells.items():
    face, n = c.most_common(1)[0]
    rows.append((cz * CELL, cx * CELL, n, face, opp[face], sum(c.values())))
rows.sort()
print(f"{'z':>6} {'x':>6} {'n':>4}  facing   -> travel")
for z, x, n, f, t, tot in rows:
    print(f"{z:6d} {x:6d} {n:4d}  {f:8s} -> {t}   (total {tot})")
