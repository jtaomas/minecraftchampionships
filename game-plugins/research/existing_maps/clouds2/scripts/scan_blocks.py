#!/usr/bin/env python3
"""Pass B: block type counts using numpy vectorised palette decode."""
import gzip
import io
import json
import os
import sys
import zlib
from collections import Counter

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_region(path):
    with open(path, "rb") as fh:
        data = fh.read()
    for idx in range(1024):
        entry = data[idx * 4: idx * 4 + 4]
        if len(entry) < 4:
            continue
        off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
        if not off:
            continue
        pos = off * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        raw = data[pos + 5: pos + 4 + length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            elif comp != 3:
                continue
            yield idx, nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue


def state_name(state):
    return str(state.get("Name", "minecraft:air"))


def main():
    region_dir = os.path.join(WORLD, "region")
    blocks = Counter()
    sections_total = 0
    sections_nonair = 0
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        for idx, chunk in read_region(os.path.join(region_dir, fname)):
            for section in chunk.get("sections", []):
                bs = section.get("block_states")
                if not bs:
                    continue
                sections_total += 1
                pal = bs.get("palette", [])
                if not pal:
                    continue
                names = [state_name(p) for p in pal]
                data = bs.get("data")
                if data is None:
                    n = names[0]
                    if n not in AIR:
                        blocks[n] += 4096
                        sections_nonair += 1
                    continue
                if all(n in AIR for n in names):
                    continue
                sections_nonair += 1
                bits = max(4, (len(pal) - 1).bit_length())
                per_long = 64 // bits
                mask = (1 << bits) - 1
                longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype=np.uint64)
                vals = []
                for shift_i in range(per_long):
                    vals.append(((longs >> np.uint64(shift_i * bits)) & np.uint64(mask)).astype(np.int32))
                flat = np.stack(vals, axis=1).reshape(-1)[:4096]
                counts = np.bincount(flat, minlength=len(pal))
                for i, c in enumerate(counts):
                    if c and names[i] not in AIR:
                        blocks[names[i]] += int(c)
        print(f"{fname} done", file=sys.stderr)

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "block_counts.json"), "w", encoding="utf-8") as fh:
        json.dump(blocks.most_common(), fh, indent=1)
    print(json.dumps({"sections_total": sections_total, "sections_nonair": sections_nonair,
                      "distinct": len(blocks), "total": sum(blocks.values())}, indent=1))


if __name__ == "__main__":
    main()
