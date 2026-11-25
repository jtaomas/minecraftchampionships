import csv, sys
from collections import Counter, defaultdict

isl = int(sys.argv[1])
rows = [r for r in csv.DictReader(open("data/blocks_positions.csv", encoding="utf-8")) if int(r["island"]) == isl]
pts = [(int(r["x"]), int(r["y"]), int(r["z"]), r["block"].replace("minecraft:", "")) for r in rows]
print("blocks", len(pts), "x", min(p[0] for p in pts), max(p[0] for p in pts),
      "y", min(p[1] for p in pts), max(p[1] for p in pts),
      "z", min(p[2] for p in pts), max(p[2] for p in pts))
c = Counter(p[3] for p in pts)
print("palette:", ", ".join(f"{k}={v}" for k, v in c.most_common(20)))
yh = Counter(p[1] for p in pts)
print("y hist:", " ".join(f"{y}:{yh[y]}" for y in sorted(yh)))
print("per-y top block:")
byy = defaultdict(Counter)
for x, y, z, b in pts:
    byy[y][b] += 1
for y in sorted(byy):
    print(f"  y={y:3d} n={sum(byy[y].values()):5d}  " + ", ".join(f"{k}={v}" for k, v in byy[y].most_common(4)))
