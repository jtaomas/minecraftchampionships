#!/usr/bin/env python3
"""Column probe + special pad listing around the start."""
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
        x, y, z = int(x), int(y), int(z)
        cols[(x, z)].append((y, b))

for p in [(17, 27), (6, 29), (14, 29), (17, 40), (17, 55), (14, 12), (20, 16), (21, 16),
          (23, 16), (15, 16), (16, 16), (18, 16), (19, 16), (12, 12), (22, 12), (10, 22),
          (10, 25), (10, 28)]:
    print(f"--- column {p}")
    for y, b in sorted(cols.get(p, [])):
        print("   ", y, b)

print("\n=== magenta glazed terracotta (all) ===")
for (x, z), v in sorted(cols.items()):
    for y, b in v:
        if "magenta_glazed" in b:
            print(x, y, z, b)

print("\n=== orange concrete (all) ===")
for (x, z), v in sorted(cols.items()):
    for y, b in v:
        if "orange_concrete" in b:
            print(x, y, z, b)

print("\n=== diamond block (all) ===")
for (x, z), v in sorted(cols.items()):
    for y, b in v:
        if "diamond_block" in b:
            print(x, y, z)
