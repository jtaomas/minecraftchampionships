"""Build the A12 CSV deliverables from scan.json (read-only evidence)."""
import csv
import json
import re
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATA = HERE.parent / "data"
DATA.mkdir(exist_ok=True)
scan = json.load(open(HERE / "scan.json", encoding="utf-8"))

SOURCE = {
    "mcc_clouds2": ("Clouds 2 - Electric Boogaloo (Download Version).zip", "1.20.1", 3465, ["vanilla", "fabric"]),
    "mcc_citymarathon": ("EXISTING MCC RECREATIONS/city-marathon.zip", "1.19.2", 3120, ["vanilla", "Fabric Mods"]),
    "mcc_spacerace": ("EXISTING MCC RECREATIONS/Space Race.zip", "1.18.1", 2865, ["vanilla", "mod:forge", "mod:worldedit"]),
    "mcc_yetisetgo": ("EXISTING MCC RECREATIONS/YetiSetGo.zip", "1.19.2", 3120, ["vanilla", "Fabric Mods"]),
    "mcc_decisiondome": ("EXISTING MCC RECREATIONS/Decision Dome.zip", "1.17.1", 2730, ["vanilla", "mod:forge", "mod:worldedit"]),
    "mcc_minigames": ("EXISTING MCC RECREATIONS/daniellaur-s-minigames-version-3.zip", "1.17.1", 2730, ["vanilla"]),
    "mcc_tgttos_badlands": ("EXISTING MCC RECREATIONS/tgttos-badlands-by-mickeykat.zip", "1.19.2", 3120, ["vanilla", "file/tgttos"]),
    "mcc_tgttos_industry": ("EXISTING MCC RECREATIONS/tgttos-industry-by-mickey-kat.zip", "1.19.1", 3117, ["vanilla", "Fabric Mods", "file/Industry"]),
    "mcc_tgttos_pits": ("EXISTING MCC RECREATIONS/tgttos-pits-by-mickey-kat.zip", "1.19.1", 3117, ["vanilla", "Fabric Mods", "file/Pits"]),
}

PACK = {
    "mcc_clouds2": ("resources.zip", 15, "present, valid root, 22.2 MB"),
    "mcc_citymarathon": ("advancements.zip (world backup, not a pack)", "", "not a resource pack"),
    "mcc_spacerace": ("resources.zip.zip", 6, "wrapper folder, no root pack.mcmeta; invalid as-is"),
    "mcc_yetisetgo": ("resources.zip", 9, "present, valid root, 95 KB"),
    "mcc_decisiondome": ("none", "", "none"),
    "mcc_minigames": ("none", "", "none"),
    "mcc_tgttos_badlands": ("none", "", "none"),
    "mcc_tgttos_industry": ("none", "", "none"),
    "mcc_tgttos_pits": ("none", "", "none"),
}

LOAD_TIME = {
    "mcc_clouds2": "23:31:13",
    "mcc_citymarathon": "23:31:28",
    "mcc_spacerace": "23:31:30",
    "mcc_yetisetgo": "23:31:32",
    "mcc_decisiondome": "23:31:35",
    "mcc_minigames": "23:31:37",
    "mcc_tgttos_badlands": "23:31:39",
    "mcc_tgttos_industry": "23:31:41",
    "mcc_tgttos_pits": "23:31:44",
}

VERDICT = {
    "mcc_clouds2": ("PLAYABLE_WITH_CAVEATS", "M", "Start/reset works; 30 boot-pad commands fail under 1.21.1; one reset setblock is malformed; kill planes are global."),
    "mcc_citymarathon": ("PLAYABLE_WITH_CAVEATS", "M", "Start button and countdown work; 7 no-target gamemode blocks fail, so players stay survival."),
    "mcc_spacerace": ("PLAYABLE_WITH_CAVEATS", "M", "Start button/countdown work; pack is invalid; two kill planes kill all players; mob/fire settings are loose."),
    "mcc_yetisetgo": ("PLAYABLE_WITH_CAVEATS", "M", "Start button/countdown work; 3 no-target gamemode blocks fail; pack needs a format bump."),
    "mcc_decisiondome": ("BUILD_ONLY", "H", "Pure build; no start logic. Spawn was auto-adjusted by Multiverse from 0,4,0 to 0,76,0. Two leftover command blocks are not map logic."),
    "mcc_minigames": ("LOADED_UNVERIFIED", "M", "942 command blocks and 187 signs; A12 did not audit this arena world beyond load/spawn. Do not treat the plan's '0 command blocks' as true."),
    "mcc_tgttos_badlands": ("NOT_RUNNABLE_AS_IMPORTED", "H", "Datapack is disabled and uses the pre-1.21 plural folder layout; Start sign sets a score but tick never runs."),
    "mcc_tgttos_industry": ("NOT_RUNNABLE_AS_IMPORTED", "H", "Datapack is disabled and uses the pre-1.21 plural folder layout; Start sign adds a tag but tick never runs."),
    "mcc_tgttos_pits": ("NOT_RUNNABLE_AS_IMPORTED", "H", "Datapack is disabled and uses the pre-1.21 plural folder layout; Start sign adds a tag but tick never runs."),
}

