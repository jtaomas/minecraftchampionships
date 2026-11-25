#!/usr/bin/env python3
"""
Unified Java Anvil (.mca) scanner & exporter for MCMDUMP (render_preview.py) and MCMI (.mcmi.gz).
Supports modern 1.18+, flattening 1.13-1.17, and legacy 1.2-1.12 numeric block IDs.
"""
import argparse
import glob
import gzip
import io
import os
import re
import struct
import sys
import zlib
from collections import deque

import nbtlib
import numpy as np

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
COLORS = (
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
)
WOODS = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")
STONE_BRICKS = ("stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks")

BANNED = {
    "minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_portal_frame",
    "minecraft:end_gateway", "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:structure_block", "minecraft:structure_void",
    "minecraft:jigsaw", "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault",
    "minecraft:respawn_anchor", "minecraft:tnt", "minecraft:sculk_shrieker",
    "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor",
}

STATE_RE = re.compile(
    r"^minecraft:[a-z0-9_]{1,64}(\[[a-z0-9_]{1,32}=[a-z0-9_]{1,32}(,[a-z0-9_]{1,32}=[a-z0-9_]{1,32}){0,15}])?$"
)

LEGACY_SIMPLE = {
    1: "minecraft:stone", 2: "minecraft:grass_block", 3: "minecraft:dirt", 4: "minecraft:cobblestone",
    7: "minecraft:bedrock", 8: "minecraft:water", 9: "minecraft:water", 10: "minecraft:lava", 11: "minecraft:lava",
    12: "minecraft:sand", 13: "minecraft:gravel", 14: "minecraft:gold_ore", 15: "minecraft:iron_ore",
    16: "minecraft:coal_ore", 19: "minecraft:sponge", 20: "minecraft:glass", 21: "minecraft:lapis_ore",
    22: "minecraft:lapis_block", 23: "minecraft:dispenser", 24: "minecraft:sandstone", 25: "minecraft:note_block",
    27: "minecraft:powered_rail", 28: "minecraft:detector_rail", 29: "minecraft:sticky_piston",
    30: "minecraft:cobweb", 31: "minecraft:short_grass", 32: "minecraft:dead_bush", 33: "minecraft:piston",
    37: "minecraft:dandelion", 38: "minecraft:poppy", 39: "minecraft:brown_mushroom", 40: "minecraft:red_mushroom",
    41: "minecraft:gold_block", 42: "minecraft:iron_block", 45: "minecraft:bricks", 46: "minecraft:tnt",
    47: "minecraft:bookshelf", 48: "minecraft:mossy_cobblestone", 49: "minecraft:obsidian",
    50: "minecraft:torch", 51: "minecraft:fire", 53: "minecraft:oak_stairs", 54: "minecraft:chest",
    56: "minecraft:diamond_ore", 57: "minecraft:diamond_block", 58: "minecraft:crafting_table",
    59: "minecraft:wheat", 60: "minecraft:farmland", 61: "minecraft:furnace", 62: "minecraft:furnace",
    63: "minecraft:oak_sign", 64: "minecraft:oak_door", 65: "minecraft:ladder", 66: "minecraft:rail",
    67: "minecraft:cobblestone_stairs", 69: "minecraft:lever", 70: "minecraft:stone_pressure_plate",
    71: "minecraft:iron_door", 72: "minecraft:oak_pressure_plate", 73: "minecraft:redstone_ore",
    75: "minecraft:redstone_torch", 76: "minecraft:redstone_torch", 77: "minecraft:stone_button",
    78: "minecraft:snow", 79: "minecraft:ice", 80: "minecraft:snow_block", 81: "minecraft:cactus",
    82: "minecraft:clay", 83: "minecraft:sugar_cane", 84: "minecraft:jukebox", 85: "minecraft:oak_fence",
    86: "minecraft:carved_pumpkin", 87: "minecraft:netherrack", 88: "minecraft:soul_sand",
    89: "minecraft:glowstone", 91: "minecraft:jack_o_lantern", 96: "minecraft:oak_trapdoor",
    99: "minecraft:brown_mushroom_block", 100: "minecraft:red_mushroom_block",
    101: "minecraft:iron_bars", 102: "minecraft:glass_pane", 103: "minecraft:melon",
    106: "minecraft:vine", 107: "minecraft:oak_fence_gate", 108: "minecraft:brick_stairs",
    109: "minecraft:stone_brick_stairs", 110: "minecraft:mycelium", 111: "minecraft:lily_pad",
    112: "minecraft:nether_bricks", 113: "minecraft:nether_brick_fence", 114: "minecraft:nether_brick_stairs",
    116: "minecraft:enchanting_table", 118: "minecraft:cauldron", 121: "minecraft:end_stone",
    123: "minecraft:redstone_lamp", 124: "minecraft:redstone_lamp", 128: "minecraft:sandstone_stairs",
    129: "minecraft:emerald_ore", 130: "minecraft:ender_chest", 133: "minecraft:emerald_block",
    134: "minecraft:spruce_stairs", 135: "minecraft:birch_stairs", 136: "minecraft:jungle_stairs",
    138: "minecraft:beacon", 139: "minecraft:cobblestone_wall", 145: "minecraft:anvil",
    146: "minecraft:trapped_chest", 151: "minecraft:daylight_detector", 152: "minecraft:redstone_block",
    154: "minecraft:hopper", 155: "minecraft:quartz_block", 156: "minecraft:quartz_stairs",
    163: "minecraft:acacia_stairs", 164: "minecraft:dark_oak_stairs", 165: "minecraft:slime_block",
    166: "minecraft:barrier", 167: "minecraft:iron_trapdoor", 168: "minecraft:prismarine",
    169: "minecraft:sea_lantern", 170: "minecraft:hay_block", 172: "minecraft:terracotta",
    173: "minecraft:coal_block", 174: "minecraft:packed_ice", 175: "minecraft:tall_grass",
    179: "minecraft:red_sandstone", 180: "minecraft:red_sandstone_stairs",
    198: "minecraft:end_rod", 201: "minecraft:purpur_block", 206: "minecraft:end_stone_bricks",
    208: "minecraft:dirt_path", 213: "minecraft:magma_block", 215: "minecraft:red_nether_bricks",
    216: "minecraft:bone_block",
}


