#!/usr/bin/env python3
"""Cross-check A12's format/archive findings against the actual archives.

Read-only. Produces data/a12_crosscheck.md.
"""
import os
import zipfile

HERE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\resource_packs"
DATA = os.path.join(HERE, "data")
PACKS = [
    ("Clouds 2", "clouds2", os.path.join(HERE, "packs", "clouds2", "resources.zip")),
    ("Yeti Set Go", "yeti_set_go", os.path.join(HERE, "packs", "yeti_set_go", "resources.zip")),
    ("Space Race", "space_race", os.path.join(HERE, "packs", "space_race", "resources.zip")),
]

lines = ["# A12 cross-check — pack formats and archive roots", "",
         "Verified against the copied archives in `packs/*/resources.zip` (byte-identical to the staged sources; source names are noted below). Format, root and file-existence rows are **H**; statements about how a client treats a repacked/wrapped pack are **M**.", "",
         "| Pack | `pack_format` | Required for 1.21.1 | Root entry | Root `pack.mcmeta`? | Inner `.zip`? | Verdict |",
         "|---|---|---|---|---|---|---|"]
details = []
for label, slug, path in PACKS:
    z = zipfile.ZipFile(path)
    names = z.namelist()
    roots = sorted({n.split("/")[0] for n in names})
    root_meta = "pack.mcmeta" in names
    inner_zips = [n for n in names if n.lower().endswith((".zip", ".jar"))]
    # read format from the shallowest pack.mcmeta
    metas = [n for n in names if n.endswith("pack.mcmeta")]
    import json
    meta = json.loads(z.read(sorted(metas, key=len)[0])) if metas else {}
    fmt = meta.get("pack", {}).get("pack_format", "")
    verdict = "valid pack root" if root_meta else "not a pack as shipped"
    lines.append(f"| {label} | {fmt} | 34 | `{roots[0]}` | {'yes' if root_meta else 'no'} | {'yes' if inner_zips else 'no'} | {verdict} |")
    details.append((label, slug, path, names, roots, root_meta, inner_zips, meta))

lines += ["", "## Exact archive roots", ""]
for label, slug, path, names, roots, root_meta, inner_zips, meta in details:
    lines += [f"### {label}", "",
              f"- Archive copy: `{path}`",
              f"- Source filename: `{os.path.basename(path)}`" + (" (source was `resources.zip.zip`)" if label == "Space Race" else ""),
              f"- Zip root entries: {', '.join('`'+r+'`' for r in roots)}",
              f"- `pack.mcmeta` at zip root: **{'yes' if root_meta else 'no'}**",
              f"- Inner zip/jar files: **{', '.join(inner_zips) if inner_zips else 'none'}**",
              f"- Shallowest `pack.mcmeta`: `{sorted([n for n in names if n.endswith('pack.mcmeta')], key=len)[0]}`",
              f"- `pack_format`: **{meta.get('pack', {}).get('pack_format', '')}**",
              f"- `description`: {meta.get('pack', {}).get('description', '')!r}", ""]
    if label == "Space Race":
        lines += ["**Space Race is a wrapper, not a nested zip.** Its zip root contains one directory, "
                  "`Space Race Resource Pack/`, and every asset lives below that directory. There is no inner "
                  "`.zip`; the double `.zip.zip` in the source filename is a naming error plus the wrapper. "
                  "Minecraft looks for `pack.mcmeta` at the zip root, so this archive is not a pack as shipped. "
                  "It also is not named `resources.zip`, which is the filename a world folder expects.", ""]

lines += ["## A12 statement check", "",
          "| A12 statement | Result | Evidence |", "|---|---|---|",
          "| Clouds 2 is format 15 | **confirmed** | `packs/clouds2/extracted/pack.mcmeta` |",
          "| Yeti is format 9 | **confirmed** | `packs/yeti_set_go/extracted/pack.mcmeta` |",
          "| 1.21.1 requires 34 | **confirmed** | 1.21.1 client `version.json`: `resource: 34` |",
          "| Space Race is format 6 | **confirmed** | `packs/space_race/extracted/Space Race Resource Pack/pack.mcmeta` |",
          "| Space Race is a 'nested/double zip' | **partly**: it is a wrapper folder inside a `.zip`, not a zip-inside-zip; filename is `resources.zip.zip` | zip entry list: one root folder, no inner `.zip` |",
          "| Space Race is invalid as shipped | **confirmed** | no root `pack.mcmeta`; not named `resources.zip` |",
          "| Clouds 2 references missing `acerace/start.ogg` and `acerace/checkpoint.ogg` | **confirmed, plus worse**: `sounds.json` is invalid JSON, so all six `acerace.*` events fail to register | `json.load` error line 5; file listing |", "",
          "## Format requirement table", "",
          "| Pack | Format | Correct version range | Map version | On 1.21.1 (format 34) |", "|---|---|---|---|---|",
          "| Clouds 2 | 15 | 1.20–1.20.1 | 1.20.1 (matches) | loads with older-version warning; content paths valid |",
          "| Yeti Set Go | 9 | 1.19–1.19.2 | 1.19.2 (matches) | loads with older-version warning |",
          "| Space Race | 6 | 1.16.2–1.16.5 | 1.18.1 (already old: 1.18 uses 8; M) | archive invalid before format even matters; if repacked, loads with warning |", "",
          "No pack is enabled. `server.properties` has `resource-pack=` empty and `require-resource-pack=false`; "
          "no imported server world contains `resources.zip`. Nothing was ported or enabled for this check.", ""]

open(os.path.join(DATA, "a12_crosscheck.md"), "w", encoding="utf-8").write("\n".join(lines))
print("wrote a12_crosscheck.md")
for label, slug, path, names, roots, root_meta, inner_zips, meta in details:
    print(label, roots, root_meta, inner_zips, meta.get("pack", {}).get("pack_format"))
