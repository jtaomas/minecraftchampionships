#!/usr/bin/env python3
"""Dump a 1.17.1 region subset to MCMDUMP (for tools/render_preview.py).

Usage:
  python dump_117_mcmdump.py <world> <out.mcmdump.gz> --min X Y Z --max X Y Z
"""
import argparse
import gzip
import io
import os
import zlib
import gzip as gz

import nbtlib

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
    if not off:
        return None
    pos = off * 4096
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


def unpack(palette, states):
    n = len(palette)
    if states is None or len(states) == 0:
        return [0] * 4096
    bits = max(4, (n - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in states]
    vals = []
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            vals.append(0)
            continue
        shift = (i % per_long) * bits
        vals.append((longs[li] >> shift) & mask)
    return vals


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("world")
    ap.add_argument("output")
    ap.add_argument("--min", nargs=3, type=int, required=True)
    ap.add_argument("--max", nargs=3, type=int, required=True)
    ap.add_argument("--margin", type=int, default=8)
    args = ap.parse_args()
    lo = list(args.min)
    hi = list(args.max)
    world = os.path.abspath(args.world)
    region_dir = os.path.join(world, "region")
    blocks = []
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    for fname in files:
        data = open(os.path.join(region_dir, fname), "rb").read()
        for idx in range(1024):
            ch = read_chunk(data, idx)
            if not ch:
                continue
            lv = ch.get("Level", ch)
            cx, cz = int(lv.get("xPos", 0)), int(lv.get("zPos", 0))
            if cx * 16 + 15 < lo[0] or cx * 16 > hi[0] or cz * 16 + 15 < lo[2] or cz * 16 > hi[2]:
                continue
            for sec in lv.get("Sections", []):
                pal = sec.get("Palette")
                if not pal:
                    continue
                names = [str(p.get("Name", "minecraft:air")) for p in pal]
                if all(n in AIR for n in names):
                    continue
                vals = unpack(pal, sec.get("BlockStates"))
                sy = int(sec.get("Y", 0))
                for i, v in enumerate(vals):
                    if v >= len(names):
                        continue
                    nm = names[v]
                    if nm in AIR:
                        continue
                    bx = cx * 16 + (i & 15)
                    by = sy * 16 + (i >> 8)
                    bz = cz * 16 + ((i >> 4) & 15)
                    if lo[0] <= bx <= hi[0] and lo[1] <= by <= hi[1] and lo[2] <= bz <= hi[2]:
                        blocks.append((bx, by, bz, nm))
    if not blocks:
        raise SystemExit("no blocks in bounds")
    bx0 = min(b[0] for b in blocks) - args.margin
    by0 = min(b[1] for b in blocks)
    bz0 = min(b[2] for b in blocks) - args.margin
    bx1 = max(b[0] for b in blocks) + args.margin
    by1 = max(b[1] for b in blocks)
    bz1 = max(b[2] for b in blocks) + args.margin
    sx, sy, sz = bx1 - bx0 + 1, by1 - by0 + 1, bz1 - bz0 + 1
    with gzip.open(args.output, "wt", encoding="utf-8", compresslevel=6) as out:
        out.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x, y, z, b in blocks:
            out.write(f"{x-bx0} {y-by0} {z-bz0} {b}\n")
    print(f"wrote {args.output}: {len(blocks)} blocks, real bounds {bx0+args.margin},{by0},{bz0+args.margin} -> "
          f"{bx1-args.margin},{by1},{bz1-args.margin}, size {sx}x{sy}x{sz}")


if __name__ == "__main__":
    main()
