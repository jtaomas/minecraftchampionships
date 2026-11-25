import csv, math
from collections import Counter, defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0],row["block"]))
B={(x,y,z):n for x,y,z,n,_ in rows}

def runs(vals):
    vals=sorted(vals); out=[]; s=None; p=None
    for v in vals:
        if s is None: s=p=v
        elif v==p+1: p=v
        else: out.append((s,p)); s=p=v
    if s is not None: out.append((s,p))
    return out

for z in [0,8,16,24,32]:
    print(f"\nz={z} at y58 gray:", runs([x for x in range(-50,50) if B.get((x,58,z))=="gray_concrete"]))
    print(f"z={z} at y58 white:", runs([x for x in range(-50,50) if B.get((x,58,z))=="white_concrete"]))
for x in [-3,0,8,-12]:
    print(f"\nx={x} at y58 gray z:", runs([z for z in range(-30,50) if B.get((x,58,z))=="gray_concrete"]))

print("\npod centroids at y70 by colour:")
for word in ["red","orange","yellow","lime","green","cyan","light_blue","blue","purple","pink"]:
    sel=[(x,z) for (x,y,z,n,_) in rows if y==70 and n==f"{word}_concrete"]
    if sel:
        print(word, len(sel), "centroid", round(sum(a for a,b in sel)/len(sel),2), round(sum(b for a,b in sel)/len(sel),2), "bbox", min(a for a,b in sel),max(a for a,b in sel),min(b for a,b in sel),max(b for a,b in sel))

print("\ny68 white concrete runs x at z=16:", runs([x for x in range(-50,50) if B.get((x,68,16))=="white_concrete"]))
print("y68 white concrete runs z at x=-3:", runs([z for z in range(-30,50) if B.get((-3,68,z))=="white_concrete"]))
print("y68 gray runs x at z=16:", runs([x for x in range(-50,50) if B.get((x,68,16))=="gray_concrete"]))
print("y68 gray runs z at x=-3:", runs([z for z in range(-30,50) if B.get((-3,68,z))=="gray_concrete"]))

print("\nGold at y69/70/71/72 radius hist:")
for y in [69,70,71,72,73]:
    sel=[(x,z) for (x,yy,z,n,_) in rows if yy==y and n=="gold_block"]
    if sel:
        ds=[math.hypot(x+3,z-16) for x,z in sel]
        print(y, len(sel), "r min/max/avg", round(min(ds),2), round(max(ds),2), round(sum(ds)/len(ds),2))

print("\nBarrier positions y71:")
sel=[(x,z) for (x,yy,z,n,_) in rows if yy==71 and n=="barrier"]
print(len(sel), "r range", round(min(math.hypot(x+3,z-16) for x,z in sel),2), round(max(math.hypot(x+3,z-16) for x,z in sel),2))
# barrier by 10-degree bin
bins=Counter(int((math.degrees(math.atan2(z-16,x+3))+360)%360)//10 for x,z in sel)
print(sorted(bins.items()))

print("\nColoured pod footprint at each y (lime only):")
for y in range(26,79):
    sel=[(x,z) for (x,yy,z,n,_) in rows if yy==y and n=="lime_concrete"]
    if sel:
        print(y, len(sel), "x", min(a for a,b in sel), max(a for a,b in sel), "z", min(b for a,b in sel), max(b for a,b in sel))

print("\nFull block palette:")
c=Counter(n for x,y,z,n,_ in rows)
for n,v in c.most_common():
    print(f"{v:6d} {n}")
