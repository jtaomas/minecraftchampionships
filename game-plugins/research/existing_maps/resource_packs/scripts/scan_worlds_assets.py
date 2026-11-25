#!/usr/bin/env python3
"""Scan staged worlds for resource-pack references: sounds, custom item names/lore,
CustomModelData, and containers. Asset mapping only; no route analysis.

Writes JSON + CSV inside resource_packs/data/.
"""
import csv
import gzip
import io
import json
import os
import re
import zlib

import nbtlib

MAPS = {
    "clouds2": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Clouds_2_-_Electric_Boogaloo__Download_Version_\Clouds 2 - Electric Boogaloo",
    "city_marathon": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\city-marathon\City Marathon",
    "space_race": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race",
    "yeti_set_go": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\YetiSetGo",
    "decision_dome": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Decision_Dome\Decision Dome - Recreation",
    "minigames": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3",
    "tgttos_badlands": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat",
    "tgttos_industry": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat",
    "tgttos_pits": r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat",
}
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data"
KEYWORDS = re.compile(r"acerace|playsound|resourcepack|custom_model_data|CustomModelData|give |summon item|item replace|rotten_flesh|glazed_terracotta", re.I)


def iter_region(path):
    with open(path, "rb") as fh:
        data = fh.read()
    if len(data) < 8192:
        return
    for idx in range(1024):
        entry = data[idx * 4: idx * 4 + 4]
        if len(entry) < 4:
            continue
        off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
        if not off:
            continue
        pos = off * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos + 4], "big")
        comp = data[pos + 4]
        if comp not in (1, 2, 3):
            continue
        raw = data[pos + 5: pos + 4 + length]
        try:
            if comp == 2:
                raw = zlib.decompress(raw)
            elif comp == 1:
                raw = gzip.decompress(raw)
            yield nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue


def s(v):
    return str(v) if v is not None else ""


def text_of(node):
    """Flatten a chat-component NBT into a readable string."""
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    if isinstance(node, (int, float)):
        return str(node)
    if isinstance(node, dict) or hasattr(node, "items"):
        out = []
        if "text" in node:
            out.append(str(node["text"]))
        if "extra" in node:
            for e in node["extra"]:
                out.append(text_of(e))
        if "translate" in node:
            out.append(str(node["translate"]))
        if not out:
            try:
                return json.dumps(nbtlib.serialize_tag(node), ensure_ascii=False)
            except Exception:
                return str(node)
        return "".join(out)
    try:
        return str(node)
    except Exception:
        return ""


def walk_items(node, path, out):
    """Find every item stack: dicts with id + Count, and record display/model data."""
    if isinstance(node, list):
        for i, v in enumerate(node):
            walk_items(v, f"{path}[{i}]", out)
        return
    if isinstance(node, dict) or hasattr(node, "items"):
        keys = set(node.keys())
        if "id" in keys and ("Count" in keys or "count" in keys):
            item = s(node.get("id"))
            tag = node.get("tag") or node.get("components")
            if tag:
                cmm = None
                name = lore = ""
                if "CustomModelData" in tag:
                    cmm = s(tag["CustomModelData"])
                disp = tag.get("display")
                if disp:
                    name = text_of(disp.get("Name"))
                    if "Lore" in disp:
                        lore = " | ".join(text_of(x) for x in disp["Lore"])
                comps = tag.get("minecraft:custom_model_data") or tag.get("custom_model_data")
                if comps is not None and cmm is None:
                    cmm = s(comps)
                if name or lore or cmm:
                    out.append({"path": path, "item": item, "name": name, "lore": lore, "custom_model_data": cmm or "",
                                "raw_tag": json.dumps(nbtlib.serialize_tag(tag), ensure_ascii=False)[:2000]})
        for k, v in node.items():
            walk_items(v, f"{path}/{k}", out)


def main():
    os.makedirs(OUT, exist_ok=True)
    commands = []
    items = []
    sound_refs = []
    containers = []
    for slug, root in MAPS.items():
        print("scan", slug)
        region_dirs = [os.path.join(root, "region")]
        for extra in ("DIM-1/region", "DIM1/region"):
            p = os.path.join(root, extra)
            if os.path.isdir(p):
                region_dirs.append(p)
        for rd in region_dirs:
            if not os.path.isdir(rd):
                continue
            for fn in sorted(os.listdir(rd)):
                if not fn.endswith(".mca"):
                    continue
                for chunk in iter_region(os.path.join(rd, fn)):
                    cx = int(chunk.get("xPos", 0)); cz = int(chunk.get("zPos", 0))
                    for be in chunk.get("block_entities", []):
                        bid = s(be.get("id"))
                        x, y, z = int(be.get("x", 0)), int(be.get("y", 0)), int(be.get("z", 0))
                        if bid == "minecraft:command_block":
                            cmd = s(be.get("Command"))
                            commands.append({"map": slug, "x": x, "y": y, "z": z, "command": cmd,
                                             "auto": s(be.get("auto")), "conditional": s(be.get("conditionMet")),
                                             "powered": s(be.get("powered"))})
                            for m in re.finditer(r"playsound\s+([a-z0-9_.:/-]+)", cmd, re.I):
                                sound_refs.append({"map": slug, "x": x, "y": y, "z": z, "sound": m.group(1), "via": "command_block"})
                        if bid in ("minecraft:shulker_box",) or "Items" in be:
                            found = []
                            walk_items(be.get("Items"), f"{slug}@{x},{y},{z}", found)
                            containers.append({"map": slug, "x": x, "y": y, "z": z, "id": bid, "items": len(found)})
                            items.extend(found)
                    # entity-stored containers? scan chunk 'Entities' if present; plus structure templates handled later
                    for ent in chunk.get("Entities", []) or []:
                        eid = s(ent.get("id"))
                        if eid in ("minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:armor_stand", "minecraft:item"):
                            found = []
                            walk_items(ent.get("Item"), f"{slug}@entity", found)
                            found2 = []
                            walk_items(ent, f"{slug}@entity", found2)
                            items.extend(found)
                            items.extend(found2)
    with open(os.path.join(OUT, "command_asset_refs.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "x", "y", "z", "command", "auto", "conditional", "powered"])
        w.writeheader(); w.writerows(commands)
    with open(os.path.join(OUT, "sound_refs.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "x", "y", "z", "sound", "via"])
        w.writeheader(); w.writerows(sound_refs)
    with open(os.path.join(OUT, "item_nbt.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "path", "item", "name", "lore", "custom_model_data", "raw_tag"])
        w.writeheader(); w.writerows(items)
    with open(os.path.join(OUT, "containers.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "x", "y", "z", "id", "items"])
        w.writeheader(); w.writerows(containers)
    print("commands", len(commands), "sound refs", len(sound_refs), "items", len(items), "containers", len(containers))


if __name__ == "__main__":
    main()
