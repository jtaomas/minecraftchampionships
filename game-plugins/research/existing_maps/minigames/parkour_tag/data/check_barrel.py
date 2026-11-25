import gzip, io, os, zlib
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"

def read_chunk(data, index):
    if len(data) < 4096:
        return None
    e = data[index * 4:index * 4 + 4]
    off = (e[0] << 16) | (e[1] << 8) | e[2]
    if not off:
        return None
    pos = off * 4096
    ln = int.from_bytes(data[pos:pos + 4], "big")
    comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + ln]
    if comp == 2:
        raw = zlib.decompress(raw)
    elif comp == 1:
        raw = gz.decompress(raw)
    elif comp != 3:
        return None
    return nbtlib.File.parse(io.BytesIO(raw))

data = open(os.path.join(WORLD, "region", "r.2.1.mca"), "rb").read()
for idx in range(1024):
    ch = read_chunk(data, idx)
    if not ch:
        continue
    lv = ch.get("Level", ch)
    for te in lv.get("TileEntities", []):
        x, y, z = int(te.get("x")), int(te.get("y")), int(te.get("z"))
        if (x, y, z) == (1067, 12, 992):
            print("found", te)
