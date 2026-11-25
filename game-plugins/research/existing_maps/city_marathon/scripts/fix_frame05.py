from PIL import Image, ImageDraw, ImageFont
import os
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
im=Image.open(os.path.join(R,"courtyard_floor.png")).convert("RGB")
d=ImageDraw.Draw(im)
def P(x,z): return ((x+350)*8,(z-15)*8)
font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",18)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",20)
d.text(P(-345,31),"north road z37",font=fontb,fill=(255,180,255))
d.text(P(-345,58),"north detour x-311",font=fontb,fill=(255,180,255))
d.text(P(-330,73),"pool",font=font,fill=(120,220,255))
d.text(P(-252,80),"BLUE DOME blocks z88",font=fontb,fill=(120,220,255),anchor="rs")
d.text(P(-345,122),"south detour x-311",font=fontb,fill=(255,180,255))
d.text(P(-345,140),"south road z139",font=fontb,fill=(255,180,255))
H=84
out=Image.new("RGB",(im.width,im.height+H),(14,16,22)); out.paste(im,(0,H))
d=ImageDraw.Draw(out)
fontb2=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30); font2=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",19)
d.text((16,10),"05  Plaza block - the direct z88 line is blocked",font=fontb2,fill=(240,240,240))
d.text((16,48),"Blue dome (x-245) and long pool (x-295) force the north z37/44 or south z139 detour; both are pad-lined (H).",font=font2,fill=(180,180,180))
out.save(os.path.join(F,"05_plaza_detours.png"))
print(out.size)
