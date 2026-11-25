import json, collections
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
sel = json.load(open(base + r"\selected_blocks.json", encoding="utf-8"))

def clusters(name):
    pts=[(x,y,z,st) for x,y,z,st,_ in sel if st.split("[")[0].endswith(name)]
    S={(x,y,z) for x,y,z,_ in pts}; out=[]
    while S:
        seed=S.pop(); comp={seed}; stack=[seed]
        while stack:
            x,y,z=stack.pop()
            for dx in (-1,0,1):
                for dy in (-1,0,1):
                    for dz in (-1,0,1):
                        q=(x+dx,y+dy,z+dz)
                        if q in S:
                            S.remove(q); comp.add(q); stack.append(q)
        out.append(comp)
    out.sort(key=lambda c:(min(p[0] for p in c),min(p[2] for p in c)))
    return out

for name in ["yellow_glazed_terracotta","orange_wool","lime_wool","yellow_concrete","green_carpet","red_glazed_terracotta","pink_glazed_terracotta","magenta_glazed_terracotta","polished_diorite"]:
    cs=clusters(name)
    print("\n==",name,len(cs),"clusters")
    for c in cs:
        xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
        print(f"  n={len(c):3d} bbox {min(xs)},{min(ys)},{min(zs)} -> {max(xs)},{max(ys)},{max(zs)} center {(min(xs)+max(xs))/2:.1f},{(min(ys)+max(ys))/2:.1f},{(min(zs)+max(zs))/2:.1f}")
