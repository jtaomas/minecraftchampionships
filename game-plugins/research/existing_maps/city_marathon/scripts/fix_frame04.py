from PIL import Image, ImageDraw, ImageFont
import os
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
R=os.path.join(BASE,"renders"); F=os.path.join(BASE,"frames")
im=Image.open(os.path.join(R,"park_floor.png")).convert("RGB")
d=ImageDraw.Draw(im)
def P(x,z): return ((x+300)*6,(z+10)*6)
font=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",18)
fontb=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",20)
# paths
d.line([P(-75,73),P(-225,73)],fill=(120,255,60),width=4)
d.line([P(-225,88),P(-91,88)],fill=(255,120,255),width=4)
# labels, kept inside the frame
d.text(P(-222,66),"north path z73",font=fontb,fill=(120,255,60))
d.text(P(-222,81),"central path z88",font=fontb,fill=(255,120,255))
d.text(P(-160,79),"fountain / plaza",font=font,fill=(255,255,0))
d.text(P(-98,84),"NO ENTRY gate",font=font,fill=(255,120,120),anchor="rs")
d.text(P(-222,92),"west exit to plaza",font=font,fill=(255,180,255))
d.text(P(-160,116),"south path",font=font,fill=(120,220,255))
# header
H=84
out=Image.new("RGB",(im.width,im.height+H),(14,16,22)); out.paste(im,(0,H))
d=ImageDraw.Draw(out)
fontb2=ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf",30); font2=ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf",19)
d.text((16,10),"04  Park paths and the two east-west lines",font=fontb2,fill=(240,240,240))
d.text((16,48),"Green = north entry z73; magenta = central line z88; the NO ENTRY gate faces east and is at the central exit (H).",font=font2,fill=(180,180,180))
out.save(os.path.join(F,"04_park_paths.png"))
print(out.size)
