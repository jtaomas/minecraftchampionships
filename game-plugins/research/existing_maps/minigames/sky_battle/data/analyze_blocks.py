import csv, collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
print("total",len(rows))
c=collections.Counter(r["block"].replace("minecraft:","") for r in rows)
for k,v in c.most_common(60): print(f"{k:40s} {v:6d} {100*v/len(rows):5.2f}%")
print("\n--- y layers ---")
byy=collections.defaultdict(list)
for r in rows: byy[int(r["y"])].append(r)
for y in sorted(byy):
    rr=byy[y]; nb=[r for r in rr if "barrier" not in r["block"]]
    xs=[int(r["x"]) for r in rr]; zs=[int(r["z"]) for r in rr]
    nbx=[int(r["x"]) for r in nb]; nbz=[int(r["z"]) for r in nb]
    print(f"y={y:3d} total={len(rr):5d} solid={len(nb):5d} x={min(xs)}..{max(xs)} z={min(zs)}..{max(zs)}" + (f" solidxz={min(nbx)}..{max(nbx)}/{min(nbz)}..{max(nbz)}" if nb else ""))
