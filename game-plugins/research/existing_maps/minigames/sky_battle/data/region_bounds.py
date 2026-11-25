import csv,math,collections
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
def in_island(x,z):
    return (1961<=x<=1973 and -6<=z<=6) or (2027<=x<=2039 and -6<=z<=6) or (1994<=x<=2006 and 27<=z<=39) or (1994<=x<=2006 and -39<=z<=-27)
def in_balloon(x,z):
    return (1970<=x<=1978 and -30<=z<=-22) or (1970<=x<=1978 and 22<=z<=30) or (2022<=x<=2030 and -30<=z<=-22) or (2022<=x<=2030 and 22<=z<=30)
ring=[r for r in rows if not in_island(int(r["x"]),int(r["z"])) and not in_balloon(int(r["x"]),int(r["z"])) and math.hypot(int(r["x"])-2000,int(r["z"]))<=22 and int(r["y"])<45]
print("ring blocks",len(ring))
for name,flt in [
 ("all ring",lambda r:True),
 ("canopy y>=27",lambda r:int(r["y"])>=27),
 ("floor y25",lambda r:int(r["y"])==25),
 ("inner lip y19",lambda r:int(r["y"])==19),
 ("outer rim y25 r19",lambda r:int(r["y"])==25 and 18.5<=math.hypot(int(r["x"])-2000,int(r["z"]))<=19.5),
 ("fence",lambda r:"fence" in r["block"] or "leaves" in r["block"]),
]:
    sub=[r for r in ring if flt(r)]
    if not sub: continue
    xs=[int(r["x"]) for r in sub]; ys=[int(r["y"]) for r in sub]; zs=[int(r["z"]) for r in sub]
    print(name,len(sub),f"x{min(xs)}..{max(xs)} y{min(ys)}..{max(ys)} z{min(zs)}..{max(zs)}")
# mid
mid=[r for r in rows if math.hypot(int(r["x"])-2000,int(r["z"]))<10 and int(r["y"])<40]
xs=[int(r["x"]) for r in mid]; ys=[int(r["y"]) for r in mid]; zs=[int(r["z"]) for r in mid]
print("mid",len(mid),f"x{min(xs)}..{max(xs)} y{min(ys)}..{max(ys)} z{min(zs)}..{max(zs)}")
print(collections.Counter(r["block"].replace("minecraft:","") for r in mid))
