#!/usr/bin/env python3
"""Fast MCMDUMP writer for render_preview.py (read-only source world).

Same MCMDUMP 1 format as tools/world_to_mcmdump.py, but unpacks section
palettes with numpy so a 4.5k-chunk world finishes in seconds.
"""
import argparse
import gzip
import io
import os
import zlib

import numpy as np
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


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def unpack_section(section):
    """Yield (index, block_state) for every non-air block in the section."""
    bs = section.get("block_states")
    if not bs:
        return
    palette = bs.get("palette", [])
    if not palette:
        return
    names = [state_string(p) for p in palette]
    if len(palette) == 1:
        if names[0] not in AIR:
            for i in range(4096):
                yield i, names[0]
        return
    data = bs.get("data")
    if data is None:
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype="<u8")
    bitmat = np.unpackbits(longs.view(np.uint8), bitorder="little").reshape(len(longs), 64)
    usable = per_long * bits
    bitmat = bitmat[:, :usable].reshape(len(longs) * per_long, bits)
    weights = (1 << np.arange(bits, dtype=np.uint64))
    vals = (bitmat.astype(np.uint64) * weights).sum(axis=1).astype(np.int64)
    vals = vals[:4096].reshape(16, 16, 16)  # y,z,x
    for pi, name in enumerate(names):
        if name in AIR:
            continue
        for y, z, x in np.argwhere(vals == pi):
            yield int(y) * 256 + int(z) * 16 + int(x), name


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("world")
    ap.add_argument("output")
    ap.add_argument("--min", nargs=3, type=int, metavar=("X", "Y", "Z"))
    ap.add_argument("--max", nargs=3, type=int, metavar=("X", "Y", "Z"))
    ap.add_argument("--scale", type=int, default=1)
    ap.add_argument("--include", action="append", default=None,
                    help="only include block ids containing this substring (repeatable)")
    args = ap.parse_args()

    world = os.path.abspath(args.world)
    region_dir = os.path.join(world, "region")
    lo = args.min or [-30000000, -64, -30000000]
    hi = args.max or [30000000, 320, 30000000]
    scale = max(1, args.scale)
    blocks = []
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        with open(os.path.join(region_dir, fname), "rb") as fh:
            region_data = fh.read()
        for idx in range(1024):
            chunk = read_chunk(region_data, idx)
            if not chunk:
                continue
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            if cx * 16 + 15 < lo[0] or cx * 16 > hi[0] or cz * 16 + 15 < lo[2] or cz * 16 > hi[2]:
                continue
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                if sy * 16 + 15 < lo[1] or sy * 16 > hi[1]:
                    continue
                for i, block in unpack_section(section):
                    bx = cx * 16 + (i & 15)
                    by = sy * 16 + (i >> 8)
                    bz = cz * 16 + ((i >> 4) & 15)
                    if bx < lo[0] or bx > hi[0] or by < lo[1] or by > hi[1] or bz < lo[2] or bz > hi[2]:
                        continue
                    if args.include and not any(s in block for s in args.include):
                        continue
                    blocks.append((bx, by, bz, block))
        print(f"  {fname}: {len(blocks)} blocks", flush=True)

    if not blocks:
        raise SystemExit("no blocks found in bounds")
    bx0 = min(b[0] for b in blocks)
    by0 = min(b[1] for b in blocks)
    bz0 = min(b[2] for b in blocks)
    bx1 = max(b[0] for b in blocks)
    by1 = max(b[1] for b in blocks)
    bz1 = max(b[2] for b in blocks)

    if scale > 1:
        seen = {}
        for x, y, z, b in blocks:
            key = (x // scale, y // scale, z // scale)
            seen.setdefault(key, b)
        blocks = [(x * scale, y * scale, z * scale, b) for (x, y, z), b in seen.items()]

    sx = (bx1 - bx0) // scale + 1
    sy = (by1 - by0) // scale + 1
    sz = (bz1 - bz0) // scale + 1
    with gzip.open(args.output, "wt", encoding="utf-8", compresslevel=6) as out:
        out.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x, y, z, b in blocks:
            out.write(f"{(x-bx0)//scale} {(y-by0)//scale} {(z-bz0)//scale} {b}\n")
    print(f"wrote {args.output}: {len(blocks)} blocks, bounds {bx0},{by0},{bz0} -> {bx1},{by1},{bz1}, size {sx}x{sy}x{sz}")


if __name__ == "__main__":
    main()
