# Pits datapack — line-by-line forensics

Source: `C:\Users\LeonS\AppData\Local\Temp\opencode\mcc_maps\tgttos-pits-by-mickey-kat\TGTTOS Pits - By Mickey_Kat\datapacks\Pits`
Exact verbatim export: `commands_export.txt` (every line, numbered). Tables: `datapack_manifest.csv`, `scoreboards.csv`, `tags.csv`, `timeline.csv`.

Version: `pack.mcmeta` `pack_format: 10` = 1.19–1.19.2 (world DataVersion 3117 = 1.19.1). The live server is Paper 1.21.1, data pack format **48**.

Confidence tags: **[H]** directly observed in the file/world, **[M]** inference from two sources, **[L]** guess.

## 1. Manifest

| Part | Path | Lines | Non-empty | Bytes |
|---|---|---:|---:|---:|
| pack.mcmeta | `pack.mcmeta` | 6 | 6 | 91 |
| load tag | `data/minecraft/tags/functions/load.json` | 5 | 5 | 48 |
| tick tag | `data/minecraft/tags/functions/tick.json` | 9 | 9 | 156 |
| load | `data/tgttos/functions/load.mcfunction` | 23 | 20 | 883 |
| loop | `data/tgttos/functions/loop.mcfunction` | 129 | 85 | 34,996 |
| color | `data/tgttos/functions/color.mcfunction` | 111 | 83 | 7,380 |
| start_timer | `data/tgttos/functions/start_timer.mcfunction` | 25 | 15 | 1,349 |
| gamerules | `data/tgttos/functions/gamerules.mcfunction` | 16 | 14 | 404 |
| respawn | `data/tgttos/functions/respawn.mcfunction` | 3 | 2 | 144 |

There is **no** `block_reset.mcfunction` in Pits (Industry has one). **[H]** — directory listing.
There are **0 command blocks** in the world (no `command_block` state in `blocks.csv`, no block entity). **[H]** — `world_scan.json`.

## 2. pack.mcmeta and tags

```json
{
    "pack": {
      "pack_format": 10,
      "description": "Made by Mickey_Kat"
    }
}
```

`load.json`:
```json
{ "values": [ "tgttos:load" ] }
```

`tick.json` (order is significant; functions run in this order every tick):
```json
{ "values": [
  "tgttos:loop",
  "tgttos:color",
  "tgttos:start_timer",
  "tgttos:gamerules",
  "tgttos:respawn"
] }
```

**[H]** The tags use the 1.19 plural directories `tags/functions` and `functions`; 1.21.1 expects `tags/function` and `function` (see §11).

## 3. Scoreboards (`load.mcfunction`)

All 14 objectives are created on load:

```mcfunction
scoreboard objectives add hit_chicken minecraft.custom:minecraft.damage_dealt_resisted
scoreboard objectives add playing dummy
scoreboard objectives add countdown dummy
scoreboard objectives add spawntime dummy
scoreboard objectives add playtimeMi1 dummy
scoreboard objectives add playtimeMi2 dummy
scoreboard objectives add playtimeSe1 dummy
scoreboard objectives add playtimeSe2 dummy
scoreboard objectives add playtimeMs1 dummy
scoreboard objectives add playtimeMs2 dummy
scoreboard objectives add finished dummy
scoreboard objectives add respawn dummy
scoreboard objectives add color trigger
scoreboard objectives add selectColor dummy

scoreboard players add @a playtimeMs1 0
scoreboard players add @a playtimeMs2 0
scoreboard players add @a playtimeSe1 0
scoreboard players add @a playtimeSe2 0
scoreboard players add @a playtimeMi1 0
scoreboard players add @a playtimeMi2 0
```

| Objective | Criteria | Default | Use |
|---|---|---|---|
| `hit_chicken` | custom stat `damage_dealt_resisted` | 0 | finish trigger |
| `playing` | dummy | 0 (tag proxy) | 1 = timer running |
| `countdown` | dummy | 0 | ticks from Start click |
| `spawntime` | dummy | 0 | ticks alive while playing; 20 = cleanup anchor |
| `playtimeMi1/Mi2` | dummy | 0 | minutes ones/tens |
| `playtimeSe1/Se2` | dummy | 0 | seconds ones/tens |
| `playtimeMs1/Ms2` | dummy | 0 | hundredths / tenths |
| `finished` | dummy | 0 | ticks since finish |
| `respawn` | dummy | 0 | last stored feet Y |
| `color` | trigger | 0 | player `/trigger color` menu |
| `selectColor` | dummy | 0 | selected colour id |

