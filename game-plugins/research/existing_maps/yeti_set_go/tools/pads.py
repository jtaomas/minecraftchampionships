#!/usr/bin/env python3
"""Export gameplay pad clusters and draw a numbered pad map."""
import csv
import gzip
from collections import defaultdict, deque

from PIL import Image, ImageDraw, ImageFont

PADS = [
    "minecraft:yellow_glazed_terracotta",
    "minecraft:pink_glazed_terracotta",
    "minecraft:magenta_glazed_terracotta",
    "minecraft:red_glazed_terracotta",
    "minecraft:yellow_carpet",
    "minecraft:lime_wool",
    "minecraft:lodestone",
]

blocks = {}
with gzip.open("data/yeti_full.mcmdump.gz", "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        x, y, z, name = line.rstrip("\n").split(" ", 3)
        blocks[(int(x) - 182, int(y) - 64, int(z) - 140)] = name.split("[")[0]

rows = []
img = Image.new("RGB", (1110, 922), (245, 247, 250))
dr = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("arial.ttf", 11)
    big = ImageFont.truetype("arial.ttf", 13)
except Exception:  # noqa: BLE001
    font = ImageFont.load_default()
    big = font
x0, z0, scale = -182, -140, 4
for gx in range(-176, 78, 16):
    px = 70 + (gx - x0) * scale
    dr.line([px, 30, px, 30 + 872], fill=(205, 210, 218))
    dr.text((px + 2, 12), str(gx), fill=(60, 60, 60), font=font)
for gz in range(-136, 78, 16):
    py = 30 + (gz - z0) * scale
    dr.line([70, py, 1070, py], fill=(205, 210, 218))
    dr.text((38, py + 1), str(gz), fill=(60, 60, 60), font=font)

COLS = {
    "minecraft:yellow_glazed_terracotta": (230, 180, 0),
    "minecraft:pink_glazed_terracotta": (235, 100, 150),
    "minecraft:magenta_glazed_terracotta": (170, 30, 140),
    "minecraft:red_glazed_terracotta": (200, 40, 30),
    "minecraft:yellow_carpet": (255, 225, 60),
    "minecraft:lime_wool": (90, 220, 40),
    "minecraft:lodestone": (90, 90, 100),
}
cid = 0
for ptype in PADS:
    cells = {p for p, n in blocks.items() if n == ptype}
    seen = set()
    comps = []
    for c in cells:
        if c in seen:
            continue
        q = deque([c]); seen.add(c); comp = []
        while q:
            p = q.popleft(); comp.append(p)
            for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
                nb = (p[0]+d[0], p[1]+d[1], p[2]+d[2])
                if nb in cells and nb not in seen:
                    seen.add(nb); q.append(nb)
        comps.append(comp)
    comps.sort(key=lambda c: (sum(p[2] for p in c)/len(c), sum(p[0] for p in c)/len(c)))
    for comp in comps:
        cid += 1
        xs = [p[0] for p in comp]; ys = [p[1] for p in comp]; zs = [p[2] for p in comp]
        cx, cy, cz = sum(xs)/len(xs), sum(ys)/len(ys), sum(zs)/len(zs)
        rows.append({
            "id": f"P{cid:03d}", "type": ptype.replace("minecraft:", ""),
            "n": len(comp), "x_min": min(xs), "x_max": max(xs),
            "y_min": min(ys), "y_max": max(ys), "z_min": min(zs), "z_max": max(zs),
            "cx": round(cx,1), "cy": round(cy,1), "cz": round(cz,1),
        })
        if len(comp) >= 1:
            col = COLS[ptype]
            px = 70 + (cx - x0) * scale
            py = 30 + (cz - z0) * scale
            dr.rectangle([px-3, py-3, px+3, py+3], fill=col, outline=(0,0,0))
            if len(comp) >= 3:
                dr.text((px+5, py-5), f"{cid}", fill=(0,0,0), font=font)

with open("data/pad_clusters.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)
dr.text((72, 902), "Yeti Set Go pad clusters (numbers = P id in data/pad_clusters.csv; grey=lodestone elytra zones)", fill=(20,20,20), font=big)
img.save("renders/pads_map.png")
print(f"{len(rows)} clusters -> data/pad_clusters.csv, renders/pads_map.png")