def legacy_to_modern(bid, data):
    if bid == 0:
        return "minecraft:air"
    if bid == 5:
        return f"minecraft:{WOODS[data % 6]}_planks"
    if bid == 6:
        return f"minecraft:{WOODS[data % 6]}_sapling"
    if bid == 17:
        return f"minecraft:{WOODS[data & 3]}_log"
    if bid == 162:
        return f"minecraft:{'acacia' if (data & 1) == 0 else 'dark_oak'}_log"
    if bid == 18:
        return f"minecraft:{WOODS[data & 3]}_leaves"
    if bid == 161:
        return f"minecraft:{'acacia' if (data & 1) == 0 else 'dark_oak'}_leaves"
    if bid == 35:
        return f"minecraft:{COLORS[data & 15]}_wool"
    if bid == 95:
        return f"minecraft:{COLORS[data & 15]}_stained_glass"
    if bid == 160:
        return f"minecraft:{COLORS[data & 15]}_stained_glass_pane"
    if bid == 159:
        return f"minecraft:{COLORS[data & 15]}_terracotta"
    if bid == 171:
        return f"minecraft:{COLORS[data & 15]}_carpet"
    if bid == 251:
        return f"minecraft:{COLORS[data & 15]}_concrete"
    if bid == 252:
        return f"minecraft:{COLORS[data & 15]}_concrete_powder"
    if bid == 98:
        return f"minecraft:{STONE_BRICKS[data & 3]}"
    if bid in (43, 44):
        slabs = ("smooth_stone", "sandstone", "oak", "cobblestone", "brick", "stone_brick", "nether_brick", "quartz")
        name = slabs[data & 7]
        return f"minecraft:{name}_slab"
    if bid in (125, 126):
        return f"minecraft:{WOODS[data & 7 if (data & 7) < 6 else 0]}_slab"
    if bid == 97:
        return "minecraft:stone_bricks"
    return LEGACY_SIMPLE.get(bid, "minecraft:stone")


def state_string(palette_entry):
    name = str(palette_entry.get("Name", "minecraft:air"))
    if ":" not in name:
        name = "minecraft:" + name
    props = palette_entry.get("Properties")
    if not props:
        return name
    parts = []
    for k in sorted(props.keys()):
        ks = re.sub(r"[^a-z0-9_]", "", str(k).lower())
        vs = re.sub(r"[^a-z0-9_]", "", str(props[k]).lower())
        if ks and vs:
            parts.append(f"{ks}={vs}")
    if not parts:
        return name
    s = f"{name}[{','.join(parts[:16])}]"
    return s if STATE_RE.match(s) else name