The names are misleading: `Ms1` is a 0/5 hundredths digit, `Ms2` is tenths, `Se1` is seconds, `Se2` is tens of seconds, `Mi1` is minutes, `Mi2` is tens of minutes. The HUD prints `Mi2 Mi1 : Se2 Se1 : Ms2 Ms1` = `MM:SS:hh`. **[H]** from the carry rules in `loop.mcfunction` lines 61–70 and the title JSON.

## 4. Gamerules (`gamerules.mcfunction`, runs every tick)

```mcfunction
difficulty peaceful
gamerule sendCommandFeedback false
gamerule fallDamage false
gamerule commandBlockOutput false
gamerule doEntityDrops false
gamerule disableRaids true
gamerule doFireTick false
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule fireDamage false
gamerule showDeathMessages false
gamerule freezeDamage false
gamerule keepInventory true
```

Notes:
- `difficulty peaceful` runs every tick, so any `/difficulty` change is reverted next tick. **[H]**
- `doDaylightCycle false` + level.dat `DayTime 4328` (noon) = permanent day; the black glass ceiling tints it dark. **[H]**
- The world's `level.dat` also has `doTileDrops=true`, `naturalRegeneration=true`, `doInsomnia=true`, `mobGriefing=true`; none matter for the course. **[M]**
- 13 commands/tick is wasteful but harmless. **[H]**

## 5. Respawn (`respawn.mcfunction`, last in tick order)

```mcfunction
execute as @a store result score @s respawn run data get entity @s Pos[1]
execute as @a[scores={respawn=..-41}] at @s run tp @s 15 -2 36 -90 0
```

- Every tick, each player's `Pos[1]` (feet Y) is stored as an integer score. The `store result` cast truncates; the effective kill plane is feet Y ≤ -41. **[H]** for command; **[M]** for exact truncation vs rounding.
- Teleport target is always `15 -2 36`, yaw `-90` (east), pitch 0. There is **no checkpoint**. **[H]**
- The timer is not reset on a fall; the run continues. **[H]**
- It runs last, after `loop`, so a falling player gets the kit update before the teleport. **[H]**

## 6. Start timer (`start_timer.mcfunction`)

```mcfunction
execute as @a[tag=starting] at @s run scoreboard players add @s countdown 1
execute as @a[scores={countdown=40..140}] at @s run title @a times 0 3 20
execute as @a[scores={countdown=40..140}] at @s run title @a title {"text":"Starting in","color":"aqua"}
execute as @a[scores={countdown=40..60}] at @s run title @a subtitle {"text":"5","bold":true}
execute as @a[scores={countdown=60..80}] at @s run title @a subtitle {"text":"4","bold":true}
execute as @a[scores={countdown=80..100}] at @s run title @a subtitle {"text":"3","bold":true}
execute as @a[scores={countdown=100..120}] at @s run title @a subtitle {"text":"2","bold":true}
execute as @a[scores={countdown=120..140}] at @s run title @a subtitle {"text":"1","bold":true}
execute as @a[scores={countdown=140..200}] at @s run title @a times 0 60 20
execute as @a[scores={countdown=140..200}] at @s run title @a title {"text":" ","color":"aqua"}
execute as @a[scores={countdown=140..200}] at @s run title @a subtitle {"text":"GO!!","bold":true,"color":"green"}

execute as @a[scores={countdown=140..}] at @s run tag @a add playing
execute as @a[scores={countdown=140..}] at @s run fill 14 1 46 17 -2 25 air replace barrier
execute as @a[scores={countdown=140..}] at @s run tag @a remove starting
execute as @a[scores={countdown=140..}] at @s run scoreboard players set @a countdown 0
```

Timeline **[H]**:

