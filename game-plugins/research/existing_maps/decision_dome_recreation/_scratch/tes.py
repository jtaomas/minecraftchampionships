import io, os, zlib, gzip, json
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"

def read_chunks(path):
    with open(path, "rb") as fh: data = fh.read()
    if len(data) < 8192: return
    for idx in range(1024):
        e = data[idx*4:idx*4+4]
        off = (e[0]<<16)|(e[1]<<8)|e[2]
        if not off: continue
        pos = off*4096
        if pos+5 > len(data): continue
        ln = int.from_bytes(data[pos:pos+4], "big"); comp = data[pos+4]
        raw = data[pos+5:pos+4+ln]
        try:
            if comp == 2: raw = zlib.decompress(raw)
            elif comp == 1: raw = gzip.decompress(raw)
            elif comp != 3: continue
            yield idx, nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass

def to_py(v):
    if isinstance(v, dict): return {str(k):to_py(x) for k,x in v.items()}
    if isinstance(v, (list,tuple)): return [to_py(x) for x in v]
    try:
        if hasattr(v, "item"): return v.item()
    except Exception: pass
    return str(v)

skulls=[]; banners=[]
for fname in sorted(os.listdir(os.path.join(world,"region"))):
    if not fname.endswith(".mca"): continue
    p=os.path.join(world,"region",fname)
    if os.path.getsize(p)==0: continue
    for idx,f in read_chunks(p):
        lvl=f.get("Level")
        if lvl is None: continue
        for te in lvl.get("TileEntities", []):
            tid=str(te.get("id"))
            if tid=="minecraft:skull":
                d=to_py(te)
                skulls.append(d)
            elif tid=="minecraft:banner":
                banners.append(to_py(te))
print("=== SKULLS ===")
for s in sorted(skulls,key=lambda d:(d.get("x"),d.get("z"),d.get("y"))):
    print(json.dumps(s,sort_keys=True))
print("\n=== BANNERS ===")
for s in sorted(banners,key=lambda d:(d.get("x"),d.get("z"),d.get("y"))):
    print(json.dumps(s,sort_keys=True))
