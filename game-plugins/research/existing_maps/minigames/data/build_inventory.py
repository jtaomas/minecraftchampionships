#!/usr/bin/env python3
"""Authoritative island inventory for the minigames world (1.17.1).

Assigns every chunk with real blocks to one of the connected components found by
scan_chunks.py (chunk_scan.csv), then reports exact block bounds, palettes,
tile entities (signs/commands) and optionally writes a positions CSV.
"""
import csv
import io
import os
import sys
import zlib
import gzip as gz
from collections import Counter, defaultdict

import nbtlib

WORLD = sys.argv[1]
OUT = sys.argv[2]
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def components_from_scan(path):
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    pts = {(int(r["chunk_x"]), int(r["chunk_z"])): r for r in rows}
    seen = set()
    comps = []
    for p in pts:
        if p in seen:
            continue
        stack = [p]
        comp = []
        seen.add(p)
        while stack:
            c = stack.pop()
            comp.append(c)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    q = (c[0] + dx, c[1] + dz)
                    if q in pts and q not in seen:
                        seen.add(q)
                        stack.append(q)
        comps.append(comp)
    comps.sort(key=lambda c: (min(p[0] for p in c), min(p[1] for p in c)))
    return pts, comps


def read_chunk(data, idx):
    e = data[idx * 4:idx * 4 + 4]
    if len(e) < 4:
        return None
    off = (e[0] << 16) | (e[1] << 8) | e[2]
    if not off:
        return None
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
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def unpack(pal, states):
    n = len(pal)
    if states is None or len(states) == 0:
        return [0] * 4096
    bits = max(4, (n - 1).bit_length())
    per = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in states]
    vals = []
    for i in range(4096):
        li = i // per
        vals.append(0 if li >= len(longs) else (longs[li] >> ((i % per) * bits)) & mask)
    return vals


def main():
    scan_csv = os.path.join(os.path.abspath(OUT), "chunk_scan.csv")
    pts, comps = components_from_scan(scan_csv)
    island_of = {}
    for i, comp in enumerate(comps):
        for p in comp:
            island_of[p] = i

    blocks = defaultdict(list)          # island -> (x,y,z,block)
    signs = defaultdict(list)           # island -> (x,y,z,text)
    commands = defaultdict(list)        # island -> (x,y,z,command,auto,cond)
    chunk_in = defaultdict(set)         # island -> chunks with actual blocks
    region_files = sorted(f for f in os.listdir(os.path.join(WORLD, "region")) if f.endswith(".mca"))
    for fn in region_files:
        data = open(os.path.join(WORLD, "region", fn), "rb").read()
        for idx in range(1024):
            ch = read_chunk(data, idx)
            if not ch:
                continue
            lv = ch.get("Level", ch)
            cx, cz = int(lv.get("xPos", 0)), int(lv.get("zPos", 0))
            key = (cx, cz)
            if key not in island_of:
                continue
            isl = island_of[key]
            for te in lv.get("TileEntities", []):
                tid = str(te.get("id", ""))
                x, y, z = int(te.get("x", 0)), int(te.get("y", 0)), int(te.get("z", 0))
                if tid.endswith("sign"):
                    lines = [str(te.get(k, "")).strip() for k in ("Text1", "Text2", "Text3", "Text4")]
                    signs[isl].append((x, y, z, " | ".join(l for l in lines if l)))
                elif tid.endswith("command_block"):
                    commands[isl].append((x, y, z, tid.replace("minecraft:", ""), str(te.get("Command", "")),
                                          bool(te.get("auto", 0)), bool(te.get("conditionMet", 0))))
            for sec in lv.get("Sections", []):
                pal = sec.get("Palette")
                if not pal:
                    continue
                names = [str(p.get("Name", "minecraft:air")) for p in pal]
                if all(n in AIR for n in names):
                    continue
                vals = unpack(pal, sec.get("BlockStates"))
                sy = int(sec.get("Y", 0))
                for i, v in enumerate(vals):
                    if v >= len(names) or names[v] in AIR:
                        continue
                    x = cx * 16 + (i & 15)
                    y = sy * 16 + (i >> 8)
                    z = cz * 16 + ((i >> 4) & 15)
                    blocks[isl].append((x, y, z, names[v]))
                    chunk_in[isl].add(key)

    # per-island summary
    summary = []
    for isl in sorted(blocks):
        bl = blocks[isl]
        xs = [b[0] for b in bl]; ys = [b[1] for b in bl]; zs = [b[2] for b in bl]
        pal = Counter(b[3] for b in bl)
        row = dict(id=isl, chunks=len(chunk_in[isl]),
                   xmin=min(xs), xmax=max(xs), ymin=min(ys), ymax=max(ys),
                   zmin=min(zs), zmax=max(zs), blocks=len(bl),
                   signs=len(signs[isl]), commands=len(commands[isl]),
                   top=";".join(f"{k.replace('minecraft:', '')}={v}" for k, v in pal.most_common(8)))
        summary.append(row)
        print(f"ISL {isl}: chunks={row['chunks']} blocks={row['blocks']} "
              f"x {row['xmin']}..{row['xmax']} y {row['ymin']}..{row['ymax']} z {row['zmin']}..{row['zmax']} "
              f"signs={row['signs']} cmds={row['commands']}")
        print("   ", row["top"])

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "islands.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("id,chunks,xmin,xmax,ymin,ymax,zmin,zmax,blocks,signs,commands,top_blocks\n")
        for r in summary:
            fh.write(",".join(str(r[k]) for k in
                              ("id", "chunks", "xmin", "xmax", "ymin", "ymax", "zmin", "zmax",
                               "blocks", "signs", "commands", "top")) + "\n")
    with open(os.path.join(OUT, "blocks_positions.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("island,x,y,z,block\n")
        for isl in sorted(blocks):
            for x, y, z, b in sorted(blocks[isl]):
                fh.write(f"{isl},{x},{y},{z},{b}\n")
    with open(os.path.join(OUT, "tile_entities.csv"), "w", encoding="utf-8", newline="") as fh:
        fh.write("island,type,x,y,z,detail,auto,condition_met\n")
        for isl in sorted(signs):
            for x, y, z, t in signs[isl]:
                fh.write(f"{isl},sign,{x},{y},{z},\"{t.replace(chr(34), chr(39))}\",,\n")
        for isl in sorted(commands):
            for x, y, z, kind, c, a, k in commands[isl]:
                fh.write(f"{isl},{kind},{x},{y},{z},\"{c.replace(chr(34), chr(39))}\",{int(a)},{int(k)}\n")
    print(f"wrote islands.csv, blocks_positions.csv, tile_entities.csv to {OUT}")


if __name__ == "__main__":
    main()
