import os
from PIL import Image, ImageDraw, ImageFont
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
FONT="C:\\Windows\\Fonts\\arial.ttf"; FONTB="C:\\Windows\\Fonts\\arialbd.ttf"
def f(sz,bold=False): return ImageFont.truetype(FONTB if bold else FONT, sz)

def annotate(src,out,title,sub,badges,legend,panel=170,single=False,foot=None,title_sz=24,sub_sz=13):
    im=Image.open(os.path.join(BASE,src)).convert("RGB")
    W,H=im.size; canvas=Image.new("RGB",(W,H+panel),(250,250,252)); canvas.paste(im,(0,0))
    d=ImageDraw.Draw(canvas,"RGBA")
    # top caption strip
    d.rectangle([0,0,W,54],fill=(250,250,252,235))
    d.text((20,12),title,font=f(title_sz,True),fill=(20,20,25))
    d.text((20,38),sub,font=f(sub_sz),fill=(70,70,75))
    # badges
    for num,x,y,tx,ty in badges:
        r=17
        d.line([x,y,tx,ty],fill=(255,230,60,255),width=3)
        d.ellipse([tx-r,ty-r,tx+r,ty+r],fill=(20,20,25,235),outline=(255,230,60,255),width=3)
        d.text((tx,ty),str(num),font=f(19,True),fill=(255,255,255),anchor="mm")
    # legend panel
    y0=H
    d.rectangle([0,y0,W,y0+panel],fill=(250,250,252))
    d.line([0,y0,W,y0],fill=(180,180,185),width=1)
    cols=1 if single else 2; rowh=22 if single else 24
    for i,(num,txt) in enumerate(legend):
        col=i%cols; row=i//cols
        x=24+col*(W//cols); y=y0+12+row*rowh
        d.ellipse([x,y+2,x+18,y+20],fill=(20,20,25),outline=(255,230,60),width=2)
        d.text((x+9,y+11),str(num),font=f(12,True),fill=(255,255,255),anchor="mm")
        d.text((x+28,y+2),txt,font=f(11 if single else 13),fill=(35,35,40))
    if foot:
        d.text((24,y0+panel-20),foot,font=f(12),fill=(60,60,65))
    canvas.save(os.path.join(BASE,out))
    print("saved",out,canvas.size)

annotate("iso_a0_raw.png","iso_exterior_a0_labelled.png",
 "Exterior A (south-east 3/4): drum, pod tops, crown",
 "From block dump; 20,054 blocks, 53-wide drum, 57-block total height.",
 [(1,470,700,520,700),(2,500,540,560,600),(3,600,290,620,250),(4,480,175,530,160),
  (5,120,230,100,300),(6,870,250,900,330),(7,500,95,450,95),(8,810,170,850,220)],
 [(1,"Drum shell: gray concrete, r26, 53 wide, y26-68 (43 tall)"),
  (2,"Terrace ring r25-28 (4 wide), y68; barrier rail y71"),
  (3,"10 team pod tops at r16, 36 deg, y77 (9 above terrace)"),
  (4,"Gold crown / truss, y69-82: 8 red jewels, white cross"),
  (5,"West gatehouse (10 x 12, y64-73)"),
  (6,"East gatehouse (mirror)"),
  (7,"NW glass annex: heads, banners, apiary"),
  (8,"North red carpet (y70) + egg plate")])

annotate("iso_a2_raw.png","iso_exterior_a2_labelled.png",
 "Exterior B (north 3/4): red carpet, annex, crown",
 "Rotated 180 deg from A; the north face has the ceremony carpet and the annex.",
 [(1,70,340,100,390),(2,520,470,560,520),(3,480,155,520,120),(4,410,300,380,360),
  (5,300,415,250,460),(6,800,350,840,390),(7,500,700,450,740),(8,880,330,900,390)],
 [(1,"North red carpet y70; plate y71 at (-3,-19)"),
  (2,"NW glass annex (11 x 13 roof, y73)"),
  (3,"Gold crown, top y82 (highest point)"),
  (4,"Team pod towers: 5 x 5 body, 7 x 7 crown"),
  (5,"Terrace y68, 4 wide, no outer rail"),
  (6,"Pit: 42 deep to base y26, r9.5-25"),
  (7,"Drum shell gray concrete, no dome/roof"),
  (8,"Side gatehouse (mirror pair)")])

annotate("half_a1_raw.png","iso_interior_labelled.png",
 "Interior cutaway at x=-3: pit, team towers, floating dial, crown",
 "Cut through the centre; the east half is removed so the 42-deep pit is visible.",
 [(1,80,500,140,540),(2,430,800,360,760),(3,360,340,300,340),(4,430,215,430,150),
  (5,520,470,600,430),(6,700,260,650,300),(7,790,160,850,120),(8,760,450,820,480),
  (9,700,600,740,640),(10,860,230,900,270)],
 [(1,"Drum shell: 1-thick gray concrete, r26"),
  (2,"Base disk y26 (1 thick); pit floor 42 below terrace"),
  (3,"Dial floor y58: 8 x 45 deg wedges, smooth quartz; hidden from above"),
  (4,"Crown y69-82: gold band, white cross/truss, yellow ring, red jewels"),
  (5,"Team towers: 5 x 5 hollow shafts, y27-77, 7 x 7 crown bands"),
  (6,"Terrace y68 (gray inner / white outer), 4 wide"),
  (7,"Side gatehouse / tunnel at rim level"),
  (8,"North red carpet and plate (give egg / glowing)"),
  (9,"Cut plane at x=-3; no seats, no stairs into pit"),
  (10,"NW annex roof y73 (glass, sea lanterns)")])

annotate("north_top_raw.png","entrance_north_labelled.png",
 "North entrance: red carpet, pressure plate, command blocks",
 "Top view y62-84. The plate is at (-3,71,-19); command blocks at y68/69 under the carpet.",
 [(1,470,160,300,330),(2,510,105,600,190),(3,80,180,200,240),(4,310,210,250,300)],
 [(1,"Red carpet y70, 11 wide x 5 deep head"),
  (2,"Acacia plate y71 at (-3,-19): the only input"),
  (3,"Cyan/white platform edge; sea lantern at (-8,70,-21)"),
  (4,"N platform y66-71; three 1-block steps at z -13/-15/-17")])

annotate("pod_iso_raw.png","pod_detail_labelled.png",
 "Pod detail (lime), y60-77",
 "5x5 body, 7x7 crown, 3x3 cap; 51 blocks tall.",
 [(1,170,440,240,500),(2,170,330,240,370),(3,170,220,240,250),(4,170,100,240,130)],
 [(1,"White/team glass band y58-60 and y65-69"),
  (2,"Team terracotta bands y57, y61, y71-73"),
  (3,"7x7 crown y65-75, then 5x5, 3x3 cap"),
  (4,"Top cap y77; no door or interior room")],
 panel=150,single=True,title_sz=17,sub_sz=11)

annotate("crown_iso_raw.png","crown_detail_labelled.png",
 "Crown / truss y68-82: gold pillars, white cross, yellow ring, red jewels",
 "The 17 x 17 footprint hangs over the dial; it is the highest object (y82).",
 [(1,550,500,640,560),(2,550,300,640,340),(3,550,120,640,170)],
 [(1,"8 gold-capped 3 x 3 pillars, y69-72 (gold-gray-gray-gold)"),
  (2,"White quartz cross/truss y73-74 with 4 sea lanterns"),
  (3,"Yellow ring y78-80; 8 yellow caps y81; 8 redstone jewels y82")],
 panel=150,foot="Context: the crown covers the dial from above - only 9 of 25 sampled dial points are visible from y=100 (raycast).")
