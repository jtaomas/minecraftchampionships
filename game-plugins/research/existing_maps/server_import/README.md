# A12 — Server import verification (read-only)

Status: **research complete; no server or world changes were made**
Server: `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review`
Paper: **1.21.1-133-3cb8529** (MC 1.21.1, DataVersion **3955**), Java 21.0.13.1 `(H)`
Ports: game **25580**, RCON **25576** `(H)`
Checked: 2026-11-25 00:10–02:40 local. Players online: **0**; TPS last 1m/5m/15m: **20.0/20.0/20.0** `(H)`

This dossier is based on:

- direct reads of `level.dat`, region `.mca` files, `datapacks/`, and `resources.zip` in the server folder `(H)`;
- the import-window log `logs/2026-11-24-18.log.gz` and `logs/latest.log` `(H)`;
- read-only RCON: `mv info`, `mv list`, `gamerule`, `execute in <world> run gamerule`, `version`, `plugins`, `tps`, `list` `(H)`;
- source archives in `Downloads` for original versions and original datapack state `(H)`;
- the 1.21.1 pack-format tables and version page for format numbers `(H)`: <https://minecraft.wiki/w/Pack_format>, <https://minecraft.wiki/w/Java_Edition_1.21.1>.

No `mv import`, `mv unload`, `stop`, `reload`, `kill`, `fill`, `setblock`, `gamemode`, or world edit was run. No resource pack or datapack was enabled. No file outside `game-plugins/research/existing_maps/server_import/` was written.

Evidence labels: **H** = directly observed in a file/command/frame; **M** = inference from two sources; **L** = guess. Every non-trivial claim below carries one.

---

## 1. Bottom line

| World | Source version | Load | Spawn | Command blocks | Datapack | Verdict |
|---|---|---:|---|---:|---|---|
| `mcc_clouds2` | 1.20.1 | yes | 5,4,23 safe | 322 | none | **playable with caveats** |
| `mcc_citymarathon` | 1.19.2 | yes | 8,0,5 safe | 67 | none | **playable with caveats** |
| `mcc_spacerace` | 1.18.1 | yes | 17,115,27 safe | 29 | none | **playable with caveats** |
| `mcc_yetisetgo` | 1.19.2 | yes | 21,−1,4 safe | 37 | none | **playable with caveats** |
| `mcc_decisiondome` | 1.17.1 | yes | 0,76,0 safe* | 2 | 12 zips + 1 folder, disabled | **build only** |
| `mcc_minigames` | 1.17.1 | yes | −196,24,927 safe | **942** | none | **loaded, not audited** |
| `mcc_tgttos_badlands` | 1.19.2 | yes | 10,−42,31 safe | 0 | `tgttos`, disabled | **not runnable as imported** |
| `mcc_tgttos_industry` | 1.19.1 | yes | 73,−33,11 safe | 0 | `Industry`, disabled | **not runnable as imported** |
| `mcc_tgttos_pits` | 1.19.1 | yes | 15,−2,36 safe | 0 | `Pits`, disabled | **not runnable as imported** |

\* Multiverse changed DecisionDome's saved spawn from the unsafe source value `0,4,0` to `0,76,0` during import `(H)`.

The biggest findings, in order:

1. **All three TGTTOS datapacks are disabled in `level.dat` and still use the pre-1.21 `functions`/`tags/functions` folder layout.** Their Start signs, timers, respawn and finish logic therefore do nothing on 1.21.1. They were enabled in the source worlds. `(H)`
2. **30 Clouds 2 command blocks use pre-1.20.5 item NBT** (`leather_boots{display:…,Enchantments:…}`). 1.21.1 rejects that item argument, so the six depth-strider boot pads never work. `(H)`
3. **Clouds 2 has one malformed chain command, `setblock 5 4 22`** (no block argument), in the reset chain. `(H)`
4. **City Marathon (7 blocks) and Yeti (3 blocks) issue `/gamemode adventure` with no target from command blocks.** They fail with `permissions.requires.player`; the maps never force adventure mode, and players stay in Multiverse's forced survival. `(H)`
5. **Multiverse is configured `enforce-gamemode: true` and `enforce-flight: true`, with every imported world set `gamemode: survival` and `allow-flight: false`.** Regular players are forced to survival; survival/adventure players have flight cleared, while players already in creative keep it (Multiverse returns early for `CREATIVE`). Elytra glide is a separate mechanic but should be confirmed in a live test. `(H config/code; M elytra)`
6. **Space Race's `resources.zip.zip` is not a valid resource pack in its current layout** (one zip with a wrapper folder `Space Race Resource Pack/` — not a nested zip — so `pack.mcmeta` is not at the zip root; pack_format 6). Clouds 2's pack references two OGGs that are absent. No pack is currently enabled. `(H)`
7. **The plan's command-block counts for DecisionDome (0) and Minigames (0) are wrong.** The source archives and imported worlds contain **2** and **942** command blocks respectively. A6/A13 must not rely on the plan table for those worlds. `(H)`

---

## 2. Server baseline and load verification

### 2.1 Server settings that affect every map

From `server.properties` `(H)`:

```
server-port=25580
enable-rcon=true
rcon.port=25576
enable-command-block=true
allow-flight=true
gamemode=survival
difficulty=normal
force-gamemode=false
spawn-protection=0
view-distance=8
simulation-distance=6
max-players=60
resource-pack=
require-resource-pack=false
```

From `plugins/Multiverse-Core/config.yml` `(H)`:

```
enforce-gamemode: true
enforce-flight: true
enforce-respawn-at-world-spawn: true
```

From `plugins/Multiverse-Core/worlds.yml` and `mv info` `(H)`:

