#!/usr/bin/env python3
"""Enrich the raw block counts with roles/uses and write the map data tables."""
import csv
import json
import os
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SCAN = os.path.join(HERE, "world_scan.json")
ENT = os.path.join(HERE, "entities_raw.json")

# base material -> (role, use)
ROLE = {
    "black_stained_glass": ("shell / void / finish", "Black glass: 7,216-block ceiling at y=29; 27,090 blocks in 10 horizontal layers at y=-62..-44 (the fake pit bottom); 68-block finish lip at x=59..60, y=-9. Transparent to skylight, so it tints the map dark without making it unlit."),
    "light_blue_wool": ("shell", "One-block-thick perimeter wall from y=-64 to y=29 (344 blocks per layer x 94 layers); reads as a blue horizon/sky from inside and hides the external base."),
    "cobblestone": ("road / structure / finish", "Main structural block: road-A slab, road-B tower, finish-pool tower, pool floor, park boulders, building bases."),
    "bricks": ("building / finish", "Building facades (15,650 total), the finish-pool tower and the pool side walls."),
    "gray_terracotta": ("building / road shoulder", "Building walls and road shoulders; the most common neutral facade block."),
    "quartz_block": ("road markings / trim", "White paint: 2:1 centre dashes on both roads, finish plaza slab, pool rim, building trim."),
    "light_gray_wool": ("building", "Light facade bands on the surrounding buildings."),
    "brown_wool": ("building", "Warm building walls/roofs; wholly solid columns in the east block."),
    "brown_terracotta": ("building / roof", "Brown facade and roof bands, mainly the north and south building rows."),
    "gray_wool": ("building / plaza", "Building bands and the small south plaza at z=51."),
    "shroomlight": ("lighting", "2,709-block hidden light grid at y=-64, under the pit's glass layers; gives the void a faint warm bottom."),
    "black_carpet": ("lighting", "2,709-block carpet over the shroomlight grid; hides the light source from below/above."),
    "light_gray_terracotta": ("building", "Neutral facade and trim, used heavily on the park-side blocks."),
    "end_stone_bricks": ("park / finish underlay", "Park path/rock accents (227 near the start), pool underlay and roof details."),
    "green_concrete": ("park", "The park's sculpted terrain/tree canopy west of the start (1,827 blocks, y -12..17)."),
    "blackstone": ("road surface", "Road-A and road-B asphalt; also dark accents on buildings."),
    "red_concrete": ("external banner", "Red bands of the three 25-wide banners outside the south wall at z=83 (not reachable from the course)."),
    "stone_bricks": ("building", "Building trim, window surrounds and roof details."),
    "green_wool": ("park", "Park foliage/hedges and the small green planters around the start."),
    "polished_diorite": ("building / trim", "Roof details, window sills, building trim."),
    "white_concrete": ("external banner", "White bands of the three external banners at z=83 (675 blocks = 3 x 25 x 9)."),
    "yellow_terracotta": ("road markings", "Yellow edge lines on both roads and the finish approach."),
    "black_wool": ("start / accents", "Start trench walls, dark building accents, park shadows."),
    "barrier": ("gameplay / legacy", "156 blocks form the Pits start cage (x13..17, z25..46, y -2..1); the other 184 form a sealed legacy cage at x63..84, y -33..-29, z9..13 copied with the Industry base."),
    "black_concrete": ("building", "Dark window/trim blocks on the surrounding buildings."),
    "light_blue_stained_glass": ("building", "Blue-tinted windows on the city blocks."),
    "spruce_planks": ("park furniture / trim", "Park benches/tables and building trim."),
    "red_sandstone": ("park", "Park path/rock accents; warm contrast to the green terrain."),
    "snow_block": ("park", "Small snow patches on the park canopy (174 blocks in 3x3-ish clusters)."),
    "light_blue_concrete": ("building / pool tower", "Light blue bands in the finish-pool tower and building accents."),
    "red_wool": ("park", "Flower/foliage accents in the park and along the south building row."),
    "white_wool": ("park / trim", "White accents in the park and building details."),
    "light_gray_concrete": ("start", "The start trench's west-wall filler at y=-2; only 58 blocks."),
    "diorite": ("building / trim", "Small trim blocks in the buildings."),
    "netherrack": ("park", "Dark red rock accents in the park."),
    "sandstone": ("park", "Park path/step blocks."),
    "gray_concrete": ("start", "The start sign wall's 14-block patch at y=-2."),
    "glowstone": ("lighting", "8 blocks in two lamp pairs (x30/39, y8, z5-6 and x32/41, y12, z65-66) in the north/south buildings."),
    "polished_granite": ("park", "Park boulder/trim accents."),
    "yellow_concrete": ("park", "Small yellow flower accents in the park."),
    "oak_wall_sign": ("start", "The single clickable Start sign at 14,-1,36, facing east."),
    "smooth_stone_slab": ("park / steps", "Park path edging and steps."),
    "sandstone_slab": ("park / steps", "Park steps and path edging."),
    "quartz_slab": ("park / trim", "Park shelter/table trim and steps."),
    "quartz_stairs": ("park / trim", "Park shelter/table trim."),
    "end_stone_brick_slab": ("park / trim", "Park step/trim detail."),
    "sandstone_stairs": ("park / steps", "Park stairs."),
    "polished_granite_slab": ("park", "Park boulder/step detail."),
}


