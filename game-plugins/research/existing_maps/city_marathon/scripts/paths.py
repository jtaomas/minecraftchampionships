#!/usr/bin/env python3
"""Compute and render ground-network paths between City Marathon waypoints."""
import gzip, json
from collections import deque
from PIL import Image, ImageDraw
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
OUT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"
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
    # nearest walkable cell preferring same column
    for r in range(0,20):
        for dx in range(-r,r+1):
            for dz in range(-r,r+1):
                if abs(dx)+abs(dz)!=r: continue
                if (x+dx,z+dz) in cells: return (x+dx,z+dz)
    return None
def bfs(src,dst):
    prev={src:None}; q=deque([src])
    while q:
        u=q.popleft()
        if u==dst: break
        for v in adj[u]:
            if v not in prev: prev[v]=u; q.append(v)
    if dst not in prev: return None
    path=[]; u=dst
    while u is not None: path.append(u); u=prev[u]
    return path[::-1]
def simplify(path, step=8):
    if len(path)<=2: return path
    out=[path[0]]
    for i in range(step,len(path)-1,step): out.append(path[i])
    out.append(path[-1])
    return out
paths={
 "A_start_to_station":[(8,5),(-457,86)],
 "B_station_to_highway": [(-457,86),(-476,-50)],
 "C_highway_to_start": [(-476,-50),(8,5)],
 "D_start_to_park":[(8,5),(-145,85)],
 "E_park_to_station":[(-145,85),(-457,86)],
}
img=Image.open(OUT+r"\topdown_raw.png").convert("RGB")
# topdown_raw is 4 px/block, origin -536,-74
S=4
d=ImageDraw.Draw(img)
cols={"A_start_to_station":(0,255,0),"B_station_to_highway":(0,180,255),"C_highway_to_start":(255,80,255),
      "D_start_to_park":(255,255,0),"E_park_to_station":(255,120,0)}
result={}
for name,(a,b) in paths.items():
    sa,sb=snap(*a),snap(*b)
    p=bfs(sa,sb)
    print(name,a,b,"snap",sa,sb,"len",len(p) if p else None)
    if p:
        result[name]=[[x,z] for x,z in p]
        pts=[((x+536)*S,(z+74)*S) for x,z in simplify(p)]
        d.line(pts,fill=cols[name],width=3)
        for x,z in [p[0],p[-1]]:
            X=(x+536)*S;Z=(z+74)*S; d.ellipse([X-5,Z-5,X+5,Z+5],outline=cols[name],width=2)
img.save(OUT+r"\paths_overlay.png")
json.dump(result,open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\paths.json","w"))
print("saved paths overlay")
