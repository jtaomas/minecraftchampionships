# STATUS — A6c other-minigames inventory

Updated: 2026-11-25
Owner: A6c. Folder: `game-plugins/research/existing_maps/minigames/other/`.
Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\daniellaur-s-minigames-version-3\daniellaur's minigames version 3` (1.17.1, DataVersion 2730).
Live world: `mcc_minigames` (not touched; no RCON used).

## Done

- Read A6’s `README.md`, `STATUS.md`, `data/arenas.csv`, `data/command_clusters.csv`, `data/command_blocks.csv`, `data/signs.csv`, `data/islands.csv`, `data/blocks_by_island.csv`, `data/blocks_positions.csv` and the world’s `data/scoreboard.dat`. **H**.
- Catalogued the eight assigned arenas: `MG-HUB-01`, `MG-SC-01`, `MG-SR-HUB-01`, `MG-SR-RED-01`, `MG-SR-ORG-01`, `MG-CMD-01`, `MG-REB-01`, `MG-REB-02`. **H**.
- One-page-per-arena inventory in `README.md` §2: identity, bounds, entry, palette, block count, signs, containers, commands, completeness, confidence, deep-dive verdict. **H/M**.
- `data/other_arenas.csv`: 8 rows × 31 columns (bounds, entry, chunks, total/barrier/non-barrier/renderable blocks, signs, containers, commands by kind, clusters, palette, completeness, confidence, deep dive, images, notes). **H**.
- `data/containers.csv`: 12 empty containers in A6c arenas with exact x/y/z (hub 8, Super Clash 3, Sky Race hub 1). **H** (A14 independent source scan; cross-checked).
- Container statement: **34 container block entities in the whole minigames world; 21 empty; 13 Sky Battle loot containers (93 items); A6c arenas own 12 of the empty ones.** **H**.
- `data/commands.csv`: per-arena command summary (counts, kinds, clusters, top verbs, key commands, purpose, caveat). **H**.
- `data/palette.md` and `data/route.md`: required per-arena palette and one-paragraph route/access summaries. **H/M**.
- `data/scoreboard_objectives.csv` (26 objectives with exact criteria) and `data/teams.csv` (15 teams with friendly-fire flags). **H**.
- 16 full-resolution evidence images (top + iso per arena) in `frames/` and `renders/`; all 16 opened and checked. The `frames/` set is byte-identical to the `renders/` set, so this is **16 distinct views, not 32**. **H**.
- `REFERENCE_IMAGES.md`: every cited image with dump/bounds/size, what it shows, what it proves, confidence. **H**.
- High-level 1.21.1 risk table in `README.md` §5. **M** (A12 owns the exact matrix).
- Corrections against current A6 in `README.md` §4: **no active contradictions**. The **Sky Race Red/Orange layout** and **Sky Race sign intent** findings are kept as resolved evidence rows because A6 now reflects them. The former CMD/hub/Super Clash/Rebecca rows were removed as already agreed; the full container count is stated in §1b and matches A6’s current 34/21/13 statement. **H** unless marked.

## Blocked

- None. No server query, build, paste, registry edit or code was needed. A6's files and the shared tools were read-only; no file outside `minigames/other/` was written.

## Open questions and resolved checks (with confidence)

1. **Hub crown ↔ pad deck access.** `z 951..960` is empty at every y, so there is no block path between the crown floor (y21) and the pad deck (y8–10). **H** blocks. The only deck→crown links are the “Tp to lobby” pads and the lobby-teleporter item → `-196 24 927`; there is no crown→deck command. **H** commands. **M:** a plugin/creative teleport is expected, or the deck is meant to be reached by a command not exported. This is the main reuse unknown.
2. **Super Clash villain kills.** `kill_sc_vp` uses `teamkill.green`; villains join `Super_Clash_V`, not `Green`. **H** criteria + team list. **M:** villain kills do not count, so the villain side cannot reach 10. Needs a gameplay test or an A12-style scoreboard check.
3. **Sky Race Red vs Orange layout (resolved in A6).** A6 now says they are not a reskin: Red climbs y13–82 with 98 chains + grey cloud props; Orange runs y13–54 with 24 cobwebs and no chains. **H** block positions + images. The difference is deliberate-looking; **M** that Orange is the easy variant.
4. **Sky Race sign intent (resolved in A6).** A6 now says the wool/shears signs match the loadout; “Pvp free lobby” is in the pre-start lobby, while `Sky_Race` has `AllowFriendlyFire=1` during the race. **H** signs + team. **M:** the sign means “the lobby is PvP-free”, not “the race is PvP-free”.
5. **Rebecca identity.** The kiosk/course are **H** as teleport + jump-boost geometry; the game label (parkour/race) stays **M**. The course is non-functional as shipped: kiosk leaves level 201, the jump pad requires level 1. **H** commands; **M** intent.
6. **Live-world parity.** Source has 942 command blocks, 187 signs and 34 container block entities; the live world’s command-block count was confirmed by A12, but the live sign/container state was not re-checked here. **M** that the source inventory matches live.
7. **Command room disabled block.** One repeating block at `993 15 -14` has `condition_met=0` (border-damage effect). **H**. It may be intentionally off; no gameplay test was run.
8. **Container provenance.** The 12 A6c containers come from A14’s independent source scan, not a new A6c NBT scan; all have `items=0`, so the only open question is live-world parity, not hidden loot. **H** source; **M** live.

## What a later deep dive should start with

- **Sky Race Red:** a barrier-off route render, checkpoint-free lap timing, and a per-player reset test. **M** (planning suggestion, not implementation).
- **Super Clash:** test the villain kill objective, then a roof-off interior render and a 10-kill loop test. **M**.
- **Hub:** decide whether the crown↔deck gap is a bug or a plugin responsibility before reusing the selector. **M**.
- **Command room:** use it as a checklist for plugin scope, not as a copy source; the commands are 1.17 NBT and hard-coded to Sky Battle. **M**.

## Not done

- No Sky Battle (`MG-SB-01`) or Parkour Tag (`MG-PT-01`) deep dive — A6a/A6b own those.
- No Ace Race, TGTTOS, Decision Dome or resource-pack analysis.
- No build, no paste, no schematic, no plugin/game code, no registry edit, no restart, no RCON.
- No 1.21 fix; risks only.
- No re-scan of the 18,628-chunk world; counts are A6’s decoded data, spot-checked against `blocks_positions.csv`.
- No new container NBT scan; the 12 A6c rows use A14’s independent scan. No container was edited or opened live.

## Confidence summary

| Area | Confidence | Why |
|---|---|---|
| Bounds, block counts, palettes | **H** | Decoded block table, exact counts |
| Command counts/kinds/key commands | **H** | Exact `command_blocks.csv` rows |
| Sign counts/key text | **H** | Exact `signs.csv` rows |
| Container counts/positions | **H** | A14 independent source scan; all `items=0` |
| Arena identity (HUB/SC/SR/CMD) | **H** | Signs + commands + geometry agree |
| Arena identity (REB) | **M** | Signs/commands prove teleport/jump, not the game label |
| Completeness verdicts | **H/M** | H for what exists; M for intent/bugs that need a play test |
| Reuse ranking | **M** | Judgement from the evidence above, not a play test |
| 1.21.1 risk level | **M** | Syntax observed; runtime not tested (A12 owns exact matrix) |
