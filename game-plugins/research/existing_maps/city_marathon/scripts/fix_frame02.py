from PIL import Image, ImageDraw, ImageFont
import os
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
im=Image.open(os.path.join(R,"start_gate_iso.png")).convert("RGB")
W=1100; H=im.height+96
out=Image.new("RGB",(W,H),(14,16,22))
out.paste(im,((W-im.width)//2,96))
d=ImageDraw.Draw(out)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30)
font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",19)
d.text((16,10),"02  Start pen / gate - isometric detail",font=fontb,fill=(240,240,240))
d.text((16,50),"Yellow concrete gate posts at x1/x15,z49/z57; quartz podium with 3x3 glazed pad at x4,z66; TARDIS ladder at x4,z63 (H).",font=font,fill=(180,180,180))
out.save(os.path.join(F,"02_start_gate_iso.png"))
print(out.size)
