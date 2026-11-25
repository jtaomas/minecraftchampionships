# STATUS — A7 TGTTOS Badlands

**State: DONE — research complete, read-only. No blockers.**

Scope guard: this dossier analyses **only** the Badlands world + its `tgttos` datapack + the current `McmTgttos` implementation. It does **not** analyse Industry, Pits, any Ace Race map or the Clouds 2 video. No game/plugin code, builds, pastes, registry edits, RCON, server restarts or world edits were made. Every write is inside `game-plugins/research/existing_maps/tgttos_badlands/`; shared tools and sources were read-only. [H]

---

## 1. What was done

| Work item | Result | Evidence |
|---|---|---|
| Source world unpack/scan | 16 region files, 27.0 MB; 6,559 chunks; 411,834 non-air blocks; 62 IDs / 97 states; bounds x 4–254, y −61…12, z −5…69 | `_fastdump.py`, `raw_blocks.json`, `data/blocks.csv` |
| Tile entities | 49: 1 start sign + 48 daylight detectors; **0 command blocks**, 0 containers | `raw_tile_entities.json`, `data/command_blocks.csv` |
| Entities | 28 chickens: 27 finish + 1 void; all pen chickens saved with Resistance id 11 amp 5; one has `HurtByTimestamp` | `raw_entities.json`, `data/entities.csv` |
| Datapack unpack | `pack.mcmeta` (format 10), `load`/`tick` tags, 2 functions; 8 + 130 lines; 113 non-empty loop commands; 64 cactus commands; 44-entry `CanPlaceOn` | `data/raw/`, `data/datapack.md`, `data/datapack_commands.csv` |
| Scoreboards | 9 objectives, 1 dead (`playtime`); no display slot; no teams | `data/objectives.csv` |
| Gamerules | 36; `fallDamage=false`, `doDaylightCycle=false`, `doMobSpawning=false`, `randomTickSpeed=0`, `spawnRadius=10`, `doImmediateRespawn=false` | `data/gamerules.csv` |
| Start/respawn/finish | Sign click → countdown 130 → barrier gate; no respawn handler; finish = `damage_dealt_resisted` on Resistance VI chickens; 3 s spectator → `tp @a` start | `data/datapack.md` §§3–4 |
| Block reset | Marker armor stands + 31×31×21 box `air replace blue_concrete`; 64 cactus fills = 179 positions; 4 cacti missing, 2 stale | `data/cactus_reset.csv` |
| Barriers | 184 = 140 gate walls + 44 x=8 lip; countdown removes the 140; finish restores them | `_parse_pack.py` output, §6 datapack |
| Route | 6 legs, coordinates, sightlines, hazards, finish ledge, alternatives, no checkpoints/spectator area | `data/route.md` |
| Palette/lighting | 97.69% terrain, 1.45% built masonry, 0.52% accents; 94 sea lanterns + 12 lanterns; time 0 | `data/palette.md` |
| 1.21.1 risks | Folder rename (fatal), item NBT (fatal for kit), pack format, sign hotspot, mode, respawn, chickens | `README.md` §6, `data/datapack.md` §8 |
| `McmTgttos` comparison | Read-only review of `TgttosGame.java`, `GameListener.java`, `TgMapData.java`, `config.yml`, `README.md` | `README.md` §7 |
| Import-state confirmation (A12) | Source `Enabled=[vanilla,file/tgttos]`; imported `Enabled=[vanilla,file/bukkit,paper]`, `file/tgttos` absent; imported pack byte-identical; imported `GameType=0` (Survival); 1.21.1 DataVersion 3955 | `data/import_and_121_verification.md`, `data/import_state.json` |
| Images | 33 renders + 10 frames + icon; every cited image opened | `renders/`, `frames/`, `REFERENCE_IMAGES.md` |

---

## 2. Verification log (exact numbers)

