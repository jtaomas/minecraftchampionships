import csv, json, re, sys
from collections import Counter

# --- signs ---
def parse_text(raw):
    # raw is a string like "{'text':'...'} | {'text':'...'}"
    parts = []
    for chunk in raw.split(" | "):
        try:
            s = chunk.replace("'", '"')
            o = json.loads(s)
            t = o.get("text", "")
            if "extra" in o:
                t += "".join(e.get("text", "") for e in o["extra"])
            parts.append(t)
        except Exception:
            parts.append(chunk)
    return parts

rows = list(csv.DictReader(open("data/signs.csv", encoding="utf-8")))
print("=== SIGNS", len(rows), "===")
for r in sorted(rows, key=lambda r: (int(r["island"]), int(r["x"]), int(r["z"]))):
    lines = [l for l in parse_text(r["text"]) if l.strip()]
    if lines:
        print(f"isl{r['island']:>2} ({r['x']:>7},{r['y']:>4},{r['z']:>7})  " + " / ".join(lines))

print()
print("=== COMMANDS by island and keyword ===")
rows = list(csv.DictReader(open("data/command_blocks.csv", encoding="utf-8")))
byisl = Counter(r["island"] for r in rows)
print("counts", dict(sorted(byisl.items(), key=lambda kv: int(kv[0]))))
kw = Counter()
for r in rows:
    c = r["command"]
    for k in ["structure", "fill", "clone", "setblock", "summon", "tp ", "title", "tellraw", "scoreboard",
              "effect", "give", "clear", "gamemode", "time set", "weather", "playsound", "particle", "function"]:
        if k in c:
            kw[k] += 1
print("keywords", kw.most_common())
print()
print("=== sample commands per island ===")
seen = Counter()
for r in rows:
    i = int(r["island"])
    if seen[i] >= 8:
        continue
    seen[i] += 1
    print(f"isl{i:>2} ({r['x']},{r['y']},{r['z']}) {r['command'][:220]}")
