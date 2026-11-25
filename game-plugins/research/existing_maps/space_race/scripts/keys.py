#!/usr/bin/env python3
"""Group key gameplay blocks by 8-block Y bands and dump coordinates per group."""
import gzip
import os
from collections import Counter, defaultdict

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")

KEYS = ("minecraft:orange_concrete", "minecraft:red_concrete", "minecraft:magenta_glazed_terracotta",
        "minecraft:diamond_block", "minecraft:bedrock", "minecraft:lime_stained_glass",
        "minecraft:red_sandstone", "minecraft:water")
c = Counter()
pts = defaultdict(list)
with gzip.open(P, "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        p = line.rstrip("\n").split(",", 3)
        b = p[3].split("[")[0]
        y = int(p[1])
        if b in KEYS:
            c[(b, y // 8 * 8)] += 1
            pts[b].append((int(p[0]), y, int(p[2])))

for k, v in sorted(c.items()):
    print(k, v)

for b in KEYS:
    print("==", b, len(pts[b]))
    # cluster by 24-block Y band summary of x/z extents
    bands = defaultdict(list)
    for x, y, z in pts[b]:
        bands[y // 24 * 24].append((x, y, z))
    for band, arr in sorted(bands.items()):
        xs = [a[0] for a in arr]; zs = [a[2] for a in arr]
        print(f"   y~{band}: n={len(arr)} x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)}")
