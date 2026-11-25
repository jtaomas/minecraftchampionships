#!/usr/bin/env python3
"""Read SkyLight / BlockLight at sample points from the Pits region files."""
import gzip
import io
import os
import sys
import zlib

import nbtlib

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat"
REG = os.path.join(SRC, "region")


def read_chunk(data, index):
    e = data[index * 4:index * 4 + 4]
    off = (e[0] << 16) | (e[1] << 8) | e[2]
    if not off:
        return None
    pos = off * 4096
    length = int.from_bytes(data[pos:pos + 4], "big")
    comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + length]
    if comp == 2:
        raw = zlib.decompress(raw)
    elif comp == 1:
        raw = gzip.decompress(raw)
    elif comp != 3:
        return None
    return nbtlib.File.parse(io.BytesIO(raw))


def nibble(arr, idx):
    b = int(arr[idx >> 1]) & 0xFF
    return (b >> 4) & 0xF if idx & 1 else b & 0xF


def main():
    # world points to sample
    pts = [
        (15, -1, 36, "start head"),
        (22, -2, 36, "road A head"),
        (42, -6, 36, "road B head"),
        (65, -9, 36, "finish pit mouth"),
        (65, -10, 36, "finish chicken"),
        (65, -63, 36, "pit above carpet"),
        (65, -64, 36, "pit floor shroomlight"),
        (65, -43, 36, "above fake glass bottom"),
        (22, 4, 36, "above road A, below ceiling"),
        (22, 28, 36, "just below ceiling"),
        (5, 5, 30, "park top"),
        (70, 5, 30, "north building roof"),
        (70, 1, 80, "south plaza"),
        (0, 0, 0, "corner void"),
    ]
    chunks = {}
    for fname in os.listdir(REG):
        if not fname.endswith(".mca"):
            continue
        data = open(os.path.join(REG, fname), "rb").read()
        for idx in range(1024):
            c = read_chunk(data, idx)
            if c:
                chunks[(int(c.get("xPos", 0)), int(c.get("zPos", 0)))] = c
    print("chunks loaded", len(chunks))
    for x, y, z, label in pts:
        c = chunks.get((x >> 4, z >> 4))
        if not c:
            print(f"{label:28s} ({x},{y},{z}) no chunk")
            continue
        sec = None
        for s in c.get("sections", []):
            if int(s.get("Y", 0)) == (y >> 4):
                sec = s
                break
        if sec is None:
            print(f"{label:28s} ({x},{y},{z}) no section")
            continue
        local = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15)
        sky = nibble(sec.get("SkyLight", [0] * 2048), local) if "SkyLight" in sec else None
        blk = nibble(sec.get("BlockLight", [0] * 2048), local) if "BlockLight" in sec else None
        print(f"{label:28s} ({x:4d},{y:4d},{z:4d}) sky={sky} block={blk} isLightOn={sec.get('isLightOn')}")


if __name__ == "__main__":
    main()
