#!/usr/bin/env python3
"""Annotated route furniture map for City Marathon."""
import gzip, json, os
from PIL import Image, ImageDraw
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
OUT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"
origin=(-536,-64,-74)
COL={
 "gray_wool":(45,48,55),"yellow_wool":(140,120,20),"yellow_glazed_terracotta":(210,150,20),
 "polished_diorite":(0,220,220),"err":(255,0,0),"orange_wool":(255,110,0),"lime_wool":(130,255,0),
 "red_glazed_terracotta":(220,30,30),"pink_glazed_terracotta":(255,120,180),"magenta_glazed_terracotta":(255,0,255),
 "yellow_concrete":(255,255,150),"green_carpet":(0,255,80),"barrier":(150,0,0),
 "gray_concrete":(80,80,80),"white_concrete":(180,180,180),"black_concrete":(10,10,10),
 "grass_block":(40,80,30),"moss_block":(35,65,25),"water":(30,60,120),"glass":(80,130,150),
 "stone_bricks":(70,70,70),"bricks":(90,50,40),"mud_bricks":(60,45,35),"white_terracotta":(120,100,90),
 "deepslate_tile_slab":(40,40,42),"polished_andesite":(70,72,72),"smooth_stone":(90,90,90),
 "light":(255,255,200),
}
def load():
    rows=[]
    with gzip.open(DUMP,"rt",encoding="utf-8") as f:
        hdr=f.readline().split(); sx,sy,sz=map(int,hdr[2:5])
        for line in f:
            p=line.rstrip().split(" ",3)
            if len(p)<4: continue
            rows.append((int(p[0])+origin[0],int(p[1])+origin[1],int(p[2])+origin[2],p[3]))
    return (sx,sy,sz),rows

def clusters(pts):
    S=set(pts); out=[]
    while S:
        seed=S.pop(); comp={seed}; st=[seed]
        while st:
            x,y,z=st.pop()
            for dx in (-1,0,1):
                for dy in (-1,0,1):
                    for dz in (-1,0,1):
                        q=(x+dx,y+dy,z+dz)
                        if q in S: S.remove(q); comp.add(q); st.append(q)
        out.append(comp)
    return out

def main():
    (sx,sy,sz),rows=load()
    top={}
    for x,y,z,b in rows:
        k=(x,z)
        if k not in top or y>top[k][0]: top[k]=(y,b)
    scale=6
    img=Image.new("RGB",(sx*scale,sz*scale),(8,8,12)); px=img.load()
    for (x,z),(y,b) in top.items():
        c=COL.get(b)
        if c is None: continue
        xx=x-origin[0]; zz=z-origin[2]
        for dx in range(scale):
            for dz in range(scale):
                px[xx*scale+dx,zz*scale+dz]=c
    d=ImageDraw.Draw(img)
    for x in range(-525,23,25):
        X=(x-origin[0])*scale; d.line([(X,0),(X,img.height)],fill=(35,35,45)); d.text((X+1,1),str(x),fill=(160,160,160))
    for z in range(-50,167,25):
        Z=(z-origin[2])*scale; d.line([(0,Z),(img.width,Z)],fill=(35,35,45)); d.text((1,Z+1),str(z),fill=(160,160,160))
    # pads
    for name,color,label in [("yellow_glazed_terracotta",(255,210,0),"S"),("red_glazed_terracotta",(255,60,60),"P"),
                             ("magenta_glazed_terracotta",(255,0,255),"P"),("orange_wool",(255,120,0),"O"),
                             ("lime_wool",(120,255,0),"L"),("green_carpet",(0,255,80),"G"),
                             ("yellow_concrete",(255,255,120),"C"),("polished_diorite",(0,255,255),"E")]:
        pts=[(x,y,z) for x,y,z,b in rows if b==name]
        cs=clusters(pts)
        for c in cs:
            xs=[p[0] for p in c]; ys=[p[1] for p in c]; zs=[p[2] for p in c]
            cx=(min(xs)+max(xs))/2; cy=(min(ys)+max(ys))/2; cz=(min(zs)+max(zs))/2
            if len(c)==1 and name=="polished_diorite": continue
            X=(cx-origin[0])*scale; Z=(cz-origin[2])*scale
            r=4 if len(c)<20 else 6
            d.ellipse([X-r,Z-r,X+r,Z+r],outline=color,width=2)
            d.text((X+5,Z-5),f"{label}{len(c)}",fill=color)
    # command clusters
    be=json.load(open(BASE+r"\block_entities.json",encoding="utf-8"))
    cmds=[(b["x"],b["y"],b["z"]) for b in be if b["id"]=="minecraft:command_block"]
    for x,y,z in cmds:
        X=(x-origin[0])*scale; Z=(z-origin[2])*scale
        d.rectangle([X-2,Z-2,X+2,Z+2],fill=(255,0,255))
    # signs
    for b in be:
        if b["id"]=="minecraft:sign":
            X=(b["x"]-origin[0])*scale; Z=(b["z"]-origin[2])*scale
            d.rectangle([X-3,Z-3,X+3,Z+3],outline=(255,255,255),width=2)
    # start
    X=(8-origin[0])*scale; Z=(5-origin[2])*scale
    d.ellipse([X-10,Z-10,X+10,Z+10],outline=(0,255,0),width=3)
    d.text((X+12,Z-12),"START 8,0,5",fill=(0,255,0))
    img.save(OUT+os.sep+"route_furniture.png")
    print("saved",img.size)

if __name__=="__main__": main()
