import csv
from collections import defaultdict
path=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv"
col=defaultdict(list)
for row in csv.DictReader(open(path,encoding="utf-8")):
    col[(int(row["x"]),int(row["z"]))].append((int(row["y"]),row["block"].replace("minecraft:","")))

for x in range(-30,11):
    lst=sorted(col[(x,16)])
    s=", ".join(f"{y}:{n.split('[')[0]}" for y,n in lst)
    print(f"x={x:4d} z=16 -> {s}")
