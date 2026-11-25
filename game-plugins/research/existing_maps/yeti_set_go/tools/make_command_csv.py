#!/usr/bin/env python3
"""Rebuild data/command_blocks.csv with trigger/effect/reset/1.21 columns."""
import csv
import json

rows = list(csv.DictReader(open("data/command_blocks.csv", encoding="utf-8")))

META = {
    (24, -3, -8): ("always (repeating)", "Tries to set every player to adventure; fails (no player context).", "none", "No syntax break, but still a no-op in 1.21.1: command block executes as the server, not a player. Multiverse/server gamemode applies (survival on the review server), so the build is breakable.", "LastOutput = permissions.requires.player (H); A12 confirms."),
    (24, -3, -6): ("always (repeating)", "Same as above, with leading slash.", "none", "Same no-op in 1.21.1; Multiverse/server gamemode applies.", "Leading slash is tolerated; SuccessCount=0 (H)."),
    (19, -3, -5): ("always (repeating)", "Enchants the item in each player's main hand with Unbreaking III.", "item change / death (keepInventory keeps it)", "Valid in 1.21.1; id minecraft:unbreaking unchanged.", "Runs every tick; re-applies after item swaps."),
    (19, -3, -4): ("always (repeating)", "Enchants the held item with Riptide I.", "item change / death", "Valid; minecraft:riptide unchanged.", "Needs the trident selected (slot 0)."),
    (23, -3, -2): ("always (repeating); pad under player", "Any yellow glazed pad under any player sets the GLOBAL world spawn there.", "next yellow pad overwrites", "Valid in 1.21.1; global spawn semantics unchanged.", "Turns every yellow speed pad into a checkpoint (H)."),
    (22, -3, 0): ("always (repeating)", "Spams actionbar 'You are in 1st place' to all players every tick.", "none", "Valid; still spams in 1.21.1.", "Likely a placeholder for a plugin-managed place display (M)."),
    (20, -4, 1): ("chain (after 20,-4,2)", "Tries to set adventure mode; fails.", "none", "Same no-op in 1.21.1; Multiverse/server gamemode applies.", "Chain runs north from the impulse at z3; SuccessCount=0 (H)."),
    (20, -4, 2): ("chain (after 20,-4,3)", "Sets subtitle '>3<' in red.", "next subtitle/title", "Valid JSON text in 1.21.1; 1.21.5+ prefers SNBT.", "Only '>3<' is visible because title is set after 1/2 (M)."),
    (26, -3, 2): ("always (repeating); lime pad", "Jump Boost VII for 1 s to ALL players.", "1 s duration", "Valid; jump_boost id unchanged.", "`@a` means one player's pad buffs everyone (H)."),
    (11, -4, 3): ("impulse (start button, first in chain)", "Clears the 11x3x1 start gate at z7 (glass panes + barrier).", "gate is rebuilt by hand/plugin", "Valid; 33-block fill under 1.21 limit.", "SuccessCount=0 and gate is closed in the saved world (H)."),
    (14, -4, 3): ("impulse (start button)", "Sets subtitle '>1<' in green.", "next subtitle", "Valid JSON.", "Fires before 'Starting In', so 1/2 are invisible (M)."),
    (17, -4, 3): ("impulse (start button)", "Sets subtitle '>2<' in yellow.", "next subtitle", "Valid JSON.", "Same ordering bug."),
    (20, -4, 3): ("impulse (start button) + chain", "Sets title 'Starting In' in aqua; chains subtitle >3< and gamemode.", "title timeout", "Valid JSON.", "Fires 48 ticks after the gate fill (H)."),
    (22, -3, 3): ("impulse (start button)", "Gives every player a trident in hotbar slot 0.", "next kit / item replace", "Valid; item replace syntax unchanged.", "Fires last, ~0.1 s after the title (H)."),
    (26, -5, 3): ("always (repeating); magenta pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Pad corner/centre checks; @a buffs everyone (H)."),
    (26, -4, 3): ("always (repeating); pink pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Same."),
    (26, -3, 3): ("always (repeating); red pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Same."),
    (26, -5, 4): ("always (repeating); magenta pad", "Speed IX for 1 s to all players.", "1 s", "Valid.", "Amplifier 8 is extreme but legal."),
    (26, -4, 4): ("always (repeating); pink pad", "Speed IX for 1 s to all players.", "1 s", "Valid.", "Same."),
    (26, -3, 4): ("always (repeating); red pad", "Speed IX for 1 s to all players.", "1 s", "Valid.", "Same."),
    (26, -3, 5): ("always (repeating); yellow pad", "Speed IV for 2 s to all players.", "2 s", "Valid.", "Main speed strip buff."),
    (26, -3, 6): ("always (repeating); yellow carpet", "Speed IV for 2 s to all players.", "2 s", "Valid.", "Carpet variant of the same pad."),
    (-157, 32, 27): ("always (repeating); pink pad", "Speed VIII for 1 s to all players.", "1 s", "Valid.", "Station-side pad; @a buffs everyone (H)."),
    (-157, 33, 27): ("always (repeating); pink pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Same."),
    (-156, 32, 27): ("always (repeating); magenta pad", "Speed VIII for 1 s to all players.", "1 s", "Valid.", "Same."),
    (-156, 33, 27): ("always (repeating); magenta pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Same."),
    (-155, 30, 27): ("always (repeating); yellow carpet", "Speed IV for 2 s to all players.", "2 s", "Valid.", "Same."),
    (-155, 31, 27): ("always (repeating); yellow pad", "Speed IV for 2 s to all players + setworldspawn is a separate block.", "2 s", "Valid.", "Same."),
    (-155, 32, 27): ("always (repeating); red pad", "Speed VIII for 1 s to all players.", "1 s", "Valid.", "Same."),
    (-155, 33, 27): ("always (repeating); red pad", "Jump Boost V for 1 s to all players.", "1 s", "Valid.", "Same."),
    (-155, 34, 27): ("always (repeating); lime pad", "Jump Boost VIII for 1 s to all players.", "1 s", "Valid.", "Amplifier 7 is legal."),
    (-155, 35, 27): ("always (repeating); yellow pad", "Sets GLOBAL world spawn at the station (checkpoint).", "next yellow pad", "Valid in 1.21.1.", "The only explicit mid-course checkpoint (H)."),
    (-157, 32, 28): ("always (repeating)", "Unbreaking III on held item for all players.", "item change", "Valid.", "Station kit refresh."),
    (-156, 32, 28): ("always (repeating)", "Riptide II on held item for all players.", "item change", "Valid; level 2 legal.", "Needed for the final waterfall riptide (M)."),
    (-155, 33, 28): ("always (repeating); stone under player", "Removes the chestplate (elytra) from ALL players.", "next lodestone pad", "Valid; armor.chest slot unchanged.", "One player's stone pad strips everyone's elytra (H)."),
    (-155, 34, 28): ("always (repeating); yellow pad", "Removes the chestplate (elytra) from ALL players.", "next lodestone pad", "Valid.", "Second elytra-off pad."),
    (-155, 35, 28): ("always (repeating); lodestone under player", "Equips an elytra on ALL players.", "stone/yellow pads", "Valid; armor.chest slot unchanged.", "Only elytra grant in the map; works at the station, needs sim>=10 for the lodge, >=12 for the start (H/M)."),
}

out = []
for r in rows:
    key = (int(r["x"]), int(r["y"]), int(r["z"]))
    trig, eff, reset, risk, note = META[key]
    r["trigger"] = trig
    r["effect"] = eff
    r["reset"] = reset
    r["risk_1_21"] = risk
    r["notes"] = note
    out.append(r)

fields = ["x", "y", "z", "block", "facing", "conditional", "auto", "powered",
          "command", "trigger", "effect", "reset", "risk_1_21", "notes",
          "last_execution", "last_output", "custom_name"]
with open("data/command_blocks.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=fields)
    w.writeheader()
    for r in out:
        w.writerow({k: r.get(k, "") for k in fields})
print(f"wrote {len(out)} rows with trigger/effect/reset/risk")
