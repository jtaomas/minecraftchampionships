import os, io, zlib, gzip as gz, nbtlib
from collections import Counter

BASE = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
ed = os.path.join(BASE, "entities")
print("entity region files:", len([f for f in os.listdir(ed) if f.endswith('.mca')]))
counts = Counter()
samples = []
for fn in sorted(os.listdir(ed)):
    if not fn.endswith(".mca"):
        continue
    data = open(os.path.join(ed, fn), "rb").read()
    if len(data) < 4096:
        continue
    for idx in range(1024):
        e = data[idx*4:idx*4+4]
        off = (e[0] << 16) | (e[1] << 8) | e[2]
        if not off:
            continue
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
                continue
            ch = nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue
        lv = ch.get("Level", ch)
        ents = lv.get("Entities", [])
        for ent in ents:
            tid = str(ent.get("id", "?"))
            counts[tid] += 1
            if len(samples) < 12:
                posl = ent.get("Pos")
                samples.append((tid, [round(float(x), 1) for x in posl] if posl else None))
print("entities:", sum(counts.values()))
for k, v in counts.most_common(40):
    print(f"  {k}: {v}")
print("samples", samples)
