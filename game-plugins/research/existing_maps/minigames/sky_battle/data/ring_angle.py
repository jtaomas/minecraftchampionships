import csv,collections,math
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
top={}; blk={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    d=math.hypot(x-2000,z)
    if 11<=d<=21.5:
        if y>top.get((x,z),-1): top[(x,z)]=y; blk[(x,z)]=r["block"].replace("minecraft:","")
import math as m
for deg in range(0,360,10):
    a=m.radians(deg)
    print("angle",deg)
    for rad in (12,13,14,15,16,17,18,19,20,21):
        x=round(2000+rad*m.cos(a)); z=round(rad*m.sin(a))
        print(f"  r{rad}={top.get((x,z),'-')}/{blk.get((x,z),'')[:6]}",end="")
    print()
