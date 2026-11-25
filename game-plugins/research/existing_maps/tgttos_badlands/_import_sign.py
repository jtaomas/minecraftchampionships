#!/usr/bin/env python3
"""Read the imported world's start-sign block entity after the 1.21.1 upgrade (read-only)."""
import io, os, zlib, gzip, json
import nbtlib

REGION = r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review\mcc_tgttos_badlands\region\r.0.0.mca"


def read_chunk(data, index):
    entry = data[index * 4:index * 4 + 4]
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
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


def plain(t):
    if isinstance(t, nbtlib.Compound):
        return {str(k): plain(v) for k, v in t.items()}
    if isinstance(t, nbtlib.List):
        return [plain(v) for v in t]
    if isinstance(t, nbtlib.String):
        return str(t)
    if isinstance(t, (nbtlib.Byte, nbtlib.Short, nbtlib.Int, nbtlib.Long)):
        return int(t)
    if isinstance(t, (nbtlib.Float, nbtlib.Double)):
        return float(t)
    return str(t)


def main():
    data = open(REGION, "rb").read()
    # world x=9,z=31 -> chunk x=0,z=1 -> index = 0 + 1*32 = 32
    for idx in (32, 0, 1):
        ch = read_chunk(data, idx)
        if not ch:
            continue
        if int(ch.get("xPos", 99)) != 0 or int(ch.get("zPos", 99)) != 1:
            continue
        for be in ch.get("block_entities", []):
            n = plain(be)
            if n.get("id") == "minecraft:sign":
                print(json.dumps(n, indent=1, ensure_ascii=False))
        print("chunk DataVersion", ch.get("DataVersion"))
    print("done")


if __name__ == "__main__":
    main()
