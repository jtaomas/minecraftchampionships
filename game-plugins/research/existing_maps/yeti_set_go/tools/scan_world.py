#!/usr/bin/env python3
"""Read-only forensic scan of the Yeti Set Go world.

Writes data/scan.json with:
  - every block entity (command blocks, signs, containers, others)
  - block palette counts (full block states + base ids)
  - entity list
  - chunk/region inventory, bounds, per-Y histogram

Run from the yeti_set_go folder:
  python tools/scan_world.py <world-dir>
"""
import io
import json
import os
import sys
import zlib
import gzip

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
    except Exception as exc:  # noqa: BLE001
        print(f"  chunk parse fail r{index}: {exc}", file=sys.stderr)
        return None


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]"


def unpack_palette(section):
    bs = section.get("block_states")
    if not bs:
        return
    palette = bs.get("palette", [])
    if not palette:
        return
    data = bs.get("data")
    if data is None:
        yield 0, state_string(palette[0]), 4096
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
    counts = {}
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            break
        shift = (i % per_long) * bits
        val = (longs[li] >> shift) & mask
        if val >= len(palette):
            continue
        counts[val] = counts.get(val, 0) + 1
    for val, n in counts.items():
        yield val, state_string(palette[val]), n


def item_list(items):
    out = []
    for it in items or []:
        d = {
            "slot": int(it.get("Slot", -1)),
            "id": str(it.get("id", "")),
            "count": int(it.get("Count", 1)),
        }
        tag = it.get("tag")
        if tag:
            simple = {}
            for k in ("display", "CustomModelData", "Enchantments", "AttributeModifiers",
                      "Unbreakable", "SkullOwner", "HideFlags", "Potion", "Damage"):
                if k in tag:
                    simple[k] = jsonable(tag[k])
            if simple:
                d["tag"] = simple
        out.append(d)
    return sorted(out, key=lambda x: x["slot"])


def jsonable(tag):
    if isinstance(tag, nbtlib.Compound) or hasattr(tag, "keys"):
        return {str(k): jsonable(v) for k, v in tag.items()}
    if isinstance(tag, (list, tuple)) or hasattr(tag, "__iter__") and not isinstance(tag, (str, bytes)):
        try:
            return [jsonable(v) for v in tag]
        except TypeError:
            pass
    if hasattr(tag, "unpack"):
        return tag.unpack()
    try:
        return tag.item if hasattr(tag, "item") else tag
    except Exception:  # noqa: BLE001
        return str(tag)


def main():
    world = os.path.abspath(sys.argv[1])
    region_dir = os.path.join(world, "region")
    out = {
        "world": world,
        "chunks": 0,
        "regions": [],
        "bounds": None,
        "block_states": {},
        "base_blocks": {},
        "block_entities": [],
        "entities": [],
        "y_hist": {},
        "biomes": {},
    }

    xs, ys, zs = [], [], []
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        path = os.path.join(region_dir, fname)
        data = open(path, "rb").read()
        if len(data) < 4096:
            continue
        n_chunks = 0
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            n_chunks += 1
            out["chunks"] += 1
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            xs += [cx * 16, cx * 16 + 15]
            zs += [cz * 16, cz * 16 + 15]
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                for _, block, n in unpack_palette(section):
                    if block in AIR:
                        continue
                    out["block_states"][block] = out["block_states"].get(block, 0) + n
                    base = block.split("[")[0]
                    out["base_blocks"][base] = out["base_blocks"].get(base, 0) + n
                    ys += [sy * 16] if block else []
                    out["y_hist"][str(sy)] = out["y_hist"].get(str(sy), 0) + n
            for be in chunk.get("block_entities", []) or []:
                d = {str(k): jsonable(v) for k, v in be.items()}
                d["_chunk"] = [cx, cz]
                out["block_entities"].append(d)
        out["regions"].append({"file": fname, "bytes": len(data), "chunks": n_chunks})

    for fname in sorted(os.listdir(os.path.join(world, "entities"))):
        if not fname.endswith(".mca"):
            continue
        data = open(os.path.join(world, "entities", fname), "rb").read()
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            pos = chunk.get("Position")
            if pos is not None and not hasattr(pos, "get"):
                cx, cz = int(pos[0]), int(pos[1])
            else:
                cx = int((pos or {}).get("x", chunk.get("xPos", 0)))
                cz = int((pos or {}).get("z", chunk.get("zPos", 0)))
            for e in chunk.get("Entities", []) or []:
                d = {str(k): jsonable(v) for k, v in e.items()}
                d["_chunk"] = [cx, cz]
                out["entities"].append(d)

    out["bounds"] = {
        "x": [min(xs), max(xs)] if xs else None,
        "z": [min(zs), max(zs)] if zs else None,
        "y_sections": [min(int(k) for k in out["y_hist"]), max(int(k) for k in out["y_hist"])] if out["y_hist"] else None,
    }
    out["block_entities"].sort(key=lambda b: (b.get("z", 0), b.get("x", 0), b.get("y", 0)))
    with open(os.path.join("data", "scan.json"), "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1, default=str)
    print(f"chunks={out['chunks']} block_entities={len(out['block_entities'])} entities={len(out['entities'])}")
    print(f"bounds={out['bounds']}")
    print(f"distinct states={len(out['block_states'])} base={len(out['base_blocks'])}")


if __name__ == "__main__":
    main()
