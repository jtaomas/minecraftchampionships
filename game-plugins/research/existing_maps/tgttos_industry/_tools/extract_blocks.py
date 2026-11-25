#!/usr/bin/env python3
"""Dump every non-air block of the Industry world to CSV (absolute coords)."""
import csv
import io
import os
import zlib

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data\blocks_xyz.csv"
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
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
            raw = zlib.decompress(raw, 16 + 15)
        elif comp != 3:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def section_pairs(section):
    bs = section.get("block_states")
    if not bs:
        return []
    palette = bs.get("palette", [])
    if not palette:
        return []
    names = [state_string(p) for p in palette]
    if len(palette) == 1:
        return [(i, names[0]) for i in range(4096)] if names[0] not in AIR else []
    data = bs.get("data")
    if data is None:
        return []
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype="<u8")
    bitmat = np.unpackbits(longs.view(np.uint8), bitorder="little").reshape(len(longs), 64)
    usable = per_long * bits
    bitmat = bitmat[:, :usable].reshape(len(longs) * per_long, bits)
    weights = (1 << np.arange(bits, dtype=np.uint64))
    vals = (bitmat.astype(np.uint64) * weights).sum(axis=1).astype(np.int64)[:4096]
    out = []
    for pi, name in enumerate(names):
        if name in AIR:
            continue
        for i in np.flatnonzero(vals == pi):
            out.append((int(i), name))
    return out


def main():
    rows = []
    for fname in sorted(os.listdir(os.path.join(WORLD, "region"))):
        if not fname.endswith(".mca"):
            continue
        with open(os.path.join(WORLD, "region", fname), "rb") as fh:
            data = fh.read()
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            cx, cz = int(chunk.get("xPos", 0)), int(chunk.get("zPos", 0))
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                for i, block in section_pairs(section):
                    rows.append((cx * 16 + (i & 15), sy * 16 + (i >> 8),
                                 cz * 16 + ((i >> 4) & 15), block))
        print(fname, len(rows), flush=True)
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "block"])
        for r in sorted(rows, key=lambda r: (r[1], r[2], r[0])):
            w.writerow(r)
    print("wrote", OUT, len(rows))


if __name__ == "__main__":
    main()