def decode_section(section, min_y, max_y):
    y_byte = int(section.get("Y", 0))
    base_y = y_byte * 16
    if base_y + 15 < min_y or base_y > max_y:
        return []

    # 1.18+ format
    bs = section.get("block_states")
    if bs and bs.get("palette"):
        palette = bs["palette"]
        states = [state_string(p) for p in palette]
        n = len(states)
        if n == 1:
            if states[0] in AIR:
                return []
            return [(i, base_y + (i >> 8), states[0]) for i in range(4096) if min_y <= base_y + (i >> 8) <= max_y]
        data = bs.get("data")
        if data is None:
            return []
        bits = max(4, (n - 1).bit_length())
        per_long = 64 // bits
        mask = (1 << bits) - 1
        longs = np.asarray(data, dtype=np.uint64)
        shifts = np.arange(per_long, dtype=np.uint64) * np.uint64(bits)
        indices = ((longs[:, None] >> shifts) & np.uint64(mask)).reshape(-1)[:4096]
        return [
            (i, base_y + (i >> 8), states[int(idx) if int(idx) < n else 0])
            for i, idx in enumerate(indices)
            if min_y <= base_y + (i >> 8) <= max_y and states[int(idx) if int(idx) < n else 0] not in AIR
        ]

    # 1.13-1.17 format
    pal_old = section.get("Palette")
    bs_old = section.get("BlockStates")
    if pal_old and bs_old is not None:
        states = [state_string(p) for p in pal_old]
        n = len(states)
        bits = max(4, (n - 1).bit_length())
        mask = (1 << bits) - 1
        longs = np.asarray(bs_old, dtype=np.uint64)
        out = []
        for i in range(4096):
            y = base_y + (i >> 8)
            if y < min_y or y > max_y:
                continue
            bit_idx = i * bits
            w = bit_idx >> 6
            off = bit_idx & 63
            val = (int(longs[w]) >> off) & mask
            if off + bits > 64 and w + 1 < len(longs):
                val |= (int(longs[w + 1]) << (64 - off)) & mask
            val &= mask
            s = states[val if val < n else 0]
            if s not in AIR:
                out.append((i, y, s))
        return out

    # Legacy 1.2-1.12 format
    blocks_arr = section.get("Blocks")
    if blocks_arr is not None:
        bbytes = np.asarray(blocks_arr, dtype=np.uint8)
        data_arr = section.get("Data")
        dbytes = np.asarray(data_arr, dtype=np.uint8) if data_arr is not None else None
        add_arr = section.get("Add")
        abytes = np.asarray(add_arr, dtype=np.uint8) if add_arr is not None else None
        nz = np.nonzero(bbytes)[0]
        out = []
        for i_np in nz:
            i = int(i_np)
            y = base_y + (i >> 8)
            if y < min_y or y > max_y:
                continue
            bid = int(bbytes[i])
            if abytes is not None:
                nib = int(abytes[i >> 1])
                bid |= ((nib & 0x0F) if (i & 1) == 0 else (nib >> 4)) << 8
            dval = 0
            if dbytes is not None:
                dnib = int(dbytes[i >> 1])
                dval = (dnib & 0x0F) if (i & 1) == 0 else (dnib >> 4)
            s = legacy_to_modern(bid, dval)
            if s not in AIR:
                out.append((i, y, s))
        return out

    return []


