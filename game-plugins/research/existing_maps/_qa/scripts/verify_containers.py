"""Independent read-only container scan of the staged source worlds.

Supports 1.18+ (sections/block_entities) and 1.17 (Level/TileEntities).
Prints per-world container counts and item totals. Writes only to _qa/data.
"""
from __future__ import annotations

import csv
import io
import struct
import zlib
from collections import Counter, defaultdict
from pathlib import Path

import nbtlib

BASE = Path(r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps")
OUT = Path(__file__).resolve().parents[1] / "data" / "container_source_scan.csv"

WORLDS = {
    "clouds2": BASE / "Clouds_2_-_Electric_Boogaloo__Download_Version_" / "Clouds 2 - Electric Boogaloo",
    "city_marathon": BASE / "city-marathon" / "City Marathon",
    "yeti_set_go": BASE / "YetiSetGo",
    "space_race": BASE / "Space_Race" / "Space Race",
    "decision_dome": BASE / "Decision_Dome" / "Decision Dome - Recreation",
    "tgttos_badlands": BASE / "tgttos-badlands-by-mickeykat" / "TGTTOS Badlands - By MickeyKat",
    "tgttos_industry": BASE / "tgttos-industry-by-mickey-kat" / "TGTTOS Industry - By Mickey_Kat",
    "tgttos_pits": BASE / "tgttos-pits-by-mickey-kat" / "TGTTOS Pits - By Mickey_Kat",
    "minigames": BASE / "daniellaur-s-minigames-version-3" / "daniellaur's minigames version 3",
}

CONTAINER_IDS = {
    "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel",
    "minecraft:shulker_box", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
    "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box",
    "minecraft:lime_shulker_box", "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
    "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box", "minecraft:purple_shulker_box",
    "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
    "minecraft:red_shulker_box", "minecraft:black_shulker_box",
    "minecraft:hopper", "minecraft:dispenser", "minecraft:dropper", "minecraft:furnace",
    "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand",
    "minecraft:campfire", "minecraft:soul_campfire", "minecraft:lectern",
}


def read_chunk(raw: bytes):
    if len(raw) < 5:
        return None
    length = struct.unpack(">I", raw[:4])[0]
    ctype = raw[4]
    payload = raw[5:4 + length]
    if ctype == 1:
        data = zlib.decompress(payload, 16 + zlib.MAX_WBITS)
    elif ctype == 2:
        data = zlib.decompress(payload)
    elif ctype == 3:
        data = payload
    else:
        return None
    return nbtlib.File.parse(io.BytesIO(data))


def iter_region(path: Path):
    data = path.read_bytes()
    if len(data) < 8192:
        return
    for i in range(1024):
        off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
        if off == 0:
            continue
        start = off * 4096
        if start + 5 > len(data):
            continue
        try:
            chunk = read_chunk(data[start:start + 4096 * 8])
        except Exception:
            continue
        if chunk is not None:
            yield chunk


def container_list(chunk):
    root = chunk
    if "Level" in root:
        level = root["Level"]
        return level.get("TileEntities", [])
    return root.get("block_entities", [])


def main() -> None:
    rows = []
    for name, world in WORLDS.items():
        region = world / "region"
        if not region.exists():
            print(f"{name}: missing {region}")
            continue
        by_id = Counter()
        empty = Counter()
        items_total = Counter()
        coords = defaultdict(list)
        chunks = 0
        for mca in sorted(region.glob("*.mca")):
            for chunk in iter_region(mca):
                chunks += 1
                for te in container_list(chunk):
                    tid = str(te.get("id", ""))
                    if tid not in CONTAINER_IDS:
                        continue
                    items = te.get("Items", [])
                    n = len(items) if items is not None else 0
                    by_id[tid] += 1
                    items_total[tid] += n
                    if n == 0:
                        empty[tid] += 1
                    x = int(te.get("x", 0)); y = int(te.get("y", 0)); z = int(te.get("z", 0))
                    coords[tid].append((x, y, z))
        total = sum(by_id.values())
        empties = sum(empty.values())
        print(f"{name}: chunks={chunks} containers={total} empty={empties}")
        for tid, count in sorted(by_id.items()):
            print(f"  {tid}: {count} (empty {empty[tid]}, items {items_total[tid]})")
            rows.append({
                "world": name, "container": tid, "count": count,
                "empty": empty[tid], "item_entries": items_total[tid],
                "sample_coords": "; ".join(f"{x},{y},{z}" for x, y, z in coords[tid][:6]),
            })
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["world", "container", "count", "empty", "item_entries", "sample_coords"])
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    main()
