import nbtlib, io, zlib, gzip, os, csv, re
world=r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
region_dir=os.path.join(world,"region")
targets={(1965,24,0),(2035,24,0),(2000,24,35),(2000,24,-35),(1974,26,-26),(1974,26,26),(2026,26,-26),(2026,26,26),(1974,14,-26),(1974,14,26),(2026,14,-26),(2026,14,26),(2000,29,0)}
idmap={}
for r in csv.DictReader(open("data/chests.csv")):
    idmap[(int(r["x"]),int(r["y"]),int(r["z"]))]=r["id"]
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
                    for it in te.get("Items",[]):
                        tag=str(it.get("tag",""))
                        summary=[]
                        if "CanPlaceOn" in tag: summary.append("CanPlaceOn 38")
                        m=re.search(r"Potion': String\('minecraft:([a-z_]+)'\)",tag)
                        if m: summary.append("potion="+m.group(1))
                        if "Enchantments" in tag: summary.append("Loyalty I")
                        if "Damage" in tag: summary.append("Damage 0")
                        found[(x,y,z,int(it.get("Slot",0)))]=(str(it.get("id","")).replace("minecraft:",""),int(it.get("Count",1)),"; ".join(summary))
rows=[]
for (x,y,z,slot),(item,count,summary) in sorted(found.items(),key=lambda kv:(idmap[kv[0][:3]],kv[0][3])):
    rows.append(dict(container_id=idmap[(x,y,z)],x=x,y=y,z=z,slot=slot,item=item,count=count,source_nbt=summary or "-"))
with open("data/container_nbt.csv","w",newline="",encoding="utf-8") as f:
    w=csv.DictWriter(f,fieldnames=["container_id","x","y","z","slot","item","count","source_nbt"]); w.writeheader(); w.writerows(rows)
print("wrote",len(rows))
