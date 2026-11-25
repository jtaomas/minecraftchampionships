# Industry — exact load path and 1.21.1 conversion requirements

Research only. Nothing here was edited, enabled, renamed or ported. Written 2026-11-25 for A8.

## 1. Source archive (confirmed)

| Item | Value | Evidence |
|---|---|---|
| Archive | `C:\Users\LeonS\Downloads\EXISTING MCC RECREATIONS\tgttos-industry-by-mickey-kat.zip` (2,214,645 bytes) | H (file exists, zip opened) |
| Root folder in zip | `TGTTOS Industry - By Mickey_Kat/` | H |
| World name | `TGTTOS Industry - Practise map` | H (`level.dat`) |
| World version | 1.19.1 / DataVersion **3117** | H (`level.dat`) |
| Datapack | `datapacks/Industry/` | H (zip listing) |
| `pack.mcmeta` | `pack_format: 10`, description `Made by Mickey_Kat` | H (zip entry read) |
| Datapack dirs | `data/tgttos/functions/` (7 `.mcfunction`), `data/minecraft/tags/functions/` (load, tick) | H (zip listing) |
| Source `level.dat` DataPacks | Enabled: `vanilla`, `Fabric Mods`, `file/Industry`; Disabled: none | H (NBT read from zip) |
| Source spawn / mode | 73, -33, 11; GameType 1 (creative); Difficulty 0 (peaceful) | H (`level.dat`) |

The archive's datapack is byte-identical to the extracted copy at
`C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat\datapacks\Industry`
(all 10 files SHA-256 match; H).

## 2. Review-server copies

The review server is the Paper 1.21.1 server under
`C:\Users\LeonS\AppData\Local\Temp\claude\3ce71e194a20101d\adb21d58-93cf-493d-abe5-7c5a160bc899\scratchpad\review`
(`version_history.json`: `1.21.1-133-3cb8529 (MC: 1.21.1)`, H).

| Path | What it is | Version | Industry pack state |
|---|---|---|---|
| `review\tgttos_industry` | untouched 1.19.1 source copy | DataVersion 3117, LevelName `TGTTOS Industry - Practise map` | `Enabled: [vanilla, Fabric Mods, file/Industry]` — **enabled** |
| `review\mcc_tgttos_industry` | Multiverse live world | DataVersion **3955**, LevelName `mcc_tgttos_industry` | `Enabled: [vanilla, file/bukkit, paper]`, `Disabled: [bundle, trade_rebalance]` — **Industry is in neither list** |

Both live/source copies contain `datapacks/Industry/` with the same 10 files (SHA-256 match; H).
The live world's `datapacks/Industry/` still has the **plural** `data/tgttos/functions/` and `data/minecraft/tags/functions/` and `pack_format: 10` (H).
Multiverse registers it as `minecraft:mcc_tgttos_industry` in `plugins/Multiverse-Core/worlds.yml` (H).

Conclusion: the imported live world is 1.21.1, the datapack files were copied, but the world's datapack selection was rebuilt without `file/Industry`, so the pack is **present on disk and not loaded**. The source archive does have it enabled; the loss happened on the import/upgrade path, not in the archive. [H for the lists; M for "the import rewrote DataPacks" — the source has it, the live world does not, and the live world's LevelName/GameType were also rewritten by the import.]

## 3. Exact load path

```
Downloads\EXISTING MCC RECREATIONS\tgttos-industry-by-mickey-kat.zip
  └─ TGTTOS Industry - By Mickey_Kat\            (1.19.1, DataVersion 3117)
       ├─ level.dat                              (Enabled: vanilla, Fabric Mods, file/Industry)
       └─ datapacks\Industry\
            ├─ pack.mcmeta                       (pack_format 10)
            └─ data\
                 ├─ minecraft\tags\functions\    (load.json, tick.json)
                 └─ tgttos\functions\            (7 .mcfunction)
        │
        ├─ extracted to  %TEMP%\opencode\mcc_maps\tgttos-industry-by-mickey-kat\TGTTOS Industry - By Mickey_Kat
        │
        ├─ review\tgttos_industry\                (source copy, untouched, 1.19.1)
        │
        └─ review\mcc_tgttos_industry\            (Multiverse import, upgraded to 1.21.1, DataVersion 3955)
             ├─ level.dat                         (Enabled: vanilla, file/bukkit, paper; Industry absent)
             └─ datapacks\Industry\               (files present, plural folders, pack_format 10, not loaded)
```

There is **no paste/build path into `McmTgttos`** for this world. `McmTgttos` is a pure-Java map generator (Paper 1.21.1) that pastes generated `TgMapData` blocks into its own `mcc_tgttos` slots; it does not load imported worlds or datapacks. The only live path is the review-server Multiverse world. Using Industry in the game would require a new world/datapack adapter or a rewrite, not a paste (H from `game-plugins/Tgttos/README.md`, `TgMapData.java`, and the absence of a world loader in `McmTgttos`).

