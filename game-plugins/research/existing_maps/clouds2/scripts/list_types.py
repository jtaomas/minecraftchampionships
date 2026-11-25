#!/usr/bin/env python3
import json
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
print("non-repeating and non-auto repeating:")
for c in sorted(cb, key=lambda c: (c["y"], c["z"], c["x"])):
    if c["block"] != "minecraft:repeating_command_block" or not c["auto"]:
        t = c["block"].replace("minecraft:", "")
        print(f"{c['x']:5d} {c['y']:3d} {c['z']:5d} {t:26s} auto={int(c['auto'])}  {c['command'][:90]}")
