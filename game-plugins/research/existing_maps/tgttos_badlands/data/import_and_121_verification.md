# Import state + 1.21.1 verification — Badlands `tgttos` pack

**Read-only verification of A12's finding, 2026-11-25.** No pack, world, `level.dat`, `scoreboard.dat` or datapack file was edited, enabled, ported, moved or reloaded. No RCON. The imported world and the source archive were read directly from disk.

Evidence key: **H** = directly observed in the file/NBT; **M** = inference from two sources; **L** = guess.
Machine-readable copy of every value below: **`data/import_state.json`**.

---

## 1. Verdict on A12's two claims

| A12 claim | What I observed | Verdict |
|---|---|---|
| The imported Badlands `level.dat` has the `tgttos` datapack **disabled**. | Imported `mcc_tgttos_badlands\level.dat` → `Data.DataPacks.Enabled = ["vanilla","file/bukkit","paper"]`; `Disabled = ["bundle","trade_rebalance"]`. **`file/tgttos` is in neither list.** The pack is on disk at `<world>\datapacks\tgttos`, but it is not loaded. A12's "disabled" is correct in effect; precisely it is **not enabled** (it is not listed in `Disabled` either). | **CONFIRMED**, with that precision. [H] |
| The source archive had it **enabled**. | I read the source zip itself (`C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS\tgttos-badlands-by-mickeykat.zip`) and its `TGTTOS Badlands - By MickeyKat/level.dat` (gzip-compressed inside the zip): `DataPacks.Enabled = ["vanilla","file/tgttos"]`; `Disabled = []`; DataVersion 3120; GameType 1. The extracted source world matches. | **CONFIRMED**. [H] |
| The pack uses `data/.../functions` + `tags/functions`; 1.21.1 expects `function` + `tags/function`. | Both source and imported pack trees are `data/tgttos/functions/load.mcfunction`, `loop.mcfunction`; `data/minecraft/tags/functions/load.json`, `tick.json`. No `function` / `tags/function` folders exist. | **CONFIRMED**. [H] |
| `pack_format` 10, 1.21.1 expects **48**. | Both `pack.mcmeta` files: `"pack_format": 10`, description "Made by Mickey_Kat". 1.21–1.21.1 data pack format is 48. | **CONFIRMED**. [H] |
| 2 functions / **120** lines. | 2 functions. `load.mcfunction` = 8 lines; `loop.mcfunction` = 130 lines (113 non-empty). Total 138 lines, **121 non-empty** (8 + 113). The imported files are **byte-identical** to the source (all 5 files, SHA-256 match). A12's 120 differs by one from my 121 non-empty; the files are byte-identical, so this is a counting difference, not a content difference. | **CONFIRMED except the line count** (120 vs 121 non-empty). [H] |

A12's other relevant findings are also confirmed directly: the imported world is DataVersion **3955** (1.21.1), `LevelName` = `mcc_tgttos_badlands`, `GameType` = **0 (Survival)**, spawn `10,−42,31`, `scoreboard.dat` still has the 9 objectives, and the start sign survived the upgrade as `front_text.messages` with the clickEvent preserved on **empty line 1** (visible "Start" is line 2). [H]

---

## 2. Exact load/build path

### 2.1 Source archive (1.19.2)

Source zip (read directly): `C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS\tgttos-badlands-by-mickeykat.zip` (3,601,701 bytes). Inside it, `TGTTOS Badlands - By MickeyKat/level.dat` is gzip-compressed; decompressed and parsed, it has `DataVersion=3120`, `LevelName=TGTTOS Badlands Practise`, `GameType=1`, `DataPacks.Enabled=[vanilla, file/tgttos]`, `DataPacks.Disabled=[]`. The extracted source world I also read is `...\TGTTOS Badlands - By MickeyKat` (same map, same state). The archive contains the plural pack paths (`datapacks/tgttos/data/tgttos/functions/...`, `datapacks/tgttos/data/minecraft/tags/functions/...`). [H]

```
...\TGTTOS Badlands - By MickeyKat\level.dat
...\TGTTOS Badlands - By MickeyKat\datapacks\tgttos\pack.mcmeta
...\TGTTOS Badlands - By MickeyKat\datapacks\tgttos\data\tgttos\functions\load.mcfunction
...\TGTTOS Badlands - By MickeyKat\datapacks\tgttos\data\tgttos\functions\loop.mcfunction
...\TGTTOS Badlands - By MickeyKat\datapacks\tgttos\data\minecraft\tags\functions\load.json
...\TGTTOS Badlands - By MickeyKat\datapacks\tgttos\data\minecraft\tags\functions\tick.json
```

