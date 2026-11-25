#!/usr/bin/env python3
"""Top-down of the highest block at or below YMAX, cropped, with palette."""
import gzip, sys
from PIL import Image, ImageDraw
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
OUT=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"
origin=(-536,-64,-74)
COL={
 "gray_wool":(63,68,72),"yellow_wool":(248,198,39),"yellow_glazed_terracotta":(234,192,88),
 "polished_diorite":(205,207,209),"orange_wool":(240,118,19),"lime_wool":(112,185,25),
 "red_glazed_terracotta":(179,74,60),"pink_glazed_terracotta":(213,123,135),"magenta_glazed_terracotta":(206,80,180),
 "yellow_concrete":(241,175,21),"green_carpet":(85,110,27),"barrier":(180,20,20),
 "gray_concrete":(110,110,110),"smooth_stone":(158,158,158),"stone_slab":(125,125,125),
 "polished_andesite":(132,134,133),"white_concrete":(207,213,214),"black_concrete":(20,20,20),
 "light_gray_concrete":(125,125,115),"cyan_terracotta":(87,91,91),"granite":(149,103,85),
 "bricks":(150,84,68),"stone_bricks":(122,121,122),"mud_bricks":(92,68,58),
 "grass_block":(95,159,53),"moss_block":(89,109,45),"dirt_path":(148,121,65),
 "oak_planks":(162,130,78),"spruce_planks":(114,84,48),"dark_oak_planks":(66,43,20),
 "glass":(160,210,230),"water":(52,98,190),"red_concrete":(142,32,32),"blue_concrete":(44,46,143),
 "white_terracotta":(210,178,161),"terracotta":(152,94,67),"deepslate_tile_slab":(55,55,57),
 "stone":(125,125,125),"andesite":(132,134,133),"sandstone":(216,203,155),
}
def main():
    x1,x2,y1,y2,z1,z2=map(int,sys.argv[1:7]); scale=int(sys.argv[7]); name=sys.argv[8]; grid=int(sys.argv[9])
    top={}
    with gzip.open(DUMP,"rt",encoding="utf-8") as f:
        f.readline()
        for line in f:
            p=line.rstrip().split(" ",3)
            if len(p)<4: continue
            x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
            if not (x1<=x<=x2 and y1<=y<=y2 and z1<=z<=z2): continue
            k=(x,z)
            if k not in top or y>top[k][0]: top[k]=(y,p[3])
    img=Image.new("RGB",((x2-x1+1)*scale,(z2-z1+1)*scale),(12,12,16)); px=img.load()
    for (x,z),(y,b) in top.items():
        c=COL.get(b)
        if c is None:
            h=hash(b); c=(70+(h%150),70+((h>>8)%150),70+((h>>16)%150))
        f=0.72+min(0.4,max(-0.25,y/60))
        c=tuple(min(255,int(v*f)) for v in c)
        for dx in range(scale):
            for dz in range(scale):
                px[(x-x1)*scale+dx,(z-z1)*scale+dz]=c
    d=ImageDraw.Draw(img)
    for x in range((x1//grid)*grid,x2+1,grid):
        X=(x-x1)*scale; d.line([(X,0),(X,img.height)],fill=(250,250,250)); d.text((X+1,1),str(x),fill=(255,255,255))
    for z in range((z1//grid)*grid,z2+1,grid):
        Z=(z-z1)*scale; d.line([(0,Z),(img.width,Z)],fill=(250,250,250)); d.text((1,Z+1),str(z),fill=(255,255,255))
    img.save(OUT+os.sep+name)
    print("saved",name,img.size,len(top))
import os
main()
