#!/usr/bin/env python3
"""Draw the labelled City Marathon route overlay from the ground-level map (v2)."""
import json, os
from PIL import Image, ImageDraw, ImageFont

BASE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
OUT = os.path.join(BASE, "renders")
S = 6
OX, OZ = -536, -74

base = Image.open(os.path.join(OUT, "ground_layout.png")).convert("RGB")
base = base.resize((base.width * 2, base.height * 2), Image.NEAREST)
RIGHT = 330
PANEL = 250
img = Image.new("RGB", (base.width + RIGHT, base.height + PANEL), (14, 16, 22))
img.paste(base, (0, 0))
d = ImageDraw.Draw(img)
font = ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 20)
fontb = ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 26)
fonts = ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 16)

def P(x, z):
    return ((x - OX) * S, (z - OZ) * S)

def line(pts, col, width=4):
    d.line([P(x, z) for x, z in pts], fill=col, width=width, joint="curve")

def dashed(pts, col, width=3, dash=14, gap=10):
    for i in range(len(pts)-1):
        x1,z1=pts[i]; x2,z2=pts[i+1]
        dx,dz=x2-x1,z2-z1
        dist=max(abs(dx),abs(dz))
        n=max(1,int(dist*S//(dash+gap)))
        for k in range(n+1):
            t0=k*(dash+gap)/(dist*S); t1=min(1,t0+dash/(dist*S))
            if t0>=1: break
            d.line([P(x1+dx*t0,z1+dz*t0),P(x1+dx*t1,z1+dz*t1)],fill=col,width=width)

main = [(8,5),(8,10),(8,53),(8,88),(-3,88),(-27,88),(-52,88),(-85,88),(-91,88),
        (-145,88),(-172,88),(-208,88),(-225,88),(-225,60),(-225,44),(-243,37),
        (-294,37),(-311,44),(-311,54),(-337,52),(-361,52),(-388,51),(-425,52),
        (-457,52),(-457,70),(-457,88),(-476,86),(-476,20),(-476,-50),
        (-400,-50),(-300,-50),(-200,-50),(-100,-50),(8,-50),(8,5)]
line(main, (0,0,0), 10)
line(main, (90,220,255), 5)

dashed([(-225,88),(-225,139),(-244,139),(-294,139),(-311,126),(-311,88),(-337,88)], (255,120,255), 3)
dashed([(8,73),(-67,73),(-145,73),(-145,88)], (255,220,0), 3)
dashed([(-457,88),(-443,88),(-395,88),(-380,88),(-351,88)], (255,60,60), 3)
dashed([(-351,88),(-351,23)], (0,255,200), 2)
dashed([(-457,52),(-476,44),(-476,-50)], (255,255,255), 2)

sel = json.load(open(os.path.join(BASE, "data", "selected_blocks.json"), encoding="utf-8"))
def centers(name):
    pts = [(x,y,z) for x,y,z,st,_ in sel if st.split("[")[0].endswith(name)]
    Sset=set(pts); out=[]
    while Sset:
        seed=Sset.pop(); c={seed}; st=[seed]
        while st:
            x,y,z=st.pop()
            for dx in (-1,0,1):
                for dy in (-1,0,1):
                    for dz in (-1,0,1):
                        q=(x+dx,y+dy,z+dz)
                        if q in Sset: Sset.remove(q); c.add(q); st.append(q)
        out.append(c)
    return [((min(p[0] for p in c)+max(p[0] for p in c))/2,(min(p[2] for p in c)+max(p[2] for p in c))/2) for c in out]

for name, col, r in [("yellow_glazed_terracotta",(255,210,0),4),("red_glazed_terracotta",(255,60,60),3),
                     ("orange_wool",(255,120,0),4),("lime_wool",(120,255,0),4),
                     ("green_carpet",(0,230,80),4),("yellow_concrete",(255,255,150),3)]:
    for x,z in centers(name):
        X,Z=P(x,z); d.ellipse([X-r,Z-r,X+r,Z+r], fill=col, outline=(0,0,0))

for x,z in [(-127,-66),(-127,-36),(-216,25),(-216,49),(-216,127),(-216,151)]:
    X,Z=P(x,z); d.rectangle([X-9,Z-9,X+9,Z+9], outline=(0,255,255), width=3)
    d.text((X+11,Z-10), "E", font=fonts, fill=(0,255,255))
d.rectangle([P(-355,23)[0],P(-355,23)[1],P(-347,154)[0],P(-347,154)[1]], outline=(0,255,255), width=3)
d.text(P(-353,150), "elytra strip 9x132", font=fonts, fill=(0,255,255))

# train box
d.rectangle([P(-462,83)[0],P(-462,83)[1],P(-378,93)[0],P(-378,93)[1]], outline=(255,80,80), width=3)
d.text(P(-460,96), "TRAIN (roof y8)", font=fonts, fill=(255,120,120))

# start/finish labels to the left
X,Z=P(8,5); d.ellipse([X-12,Z-12,X+12,Z+12], outline=(0,255,0), width=4)
d.text((X-18,Z), "START PEN (8,0,5)", font=fontb, fill=(0,255,0), anchor="rm")
X,Z=P(8,53); d.polygon([(X,Z-15),(X+15,Z),(X,Z+15),(X-15,Z)], outline=(0,255,0), width=3)
d.text((X-18,Z), "START / FINISH GATE (8,53)", font=fontb, fill=(0,255,0), anchor="rm")
X,Z=P(8,10); d.line([P(0,10),P(18,10)], fill=(255,50,50), width=5)
d.text((X-18,Z), "release gate", font=fonts, fill=(255,80,80), anchor="rm")

for x,z,label in [(-208,88,"CP2"),(-388,51,"CP3")]:
    X,Z=P(x,z); d.polygon([(X,Z-10),(X+10,Z),(X,Z+10),(X-10,Z)], fill=(60,120,255), outline=(255,255,255))
    d.text((X+12,Z-10),label,font=fontb,fill=(120,170,255))

for x,z,t,c in [(-145,18,"PARK",(120,255,120)),(-270,18,"PLAZA",(255,180,255)),
                (-430,25,"STATION",(255,120,120)),(-250,-62,"NORTH HIGHWAY",(255,230,120)),
                (-50,110,"EAST ROAD",(120,220,255)),(-430,150,"TRAIN / PLATFORM",(255,150,100))]:
    d.text(P(x,z),t,font=fontb,fill=c)

for n,x,z in [(1,8,30),(2,8,88),(3,-120,88),(4,-225,55),(5,-311,88),(6,-457,72),(7,-476,0),(8,-200,-50),(9,8,0)]:
    X,Z=P(x,z); d.ellipse([X-13,Z-13,X+13,Z+13], fill=(20,20,25), outline=(90,220,255), width=3)
    d.text((X-6,Z-10),str(n),font=fontb,fill=(255,255,255))

# compass + title in right margin
d.text((base.width+20, 20), "CITY MARATHON", font=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 34), fill=(240,240,240))
d.text((base.width+20, 62), "1.19.2  |  6,374 chunks", font=font, fill=(180,180,180))
d.text((base.width+20, 92), "67 command blocks", font=font, fill=(180,180,180))
d.text((base.width+20, 122), "140 shulker boxes (empty)", font=font, fill=(180,180,180))
d.text((base.width+20, 152), "84 banners", font=font, fill=(180,180,180))
d.text((base.width+20, 190), "N", font=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 44), fill=(255,255,255))
d.line([(base.width+38, 250),(base.width+38, 310)], fill=(255,255,255), width=4)
d.polygon([(base.width+38,235),(base.width+30,255),(base.width+46,255)], fill=(255,255,255))
d.text((base.width+20, 320), "scale 6 px = 1 block", font=fonts, fill=(160,160,160))

