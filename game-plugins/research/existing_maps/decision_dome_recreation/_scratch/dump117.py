import io, os, sys, zlib, gzip, argparse
import nbtlib

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

def read_chunks(path):
    with open(path, "rb") as fh:
        data = fh.read()
    if len(data) < 8192:
        return
    for idx in range(1024):
        entry = data[idx*4:idx*4+4]
        off = (entry[0]<<16)|(entry[1]<<8)|entry[2]
        if not off:
            continue
        pos = off*4096
        length = int.from_bytes(data[pos:pos+4], "big")
        comp = data[pos+4]
        raw = data[pos+5:pos+4+length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            elif comp != 3:
                continue
            yield idx, nbtlib.File.parse(io.BytesIO(raw))
        except Exception as e:
            print("ERR", path, idx, e, file=sys.stderr)

def decode_section(section):
    pal = section.get("Palette")
    if pal is None:
        return
    names = []
    for p in pal:
        name = str(p.get("Name"))
        props = p.get("Properties")
        if props:
            name += "[" + ",".join(f"{k}={v}" for k,v in props.items()) + "]"
        names.append(name)
    states = section.get("BlockStates")
    if states is None:
        for i in range(4096):
            yield i, names[0]
        return
    bits = max(4, (len(names)-1).bit_length())
    longs = [int(v) & 0xFFFFFFFFFFFFFFFF for v in states]
    mask = (1<<bits)-1
    per = 64//bits
    for i in range(4096):
        li = i//per
        if li >= len(longs): break
        val = (longs[li] >> ((i%per)*bits)) & mask
        if val < len(names):
            yield i, names[val]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("world")
    ap.add_argument("output")
    ap.add_argument("--min", nargs=3, type=int)
    ap.add_argument("--max", nargs=3, type=int)
    ap.add_argument("--y-min", type=int, default=None)
    ap.add_argument("--y-max", type=int, default=None)
    ap.add_argument("--strip", nargs="*", default=[])
    args = ap.parse_args()

    lo = args.min or [-30000000, -64, -30000000]
    hi = args.max or [30000000, 320, 30000000]
    if args.y_min is not None: lo[1] = args.y_min
    if args.y_max is not None: hi[1] = args.y_max

    region_dir = os.path.join(os.path.abspath(args.world), "region")
    blocks = []
    for fname in sorted(os.listdir(region_dir)):
        if not fname.endswith(".mca"): continue
        path = os.path.join(region_dir, fname)
        if os.path.getsize(path) == 0: continue
        for idx, f in read_chunks(path):
            lvl = f.get("Level")
            if lvl is None: continue
            cx, cz = int(lvl["xPos"]), int(lvl["zPos"])
            if cx*16+15 < lo[0] or cx*16 > hi[0] or cz*16+15 < lo[2] or cz*16 > hi[2]:
                continue
            for s in lvl.get("Sections", []):
                pal = s.get("Palette")
                if pal is None: continue
                sy = int(s["Y"])
                if sy*16+15 < lo[1] or sy*16 > hi[1]: continue
                for i, name in decode_section(s):
                    if name in AIR: continue
                    bx = cx*16 + (i & 15)
                    by = sy*16 + (i >> 8)
                    bz = cz*16 + ((i >> 4) & 15)
                    if bx < lo[0] or bx > hi[0] or by < lo[1] or by > hi[1] or bz < lo[2] or bz > hi[2]:
                        continue
                    blocks.append((bx, by, bz, name))
    if not blocks:
        raise SystemExit("no blocks")
    # apply optional strip of full block-state suffixes for readability
    if args.strip:
        blocks = [(x,y,z,n.split("[")[0] if any(n.startswith(s) for s in args.strip) else n) for x,y,z,n in blocks]

    bx0 = min(b[0] for b in blocks); by0 = min(b[1] for b in blocks); bz0 = min(b[2] for b in blocks)
    bx1 = max(b[0] for b in blocks); by1 = max(b[1] for b in blocks); bz1 = max(b[2] for b in blocks)
    out = gzip.open(args.output, "wt", encoding="utf-8", compresslevel=6)
    out.write(f"MCMDUMP 1 {bx1-bx0+1} {by1-by0+1} {bz1-bz0+1}\n")
    for x,y,z,n in blocks:
        out.write(f"{x-bx0} {y-by0} {z-bz0} {n}\n")
    out.close()
    print(f"wrote {args.output}: {len(blocks)} blocks, bounds {bx0},{by0},{bz0} -> {bx1},{by1},{bz1}, size {bx1-bx0+1}x{by1-by0+1}x{bz1-bz0+1}", file=sys.stderr)

if __name__ == "__main__":
    main()
