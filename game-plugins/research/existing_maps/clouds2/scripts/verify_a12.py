#!/usr/bin/env python3
"""Verify A12's failure list against the extracted command table."""
import csv, json, re
from collections import Counter

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))

nbt_item = [c for c in cb if re.search(r"\b(leather_boots|elytra|trident)\{", c["command"])]
print("commands with item{NBT} syntax:", len(nbt_item))
kinds = Counter()
for c in nbt_item:
    m = re.search(r"with (?:minecraft:)?([a-z_]+)\{([^}]*)", c["command"])
    kinds[(m.group(1) if m else "?", c["command"].split("with ")[1][:60])] += 1
for k, v in kinds.most_common():
    print(f"  {v:3d}  {k}")

# exact distinct strings and their block locations
by_cmd = {}
for c in nbt_item:
    by_cmd.setdefault(c["command"], []).append((c["x"], c["y"], c["z"]))
print("\ndistinct commands:", len(by_cmd))
for cmd, locs in sorted(by_cmd.items(), key=lambda kv: -len(kv[1])):
    print(f"\n  x{len(locs)}  {cmd}")
    print("     at", locs)

# malformed commands: /setblock with < 4 tokens
print("\n-- setblock / fill sanity --")
for c in cb:
    t = c["command"].lstrip("/").split()
    if t and t[0] in ("setblock", "fill"):
        need = 5 if t[0] == "setblock" else 8
        print(f"  tokens={len(t):2d} {c['x']},{c['y']},{c['z']}  {c['command']}")

# commands with leading slash
print("\nleading-slash commands:", sum(1 for c in cb if c["command"].startswith("/")))
for c in cb:
    if c["command"].startswith("/"):
        print("   ", c["x"], c["y"], c["z"], c["command"][:90])
