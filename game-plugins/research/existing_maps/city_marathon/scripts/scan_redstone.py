#!/usr/bin/env python3
"""List every redstone component and interesting block by coordinate."""
import io, os, struct, zlib, gzip, json
import nbtlib
WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"

KEYS = ("pressure_plate","redstone","repeater","comparator","observer","torch","lever","button",
        "tripwire","sculk","target","daylight","note_block","piston","dispenser","dropper","hopper",
        "rail","minecart","sensor","trapdoor","fence_gate","door")

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

def ss(p):
    n=str(p.get("Name","minecraft:air")).replace("minecraft:","")
    pr=p.get("Properties")
    if not pr: return n
    return n+"["+",".join(f"{k}={pr[k]}" for k in sorted(pr.keys()))+"]"

out=[]
for fn in sorted(os.listdir(os.path.join(WORLD,"region"))):
    if not fn.endswith(".mca"): continue
    p=os.path.join(WORLD,"region",fn)
    if os.path.getsize(p)==0: continue
    for root in read_region(p):
        cx=int(root.get("xPos",0)); cz=int(root.get("zPos",0))
        for sec in root.get("sections",[]):
            sy=int(sec.get("Y",0)); bs=sec.get("block_states")
            if not bs: continue
            pal=bs.get("palette",[])
            if not pal: continue
            names=[ss(x) for x in pal]
            if not any(any(k in n for k in KEYS) for n in names): continue
            if len(pal)==1: idxs=[0]*4096
            else:
                data=bs.get("data")
                if data is None: continue
                n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                idxs=[0 if i//per>=len(longs) else (longs[i//per]>>((i%per)*bits))&mask for i in range(4096)]
            for i,idx in enumerate(idxs):
                if idx>=len(names): continue
                st=names[idx]
                if not any(k in st for k in KEYS): continue
                out.append((cx*16+(i&15), sy*16+(i>>8), cz*16+((i>>4)&15), st))
with open(os.path.join(OUT,"redstone.json"),"w") as f: json.dump(out,f,indent=1)
print(len(out))
from collections import Counter
print(Counter(x[3].split("[")[0] for x in out).most_common(40))
for p in sorted(out, key=lambda a:(a[3],a[0],a[2],a[1])):
    if "command" in p[3]: continue
    print(p)