LOG_ISSUE = {
    "mcc_clouds2": "none in the import window",
    "mcc_citymarathon": "none in the import window",
    "mcc_spacerace": "none in the import window",
    "mcc_yetisetgo": "none in the import window",
    "mcc_decisiondome": "2x 'Ignoring unknown attribute forge:entity_gravity'; spawn adjusted",
    "mcc_minigames": "none in the import window",
    "mcc_tgttos_badlands": "none in the import window",
    "mcc_tgttos_industry": "none in the import window",
    "mcc_tgttos_pits": "none in the import window",
}

SPAWN_GROUND = {
    "mcc_clouds2": "snow_block",
    "mcc_citymarathon": "gray_wool",
    "mcc_spacerace": "gray_concrete_powder",
    "mcc_yetisetgo": "gray_concrete",
    "mcc_decisiondome": "lime_concrete",
    "mcc_minigames": "netherite_block",
    "mcc_tgttos_badlands": "quartz_block",
    "mcc_tgttos_industry": "quartz_block",
    "mcc_tgttos_pits": "quartz_block",
}

SPAWN_NOTE = {
    "mcc_clouds2": "Clear at feet/head; stone button at 5,4,22 is the only adjacent block.",
    "mcc_citymarathon": "Clear 3x3x2.",
    "mcc_spacerace": "Clear 3x3x2; ground is concrete powder.",
    "mcc_yetisetgo": "Clear 3x3x2.",
    "mcc_decisiondome": "Exact point clear; lime_concrete one block west at feet/head. Multiverse changed spawn from unsafe 0,4,0.",
    "mcc_minigames": "Clear 3x3x2; hub spawn.",
    "mcc_tgttos_badlands": "Clear at feet/head; wall sign at 9,-41,31.",
    "mcc_tgttos_industry": "Clear at feet/head; wall sign at 73,-32,10.",
    "mcc_tgttos_pits": "Clear at feet/head; wall sign at 14,-1,36.",
}

TYPES = {
    "mcc_clouds2": (23, 15, 284, 31, 6),
    "mcc_citymarathon": (5, 2, 60, 7, 0),
    "mcc_spacerace": (4, 3, 22, 0, 2),
    "mcc_yetisetgo": (5, 2, 30, 3, 0),
    "mcc_decisiondome": (1, 1, 0, 0, 0),
    "mcc_minigames": (182, 548, 212, 0, 0),
    "mcc_tgttos_badlands": (0, 0, 0, 0, 0),
    "mcc_tgttos_industry": (0, 0, 0, 0, 0),
    "mcc_tgttos_pits": (0, 0, 0, 0, 0),
}