| Countdown | Real time | Display |
|---:|---:|---|
| 1–39 | 0–1.95 s | nothing (2 s lead-in) |
| 40–59 | 2.0–2.95 s | “Starting in” + **5** |
| 60–79 | 3.0–3.95 s | 4 |
| 80–99 | 4.0–4.95 s | 3 |
| 100–119 | 5.0–5.95 s | 2 |
| 120–139 | 6.0–6.95 s | 1 |
| 140 | 7.0 s | **GO!!** (3 s), `playing` added, cage opened, `starting` removed, countdown 0 |

- The countdown only advances for players tagged `starting`; the Start sign adds that tag to **all** players. **[H]**
- `title @a` is run from inside `as @a[scores=...]`, so with N players the same title command runs N times per tick. Cosmetic/performance only. **[H]**
- The cage fill is `14 1 46 17 -2 25 air replace barrier` = x14..17, y -2..1, z25..46. It only replaces barrier; the quartz floor at y=-3 and the solid side walls remain. **[H]**

## 7. Main loop (`loop.mcfunction`)

### 7.1 Every-tick housekeeping (lines 1–5)

```mcfunction
kill @e[type=item]
effect give @e[type=chicken] resistance 1 5 true
fill 14 1 46 17 -2 25 air replace #minecraft:mineable/pickaxe
setworldspawn 15 -2 36
spawnpoint @a 15 -2 36
```

- Drops (including chicken eggs) are deleted every tick. **[H]**
- Every chicken gets Resistance for 1 s at amplifier 5, hidden particles, refreshed every tick. This is the mechanism that makes a punch count as `damage_dealt_resisted` instead of damaging the chicken. **[H]**
- The start box is cleared of any pickaxe-mineable block every tick. Because the floor is at y=-3 and the fill is y -2..1, the floor is not touched. **[H]**
- World spawn and every player's spawn point are forced to the start every tick. **[H]**

### 7.2 Finish detection (lines 8–16)

```mcfunction
execute as @a[scores={hit_chicken=1..}] at @s run tag @s add finished
execute as @a[tag=finished] at @s run gamemode spectator @s
execute as @a[tag=finished] at @s run scoreboard players add @s finished 1
execute as @a[tag=finished] at @s run tag @a remove playing
execute as @a[scores={finished=1..5}] at @s run title @a times 0 100 20
execute as @a[scores={finished=1..5}] at @s run title @s subtitle [ ... time JSON ... ]
execute as @a[scores={finished=1..5}] at @s run title @a title {"text":"Finished","color":"green"}
```

- `hit_chicken` is the custom stat `damage_dealt_resisted`. A player punch on a Resistance-V chicken deals 1 damage, all resisted, so the stat increments by 1. The first point adds `finished`. **[H]** command + **[M]** stat behaviour; the maker's own stats show `damage_dealt_resisted: 200` and `damage_dealt: 0`. **[H]**
- Any of the 20 chickens counts; there is no per-chicken or per-player check. **[H]**
- The finisher is set to spectator immediately and their `finished` score starts counting ticks. **[H]**
- **Bug B1:** `tag @a remove playing` removes `playing` from **everyone**, not just the finisher. The first finisher stops every player's timer. **[H]**
- The time title only runs while `finished` is 1..5 (5 ticks); the `times 0 100 20` keeps “Finished”/time on screen for 5 s. **[H]**
- Time JSON order: `playtimeMi2`, `playtimeMi1`, `:`, `playtimeSe2`, `playtimeSe1`, `:`, `playtimeMs2`, `playtimeMs1`, gray bold. **[H]**

### 7.3 Concrete cleanup (lines 18–27)

For each of eight colours:

```mcfunction
execute as @a[scores={finished=60..}] at @s run execute as @e[tag=remove_block] at @s run fill ~-15 ~-25 ~-15 ~15 ~5 ~15 air replace red_concrete
... orange, lime, cyan, light_blue, blue, purple, pink ...
execute as @a[scores={finished=60..}] at @s run kill @e[type=armor_stand,tag=remove_block]
```

