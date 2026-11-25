from PIL import Image, ImageDraw, ImageFont
import os
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
p=os.path.join(R,"route_overlay.png")
im=Image.open(p).convert("RGB"); d=ImageDraw.Draw(im)
# cover the stale bottom-right "EAST ROAD" baked label (black map area)
d.rectangle([2780,1080,3230,1150], fill=(8,8,12))
im.save(p)
# regenerate frame 01
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30); font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",20)
H=84
out=Image.new("RGB",(im.width,im.height+H),(14,16,22)); out.paste(im,(0,H))
d=ImageDraw.Draw(out)
d.text((16,10),"01  City Marathon - overview + main route",font=fontb,fill=(240,240,240))
d.text((16,48),"Main line cyan; alternates dashed. All coordinates are world X/Z. Source: block dump + NBT command blocks (H).",font=font,fill=(180,180,180))
out.save(os.path.join(F,"01_overview_route.png"))
print("ok",out.size)
