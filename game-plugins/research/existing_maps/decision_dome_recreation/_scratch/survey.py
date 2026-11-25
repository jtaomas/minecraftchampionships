import io, os, sys, zlib, gzip, math
from collections import Counter
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"
region_dir = os.path.join(world, "region")

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

def read_chunks(path):
    with open(path, "rb") as fh:
        data = fh.read()
    if len(data) < 8192:
        return
    for idx in range(1024):
        entry = data[idx*4:idx*4+4]
        off = (entry[0]<<16)|(entry[1]<<8)|entry[2]
        if not off:
            continue
        pos = off*4096
        if pos+5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos+4], "big")
        comp = data[pos+4]
        raw = data[pos+5:pos+4+length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            elif comp != 3:
                continue
            yield idx, nbtlib.File.parse(io.BytesIO(raw))
        except Exception as e:
            print("ERR", path, idx, e, file=sys.stderr)

def decode_section(section, no_cross=True):
    pal = section.get("Palette")
    if pal is None:
        return
    names = []
    for p in pal:
        name = str(p.get("Name"))
        props = p.get("Properties")
        if props:
            name += "[" + ",".join(f"{k}={v}" for k,v in props.items()) + "]"
        names.append(name)
    states = section.get("BlockStates")
    if states is None:
        for i in range(4096):
            yield i, names[0]
        return
    bits = max(4, (len(names)-1).bit_length())
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in states]
    mask = (1<<bits)-1
    if no_cross:
        per = 64//bits
        for i in range(4096):
            li = i//per
            if li >= len(longs): break
            val = (longs[li] >> ((i%per)*bits)) & mask
            if val < len(names):
                yield i, names[val]
    else:
        for i in range(4096):
            bit = i*bits
            li = bit//64
            if li+1 >= len(longs): break
            off = bit % 64
            val = (longs[li] >> off) | (longs[li+1] << (64-off) if off else 0)
            val &= mask
            if val < len(names):
                yield i, names[val]

counts = Counter()
chunk_count = 0
sect_count = 0
bounds = [10**9, 10**9, 10**9, -10**9, -10**9, -10**9]
chunks = []
tile_entities = []
for fname in sorted(os.listdir(region_dir)):
    if not fname.endswith(".mca"): continue
    path = os.path.join(region_dir, fname)
    if os.path.getsize(path) == 0: continue
    for idx, f in read_chunks(path):
        lvl = f.get("Level")
        if lvl is None: continue
        cx, cz = int(lvl["xPos"]), int(lvl["zPos"])
        chunk_count += 1
        chunks.append((cx, cz))
        for s in lvl.get("Sections", []):
            pal = s.get("Palette")
            if pal is None or len(pal) == 1 and str(pal[0].get("Name")) in AIR:
                continue
            sect_count += 1
            sy = int(s["Y"])
            n = 0
            for i, name in decode_section(s):
                if name in AIR: continue
                bx = cx*16 + (i & 15)
                by = sy*16 + (i >> 8)
                bz = cz*16 + ((i >> 4) & 15)
                counts[name] += 1
                n += 1
                if bx < bounds[0]: bounds[0]=bx
                if by < bounds[1]: bounds[1]=by
                if bz < bounds[2]: bounds[2]=bz
                if bx > bounds[3]: bounds[3]=bx
                if by > bounds[4]: bounds[4]=by
                if bz > bounds[5]: bounds[5]=bz
        for te in lvl.get("TileEntities", []):
            tile_entities.append((str(te.get("id")), int(te.get("x")), int(te.get("y")), int(te.get("z"))))

print("chunks:", chunk_count, "nonempty sections:", sect_count)
print("chunk bounds:", min(c[0] for c in chunks), max(c[0] for c in chunks), min(c[1] for c in chunks), max(c[1] for c in chunks))
print("block bounds:", bounds)
print("total blocks:", sum(counts.values()))
print("unique block states:", len(counts))
print("tile entities:", len(tile_entities), Counter(t[0] for t in tile_entities).most_common(20))
print("top 40:")
for name, n in counts.most_common(40):
    print(f"{n:10d} {name}")
