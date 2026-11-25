#!/usr/bin/env python3
"""Read scoreboard objectives + gamerules from the 1.17.1 source world (read-only)."""
import os
import nbtlib

WORLD = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3"

sb = nbtlib.load(os.path.join(WORLD, "data", "scoreboard.dat"))
print("== scoreboard.dat root keys:", list(sb.keys()))
data = sb.get("data", sb)
print("data keys:", list(data.keys()))
for k in ("Objectives", "PlayerScores", "Teams", "DisplaySlots"):
    v = data.get(k)
    print(f"-- {k}:", len(v) if hasattr(v, "__len__") else v)
    if k == "Objectives" and v:
        for o in v:
            print("   ", {kk: o[kk] for kk in o.keys()})
    if k == "Teams" and v:
        for t in v:
            print("   ", {kk: t[kk] for kk in t.keys() if kk in ("Name", "DisplayName", "TeamColor", "FriendlyFire", "SeeFriendlyInvisibles", "AllowFriendlyFire")})
    if k == "PlayerScores" and v:
        for s in v:
            name = s.get("Name")
            if "parkour" in str(name).lower() or "timer" in str(name).lower():
                print("   ", {kk: s[kk] for kk in s.keys()})

lvl = nbtlib.load(os.path.join(WORLD, "level.dat"))
d = lvl.get("Data", lvl)
print("\n== level DataVersion:", d.get("DataVersion"), "LevelName:", d.get("LevelName"))
g = d.get("GameRules")
for k in sorted(g.keys()):
    print(f"   {k} = {g[k]}")
print("allowCommands:", d.get("allowCommands"), "GameType:", d.get("GameType"), "Difficulty:", d.get("Difficulty"))
