import csv,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
for y in (19,20,21,22,23,24,25,28,29,30,31):
    sub=[r for r in rows if int(r["y"])==y and 1992<=int(r["x"])<=2008 and -8<=int(r["z"])<=8]
    if not sub: continue
    print("y",y)
    for z in range(-8,9):
        line=""
        for x in range(1992,2009):
            bs=[r["block"].replace("minecraft:","") for r in sub if int(r["x"])==x and int(r["z"])==z]
            line+= "." if not bs else {"white_concrete":"W","stone_bricks":"S","smooth_quartz":"Q","barrel":"A","crafting_table":"C","chain":"c","polished_blackstone":"P","stripped_spruce_log":"L","campfire":"F","white_shulker_box":"B"}.get(bs[0],bs[0][0].upper())
        print("  ",line)