def main():
    with open(SCAN, encoding="utf-8") as f:
        scan = json.load(f)
    rows = []
    with open(os.path.join(HERE, "blocks.csv"), encoding="utf-8") as f:
        r = csv.DictReader(f)
        for row in r:
            rows.append(row)
    out = []
    for row in rows:
        state = row["block_state"]
        base = state.split("[")[0].replace("minecraft:", "")
        role, use = ROLE.get(base, ("other", "Unclassified; see blocks.csv raw count."))
        out.append({
            "block_state": state,
            "count": row["count"],
            "share_percent": row["share_percent"],
            "role": role,
            "use": use,
            "first_x": row["first_x"],
            "first_y": row["first_y"],
            "first_z": row["first_z"],
        })
    out.sort(key=lambda r: int(r["count"]), reverse=True)
    with open(os.path.join(HERE, "blocks.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["block_state", "count", "share_percent", "role", "use",
                                         "first_x", "first_y", "first_z"])
        w.writeheader()
        w.writerows(out)
    print("blocks.csv", len(out), "rows, total", scan["total_non_air_blocks"])

    # signs.csv exact
    with open(os.path.join(HERE, "signs.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "block", "facing", "front_line1", "front_line2", "front_line3",
                    "front_line4", "back", "color", "glowing", "click_line", "click_action", "click_value"])
        w.writerow([14, -1, 36, "minecraft:oak_wall_sign", "east",
                    '{"text":""} (clickEvent run_command)',
                    '{"text":"Start","color":"green"}',
                    '{"text":""}', '{"text":""}', "(none / 1.19 Text1-4 only)",
                    "black", "false", "1", "run_command", "tag @a add starting"])
    print("signs.csv 1 row")

    # checkpoints / route gates (there are no datapack checkpoints)
    with open(os.path.join(HERE, "checkpoints.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["order", "type", "name", "x", "y", "z", "gate_axis", "width", "what_it_grants_or_clears",
                    "confidence", "evidence"])
        w.writerow([1, "start_button", "Start sign", 14, -1, 36, "east", 1,
                    "tags all players 'starting'; begins the 7 s countdown", "H",
                    "sign NBT Text1 clickEvent; start_timer.mcfunction"])
        w.writerow([2, "start_gate", "Cage east exit", 17, -2, 36, "z", 22,
                    "opens at GO; step out onto Road A (y=-3)", "H",
                    "barrier shell x13/x17,z25/z46; start_timer fill"])
        w.writerow([3, "start_gate", "Cage north exit", 15, -2, 25, "x", 4,
                    "opens at GO; 2-high corridor z21-24 into the park", "H",
                    "barrier shell; column probe x15 z21-25"])
        w.writerow([4, "start_gate", "Cage south exit", 15, -2, 46, "x", 4,
                    "opens at GO; 2-high corridor z47-50 to the south plaza", "H",
                    "barrier shell; column probe x15 z47-51"])
        w.writerow([5, "pit", "Pit 1", 31, -3, 36, "x", 12,
                    "no checkpoint: falling below y=-41 teleports to start", "H",
                    "respawn.mcfunction; surface/platform scan"])
        w.writerow([6, "pit", "Pit 2", 52, -7, 36, "x", 12,
                    "no checkpoint: falling below y=-41 teleports to start", "H",
                    "respawn.mcfunction; surface/platform scan"])
        w.writerow([7, "finish", "Chicken pool", 64, -11, 36, "x", 7,
                    "punch any of 20 chickens -> finished tag, spectator, time title", "H",
                    "loop.mcfunction finish block; entities/*.mca"])
        w.writerow([8, "reset", "Round reset", 15, -2, 36, "-", 0,
                    "7 s after first finish: tp all to start, refill barriers, clear kit, reset timer", "H",
                    "loop.mcfunction finished=140 block"])
    print("checkpoints.csv 8 rows (no real checkpoints)")

    # entities.csv
    with open(ENT, encoding="utf-8") as f:
        ents = json.load(f)
    with open(os.path.join(HERE, "entities.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["index", "type", "x", "y", "z", "tags", "custom_name", "persistent", "effects",
                    "on_ground", "role"])
        i = 0
        for e in sorted(ents, key=lambda r: (r["z"], r["x"])):
            i += 1
            w.writerow([i, e["id"], round(e["x"], 3), round(e["y"], 3), round(e["z"], 3),
                        ";".join(e["tags"]), e["custom_name"], "PersistenceRequired=0",
                        "Resistance amplifier 5, 19-tick saved duration, no particles",
                        "1", "finish chicken"])
    print("entities.csv", i, "rows")

    # functions.csv
    funcs = [
        ("load", "data/tgttos/functions/load.mcfunction", 14, "load tag", "Creates 14 scoreboard objectives and initialises the per-player time scores."),
        ("loop", "data/tgttos/functions/loop.mcfunction", 129, "tick tag", "Main state machine: finish detection, spectator, cleanup anchors, timing, HUD, kit, block reset, round reset."),
        ("color", "data/tgttos/functions/color.mcfunction", 111, "tick tag", "Colour picker tellraw menu + tag application; default red."),
        ("start_timer", "data/tgttos/functions/start_timer.mcfunction", 25, "tick tag", "7 s countdown titles, GO, opens cage, adds playing tag."),
        ("gamerules", "data/tgttos/functions/gamerules.mcfunction", 15, "tick tag", "Re-applies 13 gamerules and difficulty every tick."),
        ("respawn", "data/tgttos/functions/respawn.mcfunction", 3, "tick tag", "Stores each player's Y; below -41 teleports to the start."),
        ("(no block_reset)", "-", 0, "-", "Pits has no separate block_reset; cleanup is inline in loop via remove_block armor stands."),
    ]
    with open(os.path.join(HERE, "functions.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["function", "path", "lines", "tag", "role"])
        w.writerows(funcs)
    print("functions.csv", len(funcs), "rows")

    # empty command_blocks.csv with a note row in a separate markdown
    with open(os.path.join(HERE, "command_blocks.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "type", "auto", "condition", "command", "trigger", "effect", "1.21_risk"])
        # no command blocks: world scan and block palette both have zero command_block entries
    print("command_blocks.csv header only (0 command blocks)")


if __name__ == "__main__":
    main()
