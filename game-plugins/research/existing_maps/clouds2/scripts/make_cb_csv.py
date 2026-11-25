#!/usr/bin/env python3
"""Generate data/command_blocks.csv with trigger/effect/reset/multiplayer/1.21 columns."""
import csv, json, re

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
BANK_MEANING = {
    "A": "redundant sensor bank 1 (bowl/crater, y=-51)",
    "B": "redundant sensor bank 2 (ruin/crater, y=-38)",
    "C": "redundant sensor bank 3 (right chain, y=-12)",
    "D": "redundant sensor bank 4 (start area, y=-8; also forces adventure + time)",
    "E": "redundant sensor bank 5 (castle, y=-38; andesite elytra variant)",
    "F": "redundant sensor bank 6 (planet rim, y=6)",
}

def bank_of(x, y, z):
    for k, (x0, y0, z0, x1, y1, z1) in BANKS.items():
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            return k
    return "START"

def trigger(c):
    if c["block"] == "minecraft:command_block":
        if c["z"] == 25 and c["x"] <= -5:
            return "Reset button at (-3,4,25) -> impulse at (-5,4,25) -> chain"
        if c["z"] == 22 and c["x"] == 3:
            return "Start button at (5,4,22) -> repeater delay line -> impulse"
        return "Redstone pulse from the start/reset circuit"
    if c["block"] == "minecraft:chain_command_block":
        return "Chained from the previous block in its row"
    if not c["auto"]:
        return "Redstone pulse only (auto=0)"
    return "Every tick while its chunk is loaded (always active)"

def effect(cmd):
    c = cmd.lstrip("/")
    if c.startswith("execute as @a at @s if block"):
        m = re.search(r"if block ~ ~(-?\d*) ~ (?:minecraft:)?([a-z_]+) run (.+)", c)
        if m:
            return f"if the block {m.group(1) or '~0'} below the player is {m.group(2)}: {act(m.group(3))}"
    if c.startswith("execute as @a at @a if entity"):
        return "kill the nearest player when any player's box intersects y=-60 (void plane)"
    if c.startswith("effect give"):
        m = re.search(r"effect give @a minecraft:(\w+) (\S+)(?: (\d+))?", c)
        return f"permanent {m.group(1)} {m.group(3) or ''} on every player" if m else c
    if c.startswith("effect clear"):
        m = re.search(r"effect clear @a minecraft:(\w+)", c)
        return f"clear {m.group(1)} from every player"
    if c.startswith("title @a actionbar"):
        return "set/clear the actionbar text"
    if c.startswith("title @a title") or c.startswith("title @a subtitle"):
        return "show a title/subtitle"
    if c.startswith("playsound"):
        return "play a sound to every player"
    if c.startswith("enchant"):
        return "enchant the held item of every player"
    if c.startswith("xp set"):
        return "reset XP " + ("levels" if "levels" in c else "points")
    if c.startswith("item replace"):
        m = re.search(r"replace entity @a (\S+) with (\S+)", c)
        return f"put {m.group(2)} into {m.group(1)} of every player" if m else c
    if c.startswith("clear"):
        return "clear inventory of every player" + (" (trident only)" if "trident" in c else "")
    if c.startswith("kill @e"):
        return "remove all dropped items"
    if c.startswith("setworldspawn"):
        return "set the world spawn to the executing player (checkpoint)"
    if c.startswith("tp "):
        return "teleport every player to the start box, facing north"
    if c.startswith("gamemode"):
        return "force every player into adventure mode"
    if c.startswith("time set"):
        return "set time to 200 (morning)"
    if c.startswith("stopsound"):
        return "stop all sounds for every player"
    if c.startswith("fill") or c.startswith("setblock"):
        return "world edit: " + c
    return c