- all nine imported worlds: `gamemode: survival`, `allow-flight: false`, `keep-spawn-in-memory: true`, `pvp: true`, `hunger: true`, `auto-load: true`;
- `mv info --page 3` reports **Keep Spawn In Memory: true** and **Allow Flight: false** for all nine `(H)`;
- `mv info --page 4` reports **PVP Enabled: true** and **Hunger Depletes: true** for all nine `(H)`;
- `mv info --page 2` reports **World Type: FLAT** and `--page 3` reports **Generate Structures: false** for all nine. Existing chunks are the imported builds, but new chunks beyond them will generate flat `(H)`.

Consequences:

- **Regular players are forced to survival on entry.** Ops (level 4) usually bypass Multiverse checks; do not assume a normal player is in adventure. `(H config; M bypass for ops)`
- **Creative flight is not removed for players already in creative.** Multiverse's `handleFlightEnforcement` returns early when the player's game mode is `CREATIVE`; survival/adventure players with `allowFlight` set are cleared. Elytra glide is a separate flag and was not live-tested. `(H code; M elytra)`
- **Spawn chunks stay loaded**, so always-active command blocks near spawn tick even with no players nearby. The audited race worlds had non-default `LastExecution` values, and their start chains show sequential ticks. `(H)`

### 2.2 World load evidence

All nine worlds were loaded by RCON at 23:31 during the review server startup `(H)`:

| World | Import/load time (log 18) | `mv info` spawn | `execute in <world> run gamerule doDaylightCycle` |
|---|---|---|---|
| `mcc_clouds2` | 23:31:13 | 5,4,23 | false |
| `mcc_citymarathon` | 23:31:28 | 8,0,5 | false |
| `mcc_spacerace` | 23:31:30 | 17,115,27 | false |
| `mcc_yetisetgo` | 23:31:32 | 21,−1,4 | false |
| `mcc_decisiondome` | 23:31:35 | 0,76,0 | false |
| `mcc_minigames` | 23:31:37 | −196,24,927 | false |
| `mcc_tgttos_badlands` | 23:31:39 | 10,−42,31 | false |
| `mcc_tgttos_industry` | 23:31:41 | 73,−33,11 | false |
| `mcc_tgttos_pits` | 23:31:44 | 15,−2,36 | false |

`mv list` returns all registered worlds; `mv info` succeeds for every one; `execute in` proves each dimension is loaded, not merely registered. `(H)`

### 2.3 Spawn safety

Every spawn was read block-by-block from the region files `(H)`:

| World | Spawn | Ground below | Feet/head | Safe? |
|---|---|---|---|---|
| `mcc_clouds2` | 5,4,23 | `snow_block` | air | yes; floor button at 5,4,22 |
| `mcc_citymarathon` | 8,0,5 | `gray_wool` | air | yes |
| `mcc_spacerace` | 17,115,27 | `gray_concrete_powder` | air | yes |
| `mcc_yetisetgo` | 21,−1,4 | `gray_concrete` | air | yes |
| `mcc_decisiondome` | 0,76,0 | `lime_concrete` | air | yes, but lime concrete is one block west at feet/head |
| `mcc_minigames` | −196,24,927 | `netherite_block` | air | yes; hub spawn |
| `mcc_tgttos_badlands` | 10,−42,31 | `quartz_block` | air | yes; Start sign at 9,−41,31 |
| `mcc_tgttos_industry` | 73,−33,11 | `quartz_block` | air | yes; Start sign at 73,−32,10 |
| `mcc_tgttos_pits` | 15,−2,36 | `quartz_block` | air | yes; Start sign at 14,−1,36 |

No spawn is inside a solid block, suffocating, or over void. DecisionDome's spawn is tight but valid: the exact point is air and the adjacent lime concrete is at x=−1, outside the player's 0.6-wide box. `(H)`

### 2.4 Logs

Import-window log `2026-11-24-18.log.gz` `(H)`:

- `[23:31:35] Ignoring unknown attribute 'forge:entity_gravity'` ×2 for DecisionDome;
- `[23:31:35] [Multiverse-Core] Spawn location from world.dat file was unsafe. Adjusting...` and `Original Location: X: 0 Y: 4 Z: 0`, new spawn `0.00,76.00,0.00` for `mcc_decisiondome`;
- no datapack, command-block, chunk, or world-load exception for any imported world.

`latest.log` `(H)`:

- `[00:05:48] [Paper Watchdog Thread/ERROR]: The server has not responded for 10 seconds! Creating thread dump` — stack is `LevelStorageSource.saveIncremental → saveLevelData → WindowsFileCopy.move`; this is a **world-save stall**, not a map crash. Server continued and later TPS was 20.0.
- `[01:48:35] [PaperVersionFetcher] Error while parsing version` — Paper API network failure, unrelated.
- no per-world error after import. `(H)`

---

## 3. Per-world load verification

### 3.1 `mcc_clouds2` — Clouds 2 (1.20.1 → 1.21.1)

