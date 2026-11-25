#!/usr/bin/env python3
"""Scan selected block states with coordinates from the City Marathon world."""
import io, os, struct, zlib, gzip, json
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"

WANT = (
    "command_block", "chain_command_block", "repeating_command_block",
    "structure_block", "jigsaw", "barrier", "light",
    "polished_diorite", "yellow_glazed_terracotta", "orange_wool", "lime_wool",
    "red_glazed_terracotta", "pink_glazed_terracotta", "magenta_glazed_terracotta",
    "yellow_carpet", "green_carpet", "yellow_concrete", "gray_wool",
    "elytra", "chest", "trapped_chest", "barrel", "shulker_box",
    "oak_sign", "oak_wall_sign", "sign", "banner", "white_banner",
    "end_gateway", "end_portal", "nether_portal", "dragon_egg",
    "beacon", "conduit", "lodestone", "respawn_anchor",
    "redstone_block", "lever", "stone_button", "oak_button",
    "pressure_plate", "stone_pressure_plate", "oak_pressure_plate",
    "tripwire_hook", "tripwire", "observer", "repeater", "comparator",
)


def read_region(path):
    with open(path, "rb") as f:
        header = f.read(4096)
    for i in range(1024):
        off = int.from_bytes(header[i*4:i*4+3], "big")
        if off == 0:
            continue
        with open(path, "rb") as f:
            f.seek(off*4096)
            ln = struct.unpack(">I", f.read(4))[0]
            comp = f.read(1)[0]
            raw = f.read(ln-1)
        if comp == 1: raw = gzip.decompress(raw)
        elif comp == 2: raw = zlib.decompress(raw)
        else: continue
        try:
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue


def state_string(p):
    name = str(p.get("Name", "minecraft:air"))
    props = p.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={props[k]}" for k in sorted(props.keys())) + "]"


def main():
    found = []
    minx=miny=minz=10**9; maxx=maxy=maxz=-10**9
    for fname in sorted(os.listdir(os.path.join(WORLD, "region"))):
        if not fname.endswith(".mca"): continue
        path = os.path.join(WORLD, "region", fname)
        if os.path.getsize(path) == 0: continue
        for root in read_region(path):
            cx = int(root.get("xPos", 0)); cz = int(root.get("zPos", 0))
            for section in root.get("sections", []):
                sy = int(section.get("Y", 0))
                bs = section.get("block_states")
                if not bs: continue
                pal = bs.get("palette", [])
                if not pal: continue
                states = [state_string(p) for p in pal]
                if len(pal) == 1:
                    if states[0].startswith("minecraft:air"): continue
                    data = None
                    idxs = [0]*4096
                else:
                    data = bs.get("data")
                    if data is None: continue
                    n = len(pal); bits = max(4, (n-1).bit_length()); per = 64//bits; mask=(1<<bits)-1
                    longs = [int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                    idxs = []
                    for i in range(4096):
                        li = i//per
                        if li >= len(longs): idxs.append(0); continue
                        idxs.append((longs[li] >> ((i%per)*bits)) & mask)
                for i, idx in enumerate(idxs):
                    if idx >= len(states): continue
                    st = states[idx]
                    base = st.split("[")[0].replace("minecraft:", "")
                    if base in ("air", "cave_air", "void_air"): continue
                    x = cx*16 + (i & 15); y = sy*16 + (i >> 8); z = cz*16 + ((i >> 4) & 15)
                    if x<minx: minx=x
                    if y<miny: miny=y
                    if z<minz: minz=z
                    if x>maxx: maxx=x
                    if y>maxy: maxy=y
                    if z>maxz: maxz=z
                    if base in WANT:
                        found.append((x,y,z,st,fname))
    print("bounds", minx,miny,minz,"->",maxx,maxy,maxz,"blocks", len(found))
    with open(os.path.join(OUT, "selected_blocks.json"), "w", encoding="utf-8") as f:
        json.dump(found, f)
    import collections
    print(collections.Counter(x[3].split("[")[0] for x in found).most_common())


if __name__ == "__main__":
    main()
