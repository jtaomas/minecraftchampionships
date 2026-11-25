#!/usr/bin/env python3
"""Fast chunk occupancy scan for the minigames world (1.17.1 format).

Reads region/*.mca, decodes only section palettes (no block data), and reports
per-chunk: non-air section count, palette size, min/max content Y, and the
top palette entries. Used to locate distinct arenas before full dumps.
Handles both pre-1.18 ("Level" wrapper, "Sections"/"Palette") and 1.18+
("sections"/"block_states") chunk layouts.
"""
import io
import os
import sys
import zlib
import gzip as gz

import nbtlib

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
    if len(entry) < 4:
        return None
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
    if not off:
        return None
    pos = off * 4096
    if pos + 5 > len(data):
        return None
    length = int.from_bytes(data[pos:pos + 4], "big")
    comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + length]
    try:
        if comp == 2:
            raw = zlib.decompress(raw)
        elif comp == 1:
            raw = gz.decompress(raw)
        elif comp != 3:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def sections_of(chunk):
    """Yield (section_y, palette_name_list) for either chunk format."""
    if "Level" in chunk:
        lv = chunk["Level"]
        for sec in lv.get("Sections", []):
            pal = sec.get("Palette")
            if not pal:
                continue
            yield int(sec.get("Y", 0)), [str(p.get("Name", "minecraft:air")) for p in pal]
    for sec in chunk.get("sections", []):
        bs = sec.get("block_states")
        if not bs:
            continue
        pal = bs.get("palette", [])
        if not pal:
            continue
        yield int(sec.get("Y", 0)), [str(p.get("Name", "minecraft:air")) for p in pal]


def main():
    world = os.path.abspath(sys.argv[1])
    out = sys.argv[2]
    region_dir = os.path.join(world, "region")
    rows = []
    files = sorted(f for f in os.listdir(region_dir) if f.endswith(".mca"))
    stats = {"chunks": 0, "with_sections": 0}
    for fi, fname in enumerate(files):
        with open(os.path.join(region_dir, fname), "rb") as fh:
            data = fh.read()
        for idx in range(1024):
            ch = read_chunk(data, idx)
            if not ch:
                continue
            stats["chunks"] += 1
            lv = ch.get("Level", ch)
            cx = int(lv.get("xPos", 0))
            cz = int(lv.get("zPos", 0))
            status = str(lv.get("Status", ""))
            ymin, ymax = 999, -999
            nonair_sections = 0
            pal_counts = {}
            for sy, names in sections_of(ch):
                if all(n in AIR for n in names):
                    continue
                nonair_sections += 1
                ymin = min(ymin, sy * 16)
                ymax = max(ymax, sy * 16 + 15)
                for n in names:
                    pal_counts[n] = pal_counts.get(n, 0) + 1
            if nonair_sections == 0:
                continue
            stats["with_sections"] += 1
            top = sorted(pal_counts.items(), key=lambda kv: -kv[1])[:8]
            rows.append((cx, cz, status, nonair_sections, ymin, ymax,
                         "|".join(f"{k}:{v}" for k, v in top)))
        print(f"{fname} done ({fi+1}/{len(files)})", file=sys.stderr)
    with open(out, "w", encoding="utf-8", newline="") as fh:
        fh.write("chunk_x,chunk_z,status,sections,ymin,ymax,palette_top\n")
        for r in rows:
            fh.write(",".join(str(v) for v in r) + "\n")
    print(f"wrote {out}: {len(rows)} non-empty chunks of {stats['chunks']} present", file=sys.stderr)


if __name__ == "__main__":
    main()
