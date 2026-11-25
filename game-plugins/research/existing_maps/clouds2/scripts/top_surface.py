#!/usr/bin/env python3
"""Top-surface block histogram (excludes transparent/decorative)."""
import gzip
from collections import Counter
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
SKIP = {"minecraft:barrier", "minecraft:structure_void", "minecraft:light",
        "minecraft:water", "minecraft:bubble_column", "minecraft:seagrass",
        "minecraft:tall_seagrass", "minecraft:vine", "minecraft:grass",
        "minecraft:tall_grass", "minecraft:oak_leaves", "minecraft:spruce_leaves",
        "minecraft:azalea_leaves", "minecraft:dark_oak_leaves", "minecraft:jungle_leaves",
        "minecraft:flowering_azalea_leaves", "minecraft:snow"}
cols = {}
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]), int(p[1]), int(p[2])
        n = p[3].strip().split("[")[0]
        if n in SKIP: continue
        k = (x, z)
        if k not in cols or y > cols[k][0]:
            cols[k] = (y, n)
c = Counter(n for y, n in cols.values())
print("columns", len(cols))
for n, v in c.most_common(40):
    print(f"{v:6d} {v/len(cols)*100:5.1f}%  {n}")
