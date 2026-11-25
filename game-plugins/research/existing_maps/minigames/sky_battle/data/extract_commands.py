import csv
src=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\minigames\data\command_blocks.csv"
rows=[r for r in csv.DictReader(open(src)) if r["island"] in ("3","5")]
for r in rows:
    x,y,z=int(r["x"]),int(r["y"]),int(r["z"])
    cl="CB-15" if (993<=x<=1004 and 15<=y<=16 and -16<=z<=-8) else ("CB-14" if r["island"]=="3" else "CB-18")
    r["cluster"]=cl
w=csv.DictWriter(open("data/commands_raw.csv","w",newline=""),fieldnames=["cluster","x","y","z","kind","auto","condition_met","command"])
w.writeheader()
for r in rows: w.writerow({k:r[k] for k in w.fieldnames})
print(len(rows))
from collections import Counter
print(Counter(r["cluster"] for r in rows))
