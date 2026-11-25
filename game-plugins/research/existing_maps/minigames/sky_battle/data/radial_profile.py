import csv,math,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
prof=collections.defaultdict(list)
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    d=math.hypot(x-2000,z)
    if d<45: prof[round(d)].append((y,r["block"].replace("minecraft:","")))
print("radius  n   ymin ymax  top-block-types")
for d in sorted(prof):
    if d>25: continue
    ys=[p[0] for p in prof[d]]
    tops=collections.Counter(p[1] for p in prof[d] if p[0]==max(ys))
    print(f"{d:3d} {len(prof[d]):5d} {min(ys):4d} {max(ys):4d}  {tops.most_common(4)}")
