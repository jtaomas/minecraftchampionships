import math, os
from PIL import Image, ImageDraw, ImageFont

BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
FONT="C:\\Windows\\Fonts\\arial.ttf"; FONTB="C:\\Windows\\Fonts\\arialbd.ttf"
def f(sz,bold=False): return ImageFont.truetype(FONTB if bold else FONT, sz)
CX,CZ=-3,16
PODS=[("Lime",-90,"#6fbf3a"),("Green",-54.7,"#2f7a2a"),("Cyan",-18.4,"#1e8c96"),("Light Blue",18.4,"#3d96d2"),("Blue",54.7,"#2d3caa"),("Purple",90,"#6e329f"),("Pink",124.7,"#e682b4"),("Red",161.6,"#be2828"),("Orange",198.4,"#e6781e"),("Yellow",235.3,"#f0c832")]

S=14; W,H=1500,1220; cx,cy=720,650
im=Image.new("RGB",(W,H),(244,246,249)); d=ImageDraw.Draw(im,"RGBA")
def P(x,z): return (cx+(x-CX)*S, cy+(z-CZ)*S)
def Q(px,pz): return P(CX+px,CZ+pz)
def circ(r,fill=None,outline=None,w=1): d.ellipse([cx-r*S,cy-r*S,cx+r*S,cy+r*S],fill=fill,outline=outline,width=w)

# drum, terrace, pit, dial rim
circ(26,(58,58,64,255),(30,30,34,255),2)
circ(26,(110,110,116,255))
circ(28,(228,228,222,255),(120,120,126,255),2)
circ(27,(198,198,194,255))
circ(25,(92,92,98,255)); circ(25,(38,38,44,255))
circ(9.6,(150,150,154,255),(40,40,44,255),2)
# dial
circ(9.0,(233,223,199,255))
for k in range(8):
    a=math.radians(k*45); d.line([Q(0,0),Q(9*math.cos(a),9*math.sin(a))],fill=(140,140,145,255),width=4)
# crown
circ(7.2,None,(198,158,34,255),9)
for adeg in (16,74,106,164,196,254,286,344):
    a=math.radians(adeg); px,pz=7.2*math.cos(a),7.2*math.sin(a)
    d.rectangle([Q(px,pz)[0]-6,Q(px,pz)[1]-6,Q(px,pz)[0]+6,Q(px,pz)[1]+6],fill=(240,196,40,255),outline=(120,90,20,255))
d.rectangle([Q(-9,0)[0],Q(-9,0)[1]-3,Q(9,0)[0],Q(9,0)[1]+3],fill=(245,242,232,235),outline=(150,145,135,255))
d.rectangle([Q(0,-9)[0]-3,Q(0,-9)[1],Q(0,9)[0]+3,Q(0,9)[1]],fill=(245,242,232,235),outline=(150,145,135,255))
# barrier
for k in range(0,360,6):
    a=math.radians(k); a2=math.radians(k+3)
    d.line([Q(24.5*math.cos(a),24.5*math.sin(a)),Q(24.5*math.cos(a2),24.5*math.sin(a2))],fill=(210,40,40,220),width=3)
# entrances
for sx in (-1,1):
    x0,x1=(-45,-36) if sx<0 else (30,39)
    d.rectangle([P(x0,11)[0],P(x0,11)[1],P(x1,22)[0],P(x1,22)[1]],fill=(30,30,34,235),outline=(255,255,255,255),width=2)
    d.text(P((x0+x1)/2,16.5),"W gate" if sx<0 else "E gate",font=f(15,True),fill=(255,255,255),anchor="mm")