- `mv info`: spawn 5,4,23, survival, peaceful; `mv info --page 3`: keep-spawn true, allow-flight false; page 4: PvP true, hunger true. `(H)`
- 21,904 present chunks across 30 overworld region files; 109.5 MB world folder. `(H)`
- 285 signs, 225 shulker boxes; 322 command blocks: **284 repeating, 23 impulse, 15 chain**; **298 always-active**, **24 need redstone**; none powered at read time. `(H)`
- Gamerules include `commandBlockOutput=false`, `doDaylightCycle=false`, `doWeatherCycle=false`, `doMobSpawning=false`, `keepInventory=true`, `fallDamage=false`, `doFireTick=false`, `randomTickSpeed=0`, `spawnRadius=0`, `mobGriefing=false`. `(H)`
- Datapacks enabled: `vanilla, file/bukkit, paper`; no map datapack. `(H)`
- Resource pack: `resources.zip`, pack_format **15** (1.20.x), 22.2 MB, 100 entries. `(H)`
- Start/reset blocks executed in the source save: the reset chain `LastExecution` values are non-default and cluster in the reset sequence (e.g. −49,1,22 = 7464824; −35,1,22 = 7464804; −5,4,25 = 7464937). `(H)`
- **1.21.1 failures:** 30 item-NBT boot commands + 1 malformed `setblock` (Section 4). `(H)`
- **Verdict:** playable start/reset; boot pads broken; avoid multi-player kill-plane test (Section 4.3). `(M)`

### 3.2 `mcc_citymarathon` — City Marathon (1.19.2 → 1.21.1)

- `mv info`: spawn 8,0,5, survival, peaceful; allow-flight false; keep-spawn true. `(H)`
- 6,374 chunks, 72 overworld region files; 29.5 MB. `(H)`
- **7 signs**; 140 shulker boxes, 84 banners. `(H)`
- 67 command blocks: **60 repeating, 5 impulse, 2 chain**; **62 always-active**, 5 need redstone; none powered. `(H)`
- Gamerules: `commandBlockOutput=false`, `sendCommandFeedback=true`, `doDaylightCycle=false`, `fallDamage=false`, `keepInventory=true`, `randomTickSpeed=0`. `(H)`
- No datapack; no resource pack. (`advancements.zip` is a world backup, not a pack; Section 6.) `(H)`
- Start button: polished blackstone floor button at **8,0,8**, three blocks south of spawn; start sequence includes fills at z=10 and countdown titles; `LastExecution` for the title blocks 110297/110313/110329 (16-tick spacing). `(H)`
- **1.21.1 failures:** 7 `/gamemode adventure` blocks with no target (Section 4.2). `(H)`
- **Verdict:** start works; players stay survival; no adventure lockdown. `(M)`

### 3.3 `mcc_spacerace` — Space Race (1.18.1 → 1.21.1)

- `mv info`: spawn 17,115,27, survival, peaceful; allow-flight false. `(H)`
- 14,198 chunks, 22 overworld region files; 56.0 MB. `(H)`
- 22 signs; 29 command blocks: **22 repeating, 4 impulse, 3 chain**; 22 always-active, 7 need redstone; none powered. `(H)`
- Gamerules are **looser** than the other race maps: `doMobSpawning=true`, `mobGriefing=true`, `doFireTick=true`, `doEntityDrops=true`, `randomTickSpeed=3`, `spawnRadius=1`; `fallDamage=false`, `keepInventory=true`, `commandBlockOutput=false`. `(H)`
- No datapack; `resources.zip.zip` exists but is not a valid resource pack (Section 5). `(H)`
- Start: wall stone button at **6,116,28** beside sign **6,116,29** `"START ->"`; countdown command blocks at 10/11,111,22/25/28 (`LastExecution` 2425233/2425249/2425265). `(H)`
- **1.21.1 failures:** none found in command strings. `(H)`
- **Behaviour risks:** two kill planes `kill @a`; mob/fire settings. `(H)`
- **Verdict:** start/countdown should work; pack cosmetic only; kill planes are dangerous with more than one player. `(M)`

### 3.4 `mcc_yetisetgo` — Yeti Set Go (1.19.2 → 1.21.1)

- `mv info`: spawn 21,−1,4, survival, peaceful; allow-flight false. `(H)`
- 3,357 chunks, 73 overworld region files; 13.9 MB. `(H)`
- 14 signs; 3 shulker boxes; 37 command blocks: **30 repeating, 5 impulse, 2 chain**; 32 always-active, 5 need redstone; none powered. `(H)`
- Gamerules: `commandBlockOutput=false`, `sendCommandFeedback=true`, `doDaylightCycle=false`, `fallDamage=false`, `keepInventory=true`, `randomTickSpeed=3`. `(H)`
- No datapack; `resources.zip` pack_format **9** (1.19.x). `(H)`
- Start: polished blackstone floor button at **21,−1,6**, two blocks south of spawn; title chain `LastExecution` 1083008/1083024/1083040. `(H)`
- **1.21.1 failures:** 3 `/gamemode adventure` blocks with no target (Section 4.2). `(H)`
- **Verdict:** start works; no adventure lockdown; pack needs format bump. `(M)`

### 3.5 `mcc_decisiondome` — Decision Dome recreation (1.17.1 → 1.21.1)

- Source `level.dat` spawn was **0,4,0**; Multiverse declared it unsafe and changed it to **0,76,0** at import. `(H)`
- 1,344 chunks, 6 region files; 6.1 MB. Pure build: **0 signs**, 24 banners, 17 skulls, 2 beehives. `(H)`
- **2 command blocks** at −3,68,−19: chain `effect give @p minecraft:glowing 999999 255 true` and impulse `give @p minecraft:egg`. They are leftovers, not start logic; both parse in 1.21.1. `(H)`
- Datapacks on disk: 12 VanillaTweaks zips (all pack_format **7**, 1.17) plus one `VanillaTweaks_d535816_UNZIP_ME` folder whose root has no `pack.mcmeta`; **none enabled** in `level.dat`. `(H)`
- No resource pack. `(H)`
- **Verdict:** build-only; do not expect a start button, timer, or game logic. `(H)`

### 3.6 `mcc_minigames` — daniellaur's minigames (1.17.1 → 1.21.1)