- Source size: `region/` 16 files, **27,025,408 bytes**; `level.dat` 3,879 bytes; `icon.png` 8,371 bytes. [H]
- `level.dat`: DataVersion **3120** (1.19.2); `LevelName=TGTTOS Badlands Practise`; `GameType=1` (Creative); `allowCommands=1`; `Spawn=10,−42,31`; `DayTime=0`; `Time=468604`; `DataPacks.Enabled=[vanilla, file/tgttos]`; `WorldGenSettings` = flat, biome `the_void`, air layers. [H]
- Playerdata (`34110a7b-…dat`): `Pos=10.29,−42,31.41`; `playerGameType=2` (Adventure); `SpawnForced=1`; inventory = iron pickaxe + 1 blue concrete. [H]
- Chunks: **6,559**; non-air blocks: **411,834** (render tool reports 411,650 visible after excluding 184 barriers); block IDs **62**, states **97**. [H]
- Tile entities **49** (1 sign + 48 daylight detectors); command blocks **0**; containers **0**; item frames/armor stands/displays **0**. [H]
- Entities **28**: 27 chickens at x≈241, y=−1, z 11–52; 1 at 222.9,−114.4,−48.0 (void). All 27 pen chickens have `ActiveEffects` id 11, amp 5, duration 20, ambient 0. [H]
- Start sign NBT: `oak_wall_sign`, x=9, y=−41, z=31, facing east, `Color=black`, `GlowingText=0`; `Text1` empty + clickEvent `run_command: scoreboard players add @s start 1`; `Text2` bold green "Start"; Text3/4 empty. [H]
- Datapack: `pack_format=10`, description "Made by Mickey_Kat"; `loop` 130 lines, 113 non-empty; `load` 8 lines; cactus commands 64 (lines 56–120, line 101 blank), reset positions 179. [H]
- Cactus comparison: world 181, reset 179, intersection 177; missing = `(154,−3..0,33)` (4); stale = `(22,−42..−41,25)` (2). [H]
- Barrier comparison: world 184; 3 reset walls union 140; `world − reset = 44` (all x=8, y −39…−38, z 20–41). [H]
- `CanPlaceOn`: 44 entries; world palette 62; 19 palette IDs not placeable-on; 1 allow-list entry not in the world (`blue_concrete`). [H]
- Server: Paper **1.21.1-133** (`version_history.json`, `logs/latest.log`); data pack format 48. [H]
- Import state (A12 server path, read-only): source `level.dat` DataVersion 3120, `GameType=1`, `DataPacks.Enabled=[vanilla,file/tgttos]`, `Disabled=[]`; imported `level.dat` DataVersion **3955**, `GameType=0`, `DataPacks.Enabled=[vanilla,file/bukkit,paper]`, `Disabled=[bundle,trade_rebalance]`, `file/tgttos` in neither. Imported pack on disk is byte-identical to source (5/5 SHA-256). Imported `scoreboard.dat` has the 9 objectives; imported sign is `front_text` with the clickEvent preserved on empty line 1. [H]
- Images: 33 files in `renders/`, 10 in `frames/` (including `map_icon.png`). Every image cited in the dossier was opened in this session. [H]

---

## 3. Deliverables checklist (PLAN.md §4)

| Required | Status | Path |
|---|---|---|
| `README.md` full dossier with H/M/L | ✅ | `README.md` |
| `REFERENCE_IMAGES.md` | ✅ | `REFERENCE_IMAGES.md` |
| `frames/` actual full-resolution evidence | ✅ | `frames/` (10 files) |
| `data/blocks.csv` every type, count, share, use | ✅ | 97 rows, 411,834 blocks |
| `data/command_blocks.csv` every command block | ✅ | 0 rows + explicit note |
| `data/signs.csv` front/back, colour, glow, click | ✅ | 1 row |
| `data/checkpoints.csv` order/gate/grants | ✅ | 1 `none` row (no checkpoints) |
| `data/route.md` leg-by-leg | ✅ | 6 legs + alternatives + finish |
| `data/palette.md` palette/accents/gradients/lighting | ✅ | yes |
| `data/entities.csv` | ✅ | 28 chickens |
| `renders/` top-down + 2 iso angles, labelled | ✅ | top + angle 0/1/2 + labelled route + profile + sections |
| `STATUS.md` | ✅ | this file |
| ≥6 full-resolution images | ✅ | 43 images |
| "Better/worse than the others" + reusable | ✅ | `README.md` §7 |
| No claim without H/M/L | ✅ | inline tags throughout |

