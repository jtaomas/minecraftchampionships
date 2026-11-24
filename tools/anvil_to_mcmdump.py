#!/usr/bin/env python3
"""Convert a Java Anvil region file (.mca) into an MCMDUMP gzip for tools/render_preview.py.

Supports the 1.18+ chunk format (`sections[].block_states.palette/data`).
The map is read only; nothing is written back to the world.

Usage:
  python tools/anvil_to_mcmdump.py <region.mca> <out.mcmdump.gz> [--min-y 80] [--max-y 320] [--top]

--top keeps only the highest block per column, which is the right mode for a fast
top-down map of a large elytra course. Without --top every non-air block in the
Y range is written (smaller regions only).
"""
import gzip
import io
import struct
import sys
import zlib

import nbtlib
import numpy as np

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def state_string(palette_entry):
    name = str(palette_entry.get("Name", "minecraft:air"))
    props = palette_entry.get("Properties")
    if not props:
        return name
    parts = ",".join(f"{k}={props[k]}" for k in sorted(props.keys()))
    return f"{name}[{parts}]"


def decode_section(section, min_y, max_y):
    y_byte = int(section.get("Y", 0))
    base_y = y_byte * 16
    bs = section.get("block_states")
    if not bs:
        return []
    palette = bs.get("palette")
    if not palette:
        return []
    n = len(palette)
    states = [state_string(p) for p in palette]
    if n == 1:
        state = states[0]
        if state in AIR:
            return []
        return [(i, base_y + (i >> 8), state) for i in range(4096)
                if min_y <= base_y + (i >> 8) <= max_y]
    data = bs.get("data")
    if data is None:
        return []
    bits = max(4, (n - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = np.asarray(data, dtype=np.uint64)
    shifts = (np.arange(per_long, dtype=np.uint64) * np.uint64(bits))
    indices = ((longs[:, None] >> shifts) & np.uint64(mask)).reshape(-1)[:4096]
    out = []
    for i, idx in enumerate(indices):
        idx = int(idx)
        if idx >= n:
            idx = 0
        state = states[idx]
        if state in AIR:
            continue
        y = base_y + (i >> 8)
        if y < min_y or y > max_y:
            continue
        out.append((i, y, state))
    return out


def read_region(path):
    with open(path, "rb") as f:
        header = f.read(4096)
    chunks = []
    for i in range(1024):
        off = int.from_bytes(header[i * 4:i * 4 + 3], "big")
        if off == 0:
            continue
        with open(path, "rb") as f:
            f.seek(off * 4096)
            ln = struct.unpack(">I", f.read(4))[0]
            comp = f.read(1)[0]
            raw = f.read(ln - 1)
        if comp == 2:
            raw = zlib.decompress(raw)
        elif comp == 1:
            raw = gzip.decompress(raw)
        try:
            root = nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue
        sections = root.get("sections")
        if sections is None:
            level = root.get("Level")
            sections = level.get("Sections") if level is not None else None
        if not sections:
            continue
        x = int(root.get("xPos", 0))
        z = int(root.get("zPos", 0))
        chunks.append((x, z, sections))
    return chunks


def main():
    args = sys.argv[1:]
    if len(args) < 2:
        print(__doc__)
        return
    path, out = args[0], args[1]
    min_y = 80
    max_y = 320
    top = False
    x1 = z1 = x2 = z2 = None
    i = 2
    while i < len(args):
        if args[i] == "--min-y":
            min_y = int(args[i + 1]); i += 2
        elif args[i] == "--max-y":
            max_y = int(args[i + 1]); i += 2
        elif args[i] == "--top":
            top = True; i += 1
        elif args[i] == "--x1":
            x1 = int(args[i + 1]); i += 2
        elif args[i] == "--x2":
            x2 = int(args[i + 1]); i += 2
        elif args[i] == "--z1":
            z1 = int(args[i + 1]); i += 2
        elif args[i] == "--z2":
            z2 = int(args[i + 1]); i += 2
        else:
            i += 1

    blocks = {}
    for cx, cz, sections in read_region(path):
        for section in sections:
            y_byte = int(section.get("Y", 0))
            base_y = y_byte * 16
            if base_y + 15 < min_y or base_y > max_y:
                continue
            decoded = decode_section(section, min_y, max_y)
            if not decoded:
                continue
            for idx, y, state in decoded:
                x = cx * 16 + (idx & 15)
                z = cz * 16 + ((idx >> 4) & 15)
                if x1 is not None and (x < x1 or x > x2): continue
                if z1 is not None and (z < z1 or z > z2): continue
                key = (x, z)
                if top:
                    old = blocks.get(key)
                    if old is None or y > old[0]:
                        blocks[key] = (y, state)
                else:
                    blocks[(x, y, z)] = state

    if not blocks:
        print("no blocks found")
        return
    if top:
        out_blocks = [(x, y, z, state) for (x, z), (y, state) in blocks.items()]
    else:
        out_blocks = [(x, y, z, state) for (x, y, z), state in blocks.items()]
    minx = min(b[0] for b in out_blocks); maxx = max(b[0] for b in out_blocks)
    miny = min(b[1] for b in out_blocks); maxy = max(b[1] for b in out_blocks)
    minz = min(b[2] for b in out_blocks); maxz = max(b[2] for b in out_blocks)
    sx, sy, sz = maxx - minx + 1, maxy - miny + 1, maxz - minz + 1
    with gzip.open(out, "wt", encoding="utf-8") as f:
        f.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x, y, z, state in out_blocks:
            f.write(f"{x - minx} {y - miny} {z - minz} {state}\n")
    print(f"wrote {len(out_blocks)} blocks to {out} ({sx}x{sy}x{sz})")


if __name__ == "__main__":
    main()
