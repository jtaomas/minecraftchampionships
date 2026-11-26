"""Minimal, stdlib-only Anvil (1.15.2) world reader for the MCC 6 Parkour Warrior replica. DATA ONLY: nothing in the
world (command blocks, signs, datapacks, functions) is ever executed; run it with `python -I`.

    from mc6world import World
    w = World("path/to/Championship Parkour/region")
    for ch in w.chunks():            # dict: x, z, dv, sections {sy: (palette, indices-bytes-as-list)}, tiles [...], ents [...]
"""
import gzip, os, struct, sys, zlib

TAG_END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)


class _R:
    __slots__ = ("b", "i")

    def __init__(self, b):
        self.b = b
        self.i = 0


def _read(r, t):
    b = r.b
    if t == BYTE:
        v = struct.unpack_from(">b", b, r.i)[0]; r.i += 1; return v
    if t == SHORT:
        v = struct.unpack_from(">h", b, r.i)[0]; r.i += 2; return v
    if t == INT:
        v = struct.unpack_from(">i", b, r.i)[0]; r.i += 4; return v
    if t == LONG:
        v = struct.unpack_from(">q", b, r.i)[0]; r.i += 8; return v
    if t == FLOAT:
        v = struct.unpack_from(">f", b, r.i)[0]; r.i += 4; return v
    if t == DOUBLE:
        v = struct.unpack_from(">d", b, r.i)[0]; r.i += 8; return v
    if t == BYTES:
        n = struct.unpack_from(">i", b, r.i)[0]; r.i += 4
        v = b[r.i:r.i + n]; r.i += n; return v
    if t == STRING:
        n = struct.unpack_from(">H", b, r.i)[0]; r.i += 2
        v = b[r.i:r.i + n].decode("utf-8", "replace"); r.i += n; return v
    if t == LIST:
        et = b[r.i]; n = struct.unpack_from(">i", b, r.i + 1)[0]; r.i += 5
        return [_read(r, et) for _ in range(n)]
    if t == COMPOUND:
        d = {}
        while True:
            tt = b[r.i]; r.i += 1
            if tt == TAG_END:
                return d
            n = struct.unpack_from(">H", b, r.i)[0]; r.i += 2
            k = b[r.i:r.i + n].decode("utf-8", "replace"); r.i += n
            d[k] = _read(r, tt)
    if t == INTS:
        n = struct.unpack_from(">i", b, r.i)[0]; r.i += 4
        v = list(struct.unpack_from(">%di" % n, b, r.i)); r.i += 4 * n; return v
    if t == LONGS:
        n = struct.unpack_from(">i", b, r.i)[0]; r.i += 4
        v = b[r.i:r.i + 8 * n]; r.i += 8 * n; return v     # raw big-endian bytes (decoded by unpack_indices)
    raise ValueError("bad tag %d" % t)


def nbt(raw):
    r = _R(raw)
    t = raw[0]; r.i = 1
    n = struct.unpack_from(">H", raw, r.i)[0]; r.i += 2 + n
    return _read(r, t)


def prop_name(p):
    n = p["Name"]
    pr = p.get("Properties")
    if pr:
        n += "[" + ",".join("%s=%s" % (k, pr[k]) for k in sorted(pr)) + "]"
    return n


def unpack_indices(longs_be, nbits_palette):
    """1.15 (pre-1.16) packing: indices run continuously across 64-bit longs. Returns list of 4096 ints."""
    bits = max(4, (nbits_palette - 1).bit_length())
    big = int.from_bytes(longs_be, "big")
    total = len(longs_be) * 8
    mask = (1 << bits) - 1
    out = [0] * 4096
    # big-endian long array == the bit stream as the Minecraft packing reads it little-endian per long.
    # Re-assemble as little-endian per long.
    n = len(longs_be) // 8
    vals = struct.unpack(">%dQ" % n, longs_be)
    stream = 0
    for k in range(n - 1, -1, -1):
        stream = (stream << 64) | vals[k]
    for i in range(4096):
        out[i] = (stream >> (i * bits)) & mask
    return out


def chunks_of_region(path):
    data = open(path, "rb").read()
    if len(data) < 8192:
        return
    for i in range(1024):
        off = int.from_bytes(data[i * 4:i * 4 + 3], "big"); cnt = data[i * 4 + 3]
        if off == 0 or cnt == 0:
            continue
        p = off * 4096
        if p + 5 > len(data):
            continue
        ln = int.from_bytes(data[p:p + 4], "big"); comp = data[p + 4]
        raw = data[p + 5:p + 4 + ln]
        try:
            raw = zlib.decompress(raw) if comp == 2 else (gzip.decompress(raw) if comp == 1 else raw)
            yield nbt(raw)
        except Exception as e:  # noqa
            print("chunk error", path, i, e, file=sys.stderr)


class World:
    def __init__(self, region_dir):
        self.dir = region_dir

    def region_files(self):
        out = []
        for f in sorted(os.listdir(self.dir)):
            if f.startswith("r.") and f.endswith(".mca"):
                _, rx, rz, _ = f.split(".")
                out.append((int(rx), int(rz), os.path.join(self.dir, f)))
        return out

    def chunks(self, regions=None, decode=True):
        for rx, rz, path in self.region_files():
            if regions is not None and (rx, rz) not in regions:
                continue
            for ch in chunks_of_region(path):
                lvl = ch.get("Level", ch)
                secs = {}
                for s in lvl.get("Sections", []):
                    if "Palette" not in s or "BlockStates" not in s:
                        continue
                    pal = [prop_name(p) for p in s["Palette"]]
                    if len(pal) == 1:
                        secs[s["Y"]] = (pal, None)
                    elif decode:
                        secs[s["Y"]] = (pal, unpack_indices(s["BlockStates"], len(pal)))
                    else:
                        secs[s["Y"]] = (pal, None)
                yield {"x": lvl["xPos"], "z": lvl["zPos"], "dv": ch.get("DataVersion"), "sections": secs,
                       "tiles": lvl.get("TileEntities", []), "ents": lvl.get("Entities", []),
                       "status": lvl.get("Status")}


# ------------------------------------------------------------------ 1.18+ (block_states) chunks, for reading our own server's world back

def unpack_modern(longs_be, palette_len):
    """1.16+ packing: entries never span two longs. Returns 4096 palette indices."""
    bits = max(4, (palette_len - 1).bit_length())
    per = 64 // bits
    n = len(longs_be) // 8
    vals = struct.unpack(">%dQ" % n, longs_be)
    mask = (1 << bits) - 1
    out = []
    for v in vals:
        for k in range(per):
            out.append((v >> (k * bits)) & mask)
            if len(out) == 4096:
                return out
    return out


def modern_chunks(region_dir):
    """Yield (chunk x, chunk z, {section y: (palette names, indices or None)}) from a 1.18+ world's region folder."""
    for f in sorted(os.listdir(region_dir)):
        if not (f.startswith("r.") and f.endswith(".mca")):
            continue
        for ch in chunks_of_region(os.path.join(region_dir, f)):
            secs = {}
            for s in ch.get("sections", []):
                bs = s.get("block_states")
                if not bs:
                    continue
                pal = [prop_name(p) for p in bs["palette"]]
                if len(pal) == 1:
                    secs[s["Y"]] = (pal, None)
                else:
                    secs[s["Y"]] = (pal, unpack_modern(bs["data"], len(pal)))
            yield ch["xPos"], ch["zPos"], secs
