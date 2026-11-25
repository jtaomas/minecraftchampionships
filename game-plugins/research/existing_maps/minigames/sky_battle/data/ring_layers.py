import csv,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
lay=collections.defaultdict(set)
for r in rows:
    y=int(r["y"])
    if y in (19,25,30,37):
        lay[y].add((int(r["x"]),int(r["z"]),r["block"].replace("minecraft:","")))
for y in (19,25,30,37):
    print("=== y",y,"===")
    for z in range(-23,24):
        line=""
        for x in range(1976,2025):
            bs=[b for (xx,zz,b) in lay[y] if xx==x and zz==z]
            if not bs: line+="."
            else:
                b=bs[0]
                line+= {"white_concrete":"W","red_concrete":"R","yellow_concrete":"Y","green_concrete":"G","blue_concrete":"B","red_terracotta":"r","yellow_terracotta":"y","green_terracotta":"g","blue_terracotta":"b","smooth_quartz":"Q","smooth_quartz_stairs":"q","stone_bricks":"S","oak_fence":"f","stone_brick_wall":"w","polished_blackstone_wall":"p","chain":"c"}.get(b,b[0].upper())
        print(f"{z:3d} {line}")
