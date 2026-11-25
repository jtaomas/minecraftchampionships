#!/usr/bin/env python3
"""Read-only top-down surface map of the downloaded Clouds 2 world.

Scans every section (not the Heightmaps, which are unreliable in this world)
and keeps the highest non-air block per column. Visual orientation only; A1
owns the full block inventory. Writes only into video_clouds2/.
"""
import io
import os
import sys
import zlib
import gzip
from collections import Counter

import nbtlib
from PIL import Image, ImageDraw

sys.path.insert(0, r"C:\Users\LeonS\Desktop\Mcc\tools")
import render_preview as rp  # read-only shared colour table

WORLD = (r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps"
         r"\Clouds_2_-_Electric_Boogaloo__Download_Version_"
         r"\Clouds 2 - Electric Boogaloo")
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\video_clouds2"

MIN_Y = -64
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunks(path):
    data = open(path, "rb").read()
    for idx in range(1024):
        e = data[idx * 4: idx * 4 + 4]
        if len(e) < 4:
            continue
        off = (e[0] << 16) | (e[1] << 8) | e[2]
        if not off:
            continue
        pos = off * 4096
        if pos + 5 > len(data):
            continue
        ln = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        raw = data[pos + 5: pos + 4 + ln]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            elif comp != 3:
                continue
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue


def main():
    region_dir = os.path.join(WORLD, "region")
    pixels = {}
    minx = minz = 10 ** 9
    maxx = maxz = -10 ** 9
    topcount = Counter()
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    for fi, fn in enumerate(files):
        for chunk in read_chunks(os.path.join(region_dir, fn)):
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            for s in chunk["sections"]:
                bs = s.get("block_states")
                if not bs:
                    continue
                pal = bs.get("palette", [])
                if not pal:
                    continue
                names = [str(x.get("Name", "minecraft:air")) for x in pal]
                if all(n in AIR for n in names):
                    continue
                sy = int(s.get("Y", 0))
                data = bs.get("data")
                if data is None:
                    # single-state section
                    n = names[0]
                    if n in AIR:
                        continue
                    for i in range(4096):
                        x = cx * 16 + (i & 15)
                        z = cz * 16 + (i >> 4 & 15)
                        y = sy * 16 + (i >> 8)
                        if (x, z) not in pixels or pixels[(x, z)][1] < y:
                            pixels[(x, z)] = (n, y)
                            topcount[n] += 1
                            minx = min(minx, x); maxx = max(maxx, x)
                            minz = min(minz, z); maxz = max(maxz, z)
                    continue
                bits = max(4, (len(pal) - 1).bit_length())
                per = 64 // bits
                mask = (1 << bits) - 1
                for i in range(4096):
                    li = i // per
                    if li >= len(data):
                        break
                    v = (int(data[li]) >> ((i % per) * bits)) & mask
                    n = names[v] if v < len(names) else "minecraft:air"
                    if n in AIR:
                        continue
                    x = cx * 16 + (i & 15)
                    z = cz * 16 + (i >> 4 & 15)
                    y = sy * 16 + (i >> 8)
                    if (x, z) not in pixels or pixels[(x, z)][1] < y:
                        pixels[(x, z)] = (n, y)
                        topcount[n] += 1
                        minx = min(minx, x); maxx = max(maxx, x)
                        minz = min(minz, z); maxz = max(maxz, z)
        print(f"{fi+1}/{len(files)} {fn}: {len(pixels)} cols, bounds {minx},{minz}..{maxx},{maxz}", flush=True)

    W = maxx - minx + 1
    H = maxz - minz + 1
    scale = 2
    img = Image.new("RGB", (W * scale, H * scale), (222, 232, 244))
    px = img.load()
    for (x, z), (name, y) in pixels.items():
        top, side, alpha, emit, solid, strip = rp.guess_block(name)
        col = top[:3] if len(top) == 4 else top
        shade = 0.72 + min(0.45, max(-0.18, (y - 4) / 90.0))
        col = tuple(max(0, min(255, int(c * shade))) for c in col)
        ox = (x - minx) * scale
        oz = (z - minz) * scale
        for dx in range(scale):
            for dz in range(scale):
                px[ox + dx, oz + dz] = col
    d = ImageDraw.Draw(img)
    step = 64
    for gx in range((minx // step) * step, maxx + 1, step):
        ox = (gx - minx) * scale
        d.line([(ox, 0), (ox, H * scale)], fill=(90, 110, 140), width=1)
        d.text((ox + 3, 4), str(gx), fill=(40, 50, 70))
    for gz in range((minz // step) * step, maxz + 1, step):
        oz = (gz - minz) * scale
        d.line([(0, oz), (W * scale, oz)], fill=(90, 110, 140), width=1)
        d.text((3, oz + 2), str(gz), fill=(40, 50, 70))
    d.text((W * scale - 120, H * scale - 18), "N is up (-Z)", fill=(20, 30, 50))
    os.makedirs(os.path.join(OUT, "renders"), exist_ok=True)
    out = os.path.join(OUT, "renders", "world_topdown.png")
    img.save(out)
    with open(os.path.join(OUT, "data", "world_top_blocks.csv"), "w", encoding="utf-8") as fh:
        fh.write("block,columns\n")
        for name, n in topcount.most_common():
            fh.write(f"{name},{n}\n")
    print("bounds", minx, minz, maxx, maxz, "columns", len(pixels), "->", out)


if __name__ == "__main__":
    main()
