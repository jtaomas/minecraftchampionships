#!/usr/bin/env python3
"""Build the A8 data CSVs from the scan, world NBT and unpacked datapack."""
import csv
import io
import json
import os
import zlib
from collections import Counter

import nbtlib

ROOT = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\tgttos_industry"
DATA = os.path.join(ROOT, "data")
WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat"

# ---------------------------------------------------------------- chickens
def read_chunk(data, index):
    if len(data) < 4096:
        return None
    entry = data[index * 4:index * 4 + 4]
    off = (entry[0] << 16) | (entry[1] << 8) | entry[2]
    if not off:
        return None
    pos = off * 4096
    if pos + 5 > len(data):
        return None
    length = int.from_bytes(data[pos:pos + 4], "big")
    comp = data[pos + 4]
    raw = data[pos + 5:pos + 4 + length]
    if comp == 1:
        raw = zlib.decompress(raw, 16 + 15)
    elif comp == 2:
        raw = zlib.decompress(raw)
    elif comp != 3:
        return None
    return nbtlib.File.parse(io.BytesIO(raw))


chickens = []
for fn in sorted(os.listdir(os.path.join(WORLD, "entities"))):
    if not fn.endswith(".mca"):
        continue
    data = open(os.path.join(WORLD, "entities", fn), "rb").read()
    for i in range(1024):
        ch = read_chunk(data, i)
        if not ch:
            continue
        for e in ch.get("Entities", []):
            pos = [float(v) for v in e.get("Pos", [0, 0, 0])]
            eff = []
            for a in e.get("ActiveEffects", []):
                eff.append(f"id={int(a.get('Id'))} amp={int(a.get('Amplifier'))} dur={int(a.get('Duration'))}")
            chickens.append({
                "id": str(e.get("id", "")),
                "x": round(pos[0], 2), "y": round(pos[1], 2), "z": round(pos[2], 2),
                "persistent": int(e.get("PersistenceRequired", 0)),
                "on_ground": int(e.get("OnGround", 0)),
                "health": float(e.get("Health", 0)),
                "effects": ";".join(eff),
                "chunk": f"{int(ch['Position'][0])},{int(ch['Position'][1])}",
            })
