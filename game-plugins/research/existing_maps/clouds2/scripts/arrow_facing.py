#!/usr/bin/env python3
"""Report facing of yellow glazed (wayfinding arrows) by z band."""
import gzip, re
from collections import Counter, defaultdict
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
bands = defaultdict(Counter)
pts = []
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        n = p[3].strip()
        if n.startswith("minecraft:yellow_glazed_terracotta"):
            m = re.search(r"facing=(\w+)", n)
            f = m.group(1) if m else "?"
            x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
            band = (z // 25) * 25
            bands[band][f] += 1
            pts.append((z, x, y, f))
for band in sorted(bands, reverse=True):
    c = bands[band]
    print(f"z {band:5d}..{band+24:5d}  " + "  ".join(f"{k}={v}" for k, v in c.most_common()))
print()
print("sample along the first leg (z 30..-60):")
for z, x, y, f in sorted(pts, reverse=True):
    if -60 <= z <= 30:
        print(f"  z={z:4d} x={x:4d} y={y:3d} facing={f}")
