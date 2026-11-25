#!/usr/bin/env python3
"""Download matching vanilla client jars and compare every pack asset to vanilla.

Jars stay in the temp analysis directory, never in the deliverable.
"""
import hashlib
import json
import os
import urllib.request
import zipfile

TMP = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_vanilla_jars"
OUT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs\data"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def sha(b):
    return hashlib.sha256(b).hexdigest()


def main():
    os.makedirs(TMP, exist_ok=True)
    man = json.loads(urllib.request.urlopen(MANIFEST, timeout=60).read())
    versions = {v["id"]: v["url"] for v in man["versions"]}
    jars = {}
    for ver in ("1.16.5", "1.19.2", "1.20.1", "1.21.1"):
        path = os.path.join(TMP, f"{ver}.jar")
        if not os.path.exists(path):
            meta = json.loads(urllib.request.urlopen(versions[ver], timeout=60).read())
            url = meta["downloads"]["client"]["url"]
            print("downloading", ver)
            with urllib.request.urlopen(url, timeout=300) as r, open(path, "wb") as f:
                f.write(r.read())
        jars[ver] = zipfile.ZipFile(path)
    # compare each pack file (textures + lang + models + sounds.json) against candidate jars
    import csv
    rows = list(csv.DictReader(open(os.path.join(OUT, "inventory.csv"), encoding="utf-8")))
    out = []
    for r in rows:
        if r["pack"] not in ("clouds2", "yeti_set_go", "space_race"):
            continue
        rel = r["path"]
        if not (rel.endswith(".png") or rel.endswith("en_us.json") or rel.endswith(".json")):
            continue
        path = os.path.join(os.path.dirname(OUT), "packs", r["pack"], "extracted", rel)
        data = open(path, "rb").read()
        h = sha(data)
        match = ""
        for ver, z in jars.items():
            for cand in (rel, "assets/" + rel):
                try:
                    if sha(z.read(cand)) == h:
                        match = ver
                        break
                except KeyError:
                    pass
            if match:
                break
        out.append({"pack": r["pack"], "path": rel, "sha256": h, "identical_to_vanilla": match})
    with open(os.path.join(OUT, "vanilla_match.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["pack", "path", "sha256", "identical_to_vanilla"])
        w.writeheader()
        w.writerows(out)
    print("compared", len(out))


if __name__ == "__main__":
    main()
