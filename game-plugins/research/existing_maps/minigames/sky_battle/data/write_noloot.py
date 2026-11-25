import csv
rows=[r for r in csv.DictReader(open("data/commands_raw.csv")) if r["cluster"]=="CB-14"]
rows.sort(key=lambda r:(int(r["x"]),int(r["z"]),int(r["y"])))
out=[]
for i,r in enumerate(rows):
    if r["command"].startswith("/item "): continue
    out.append("%3d %s %s,%s,%s a=%s c=%s %s" % (i,r["kind"][:4],r["x"],r["y"],r["z"],r["auto"],r["condition_met"],r["command"]))
open("data/cb14_noloot.txt","w",encoding="utf-8").write("\n".join(out))
print(len(out))
