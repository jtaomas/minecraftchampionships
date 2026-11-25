import gzip
from collections import Counter
DUMP=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"
origin=(-536,-64,-74)
pts={}
with gzip.open(DUMP,"rt",encoding="utf-8") as f:
    f.readline()
    for line in f:
        p=line.rstrip().split(" ",3)
        if len(p)<4: continue
        x=int(p[0])+origin[0]; y=int(p[1])+origin[1]; z=int(p[2])+origin[2]
        if p[3]=="water":
            pts[(x,y,z)]=1
for label,(x1,x2,z1,z2) in {"west_well":(-298,-290,45,75),"east_well":(-298,-290,100,132),"park_pond":(-170,-150,55,75),"dome":(-245,-212,78,100),"lake":(-125,-80,90,145)}.items():
    sel=[p for p in pts if x1<=p[0]<=x2 and z1<=p[2]<=z2]
    print(label,len(sel),"y",min((p[1] for p in sel),default=None),max((p[1] for p in sel),default=None),"x",min((p[0] for p in sel),default=None),max((p[0] for p in sel),default=None),"z",min((p[2] for p in sel),default=None),max((p[2] for p in sel),default=None))
    print(" y hist",Counter(p[1] for p in sel).most_common(10))
