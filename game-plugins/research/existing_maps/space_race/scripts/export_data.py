#!/usr/bin/env python3
"""Export command_blocks.csv, signs.csv, blocks.csv, containers.csv from the raw scan."""
import gzip
import json
import os
import re
from collections import Counter

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(BASE, "data_raw")
DATA = os.path.join(BASE, "data")
os.makedirs(DATA, exist_ok=True)

T = json.load(open(os.path.join(RAW, "tile_entities.json"), encoding="utf-8"))
TE = {(t["x"], t["y"], t["z"]): t for t in T}

# block states for command blocks and signs from the full dump
block_at = {}
containers = Counter()
container_rows = []
with gzip.open(os.path.join(RAW, "blocks_full.csv.gz"), "rt", encoding="utf-8") as fh:
    next(fh)
    for line in fh:
        x, y, z, b = line.rstrip("\n").split(",", 3)
        base = b.split("[")[0]
        if base == "minecraft:command_block" or base in (
                "minecraft:repeating_command_block", "minecraft:chain_command_block") or base.endswith("sign"):
            block_at[(int(x), int(y), int(z))] = b
        if base in ("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:hopper",
                    "minecraft:dropper", "minecraft:dispenser", "minecraft:shulker_box") or base.endswith("shulker_box"):
            containers[base] += 1
            container_rows.append((int(x), int(y), int(z), b))

# --- command blocks ---
ANNOT = {
    (12, 112, 12): ("player stands on red sandstone (0.35 below feet)", "clears armor.chest (removes elytra/chestplate)", "each player independently (@a)", "automatic next tick", "none"),
    (14, 112, 12): ("diamond block at foot level 1 block north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: horizontal offset, fires only if a diamond block is at foot height north; unreliable in flight"),
    (15, 112, 12): ("diamond block at foot level 2 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 2 blocks"),
    (16, 112, 12): ("diamond block at foot level 3 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 3 blocks"),
    (17, 112, 12): ("diamond block at foot level 4 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 4 blocks"),
    (18, 112, 12): ("diamond block at foot level 5 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 5 blocks"),
    (19, 112, 12): ("diamond block at foot level 6 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 6 blocks"),
    (20, 112, 12): ("diamond block at foot level 7 blocks north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: as above, 7 blocks"),
    (22, 112, 12): ("player stands on red concrete (0.35 below feet)", "clears armor.chest", "each player independently (@a)", "automatic next tick", "none"),
    (12, 112, 14): ("player stands on water (0.35 below feet)", "clears armor.chest", "each player independently (@a)", "automatic next tick", "none"),
    (14, 112, 14): ("diamond block at foot level 1 block north", "sets armor.chest = elytra", "nearest player (@p)", "none", "semantic: duplicate of 14,112,12"),
    (16, 112, 14): ("player stands on magenta glazed terracotta", "speed II for 4 s", "nearest player (@p)", "effect expires", "none"),
    (20, 112, 14): ("unconditional, every tick", "dolphins grace 999999 s, amplifier 1, to all players", "all players (@a)", "refreshed every tick", "none"),
    (22, 112, 14): ("player stands on gray concrete", "clears armor.chest", "each player independently (@a)", "automatic next tick", "none"),
    (11, 112, 16): ("any player stands on any magenta glazed terracotta", "setworldspawn to that player's position (723 magenta blocks map-wide)", "GLOBAL world spawn", "none; last pad stepped on wins", "none syntactically; global side effect (respawn point moves mid-lap)"),
    (13, 112, 16): ("unconditional, every tick", "actionbar 'You are in 1st place' to all players", "all players (@a)", "repeated every tick", "none"),
    (15, 112, 16): ("player stands on red concrete", "levitation 23 for 2 s (~48 blocks)", "nearest player (@p)", "effect expires (2 s)", "none"),
    (16, 112, 16): ("player stands on red concrete", "speed 30 for 2 s", "nearest player (@p)", "effect expires (2 s)", "none"),
    (18, 112, 16): ("player stands on orange concrete", "levitation 70 for 1 s (~71 blocks)", "nearest player (@p)", "effect expires (1 s)", "none"),
    (19, 112, 16): ("player stands on orange concrete", "speed 255 for 1 s", "nearest player (@p)", "effect expires (1 s)", "none"),
    (21, 112, 16): ("ANY player stands on bedrock (1,852 standable columns, y 154-185)", "kill @a - kills EVERY player in the dimension", "all players in the dimension (whole lobby); one player's trigger wipes it", "immediate respawn (doImmediateRespawn=true), keepInventory=true", "none syntactically; global kill vector / grief surface"),
    (23, 112, 16): ("ANY player stands on lime stained glass (6,090 standable columns, mostly the cavern floor y=107)", "kill @a - kills EVERY player in the dimension", "all players in the dimension (whole lobby); one player's trigger wipes it", "immediate respawn (doImmediateRespawn=true), keepInventory=true", "none syntactically; global kill vector / grief surface"),
    (10, 111, 19): ("redstone pulse from the 6-repeater clock (button 6,116,28)", "clears the 23x3x1 glass gate at z=24 (x 6-28, y 115-117)", "world blocks", "never refilled", "none"),
    (10, 111, 22): ("redstone pulse (clock), 16 ticks after the >2< pulse", "title 'Starting in'", "all players (@a)", "one-shot", "none"),
    (11, 111, 22): ("chain after impulse 10,111,22", "subtitle '>1<' green", "all players (@a)", "one-shot", "none"),
    (10, 111, 25): ("redstone pulse (clock)", "title 'Starting in'", "all players (@a)", "one-shot", "none"),
    (11, 111, 25): ("chain after impulse 10,111,25", "subtitle '>2<' yellow", "all players (@a)", "one-shot", "none"),
    (10, 111, 28): ("redstone pulse (clock)", "title 'Starting in'", "all players (@a)", "one-shot", "none"),
    (11, 111, 28): ("chain after impulse 10,111,28", "subtitle '>3<' red", "all players (@a)", "one-shot", "none"),
}
rows = []
for t in sorted([t for t in T if t["id"] == "minecraft:command_block"], key=lambda a: (a["z"], a["x"], a["y"])):
    state = block_at.get((t["x"], t["y"], t["z"]), "?")
    kind = "repeating" if "repeating" in state else "chain" if "chain" in state else "impulse"
    facing = re.search(r"facing=(\w+)", state)
    cond = re.search(r"conditional=(\w+)", state)
    cmd = str(t.get("Command", ""))
    trig, eff, scope, reset, risk = ANNOT.get((t["x"], t["y"], t["z"]), ("", "", "", "", ""))
    rows.append({
        "x": t["x"], "y": t["y"], "z": t["z"], "type": kind,
        "facing": facing.group(1) if facing else "?",
        "conditional": cond.group(1) if cond else "?",
        "auto": t.get("auto"),
        "last_execution": t.get("LastExecution"),
        "success_count": t.get("SuccessCount"),
        "command": cmd,
        "trigger": trig,
        "effect": eff,
        "target_scope": scope,
        "reset": reset,
        "syntax_1_21": risk,
    })