py = base.height + 12
d.text((18, py), "CITY MARATHON - forensic route overlay", font=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 30), fill=(240,240,240))
d.text((18, py+42), "Main line (cyan, legs 1-9): start pen -> East Road -> Park -> Plaza north -> Station -> West Link -> North Highway -> East Road -> finish.", font=font, fill=(200,200,200))
d.text((18, py+70), "Dashed: magenta = Plaza south alternate; yellow = Park north entry; red = train-roof variant; cyan = long diorite platform; white = West-Link bypass.", font=fonts, fill=(190,190,190))
items=[("yellow dot = speed strip (15 blocks)",(255,210,0)),("red circle = 3x3 boost pad",(255,60,60)),
       ("orange = Jump Boost X / Speed XII",(255,120,0)),("green = effect clear",(0,230,80)),
       ("cyan square = elytra auto-equip command/pad",(0,255,255)),("green star = start/finish gate",(0,255,0)),
       ("blue diamond = CP2/CP3 command cluster",(60,120,255))]
for i,(t,c) in enumerate(items):
    X=18+(i%2)*1050; Y=py+105+(i//2)*24
    d.rectangle([X,Y+5,X+18,Y+17], fill=c)
    d.text((X+24,Y),t,font=fonts,fill=(210,210,210))

img.save(os.path.join(OUT, "route_overlay.png"))
print("saved route_overlay.png", img.size)
