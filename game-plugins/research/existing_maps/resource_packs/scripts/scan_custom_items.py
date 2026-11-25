#!/usr/bin/env python3
"""Scan every staged world's playerdata + item frames for custom item display names/lore/NBT."""
import csv
import glob
import os

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


def text_of(node):
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    out = []
    if hasattr(node, "get"):
        if "text" in node:
            out.append(str(node["text"]))
        if "extra" in node:
            for e in node["extra"]:
                out.append(text_of(e))
        if "translate" in node:
            out.append(str(node["translate"]))
    return "".join(out) or str(node)


def walk(n, path, out):
    if isinstance(n, list):
        for i, v in enumerate(n):
            walk(v, f"{path}[{i}]", out)
        return
    if hasattr(n, "items"):
        if "id" in n and ("Count" in n or "count" in n):
            tag = n.get("tag") or n.get("components")
            name = lore = cmm = ""
            if tag:
                d = tag.get("display")
                if d:
                    name = text_of(d.get("Name"))
                    if "Lore" in d:
                        lore = " | ".join(text_of(x) for x in d["Lore"])
                cmm = str(tag.get("CustomModelData", ""))
            out.append((path, str(n["id"]), name, lore, cmm))
        for k, v in n.items():
            walk(v, f"{path}/{k}", out)


def main():
    rows = []
    for slug, root in MAPS.items():
        files = glob.glob(os.path.join(root, "playerdata", "*.dat")) + [os.path.join(root, "level.dat")]
        for p in files:
            if not os.path.exists(p):
                continue
            try:
                f = nbtlib.load(p)
            except Exception:
                continue
            out = []
            walk(f, os.path.basename(p), out)
            for path, item, name, lore, cmm in out:
                if name or lore or cmm:
                    rows.append({"map": slug, "file": path, "item": item, "name": name, "lore": lore,
                                 "custom_model_data": cmm})
    with open(os.path.join(OUT, "custom_items.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["map", "file", "item", "name", "lore", "custom_model_data"])
        w.writeheader()
        w.writerows(rows)
    for r in rows:
        print(r)


if __name__ == "__main__":
    main()
