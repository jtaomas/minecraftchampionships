#!/usr/bin/env python3
"""Find clusters of a block name in the full dump."""
import gzip, sys
from collections import defaultdict
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
want = sys.argv[1]
rows = []
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        if p[3].strip().split("[")[0] == want:
            rows.append((int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]))
print(want, len(rows))
# grid cluster by 12 blocks
buckets = defaultdict(list)
for x, y, z in rows:
    buckets[(x // 12, z // 12)].append((x, y, z))
for k, v in sorted(buckets.items(), key=lambda kv: -len(kv[1]))[:20]:
    xs = [q[0] for q in v]; ys = [q[1] for q in v]; zs = [q[2] for q in v]
    print(f"  n={len(v):4d} x {min(xs)}..{max(xs)} y {min(ys)}..{max(ys)} z {min(zs)}..{max(zs)}")
