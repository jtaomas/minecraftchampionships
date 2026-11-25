#!/usr/bin/env python3
"""Join command-block block entities to their block variant from the dump."""
import gzip, json
from collections import Counter
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
O = (-258, -64, -316)
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
want = {(c["x"], c["y"], c["z"]) for c in cb}
variants = {}
with gzip.open(D + r"\clouds2_full.mcmdump.gz", "rt", encoding="utf-8") as fh:
    fh.readline()
    for line in fh:
        p = line.split(" ", 3)
        if len(p) < 4: continue
        x, y, z = int(p[0]) + O[0], int(p[1]) + O[1], int(p[2]) + O[2]
        if (x, y, z) in want:
            variants[(x, y, z)] = p[3].strip().split("[")[0]
print("matched", len(variants), "of", len(want))
print(Counter(variants.values()))
for c in cb:
    c["block"] = variants.get((c["x"], c["y"], c["z"]), "?")
json.dump(cb, open(D + r"\command_blocks.json", "w", encoding="utf-8"), indent=1)
print(Counter((c["block"], c["auto"]) for c in cb))