- `mv info`: spawn −196,24,927, **normal** difficulty, survival; allow-flight false. `(H)`
- 18,628 chunks, 68 overworld region files; 74.1 MB. `(H)`
- **942 command blocks**: 182 impulse, 548 chain, 212 repeating; 731 always-active, 211 need redstone, **6 powered at read time**; 1 empty command. `(H)`
- 187 signs, chests, barrels, lecterns. Spawn is a hub with signs for Sky Battle, Parkour Tag, Super Clash, Gladiators (maintenance), Theme Race (event only), Sky Race, Guest's Minigames (closed). `(H)`
- No datapack; no resource pack. `(H)`
- **A12 did not audit the 942 commands** (out of the four-world scope) beyond load/spawn. Some already use 1.20.5+ components (`trident[minecraft:enchantments=…]`), while others still use old firework NBT; A6 must handle this world. `(H)`
- **Verdict:** loaded and spawn-safe; **not** "0 command blocks" as the plan says. Do not test blindly. `(H/M)`

### 3.7 TGTTOS worlds — loaded but inert

All three load and have safe spawns. All three have **zero command blocks**. Their entire logic is a datapack, and the datapack is not running.

| World | Spawn | Start sign | Pack on disk | Enabled now? | Source enabled? |
|---|---|---|---|---|---|
| `mcc_tgttos_badlands` | 10,−42,31 | 9,−41,31, wall, facing east | `tgttos`, format 10 | **no** | **yes** (`file/tgttos`) |
| `mcc_tgttos_industry` | 73,−33,11 | 73,−32,10, wall, facing south | `Industry`, format 10 | **no** | **yes** (`file/Industry`) |
| `mcc_tgttos_pits` | 15,−2,36 | 14,−1,36, wall, facing east | `Pits`, format 10 | **no** | **yes** (`file/Pits`) |

`level.dat` DataPacks for each: `Enabled=[vanilla,file/bukkit,paper]`, `Disabled=[bundle,trade_rebalance]`. The map pack is not in Enabled. The source archives' `level.dat` had `file/tgttos` / `file/Industry` / `file/Pits` enabled. `(H)`

`scoreboard.dat` still contains the objectives from the source save (Badlands: `start`, `countdown`, `playing`, `finished`, `hit_chicken`, etc.; Industry/Pits: `countdown`, `color`, `selectColor`, `playing`, `finished`, `respawn`, etc.). That does **not** make the maps run: no tick function is active. `(H)`

**Verdict:** **not runnable as imported**. Start signs and their `clickEvent` NBT survive the 1.21.1 upgrade `(H)`, but the start command only sets a score/tag and no function consumes it `(H)`. Whether the client still executes sign click events was not live-tested `(M)`.

---

## 4. 1.21.1 command-block audit

### 4.1 Method

- Read every command-block block entity from the four named worlds: **322 + 67 + 29 + 37 = 455** `(H)`.
- Decoded the block-state palette for mode (impulse/chain/repeating), facing, conditional; read `Command`, `auto`, `powered`, `LastExecution`, `LastOutput`, `TrackOutput` `(H)`.
- Checked each command against 1.21.1 syntax and against the 1.20.5 item-component and 1.21 folder changes `(H/M)`.
- Did **not** execute any command. Commands marked FAIL are syntax/format failures; RISK is a behaviour hazard, not a version failure.

Full per-block export: `data/commands_risk.csv` (455 rows). Summary:

| World | Total | FAIL | RISK | OK |
|---|---:|---:|---:|---:|
| Clouds 2 | 322 | 31 | 6 | 285 |
| City Marathon | 67 | 7 | 0 | 60 |
| Space Race | 29 | 0 | 2 | 27 |
| Yeti Set Go | 37 | 3 | 0 | 34 |
| **Total** | **455** | **41** | **8** | **406** |

### 4.2 Exact failures

#### A. Clouds 2 — 30 blocks, pre-1.20.5 item NBT `(H)`

Six boot-pad clusters, five command blocks each, all repeating/always-active:

| Cluster (x,y,z range) | Count |
|---|---:|
| −174,−38,−204 … −200 | 5 |
| −1,−12,−136 … −132 | 5 |
| −152,−51,184 … 180 | 5 |
| 43,6,−280 … −276 | 5 |
| 0,−8,−17 … −13 | 5 |
| 37..41,−38,233 | 5 |

Exact command (each block differs only in the relative `~ ~-N ~` offset):

```mcfunction
execute as @a at @s if block ~ ~-1 ~ minecraft:red_terracotta run item replace entity @a armor.feet with leather_boots{display:{color:16777215},Enchantments:[{id:depth_strider,lvl:255}],HideFlags:1} 1
```

Why it fails: 1.20.5 (data pack format 41) replaced unstructured item NBT with structured **item components**; 1.21.1's item argument expects `item_id[component=value,…]`, not `item_id{display:…}`. The whole `execute … run item …` command fails to parse, so the boots are never equipped. `(H)` The six pads are therefore cosmetic-only; the player keeps whatever boots they had. `(M)`

#### B. Clouds 2 — 1 malformed chain block `(H)`

```mcfunction
setblock 5 4 22
```

at **−49,1,27**, chain command block, `auto=1`, `conditional=false`. The block argument is missing. It is a parse error in 1.20.1 too, so this is a pre-existing bug, not a 1.21 regression. It is part of the RESET COURSE chain; it likely intended to restore the spawn button at 5,4,22, but that intent is an inference. `(H parse error; M intent)`

#### C. City Marathon — 7 no-target `/gamemode adventure` `(H)`

Coordinates:

```
(-387,-8,45)  (-387,-8,47)
(-208,-2,87)  (-208,-2,86)
(11,-2,-4)    (11,-2,-6)
(7,-3,3)
```

Exact command:

