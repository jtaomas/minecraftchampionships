import io, os, sys, zlib, gzip, json
from collections import Counter
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"

def read_chunks(path):
    with open(path, "rb") as fh:
        data = fh.read()
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
        except Exception as e:
            pass

print("== level.dat ==")
lf = nbtlib.load(os.path.join(world, "level.dat"))
data = lf.get("Data") or lf
for key in ["LevelName","DataVersion","Version","SpawnX","SpawnY","SpawnZ","GameType","Difficulty","hardcore","allowCommands","initialized","Time","DayTime","raining","thundering","generatorName","LastPlayed"]:
    if key in data:
        print(key, "=", data[key])
if "GameRules" in data:
    print("GameRules:", {str(k): str(v) for k,v in data["GameRules"].items()})

print("\n== tile entities ==")
rows=[]
for fname in sorted(os.listdir(os.path.join(world,"region"))):
    if not fname.endswith(".mca"): continue
    path=os.path.join(world,"region",fname)
    if os.path.getsize(path)==0: continue
    for idx,f in read_chunks(path):
        lvl=f.get("Level")
        if lvl is None: continue
        for te in lvl.get("TileEntities", []):
            tid=str(te.get("id"))
            x,y,z = int(te.get("x")),int(te.get("y")),int(te.get("z"))
            extra={}
            for k in ["Color","Base","Rotation","Type","Patterns","Sections","CustomName","Command","auto","conditionMet","powered","facing","SkullType","Rot"]:
                if k in te:
                    v=te[k]
                    try: v=json.loads(json.dumps(v, default=str))
                    except Exception: v=str(v)
                    extra[k]=v
            rows.append((tid,x,y,z,extra))
for r in sorted(rows, key=lambda t:(t[1],t[3],t[2])):
    print(r)
print("counts", Counter(r[0] for r in rows))
