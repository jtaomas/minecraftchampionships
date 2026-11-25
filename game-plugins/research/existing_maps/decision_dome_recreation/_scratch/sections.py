import csv, os
from PIL import Image, ImageDraw, ImageFont

path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
B={}
for row in csv.DictReader(open(path,encoding="utf-8")):
    B[(int(row["x"]),int(row["y"]),int(row["z"]))]=row["block"].replace("minecraft:","").split("[")[0]

COL={
 "gray_concrete":(110,110,112),"white_concrete":(225,225,220),"black_concrete":(25,25,28),
 "red_concrete":(190,40,40),"orange_concrete":(230,120,30),"yellow_concrete":(240,200,50),
 "lime_concrete":(90,190,45),"green_concrete":(45,110,40),"cyan_concrete":(30,140,150),
 "light_blue_concrete":(60,150,210),"blue_concrete":(45,60,170),"purple_concrete":(110,50,160),
 "pink_concrete":(230,130,180),"light_gray_concrete":(160,160,160),
 "white_stained_glass":(235,245,250),"white_stained_glass_pane":(235,245,250),
 "light_gray_stained_glass_pane":(200,200,205),
 "lime_stained_glass_pane":(120,210,70),"orange_stained_glass_pane":(240,150,60),
 "yellow_stained_glass_pane":(245,220,80),"red_stained_glass_pane":(210,70,70),
 "pink_stained_glass_pane":(240,160,200),"cyan_stained_glass_pane":(60,180,190),
 "light_blue_stained_glass_pane":(90,180,230),"blue_stained_glass_pane":(70,90,200),
 "purple_stained_glass_pane":(140,80,190),"green_stained_glass_pane":(70,140,60),
 "black_stained_glass_pane":(50,50,55),
 "lime_stained_glass":(120,210,70),"orange_stained_glass":(240,150,60),
 "yellow_stained_glass":(245,220,80),"red_stained_glass":(210,70,70),
 "pink_stained_glass":(240,160,200),"cyan_stained_glass":(60,180,190),
 "blue_stained_glass":(70,90,200),"purple_stained_glass":(140,80,190),
 "green_stained_glass":(70,140,60),"light_blue_stained_glass":(90,180,230),
 "gold_block":(250,200,40),"smooth_quartz_slab":(238,232,220),"quartz_slab":(238,232,220),
 "quartz_stairs":(238,232,220),"smooth_quartz_stairs":(238,232,220),"quartz_block":(238,232,220),
 "stone_slab":(140,140,140),"polished_andesite_slab":(150,150,150),"polished_andesite_stairs":(150,150,150),
 "polished_diorite":(200,200,200),"polished_diorite_stairs":(200,200,200),
 "light_gray_terracotta":(150,140,130),"red_terracotta":(150,80,65),"orange_terracotta":(165,105,60),
 "yellow_terracotta":(185,145,70),"lime_terracotta":(105,125,55),"green_terracotta":(90,110,65),
 "cyan_terracotta":(85,105,110),"light_blue_terracotta":(120,135,145),"blue_terracotta":(85,90,120),
 "purple_terracotta":(115,85,105),"pink_terracotta":(170,120,115),
 "barrier":(255,0,0),"light_gray_carpet":(190,190,190),"gray_carpet":(90,90,90),
 "chain":(90,90,90),"sea_lantern":(220,240,230),"iron_trapdoor":(170,170,170),
 "smooth_red_sandstone_slab":(200,120,60),"yellow_wool":(245,215,70),
 "redstone_block":(200,20,20),"beehive":(190,160,80),"player_head":(160,120,100),
 "netherite_block":(70,60,60),"oak_leaves":(60,120,40),"iron_door":(180,180,180),
 "stone":(130,130,130),"white_wall_banner":(235,235,235),"acacia_pressure_plate":(180,100,60),
}
def col(n):
    return COL.get(n,(255,0,255))

def draw_slice_z(z, x0, x1, y0, y1, out, px=10):
    W=(x1-x0+1)*px+140; H=(y1-y0+1)*px+90
    im=Image.new("RGB",(W,H),(250,250,250)); d=ImageDraw.Draw(im)
    ox,oy=120,60
    for x in range(x0,x1+1):
        for y in range(y0,y1+1):
            n=B.get((x,y,z))
            if n:
                X=ox+(x-x0)*px; Y=oy+(y1-y)*px
                d.rectangle([X,Y,X+px-1,Y+px-1], fill=col(n))
    # grid
    for x in range(x0,x1+1):
        if x%5==0:
            X=ox+(x-x0)*px
            d.line([X,oy,X,oy+(y1-y0+1)*px],fill=(200,200,200),width=1)
            d.text((X-8,H-25),str(x),fill=(40,40,40))
    for y in range(y0,y1+1):
        if y%5==0:
            Y=oy+(y1-y)*px
            d.line([ox,Y,ox+(x1-x0+1)*px,Y],fill=(200,200,200),width=1)
            d.text((60,Y-5),str(y),fill=(40,40,40))
    d.text((ox,H-45),f"z = {z}  (section looking north, x right)",fill=(20,20,20))
    im.save(out)

def draw_slice_x(x, z0, z1, y0, y1, out, px=10):
    W=(z1-z0+1)*px+140; H=(y1-y0+1)*px+90
    im=Image.new("RGB",(W,H),(250,250,250)); d=ImageDraw.Draw(im)
    ox,oy=120,60
    for z in range(z0,z1+1):
        for y in range(y0,y1+1):
            n=B.get((x,y,z))
            if n:
                X=ox+(z-z0)*px; Y=oy+(y1-y)*px
                d.rectangle([X,Y,X+px-1,Y+px-1], fill=col(n))
    for z in range(z0,z1+1):
        if z%5==0:
            X=ox+(z-z0)*px
            d.line([X,oy,X,oy+(y1-y0+1)*px],fill=(200,200,200),width=1)
            d.text((X-8,H-25),str(z),fill=(40,40,40))
    for y in range(y0,y1+1):
        if y%5==0:
            Y=oy+(y1-y)*px
            d.line([ox,Y,ox+(z1-z0+1)*px,Y],fill=(200,200,200),width=1)
            d.text((60,Y-5),str(y),fill=(40,40,40))
    d.text((ox,H-45),f"x = {x}  (section looking east, z right)",fill=(20,20,20))
    im.save(out)

base=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
draw_slice_z(16,-34,30,52,84,os.path.join(base,"section_z16_raw.png"))
draw_slice_z(0,-34,30,52,84,os.path.join(base,"section_z0_raw.png"))
draw_slice_x(-3,-16,46,52,84,os.path.join(base,"section_x-3_raw.png"))
draw_slice_x(-29,-16,46,52,84,os.path.join(base,"section_x-29_raw.png"))
print("done")
