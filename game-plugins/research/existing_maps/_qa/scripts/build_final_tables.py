"""Final post-fix acceptance and claim tables."""
from __future__ import annotations

import csv
from pathlib import Path

BASE = Path(__file__).resolve().parents[1] / "data"

ACCEPT = [
    ["A1", "clouds2", "README, REFERENCE_IMAGES, frames (2 JPG), data, renders (51), STATUS", "322 commands / 285 signs / 225 empty shulkers / 150,414 blocks / 111 checkpoint-pad rows; 31 failures = A12", "41 cited, 0 missing; 2 frames but 51 renders; 4 opened", "H/M/L used; route and containers checked", "PASS (P2: thin frames/)"],
    ["A2", "city_marathon", "README, REFERENCE_IMAGES, frames (10), data, renders (33), STATUS", "67 commands / 156 containers / 7 signs / 39 item frames / 207,440 blocks; 7 failures = A12", "40 cited, 0 missing; station image now points to station_interior_iso; 3 opened", "H/M/L used; scoreboard evidence now cites data/scoreboard.dat", "PASS"],
    ["A3", "space_race", "README, REFERENCE_IMAGES, frames (9), data, renders (36), STATUS", "29 commands / 0 containers / 22 signs / 296,699 blocks; 0 syntax failures", "37 cited, 0 missing; 3 opened", "H/M/L used; gates/rings checked", "PASS"],
    ["A4", "yeti_set_go", "README, REFERENCE_IMAGES, frames (20), data, renders (43), pack/, STATUS", "37 commands / 14 signs / 6 container block entities (3 shulkers + hopper + 2 campfires) / 28,533 blocks; 3 failures = A12", "36 cited, 0 missing after documented pack base; 2 opened", "H/M/L used; container definition aligned with A10/A13", "PASS"],
    ["A5", "decision_dome_recreation", "README, REFERENCE_IMAGES, frames (14 + heads), data, renders (43), STATUS", "2 commands / 45 block entities / 20,054 blocks / 85 types; 0 containers; 12 zips installed, 0 active", "29 cited, 0 missing; 5 opened", "H/M/L used; source spawn 0,4,0 vs live 0,76,0 explicit", "PASS"],
    ["A6", "minigames", "README, REFERENCE_IMAGES, frames (12), data (incl. 34-row containers.csv), renders (45), subfolders, STATUS", "942 commands / 187 signs / 133,030 blocks / 34 container BEs (21 empty, 13 loot/93 items); 10 arenas", "14 cited (drafts excluded), 0 missing; atlas duplicate accounting noted", "H/M/L used; 34/21/13 statement matches source scan", "PASS WITH FIXES (atlas panel 07 still stale; QA provenance)"],
    ["A6a", "minigames/sky_battle", "README, REFERENCE_IMAGES, frames (17), data, renders (30), STATUS; 477-row command_blocks.csv alias", "477 commands / 19 container BEs = 13 loot (93 items) + 5 campfires + 1 lectern; 47,874 blocks", "17 cited, 0 missing; 3 opened; alias hash identical", "H/M/L used; 93/93 loot match; stale A6 notes withdrawn", "PASS WITH FIXES (container source cites _qa scan)"],
    ["A6b", "minigames/parkour_tag", "README, REFERENCE_IMAGES, frames (16), data, renders (25), STATUS; 123-row command_blocks.csv alias", "123 commands / 25 signs / 3 container BEs = 1 barrel + 2 lecterns (guide books); 8,375 blocks", "29 cited, 0 missing; 2 opened; alias hash identical", "H/M/L used; guide-book text checked; stale A6 note corrected", "PASS WITH FIXES (container source cites _qa scan)"],
    ["A6c", "minigames/other", "README, REFERENCE_IMAGES, frames (16), data (12-row containers.csv, palette.md, route.md), renders (16), STATUS", "8 arenas / 12 containers (hub 8, Super Clash 3, Sky Race hub 1); 34/21/13 global statement", "18 cited, 0 missing; 6 opened; Orange vs Red images compared", "H/M/L used; phantom rows removed; palette/route added", "PASS WITH FIXES (container source cites _qa scan; atlas note)"],
    ["A7", "tgttos_badlands", "README, REFERENCE_IMAGES, frames (10), data, renders (33), STATUS", "0 commands / 0 containers / 1 sign / 411,834 blocks; 27 finish chickens", "43 cited, 0 missing; 3 opened", "H/M/L used; route/finish checked", "PASS"],
    ["A8", "tgttos_industry", "README, REFERENCE_IMAGES, frames (4), data, renders (41), STATUS", "0 commands / 0 containers / 1 sign / 122,276 blocks; 21 item-NBT lines", "24 cited, 0 missing; 3 opened", "H/M/L used; reset coverage checked", "PASS WITH FIXES (frames/ only 4; renders meet image rule)"],
    ["A9", "tgttos_pits", "README, REFERENCE_IMAGES, frames (10), data, renders (22), STATUS", "0 commands / 0 containers / 1 sign / 164,145 blocks; 20-chicken finish", "32 cited, 0 missing; 2 opened", "H/M/L used; route/finish checked", "PASS"],
    ["A10", "resource_packs", "README, REFERENCE_IMAGES, frames (5), textures (8), data, packs/, STATUS; no renders/", "3 packs / 71 textures / 455 commands / 387 containers (225+156+6); 0 active", "18 cited, 0 missing; 3 opened", "H/M/L used; hashes/counts checked", "PASS"],
    ["A11", "video_clouds2", "README, REFERENCE_IMAGES, frames (21), data, renders (8), STATUS", "17 video stills / 48 cuts / 39 timeline rows; exact HUD inventory", "27 cited, 0 missing; 3 opened; countdown/HUD confirmed", "H/M/L used; frames checked", "PASS"],
    ["A12", "server_import", "README, STATUS, data; REFERENCE_IMAGES/frames/renders n/a (non-map)", "455 commands / 41 failures (30+1+10) / 9 worlds / 3 packs", "no images (file/log evidence by design)", "H/M/L used; wrapper-folder wording fixed", "PASS"],
    ["A13", "comparison", "README, STATUS, data (5), renders (1); REFERENCE_IMAGES/frames n/a (synthesis)", "4x18 matrix / 13 runtime capabilities / 1 composite sheet; primary_sources added", "12 cited; 2 missing-prefix paths (files exist) + 1 fix-log quote; 8 source images opened", "H/M/L used; Space @p, Yeti 6, Orange, creeper, §4.11, sim scope fixed", "PASS WITH FIXES (2 broken relative image citations; historical table wording)"],
]

