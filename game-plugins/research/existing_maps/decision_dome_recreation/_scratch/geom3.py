import csv, math
from collections import Counter, defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0],row["block"]))
CX,CZ=-3,16

def rad(x,z): return math.hypot(x-CX,z-CZ)
def ang(x,z): return (math.degrees(math.atan2(z-CZ,x-CX))+360)%360

print("=== radial hist y=68 by material ===")
for mat in ["white_concrete","gray_concrete","black_concrete"]:
    sel=[r for r in rows if r[1]==68 and r[3]==mat]
    h=Counter(round(rad(r[0],r[2])) for r in sel)
    print(mat, len(sel), sorted(h.items()))
print("barrier y71 radial:", sorted(Counter(round(rad(r[0],r[2])) for r in rows if r[1]==71 and r[3]=="barrier").items()))
print("barrier angle bins:", sorted(Counter(int(ang(r[0],r[2]))//15*15 for r in rows if r[1]==71 and r[3]=="barrier").items()))

print("\n=== outer wall y40 radial ===")
sel=[r for r in rows if r[1]==40 and r[3]=="gray_concrete"]
print(len(sel), sorted(Counter(round(rad(r[0],r[2])) for r in sel).items()))

print("\n=== y58 dial radial by material ===")
for mat in ["gray_concrete","black_concrete","smooth_quartz_slab","quartz_slab"]:
    sel=[r for r in rows if r[1]==58 and r[3]==mat]
    if sel: print(mat, len(sel), sorted(Counter(round(rad(r[0],r[2])) for r in sel).items()))
print("black y58 angle 5deg bins:", sorted(Counter(int(ang(r[0],r[2])//5*5) for r in rows if r[1]==58 and r[3]=="black_concrete").items()))

print("\n=== pod centres from y68 coloured glass rings ===")
for col in ["red","orange","yellow","lime","green","cyan","light_blue","blue","purple","pink"]:
    sel=[r for r in rows if r[1]==68 and r[3]==f"{col}_stained_glass"]
    if sel:
        xs=[r[0] for r in sel]; zs=[r[2] for r in sel]
        cx=sum(xs)/len(xs); cz=sum(zs)/len(zs)
        print(f"{col:11s} n={len(sel):3d} centre {cx:.2f},{cz:.2f} r={rad(cx,cz):.2f} ang={ang(cx,cz):.1f} bbox x{min(xs)}..{max(xs)} z{min(zs)}..{max(zs)}")

print("\n=== pod vertical profile (lime) counts and bbox ===")
for y in range(26,79):
    sel=[r for r in rows if r[1]==y and r[0]>=-8 and r[0]<=2 and r[2]>=-5 and r[2]<=5 and r[3].startswith("lime")]
    if sel:
        print(y, len(sel), "x",min(r[0] for r in sel),max(r[0] for r in sel),"z",min(r[2] for r in sel),max(r[2] for r in sel), Counter(r[3] for r in sel))

print("\n=== chandelier layers y69..82 central box ===")
for y in range(69,83):
    sel=[r for r in rows if r[1]==y and -12<=r[0]<=8 and 6<=r[2]<=26]
    if sel:
        print(y, len(sel), dict(Counter(r[3] for r in sel)))

print("\n=== decorative blocks (stairs/slabs/etc) ===")
for mat in ["polished_andesite_stairs","polished_andesite_slab","smooth_quartz_slab","quartz_slab","quartz_stairs","smooth_quartz_stairs","stone_slab","red_nether_brick_slab","smooth_red_sandstone_slab","iron_trapdoor","iron_door","acacia_pressure_plate","sea_lantern","chain","barrier","light_gray_carpet","gray_carpet","yellow_wool","light_blue_wool","lime_wool","purple_wool","gray_wool","redstone_block","oak_leaves","player_head","beehive"]:
    sel=[r for r in rows if r[3]==mat]
    if not sel: continue
    print(f"\n{mat}: n={len(sel)}")
    # group by y
    for y,c in sorted(Counter(r[1] for r in sel).items()):
        pts=[(r[0],r[2]) for r in sel if r[1]==y]
        print(f"  y={y} n={c} x{min(p[0] for p in pts)}..{max(p[0] for p in pts)} z{min(p[1] for p in pts)}..{max(p[1] for p in pts)}")

print("\n=== command block / plate / door context ===")
for r in rows:
    if r[3] in ("command_block","chain_command_block","acacia_pressure_plate","iron_door","iron_trapdoor","redstone_block"):
        print(r[:4])
