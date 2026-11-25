#!/usr/bin/env python3
"""Crop a mcmdump to bounds (world coords) and write a new dump."""
import gzip, sys
SRC=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
x1,x2,y1,y2,z1,z2=map(int,sys.argv[1:7]); dst=sys.argv[7]
rows=[]
with gzip.open(SRC,"rt",encoding="utf-8") as f:
    hdr=f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if x1<=x<=x2 and y1<=y<=y2 and z1<=z<=z2:
            rows.append((x,y,z,p[3]))
with gzip.open(dst,"wt",encoding="utf-8") as f:
    f.write(f"MCMDUMP 1 {x2-x1+1} {y2-y1+1} {z2-z1+1}\n")
    for x,y,z,b in rows:
        f.write(f"{x-x1} {y-y1} {z-z1} {b}\n")
print("crop",len(rows),"->",dst)
