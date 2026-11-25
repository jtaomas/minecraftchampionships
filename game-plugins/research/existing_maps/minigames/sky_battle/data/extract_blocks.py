import csv
src=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\minigames\data\blocks_positions.csv"
out=r"data\blocks_mg_sb_01.csv"
n=0
with open(src,newline="") as f, open(out,"w",newline="") as g:
    r=csv.DictReader(f); w=csv.writer(g); w.writerow(["x","y","z","block"])
    for row in r:
        if row["island"]!="5": continue
        w.writerow([row["x"],row["y"],row["z"],row["block"]]); n+=1
print("rows",n)
