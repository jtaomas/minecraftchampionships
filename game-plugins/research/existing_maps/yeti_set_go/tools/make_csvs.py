#!/usr/bin/env python3
"""Build command_blocks.csv, signs.csv, containers.csv from scan.json + dump states."""
import csv
import gzip
import json

ORIGIN = (-182, -64, -140)
states = {}
for line in gzip.open("data/yeti_full.mcmdump.gz", "rt"):
    if line.startswith("MCMDUMP"):
        continue
    x, y, z, n = line.rstrip().split(" ", 3)
    states[(int(x) + ORIGIN[0], int(y) + ORIGIN[1], int(z) + ORIGIN[2])] = n

scan = json.load(open("data/scan.json", encoding="utf-8"))
bes = scan["block_entities"]

cmd_rows = []
for b in bes:
    if b.get("id") != "minecraft:command_block":
        continue
    x, y, z = int(b["x"]), int(b["y"]), int(b["z"])
    st = states.get((x, y, z), "")
    base = st.split("[")[0].replace("minecraft:", "")
    facing = st.split("facing=")[1].split("]")[0].split(",")[0] if "facing=" in st else ""
    cond = st.split("conditional=")[1].split(",")[0] if "conditional=" in st else "false"
    cmd_rows.append({
        "x": x, "y": y, "z": z,
        "block": base,
        "facing": facing,
        "conditional": cond,
        "auto": int(b.get("auto", 0)),
        "powered": int(b.get("powered", 0)),
        "last_execution": int(b.get("LastExecution", -1)),
        "last_output": (b.get("LastOutput") or "").replace("\n", " ")[:160],
        "command": b.get("Command", ""),
        "custom_name": str(b.get("CustomName", "")) if b.get("CustomName") else "",
    })
cmd_rows.sort(key=lambda r: (r["z"], r["x"], r["y"]))
with open("data/command_blocks.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(cmd_rows[0].keys()))
    w.writeheader()
    w.writerows(cmd_rows)

sign_rows = []
for b in bes:
    if b.get("id") != "minecraft:sign":
        continue
    x, y, z = int(b["x"]), int(b["y"]), int(b["z"])
    st = states.get((x, y, z), "minecraft:sign")
    sign_rows.append({
        "x": x, "y": y, "z": z, "block": st,
        "colour": b.get("Color", ""), "glowing": int(b.get("GlowingText", 0)),
        "text1": str(b.get("Text1", "")),
        "text2": str(b.get("Text2", "")),
        "text3": str(b.get("Text3", "")),
        "text4": str(b.get("Text4", "")),
        "click_event": "",
    })
sign_rows.sort(key=lambda r: (r["z"], r["x"], r["y"]))
with open("data/signs.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(sign_rows[0].keys()))
    w.writeheader()
    w.writerows(sign_rows)

cont_rows = []
for b in bes:
    if b.get("id") not in ("minecraft:shulker_box", "minecraft:hopper", "minecraft:chest",
                           "minecraft:barrel", "minecraft:campfire", "minecraft:soul_campfire"):
        continue
    x, y, z = int(b["x"]), int(b["y"]), int(b["z"])
    items = b.get("Items") or []
    kind = "cooking" if "campfire" in b.get("id", "") else "storage"
    cont_rows.append({
        "x": x, "y": y, "z": z, "block": states.get((x, y, z), ""),
        "kind": kind,
        "items": json.dumps(items, ensure_ascii=False),
        "slots_used": len(items),
    })
with open("data/containers.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(cont_rows[0].keys()))
    w.writeheader()
    w.writerows(cont_rows)

print(f"command blocks: {len(cmd_rows)}")
print(f"signs: {len(sign_rows)}")
print(f"containers: {len(cont_rows)}")
