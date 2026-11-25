import csv
from collections import Counter, defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    x,y,z=int(row["x"]),int(row["y"]),int(row["z"]); n=row["block"].replace("minecraft:","").split("[")[0]
    rows.append((x,y,z,n,row["block"]))
print("NW region x -33..-13 z -17..3 by y:")
for y in range(64,79):
    sel=[r for r in rows if r[1]==y and -33<=r[0]<=-13 and -17<=r[2]<=3]
    if sel:
        print(y, len(sel), dict(Counter(r[3] for r in sel)))
print("\nAll player_head / wall_banner / beehive coords:")
for r in rows:
    if r[3] in ("player_head","white_wall_banner","orange_wall_banner","yellow_wall_banner","green_wall_banner","cyan_wall_banner","beehive"):
        print(r[:4])
print("\nNorth path region x -12..6 z -22..6 by y:")
for y in range(66,76):
    sel=[r for r in rows if r[1]==y and -12<=r[0]<=6 and -22<=r[2]<=6]
    if sel:
        print(y, len(sel), dict(Counter(r[3] for r in sel)))
print("\nSouth deco x -12..8 z 34..45 by y:")
for y in range(66,76):
    sel=[r for r in rows if r[1]==y and -12<=r[0]<=8 and 34<=r[2]<=45]
    if sel:
        print(y, len(sel), dict(Counter(r[3] for r in sel)))
