import gzip, json
from collections import Counter
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
b=[]
y64=[]
with gzip.open(DUMP,"rt",encoding="utf-8") as f:
    f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if p[3].startswith("barrier"): b.append((x,y,z))
        if y==-64: y64.append((x,y,z,p[3]))
print("barriers",len(b),"y",Counter(y for x,y,z in b).most_common())
# cluster 2d by x,z
S={(x,z) for x,y,z in b}; comps=[]
while S:
    seed=S.pop(); c={seed}; st=[seed]
    while st:
        x,z=st.pop()
        for dx in (-1,0,1):
            for dz in (-1,0,1):
                q=(x+dx,z+dz)
                if q in S: S.remove(q); c.add(q); st.append(q)
    comps.append(c)
comps.sort(key=len,reverse=True)
for c in comps[:20]:
    xs=[p[0] for p in c]; zs=[p[1] for p in c]
    print(len(c),"bbox",min(xs),min(zs),"->",max(xs),max(zs))
print("y=-64 blocks",len(y64),Counter(b for *_,b in y64).most_common())
print("y=-64 bbox",(min(x for x,y,z,b in y64),min(z for x,y,z,b in y64),max(x for x,y,z,b in y64),max(z for x,y,z,b in y64)) if y64 else None)
