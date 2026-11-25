#!/usr/bin/env python3
"""Extract the staged resource packs into resource_packs/packs/ and inventory every file.

Read-only on the source archives; writes only inside resource_packs/.
"""
import csv
import hashlib
import json
import os
import shutil
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps"
PACKS = [
    ("clouds2", os.path.join(SRC, "Clouds_2_-_Electric_Boogaloo__Download_Version_", "Clouds 2 - Electric Boogaloo", "resources.zip")),
    ("space_race", os.path.join(SRC, "Space_Race", "Space Race", "resources.zip.zip")),
    ("yeti_set_go", os.path.join(SRC, "YetiSetGo", "resources.zip")),
    ("yeti_zipped_map_dup", os.path.join(SRC, "zipped-map", "resources.zip")),
]


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def classify(name):
    low = name.lower()
    if low.endswith("pack.mcmeta"):
        return "metadata"
    if low.endswith("pack.png"):
        return "pack_icon"
    if low.endswith(".mcmeta"):
        return "animation_meta"
    if low.endswith(".png"):
        return "texture"
    if low.endswith(".psd"):
        return "source_file"
    if low.endswith(".ogg"):
        return "sound"
    if low.endswith(".json"):
        if "/lang/" in low:
            return "language"
        if "/models/" in low:
            return "model"
        if "/blockstates/" in low:
            return "blockstate"
        if low.endswith("sounds.json"):
            return "sound_index"
        return "json_other"
    if low.endswith(".txt") or low.endswith(".ini") or low.endswith(".dat"):
        return "junk"
    return "other"


def main():
    os.makedirs(os.path.join(HERE, "packs"), exist_ok=True)
    rows = []
    hashes = {}
    for slug, src in PACKS:
        assert os.path.isfile(src), src
        dest = os.path.join(HERE, "packs", slug)
        os.makedirs(dest, exist_ok=True)
        orig = os.path.join(dest, "resources.zip")
        if not os.path.exists(orig):
            shutil.copy2(src, orig)
        extracted = os.path.join(dest, "extracted")
        if os.path.isdir(extracted):
            shutil.rmtree(extracted)
        with zipfile.ZipFile(src) as z:
            z.extractall(extracted)
        digest = sha256(src)
        hashes[slug] = digest
        with zipfile.ZipFile(src) as z:
            for i in z.infolist():
                if i.is_dir():
                    continue
                path = os.path.join(extracted, i.filename)
                kind = classify(i.filename)
                w = h = None
                mode = None
                if kind == "texture" or kind == "pack_icon":
                    try:
                        with Image.open(path) as im:
                            w, h = im.size
                            mode = im.mode
                    except Exception:
                        pass
                rows.append({
                    "pack": slug,
                    "path": i.filename,
                    "bytes": i.file_size,
                    "sha256": sha256(path),
                    "kind": kind,
                    "width": w or "",
                    "height": h or "",
                    "mode": mode or "",
                    "crc32": f"{i.CRC:08x}",
                })
    os.makedirs(os.path.join(HERE, "data"), exist_ok=True)
    with open(os.path.join(HERE, "data", "inventory.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    with open(os.path.join(HERE, "data", "pack_hashes.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["pack", "sha256", "source_path", "files", "bytes"])
        for slug, src in PACKS:
            n = sum(1 for r in rows if r["pack"] == slug)
            b = sum(r["bytes"] for r in rows if r["pack"] == slug)
            w.writerow([slug, hashes[slug], src, n, b])
    print(f"{len(rows)} files inventoried")
    for slug in hashes:
        print(slug, hashes[slug])


if __name__ == "__main__":
    main()