```mcfunction
gamemode adventure
```

There is no target selector. A command block's command source is the block, not the player who pressed the button; the command fails with `permissions.requires.player`. `LastOutput` directly confirms this on (−387,−8,45), (−208,−2,87), and (11,−2,−6). `(H)` Result: players are never put into adventure mode by the map. `(H)`

#### D. Yeti Set Go — 3 no-target `/gamemode adventure` `(H)`

```
(24,-3,-6)  (24,-3,-8)  (20,-4,1)
```

Same command and same failure. `LastOutput` at (24,−3,−8) is `permissions.requires.player`. `(H)`

#### E. Not failures but notable

- **Leading `/` in command-block strings is accepted**: City's `/gamemode adventure` block reached the player-target check (`permissions.requires.player`), not an “Unknown command” error. `(H)`
- **Clouds 2 1.20.1-era `infinite` effect durations** and `255` amplifiers are valid on 1.21.1. `(H)`
- **`tp @a 5.5 4 24.5 facing 5 5 -20`** and **`setworldspawn`** with no position are valid. `(H)`
- **`fill … replace <block>`**, `item replace` with plain items (`air`, `minecraft:elytra`, `minecraft:trident`), `effect clear`, `enchant`, `title`, `playsound`, `stopsound`, `xp set`, `clear`, `kill @e[type=item]` all parse in 1.21.1. `(H/M)`

### 4.3 Behaviour risks (not 1.21 syntax)

#### Global kill planes — Clouds 2, 6 blocks `(H)`

```mcfunction
execute as @a at @a if entity @a[y=-60,dy=0] run kill @p
```

at (−174,−38,−198), (−1,−12,−130), (−152,−51,186), (43,6,−274), (0,−8,−11), (35,−38,233).

The `if entity @a[y=-60,dy=0]` is global: if **any** player is at y=−60, the command runs once for every player, and `kill @p` selects the nearest player (normally the executing player). In practice one player falling to the kill plane can kill the whole lobby. `(H command semantics; M multiplayer impact)`

#### Global kill planes — Space Race, 2 blocks `(H)`

```mcfunction
execute at @a if block ~ ~-0.35 ~ minecraft:bedrock run kill @a
execute at @a if block ~ ~-0.35 ~ minecraft:lime_stained_glass run kill @a
```

at (21,112,16) and (23,112,16). One player touching bedrock or lime stained glass triggers `kill @a` for all players. `(H)`

#### Other command-block state

- All 455 blocks have `powered=0` at rest; none is stuck powered. `(H)`
- `conditional=false` for all 455; there are no conditional chains. `(H)`
- Clouds 2: 298/322 always-active; 24 need redstone. City: 62/67 always-active. Space: 22/29. Yeti: 32/37. `(H)`
- No block has an empty `Command` in these four worlds. `(H)`

---

## 5. Datapack audit — TGTTOS Badlands / Industry / Pits

### 5.1 What 1.21.1 expects

- **Data pack format 48** for 1.21–1.21.1 `(H)`: <https://minecraft.wiki/w/Pack_format> / <https://minecraft.wiki/w/Java_Edition_1.21.1>.
- 1.20.5 (format 41) replaced item NBT with **item components** `(H)`.
- 1.21 (format 48) renamed legacy folders, including **`functions` → `function`** and **`tags/functions` → `tags/function`** `(H)`. The local 1.21.1 Paper jar confirms singular data folders (`data/minecraft/advancement`, `loot_table`, `recipe`, `structure`, `tags/block`, `tags/item`) `(H)`.

### 5.2 State as imported

| World | Pack | `pack.mcmeta` | Actual function path | Expected 1.21.1 path | Functions / lines | Old item-NBT lines | Enabled now | Source enabled |
|---|---|---|---:|---|---:|---:|---|---|
| Badlands | `tgttos` | 10 | `data/tgttos/functions` | `data/tgttos/function` | 2 / 120 | 3 | **no** | **yes** |
| Industry | `Industry` | 10 | `data/tgttos/functions` | `data/tgttos/function` | 7 / 251 | 21 | **no** | **yes** |
| Pits | `Pits` | 10 | `data/tgttos/functions` | `data/tgttos/function` | 6 / 219 | 21 | **no** | **yes** |

Each pack also has `data/minecraft/tags/functions/load.json` and `tick.json`; 1.21.1 looks for `data/minecraft/tags/function/load.json` and `tick.json`. `(H)`

`level.dat` currently has `Enabled=[vanilla,file/bukkit,paper]` for all three; the map packs are not in the list. `(H)`

### 5.3 Why nothing runs

1. **Not enabled.** Even with perfect syntax, no pack is loaded. `(H)`
2. **Old folder names.** If enabled, 1.21.1 would not discover the functions or load/tick tags because they are under `functions` and `tags/functions`. `(H)`
3. **Old item components.** The item-granting functions use pre-1.20.5 NBT, e.g.:

```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:red_concrete",…],Unbreakable:1b,Enchantments:[{id:"minecraft:efficiency",lvl:2s}]}
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:["minecraft:red_concrete",…]} 2
```

1.21.1 rejects `{CanDestroy:…}`, `{CanPlaceOn:…}`, `{Unbreakable:…}` and `{Enchantments:…}` in an item argument. These lines would fail even after the folder rename. `(H)`
4. **The countdown never advances.** Badlands' Start sign runs `scoreboard players add @s start 1`; Industry/Pits run `tag @a add starting`. Without the tick function, nothing reads those values. `(H)`

### 5.4 Per-map effect

