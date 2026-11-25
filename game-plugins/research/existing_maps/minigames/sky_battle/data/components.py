import csv, collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
S={(int(r["x"]),int(r["y"]),int(r["z"])) for r in rows}
print("solid blocks",len(S))
# 6-connectivity components
seen=set(); comps=[]
for p in S:
    if p in seen: continue
    stack=[p]; seen.add(p); comp=[]
    while stack:
        x,y,z=stack.pop(); comp.append((x,y,z))
        for q in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
            if q in S and q not in seen:
                seen.add(q); stack.append(q)
    comps.append(comp)
comps.sort(key=len,reverse=True)
for i,c in enumerate(comps[:20]):
    xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
    print(f"C{i:02d} n={len(c):5d} x={min(xs)}..{max(xs)} y={min(ys)}..{max(ys)} z={min(zs)}..{max(zs)}")
print("components",len(comps))
