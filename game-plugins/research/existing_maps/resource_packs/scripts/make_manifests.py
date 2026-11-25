#!/usr/bin/env python3
"""Emit animation_manifest.csv, model_inventory.csv, lang_diff.csv, pack_meta.csv."""
import csv
import json
import os
import zipfile

from PIL import Image

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
DATA = os.path.join(HERE, "data")
JARS = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_vanilla_jars"


def write(name, fields, rows):
    with open(os.path.join(DATA, name), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(name, len(rows))


def main():
    # animations
    anim = []
    for pack in ("clouds2", "yeti_set_go", "space_race"):
        root = os.path.join(HERE, "packs", pack, "extracted")
        for base, _, files in os.walk(root):
            for f in files:
                if not f.endswith(".png.mcmeta"):
                    continue
                p = os.path.join(base, f)
                png = p[:-7]
                meta = json.load(open(p, encoding="utf-8"))["animation"]
                w, h = Image.open(png).size
                ft = meta.get("frametime", 1)
                frames = meta.get("frames")
                n = h // 16 if w == 16 else (w // 16 if h == 16 else h // 16)
                if frames:
                    n = len(frames)
                anim.append({"pack": pack, "texture": os.path.relpath(png, root).replace("\\", "/"),
                             "width": w, "height": h, "frames": n, "frametime": ft,
                             "frames_list": json.dumps(frames) if frames else "",
                             "interpolate": meta.get("interpolate", False),
                             "purpose": "glazed pad" if "glazed" in f else ("light" if "sea_lantern" in f else "Energy item")})
    write("animation_manifest.csv", list(anim[0].keys()), anim)

    # models
    models = []
    for pack in ("clouds2", "yeti_set_go"):
        root = os.path.join(HERE, "packs", pack, "extracted")
        for base, _, files in os.walk(os.path.join(root, "assets", "minecraft", "models")):
            for f in sorted(files):
                if not f.endswith(".json"):
                    continue
                p = os.path.join(base, f)
                j = json.load(open(p, encoding="utf-8"))
                rel = os.path.relpath(p, root).replace("\\", "/")
                target = rel.replace("assets/minecraft/models/", "").replace(".json", "")
                vanilla = ""
                try:
                    v = json.loads(zipfile.ZipFile(os.path.join(JARS, "1.20.1.jar")).read(rel))
                    vanilla = v.get("parent", "") + " " + json.dumps(v.get("textures", {}), sort_keys=True)
                except Exception:
                    pass
                models.append({"pack": pack, "model": rel, "target": target,
                               "parent": j.get("parent", ""), "textures": json.dumps(j.get("textures", {}), sort_keys=True),
                               "vanilla_1_20_1": vanilla, "note": "cube_all override" if "glazed" in rel else "vanilla-identical item model"})
    write("model_inventory.csv", list(models[0].keys()), models)

    # lang diff
    van = json.loads(zipfile.ZipFile(os.path.join(JARS, "1.19.2.jar")).read("assets/minecraft/lang/en_us.json").decode("utf-8-sig"))
    rows = []
    for pack in ("clouds2", "yeti_set_go"):
        p = os.path.join(HERE, "packs", pack, "extracted", "assets", "minecraft", "lang", "en_us.json")
        j = json.load(open(p, encoding="utf-8-sig"))
        for k, v in j.items():
            if k not in van:
                rows.append({"pack": pack, "key": k, "status": "pack-only", "vanilla_1_19_2": "", "pack_value": v})
            elif van[k] != v:
                rows.append({"pack": pack, "key": k, "status": "changed", "vanilla_1_19_2": van[k], "pack_value": v})
    write("lang_diff.csv", list(rows[0].keys()), rows)

    # pack meta
    metas = []
    for pack in ("clouds2", "space_race", "yeti_set_go"):
        root = os.path.join(HERE, "packs", pack, "extracted")
        if pack == "space_race":
            root = os.path.join(root, "Space Race Resource Pack")
        j = json.load(open(os.path.join(root, "pack.mcmeta"), encoding="utf-8"))
        metas.append({"pack": pack, "pack_format": j["pack"].get("pack_format"),
                      "description": j["pack"].get("description", ""),
                      "supported_formats": j["pack"].get("supported_formats", ""),
                      "root_pack_mcmeta": os.path.exists(os.path.join(HERE, "packs", pack, "extracted", "pack.mcmeta")),
                      "archive_root_ok": pack != "space_race"})
    write("pack_meta.csv", list(metas[0].keys()), metas)


if __name__ == "__main__":
    main()
