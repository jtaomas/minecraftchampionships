#!/usr/bin/env python3
"""Fast MCMDUMP writer for sparse void maps (read-only).

The shared `tools/world_to_mcmdump.py` expands every air cell (4096 per
section) and is very slow on mostly-empty maps with 3357 chunks. This
writer keeps coordinates, skips air cheaply, and uses the same output
format so `tools/render_preview.py` can render it unchanged.

Usage: python tools/dump_blocks.py <world-dir> <out.mcmdump.gz>
"""
import gzip
import io
import os
import sys
import zlib

import nbtlib

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


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
    except Exception:  # noqa: BLE001
        return None


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"


def cells(section):
    bs = section.get("block_states")
    if not bs:
        return
    palette = bs.get("palette", [])
    if not palette:
        return
    data = bs.get("data")
    if data is None:
        name = state_string(palette[0])
        if name not in AIR:
            for i in range(4096):
                yield i, name
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
    names = [state_string(p) for p in palette]
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            break
        shift = (i % per_long) * bits
        val = (longs[li] >> shift) & mask
        if val >= len(names):
            continue
        name = names[val]
        if name in AIR:
            continue
        yield i, name


def main():
    world = os.path.abspath(sys.argv[1])
    out_path = sys.argv[2]
    region_dir = os.path.join(world, "region")
    lo = [-30000000, -64, -30000000]
    hi = [30000000, 320, 30000000]
    args = sys.argv[3:]
    if "--min" in args:
        i = args.index("--min")
        lo = [int(v) for v in args[i + 1:i + 4]]
    if "--max" in args:
        i = args.index("--max")
        hi = [int(v) for v in args[i + 1:i + 4]]
    blocks = []
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        data = open(os.path.join(region_dir, fname), "rb").read()
        n = 0
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            n += 1
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            if cx * 16 + 15 < lo[0] or cx * 16 > hi[0] or cz * 16 + 15 < lo[2] or cz * 16 > hi[2]:
                continue
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                if sy * 16 + 15 < lo[1] or sy * 16 > hi[1]:
                    continue
                for i, name in cells(section):
                    bx = cx * 16 + (i & 15)
                    by = sy * 16 + (i >> 8)
                    bz = cz * 16 + ((i >> 4) & 15)
                    blocks.append((bx, by, bz, name))
        print(f"  {fname}: {n} chunks, running total {len(blocks)}", file=sys.stderr, flush=True)

    bx0 = min(b[0] for b in blocks)
    by0 = min(b[1] for b in blocks)
    bz0 = min(b[2] for b in blocks)
    bx1 = max(b[0] for b in blocks)
    by1 = max(b[1] for b in blocks)
    bz1 = max(b[2] for b in blocks)
    with gzip.open(out_path, "wt", encoding="utf-8", compresslevel=6) as out:
        out.write(f"MCMDUMP 1 {bx1 - bx0 + 1} {by1 - by0 + 1} {bz1 - bz0 + 1}\n")
        for x, y, z, b in blocks:
            out.write(f"{x - bx0} {y - by0} {z - bz0} {b}\n")
    print(f"wrote {out_path}: {len(blocks)} blocks, bounds {bx0},{by0},{bz0} -> {bx1},{by1},{bz1}", file=sys.stderr)


if __name__ == "__main__":
    main()
