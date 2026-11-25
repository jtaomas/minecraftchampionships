import csv
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\elytra.csv"
rows=list(csv.DictReader(open(p,encoding="utf-8")))
for r in rows:
    if r["id"]=="E2":
        r["pad"]="polished_andesite roof; NO polished_diorite - predicate cannot match"
    if r["id"] in ("E3","E4","E5","E6"):
        r["pad"]="polished_andesite roof; NO polished_diorite - predicate cannot match"
    if r["id"]=="E7":
        r["command"]="global CB01/02/29/34/66/67"
w=csv.DictWriter(open(p,"w",newline="",encoding="utf-8"),fieldnames=list(rows[0].keys()))
w.writeheader(); w.writerows(rows)
print("patched elytra.csv")