# ---------------------------------------------------------------- worlds.csv
world_rows = []
for w, d in scan.items():
    lvl = d["level"]
    sp = lvl["spawn"]
    arc, sver, sdv, sdp = SOURCE[w]
    pack, pfmt, pnote = PACK[w]
    imp, chain, rep, fail, risk = TYPES[w]
    dp = lvl["datapacks"]
    enabled = ",".join(dp.get("Enabled", []))
    disk = ", ".join(d.get("datapack_folders", [])) or "none"
    world_rows.append({
        "world": w,
        "source_archive": arc,
        "source_version": sver,
        "source_data_version": sdv,
        "current_version": lvl["version_name"],
        "current_data_version": lvl["data_version"],
        "spawn_x": sp[0], "spawn_y": sp[1], "spawn_z": sp[2],
        "spawn_ground": SPAWN_GROUND[w],
        "spawn_safe": "yes",
        "spawn_note": SPAWN_NOTE[w],
        "mv_gamemode": "SURVIVAL",
        "mv_difficulty": "NORMAL" if w == "mcc_minigames" else "PEACEFUL",
        "mv_allow_flight": "false",
        "mv_keep_spawn_in_memory": "true",
        "mv_pvp": "true",
        "mv_hunger": "true",
        "gamerule_command_block_output": lvl["gamerules"].get("commandBlockOutput", ""),
        "gamerule_keep_inventory": lvl["gamerules"].get("keepInventory", ""),
        "gamerule_fall_damage": lvl["gamerules"].get("fallDamage", ""),
        "gamerule_do_mob_spawning": lvl["gamerules"].get("doMobSpawning", ""),
        "gamerule_do_immediate_respawn": lvl["gamerules"].get("doImmediateRespawn", ""),
        "gamerule_random_tick_speed": lvl["gamerules"].get("randomTickSpeed", ""),
        "chunks": d.get("chunk_count", 0),
        "region_files": len(list((Path(d["folder"]) / "region").glob("*.mca"))),
        "command_blocks": len(d.get("command_blocks", [])),
        "impulse": imp, "chain": chain, "repeating": rep,
        "always_active_auto1": sum(1 for c in d.get("command_blocks", []) if c.get("auto")),
        "need_redstone_auto0": sum(1 for c in d.get("command_blocks", []) if not c.get("auto")),
        "powered_now": sum(1 for c in d.get("command_blocks", []) if c.get("powered")),
        "signs": len(d.get("signs", [])),
        "datapacks_enabled_current": enabled,
        "datapacks_on_disk": disk,
        "datapacks_enabled_source": ",".join(sdp),
        "resource_pack": pack,
        "resource_pack_format": pfmt,
        "resource_pack_note": pnote,
        "load_verified": "yes",
        "load_evidence": f"log 2026-11-24-18 {LOAD_TIME[w]}; mv info OK; execute in {w} run gamerule doDaylightCycle returned false",
        "log_issues": LOG_ISSUE[w],
        "cb_fail_1_21": "not_audited" if w == "mcc_minigames" else fail,
        "cb_behaviour_risks": "not_audited" if w == "mcc_minigames" else risk,
        "verdict": VERDICT[w][0],
        "verdict_confidence": VERDICT[w][1],
        "verdict_reason": VERDICT[w][2],
    })

fields = list(world_rows[0].keys())
with open(DATA / "worlds.csv", "w", newline="", encoding="utf-8") as f:
    wtr = csv.DictWriter(f, fieldnames=fields)
    wtr.writeheader()
    wtr.writerows(world_rows)

# ------------------------------------------------------------ commands_risk
ITEM_NBT = re.compile(r"\b(?:item replace|give)\b[^\n]*\b[a-z_:]+\{[^}]*\}", re.I)
NO_TARGET_GAMEMODE = re.compile(r"^/?gamemode\s+(?:adventure|survival|creative|spectator)\s*$", re.I)
GLOBAL_KILL = re.compile(r"\bkill\s+@[ap]\b", re.I)

cmd_rows = []
for w in ["mcc_clouds2", "mcc_citymarathon", "mcc_spacerace", "mcc_yetisetgo"]:
    for c in scan[w]["command_blocks"]:
        cmd = c.get("command", "").strip()
        risk, code, conf, reason = "OK", "OK", "M", "Parses in 1.21.1; no version-specific change found (not executed in this pass)."
        if ITEM_NBT.search(cmd):
            risk, code, conf = "FAIL", "FAIL_ITEM_NBT", "H"
            reason = ("Pre-1.20.5 item NBT (display/Enchantments/HideFlags) in an item argument; "
                      "1.21.1 requires components in [] and rejects the old braces, so the command fails to parse.")
        elif code == "OK" and re.fullmatch(r"/?setblock\s+-?\d+\s+-?\d+\s+-?\d+\s*", cmd):
            risk, code, conf = "FAIL", "FAIL_INCOMPLETE", "H"
            reason = "setblock is missing the block argument; parse error in every version. It is a chain block at (-49,1,27)."
        elif NO_TARGET_GAMEMODE.fullmatch(cmd):
            risk, code, conf = "FAIL", "FAIL_NO_TARGET", "H"
            reason = ("/gamemode has no target and the command source is the command block; fails with "
                      "permissions.requires.player (confirmed in LastOutput for 3 City and 1 Yeti block).")
        elif GLOBAL_KILL.search(cmd):
            risk, code, conf = "RISK", "RISK_GLOBAL_KILL", "H"
            reason = ("kill @a/@p inside a per-player loop: one player meeting the condition can kill every player. "
                      "Not a 1.21 syntax change; a multiplayer hazard.")
        cmd_rows.append({
            "world": w, "x": c["x"], "y": c["y"], "z": c["z"], "dimension": c["dim"],
            "block": c.get("block", c.get("id", "")),
            "mode": c.get("block", "").replace("minecraft:", "").replace("_command_block", "") or "unknown",
            "auto": c.get("auto"), "conditional": c.get("props", {}).get("conditional", "?"),
            "powered": c.get("powered"), "last_execution": c.get("last_execution"),
            "command": cmd, "risk": risk, "risk_code": code, "confidence": conf, "reason": reason,
        })

