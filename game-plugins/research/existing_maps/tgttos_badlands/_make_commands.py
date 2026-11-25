#!/usr/bin/env python3
"""Line-by-line command export of the tgttos loop function."""
import os, csv

HERE = os.path.dirname(os.path.abspath(__file__))
LOOP = os.path.join(HERE, "data", "raw", "loop.mcfunction")
OUT = os.path.join(HERE, "data", "datapack_commands.csv")

GROUPS = {
    1: ("item hygiene", "every tick", "kill all dropped item entities"),
    2: ("chicken immunity", "every tick", "Resistance VI 1 s to every chicken"),
    3: ("start-pen hygiene", "every tick", "clear blue concrete in x 9..11, y -42..-40, z 21..40"),
    5: ("kit", "every tick", "offhand blue concrete, count 2, CanPlaceOn 44 blocks"),
    6: ("kit", "every tick", "offhand blue concrete, count 1"),
    7: ("kit", "every tick", "hotbar.0 iron pickaxe, Efficiency II, CanDestroy blue_concrete"),
}


def group_of(i):
    if i in GROUPS:
        return GROUPS[i]
    if 9 <= i <= 23:
        return ("countdown", "start>=1", "advance countdown, titles, open gate, set playing")
    if 25 <= i <= 28:
        return ("playtime", "playing>=1", "advance spawntime/playtimeT/playtimeS")
    if 30 <= i <= 31:
        return ("reset markers", "spawntime>=20", "summon invisible armor_stand 10 above player; reset spawntime")
    if 33 <= i <= 40:
        return ("finish detection", "hit_chicken>=1", "stop playing, title time, spectator, finished+1, particles/sound")
    if 41 <= i <= 49:
        return ("finish reset", "finished>=60", "barriers, clear, adventure, tp all to start, reset timers")
    if 52 <= i <= 53:
        return ("block cleanup", "finished>=60", "fill air replace blue_concrete around every remove_block stand; kill stands")
    if 56 <= i <= 120:
        return ("cactus reset", "finished>=60", "restore cacti at fixed coordinates")
    if 123 <= i <= 124:
        return ("score reset", "finished>=60", "clear hit_chicken and finished for all players")
    return ("", "", "")


def risk_of(i, line):
    if i in (5, 6, 7):
        return "FAILS in 1.21.1: pre-1.20.5 item NBT (CanPlaceOn/CanDestroy/Enchantments). Use item components."
    if i == 3:
        return "OK 1.21.1 (fill air replace)."
    if i == 2:
        return "OK 1.21.1."
    if 9 <= i <= 23:
        return "OK 1.21.1; logic bug: countdown advances once per started player."
    if 33 <= i <= 49:
        return "OK 1.21.1; multiplayer: @a title/tp/clear reset everyone."
    if 52 <= i <= 53:
        return "OK 1.21.1; multiplayer: global cleanup wipes other racers' bridges."
    if 56 <= i <= 120:
        return "OK 1.21.1; 4 cacti at x=154,z=33 missing; 2 stale at x=22,z=25."
    if 123 <= i <= 124:
        return "OK 1.21.1; resets all players, not just the finisher."
    return "OK 1.21.1 (entity NBT / scoreboard)."


def main():
    lines = open(LOOP, encoding="utf-8").read().splitlines()
    with open(OUT, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["line", "group", "trigger", "command", "effect", "risk_1_21_1"])
        for i, line in enumerate(lines, 1):
            if not line.strip():
                continue
            g, trig, eff = group_of(i)
            w.writerow([i, g, trig, line, eff, risk_of(i, line)])
    print("wrote", OUT, "rows", len([l for l in lines if l.strip()]))


if __name__ == "__main__":
    main()
