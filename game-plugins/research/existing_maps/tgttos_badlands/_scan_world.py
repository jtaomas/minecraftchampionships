#!/usr/bin/env python3
"""A7 read-only scanner for TGTTOS Badlands. Writes JSON/CSV in this folder only."""
import io, os, sys, zlib, gzip, json, csv, collections
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat"
OUT = os.path.dirname(os.path.abspath(__file__))
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
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
            raw = gzip.decompress(raw)
        elif comp != 3:
            return None
        return nbtlib.File.parse(io.BytesIO(raw))
    except Exception:
        return None


def state_string(state):
    name = str(state.get("Name", "minecraft:air"))
    props = state.get("Properties")
    if not props:
        return name
    return name + "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]"


def unpack_section(section):
    bs = section.get("block_states")
    if not bs:
        return
    palette = bs.get("palette", [])
    if not palette:
        return
    data = bs.get("data")
    if data is None:
        for i in range(4096):
            yield i, state_string(palette[0])
        return
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in data]
    for i in range(4096):
        li = i // per_long
        if li >= len(longs):
            break
        shift = (i % per_long) * bits
        val = (longs[li] >> shift) & mask
        if val >= len(palette):
            continue
        yield i, state_string(palette[val])


def nbt_to_plain(tag):
    if isinstance(tag, nbtlib.Compound):
        return {str(k): nbt_to_plain(v) for k, v in tag.items()}
    if isinstance(tag, nbtlib.List):
        return [nbt_to_plain(v) for v in tag]
    if isinstance(tag, nbtlib.String):
        return str(tag)
    if isinstance(tag, (nbtlib.Byte, nbtlib.Short, nbtlib.Int, nbtlib.Long)):
        return int(tag)
    if isinstance(tag, (nbtlib.Float, nbtlib.Double)):
        return float(tag)
    if isinstance(tag, nbtlib.ByteArray):
        return [int(v) for v in tag]
    if isinstance(tag, nbtlib.IntArray):
        return [int(v) for v in tag]
    if isinstance(tag, nbtlib.LongArray):
        return [int(v) for v in tag]
    return str(tag)


def scan_region_dir(dirpath, kind):
    counts = collections.Counter()
    tile_entities = []
    entities = []
    chunks = 0
    bounds = [999999, 999999, 999999, -999999, -999999, -999999]
    chunk_set = set()
    for fname in sorted(os.listdir(dirpath)):
        if not fname.endswith(".mca"):
            continue
        p = os.path.join(dirpath, fname)
        with open(p, "rb") as fh:
            data = fh.read()
        for idx in range(1024):
            ch = read_chunk(data, idx)
            if not ch:
                continue
            cx = int(ch.get("xPos", 0)); cz = int(ch.get("zPos", 0))
            chunk_set.add((cx, cz))
            if kind == "blocks":
                chunks += 1
                for sec in ch.get("sections", []):
                    sy = int(sec.get("Y", 0))
                    for i, b in unpack_section(sec):
                        if b in AIR:
                            continue
                        bx = cx * 16 + (i & 15)
                        by = sy * 16 + (i >> 8)
                        bz = cz * 16 + ((i >> 4) & 15)
                        counts[b] += 1
                        bounds[0] = min(bounds[0], bx); bounds[1] = min(bounds[1], by); bounds[2] = min(bounds[2], bz)
                        bounds[3] = max(bounds[3], bx); bounds[4] = max(bounds[4], by); bounds[5] = max(bounds[5], bz)
            be = ch.get("block_entities")
            if be:
                for t in be:
                    tile_entities.append({"file": fname, "chunk": [cx, cz], "nbt": nbt_to_plain(t)})
            ent = ch.get("Entities")
            if ent:
                for t in ent:
                    entities.append({"file": fname, "chunk": [cx, cz], "nbt": nbt_to_plain(t)})
    return counts, tile_entities, entities, chunks, bounds, chunk_set


def main():
    bc, tes, ents, chunks, bounds, cset = scan_region_dir(os.path.join(WORLD, "region"), "blocks")
    # entities region
    _, _, eents, _, _, _ = scan_region_dir(os.path.join(WORLD, "entities"), "entities")
    ents.extend(eents)
    print(f"chunks={chunks} bounds={bounds} blocktypes={len(bc)} total={sum(bc.values())}", file=sys.stderr)
    with open(os.path.join(OUT, "raw_blocks.json"), "w", encoding="utf-8") as fh:
        json.dump({"bounds": bounds, "chunks": chunks, "counts": bc.most_common()}, fh, indent=1)
    with open(os.path.join(OUT, "raw_tile_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(tes, fh, indent=1)
    with open(os.path.join(OUT, "raw_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(ents, fh, indent=1)

    # chunk list
    with open(os.path.join(OUT, "raw_chunks.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh); w.writerow(["chunk_x", "chunk_z"])
        for cx, cz in sorted(cset):
            w.writerow([cx, cz])

    # blocks CSV
    total = sum(bc.values())
    with open(os.path.join(OUT, "blocks.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh); w.writerow(["block", "count", "share_pct"])
        for b, n in bc.most_common():
            w.writerow([b, n, f"{100.0*n/total:.3f}"])
    print("done", file=sys.stderr)


if __name__ == "__main__":
    main()
