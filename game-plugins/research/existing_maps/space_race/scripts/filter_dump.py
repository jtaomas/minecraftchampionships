#!/usr/bin/env python3
"""Build a small mcmdump from blocks_full.csv with selected block types removed."""
import argparse
import gzip
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--min", nargs=3, type=int, required=True)
    ap.add_argument("--max", nargs=3, type=int, required=True)
    ap.add_argument("--drop", nargs="*", default=[])
    args = ap.parse_args()
    lo, hi = args.min, args.max
    drop = set(args.drop)
    rows = []
    with gzip.open(P, "rt", encoding="utf-8") as fh:
        next(fh)
        for line in fh:
            x, y, z, b = line.rstrip("\n").split(",", 3)
            x, y, z = int(x), int(y), int(z)
            if not (lo[0] <= x <= hi[0] and lo[1] <= y <= hi[1] and lo[2] <= z <= hi[2]):
                continue
            if b.split("[")[0] in drop:
                continue
            rows.append((x, y, z, b))
    bx0 = min(r[0] for r in rows); by0 = min(r[1] for r in rows); bz0 = min(r[2] for r in rows)
    bx1 = max(r[0] for r in rows); by1 = max(r[1] for r in rows); bz1 = max(r[2] for r in rows)
    with gzip.open(args.out, "wt", encoding="utf-8") as out:
        out.write(f"MCMDUMP 1 {bx1-bx0+1} {by1-by0+1} {bz1-bz0+1}\n")
        for x, y, z, b in rows:
            out.write(f"{x-bx0} {y-by0} {z-bz0} {b}\n")
    print("wrote", args.out, len(rows), "bounds", bx0, by0, bz0, bx1, by1, bz1)


if __name__ == "__main__":
    main()
