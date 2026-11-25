#!/usr/bin/env python3
"""Emit command_blocks.json (full) and signs.json from block_entities.json."""
import json
import os
from collections import Counter

D = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\clouds2\data"
be = json.load(open(os.path.join(D, "block_entities.json"), encoding="utf-8"))

cmds = [b for b in be if b["id"] == "minecraft:command_block"]
signs = [b for b in be if b["id"] == "minecraft:sign"]

out = []
for b in cmds:
    out.append({
        "x": b["x"], "y": b["y"], "z": b["z"],
        "auto": bool(b.get("auto", 0)),
        "conditional": bool(b.get("conditionMet", 0)),
        "powered": bool(b.get("powered", 0)),
        "success": b.get("SuccessCount", 0),
        "last_exec": b.get("LastExecution", 0),
        "track_output": bool(b.get("TrackOutput", 0)),
        "update_last": bool(b.get("UpdateLastExecution", 0)),
        "name": b.get("CustomName", ""),
        "command": b.get("Command", ""),
    })
out.sort(key=lambda r: (r["y"], r["z"], r["x"]))
json.dump(out, open(os.path.join(D, "command_blocks.json"), "w", encoding="utf-8"), indent=1)

print("commands:", len(out))
cc = Counter()
for r in out:
    cc[r["command"].split()[0]] += 1
for k, v in cc.most_common():
    print(f"{v:4d}  {k}")
print()
print("CustomName values:")
for k, v in Counter(r["name"] for r in out).most_common():
    print(f"{v:4d}  {k!r}")

# signs
sig = []
for s in signs:
    def txt(side):
        t = s.get(side) or {}
        msgs = t.get("messages", [])
        if isinstance(msgs, str):
            msgs = [msgs]
        return [str(m) for m in msgs]
    sig.append({
        "x": s["x"], "y": s["y"], "z": s["z"],
        "front": txt("front_text"),
        "back": txt("back_text"),
        "color": (s.get("front_text") or {}).get("color", ""),
        "glow": bool((s.get("front_text") or {}).get("has_glowing_text", 0)),
        "back_color": (s.get("back_text") or {}).get("color", ""),
        "back_glow": bool((s.get("back_text") or {}).get("has_glowing_text", 0)),
        "waxed": bool(s.get("is_waxed", 0)),
    })
json.dump(sig, open(os.path.join(D, "signs.json"), "w", encoding="utf-8"), indent=1)
print("\nsigns:", len(sig))
nonempty = [s for s in sig if any(x.strip() for x in s["front"]) or any(x.strip() for x in s["back"])]
print("non-empty signs:", len(nonempty))
for s in nonempty[:60]:
    print(s["x"], s["y"], s["z"], s["color"], "|", " / ".join(x for x in s["front"] if x.strip()))
