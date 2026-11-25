import os
from PIL import Image, ImageDraw, ImageFont
B=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
F="C:\\Windows\\Fonts\\arial.ttf"; FB="C:\\Windows\\Fonts\\arialbd.ttf"
def f(s,b=False): return ImageFont.truetype(FB if b else F,s)
items=[("plan_labelled.png","Measured plan (terrace y68)"),("iso_exterior_a0_labelled.png","Exterior A (SE)"),("iso_interior_labelled.png","Interior cutaway x=-3"),("elevation_labelled.png","Section z=16 / heights"),("dial_plan_labelled.png","Dial plan y58"),("pod_detail_labelled.png","Pod detail")]
W,H=1800,1250; cols=3; rows=2; cw,ch=W//cols,H//rows
im=Image.new("RGB",(W,H),(245,246,249)); d=ImageDraw.Draw(im)
d.text((24,18),"Decision Dome Recreation - evidence sheet (20,054 blocks; 53-wide drum; 57 tall)",font=f(24,True),fill=(20,20,25))
for i,(fn,cap) in enumerate(items):
    col=i%cols; row=i//cols; x=col*cw+20; y=60+row*ch
    src=Image.open(os.path.join(B,fn)).convert("RGB")
    maxw=cw-40; maxh=ch-80
    r=min(maxw/src.width,maxh/src.height)
    nw,nh=max(1,int(src.width*r)),max(1,int(src.height*r))
    src=src.resize((nw,nh),Image.LANCZOS)
    im.paste(src,(x+(maxw-nw)//2,y))
    d.text((x,y+maxh+8),cap,font=f(16,True),fill=(30,30,35))
im.save(os.path.join(B,"contact_sheet.png"))
print(im.size)