- `level.dat DataVersion` = **3120**; `LevelName` = `TGTTOS Badlands Practise`; `GameType` = **1 (Creative)**; `allowCommands=1`; spawn `10,−42,31`; `DataPacks.Enabled = [vanilla, file/tgttos]`; `Disabled = []`. [H]
- Datapack ID in the save: **`file/tgttos`**. The `file/` prefix is the world-datapack namespace; `tgttos` is the folder name under `datapacks/`. [H/M]
- Load sequence (1.19.2): the world reads `DataPacks.Enabled` → finds `datapacks/tgttos/pack.mcmeta` → scans `data/tgttos/functions/*.mcfunction` → reads `data/minecraft/tags/functions/load.json` (`tgttos:load`) and `tick.json` (`tgttos:loop`) → runs `tgttos:load` once at world load and `tgttos:loop` at the start of every tick. [H/M]
- The world is a void superflat (`the_void`, air layers); the island is hand-built in `region/*.mca`; there is no structure/loot/recipe data in the pack. [H]

### 2.2 Imported live world (1.21.1)

```
C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review\mcc_tgttos_badlands\level.dat
...\mcc_tgttos_badlands\datapacks\tgttos\pack.mcmeta
...\mcc_tgttos_badlands\datapacks\tgttos\data\tgttos\functions\load.mcfunction
...\mcc_tgttos_badlands\datapacks\tgttos\data\tgttos\functions\loop.mcfunction
...\mcc_tgttos_badlands\datapacks\tgttos\data\minecraft\tags\functions\load.json
...\mcc_tgttos_badlands\datapacks\tgttos\data\minecraft\tags\functions\tick.json
...\mcc_tgttos_badlands\data\scoreboard.dat
```

- `level.dat DataVersion` = **3955** (1.21.1); `LevelName` = `mcc_tgttos_badlands`; `GameType` = **0 (Survival)**; `allowCommands=1`; Difficulty 0 (Peaceful); spawn `10,−42,31`. [H]
- `DataPacks.Enabled = ["vanilla","file/bukkit","paper"]`; `Disabled = ["bundle","trade_rebalance"]`. **`file/tgttos` absent from both.** [H]
- Multiverse entry `minecraft:mcc_tgttos_badlands` in `plugins/Multiverse-Core/worlds.yml`: `gamemode: survival`, `difficulty: peaceful`, `allow-flight: false`, `keep-spawn-in-memory: true`, `auto-load: true`, `legacy-world-name: mcc_tgttos_badlands`. [H]
- The pack on disk is **byte-identical** to the source: `pack.mcmeta`, both tag JSONs and both `.mcfunction` files hash the same (SHA-256 prefixes `13B562281DEA`, `2AE1D09977EA`, `538179953EEB`, `552F3D2F8EA3`, `649999A8AEA1`); full source hashes are in `data/source_hashes.csv`. [H]
- Gamerules survived the upgrade: `fallDamage=false`, `doDaylightCycle=false`, `doWeatherCycle=false`, `keepInventory=false`, `doImmediateRespawn=false`, `spawnRadius=10`, `doMobSpawning=false`, `randomTickSpeed=0`, `showDeathMessages=false`, `doTileDrops=false`. [H]
- `scoreboard.dat` objectives: `countdown`, `start`, `playtimeS`, `hit_chicken`, `playtimeT`, `playing`, `finished`, `playtime`, `spawntime` (9). [H]
- Start sign (imported block entity): `minecraft:sign` at `9,−41,31`, `front_text.color=black`, `has_glowing_text=0`, `is_waxed=0`, messages:
  1. `{"clickEvent":{"action":"run_command","value":"scoreboard players add @s start 1"},"text":""}` ← **clickEvent on empty line 1**
  2. `{"bold":true,"color":"green","text":"Start"}` ← visible line, no clickEvent
  3. `""`
  4. `""` [H]

### 2.3 Why the import difference matters

There are **two independent gates**, in this order:

1. **Registration gate (imported world):** `file/tgttos` is not in `DataPacks.Enabled`, so the pack is never loaded. A12 called this "disabled"; the effect is the same, but the exact fix is to **add** `file/tgttos` to `Enabled` — it is not in `Disabled`, so there is nothing to remove from that list. [H]
2. **Layout/format gate (both worlds):** even after enabling, 1.21.1 scans `data/tgttos/function/` and `data/minecraft/tags/function/`; the pack only has `functions/` and `tags/functions/`. `pack_format` 10 also declares 1.19.2 rather than 1.21.1's 48. [H]

So enabling alone gives an enabled-but-empty pack; renaming alone leaves the pack unloaded. Both are needed. The 3 old item-NBT lines are a third, independent failure after both are fixed. [H/M]

---

## 3. 1.21.1 fix requirements (requirements only — **not applied**)

### 3.1 Minimum to make the pack load and grant items

