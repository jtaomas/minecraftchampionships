#!/usr/bin/env python3
"""Layer maps from the MCMDUMP: roads, track furniture, barriers."""
import gzip, os
from PIL import Image, ImageDraw

DUMP = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"

def load():
    rows=[]
    with gzip.open(DUMP,"rt",encoding="utf-8") as f:
        hdr=f.readline().split()
        sx,sy,sz=map(int,hdr[2:5])
        for line in f:
            p=line.rstrip().split(" ",3)
            if len(p)<4: continue
            rows.append((int(p[0]),int(p[1]),int(p[2]),p[3]))
    return (sx,sy,sz),rows

def main():
    (sx,sy,sz),rows=load()
    print(sx,sy,sz,len(rows))
    # per-column top
    top={}
    for x,y,z,b in rows:
        k=(x,z)
        if k not in top or y>top[k][0]: top[k]=(y,b)
    colors={
        "yellow_wool":(255,210,0),
        "yellow_glazed_terracotta":(255,170,0),
        "polished_diorite":(0,255,255),
        "orange_wool":(255,120,0),
        "lime_wool":(120,255,0),
        "red_glazed_terracotta":(255,40,40),
        "pink_glazed_terracotta":(255,120,180),
        "magenta_glazed_terracotta":(255,0,255),
        "yellow_concrete":(255,255,120),
        "green_carpet":(0,255,80),
        "barrier":(120,0,0),
        "light":(255,255,220),
        "gray_wool":(45,48,55),
        "smooth_stone":(90,90,90),
        "polished_andesite":(80,84,84),
    }
    scale=4
    img=Image.new("RGB",(sx*scale,sz*scale),(10,10,14)); px=img.load()
    for (x,z),(y,b) in top.items():
        c=colors.get(b)
        if c is None: continue
        for dx in range(scale):
            for dz in range(scale):
                px[x*scale+dx,z*scale+dz]=c
    draw=ImageDraw.Draw(img)
    for x in range(0,sx,25):
        X=x*scale; draw.line([(X,0),(X,img.height)],fill=(50,50,60)); draw.text((X+1,1),str(x-536),fill=(180,180,180))
    for z in range(0,sz,25):
        Z=z*scale; draw.line([(0,Z),(img.width,Z)],fill=(50,50,60)); draw.text((1,Z+1),str(z-74),fill=(180,180,180))
    img.save(os.path.join(OUT,"track_layers.png"))
    print("saved",img.size)

if __name__=="__main__": main()
