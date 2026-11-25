#!/usr/bin/env python3
"""Pixel-level and JSON-level comparison of pack assets against vanilla jars."""
import csv
import json
import os
import zipfile

import numpy as np
from PIL import Image

TMP = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_vanilla_jars"
HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
OUT = os.path.join(HERE, "data")
VERS = ("1.16.5", "1.19.2", "1.20.1", "1.21.1")


def load_img_bytes(b):
    import io
    return Image.open(io.BytesIO(b)).convert("RGBA")


def main():
    jars = {v: zipfile.ZipFile(os.path.join(TMP, f"{v}.jar")) for v in VERS}
    rows = list(csv.DictReader(open(os.path.join(OUT, "inventory.csv"), encoding="utf-8")))
    out = []
    for r in rows:
        if r["pack"] not in ("clouds2", "yeti_set_go", "space_race"):
            continue
        rel = r["path"]
        vrel = rel
        if vrel.startswith("Space Race Resource Pack/"):
            vrel = vrel[len("Space Race Resource Pack/"):]
        path = os.path.join(HERE, "packs", r["pack"], "extracted", rel)
        if rel.endswith(".png"):
            img = load_img_bytes(open(path, "rb").read())
            arr = np.asarray(img).astype(int)
            best = None
            for ver in VERS:
                vimg = None
                for cand in (vrel, "assets/" + vrel):
                    try:
                        vimg = load_img_bytes(jars[ver].read(cand))
                        break
                    except KeyError:
                        continue
                if vimg is None or vimg.size != img.size:
                    continue
                varr = np.asarray(vimg).astype(int)
                diff = np.abs(arr - varr)
                mad = float(diff.mean())
                exact = bool((diff == 0).all())
                if best is None or mad < best[1]:
                    best = (ver, mad, exact)
            out.append({"pack": r["pack"], "path": rel, "kind": r["kind"],
                        "nearest_vanilla": best[0] if best else "",
                        "mean_abs_diff": round(best[1], 3) if best else "",
                        "pixel_identical": best[2] if best else ""})
        elif rel.endswith("en_us.json"):
            pack = json.load(open(path, encoding="utf-8-sig"))
            best = None
            for ver in VERS:
                try:
                    van = json.loads(jars[ver].read("assets/minecraft/lang/en_us.json").decode("utf-8-sig"))
                except KeyError:
                    continue
                same = sum(1 for k, val in pack.items() if van.get(k) == val)
                only = [k for k in pack if k not in van]
                diff = [k for k in pack if k in van and van[k] != pack[k]]
                missing = [k for k in van if k not in pack]
                if best is None or (same - len(diff)) > best[1]:
                    best = (ver, same, len(only), len(diff), len(missing))
            out.append({"pack": r["pack"], "path": rel, "kind": r["kind"],
                        "nearest_vanilla": best[0] if best else "",
                        "mean_abs_diff": "",
                        "pixel_identical": "",
                        "json_same": best[1] if best else "",
                        "json_pack_only": best[2] if best else "",
                        "json_value_changed": best[3] if best else "",
                        "json_missing_from_pack": best[4] if best else ""})
        else:
            out.append({"pack": r["pack"], "path": rel, "kind": r["kind"],
                        "nearest_vanilla": "", "mean_abs_diff": "", "pixel_identical": ""})
    fields = ["pack", "path", "kind", "nearest_vanilla", "mean_abs_diff", "pixel_identical",
              "json_same", "json_pack_only", "json_value_changed", "json_missing_from_pack"]
    with open(os.path.join(OUT, "vanilla_pixel_match.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(out)
    for r in out:
        if r["kind"] in ("texture",):
            print(f"{r['pack']:12} {str(r['pixel_identical']):5} mad={str(r['mean_abs_diff']):>8} {r['nearest_vanilla']:7} {r['path']}")
    for r in out:
        if r["kind"] == "language":
            print("LANG", r)


if __name__ == "__main__":
    main()