## 4. Why it does not load on 1.21.1

Four independent blockers, in order:

1. **Not enabled.** `mcc_tgttos_industry\level.dat` `DataPacks.Enabled` is `[vanilla, file/bukkit, paper]`; `file/Industry` is absent from both Enabled and Disabled. A pack that is present but not selected is not loaded. [H]
2. **Folder names.** 1.21 renamed the data-pack directories from plural to singular: `functions` → `function`, `tags/functions` → `tags/function` (also `advancements`→`advancement`, `loot_tables`→`loot_table`, etc.). The pack still uses `data/tgttos/functions` and `data/minecraft/tags/functions`, so even if enabled it would expose no functions and no `#minecraft:load`/`#minecraft:tick` tags. [H for the pack files; H for the 1.21 rename, documented in the 1.21 breaking-changes list]
3. **Pack format.** `pack_format: 10` is the 1.19–1.19.3 data format. 1.21–1.21.1 is **48** (1.21.2–1.21.3 = 57; 1.21.4 = 61; 1.21.5 = 71). A format-10 pack is flagged as made for an older version. [H for the value; H for 48 from the data-pack format table]
4. **Item NBT.** Even after enabling and renaming, 21 `item replace` lines still use pre-1.20.5 item NBT (`{CanDestroy:...}`, `{CanPlaceOn:...}`, `Unbreakable:1b`, `Enchantments:[...]`). 1.20.5 replaced item NBT with data components, so every one of those lines fails to parse; the kit is never given. [H for the lines; H for the 1.20.5 component change]

One thing that **does** survive: the sign. The live 1.21.1 chunk at 73,-32,10 has been DataFixerUpper-migrated from `Text1..Text4`/`Color`/`GlowingText` to `front_text.messages`/`front_text.color`/`front_text.has_glowing_text` and `back_text`; the clickEvent JSON is preserved on `front_text.messages[0]`. The empty first line is not a bug: 1.21.1 `SignBlockEntity` loops all four messages and runs every `RUN_COMMAND` click event when the sign is used, so the empty-line event fires. [H, live chunk + 1.21.1 class]

The chicken entities also survived the world upgrade: 24 chickens at y=-39, `PersistenceRequired:0`, `ActiveEffects` id 11 (resistance) amplifier 5 duration 19. [H, live entity NBT]

## 5. What the 1.21.1 fix requires (do not apply here)

Minimum to make the **current** pack run on 1.21.1:

1. **Enable** `file/Industry` for `mcc_tgttos_industry` (add it to `level.dat` `DataPacks.Enabled`, or `/datapack enable "file/Industry"` from the console). The folder is already in `mcc_tgttos_industry/datapacks/Industry`.
2. **Rename** the two directory trees:
   - `data/tgttos/functions/` → `data/tgttos/function/`
   - `data/minecraft/tags/functions/` → `data/minecraft/tags/function/`
   The JSON contents do not change: `load.json` stays `["tgttos:load"]`; `tick.json` stays `["tgttos:loop","tgttos:color","tgttos:start_timer","tgttos:gamerules","tgttos:respawn"]`. Function IDs (`tgttos:load`, `tgttos:loop`, ...) do not change.
