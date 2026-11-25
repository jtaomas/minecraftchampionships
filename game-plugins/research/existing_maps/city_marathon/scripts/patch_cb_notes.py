import csv
p=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data\command_blocks.csv"
rows=list(csv.DictReader(open(p,encoding="utf-8")))
for r in rows:
    if r["command"].strip().startswith("/gamemode"):
        r["notes"]="Leading slash is accepted by command blocks (the /fill and /title blocks have SuccessCount=1); this block fails because it has no player target, not because of the slash. SuccessCount=0."
w=csv.DictWriter(open(p,"w",newline="",encoding="utf-8"),fieldnames=list(rows[0].keys()))
w.writeheader(); w.writerows(rows)
print("patched")
