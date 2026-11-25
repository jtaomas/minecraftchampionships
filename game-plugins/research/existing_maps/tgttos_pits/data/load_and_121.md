# Pits — import state, load/build path, and 1.21.1 requirements

Purpose: answer A12's finding that the imported `mcc_tgttos_pits` `level.dat` has the `Pits` datapack **disabled**, and still uses the old `functions` / `tags/functions` layout. Confirm against the source archive, document the exact load/build path, list the 1.21.1 fix requirements, and list every old item-NBT line. **No edit, enable, port, reload or build was performed.**

Confidence: **[H]** directly observed here or in the imported server world read here / by A12 (report: `../server_import/README.md`, `../server_import/data/datapacks.csv`, `../server_import/data/worlds.csv`); **[M]** inference; **[L]** guess.

## 1. Confirmation — source vs imported

| Item | Source archive | Imported live world `mcc_tgttos_pits` | Confidence |
|---|---|---|---|
| `level.dat` DataVersion | **3117** (1.19.1) | **3955** (1.21.1) | H |
| `DataPacks.Enabled` | **`[vanilla, Fabric Mods, file/Pits]`** | **`[vanilla, file/bukkit, paper]`** | H |
| `DataPacks.Disabled` | `[]` | `[bundle, trade_rebalance]` | H |
| `file/Pits` enabled? | **yes** | **no** (not in Enabled; not in Disabled either) | H |
| Datapack on disk | yes: `<source world>/datapacks/Pits/` | yes: `<review server>/mcc_tgttos_pits/datapacks/Pits/` | H |
| Pack files | 9 files | **byte-identical SHA-1 (9/9)** | H |
| `level.dat` GameType | **1 (creative)** | **0 (survival)**; MV `SURVIVAL` | H |
| `level.dat` allowCommands | 1 | 1 | H |
| Pack format | 10 | 10 (unchanged) | H |
| Function folder | `data/tgttos/functions/` | same | H |
| Load/tick tags | `data/minecraft/tags/functions/{load,tick}.json` | same | H |
| Functions / non-empty lines | 6 / 219 | 6 / 219 | H |
| Old item-NBT lines | **21** | **21** | H |
| Scoreboard add lines | 21 (14 objectives + 7 score inits) | 21 | H |
| Command blocks | 0 | 0 | H |
| Imported spawn | 15,-2,36 | 15,-2,36, quartz, safe | H |
| Imported MV settings | — | `SURVIVAL`, `PEACEFUL`, `allow_flight=false`, `keep_spawn_in_memory=true`, PvP true, hunger true | H |

**Conclusion:** the source archive had `file/Pits` **enabled**; the import did **not** carry it into `Enabled`, so the pack is on disk but not loaded. This is the **first** blocker, before the 1.21 folder rename and item-NBT issues. **[H]** source; **[H]** import (direct read of the review server world; same as A12). A12's verdict `NOT_RUNNABLE_AS_IMPORTED` is correct. **[H]**

**Direct confirmation (read-only):** the review server world `mcc_tgttos_pits/level.dat` was read directly and shows `DataVersion 3955`, `LevelName mcc_tgttos_pits`, `Spawn 15,-2,36`, `GameType 0` (survival), `Difficulty 0`, `allowCommands 1`, `Enabled [vanilla,file/bukkit,paper]`, `Disabled [bundle,trade_rebalance]`. The `datapacks/Pits/` folder contains all 9 files, **byte-identical to the source archive by SHA-1** (pack.mcmeta, 2 tags, 6 functions); the import copied the pack unchanged, it only failed to enable it. Machine-readable record: `import_state.json` (full SHA-1s, both level.dat summaries). **[H]**

The imported `level.dat` still carries the source gamerules (`keepInventory=true`, `fallDamage=false`, `doDaylightCycle=false`, `doMobSpawning=false`, `commandBlockOutput=false`, `doImmediateRespawn=false`, `randomTickSpeed=3`) **[H]**. Those settings survive because they are world NBT, not datapack output; they do not restore the start/timer/finish loop.

A12's exact evidence string: `pack.mcmeta format 10; plural functions/tags paths; level.dat Enabled=[vanilla,file/bukkit,paper]; source level.dat Enabled=[vanilla,Fabric Mods,file/Pits].` **[H]**

## 2. Exact load/build path

### 2.1 Source archive layout