chickens.sort(key=lambda c: (c["z"], c["x"]))
with open(os.path.join(DATA, "entities.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=["id", "x", "y", "z", "persistent", "on_ground", "health", "effects", "chunk"])
    w.writeheader()
    w.writerows(chickens)

# ---------------------------------------------------------------- checks
with open(os.path.join(DATA, "scan_summary.json")) as f:
    summary = json.load(f)

# ---------------------------------------------------------------- blocks
USE = {
    "minecraft:smooth_stone": ("foundation", "21-block-thick table/plinth mass; deck underlay; side walls"),
    "minecraft:smooth_stone_slab": ("foundation", "double slabs in the plinth; top/bottom slabs as trim and roof units"),
    "minecraft:blackstone": ("deck", "conveyor belt rubber strips, table edge, roof sheets"),
    "minecraft:polished_andesite": ("deck", "conveyor side rails and belt support frames"),
    "minecraft:light_blue_stained_glass": ("glass", "39-high fins in gap A, 31x105 skylight at y=9, curtain windows, window bands"),
    "minecraft:red_concrete": ("paint", "red paint curtains at z=66 and z=83 (6 segments); reset by block_reset"),
    "minecraft:yellow_concrete": ("paint", "yellow paint curtain at z=74 (4 segments); reset by block_reset"),
    "minecraft:bricks": ("machinery", "machine tower shafts and head trim"),
    "minecraft:magenta_terracotta": ("machinery", "machine tower banding / accent"),
    "minecraft:black_wool": ("deck", "conveyor seam strips, start-room border, catwalk underside"),
    "minecraft:gray_wool": ("deck", "conveyor belt slats (1-wide gray stripes)"),
    "minecraft:black_concrete": ("machinery", "machine panels, start-room walls, border around yellow windows"),
    "minecraft:light_blue_stained_glass_pane": ("glass", "machine head window, catwalk glazing, small screens"),
    "minecraft:quartz_pillar": ("machinery", "conveyor support legs under the deck (y=-31..-27)"),
    "minecraft:white_concrete": ("machinery", "machine control panels, curtain window borders and paint drips, roof units"),
    "minecraft:iron_bars": ("trim", "catwalk handrails / safety cages"),
    "minecraft:oak_fence": ("machinery", "thin pipe runs above the catwalk"),
    "minecraft:acacia_planks": ("machinery", "pipe/valve detail on the machine heads"),
    "minecraft:polished_diorite": ("trim", "catwalk end caps and fascia"),
    "minecraft:yellow_wool": ("machinery", "hazard stripe blocks on the catwalk underside / machine trim"),
    "minecraft:iron_trapdoor": ("catwalk", "y=-20 grating walkways (702) and the glass-fin top caps"),
    "minecraft:cobblestone_wall": ("catwalk", "y=-22 catwalk edge / pipe rail"),
    "minecraft:barrier": ("functional", "start-box walls (184); removed at GO and rebuilt on reset"),
    "minecraft:red_sandstone": ("trim", "machine head frames, finish-pen coping, catwalk supports"),
    "minecraft:stone_bricks": ("trim", "small machine/tower detail"),
    "minecraft:red_sandstone_wall": ("trim", "railing/piping on catwalks and machine frames"),
    "minecraft:acacia_slab": ("machinery", "pipe brackets and small shelves"),
    "minecraft:quartz_block": ("start", "start-room floor (20x3)"),
    "minecraft:red_sandstone_stairs": ("trim", "catwalk underside trim and curtain top rails"),
    "minecraft:acacia_trapdoor": ("machinery", "vertical pipe clamps (21-high columns at x41/x105)"),
    "minecraft:gray_concrete": ("start", "start-room wall behind the sign"),
    "minecraft:acacia_fence": ("machinery", "pipe runs at y=-18"),
    "minecraft:blackstone_stairs": ("deck", "belt end transitions at y=-35/-38"),
    "minecraft:diorite_wall": ("trim", "roof edge / parapet detail"),
    "minecraft:ladder": ("machinery", "7-high maintenance ladder on the towers at x43/x103"),
    "minecraft:sea_lantern": ("light", "9 hidden light sources (machine buttons / roof units)"),
    "minecraft:iron_block": ("machinery", "machine base plates and valve bodies"),
    "minecraft:oak_wall_sign": ("functional", "the single start sign at 73,-32,10"),
    "minecraft:tripwire_hook": ("machinery", "decorative hooks on the machine heads (8)"),
}

base = Counter()
with open(os.path.join(DATA, "blocks_xyz.csv"), encoding="utf-8") as f:
    for d in csv.DictReader(f):
        base[d["block"].split("[")[0]] += 1
total = sum(base.values())
with open(os.path.join(DATA, "blocks.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["block", "count", "share_pct", "group", "use"])
    for k, v in base.most_common():
        g, u = USE.get(k, ("detail", "detail block"))
        w.writerow([k.replace("minecraft:", ""), v, f"{100*v/total:.3f}", g, u])

# ---------------------------------------------------------------- signs
sign = {
    "x": 73, "y": -32, "z": 10, "block": "oak_wall_sign[facing=south,waterlogged=false]",
    "front_1": '{"text":"","clickEvent":{"action":"run_command","value":"tag @a add starting"}}',
    "front_2": '{"text":"Start","bold":true,"color":"green"}',
    "front_3": '{"text":""}', "front_4": '{"text":""}',
    "back": "none", "color": "black", "glowing": 0,
    "click": "line 1 (empty) -> tag @a add starting; line 2 'Start' has no clickEvent",
}
with open(os.path.join(DATA, "signs.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=list(sign))
    w.writeheader()
    w.writerow(sign)

# ---------------------------------------------------------------- command blocks
with open(os.path.join(DATA, "command_blocks.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["x", "y", "z", "type", "auto", "condition", "exact_command", "trigger", "effect", "1_21_risk"])
    # intentionally no rows: this world has zero command blocks

# ---------------------------------------------------------------- checkpoints
rows = [
    [0, "start / respawn", 73, -33, 11, "box 22x5x5 (x63-84, y-33..-29, z9-13)", "1 spawn point, centre of a 20x3 quartz tray",
     "click sign -> 'starting'; 140 ticks later -> 'playing', barriers removed; fall y<=-58 -> tp back here",
     "clear inventory + timer at finished=140; barriers rebuilt"],
    [1, "finish pen", 73, -39, 93, "trench 31x11 (x58-88, z88-98), floor y=-40", "24 chickens x61-87, z89-97",
     "punch any chicken with Resistance V -> hit_chicken stat -> finished", "spectator + time title; playing removed from all"],
]
with open(os.path.join(DATA, "checkpoints.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["order", "name", "x", "y", "z", "gate_axis_width", "what_it_grants", "what_it_clears", "notes"])
    for r in rows:
        w.writerow([r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], ""])
    w.writerow(["", "MID-COURSE CHECKPOINTS", "", "", "", "", "none", "", "no checkpoint/region system exists in the Industry datapack"])

# ---------------------------------------------------------------- datapack functions
RISK_ITEM = "BREAK 1.20.5+: item NBT {...} removed; needs components [...]"
RISK_SLASH = "REVIEW 1.21.5+: run_command click values should not start with '/'"
RISK_FOLDER = "BLOCKED 1.21: functions/ -> function/, tags/functions/ -> tags/function/"

rows = []
for fn in ["load", "loop", "color", "start_timer", "gamerules", "respawn", "block_reset"]:
    path = os.path.join(DATA, "datapack", "data", "tgttos", "functions", fn + ".mcfunction")
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f, 1):
            s = line.strip()
            if not s:
                continue
            kind = "comment" if s.startswith("#") else "command"
            risk = "OK 1.21.1"
            note = ""
            if kind == "command" and "item replace" in s and "{" in s:
                risk = RISK_ITEM
            elif "clickEvent" in s and '"value":"/' in s:
                risk = RISK_SLASH
            elif "minecraft.custom:minecraft.damage_dealt_resisted" in s:
                note = "stat exists in 1.21.1 (en_us: stat.minecraft.damage_dealt_resisted)"
            elif "data get entity @s Pos[1]" in s:
                note = "double -> score truncates; -57.9 floors to -58, threshold is effectively y<=-57.9"
            elif s.startswith("summon armor_stand"):
                note = "entity NBT unchanged in 1.21.1"
            elif "replace #minecraft:mineable/pickaxe" in s:
                note = "vanilla block tag still #minecraft:mineable/pickaxe"
            elif s.startswith("difficulty") or s.startswith("gamerule"):
                note = "valid 1.21.1"
            rows.append([fn, i, kind, s, risk, RISK_FOLDER, note])
with open(os.path.join(DATA, "datapack_functions.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["file", "line", "kind", "command", "line_1_21_risk", "file_1_21_risk", "notes"])
    w.writerows(rows)

# ---------------------------------------------------------------- timeline
with open(os.path.join(DATA, "timeline.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["tick", "seconds", "event", "source"])
    w.writerow([0, 0.0, "sign click: tag @a add starting; countdown starts at 0", "sign / start_timer"])
    w.writerow([40, 2.0, "title 'Starting in' + subtitle 5", "start_timer"])
    w.writerow([60, 3.0, "subtitle 4", "start_timer"])
    w.writerow([80, 4.0, "subtitle 3", "start_timer"])
    w.writerow([100, 5.0, "subtitle 2", "start_timer"])
    w.writerow([120, 6.0, "subtitle 1", "start_timer"])
    w.writerow([140, 7.0, "GO!!; barriers removed; tag playing; countdown reset", "start_timer"])
    w.writerow(["-", "-", "playing: +5 to playtimeMs1 each tick; +1 spawntime", "loop"])
    w.writerow(["-", "-", "every 20 ticks: summon invisible armor_stand 10 above each player (remove_block marker)", "loop"])
    w.writerow(["-", "-", "fall below y=-58: tp to 73,-33,11 (timer keeps running)", "respawn"])
    w.writerow([0, 0.0, "finish: hit chicken -> hit_chicken>=1 -> finished tag, spectator", "loop"])
    w.writerow([60, 3.0, "clear 10 concrete colours within 15 blocks of every remove_block marker", "loop"])
    w.writerow([65, 3.25, "block_reset: rebuild 7 of 10 paint-curtain segments", "loop -> block_reset"])
    w.writerow([140, 7.0, "tp @a to 73,-33,11; barriers rebuilt; adventure; clear; timer/finished reset", "loop"])

print("wrote data files; chickens", len(chickens), "blocks", total, "function rows", len(rows))
