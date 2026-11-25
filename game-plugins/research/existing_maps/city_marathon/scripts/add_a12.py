#!/usr/bin/env python3
"""Add A12 server-verification cross-reference to the City Marathon command table."""
import csv, os
BASE = r"C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps\city_marathon"
path = os.path.join(BASE, "data", "command_blocks.csv")
rows = list(csv.DictReader(open(path, encoding="utf-8")))
fields = list(rows[0].keys())
if "a12_verification" not in fields:
    fields.insert(fields.index("notes") + 1, "a12_verification")
for r in rows:
    cmd = r["command"].strip()
    if cmd in ("gamemode adventure", "/gamemode adventure"):
        r["a12_verification"] = "FAIL - permissions.requires.player (A12); no target player for a command-block source."
        r["notes"] = (r["notes"] + " " if r["notes"] else "") + "A12 confirms this exact block fails in the live server; SuccessCount=0 here agrees."
    else:
        r["a12_verification"] = "No failure reported in A12's 7-failure list."
with open(path, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=fields)
    w.writeheader(); w.writerows(rows)
print("updated", path, len(rows))
