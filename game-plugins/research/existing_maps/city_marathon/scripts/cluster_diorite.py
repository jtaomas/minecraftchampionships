import json, collections
base = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
sel = json.load(open(base + r"\selected_blocks.json", encoding="utf-8"))
pts=[(x,y,z) for x,y,z,st,_ in sel if st.startswith("minecraft:polished_diorite")]
# cluster by (y, zish) grid proximity using flood fill on 3D within 1 block
S=set(pts); clusters=[]
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
    clusters.append(comp)
clusters.sort(key=len, reverse=True)
print("clusters",len(clusters))
for c in clusters:
    xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
    print(len(c), "bbox", min(xs),min(ys),min(zs),"->",max(xs),max(ys),max(zs), "center", (min(xs)+max(xs))/2,(min(ys)+max(ys))/2,(min(zs)+max(zs))/2)