1. **Enable it in the imported world.** Add `"file/tgttos"` to `Data.DataPacks.Enabled` in `mcc_tgttos_badlands\level.dat` **with the server stopped** (back up `level.dat` first), or run `/datapack enable "file/tgttos"` in the `mcc_tgttos_badlands` world context as an operator. It is **not** in `Disabled` (`[bundle,trade_rebalance]`), so no removal is required. Do not edit `level.dat` while the server is running; it will be overwritten. [H/M]
2. **Rename the pack folders:**
   - `data/tgttos/functions/` → `data/tgttos/function/`
   - `data/minecraft/tags/functions/` → `data/minecraft/tags/function/`
   The JSON values stay `tgttos:load` and `tgttos:loop`; only the folder names change. [H]
3. **Update `pack.mcmeta`:** `"pack_format": 10` → `"pack_format": 48` (1.21–1.21.1). A `supported_formats` range can be used instead, but 48 is the target. The description can stay. [H]
4. **Rewrite the 3 old item-NBT lines** (§4). The old braces are rejected by 1.21.1 even after the folder rename. [H]

### 3.2 Needed for the map's intended behaviour (not just loading)

5. **Adventure mode.** The imported world is `GameType=0` (Survival) and Multiverse `gamemode: survival`; the source's saved player was Adventure. `CanPlaceOn`/`CanDestroy` only apply in Adventure. In Survival, the 44-block allow-list is ignored, players can place concrete anywhere and can break the build. Set the world/Multiverse gamemode to **adventure** (or have the game wrapper set it at start). The pack only sets Adventure after a finish (line 45). [H/M]
6. **Start sign click target.** The clickEvent is on **empty line 1**; the visible "Start" is line 2 with no clickEvent. The 1.21.1 upgrade preserved the JSON, but whether the client executes a click on an empty line was not live-tested (A12: M). If it does not fire, move the clickEvent to the visible "Start" message (line 2). This is an asset edit, not a format requirement. [H NBT; M client behaviour]
7. **Objectives already exist.** `scoreboard.dat` has all 9; the `load` function's `scoreboard objectives add` lines will log "already exists" but are not fatal. Do not remove the objectives; the sign's `start` objective exists. On a fresh world the load function would create them. [H/M]

### 3.3 Not required on 1.21.1

- `clickEvent`/`value` is the correct syntax up to 1.21.4; the `click_event`/`command` rename is 1.21.5. The server is 1.21.1, so the sign JSON needs no text-component change now. [H/M]
- `title` JSON text components are accepted on 1.21.1; the SNBT rewrite is 1.21.5. [H/M]
- `summon armor_stand {NoGravity:…,Tags:[…]}` is entity NBT, still valid. [H/M]
- `fill`, `effect`, `particle`, `playsound`, `clear`, `tp`, `gamemode`, `kill` and the `scoreboard` commands are valid. [H/M]

### 3.4 What the port does **not** fix

Even with all of §3.1–3.2, the map remains a single-player practice map: no respawn handler, no checkpoints, finish-only block/cactus cleanup, `@a` countdown scaling, one finisher teleports/resets everyone, global block cleanup, 4 cacti missing from the reset, no results/points/round loop. The current `McmTgttos` plugin provides that layer for its own maps; this pack does not. [H/M]

---

## 4. The old item-NBT lines (exact, source and imported are identical)

All three are in `data/tgttos/functions/loop.mcfunction`. They run every tick. [H]

**Line 5** — offhand blue concrete, count 2, 44-entry `CanPlaceOn`:

```mcfunction
item replace entity @a weapon.offhand with blue_concrete{CanPlaceOn:["minecraft:blue_concrete","minecraft:gray_wool","minecraft:jungle_planks","minecraft:jungle_fence","minecraft:red_wool","minecraft:light_gray_concrete_powder","minecraft:gray_concrete","minecraft:nether_brick_wall","minecraft:red_concrete","minecraft:sea_lantern","minecraft:chain","minecraft:red_terracotta","minecraft:diorite","minecraft:white_terracotta","minecraft:white_concrete","minecraft:cactus","minecraft:soul_sand","minecraft:red_sand","minecraft:cyan_concrete_powder","minecraft:smooth_stone","minecraft:stripped_spruce_log","minecraft:acacia_leaves","minecraft:gravel","minecraft:diorite_wall","minecraft:nether_brick_slab","minecraft:iron_trapdoor","minecraft:gray_terracotta","minecraft:andesite","minecraft:red_sandstone_stairs","minecraft:light_gray_concrete","minecraft:light_gray_terracotta","minecraft:spruce_planks","minecraft:brown_concrete_powder","minecraft:granite","minecraft:terracotta","minecraft:stone","minecraft:smooth_red_sandstone","minecraft:stone_bricks","minecraft:blackstone","minecraft:iron_bars","minecraft:cyan_terracotta","minecraft:oak_leaves","minecraft:black_terracotta","minecraft:black_concrete"]} 2
```

