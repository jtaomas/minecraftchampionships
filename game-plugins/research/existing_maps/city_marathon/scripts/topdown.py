#!/usr/bin/env python3
"""Top-down orthographic map of City Marathon from region files (read-only)."""
import io, os, struct, zlib, gzip, json, math
from collections import Counter
import nbtlib
from PIL import Image, ImageDraw, ImageFont

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\renders"

COL = {
    "stone": (125,125,125), "stone_bricks": (122,121,122), "mud_bricks": (92,68,58),
    "bricks": (150,84,68), "white_terracotta": (210,178,161), "terracotta": (152,94,67),
    "polished_andesite": (132,134,133), "grass_block": (95,159,53), "granite": (149,103,85),
    "gray_wool": (63,68,72), "white_wool": (233,236,236), "yellow_wool": (248,198,39),
    "brown_wool": (114,71,40), "yellow_glazed_terracotta": (234,192,88),
    "deepslate_tile_slab": (55,55,57), "smooth_stone": (158,158,158),
    "blackstone_slab": (43,40,43), "sandstone": (216,203,155), "andesite": (132,134,133),
    "sea_lantern": (207,224,220), "glass": (200,220,230), "glass_pane": (200,220,230),
    "water": (52,98,190), "moss_block": (89,109,45), "calcite": (223,224,220),
    "dirt_path": (148,121,65), "coarse_dirt": (119,86,59), "rooted_dirt": (144,104,79),
    "mushroom_stem": (203,196,185), "cobblestone_slab": (110,110,110),
    "yellow_concrete": (241,175,21), "lime_wool": (112,185,25),
    "red_glazed_terracotta": (179,74,60), "pink_glazed_terracotta": (213,123,135),
    "magenta_glazed_terracotta": (206,80,180), "orange_wool": (240,118,19),
    "green_carpet": (85,110,27), "yellow_carpet": (232,188,35),
    "barrier": (200,30,30), "light": (255,250,200),
    "polished_diorite": (190,192,194), "iron_bars": (80,80,80),
}


def read_region(path):
    with open(path, "rb") as f:
        header = f.read(4096)
    for i in range(1024):
        off = int.from_bytes(header[i*4:i*4+3], "big")
        if off == 0: continue
        with open(path, "rb") as f:
            f.seek(off*4096)
            ln = struct.unpack(">I", f.read(4))[0]
            comp = f.read(1)[0]
            raw = f.read(ln-1)
        if comp == 1: raw = gzip.decompress(raw)
        elif comp == 2: raw = zlib.decompress(raw)
        else: continue
        try: yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception: pass


def main():
    WANT = None
    height = {}
    topblock = {}
    allblocks = {}
    for fname in sorted(os.listdir(os.path.join(WORLD, "region"))):
        if not fname.endswith(".mca"): continue
        path = os.path.join(WORLD, "region", fname)
        if os.path.getsize(path) == 0: continue
        for root in read_region(path):
            cx = int(root.get("xPos",0)); cz = int(root.get("zPos",0))
            for section in root.get("sections", []):
                sy = int(section.get("Y",0))
                bs = section.get("block_states")
                if not bs: continue
                pal = bs.get("palette", [])
                if not pal: continue
                names = [str(p.get("Name","minecraft:air")).replace("minecraft:","") for p in pal]
                if len(pal) == 1:
                    if names[0] in ("air","cave_air","void_air"): continue
                    idxs = [0]*4096
                else:
                    data = bs.get("data")
                    if data is None: continue
                    n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                    longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                    idxs=[]
                    for i in range(4096):
                        li=i//per
                        idxs.append(0 if li>=len(longs) else (longs[li]>>((i%per)*bits))&mask)
                for i, idx in enumerate(idxs):
                    if idx >= len(names): continue
                    nm = names[idx]
                    if nm in ("air","cave_air","void_air"): continue
                    x = cx*16 + (i & 15); y = sy*16 + (i >> 8); z = cz*16 + ((i >> 4) & 15)
                    allblocks[nm] = allblocks.get(nm,0)+1
                    k = (x,z)
                    if k not in height or y > height[k]:
                        height[k] = y; topblock[k] = nm
    xs=[k[0] for k in height]; zs=[k[1] for k in height]
    minx,maxx,minz,maxz=min(xs),max(xs),min(zs),max(zs)
    print("bounds", minx,maxx,minz,maxz, "cols", len(height))
    scale=4
    img=Image.new("RGB", ((maxx-minx+1)*scale, (maxz-minz+1)*scale), (25,28,34))
    px=img.load()
    for (x,z),y in height.items():
        nm=topblock[(x,z)]
        c=COL.get(nm)
        if c is None:
            h=hash(nm)
            c=(80+(h%140), 80+((h>>8)%140), 80+((h>>16)%140))
        # height shading
        f=0.75 + min(0.45, max(-0.25, (y-0)/90))
        c=tuple(min(255,int(v*f)) for v in c)
        for dx in range(scale):
            for dz in range(scale):
                px[(x-minx)*scale+dx,(z-minz)*scale+dz]=c
    draw=ImageDraw.Draw(img)
    # grid every 50
    for x in range(math.ceil(minx/50)*50, maxx+1, 50):
        X=(x-minx)*scale
        draw.line([(X,0),(X,img.height)], fill=(255,255,255,40), width=1)
        draw.text((X+2,2), str(x), fill=(240,240,240))
    for z in range(math.ceil(minz/50)*50, maxz+1, 50):
        Z=(z-minz)*scale
        draw.line([(0,Z),(img.width,Z)], fill=(255,255,255,40), width=1)
        draw.text((2,Z+1), str(z), fill=(240,240,240))
    img.save(os.path.join(OUT,"topdown_raw.png"))
    print("saved", img.size)
    print(Counter(topblock.values()).most_common(30))


if __name__=="__main__":
    main()
