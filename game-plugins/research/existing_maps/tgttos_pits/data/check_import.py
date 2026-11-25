#!/usr/bin/env python3
"""Read-only comparison: source Pits world vs imported review-server world.

Writes data/import_state.json. Does not modify either world, does not use RCON.
"""
import hashlib
import json
import os

import nbtlib

SRC = r"C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat"
REVIEW_ROOT = r"C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review"
IMP = os.path.join(REVIEW_ROOT, "mcc_tgttos_pits")
HERE = os.path.dirname(os.path.abspath(__file__))
PACK_REL = os.path.join("datapacks", "Pits")


def level_info(path):
    d = nbtlib.load(path)["Data"]
    dp = d.get("DataPacks", {})
    return {
        "level_name": str(d.get("LevelName", "")),
        "data_version": int(d.get("DataVersion", 0)),
        "spawn": [int(d.get("SpawnX", 0)), int(d.get("SpawnY", 0)), int(d.get("SpawnZ", 0))],
        "spawn_angle": float(d.get("SpawnAngle", 0)),
        "game_type": int(d.get("GameType", 0)),
        "difficulty": int(d.get("Difficulty", 0)),
        "allow_commands": int(d.get("allowCommands", 0)),
        "datapacks_enabled": [str(x) for x in dp.get("Enabled", [])],
        "datapacks_disabled": [str(x) for x in dp.get("Disabled", [])],
        "gamerules": {k: str(v) for k, v in d.get("GameRules", {}).items()
                      if k in ("keepInventory", "fallDamage", "doDaylightCycle", "doMobSpawning",
                               "commandBlockOutput", "doImmediateRespawn", "randomTickSpeed",
                               "sendCommandFeedback", "freezeDamage", "disableRaids")},
    }


def pack_hashes(world):
    root = os.path.join(world, PACK_REL)
    out = {}
    for base, dirs, files in os.walk(root):
        for fn in files:
            p = os.path.join(base, fn)
            rel = os.path.relpath(p, root).replace("\\", "/")
            data = open(p, "rb").read()
            out[rel] = {"bytes": len(data), "sha1": hashlib.sha1(data).hexdigest()}
    return out


def main():
    src_info = level_info(os.path.join(SRC, "level.dat"))
    imp_info = level_info(os.path.join(IMP, "level.dat"))
    src_pack = pack_hashes(SRC)
    imp_pack = pack_hashes(IMP)
    same = src_pack.keys() == imp_pack.keys() and all(src_pack[k] == imp_pack[k] for k in src_pack)
    out = {
        "source_world": SRC,
        "imported_world": IMP,
        "review_server_root": REVIEW_ROOT,
        "read_only": True,
        "source": src_info,
        "imported": imp_info,
        "pack_path_relative": PACK_REL,
        "pack_files_source": src_pack,
        "pack_files_imported": imp_pack,
        "pack_files_byte_identical": same,
        "notes": [
            "Source has file/Pits enabled; imported world does not.",
            "Imported level.dat is 1.21.1 DataVersion 3955, GameType 0 (survival).",
            "Pack files are copied unchanged: old functions/tags paths and pack_format 10.",
        ],
    }
    with open(os.path.join(HERE, "import_state.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print(json.dumps({k: out[k] for k in ("source", "imported", "pack_files_byte_identical")}, indent=2))


if __name__ == "__main__":
    main()
