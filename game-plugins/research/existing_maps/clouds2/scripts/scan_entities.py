#!/usr/bin/env python3
"""Scan entities/*.mca for entity types and positions."""
import gzip, io, json, os, sys, zlib
from collections import Counter
import nbtlib

W = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"

def read_region(path):
    data = open(path, "rb").read()
    for idx in range(1024):
        e = data[idx*4:idx*4+4]
        if len(e) < 4: continue
        off = (e[0] << 16) | (e[1] << 8) | e[2]
        if not off: continue
        pos = off*4096
        if pos+5 > len(data): continue
        ln = int.from_bytes(data[pos:pos+4], "big"); comp = data[pos+4]
        raw = data[pos+5:pos+4+ln]
        try:
            if comp == 2: raw = zlib.decompress(raw)
            elif comp == 1: raw = gzip.decompress(raw)
            elif comp != 3: continue
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: continue

def plain(v):
    if hasattr(v, "unpack"):
        try: return plain(v.unpack())
        except Exception: pass
    if isinstance(v, dict): return {str(k): plain(x) for k, x in v.items()}
    if isinstance(v, list): return [plain(x) for x in v]
    if isinstance(v, (bytes, bytearray)): return v.decode("utf-8", "replace")
    return v

cnt = Counter()
out = []
ed = os.path.join(W, "entities")
for fn in sorted(os.listdir(ed)):
    if not fn.endswith(".mca"): continue
    for ch in read_region(os.path.join(ed, fn)):
        for ent in ch.get("Entities", []):
            e = plain(ent)
            t = e.get("id", "?")
            cnt[t] += 1
            out.append({
                "type": t,
                "x": e.get("Pos", [None, None, None])[0],
                "y": e.get("Pos", [None, None, None])[1],
                "z": e.get("Pos", [None, None, None])[2],
                "custom_name": e.get("CustomName", ""),
                "tags": e.get("Tags", []),
                "items": len(e.get("Items", [])) if isinstance(e.get("Items"), list) else 0,
                "armor": len(e.get("ArmorItems", [])) if isinstance(e.get("ArmorItems"), list) else 0,
            })
print("entities:", sum(cnt.values()))
for k, v in cnt.most_common(): print(f"  {v:4d} {k}")
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
json.dump(out, open(D + r"\entities.json", "w"), indent=1)