- **Badlands.** Start sign at 9,−41,31; the `clickEvent` is on the **blank first line**, not on the visible `"Start"` line (message 1) `(H)`. The objective `start` still exists in `scoreboard.dat`, so the command would succeed **if the click fires**; `loop.mcfunction` never runs, so there is no countdown, no `playing=1`, no barrier open, no finish detection. `(H command; M click target)`
- **Industry.** Start sign at 73,−32,10; click adds `starting`, but `start_timer.mcfunction` is never ticked. No countdown, no `playing`, no barrier open, no finish. `(H)`
- **Pits.** Start sign at 14,−1,36; same as Industry. `(H)`
- **Saved gamerules partially mask the failure.** Industry/Pits `level.dat` already has `keepInventory=true`, `fallDamage=false`, etc. from the source save; Badlands does not (`keepInventory=false`, `doImmediateRespawn=false`). This does not restore the game loop. `(H)`

### 5.5 Minimal porting checklist (not executed)

If these maps are ever ported: `(M)`

1. Rename `data/<ns>/functions` → `data/<ns>/function`; `data/minecraft/tags/functions` → `data/minecraft/tags/function`.
2. Set `pack.mcmeta` `pack_format` to 48 (or add `supported_formats` covering 10–48 if the loader accepts it).
3. Rewrite all 45 old item-NBT lines to item components, e.g. `red_concrete[minecraft:can_place_on={predicates:[…]}]`, `iron_pickaxe[minecraft:can_break=…,minecraft:unbreakable={},minecraft:enchantments={levels:{"minecraft:efficiency":2}}]`.
4. Enable the pack in `level.dat` (or `/datapack enable` through an authorised operator; not done here).
5. Re-check `scoreboard objectives add` duplicates: the saved objectives already exist, so load-function lines will log “already exists” but should not be fatal.
6. Test the Start sign, countdown, respawn and finish with one player.

---

## 6. Resource-pack compatibility

### 6.1 Target format

- 1.21.1 resource pack format: **34** `(H)`: <https://minecraft.wiki/w/Java_Edition_1.21.1>.
- 1.21.1 data pack format: **48** `(H)`.

### 6.2 Packs found

| World | File | `pack_format` | Valid pack? | Contents | Verdict |
|---|---|---:|---|---|---|
| Clouds 2 | `resources.zip` | 15 (1.20.x) | yes, root-level | 100 entries: block models/textures for glazed terracotta, rotten flesh item, animated textures, `sounds.json`, `en_us.json` | needs format bump to 34; models/textures are simple and should survive |
| City Marathon | `advancements.zip` | — | **not a pack** | full world backup: `region/`, `entities/`, `level.dat`, `data/lod/` | do not load as a pack |
| Space Race | `resources.zip.zip` | 6 (1.16.x) | **no** | 46 entries under `Space Race Resource Pack/`, including `desktop.ini` | one zip with a wrapper folder, so no root `pack.mcmeta`; repack flat and bump to 34 |
| Yeti Set Go | `resources.zip` | 9 (1.19.x) | yes, root-level | 32 entries: same custom block models/textures, `en_us.json` | needs format bump to 34 |
| DecisionDome | — | — | — | none | — |
| Minigames | — | — | — | none | — |
| TGTTOS ×3 | — | — | — | none | — |

### 6.3 Specific pack problems

- **Clouds 2 `sounds.json` references missing files.** It declares `acerace.start` and `acerace.checkpoint`, but `assets/minecraft/sounds/acerace/` contains only `jumppad.ogg`, `megajumppad.ogg`, `speed.ogg`, `waterboost.ogg`. `start.ogg` and `checkpoint.ogg` are absent `(H)`. No command block in Clouds 2 uses `acerace.*` sounds (they use vanilla `block.*` / `music_disc.far`), so the missing files are cosmetic only. `(H)`
- **Space Race pack is structurally invalid.** A resource pack must have `pack.mcmeta` at the zip root; this one has `Space Race Resource Pack/pack.mcmeta`. The file is also named `resources.zip.zip`, which the server does not treat as a world pack. `(H)`
- **City `advancements.zip` is a world backup**, not a resource pack; its 522 entries include `region/*.mca` and `level.dat`. It must not be sent as a pack. `(H)`
- **No global pack is enabled:** `resource-pack=` empty, `require-resource-pack=false`. Clients currently see vanilla textures; the maps' command blocks test vanilla block IDs, so gameplay logic does not depend on the pack. `(H)`

### 6.4 What the server would need

1. Repack each valid pack with `pack.mcmeta` at the zip root and `pack_format: 34`.
2. Host it at an HTTPS URL; set `resource-pack=` and `resource-pack-sha1=` in `server.properties`.
3. Or use a per-world resource-pack plugin. Multiverse 5.8.2 does not appear to send a world's `resources.zip` on a world switch; the vanilla `resources.zip` is a world pack and the server's global `resource-pack` is what clients are sent. `(M)`
4. Do not set `require-resource-pack=true` until the pack is verified; a failed pack download would block players.
5. For Space Race, flatten the nested folder and rename to `resources.zip` before any hosting.
6. For Clouds 2, either add the two missing OGGs or remove the dead `sounds.json` entries.

**No pack was enabled or uploaded in this pass.** `(H)`

---

## 7. Map start instructions (player/op)

Common rules for all worlds:

- Enter with `/mv tp <world>` (or `/mvtp <world>`) as an op; the world names are exactly `mcc_clouds2`, `mcc_citymarathon`, `mcc_spacerace`, `mcc_yetisetgo`, `mcc_decisiondome`, `mcc_minigames`, `mcc_tgttos_badlands`, `mcc_tgttos_industry`, `mcc_tgttos_pits`. `(H)`
- Multiverse forces **survival** for regular players; survival/adventure players cannot fly, but players already in creative keep flight. Use spectator or creative for fly-throughs. `(H config/code; M player effect)`
- `keep-spawn-in-memory` is true, so start-area command blocks tick without a nearby player. `(H)`

