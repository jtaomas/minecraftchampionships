import io, os, zlib, gzip, json
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"

def read_chunks(path):
    with open(path,"rb") as fh: data=fh.read()
    for idx in range(1024):
        e=data[idx*4:idx*4+4]
        off=(e[0]<<16)|(e[1]<<8)|e[2]
        if not off: continue
        pos=off*4096
        if pos+5>len(data): continue
        ln=int.from_bytes(data[pos:pos+4],"big"); comp=data[pos+4]
        raw=data[pos+5:pos+4+ln]
        try:
            if comp==2: raw=zlib.decompress(raw)
            elif comp==1: raw=gzip.decompress(raw)
            elif comp!=3: continue
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass

def conv(v):
    if isinstance(v, dict): return {str(k):conv(x) for k,x in v.items()}
    if isinstance(v,(list,tuple)): return [conv(x) for x in v]
    try:
        if hasattr(v,"item"): return v.item()
    except Exception: pass
    return str(v)

print("== armor stands ==")
for f in read_chunks(os.path.join(world,"entities","r.0.0.mca")):
    for e in f.get("Entities", []):
        if str(e.get("id"))=="minecraft:armor_stand":
            print(json.dumps(conv(e), indent=1, sort_keys=True))

print("\n== datapacks ==")
dp=os.path.join(world,"datapacks")
for name in sorted(os.listdir(dp)):
    p=os.path.join(dp,name)
    if os.path.isdir(p):
        print(name, "DIR", os.listdir(p)[:10])
    else:
        print(name, os.path.getsize(p))

print("\n== player stats summary ==")
st=json.load(open(os.path.join(world,"stats","9ed6d016-4235-458f-b5c9-59b56ef4900e.json")))
print(list(st.keys()))
custom=st.get("stats",{}).get("minecraft:custom",{})
for k in sorted(custom, key=lambda k: -custom[k])[:25]:
    print(k, custom[k])
