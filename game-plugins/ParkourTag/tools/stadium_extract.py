#!/usr/bin/env python3
"""Raw extract of daniellaur's Parkour Tag stadium (1.17.1 world) with full block states.

The source world is untrusted download data: run this ONLY as `python -I stadium_extract.py <world> <out.json.gz>`.
It uses the standard library only, parses region/NBT bytes itself and never executes anything from the world.

Writes {"min":[x,y,z],"max":[x,y,z],"blocks":[[x,y,z,"name[props]"],...],"tiles":[{id,x,y,z,...}]} for the
copy envelope x 1035..1078, y 0..58, z 972..1017 (air dropped). Tile entities keep only id/position (+ sign text,
command text for the report); nothing else is read.
"""
import gzip, io, json, os, struct, sys, zlib

LO = (1035, 0, 972)
HI = (1078, 58, 1017)
AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


class R:
    def __init__(self, b): self.b = b; self.p = 0
    def u(self, fmt):
        n = struct.calcsize(fmt); v = struct.unpack_from(fmt, self.b, self.p)[0]; self.p += n; return v
    def tag(self, t):
        if t == 1: return self.u(">b")
        if t == 2: return self.u(">h")
        if t == 3: return self.u(">i")
        if t == 4: return self.u(">q")
        if t == 5: return self.u(">f")
        if t == 6: return self.u(">d")
        if t == 7:
            n = self.u(">i"); v = self.b[self.p:self.p + n]; self.p += n; return list(v)
        if t == 8:
            n = self.u(">H"); s = self.b[self.p:self.p + n].decode("utf-8", "replace"); self.p += n; return s
        if t == 9:
            it = self.u(">b"); n = self.u(">i"); return [self.tag(it) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                it = self.u(">b")
                if it == 0: return d
                n = self.u(">H"); k = self.b[self.p:self.p + n].decode("utf-8", "replace"); self.p += n
                d[k] = self.tag(it)
        if t == 11:
            n = self.u(">i"); v = list(struct.unpack_from(">%di" % n, self.b, self.p)); self.p += 4 * n; return v
        if t == 12:
            n = self.u(">i"); v = list(struct.unpack_from(">%dq" % n, self.b, self.p)); self.p += 8 * n; return v
        raise ValueError("bad tag %d" % t)


def parse(raw):
    r = R(raw)
    t = r.u(">b"); n = r.u(">H"); r.p += n
    return r.tag(t)


def read_chunk(data, idx):
    if len(data) < 8192: return None
    e = data[idx * 4:idx * 4 + 4]
    off = (e[0] << 16) | (e[1] << 8) | e[2]
    if not off: return None
    pos = off * 4096
    ln = int.from_bytes(data[pos:pos + 4], "big"); comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + ln]
    raw = zlib.decompress(raw) if comp == 2 else gzip.decompress(raw) if comp == 1 else raw
    return parse(raw)


def unpack(n, states):
    if not states: return [0] * 4096
    bits = max(4, (n - 1).bit_length()); per = 64 // bits; mask = (1 << bits) - 1
    longs = [v & 0xFFFFFFFFFFFFFFFF for v in states]
    return [(longs[i // per] >> ((i % per) * bits)) & mask if i // per < len(longs) else 0 for i in range(4096)]


def state(p):
    props = p.get("Properties")
    name = p["Name"]
    if props: name += "[" + ",".join("%s=%s" % (k, props[k]) for k in sorted(props)) + "]"
    return name


def main():
    world, out = sys.argv[1], sys.argv[2]
    rf = os.path.join(world, "region", "r.2.1.mca")
    data = open(rf, "rb").read()
    blocks, tiles = [], []
    for idx in range(1024):
        ch = read_chunk(data, idx)
        if not ch: continue
        lv = ch.get("Level", ch)
        cx, cz = lv["xPos"], lv["zPos"]
        if cx * 16 + 15 < LO[0] or cx * 16 > HI[0] or cz * 16 + 15 < LO[2] or cz * 16 > HI[2]: continue
        for sec in lv.get("Sections", []):
            pal = sec.get("Palette")
            if not pal: continue
            names = [state(p) for p in pal]
            vals = unpack(len(pal), sec.get("BlockStates"))
            sy = sec["Y"]
            for i, v in enumerate(vals):
                nm = names[v] if v < len(names) else "minecraft:air"
                if nm.split("[")[0] in AIR: continue
                x = cx * 16 + (i & 15); y = sy * 16 + (i >> 8); z = cz * 16 + ((i >> 4) & 15)
                if LO[0] <= x <= HI[0] and LO[1] <= y <= HI[1] and LO[2] <= z <= HI[2]:
                    blocks.append([x, y, z, nm])
        for te in lv.get("TileEntities", []):
            x, y, z = te.get("x"), te.get("y"), te.get("z")
            if not (LO[0] <= x <= HI[0] and LO[1] <= y <= HI[1] and LO[2] <= z <= HI[2]): continue
            d = {"id": te.get("id"), "x": x, "y": y, "z": z}
            for k in ("Text1", "Text2", "Text3", "Text4", "Command", "Items"):
                if k in te: d[k] = te[k] if k != "Items" else len(te[k])
            tiles.append(d)
    blocks.sort(key=lambda b: (b[1], b[0], b[2]))
    with gzip.open(out, "wt", encoding="utf-8") as f:
        json.dump({"min": LO, "max": HI, "blocks": blocks, "tiles": tiles}, f)
    print("blocks", len(blocks), "tiles", len(tiles))


if __name__ == "__main__":
    main()
