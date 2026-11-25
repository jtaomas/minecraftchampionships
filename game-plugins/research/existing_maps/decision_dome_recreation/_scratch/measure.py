import csv, math
from collections import Counter, defaultdict
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(p,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","").split("[")[0]))
def bbox(sel):
    if not sel: return None
    return (min(x for x,y,z in sel),max(x for x,y,z in sel),min(y for x,y,z in sel),max(y for x,y,z in sel),min(z for x,y,z in sel),max(z for x,y,z in sel))
def show(name,sel): print(f"{name:28s} n={len(sel):5d} bbox x{bbox(sel)[0]}..{bbox(sel)[1]} y{bbox(sel)[2]}..{bbox(sel)[3]} z{bbox(sel)[4]}..{bbox(sel)[5]}")

show("west gate all",[(x,y,z) for x,y,z,n in rows if -46<=x<=-36 and 10<=z<=22 and y>=64])
show("east gate all",[(x,y,z) for x,y,z,n in rows if 30<=x<=40 and 10<=z<=22 and y>=64])
show("north red path",[(x,y,z) for x,y,z,n in rows if -9<=x<=3 and -22<=z<=-5 and n=="red_concrete"])
show("north platform",[(x,y,z) for x,y,z,n in rows if -9<=x<=3 and -22<=z<=-5 and y>=66])
show("NW annex all",[(x,y,z) for x,y,z,n in rows if -33<=x<=-20 and -16<=z<=-1 and y>=64])
show("NW glass roof",[(x,y,z) for x,y,z,n in rows if -33<=x<=-20 and -16<=z<=-1 and y==73])
show("SE door frame",[(x,y,z) for x,y,z,n in rows if 16<=x<=24 and 34<=z<=42 and y>=64])
show("base y26",[(x,y,z) for x,y,z,n in rows if y==26])
show("drum wall y40",[(x,y,z) for x,y,z,n in rows if y==40 and n=="gray_concrete"])
show("terrace white y68",[(x,y,z) for x,y,z,n in rows if y==68 and n=="white_concrete"])
show("barrier y71",[(x,y,z) for x,y,z,n in rows if y==71 and n=="barrier"])
show("dial quartz y58",[(x,y,z) for x,y,z,n in rows if y==58 and n=="smooth_quartz_slab"])
show("crown all",[(x,y,z) for x,y,z,n in rows if -11<=x<=5 and 8<=z<=24 and 69<=y<=82])
show("pod lime all",[(x,y,z) for x,y,z,n in rows if -7<=x<=1 and -4<=z<=4 and 26<=y<=78 and n.startswith("lime")])
# materials by family
fam=defaultdict(int)
def family(n):
    if "concrete" in n: return "concrete"
    if "terracotta" in n: return "terracotta"
    if "glass" in n: return "glass"
    if n.startswith("polished_diorite") or n in ("stone","stone_slab"): return "diorite/stone"
    if "quartz" in n: return "quartz"
    if "wool" in n: return "wool"
    if "carpet" in n: return "carpet"
    if "stained_glass" in n: return "glass"
    if n in ("gold_block",): return "gold"
    if n in ("chain",): return "metal"
    if n in ("sea_lantern",): return "light"
    if "slab" in n or "stairs" in n: return "slabs/stairs"
    if "banner" in n: return "banner"
    if "head" in n: return "head"
    return "other"
for x,y,z,n in rows: fam[family(n)]+=1
print("\nfamily totals:")
for k,v in sorted(fam.items(),key=lambda kv:-kv[1]): print(f"  {k:14s} {v:6d} {v/len(rows)*100:5.2f}%")
