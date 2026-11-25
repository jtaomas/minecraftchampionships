#!/usr/bin/env python3
"""Top-surface map for the Pits course band, plus barrier/rare-block clusters."""
import collections
import gzip
import os

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9


def load():
    blocks = []
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        hdr = f.readline().split()
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            blocks.append((int(x) + OX, int(y) + OY, int(z) + OZ, b))
    return blocks


def short(b):
    return b.split("[")[0].replace("minecraft:", "")


def clusters(pts, gap=2):
    """Greedy axis-aligned bounding boxes for a point set."""
    pts = set(pts)
    out = []
    while pts:
        seed = pts.pop()
        box = [seed[0], seed[1], seed[2], seed[0], seed[1], seed[2]]
        changed = True
        while changed:
            changed = False
            for p in list(pts):
                if (box[0] - gap <= p[0] <= box[3] + gap and
                        box[1] - gap <= p[1] <= box[4] + gap and
                        box[2] - gap <= p[2] <= box[5] + gap):
                    box[0] = min(box[0], p[0]); box[3] = max(box[3], p[0])
                    box[1] = min(box[1], p[1]); box[4] = max(box[4], p[1])
                    box[2] = min(box[2], p[2]); box[5] = max(box[5], p[2])
                    pts.remove(p); changed = True
        out.append(tuple(box))
    return sorted(out, key=lambda b: (-(b[4] - b[1]), b[0]))


def main():
    blocks = load()
    print("world bounds:", min(b[0] for b in blocks), min(b[1] for b in blocks), min(b[2] for b in blocks),
          "->", max(b[0] for b in blocks), max(b[1] for b in blocks), max(b[2] for b in blocks))

    # walkable surface: highest block at or below y=5 per column, in the course band
    surf = {}
    for x, y, z, b in blocks:
        if not (-16 <= y <= 5):
            continue
        m = short(b)
        if m in ("barrier",):
            continue
        k = (x, z)
        if k not in surf or y > surf[k][0]:
            surf[k] = (y, m)
    print("surface columns:", len(surf))
    hs = collections.Counter(v[0] for v in surf.values())
    print("surface heights:", hs.most_common(20))
    ms = collections.Counter(v[1] for v in surf.values())
    print("surface materials:", ms.most_common(25))

    # where are the barrier blocks?
    barriers = [(x, y, z) for x, y, z, b in blocks if "barrier" in b]
    print("\nbarriers:", len(barriers))
    for c in clusters(barriers, 1):
        n = sum(1 for p in barriers if c[0] <= p[0] <= c[3] and c[1] <= p[1] <= c[4] and c[2] <= p[2] <= c[5])
        print("  barrier cluster", c, "blocks", n)

    for mat in ("red_concrete", "white_concrete", "green_concrete", "snow_block", "end_stone_bricks",
                "black_stained_glass", "shroomlight", "black_carpet", "light_blue_wool", "glowstone"):
        pts = [(x, y, z) for x, y, z, b in blocks if short(b) == mat]
        print(f"\n{mat}: {len(pts)}")
        for c in clusters(pts, 2)[:8]:
            n = sum(1 for p in pts if c[0] <= p[0] <= c[3] and c[1] <= p[1] <= c[4] and c[2] <= p[2] <= c[5])
            print("  ", c, "n", n)

    # ascii top surface (1 char per block), materials
    x0 = min(k[0] for k in surf); x1 = max(k[0] for k in surf)
    z0 = min(k[1] for k in surf); z1 = max(k[1] for k in surf)
    print(f"\nsurface x {x0}..{x1} z {z0}..{z1}")

    def ch(y, m):
        base = {"cobblestone": "C", "bricks": "B", "gray_terracotta": "t", "brown_terracotta": "T",
                "quartz_block": "Q", "blackstone": "K", "light_gray_wool": "w", "gray_wool": "g",
                "brown_wool": "b", "green_concrete": "E", "green_wool": "e", "light_gray_terracotta": "l",
                "end_stone_bricks": "N", "red_concrete": "R", "white_concrete": "O", "stone_bricks": "S",
                "polished_diorite": "D", "yellow_terracotta": "Y", "black_concrete": "X", "snow_block": "s",
                "red_wool": "r", "spruce_planks": "p", "red_sandstone": "d", "sandstone": "d",
                "glowstone": "L", "netherrack": "n", "light_blue_concrete": "A", "black_wool": "a",
                "white_wool": "o", "light_gray_concrete": "i", "black_stained_glass": "G",
                "light_blue_stained_glass": "H", "black_carpet": "c", "shroomlight": "L",
                "diorite": "D", "oak_wall_sign": "!"}.get(m, "?")
        return base

    for z in range(z0, z1 + 1):
        row = []
        for x in range(x0, x1 + 1):
            v = surf.get((x, z))
            row.append(ch(*v) if v else " ")
        print(f"z={z:4d} " + "".join(row))


if __name__ == "__main__":
    main()
