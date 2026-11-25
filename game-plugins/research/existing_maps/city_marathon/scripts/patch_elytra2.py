import csv
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\elytra.csv"
rows=list(csv.DictReader(open(p,encoding="utf-8")))
for r in rows:
    if r["id"]=="E7":
        r["pad"]="9x132 checkered platform x-351,y4,z23..154: 594 polished_diorite + 594 andesite tiles"
w=csv.DictWriter(open(p,"w",newline="",encoding="utf-8"),fieldnames=list(rows[0].keys()))
w.writeheader(); w.writerows(rows)
print("ok")
