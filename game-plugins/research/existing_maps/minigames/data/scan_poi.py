import os, io, zlib, gzip as gz
from collections import Counter
import nbtlib

BASE = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
pd = os.path.join(BASE, "poi")
counts = Counter()
files = 0
chunks = 0
for fn in sorted(os.listdir(pd)):
    if not fn.endswith(".mca"):
        continue
    files += 1
    data = open(os.path.join(pd, fn), "rb").read()
    if len(data) < 4096:
        continue
    for idx in range(1024):
        e = data[idx * 4:idx * 4 + 4]
        off = (e[0] << 16) | (e[1] << 8) | e[2]
        if not off:
            continue
        pos = off * 4096
        ln = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        raw = data[pos + 5:pos + 4 + ln]
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
        chunks += 1
        for sec in ch.get("Sections", {}).values():
            for rec in sec.get("Records", []):
                counts[str(rec.get("type", "?"))] += 1
print("poi files", files, "chunks", chunks)
for k, v in counts.most_common(20):
    print(k, v)
