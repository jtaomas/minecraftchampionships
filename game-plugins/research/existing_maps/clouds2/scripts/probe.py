#!/usr/bin/env python3
"""Probe the dump: vertical profiles at given x,z, or a box summary."""
import gzip, sys
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)

cols = {}
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        cols.setdefault((x, z), []).append((y, p[3].strip()))

def show(x, z):
    print(f"\n--- column x={x} z={z} ---")
    for y, n in sorted(cols.get((x, z), [])):
        print(f"  y={y:5d} {n}")

for arg in sys.argv[1:]:
    x, z = map(int, arg.split(","))
    show(x, z)
