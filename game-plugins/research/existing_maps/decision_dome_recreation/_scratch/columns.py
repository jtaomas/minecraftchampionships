import csv
from collections import Counter
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
rows=[]
for row in csv.DictReader(open(path,encoding="utf-8")):
    rows.append((int(row["x"]),int(row["y"]),int(row["z"]),row["block"].replace("minecraft:","")))
from collections import defaultdict
col=defaultdict(list)
for x,y,z,n in rows: col[(x,z)].append((y,n))

for p in [(-3,16),(-3,10),(-3,22),(-3,4),(-3,28),(5,16),(-11,16),(0,13),(0,19)]:
    print(f"\ncolumn {p}:")
    for y,n in sorted(col[p]):
        print(" ",y,n)

print("\nAll blocks y>=60 in central box x-14..8 z2..30 grouped by y:")
for y in range(60,80):
    sel=[(x,z,n) for (x,yy,z,n) in rows if yy==y and -14<=x<=8 and 2<=z<=30]
    c=Counter(n for x,z,n in sel)
    print(y, len(sel), dict(c))
