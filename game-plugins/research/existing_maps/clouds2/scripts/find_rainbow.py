#!/usr/bin/env python3
"""Find the rainbow: columns in the bottom-left with many distinct bright colours."""
import gzip, re
from collections import defaultdict, Counter
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
COLOURS = ("white","orange","magenta","light_blue","yellow","lime","pink","gray",
           "light_gray","cyan","purple","blue","brown","green","red","black")
box = (-200, -100, 60, 140)
cols = defaultdict(set)
pts = []
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        if not (box[0] <= x <= box[1] and box[2] <= z <= box[3]): continue
        n = p[3].strip().split("[")[0].replace("minecraft:", "")
        m = re.match(r"(white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black)_(wool|concrete|terracotta|stained_glass)", n)
        if m:
            cols[(x, z)].add(m.group(1))
            pts.append((x, y, z, n))
best = sorted(cols.items(), key=lambda kv: -len(kv[1]))[:20]
for (x, z), s in best:
    print(f"x={x:5d} z={z:5d}  {len(s)} colours: {sorted(s)}")
print("\ncount of coloured blocks:", len(pts))
ys = [p[1] for p in pts]
print("y range", min(ys), max(ys))
