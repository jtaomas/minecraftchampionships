import io, os, zlib, gzip as gz
from collections import Counter
import nbtlib

base = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
rd = os.path.join(base, "region")
cnt = Counter()
pal_sizes = Counter()
chunks_with_data = 0
chunks_with_sections = 0
sections_total = 0
for fn in sorted(os.listdir(rd)):
    if not fn.endswith(".mca"):
        continue
    data = open(os.path.join(rd, fn), "rb").read()
    if len(data) < 4096:
        continue
    for idx in range(1024):
        e = data[idx*4:idx*4+4]
        off = (e[0] << 16) | (e[1] << 8) | e[2]
        if not off:
            continue
        pos = off * 4096
        length = int.from_bytes(data[pos:pos+4], "big")
        comp = data[pos+4]
        raw = data[pos+5:pos+4+length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gz.decompress(raw)
            else:
                continue
            ch = nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue
        lv = ch.get("Level", ch)
        secs = lv.get("Sections", [])
        if not secs:
            continue
        chunks_with_sections += 1
        has = False
        for s in secs:
            pal = s.get("Palette")
            if not pal:
                continue
            sections_total += 1
            st = s.get("BlockStates")
            ln = 0 if st is None else len(st)
            cnt["nonempty" if ln else "empty"] += 1
            pal_sizes[min(len(pal), 40)] += 1
            if ln:
                has = True
        if has:
            chunks_with_data += 1
print("chunks with sections", chunks_with_sections, "with any block data", chunks_with_data)
print("sections", sections_total, cnt)
print("palette sizes", sorted(pal_sizes.items())[:20])
