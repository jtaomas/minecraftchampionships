#!/usr/bin/env python3
"""Turn the raw stadium extract (tools/stadium_extract.py output) into the plugin resource, with all command-block
machinery stripped. Only reads the JSON extract, never the world.

Usage: python -I stadium_build.py raw.json.gz ../src/main/resources/maps/stadium.mcmdump.gz

Local coordinates: lx = x - 1035, ly = y - 9 (the arena floor y10 becomes ly 1), lz = z - 972; size 44 x 50 x 46.
Prints what was stripped. The start rooms (x 1057..1064, y 15..21, z 979..984 / 1005..1010) are kept here and
rebuilt as cages by StadiumMap.
"""
import collections, gzip, json, sys

OX, OY, OZ = 1035, 9, 972
SX, SY, SZ = 44, 50, 46


def in_arena(x, y, z):
    return 1050 <= x <= 1071 and 10 <= y <= 26 and 984 <= z <= 1005


def in_room(x, y, z):
    return 1057 <= x <= 1064 and 15 <= y <= 21 and (979 <= z <= 984 or 1005 <= z <= 1010)


DROP = {"command_block", "chain_command_block", "repeating_command_block", "redstone_wire", "repeater", "comparator",
        "observer", "redstone_block", "lever", "birch_button", "stone_button", "stone_pressure_plate",
        "polished_blackstone_pressure_plate", "birch_wall_sign", "sticky_piston", "soul_lantern", "glowstone",
        "sea_lantern"}


def main():
    d = json.load(gzip.open(sys.argv[1], "rt"))
    stats = collections.Counter()
    out = []
    for x, y, z, n in d["blocks"]:
        base = n.split("[")[0].replace("minecraft:", "")
        if y < 10:
            stats["basement y<10: " + base] += 1
            continue
        if y <= 26 and not in_arena(x, y, z) and not in_room(x, y, z):
            stats["machine outside the arena y10..26: " + base] += 1
            continue
        if base in DROP:
            if base == "sticky_piston" and in_arena(x, y, z):
                pass
            else:
                stats["stripped " + base] += 1
                continue
        if base in ("piston_head", "piston", "sticky_piston"):
            stats["piston -> stone_bricks (wall/floor kept solid): " + base] += 1
            n = "minecraft:stone_bricks"
        elif base == "infested_stone_bricks":
            stats["infested_stone_bricks -> stone_bricks"] += 1
            n = "minecraft:stone_bricks"
        elif base == "barrel":
            stats["barrel -> spruce_planks"] += 1
            n = "minecraft:spruce_planks"
        elif base in ("chiseled_stone_bricks", "chiseled_polished_blackstone") and y == 27:
            stats["role pad -> smooth_quartz"] += 1
            n = "minecraft:smooth_quartz"
        elif base == "lectern":
            n = n.replace("has_book=true", "has_book=false")
        out.append((x - OX, y - OY, z - OZ, n))
    with gzip.open(sys.argv[2], "wt", encoding="utf-8", newline="\n", compresslevel=9) as f:
        f.write("MCMDUMP 1 %d %d %d\n" % (SX, SY, SZ))
        for x, y, z, n in out:
            assert 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ, (x, y, z)
            f.write("%d %d %d %s\n" % (x, y, z, n))
    tot = collections.Counter()
    for k, v in sorted(stats.items()):
        print("%5d %s" % (v, k))
        tot[k.split(":")[0].split(" ")[0]] += v
    print("kept", len(out), "of", len(d["blocks"]), "(%d dropped)" % (len(d["blocks"]) - len(out)))


if __name__ == "__main__":
    main()
