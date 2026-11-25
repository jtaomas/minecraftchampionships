import io, os, sys, zlib, gzip, json
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"
region_dir = os.path.join(world, "region")

def read_chunk_header(fh):
    fh.seek(0)
    locations = fh.read(4096)

for fname in sorted(os.listdir(region_dir)):
    if not fname.endswith(".mca"):
        continue
    path = os.path.join(region_dir, fname)
    print("===", fname, os.path.getsize(path))
    if os.path.getsize(path) == 0:
        continue
    with open(path, "rb") as fh:
        data = fh.read()
    # first chunk
    for idx in range(1024):
        entry = data[idx*4:idx*4+4]
        off = (entry[0]<<16)|(entry[1]<<8)|entry[2]
        if not off:
            continue
        pos = off*4096
        length = int.from_bytes(data[pos:pos+4], "big")
        comp = data[pos+4]
        raw = data[pos+5:pos+4+length]
        if comp == 2:
            raw = zlib.decompress(raw)
        elif comp == 1:
            raw = gzip.decompress(raw)
        f = nbtlib.File.parse(io.BytesIO(raw))
        print("root keys:", list(f.keys()))
        print("DataVersion", f.get("DataVersion"))
        if "Level" in f:
            lvl = f["Level"]
            print("Level keys:", list(lvl.keys()))
            print("xPos,zPos", lvl.get("xPos"), lvl.get("zPos"))
            sections = lvl.get("Sections", [])
            print("sections", len(sections))
            if sections:
                s = sections[0]
                print("section keys:", list(s.keys()))
                print("section Y:", s.get("Y"))
                pal = s.get("Palette")
                if pal is not None:
                    print("palette len", len(pal), "first", pal[:2])
                print("BlockStates present:", s.get("BlockStates") is not None)
        else:
            print("keys:", list(f.keys()))
        break
    break
