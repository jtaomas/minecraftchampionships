#!/usr/bin/env python3
"""Print all blocks in a box, layer by layer."""
import io, os, struct, zlib, gzip, sys
import nbtlib
WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"

def read_region(path):
    with open(path,"rb") as f: header=f.read(4096)
    for i in range(1024):
        off=int.from_bytes(header[i*4:i*4+3],"big")
        if off==0: continue
        with open(path,"rb") as f:
            f.seek(off*4096); ln=struct.unpack(">I",f.read(4))[0]; comp=f.read(1)[0]; raw=f.read(ln-1)
        if comp==1: raw=gzip.decompress(raw)
        elif comp==2: raw=zlib.decompress(raw)
        else: continue
        try: yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass

chunks={}
for fn in os.listdir(os.path.join(WORLD,"region")):
    if not fn.endswith(".mca"): continue
    p=os.path.join(WORLD,"region",fn)
    if os.path.getsize(p)==0: continue
    for root in read_region(p): chunks[(int(root.get("xPos",0)),int(root.get("zPos",0)))]=root

def get(x,y,z):
    root=chunks.get((x//16,z//16))
    if not root: return "air"
    for sec in root.get("sections",[]):
        sy=int(sec.get("Y",0))
        if not (sy*16<=y<sy*16+16): continue
        bs=sec.get("block_states")
        if not bs: return "air"
        pal=bs.get("palette",[])
        if not pal: return "air"
        if len(pal)==1: idx=0
        else:
            data=bs.get("data")
            if data is None: return "air"
            n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
            i=(x%16)+((y%16)*256)+((z%16)<<4)
            li=i//per
            longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
            if li>=len(longs): return "air"
            idx=(longs[li]>>((i%per)*bits))&mask
        if idx>=len(pal): return "air"
        return str(pal[idx].get("Name","minecraft:air")).replace("minecraft:","")
    return "air"

x1,x2,y1,y2,z1,z2=map(int,sys.argv[1:7])
for y in range(y1,y2+1):
    print("=== y",y)
    for z in range(z1,z2+1):
        line=[]
        for x in range(x1,x2+1):
            b=get(x,y,z)
            line.append("." if b=="air" else b[:2])
        print(f"z{z:4d} "+" ".join(line))
