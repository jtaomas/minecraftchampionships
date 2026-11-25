import csv
from collections import defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
col=defaultdict(list); gold=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    x,y,z=int(row["x"]),int(row["y"]),int(row["z"]); n=row["block"].replace("minecraft:","")
    col[(x,z)].append((y,n))
    if y==69 and n.startswith("gold_block"): gold.append((x,z))
print("gold y69:", sorted(gold))
# cluster gold plates
seen=set(); plates=[]
gs=set(gold)
for p in gold:
    if p in seen: continue
    stack=[p]; comp=[]
    while stack:
        q=stack.pop()
        if q in seen or q not in gs: continue
        seen.add(q); comp.append(q)
        x,z=q
        for d in [(1,0),(-1,0),(0,1),(0,-1)]:
            stack.append((x+d[0],z+d[1]))
    plates.append(comp)
for c in plates:
    print("plate", len(c), "center", round(sum(a for a,b in c)/len(c),2), round(sum(b for a,b in c)/len(c),2), "bbox", min(a for a,b in c),max(a for a,b in c),min(b for a,b in c),max(b for a,b in c))

for p in [(-5,5),(-5,6),(0,4),(0,5),(-9,12),(-9,13)]:
    print("\ncolumn",p)
    for y,n in sorted(col[p]): print(" ",y,n)
