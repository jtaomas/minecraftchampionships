#!/usr/bin/env python3
"""Print all non-air blocks in a box with names/states."""
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

def state_string(p):
    n=str(p.get("Name","minecraft:air"))
    pr=p.get("Properties")
    if not pr: return n.replace("minecraft:","")
    return n.replace("minecraft:","")+"["+",".join(f"{k}={pr[k]}" for k in sorted(pr.keys()))+"]"

def main():
    x1,x2,y1,y2,z1,z2=map(int,sys.argv[1:7])
    out=[]
    for (cx,cz),root in chunks.items():
        for sec in root.get("sections",[]):
            sy=int(sec.get("Y",0))
            if sy*16+15<y1 or sy*16>y2: continue
            bs=sec.get("block_states")
            if not bs: continue
            pal=bs.get("palette",[])
            if not pal: continue
            if len(pal)==1:
                idxs=[0]*4096
            else:
                data=bs.get("data")
                if data is None: continue
                n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                idxs=[0 if i//per>=len(longs) else (longs[i//per]>>((i%per)*bits))&mask for i in range(4096)]
            for i,idx in enumerate(idxs):
                if idx>=len(pal): continue
                st=state_string(pal[idx])
                if st.startswith(("air","cave_air","void_air")): continue
                x=cx*16+(i&15); y=sy*16+(i>>8); z=cz*16+((i>>4)&15)
                if x1<=x<=x2 and y1<=y<=y2 and z1<=z<=z2:
                    out.append((y,x,z,st))
    for y,x,z,st in sorted(out):
        print(f"{x:5d} {y:4d} {z:5d}  {st}")

if __name__=="__main__": main()
