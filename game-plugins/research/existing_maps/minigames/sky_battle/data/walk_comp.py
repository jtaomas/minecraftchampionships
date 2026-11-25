import csv,collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
solid={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"]); b=r["block"].replace("minecraft:","")
    if b!="barrier": solid[(x,y,z)]=b
def air(p): return p not in solid
walk=set()
for (x,y,z),b in solid.items():
    if b in ("oak_fence","oak_leaves","chain","cobblestone_wall","stone_brick_wall","polished_blackstone_wall","spruce_fence","campfire","lectern"): continue
    if air((x,y+1,z)) and air((x,y+2,z)): walk.add((x,y,z))
comp={}; cid=0
for p in walk:
    if p in comp: continue
    st=[p]; comp[p]=cid; c=0
    while st:
        x,y,z=st.pop(); c+=1
        for dx in (-1,0,1):
            for dz in (-1,0,1):
                if dx==0 and dz==0: continue
                for dy in (-1,0,1):
                    q=(x+dx,y+dy,z+dz)
                    if q in walk and q not in comp:
                        comp[q]=cid; st.append(q)
    cid+=1
sizes=collections.Counter(comp.values())
for pt in [(1970,22,0),(2000,22,30),(2030,22,0),(2000,22,-30),(1985,37,0),(2000,37,0),(2000,25,-19),(2000,19,0),(2000,25,0),(2000,27,0),(1981,25,0),(2019,25,0)]:
    c=comp.get(pt)
    print("point",pt,"walk",pt in walk,"comp",c,"size",sizes.get(c))
print("top comps:",[(k,v) for k,v in sizes.most_common(12)])
