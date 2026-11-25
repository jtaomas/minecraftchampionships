#!/usr/bin/env python3
"""Shape analysis of the Pits MCMDUMP: level histograms, column heights, slices."""
import collections
import csv
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")


def load():
    blocks = []
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        hdr = f.readline().split()
        assert hdr[0] == "MCMDUMP"
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            blocks.append((int(x), int(y), int(z), b))
    return blocks


def main():
    blocks = load()
    xs = [b[0] for b in blocks]; ys = [b[1] for b in blocks]; zs = [b[2] for b in blocks]
    print("bounds", min(xs), min(ys), min(zs), "->", max(xs), max(ys), max(zs), "blocks", len(blocks))

    per_y = collections.Counter(b[1] for b in blocks)
    per_y_mat = collections.defaultdict(collections.Counter)
    for x, y, z, b in blocks:
        per_y_mat[y][b.split("[")[0].replace("minecraft:", "")] += 1

    with open(os.path.join(HERE, "levels.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["y", "blocks", "top_materials"])
        for y in sorted(per_y, reverse=True):
            mats = "; ".join(f"{k}={v}" for k, v in per_y_mat[y].most_common(6))
            w.writerow([y, per_y[y], mats])
            print(f"y={y:4d} {per_y[y]:6d}  {mats}")

    # per-level footprint
    print("\n-- level footprints --")
    for y in sorted(per_y, reverse=True):
        pts = [(x, z) for x, yy, z, b in blocks if yy == y]
        if not pts:
            continue
        x0 = min(p[0] for p in pts); x1 = max(p[0] for p in pts)
        z0 = min(p[1] for p in pts); z1 = max(p[1] for p in pts)
        print(f"y={y:4d} n={len(pts):6d} x {x0:4d}..{x1:4d} z {z0:4d}..{z1:4d}  area={len(pts)}")

    # highest block per (x,z)
    cols = {}
    for x, y, z, b in blocks:
        k = (x, z)
        if k not in cols or y > cols[k][0]:
            cols[k] = (y, b)
    print("\nheightmap: columns", len(cols))
    hh = collections.Counter(v[0] for v in cols.values())
    print("top heights", hh.most_common(15))
    hm = collections.Counter(v[1].split("[")[0].replace("minecraft:", "") for v in cols.values())
    print("top column materials", hm.most_common(15))

    with open(os.path.join(HERE, "columns.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["x", "z", "top_y", "top_block"])
        for (x, z), (y, b) in sorted(cols.items()):
            w.writerow([x, z, y, b])

    # ascii heightmap, 2x2 downsample, only over occupied columns
    x0 = min(c[0] for c in cols); x1 = max(c[0] for c in cols)
    z0 = min(c[1] for c in cols); z1 = max(c[1] for c in cols)
    chars = " .:-=+*#%@"
    print("\n-- height map (north up, 2x2) --")
    for z in range(z0, z1 + 1, 2):
        row = []
        for x in range(x0, x1 + 1, 2):
            vals = [cols[(x + dx, z + dz)][0] for dx in (0, 1) for dz in (0, 1) if (x + dx, z + dz) in cols]
            if not vals:
                row.append(" ")
            else:
                h = max(vals)
                idx = max(0, min(len(chars) - 1, int((h + 64) / 94 * (len(chars) - 1))))
                row.append(chars[idx])
        print(f"{z:4d} " + "".join(row))


if __name__ == "__main__":
    main()
