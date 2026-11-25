import math, os
from PIL import Image, ImageDraw, ImageFont

BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\renders"
FONT="C:\\Windows\\Fonts\\arial.ttf"
FONTB="C:\\Windows\\Fonts\\arialbd.ttf"
def f(sz,bold=False):
    return ImageFont.truetype(FONTB if bold else FONT, sz)

CX,CZ=-3,16
PODS=[("Lime",-90,"#6fbf3a"),("Green",-54.7,"#2f7a2a"),("Cyan",-18.4,"#1e8c96"),("Light Blue",18.4,"#3d96d2"),("Blue",54.7,"#2d3caa"),("Purple",90,"#6e329f"),("Pink",124.7,"#e682b4"),("Red",161.6,"#be2828"),("Orange",198.4,"#e6781e"),("Yellow",235.3,"#f0c832")]

def plan():
    S=14; W,H=1500,1180; cx,cy=720,560
    im=Image.new("RGB",(W,H),(244,246,249)); d=ImageDraw.Draw(im,"RGBA")
    def P(x,z): return (cx+x*S, cy+z*S)
    def circ(r,fill=None,outline=None,w=1):
        d.ellipse([cx-r*S,cy-r*S,cx+r*S,cy+r*S],fill=fill,outline=outline,width=w)
    # base + drum + terrace + pit
    circ(26,(58,58,64,255),(30,30,34,255),2)
    circ(26,(110,110,116,255))
    circ(28,(228,228,222,255),(120,120,126,255),2)
    circ(27,(198,198,194,255))
    circ(25,(92,92,98,255))
    circ(25,(38,38,44,255))
    circ(9.6,(150,150,154,255),(40,40,44,255),2)
    # dial 8 wedges
    circ(9.0,(233,223,199,255))
    for k in range(8):
        a=math.radians(k*45)
        d.line([P(0,0),P(9*math.cos(a),9*math.sin(a))],fill=(140,140,145,255),width=4)
    # crown
    circ(7.2,None,(198,158,34,255),9)
    for k in range(8):
        a=math.radians(16+45*k)
        px,pz=7.2*math.cos(a),7.2*math.sin(a)
        d.rectangle([P(px,pz)[0]-6,P(px,pz)[1]-6,P(px,pz)[0]+6,P(px,pz)[1]+6],fill=(240,196,40,255),outline=(120,90,20,255))
    # cross
    d.rectangle([P(-9,0)[0],P(-9,0)[1]-3,P(9,0)[0],P(9,0)[1]+3],fill=(245,242,232,235),outline=(150,145,135,255))
    d.rectangle([P(0,-9)[0]-3,P(0,-9)[1],P(0,9)[0]+3,P(0,9)[1]],fill=(245,242,232,235),outline=(150,145,135,255))
    # barrier ring
    for k in range(0,360,6):
        a=math.radians(k); a2=math.radians(k+3)
        d.line([P(24.5*math.cos(a),24.5*math.sin(a)),P(24.5*math.cos(a2),24.5*math.sin(a2))],fill=(210,40,40,220),width=3)
    # entrances
    # west/east gatehouses
    for sx in (-1,1):
        x0=36 if sx<0 else 30
        x1=45 if sx<0 else 39
        if sx<0: x0,x1=-45,-36
        d.rectangle([P(x0,11)[0],P(x0,11)[1],P(x1,22)[0],P(x1,22)[1]],fill=(30,30,34,235),outline=(255,255,255,255),width=2)
        d.text(P((x0+x1)/2,16.5),"W gate" if sx<0 else "E gate",font=f(15,True),fill=(255,255,255),anchor="mm")
    # north platform
    d.rectangle([P(-9,-21)[0],P(-9,-21)[1],P(3,-8)[0],P(3,-8)[1]],fill=(210,210,205,145),outline=(90,90,95,255),width=2)
    d.rectangle([P(-8,-21)[0],P(-8,-21)[1],P(2,-16)[0],P(2,-16)[1]],fill=(150,32,32,200),outline=(255,255,255,255),width=1)
    d.text(P(-3,-18.5),"N red carpet",font=f(14,True),fill=(255,255,255),anchor="mm")
    d.text(P(-3,-12.7),"N platform y70",font=f(12),fill=(30,30,30),anchor="mm")
    # NW annex
    d.rectangle([P(-30,-15)[0],P(-30,-15)[1],P(-20,-1)[0],P(-20,-1)[1]],fill=(150,200,215,235),outline=(40,100,120,255),width=2)
    d.text(P(-25,-9.5),"NW annex",font=f(12,True),fill=(20,50,60),anchor="mm")
    d.text(P(-25,-7.0),"heads, banners, apiary",font=f(10),fill=(20,50,60),anchor="mm")
    # SE door
    d.rectangle([P(16,34)[0],P(16,34)[1],P(23,41)[0],P(23,41)[1]],fill=(40,40,44,220),outline=(255,255,255,255),width=2)
    d.text(P(19.5,37.5),"SE door",font=f(11,True),fill=(255,255,255),anchor="mm")
    # pods (drawn over platform for legibility)
    for i,(name,ang,col) in enumerate(PODS,1):
        a=math.radians(ang); px,pz=16*math.cos(a),16*math.sin(a)
        x0,z0=px-3.5,pz-3.5
        d.rectangle([P(x0,z0)[0],P(x0,z0)[1],P(x0+7,z0+7)[0],P(x0+7,z0+7)[1]],fill=col,outline=(25,25,25,255),width=2)
        d.rectangle([P(x0+1,z0+1)[0],P(x0+1,z0+1)[1],P(x0+6,z0+6)[0],P(x0+6,z0+6)[1]],outline=(255,255,255,160),width=1)
        d.text(P(px,pz),str(i),font=f(16,True),fill=(20,20,20),anchor="mm")
    # labels
    d.text((W//2,24),"Decision Dome - Recreation: measured plan (world X/Z, y=68 terrace)",font=f(26,True),fill=(20,20,25),anchor="ma")
    d.text((W//2,58),"MCC layout model / sculpture, not a playable arena. Centre (-3,16). N = -Z.",font=f(16),fill=(60,60,65),anchor="ma")
    # compass
    d.line([(60,120),(60,60)],fill=(20,20,20),width=3); d.polygon([(60,50),(52,66),(68,66)],fill=(20,20,20))
    d.text((60,128),"N",font=f(20,True),fill=(20,20,20),anchor="ma")
    d.text((60,80),"",font=f(10))
    # dimension labels
    d.line([P(-9.5,0),P(-9.5,-22)],fill=(30,30,30),width=1)
    d.text((P(0,-24)[0],P(0,-24)[1]),"dial r 9.5 (19 wide, 8 x 45 deg wedges)",font=f(13),fill=(30,30,30),anchor="ma")
    d.text((P(0,28.5)[0],P(0,28.5)[1]),"drum outer r 26 (53 wide)  |  terrace r 25-28 (4 wide)  |  pit r 9.5-25, 42 deep",font=f(14,True),fill=(20,20,25),anchor="ma")
    d.text((P(0,32.5)[0],P(0,32.5)[1]),"10 pods at r 16, 36 deg spacing, 7 x 7 max footprint  |  dial 8 x 45 deg, r 9.0 (rim 9.5)  |  crown r 7",font=f(12),fill=(50,50,55),anchor="ma")
    # scale + legend
    bx,by=1140,700
    leg=[("drum / base",(110,110,116)),("terrace",(228,228,222)),("pit",(38,38,44)),("dial wedges",(233,223,199)),("crown",(198,158,34)),("team pod",(120,160,120)),("barrier",(210,40,40))]
    for i,(t,c) in enumerate(leg):
        yy=by+i*22
        d.rectangle([bx,yy,bx+14,yy+12],fill=c+(255,),outline=(60,60,60)); d.text((bx+22,yy-2),t,font=f(13),fill=(30,30,30))
    sy=by+7*22+18
    d.rectangle([bx,sy,bx+10*S,sy+10],fill=(30,30,30)); d.text((bx,sy+16),"10 blocks",font=f(12),fill=(30,30,30))
    d.text(P(0,11.5),"dial floor y58, 10 below terrace; crown y69-82 above",font=f(11),fill=(80,70,50),anchor="mm")
    # pod legend
    lx,ly=60,900
    d.text((lx,ly-26),"Pod order clockwise from N:",font=f(13,True),fill=(20,20,20))
    for i,(name,ang,col) in enumerate(PODS):
        yy=ly+i*20
        d.rectangle([lx,yy,lx+14,yy+12],fill=col,outline=(30,30,30))
        d.text((lx+22,yy-2),f"{i+1}. {name} ({ang:.0f} deg)",font=f(13),fill=(30,30,30))
    im.save(os.path.join(BASE,"plan_labelled.png"))
    print("saved plan")

def section():
    S=9; W,H=1500,760; ox,oy=750,690
    im=Image.new("RGB",(W,H),(250,251,253)); d=ImageDraw.Draw(im,"RGBA")
    def P(x,y): return (ox+x*S, oy-(y-24)*S)
    def rect_xy(x0,x1,y0,y1,fill,outline=None,w=1):
        d.rectangle([P(x0,y1)[0],P(x0,y1)[1],P(x1,y0)[0],P(x1,y0)[1]],fill=fill,outline=outline,width=w)
    # base (1 thick)
    rect_xy(-26,26,25,26,(110,110,116),(30,30,34),2)
    # outer walls (1 thick, r26)
    rect_xy(-27,-26,26,68,(110,110,116),(30,30,34))
    rect_xy(26,27,26,68,(110,110,116),(30,30,34))
    # terrace ring (r25-28)
    rect_xy(-28,-25,67,68,(228,228,222),(100,100,105))
    rect_xy(25,28,67,68,(228,228,222),(100,100,105))
    # dial rim + floor
    rect_xy(-9.5,-8.5,53,58,(150,150,154),(60,60,60))
    rect_xy(8.5,9.5,53,58,(150,150,154),(60,60,60))
    rect_xy(-9,9,57.5,58,(233,223,199),(120,115,105))
    # crown gold band, cross, yellow top ring, posts
    rect_xy(-7,7,69,72,(198,158,34),(100,75,10))
    rect_xy(-8,8,73,74,(240,236,225),(120,115,105))
    rect_xy(-7,7,78,80,(240,196,40),(100,75,10))
    for x in (-7,7):
        rect_xy(x-0.5,x+0.5,74,81,(150,150,154),(80,80,85))
        rect_xy(x-0.5,x+0.5,81,82,(190,30,30),(80,20,20))
    # pods projected
    for x in (-16,16):
        d.rectangle([P(x-3,77)[0],P(x-3,77)[1],P(x+3,27)[0],P(x+3,27)[1]],outline=(90,150,90,230),width=2)
        d.text((P(x,72)[0],P(x,72)[1]),"pod tower (y27-77)",font=f(10,True),fill=(40,90,40),anchor="lm")
    # labels
    d.text((750,26),"Section at z=16: height stack (base y26 to crown y82)",font=f(24,True),fill=(20,20,25),anchor="ma")
    levels=[(26,"base y26"),(58,"dial floor y58"),(68,"terrace y68"),(77,"pod tops y77"),(82,"crown top y82")]
    for yy,t in levels:
        d.line([P(-31,yy),P(31,yy)],fill=(180,180,185),width=1)
        d.text((P(30,yy)[0]+8,P(30,yy)[1]),f"{t}",font=f(12,True),fill=(40,40,45),anchor="lm")
    # vertical dims
    d.line([P(-31,26),P(-31,68)],fill=(30,30,30),width=2)
    d.text((P(-31,47)[0]-8,P(-31,47)[1]),"42",font=f(14,True),fill=(30,30,30),anchor="rm")
    d.line([P(-31,58),P(-31,68)],fill=(30,30,30),width=2)
    d.text((P(-31,63)[0]-8,P(-31,63)[1]),"10",font=f(14,True),fill=(30,30,30),anchor="rm")
    d.text((750,700),"crown y69-82: 8 gold-capped pillars, white cross/truss, yellow top ring, 8 red jewels",font=f(12),fill=(120,90,20),anchor="ma")
    d.text((750,720),"H: block bounds y26..82; terrace walk y68; dial floor y58; pit depth 42; no stairs between them.",font=f(13),fill=(50,50,55),anchor="ma")
    d.text((750,740),"Pods at x +/-16 are projected; the z=16 section runs between the 10 pods, not through one.",font=f(11),fill=(60,60,65),anchor="ma")
    im.save(os.path.join(BASE,"elevation_labelled.png"))
    print("saved elevation")

section()
