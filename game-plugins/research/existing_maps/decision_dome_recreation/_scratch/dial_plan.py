import os, math
from PIL import Image, ImageDraw, ImageFont
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
F="C:\\Windows\\Fonts\\arial.ttf"; FB="C:\\Windows\\Fonts\\arialbd.ttf"
def font(sz,b=False): return ImageFont.truetype(FB if b else F,sz)
im=Image.open(os.path.join(BASE,"y58_top_raw.png")).convert("RGB")
W,H=im.size
canvas=Image.new("RGB",(W,H+150),(250,250,252)); canvas.paste(im,(0,0))
d=ImageDraw.Draw(canvas)
d.rectangle([0,0,W,56],fill=(250,250,252))
d.text((18,10),"Dial floor y58: 8 wedges x 45 deg",font=font(22,True),fill=(20,20,25))
d.text((18,36),"Smooth quartz plates on gray concrete dividers; rim r9.5; 10 blocks below the terrace.",font=font(12),fill=(60,60,65))
cx,cy=W/2,H/2
for k in range(8):
    a=math.radians(22.5+45*k); r=6.6*24
    x=cx+r*math.cos(a); y=cy+r*math.sin(a)
    d.ellipse([x-15,y-15,x+15,y+15],fill=(20,20,25),outline=(255,230,60),width=2)
    d.text((x,y),str(k+1),font=font(15,True),fill=(255,255,255),anchor="mm")
d.line([cx-6,cy,cx+6,cy],fill=(200,30,30),width=3); d.line([cx,cy-6,cx,cy+6],fill=(200,30,30),width=3)
d.text((18,H+62),"Boundaries at 0/45/90/135/180/225/270/315 deg: the E-W (0/180) and N-S (90/270) axes are dividers, not wedge centres.",font=font(13),fill=(40,40,45))
d.text((18,H+86),"The 270 deg (north) axis faces the north dais; the first view is a divider. No wedge is centred on an entrance axis (H).",font=font(13),fill=(40,40,45))
d.text((18,H+108),"Cardinal dividers are continuous; the four diagonal dividers are dashed, with 1-block gaps at r2 and r5 (H).",font=font(13),fill=(40,40,45))
canvas.save(os.path.join(BASE,"dial_plan_labelled.png"))
print(canvas.size)
