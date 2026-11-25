#!/usr/bin/env python3
"""Fast pass: chunk list, block entities, entities. No block decode."""
import io
import json
import os
import zlib
import gzip as gz

import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data_raw")
os.makedirs(OUT, exist_ok=True)


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


def to_plain(v):
    if hasattr(v, "keys"):
        return {str(k): to_plain(v[k]) for k in v.keys()}
    if isinstance(v, (list, tuple)):
        return [to_plain(x) for x in v]
    if isinstance(v, bool):
        return v
    if isinstance(v, (int, float, str)):
        return v
    try:
        return v.item() if hasattr(v, "item") else str(v)
    except Exception:
        return str(v)


def main():
    region_dir = os.path.join(WORLD, "region")
    tile_entities = []
    entities = []
    chunks = []
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    for fname in files:
        path = os.path.join(region_dir, fname)
        with open(path, "rb") as fh:
            data = fh.read()
        n_before = len(chunks)
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            chunks.append([cx, cz])
            for te in chunk.get("block_entities", chunk.get("TileEntities", [])):
                rec = to_plain(te)
                rec["_chunk"] = [cx, cz]
                tile_entities.append(rec)
            for en in chunk.get("Entities", []):
                rec = to_plain(en)
                rec["_chunk"] = [cx, cz]
                entities.append(rec)
        print(f"{fname}: +{len(chunks)-n_before} chunks, te={len(tile_entities)} ent={len(entities)}", flush=True)

    with open(os.path.join(OUT, "tile_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(tile_entities, fh, indent=1)
    with open(os.path.join(OUT, "entities.json"), "w", encoding="utf-8") as fh:
        json.dump(entities, fh, indent=1)
    with open(os.path.join(OUT, "chunks.json"), "w", encoding="utf-8") as fh:
        json.dump(chunks, fh)
    print("chunks", len(chunks), "te", len(tile_entities), "ent", len(entities))


if __name__ == "__main__":
    main()
