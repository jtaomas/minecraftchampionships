"""List every minigames container with coords and item slots (read-only)."""
from __future__ import annotations

import io
import struct
import zlib
from pathlib import Path

import nbtlib

WORLD = Path(r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3")
OUT = Path(__file__).resolve().parents[1] / "data" / "minigames_containers.csv"

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
    for i in range(1024):
        off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
        if off == 0:
            continue
        start = off * 4096
        if start + 5 > len(data):
            continue
        try:
            chunk = read_chunk(data[start:start + 32768])
        except Exception:
            continue
        if chunk is not None:
            yield chunk


def te_list(chunk):
    if "Level" in chunk:
        return chunk["Level"].get("TileEntities", [])
    return chunk.get("block_entities", [])


rows = []
for mca in sorted((WORLD / "region").glob("*.mca")):
    for chunk in iter_region(mca):
        for te in te_list(chunk):
            tid = str(te.get("id", ""))
            if tid not in CONTAINER_IDS:
                continue
            items = te.get("Items", [])
            contents = []
            for it in items or []:
                slot = int(it.get("Slot", 0))
                count = int(it.get("Count", 0))
                contents.append(f"{slot}:{it.get('id')} x{count}")
            rows.append({
                "x": int(te.get("x", 0)),
                "y": int(te.get("y", 0)),
                "z": int(te.get("z", 0)),
                "id": tid,
                "items": len(items or []),
                "contents": " | ".join(contents),
            })

rows.sort(key=lambda r: (r["x"], r["z"], r["y"]))
OUT.parent.mkdir(parents=True, exist_ok=True)
import csv
with OUT.open("w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=["x", "y", "z", "id", "items", "contents"])
    w.writeheader(); w.writerows(rows)
print(f"{len(rows)} containers -> {OUT}")
for r in rows:
    print(f"{r['x']:>7} {r['y']:>4} {r['z']:>7}  {r['id']:<32} items={r['items']:>2}  {r['contents']}")
