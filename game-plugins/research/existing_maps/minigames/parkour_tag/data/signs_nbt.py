#!/usr/bin/env python3
"""Full sign NBT export for MG-PT-01 (1.17.1 region, read-only)."""
import gzip, io, os, zlib, csv
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"

def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
    if not off:
        return None
    pos = off * 4096
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
    except Exception as e:
        return None

def text_of(tag):
    if tag is None:
        return ''
    try:
        return str(tag)
    except Exception:
        return ''

out = []
path = os.path.join(WORLD, "region", "r.2.1.mca")
data = open(path, "rb").read()
for idx in range(1024):
    ch = read_chunk(data, idx)
    if not ch:
        continue
    lv = ch.get("Level", ch)
    cx, cz = int(lv.get("xPos", 0)), int(lv.get("zPos", 0))
    if not (64 <= cx <= 67 and 60 <= cz <= 63):
        continue
    for te in lv.get("TileEntities", []):
        if str(te.get("id")) != "minecraft:sign":
            continue
        x, y, z = int(te.get("x")), int(te.get("y")), int(te.get("z"))
        if not (1035 <= x <= 1078 and 0 <= y <= 58 and 972 <= z <= 1017):
            continue
        out.append({
            "x": x, "y": y, "z": z,
            "color": str(te.get("Color", "black")),
            "glow": int(te.get("GlowingText", 0)),
            "front1": text_of(te.get("Text1")),
            "front2": text_of(te.get("Text2")),
            "front3": text_of(te.get("Text3")),
            "front4": text_of(te.get("Text4")),
            "click": text_of(te.get("Command")) if te.get("Command") is not None else "",
        })

out.sort(key=lambda r: (r["y"], r["z"], r["x"]))
with open("data/signs_full.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(out[0].keys()))
    w.writeheader(); w.writerows(out)
print("signs", len(out))
for r in out:
    print(r)