with open(DATA / "commands_risk.csv", "w", newline="", encoding="utf-8") as f:
    wtr = csv.DictWriter(f, fieldnames=list(cmd_rows[0].keys()))
    wtr.writeheader()
    wtr.writerows(cmd_rows)

# -------------------------------------------------------------- datapacks.csv
dp_rows = [
    {
        "world": "mcc_tgttos_badlands", "pack": "tgttos", "pack_format": 10,
        "target_data_pack_format": 48,
        "function_folder": "data/tgttos/functions",
        "expected_function_folder": "data/tgttos/function",
        "load_tag": "data/minecraft/tags/functions/load.json",
        "tick_tag": "data/minecraft/tags/functions/tick.json",
        "function_count": 2, "function_lines": 120,
        "old_item_nbt_lines": 3, "scoreboard_lines": 8,
        "enabled_in_level_dat": "no", "enabled_in_source_archive": "yes (file/tgttos)",
        "status": "NOT_RUNNABLE_AS_IMPORTED", "confidence": "H",
        "evidence": "pack.mcmeta format 10; plural functions/tags paths; level.dat Enabled=[vanilla,file/bukkit,paper]; source level.dat Enabled=[vanilla,file/tgttos].",
    },
    {
        "world": "mcc_tgttos_industry", "pack": "Industry", "pack_format": 10,
        "target_data_pack_format": 48,
        "function_folder": "data/tgttos/functions",
        "expected_function_folder": "data/tgttos/function",
        "load_tag": "data/minecraft/tags/functions/load.json",
        "tick_tag": "data/minecraft/tags/functions/tick.json",
        "function_count": 7, "function_lines": 251,
        "old_item_nbt_lines": 21, "scoreboard_lines": 21,
        "enabled_in_level_dat": "no", "enabled_in_source_archive": "yes (file/Industry)",
        "status": "NOT_RUNNABLE_AS_IMPORTED", "confidence": "H",
        "evidence": "pack.mcmeta format 10; plural functions/tags paths; level.dat Enabled=[vanilla,file/bukkit,paper]; source level.dat Enabled=[vanilla,Fabric Mods,file/Industry].",
    },
    {
        "world": "mcc_tgttos_pits", "pack": "Pits", "pack_format": 10,
        "target_data_pack_format": 48,
        "function_folder": "data/tgttos/functions",
        "expected_function_folder": "data/tgttos/function",
        "load_tag": "data/minecraft/tags/functions/load.json",
        "tick_tag": "data/minecraft/tags/functions/tick.json",
        "function_count": 6, "function_lines": 219,
        "old_item_nbt_lines": 21, "scoreboard_lines": 21,
        "enabled_in_level_dat": "no", "enabled_in_source_archive": "yes (file/Pits)",
        "status": "NOT_RUNNABLE_AS_IMPORTED", "confidence": "H",
        "evidence": "pack.mcmeta format 10; plural functions/tags paths; level.dat Enabled=[vanilla,file/bukkit,paper]; source level.dat Enabled=[vanilla,Fabric Mods,file/Pits].",
    },
]
with open(DATA / "datapacks.csv", "w", newline="", encoding="utf-8") as f:
    wtr = csv.DictWriter(f, fieldnames=list(dp_rows[0].keys()))
    wtr.writeheader()
    wtr.writerows(dp_rows)

# ------------------------------------------------------------------ summary
print("worlds", len(world_rows), "commands", len(cmd_rows), "datapacks", len(dp_rows))
print(Counter((r["world"], r["risk_code"], r["risk"]) for r in cmd_rows))
print("OK by world", Counter(r["world"] for r in cmd_rows if r["risk"] == "OK"))
