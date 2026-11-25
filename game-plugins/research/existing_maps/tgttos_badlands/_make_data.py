#!/usr/bin/env python3
"""Generate the A7 CSV evidence pack from the scanned world and parsed datapack."""
import os, re, json, csv, gzip, hashlib
import nbtlib

ROOT = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-badlands-by-mickeykat\TGTTOS Badlands - By MickeyKat"
HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")

ROLE = {
 'stone': 'terrain', 'terracotta': 'terrain', 'granite': 'terrain', 'andesite': 'terrain',
 'red_sand': 'terrain', 'dirt': 'terrain', 'brown_concrete_powder': 'terrain',
 'red_terracotta': 'terrain', 'gray_terracotta': 'terrain', 'diorite': 'terrain',
 'polished_granite': 'terrain', 'light_gray_terracotta': 'terrain', 'cyan_terracotta': 'terrain',
 'soul_sand': 'terrain / slow hazard', 'gravel': 'terrain', 'black_terracotta': 'terrain',
 'smooth_red_sandstone': 'terrain / build', 'cracked_stone_bricks': 'build (ruin)',
 'white_terracotta': 'build (walls, towers, roofs)', 'quartz_block': 'build (start floor, finish approach)',
 'stone_bricks': 'build (paving, ruins)', 'smooth_stone': 'build (paving, roads)', 'smooth_stone_slab': 'build (paving)',
 'white_concrete': 'sign / accent', 'red_concrete': 'sign letters / accent', 'black_concrete': 'accent (rail/wall)',
 'gray_concrete': 'accent (sign base)', 'light_gray_concrete': 'accent', 'cyan_concrete': 'accent',
 'light_blue_concrete': 'accent', 'black_concrete_powder': 'terrain / accent',
 'red_wool': 'accent (sign, markers)', 'black_wool': 'accent (start frame, rail bed)',
 'white_wool': 'accent', 'gray_wool': 'accent (sign cap)', 'nether_brick_wall': 'industrial trim',
 'nether_brick_slab': 'industrial trim', 'nether_brick_fence': 'industrial trim',
 'blackstone': 'industrial trim / rail', 'iron_block': 'industrial trim', 'iron_bars': 'fence',
 'iron_trapdoor': 'roof / fence / chicken ledge', 'chain': 'hanging detail / fence',
 'sea_lantern': 'light', 'lantern': 'light', 'daylight_detector': 'solar-panel detail',
 'jungle_planks': 'crates / scaffolding', 'jungle_fence': 'crates', 'spruce_planks': 'start pen / detail',
 'stripped_spruce_log': 'posts / trees', 'stripped_dark_oak_log': 'post',
 'acacia_leaves': 'vegetation', 'oak_leaves': 'vegetation', 'cactus': 'vegetation / hazard',
 'dead_bush': 'vegetation', 'barrier': 'technical (start gate)', 'oak_wall_sign': 'technical (start trigger)',
 'light_blue_concrete_powder': 'accent', 'light_gray_concrete_powder': 'accent',
 'cyan_concrete_powder': 'terrain / accent', 'red_sandstone_stairs': 'stairs / detail', 'diorite_wall': 'detail',
}

def load():
    blocks = json.load(open(os.path.join(HERE, "raw_blocks.json")))
    ents = json.load(open(os.path.join(HERE, "raw_entities.json")))
    tes = json.load(open(os.path.join(HERE, "raw_tile_entities.json")))
    pack = json.load(open(os.path.join(HERE, "raw_datapack.json")))
    return blocks, ents, tes, pack


