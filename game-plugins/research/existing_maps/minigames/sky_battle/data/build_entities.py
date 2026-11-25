import csv
src=list(csv.DictReader(open(r"..\data\entities.csv")))
rows=[]
for r in src:
    x=float(r["x"])
    if 1948<=x<=2052:
        rows.append(dict(type=r["type"],x=r["x"],y=r["y"],z=r["z"],role="border ring marker (spins via repeating tp, emits campfire smoke particles)",confidence="H"))
with open("data/entities.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=["type","x","y","z","role","confidence"]); w.writeheader(); w.writerows(rows)
print(rows)
