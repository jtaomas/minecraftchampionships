#!/usr/bin/env python3
"""Fast numpy scanner + MCMDUMP writer for TGTTOS Badlands (read-only source)."""
import io, os, sys, zlib, gzip, json, csv, collections, time
import numpy as np
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat"
OUT = os.path.dirname(os.path.abspath(__file__))
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
MASK64 = (1 << 64) - 1


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


def section_arrays(sec):
    """Return (palette_state_strings, decoded_index_array or None)."""
    bs = sec.get("block_states")
    if not bs:
        return None, None
    pal = [state_string(p) for p in bs.get("palette", [])]
    if not pal:
        return None, None
    data = bs.get("data")
    if data is None:
        return pal, None
    bits = max(4, (len(pal) - 1).bit_length())
    per = 64 // bits
    longs = np.array([int(v) & MASK64 for v in data], dtype=np.uint64)
    n = min(4096, len(longs) * per)
    idx = np.arange(n, dtype=np.uint64)
    vals = (longs[idx // per] >> ((idx % per) * np.uint64(bits))) & np.uint64((1 << bits) - 1)
    return pal, vals.astype(np.int32)


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


def iter_region(dirpath):
    for fname in sorted(os.listdir(dirpath)):
        if not fname.endswith(".mca"):
            continue
        p = os.path.join(dirpath, fname)
        with open(p, "rb") as fh:
            data = fh.read()
        for idx in range(1024):
            ch = read_chunk(data, idx)
            if ch:
                yield fname, ch


def main():
    t0 = time.time()
    counts = collections.Counter()
    tes, ents = [], []
    chunks = 0
    bounds = [1 << 30, 1 << 30, 1 << 30, -(1 << 30), -(1 << 30), -(1 << 30)]
    dump_path = os.path.join(OUT, "world.mcmdump.gz")
    out = gzip.open(dump_path, "wt", encoding="utf-8", compresslevel=5)
    # header patched later: write placeholder, collect lines
    lines = []
    for fname, ch in iter_region(os.path.join(WORLD, "region")):
        cx = int(ch.get("xPos", 0)); cz = int(ch.get("zPos", 0))
        chunks += 1
        for sec in ch.get("sections", []):
            sy = int(sec.get("Y", 0))
            pal, vals = section_arrays(sec)
            if not pal:
                continue
            if vals is None:
                for pi, ps in enumerate(pal):
                    if ps in AIR:
                        continue
                    counts[ps] += 4096
                    ax = np.arange(4096)
                    xs = cx * 16 + (ax & 15); ys = sy * 16 + (ax >> 8); zs = cz * 16 + ((ax >> 4) & 15)
                    bounds[0] = min(bounds[0], int(xs.min())); bounds[3] = max(bounds[3], int(xs.max()))
                    bounds[1] = min(bounds[1], int(ys.min())); bounds[4] = max(bounds[4], int(ys.max()))
                    bounds[2] = min(bounds[2], int(zs.min())); bounds[5] = max(bounds[5], int(zs.max()))
                    for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
                        lines.append(f"{x} {y} {z} {ps}")
                continue
            if len(vals) < 4096:
                continue
            nz = vals < len(pal)
            vals = vals[nz]
            if len(vals) == 0:
                continue
            # aggregate by palette entry
            binc = np.bincount(vals, minlength=len(pal))
            ax = np.nonzero(nz)[0]
            xs = cx * 16 + (ax & 15); ys = sy * 16 + (ax >> 8); zs = cz * 16 + ((ax >> 4) & 15)
            bounds[0] = min(bounds[0], int(xs.min())); bounds[3] = max(bounds[3], int(xs.max()))
            bounds[1] = min(bounds[1], int(ys.min())); bounds[4] = max(bounds[4], int(ys.max()))
            bounds[2] = min(bounds[2], int(zs.min())); bounds[5] = max(bounds[5], int(zs.max()))
            air_idx = {i for i, s in enumerate(pal) if s in AIR}
            for pi, ps in enumerate(pal):
                n = int(binc[pi]) if pi < len(binc) else 0
                if not n or pi in air_idx:
                    continue
                counts[ps] += n
            for x, y, z, v in zip(xs.tolist(), ys.tolist(), zs.tolist(), vals.tolist()):
                ps = pal[v]
                if ps in AIR:
                    continue
                lines.append(f"{x} {y} {z} {ps}")
        be = ch.get("block_entities")
        if be:
            for t in be:
                tes.append({"file": fname, "chunk": [cx, cz], "nbt": nbt_to_plain(t)})
        e = ch.get("Entities")
        if e:
            for t in e:
                ents.append({"file": fname, "chunk": [cx, cz], "nbt": nbt_to_plain(t)})
        if chunks % 500 == 0:
            print(f"{chunks} chunks {time.time()-t0:.1f}s lines={len(lines)}", file=sys.stderr)
    # entities folder
    for fname, ch in iter_region(os.path.join(WORLD, "entities")):
        for t in ch.get("Entities", []):
            ents.append({"file": fname, "chunk": [int(ch.get("xPos", 0)), int(ch.get("zPos", 0))], "nbt": nbt_to_plain(t)})

    bx0, by0, bz0, bx1, by1, bz1 = bounds
    out.write(f"MCMDUMP 1 {bx1-bx0+1} {by1-by0+1} {bz1-bz0+1}\n")
    for ln in lines:
        x, y, z, b = ln.split(" ", 3)
        out.write(f"{int(x)-bx0} {int(y)-by0} {int(z)-bz0} {b}\n")
    out.close()
    total = sum(counts.values())
    print(f"chunks={chunks} bounds={bounds} types={len(counts)} total={total} time={time.time()-t0:.1f}s", file=sys.stderr)
    with open(os.path.join(OUT, "raw_blocks.json"), "w", encoding="utf-8") as fh:
        json.dump({"bounds": bounds, "chunks": chunks, "counts": counts.most_common()}, fh, indent=1)
    with open(os.path.join(OUT, "raw_tile_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(tes, fh, indent=1)
    with open(os.path.join(OUT, "raw_entities.json"), "w", encoding="utf-8") as fh:
        json.dump(ents, fh, indent=1)
    with open(os.path.join(OUT, "blocks.csv"), "w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh); w.writerow(["block", "count", "share_pct"])
        for b, n in counts.most_common():
            w.writerow([b, n, f"{100.0*n/total:.3f}"])
    print(f"tile_entities={len(tes)} entities={len(ents)}", file=sys.stderr)


if __name__ == "__main__":
    main()
