import csv, os
from collections import defaultdict

D = os.path.dirname(__file__)
rows = list(csv.DictReader(open(os.path.join(D, "blocks_positions.csv"), encoding="utf-8")))
by = defaultdict(list)
for r in rows:
    by[int(r["island"])].append(r)

ids = {0: "MG-REB-02", 1: "MG-REB-01", 2: "MG-HUB-01", 3: "MG-CMD-01", 4: "MG-PT-01",
       5: "MG-SB-01", 6: "MG-SC-01", 7: "MG-SR-HUB-01", 8: "MG-SR-RED-01", 9: "MG-SR-ORG-01"}
with open(os.path.join(D, "regions.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("id,chunk_x_min,chunk_x_max,chunk_z_min,chunk_z_max,chunk_columns,region_files\n")
    for i in sorted(by):
        ch = {(int(r["x"]) // 16, int(r["z"]) // 16) for r in by[i]}
        regs = sorted({(c[0] // 32, c[1] // 32) for c in ch})
        fh.write(f"{ids[i]},{min(c[0] for c in ch)},{max(c[0] for c in ch)},"
                 f"{min(c[1] for c in ch)},{max(c[1] for c in ch)},{len(ch)},"
                 f"{';'.join(f'r.{a}.{b}' for a, b in regs)}\n")

entries = [
    ("MG-HUB-01", "world spawn", "-196 24 927", "Level.dat SpawnX/Y/Z"),
    ("MG-SB-01", "sky lobby", "2000 78 0", "hub pad -208 10 970 (XP 8)"),
    ("MG-PT-01", "waiting lobby", "1061 28 995", "hub pad -212 10 975 (XP 4)"),
    ("MG-SC-01", "arena centre", "5000 21 0", "hub pad -205 10 970 (XP 15)"),
    ("MG-SR-HUB-01", "level select", "20000 41 20000", "hub pad -201 10 976 (XP 100)"),
    ("MG-SR-RED-01", "course start", "21000 31 20000", "hub red pad (XP 101)"),
    ("MG-SR-ORG-01", "course start", "21000 31 21000", "hub orange pad (XP 103)"),
    ("MG-CMD-01", "service room", "none", "no player entry"),
    ("MG-REB-01", "kiosk", "-50000 100 -50000", "no hub pad; reachable only by teleport"),
    ("MG-REB-02", "course start", "-50000 100 -51000", "kiosk pad (XP 200/201)"),
]
with open(os.path.join(D, "entries.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("id,entry_name,coords,how_reached\n")
    for r in entries:
        fh.write(",".join(r) + "\n")
print("wrote regions.csv, entries.csv")
