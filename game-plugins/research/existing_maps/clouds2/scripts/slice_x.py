#!/usr/bin/env python3
"""Print a horizontal run of blocks: --y Y --z Z [--x0 .. --x1 ..] etc."""
import gzip, sys
SRC = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
O = (-258, -64, -316)
rows = []
with gzip.open(SRC, "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        rows.append((int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2], p[3].strip()))

def run_x(y, z, x0, x1):
    print(f"\n== y={y} z={z}  x {x0}..{x1} ==")
    cur = None; start = None
    for x in range(x0, x1 + 1):
        hits = [n for (bx, by, bz, n) in rows if bx == x and by == y and bz == z]
        n = hits[0] if hits else "-"
        if n != cur:
            if cur is not None:
                print(f"  x {start:5d}..{x-1:5d}  {cur}")
            cur = n; start = x
    print(f"  x {start:5d}..{x1:5d}  {cur}")

if __name__ == "__main__":
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--y", type=int, required=True)
    ap.add_argument("--z", type=int, required=True)
    ap.add_argument("--x0", type=int, required=True)
    ap.add_argument("--x1", type=int, required=True)
    a = ap.parse_args()
    run_x(a.y, a.z, a.x0, a.x1)
