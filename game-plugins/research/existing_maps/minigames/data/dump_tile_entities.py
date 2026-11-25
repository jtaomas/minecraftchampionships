import csv, json, re
from collections import defaultdict

def parse_text(raw):
    parts = []
    for chunk in raw.split(" | "):
        try:
            o = json.loads(chunk.replace("'", '"'))
            t = o.get("text", "")
            if "extra" in o:
                t += "".join(e.get("text", "") for e in o["extra"])
            parts.append(t)
        except Exception:
            parts.append(chunk)
    return [p for p in parts if p.strip()]

rows = list(csv.DictReader(open("data/tile_entities.csv", encoding="utf-8")))
for isl in sorted(set(int(r["island"]) for r in rows)):
    print(f"===== ISLAND {isl} =====")
    signs = [r for r in rows if int(r["island"]) == isl and r["type"] == "sign"]
    for r in sorted(signs, key=lambda r: (int(r["y"]), int(r["x"]), int(r["z"]))):
        print(f"S ({r['x']},{r['y']},{r['z']}) " + " / ".join(parse_text(r["detail"])))
    cmds = [r for r in rows if int(r["island"]) == isl and r["type"] == "command_block"]
    print(f"-- commands: {len(cmds)}")
    for r in sorted(cmds, key=lambda r: (int(r["y"]), int(r["x"]), int(r["z"]))):
        print(f"C ({r['x']},{r['y']},{r['z']}) {r['detail']}")
