#!/usr/bin/env python3
"""Locate cave_air (elytra-clear zones) and light blocks."""
import io, os, zlib, gzip
import numpy as np, nbtlib
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

pts = []
for fn in sorted(os.listdir(os.path.join(W, "region"))):
    if not fn.endswith(".mca"): continue
    for ch in read_region(os.path.join(W, "region", fn)):
        cx = int(ch.get("xPos", 0)); cz = int(ch.get("zPos", 0))
        for sec in ch.get("sections", []):
            bs = sec.get("block_states")
            if not bs: continue
            pal = bs.get("palette", [])
            names = [str(p.get("Name", "")) for p in pal]
            if "minecraft:cave_air" not in names:
                continue
            sy = int(sec.get("Y", 0))
            dat = bs.get("data")
            bits = max(4, (len(pal) - 1).bit_length()); per = 64 // bits; mask = (1 << bits) - 1
            idx = np.array([i for i, n in enumerate(names) if n == "minecraft:cave_air"])
            if dat is None:
                hit = np.arange(4096)
            else:
                longs = np.array([int(v) & 0xFFFFFFFFFFFFFFFF for v in dat], dtype=np.uint64)
                arr = np.stack([((longs >> np.uint64(k*bits)) & np.uint64(mask)).astype(np.int32) for k in range(per)], axis=1).reshape(-1)[:4096]
                hit = np.nonzero(np.isin(arr, idx))[0]
            for i in hit:
                pts.append((cx*16 + (int(i) & 15), sy*16 + (int(i) >> 8), cz*16 + ((int(i) >> 4) & 15)))
print("cave_air blocks:", len(pts))
if pts:
    xs=[p[0] for p in pts]; ys=[p[1] for p in pts]; zs=[p[2] for p in pts]
    print("bounds", min(xs),min(ys),min(zs), "->", max(xs),max(ys),max(zs))
with gzip.open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data\cave_air.txt.gz", "wt") as fh:
    for x,y,z in pts: fh.write(f"{x} {y} {z}\n")
