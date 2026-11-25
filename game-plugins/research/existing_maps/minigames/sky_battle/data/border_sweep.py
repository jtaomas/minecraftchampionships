import csv,re
rows=list(csv.DictReader(open("data/commands_raw.csv")))
zs=set()
for r in rows:
    c=r["command"]
    if c.startswith("/fill") and "air replace" in c:
        m=re.match(r"/fill \d+ \d+ (-?\d+) \d+ \d+ (-?\d+)",c)
        zs.add((int(m.group(1)),int(m.group(2))))
print("z strips",sorted(zs))
print("count",len(zs))
for r in rows:
    c=r["command"]
    if "timer_skybattle=" in c and "tellraw" in c:
        print(c.split("timer_skybattle=")[1].split("]")[0], c.split("'")[3] if len(c.split("'"))>3 else "")
print("border trigger x -> threshold")
for r in rows:
    c=r["command"]
    if c.startswith("/execute") and "timer_skybattle=" in c and "redstone_block" in c:
        print(r["x"], c.split("timer_skybattle=")[1].split("]")[0])
