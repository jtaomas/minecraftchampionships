#!/usr/bin/env python3
import gzip
from collections import Counter
O = (-258, -64, -316)
c = Counter()
for line in gzip.open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz", "rt", encoding="utf-8"):
    p = line.split(" ", 3)
    if len(p) < 4 or p[0] == "MCMDUMP": continue
    x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
    if 10 <= x <= 45 and 145 <= z <= 190:
        c[p[3].strip()] += 1
for k, v in c.most_common(25):
    print(v, k)
