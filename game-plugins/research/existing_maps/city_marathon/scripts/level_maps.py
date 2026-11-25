#!/usr/bin/env python3
"""Per-level road maps from the dump."""
import gzip, os
from PIL import Image, ImageDraw
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
OUT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"
origin=(-536,-64,-74)
def load():
    rows=[]
    with gzip.open(DUMP,"rt",encoding="utf-8") as f:
        hdr=f.readline().split(); sx,sy,sz=map(int,hdr[2:5])
        for line in f:
            p=line.rstrip().split(" ",3)
            if len(p)<4: continue
            rows.append((int(p[0])+origin[0],int(p[1])+origin[1],int(p[2])+origin[2],p[3]))
    return (sx,sy,sz),rows
(sx,sy,sz),rows=load()
print(sx,sy,sz,len(rows))
colors={
 "gray_wool":(60,63,70),"yellow_wool":(255,210,0),"yellow_glazed_terracotta":(255,160,0),
 "polished_diorite":(0,255,255),"orange_wool":(255,110,0),"lime_wool":(130,255,0),
 "red_glazed_terracotta":(255,40,40),"pink_glazed_terracotta":(255,120,180),"magenta_glazed_terracotta":(255,0,255),
 "yellow_concrete":(255,255,150),"green_carpet":(0,255,80),"barrier":(160,0,0),
 "gray_concrete":(110,110,110),"smooth_stone":(120,120,120),"stone_slab":(100,100,100),
 "white_concrete":(240,240,240),"black_concrete":(20,20,20),"purple_concrete":(120,60,160),
}
for ylev in (-1,0,4,8,17,28):
    pts={}
    for x,y,z,b in rows:
        if y!=ylev: continue
        c=colors.get(b)
        if c is None: continue
        pts[(x,z)]=c
    scale=4
    img=Image.new("RGB",(sx*scale,sz*scale),(8,8,10)); px=img.load()
    for (x,z),c in pts.items():
        xx=x-origin[0]; zz=z-origin[2]
        for dx in range(scale):
            for dz in range(scale):
                px[xx*scale+dx,zz*scale+dz]=c
    d=ImageDraw.Draw(img)
    for x in range(-525,23,25):
        X=(x-origin[0])*scale; d.line([(X,0),(X,img.height)],fill=(45,45,55)); d.text((X+1,1),str(x),fill=(160,160,160))
    for z in range(-50,167,25):
        Z=(z-origin[2])*scale; d.line([(0,Z),(img.width,Z)],fill=(45,45,55)); d.text((1,Z+1),str(z),fill=(160,160,160))
    img.save(os.path.join(OUT,f"level_{ylev}.png"))
    print("saved",ylev,len(pts))
