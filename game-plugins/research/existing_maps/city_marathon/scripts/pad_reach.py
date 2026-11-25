#!/usr/bin/env python3
"""Reachability and ground height for every route pad center."""
import gzip, json
from collections import deque
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
solid={}
with gzip.open(DUMP,"rt",encoding="utf-8") as f:
    f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if y<=8: solid.setdefault((x,z),[]).append((y,p[3]))
def walk(x,z):
    col=solid.get((x,z))
    if not col: return None
    ys=sorted(y for y,b in col if y<=5)
    if not ys: return None
    y=ys[-1]; occ={yy:b for yy,b in col}
    if (y+2) in occ: return None
    return y
cells={k:walk(*k) for k in solid if walk(*k) is not None}
adj={k:[] for k in cells}
for (x,z),y in cells.items():
    for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
        n=(x+dx,z+dz)
        if n in cells and abs(cells[n]-y)<=1: adj[(x,z)].append(n)
def snap(x,z):
    for r in range(0,12):
        for dx in range(-r,r+1):
            for dz in range(-r,r+1):
                if abs(dx)+abs(dz)!=r: continue
                if (x+dx,z+dz) in cells: return (x+dx,z+dz)
    return None
start=(8,5); dist={start:0}; q=deque([start])
while q:
    u=q.popleft()
    for v in adj[u]:
        if v not in dist: dist[v]=dist[u]+1; q.append(v)
centers={
"S":[( -476,-51),(-476,5),(-468,57),(-443,88),(-396,52),(-395,88),(-362,81),(-362,95),(-360,-55),(-337,52),(-337,124),(-335,-47),(-334,88),(-310,-51),(-311,54),(-311,126),(-294,37),(-295,84),(-295,92),(-294,139),(-268,88),(-244,139),(-243,37),(-225,-67),(-178,-66),(-172,88),(-159,96),(-150,88),(-140,120),(-85,88),(-67,49),(-67,131),(-52,84),(-52,92),(-27,88),(-3,88),(4,33),(8,20),(8,69),(12,43)],
"P":[( -480,-18),(-473,31),(-461,74),(-425,52),(-385,80),(-385,96),(-311,88),(-280,-47),(-198,-67),(-159,120),(-153,-67),(-134,88),(-112,82),(-48,48),(4,66),(8,53)],
"O":[(-450,-55),(-420,-47),(-417,88),(-390,-51),(-361,52),(-240,-37),(-213,37),(-213,139),(-67,20),(-67,156)],
"L":[(-248,-42)],"G":[(-98,82),(-97,90)],
}
for typ,arr in centers.items():
    print("==",typ)
    for x,z in arr:
        s=snap(x,z)
        print(f"  {x:5d},{z:5d} snap {s} y={cells.get(s) if s else None} dist={dist.get(s) if s else 'UNREACHABLE'}")
