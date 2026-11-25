import io, os, sys, zlib, gzip, math, csv
from collections import Counter, defaultdict
import nbtlib

world = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation"
region_dir = os.path.join(world, "region")
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

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
            print("ERR", path, idx, e, file=sys.stderr)

def decode(section):
    pal = section.get("Palette")
    if pal is None: return
    names = []
    for p in pal:
        name = str(p.get("Name"))
        props = p.get("Properties")
        if props:
            name += "[" + ",".join(f"{k}={v}" for k,v in props.items()) + "]"
        names.append(name)
    states = section.get("BlockStates")
    if states is None:
        for i in range(4096): yield i, names[0]
        return
    bits = max(4, (len(names)-1).bit_length())
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in states]
    mask = (1<<bits)-1; per = 64//bits
    for i in range(4096):
        li = i//per
        if li >= len(longs): break
        v = (longs[li] >> ((i%per)*bits)) & mask
        if v < len(names): yield i, names[v]

blocks = []
for fname in sorted(os.listdir(region_dir)):
    if not fname.endswith(".mca"): continue
    path = os.path.join(region_dir, fname)
    if os.path.getsize(path) == 0: continue
    for idx, f in read_chunks(path):
        lvl = f.get("Level")
        if lvl is None: continue
        cx, cz = int(lvl["xPos"]), int(lvl["zPos"])
        for s in lvl.get("Sections", []):
            if s.get("Palette") is None: continue
            sy = int(s["Y"])
            for i, name in decode(s):
                if name in AIR: continue
                x = cx*16 + (i & 15); y = sy*16 + (i >> 8); z = cz*16 + ((i >> 4) & 15)
                blocks.append((x,y,z,name))

print("blocks", len(blocks))
xs=[b[0] for b in blocks]; ys=[b[1] for b in blocks]; zs=[b[2] for b in blocks]
print("bounds", min(xs),min(ys),min(zs), max(xs),max(ys),max(zs))

# per-y profile
byy = defaultdict(list)
for x,y,z,n in blocks: byy[y].append(n)
print("\nY profile: y count top materials")
for y in sorted(byy):
    c = Counter(n.split("[")[0] for n in byy[y])
    print(f"{y:4d} {len(byy[y]):5d} " + ", ".join(f"{k.replace('minecraft:','')}:{v}" for k,v in c.most_common(6)))

# color pods bbox
print("\nColored concrete bboxes+centroid:")
for word in ["red","orange","yellow","lime","green","cyan","light_blue","blue","purple","pink","white","gray","black","light_gray"]:
    sel = [b for b in blocks if b[3].startswith(f"minecraft:{word}_concrete")]
    if not sel: continue
    sx=sum(b[0] for b in sel)/len(sel); sy=sum(b[1] for b in sel)/len(sel); sz=sum(b[2] for b in sel)/len(sel)
    print(f"{word:11s} n={len(sel):5d} bbox x{min(b[0] for b in sel)}..{max(b[0] for b in sel)} y{min(b[1] for b in sel)}..{max(b[1] for b in sel)} z{min(b[2] for b in sel)}..{max(b[2] for b in sel)} centroid {sx:.1f},{sy:.1f},{sz:.1f}")

# gray concrete radius profile by y (assume center later fitted)
print("\nGray concrete footprint extents by y:")
for y in sorted(set(b[1] for b in blocks if b[3].startswith("minecraft:gray_concrete"))):
    sel=[b for b in blocks if b[1]==y and b[3].startswith("minecraft:gray_concrete")]
    print(f"y={y}: n={len(sel)} x{min(b[0] for b in sel)}..{max(b[0] for b in sel)} z{min(b[2] for b in sel)}..{max(b[2] for b in sel)}")

# save csv for later
with open(r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\decision_dome_recreation\_scratch\blocks_raw.csv","w",newline="",encoding="utf-8") as fh:
    w=csv.writer(fh); w.writerow(["x","y","z","block"])
    w.writerows(blocks)
