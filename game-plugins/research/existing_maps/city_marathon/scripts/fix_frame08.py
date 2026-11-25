from PIL import Image, ImageDraw, ImageFont
import os
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
im=Image.open(os.path.join(R,"sw_tower_iso.png")).convert("RGB")
W=1320; H=im.height+104
out=Image.new("RGB",(W,H),(14,16,22)); out.paste(im,((W-im.width)//2,104))
d=ImageDraw.Draw(out)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30); font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",19)
d.text((16,10),"08  Elytra roof pad - unreachable and non-matching",font=fontb,fill=(240,240,240))
d.text((16,52),"Rooftop y28 at -216,27,151; the roof is polished andesite, NOT the polished diorite the command checks.",font=font,fill=(180,180,180))
d.text((16,76),"There is no ladder or stair to the roof on this tower (H).",font=font,fill=(180,180,180))
out.save(os.path.join(F,"08_elytra_roof_pad.png"))
print(out.size)
