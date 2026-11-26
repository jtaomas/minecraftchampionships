import re, collections, json, pathlib
p = pathlib.Path("game-plugins/BuildMart/src/main/java/com/mcm/games/buildmart/BuildPreset.java").read_text()
legend = dict(re.findall(r"LEGEND\.put\('(.)', Material\.([A-Z_]+)\);", p))
presets = []
for m in re.finditer(r'p\("([^"]+)",\s*(.*?)\)\s*,?\s*(?=p\("|\)\s*;)', p, re.S):
    name, body = m.group(1), m.group(2)
    layers = re.findall(r'L\(([^)]*)\)', body)
    grid=[]
    for lay in layers:
        rows = re.findall(r'"([^"]*)"', lay)
        grid.append(rows)
    counts=collections.Counter()
    for lay in grid:
        for row in lay:
            for ch in row:
                if ch!='.':
                    counts[legend[ch]]+=1
    presets.append((name, counts, sum(counts.values())))
print(len(presets))
tot=collections.Counter()
for n,c,b in presets:
    tot.update(c)
    print(f"{n}\t{b}\t"+", ".join(f"{k}:{v}" for k,v in c.items()))
print("TOTAL")
for k,v in tot.most_common(): print(k,v)
