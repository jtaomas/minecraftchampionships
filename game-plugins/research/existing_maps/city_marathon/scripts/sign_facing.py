#!/usr/bin/env python3
"""Read exact block states at the seven sign positions."""
import io, os, struct, zlib, gzip, json
import nbtlib
WORLD=r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
BASE=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
be=json.load(open(BASE+r"\block_entities.json",encoding="utf-8"))
signs={(b["x"],b["y"],b["z"]) for b in be if b["id"]=="minecraft:sign"}
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
found={}
for fn in sorted(os.listdir(os.path.join(WORLD,"region"))):
    if not fn.endswith(".mca"): continue
    p=os.path.join(WORLD,"region",fn)
    if os.path.getsize(p)==0: continue
    for root in read_region(p):
        cx=int(root.get("xPos",0)); cz=int(root.get("zPos",0))
        # only chunks containing a sign
        if not any((cx*16<=sx<cx*16+16 and cz*16<=sz<cz*16+16) for sx,sy,sz in signs): continue
        for sec in root.get("sections",[]):
            sy=int(sec.get("Y",0)); bs=sec.get("block_states")
            if not bs: continue
            pal=bs.get("palette",[])
            if not pal: continue
            names=[ss(x) for x in pal]
            if not any("sign" in n for n in names): continue
            if len(pal)==1: idxs=[0]*4096
            else:
                data=bs.get("data")
                if data is None: continue
                n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                idxs=[0 if i//per>=len(longs) else (longs[i//per]>>((i%per)*bits))&mask for i in range(4096)]
            for i,idx in enumerate(idxs):
                if idx>=len(names): continue
                x=cx*16+(i&15); y=sy*16+(i>>8); z=cz*16+((i>>4)&15)
                if (x,y,z) in signs: found[(x,y,z)]=names[idx]
for s in sorted(signs):
    print(s, found.get(s))
json.dump({f"{x},{y},{z}":v for (x,y,z),v in found.items()},open(BASE+r"\sign_states.json","w"),indent=1)
