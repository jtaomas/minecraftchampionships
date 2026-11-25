import gzip
from collections import Counter
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
water=[]
with gzip.open(DUMP,"rt",encoding="utf-8") as f:
    f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if p[3] in ("water","bubble_column","soul_sand","magma_block"):
            water.append((x,y,z,p[3]))
print(len(water))
# cluster 3d
S=set((x,y,z) for x,y,z,b in water); comps=[]
while S:
    seed=S.pop(); c={seed}; st=[seed]
    while st:
        x,y,z=st.pop()
        for dx in (-1,0,1):
            for dy in (-1,0,1):
                for dz in (-1,0,1):
                    q=(x+dx,y+dy,z+dz)
                    if q in S: S.remove(q); c.add(q); st.append(q)
    comps.append(c)
comps.sort(key=len, reverse=True)
for c in comps:
    xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
    print(len(c), "bbox", min(xs),min(ys),min(zs),"->",max(xs),max(ys),max(zs))
