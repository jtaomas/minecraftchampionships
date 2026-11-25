import io, os, zlib, gzip as gz, nbtlib
from collections import Counter

BASE = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"


def read_chunk(data, idx):
    e = data[idx*4:idx*4+4]
    if len(e) < 4:
        return None
    off = (e[0] << 16) | (e[1] << 8) | e[2]
    if not off:
        return None
    pos = off * 4096
    ln = int.from_bytes(data[pos:pos+4], "big")
    comp = data[pos+4]
    raw = data[pos+5:pos+4+ln]
    try:
        if comp == 2:
            raw = zlib.decompress(raw)
        elif comp == 1:
            raw = gz.decompress(raw)
        else:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


for rn in ["r.2.1.mca", "r.-1.1.mca", "r.9.-1.mca", "r.40.38.mca"]:
    data = open(os.path.join(BASE, "region", rn), "rb").read()
    for idx in range(1024):
        ch = read_chunk(data, idx)
        if not ch:
            continue
        lv = ch.get("Level", ch)
        cx, cz = int(lv.get("xPos", 0)), int(lv.get("zPos", 0))
        for s in lv.get("Sections", []):
            pal = s.get("Palette")
            if not pal or len(pal) < 2:
                continue
            names = [str(p.get("Name")) for p in pal]
            nonair = [n for n in names if "air" not in n]
            if not nonair:
                continue
            st = s.get("BlockStates")
            vals = [int(v) for v in st] if st is not None else []
            nz = sum(1 for v in vals if v)
            print(f"{rn} chunk({cx},{cz}) Y{int(s['Y'])} pal={len(pal)} longs={len(vals)} nonzero_longs={nz} "
                  f"nonair={nonair[:4]}")
            print("   first longs", vals[:6])
            break
