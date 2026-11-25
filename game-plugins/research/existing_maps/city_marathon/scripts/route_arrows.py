#!/usr/bin/env python3
"""Add direction arrows and labels to the route overlay."""
import os, math
from PIL import Image, ImageDraw, ImageFont
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
OUT=os.path.join(BASE,"renders")
S=6; OX,OZ=-536,-74
img=Image.open(os.path.join(OUT,"route_overlay.png")).convert("RGB")
d=ImageDraw.Draw(img)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",24)
fonts=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",17)
def P(x,z): return ((x-OX)*S,(z-OZ)*S)
def arrow(x1,z1,x2,z2,col=(255,255,255)):
    X1,Z1=P(x1,z1); X2,Z2=P(x2,z2)
    mx,my=(X1+X2)/2,(Z1+Z2)/2
    ang=math.atan2(Z2-Z1,X2-X1)
    L=16; W=9
    pts=[(mx+math.cos(ang)*L,my+math.sin(ang)*L),
         (mx+math.cos(ang+2.5)*W,my+math.sin(ang+2.5)*W),
         (mx+math.cos(ang-2.5)*W,my+math.sin(ang-2.5)*W)]
    d.polygon(pts,fill=col)
# main route segments with arrows
main=[(8,5),(8,10),(8,53),(8,88),(-3,88),(-27,88),(-52,88),(-85,88),(-91,88),
      (-145,88),(-172,88),(-208,88),(-225,88),(-225,60),(-225,44),(-243,37),
      (-294,37),(-311,44),(-311,54),(-337,52),(-361,52),(-388,51),(-425,52),
      (-457,52),(-457,70),(-457,88),(-476,86),(-476,20),(-476,-50),
      (-400,-50),(-300,-50),(-200,-50),(-100,-50),(8,-50),(8,5)]
for i in range(0,len(main)-1,2):
    arrow(*main[i],*main[i+1],(255,255,255))
# labels
d.text(P(30,80),"EAST ROAD (x~8)",font=fontb,fill=(120,220,255))
d.text(P(-120,80),"CITY AVENUE (z88)",font=fontb,fill=(120,220,255))
d.text(P(-300,28),"PLAZA NORTH (z44)",font=fontb,fill=(255,180,255))
d.text(P(-300,148),"PLAZA SOUTH (z139)",font=fontb,fill=(255,180,255))
d.text(P(-476,30),"WEST LINK (x-476)",font=fontb,fill=(255,255,255))
d.text(P(-145,60),"PARK NORTH PATH (z73)",font=fonts,fill=(255,220,0))
d.text(P(10,57),"FINISH",font=fontb,fill=(0,255,0))
# key signs
for x,z in [(-91,88),(-2,47),(4,63)]:
    X,Z=P(x,z); d.rectangle([X-5,Z-5,X+5,Z+5],outline=(255,255,255),width=2)
d.text(P(-91,82),"NO ENTRY sign",font=fonts,fill=(255,255,255))
d.text(P(4,58),"D0CT WH0",font=fonts,fill=(255,255,255))
img.save(os.path.join(OUT,"route_overlay.png"))
print("updated",img.size)