3. **Set `pack_format` to 48** in `pack.mcmeta` (1.21–1.21.1). If the pack must also load on 1.21.2+, use 57/61/71 or `supported_formats` plus overlays; a single 48 value is correct only for 1.21–1.21.1.
4. **Replace the 21 item-NBT lines** with 1.21.1 components (see section 6). Without this the kit is missing even if the functions run.
5. **Re-enable after the repack**, then reload (`/reload` or restart). `function-permission-level=2` is enough for the functions; `difficulty`/`gamerule` run at level 2.
6. **Do not remove** the sign data: it migrated correctly. The click event is still on the empty first line (see the dossier's sign finding).
7. **Chicken persistence** is a runtime robustness fix, not a 1.21 syntax fix: set `PersistenceRequired:1b` on the 24 chickens (or hold their chunks). They are currently `0` (H). On the review server (`simulation-distance=6`, view-distance 8) the 81-block start-to-pen distance keeps them ticking near the start, so they are not guaranteed to despawn, but a smaller simulation distance or a player far away can unload them. [H for NBT; M for the risk]

Everything else in the pack is 1.21.1-valid and needs no syntax change: `scoreboard objectives add ... minecraft.custom:minecraft.damage_dealt_resisted` (the stat key exists in 1.21.1 `en_us.json`), `execute ... store result ... data get entity @s Pos[1]`, `summon armor_stand {NoGravity:...}`, `effect give @e[type=chicken] resistance 1 5 true`, `fill ... replace #minecraft:mineable/pickaxe`, `clone`, `gamerule`, `difficulty`, `title` JSON, `tellraw` JSON, `item replace ... <count>`, `function`. [H for the lines; H for the stat key; H for the tag; M for full runtime equivalence]

The one tellraw line (`color.mcfunction` line 3) uses `clickEvent.run_command` values that begin with `/`. 1.21.1 accepts this; the leading slash is a **1.21.5+** compatibility risk, not a 1.21.1 blocker. [M]

## 6. Old item-NBT lines (all 21)

All are in `data/tgttos/functions/loop.mcfunction` (original line numbers). Each is a hard parse failure on 1.20.5+.

| Line | Item | Old NBT (abridged) | 1.21.1 replacement shape |
|---:|---|---|---|
| 84 | `iron_pickaxe` | `{CanDestroy:[...], Unbreakable:1b, Enchantments:[{id:"minecraft:efficiency",lvl:2s}]}` | `iron_pickaxe[can_break={blocks:[...]},unbreakable={},enchantments={levels:{"minecraft:efficiency":2}}]` |
| 86–87 | `red_concrete` ×2 | `{CanPlaceOn:[...]}` | `red_concrete[can_place_on={blocks:[...]}]` |
| 89–90 | `orange_concrete` ×2 | `{CanPlaceOn:[...]}` | `orange_concrete[can_place_on={blocks:[...]}]` |
| 92–93 | `yellow_concrete` ×2 | `{CanPlaceOn:[...]}` | `yellow_concrete[can_place_on={blocks:[...]}]` |
| 95–96 | `lime_concrete` ×2 | `{CanPlaceOn:[...]}` | `lime_concrete[can_place_on={blocks:[...]}]` |
| 98–99 | `green_concrete` ×2 | `{CanPlaceOn:[...]}` | `green_concrete[can_place_on={blocks:[...]}]` |
| 101–102 | `cyan_concrete` ×2 | `{CanPlaceOn:[...]}` | `cyan_concrete[can_place_on={blocks:[...]}]` |
| 104–105 | `light_blue_concrete` ×2 (tag `aqua`) | `{CanPlaceOn:[...]}` | `light_blue_concrete[can_place_on={blocks:[...]}]` |
| 107–108 | `blue_concrete` ×2 | `{CanPlaceOn:[...]}` | `blue_concrete[can_place_on={blocks:[...]}]` |
| 110–111 | `purple_concrete` ×2 | `{CanPlaceOn:[...]}` | `purple_concrete[can_place_on={blocks:[...]}]` |
| 113–114 | `pink_concrete` ×2 | `{CanPlaceOn:[...]}` | `pink_concrete[can_place_on={blocks:[...]}]` |

The count argument (`... with <item> 1|2`) is unchanged. `CanDestroy` maps to `minecraft:can_break`; `CanPlaceOn` maps to `minecraft:can_place_on`; both take `{blocks:[...]}` in 1.21.1 (the `predicates:[...]` wrapper is also accepted; both the shorthand and the wrapper are accepted). `Unbreakable:1b` maps to `unbreakable={}`; `Enchantments:[{id,lvl}]` maps to `enchantments={levels:{"minecraft:efficiency":2}}` in 1.21.1 (the inlined `enchantments={efficiency:2}` form is 1.21.5+). [H for the old lines; M/H for the component shapes from the 1.20.5 component format and 1.21.5 change notes]

The full command text for every line is in `data/datapack_functions.csv` (`line_1_21_risk` column), and the unpacked pack is copied at `data/datapack/`.

## 7. 1.21.1 risk summary

| Risk | Status | Exact effect |
|---|---|---|
| Pack not enabled in live `level.dat` | **Blocker** | `file/Industry` absent from Enabled/Disabled; nothing runs |
| `functions/` vs `function/` | **Blocker** | No `tgttos:*` functions found; no load/tick tags |
| `tags/functions/` vs `tags/function/` | **Blocker** | `#minecraft:load` / `#minecraft:tick` empty |
| `pack_format: 10` vs 48 | **Blocker/warning** | flagged as old; repack for a clean 1.21.1 load |
| 21 item-NBT lines | **Blocker** | parse error each tick; no pickaxe, no offhand concrete |
| `damage_dealt_resisted` criterion | OK | stat key exists in 1.21.1 |
| `data get Pos[1]`, `summon`, `effect`, `fill`, `clone`, `gamerule`, `title` | OK | no syntax change |
| `tellraw` click values starting `/` | 1.21.5+ only | 1.21.1 works |
| Sign NBT | OK (migrated) | `front_text` in the live chunk; click JSON preserved |
| Chicken NBT | OK (survived) | 24 chickens, resistance 11/5/19; persistence still 0 |
