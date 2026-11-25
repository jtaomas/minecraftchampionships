#!/usr/bin/env python3
"""Fill sign block variants in signs.csv."""
import csv, gzip
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
O = (-258, -64, -316)
rows = list(csv.DictReader(open(D + r"\signs.csv", encoding="utf-8-sig")))
want = {(int(r["x"]), int(r["y"]), int(r["z"])) for r in rows}
var = {}
with gzip.open(D + r"\clouds2_full.mcmdump.gz", "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        if (x, y, z) in want:
            var[(x, y, z)] = p[3].strip().split("[")[0].replace("minecraft:", "")
for r in rows:
    r["block"] = var.get((int(r["x"]), int(r["y"]), int(r["z"])), "?")
with open(D + r"\signs.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)
from collections import Counter
print(Counter(r["block"] for r in rows))
