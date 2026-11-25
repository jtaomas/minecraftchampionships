import csv
from PIL import Image, ImageDraw, ImageFont
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
top={}; blk={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"]); b=r["block"].replace("minecraft:","")
    if b=="barrier": continue
    if y>top.get((x,z),-1): top[(x,z)]=y; blk[(x,z)]=b
colmap={"white_concrete":(215,220,220),"stone_bricks":(125,125,125),"red_concrete":(150,35,35),"yellow_concrete":(240,180,25),"green_concrete":(80,110,40),"blue_concrete":(50,55,150),"red_terracotta":(143,61,47),"yellow_terracotta":(186,133,35),"green_terracotta":(76,83,42),"blue_terracotta":(74,60,91),"smooth_quartz":(235,229,219),"smooth_quartz_stairs":(235,229,219),"oak_fence":(150,115,70),"oak_leaves":(60,105,40),"orange_wool":(240,118,20),"cyan_wool":(20,140,145),"lime_wool":(110,185,25),"purple_wool":(120,42,173),"gray_wool":(65,70,75),"gold_block":(245,205,60),"raw_gold_block":(190,145,80),"red_glazed_terracotta":(160,50,45),"green_glazed_terracotta":(100,130,55),"yellow_glazed_terracotta":(210,170,65),"blue_glazed_terracotta":(60,75,130),"white_shulker_box":(235,235,235),"chest":(160,110,45),"barrel":(120,85,50),"crafting_table":(130,95,60),"spruce_slab":(120,90,55),"campfire":(90,60,35),"chain":(80,80,85),"iron_block":(220,220,220),"stripped_spruce_log":(150,120,80),"spruce_fence":(100,75,45),"oak_trapdoor":(160,125,75),"spruce_trapdoor":(100,75,45),"light":(255,240,180)}
S=6; x0,x1,z0,z1=1955,2045,-45,45
W,H=(x1-x0)*S,(z1-z0)*S
img=Image.new("RGB",(W,H),(230,238,246)); d=ImageDraw.Draw(img)
for z in range(z0,z1):
    for x in range(x0,x1):
        b=blk.get((x,z))
        if not b: continue
        px,py=(x-x0)*S,(z-z0)*S
        d.rectangle([px,py,px+S-1,py+S-1],fill=colmap.get(b,(130,130,130)))
f=ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf",16); fs=ImageFont.truetype("C:/Windows/Fonts/arial.ttf",11)
def label(x,z,text,fill=(0,0,0),anchor="mm"):
    d.text(((x-x0)*S,(z-z0)*S),text,font=f,fill=fill,anchor=anchor,stroke_width=2,stroke_fill=(255,255,255))
label(1970,0,"RED",(150,20,20)); label(2030,0,"GREEN",(30,90,20)); label(2000,30,"BLUE",(20,30,140)); label(2000,-30,"YELLOW",(160,120,0))
label(2000,0,"MID",(0,0,0))
for (x,z,t) in [(1974,26,"cyan balloon\n(barrel+chest)"),(2026,26,"purple balloon\n(barrel+chest)"),(1974,-26,"orange balloon\n(barrel+chest)"),(2026,-26,"lime balloon\n(barrel+chest)")]:
    d.text(((x-x0)*S,(z-z0)*S),t,font=fs,fill=(20,20,20),anchor="mm",stroke_width=2,stroke_fill=(255,255,255))
# outer labels
d.text((10,8),"Sky Battle MG-SB-01 - top surfaces (barriers hidden)",font=f,fill=(0,0,0))
d.text((10,H-20),"N = up (-z). 1 block = 6 px. Cross-section envelope x1948..2052, z-52..52, roof y56.",font=fs,fill=(40,40,40))
# compass
d.line([(W-30,40),(W-30,12)],fill=(0,0,0),width=3); d.polygon([(W-30,6),(W-36,18),(W-24,18)],fill=(0,0,0)); d.text((W-30,44),"N",font=f,fill=(0,0,0),anchor="ma")
# ring arrows (clockwise)
for (ax,az,dx,dz) in [(2010,-10,1,1),(2000,10,1,0),(1990,10,-1,0),(2000,-10,-1,-1)]:
    pass
img.save("renders/sb_top_labelled.png")
print("saved",W,H)
