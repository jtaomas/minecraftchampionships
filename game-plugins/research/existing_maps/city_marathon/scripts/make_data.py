#!/usr/bin/env python3
"""Build City Marathon CSV evidence files from NBT + block dump."""
import gzip, json, csv, os, re
from collections import Counter, defaultdict

BASE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
DATA = os.path.join(BASE, "data")
ORIGIN = (-536, -64, -74)

# ---------- block dump lookup ----------
dump = {}
with gzip.open(os.path.join(BASE, "renders", "city_marathon.mcmdump.gz"), "rt", encoding="utf-8") as f:
    hdr = f.readline().split()
    for line in f:
        p = line.rstrip().split(" ", 3)
        if len(p) < 4:
            continue
        x = int(p[0]) + ORIGIN[0]; y = int(p[1]) + ORIGIN[1]; z = int(p[2]) + ORIGIN[2]
        dump[(x, y, z)] = p[3]

be = json.load(open(os.path.join(DATA, "block_entities.json"), encoding="utf-8"))
sel = json.load(open(os.path.join(DATA, "selected_blocks.json"), encoding="utf-8"))
state_by_pos = {(x, y, z): st for x, y, z, st, _ in sel}

# ---------- helpers ----------
def q(s):
    return s.replace("\n", " ").strip()

