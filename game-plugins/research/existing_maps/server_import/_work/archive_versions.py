import io
import json
import zipfile
from pathlib import Path

import nbtlib

BASE = Path(r"C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS")
ARCHIVES = {
    "mcc_clouds2": None,
    "mcc_citymarathon": "city-marathon.zip",
    "mcc_spacerace": "Space Race.zip",
    "mcc_yetisetgo": "YetiSetGo.zip",
    "mcc_decisiondome": "Decision Dome.zip",
    "mcc_minigames": "daniellaur-s-minigames-version-3.zip",
    "mcc_tgttos_badlands": "tgttos-badlands-by-mickeykat.zip",
    "mcc_tgttos_industry": "tgttos-industry-by-mickey-kat.zip",
    "mcc_tgttos_pits": "tgttos-pits-by-mickey-kat.zip",
}
# Clouds 2 archive sits in Downloads root, not the grouped folder.
CLOUDS2 = Path(r"C:\Users\LeonS\Downloads\Clouds 2 - Electric Boogaloo (Download Version).zip")

for world, arc in ARCHIVES.items():
    if not arc:
        p = CLOUDS2
        arc = p.name
    else:
        p = BASE / arc
    if not p.exists():
        print(world, "missing", p)
        continue
    try:
        z = zipfile.ZipFile(p)
        levels = [n for n in z.namelist() if n.endswith("level.dat")]
        if not levels:
            print(world, arc, "no level.dat")
            continue
        # shortest path = top-level world
        levels.sort(key=len)
        raw = z.read(levels[0])
        if raw[:2] == b"\x1f\x8b":
            import gzip
            raw = gzip.decompress(raw)
        f = nbtlib.File.parse(io.BytesIO(raw))
        d = f["Data"]
        print(world, arc, "level", levels[0], "version", str(d.get("Version", {}).get("Name", "")),
              "dataver", int(d.get("DataVersion", 0)),
              "spawn", int(d.get("SpawnX", 0)), int(d.get("SpawnY", 0)), int(d.get("SpawnZ", 0)),
              "datapacks", [str(x) for x in d.get("DataPacks", {}).get("Enabled", [])])
    except Exception as exc:
        print(world, arc, "ERR", exc)
