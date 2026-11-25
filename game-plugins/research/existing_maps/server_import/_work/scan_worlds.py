"""Read-only scanner for the imported review worlds (A12).

Reads level.dat and region files directly from the review server directory.
Writes nothing outside server_import/_work and data/.
"""
import json
import math
import os
import struct
import sys
import zlib
from pathlib import Path

import nbtlib

SERVER = Path(r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review")
WORLDS = [
    "mcc_clouds2", "mcc_citymarathon", "mcc_spacerace", "mcc_yetisetgo",
    "mcc_decisiondome", "mcc_minigames", "mcc_tgttos_badlands",
    "mcc_tgttos_industry", "mcc_tgttos_pits",
]
OUT = Path(__file__).resolve().parent


def nbt_to_python(tag):
    """Convert nbtlib tags to plain python for JSON."""
    import nbtlib.tag as t
    if isinstance(tag, t.Compound):
        return {str(k): nbt_to_python(v) for k, v in tag.items()}
    if isinstance(tag, t.List):
        return [nbt_to_python(v) for v in tag]
    if isinstance(tag, t.String):
        return str(tag)
    if isinstance(tag, (t.Byte, t.Short, t.Int, t.Long)):
        return int(tag)
    if isinstance(tag, (t.Float, t.Double)):
        return float(tag)
    if isinstance(tag, t.ByteArray):
        return list(tag)
    if isinstance(tag, t.IntArray):
        return list(tag)
    if isinstance(tag, t.LongArray):
        return list(tag)
    return tag


def read_region(path):
    """Yield (cx, cz, nbt-root) for every present chunk in a .mca file."""
    data = path.read_bytes()
    if len(data) < 8192:
        return
    parts = path.stem.split(".")
    try:
        rx, rz = int(parts[1]), int(parts[2])
    except (IndexError, ValueError):
        rx, rz = 0, 0
    for i in range(1024):
        off = int.from_bytes(data[i * 4:i * 4 + 3], "big")
        sectors = data[i * 4 + 3]
        if off == 0 or sectors == 0:
            continue
        start = off * 4096
        if start + 5 > len(data):
            continue
        length = int.from_bytes(data[start:start + 4], "big")
        comp = data[start + 4]
        payload = data[start + 5:start + 4 + length]
        try:
            if comp == 1:
                raw = zlib.decompress(payload, 16 + zlib.MAX_WBITS)
            elif comp == 2:
                raw = zlib.decompress(payload)
            elif comp == 3:
                raw = payload
            else:
                continue
            root = nbtlib.File.parse(__import__("io").BytesIO(raw))
            yield rx * 32 + i % 32, rz * 32 + i // 32, root
        except Exception as exc:  # noqa: BLE001
            print(f"  ! chunk {i % 32},{i // 32} in {path.name}: {exc}", file=sys.stderr)


def level_of(root):
    return root.get("Level", root)


def decode_palette(palette, longs, bits, count=4096):
    """Decode 1.16+ non-spanning packed block states."""
    if not palette:
        return []
    bits = max(4, bits)
    per_long = 64 // bits
    mask = (1 << bits) - 1
    out = []
    if longs is None or len(longs) == 0:
        return [0] * count
    for value in longs:
        v = int(value) & 0xFFFFFFFFFFFFFFFF
        for _ in range(per_long):
            out.append(v & mask)
            v >>= bits
            if len(out) >= count:
                return out
    while len(out) < count:
        out.append(0)
    return out


def section_palette(section):
    if "block_states" in section:
        bs = section["block_states"]
        return bs.get("palette", []), bs.get("data", [])
    return section.get("Palette", []), section.get("BlockStates", [])


def section_blocks(section):
    """Return dict local index -> palette entry as (name, props)."""
    palette, longs = section_palette(section)
    n = len(palette)
    if n == 0:
        return {}
    bits = max(4, math.ceil(math.log2(n))) if n > 1 else 4
    indices = decode_palette(palette, longs, bits)
    pal = []
    for p in palette:
        pal.append((str(p.get("Name", "")), {str(k): str(v) for k, v in p.get("Properties", {}).items()}))
    return {i: pal[idx] for i, idx in enumerate(indices) if idx < len(pal)}


def scan_world(world):
    wdir = SERVER / world
    out = {"world": world, "folder": str(wdir)}
    if not wdir.exists():
        out["missing"] = True
        return out
    lvl = nbtlib.load(str(wdir / "level.dat"))
    data = lvl["Data"]
    out["level"] = {
        "version_name": str(data.get("Version", {}).get("Name", "")),
        "data_version": int(data.get("DataVersion", 0)),
        "spawn": [int(data.get("SpawnX", 0)), int(data.get("SpawnY", 0)), int(data.get("SpawnZ", 0))],
        "spawn_angle": [float(data.get("SpawnAngle", 0.0)), float(data.get("SpawnPitch", 0.0))],
        "difficulty": int(data.get("Difficulty", 2)),
        "hardcore": int(data.get("hardcore", 0)),
        "raining": int(data.get("raining", 0)),
        "thundering": int(data.get("thundering", 0)),
        "day_time": int(data.get("DayTime", 0)),
        "time": int(data.get("Time", 0)),
        "last_played": int(data.get("LastPlayed", 0)),
        "level_name": str(data.get("LevelName", "")),
        "generator": int(data.get("generatorName", 0)) if "generatorName" in data else None,
        "gamerules": {str(k): str(v) for k, v in data.get("GameRules", {}).items()},
        "datapacks": nbt_to_python(data.get("DataPacks", {})),
        "dimensions": list(data.get("WorldGenSettings", {}).get("dimensions", {}).keys()),
        "enabled_features": nbt_to_python(data.get("WorldGenSettings", {}).get("enabled_features", [])),
    }
    out["paper_world_yml"] = (wdir / "paper-world.yml").read_text(errors="replace")[:20000] if (wdir / "paper-world.yml").exists() else ""
    out["datapack_folders"] = sorted(p.name for p in (wdir / "datapacks").glob("*")) if (wdir / "datapacks").exists() else []
    out["resources_zip"] = sorted(p.name for p in wdir.glob("resources*"))

    cmd_blocks = []
    state_cmd = {}
    signs = []
    other_be = {}
    chunks = 0
    region_files = list((wdir / "region").glob("*.mca"))
    for dim, sub in (("overworld", "region"), ("nether", "DIM-1/region"), ("end", "DIM1/region")):
        for rp in sorted((wdir / sub).glob("*.mca")):
            region_files.append(rp) if sub != "region" else None
            for cx, cz, root in read_region(rp):
                chunks += 1
                lev = level_of(root)
                be_list = lev.get("block_entities", lev.get("TileEntities", []))
                for be in be_list:
                    bid = str(be.get("id", ""))
                    rec = {
                        "dim": dim,
                        "x": int(be.get("x", 0)), "y": int(be.get("y", 0)), "z": int(be.get("z", 0)),
                        "id": bid,
                    }
                    if "command_block" in bid:
                        rec.update({
                            "command": str(be.get("Command", "")),
                            "auto": int(be.get("auto", 0)),
                            "conditionMet": int(be.get("conditionMet", 0)),
                            "powered": int(be.get("powered", 0)),
                            "track_output": int(be.get("TrackOutput", 1)),
                            "last_output": nbt_to_python(be.get("LastOutput", "")),
                            "last_execution": int(be.get("LastExecution", -1)),
                            "update_last_execution": int(be.get("UpdateLastExecution", 1)),
                            "custom_name": nbt_to_python(be.get("CustomName", "")),
                        })
                        cmd_blocks.append(rec)
                    elif "sign" in bid:
                        rec["front_text"] = nbt_to_python(be.get("front_text", {}))
                        rec["back_text"] = nbt_to_python(be.get("back_text", {}))
                        for i in range(1, 5):
                            rec[f"text{i}"] = nbt_to_python(be.get(f"Text{i}", ""))
                        rec["color"] = str(be.get("Color", ""))
                        rec["glowing"] = int(be.get("GlowingText", 0))
                        signs.append(rec)
                    else:
                        other_be[bid] = other_be.get(bid, 0) + 1
                # block states, to identify command block mode/type and inspect spawn
                for sec in lev.get("sections", lev.get("Sections", [])):
                    y = int(sec.get("Y", 0))
                    palette, _longs = section_palette(sec)
                    if not any("command_block" in str(p.get("Name", "")) for p in palette):
                        continue
                    blocks = section_blocks(sec)
                    for idx, (name, props) in blocks.items():
                        if "command_block" in name:
                            lx, lz, ly = idx & 15, (idx >> 4) & 15, idx >> 8
                            state_cmd[(dim, cx * 16 + lx, y * 16 + ly, cz * 16 + lz)] = (name, props)
    for c in cmd_blocks:
        key = (c["dim"], c["x"], c["y"], c["z"])
        if key in state_cmd:
            c["block"], c["props"] = state_cmd[key]
        else:
            c["block"] = c.get("id", "")
            c["props"] = {}
    out["chunk_count"] = chunks
    out["region_file_count"] = len(region_files)
    out["command_blocks"] = cmd_blocks
    out["signs"] = signs
    out["other_block_entities"] = other_be
    return out


def main():
    result = {}
    for w in WORLDS:
        print(f"scanning {w} ...", flush=True)
        result[w] = scan_world(w)
        r = result[w]
        print(f"  chunks={r.get('chunk_count')} signs={len(r.get('signs', []))} cmd={len(r.get('command_blocks', []))}", flush=True)
    (OUT / "scan.json").write_text(json.dumps(result, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main()
