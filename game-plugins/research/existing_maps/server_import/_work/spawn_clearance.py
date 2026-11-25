import json
from pathlib import Path
import block_lookup as bl
import scan_worlds as sw

for w in sw.WORLDS:
    d = nbt = sw.nbtlib.load(str(sw.SERVER / w / "level.dat"))["Data"]
    sx, sy, sz = int(d["SpawnX"]), int(d["SpawnY"]), int(d["SpawnZ"])
    bad = []
    for x in range(sx - 1, sx + 2):
        for y in range(sy, sy + 2):
            for z in range(sz - 1, sz + 2):
                name, props = bl.get_block(w, "overworld", x, y, z)
                if name != "minecraft:air":
                    bad.append((x, y, z, name, props))
    ground = bl.get_block(w, "overworld", sx, sy - 1, sz)
    print(w, (sx, sy, sz), "ground", ground[0], "blocking", bad)