import csv
with open(os.path.join(DATA, "command_blocks.csv"), "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["x", "y", "z", "type", "facing", "conditional", "auto", "last_execution",
                "success_count", "command", "trigger", "effect", "target_scope", "reset",
                "syntax_1_21_risk"])
    for r in rows:
        w.writerow([r["x"], r["y"], r["z"], r["type"], r["facing"], r["conditional"],
                    r["auto"], r["last_execution"], r["success_count"], r["command"],
                    r["trigger"], r["effect"], r["target_scope"], r["reset"], r["syntax_1_21"]])

# --- signs ---
srows = []
for t in sorted([t for t in T if t["id"] == "minecraft:sign"], key=lambda a: (a["z"], a["x"], a["y"])):
    state = block_at.get((t["x"], t["y"], t["z"]), "?")
    texts = []
    for n in range(1, 5):
        raw = str(t.get(f"Text{n}", '{"text":""}'))
        m = re.search(r'"text"\s*:\s*"([^"]*)"', raw)
        texts.append(m.group(1) if m else "")
    srows.append({
        "x": t["x"], "y": t["y"], "z": t["z"],
        "state": state,
        "color": t.get("Color"),
        "glowing": t.get("GlowingText"),
        "line1": texts[0], "line2": texts[1], "line3": texts[2], "line4": texts[3],
        "click_event": "none",
    })
with open(os.path.join(DATA, "signs.csv"), "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["x", "y", "z", "block_state", "color", "glowing", "line1", "line2", "line3", "line4",
                "click_event"])
    for r in srows:
        w.writerow([r["x"], r["y"], r["z"], r["state"], r["color"], r["glowing"],
                    r["line1"], r["line2"], r["line3"], r["line4"], r["click_event"]])

# --- containers ---
with open(os.path.join(DATA, "containers.csv"), "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["x", "y", "z", "block", "items"])
    for r in sorted(container_rows):
        w.writerow([r[0], r[1], r[2], r[3], ""])

# --- blocks ---
counts = json.load(open(os.path.join(RAW, "blocks.json"), encoding="utf-8"))
total = sum(counts.values())
with open(os.path.join(DATA, "blocks.csv"), "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["block", "count", "share_pct"])
    for b, c in counts.items():
        w.writerow([b, c, f"{100*c/total:.4f}"])

print("command blocks", len(rows), "signs", len(srows), "containers", len(container_rows),
      "block types", len(counts), "blocks", total)
print("containers:", containers)