- At `finished=60` (3 s after the first finish), every `remove_block` armor stand triggers a 31×31×31 air fill that removes the eight selectable concrete colours. **[H]**
- The stands are created in §7.6 every second at each playing player's position +10 Y. **[H]**
- **Bug B2:** the first finisher's cleanup kills **all** stands, including those recorded for other players still racing. Later concrete from those players is not cleaned. **[H]** (command) / **[M]** (effect).
- It only removes the 8 player colours; it never removes yellow/green/black/white/gray concrete, nor map blocks. **[H]**
- The 31³ fill is 29,791 blocks, under the 32,768 `/fill` limit. **[H]**

### 7.4 Round reset (lines 29–48)

```mcfunction
execute as @a[scores={finished=140..}] at @s run fill 14 1 25 17 -2 25 barrier replace air
execute as @a[scores={finished=140..}] at @s run fill 17 1 25 17 -2 46 barrier replace air
execute as @a[scores={finished=140..}] at @s run fill 17 1 46 13 -2 46 barrier replace air
execute as @a[scores={finished=140..}] at @s run fill 14 1 25 17 -2 25 barrier replace #minecraft:mineable/pickaxe
execute as @a[scores={finished=140..}] at @s run fill 17 1 25 17 -2 46 barrier replace #minecraft:mineable/pickaxe
execute as @a[scores={finished=140..}] at @s run fill 17 1 46 13 -2 46 barrier replace #minecraft:mineable/pickaxe
execute as @a[scores={finished=140..}] at @s run tp @a 15 -2 36 -90 0
execute as @a[scores={finished=140..}] at @s run tag @a remove finished
execute as @a[scores={finished=140..}] at @s run gamemode adventure @a
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a hit_chicken 0
execute as @a[scores={finished=140..}] at @s run clear @a
execute as @a[scores={finished=140..}] at @s run scoreboard players set @a playtimeMs1 0
... playtimeMs2, Se1, Se2, Mi1, Mi2 ...
execute as @a[scores={finished=140..}] at @s run scoreboard players set @s finished 0
```

- 7 s after the first finisher, **all players** are teleported to the start, put in adventure, cleared, and the three barrier walls are rebuilt. **[H]**
- **Bug B3:** this is global, not per-player. Unfinished racers are pulled back and their timers reset too. **[H]**
- The three barrier walls are north z25, east x17, south z46; the west x13 wall is solid. **[H]**
- The `replace air` and `replace #minecraft:mineable/pickaxe` pairs mean any player concrete in those planes becomes barrier. **[H]**
- `scoreboard players set @s finished 0` only resets the players who actually have `finished>=140`; others are already 0. **[H]**

### 7.5 Timer maths (lines 53–70)

```mcfunction
execute as @a[tag=playing] at @s run scoreboard players add @s playtimeMs1 5
execute as @a[tag=playing] at @s run scoreboard players add @s spawntime 1
...
execute as @a[scores={playtimeMs1=10..}] at @s run scoreboard players add @s playtimeMs2 1
execute as @a[scores={playtimeMs1=10..}] at @s run scoreboard players set @s playtimeMs1 0
execute as @a[scores={playtimeMs2=10..}] at @s run scoreboard players add @s playtimeSe1 1
...
execute as @a[scores={playtimeSe1=10..}] at @s run scoreboard players add @s playtimeSe2 1
execute as @a[scores={playtimeSe2=6..}] at @s run scoreboard players add @s playtimeMi1 1
execute as @a[scores={playtimeMi1=10..}] at @s run scoreboard players add @s playtimeMi2 1
```

- 5 points per tick = 100 points/s = 0.01 s per point; display resolution 0.05 s. **[H]**
- `spawntime` counts ticks and drives the cleanup anchors. **[H]**
- The timer runs only while `playing`; the tag is added at GO and removed at the first finish (bug B1). **[H]**

### 7.6 Cleanup anchors (lines 56–58)

```mcfunction
execute as @a[scores={spawntime=20..}] at @s run summon armor_stand ~ ~10 ~ {NoGravity:1b,Marker:1b,Silent:1b,Invulnerable:1b,Small:1b,Invisible:1b,NoBasePlate:1b,Tags:["remove_block"]}
execute as @s[scores={spawntime=5..}] at @s run execute as @a[scores={respawn=..-30}] at @s run summon armor_stand ~ ~10 ~ {...}
execute as @a[scores={spawntime=20..}] at @s run scoreboard players set @s spawntime 0
```

