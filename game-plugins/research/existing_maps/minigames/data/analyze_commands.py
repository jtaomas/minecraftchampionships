import csv, re
from collections import Counter, defaultdict

rows = list(csv.DictReader(open("data/command_blocks.csv", encoding="utf-8")))
print("total commands", len(rows))
# tp destinations
dests = Counter()
for r in rows:
    for m in re.finditer(r"\btp\s+(?:@[a-z]|@p|@r|@s)[^\s]*\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)", r["command"]):
        dests[(int(m.group(1)), int(m.group(2)), int(m.group(3)))] += 1
print("--- tp destinations ---")
for d, n in dests.most_common(40):
    print(n, d)
print("--- setblock/fill areas ---")
areas = Counter()
for r in rows:
    m = re.search(r"\b(?:setblock|fill)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)", r["command"])
    if m:
        areas[(int(m.group(1))//16*16, int(m.group(3))//16*16)] += 1
for a, n in areas.most_common(20):
    print(n, a)
print("--- scoreboard objectives ---")
obj = Counter()
for r in rows:
    m = re.search(r"scoreboard objectives add (\S+)", r["command"])
    if m:
        obj[m.group(1)] += 1
print(obj.most_common(60))
print("--- unique command verbs ---")
verbs = Counter()
for r in rows:
    c = r["command"].strip()
    if c.startswith("/"):
        c = c[1:]
    verbs[c.split()[0] if c.split() else "?"] += 1
print(verbs.most_common(40))