`data/command_blocks.csv` and `data/checkpoints.csv` use an explicit `none` row so the zero is accounted for rather than missing. Containers/shulkers: **none exist** (49 tile entities checked); "every container opened" is vacuously satisfied. [H]

---

## 4. Open questions (not blockers)

1. **Sign click hotspot in 1.21.1**: the clickEvent is on empty line 1; the visible "Start" is line 2. The imported sign upgrade **preserved** the clickEvent in `front_text.messages[0]` (H), but whether the client executes a click on the empty line was not live-tested. Tag **H NBT / M/L click**.
2. **Live world state**: `mcc_tgttos_badlands` was not queried through RCON (A12 owns that), but its files were read read-only at A12's server path. Confirmed: DataVersion 3955, `GameType=0` (Survival), Multiverse `gamemode: survival`, pack on disk byte-identical but **not enabled**, gamerules and scoreboard survived, sign upgraded with the clickEvent on empty line 1. The only unverified live behaviour is the sign click / actual in-game start. Tag **H files / M live UI**.
3. **Exact 1.21.1 replacement syntax** for `CanPlaceOn`/`CanDestroy`/`Enchantments`: the failure is certain; the exact component string should be validated by A12's compatibility matrix. Tag **H failure / M fix**.
4. **North low bypass** around the mesa (z≈0–8) is a terrain read, not walked end-to-end. Tag **M/L**.
5. **Speedrun times / fastest line**: not measured; no in-game run. Tag **L**.
6. **Void chicken**: below the world at y=−114; it will be removed on load. It is not one of the 27 pen chickens. Tag **H**.

---

## 5. What was intentionally not done

- No analysis of Industry, Pits, any Ace Race map or the video (A6/A8/A9/A11).
- No world edit, no paste, no build, no plugin code, no registry edit, no server restart, no RCON.
- No changes to `tools/world_to_mcmdump.py`, `tools/render_preview.py` or any other agent's folder.
- No attempt to "fix" the 1.19.2 datapack; only the exact failure modes are recorded.
- No claim that the live server map is playable; on the contrary, §6 of the README records why it is not, as-is, on 1.21.1.

---

## 6. Files added by this agent

```
README.md
REFERENCE_IMAGES.md
STATUS.md
_ascii.py  _crop.py  _fastdump.py  _heightmap.py  _label.py  _make_commands.py
_make_data.py  _parse_pack.py  _scan_world.py  _section.py  _stats.py  _import_sign.py
data/blocks.csv  data/cactus_reset.csv  data/can_place_on.csv
data/checkpoints.csv  data/command_blocks.csv  data/datapack.md
data/datapack_commands.csv  data/entities.csv  data/gamerules.csv
data/objectives.csv  data/palette.md  data/route.md  data/route_waypoints.csv
data/signs.csv  data/source_hashes.csv  data/raw/*
data/import_and_121_verification.md  data/import_state.json
frames/*  renders/*
world.mcmdump.gz  raw_blocks.json  raw_entities.json  raw_tile_entities.json
raw_datapack.json  raw_chunks.csv  data/*.mcmdump.gz (crops)
```

The analysis helpers are local to this folder; the source world and shared tools were not modified. The large intermediate `world.mcmdump.gz` (972,842 bytes) and crop dumps are kept for reproducibility; they can be deleted without affecting the Markdown/CSV evidence. [H]
