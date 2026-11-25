#!/usr/bin/env python3
import json
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))

clusters = [
    ("tutorial-A", (-160, -55, 170), (-145, -45, 200)),
    ("tutorial-B", (-180, -42, -212), (-168, -33, -185)),
    ("tutorial-C", (-8, -15, -140), (5, -8, -115)),
    ("tutorial-D", (-8, -11, -25), (5, -4, 0)),
    ("tutorial-E", (20, -42, 225), (50, -33, 240)),
]
rest = []
for c in cb:
    inside = False
    for name, (x0, y0, z0), (x1, y1, z1) in clusters:
        if x0 <= c["x"] <= x1 and y0 <= c["y"] <= y1 and z0 <= c["z"] <= z1:
            inside = True
            break
    if not inside:
        rest.append(c)
print("outside tutorial clusters:", len(rest))
for c in sorted(rest, key=lambda r: (r["y"], r["z"], r["x"])):
    print("%5d %4d %5d a=%d  %s" % (c["x"], c["y"], c["z"], c["auto"], c["command"]))
