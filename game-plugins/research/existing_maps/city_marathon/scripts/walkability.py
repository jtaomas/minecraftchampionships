#!/usr/bin/env python3
"""Ground walkability graph of City Marathon from the block dump."""
import gzip, json, sys
from collections import deque, Counter
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
solid={}
with gzip.open(DUMP,"rt",encoding="utf-8") as f:
    f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if y<=8:
            solid.setdefault((x,z),[]).append((y,p[3]))
# walkable top: highest solid <=5, and no solid at +1,+2 (headroom); allow slab/stairs
def walk(x,z):
    col=solid.get((x,z))
    if not col: return None
    ys=sorted(y for y,b in col if y<=5)
    if not ys: return None
    y=ys[-1]
    # headroom: no block at y+2 (feet at y+1, head y+2)
    occ={yy:b for yy,b in col}
    if (y+2) in occ: return None
    return y
start=(8,5)
comp={}
q=deque([start]); comp[start]=0
while q:
    x,z=q.popleft(); y0=walk(x,z)
    for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
        n=(x+dx,z+dz)
        if n in comp: continue
        y1=walk(*n)
        if y1 is None: continue
        if y0 is None: continue
        if abs(y1-y0)>1: continue
        comp[n]=comp[(x,z)]+1
        q.append(n)
print("reachable cells",len(comp))
# pad centers
pads={
 "start":(8,5),"gate":(8,53),"park_east":(-91,88),"park_plaza":(-145,85),"plaza_dome":(-245,88),
 "station_floor":(-457,88),"train_stairs":(-457,86),"platform_n":(-351,23),"platform_s":(-351,154),
 "highway_w":(-476,-50),"spur_s":(-476,5),"highway_mid":(-300,-50),"east_junction":(8,85),
 "roof_pad1":(-216,25),"roof_pad2":(-216,49),"roof_pad3":(-216,127),"roof_pad4":(-216,151),
 "roof_pad5":(-127,-66),"roof_pad6":(-127,-36),"ladder_n":(-243,-37),
}
for k,(x,z) in pads.items():
    print(f"{k:14s} {x:5d},{z:4d} reachable={ (x,z) in comp } groundY={walk(x,z)}")
print("start comp size",len(comp))
# unreachable map preview
minx=min(x for x,z in comp); maxx=max(x for x,z in comp); minz=min(z for x,z in comp); maxz=max(z for x,z in comp)
print("comp bbox",minx,maxx,minz,maxz)
from PIL import Image,ImageDraw
img=Image.new("RGB",(maxx-minx+1,maxz-minz+1),(10,10,10)); px=img.load()
for (x,z) in comp: px[x-minx,z-minz]=(60,180,60)
img.save(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\walkable_component.png")
