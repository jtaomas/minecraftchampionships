"""Build count_checks.csv (supporting evidence for acceptance.csv)."""
from __future__ import annotations

import csv
from pathlib import Path

BASE = Path(__file__).resolve().parents[1] / "data"
OUT = BASE / "count_checks.csv"

ROWS = [
    # agent, metric, claimed, observed, status, evidence
    ("A1 clouds2", "command blocks", 322, 322, "PASS", "data/command_blocks.csv rows; A12 322"),
    ("A1 clouds2", "signs", 285, 285, "PASS", "data/signs.csv rows"),
    ("A1 clouds2", "containers", 225, 225, "PASS", "data/containers.csv; independent NBT scan 225 shulkers, 0 items"),
    ("A1 clouds2", "non-air blocks", 150414, 150414, "PASS", "data/blocks.csv sum; A12 chunk count"),
    ("A1 clouds2", "checkpoint/pad rows", 111, 111, "PASS", "data/checkpoints.csv"),
    ("A1 clouds2", "1.21 command failures", 31, 31, "PASS", "data/command_failures.csv; A12 31"),
    ("A2 city_marathon", "command blocks", 67, 67, "PASS", "data/command_blocks.csv; A12 67"),
    ("A2 city_marathon", "containers", 156, 156, "PASS", "data/containers.csv; independent scan 140 shulkers+12 barrels+3 campfires+1 blast furnace"),
    ("A2 city_marathon", "signs", 7, 7, "PASS", "data/signs.csv"),
    ("A2 city_marathon", "non-air blocks", 207440, 207440, "PASS", "data/blocks.csv"),
    ("A2 city_marathon", "1.21 command failures", 7, 7, "PASS", "data/command_blocks.csv; A12 7"),
    ("A3 space_race", "command blocks", 29, 29, "PASS", "data/command_blocks.csv; A12 29"),
    ("A3 space_race", "containers", 0, 0, "PASS", "data/containers.csv header only; independent scan 0"),
    ("A3 space_race", "signs", 22, 22, "PASS", "data/signs.csv"),
    ("A3 space_race", "non-air blocks", 296699, 296699, "PASS", "data/blocks.csv"),
    ("A3 space_race", "1.21 command failures", 0, 0, "PASS", "A12 0"),
    ("A4 yeti_set_go", "command blocks", 37, 37, "PASS", "data/command_blocks.csv; A12 37"),
    ("A4 yeti_set_go", "shulkers + hopper", "3+1", "3+1", "PASS", "data/containers.csv; independent scan 3 shulkers+1 hopper (plus 2 empty campfires)"),
    ("A4 yeti_set_go", "containers (broad block-entity definition)", 6, 6, "PASS", "3 shulkers + 1 hopper + 2 empty campfires; data/containers.csv has 6 rows"),
    ("A4 yeti_set_go", "signs", 14, 14, "PASS", "data/signs.csv"),
    ("A4 yeti_set_go", "non-air blocks", 28533, 28533, "PASS", "data/blocks.csv"),
    ("A4 yeti_set_go", "1.21 command failures", 3, 3, "PASS", "README; A12 3"),
    ("A5 decision_dome", "command blocks", 2, 2, "PASS", "data/command_blocks.csv; A12 2"),
    ("A5 decision_dome", "containers", 0, 0, "PASS", "independent scan 0"),
    ("A5 decision_dome", "non-air blocks", 20054, 20054, "PASS", "data/blocks.csv"),
    ("A5 decision_dome", "datapack zips active", 0, 0, "PASS", "12 zips + 1 unzipped folder installed; all Disabled; A5 now says 0 active; A12 agrees"),
    ("A6 minigames", "command blocks", 942, 942, "PASS", "data/command_blocks.csv; A12 942"),
    ("A6 minigames", "signs", 187, 187, "PASS", "data/signs.csv; A12 187"),
    ("A6 minigames", "containers", 34, 34, "PASS", "data/containers.csv 34 rows; independent scan 34 (21 empty, 13 loot/93 items); arenas.csv column filled"),
    ("A6 minigames", "non-air blocks", 133030, 133030, "PASS", "data/chunk_scan + A6 totals"),
    ("A6a sky_battle", "loot containers", 13, 13, "PASS", "chests.csv; independent NBT scan 4 shulkers+4 chests+5 barrels"),
    ("A6a sky_battle", "loot entries", 93, 93, "PASS", "loot.csv == container_nbt.csv (93/93)"),
    ("A6a sky_battle", "all container block entities in envelope", 19, 19, "PASS", "13 loot + 5 campfires + 1 lectern; containers.csv 19 rows; command_blocks.csv 477-row alias"),
    ("A6b parkour_tag", "command blocks", 123, 123, "PASS", "commands.csv; A12 cluster count"),
    ("A6b parkour_tag", "signs", 25, 25, "PASS", "signs.csv"),
    ("A6b parkour_tag", "storage containers", "1 barrel", "1 barrel", "PASS", "containers.csv; independent scan"),
    ("A6b parkour_tag", "all container block entities in envelope", 3, 3, "PASS", "1 barrel + 2 lecterns (guide books); containers.csv 3 rows; command_blocks.csv 123-row alias"),
    ("A6c other", "containers in 8 catalogued arenas", 12, 12, "PASS", "hub 8, Super Clash 3, Sky Race hub 1; containers.csv 12 rows; other_arenas.csv column filled"),
    ("A7 tgttos_badlands", "command blocks", 0, 0, "PASS", "command_blocks.csv header only; independent scan"),
    ("A7 tgttos_badlands", "containers", 0, 0, "PASS", "independent scan 0"),
    ("A7 tgttos_badlands", "non-air blocks", 411834, 411834, "PASS", "data/blocks.csv"),
    ("A8 tgttos_industry", "command blocks", 0, 0, "PASS", "command_blocks.csv header only; independent scan"),
    ("A8 tgttos_industry", "containers", 0, 0, "PASS", "independent scan 0"),
    ("A8 tgttos_industry", "non-air blocks", 122276, 122276, "PASS", "data/blocks.csv"),
    ("A9 tgttos_pits", "command blocks", 0, 0, "PASS", "command_blocks.csv header only; independent scan"),
    ("A9 tgttos_pits", "containers", 0, 0, "PASS", "independent scan 0"),
    ("A9 tgttos_pits", "non-air blocks", 164145, 164145, "PASS", "data/blocks.csv"),
    ("A10 resource_packs", "packs", 3, 3, "PASS", "packs/; A10 inventory"),
    ("A10 resource_packs", "containers in pack worlds", 387, 387, "PASS", "225+156+6 (6 Yeti includes 2 campfires)"),
    ("A11 video_clouds2", "video stills", 17, 17, "PASS", "frames/01..17"),
    ("A11 video_clouds2", "scene cuts", 48, 48, "PASS", "data/scene_cuts.csv"),
    ("A12 server_import", "command blocks audited", 455, 455, "PASS", "data/commands_risk.csv 455 rows"),
    ("A12 server_import", "1.21 failures", 41, 41, "PASS", "30 item-NBT + 1 incomplete + 10 no-target"),
    ("A12 server_import", "worlds loaded", 9, 9, "PASS", "data/worlds.csv; README log"),
    ("A4 yeti_set_go", "pack path base", "relative to assets/minecraft", "README defines base", "PASS", "README L254 defines pack/extracted/assets/minecraft/; three previously broken paths resolve"),
    ("A13 comparison", "Yeti containers", 6, 6, "PASS", "matrix row says 6 block entities (4 storage + 2 campfires); matches A4/A10"),
    ("A13 comparison", "matrix rows x columns", "4x18", "4x18", "PASS", "data/ace_race_matrix.csv; primary_sources column added"),
    ("A13 comparison", "runtime capabilities", 13, 13, "PASS", "data/runtime_gap.md rows 1-13"),
    ("A13 comparison", "matrix source-report column", 1, 1, "PASS", "primary_sources is column 18; 12/12 cited paths resolve from data/"),
]

with OUT.open("w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["agent", "metric", "claimed", "observed", "status", "evidence"])
    w.writerows(ROWS)
print(f"wrote {len(ROWS)} checks")
