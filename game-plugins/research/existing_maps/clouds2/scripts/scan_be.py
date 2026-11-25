#!/usr/bin/env python3
"""Pass A: block entities, chunk bounds, chunk count. No block decoding."""
import gzip
import io
import json
import os
import sys
import zlib

import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_region(path):
    with open(path, "rb") as fh:
        data = fh.read()
    for idx in range(1024):
        entry = data[idx * 4: idx * 4 + 4]
        if len(entry) < 4:
            continue
        off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
        if not off:
            continue
        pos = off * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        raw = data[pos + 5: pos + 4 + length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            elif comp != 3:
                continue
            yield idx, nbtlib.File.parse(io.BytesIO(raw))
        except Exception as e:
            print(f"  !! {os.path.basename(path)} idx {idx}: {e}", file=sys.stderr)


def plain(v):
    if isinstance(v, (int, float, str, bool)):
        return v
    if isinstance(v, bytes):
        return v.decode("utf-8", "replace")
    if hasattr(v, "unpack"):
        try:
            return plain(v.unpack())
        except Exception:
            pass
    if isinstance(v, list):
        return [plain(x) for x in v]
    if isinstance(v, dict):
        return {str(k): plain(x) for k, x in v.items()}
    return str(v)


def main():
    region_dir = os.path.join(WORLD, "region")
    out_be = []
    n_chunks = 0
    minx = minz = 10 ** 9
    maxx = maxz = -10 ** 9
    miny, maxy = 10 ** 9, -10 ** 9
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"):
            continue
        n_in_file = 0
        for idx, chunk in read_region(os.path.join(region_dir, fname)):
            n_chunks += 1
            n_in_file += 1
            cx = int(chunk.get("xPos", 0))
            cz = int(chunk.get("zPos", 0))
            minx = min(minx, cx * 16); maxx = max(maxx, cx * 16 + 15)
            minz = min(minz, cz * 16); maxz = max(maxz, cz * 16 + 15)
            cy = 0
            for section in chunk.get("sections", []):
                sy = int(section.get("Y", 0))
                bs = section.get("block_states")
                if not bs:
                    continue
                pal = bs.get("palette", [])
                if len(pal) == 1:
                    nm = str(pal[0].get("Name", ""))
                    if nm in AIR:
                        continue
                cy = sy
                miny = min(miny, sy * 16); maxy = max(maxy, sy * 16 + 15)
            for be in chunk.get("block_entities", []):
                rec = {"region": fname, "chunk": [cx, cz], "sectionY": cy}
                for k, v in be.items():
                    if k == "keepPacked":
                        continue
                    rec[k] = v
                out_be.append({k: plain(v) for k, v in rec.items()})
        print(f"{fname}: {n_in_file} chunks", file=sys.stderr)

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "block_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(out_be, fh, indent=1)
    summary = {
        "chunks": n_chunks,
        "bounds": {"min": [minx, miny, minz], "max": [maxx, maxy, maxz]},
        "block_entities": len(out_be),
    }
    with open(os.path.join(OUT, "scan_summary.json"), "w", encoding="utf-8") as fh:
        json.dump(summary, fh, indent=1)
    print(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main()
