#!/usr/bin/env python3
"""NumPy MCMDUMP writer (same format as tools/world_to_mcmdump.py).

Usage: dump_mcmdump.py <out.gz> [--min x y z] [--max x y z] [--scale N]
Source world is fixed to the Space Race extraction (read-only).
"""
import argparse
import gzip
import io
import os
import zlib

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
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
            raw = gzip.decompress(raw)
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
    ap = argparse.ArgumentParser()
    ap.add_argument("output")
    ap.add_argument("--min", nargs=3, type=int, default=[-30000000, -64, -30000000])
    ap.add_argument("--max", nargs=3, type=int, default=[30000000, 320, 30000000])
    ap.add_argument("--scale", type=int, default=1)
    args = ap.parse_args()
    lo, hi, scale = args.min, args.max, max(1, args.scale)

    region_dir = os.path.join(WORLD, "region")
    blocks = []
    for fname in sorted(f for f in os.listdir(region_dir) if f.endswith(".mca")):
        with open(os.path.join(region_dir, fname), "rb") as fh:
            data = fh.read()
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            cx = int(chunk.get("xPos", 0)); cz = int(chunk.get("zPos", 0))
            if cx * 16 + 15 < lo[0] or cx * 16 > hi[0] or cz * 16 + 15 < lo[2] or cz * 16 > hi[2]:
                continue
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                if sy * 16 + 15 < lo[1] or sy * 16 > hi[1]:
                    continue
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
                for i in np.nonzero(vals > -1)[0]:
                    nm = names[vals[i]]
                    if nm in AIR:
                        continue
                    bx = cx * 16 + int(i & 15)
                    by = sy * 16 + int(i >> 8)
                    bz = cz * 16 + int((i >> 4) & 15)
                    if bx < lo[0] or bx > hi[0] or by < lo[1] or by > hi[1] or bz < lo[2] or bz > hi[2]:
                        continue
                    blocks.append((bx, by, bz, nm))
        print(fname, len(blocks), flush=True)

    if not blocks:
        raise SystemExit("no blocks")
    bx0 = min(b[0] for b in blocks); by0 = min(b[1] for b in blocks); bz0 = min(b[2] for b in blocks)
    bx1 = max(b[0] for b in blocks); by1 = max(b[1] for b in blocks); bz1 = max(b[2] for b in blocks)
    if scale > 1:
        seen = {}
        for x, y, z, b in blocks:
            seen.setdefault((x // scale, y // scale, z // scale), b)
        blocks = [(x * scale, y * scale, z * scale, b) for (x, y, z), b in seen.items()]
    sx = (bx1 - bx0) // scale + 1; sy = (by1 - by0) // scale + 1; sz = (bz1 - bz0) // scale + 1
    with gzip.open(args.output, "wt", encoding="utf-8", compresslevel=6) as out:
        out.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x, y, z, b in blocks:
            out.write(f"{(x-bx0)//scale} {(y-by0)//scale} {(z-bz0)//scale} {b}\n")
    print(f"wrote {args.output}: {len(blocks)} blocks, bounds {bx0},{by0},{bz0} -> {bx1},{by1},{bz1}, size {sx}x{sy}x{sz}")


if __name__ == "__main__":
    main()
