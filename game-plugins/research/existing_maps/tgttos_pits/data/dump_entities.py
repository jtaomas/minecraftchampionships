#!/usr/bin/env python3
"""Dump full NBT of the Pits entities (world entities/*.mca)."""
import gzip
import io
import json
import os
import zlib

import nbtlib

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat\entities"
OUT = os.path.dirname(os.path.abspath(__file__))


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    e = data[index * 4:index * 4 + 4]
    if len(e) < 4:
        return None
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


def main():
    out = []
    for fname in sorted(os.listdir(SRC)):
        if not fname.endswith(".mca"):
            continue
        data = open(os.path.join(SRC, fname), "rb").read()
        for idx in range(1024):
            c = read_chunk(data, idx)
            if not c:
                continue
            for e in c.get("Entities", []):
                out.append(str(e))
    with open(os.path.join(OUT, "entities_full.txt"), "w", encoding="utf-8") as fh:
        fh.write("\n\n".join(out))
    print("entities", len(out))
    if out:
        print(out[0])


if __name__ == "__main__":
    main()
