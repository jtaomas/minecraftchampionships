import csv,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
sub=[r for r in rows if 1961<=int(r["x"])<=1973 and -6<=int(r["z"])<=6 and 11<=int(r["y"])<=26]
byy=collections.defaultdict(dict)
for r in sub: byy[int(r["y"])][(int(r["x"]),int(r["z"]))]=r["block"].replace("minecraft:","")
chars={"white_concrete":"W","red_concrete":"R","red_terracotta":"t","red_glazed_terracotta":"G","red_stained_glass":"g","smooth_quartz":"Q","smooth_quartz_stairs":"q","white_shulker_box":"S","crafting_table":"C","red_wool":"w","oak_trapdoor":"T","stone_bricks":"B","spruce_slab":"s","chest":"H","barrel":"A","iron_block":"I"}
for y in sorted(byy):
    print("y",y)
    for z in range(-6,7):
        line=""
        for x in range(1961,1974):
            b=byy[y].get((x,z))
            line+= "." if not b else chars.get(b,b[0].upper())
        print("  ",line)
