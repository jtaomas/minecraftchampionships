#!/usr/bin/env python3
"""Dump every non-air block with absolute coordinates: x y z id -> data_raw/blocks_full.csv.gz"""
import gzip
import io
import os
import zlib
import gzip as gz

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "data_raw", "blocks_full.csv.gz")
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


def main():
    region_dir = os.path.join(WORLD, "region")
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    n = 0
    with gzip.open(OUT, "wt", encoding="utf-8", compresslevel=1) as out:
        out.write("x,y,z,block\n")
        for fname in files:
            with open(os.path.join(region_dir, fname), "rb") as fh:
                data = fh.read()
            for idx in range(1024):
                chunk = read_chunk(data, idx)
                if not chunk:
                    continue
                cx = int(chunk.get("xPos", 0))
                cz = int(chunk.get("zPos", 0))
                for section in chunk.get("sections", []):
                    sy = int(section.get("Y", 0))
                    bs = section.get("block_states")
                    if not bs:
                        continue
                    palette = bs.get("palette", [])
                    if not palette:
                        continue
                    names = [state_name(p) for p in palette]
                    dat = bs.get("data")
                    if dat is None:
                        vals = np.zeros(4096, dtype=np.int64)
                    else:
                        bits = max(4, (len(palette) - 1).bit_length())
                        per_long = 64 // bits
                        mask = np.uint64((1 << bits) - 1)
                        longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in dat], dtype=np.uint64)
                        ii = np.arange(4096, dtype=np.int64)
                        li = ii // per_long
                        sh = (ii % per_long) * bits
                        if li.max() >= len(longs):
                            keep = li < len(longs)
                            ii, li, sh = ii[keep], li[keep], sh[keep]
                        vals = ((longs[li] >> sh.astype(np.uint64)) & mask).astype(np.int64)
                        vals = np.where(vals < len(names), vals, 0)
                    for i in range(len(vals)):
                        nm = names[vals[i]]
                        if nm in AIR:
                            continue
                        bx = cx * 16 + (i & 15)
                        by = sy * 16 + (i >> 8)
                        bz = cz * 16 + ((i >> 4) & 15)
                        out.write(f"{bx},{by},{bz},{nm}\n")
                        n += 1
            print(fname, n, flush=True)
    print("total", n)


if __name__ == "__main__":
    main()
