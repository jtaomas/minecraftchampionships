#!/usr/bin/env python3
"""Export the Pits datapack functions/tags verbatim (read-only source) and build tables."""
import csv
import os
import re

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat\datapacks\Pits"
HERE = os.path.dirname(os.path.abspath(__file__))

FILES = [
    ("pack.mcmeta", "pack.mcmeta"),
    ("load tag", r"data\minecraft\tags\functions\load.json"),
    ("tick tag", r"data\minecraft\tags\functions\tick.json"),
    ("load", r"data\tgttos\functions\load.mcfunction"),
    ("loop", r"data\tgttos\functions\loop.mcfunction"),
    ("color", r"data\tgttos\functions\color.mcfunction"),
    ("start_timer", r"data\tgttos\functions\start_timer.mcfunction"),
    ("gamerules", r"data\tgttos\functions\gamerules.mcfunction"),
    ("respawn", r"data\tgttos\functions\respawn.mcfunction"),
]


def main():
    export = []
    manifest = []
    for name, rel in FILES:
        path = os.path.join(SRC, rel)
        text = open(path, encoding="utf-8").read()
        lines = text.splitlines()
        export.append(f"===== {name} ({rel}) =====")
        for i, line in enumerate(lines, 1):
            export.append(f"{i:4d}| {line}")
        export.append("")
        cmds = [l for l in lines if l.strip() and not l.strip().startswith("#")]
        manifest.append((name, rel.replace("\\", "/"), len(lines), len(cmds), len(text)))
    with open(os.path.join(HERE, "commands_export.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(export))
    with open(os.path.join(HERE, "datapack_manifest.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["part", "path", "lines", "non_empty_lines", "bytes"])
        w.writerows(manifest)

    objectives = [
        ("hit_chicken", "minecraft.custom:minecraft.damage_dealt_resisted", "hit_chicken", "0", "finish detection: increments when the player's damage is resisted by a chicken"),
        ("playing", "dummy", "playing", "0", "tag proxy: 1 = timer running; removed at first finish"),
        ("countdown", "dummy", "countdown", "0", "ticks since Start sign click; 40..140 = 5..1, >=140 = GO"),
        ("spawntime", "dummy", "spawntime", "0", "ticks alive while playing; every 20 spawns a cleanup armor stand"),
        ("playtimeMi1", "dummy", "playtimeMi1", "0", "minutes ones / timer digit"),
        ("playtimeMi2", "dummy", "playtimeMi2", "0", "minutes tens / timer digit"),
        ("playtimeSe1", "dummy", "playtimeSe1", "0", "seconds ones"),
        ("playtimeSe2", "dummy", "playtimeSe2", "0", "seconds tens"),
        ("playtimeMs1", "dummy", "playtimeMs1", "0", "hundredths (0 or 5), +5 per tick"),
        ("playtimeMs2", "dummy", "playtimeMs2", "0", "tenths of a second"),
        ("finished", "dummy", "finished", "0", "ticks since finish; 60 = cleanup, 140 = reset"),
        ("respawn", "dummy", "respawn", "0", "last stored feet Y (truncated); <=-41 teleports to start"),
        ("color", "trigger", "color", "0", "player-triggered colour menu; /trigger color"),
        ("selectColor", "dummy", "selectColor", "0", "selected colour id: 1 red, 2 orange, 4 lime, 6 cyan, 7 aqua, 8 blue, 9 purple, 10 pink"),
    ]
    with open(os.path.join(HERE, "scoreboards.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["objective", "criteria", "display", "default", "role"])
        w.writerows(objectives)

    with open(os.path.join(HERE, "tags.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["tag", "path", "values", "meaning"])
        w.writerow(["load", "data/minecraft/tags/functions/load.json", "tgttos:load", "runs once on datapack load/reload"])
        w.writerow(["tick", "data/minecraft/tags/functions/tick.json", "tgttos:loop, tgttos:color, tgttos:start_timer, tgttos:gamerules, tgttos:respawn", "runs every tick in that order"])

    timeline = [
        ("join", "player joins / world loads", "-", "loop gives kit every tick; default red tag; timer 00:00:00", "H"),
        ("start", "click sign line 1 (14,-1,36)", "t=0", "tag @a add starting; everyone begins countdown", "H"),
        ("countdown quiet", "countdown 1..39", "0-1.95 s", "no title (2 s lead-in)", "H"),
        ("countdown 5..1", "countdown 40..139", "2.0-7.0 s", "title 'Starting in' + 5/4/3/2/1 (1 s each)", "H"),
        ("GO", "countdown 140", "7.0 s", "GO!! title, tag playing, cage fill air, starting removed", "H"),
        ("bridge 1", "road A edge x25 -> road B x38", "~1-4 s", "12 concrete blocks over Pit 1; drop 4 to y=-7", "M"),
        ("bridge 2", "road B edge x46 -> finish lip x59", "~1-4 s", "12 concrete blocks over Pit 2; drop 2 to y=-9", "M"),
        ("finish", "punch any chicken (hit_chicken>=1)", "run time", "tag finished, spectator, time title, playing removed for all", "H"),
        ("cleanup", "finished=60", "+3 s", "remove 8 concrete colours in 31^3 boxes around recorded armor stands; kill stands", "H"),
        ("reset", "finished=140", "+7 s", "tp all to start, refill 3 barrier walls, adventure, clear, timers 0, finished 0", "H"),
        ("fall", "feet y<=-41", "next tick", "tp to 15,-2,36; timer keeps running; no checkpoint", "H"),
    ]
    with open(os.path.join(HERE, "timeline.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["event", "trigger", "when", "action", "confidence"])
        w.writerows(timeline)
    print("wrote commands_export.txt, datapack_manifest.csv, scoreboards.csv, tags.csv, timeline.csv")


if __name__ == "__main__":
    main()