def act(a):
    a = a.strip()
    if a.startswith("clear"):
        return "clear inventory" + (" (trident)" if "trident" in a else "")
    if a.startswith("effect give"):
        m = re.search(r"minecraft:(\w+) (\d+) (\d+)", a)
        return f"give {m.group(1)} for {m.group(2)}s amp {m.group(3)}" if m else a
    if a.startswith("effect clear"):
        return "clear " + a.split("minecraft:")[-1]
    if a.startswith("item replace"):
        m = re.search(r"(\S+) with (\S+)", a)
        return f"set {m.group(1)} = {m.group(2)}" if m else a
    if a.startswith("playsound"):
        m = re.search(r"playsound (\S+)", a)
        return f"play {m.group(1)}"
    if a.startswith("setworldspawn"):
        return "set world spawn"
    return a

def reset(cmd):
    c = cmd.lstrip("/")
    if "effect give @a minecraft:jump_boost 1 " in c or "effect give @a minecraft:speed 1 15" in c:
        return "auto-expires (1-3 s)"
    if "effect give @a minecraft:slowness 1 0" in c:
        return "auto-expires (1 s)"
    if "effect give @a minecraft:speed 3 2" in c:
        return "auto-expires (3 s)"
    if "effect give @a minecraft:jump_boost 1 14" in c:
        return "auto-expires (1 s)"
    if "infinite" in c:
        return "permanent for the session; not cleared anywhere"
    if "leather_boots{" in c:
        return "removed when the block below the feet is air"
    if "armor.chest with minecraft:elytra" in c:
        return "removed when the player's body block is cave_air"
    if "armor.chest with air" in c:
        return "elytra clear (this is the reset)"
    if "item replace" in c or c.startswith("clear") or c.startswith("xp set"):
        return "state change; undone only by the reset button / another pad"
    if c.startswith("fill") or c.startswith("setblock"):
        return "world edit; fire-and-forget"
    return "none"

def mp(c):
    cmd = c["command"]
    if bank_of(c["x"], c["y"], c["z"]) != "START":
        base = "global: one player's trigger applies to @a"
    else:
        base = "global: affects every player"
    if "if block" in cmd and "@a" in cmd.split(" run ")[-1]:
        return base + "; condition is per-player, effect is not"
    if "at @a" in cmd:
        return "N^2 execution in multiplayer; one falling player can kill everyone"
    if "setworldspawn" in cmd:
        return "shared world spawn: one player's checkpoint moves everyone's respawn"
    if "gamemode" in cmd:
        return "forces adventure for all players while bank D is loaded"
    return base

def risk(c):
    cmd = c["command"]
    if re.search(r"(leather_boots|elytra|trident)\{", cmd):
        return "FAIL 1.20.5+: item NBT removed; must use item components"
    if cmd.strip() == "setblock 5 4 22":
        return "FAIL even in 1.20.1: missing block argument"
    if cmd.lstrip("/").startswith("title") and "{" in cmd:
        return "OK 1.21.1: JSON text still accepted (SNBT preferred later)"
    if "cherrywood" in cmd:
        return "sound path renamed in 1.20.5+"
    return "OK 1.21.1 syntax"

out = []
for c in sorted(cb, key=lambda r: (bank_of(r["x"], r["y"], r["z"]), r["z"], r["x"], r["y"])):
    b = bank_of(c["x"], c["y"], c["z"])
    out.append({
        "x": c["x"], "y": c["y"], "z": c["z"],
        "block_type": c["block"].replace("minecraft:", ""),
        "auto": int(c["auto"]),
        "conditional": 0,
        "powered": int(c["powered"]),
        "bank": b,
        "bank_note": BANK_MEANING.get(b, "start / reset circuit"),
        "command": c["command"],
        "trigger": trigger(c),
        "effect": effect(c["command"]),
        "reset": reset(c["command"]),
        "multiplayer": mp(c),
        "syntax_1_21": risk(c),
    })

with open(D + r"\command_blocks.csv", "w", newline="", encoding="utf-8-sig") as fh:
    w = csv.DictWriter(fh, fieldnames=list(out[0].keys()))
    w.writeheader()
    w.writerows(out)
print("wrote", len(out), "rows")
print("fails:", sum(1 for r in out if r["syntax_1_21"].startswith("FAIL")))
