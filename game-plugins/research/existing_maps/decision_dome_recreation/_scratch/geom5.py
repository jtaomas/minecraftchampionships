import csv, math
from collections import Counter, defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0],row["block"]))
B={(x,y,z):n for x,y,z,n,_ in rows}

def runs(vals):
    vals=sorted(vals); out=[]; s=p=None
    for v in vals:
        if s is None: s=p=v
        elif v==p+1: p=v
        else: out.append((s,p)); s=p=v
    if s is not None: out.append((s,p))
    return out

print("floor y68 solid x runs at z=16:", runs([x for x in range(-50,50) if B.get((x,68,16)) in ("white_concrete","gray_concrete","black_concrete","smooth_quartz_stairs","quartz_slab")]))
print("floor y68 solid z runs at x=-35:", runs([z for z in range(-30,50) if B.get((-35,68,z)) in ("white_concrete","gray_concrete","black_concrete","smooth_quartz_stairs","quartz_slab")]))
print("floor y68 solid z runs at x=30:", runs([z for z in range(-30,50) if B.get((30,68,z)) in ("white_concrete","gray_concrete","black_concrete","smooth_quartz_stairs","quartz_slab")]))
print("y68 blocks along z=16 x -46..-26:")
for x in range(-46,-25):
    print(" ",x,B.get((x,68,16)),B.get((x,67,16)),B.get((x,69,16)))
print("\ny68 blocks along x=-35 z 4..28:")
for z in range(4,29):
    print(" ",z,B.get((-35,68,z)),B.get((-35,67,z)),B.get((-35,69,z)))
print("\ny70 north path along z=-19 x -12..6:")
for x in range(-12,7):
    print(" ",x,B.get((x,70,-19)),B.get((x,71,-19)),B.get((x,69,-19)))
print("\ny70 north path along x=-3 z -22..-6:")
for z in range(-22,-5):
    print(" ",z,B.get((-3,70,z)),B.get((-3,71,z)),B.get((-3,69,z)))
print("\npressure plate / command context:")
for y in range(67,73):
    print("y",y,[(x,B.get((x,y,-19))) for x in range(-5,-1)])
print("\nwalkable y68 count and materials:", Counter(n for (x,y,z),n in B.items() if y==68 and n in ("white_concrete","gray_concrete","black_concrete")))
print("\nExtents of white concrete terrace y68:", [(x,z) for (x,y,z),n in B.items() if y==68 and n=="white_concrete"][:5])
pts=[(x,z) for (x,y,z),n in B.items() if y==68 and n=="white_concrete"]
print("white x",min(p[0] for p in pts),max(p[0] for p in pts),"z",min(p[1] for p in pts),max(p[1] for p in pts))
