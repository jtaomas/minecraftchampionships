#!/usr/bin/env python3
"""Print command blocks and signs with exact coords and full command text."""
import json
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
T = json.load(open(os.path.join(BASE, "data_raw", "tile_entities.json"), encoding="utf-8"))

cbs = [t for t in T if t["id"] == "minecraft:command_block"]
signs = [t for t in T if t["id"] == "minecraft:sign"]

print("== COMMAND BLOCKS", len(cbs), "==")
for i, t in enumerate(sorted(cbs, key=lambda a: (a["z"], a["x"], a["y"])), 1):
    print(f"[{i}] x={t['x']} y={t['y']} z={t['z']} auto={t.get('auto')} cond={t.get('conditionMet')} "
          f"cmd={str(t.get('Command'))!r}")

print("\n== SIGNS", len(signs), "==")
for i, t in enumerate(sorted(signs, key=lambda a: (a["z"], a["x"], a["y"])), 1):
    texts = [str(t.get(f"Text{n}", "")) for n in range(1, 5)]
    print(f"[{i}] x={t['x']} y={t['y']} z={t['z']} color={t.get('Color')} glow={t.get('GlowingText')} texts={texts}")