d.rectangle([P(-9,-21)[0],P(-9,-21)[1],P(3,-8)[0],P(3,-8)[1]],fill=(210,210,205,130),outline=(90,90,95,255),width=2)
d.rectangle([P(-8,-21)[0],P(-8,-21)[1],P(2,-16)[0],P(2,-16)[1]],fill=(150,32,32,210),outline=(255,255,255,255),width=1)
d.text(P(-3,-18.5),"N red carpet",font=f(14,True),fill=(255,255,255),anchor="mm")
d.text(P(-3,-11.5),"N platform y70",font=f(12),fill=(30,30,30),anchor="mm")
d.rectangle([P(-30,-15)[0],P(-30,-15)[1],P(-20,-1)[0],P(-20,-1)[1]],fill=(150,200,215,235),outline=(40,100,120,255),width=2)
d.text(P(-25,-9.5),"NW annex",font=f(12,True),fill=(20,50,60),anchor="mm")
d.text(P(-25,-7.0),"heads, banners, apiary",font=f(10),fill=(20,50,60),anchor="mm")
d.rectangle([P(16,34)[0],P(16,34)[1],P(23,41)[0],P(23,41)[1]],fill=(40,40,44,220),outline=(255,255,255,255),width=2)
d.text(P(19.5,37.5),"SE door",font=f(11,True),fill=(255,255,255),anchor="mm")
# pods
for i,(name,ang,col) in enumerate(PODS,1):
    a=math.radians(ang); px,pz=CX+16*math.cos(a),CZ+16*math.sin(a)
    x0,z0=px-3.5,pz-3.5
    d.rectangle([P(x0,z0)[0],P(x0,z0)[1],P(x0+7,z0+7)[0],P(x0+7,z0+7)[1]],fill=col,outline=(25,25,25,255),width=2)
    d.rectangle([P(x0+1,z0+1)[0],P(x0+1,z0+1)[1],P(x0+6,z0+6)[0],P(x0+6,z0+6)[1]],outline=(255,255,255,170),width=1)
    d.text(P(px,pz),str(i),font=f(16,True),fill=(20,20,20),anchor="mm")
# titles
d.text((W//2,24),"Decision Dome - Recreation: measured plan (world X/Z, terrace y=68)",font=f(26,True),fill=(20,20,25),anchor="ma")
d.text((W//2,58),"MCC layout model / sculpture, not a playable arena. Centre (-3,16). N = -Z.",font=f(16),fill=(60,60,65),anchor="ma")
d.text((W//2,84),"Dial floor y58 (10 below terrace); crown y69-82; pit down to base y26 (42 deep).",font=f(15),fill=(80,70,50),anchor="ma")
# compass
d.line([(60,160),(60,100)],fill=(20,20,20),width=3); d.polygon([(60,90),(52,106),(68,106)],fill=(20,20,20))
d.text((60,168),"N",font=f(20,True),fill=(20,20,20),anchor="ma")
# dimension labels
d.text(P(CX,CZ+45),"drum outer r 26 (53 wide)  |  terrace r 25-28 (4 wide)  |  pit r 9.5-25, 42 deep",font=f(14,True),fill=(20,20,25),anchor="ma")
d.text(P(CX,CZ+49),"10 pods at r 16, 36 deg spacing, 7 x 7 max footprint  |  dial 8 x 45 deg, r 9.0 (rim 9.5)  |  crown r 7",font=f(12),fill=(50,50,55),anchor="ma")
# legend
bx,by=1150,115
leg=[("drum / base",(110,110,116)),("terrace",(228,228,222)),("pit",(38,38,44)),("dial wedges",(233,223,199)),("crown",(198,158,34)),("team pod",(120,160,120)),("barrier",(210,40,40))]
for i,(t,c) in enumerate(leg):
    yy=by+i*22; d.rectangle([bx,yy,bx+14,yy+12],fill=c+(255,),outline=(60,60,60)); d.text((bx+22,yy-2),t,font=f(13),fill=(30,30,30))
sy=by+7*22+18; d.rectangle([bx,sy,bx+10*S,sy+10],fill=(30,30,30)); d.text((bx,sy+16),"10 blocks",font=f(12),fill=(30,30,30))
# pod legend
lx,ly=60,900
d.text((lx,ly-26),"Pod order clockwise from N:",font=f(13,True),fill=(20,20,20))
for i,(name,ang,col) in enumerate(PODS):
    yy=ly+i*20; d.rectangle([lx,yy,lx+14,yy+12],fill=col,outline=(30,30,30))
    d.text((lx+22,yy-2),f"{i+1}. {name} ({ang:.0f} deg)",font=f(13),fill=(30,30,30))
im.save(os.path.join(BASE,"plan_labelled.png"))
print("saved plan")
