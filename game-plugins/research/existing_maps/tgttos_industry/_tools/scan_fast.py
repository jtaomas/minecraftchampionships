#!/usr/bin/env python3
"""Fast read-only world scan (numpy bit unpack) for A8 TGTTOS Industry."""
import csv
import gzip
import io
import json
import os
import zlib
from collections import Counter

import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry\data"
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


def section_counts(section):
    """Return dict state -> (count, minx,miny,minz,maxx,maxy,maxz) relative to section."""
    bs = section.get("block_states")
    if not bs:
        return {}
    palette = bs.get("palette", [])
    if not palette:
        return {}
    names = [state_string(p) for p in palette]
    if len(palette) == 1:
        if names[0] in AIR:
            return {}
        return {names[0]: (4096, 0, 0, 0, 15, 15, 15)}
    data = bs.get("data")
    if data is None:
        return {names[0]: (4096, 0, 0, 0, 15, 15, 15)}
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in data], dtype="<u8")
    bitmat = np.unpackbits(longs.view(np.uint8), bitorder="little").reshape(len(longs), 64)
    usable = per_long * bits
    bitmat = bitmat[:, :usable].reshape(len(longs) * per_long, bits)
    weights = (1 << np.arange(bits, dtype=np.uint64))
    vals = (bitmat.astype(np.uint64) * weights).sum(axis=1).astype(np.int64)
    vals = vals[:4096].reshape(16, 16, 16)  # y, z, x
    out = {}
    for pi, name in enumerate(names):
        if name in AIR:
            continue
        mask = vals == pi
        c = int(mask.sum())
        if not c:
            continue
        idx = np.argwhere(mask)  # y,z,x
        out[name] = (c, int(idx[:, 2].min()), int(idx[:, 0].min()), int(idx[:, 1].min()),
                     int(idx[:, 2].max()), int(idx[:, 0].max()), int(idx[:, 1].max()))
    return out


def nbt_to_jsonable(obj):
    if isinstance(obj, dict):
        return {str(k): nbt_to_jsonable(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [nbt_to_jsonable(v) for v in obj]
    if isinstance(obj, (str, int, float, bool)):
        return obj
    return str(obj)


def dump(directory, do_blocks=True, do_entities=False):
    hist = Counter()
    bbox = [10**9, 10**9, 10**9, -10**9, -10**9, -10**9]
    block_entities = []
    entities = []
    yhist = Counter()
    chunks = 0
    chunks_list = []
    for fname in sorted(os.listdir(directory)):
        if not fname.endswith(".mca"):
            continue
        path = os.path.join(directory, fname)
        with open(path, "rb") as fh:
            region_data = fh.read()
        n_this = 0
        for idx in range(1024):
            chunk = read_chunk(region_data, idx)
            if not chunk:
                continue
            chunks += 1
            n_this += 1
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            chunks_list.append((cx, cz))
            if do_blocks:
                for section in chunk.get("sections", []):
                    sy = int(section.get("Y", 0))
                    counts = section_counts(section)
                    for name, (c, x0, y0, z0, x1, y1, z1) in counts.items():
                        hist[name] += c
                        yhist[sy * 16 + y0] += c  # lower-edge only, approximate; fixed below if needed
                        bx0, by0, bz0 = cx * 16 + x0, sy * 16 + y0, cz * 16 + z0
                        bx1, by1, bz1 = cx * 16 + x1, sy * 16 + y1, cz * 16 + z1
                        if bx0 < bbox[0]: bbox[0] = bx0
                        if by0 < bbox[1]: bbox[1] = by0
                        if bz0 < bbox[2]: bbox[2] = bz0
                        if bx1 > bbox[3]: bbox[3] = bx1
                        if by1 > bbox[4]: bbox[4] = by1
                        if bz1 > bbox[5]: bbox[5] = bz1
            if do_entities:
                for be in chunk.get("block_entities", []):
                    d = {k: be.get(k) for k in be.keys()}
                    d["_cx"] = cx
                    d["_cz"] = cz
                    block_entities.append(d)
                for e in chunk.get("entities", []):
                    d = {k: e.get(k) for k in e.keys()}
                    d["_cx"] = cx
                    d["_cz"] = cz
                    entities.append(d)
        print(f"{fname}: {n_this} chunks, total solid {sum(hist.values())}", flush=True)
    return dict(chunks=chunks, hist=hist, bbox=bbox, block_entities=block_entities,
                entities=entities, yhist=yhist, chunks_list=chunks_list)


def main():
    os.makedirs(OUT, exist_ok=True)
    blocks = scan = dump(os.path.join(WORLD, "region"), True, True)
    ents = dump(os.path.join(WORLD, "entities"), False, True)

    with open(os.path.join(OUT, "block_histogram.json"), "w", encoding="utf-8") as f:
        json.dump(blocks["hist"].most_common(), f, indent=1)

    total = sum(blocks["hist"].values())
    with open(os.path.join(OUT, "blocks_raw.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["block_state", "count", "share_pct"])
        for k, v in blocks["hist"].most_common():
            w.writerow([k, v, f"{100*v/total:.4f}"])

    base = Counter()
    for k, v in blocks["hist"].items():
        base[k.split("[")[0]] += v
    with open(os.path.join(OUT, "block_base_histogram.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["block", "count", "share_pct"])
        for k, v in base.most_common():
            w.writerow([k, v, f"{100*v/total:.4f}"])

    with open(os.path.join(OUT, "block_entities.json"), "w", encoding="utf-8") as f:
        json.dump([nbt_to_jsonable(d) for d in blocks["block_entities"]], f, indent=1)
    with open(os.path.join(OUT, "entities_raw.json"), "w", encoding="utf-8") as f:
        json.dump([nbt_to_jsonable(d) for d in ents["entities"]], f, indent=1)

    with open(os.path.join(OUT, "chunks.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["chunk_x", "chunk_z"])
        for cx, cz in sorted(blocks["chunks_list"]):
            w.writerow([cx, cz])

    summary = {
        "chunks_region": blocks["chunks"],
        "chunks_entities": ents["chunks"],
        "bbox_exact": blocks["bbox"],
        "total_solid_blocks": total,
        "distinct_block_states": len(blocks["hist"]),
        "distinct_base_blocks": len(base),
        "block_entity_count": len(blocks["block_entities"]),
        "entity_count": len(ents["entities"]),
    }
    with open(os.path.join(OUT, "scan_summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=1)
    print(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main()
