#!/usr/bin/env python3
import json, re
from collections import Counter
D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))
BANKS = {
    "A": (-160, -55, 170, -145, -45, 200),
    "B": (-180, -42, -212, -168, -33, -185),
    "C": (-8, -15, -140, 5, -8, -115),
    "D": (-8, -11, -25, 5, -4, 0),
    "E": (20, -42, 225, 50, -33, 240),
    "F": (35, 0, -290, 50, 10, -260),
}
sets = {}
for k, (x0, y0, z0, x1, y1, z1) in BANKS.items():
    sets[k] = Counter(c["command"].lstrip("/") for c in cb
                      if x0 <= c["x"] <= x1 and y0 <= c["y"] <= y1 and z0 <= c["z"] <= z1)
base = sets["A"]
for k in "BCDEF":
    print(f"\n=== A vs {k} ===")
    print("only A:")
    for cmd, n in (base - sets[k]).items(): print("   ", n, cmd)
    print(f"only {k}:")
    for cmd, n in (sets[k] - base).items(): print("   ", n, cmd)