```
TGTTOS Pits - By Mickey_Kat/
  level.dat                       DataPacks.Enabled = [vanilla, Fabric Mods, file/Pits]
  datapacks/
    Pits/
      pack.mcmeta                 pack_format 10, "Made by Mickey_Kat"
      data/
        minecraft/tags/functions/
          load.json               values: ["tgttos:load"]
          tick.json                values: ["tgttos:loop","tgttos:color","tgttos:start_timer","tgttos:gamerules","tgttos:respawn"]
        tgttos/functions/
          load.mcfunction
          loop.mcfunction
          color.mcfunction
          start_timer.mcfunction
          gamerules.mcfunction
          respawn.mcfunction
```

- The pack id is `file/Pits` (folder name `Pits` under `<world>/datapacks/`). **[H]**
- The source `level.dat` lists it in `Enabled`; `Disabled` is empty. **[H]**
- There is **no build step**: `.mcfunction` files are plain text loaded by the server at world load/reload; `pack.mcmeta` is metadata. No compilation, no plugin, no paste. **[H]**

### 2.2 Imported world layout

```
<server>/<world container>/mcc_tgttos_pits/
  level.dat                       DataPacks.Enabled = [vanilla, file/bukkit, paper]
                                  DataPacks.Disabled = [bundle, trade_rebalance]
  datapacks/
    Pits/                         on disk; NOT in Enabled -> not loaded
      pack.mcmeta
      data/minecraft/tags/functions/{load,tick}.json
      data/tgttos/functions/*.mcfunction
```

