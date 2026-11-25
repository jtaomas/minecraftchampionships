#!/usr/bin/env python3
"""Read-only scan of the TGTTOS Pits source world.

Writes:
  data/world_scan.json   extents, per-region/chunk counts, block palette, block entities
  data/blocks.csv        block id -> count, share, first/last coordinate
  data/signs.csv         every sign (block entity) with front/back text and click events
  data/containers.csv    every container block entity with its items
  data/entities_raw.json every entity in entities/*.mca (summarised)

This is an analysis tool owned by A9 (tgttos_pits). It does not modify the
source world; the shared tools/ renderer and world_to_mcmdump.py are untouched.
"""
import csv
import gzip
import io
import json
import os
import re
import sys
import zlib
from collections import Counter, defaultdict

import nbtlib

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)))
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
    except Exception:
        return None


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def unpack_section(section):
    bs = section.get("block_states")
    if not bs:
        return
    palette = bs.get("palette", [])
    if not palette:
        return
    data = bs.get("data")
    if data is None:
        for i in range(4096):
            yield i, state_string(palette[0])
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            break
        shift = (i % per_long) * bits
        val = (longs[li] >> shift) & mask
        if val >= len(palette):
            continue
        yield i, state_string(palette[val])


def text_of(component):
    """Flatten a chat component (str / compound / list) to plain text, keep click events."""
    if component is None:
        return "", None
    if isinstance(component, str):
        return str(component), None
    if isinstance(component, list):
        parts = []
        click = None
        for c in component:
            t, cl = text_of(c)
            parts.append(t)
            click = click or cl
        return "".join(parts), click
    try:
        txt = str(component.get("text", ""))
        click = None
        ce = component.get("clickEvent") or component.get("click_event")
        if ce:
            act = ce.get("action")
            val = ce.get("value", "")
            click = f"{act}:{val}"
        extra = component.get("extra")
        if extra:
            t2, c2 = text_of(extra)
            txt += t2
            click = click or c2
        return txt, click
    except Exception:
        return str(component), None


