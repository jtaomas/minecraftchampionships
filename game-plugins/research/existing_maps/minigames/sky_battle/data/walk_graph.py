import csv,collections
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
solid={}; names={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    b=r["block"].replace("minecraft:","")
    names[(x,y,z)]=b
    if b!="barrier": solid[(x,y,z)]=b
# walkable: solid with 2 air above, inside envelope
def air(p): return p not in solid
walk=set()
for (x,y,z),b in solid.items():
    if b in ("oak_fence","oak_leaves","chain","cobblestone_wall","stone_brick_wall","polished_blackstone_wall","spruce_fence","campfire","lectern"): continue
    if air((x,y+1,z)) and air((x,y+2,z)):
        walk.add((x,y,z))
print("walkable",len(walk))
# components, step dx,dz in -1..1, dy in -1..1
seen=set(); comps=[]
for p in walk:
    if p in seen: continue
    st=[p]; seen.add(p); comp=[]
    while st:
        x,y,z=st.pop(); comp.append((x,y,z))
        for dx in (-1,0,1):
            for dz in (-1,0,1):
                if dx==0 and dz==0: continue
                for dy in (-1,0,1):
                    q=(x+dx,y+dy,z+dz)
                    if q in walk and q not in seen:
                        seen.add(q); st.append(q)
    comps.append(comp)
comps.sort(key=len,reverse=True)
for i,c in enumerate(comps[:12]):
    xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
    print(f"W{i:02d} n={len(c):5d} x={min(xs)}..{max(xs)} y={min(ys)}..{max(ys)} z={min(zs)}..{max(zs)}")
print("components",len(comps))
# which comp contains team spawns / ring top
for pt in [(1970,22,0),(2000,22,30),(2030,22,0),(2000,22,-30),(1985,37,0),(1981,37,0),(2000,25,-19),(2000,30,0),(2000,29,0)]:
    print("point",pt,"in walk",pt in walk)
