#!/usr/bin/env python3
"""Fast full-block MCMDUMP writer for City Marathon (read-only)."""
import io, os, struct, zlib, gzip, sys
import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders\city_marathon.mcmdump.gz"

def read_region(path):
    with open(path,"rb") as f:
        header=f.read(4096)
    for i in range(1024):
        off=int.from_bytes(header[i*4:i*4+3],"big")
        if off==0: continue
        with open(path,"rb") as f:
            f.seek(off*4096)
            ln=struct.unpack(">I",f.read(4))[0]
            comp=f.read(1)[0]
            raw=f.read(ln-1)
        if comp==1: raw=gzip.decompress(raw)
        elif comp==2: raw=zlib.decompress(raw)
        else: continue
        try: yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass

def main():
    rows=[]
    for fname in sorted(os.listdir(os.path.join(WORLD,"region"))):
        if not fname.endswith(".mca"): continue
        p=os.path.join(WORLD,"region",fname)
        if os.path.getsize(p)==0: continue
        for root in read_region(p):
            cx=int(root.get("xPos",0)); cz=int(root.get("zPos",0))
            for sec in root.get("sections",[]):
                sy=int(sec.get("Y",0))
                bs=sec.get("block_states")
                if not bs: continue
                pal=bs.get("palette",[])
                if not pal: continue
                names=[str(x.get("Name","minecraft:air")) for x in pal]
                nonair=[i for i,n in enumerate(names) if n not in ("minecraft:air","minecraft:cave_air","minecraft:void_air")]
                if not nonair: continue
                n=len(pal)
                if n==1:
                    idx=np.zeros(4096,dtype=np.uint16)
                else:
                    data=bs.get("data")
                    if data is None: continue
                    bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                    longs=np.asarray(data,dtype=np.uint64)
                    shifts=(np.arange(per,dtype=np.uint64)*np.uint64(bits))
                    idx=((longs[:,None]>>shifts)&np.uint64(mask)).reshape(-1)[:4096]
                # filter non-air
                sel=np.isin(idx,np.asarray(nonair,dtype=np.uint16))
                ii=np.nonzero(sel)[0]
                for i in ii:
                    nm=names[int(idx[i])].replace("minecraft:","")
                    x=cx*16+(int(i)&15); y=sy*16+(int(i)>>8); z=cz*16+((int(i)>>4)&15)
                    rows.append((x,y,z,nm))
    if not rows: raise SystemExit("no blocks")
    bx0=min(r[0] for r in rows); by0=min(r[1] for r in rows); bz0=min(r[2] for r in rows)
    bx1=max(r[0] for r in rows); by1=max(r[1] for r in rows); bz1=max(r[2] for r in rows)
    sx, sy, sz = bx1-bx0+1, by1-by0+1, bz1-bz0+1
    with gzip.open(OUT,"wt",encoding="utf-8",compresslevel=6) as f:
        f.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x,y,z,nm in rows:
            f.write(f"{x-bx0} {y-by0} {z-bz0} {nm}\n")
    print("wrote",len(rows),"blocks",sx,sy,sz,"bounds",bx0,by0,bz0,bx1,by1,bz1)

if __name__=="__main__": main()
