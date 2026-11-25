#!/usr/bin/env python3
"""Classify command blocks, detect duplicate sensor banks, emit CSV."""
import csv
import json
import re
from collections import Counter, defaultdict

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
cb = json.load(open(D + r"\command_blocks.json", encoding="utf-8"))

BANKS = [
    ("BANK_A", (-160, -55, 170), (-145, -45, 200)),
    ("BANK_B", (-180, -42, -212), (-168, -33, -185)),
    ("BANK_C", (-8, -15, -140), (5, -8, -115)),
    ("BANK_D", (-8, -11, -25), (5, -4, 0)),
    ("BANK_E", (20, -42, 225), (50, -33, 240)),
    ("BANK_F", (35, 0, -290), (50, 10, -260)),
]

def bank_of(x, y, z):
    for name, (x0, y0, z0), (x1, y1, z1) in BANKS:
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            return name
    return "START"

rows = []
for c in cb:
    rows.append(dict(c, bank=bank_of(c["x"], c["y"], c["z"])))

print("per bank:", Counter(r["bank"] for r in rows))
print("auto:", Counter(r["auto"] for r in rows))
print("conditional-key present:", sum(1 for r in cb if "conditional" in r))

# duplicate detection: signature = sorted command list per bank
sig = defaultdict(list)
for r in rows:
    if r["bank"].startswith("BANK_"):
        sig[r["bank"]].append(r["command"])
for b, cmds in sig.items():
    print(b, len(cmds), "unique", len(set(cmds)), "hash", hash(tuple(sorted(cmds))) % 10**8)

# how many banks share the exact same multiset
from collections import Counter as C2
groups = defaultdict(list)
for b, cmds in sig.items():
    groups[tuple(sorted(cmds))].append(b)
for k, v in groups.items():
    print("same multiset:", v, len(k))

# command families
def family(cmd):
    c = cmd.lstrip("/")
    if c.startswith("effect give"):
        m = re.search(r"effect give @a minecraft:(\w+)", c)
        return "global effect " + (m.group(1) if m else "?")
    if c.startswith("effect clear"):
        return "pad clear effect"
    if "if block" in c:
        m = re.search(r"if block ~ ~(-?\d*) ~ (?:minecraft:)?([a-z_]+)", c)
        act = c.split(" run ", 1)[1] if " run " in c else "?"
        return f"pad[{m.group(2) if m else '?'}] -> {act.split()[0]}"
    if "if entity" in c:
        return "kill plane"
    if c.startswith("title") or c.startswith("title "):
        return "title"
    if c.startswith("playsound"):
        return "playsound"
    if c.startswith("enchant"):
        return "enchant"
    if c.startswith("xp "):
        return "xp reset"
    if c.startswith("item replace"):
        return "give item"
    if c.startswith("tp "):
        return "teleport"
    if c.startswith("fill") or c.startswith("setblock"):
        return "world edit"
    if c.startswith("clear"):
        return "clear"
    if c.startswith("kill"):
        return "kill entities"
    if c.startswith("setworldspawn"):
        return "set spawn"
    if c.startswith("time set"):
        return "time"
    if c.startswith("stopsound"):
        return "stopsound"
    if c.startswith("gamemode"):
        return "gamemode"
    return "other"

print()
fam = Counter(family(r["command"]) for r in rows)
for k, v in fam.most_common():
    print(f"{v:4d}  {k}")