### 7.1 Clouds 2 — `mcc_clouds2`

- Spawn: **5,4,23** on snow. `(H)`
- Start/reset: wall stone button at **−3,4,25**, directly under the `RESET COURSE` sign at −3,5,25. `(H)`
- Spawn-side button: floor stone button at **5,4,22**, one block north of spawn; command block at 3,2,22 gives a trident. The button and the block exist, but the redstone path was not fully traced; treat “press for trident” as **M**, not H. `(M)`
- Pressing RESET runs the reset fills, `clear @a`, countdown titles, and finally `tp @a 5.5 4 24.5 facing 5 5 -20`. Expect all players to be pulled to the start line. `(H command; M runtime)`
- 1.21.1 caveats: the six boot pads do nothing (30 failed commands); one reset action (`setblock 5 4 22`) fails; the kill plane can kill the whole lobby. `(H)`

### 7.2 City Marathon — `mcc_citymarathon`

- Spawn: **8,0,5** on gray wool. `(H)`
- Start: polished blackstone **floor button at 8,0,8**, three blocks south of spawn. No sign. `(H)`
- Pressing it starts the countdown title chain and resets the start-area fill at z=10. There is no teleport; players start where they stand. `(H commands; M sequence)`
- 1.21.1 caveat: the 7 `/gamemode adventure` blocks fail; regular players remain in survival and can break the course. `(H)`

### 7.3 Space Race — `mcc_spacerace`

- Spawn: **17,115,27** on gray concrete powder. `(H)`
- Start: wall stone button at **6,116,28**, next to the `START ->` sign at 6,116,29. `(H)`
- Pressing it starts the countdown and clears the glass wall `fill 28 115 24 6 117 24 air`. There is no teleport. `(H)`
- 1.21.1 caveat: no syntax failures found; the two kill planes can kill all players; the resource pack is invalid; mob spawning/fire are enabled by gamerule. `(H)`

### 7.4 Yeti Set Go — `mcc_yetisetgo`

- Spawn: **21,−1,4** on gray concrete. `(H)`
- Start: polished blackstone **floor button at 21,−1,6**, two blocks south of spawn. No sign. `(H)`
- Pressing it starts the countdown and clears the start gate `fill 16 -1 7 26 1 7 air`. No teleport. `(H)`
- 1.21.1 caveat: 3 `/gamemode adventure` blocks fail; players remain survival. `(H)`

### 7.5 Decision Dome — `mcc_decisiondome`

- Spawn: **0,76,0** on lime concrete; Multiverse adjusted from `0,4,0`. `(H)`
- No start button, sign, timer, or game logic. This is a build-only world. `(H)`
- Two leftover command blocks at −3,68,−19 (`give @p egg`, `effect give @p glowing`) are not a start mechanism. `(H)`

### 7.6 Minigames — `mcc_minigames`

- Spawn: **−196,24,927** on netherite block; a hub with signs around it. `(H)`
- Sign/labels within ~14 blocks: Sky Battle, Parkour Tag, Super Clash, Gladiators (maintenance), Theme Race (event only), Sky Race, Guest's Minigames (closed). `(H)`
- No single global start at spawn; arena entry is via the hub routes, which A6 owns. Do not assume a button/command will start an arena. `(H/M)`
- 942 command blocks exist; 6 were powered at read time. Do not test blindly. `(H)`

### 7.7 TGTTOS Badlands / Industry / Pits

All three have a wall sign 1.4 blocks from spawn. The `clickEvent` is on the **top blank line**, not on the visible word `Start`:

| World | Sign | Click command | Current effect |
|---|---|---|---|
| Badlands | 9,−41,31, facing east | `scoreboard players add @s start 1` | objective `start` exists, but no tick; nothing starts `(H)` |
| Industry | 73,−32,10, facing south | `tag @a add starting` | tag is added, but no tick; nothing starts `(H)` |
| Pits | 14,−1,36, facing east | `tag @a add starting` | tag is added, but no tick; nothing starts `(H)` |

The sign text and `clickEvent` survive in the 1.21.1 block entity NBT `(H)`. The `clickEvent` is attached to the blank first line, while the visible `"Start"` word is the second line with no click event; clicking the visible word therefore does nothing, and whether a blank sign line can receive the click was not tested `(H NBT; M click behaviour)`.

**Do not advertise these maps as playable until the datapack port in Section 5.5 is done.** `(H)`

---

## 8. Known broken vs safe to test

### 8.1 Confirmed failures (H)

| # | World | What fails | Exact trigger | Effect |
|---:|---|---|---|---|
| 1 | Clouds 2 | 30 × pre-1.20.5 item NBT | stand on the six red-terracotta boot pads | no depth-strider boots; command parse error |
| 2 | Clouds 2 | `setblock 5 4 22` malformed | RESET COURSE chain at −49,1,27 | intended button reset never happens |
| 3 | City Marathon | 7 × `/gamemode adventure` no target | start/checkpoint sequences | `permissions.requires.player`; players stay survival |
| 4 | Yeti | 3 × `/gamemode adventure` no target | start/checkpoint sequences | same |
| 5 | TGTTOS Badlands | datapack disabled + pre-1.21 folder layout | click Start sign | no countdown/playing/finish |
| 6 | TGTTOS Industry | datapack disabled + pre-1.21 folder layout | click Start sign | no countdown/playing/finish |
| 7 | TGTTOS Pits | datapack disabled + pre-1.21 folder layout | click Start sign | no countdown/playing/finish |
| 8 | Space Race | `resources.zip.zip` invalid | any attempt to use the world pack | pack not loaded |
| 9 | Clouds 2 | two missing OGGs | `acerace.start` / `acerace.checkpoint` playback | silent events; no command uses them |
| 10 | DecisionDome | no game logic | enter world | build-only; 2 leftover blocks not a start |

