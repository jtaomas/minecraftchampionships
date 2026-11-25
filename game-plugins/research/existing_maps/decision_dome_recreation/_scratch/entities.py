import io, os, zlib, gzip
import nbtlib
from collections import Counter

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"
for dim in ["", "DIM-1", "DIM1"]:
    print("==", dim or "overworld")
    ent = os.path.join(world, dim, "entities") if dim else os.path.join(world, "entities")
    if not os.path.isdir(ent):
        print(" no entities dir"); continue
    files=[f for f in os.listdir(ent) if f.endswith(".mca")]
    print(" files:", files)
    types=Counter(); total=0
    for fn in files:
        p=os.path.join(ent,fn)
        if os.path.getsize(p)==0: continue
        data=open(p,"rb").read()
        for idx in range(1024):
            e=data[idx*4:idx*4+4]
            off=(e[0]<<16)|(e[1]<<8)|e[2]
            if not off: continue
            pos=off*4096; ln=int.from_bytes(data[pos:pos+4],"big"); comp=data[pos+4]
            raw=data[pos+5:pos+4+ln]
            try:
                if comp==2: raw=zlib.decompress(raw)
                elif comp==1: raw=gzip.decompress(raw)
                elif comp!=3: continue
                f=nbtlib.File.parse(io.BytesIO(raw))
            except Exception: continue
            for ent_ in f.get("Entities", []):
                types[str(ent_.get("id"))]+=1; total+=1
    print(" entities:", total, dict(types))

# chunk counts per region and dimension
for dim in ["", "DIM-1", "DIM1"]:
    reg = os.path.join(world, dim, "region") if dim else os.path.join(world, "region")
    print("==", dim or "overworld", "region")
    if not os.path.isdir(reg): print(" none"); continue
    for fn in sorted(os.listdir(reg)):
        if not fn.endswith(".mca"): continue
        p=os.path.join(reg,fn)
        size=os.path.getsize(p)
        n=0
        if size:
            data=open(p,"rb").read()
            for idx in range(1024):
                e=data[idx*4:idx*4+4]
                if ((e[0]<<16)|(e[1]<<8)|e[2]): n+=1
        print(f"  {fn} size={size} chunks={n}")

# check datapacks and playerdata
for sub in ["datapacks","playerdata","advancements","stats","poi"]:
    p=os.path.join(world,sub)
    print(sub, os.listdir(p) if os.path.isdir(p) else "missing")
