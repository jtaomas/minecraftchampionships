# Agent Handoff — Existing MCC Maps Research

**Status:** complete research pack. No game/plugin build, no map paste, no registry change, no world edit after import.
**Owner:** project owner. Do not start a build from this folder without an explicit instruction.
**Root:** `game-plugins/research/existing_maps/`
**Master index:** `README.md` in this folder.

## 1. What this pack is

Deep, evidence-first research on nine downloaded Minecraft map archives, all imported to the `25580` review server:

| Server world | Source | Version | Core result |
|---|---|---|---|
| `mcc_clouds2` | Clouds 2 - Electric Boogaloo | 1.20.1 | Best Ace Race route grammar; not a complete game |
| `mcc_citymarathon` | city-marathon.zip | 1.19.2 | City palette/skyline; no game logic; broken elytra |
| `mcc_spacerace` | Space Race.zip | 1.18.1 | Best vertical set pieces; two lobby-wide kill planes |
| `mcc_yetisetgo` | YetiSetGo.zip (= zipped-map.zip) | 1.19.2 | Compact circuit; backwards countdown, `@a` bugs |
| `mcc_decisiondome` | Decision Dome.zip | 1.17.1 | Display model, not a playable arena |
| `mcc_minigames` | daniellaur's minigames version 3 | 1.17.1 | 10 arenas; Sky Battle and Parkour Tag are real games |
| `mcc_tgttos_badlands` | tgttos-badlands-by-mickeykat.zip | 1.19.2 | Map/datapack intact, not runnable as imported |
| `mcc_tgttos_industry` | tgttos-industry-by-mickey-kat.zip | 1.19.1 | Same; 21 old item-NBT lines |
| `mcc_tgttos_pits` | tgttos-pits-by-mickey-kat.zip | 1.19.1 | Same; 21 old item-NBT lines |

`zipped-map.zip` is byte-identical to `YetiSetGo.zip`; it is not a separate map.

## 2. Read order

1. `README.md` — folder map, headline findings, status.
2. `comparison/README.md` — what each Ace Race map contributes, what fails, and the recommended template.
3. `_qa/README.md` and `_qa/GAPS.md` — independent audit; **0 FAIL, 0 active P0/P1**, residual P2 list.
4. `clouds2/README.md` — the deepest dossier.
5. `minigames/README.md` — 10-arena index; then `sky_battle/`, `parkour_tag/`, `other/`.
6. `resource_packs/README.md` — every copied pack/asset and its compatibility.
7. `server_import/README.md` — import state, 455 commands, 41 exact 1.21.1 failures.

## 3. What is committed in this handoff

- The entire `game-plugins/research/existing_maps/` pack: dossiers, CSVs, command/container inventories, 700 image files, copied resource packs/assets, synthesis, QA.
- `tools/world_to_mcmdump.py` — the world-to-render converter used for map previews.
- A separate Decision Dome vote-engine commit: plugin source, tests, vote API, integration, jar. The dome geometry and its previews are **not** in that commit.

## 4. What is deliberately NOT committed

- `game-plugins/AceRace/src/main/java/com/mcm/games/acerace/map/maps/KesslerRun.java`
- `game-plugins/AceRace/src/main/java/com/mcm/games/acerace/map/maps/SunsetSkyway.java`
- Ace Race tools, map previews, and Ace Race jar changes.
- `plugin-src/com/mcm/lobby/HubVenue.java`, `HubLayout.java`, `HubManager.java`, `HubGenerator.java` — Nocturne Orrery core geometry.
- `game-plugins/DecisionDome/previews/nocturne_orrery/` — dome geometry previews.
- `game-plugins/DecisionDome/SCHEMATIC.md` and `SPEC.md` — Nocturne Orrery geometry/spec docs.
- Any unrelated working-tree changes (TGTTOS, Parkour Warrior, server worlds, configs, jars other than DecisionDome).

If the owner later asks to commit the geometry, do it in a separate, clearly labelled commit after reviewing those paths.

## 5. Key conclusions for a future builder

- **None of the four Ace Race maps is a complete 40-player game.** They are courses/showcases: no timer, laps, ordered checkpoints, finish detection or live positions in most; `@a` effects and one-shot gates are common.
- **Clouds 2** contributes route grammar and set-piece sequencing; **Yeti** compactness/theme unity; **City Marathon** palette/skyline; **Space Race** vertical elytra/trident choreography.
- **Use the lap and runtime, not the download.** Original geometry, 2–3 laps, 40 slots, 10×4 teams, three ordered sections, one elytra and one trident beat per lap, server-side runtime.
- **Never copy:** global `@a` pad actions, static “1st place” HUD, one-shot gates, empty containers as “loot,” old item NBT, lobby-wide `kill @a`, solid pods with no seats/stairs, or the downloaded resource packs as-is (no licence; largest is 92% a copyrighted MCC track).
- **Decision Dome reference is a sculpture:** 20,054 blocks, 2 commands, 0 seats, 0 stairs, 42-deep pit, no roof. The playable Nocturne Orrery is the better game space; adopt only its team-colour verticals, hero crown, red-carpet arrival and shallow reveal pit.
- **Sky Battle** is a real game: 13 loot containers / 93 items, 477 commands, 40/477 fail on 1.21.1, reset destroys containers. **Parkour Tag** is a real 60-second game: 123 commands, fragile redstone team split, glowing runners invert MCC.
- **TGTTOS** is a finite porting job: enable the pack, rename `functions`→`function`, `tags/functions`→`tags/function`, bump `pack_format` to 48, convert 45 item-NBT lines. Not runnable as imported.

## 6. Server / source facts

- Review server: Paper 1.21.1, port `25580`, RCON `25576` (local only).
- Command blocks enabled for the imported maps; all nine worlds load and spawns are safe.
- `mcc_decisiondome` live spawn is `0,76,0`; source spawn `0,4,0` is unsafe.
- Multiverse forces survival; `allow-flight=false`; many maps expect creative/adventure. Do not change these blindly.
- Source archives: `C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS` plus `Clouds 2 - Electric Boogaloo (Download Version).zip`.
- Staging copies: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\`.

## 7. Next actions (not started)

1. If the owner wants a playable Ace Race, start from `comparison/data/runtime_gap.md` and build a server-side runtime; use the four maps as design references, not copied worlds.
2. If the owner wants TGTTOS playable, port one map first (Pits or Industry) and runtime-test one player end-to-end.
3. If the owner wants Sky Battle/Parkour Tag, use A6a/A6b as design references; fix the known 1.21/NBT and team-split defects before importing logic.
4. If the owner wants the Nocturne Orrery committed, review the excluded geometry paths and commit them separately.
5. Close the six residual P2s in `_qa/GAPS.md` before treating the pack as final-authoritative.

## 8. Guardrails

- Research first; no code or build until the owner explicitly asks.
- One agent per folder; do not edit another agent's dossier.
- The live server is read-only for research agents; no restarts, imports, unloads or world edits.
- Every claim needs H/M/L and a path. Do not replace evidence with prose.
- Preserve the large image/asset pack; do not add duplicate image sets.
