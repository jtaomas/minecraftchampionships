import csv, sys
from collections import Counter

isl = int(sys.argv[1])
rows = [r for r in csv.DictReader(open("data/blocks_positions.csv", encoding="utf-8")) if int(r["island"]) == isl]
pts = [(int(r["x"]), int(r["y"]), int(r["z"]), r["block"].replace("minecraft:", "")) for r in rows]
bands = [(0, 9), (10, 26), (27, 39), (40, 47), (48, 58), (59, 90)]
for a, b in bands:
    sel = [p for p in pts if a <= p[1] <= b]
    if not sel:
        continue
    xs = [p[0] for p in sel]; zs = [p[2] for p in sel]
    print(f"y {a}..{b}: n={len(sel)} x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)}")
# horizontal slices at key y
for y in sorted(set(p[1] for p in pts)):
    if y % 1:
        continue
    sel = [p for p in pts if p[1] == y]
    xs = sorted(set(p[0] for p in sel)); zs = sorted(set(p[2] for p in sel))
    if len(sel) > 300 and y not in (0, 1):
        print(f"y={y}: n={len(sel)} x {min(xs)}..{max(xs)} z {min(zs)}..{max(zs)}")
