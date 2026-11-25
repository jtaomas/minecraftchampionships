#!/usr/bin/env python3
"""Cluster notable gameplay blocks (pads, barriers, containers) in 3D."""
import gzip
import sys
from collections import defaultdict, deque

TARGETS = {
    "minecraft:yellow_glazed_terracotta", "minecraft:pink_glazed_terracotta",
    "minecraft:magenta_glazed_terracotta", "minecraft:red_glazed_terracotta",
    "minecraft:yellow_carpet", "minecraft:lime_wool", "minecraft:lodestone",
    "minecraft:barrier", "minecraft:light_gray_shulker_box", "minecraft:hopper",
    "minecraft:iron_trapdoor", "minecraft:stone", "minecraft:packed_ice",
    "minecraft:ice", "minecraft:bubble_column", "minecraft:water",
    "minecraft:netherite_block", "minecraft:command_block",
    "minecraft:repeating_command_block", "minecraft:chain_command_block",
    "minecraft:redstone_block", "minecraft:lever", "minecraft:repeater",
}

blocks = {}
with gzip.open("data/yeti_full.mcmdump.gz", "rt", encoding="utf-8") as fh:
    header = fh.readline().split()
    for line in fh:
        x, y, z, name = line.rstrip("\n").split(" ", 3)
        x, y, z = int(x), int(y), int(z)
        base = name.split("[")[0]
        blocks[(x - 182, y - 64, z - 140)] = base

by_type = defaultdict(set)
for pos, name in blocks.items():
    if name in TARGETS:
        by_type[name].add(pos)

for name in sorted(by_type):
    cells = by_type[name]
    seen = set()
    clusters = []
    for c in cells:
        if c in seen:
            continue
        q = deque([c])
        seen.add(c)
        comp = []
        while q:
            p = q.popleft()
            comp.append(p)
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
                if n in cells and n not in seen:
                    seen.add(n)
                    q.append(n)
        clusters.append(comp)
    clusters.sort(key=len, reverse=True)
    print(f"### {name} clusters={len(clusters)}")
    for comp in clusters[:30]:
        xs = [p[0] for p in comp]
        ys = [p[1] for p in comp]
        zs = [p[2] for p in comp]
        print(f"  n={len(comp):4d} x {min(xs)}..{max(xs)} y {min(ys)}..{max(ys)} z {min(zs)}..{max(zs)}")
        if len(comp) <= 12:
            print("      ", sorted(comp))
