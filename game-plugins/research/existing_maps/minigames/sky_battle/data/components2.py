import csv,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
S={(int(r["x"]),int(r["y"]),int(r["z"])) for r in rows}
B={ (int(r["x"]),int(r["y"]),int(r["z"])):r["block"].replace("minecraft:","") for r in rows}
seen=set(); comps=[]
for p in S:
    if p in seen: continue
    st=[p]; seen.add(p); comp=[]
    while st:
        x,y,z=st.pop(); comp.append((x,y,z))
        for q in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
            if q in S and q not in seen:
                seen.add(q); st.append(q)
    comps.append(comp)
comps.sort(key=len,reverse=True)
for i,c in enumerate(comps[:20]):
    xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
    tc=collections.Counter(B[p] for p in c).most_common(5)
    print(f"C{i:02d} n={len(c):5d} x={min(xs)}..{max(xs)} y={min(ys)}..{max(ys)} z={min(zs)}..{max(zs)} {tc}")
