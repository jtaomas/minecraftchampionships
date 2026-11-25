#!/usr/bin/env python3
"""Build annotated evidence frames for the City Marathon dossier."""
import os
from PIL import Image, ImageDraw, ImageFont
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
os.makedirs(F,exist_ok=True)
font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",20)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30)
fonts=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",16)

def frame(src,out,title,sub):
    im=Image.open(os.path.join(R,src)).convert("RGB")
    H=84
    outim=Image.new("RGB",(im.width,im.height+H),(14,16,22))
    outim.paste(im,(0,H))
    d=ImageDraw.Draw(outim)
    d.text((16,10),title,font=fontb,fill=(240,240,240))
    d.text((16,48),sub,font=font,fill=(180,180,180))
    outim.save(os.path.join(F,out))
    print("frame",out,outim.size)
    return outim

frame("route_overlay.png","01_overview_route.png","01  City Marathon - overview + main route",
      "Main line cyan; alternates dashed. All coordinates are world X/Z. Source: block dump + NBT command blocks (H).")
frame("start_gate_iso.png","02_start_gate_iso.png","02  Start pen / gate - isometric detail",
      "Yellow concrete gate posts at x1/x15,z49/z57; quartz podium and 3x3 glazed pad at x4,z66; TARDIS ladder at x4,z63 (H).")
frame("topdown_start_close.png","03_start_topdown.png","03  Start area - top-down, 1 px = 1 block",
      "Spawn 8,0,5; button 8,0,8; release barrier z10; gate z49-57; TARDIS at x4,z63 (H).")

# park annotated
im=Image.open(os.path.join(R,"park_floor.png")).convert("RGB")
d=ImageDraw.Draw(im)
# mapping: x -300..-60, z -10..170, scale 6
def PP(x,z): return ((x+300)*6,(z+10)*6)
for x,z,t,c in [(-145,85,"FOUNTAIN / PLAZA",(255,255,0)),(-91,88,"NO ENTRY gate (east exit)",(255,120,120)),
                (-75,73,"north entry z73",(120,255,120)),(-225,88,"west exit to plaza",(255,180,255)),
                (-145,120,"south path",(120,220,255))]:
    X,Z=PP(x,z); d.text((X+4,Z-8),t,font=fonts,fill=c)
d.line([PP(-75,73),PP(-225,73)],fill=(120,255,120),width=3)
d.line([PP(-225,88),PP(-75,88)],fill=(255,180,255),width=3)
im.save(os.path.join(F,"04_park_paths.png"))
print("frame 04_park_paths.png",im.size)

# plaza annotated
im=Image.open(os.path.join(R,"courtyard_floor.png")).convert("RGB")
d=ImageDraw.Draw(im)
def PQ(x,z): return ((x+350)*8,(z-15)*8)
for x,z,t,c in [(-245,88,"BLUE DOME (blocks z88)",(120,220,255)),(-295,90,"pool",(120,220,255)),
                (-311,54,"north detour x-311",(255,180,255)),(-311,126,"south detour x-311",(255,180,255)),
                (-294,37,"north road z37",(255,180,255)),(-294,139,"south road z139",(255,180,255))]:
    X,Z=PQ(x,z); d.text((X+4,Z-8),t,font=fonts,fill=c)
im.save(os.path.join(F,"05_plaza_detours.png"))
print("frame 05_plaza_detours.png",im.size)

frame("station_floor.png","06_station_floor.png","06  Station floor and train - top-down",
      "Train at z85-91, x-460..-380; red/blue/white cars, yellow-glazed roof strip at x-445..-441 (H).")
frame("train_iso.png","07_train_iso.png","07  Train - isometric detail",
      "Hazard-striped platform edges; roof y8; stairs at x-457; red/blue cars (H).")
frame("sw_tower_iso.png","08_elytra_roof_pad.png","08  Elytra roof pad - unreachable by ground",
      "Rooftop y28; command block at -216,27,151 under a polished-diorite strip; no ladder/stairs on this tower (H).")
frame("topdown_highway.png","09_highway.png","09  North Highway - west end",
      "z-50 road; yellow speed strips at x-476, -360, -335, -310; orange pads at -450, -420, -390 (H).")
frame("station_interior_iso.png","10_station_iso.png","10  Station - cutaway isometric",
      "Open-air concourse, arched glass train shed, platforms and train; roof cut at y32 (H).")
