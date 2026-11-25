import csv, math
from collections import Counter
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(p,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0]))
CX,CZ=-3,16
def ang(x,z): return (math.degrees(math.atan2(z-CZ,x-CX))+360)%360
def rad(x,z): return math.hypot(x-CX,z-CZ)

print("y58 gray inner (r<9.8) angle 1-deg bins:")
sel=[(x,z) for (x,y,z,n) in rows if y==58 and n=="gray_concrete" and rad(x,z)<9.8]
for a,c in sorted(Counter(round(ang(x,z)) for x,z in sel).items()):
    print(a,c,end="; ")
print("\n\nall y58 gray by radius/angle (sample):")
for r in range(0,10):
    pts=[(round(ang(x,z)),x,z) for (x,y,z,n) in rows if y==58 and n=="gray_concrete" and round(rad(x,z))==r]
    print("r",r,"angles",sorted(set(a for a,_,_ in pts)))
