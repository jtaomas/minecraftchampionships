#!/usr/bin/env python3
"""ASCII horizontal slices of the Pits dump at given world Y levels."""
import collections
import gzip
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(HERE, "_test_start.mcmdump.gz")
OX, OY, OZ = -3, -64, -9  # dump origin in world coords


def load():
    out = collections.defaultdict(dict)
    with gzip.open(DUMP, "rt", encoding="utf-8") as f:
        f.readline()
        for line in f:
            x, y, z, b = line.rstrip("\n").split(" ", 3)
            out[int(y)][(int(x), int(z))] = b
    return out


def ch(b):
    if "air" in b:
        return " "
    m = b.split("[")[0].replace("minecraft:", "")
    if m == "black_stained_glass": return "G"
    if m == "light_blue_wool": return "W"
    if m == "black_carpet": return "c"
    if m == "shroomlight": return "L"
    if m == "cobblestone": return "C"
    if m == "bricks": return "B"
    if m == "gray_terracotta": return "t"
    if m == "brown_terracotta": return "T"
    if m == "quartz_block" or m.startswith("quartz"): return "Q"
    if m == "blackstone": return "K"
    if m == "light_gray_wool": return "w"
    if m == "gray_wool": return "g"
    if m == "brown_wool": return "b"
    if m == "green_concrete": return "E"
    if m == "green_wool": return "e"
    if m == "light_gray_terracotta": return "l"
    if m == "end_stone_bricks": return "N"
    if m == "red_concrete": return "R"
    if m == "white_concrete": return "O"
    if m == "stone_bricks": return "S"
    if m == "polished_diorite": return "D"
    if m == "yellow_terracotta": return "Y"
    if m == "black_concrete": return "X"
    if m == "barrier": return "#"
    if m == "snow_block": return "s"
    if m == "red_wool": return "r"
    if m == "green_wool": return "e"
    if m == "spruce_planks": return "p"
    if m == "red_sandstone" or m.startswith("sandstone"): return "d"
    if m == "glowstone": return "L"
    if m == "netherrack": return "n"
    if m == "light_blue_concrete": return "A"
    if m == "black_wool": return "a"
    if m == "white_wool": return "o"
    if m == "light_gray_concrete": return "i"
    return "?"


def main():
    levels = load()
    steps = 2
    raw = sys.argv[1:]
    if raw and raw[0] == "--full":
        steps = 1
        raw = raw[1:]
    if raw and raw[0] == "--box":
        # --box x0 x1 z0 z1  (world), then levels... or a single level list
        x0, x1, z0, z1 = map(int, raw[1:5])
        args = [int(a) for a in raw[5:]] or [-4]
        for wy in args:
            pts = levels.get(wy - OY, {})
            print(f"\n===== full map world y={wy}, x {x0}..{x1}, z {z0}..{z1} =====")
            for z in range(z0, z1 + 1):
                row = []
                for x in range(x0, x1 + 1):
                    b = pts.get((x - OX, z - OZ))
                    row.append(ch(b) if b else " ")
                print(f"z={z:4d} " + "".join(row))
        return
    args = [int(a) for a in raw] or [-64, -62, -60, -50, -44, -40, -30, -20, -10, -2, 0, 10, 20, 29]
    for wy in args:
        dy = wy - OY
        print(f"\n===== world y={wy} (dump y={dy}) =====")
        pts = levels.get(dy, {})
        if not pts:
            print("(empty)")
            continue
        x0 = min(p[0] for p in pts); x1 = max(p[0] for p in pts)
        z0 = min(p[1] for p in pts); z1 = max(p[1] for p in pts)
        print(f"x {x0 + OX}..{x1 + OX}  z {z0 + OZ}..{z1 + OZ}  blocks {len(pts)}")
        for z in range(z0, z1 + 1, steps):
            row = []
            for x in range(x0, x1 + 1, steps):
                counts = collections.Counter()
                for dx in range(steps):
                    for dz in range(steps):
                        b = pts.get((x + dx, z + dz))
                        if b:
                            counts[ch(b)] += 1
                row.append(counts.most_common(1)[0][0] if counts else " ")
            print(f"z={z + OZ:4d} " + "".join(row))


if __name__ == "__main__":
    main()
