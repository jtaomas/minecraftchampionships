#!/usr/bin/env python3
"""NumPy block counter for the Space Race world.

Decodes each section's packed palette indices with vectorised bit ops and
accumulates per-block counts. Far faster than a per-block Python loop.
"""
import io
import json
import os
import sys
import zlib
import gzip as gz

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data_raw")
os.makedirs(OUT, exist_ok=True)

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4: index * 4 + 4]
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
            raw = gz.decompress(raw)
        elif comp != 3:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def state_name(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def section_counts(section):
    bs = section.get("block_states")
    if not bs:
        return {}
    palette = bs.get("palette", [])
    if not palette:
        return {}
    names = [state_name(p) for p in palette]
    data = bs.get("data")
    if data is None:
        # entire section is the single palette entry
        return {names[0]: 4096}
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = np.uint64((1 << bits) - 1)
    longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype=np.uint64)
    idx = np.arange(4096, dtype=np.int64)
    li = idx // per_long
    shift = (idx % per_long) * bits
    if li.max() >= len(longs):
        keep = li < len(longs)
        idx, li, shift = idx[keep], li[keep], shift[keep]
    vals = ((longs[li] >> shift.astype(np.uint64)) & mask).astype(np.int64)
    vals = vals[vals < len(names)]
    uniq, cnt = np.unique(vals, return_counts=True)
    return {names[u]: int(c) for u, c in zip(uniq, cnt)}


def main():
    region_dir = os.path.join(WORLD, "region")
    counts = {}
    chunks = 0
    ymin, ymax = 999, -999
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    for fname in files:
        path = os.path.join(region_dir, fname)
        with open(path, "rb") as fh:
            data = fh.read()
        n0 = chunks
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            chunks += 1
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                ymin = min(ymin, sy * 16)
                ymax = max(ymax, sy * 16 + 15)
                for name, c in section_counts(section).items():
                    if name in AIR:
                        continue
                    counts[name] = counts.get(name, 0) + c
        print(f"{fname}: +{chunks-n0} chunks", flush=True)

    with open(os.path.join(OUT, "blocks.json"), "w", encoding="utf-8") as fh:
        json.dump(dict(sorted(counts.items(), key=lambda kv: -kv[1])), fh, indent=1)
    print("chunks", chunks, "distinct", len(counts), "total", sum(counts.values()),
          "y", ymin, ymax)


if __name__ == "__main__":
    main()
