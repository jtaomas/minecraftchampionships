#!/usr/bin/env python3
"""Fast MCMDUMP writer (same format as tools/world_to_mcmdump.py).

Equivalent output, numpy-vectorised palette decode. Read-only on the world.
Writes gzipped MCMDUMP 1 <sx> <sy> <sz> plus one line per non-air block,
coordinates relative to the world minimum corner.
"""
import gzip
import io
import os
import sys
import zlib

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\clouds2_full.mcmdump.gz"
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


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def main():
    region_dir = os.path.join(WORLD, "region")
    blocks = []  # (x,y,z,name)
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        for idx, chunk in read_region(os.path.join(region_dir, fname)):
            cx = int(chunk.get("xPos", 0)); cz = int(chunk.get("zPos", 0))
            for section in chunk.get("sections", []):
                bs = section.get("block_states")
                if not bs:
                    continue
                pal = bs.get("palette", [])
                if not pal:
                    continue
                names = [state_string(p) for p in pal]
                data = bs.get("data")
                sy = int(section.get("Y", 0))
                if data is None:
                    if names[0] in AIR:
                        continue
                    for i in range(4096):
                        blocks.append((cx * 16 + (i & 15), sy * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15), names[0]))
                    continue
                if all(n.split("[")[0] in AIR for n in names):
                    continue
                bits = max(4, (len(pal) - 1).bit_length())
                per_long = 64 // bits
                mask = (1 << bits) - 1
                longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype=np.uint64)
                vals = [((longs >> np.uint64(k * bits)) & np.uint64(mask)).astype(np.int32) for k in range(per_long)]
                flat = np.stack(vals, axis=1).reshape(-1)[:4096]
                idxs, cts = np.unique(flat, return_counts=True)
                for pi, c in zip(idxs, cts):
                    if pi >= len(names):
                        continue
                    nm = names[pi]
                    if nm.split("[")[0] in AIR:
                        continue
                    blocks.append((cx * 16, sy * 16, cz * 16, nm, int(pi), int(c)))
        print(fname, len(blocks), file=sys.stderr)

    # second pass: expand the per-palette groups using the same packed decode,
    # recording coordinates. We re-decode here to avoid storing 150k+ group lists.
    out_lines = []
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        for idx, chunk in read_region(os.path.join(region_dir, fname)):
            cx = int(chunk.get("xPos", 0)); cz = int(chunk.get("zPos", 0))
            for section in chunk.get("sections", []):
                bs = section.get("block_states")
                if not bs:
                    continue
                pal = bs.get("palette", [])
                if not pal:
                    continue
                names = [state_string(p) for p in pal]
                data = bs.get("data")
                sy = int(section.get("Y", 0))
                if data is None:
                    if names[0].split("[")[0] in AIR:
                        continue
                    for i in range(4096):
                        out_lines.append((cx * 16 + (i & 15), sy * 16 + (i >> 8), cz * 16 + ((i >> 4) & 15), names[0]))
                    continue
                if all(n.split("[")[0] in AIR for n in names):
                    continue
                bits = max(4, (len(pal) - 1).bit_length())
                per_long = 64 // bits
                mask = (1 << bits) - 1
                longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype=np.uint64)
                arrs = [((longs >> np.uint64(k * bits)) & np.uint64(mask)).astype(np.int32) for k in range(per_long)]
                flat = np.stack(arrs, axis=1).reshape(-1)[:4096]
                for i in np.nonzero(np.array([names[v].split("[")[0] not in AIR for v in flat]))[0]:
                    out_lines.append((cx * 16 + (int(i) & 15), sy * 16 + (int(i) >> 8), cz * 16 + ((int(i) >> 4) & 15), names[int(flat[i])]))

    print("expanded", len(out_lines), file=sys.stderr)
    x0 = min(b[0] for b in out_lines); y0 = min(b[1] for b in out_lines); z0 = min(b[2] for b in out_lines)
    x1 = max(b[0] for b in out_lines); y1 = max(b[1] for b in out_lines); z1 = max(b[2] for b in out_lines)
    with gzip.open(OUT, "wt", encoding="utf-8", compresslevel=6) as fh:
        fh.write(f"MCMDUMP 1 {x1-x0+1} {y1-y0+1} {z1-z0+1}\n")
        for x, y, z, nm in out_lines:
            fh.write(f"{x-x0} {y-y0} {z-z0} {nm}\n")
    print(f"wrote {OUT}: {len(out_lines)} blocks, bounds {x0},{y0},{z0} -> {x1},{y1},{z1}", file=sys.stderr)


if __name__ == "__main__":
    main()
