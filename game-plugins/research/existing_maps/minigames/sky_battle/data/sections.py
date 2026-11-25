import csv,math,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
def col(x,z):
    return sorted([(int(r["y"]),r["block"].replace("minecraft:","")) for r in rows if int(r["x"])==x and int(r["z"])==z])
for p in [(2000,21),(2000,20),(2000,19),(2000,18),(2000,17),(2000,16),(2000,15),(2000,14),(2000,13),(2000,12),(2000,11),(2000,10),(2000,8)]:
    print(p,col(*p))
