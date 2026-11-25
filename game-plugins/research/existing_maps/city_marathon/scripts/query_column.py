#!/usr/bin/env python3
"""Print one column (top-down blocks) and local slices."""
import io, os, struct, zlib, gzip, sys
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon"


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


class World:
    def __init__(self):
        self.chunks = {}
        for fname in sorted(os.listdir(os.path.join(WORLD, "region"))):
            if not fname.endswith(".mca"): continue
            path = os.path.join(WORLD, "region", fname)
            if os.path.getsize(path) == 0: continue
            for root in read_region(path):
                self.chunks[(int(root.get("xPos",0)), int(root.get("zPos",0)))] = root
    def column(self, x, z):
        root = self.chunks.get((x//16, z//16))
        if root is None: return []
        out=[]
        for section in root.get("sections", []):
            sy=int(section.get("Y",0))
            bs=section.get("block_states")
            if not bs: continue
            pal=bs.get("palette", [])
            if not pal: continue
            if len(pal)==1:
                idx=0
            else:
                data=bs.get("data")
                if data is None: continue
                n=len(pal); bits=max(4,(n-1).bit_length()); per=64//bits; mask=(1<<bits)-1
                longs=[int(v)&0xFFFFFFFFFFFFFFFF for v in data]
                i=(x%16) + ((z%16)<<4) + ((sy*16 & 0)*0)
                # local index y bits come from by
                # We'll compute inside loop below instead
                idx=None
            for by in range(16):
                y=sy*16+by
                if len(pal)==1:
                    ii=0
                else:
                    i=(x%16) + (by*256) + ((z%16)<<4)
                    li=i//per
                    if li>=len(longs): continue
                    ii=(longs[li]>>((i%per)*bits))&mask
                if ii>=len(pal): continue
                nm=str(pal[ii].get("Name","minecraft:air")).replace("minecraft:","")
                if nm in ("air","cave_air","void_air"): continue
                out.append((y,nm))
        return out


if __name__=="__main__":
    w=World()
    print("chunks",len(w.chunks))
    for pair in sys.argv[1:]:
        x,z=map(int,pair.split(","))
        print("COLUMN",x,z)
        for y,nm in w.column(x,z): print("  ",y,nm)
