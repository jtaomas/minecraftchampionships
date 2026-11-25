import csv
from PIL import Image, ImageDraw
rows=list(csv.DictReader(open("data/blocks_mg_sb_01.csv")))
colmap={"white_concrete":(207,213,214),"stone_bricks":(122,122,122),"red_concrete":(142,33,33),"yellow_concrete":(241,175,21),"green_concrete":(73,91,36),"blue_concrete":(45,47,143),"red_terracotta":(143,61,47),"yellow_terracotta":(186,133,35),"green_terracotta":(76,83,42),"blue_terracotta":(74,60,91),"smooth_quartz":(232,226,216),"smooth_quartz_stairs":(232,226,216),"oak_fence":(156,122,74),"oak_leaves":(63,107,42),"gray_wool":(63,68,72)}
S=12
for axis,fixed in (("x",2000),("z",0)):
    W,H=90*S,36*S
    img=Image.new("RGB",(W,H),(245,248,252)); d=ImageDraw.Draw(img)
    for r in rows:
        x,y,z=int(r["x"]),int(r["y"]),int(r["z"]); b=r["block"].replace("minecraft:","")
        if b=="barrier": continue
        if axis=="x":
            if x!=fixed: continue
            u=z
        else:
            if z!=fixed: continue
            u=x-2000
        px=int((u+45)*S); py=int((45-y)*S)
        d.rectangle([px,py,px+S-1,py+S-1],fill=colmap.get(b,(120,120,120)))
    d.text((6,4),f"section {axis}={fixed}  horizontal {axis} -45..45, y 10..45",fill=(0,0,0))
    img.save(f"renders/section_{axis}.png")
print("ok")
