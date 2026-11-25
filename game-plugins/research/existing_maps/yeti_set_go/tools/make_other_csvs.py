#!/usr/bin/env python3
"""Write data/entities.csv, data/checkpoints.csv, data/pack_mapping.csv."""
import csv
import json

scan = json.load(open("data/scan.json", encoding="utf-8"))

# entities
rows = []
for e in scan["entities"]:
    rows.append({
        "id": e.get("id", ""),
        "x": e.get("Pos", [0, 0, 0])[0], "y": e.get("Pos", [0, 0, 0])[1], "z": e.get("Pos", [0, 0, 0])[2],
        "tile": f"{e.get('TileX','')},{e.get('TileY','')},{e.get('TileZ','')}",
        "variant": e.get("Motive", e.get("variant", "not stored")),
        "facing": e.get("Facing", ""),
        "note": "decorative painting inside the station car (H)",
    })
with open("data/entities.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader(); w.writerows(rows)

# checkpoints
cp = [
    ("CP0", "Start / finish gate", 21, -1, 7, "z", 11, "gate plane x16..26, y-1..1", "Opens at GO (fill); finish crossed southbound", "H"),
    ("CP1", "Start spawn", 21, -1, 4, "-", 1, "level.dat SpawnX/Y/Z", "Initial world spawn", "H"),
    ("CP2", "Station checkpoint pad", -167, 36, 31, "-", 1, "yellow pad x -168..-166, z 29..33; setworldspawn command at -155,35,27", "Sets global spawn at station", "H"),
    ("CP3", "Station elytra pad", -163, 36, 31, "-", 1, "lodestone; equip command at -155,35,28", "Elytra on, chest slot", "H (local chunk)"),
    ("CP4", "Lodge roof elytra pad", -17, 35, -36, "-", 1, "lodestone; same command block 152 m away", "Elytra on only if sim distance >=10 chunks", "M"),
    ("CP5", "Start side-lane elytra pads", 29, -1, 0, "-", 1, "lodestone strips x28..31 and x11..14", "Elytra on only if sim distance >=12 chunks", "L/M"),
    ("CP6", "North chain", 15, -20, -132, "-", 1, "no checkpoint block", "No respawn beyond the station/start pads", "H"),
]
pads = list(csv.DictReader(open("data/pad_clusters.csv", encoding="utf-8")))
for i, p in enumerate([p for p in pads if p["type"] == "yellow_glazed_terracotta"], start=7):
    cp.append((f"CP{i:02d}", "Yellow speed pad / spawn pad", p["cx"], p["cy"], p["cz"], "-", p["n"],
               f"x{p['x_min']}..{p['x_max']} z{p['z_min']}..{p['z_max']}", "Sets global spawn when stepped on (if command chunk loaded)", "H/M"))
with open("data/checkpoints.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["order", "name", "x", "y", "z", "gate_axis", "width", "detail", "grants_or_clears", "confidence"])
    w.writerows(cp)

# pack mapping
pack = [
    ("pack.mcmeta", "pack metadata", "pack_format 9 (1.19.2); must be 34 for 1.21.1; description '§c Ace Race Animation Example'", "whole map", "H"),
    ("pack.png", "pack icon", "world pack icon", "UI only", "H"),
    ("assets/minecraft/models/block/yellow_glazed_terracotta.json", "block model override", "cube_all -> custom animated pad texture", "yellow speed pads (377 blocks)", "H"),
    ("assets/minecraft/models/block/pink_glazed_terracotta.json", "block model override", "cube_all -> custom animated pad texture", "pink jump-pad corners (92 blocks)", "H"),
    ("assets/minecraft/models/block/red_glazed_terracotta.json", "block model override", "cube_all -> custom animated pad texture", "red jump-pad edges (92 blocks)", "H"),
    ("assets/minecraft/models/block/magenta_glazed_terracotta.json", "block model override", "cube_all -> custom animated pad texture", "magenta jump-pad centres (23 blocks)", "H"),
    ("assets/minecraft/models/block/purple_glazed_terracotta.json", "block model override", "cube_all -> custom animated pad texture", "UNUSED: no purple glazed in the world", "H"),
    ("assets/minecraft/models/item/rotten_flesh.json", "item model", "item/generated with custom texture", "'Energy' item; not given by any command/container", "H"),
    ("assets/minecraft/textures/block/yellow_glazed_terracotta.png", "block texture (16x256, 16 frames)", "animated yellow chevron speed pad", "yellow pads", "H"),
    ("assets/minecraft/textures/block/pink_glazed_terracotta.png", "block texture (16x112, 7 frames)", "animated pink ripple jump pad", "pink pads", "H"),
    ("assets/minecraft/textures/block/red_glazed_terracotta.png", "block texture (16x112, 7 frames)", "animated red ripple jump pad", "red pads", "H"),
    ("assets/minecraft/textures/block/magenta_glazed_terracotta.png", "block texture (16x112, 7 frames)", "animated magenta ripple jump pad", "magenta pads", "H"),
    ("assets/minecraft/textures/block/purple_glazed_terracotta.png", "block texture (16x112, 7 frames)", "animated purple ripple jump pad", "UNUSED", "H"),
    ("assets/minecraft/textures/block/glowstone.png", "block texture (16x16)", "custom glowstone texture", "UNUSED: no glowstone blocks in the world", "H"),
    ("assets/minecraft/textures/block/AnimPad.psd", "source art (29,647 B)", "Photoshop source for the pad animation", "not loaded by the game", "H"),
    ("assets/minecraft/textures/item/rotten_flesh.png", "item texture (32x16, 2 frames)", "animated red/blue 'Energy' icon", "not used in commands/containers", "H"),
    ("assets/minecraft/lang/en_us.json", "language file (5,155 keys)", "full 1.19.2 en_us snapshot; custom rename item.minecraft.rotten_flesh='Energy'", "tooltip only", "H (rename) / M (rest vanilla)"),
]
with open("data/pack_mapping.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["file", "kind", "what_it_does", "map_use", "confidence"])
    w.writerows(pack)

print(f"entities {len(rows)}, checkpoints {len(cp)}, pack rows {len(pack)}")
