#!/usr/bin/env python3
"""Summarise the 51 tile entities: types, coords, and key NBT."""
import json
import os
from collections import Counter

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
T = json.load(open(os.path.join(BASE, "data_raw", "tile_entities.json"), encoding="utf-8"))

print("count", len(T))
print(Counter(t["id"] for t in T))
print()
for t in T:
    pos = t.get("Pos") or t.get("pos") or t.get("x")
    print("---", t["id"], pos, "chunk", t["_chunk"])
    for k, v in t.items():
        if k in ("id", "Pos", "keepPacked", "x", "y", "z", "_chunk"):
            continue
        s = str(v)
        if len(s) > 300:
            s = s[:300] + "..."
        print("   ", k, "=", s)