def iter_region_chunks(world_dir, min_cx=None, max_cx=None, min_cz=None, max_cz=None):
    reg_dir = os.path.join(world_dir, "region")
    if not os.path.isdir(reg_dir):
        reg_dir = world_dir
    for path in sorted(glob.glob(os.path.join(reg_dir, "r.*.*.mca"))):
        base = os.path.basename(path).split(".")
        if len(base) == 4:
            rx, rz = int(base[1]), int(base[2])
            rcx0, rcz0 = rx * 32, rz * 32
            if min_cx is not None and (rcx0 + 31 < min_cx or rcx0 > max_cx):
                continue
            if min_cz is not None and (rcz0 + 31 < min_cz or rcz0 > max_cz):
                continue
        with open(path, "rb") as f:
            data = f.read()
        if len(data) < 4096:
            continue
        header = data[:4096]
        for i in range(1024):
            off = int.from_bytes(header[i * 4:i * 4 + 3], "big")
            if off == 0 or off * 4096 + 5 > len(data):
                continue
            pos = off * 4096
            ln = struct.unpack(">I", data[pos:pos + 4])[0]
            comp = data[pos + 4]
            raw = data[pos + 5:pos + 4 + ln]
            try:
                if comp == 2:
                    raw = zlib.decompress(raw)
                elif comp == 1:
                    raw = gzip.decompress(raw)
                root = nbtlib.File.parse(io.BytesIO(raw))
            except Exception:
                continue
            sections = root.get("sections")
            if sections is None:
                level = root.get("Level")
                if level is not None:
                    sections = level.get("Sections")
                    cx = int(level.get("xPos", 0))
                    cz = int(level.get("zPos", 0))
                else:
                    continue
            else:
                cx = int(root.get("xPos", 0))
                cz = int(root.get("zPos", 0))
            if min_cx is not None and (cx < min_cx or cx > max_cx):
                continue
            if min_cz is not None and (cz < min_cz or cz > max_cz):
                continue
            yield cx, cz, sections


def collect_blocks(world_dir, min_x, max_x, min_y, max_y, min_z, max_z, top_only=False):
    min_cx = (min_x >> 4) if min_x is not None else None
    max_cx = (max_x >> 4) if max_x is not None else None
    min_cz = (min_z >> 4) if min_z is not None else None
    max_cz = (max_z >> 4) if max_z is not None else None

    blocks = {}
    for cx, cz, sections in iter_region_chunks(world_dir, min_cx, max_cx, min_cz, max_cz):
        for section in sections:
            for idx, y, state in decode_section(section, min_y, max_y):
                x = cx * 16 + (idx & 15)
                z = cz * 16 + ((idx >> 4) & 15)
                if min_x is not None and (x < min_x or x > max_x):
                    continue
                if min_z is not None and (z < min_z or z > max_z):
                    continue
                if top_only:
                    old = blocks.get((x, z))
                    if old is None or y > old[0]:
                        blocks[(x, z)] = (y, state)
                else:
                    blocks[(x, y, z)] = state
    if top_only:
        return [(x, y, z, st) for (x, z), (y, st) in blocks.items()]
    return [(x, y, z, st) for (x, y, z), st in blocks.items()]


def cmd_scan(args):
    level_dat = os.path.join(args.world, "level.dat")
    spawn = None
    if os.path.isfile(level_dat):
        try:
            ld = nbtlib.load(level_dat)
            d = ld.get("Data", ld)
            spawn = (int(d.get("SpawnX", 0)), int(d.get("SpawnY", 64)), int(d.get("SpawnZ", 0)))
        except Exception:
            pass
    print(f"World: {args.world}  level.dat spawn={spawn}")

    chunk_stats = {}
    for cx, cz, sections in iter_region_chunks(args.world):
        cnt = 0
        min_y, max_y = 999, -999
        chests = 0
        wool_or_conc = 0
        for s in sections:
            dec = decode_section(s, -64, 320)
            cnt += len(dec)
            for _, y, st in dec:
                if y < min_y: min_y = y
                if y > max_y: max_y = y
                if "chest" in st: chests += 1
                if "_wool" in st or "_concrete" in st: wool_or_conc += 1
        if cnt > 0:
            chunk_stats[(cx, cz)] = (cnt, min_y, max_y, chests, wool_or_conc)

    print(f"Non-empty chunks: {len(chunk_stats)}")
    visited = set()
    clusters = []
    for c in chunk_stats:
        if c in visited:
            continue
        q = deque([c])
        visited.add(c)
        comp = []
        while q:
            ux, uz = q.popleft()
            comp.append((ux, uz))
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    nb = (ux + dx, uz + dz)
                    if nb in chunk_stats and nb not in visited:
                        visited.add(nb)
                        q.append(nb)
        total_b = sum(chunk_stats[p][0] for p in comp)
        total_ch = sum(chunk_stats[p][3] for p in comp)
        xs = [p[0] for p in comp]
        zs = [p[1] for p in comp]
        min_y = min(chunk_stats[p][1] for p in comp)
        max_y = max(chunk_stats[p][2] for p in comp)
        clusters.append((total_b, len(comp), min(xs) * 16, max(xs) * 16 + 15, min_y, max_y, min(zs) * 16, max(zs) * 16 + 15, total_ch))

    clusters.sort(reverse=True)
    for i, cl in enumerate(clusters[:25]):
        tb, nc, x0, x1, y0, y1, z0, z1, ch = cl
        print(f"Cluster {i:2d}: chunks={nc:4d} blocks={tb:8d} chests={ch:4d} box=[x:{x0}..{x1}, y:{y0}..{y1}, z:{z0}..{z1}] size=({x1-x0+1}x{y1-y0+1}x{z1-z0+1})")