def write_csv(name, fields, rows):
    with open(os.path.join(DATA, name), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print("wrote", name, len(rows))

# ---------- command blocks ----------
trigger_map = {}
effect_map = {}
reset_map = {}
risk_map = {}
for cmd in set(x["Command"] for x in be if x["id"] == "minecraft:command_block"):
    c = cmd.lower()
    if "polished_diorite" in c:
        effect_map[cmd] = "Replaces each player's chestplate with an elytra (auto-equip)."
        reset_map[cmd] = "Persists until the gray-wool clear command removes it; re-applied every tick while standing on the pad."
        risk_map[cmd] = "Low - execute/if block/item replace syntax valid in 1.21."
    elif "gray_wool" in c:
        effect_map[cmd] = "Replaces each player's chestplate with air (clears elytra)."
        reset_map[cmd] = "Self-clearing; re-applied every tick while on gray wool."
        risk_map[cmd] = "Low - valid 1.21 syntax; item id air valid."
    elif "jump_boost" in c:
        m = re.search(r"jump_boost (\d+) (\d+)", c)
        effect_map[cmd] = f"Jump Boost for {m.group(1)}s at amplifier {m.group(2)} (level {int(m.group(2))+1})."
        reset_map[cmd] = "Expires after the short duration; refreshed while standing."
        risk_map[cmd] = "Low - effect give syntax valid in 1.21."
    elif "speed" in c:
        m = re.search(r"speed (\d+) (\d+)", c)
        effect_map[cmd] = f"Speed for {m.group(1)}s at amplifier {m.group(2)} (level {int(m.group(2))+1})."
        reset_map[cmd] = "Expires after the short duration; refreshed while standing."
        risk_map[cmd] = "Low - effect give syntax valid in 1.21."
    elif "effect clear" in c:
        effect_map[cmd] = "Clears all status effects."
        reset_map[cmd] = "Immediate; no state."
        risk_map[cmd] = "Low - effect clear valid in 1.21."
    elif "setworldspawn" in c:
        effect_map[cmd] = "Sets the world spawn to the executing player's position (soft checkpoint)."
        reset_map[cmd] = "Persistent; overwritten by the next yellow-glazed pad."
        risk_map[cmd] = "Low - setworldspawn valid in 1.21."
    elif "armor.chest" in c:
        effect_map[cmd] = "Armour chest slot edit."
        reset_map[cmd] = "n/a"
        risk_map[cmd] = "Low"
    elif "actionbar" in c:
        effect_map[cmd] = "Shows the hard-coded actionbar 'You are in 1st place' to all players."
        reset_map[cmd] = "Re-applied every tick; never cleared by the map."
        risk_map[cmd] = "Low - title actionbar string form valid in 1.21."
    elif "enchant" in c:
        effect_map[cmd] = "Enchants each player's held item (unbreaking III or riptide I)."
        reset_map[cmd] = "Persistent on the item; re-applied while the command block is loaded."
        risk_map[cmd] = "Low-Medium - enchant target must hold an enchantable item; 1.21 registry ids unchanged."
    elif "gamemode" in c:
        effect_map[cmd] = "Sets gamemode to adventure; no target selector, so a command block source has no player."
        reset_map[cmd] = "Persistent if it runs; likely fails (see risk)."
        risk_map[cmd] = "High - /gamemode <mode> without targets fails from a command block in 1.19.2 and 1.21."
    elif "fill" in c:
        effect_map[cmd] = "Clears the invisible barrier start gate at x3..13, y0..2, z10."
        reset_map[cmd] = "One-shot; no automatic reset. Saved world still has the barrier gate (H)."
        risk_map[cmd] = "Low-Medium - leading slash was accepted in 1.19.2 (SuccessCount=1); keep or normalize."
    elif "title" in c and "subtitle" in c:
        effect_map[cmd] = "Sets the subtitle for the start countdown."
        reset_map[cmd] = "One-shot title; replaced by the next countdown line."
        risk_map[cmd] = "Low - JSON text component valid in 1.21."
    elif "title" in c:
        effect_map[cmd] = "Sets the start countdown title 'Starting In'."
        reset_map[cmd] = "One-shot title."
        risk_map[cmd] = "Low - JSON text component valid in 1.21."
    elif "trident" in c:
        effect_map[cmd] = "Puts a trident in hotbar slot 0 for every player."
        reset_map[cmd] = "One-shot; overwrites slot 0."
        risk_map[cmd] = "Low - item replace syntax valid in 1.21."
    else:
        effect_map[cmd] = "Unclassified."
        reset_map[cmd] = "n/a"
        risk_map[cmd] = "Unknown"

rows = []
for i, b in enumerate(sorted([x for x in be if x["id"] == "minecraft:command_block"], key=lambda a: (a["z"], a["x"], a["y"])), 1):
    x, y, z = b["x"], b["y"], b["z"]
    st = state_by_pos.get((x, y, z), "minecraft:command_block")
    m = re.search(r"\[(.*)\]", st)
    props = dict(p.split("=") for p in m.group(1).split(",")) if m else {}
    block = st.split("[")[0].replace("minecraft:", "")
    mode = {"command_block": "impulse", "chain_command_block": "chain", "repeating_command_block": "repeat"}.get(block, block)
    if mode == "repeat":
        trig = "Auto: repeats every tick while its chunk is loaded (auto=1, no redstone needed)."
    elif mode == "impulse" and b.get("auto") == 0:
        trig = "Redstone pulse; start circuit under East Road (button at 8,0,8). LastExecution proves the countdown/fill fired once."
    elif mode == "chain":
        trig = "Chain after the previous command block; no LastExecution, so never fired in the saved world."
    else:
        trig = "Auto impulse; see notes."
    note = ""
    if "yellow_carpet" in b["Command"]:
        note = "Dead predicate: no yellow_carpet blocks exist in the world (H)."
    if b["Command"].startswith("/"):
        note = (note + " " if note else "") + "Leading slash present; it executed successfully in 1.19.2 (SuccessCount=1)."
    if b["Command"].strip().startswith("gamemode"):
        note = (note + " " if note else "") + "No @a selector; command-block source has no player."
    rows.append({
        "id": f"CB{i:02d}", "x": x, "y": y, "z": z, "block": block, "mode": mode,
        "auto": b.get("auto"), "conditional": props.get("conditional", "false"),
        "facing": props.get("facing", ""), "custom_name": b.get("CustomName", ""),
        "command": b["Command"], "trigger": q(trig),
        "effect": q(effect_map.get(b["Command"], "")), "reset": q(reset_map.get(b["Command"], "")),
        "risk_1_21": q(risk_map.get(b["Command"], "")),
        "last_execution": b.get("LastExecution", ""), "success_count": b.get("SuccessCount", ""),
        "notes": q(note), "evidence": "H",
    })
write_csv("command_blocks.csv", list(rows[0].keys()), rows)

# ---------- containers ----------
sh = [b for b in be if b["id"] == "minecraft:shulker_box"]
# cluster by 3D adjacency (they form 2x2/2x3 banks)
S = {(b["x"], b["y"], b["z"]) for b in sh}
clusters = []
while S:
    seed = S.pop(); comp = {seed}; st = [seed]
    while st:
        x, y, z = st.pop()
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    qq = (x + dx, y + dy, z + dz)
                    if qq in S:
                        S.remove(qq); comp.add(qq); st.append(qq)
    clusters.append(comp)
clusters.sort(key=lambda c: (min(p[0] for p in c), min(p[2] for p in c)))
cl_name = {}
for i, c in enumerate(clusters, 1):
    xs = [p[0] for p in c]; ys = [p[1] for p in c]; zs = [p[2] for p in c]
    name = f"roof-AC-{i:02d} ({min(xs)}..{max(xs)}, {min(ys)}..{max(ys)}, {min(zs)}..{max(zs)})"
    for p in c:
        cl_name[p] = name
rows = []
cid = 0
for b in sorted(sh, key=lambda a: (a["x"], a["z"], a["y"])):
    cid += 1
    rows.append({"id": f"C{cid:03d}", "type": "shulker_box", "block_id": "minecraft:light_gray_shulker_box",
                 "x": b["x"], "y": b["y"], "z": b["z"], "items_count": 0, "contents": "(empty)",
                 "cluster": cl_name[(b["x"], b["y"], b["z"])],
                 "inferred_use": "Rooftop air-conditioning/roof-plant prop; all 140 are empty (H).",
                 "evidence": "H"})
for b in sorted([x for x in be if x["id"] == "minecraft:barrel"], key=lambda a: (a["x"], a["z"])):
    cid += 1
    rows.append({"id": f"C{cid:03d}", "type": "barrel", "block_id": "minecraft:barrel",
                 "x": b["x"], "y": b["y"], "z": b["z"], "items_count": 0, "contents": "(empty)",
                 "cluster": "market-stall (-117..-107, z59..66)",
                 "inferred_use": "Market-stall storage under the awning; empty (H).", "evidence": "H"})
for b in sorted([x for x in be if x["id"] == "minecraft:campfire"], key=lambda a: (a["x"], a["z"])):
    cid += 1
    rows.append({"id": f"C{cid:03d}", "type": "campfire", "block_id": "minecraft:campfire",
                 "x": b["x"], "y": b["y"], "z": b["z"], "items_count": 0, "contents": "(empty)",
                 "cluster": "campfire props", "inferred_use": "Decorative campfire; no cooking items (H).", "evidence": "H"})
b = [x for x in be if x["id"] == "minecraft:blast_furnace"][0]
cid += 1
rows.append({"id": f"C{cid:03d}", "type": "blast_furnace", "block_id": "minecraft:blast_furnace",
             "x": b["x"], "y": b["y"], "z": b["z"], "items_count": 0, "contents": "(empty)",
             "cluster": "workshop", "inferred_use": "Workshop prop; empty, no fuel (H).", "evidence": "H"})
write_csv("containers.csv", list(rows[0].keys()), rows)
print("shulker clusters", len(clusters))

# ---------- signs ----------
signs = [b for b in be if b["id"] == "minecraft:sign"]
srows = []
for i, b in enumerate(sorted(signs, key=lambda a: (a["x"], a["z"])), 1):
    st = dump.get((b["x"], b["y"], b["z"]), "minecraft:acacia_sign")
    rot = ""
    m = re.search(r"rotation=(\d+)", st)
    if m: rot = "rotation=" + m.group(1)
    m = re.search(r"facing=(\w+)", st)
    if m: rot = "facing=" + m.group(1)
    texts = [b.get(f"Text{i}", "") for i in range(1, 5)]
    # clean JSON wrappers to plain text
    plain = []
    for t in texts:
        mm = re.search(r'"text":"(.*?)"', t)
        plain.append(mm.group(1) if mm else t)
    srows.append({"id": f"S{i}", "x": b["x"], "y": b["y"], "z": b["z"],
                  "block": st.split("[")[0].replace("minecraft:", ""), "rotation_facing": rot,
                  "front_line1": plain[0], "front_line2": plain[1], "front_line3": plain[2], "front_line4": plain[3],
                  "back_text": "(1.19.2 signs have no back_text; all text is front)",
                  "colour": b.get("Color", ""), "glowing": b.get("GlowingText", ""),
                  "click_event": "none", "evidence": "H"})
write_csv("signs.csv", list(srows[0].keys()), srows)

# ---------- banners ----------
bn = [b for b in be if b["id"] == "minecraft:banner"]
brows = []
for i, b in enumerate(sorted(bn, key=lambda a: (a["x"], a["z"], a["y"])), 1):
    block = dump.get((b["x"], b["y"], b["z"]), "minecraft:banner")
    brows.append({"id": f"B{i:02d}", "x": b["x"], "y": b["y"], "z": b["z"],
                  "block": block, "colour": block.replace("minecraft:", "").replace("_wall_banner", "").replace("_banner", ""),
                  "patterns": "none in NBT (H)", "evidence": "H"})
write_csv("banners.csv", list(brows[0].keys()), brows)

# ---------- entities ----------
ents = json.load(open(os.path.join(DATA, "entities.json"), encoding="utf-8"))
erows = []
for i, e in enumerate(sorted(ents, key=lambda a: (a["Pos"][0], a["Pos"][2])), 1):
    item = e.get("Item", {})
    erows.append({"id": f"E{i:02d}", "type": e.get("id", "").replace("minecraft:", ""),
                  "x": round(e["Pos"][0], 2), "y": round(e["Pos"][1], 2), "z": round(e["Pos"][2], 2),
                  "facing": e.get("Facing", ""), "item": item.get("id", ""), "item_count": item.get("Count", ""),
                  "invisible": e.get("Invisible", ""), "fixed": e.get("Fixed", ""),
                  "inferred_use": "invisible frame holding a prop (anvil / stained-glass pane)" if item else "empty frame",
                  "evidence": "H"})
write_csv("entities.csv", list(erows[0].keys()), erows)