**Line 6** — exactly the same command and the same 44-entry list, but the trailing count is `1`. (Line 5's count 2 is overwritten by line 6 in the same tick; net effect = 1 block.) [H]

**Line 7** — iron pickaxe with `CanDestroy` + `Enchantments`:

```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:blue_concrete"],Enchantments:[{id:"minecraft:efficiency",lvl:2s}]}
```

Notes [H/M]:

- These are the **only** old item-NBT lines in the Badlands pack. Line 7 has **no `Unbreakable`** (unlike the Industry/Pits packs A12 audited); it has only `CanDestroy` and `Enchantments`.
- 1.21.1 rejects `{CanPlaceOn:…}`, `{CanDestroy:…}` and `{Enchantments:…}` in an item argument. The `item replace` command structure, the slot names (`weapon.offhand`, `hotbar.0`) and the trailing count remain valid; only the item argument changes. [M]
- Required 1.21 component forms (not applied):
  - `blue_concrete[minecraft:can_place_on={blocks:[…44 ids…]}]` — or the equivalent `{predicates:[{blocks:[…44…]}]}` form. Keep the count `2`/`1` lines if preserving the exact two-step behaviour.
  - `iron_pickaxe[minecraft:can_break={blocks:"minecraft:blue_concrete"},minecraft:enchantments={levels:{"minecraft:efficiency":2}}]`.
- The 44-entry allow-list is the same in both lines. It includes `blue_concrete` and `cactus`, and excludes `dirt`, `quartz_block`, `barrier`, `oak_wall_sign`, `daylight_detector`, `lantern`, `iron_block` and other decorative blocks. `data/can_place_on.csv` has the full comparison. [H]
- `CanPlaceOn`/`CanDestroy` are Adventure-only rules; see §3.2 item 5. In Survival, even a correctly converted item ignores them. [M]

---

## 5. Exact state comparison

| Field | Source archive | Imported `mcc_tgttos_badlands` |
|---|---|---|
| `level.dat` path | `...\TGTTOS Badlands - By MickeyKat\level.dat` | `...\scratchpad\review\mcc_tgttos_badlands\level.dat` |
| DataVersion | 3120 (1.19.2) | 3955 (1.21.1) |
| LevelName | `TGTTOS Badlands Practise` | `mcc_tgttos_badlands` |
| GameType | 1 (Creative) | 0 (Survival) |
| Difficulty | 0 (Peaceful) | 0 (Peaceful) |
| Spawn | 10,−42,31 | 10,−42,31 |
| `DataPacks.Enabled` | `[vanilla, file/tgttos]` | `[vanilla, file/bukkit, paper]` |
| `DataPacks.Disabled` | `[]` | `[bundle, trade_rebalance]` |
| `file/tgttos` present? | **Enabled** | **Absent from both lists** |
| Pack on disk | `datapacks/tgttos` | `datapacks/tgttos` (byte-identical) |
| `pack_format` | 10 | 10 |
| Function folder | `data/tgttos/functions` | `data/tgttos/functions` |
| Tag folder | `data/minecraft/tags/functions` | `data/minecraft/tags/functions` |
| Functions / lines | 2 / 8 + 130 (113 non-empty) | 2 / identical (byte-identical) |
| Old item-NBT lines | 3 (5, 6, 7) | 3 (5, 6, 7) |
| `scoreboard.dat` objectives | 9 | 9 (same names) |
| Sign | `Text1..Text4` legacy | `front_text.messages`, clickEvent preserved on empty line 1 |
| Multiverse gamemode | n/a | `survival` |
| Effect | pack ran in 1.19.2 | pack **not loaded**; even if enabled, 1.21.1 ignores the old folders |

All source/imported rows are [H] from direct file/NBT reads; the "pack ran in 1.19.2" is [M] from the saved Resistance effects on the chickens and the enabled list.

---

## 6. Bottom line

- A12 is right: the source had `file/tgttos` enabled; the imported world does not have it enabled (it is absent from `Enabled`, not listed in `Disabled`), and the pack still uses `functions`/`tags/functions` + `pack_format` 10 while 1.21.1 expects `function`/`tags/function` + 48. The imported pack is byte-identical to the source. [H]
- The exact load chain is `level.dat DataPacks.Enabled → datapacks/tgttos/pack.mcmeta → data/tgttos/functions/* → data/minecraft/tags/functions/{load,tick}.json`; the 1.21.1 chain is the same with `function`/`tags/function`. [H/M]
- Minimum 1.21 fix: **enable + rename two folders + format 48 + rewrite 3 item lines**. For intended play also: **Adventure mode + verify the sign click target**. None of this was applied. [H/M]
- Even after that, the map is still a single-player practice build with no respawn/checkpoints/round loop; the existing `McmTgttos` plugin is the layer that supplies those. [H/M]
