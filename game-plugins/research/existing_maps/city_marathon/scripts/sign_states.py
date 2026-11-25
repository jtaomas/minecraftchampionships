#!/usr/bin/env python3
"""Print block states at the seven sign positions."""
import json
import subprocess, sys
base=r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\data"
be=json.load(open(base+r"\block_entities.json",encoding="utf-8"))
signs=[(b["x"],b["y"],b["z"]) for b in be if b["id"]=="minecraft:sign"]
print(signs)
# use box_dump per sign position
for x,y,z in signs:
    print("=== sign",x,y,z)
    subprocess.run([sys.executable, r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon\scripts\box_dump.py", str(x-1),str(x+1),str(y-1),str(y+1),str(z-1),str(z+1)])