- Every 20 ticks (1 s), each playing player leaves an invisible marker armor stand 10 blocks above their feet. **[H]**
- Line 57 is **dead code**: it uses `as @s` but the function runs without an entity source, so `@s` matches nothing. It was probably meant to add an anchor near a falling player (`respawn <= -30`). **[H]** command; **[M]** no-op.
- The markers are the cleanup anchors; they are killed at the first finisher's `finished=60`. **[H]**

### 7.7 HUD (lines 73–82)

Eight actionbar commands, one per team tag, each showing the same six score digits in the team colour:
`red`, `orange` (gold), `yellow`, `lime` (green), `green` (dark_green), `cyan` (dark_aqua), `aqua`, `blue`, `purple` (dark_purple), `pink` (light_purple). The title text is a JSON list with score components and `bold:true`.

- The HUD is **per-player actionbar**, not a sidebar/boss bar. **[H]**
- The default tag is `red` (colour menu §8), so a player who never selects sees a red timer. **[H]**
- The timer continues to show after finish (frozen) because the colour tag remains and the scores stop. **[H]**

### 7.8 Kit (lines 86–116)

```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:["minecraft:red_concrete",...10 colours...],Unbreakable:1b,Enchantments:[{id:"minecraft:efficiency",lvl:2s}]} 1
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:["minecraft:red_concrete","minecraft:blackstone",...40 blocks...]} 2
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:[...]} 1
...
```

- Every tick, every player gets an unbreakable Efficiency II iron pickaxe in hotbar slot 0 and a team-coloured concrete in the offhand. **[H]**
- The two concrete commands per colour set count 2 then count 1 in the same tick; **the final count is always 1**. The first command is redundant. **[H]**
- `CanPlaceOn` is a whitelist of 40 map materials (39 for yellow/green); the player can only place concrete against one of those faces, then chain off the placed concrete. This is the bridge mechanic. **[H]** list; **[M]** client placement behaviour. Exact per-line lists: `item_nbt_lines.csv`.
- `CanDestroy` lists only the 10 team concrete colours (the 8 selectable + yellow/green), so the pickaxe cannot mine the map. **[H]**
- The item NBT syntax is 1.19; on 1.21.1 all these `item replace` commands fail (see §11). **[H]**

## 8. Colour menu (`color.mcfunction`)

- `scoreboard players enable @a color` every tick. **[H]**
- If a player runs `/trigger color`, `color>=1` triggers one `tellraw` menu; the function then sets `color` back to 0. **[H]**
- The menu (line 3) is a JSON list with clickable `[Red]`, `[Orange]`, `[Lime]`, `[Cyan]`, `[Aqua]`, `[Blue]`, `[Purple]`, `[Pink]`; `[Yellow]` and `[Green]` are struck through with hover “This color is used for building this map”. **[H]**
- Click values: red 1, orange 2, lime 4, cyan 6, aqua 7, blue 8, purple 9, pink 10. Yellow 3 and green 5 are skipped; lime accepts `3..4`, cyan accepts `5..6`. **[H]**
- Default: `selectColor=0` matches `0..1` → **red**. **[H]**
- There is **no in-world instruction** to run `/trigger color`; the menu is undiscoverable unless the player knows the trigger. **[H]** (no sign/book/command block sets `color`).

## 9. State machine summary

| State | Entered by | What runs |
|---|---|---|
| Idle / cage | join; loop every tick | kit, default red tag, cage block clear, spawnpoint, timer 00:00 |
| Starting | sign click → `tag @a add starting` | countdown 1→140; 2 s quiet + 5..1; GO |
| Playing | countdown 140 | `playing`, barriers removed, timer +5/tick, anchors every 1 s |
| Finished | `hit_chicken>=1` | `finished`, spectator, time title; 3 s later cleanup; 7 s later reset |
| Reset | `finished=140` | all tp to start, adventure, clear, barriers rebuilt, timers 0 |

`respawn` (feet ≤ -41) runs in every state except spectator (spectators can fly below but `tp` still applies if their Y score is ≤ -41; `data get Pos[1]` still runs for spectators, so a spectator below -41 would be teleported to the start — harmless). **[H]** command / **[M]** effect.

