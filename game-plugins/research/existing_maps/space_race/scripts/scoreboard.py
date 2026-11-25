#!/usr/bin/env python3
"""Inspect scoreboard.dat and the level.dat Player compound."""
import nbtlib

W = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\Space_Race\Space Race"
sb = nbtlib.load(W + r"\data\scoreboard.dat")

def dump(tag, indent=0):
    pad = "  " * indent
    try:
        keys = tag.keys()
    except Exception:
        print(pad + str(tag)); return
    for k in keys:
        v = tag[k]
        try:
            sub = v.keys()
        except Exception:
            sub = None
        if sub is not None:
            print(f"{pad}{k}:")
            dump(v, indent + 1)
        elif isinstance(v, (list, tuple)):
            print(f"{pad}{k}: list[{len(v)}]")
            for item in v[:12]:
                print(f"{pad}  - {str(item)[:200]}")
        else:
            print(f"{pad}{k} = {str(v)[:200]}")

print("=== scoreboard.dat ===")
dump(sb)

print("\n=== level.dat Player ===")
lv = nbtlib.load(W + r"\level.dat")
dump(lv["Data"]["Player"])
