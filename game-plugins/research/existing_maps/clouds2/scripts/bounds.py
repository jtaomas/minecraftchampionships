#!/usr/bin/env python3
"""Exact block bounds (min/max) for the dump."""
import io, os, sys, zlib
import numpy as np, nbtlib
WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

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
            elif comp == 1: import gzip; raw = gzip.decompress(raw)
            elif comp != 3: continue
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: continue

mnx=mny=mnz=10**9; mxx=mxy=mxz=-10**9
for fn in sorted(os.listdir(os.path.join(WORLD,"region"))):
    if not fn.endswith(".mca"): continue
    for ch in read_region(os.path.join(WORLD,"region",fn)):
        cx=int(ch.get("xPos",0)); cz=int(ch.get("zPos",0))
        for sec in ch.get("sections", []):
            bs=sec.get("block_states")
            if not bs: continue
            pal=bs.get("palette", [])
            if not pal: continue
            names=[str(p.get("Name","minecraft:air")) for p in pal]
            sy=int(sec.get("Y",0))
            dat=bs.get("data")
            if dat is None:
                if names[0] in AIR: continue
                hit=np.arange(4096)
            else:
                if all(n in AIR for n in names): continue
                bits=max(4,(len(pal)-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=np.array([int(v)&0xFFFFFFFFFFFFFFFF for v in dat], dtype=np.uint64)
                arr=np.stack([((longs>>np.uint64(k*bits))&np.uint64(mask)).astype(np.int32) for k in range(per)],axis=1).reshape(-1)[:4096]
                hit=np.nonzero(np.array([names[v] not in AIR for v in arr]))[0]
                if len(hit)==0: continue
            xs=cx*16+(hit&15); ys=sy*16+(hit>>8); zs=cz*16+((hit>>4)&15)
            mnx=min(mnx,int(xs.min())); mxx=max(mxx,int(xs.max()))
            mny=min(mny,int(ys.min())); mxy=max(mxy,int(ys.max()))
            mnz=min(mnz,int(zs.min())); mxz=max(mxz,int(zs.max()))
print("bounds", mnx,mny,mnz,"->",mxx,mxy,mxz, "size", mxx-mnx+1, mxy-mny+1, mxz-mnz+1)