## 10. Bugs and edge cases

| # | Finding | Confidence |
|---|---|---|
| B1 | First finisher removes `playing` from everyone → all timers stop. | H |
| B2 | Cleanup at 60 ticks kills every `remove_block` stand → other players' concrete can survive. | H/M |
| B3 | Reset at 140 ticks teleports and resets **all** players, not just finishers. | H |
| B4 | `loop` line 57 (`execute as @s`) is a no-op dead command. | H/M |
| B5 | Two offhand commands (2 then 1) → the count is always 1; the 2 is redundant. | H |
| B6 | Sign click event is on blank line 1; visible “Start” is line 2 with no click event. | H/M |
| B7 | Source world GameType is 1 (creative); imported world GameType is 0 (survival, MV SURVIVAL). The pack only sets adventure at reset, never on join. A first-time source player can fly/break; an imported survival player can break wool/glass/terrain by hand (the pickaxe restriction only covers the pickaxe), the wool wall is 1 thick, and MV `pvp=true` with no damage guard. | H |
| B8 | No checkpoints: a fall returns to start and the timer keeps running. | H |
| B9 | A late joiner never gets `starting`/`playing`; they can finish with 00:00:00. | H/M |
| B10 | Finish detection has no `playing`/phase check; any hit on a chicken counts if a player can reach one. | H/M |
| B11 | The first finisher's 7 s reset ends the run for everyone. | H |
| B12 | Chickens are `PersistenceRequired=0`; no chunk tickets. They can despawn if the finish chunks unload. | H/M |
| B13 | Colour menu requires `/trigger color`; no sign/book/command points to it. | H |
| B14 | Yellow/green are deliberately disabled (map-building colours). | H |
| B15 | `tag @a add starting` starts everyone, not just the clicker. | H |
| B16 | `hit_chicken` depends on the Resistance effect; if it lapses, a punch may damage/kill a chicken instead of incrementing. | M |
| B17 | `title @a` inside `as @a[scores=…]` duplicates title commands once per player. | H |
| B18 | 13 gamerules + `difficulty` every tick; no functional issue, just noise. | H |
| B19 | No spectator confinement; finishers can fly through the shell and see the external base. | H/M |
| B20 | No time limit; if nobody finishes, the course never resets. | H |

## 11. 1.21.1 compatibility — exact likely failures

Target: Paper 1.21.1, `version.json` data pack format **48**, world version 3955. Source world DataVersion 3117 (1.19.1).

**Imported state (A12, [H]):** `mcc_tgttos_pits` `level.dat` has `Enabled=[vanilla,file/bukkit,paper]`; `file/Pits` is **not enabled** (on disk as `datapacks/Pits`). The source archive had `Enabled=[vanilla,Fabric Mods,file/Pits]`. So the first failure is “not loaded”; the folder rename and item NBT are the next two. Exact tables: `load_and_121.md`, `../server_import/data/datapacks.csv`, `../server_import/data/worlds.csv`.

### 11.1 Directory rename (fatal, pack-wide)

Minecraft 1.21 renamed data pack directories to singular. The 1.21.1 server jar uses `data/minecraft/advancement`, `loot_table`, `recipe`, `structure`, `tags/block`, `tags/entity_type`, etc.; the function loader looks for `function` (the `ServerFunctionLibrary` contains the literal `function` and `.mcfunction`). **[H]** — Paper 1.21.1 jar inspection.

Pits has:
- `data/tgttos/functions/*.mcfunction` → 1.21 looks in `data/tgttos/function/` → no functions exist.
- `data/minecraft/tags/functions/load.json` / `tick.json` → 1.21 looks in `data/minecraft/tags/function/` → no load/tick tags.

Result: even if the pack is enabled, it is **inert**. No objectives, no gamerules, no kit, no countdown, no respawn, no finish, no reset. The Start sign still runs `tag @a add starting`, but no function reads it. The barrier cage never opens (except by creative). **[H]** for paths; **[H/M]** for the inert result.

### 11.2 `pack_format` mismatch (warning, not the main failure)

