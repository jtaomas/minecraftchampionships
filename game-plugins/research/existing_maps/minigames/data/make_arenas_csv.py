#!/usr/bin/env python3
"""Generate clean per-island CSVs and the arena index from blocks_positions.csv."""
import csv, os
from collections import Counter, defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
D = os.path.join(ROOT, "data")
rows = list(csv.DictReader(open(os.path.join(D, "blocks_positions.csv"), encoding="utf-8")))
by = defaultdict(list)
for r in rows:
    by[int(r["island"])].append(r)

# clean block counts
with open(os.path.join(D, "blocks_by_island.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("island,block,count\n")
    for isl in sorted(by):
        c = Counter(r["block"] for r in by[isl])
        for b, n in c.most_common():
            fh.write(f"{isl},{b},{n}\n")

# split tile entities
te = list(csv.DictReader(open(os.path.join(D, "tile_entities.csv"), encoding="utf-8")))
with open(os.path.join(D, "signs.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("island,x,y,z,text\n")
    for r in te:
        if r["type"] == "sign":
            fh.write(f"{r['island']},{r['x']},{r['y']},{r['z']},\"{r['detail'].replace(chr(34), chr(39))}\"\n")
with open(os.path.join(D, "command_blocks.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("island,x,y,z,kind,auto,condition_met,command\n")
    pos = {(int(r["island"]), int(r["x"]), int(r["y"]), int(r["z"])): r["block"] for r in rows}
    for r in te:
        if "command_block" in r["type"]:
            k = pos.get((int(r["island"]), int(r["x"]), int(r["y"]), int(r["z"])), "?")
            kind = k.replace("minecraft:", "") if k != "?" else "command_block"
            fh.write(f"{r['island']},{r['x']},{r['y']},{r['z']},{kind},{r['auto']},{r['condition_met']},\"{r['detail'].replace(chr(34), chr(39))}\"\n")

META = {
    2: dict(id="MG-HUB-01", name="Main lobby ('crown' hub)", game="Hub / game select",
            entry="-196 24 927 (world spawn)", confidence="H", clusters="CB-03..CB-13 (71)",
            completeness="Complete",
            evidence="World spawn; 28 signs (Skybattle, Parkour Tag, Superclash, Sky Race); 4 teleport pads; 71 commands in 11 clusters"),
    5: dict(id="MG-SB-01", name="Sky Battle arena", game="Sky Battle",
            entry="2000 78 0 (lobby deck); pad -208 10 970", confidence="H", clusters="CB-18 (3)",
            completeness="Complete (dynamic loot)",
            evidence="10 signs 'Sky Battle'; teams Sky_Battle_Red/Blue/Green/Yellw; 4 team tp spawns; wool CanPlaceOn loadout; 474 logic commands in CB-14/15 + 3 local"),
    4: dict(id="MG-PT-01", name="Parkour Tag arena", game="Parkour Tag",
            entry="1061 28 995; pad -212 10 975", confidence="H", clusters="CB-16 (121); CB-17 (2)",
            completeness="Complete",
            evidence="25 signs; bossbar 'Parkour Tag - Timer'; tellraw '[Parkour Tag] The Runners/Hunter wins!'; 123 commands"),
    6: dict(id="MG-SC-01", name="Super Clash arena", game="Super Clash",
            entry="5000 21 0; pad -205 10 970 (lvl 15)", confidence="H", clusters="CB-19 (161); CB-20 (3); CB-21 (1)",
            completeness="Complete",
            evidence="32 signs; tellraw '[Super Clash] The Villains/Heroes win!'; teams Super_Clash_H/V; class kits; 165 commands"),
    7: dict(id="MG-SR-HUB-01", name="Sky Race level select", game="Sky Race hub",
            entry="20000 41 20000; pad -201 10 976 (lvl 100)", confidence="H", clusters="CB-22 (4); CB-23 (1)",
            completeness="Partial: 2 of 6 levels wired",
            evidence="6 signs Red/Orange/Yellow/Green/Blue/Purple Level; only red/orange pads wired"),
    8: dict(id="MG-SR-RED-01", name="Sky Race - Red course", game="Sky Race",
            entry="21000 31 20000", confidence="H", clusters="CB-24 (49)",
            completeness="Complete for this level",
            evidence="'finished the red course!'; team Sky_Race; 49 commands; white_wool reset fills; shears loadout"),
    9: dict(id="MG-SR-ORG-01", name="Sky Race - Orange course", game="Sky Race",
            entry="21000 31 21000", confidence="H", clusters="CB-25 (49)",
            completeness="Complete for this level",
            evidence="'finished the orange course!'; team Sky_Race; 49 commands; same layout as Red"),
    3: dict(id="MG-CMD-01", name="Sky Battle command room", game="Sky Battle logic",
            entry="none (service room)", confidence="H", clusters="CB-14 (445); CB-15 (29)",
            completeness="Complete",
            evidence="474 command blocks / 57 signs in two clusters; sign 'at 1000 10 10 are all the command blocks'; all commands are Sky Battle teams/loadout/loot"),
    1: dict(id="MG-REB-01", name="'Rebecca's Minigames' kiosk", game="Unknown / parkour hub (M)",
            entry="-50000 100 -50000", confidence="M", clusters="CB-02 (2)",
            completeness="Partial / demo",
            evidence="Signs 'Rebecca's Minigames', 'Lobby Teleporter'; 2 commands set XP 200/201 then tp"),
    0: dict(id="MG-REB-02", name="'Rebecca' linear course", game="Parkour / race (M)",
            entry="-50000 100 -51000", confidence="M", clusters="CB-01 (1)",
            completeness="Partial / demo",
            evidence="1 sign 'Lobby Teleporter'; jump-boost command on waxed copper; linear white-concrete course over lava"),
}
ORDER = [2, 5, 4, 6, 7, 8, 9, 3, 1, 0]
with open(os.path.join(D, "arenas.csv"), "w", encoding="utf-8", newline="") as fh:
    fh.write("id,name,likely_game,xmin,xmax,ymin,ymax,zmin,zmax,spawn_or_entry,chunks,blocks,signs,"
             "command_blocks,command_clusters,palette_top,evidence,completeness,confidence\n")
    for isl in ORDER:
        bl = by[isl]
        xs = [int(r["x"]) for r in bl]; ys = [int(r["y"]) for r in bl]; zs = [int(r["z"]) for r in bl]
        c = Counter(r["block"].replace("minecraft:", "") for r in bl)
        pal = ";".join(f"{k}={v}" for k, v in c.most_common(8))
        m = META[isl]
        ns = sum(1 for r in te if int(r["island"]) == isl and r["type"] == "sign")
        nc = sum(1 for r in te if int(r["island"]) == isl and r["type"] == "command_block")
        chunks = len({(int(r["x"]) // 16, int(r["z"]) // 16) for r in bl})
        fh.write(f"{m['id']},{m['name']},{m['game']},{min(xs)},{max(xs)},{min(ys)},{max(ys)},{min(zs)},{max(zs)},"
                 f"{m['entry']},{chunks},{len(bl)},{ns},{nc},\"{m['clusters']}\",\"{pal}\",\"{m['evidence']}\","
                 f"\"{m['completeness']}\",{m['confidence']}\n")
print("wrote arenas.csv, blocks_by_island.csv, signs.csv, command_blocks.csv")
for isl in ORDER:
    bl = by[isl]
    print(META[isl]["id"], len(bl), "blocks",
          len({(int(r['x'])//16,int(r['z'])//16) for r in bl}), "chunks")
