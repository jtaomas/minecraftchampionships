#!/usr/bin/env python3
"""Read-only NBT scan of the City Marathon world.

Writes JSON evidence next to the report: block entities (command blocks, signs,
containers), entity files, level.dat, player data.
"""
import io
import json
import os
import struct
import zlib
import gzip
import sys
from collections import Counter

import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"


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
        if comp == 1:
            raw = gzip.decompress(raw)
        elif comp == 2:
            raw = zlib.decompress(raw)
        else:
            continue
        try:
            root = nbtlib.File.parse(io.BytesIO(raw))
        except Exception as e:
            print("parse fail", path, i, e, file=sys.stderr)
            continue
        cx = int(root.get("xPos", 0)); cz = int(root.get("zPos", 0))
        chunks.append((cx, cz, root))
    return chunks


def plain(tag):
    """Convert nbtlib tag to plain python."""
    if isinstance(tag, nbtlib.Compound):
        return {k: plain(v) for k, v in tag.items()}
    if isinstance(tag, nbtlib.List):
        return [plain(v) for v in tag]
    if isinstance(tag, nbtlib.ByteArray):
        return [int(v) for v in tag]
    if isinstance(tag, nbtlib.IntArray):
        return [int(v) for v in tag]
    if isinstance(tag, nbtlib.LongArray):
        return [int(v) for v in tag]
    if isinstance(tag, nbtlib.String):
        return str(tag)
    if isinstance(tag, (nbtlib.Byte, nbtlib.Short, nbtlib.Int, nbtlib.Long)):
        return int(tag)
    if isinstance(tag, (nbtlib.Float, nbtlib.Double)):
        return float(tag)
    return tag


def main():
    os.makedirs(OUT, exist_ok=True)
    level = plain(nbtlib.load(os.path.join(WORLD, "level.dat")))
    with open(os.path.join(OUT, "level.json"), "w", encoding="utf-8") as f:
        json.dump(level, f, indent=1, default=str)

    blocks = Counter()
    block_entities = []
    chunk_index = []
    for fname in sorted(os.listdir(os.path.join(WORLD, "region"))):
        if not fname.endswith(".mca"):
            continue
        path = os.path.join(WORLD, "region", fname)
        if os.path.getsize(path) == 0:
            continue
        for cx, cz, root in read_region(path):
            chunk_index.append({"region": fname, "cx": cx, "cz": cz,
                                "status": str(root.get("Status", "")),
                                "sections": len(root.get("sections", [])),
                                "block_entities": len(root.get("block_entities", []))})
            for section in root.get("sections", []):
                bs = section.get("block_states")
                if not bs:
                    continue
                pal = bs.get("palette", [])
                if not pal:
                    continue
                data = bs.get("data")
                n = len(pal)
                if data is None:
                    cnt = 4096
                else:
                    cnt = None
                if cnt == 4096:
                    idxs = [0] * 4096
                else:
                    bits = max(4, (n - 1).bit_length())
                    per = 64 // bits
                    mask = (1 << bits) - 1
                    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
                    idxs = []
                    for i in range(4096):
                        li = i // per
                        if li >= len(longs):
                            idxs.append(0); continue
                        idxs.append((longs[li] >> ((i % per) * bits)) & mask)
                counts = Counter(idxs)
                for idx, c in counts.items():
                    if idx >= len(pal):
                        continue
                    name = str(pal[idx].get("Name", "minecraft:air"))
                    if name in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
                        continue
                    blocks[name] += c
            for be in root.get("block_entities", []):
                d = plain(be)
                d["_cx"] = cx; d["_cz"] = cz; d["_region"] = fname
                block_entities.append(d)

    with open(os.path.join(OUT, "block_entities.json"), "w", encoding="utf-8") as f:
        json.dump(block_entities, f, indent=1, default=str)
    with open(os.path.join(OUT, "block_counts.json"), "w", encoding="utf-8") as f:
        json.dump(blocks.most_common(), f, indent=1)
    with open(os.path.join(OUT, "chunk_index.json"), "w", encoding="utf-8") as f:
        json.dump(chunk_index, f, indent=1)

    # entities
    ents = []
    edir = os.path.join(WORLD, "entities")
    for fname in sorted(os.listdir(edir)):
        if not fname.endswith(".mca"):
            continue
        path = os.path.join(edir, fname)
        for cx, cz, root in read_region(path):
            for e in root.get("Entities", []):
                d = plain(e)
                d["_cx"] = cx; d["_cz"] = cz; d["_region"] = fname
                ents.append(d)
    with open(os.path.join(OUT, "entities.json"), "w", encoding="utf-8") as f:
        json.dump(ents, f, indent=1, default=str)

    # playerdata
    pdir = os.path.join(WORLD, "playerdata")
    players = []
    for fname in os.listdir(pdir):
        try:
            p = plain(nbtlib.load(os.path.join(pdir, fname)))
        except Exception as e:
            print("playerdata fail", fname, e, file=sys.stderr); continue
        p["_file"] = fname
        players.append(p)
    with open(os.path.join(OUT, "playerdata.json"), "w", encoding="utf-8") as f:
        json.dump(players, f, indent=1, default=str)

    print("chunks", len(chunk_index), "block entities", len(block_entities),
          "entities", len(ents), "block types", len(blocks), "players", len(players))
    print("Top blocks:")
    for b, c in blocks.most_common(40):
        print(f"  {c:>8} {b}")


if __name__ == "__main__":
    main()