- `worlds.csv` for `mcc_tgttos_pits`: `datapacks_enabled_current="vanilla,file/bukkit,paper"`, `datapacks_on_disk=Pits`, `datapacks_enabled_source="vanilla,Fabric Mods,file/Pits"`. **[H]**
- Exact review-server paths (A12): root `C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review`; world `<root>\mcc_tgttos_pits\`; pack `<root>\mcc_tgttos_pits\datapacks\Pits\`; `level.dat` `<root>\mcc_tgttos_pits\level.dat`. These were read directly (read-only), not modified. **[H]**
- The world is a standalone Multiverse world. **`McmTgttos` does not load it**: the plugin owns `mcc_tgttos` and generates Java maps; the Pits world is imported as a world folder and is entered with `/mv tp mcc_tgttos_pits`. There is no plugin/paste path for this map. **[H]** plugin README/`mcc_tgttos`; **[H]** world load.
- Runtime enable path (not executed): `/datapack list` → available `file/Pits`; `/datapack enable "file/Pits"` → `/reload`; or add `file/Pits` to `level.dat` `Data.DataPacks.Enabled`. An op/`allowCommands` is required. The exact command form in 1.21.1 is `/datapack enable "file/Pits"` (quotes because of the slash in the pack id). **[M]** command syntax; **[H]** current disabled state.
- If enabled on 1.19.1, the load tag runs `tgttos:load` once, then the tick tag runs the five functions each tick in order (`loop`, `color`, `start_timer`, `gamerules`, `respawn`). **[H]** source.

### 2.3 What 1.21.1 actually looks for

| Item | 1.21.1 expectation | Pits as imported |
|---|---|---|
| Data pack format | **48** (world 3955; `version.json` `pack_version.data=48`) | **10** |
| Function files | `data/<namespace>/function/*.mcfunction` | `data/tgttos/functions/*.mcfunction` |
| Load/tick tags | `data/minecraft/tags/function/load.json` and `tick.json` | `data/minecraft/tags/functions/load.json` and `tick.json` |
| Item arguments | components `item[components] count` | NBT `item{CanDestroy/CanPlaceOn/Unbreakable/Enchantments} count` |
| Sign NBT | `front_text.messages` (upgraded from `Text1..4` by DataFixer) | `Text1..4`; click event survives **[H]** |
| Pack enabled | must be in `DataPacks.Enabled` | **not enabled** |

1.21.1's `ServerFunctionLibrary` looks for the literal `function` directory and `.mcfunction`; the old `functions` directory is invisible. **[H]** — Paper 1.21.1 jar (`net/minecraft/server/ServerFunctionLibrary.class` contains `function` and `.mcfunction`; `DataComponents` contains `can_place_on`/`can_break`; `ItemParser` uses `DataComponentPatch`/`SYNTAX_START_COMPONENTS`).

**Load path consequence:** the source's `#minecraft:load`/`#minecraft:tick` tags are not discovered at the 1.21 paths, and the `.mcfunction` files are not discovered at the old `functions` path. Even if `file/Pits` were enabled, the pack would contribute zero functions. **[H]** paths; **[H/M]** result.

## 3. 1.21.1 fix requirements (not executed)

These are requirements, not a patch. Order matters.

1. **Enable the pack in the imported world.** Add `file/Pits` to `level.dat` `Data.DataPacks.Enabled` (and remove it from `Disabled` if present), or `/datapack enable "file/Pits"` + `/reload` as an op. Keep `vanilla`/`paper` etc. intact. **[M]**
2. **Set the pack format.** `pack.mcmeta` `pack_format` → **48** (or add `supported_formats` covering 48). Format 10 is flagged incompatible on 1.21.1. **[H]**
3. **Rename the function folder.** `data/tgttos/functions/` → `data/tgttos/function/` (6 `.mcfunction` files). **[H]**
4. **Rename the tag folder.** `data/minecraft/tags/functions/` → `data/minecraft/tags/function/` (`load.json`, `tick.json`). **[H]**
5. **Keep the tag values.** `load.json` still `["tgttos:load"]`; `tick.json` still the five functions in order. **[H]**
6. **Convert all 21 item-NBT lines** (table §4) to components:
   - `CanDestroy` → `can_break={blocks:[...]}`;
   - `CanPlaceOn` → `can_place_on={blocks:[...]}`;
   - `Unbreakable:1b` → `unbreakable={}`;
   - `Enchantments:[{id:"minecraft:efficiency",lvl:2s}]` → `enchantments={levels:{"minecraft:efficiency":2}}`.
   A12's full-predicate form `can_place_on={predicates:[...]}` / `can_break={predicates:[...]}` is the fully explicit shape; the compact `{blocks:[...]}` form is used in `item_nbt_lines.csv` for readability. Validate the exact accepted shape in 1.21.1 before porting. **[H]** component names; **[M]** exact predicate shape.
7. **Check the sign after DataFix.** `Text1..4` becomes `front_text.messages`; A12 reports the click event survives. The click is on line 1 (blank), not the visible “Start” line 2; if the aim is to make the visible word clickable, that is a design fix, not a 1.21 requirement. **[H]** NBT; **[M]** click target.
8. **Reconcile existing objectives.** `scoreboard.dat` already contains the objectives; `scoreboard objectives add` on load will log “already exists” (feedback is off). Nonfatal. **[H]** scoreboard.dat; **[M]** load behaviour.
9. **Runtime test one player.** Sign click → 7 s countdown → cage opens → bridge Pit 1/2 → punch a chicken → time title → 7 s reset. **[M]**
10. **Do not advertise as playable until 1–9 pass.** A12's `NOT_RUNNABLE_AS_IMPORTED` is correct. **[H]**

### 3.1 Runtime prerequisites (not 1.21 syntax, but required for a fair run)

| # | Requirement | Why |
|---|---|---|
| R1 | Put players in **adventure** on join (`mv_gamemode ADVENTURE` or a join listener), not only at reset. | Imported `level.dat` GameType 0 (survival), MV `SURVIVAL`; source GameType 1 (creative). In survival, bare hands can break the 1-thick wool wall/terrain; the kit pickaxe's `CanDestroy` only restricts the pickaxe. |
| R2 | Set PvP off or add a damage guard (`mv_pvp=false`, server `pvp=false`, or plugin damage cancel). | MV `pvp=true`; the datapack has no `onDamage`; fists can kill in survival. |
| R3 | Keep `allow_flight=false` (already set) and `keep_spawn_in_memory=true` (already set). | Prevent creative flight / keep the spawn area loaded. |
| R4 | Keep the finish chunks loaded or make chickens persistent. | Chickens `PersistenceRequired=0`, no chunk tickets; 20 targets can despawn. |
| R5 | Keep `doDaylightCycle=false` / `DayTime=4328`. | The black-glass ceiling/skylight look depends on permanent noon. |

### 3.2 Optional gameplay fixes (not compatibility)

These are code/logic issues, not 1.21 blockers; listed so a port does not preserve known bugs.

| # | Fix | Why |
|---|---|---|
| G1 | `loop` line 13 `tag @a remove playing` → `@s` | first finisher freezes every timer |
| G2 | Reset per player, not `tp @a` / `gamemode adventure @a` | first finisher ends the run for everyone |
| G3 | Add `starting`/`playing` on late join, or block join mid-round | late joiner can finish at 00:00 |
| G4 | Resolve the join gamemode (see R1/R2) | imported `level.dat` GameType=0 (survival), MV SURVIVAL; source GameType=1 (creative) |
| G5 | Make chickens persistent / hold chunks | `PersistenceRequired=0`, no tickets |
| G6 | Fix the sign click to the visible “Start” line, or keep line 1 but add a visible clickable label | usability |
| G7 | Remove the redundant count-2 concrete command | final count is always 1 |
| G8 | Cleanup stands per player, not `kill @e[tag=remove_block]` | other players' concrete survives |
| G9 | Remove dead `execute as @s` line 57 | no-op |

## 4. Old item-NBT lines (21)

All are in `loop.mcfunction`. Exact commands and a compact component form are in **`data/item_nbt_lines.csv`**; the full verbatim function is in `data/commands_export.txt` lines 86–116. Each concrete line is duplicated (count 2 then count 1); the final count is **1**. **[H]**

Old syntax pattern (ellipses only in this summary; full lines in the CSV):

```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:[...10...],Unbreakable:1b,Enchantments:[{id:"minecraft:efficiency",lvl:2s}]} 1
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:[...40...]} 2
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:[...40...]} 1
```

| Line | Item | Count | Old NBT keys | `CanDestroy` IDs | `CanPlaceOn` IDs | 1.21 component form (compact) |
|---:|---|---:|---|---:|---:|---|
| 86 | `iron_pickaxe` | 1 | `CanDestroy`, `Unbreakable`, `Enchantments` | 10 | 0 | `iron_pickaxe[unbreakable={},enchantments={levels:{"minecraft:efficiency":2}},can_break={blocks:[…10…]}] 1` |
| 88 | `red_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `red_concrete[can_place_on={blocks:[…40…]}] 2` |
| 89 | `red_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 (final) |
| 91 | `orange_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `orange_concrete[can_place_on={blocks:[…40…]}] 2` |
| 92 | `orange_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 94 | `yellow_concrete` | 2 | `CanPlaceOn` | 0 | **39** | `yellow_concrete[can_place_on={blocks:[…39…]}] 2` |
| 95 | `yellow_concrete` | 1 | `CanPlaceOn` | 0 | **39** | same, count 1 |
| 97 | `lime_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `lime_concrete[can_place_on={blocks:[…40…]}] 2` |
| 98 | `lime_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 100 | `green_concrete` | 2 | `CanPlaceOn` | 0 | **39** | `green_concrete[can_place_on={blocks:[…39…]}] 2` |
| 101 | `green_concrete` | 1 | `CanPlaceOn` | 0 | **39** | same, count 1 |
| 103 | `cyan_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `cyan_concrete[can_place_on={blocks:[…40…]}] 2` |
| 104 | `cyan_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 106 | `light_blue_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `light_blue_concrete[can_place_on={blocks:[…40…]}] 2` |
| 107 | `light_blue_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 109 | `blue_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `blue_concrete[can_place_on={blocks:[…40…]}] 2` |
| 110 | `blue_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 112 | `purple_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `purple_concrete[can_place_on={blocks:[…40…]}] 2` |
| 113 | `purple_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |
| 115 | `pink_concrete` | 2 | `CanPlaceOn` | 0 | 40 | `pink_concrete[can_place_on={blocks:[…40…]}] 2` |
| 116 | `pink_concrete` | 1 | `CanPlaceOn` | 0 | 40 | same, count 1 |

Notes:
- The pickaxe's `CanDestroy` list is exactly 10 team concretes (red, blue, lime, pink, green, orange, purple, yellow, light_blue, cyan); no map blocks. **[H]**
- The concrete `CanPlaceOn` list is 40 IDs for most colours, 39 for yellow/green (one fewer block; exact lists in the CSV). It includes map materials (blackstone, cobblestone, quartz, terracotta, wool, glass, bricks, etc.) plus the colour's own concrete. **[H]**
- The old `item replace … with item{NBT} count` syntax is the failure; the `item replace` command itself still exists in 1.21.1. **[H]**
- `CanDestroy`/`CanPlaceOn` were item NBT; 1.21.1 uses the `can_break`/`can_place_on` **components**. `Unbreakable`/`Enchantments` are also components. **[H]** — `DataComponents` / `ItemParser` in the 1.21.1 jar.

## 5. Why enabling alone is not enough

| Layer | State | Result |
|---|---|---|
| 0. Imported `level.dat` | `file/Pits` not in Enabled | pack not loaded; sign/tag does nothing | 
| 1. Pack format | 10 vs 48 | incompatible warning; not fatal by itself |
| 2. Function folder | `functions` vs `function` | no functions discovered |
| 3. Tag folder | `tags/functions` vs `tags/function` | no load/tick; no functions run |
| 4. Item NBT | 21 old lines | even with 2–3 fixed, no kit; bridging impossible |
| 5. Gameplay bugs | B1/B3/B9/B12/G1–G9 | even with 1–4 fixed, multiplayer unfair |

**As imported:** layer 0 fails; nothing runs.
**After enabling only:** layer 2/3 fail; still nothing runs.
**After folders+format:** layer 4 fails; start/timer/finish run but no kit.
**After item components:** playable, with the known gameplay bugs.
**No port was applied.** **[H]**
