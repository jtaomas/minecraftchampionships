#!/usr/bin/env python3
"""Cluster mechanic blocks from the full dump and print an ordered pad list."""
import gzip
import json
from collections import defaultdict

SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
ORIGIN = (-258, -64, -316)

SPECIAL = {
    "minecraft:yellow_glazed_terracotta": "checkpoint/trident",
    "minecraft:lime_shulker_box": "jump pad L5",
    "minecraft:orange_shulker_box": "mega jump",
    "minecraft:magenta_glazed_terracotta": "jump+speed",
    "minecraft:red_glazed_terracotta": "jump+speed",
    "minecraft:pink_glazed_terracotta": "jump+speed",
    "minecraft:green_glazed_terracotta": "slowness",
    "minecraft:red_terracotta": "depth strider pad",
    "minecraft:spruce_planks": "elytra L",
    "minecraft:andesite_slab": "elytra L2",
    "minecraft:calcite": "clear",
    "minecraft:barrier": "barrier",
    "minecraft:structure_void": "void",
}

def main():
    by = defaultdict(list)
    with gzip.open(SRC, "rt", encoding="utf-8") as fh:
        fh.readline()
        for line in fh:
            p = line.split(" ", 3)
            if len(p) < 4:
                continue
            nm = p[3].strip().split("[")[0]
            if nm in SPECIAL:
                x, y, z = int(p[0]) + ORIGIN[0], int(p[1]) + ORIGIN[1], int(p[2]) + ORIGIN[2]
                by[nm].append((x, y, z))
    for nm, pts in by.items():
        if nm in ("minecraft:barrier", "minecraft:structure_void", "minecraft:calcite"):
            print(f"{nm}: {len(pts)} blocks (not clustered here)")
            continue
        # simple clustering by 8-block cubes
        pts = sorted(pts)
        clusters = []
        for p in pts:
            for c in clusters:
                if abs(p[0] - c["cx"]) <= 6 and abs(p[1] - c["cy"]) <= 6 and abs(p[2] - c["cz"]) <= 6:
                    c["pts"].append(p)
                    c["cx"] = sum(q[0] for q in c["pts"]) / len(c["pts"])
                    c["cy"] = sum(q[1] for q in c["pts"]) / len(c["pts"])
                    c["cz"] = sum(q[2] for q in c["pts"]) / len(c["pts"])
                    break
            else:
                clusters.append({"cx": p[0], "cy": p[1], "cz": p[2], "pts": [p]})
        clusters.sort(key=lambda c: (-c["cz"], c["cx"]))
        print(f"\n== {nm} ({SPECIAL[nm]})  {len(pts)} blocks, {len(clusters)} clusters ==")
        for c in clusters:
            xs = [q[0] for q in c["pts"]]; ys = [q[1] for q in c["pts"]]; zs = [q[2] for q in c["pts"]]
            print(f"  n={len(c['pts']):4d}  x {min(xs)}..{max(xs)}  y {min(ys)}..{max(ys)}  z {min(zs)}..{max(zs)}   centroid {c['cx']:.1f},{c['cy']:.1f},{c['cz']:.1f}")

if __name__ == "__main__":
    main()
