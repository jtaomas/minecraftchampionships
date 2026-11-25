#!/usr/bin/env python3
"""Extract the MG-PT-01 subset from A6's read-only exports.

Writes pt_blocks.csv, pt_commands.csv, pt_signs.csv in this folder.
Reads only: ../data/blocks_positions.csv, ../data/command_blocks.csv, ../data/signs.csv
"""
import csv
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.abspath(os.path.join(HERE, "..", "..", "data"))

X0, X1 = 1035, 1078
Y0, Y1 = 0, 58
Z0, Z1 = 972, 1017

def inside(x, y, z):
    return X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1

# blocks
rows = []
with open(os.path.join(SRC, "blocks_positions.csv"), newline="", encoding="utf-8") as f:
    for r in csv.DictReader(f):
        x, y, z = int(r["x"]), int(r["y"]), int(r["z"])
        if inside(x, y, z):
            rows.append((x, y, z, r["block"]))
rows.sort(key=lambda t: (t[1], t[2], t[0]))
with open(os.path.join(HERE, "pt_blocks.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["x", "y", "z", "block"])
    w.writerows(rows)
print("blocks", len(rows))

# commands
rows = []
with open(os.path.join(SRC, "command_blocks.csv"), newline="", encoding="utf-8") as f:
    for r in csv.DictReader(f):
        x, y, z = int(r["x"]), int(r["y"]), int(r["z"])
        if inside(x, y, z) or r["island"] == "4":
            rows.append(r)
rows.sort(key=lambda r: (int(r["y"]), int(r["z"]), int(r["x"])))
with open(os.path.join(HERE, "pt_commands.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=["x", "y", "z", "kind", "auto", "condition_met", "command"])
    w.writeheader()
    for r in rows:
        w.writerow({k: r[k] for k in w.fieldnames})
print("commands", len(rows))

# signs
rows = []
with open(os.path.join(SRC, "signs.csv"), newline="", encoding="utf-8") as f:
    for r in csv.DictReader(f):
        x, y, z = int(r["x"]), int(r["y"]), int(r["z"])
        if r["island"] == "4" or inside(x, y, z):
            rows.append(r)
rows.sort(key=lambda r: (int(r["y"]), int(r["z"]), int(r["x"])))
with open(os.path.join(HERE, "pt_signs.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=["x", "y", "z", "text"])
    w.writeheader()
    for r in rows:
        w.writerow({k: r[k] for k in w.fieldnames})
print("signs", len(rows))
