#!/usr/bin/env python3
"""Shortest-path distances and greedy tour over the City Marathon ground network."""
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
# build adjacency
cells={}
for k in solid:
    y=walk(*k)
    if y is not None: cells[k]=y
adj={k:[] for k in cells}
for (x,z),y in cells.items():
    for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
        n=(x+dx,z+dz)
        if n in cells and abs(cells[n]-y)<=1:
            adj[(x,z)].append(n)
print("cells",len(cells),"edges",sum(len(v) for v in adj.values())//2)
def bfs(src):
    dist={src:0}; q=deque([src])
    while q:
        u=q.popleft()
        for v in adj[u]:
            if v not in dist: dist[v]=dist[u]+1; q.append(v)
    return dist
pads=json.load(open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\yellow_centers.json")) if False else None
# hardcode strip centers from earlier clustering
centers=[(-476,-51),(-476,5),(-468,57),(-443,88),(-396,52),(-395,88),(-362,81),(-362,95),(-360,-55),(-337,52),(-337,124),(-335,-47),(-334,88),(-310,-51),(-311,54),(-311,126),(-294,37),(-295,84),(-295,92),(-294,139),(-268,88),(-244,139),(-243,37),(-225,-67),(-178,-66),(-172,88),(-159,96),(-150,88),(-140,120),(-85,88),(-67,49),(-67,131),(-52,84),(-52,92),(-27,88),(-3,88),(4,33),(8,20),(8,69),(12,43)]
# snap to walkable
def snap(x,z):
    best=None;bd=999
    for (a,b),y in cells.items():
        d=abs(a-x)+abs(b-z)
        if d<bd: bd=d;best=(a,b)
    return best
start=snap(8,5)
print("start snap",start)
# all-pairs greedy from start among strips
nodes=[(n,snap(*n)) for n in centers]
print([ (n,s) for n,s in nodes if s is None])
cur=start; remaining=nodes[:]; order=[]; total=0
while remaining:
    dist=bfs(cur)
    # choose nearest reachable
    cands=[(dist[s],i) for i,(n,s) in enumerate(remaining) if s in dist]
    if not cands: print("unreachable remain",len(remaining)); break
    d,i=min(cands); n,s=remaining.pop(i)
    order.append((n,s,d)); total+=d; cur=s
print("total steps",total,"order:")
for n,s,d in order: print(n,s,"dist",d)
