import nbtlib, io, zlib, gzip, os
world=r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
region_dir=os.path.join(world,"region")
targets={(1965,24,0),(2035,24,0),(2000,24,35),(2000,24,-35),(1974,26,-26),(1974,26,26),(2026,26,-26),(2026,26,26),(1974,14,-26),(1974,14,26),(2026,14,-26),(2026,14,26),(2000,29,0)}
found={}
for fname in os.listdir(region_dir):
    if not fname.endswith(".mca"): continue
    data=open(os.path.join(region_dir,fname),"rb").read()
    if len(data)<4096: continue
    for idx in range(1024):
        entry=data[idx*4:idx*4+4]
        if len(entry)<4: continue
        off=(entry[0]<<16)|(entry[1]<<8)|entry[2]
        if not off: continue
        pos=off*4096
        if pos+5>len(data): continue
        length=int.from_bytes(data[pos:pos+4],"big"); comp=data[pos+4]
        raw=data[pos+5:pos+4+length]
        try:
            if comp==2: raw=zlib.decompress(raw)
            elif comp==1: raw=gzip.decompress(raw)
            elif comp!=3: continue
            nbt=nbtlib.File.parse(io.BytesIO(raw))
        except Exception: continue
        lv=nbt.get("Level",nbt)
        for te in lv.get("TileEntities",[]):
            tid=str(te.get("id",""))
            if tid in ("minecraft:chest","minecraft:barrel","minecraft:shulker_box"):
                x,y,z=int(te.get("x",0)),int(te.get("y",0)),int(te.get("z",0))
                if (x,y,z) in targets:
                    items=[]
                    for it in te.get("Items",[]):
                        items.append((int(it.get("Slot",0)),str(it.get("id","")),int(it.get("Count",1)),str(it.get("tag",""))))
                    found[(x,y,z)]=(tid,items)
for t in sorted(found,key=lambda p:(-p[1],p[0],p[2])):
    print(t,found[t][0],found[t][1])
print("found",len(found),"of",len(targets))
