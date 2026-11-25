"""Read-only block lookup from region files (no server commands)."""
import sys
from pathlib import Path
import scan_worlds as sw


def region_path(world, dim, cx, cz):
    rx, rz = cx // 32, cz // 32
    sub = {"overworld": "region", "nether": "DIM-1/region", "end": "DIM1/region"}[dim]
    return sw.SERVER / world / sub / f"r.{rx}.{rz}.mca"


def chunk_blocks(world, dim, cx, cz):
    p = region_path(world, dim, cx, cz)
    if not p.exists():
        return None
    for gx, gz, root in sw.read_region(p):
        if gx == cx and gz == cz:
            lev = sw.level_of(root)
            out = {}
            for sec in lev.get("sections", lev.get("Sections", [])):
                sy = int(sec.get("Y", 0))
                pal, _ = sw.section_palette(sec)
                if len(pal) == 1:
                    name = str(pal[0].get("Name", "minecraft:air"))
                    if name == "minecraft:air":
                        continue
                blocks = sw.section_blocks(sec)
                for idx, (name, props) in blocks.items():
                    lx, lz, ly = idx & 15, (idx >> 4) & 15, idx >> 8
                    out[(cx * 16 + lx, sy * 16 + ly, cz * 16 + lz)] = (name, props)
            return out
    return None


_cache = {}


def get_block(world, dim, x, y, z):
    cx, cz = x // 16, z // 16
    key = (world, dim, cx, cz)
    if key not in _cache:
        _cache[key] = chunk_blocks(world, dim, cx, cz) or {}
    return _cache[key].get((x, y, z), ("minecraft:air", {}))


def box(world, dim, x0, y0, z0, x1, y1, z1):
    out = []
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                name, props = get_block(world, dim, x, y, z)
                out.append((x, y, z, name, props))
    return out


def spawn_report(world, dim="overworld", r=4):
    sp = sw.nbtlib.load(str(sw.SERVER / world / "level.dat"))["Data"]
    sx, sy, sz = int(sp["SpawnX"]), int(sp["SpawnY"]), int(sp["SpawnZ"])
    print(f"== {world} spawn ({sx},{sy},{sz})")
    for y in range(sy - 2, sy + 3):
        name, props = get_block(world, dim, sx, y, sz)
        print(f"  {sx},{y},{sz}: {name} {props}")
    print("  nearby non-air:")
    for x, y, z, name, props in box(world, dim, sx - r, sy - 2, sz - r, sx + r, sy + 2, sz + r):
        if name != "minecraft:air":
            print(f"    {x},{y},{z} {name} {props}")


if __name__ == "__main__":
    for w in sys.argv[1:] or ["mcc_clouds2"]:
        spawn_report(w)
