#!/usr/bin/env python3
"""Parse the tgttos datapack and compare its reset lists with the world. Read-only."""
import os, re, gzip, json, collections
import nbtlib

ROOT = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat"
HERE = os.path.dirname(os.path.abspath(__file__))
LOOP = os.path.join(ROOT, "datapacks", "tgttos", "data", "tgttos", "functions", "loop.mcfunction")
LOAD = os.path.join(ROOT, "datapacks", "tgttos", "data", "tgttos", "functions", "load.mcfunction")


def read(p):
    with open(p, "r", encoding="utf-8") as f:
        return f.read().splitlines()


def parse_box(cmd):
    m = re.search(r"\bfill\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(\S+)", cmd)
    if not m:
        return None
    a = list(map(int, m.groups()[:6]))
    lo = [min(a[0], a[3]), min(a[1], a[4]), min(a[2], a[5])]
    hi = [max(a[0], a[3]), max(a[1], a[4]), max(a[2], a[5])]
    return lo, hi, m.group(7)


def main():
    lines = read(LOOP)
    print("loop lines:", len(lines))
    fills = []
    for i, ln in enumerate(lines, 1):
        b = parse_box(ln)
        if b:
            fills.append((i, ln, b))
    # barriers
    reset_barrier = set()
    for i, ln, (lo, hi, blk) in fills:
        if blk == "barrier":
            for x in range(lo[0], hi[0] + 1):
                for y in range(lo[1], hi[1] + 1):
                    for z in range(lo[2], hi[2] + 1):
                        reset_barrier.add((x, y, z))
    # cactus
    reset_cactus = set()
    cactus_cmds = []
    for i, ln, (lo, hi, blk) in fills:
        if blk == "cactus":
            cactus_cmds.append((i, ln))
            for x in range(lo[0], hi[0] + 1):
                for y in range(lo[1], hi[1] + 1):
                    for z in range(lo[2], hi[2] + 1):
                        reset_cactus.add((x, y, z))
    for i, ln in enumerate(lines, 1):
        m = re.search(r"\bsetblock\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+cactus", ln)
        if m:
            cactus_cmds.append((i, ln))
            reset_cactus.add(tuple(map(int, m.groups())))

    # world
    world_barrier = set(); world_cactus = set()
    with gzip.open(os.path.join(HERE, "world.mcmdump.gz"), "rt") as f:
        f.readline()
        for line in f:
            p = line.split(" ", 3)
            x = int(p[0]); y = int(p[1]) - 64; z = int(p[2]) - 16
            b = p[3].strip().split("[")[0].replace("minecraft:", "")
            if b == "barrier":
                world_barrier.add((x, y, z))
            elif b == "cactus":
                world_cactus.add((x, y, z))
    print("barrier world", len(world_barrier), "reset", len(reset_barrier))
    print("  reset not world", len(reset_barrier - world_barrier), sorted(reset_barrier - world_barrier)[:10])
    print("  world not reset", len(world_barrier - reset_barrier), sorted(world_barrier - reset_barrier)[:10])
    print("cactus world", len(world_cactus), "reset", len(reset_cactus), "commands", len(cactus_cmds))
    print("  reset not world", len(reset_cactus - world_cactus), sorted(reset_cactus - world_cactus)[:20])
    print("  world not reset", len(world_cactus - reset_cactus), sorted(world_cactus - reset_cactus)[:20])

    # can place on
    item_lines = [ln for ln in lines if "item replace entity @a weapon.offhand" in ln]
    print("offhand lines:", len(item_lines))
    if item_lines:
        m = re.search(r"CanPlaceOn:\[(.*?)\]", item_lines[0])
        can = re.findall(r'"minecraft:([a-z_]+)"', m.group(1))
        print("CanPlaceOn", len(can), can)
    # palette comparison
    pal = set()
    with gzip.open(os.path.join(HERE, "world.mcmdump.gz"), "rt") as f:
        f.readline()
        for line in f:
            b = line.split(" ", 3)[3].strip().split("[")[0].replace("minecraft:", "")
            pal.add(b)
    print("palette types", len(pal))
    print("not placeable-on:", sorted(p for p in pal if p not in can and p not in ("air", "cave_air", "void_air")))
    # line 3 fill start
    for i, ln in enumerate(lines, 1):
        if "air replace blue_concrete" in ln:
            print("blue clear line", i, ln)

    # save structured
    out = {
        "loop_lines": len(lines),
        "cactus_commands": [{"line": i, "cmd": c} for i, c in cactus_cmds],
        "cactus_reset_count": len(reset_cactus),
        "cactus_world_count": len(world_cactus),
        "cactus_missing_from_reset": sorted(world_cactus - reset_cactus),
        "cactus_extra_in_reset": sorted(reset_cactus - world_cactus),
        "barrier_world": len(world_barrier),
        "barrier_reset_union": len(reset_barrier),
        "can_place_on": can,
    }
    with open(os.path.join(HERE, "raw_datapack.json"), "w") as f:
        json.dump(out, f, indent=1)


if __name__ == "__main__":
    main()