### 8.2 Likely risks (M)

- **Survival enforcement.** Multiverse `enforce-gamemode: true` + `gamemode: survival` means normal players can break blocks and take PvP damage; the maps' adventure commands do not work. `(H config; M player impact)`
- **Flight enforcement.** Multiverse `allow-flight: false` + `enforce-flight: true` clears flight for survival/adventure players; players already in creative keep it. Elytra glide is separate and probably still works, but confirm with one player. `(H config/code; M elytra)`
- **Clouds 2 kill plane** can kill the entire lobby when one player falls to y=−60. `(H semantics; M multiplayer impact)`
- **Space Race kill planes** kill all players when one touches bedrock/lime glass. `(H semantics)`
- **Space Race gamerules** allow mobs, fire, drops and random ticks; this is not a race-clean ruleset. `(H)`
- **Badlands death handling**: `keepInventory=false`, `doImmediateRespawn=false`; with the datapack disabled, a death drops inventory and opens the respawn screen. `(H)`
- **Per-world resource packs** are not automatically delivered by Multiverse; a global `resource-pack` or plugin is needed. `(M)`
- **Pack formats 15/9/6** are below 34; they will be flagged incompatible and may not load without a bump. `(H format; M exact client behaviour)`
- **Watchdog save stall** at 00:05:48; the server recovered, but heavy world saves can pause the review server. `(H)`

### 8.3 Safe-to-test list (single op, no other players)

1. **DecisionDome** — safest; build only; no logic to break. `(H)`
2. **Clouds 2 start/reset** — press RESET COURSE at −3,4,25; expect countdown and teleport; boot pads will not work; do not use the kill plane with other players. `(H/M)`
3. **City Marathon start** — press 8,0,8; expect countdown; players remain survival. `(H)`
4. **Yeti start** — press 21,−1,6; expect countdown; players remain survival. `(H)`
5. **Space Race start** — press 6,116,28; stand clear of the bedrock/lime kill planes; pack not applied. `(H/M)`
6. **TGTTOS look-only** — enter, inspect spawn/signs; do not expect clicking Start to run the game. `(H)`
7. **Minigames hub only** — enter and look at signs; do not enter arenas until A6 publishes bounds. `(H/M)`

### 8.4 Do not do in this review pass

- Do not enable the TGTTOS datapacks, the DecisionDome VanillaTweaks packs, or any resource pack. `(H)`
- Do not press RESET with multiple players on Clouds 2, or cross the Space Race kill planes with a group. `(H)`
- Do not treat Minigames' 942 command blocks as audited. `(H)`

---

## 9. Corrections to the plan / out-of-scope notes

1. **DecisionDome is not 0 command blocks.** Source archive and imported world both contain **2** block entities at −3,68,−19. `(H)`
2. **Minigames is not 0 command blocks.** Source archive and imported world both contain **942** block entities (182 impulse, 548 chain, 212 repeating; 6 powered). `(H)`
3. **DecisionDome spawn was not preserved.** Source `level.dat` says 0,4,0; Multiverse changed it to 0,76,0 because 0,4,0 was unsafe. `(H)`
4. **City `advancements.zip` is not a resource pack**; it is a world backup. `(H)`
5. **Space Race pack is `resources.zip.zip`**, not `resources.zip`, and has a wrapper folder at the zip root, so `pack.mcmeta` is not at root. `(H)`
6. **`zipped-map.zip` = `YetiSetGo.zip`** is byte-identical per the plan; A4 owns the duplicate analysis. `(M)`

---

## 10. Open questions

1. Does Multiverse's `allow-flight=false` actually affect **elytra glide**, or only creative/survival flight? The code path clears `allowFlight`/`isFlying` for non-creative players; elytra uses fall-flying, so it should be unaffected. Live test needed. `(M)`
2. Does Paper send a world's `resources.zip` to clients on a Multiverse world switch? No evidence in this pass; assume it does not. `(M)`
3. Are the 30 Clouds 2 boot-pad commands intended to be rewritten, or removed? That is a map-owner decision; not done here. `(M)`
4. Are the 7 City + 3 Yeti gamemode blocks intended to target `@a`? The fix is obvious (`gamemode adventure @a`), but this is a map edit and out of scope. `(M)`
5. Why does Minigames have 942 command blocks while the plan says 0? A6 must map them; A12 did not. `(H count; open interpretation)`
6. Are the six Clouds 2 kill-plane commands intentionally global, or a bug? The command semantics say global; intent unknown. `(H semantics; L intent)`

---

## 11. Files in this folder

```
README.md                 this dossier
STATUS.md                 done / blocked / open
data/worlds.csv           9 worlds, load/spawn/config/command-block/datapack/pack summary
data/commands_risk.csv    455 command blocks, exact command, risk, confidence, reason
data/datapacks.csv        the three TGTTOS datapacks, format/layout/state/line counts
_work/scan.json           raw NBT scan (level, command blocks, signs) for all nine worlds
_work/scan_worlds.py      read-only region/NBT scanner
_work/block_lookup.py     read-only block lookup from region files
_work/import_log.txt      import-window log excerpt (23:2x–23:3x)
_work/*.py                supporting analysis scripts
_work/unique_*.txt        unique command lists for the four audited worlds
```

No images are included: A12's deliverable is compatibility/import evidence, not map art or route documentation. A1–A11 own the visual dossiers. `(H)`