def cmd_dump(args):
    blocks = collect_blocks(args.world, args.min_x, args.max_x, args.min_y, args.max_y, args.min_z, args.max_z, top_only=args.top)
    if not blocks:
        print("NO_BLOCKS_FOUND", file=sys.stderr)
        sys.exit(1)
    minx = min(b[0] for b in blocks); maxx = max(b[0] for b in blocks)
    miny = min(b[1] for b in blocks); maxy = max(b[1] for b in blocks)
    minz = min(b[2] for b in blocks); maxz = max(b[2] for b in blocks)
    sx, sy, sz = maxx - minx + 1, maxy - miny + 1, maxz - minz + 1
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with gzip.open(args.out, "wt", encoding="utf-8") as f:
        f.write(f"MCMDUMP 1 {sx} {sy} {sz}\n")
        for x, y, z, st in blocks:
            f.write(f"{x - minx} {y - miny} {z - minz} {st}\n")
    print(f"Wrote {len(blocks)} blocks -> {args.out} ({sx}x{sy}x{sz})")


def cmd_mcmi(args):
    blocks = collect_blocks(args.world, args.min_x, args.max_x, args.min_y, args.max_y, args.min_z, args.max_z, top_only=False)
    filtered = []
    for x, y, z, st in blocks:
        base = st.split("[", 1)[0]
        if base in BANNED or base in AIR:
            continue
        filtered.append((x, y, z, st))
    if not filtered:
        print("NO_BLOCKS_FOUND", file=sys.stderr)
        sys.exit(1)

    ox = args.origin_x if args.origin_x is not None else (min(b[0] for b in filtered) + max(b[0] for b in filtered)) // 2
    oy = args.origin_y if args.origin_y is not None else min(b[1] for b in filtered)
    oz = args.origin_z if args.origin_z is not None else (min(b[2] for b in filtered) + max(b[2] for b in filtered)) // 2

    palette = []
    pal_map = {}
    records = []
    for x, y, z, st in filtered:
        lx, ly, lz = x - ox, y - oy, z - oz
        idx = pal_map.get(st)
        if idx is None:
            idx = len(palette)
            palette.append(st)
            pal_map[st] = idx
        records.append((lx, ly, lz, idx))

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    with gzip.open(args.out, "wb") as f:
        f.write(b"MCMI")
        f.write(struct.pack(">ii", 1, len(palette)))
        for st in palette:
            enc = st.encode("utf-8")
            f.write(struct.pack(">H", len(enc)) + enc)
        f.write(struct.pack(">i", len(records)))
        for lx, ly, lz, idx in records:
            f.write(struct.pack(">hhhi", lx, ly, lz, idx))
    print(f"Wrote MCMI {len(records)} blocks (palette {len(palette)}, origin {ox},{oy},{oz}) -> {args.out}")


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)

    ps = sub.add_parser("scan")
    ps.add_argument("world")

    for name in ("dump", "mcmi"):
        sp = sub.add_parser(name)
        sp.add_argument("world")
        sp.add_argument("out")
        sp.add_argument("--min-x", type=int, default=None)
        sp.add_argument("--max-x", type=int, default=None)
        sp.add_argument("--min-y", type=int, default=-64)
        sp.add_argument("--max-y", type=int, default=320)
        sp.add_argument("--min-z", type=int, default=None)
        sp.add_argument("--max-z", type=int, default=None)
        if name == "dump":
            sp.add_argument("--top", action="store_true")
        else:
            sp.add_argument("--origin-x", type=int, default=None)
            sp.add_argument("--origin-y", type=int, default=None)
            sp.add_argument("--origin-z", type=int, default=None)

    args = p.parse_args()
    if args.cmd == "scan":
        cmd_scan(args)
    elif args.cmd == "dump":
        cmd_dump(args)
    else:
        cmd_mcmi(args)


if __name__ == "__main__":
    main()
