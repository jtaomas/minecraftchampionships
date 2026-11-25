import csv,collections,math
rows=[r for r in csv.DictReader(open("data/blocks_mg_sb_01.csv")) if "barrier" not in r["block"]]
top={}; blk={}
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    if 11<=math.hypot(x-2000,z)<=21.5:
        if y>top.get((x,z),-1): top[(x,z)]=y; blk[(x,z)]=r["block"].replace("minecraft:","")
c=collections.Counter(top.values())
print("top height histogram in annulus r11..21.5:")
for y in sorted(c): print(y,c[y])
print("\ntop block types:")
print(collections.Counter(blk.values()).most_common(30))
