"""Read-only audit of image citations in the existing_maps dossiers.

Writes only to _qa/data/image_audit_raw.csv and _qa/data/image_duplicates.csv.
Does not modify any agent folder.
"""
from __future__ import annotations

import csv
import hashlib
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]  # existing_maps
REPO = Path(__file__).resolve().parents[5]   # repo root
OUT = ROOT / "_qa" / "data"

AGENTS = [
    "clouds2",
    "city_marathon",
    "space_race",
    "yeti_set_go",
    "decision_dome_recreation",
    "minigames",
    "minigames/sky_battle",
    "minigames/parkour_tag",
    "minigames/other",
    "tgttos_badlands",
    "tgttos_industry",
    "tgttos_pits",
    "resource_packs",
    "video_clouds2",
    "server_import",
    "comparison",
]

IMG_RE = re.compile(
    r"(?<![\w./-])("
    r"(?:game-plugins/)?(?:\.\./)*(?:[A-Za-z0-9 _.\-]+/)*[A-Za-z0-9 _.\-]+\.(?:png|jpe?g)"
    r")(?![\w./-])",
    re.IGNORECASE,
)


def md_files(folder: Path) -> list[Path]:
    files = []
    for name in ("README.md", "REFERENCE_IMAGES.md", "STATUS.md"):
        p = folder / name
        if p.exists():
            files.append(p)
    data = folder / "data"
    if data.exists():
        files.extend(sorted(data.glob("*.md")))
    return files


def resolve(folder: Path, token: str) -> Path | None:
    token = token.strip().strip("`'\"()[]<>")
    token = token.rstrip(".,;:")
    if not token or "*" in token or "?" in token:
        return None
    token = token.replace("\\", "/")
    if token.startswith("game-plugins/"):
        return REPO / token
    if token.startswith("../"):
        return (folder / token).resolve()
    if token.startswith(("renders/", "frames/", "textures/", "packs/", "data/")):
        return folder / token
    # bare file names in a table, try known dirs
    for prefix in ("renders/", "frames/", "textures/"):
        cand = folder / prefix / token
        if cand.exists():
            return cand
    return None


def main() -> None:
    rows = []
    dimensions = {}
    hashes = {}
    for rel in AGENTS:
        folder = ROOT / rel
        if not folder.exists():
            continue
        cited: dict[Path, dict] = {}
        for md in md_files(folder):
            text = md.read_text(encoding="utf-8", errors="replace")
            for m in IMG_RE.finditer(text):
                token = m.group(1)
                p = resolve(folder, token)
                if p is None:
                    continue
                try:
                    p.relative_to(REPO)
                except ValueError:
                    continue
                if not p.exists() or not p.is_file():
                    cited.setdefault(p, {"token": token, "sources": set(), "exists": False})
                    cited[p]["sources"].add(md.name)
                    continue
                entry = cited.setdefault(p, {"token": token, "sources": set(), "exists": True})
                entry["sources"].add(md.name)
        for p, meta in sorted(cited.items(), key=lambda kv: str(kv[0]).lower()):
            w = h = ""
            mode = ""
            opened = "no"
            if meta["exists"]:
                try:
                    with Image.open(p) as im:
                        w, h = im.size
                        mode = im.mode
                        im.verify()
                    with Image.open(p) as im:
                        im.load()
                    opened = "verified-readable"
                except Exception as exc:  # noqa: BLE001
                    opened = f"OPEN-FAILED: {exc}"
                try:
                    digest = hashlib.sha256(p.read_bytes()).hexdigest()
                    hashes.setdefault(digest, []).append(p)
                except OSError:
                    pass
            try:
                relpath = p.relative_to(REPO).as_posix()
            except ValueError:
                relpath = str(p)
            rows.append(
                {
                    "agent": rel,
                    "cited_path": meta["token"],
                    "resolved_path": relpath,
                    "exists": "yes" if meta["exists"] else "NO",
                    "width": w,
                    "height": h,
                    "mode": mode,
                    "readable": opened,
                    "cited_in": ";".join(sorted(meta["sources"])),
                }
            )

    OUT.mkdir(parents=True, exist_ok=True)
    with (OUT / "image_audit_raw.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    dup_rows = []
    for digest, paths in hashes.items():
        if len(paths) > 1:
            group = sorted({p.relative_to(REPO).as_posix() for p in paths})
            dup_rows.append({"sha256": digest, "count": len(group), "paths": " | ".join(group)})
    with (OUT / "image_duplicates.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["sha256", "count", "paths"])
        writer.writeheader()
        writer.writerows(sorted(dup_rows, key=lambda r: (-r["count"], r["paths"])))

    print(f"cited unique images: {len(rows)}")
    print(f"missing: {sum(1 for r in rows if r['exists'] == 'NO')}")
    print(f"unreadable: {sum(1 for r in rows if not str(r['readable']).startswith('verified'))}")
    print(f"duplicate groups: {len(dup_rows)}")


if __name__ == "__main__":
    main()
