"""QA checks for the generated LBR-3 pack."""
import csv
import math
import os
import re

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PADS = [(5000 + 72 * math.cos(math.radians(k * 45)),
         5000 + 72 * math.sin(math.radians(k * 45))) for k in range(8)]

rows = list(csv.DictReader(open(os.path.join(HERE, "blocks.csv"), encoding="utf-8")))
print("blocks:", len(rows))
ys = [int(r["y"]) for r in rows]
print("y range:", min(ys), max(ys), "| below 121:", sum(1 for y in ys if y < 121))
xx = [int(r["x"]) for r in rows]
zz = [int(r["z"]) for r in rows]
print("x range:", min(xx), max(xx), "z range:", min(zz), max(zz))

bad_pad = 0
for r in rows:
    x, z = int(r["x"]), int(r["z"])
    for px, pz in PADS:
        if abs(x - px) <= 9.5 and abs(z - pz) <= 9.5:
            bad_pad += 1
print("blocks inside a 19x19 pad footprint:", bad_pad)

# build site bounds from the brief
outside = sum(1 for x, z in zip(xx, zz) if not (4860 <= x <= 5140 and 4860 <= z <= 5140))
print("blocks outside x/z 4860..5140:", outside)

# function files
fn = os.path.join(HERE, "functions")
total_cmds = 0
bad = []
for name in sorted(os.listdir(fn)):
    lines = open(os.path.join(fn, name), encoding="utf-8").read().splitlines()
    total_cmds += len(lines)
    for ln in lines:
        if not (ln.startswith("fill ") or ln.startswith("setblock ")):
            bad.append((name, ln))
        if ln.startswith("setblock "):
            parts = ln.split()
            if len(parts) != 5 or not parts[-1].startswith("minecraft:"):
                bad.append((name, ln))
        if ln.startswith("fill "):
            parts = ln.split()
            if len(parts) != 8 or not parts[-1].startswith("minecraft:"):
                bad.append((name, ln))
print("commands:", total_cmds, "| malformed:", len(bad))
for b in bad[:5]:
    print("  ", b)

# cross-check command block count vs csv
n_set = 0
n_fill = 0
for name in sorted(os.listdir(fn)):
    for ln in open(os.path.join(fn, name), encoding="utf-8"):
        if ln.startswith("fill "):
            p = ln.split()
            n_fill += abs(int(p[4]) - int(p[1])) + 1
        else:
            n_set += 1
print("blocks from functions:", n_set + n_fill, "(setblock", n_set, "+ fill", n_fill, ")")
