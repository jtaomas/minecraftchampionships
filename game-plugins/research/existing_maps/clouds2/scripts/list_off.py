#!/usr/bin/env python3
import json
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
off = [c for c in cb if not c["auto"]]
print(len(off), "not-always-active")
for c in sorted(off, key=lambda c: (c["z"], c["x"], c["y"])):
    print("%5d %3d %5d powered=%d  %s" % (c["x"], c["y"], c["z"], c["powered"], c["command"][:110]))
