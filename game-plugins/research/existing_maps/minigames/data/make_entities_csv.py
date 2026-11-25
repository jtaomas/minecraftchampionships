import os, io, zlib, gzip as gz, csv
import nbtlib

BASE = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"
OUT = os.path.join(os.path.dirname(__file__), "entities.csv")
rows = []
ed = os.path.join(BASE, "entities")
for fn in sorted(os.listdir(ed)):
    if not fn.endswith(".mca"):
        continue
    data = open(os.path.join(ed, fn), "rb").read()
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
        lv = ch.get("Level", ch)
        for ent in lv.get("Entities", []):
            p = ent.get("Pos")
            rows.append((str(ent.get("id", "?")), round(float(p[0]), 2), round(float(p[1]), 2),
                         round(float(p[2]), 2), fn))
with open(OUT, "w", encoding="utf-8", newline="") as fh:
    fh.write("type,x,y,z,region_file\n")
    for r in rows:
        fh.write(",".join(str(v) for v in r) + "\n")
print("wrote", OUT, len(rows), "entities")