def main():
    region_dir = os.path.join(SRC, "region")
    counts = Counter()
    first = {}
    last = {}
    minx = miny = minz = 10 ** 9
    maxx = maxy = maxz = -10 ** 9
    per_region = defaultdict(lambda: [0, 0])  # blocks, chunks with blocks
    occupied_chunks = 0
    signs = []
    containers = []
    block_entities_other = []
    chest_items = Counter()

    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    for fi, fname in enumerate(files):
        path = os.path.join(region_dir, fname)
        data = open(path, "rb").read()
        region_blocks = 0
        region_chunks = 0
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            has = False
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                bs = section.get("block_states")
                if not bs:
                    continue
                palette = bs.get("palette", [])
                if not palette:
                    continue
                # fast path: a single-state section is 4096 copies of that state
                if len(palette) == 1 and bs.get("data") is None:
                    block = state_string(palette[0])
                    if block in AIR:
                        continue
                    n = 4096
                    counts[block] += n
                    region_blocks += n
                    has = True
                    x0, y0, z0 = cx * 16, sy * 16, cz * 16
                    first.setdefault(block, (x0, y0, z0))
                    last[block] = (x0 + 15, y0 + 15, z0 + 15)
                    minx = min(minx, x0); miny = min(miny, y0); minz = min(minz, z0)
                    maxx = max(maxx, x0 + 15); maxy = max(maxy, y0 + 15); maxz = max(maxz, z0 + 15)
                    continue
                for i, block in unpack_section(section):
                    if block in AIR:
                        continue
                    bx = cx * 16 + (i & 15)
                    by = sy * 16 + (i >> 8)
                    bz = cz * 16 + ((i >> 4) & 15)
                    counts[block] += 1
                    region_blocks += 1
                    has = True
                    if block not in first:
                        first[block] = (bx, by, bz)
                    last[block] = (bx, by, bz)
                    if bx < minx: minx = bx
                    if by < miny: miny = by
                    if bz < minz: minz = bz
                    if bx > maxx: maxx = bx
                    if by > maxy: maxy = by
                    if bz > maxz: maxz = bz
            if has:
                region_chunks += 1
                occupied_chunks += 1
            for be in chunk.get("block_entities", []):
                beid = str(be.get("id", ""))
                x, y, z = int(be.get("x", 0)), int(be.get("y", 0)), int(be.get("z", 0))
                rec = {"x": x, "y": y, "z": z, "id": beid}
                if "sign" in beid:
                    front, fclick = text_of(be.get("front_text"))
                    back, bclick = text_of(be.get("back_text"))
                    if not front and "Text1" in be:
                        front = " / ".join(str(be.get(f"Text{i}", "")) for i in range(1, 5))
                    rec.update({"front": front, "back": back,
                                "front_click": fclick, "back_click": bclick,
                                "color": str(be.get("Color", "")),
                                "glowing": bool(be.get("GlowingText", 0)),
                                "raw": str(be)[:2000]})
                    signs.append(rec)
                elif any(k in beid for k in ("chest", "barrel", "shulker", "hopper", "dispenser", "dropper", "furnace")):
                    items = be.get("Items") or []
                    inv = []
                    for it in items:
                        nm = str(it.get("id", ""))
                        cnt = int(it.get("Count", 1))
                        inv.append({"slot": int(it.get("Slot", -1)), "id": nm, "count": cnt,
                                    "tag": str(it.get("tag", ""))[:400]})
                        chest_items[nm] += cnt
                    rec["items"] = inv
                    containers.append(rec)
                else:
                    rec["raw"] = str(be)[:600]
                    block_entities_other.append(rec)
        per_region[fname] = [region_blocks, region_chunks]
        print(f"{fname}: {region_blocks} blocks, {region_chunks} chunks", flush=True)

    total = sum(counts.values())
    scan = {
        "world": "TGTTOS Pits - By Mickey_Kat",
        "version": "1.19.1 (DataVersion 3117)",
        "chunks_present": 3254,
        "occupied_chunks": occupied_chunks,
        "extents": {"min": [minx, miny, minz], "max": [maxx, maxy, maxz],
                    "size": [maxx - minx + 1, maxy - miny + 1, maxz - minz + 1]},
        "total_non_air_blocks": total,
        "distinct_block_states": len(counts),
        "per_region": dict(per_region),
        "signs": len(signs),
        "containers": len(containers),
        "other_block_entities": len(block_entities_other),
    }
    with open(os.path.join(OUT, "world_scan.json"), "w", encoding="utf-8") as fh:
        json.dump(scan, fh, indent=2)

    with open(os.path.join(OUT, "blocks.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["block_state", "count", "share_percent", "first_x", "first_y", "first_z", "use"])
        for b, n in counts.most_common():
            w.writerow([b, n, round(100.0 * n / total, 4), *first[b], ""])

    with open(os.path.join(OUT, "signs.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["x", "y", "z", "front", "back", "color", "glowing", "front_click", "back_click"])
        for s in sorted(signs, key=lambda r: (r["y"], r["x"], r["z"])):
            w.writerow([s["x"], s["y"], s["z"], s["front"], s["back"], s.get("color", ""),
                        s.get("glowing", ""), s.get("front_click", ""), s.get("back_click", "")])

    with open(os.path.join(OUT, "containers.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["x", "y", "z", "id", "slot", "item", "count", "tag"])
        for c in sorted(containers, key=lambda r: (r["y"], r["x"], r["z"])):
            if not c["items"]:
                w.writerow([c["x"], c["y"], c["z"], c["id"], "", "", "", ""])
            for it in c["items"]:
                w.writerow([c["x"], c["y"], c["z"], c["id"], it["slot"], it["id"], it["count"], it["tag"]])

    with open(os.path.join(OUT, "container_items.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["item", "total_count"])
        for it, n in chest_items.most_common():
            w.writerow([it, n])

    # entities
    ent_dir = os.path.join(SRC, "entities")
    ents = []
    for fname in sorted(os.listdir(ent_dir)):
        if not fname.endswith(".mca"):
            continue
        data = open(os.path.join(ent_dir, fname), "rb").read()
        for idx in range(1024):
            chunk = read_chunk(data, idx)
            if not chunk:
                continue
            for e in chunk.get("Entities", []):
                rec = {
                    "id": str(e.get("id", "")),
                    "x": float(e.get("Pos", [0, 0, 0])[0]),
                    "y": float(e.get("Pos", [0, 0, 0])[1]),
                    "z": float(e.get("Pos", [0, 0, 0])[2]),
                    "tags": [str(t) for t in (e.get("Tags") or [])],
                    "custom_name": str(e.get("CustomName", "")),
                    "raw": str(e)[:500],
                }
                ents.append(rec)
    with open(os.path.join(OUT, "entities_raw.json"), "w", encoding="utf-8") as fh:
        json.dump(ents, fh, indent=2)
    print(json.dumps({**scan, "entities": len(ents)}, indent=2))
    print("signs:", len(signs), "containers:", len(containers), "other BE:", len(block_entities_other),
          "entities:", len(ents))


if __name__ == "__main__":
    main()