def write_blocks(blocks):
    total = sum(n for _, n in blocks["counts"])
    with open(os.path.join(DATA, "blocks.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["block_state", "block_id", "count", "share_pct", "role"])
        for state, n in blocks["counts"]:
            bid = state.split("[")[0].replace("minecraft:", "")
            w.writerow([state, bid, n, f"{100.0*n/total:.3f}", ROLE.get(bid, "detail")])
    print("blocks.csv", len(blocks["counts"]), "rows", total)


def write_entities(ents):
    with open(os.path.join(DATA, "entities.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["index", "type", "x", "y", "z", "health", "active_effects", "tags", "role", "notes"])
        for i, e in enumerate(ents):
            n = e["nbt"]
            pos = n.get("Pos", [0, 0, 0])
            eff = n.get("ActiveEffects") or []
            eff_s = ";".join(f"id={a.get('Id')},amp={a.get('Amplifier')},dur={a.get('Duration')},ambient={a.get('Ambient')}" for a in eff)
            role = "finish chicken (pen)" if pos[1] > -10 else "lost/void chicken"
            note = ""
            if pos[1] < -64:
                note = "Below the world; will be removed on load. Evidence that chickens can be lost; no respawn command."
            elif eff:
                note = "Saved with Resistance (id 11, amp 5) - datapack was running at save time."
            w.writerow([i, n.get("id"), f"{pos[0]:.2f}", f"{pos[1]:.2f}", f"{pos[2]:.2f}",
                        n.get("Health"), eff_s, ";".join(n.get("Tags") or []), role, note])
    print("entities.csv", len(ents))


def write_signs(tes):
    signs = [t for t in tes if t["nbt"].get("id") == "minecraft:sign"]
    with open(os.path.join(DATA, "signs.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "block", "facing", "line1", "line2", "line3", "line4",
                    "colour", "glow", "click_line", "click_command", "1_21_1_risk"])
        for s in signs:
            n = s["nbt"]
            lines = [str(n.get(f"Text{i}", "")) for i in range(1, 5)]
            # keep the exact JSON of the two non-empty lines, plain text for the empty ones
            w.writerow([n.get("x"), n.get("y"), n.get("z"), "oak_wall_sign", "east",
                        lines[0], lines[1], lines[2], lines[3], n.get("Color"), n.get("GlowingText"),
                        1, "scoreboard players add @s start 1",
                        "Legacy Text1-4 upgrades to front_text.messages in 1.20+. clickEvent is on empty line 1; visible 'Start' is line 2 with no clickEvent. Verify the clickable hotspot in 1.21.1."])
    print("signs.csv", len(signs))


def write_command_blocks(tes):
    cbs = [t for t in tes if "command_block" in str(t["nbt"].get("id"))]
    with open(os.path.join(DATA, "command_blocks.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["x", "y", "z", "type", "auto", "condition", "command", "trigger", "effect", "risk_1_21"])
        if not cbs:
            w.writerow(["none", "none", "none", "none", "none", "none", "none", "none",
                        "0 command blocks; 49 tile entities = 1 sign + 48 daylight detectors", "n/a"])
    print("command_blocks.csv", len(cbs))


def write_gamerules():
    d = nbtlib.load(os.path.join(ROOT, "level.dat"))["Data"]
    gr = d["GameRules"]
    with open(os.path.join(DATA, "gamerules.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["rule", "value", "effect_on_this_map"])
        notes = {
            "fallDamage": "No fall damage: the 20-block drop into the finish canyon and the mesa descent are safe.",
            "doDaylightCycle": "Permanent time (DayTime=0, sunrise); golden-hour lighting.",
            "doWeatherCycle": "No rain/thunder.",
            "doMobSpawning": "No natural mobs; only the 27 saved chickens.",
            "doTileDrops": "Mined blue concrete drops nothing; item entities are killed every tick anyway.",
            "doEntityDrops": "No entity drops.",
            "doMobLoot": "No mob loot.",
            "randomTickSpeed": "0: no crop/leaf/ice random ticks; leaves are persistent.",
            "keepInventory": "false: death drops inventory (but items are killed and the kit is re-given).",
            "doImmediateRespawn": "false: death screen, then vanilla respawn; no datapack respawn handler.",
            "spawnRadius": "10: new players without a personal spawn respawn within 10 blocks of 10,-42,31, possibly outside the start pen.",
            "showDeathMessages": "false.",
            "naturalRegeneration": "true, but no damage sources except void.",
            "drowningDamage": "true, no water.",
            "fireDamage": "true, no fire/lava.",
            "freezeDamage": "true, no powder snow.",
            "mobGriefing": "true, but no mobs.",
            "maxEntityCramming": "24.",
            "commandBlockOutput": "true, but 0 command blocks.",
            "logAdminCommands": "true.",
            "sendCommandFeedback": "true.",
            "announceAdvancements": "No advancement use.",
            "disableElytraMovementCheck": "false, no elytra.",
            "doFireTick": "No fire spread; no fire sources.",
            "doInsomnia": "No phantoms.",
            "doPatrolSpawning": "No patrols.",
            "doTraderSpawning": "No traders.",
            "doWardenSpawning": "No wardens.",
            "doLimitedCrafting": "Crafting unrestricted.",
            "forgiveDeadPlayers": "No anger/raids.",
            "universalAnger": "No anger/raids.",
            "playersSleepingPercentage": "100; not used.",
            "reducedDebugInfo": "false.",
            "spectatorsGenerateChunks": "true.",
        }
        for k in sorted(gr.keys()):
            w.writerow([k, str(gr[k]), notes.get(k, "")])
    print("gamerules.csv", len(gr))


def write_objectives():
    sb = nbtlib.load(os.path.join(ROOT, "data", "scoreboard.dat"))["data"]
    created = {"hit_chicken", "playing", "countdown", "start", "spawntime", "playtimeS", "playtimeT", "finished"}
    with open(os.path.join(DATA, "objectives.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["objective", "criterion", "created_by_load", "used_by_loop", "purpose", "notes"])
        purpose = {
            "hit_chicken": "finish detection (damage dealt that was resisted)",
            "playing": "1 while running; drives playtime and marker spawning",
            "countdown": "countdown ticks 0..130",
            "start": "sign click sets >=1; starts the countdown",
            "spawntime": "ticks since last reset-marker summon",
            "playtimeS": "whole seconds of run time, shown at finish",
            "playtimeT": "ticks remainder, shown at finish",
            "finished": "ticks since finish; 60 = return to start",
            "playtime": "dead objective saved in scoreboard.dat; not created or used by this pack",
        }
        for o in sb["Objectives"]:
            name = str(o["Name"])
            w.writerow([name, str(o["CriteriaName"]), "yes" if name in created else "no", "yes" if name in created and name != "playtime" else "no",
                        purpose.get(name, ""), "" if name in created else "legacy leftover"])
    print("objectives.csv", len(sb["Objectives"]))


def write_can_place(pack):
    blocks = json.load(open(os.path.join(HERE, "raw_blocks.json")))
    pal = sorted({b.split("[")[0].replace("minecraft:", "") for b, _ in blocks["counts"]})
    can = set(pack["can_place_on"])
    with open(os.path.join(DATA, "can_place_on.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["block_id", "in_can_place_on", "in_world_palette", "impact"])
        for b in sorted(set(pal) | can):
            impact = ""
            if b in pal and b not in can:
                impact = "Present in the map but NOT placeable-on: players cannot bridge off this block."
            elif b not in pal and b in can:
                impact = "Whitelisted but not present in the saved world (future/earlier build)."
            w.writerow([b, "yes" if b in can else "no", "yes" if b in pal else "no", impact])
    print("can_place_on.csv", len(set(pal) | can), "can", len(can), "palette", len(pal))


def write_cactus():
    pack = json.load(open(os.path.join(HERE, "raw_datapack.json")))
    with open(os.path.join(DATA, "cactus_reset.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["loop_line", "command"])
        for c in pack["cactus_commands"]:
            w.writerow([c["line"], c["cmd"]])
    print("cactus_reset.csv", len(pack["cactus_commands"]))
    print("cactus reset count", pack["cactus_reset_count"], "world", pack["cactus_world_count"])
    print("missing from reset", pack["cactus_missing_from_reset"])
    print("extra in reset", pack["cactus_extra_in_reset"])


def write_waypoints():
    rows = [
        ("START", "start pen floor", 10, -43, 31, "Quartz floor; player spawn 10,-42,31; sign 9,-41,31 on the west wall."),
        ("START", "gate east wall", 12, -42, 31, "Barrier x=12, z 20..41, y -42..-38; removed by the countdown fill at 120."),
        ("LEG1", "first stake", 25, -31, 41, "Red concrete/red wool marker stake; three stakes on the west slope."),
        ("LEG1", "first rail line", 64, -30, 31, "3-wide smooth-stone strip x 63..65, z 10..52; also the MCC billboard x=64 above."),
        ("LEG2", "mine west gate", 64, -29, 31, "Course passes under the MCC billboard/gantry at x=64."),
        ("LEG2", "mine plaza", 100, -29, 31, "Open plaza between the two white mine wings; wings at z 2..14 and 47..60."),
        ("LEG2", "solar arrays", 99, -18, 9, "Daylight-detector arrays x 94..104, z 7..12 and 50..55."),
        ("LEG3", "stone-brick platform", 120, -28, 31, "x 118..123, y -31..-28, z 12..44."),
        ("LEG3", "black rail/wall", 137, -26, 31, "x=137..139, y -29..-26, z 9..53; go around the z ends or bridge."),
        ("LEG4", "canyon floor / cave mouth", 150, -22, 31, "Mesa west face; cave mouth at x~150..160, z 14..50, y -22..-5."),
        ("LEG4", "cave chamber", 175, -20, 31, "West-facing dead-end chamber; white pillar walls at x 182..189, z 14..48."),
        ("LEG4", "mesa top", 190, -1, 31, "Mesa top y -5..+2; east face drops ~21 blocks into the finish canyon."),
        ("LEG5", "finish canyon floor", 210, -11, 31, "Open canyon; floor rises from -21 at x 195 to -5 at x 230."),
        ("LEG5", "finish trench", 235, -7, 31, "Soul sand/dirt at x 231..239; smooth-stone step x 233..235 at y -6..-4."),
        ("FINISH", "chicken ledge", 241, -2, 31, "1-wide red-terracotta ledge; 27 chickens at y=-1, z 11..52; chain/iron-bar fence x=240."),
        ("FINISH", "MCC wall", 244, 1, 31, "Gray-terracotta wall x 242..247, y -10..+2; red/white MCC sign x 241..242, y 4..12, z 22..40."),
    ]
    with open(os.path.join(DATA, "route_waypoints.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["leg", "what", "x", "y", "z", "notes"])
        for r in rows:
            w.writerow(r)
    print("route_waypoints.csv", len(rows))


def write_hashes():
    rows = []
    for rel in ["level.dat", "datapacks/tgttos/pack.mcmeta",
                "datapacks/tgttos/data/minecraft/tags/functions/load.json",
                "datapacks/tgttos/data/minecraft/tags/functions/tick.json",
                "datapacks/tgttos/data/tgttos/functions/load.mcfunction",
                "datapacks/tgttos/data/tgttos/functions/loop.mcfunction",
                "data/scoreboard.dat", "icon.png"]:
        p = os.path.join(ROOT, rel.replace("/", os.sep))
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()
        rows.append((rel, os.path.getsize(p), h))
    with open(os.path.join(DATA, "source_hashes.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["file", "bytes", "sha256"])
        for r in rows:
            w.writerow(r)
    print("source_hashes.csv", len(rows))


def main():
    os.makedirs(DATA, exist_ok=True)
    blocks, ents, tes, pack = load()
    write_blocks(blocks)
    write_entities(ents)
    write_signs(tes)
    write_command_blocks(tes)
    write_gamerules()
    write_objectives()
    write_can_place(pack)
    write_cactus()
    write_waypoints()
    write_hashes()


if __name__ == "__main__":
    main()
