#!/usr/bin/env python3
"""Connected-component pad finder: one row per physical pad/plateau."""
import gzip, json, sys
from collections import defaultdict

SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
TYPES = {
    "minecraft:yellow_glazed_terracotta": "checkpoint",
    "minecraft:lime_shulker_box": "jump",
    "minecraft:orange_shulker_box": "megajump",
    "minecraft:magenta_glazed_terracotta": "jump+speed",
    "minecraft:red_glazed_terracotta": "jump+speed",
    "minecraft:pink_glazed_terracotta": "jump+speed",
    "minecraft:green_glazed_terracotta": "slow",
    "minecraft:spruce_planks": "elytra",
    "minecraft:andesite_slab": "elytra",
    "minecraft:calcite": "calcite",
}

def main():
    by = defaultdict(set)
    with gzip.open(SRC, "rt", encoding="utf-8") as fh:
        fh.readline()
        for line in fh:
            p = line.split(" ", 3)
            if len(p) < 4: continue
            nm = p[3].strip().split("[")[0]
            if nm in TYPES:
                by[nm].add((int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]))

    out = []
    for nm, pts in by.items():
        if nm == "minecraft:calcite":
            continue
        pts = set(pts)
        comps = []
        while pts:
            seed = pts.pop()
            stack = [seed]
            comp = [seed]
            while stack:
                x, y, z = stack.pop()
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        for dz in (-1, 0, 1):
                            q = (x + dx, y + dy, z + dz)
                            if q in pts:
                                pts.remove(q)
                                stack.append(q)
                                comp.append(q)
            comps.append(comp)
        for c in comps:
            xs = [q[0] for q in c]; ys = [q[1] for q in c]; zs = [q[2] for q in c]
            out.append({
                "type": TYPES[nm], "block": nm, "n": len(c),
                "x0": min(xs), "x1": max(xs), "y0": min(ys), "y1": max(ys), "z0": min(zs), "z1": max(zs),
                "cx": round(sum(xs) / len(xs), 1), "cy": round(sum(ys) / len(ys), 1), "cz": round(sum(zs) / len(zs), 1),
            })
    out.sort(key=lambda r: (-r["cz"], r["cx"], r["cy"]))
    json.dump(out, open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\pads.json", "w"), indent=1)
    print(f"{len(out)} components")
    for r in out:
        print(f"{r['type']:12s} n={r['n']:3d}  x {r['x0']:5d}..{r['x1']:5d}  y {r['y0']:4d}..{r['y1']:4d}  z {r['z0']:5d}..{r['z1']:5d}")

if __name__ == "__main__":
    main()
