import csv, math
from collections import Counter, defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0],row["block"]))
CX,CZ=-3,16
def rad(x,z): return math.hypot(x-CX,z-CZ)
def ang(x,z): return (math.degrees(math.atan2(z-CZ,x-CX))+360)%360

print("black concrete by y:")
for y,c in sorted(Counter(r[1] for r in rows if r[3]=="black_concrete").items()):
    pts=[(r[0],r[2]) for r in rows if r[1]==y and r[3]=="black_concrete"]
    print(" y",y,c,"x",min(p[0] for p in pts),max(p[0] for p in pts),"z",min(p[1] for p in pts),max(p[1] for p in pts))

print("\ny57 layer central:")
for y in [57,58,59]:
    print("y",y)
    for z in range(7,26):
        line=""
        for x in range(-13,7):
            b=None
            for r in rows:
                if r[0]==x and r[1]==y and r[2]==z: b=r[3]; break
            ch={"black_concrete":"#","smooth_quartz_slab":"s","gray_concrete":"G","air":".",None:"."}
            line+=ch.get(b,"?")
        print(f"  z{z:02d} {line}")

print("\ny78..82 coordinates:")
for y in range(78,83):
    pts=[(r[0],r[2]) for r in rows if r[1]==y]
    print("y",y,"n",len(pts),sorted(pts)[:80])

print("\npod centres y68 (panes):")
for col in ["red","orange","yellow","lime","green","cyan","light_blue","blue","purple","pink"]:
    sel=[r for r in rows if r[1]==68 and r[3]==f"{col}_stained_glass_pane"]
    if sel:
        xs=[r[0] for r in sel]; zs=[r[2] for r in sel]
        cx=sum(xs)/len(xs); cz=sum(zs)/len(zs)
        print(f"{col:11s} n={len(sel):3d} centre {cx:.2f},{cz:.2f} r={rad(cx,cz):.2f} ang={ang(cx,cz):.1f} bbox x{min(xs)}..{max(xs)} z{min(zs)}..{max(zs)}")

print("\nsea lantern positions:")
for r in rows:
    if r[3]=="sea_lantern": print(r[:4])
print("\nchain positions:")
for r in rows:
    if r[3]=="chain": print(r[:4])
