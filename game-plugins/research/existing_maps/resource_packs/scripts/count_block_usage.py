#!/usr/bin/env python3
"""Count usage of resource-pack-overridden blocks per staged map.

Decodes only sections whose palette contains a target block. Also reports
any target block that appears in a palette but is never actually placed.
"""
import collections
import gzip
import io
import json
import os
import zlib

import nbtlib

MAPS = {
    "clouds2": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo",
    "city_marathon": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon",
    "space_race": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race",
    "yeti_set_go": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\YetiSetGo",
    "decision_dome": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation",
    "minigames": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3",
    "tgttos_badlands": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat",
    "tgttos_industry": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat",
    "tgttos_pits": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat",
}
TARGETS = {
    "minecraft:magenta_glazed_terracotta",
    "minecraft:pink_glazed_terracotta",
    "minecraft:purple_glazed_terracotta",
    "minecraft:red_glazed_terracotta",
    "minecraft:yellow_glazed_terracotta",
    "minecraft:green_glazed_terracotta",
    "minecraft:black_terracotta", "minecraft:blue_terracotta", "minecraft:brown_terracotta",
    "minecraft:cyan_terracotta", "minecraft:gray_terracotta", "minecraft:green_terracotta",
    "minecraft:light_blue_terracotta", "minecraft:light_gray_terracotta", "minecraft:lime_terracotta",
    "minecraft:magenta_terracotta", "minecraft:orange_terracotta", "minecraft:pink_terracotta",
    "minecraft:purple_terracotta", "minecraft:red_terracotta", "minecraft:white_terracotta",
    "minecraft:yellow_terracotta",
    "minecraft:diorite", "minecraft:glowstone", "minecraft:oak_planks", "minecraft:red_nether_bricks",
    "minecraft:red_stained_glass", "minecraft:sea_lantern", "minecraft:soul_sand",
    "minecraft:acacia_planks", "minecraft:birch_planks", "minecraft:birch_trapdoor", "minecraft:black_wool",
    "minecraft:blackstone", "minecraft:crimson_planks", "minecraft:dark_oak_planks", "minecraft:diamond_block",
    "minecraft:end_stone", "minecraft:glass", "minecraft:gray_wool", "minecraft:iron_bars",
    "minecraft:jungle_planks", "minecraft:light_blue_stained_glass", "minecraft:light_gray_wool",
    "minecraft:orange_wool", "minecraft:red_concrete", "minecraft:smooth_stone",
    "minecraft:stone_bricks", "minecraft:white_stained_glass", "minecraft:yellow_wool",
}
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data"


def base(state):
    return str(state.get("Name", ""))


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
    if len(entry) < 4:
        return None
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
    if not off:
        return None
    pos = off * 4096
    if pos + 5 > len(data):
        return None
    length = int.from_bytes(data[pos:pos + 4], "big")
    comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + length]
    try:
        if comp == 2:
            raw = zlib.decompress(raw)
        elif comp == 1:
            raw = gzip.decompress(raw)
        elif comp != 3:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def unpack_counts(section):
    bs = section.get("block_states")
    if not bs:
        return None
    palette = bs.get("palette", [])
    if not palette:
        return None
    counts = collections.Counter()
    data = bs.get("data")
    if data is None:
        counts[base(palette[0])] = 4096
        return counts
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            break
        shift = (i % per_long) * bits
        val = (longs[li] >> shift) & mask
        if val < len(palette):
            counts[base(palette[val])] += 1
    return counts


def main():
    summary = []
    for slug, root in MAPS.items():
        total = collections.Counter()
        palette_but_zero = set()
        chunks = 0
        for sub in ("region", "DIM-1/region", "DIM1/region"):
            rd = os.path.join(root, sub)
            if not os.path.isdir(rd):
                continue
            for fn in sorted(os.listdir(rd)):
                if not fn.endswith(".mca"):
                    continue
                data = open(os.path.join(rd, fn), "rb").read()
                for idx in range(1024):
                    chunk = read_chunk(data, idx)
                    if not chunk:
                        continue
                    chunks += 1
                    for section in chunk.get("sections", []):
                        bs = section.get("block_states")
                        if not bs:
                            continue
                        pal = {base(p) for p in bs.get("palette", [])}
                        hit = pal & TARGETS
                        if not hit:
                            continue
                        c = unpack_counts(section)
                        if not c:
                            continue
                        for k, v in c.items():
                            if k in TARGETS:
                                total[k] += v
                        for k in hit:
                            if c.get(k, 0) == 0:
                                palette_but_zero.add(k)
        for k in sorted(total):
            summary.append({"map": slug, "block": k, "count": total[k]})
        for k in sorted(palette_but_zero - set(total)):
            summary.append({"map": slug, "block": k, "count": 0})
        print(slug, "chunks", chunks, "target blocks", sum(total.values()))
    import csv
    with open(os.path.join(OUT, "block_usage.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "block", "count"])
        w.writeheader()
        w.writerows(summary)


if __name__ == "__main__":
    main()
