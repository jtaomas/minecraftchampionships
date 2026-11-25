# Existing MCC Maps — Compiled Research Pack

**Start here.** This is the single folder containing all research on the downloaded MCC recreations and minigame maps.

- Root: `C:\Users\LeonS\Desktop\Mcc\game-plugins\research\existing_maps`
- Source archives: `C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS` plus `Clouds 2 - Electric Boogaloo (Download Version).zip`
- Live worlds: imported on the `25580` review server as `mcc_clouds2`, `mcc_citymarathon`, `mcc_spacerace`, `mcc_yetisetgo`, `mcc_decisiondome`, `mcc_minigames`, `mcc_tgttos_badlands`, `mcc_tgttos_industry`, `mcc_tgttos_pits`.

**Research only:** no game code, plugin code, builds, pastes, registry edits or world changes were made. All maps are imported and loaded; the research is read-only.

## Read these first

| Order | File | Why |
|---|---|---|
| 1 | `comparison/README.md` | The synthesis: what each Ace Race map contributes, what fails, and the recommended template. |
| 2 | `_qa/README.md` and `_qa/GAPS.md` | Independent audit: final verdicts, residual P2 gaps, and exact evidence checks. |
| 3 | `clouds2/README.md` | The deepest map dossier: 13-leg route, 322 commands, 225 containers, pack mapping. |
| 4 | `minigames/README.md` | The multi-arena world: 10 arenas, 942 commands, Sky Battle and Parkour Tag bounds. |
| 5 | `resource_packs/README.md` | Every texture, sound, model and language file, with compatibility and provenance. |

## Folder map

| Folder | Contents | Key evidence |
|---|---|---|
| `clouds2/` | Clouds 2 - Electric Boogaloo | 322 commands, 285 signs, 225 empty shulkers, 150,414 blocks, 51 renders, 21.2 MB pack audit |
| `city_marathon/` | City Marathon | 67 commands, 156 empty containers, 207,440 blocks, 1,352-block loop |
| `space_race/` | Space Race | 29 commands, 296,699 blocks, elytra/trident zones, two global kill planes |
| `yeti_set_go/` | Yeti Set Go! | 37 commands, 28,533 blocks, 6 container block entities, compact circuit |
| `decision_dome_recreation/` | Downloaded Decision Dome | 20,054 blocks, 2 commands, 45 block entities, exact geometry/palette/sightlines |
| `minigames/` | daniellaur's minigames world | 10 arenas, 942 commands, 187 signs, 34 container block entities |
| `minigames/sky_battle/` | Sky Battle | 477 commands, 13 loot containers / 93 items, 47,874 non-air blocks |
| `minigames/parkour_tag/` | Parkour Tag | 123 commands, 60-second round, 1 barrel + 2 guide-book lecterns |
| `minigames/other/` | Super Clash, Sky Race, hub, command room, Rebecca | 8-arena inventory, 12 containers, reuse ranking |
| `tgttos_badlands/` | TGTTOS Badlands | Datapack, route, 411,834 blocks, 3 old item-NBT lines |
| `tgttos_industry/` | TGTTOS Industry | Datapack, route, 122,276 blocks, 21 old item-NBT lines |
| `tgttos_pits/` | TGTTOS Pits | Datapack, route, 164,145 blocks, 21 old item-NBT lines |
| `resource_packs/` | All packs and extracted assets | 3 packs, 71 textures, 455 commands / 387 containers cross-reference, 95 images |
| `video_clouds2/` | X4l2WSQv0Wk analysis | 17 full-res stills, 48 cuts, HUD/countdown evidence, 3:54.97 montage |
| `server_import/` | Import and 1.21 compatibility | All 9 worlds, 455 commands, 41 exact failures, datapacks, start instructions |
| `comparison/` | Cross-map synthesis | 4×18 Ace Race matrix, runtime-gap spec, adopt/reject list, comparison sheet |
| `_qa/` | Red-team audit | 59/59 numeric checks, 417 image citations, 407 distinct, final PASS/FAIL |

## Headline conclusions

| Map | Verdict |
|---|---|
| **Clouds 2** | Best route grammar and set-piece sequencing; **not a game** — no timer, laps, finish or positions, global `@a` effects, 31 command failures. |
| **City Marathon** | Best city palette, skyline and road hierarchy; no game logic, broken elytra, all 156 containers empty. |
| **Space Race** | Best vertical elytra/trident choreography; two lobby-wide kill planes and a broken elytra grant. |
| **Yeti Set Go** | Best compactness/theme unity; backwards countdown, `@a` bugs, simulation-distance dependence. |
| **Decision Dome** | A **display model**, not a playable arena: no seats, stairs, roof or safe pit; only 2 commands. |
| **Sky Battle** | A real game: 4 loot towers, 13 loot containers, 93 items, 477 commands; 40/477 fail on 1.21.1 and the reset destroys containers. |
| **Parkour Tag** | A real 60-second game: role pads, stadium, 123 commands; fragile redstone team split, glowing runners invert MCC. |
| **TGTTOS ×3** | Maps and datapacks are intact but **not runnable as imported**: packs disabled, old `functions/` layout, 45 item-NBT lines. |

**Ace Race template:** the lap and runtime, not the download. Adopt Clouds 2 route grammar, Yeti compactness, City palette/skyline and Space verticality. Reject global `@a` effects, static HUD, one-shot gates, empty containers, old NBT and lobby-wide kill planes.

## Evidence and images

- **1,699 files**, **288.3 MB**, **700 image files**.
- **417 image citations**, **407 distinct**; **414 resolve**, 2 need a path-prefix fix, 1 is a fix-log quote.
- QA opened **47 distinct images** across all folders; no unreadable image and no cross-agent duplicate evidence.
- Each map has `frames/` and/or `renders/`, plus `REFERENCE_IMAGES.md` mapping path → source → what it proves.
- Resource packs and extracted assets are copied under `resource_packs/packs/` and `resource_packs/textures/`; no pack is enabled on the server.

## Status

- Final QA: **0 FAIL**, **0 active P0/P1 gaps**.
- **10 PASS**, **6 PASS WITH FIXES (P2 only)**.
- Six residual P2s are listed in `_qa/GAPS.md` (stale atlas label, two path-prefix references, thin frame folders, historical wording, duplicate-frame note).
- No build agents were dispatched. No game/plugin code was written. No server world was modified after import.

## Original plan

`PLAN.md` records the 17-agent roster, effort tiers, wave dependencies, ownership rules and acceptance criteria used for this pack.
