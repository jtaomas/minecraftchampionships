#!/usr/bin/env python3
"""Trace water flow directions along the slides by level values."""
import gzip
import os
from collections import defaultdict

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")

cols = defaultdict(list)
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        b = b.split("[")[0]
        if b == "minecraft:water":
            lvl = line.split("level=")[1].split("]")[0] if "level=" in line else "?"
            cols[(int(x), int(z))].append((int(y), lvl))

print("== slide A->B: x=-142..-136, z 105..185 (top water block per column) ==")
for z in range(105, 186, 5):
    row = []
    for x in range(-145, -134):
        if (x, z) in cols:
            y, lvl = max(cols[(x, z)])
            row.append(f"{x}:{y}/{lvl}")
    if row:
        print("z", z, " ".join(row))

print("\n== bridge z=222..232, x -100..-20 ==")
for x in range(-100, -19, 5):
    row = []
    for z in range(220, 236):
        if (x, z) in cols:
            y, lvl = max(cols[(x, z)])
            row.append(f"z{z}:{y}/{lvl}")
    if row:
        print("x", x, " ".join(row))

print("\n== arrival at C x -60..-17, z 200..245 ==")
for z in range(200, 246, 4):
    row = []
    for x in range(-60, -16, 4):
        if (x, z) in cols:
            y, lvl = max(cols[(x, z)])
            row.append(f"{x}:{y}/{lvl}")
    if row:
        print("z", z, " ".join(row))