`pack_format: 10` vs 48. 1.21 marks the pack incompatible. In the imported world, `file/Pits` is not in `DataPacks.Enabled` at all (A12), so the pack is not loaded even before the format check; if enabled, 1.21 normally still loads it with a warning, but §11.1 makes it contribute nothing. **[H]** mismatch; **[M]** “still loaded”.

### 11.3 Item NBT → data components (fatal for the kit)

1.20.5+ removed the old item NBT argument. `item replace ... with iron_pickaxe{...}` is a parse error in 1.21.1; the item parser expects `[` components (`SYNTAX_START_COMPONENTS`, `DataComponentPatch`). **[H]** — `ItemParser` class.

Every `item replace` line (1 pickaxe + 2 per colour = 21 commands) fails:
```mcfunction
item replace entity @a hotbar.0 with iron_pickaxe{CanDestroy:[...],Unbreakable:1b,Enchantments:[...]} 1
item replace entity @a[tag=red] weapon.offhand with red_concrete{CanPlaceOn:[...]} 2
```
Result: no pickaxe, no concrete; bridging is impossible. The rest of the loop runs (start/finish/timer), but the map cannot be completed without creative. **[H]** syntax; **[H]** effect.

1.21 equivalents (documentation only; not applied):
- `iron_pickaxe[unbreakable={},enchantments={levels:{"minecraft:efficiency":2}},can_break={blocks:[...]}]`
- `red_concrete[can_place_on={blocks:[...]}]`

The **saved** playerdata items (pickaxe + cyan concrete) are DataFixed to components on world upgrade; only the command strings are not. **[H]** — `ItemStackComponentizationFix` / `ConverterItemStackToDataComponents` contain `minecraft:can_break` / `can_place_on`.

### 11.4 Other syntax/version checks

| Command / feature | 1.21.1 status | Confidence |
|---|---|---|
| `scoreboard objectives add … minecraft.custom:minecraft.damage_dealt_resisted` | valid; stat exists (`Stats.class`, `minecraft.custom:minecraft.` in CraftMagicNumbers) | H/M |
| `scoreboard objectives add … trigger` | valid | H |
| `effect give @e[type=chicken] resistance 1 5 true` | valid (effect id unchanged) | H |
| `fill … replace #minecraft:mineable/pickaxe` | valid (block tag exists) | H |
| `summon armor_stand ~ ~10 ~ {NoGravity:1b,…}` | valid (entity NBT unchanged) | H |
| `data get entity @s Pos[1]` | valid | H |
| `tp @s 15 -2 36 -90 0` | valid | H |
| `title` / `tellraw` JSON text components | valid in 1.21.1; `clickEvent` still accepted | H |
| `clickEvent` `run_command` with leading `/` | accepted in 1.21.1; changed in 1.21.5+ | M |
| Sign `Text1..4` | DataFixed to `front_text` on world upgrade; click event should survive | M |
| `setworldspawn`, `spawnpoint @a`, `clear @a`, `gamemode adventure @a` | valid | H |
| `difficulty peaceful`, `gamerule freezeDamage` | valid | H |

### 11.5 Failure chain to expect on the live 1.21.1 server

0. Imported `level.dat`: `file/Pits` is not enabled → no pack loaded (A12).
1. If enabled: `Pits` is listed as incompatible (format 10 vs 48).
2. No load/tick functions run (directory rename). The map is a static build.
3. Even if the folders are renamed, the 21 `item replace` commands fail; no kit.
4. Even with the kit fixed, the sign’s click is on the blank line, the timer stops at the first finish, and the 7 s reset ends the run for everyone.
5. The map is salvageable only with a port (folder rename + item components + sign/start fixes); none of that is done here.

## 12. What is *not* in the pack

- No checkpoints, no teams, no scoring, no rounds, no time limit, no spectators box, no chunk tickets, no container/chest logic, no command blocks, no resource pack.
- No `block_reset.mcfunction`; block cleanup is the armor-stand trick in `loop`.
- No function to set adventure on join; only the reset does.
- No anti-skip / no barrier around the pits; a player can bridge directly from Road A to the finish or pillar onto the roofs. **[H]** geometry + **[M]** gameplay.