CLAIMS = [
    ["Clouds 2 322 commands, 285 signs, 225 empty shulkers, 31 failures", "clouds2/README.md; data/command_blocks.csv; data/command_failures.csv", "VERIFIED", "Rows and independent NBT scan match; A12 31"],
    ["City Marathon 67 commands, 156 containers, 7 failures, no scoreboard", "city_marathon/README.md L15/L136; data/containers.csv", "VERIFIED", "67/156 rows all empty; no data/scoreboard.dat; A12 7; station image now exists"],
    ["Space Race 29 commands, 0 containers, 0 syntax failures", "space_race/README.md; data/command_blocks.csv", "VERIFIED", "29 rows; independent scan 0; A12 0"],
    ["Yeti 37 commands, 14 signs, 6 container block entities", "yeti_set_go/README.md L8/L206; data/containers.csv", "VERIFIED", "3 shulkers + hopper + 2 empty campfires; 4 storage containers; A12 3 failures"],
    ["Decision Dome 2 commands, 45 block entities, 0 active packs", "decision_dome_recreation/README.md L31/L46; STATUS.md L25", "VERIFIED", "12 zips + 1 folder all Disabled; source Enabled=[vanilla,mod:forge,mod:worldedit]; A12 agrees"],
    ["Decision Dome source spawn 0,4,0 vs live 0,76,0", "decision_dome_recreation/README.md L55/L297; A12 worlds.csv", "VERIFIED", "Source unsafe, live Multiverse safe; now stated explicitly"],
    ["Minigames 34 container block entities (21 empty, 13 loot/93 items)", "minigames/README.md L30/L65; minigames/data/containers.csv", "VERIFIED", "34 rows; independent source scan identical; per-arena column filled"],
    ["Sky Battle 19 container block entities = 13 loot/93 items + 5 campfires + 1 lectern", "minigames/sky_battle/README.md L19/L49; data/containers.csv", "VERIFIED", "19 rows; 93/93 loot entries match NBT; 477-row command alias hash-identical"],
    ["Parkour Tag 3 container block entities = 1 barrel + 2 lecterns (guide books)", "minigames/parkour_tag/README.md L36/L118; data/containers.csv", "VERIFIED", "3 rows; lecterns hold a written book, no Items; 123-row command alias hash-identical"],
    ["A6c owns 12 containers in its eight arenas", "minigames/other/README.md L31/L52; data/containers.csv", "VERIFIED", "Hub 8 + Super Clash 3 + Sky Race hub 1; other_arenas containers column filled"],
    ["A6c's correction table has no active phantom rows", "minigames/other/README.md L284", "VERIFIED", "Removed CMD/hub/Super Clash/no-container rows; current A6 text is quoted only as historical context"],
    ["A6 top-level Parkour gallery/waiting-lobby distinction", "minigames/README.md L108-115/L118", "VERIFIED", "Gallery y27-39; waiting lobby y0-9 at 1064/1058,1,985; matches A6b"],
    ["A6 top-level Parkour countdown coordinates", "minigames/README.md L127", "VERIFIED", "x1044/1048/1052, y5-8, z995; matches A6b/data/commands.csv"],
    ["A6 top-level Orange layout and sign wording", "minigames/README.md L137; REFERENCE_IMAGES.md L18", "VERIFIED", "Orange y13-54/24 cobwebs/no chains; signs match loadout, not leftover"],
    ["A6 atlas panel 07 label", "minigames/renders/arena_atlas.png; minigames/REFERENCE_IMAGES.md L23", "CONTRADICTED (stale label)", "Opened panel 07: still says 'same layout as Red, orange palette'; A6c REFERENCE L28 acknowledges it; regenerate/annotate the atlas"],
    ["A6 duplicate-image accounting", "minigames/REFERENCE_IMAGES.md L7; STATUS.md L113", "VERIFIED", "12 frames are byte-identical copies of 12 renders; 14 distinct views with atlas/world_map"],
    ["A6a 477-row command_blocks.csv alias", "minigames/sky_battle/data/command_blocks.csv", "VERIFIED", "SHA-256 identical to commands.csv; 477 rows"],
    ["A6b 123-row command_blocks.csv alias", "minigames/parkour_tag/data/command_blocks.csv", "VERIFIED", "SHA-256 identical to commands.csv; 123 rows"],
    ["A6a/A6b stale A6 correction rows withdrawn", "minigames/sky_battle/README.md L13/STATUS.md L28; minigames/parkour_tag/STATUS.md L22", "VERIFIED", "Old claims appear only in withdrawal notes; active text uses current 13+1 and no 161-fill claim"],
    ["A5 0 active VanillaTweaks packs", "decision_dome_recreation/README.md L31/L46/L299; STATUS.md L25", "VERIFIED", "12 zips + 1 folder installed, all Disabled; A12 L201 agrees"],
    ["A4 6 container block entities and pack path base", "yeti_set_go/README.md L8/L206/L254; data/containers.csv", "VERIFIED", "6 rows (4 storage + 2 cooking); paths documented relative to pack/extracted/assets/minecraft/"],
    ["A2 station image citation fixed", "city_marathon/REFERENCE_IMAGES.md L30", "VERIFIED", "Points to renders/station_interior_iso.png, which exists; station_iso.png no longer cited"],
    ["A2 scoreboard evidence cites data/scoreboard.dat", "city_marathon/README.md L15", "VERIFIED", "No data/scoreboard.dat exists; level.dat datapacks empty of map pack; conclusion correct"],
    ["A12 Space Race wrapper-folder wording", "server_import/README.md L46/L414/L559", "VERIFIED", "One zip with wrapper folder, not zip-inside-zip; A10 agrees"],
    ["A13 Space @p correction", "comparison/README.md L25-26/L100-104; data/ace_race_matrix.csv Space cell", "VERIFIED", "Clouds/City/Yeti @a; Space levitation/speed @p; elytra clear and kill planes @a"],
    ["A13 simulation-distance claim scoped to Clouds/Yeti", "comparison/README.md L118-121; data/runtime_gap.md L59/L75", "VERIFIED", "Space command room under start + keep-spawn-in-memory; no longer claims every course is dead"],
    ["A13 Orange-vs-Red contradiction row", "comparison/README.md L216-217; data/contradiction_resolution.md", "VERIFIED", "A6c correct; Red y13-82/98 chains, Orange y13-54/24 cobwebs/no chains"],
    ["A13 matrix primary_sources column", "comparison/data/ace_race_matrix.csv L1; STATUS.md L89-90", "VERIFIED", "18 columns; 12/12 paths resolve from data/"],
    ["A13 Yeti 6-container row", "comparison/data/ace_race_matrix.csv L5; yeti_set_go/data/containers.csv", "VERIFIED", "6 block entities (4 storage + 2 campfires); aligned with A4/A10"],
    ["A13 creeper marked video-only", "comparison/data/what_works.md L88", "VERIFIED", "Video-only capture, not confirmed in downloaded world"],
    ["A13 Parkour Tag §4.11 reference", "comparison/data/non_ace_takeaways.md L145", "VERIFIED", "Corrected from §4.8; A6b defect table is §4.11"],
    ["A13 route path fixed", "comparison/README.md L75; data/what_works.md L17; STATUS.md L45", "VERIFIED", "Live links point to ../clouds2/renders/route_labeled.png; STATUS line 45 is a fix-log quote"],
    ["A13 contradiction_resolution image prefixes", "comparison/data/contradiction_resolution.md L76", "CONTRADICTED", "frames/srred_iso.png and frames/srorg_iso.png do not resolve from comparison/; actual files are ../minigames/other/frames/"],
    ["A11 old presentation S5 row", "game-plugins/research/acerace/presentation/README.md L404-405/L414-416", "VERIFIED", "Countdown 13/12/11 and action-bar/custom HUD now recorded; montage caveat added"],
    ["A12 455 commands / 41 failures", "server_import/README.md L237-250; data/commands_risk.csv", "VERIFIED", "406 OK + 30 item-NBT + 1 incomplete + 10 no-target + 8 risk"],
    ["A13 matrix rows cite source reports", "comparison/data/ace_race_matrix.csv L1", "VERIFIED", "primary_sources column added; 12/12 paths resolve"],
    ["A13 matrix '111 ordered' / '10 checkpoint' wording", "comparison/data/ace_race_matrix.csv L2-3", "VERIFIED", "Changed to 'route-ordered' and 'start/gate/soft-respawn records'"],
    ["kill_sc_vip in A6/source reports", "grep over existing_maps", "NOT FOUND", "0 occurrences outside comparison's quoted error; A6 uses kill_sc_vp"],
    ["A6a/A6c container rows cite QA artifact", "minigames/sky_battle/data/containers.csv source column; minigames/other/data/containers.csv evidence column", "PARTIAL", "Numbers match A14 source scan, but provenance is _qa/data/minigames_container_by_arena.csv; A6c's data-folder evidence path ../../_qa is off by one level (should be ../../../_qa); add source NBT/region path if strict"],
    ["A1/A8 frames/ counts", "clouds2/frames; tgttos_industry/frames", "PARTIAL", "A1 has 2 JPGs but 51 renders; A8 has 4 frames but 41 renders; image rule met in renders, frames/ remains thin"],
    ["A13 §8.2 present-tense quotes of A6", "comparison/README.md L212-217", "PARTIAL", "Rows quote 'A6 says same layout/leftover' although A6 text is corrected; mark as historical/superseded or point at the stale atlas label"],
]

with (BASE / "acceptance.csv").open("w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["agent", "folder", "required_files", "count_checks", "image_checks", "hm_l", "verdict"])
    w.writerows(ACCEPT)

with (BASE / "claim_checks.csv").open("w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["claim", "source", "status", "evidence"])
    w.writerows(CLAIMS)

print(f"acceptance {len(ACCEPT)}; claims {len(CLAIMS)}")
