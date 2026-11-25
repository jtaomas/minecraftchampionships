import csv
src=list(csv.DictReader(open(r"..\data\signs.csv")))
rows=[]
for r in src:
    if r["island"]!="5": continue
    text=r["text"].replace(" | "," / ")
    rows.append(dict(x=r["x"],y=r["y"],z=r["z"],front_text=text,role=("team join" if "Join team" in text else "start button" if "start the game" in text else "bug reset" if "bugged" in text else "command-room pointer" if "command" in text else "other"),confidence="H"))
with open("data/signs.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=["x","y","z","front_text","role","confidence"]); w.writeheader(); w.writerows(rows)
print(len(rows))
for r in rows: print(r)
